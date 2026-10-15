package com.example.servinow.features.services
import com.example.servinow.domain.model.Proposal
import com.example.servinow.domain.model.ServiceData
import com.example.servinow.domain.model.ServiceReview
import com.example.servinow.domain.validation.acceptProposal
import com.example.servinow.domain.validation.addReview
import com.example.servinow.domain.validation.finishService
import com.example.servinow.domain.validation.proposalError

import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.servinow.domain.repository.ServiceRepository
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

data class ServiceUiState(val data: ServiceData = ServiceData(),
    val job: String = "demo-paint", val title: String = "Pintar una habitación", val selected: String? = null,
    val profile: String = "andres", val price: String = "", val message: String = "", val availability: String = "",
    val duration: String = "", val stars: Int = 0, val review: String = "", val filter: String = "Todas", val error: String? = null)
@HiltViewModel
class ServiceViewModel @Inject constructor(private val repository: ServiceRepository) : ViewModel() {
    val professionals=repository.professionals()
    private val _state=MutableStateFlow(ServiceUiState());val state=_state.asStateFlow()
    private var readFailed=false
    init {
        repository.read().onSuccess { data -> _state.update { it.copy(data=data) } }
            .onFailure { readFailed=true; error("No se pudieron leer los servicios guardados. No se sobrescribirán automáticamente.") }
    }
    private fun save(data: ServiceData): Boolean {
        if(readFailed) { error("No se guardaron cambios para conservar los datos que no se pudieron leer."); return false }
        val stored=repository.save(data)
        if (!stored) { error("No se pudo guardar. Inténtalo de nuevo.");return false }
        _state.update { it.copy(data=data) };return true
    }
    fun error(value: String?) { _state.update { it.copy(error=value) } }
    fun form(price: String=state.value.price,message: String=state.value.message,availability: String=state.value.availability,duration: String=state.value.duration) { _state.update { it.copy(price=price,message=message,availability=availability,duration=duration) } }
    fun forJob(id: String,title: String) { _state.update { it.copy(job=id,title=title,selected=null,price="",message="",availability="",duration="",stars=0,review="") } }
    fun select(p: Proposal) { _state.update { it.copy(job=p.job,title=p.title,selected=p.id,profile=p.worker,stars=0,review="") } }
    fun profile(id: String) { _state.update { it.copy(profile=id) } }
    fun filter(value: String) { _state.update { it.copy(filter=value) } }
    fun rating(value: Int) { _state.update { it.copy(stars=value) } }
    fun review(value: String) { _state.update { it.copy(review=value) } }
    fun send(): Boolean {
        val s=state.value;proposalError(s.price,s.message,s.availability,s.duration)?.let { error(it);return false }
        if (s.job in s.data.accepted || s.job in s.data.completed) { error("Este trabajo ya no recibe ofertas.");return false }
        if (s.data.offers.any { it.job==s.job && it.mine }) { error("Ya enviaste una oferta para este trabajo.");return false }
        return save(s.data.copy(offers=s.data.offers+Proposal(UUID.randomUUID().toString(),s.job,s.title,"andres",s.price.toLong(),s.message.trim(),s.availability.trim(),s.duration.trim(),true)))
    }
    fun accept(): Boolean {
        val s=state.value;val next=acceptProposal(s.data,s.selected ?: return false)
        if(next==s.data) { error("El trabajo ya está asignado o la oferta no está disponible.");return false };return save(next)
    }
    fun finish(): Boolean {
        val s=state.value;val next=finishService(s.data,s.job)
        if(next==s.data) { error("El trabajo debe estar asignado y no finalizado.");return false };return save(next)
    }
    fun workerDone() {
        val s=state.value
        if (s.job !in s.data.accepted || s.job in s.data.completed) return
        if(save(s.data.copy(doneRequests=s.data.doneRequests+s.job))) error("Aviso guardado localmente. El solicitante aún debe confirmar la finalización; no se envió una notificación remota.")
    }
    fun publishReview(): Boolean {
        val s=state.value;val worker=s.data.offers.find { it.id==s.data.accepted[s.job] }?.worker ?: return false
        val next=addReview(s.data,ServiceReview(s.job,worker,s.stars,s.review.trim()))
        if(next==s.data) { error("Finaliza el trabajo, elige entre 1 y 5 estrellas y escribe una reseña de hasta 1000 caracteres. Solo se admite una reseña por trabajo.");return false }
        return save(next)
    }
}
