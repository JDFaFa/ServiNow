package com.example.servinow.domain.repository
import com.example.servinow.domain.model.ServiceData
interface ServiceRepository {
 fun professionals(): List<com.example.servinow.domain.model.Professional>
 fun read(): Result<ServiceData>
 fun save(data: ServiceData): Boolean
}
