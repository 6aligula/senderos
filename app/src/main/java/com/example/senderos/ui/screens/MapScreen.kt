// ui/screens/MapScreen.kt
package com.example.senderos.ui.screens

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.example.senderos.ui.components.RequestActivityPermission
import com.example.senderos.ui.components.RequestLocationPermission
import com.example.senderos.utils.ActivityPermissionHelper
import com.example.senderos.utils.LocationPermissionHelper
import com.example.senderos.model.UserActivity
import com.google.android.gms.location.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import android.graphics.Color as AndroidColor

@SuppressLint("MissingPermission")
@Composable
fun MapScreen(
    mapViewModel: MapViewModel = viewModel()
) {
    val context = LocalContext.current
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    // 1) Permisos
    RequestLocationPermission(onGranted = {}, onDenied = {})
    RequestActivityPermission(onGranted = {}, onDenied = {})

    // 2) Estados
    val currentActivity by mapViewModel.currentActivity.collectAsState(initial = UserActivity.UNKNOWN)
    val trackPointsState by mapViewModel.trackPoints.collectAsState(initial = null)
    val routesListState by mapViewModel.routes.collectAsState(initial = emptyList())
    val selectedRoutePoints by mapViewModel.selectedRoutePoints.collectAsState(initial = null)

    // 3) Estado local mapa
    var shouldFollowLocation by remember { mutableStateOf(true) }
    val currentLocationState = remember { mutableStateOf<GeoPoint?>(null) }
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }
    val markerRef = remember { mutableStateOf<Marker?>(null) }
    val trackRef = remember { mutableStateOf<Polyline?>(null) }
    val routeRef = remember { mutableStateOf<Polyline?>(null) }

    // 4) ActivityRecognition (igual que antes)...
    val activityIntent = remember {
        Intent(context, ActivityUpdatesReceiver::class.java).apply { action = ActivityUpdatesReceiver.ACTION }
            .let { intent ->
                PendingIntent.getBroadcast(
                    context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
    }
    DisposableEffect(ActivityPermissionHelper.hasRecognitionPermission(context)) {
        if (ActivityPermissionHelper.hasRecognitionPermission(context)) {
            val client = ActivityRecognition.getClient(context)
            client.requestActivityUpdates(5000L, activityIntent)
            onDispose { client.removeActivityUpdates(activityIntent) }
        } else onDispose { }
    }
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

    // 5) Carga inicial de rutas
    LaunchedEffect(Unit) { mapViewModel.fetchRoutesList() }

    // UI principal
    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                Configuration.getInstance()
                    .load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(18.0)

                    // Desactiva seguimiento si se toca el mapa
                    setOnTouchListener { v, ev ->
                        if (ev.action == MotionEvent.ACTION_DOWN) {
                            shouldFollowLocation = false
                            v.performClick()
                        }
                        false
                    }

                    // Guardamos referencia al MapView
                    mapViewRef.value = this

                    // Creamos el Marker para la ubicación actual y lo añadimos a overlays
                    Marker(this).apply {
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        position = GeoPoint(0.0, 0.0)
                    }.also { marker ->
                        markerRef.value = marker
                        overlays.add(marker)
                    }

                    // Creamos Polyline para el track y lo añadimos a overlays
                    Polyline(this).also { poly ->
                        poly.outlinePaint.strokeWidth = 6f
                        poly.outlinePaint.color = AndroidColor.MAGENTA
                        overlays.add(poly)
                        trackRef.value = poly
                    }

                    // Creamos Polyline para la ruta y lo añadimos a overlays
                    Polyline(this).also { poly ->
                        poly.outlinePaint.strokeWidth = 8f
                        poly.outlinePaint.color = AndroidColor.CYAN
                        overlays.add(poly)
                        routeRef.value = poly
                    }
                }
            }
        )

        // Cabecera con ubicación y actividad
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.DarkGray.copy(alpha = 0.8f))
                .padding(8.dp)
                .align(Alignment.TopCenter)
        ) {
            currentLocationState.value?.let {
                Text("Lat: ${it.latitude}, Lon: ${it.longitude}", color = Color.White, fontSize = 18.sp)
            } ?: Text("Buscando ubicación…", color = Color.LightGray, fontSize = 18.sp)
            Spacer(Modifier.height(4.dp))
            Text("Actividad: $currentActivity", color = Color.White, fontSize = 18.sp)
        }

        // Botón “Ubi. actual”
        Button(
            onClick = {
                shouldFollowLocation = true
                currentLocationState.value?.let { mapViewRef.value?.controller?.animateTo(it) }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Text("Ubi. actual")
        }

        // Selector de rutas
        var expanded by remember { mutableStateOf(false) }
        Box(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Button(onClick = { expanded = true }) { Text("Rutas") }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
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

    // 6) Location updates
    DisposableEffect(LocationPermissionHelper.hasLocationPermission(context)) {
        if (LocationPermissionHelper.hasLocationPermission(context)) {
            val request = LocationRequest.Builder(5000L)
                .setMinUpdateIntervalMillis(2000L)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMinUpdateDistanceMeters(5f)
                .build()
            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val loc = result.lastLocation ?: return
                    if (loc.accuracy > 10f) return
                    mapViewModel.onLocationChanged(loc.latitude, loc.longitude)
                    val point = GeoPoint(loc.latitude, loc.longitude)
                    currentLocationState.value = point

                    // Si seguimos ubicación, centramos el mapa
                    if (shouldFollowLocation) {
                        mapViewRef.value?.controller?.animateTo(point)
                    }

                    // Actualizamos posición del marker y refrescamos el mapa
                    markerRef.value?.position = point
                    mapViewRef.value?.invalidate()
                }
            }
            fusedLocationClient.requestLocationUpdates(request, callback, null)
            onDispose { fusedLocationClient.removeLocationUpdates(callback) }
        } else onDispose { }
    }

    // 7) Dibujar track
    LaunchedEffect(trackPointsState) {
        trackPointsState?.let { pts ->
            trackRef.value?.setPoints(pts.map { GeoPoint(it.first, it.second) })
            mapViewRef.value?.invalidate()
        }
    }

    // 8) Dibujar ruta y hacer zoom a sus bounds
    LaunchedEffect(selectedRoutePoints) {
        selectedRoutePoints?.let { pts ->
            // Pintar polyline
            val geoPts = pts.map { GeoPoint(it.first, it.second) }
            routeRef.value?.setPoints(geoPts)
            mapViewRef.value?.invalidate()

            // Hacer zoom al bounding box de la ruta
            mapViewRef.value?.let { map ->
                val lats = pts.map { it.first }
                val lons = pts.map { it.second }
                val north = lats.maxOrNull() ?: return@let
                val south = lats.minOrNull() ?: return@let
                val east  = lons.maxOrNull() ?: return@let
                val west  = lons.minOrNull() ?: return@let

                val bbox = BoundingBox(north, east, south, west)
                map.zoomToBoundingBox(bbox, /* animate = */ true)
            }
        }
    }
}
