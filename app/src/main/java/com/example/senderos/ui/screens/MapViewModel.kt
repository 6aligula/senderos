package com.example.senderos.ui.screens

import android.app.Application
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.senderos.model.LocationRequest
import com.example.senderos.model.UserActivity
import com.example.senderos.utils.LocationSenderWorker
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.*

class MapViewModel(app: Application) : AndroidViewModel(app) {

    // —— Estado de ubicación ——
    private val _current = MutableStateFlow<LocationRequest?>(null)
    val current = _current.asStateFlow()

    private val MIN_DISTANCE_M = 20      // no spamear
    private var lastSent: LocationRequest? = null

    // —— Estado de actividad ——
    private val _currentActivity = MutableStateFlow(UserActivity.UNKNOWN)
    val currentActivity = _currentActivity.asStateFlow()

    /** Lógica previa: cuando cambia la ubicación */
    fun onLocationChanged(newLat: Double, newLon: Double) {
        val devId = Settings.Secure.getString(
            getApplication<Application>().contentResolver,
            Settings.Secure.ANDROID_ID
        )

        val payload = LocationRequest(
            device_id = devId,
            lat       = newLat,
            lon       = newLon,
            timestamp = System.currentTimeMillis()
        )
        _current.value = payload

        if (shouldUpload(payload)) {
            lastSent = payload
            viewModelScope.launch {
                LocationSenderWorker.schedule(getApplication(), payload)
            }
        }
    }

    /** Decide si enviar al servidor según distancia mínima */
    private fun shouldUpload(now: LocationRequest): Boolean {
        val prev = lastSent ?: return true
        val dist = haversine(prev.lat, prev.lon, now.lat, now.lon)
        return dist >= MIN_DISTANCE_M
    }

    /** Distancia Haversine en metros */
    private fun haversine(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val R = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2).pow(2.0) +
                Math.cos(Math.toRadians(lat1)) *
                Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2).pow(2.0)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }

    // —— Nueva lógica: clasificación de actividad ——

    /**
     * Traduce el código de DetectedActivity a nuestro enum UserActivity.
     */
    fun classifyActivity(type: Int): UserActivity = when (type) {
        DetectedActivity.STILL      -> UserActivity.STILL
        DetectedActivity.WALKING    -> UserActivity.WALKING
        DetectedActivity.RUNNING    -> UserActivity.RUNNING
        DetectedActivity.IN_VEHICLE -> UserActivity.IN_VEHICLE
        else                        -> UserActivity.UNKNOWN
    }

    /**
     * Recibe el resultado de la API de ActivityRecognition
     * y actualiza el estado de currentActivity.
     */
    fun onActivityRecognitionResult(result: ActivityRecognitionResult) {
        // Escoge la actividad con mayor confianza
        val mostLikely = result.probableActivities
            .maxByOrNull { it.confidence }
            ?: return

        _currentActivity.value = classifyActivity(mostLikely.type)
    }
}
