package com.example.senderos.network

import android.os.Build
import com.example.senderos.BuildConfig
import com.example.senderos.model.LocationRequest
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object LocationClient {
    private const val BASE_URL = BuildConfig.SERVER_IP      // mismo gradleField

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint       = false
                ignoreUnknownKeys = true
            })
        }
        // timeouts, retry, logging… si quieres
    }

    suspend fun sendLocation(payload: LocationRequest): Boolean {
        return try {
            val response: HttpResponse = client.post("$BASE_URL/location") {
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
            response.status == HttpStatusCode.Created
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
