package com.example.senderos

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.senderos.model.LocationRequest
import android.os.Looper
import com.example.senderos.R
import com.example.senderos.utils.LocationSenderWorker
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest as GmsLocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.location.FusedLocationProviderClient

class LocationSenderService : Service() {
    private lateinit var fusedClient: FusedLocationProviderClient
    private var callback: LocationCallback? = null

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startUpdates()
            ACTION_STOP  -> stopUpdates()
        }
        return START_STICKY
    }

    private fun startUpdates() {
        if (callback != null) return
        val request = GmsLocationRequest.Builder(5000L)
            .setMinUpdateIntervalMillis(2000L)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMinUpdateDistanceMeters(5f)
            .build()
        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                if (loc.accuracy > 10f) return
                val payload = LocationRequest(
                    device_id = deviceId(),
                    lat       = loc.latitude,
                    lon       = loc.longitude,
                    timestamp = System.currentTimeMillis()
                )
                LocationSenderWorker.schedule(applicationContext, payload)
            }
        }
        fusedClient.requestLocationUpdates(request, callback!!, Looper.getMainLooper())
        startForeground(NOTIF_ID, buildNotification())
    }

    private fun stopUpdates() {
        callback?.let { fusedClient.removeLocationUpdates(it) }
        callback = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun deviceId(): String = Settings.Secure.getString(
        contentResolver,
        Settings.Secure.ANDROID_ID
    )

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
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "location_service"
        private const val NOTIF_ID = 1
        const val ACTION_START = "com.example.senderos.LocationSenderService.START"
        const val ACTION_STOP  = "com.example.senderos.LocationSenderService.STOP"

        fun startService(context: Context) {
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
