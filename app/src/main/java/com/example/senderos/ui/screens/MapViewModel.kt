package com.example.senderos.ui.screens

import android.app.Application
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.senderos.model.LocationRequest
import com.example.senderos.model.UserActivity
import com.example.senderos.network.LocationClient
import com.example.senderos.network.RoutesClient
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

    private val MIN_DISTANCE_M = 5.0
    private var lastSent: LocationRequest? = null

    // —— Estado de actividad ——
    private val _currentActivity = MutableStateFlow(UserActivity.UNKNOWN)
    val currentActivity = _currentActivity.asStateFlow()

    // —— Estado de pasos ——
    private val _stepCount = MutableStateFlow(0)
    val stepCount = _stepCount.asStateFlow()

    //-----------------------------------------------------------------------
    // 1. UPLOAD de posición
    //-----------------------------------------------------------------------
    fun onLocationChanged(newLat: Double, newLon: Double) {
        val payload = LocationRequest(
            device_id = deviceId(),
            lat       = newLat,
            lon       = newLon,
            timestamp = System.currentTimeMillis()
        )
        _current.value = payload

        if (_currentActivity.value != UserActivity.STILL && shouldUpload(payload)) {
            lastSent = payload
            viewModelScope.launch {
                LocationSenderWorker.schedule(getApplication(), payload)
            }
        }
    }

    private fun shouldUpload(now: LocationRequest): Boolean {
        val prev = lastSent ?: return true
        val dist = haversine(prev.lat, prev.lon, now.lat, now.lon)
        return dist >= MIN_DISTANCE_M
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) *
                cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

    //-----------------------------------------------------------------------
    // 2. Clasificación de actividad
    //-----------------------------------------------------------------------
    fun classifyActivity(type: Int): UserActivity = when (type) {
        DetectedActivity.STILL      -> UserActivity.STILL
        DetectedActivity.WALKING    -> UserActivity.WALKING
        DetectedActivity.RUNNING    -> UserActivity.RUNNING
        DetectedActivity.IN_VEHICLE -> UserActivity.IN_VEHICLE
        else                        -> UserActivity.UNKNOWN
    }

    fun onActivityRecognitionResult(result: ActivityRecognitionResult) {
        val mostLikely = result.probableActivities.maxByOrNull { it.confidence } ?: return
        _currentActivity.value = classifyActivity(mostLikely.type)
    }

    //-----------------------------------------------------------------------
    // 3. Track propio
    //-----------------------------------------------------------------------
    private val _trackPoints = MutableStateFlow<List<Pair<Double, Double>>?>(null)
    val trackPoints = _trackPoints.asStateFlow()

    fun fetchTrack() = viewModelScope.launch {
        _trackPoints.value = LocationClient.getTrack(deviceId())
    }

    //-----------------------------------------------------------------------
    // 4. Lista de rutas + puntos de ruta seleccionada
    //-----------------------------------------------------------------------
    private val _routes = MutableStateFlow<List<String>>(emptyList())
    val routes = _routes.asStateFlow()

    fun fetchRoutesList() = viewModelScope.launch {
        _routes.value = RoutesClient.getRoutesList().map { it.id }
    }

    private val _selectedRoutePoints = MutableStateFlow<List<Pair<Double, Double>>?>(null)
    val selectedRoutePoints = _selectedRoutePoints.asStateFlow()

    fun fetchRouteById(id: String) = viewModelScope.launch {
        _selectedRoutePoints.value = LocationClient.getRouteById(id)
    }

    fun clearSelectedRoute() {
        _selectedRoutePoints.value = null
    }

    fun updateStepCount(count: Int) {
        _stepCount.value = count
    }

    //-----------------------------------------------------------------------
    // Helper
    //-----------------------------------------------------------------------
    private fun deviceId(): String =
        Settings.Secure.getString(
            getApplication<Application>().contentResolver,
            Settings.Secure.ANDROID_ID
        )
}
