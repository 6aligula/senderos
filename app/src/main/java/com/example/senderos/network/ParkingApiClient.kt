package com.example.senderos.network

import com.example.senderos.model.Spot
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import io.ktor.client.call.body

object ParkingApiClient {

    private const val API_URL = "http://192.168.1.180:6000/api/spots"

    // Cliente Ktor con soporte para JSON
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    // Obtiene todas las plazas (spots) del servidor
    suspend fun getSpots(): List<Spot> {
        return client.get(API_URL).body()
    }
}
