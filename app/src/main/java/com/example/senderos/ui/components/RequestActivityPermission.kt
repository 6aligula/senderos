// ui/components/RequestActivityPermission.kt
package com.example.senderos.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.senderos.utils.ActivityPermissionHelper

@Composable
fun RequestActivityPermission(
    onGranted: () -> Unit = {},
    onDenied: () -> Unit = {}
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onGranted() else onDenied()
    }

    LaunchedEffect(Unit) {
        if (!ActivityPermissionHelper.hasRecognitionPermission(context)) {
            launcher.launch(ActivityPermissionHelper.permission)
        } else {
            onGranted()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME &&
                !ActivityPermissionHelper.hasRecognitionPermission(context)
            ) {
                launcher.launch(ActivityPermissionHelper.permission)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
