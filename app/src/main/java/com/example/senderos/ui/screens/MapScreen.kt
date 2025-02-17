package com.example.senderos.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import androidx.compose.foundation.layout.systemBarsPadding

@Composable
fun MapScreen() {
    val context = LocalContext.current
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    // Estado para almacenar la ubicación actual
    val currentLocation = remember { mutableStateOf<GeoPoint?>(null) }
    // Referencias para actualizar el MapView y el Marker
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }
    val markerRef = remember { mutableStateOf<Marker?>(null) }
    // Estado para saber si debemos centrar el mapa automáticamente
    var shouldFollowLocation by remember { mutableStateOf(true) }

    // Verificar permisos de ubicación
    val hasLocationPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

    // Usamos un Box para superponer el mapa, el panel fijo superior y el botón
    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding() // Respeta la zona segura del dispositivo
    ) {
        // MapView ocupa toda la pantalla
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                // Cargar la configuración de osmdroid
                Configuration.getInstance().load(
                    ctx,
                    ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
                )
                // Crear y configurar el MapView
                val mapView = MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(18.0)
                }
                // Detectamos si el usuario toca el mapa para desactivar el centrado automático
                mapView.setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        shouldFollowLocation = false
                    }
                    false // Permitir que el MapView maneje el evento
                }
                mapViewRef.value = mapView

                // Crear un Marker para la ubicación actual
                val marker = Marker(mapView).apply {
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    position = GeoPoint(0.0, 0.0) // Posición inicial (se actualizará)
                }
                mapView.overlays.add(marker)
                markerRef.value = marker

                mapView
            }
        )

        // Panel fijo en la parte superior para mostrar las coordenadas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(Color.DarkGray.copy(alpha = 0.8f))
                .align(Alignment.TopCenter),
            contentAlignment = Alignment.Center
        ) {
            if (!hasLocationPermission) {
                Text(
                    text = "Permiso de ubicación no concedido",
                    color = Color.White,
                    fontSize = 20.sp
                )
            } else {
                currentLocation.value?.let { loc ->
                    Text(
                        text = "Lat: ${loc.latitude}, Lon: ${loc.longitude}",
                        color = Color.White,
                        fontSize = 20.sp
                    )
                } ?: Text(
                    text = "Buscando ubicación...",
                    color = Color.LightGray,
                    fontSize = 20.sp
                )
            }
        }

        // Botón fijo en la esquina inferior derecha para recenterar el mapa a la ubicación actual
        Button(
            onClick = {
                shouldFollowLocation = true
                currentLocation.value?.let { loc ->
                    mapViewRef.value?.controller?.animateTo(loc)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Text(text = "Ubi. actual")
        }
    }

    // Solicitar actualizaciones de ubicación cada 5 segundos (si hay permisos)
    DisposableEffect(key1 = context) {
        if (hasLocationPermission) {
            val locationRequest = LocationRequest.create().apply {
                interval = 5000L       // 5 segundos
                fastestInterval = 2000L  // Mínimo cada 2 segundos
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }
            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    val location = locationResult.lastLocation
                    if (location != null) {
                        val geoPoint = GeoPoint(location.latitude, location.longitude)
                        currentLocation.value = geoPoint
                        // Si seguimos el seguimiento (no se movió el mapa), centrar
                        if (shouldFollowLocation) {
                            mapViewRef.value?.controller?.animateTo(geoPoint)
                        }
                        // Actualizar la posición del Marker
                        markerRef.value?.position = geoPoint
                        mapViewRef.value?.invalidate()
                    }
                }
            }
            try {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
            } catch (ex: SecurityException) {
                // Manejo de excepción si faltan permisos
            }
            onDispose {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            }
        } else {
            onDispose { }
        }
    }
}
