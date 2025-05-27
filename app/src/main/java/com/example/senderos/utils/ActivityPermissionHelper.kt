// utils/ActivityPermissionHelper.kt
package com.example.senderos.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object ActivityPermissionHelper {
    const val permission = Manifest.permission.ACTIVITY_RECOGNITION

    fun hasRecognitionPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
