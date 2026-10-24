package com.example.servinow.domain.repository

interface GeocodingRepository {
    suspend fun address(latitude: Double, longitude: Double): String?
}
