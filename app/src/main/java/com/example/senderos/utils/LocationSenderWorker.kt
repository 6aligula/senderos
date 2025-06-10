package com.example.senderos.utils

import android.content.Context
import androidx.work.*
import com.example.senderos.model.LocationRequest
import com.example.senderos.network.LocationClient
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.TimeUnit

class LocationSenderWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result = coroutineScope {
        // Recogemos los datos que el ViewModel guardó en DataStore
        val lat    = inputData.getDouble("lat", 0.0)
        val lon    = inputData.getDouble("lon", 0.0)
        val devId  = inputData.getString("device_id") ?: return@coroutineScope Result.failure()

        val ok = LocationClient.sendLocation(
            LocationRequest(
                device_id = devId,
                lat       = lat,
                lon       = lon,
                timestamp = System.currentTimeMillis()
            )
        )
        if (ok) Result.success() else Result.retry()
    }

    companion object {
        fun schedule(context: Context, payload: LocationRequest) {
            val data = workDataOf(
                "device_id" to payload.device_id,
                "lat"       to payload.lat,
                "lon"       to payload.lon
            )

            val request = OneTimeWorkRequestBuilder<LocationSenderWorker>()
                .setInputData(data)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS
                )
                .addTag("gps_upload")
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
