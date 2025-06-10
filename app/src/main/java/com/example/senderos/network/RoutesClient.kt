package com.example.senderos.network

import com.example.senderos.BuildConfig
import com.example.senderos.model.RouteSummary
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object RoutesClient {
    private const val BASE_URL = BuildConfig.SERVER_IP
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    /**
     * Obtiene la lista de rutas como objetos RouteSummary.
     * Endpoint: GET $BASE_URL/routes
     */
    suspend fun getRoutesList(): List<RouteSummary> = try {
        client.get("$BASE_URL/routes").body()
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}