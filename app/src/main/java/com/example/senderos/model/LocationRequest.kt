package com.example.senderos.model

import kotlinx.serialization.Serializable

@Serializable
data class LocationRequest(
    val device_id: String,   // p.e. Settings.Secure.ANDROID_ID
    val lat: Double,
    val lon: Double,
    val timestamp: Long      // epoch millis
)
