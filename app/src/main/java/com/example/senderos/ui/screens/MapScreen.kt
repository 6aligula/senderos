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
import androidx.compose.material3.*
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
import org.osmdroid.views.overlay.Polyline
import android.graphics.Color as AndroidColor

@Composable
fun MapScreen(
    mapViewModel: MapViewModel = viewModel()
) {
    val context = LocalContext.current

    //----------------------------------------------------------------------
    // Permisos
    //----------------------------------------------------------------------
    val hasLocationPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val hasActivityPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACTIVITY_RECOGNITION
    ) == PackageManager.PERMISSION_GRANTED
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    //----------------------------------------------------------------------
    // Estados locales
    //----------------------------------------------------------------------
    val currentLocationState = remember { mutableStateOf<GeoPoint?>(null) }

    //----------------------------------------------------------------------
    // Estados del ViewModel
    //----------------------------------------------------------------------
    val currentActivity       by mapViewModel.currentActivity.collectAsState()
    val trackPointsState      by mapViewModel.trackPoints.collectAsState()
    val routesListState       by mapViewModel.routes.collectAsState()              // ← NUEVO
    val selectedRoutePoints   by mapViewModel.selectedRoutePoints.collectAsState() // ← NUEVO

    //----------------------------------------------------------------------
    // Referencias de mapa / overlays
    //----------------------------------------------------------------------
    val mapViewRef  = remember { mutableStateOf<MapView?>(null) }
    val markerRef   = remember { mutableStateOf<Marker?>(null) }
    val trackRef    = remember { mutableStateOf<Polyline?>(null) }
    val routeRef    = remember { mutableStateOf<Polyline?>(null) }                // ← NUEVO

    var shouldFollowLocation by remember { mutableStateOf(true) }

    //----------------------------------------------------------------------
    // 1. Reconocimiento de actividad (sin cambios)
    //----------------------------------------------------------------------
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
    DisposableEffect(hasActivityPermission) {
        if (hasActivityPermission) {
            val client = ActivityRecognition.getClient(context)
            client.requestActivityUpdates(5_000L, activityIntent)
            onDispose { client.removeActivityUpdates(activityIntent) }
        } else onDispose { }
    }

    //----------------------------------------------------------------------
    // 2. Broadcast receiver para los updates
    //----------------------------------------------------------------------
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
        ContextCompat.registerReceiver(context, receiver, filter, RECEIVER_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }

    //----------------------------------------------------------------------
    // 3.  PEDIR lista de rutas al arrancar
    //----------------------------------------------------------------------
    LaunchedEffect(Unit) { mapViewModel.fetchRoutesList() }   // ← NUEVO

    //======================================================================
    //                               UI
    //======================================================================
    Box(
        Modifier.fillMaxSize().systemBarsPadding()
    ) {
        //------------------------------------------------------------------
        // MapView
        //------------------------------------------------------------------
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

                    // marcador
                    Marker(map).apply {
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        position = GeoPoint(0.0, 0.0)
                    }.also { markerRef.value = it }

                    // polyline del track
                    Polyline(map).also { poly ->
                        poly.outlinePaint.strokeWidth = 6f
                        poly.outlinePaint.color = AndroidColor.MAGENTA
                        map.overlays.add(poly)
                        trackRef.value = poly
                    }

                    // polyline de la ruta seleccionada
                    Polyline(map).also { poly ->
                        poly.outlinePaint.strokeWidth = 8f
                        poly.outlinePaint.color = AndroidColor.CYAN
                        map.overlays.add(poly)
                        routeRef.value = poly
                    }
                }
            }
        )

        //------------------------------------------------------------------
        // Panel superior
        //------------------------------------------------------------------
        Column(
            Modifier.fillMaxWidth()
                .background(Color.DarkGray.copy(alpha = 0.8f))
                .padding(8.dp)
                .align(Alignment.TopCenter)
        ) {
            if (!hasLocationPermission) {
                Text("Permiso de ubicación no concedido", color = Color.White, fontSize = 18.sp)
            } else {
                currentLocationState.value?.let {
                    Text("Lat: ${it.latitude}, Lon: ${it.longitude}",
                        color = Color.White, fontSize = 18.sp)
                } ?: Text("Buscando ubicación…", color = Color.LightGray, fontSize = 18.sp)
            }
            Spacer(Modifier.height(4.dp))
            Text("Actividad: $currentActivity", color = Color.White, fontSize = 18.sp)
        }

        //------------------------------------------------------------------
        //  Botón: centrar
        //------------------------------------------------------------------
        Button(
            onClick = {
                shouldFollowLocation = true
                currentLocationState.value?.let {
                    mapViewRef.value?.controller?.animateTo(it)
                }
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) { Text("Ubi. actual") }

        //------------------------------------------------------------------
        //  Botón + Dropdown: lista de rutas (NUEVO)
        //------------------------------------------------------------------
        var expanded by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
        ) {
            Button(onClick = { expanded = true }) { Text("Rutas") }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                routesListState.forEach { id ->
                    DropdownMenuItem(
                        text = { Text(id) },
                        onClick = {
                            expanded = false
                            mapViewModel.fetchRouteById(id)
                        }
                    )
                }
            }
        }
    }

    //----------------------------------------------------------------------
    // 4.  Location updates
    //----------------------------------------------------------------------
    DisposableEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            val locationRequest = LocationRequest.Builder(5_000L)
                .setMinUpdateIntervalMillis(2_000L)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMinUpdateDistanceMeters(5f)
                .build()

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val loc = result.lastLocation ?: return
                    if (loc.accuracy > 10f) return

                    mapViewModel.onLocationChanged(loc.latitude, loc.longitude)

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
        } else onDispose { }
    }

    //----------------------------------------------------------------------
    // 5.  Redibujar TRACK
    //----------------------------------------------------------------------
    LaunchedEffect(trackPointsState) {
        val pts = trackPointsState ?: return@LaunchedEffect
        val geo = pts.map { (lat, lon) -> GeoPoint(lat, lon) }
        trackRef.value?.setPoints(geo)
        mapViewRef.value?.invalidate()
    }

    //----------------------------------------------------------------------
    // 6.  Redibujar RUTA seleccionada (NUEVO)
    //----------------------------------------------------------------------
    LaunchedEffect(selectedRoutePoints) {
        val pts = selectedRoutePoints ?: return@LaunchedEffect
        val geo = pts.map { (lat, lon) -> GeoPoint(lat, lon) }
        routeRef.value?.setPoints(geo)
        mapViewRef.value?.invalidate()
    }
}
