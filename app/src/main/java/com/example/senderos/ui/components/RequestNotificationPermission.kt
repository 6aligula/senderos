package com.example.senderos.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.senderos.utils.NotificationPermissionHelper

@Composable
fun RequestNotificationPermission(
    onGranted: () -> Unit = {},
    onDenied: () -> Unit = {}
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // No creamos launcher si no hace falta (APIs < 33)
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onGranted() else onDenied()
    }

    /* ---- 1ª ejecución ---- */
    LaunchedEffect(Unit) {
        if (!NotificationPermissionHelper.hasNotificationPermission(context)) {
            launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onGranted()
        }
    }

    /* ---- Re-chequeo al volver al foreground ---- */
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME &&
                !NotificationPermissionHelper.hasNotificationPermission(context)
            ) {
                launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
