package com.example.senderos.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.senderos.model.LocationRequest
import com.example.senderos.utils.LocationSenderWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MapViewModel(app: Application) : AndroidViewModel(app) {

    private val _current = MutableStateFlow<LocationRequest?>(null)
    val current = _current.asStateFlow()

    private val MIN_DISTANCE_M = 20      // no spamear
    private var lastSent: LocationRequest? = null

    fun onLocationChanged(newLat: Double, newLon: Double) {
        val devId = android.provider.Settings.Secure.getString(
            getApplication<Application>().contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
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
                // Programamos el Worker para que se ocupe del envío (funciona en BG)
                LocationSenderWorker.schedule(getApplication(), payload)
            }
        }
    }

    private fun shouldUpload(now: LocationRequest): Boolean {
        val prev = lastSent ?: return true
        val dist = haversine(prev.lat, prev.lon, now.lat, now.lon)
        return dist >= MIN_DISTANCE_M
    }

    /** Distancia Haversine en metros */
    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) *
                Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }
}
