package com.example.senderos.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * 1)  Lógica “pura” que puedes invocar desde ViewModel, Service, Worker, etc.
 */
object NotificationPermissionHelper {

    /** Sólo hace falta el permiso en Android 13 (API 33) o superior */
    val requiredPermissions = arrayOf(
        Manifest.permission.POST_NOTIFICATIONS
    )

    /** ¿Ya lo tengo? */
    fun hasNotificationPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // En APIs < 33 el permiso se concede en install-time
            true
        }
}
