package com.example.senderos.ui.screens

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.RECEIVER_EXPORTED
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun MapScreen(
    mapViewModel: MapViewModel = viewModel()
) {
    val context = LocalContext.current

    // 1) Permisos
    val hasLocationPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val hasActivityPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACTIVITY_RECOGNITION
    ) == PackageManager.PERMISSION_GRANTED

    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    // 2) Estado local de ubicación
    val currentLocationState = remember { mutableStateOf<GeoPoint?>(null) }

    // 3) Estado de actividad desde el ViewModel
    val currentActivity by mapViewModel.currentActivity.collectAsState()

    // 4) Referencias de mapa y marcador
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }
    val markerRef  = remember { mutableStateOf<Marker?>(null) }

    // 5) Control de centrado automático
    var shouldFollowLocation by remember { mutableStateOf(true) }

    // 6) Configurar PendingIntent para recibir ActivityUpdates
    val activityIntent = remember {
        Intent(context, ActivityUpdatesReceiver::class.java)
            .apply { action = ActivityUpdatesReceiver.ACTION }
            .let { intent ->
                PendingIntent.getBroadcast(
                    context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
    }

    // 7) Solicitar Activity Recognition solo si tenemos permiso
    DisposableEffect(hasActivityPermission) {
        if (hasActivityPermission) {
            val client = ActivityRecognition.getClient(context)
            client.requestActivityUpdates(5_000L, activityIntent)
            onDispose { client.removeActivityUpdates(activityIntent) }
        } else {
            onDispose { }
        }
    }

    // 8) Capturar el broadcast y pasarlo al ViewModel
    DisposableEffect(context) {
        val filter = IntentFilter(ActivityUpdatesReceiver.ACTION)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (ActivityRecognitionResult.hasResult(intent)) {
                    val result = ActivityRecognitionResult.extractResult(intent)!!
                    mapViewModel.onActivityRecognitionResult(result)
                }
            }
        }
        // Ahora con flag explícito para Android 14+
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            RECEIVER_EXPORTED
        )
        onDispose { context.unregisterReceiver(receiver) }
    }

    Box(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        // --- MapView ---
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                Configuration.getInstance()
                    .load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
                MapView(ctx).also { map ->
                    map.setTileSource(TileSourceFactory.MAPNIK)
                    map.setMultiTouchControls(true)
                    map.controller.setZoom(18.0)
                    map.setOnTouchListener { v, ev ->
                        if (ev.action == MotionEvent.ACTION_DOWN) {
                            shouldFollowLocation = false
                            v.performClick()
                        }
                        false
                    }
                    mapViewRef.value = map

                    Marker(map).apply {
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        position = GeoPoint(0.0, 0.0)
                    }.also { marker ->
                        map.overlays.add(marker)
                        markerRef.value = marker
                    }
                }
            }
        )

        // --- Panel superior: coordenadas + actividad ---
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color.DarkGray.copy(alpha = 0.8f))
                .padding(8.dp)
                .align(Alignment.TopCenter)
        ) {
            if (!hasLocationPermission) {
                Text(
                    text = "Permiso de ubicación no concedido",
                    color = Color.White, fontSize = 18.sp
                )
            } else {
                currentLocationState.value?.let {
                    Text(
                        text = "Lat: ${it.latitude}, Lon: ${it.longitude}",
                        color = Color.White, fontSize = 18.sp
                    )
                } ?: Text(
                    text = "Buscando ubicación…",
                    color = Color.LightGray, fontSize = 18.sp
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Actividad: $currentActivity",
                color = Color.White, fontSize = 18.sp
            )
        }

        // --- Botón para centrar ---
        Button(
            onClick = {
                shouldFollowLocation = true
                currentLocationState.value?.let {
                    mapViewRef.value?.controller?.animateTo(it)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Text("Ubi. actual")
        }
    }

    // 9) Location updates
    DisposableEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            val locationRequest = LocationRequest.Builder(5_000L)
                .setMinUpdateIntervalMillis(2_000L)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build()

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val loc = result.lastLocation ?: return
                    // 9.1 Actualiza ViewModel
                    mapViewModel.onLocationChanged(loc.latitude, loc.longitude)
                    // 9.2 Actualiza UI local
                    val point = GeoPoint(loc.latitude, loc.longitude)
                    currentLocationState.value = point
                    if (shouldFollowLocation) {
                        mapViewRef.value?.controller?.animateTo(point)
                    }
                    markerRef.value?.position = point
                    mapViewRef.value?.invalidate()
                }
            }

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
            onDispose { fusedLocationClient.removeLocationUpdates(locationCallback) }
        } else {
            onDispose { }
        }
    }
}
