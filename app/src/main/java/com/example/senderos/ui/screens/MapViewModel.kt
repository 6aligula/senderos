package com.example.senderos.ui.screens

import android.app.Application
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.senderos.model.LocationRequest
import com.example.senderos.model.UserActivity
import com.example.senderos.network.LocationClient       // 🆕
import com.example.senderos.utils.LocationSenderWorker
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.*

//────────────────────────── EXISTENTE ──────────────────────────
class MapViewModel(app: Application) : AndroidViewModel(app) {

    // —— Estado de ubicación ——
    private val _current = MutableStateFlow<LocationRequest?>(null)
    val current = _current.asStateFlow()

    private val MIN_DISTANCE_M = 5.0
    private var lastSent: LocationRequest? = null

    // —— Estado de actividad ——
    private val _currentActivity = MutableStateFlow(UserActivity.UNKNOWN)
    val currentActivity = _currentActivity.asStateFlow()

    /** Lógica previa: cuando cambia la ubicación */
    fun onLocationChanged(newLat: Double, newLon: Double) {
        val payload = LocationRequest(
            device_id = deviceId(),           // ⬅️ helper reutilizado
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
    fun classifyActivity(type: Int): UserActivity = when (type) {
        DetectedActivity.STILL      -> UserActivity.STILL
        DetectedActivity.WALKING    -> UserActivity.WALKING
        DetectedActivity.RUNNING    -> UserActivity.RUNNING
        DetectedActivity.IN_VEHICLE -> UserActivity.IN_VEHICLE
        else                        -> UserActivity.UNKNOWN
    }

    fun onActivityRecognitionResult(result: ActivityRecognitionResult) {
        val mostLikely = result.probableActivities
            .maxByOrNull { it.confidence } ?: return

        _currentActivity.value = classifyActivity(mostLikely.type)
    }

    //────────────────────────── NUEVO ──────────────────────────
    // ——— Track guardado en Firestore ———
    private val _trackPoints = MutableStateFlow<List<Pair<Double, Double>>?>(null)
    val trackPoints = _trackPoints.asStateFlow()

    // ——— Ruta “snap-to-road” devuelta por el backend ———
    private val _routePoints = MutableStateFlow<List<Pair<Double, Double>>?>(null)
    val routePoints = _routePoints.asStateFlow()

    /** Descarga el track completo del dispositivo y lo expone en `trackPoints` */
    fun fetchTrack() {
        viewModelScope.launch {
            _trackPoints.value = LocationClient.getTrack(deviceId())
        }
    }

    /**
     * Calcula la ruta óptima desde la ubicación actual hasta [target] y la expone en `routePoints`.
     * @param target par (lat, lon) destino.
     * @param profile "foot-walking", "driving-car", etc.
     */
    fun fetchRouteTo(
        target: Pair<Double, Double>,
        profile: String = "foot-walking"
    ) {
        val start = _current.value ?: return      // aún sin ubicación
        viewModelScope.launch {
            _routePoints.value = LocationClient.getRoute(
                start = start.lat to start.lon,
                end   = target,
                profile = profile
            )
        }
    }

    /** Limpia la ruta calculada (p.e. al cancelar navegación) */
    fun clearRoute() {
        _routePoints.value = null
    }

    // ——— Helper reutilizado ———
    private fun deviceId(): String =
        Settings.Secure.getString(
            getApplication<Application>().contentResolver,
            Settings.Secure.ANDROID_ID
        )
}
