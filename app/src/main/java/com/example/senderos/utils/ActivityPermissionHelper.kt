// utils/ActivityPermissionHelper.kt
package com.example.senderos.utils

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat

object ActivityPermissionHelper {
    const val permission = android.Manifest.permission.ACTIVITY_RECOGNITION

    fun hasRecognitionPermission(context: Context): Boolean {
        val granted = ContextCompat.checkSelfPermission(
            context, permission
        ) == PackageManager.PERMISSION_GRANTED
        Log.d("PermCheck", "ACTIVITY_RECOGNITION granted? $granted")
        return granted
    }
}


