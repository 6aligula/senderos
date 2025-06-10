// utils/LocationPermissionHelper.kt
package com.example.senderos.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * 1)  Lógica “pura” que puedes llamar desde cualquier sitio
 *     (incluido ViewModel o service si lo necesitas).
 */
object LocationPermissionHelper {

    /** Lista de permisos que necesitamos en todas las APIs modernas.  */
    val requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    /** ¿Ya los tengo?  */
    fun hasLocationPermission(context: Context): Boolean =
        requiredPermissions.all { p ->
            ContextCompat.checkSelfPermission(
                context, p
            ) == PackageManager.PERMISSION_GRANTED
        }
}

/**
 * 2)  *Composable* plug‑and‑play.
 *     La llamas en cualquier pantalla y ella se encarga de:
 *     • Pedir los permisos la primera vez que la pantalla aparece
 *     • Re‑chequear cuando el usuario vuelve desde Ajustes
 */
@Composable
fun RequestLocationPermission(
    onGranted: () -> Unit = {},
    onDenied: () -> Unit = {}
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Launcher “remember” para sobrevivir recomposiciones
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result.values.all { it }
        if (granted) onGranted() else onDenied()
    }

    /* ---- 1ª ejecución: ¿ya tengo permiso? Si no, lo pido ---- */
    LaunchedEffect(Unit) {
        if (!LocationPermissionHelper.hasLocationPermission(context)) {
            launcher.launch(LocationPermissionHelper.requiredPermissions)
        } else {
            onGranted()
        }
    }

    /* ---- Al volver a primer plano (ON_RESUME) vuelve a comprobar ---- */
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME &&
                !LocationPermissionHelper.hasLocationPermission(context)
            ) {
                launcher.launch(LocationPermissionHelper.requiredPermissions)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
