// ui/components/RequestLocationPermission.kt
package com.example.senderos.ui.components

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.senderos.utils.LocationPermissionHelper

@Composable
fun RequestLocationPermission(
    onGranted: () -> Unit = {},
    onDenied: () -> Unit = {}
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result.values.all { it }
        if (granted) onGranted() else onDenied()
    }

    LaunchedEffect(Unit) {
        if (!LocationPermissionHelper.hasLocationPermission(context)) {
            launcher.launch(LocationPermissionHelper.requiredPermissions)
        } else {
            onGranted()
        }
    }

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
