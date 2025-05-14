package com.example.senderos.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityRecognitionResult
import com.example.senderos.ui.screens.MapViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner

/**
 * Recibe los Intent que envía la API de Activity Recognition
 * y los pasa directamente al ViewModel.
 */
class ActivityUpdatesReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Asegurarnos de que vienen datos de Activity Recognition
        if (!ActivityRecognitionResult.hasResult(intent)) return

        // Extraer el resultado
        val result = ActivityRecognitionResult.extractResult(intent)!!

        // Obtener la instancia del ViewModel desde el contexto de la Activity
        // (suponemos que MapScreen se usa dentro de una Activity que implementa ViewModelStoreOwner)
        val owner = context as? ViewModelStoreOwner ?: return
        val vm = ViewModelProvider(owner)
            .get(MapViewModel::class.java)

        // Llamar al método del ViewModel
        vm.onActivityRecognitionResult(result)
    }

    companion object {
        const val ACTION = "com.example.senderos.ACTION_ACTIVITY_UPDATES"
    }
}
