// ui/screens/ActivityUpdatesReceiver.kt
package com.example.senderos.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.google.android.gms.location.ActivityRecognitionResult

/**
 * Recibe los Intent que envía la API de Activity Recognition
 * y los pasa directamente al ViewModel.
 */
class ActivityUpdatesReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (!ActivityRecognitionResult.hasResult(intent)) return
        val result = ActivityRecognitionResult.extractResult(intent) ?: return

        val owner = context as? ViewModelStoreOwner ?: return
        val vm = ViewModelProvider(owner).get(MapViewModel::class.java)
        vm.onActivityRecognitionResult(result)
    }

    companion object {
        const val ACTION = "com.example.senderos.ACTION_ACTIVITY_UPDATES"
    }
}
