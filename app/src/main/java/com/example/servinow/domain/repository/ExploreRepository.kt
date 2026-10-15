package com.example.servinow.domain.repository
import com.example.servinow.domain.model.ExploreActivity
interface ExploreRepository {
 fun jobs(): List<com.example.servinow.domain.model.Job>
 fun read(): ExploreActivity
 fun save(activity: ExploreActivity): Boolean
}
