package com.example.senderos

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.pm.ServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.senderos.model.LocationRequest
import com.example.senderos.utils.LocationPermissionHelper
import com.example.senderos.utils.LocationSenderWorker
import com.google.android.gms.location.*

class LocationSenderService : Service() {

    private lateinit var fusedClient: FusedLocationProviderClient
    private var callback: LocationCallback? = null

    /* -------------------------------------------------- *
     * Ciclo de vida
     * -------------------------------------------------- */
    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startUpdates()
            ACTION_STOP  -> stopUpdates()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /* -------------------------------------------------- *
     * Localización
     * -------------------------------------------------- */

    @RequiresPermission(
        anyOf = [           // 👈 NEW: obliga a quien lo llame a tener permiso
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ]
    )
    @SuppressLint("MissingPermission")
    private fun startUpdates() {
        // 0) ¿Tenemos permiso?  — cinturón y tirantes
        if (!LocationPermissionHelper.hasLocationPermission(this)) {
            stopSelf()
            return
        }

        // 1) ¿Ya estaba suscrito?
        if (callback != null) return

        // 2) Construir la petición
        val request = com.google.android.gms.location.LocationRequest.Builder(5_000L)
            .setMinUpdateIntervalMillis(2_000L)
            .setMinUpdateDistanceMeters(5f)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()

        // 3) Crear el callback
        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                if (loc.accuracy > 10f) return            // filtro rápido
                LocationSenderWorker.schedule(
                    applicationContext,
                    LocationRequest(
                        device_id = deviceId(),
                        lat       = loc.latitude,
                        lon       = loc.longitude,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }

        // 4) Suscribirnos
        try {
            @Suppress("MissingPermission")
            fusedClient.requestLocationUpdates(
                request,
                callback!!,
                Looper.getMainLooper()
            )
        } catch (se: SecurityException) {                // cinturón n.º 2
            stopSelf()
            return
        }

        // 5) Servicio en primer plano
        val notif = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIF_ID,
                notif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }

    private fun stopUpdates() {
        callback?.let { fusedClient.removeLocationUpdates(it) }
        callback = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /* -------------------------------------------------- *
     * Utilidades varias
     * -------------------------------------------------- */
    private fun deviceId(): String =
        Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, javaClass).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Senderos")
            .setContentText("Enviando ubicación…")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .addAction(0, "Detener", stopPending)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    /* -------------------------------------------------- *
     * API estática
     * -------------------------------------------------- */
    companion object {
        private const val CHANNEL_ID = "location_service"
        private const val NOTIF_ID   = 1
        const val ACTION_START       = "com.example.senderos.LocationSenderService.START"
        const val ACTION_STOP        = "com.example.senderos.LocationSenderService.STOP"

        /** Arranca el servicio **solo** si ya tenemos permiso. */
        fun startService(context: Context) {
            if (!LocationPermissionHelper.hasLocationPermission(context)) return // 👈 NEW
            val intent = Intent(context, LocationSenderService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, LocationSenderService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
