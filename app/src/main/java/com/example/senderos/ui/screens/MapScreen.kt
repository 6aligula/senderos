package com.example.senderos.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun MapScreen() {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            // Inicializa la configuración de osmdroid.
            Configuration.getInstance().load(
                context,
                context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
            )

            // Crea el MapView.
            MapView(context).apply {
                // Establece la fuente de teselas a MAPNIK (OpenStreetMap)
                setTileSource(TileSourceFactory.MAPNIK)
                // Habilita controles multitáctiles
                setMultiTouchControls(true)
                // Configura el zoom y la posición inicial (ejemplo: Madrid, España)
                controller.setZoom(15.0)
                controller.setCenter(GeoPoint(40.4168, -3.7038))

                // Opcional: Agrega un marcador en el centro
                val marker = Marker(this)
                marker.position = GeoPoint(40.4168, -3.7038)
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                overlays.add(marker)
            }
        }
    )
}
