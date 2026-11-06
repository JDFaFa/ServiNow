package com.example.servinow.features.cloud

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.servinow.data.cloud.CloudGateway
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CloudToolsState(val busy: Boolean = false, val suggestion: String? = null, val imageUrl: String? = null, val error: String? = null)
@HiltViewModel
class CloudToolsViewModel @Inject constructor(private val gateway: CloudGateway) : ViewModel() {
    private val mutable = MutableStateFlow(CloudToolsState())
    val state = mutable.asStateFlow()
    fun clear() { if (!mutable.value.busy) mutable.value = CloudToolsState() }
    fun improve(text: String, language: String) = run { mutable.value = mutable.value.copy(suggestion = gateway.improve(text, language)) }
    fun upload(uri: String) = run { mutable.value = mutable.value.copy(imageUrl = gateway.upload(uri)) }
    private fun run(block: suspend () -> Unit) {
        if (mutable.value.busy) return
        mutable.value = CloudToolsState(busy = true)
        viewModelScope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.value = mutable.value.copy(error = e.message ?: "SERVICE_ERROR") }
            finally { mutable.value = mutable.value.copy(busy = false) }
        }
    }
}
