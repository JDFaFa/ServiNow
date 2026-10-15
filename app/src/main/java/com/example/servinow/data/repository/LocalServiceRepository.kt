package com.example.servinow.data.repository
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import com.example.servinow.domain.model.ServiceData
import com.example.servinow.domain.repository.ServiceRepository
import com.example.servinow.data.local.ServiceCodec
import com.example.servinow.data.demo.demoOffers
class LocalServiceRepository @Inject constructor(@ApplicationContext context: Context): ServiceRepository {
 private val prefs=context.getSharedPreferences("services_local",0)
 override fun professionals()=com.example.servinow.data.demo.professionals
 override fun read():Result<ServiceData> = runCatching { prefs.getString("data",null)?.let(ServiceCodec::decode) ?: ServiceData(offers=demoOffers) }
 override fun save(data:ServiceData):Boolean=runCatching { prefs.edit().putString("data",ServiceCodec.encode(data)).commit() }.getOrDefault(false)
}
