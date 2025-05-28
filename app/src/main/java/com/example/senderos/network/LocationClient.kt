package com.example.senderos.network

import com.example.senderos.BuildConfig
import com.example.senderos.model.LocationRequest
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object LocationClient {

    private const val BASE_URL = BuildConfig.SERVER_IP

    // ────────────────────────── HTTP client común ──────────────────────────
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint       = false
                ignoreUnknownKeys = true
            })
        }
    }

    // ──────────────────────── 1) Enviar coordenada ────────────────────────
    suspend fun sendLocation(payload: LocationRequest): Boolean = try {
        val r: HttpResponse = client.post("$BASE_URL/location") {
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
        r.status == HttpStatusCode.Created
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }

    // ─────────────────────── 2) Track de ESTE dispositivo ─────────────────
    suspend fun getTrack(deviceId: String): List<Pair<Double, Double>> = try {
        val resp: LocationOutDTO = client
            .get("$BASE_URL/location/$deviceId")
            .body()
        resp.locations.map { it.lat to it.lon }
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }

    // DTOs para getTrack
    @Serializable
    private data class LocationOutDTO(
        val locations: List<LocationEntryDTO> = emptyList()
    )
    @Serializable
    private data class LocationEntryDTO(
        val lat: Double,
        val lon: Double
    )

    // ────────────────── 3) Ruta “snap-to-road” genérica ───────────────────
    suspend fun getRoute(
        start: Pair<Double, Double>,
        end: Pair<Double, Double>,
        profile: String = "foot-walking"
    ): List<Pair<Double, Double>> = try {
        val g: GeoJsonDTO = client.get("$BASE_URL/route") {
            parameter("start_lat", start.first)
            parameter("start_lon", start.second)
            parameter("end_lat",   end.first)
            parameter("end_lon",   end.second)
            parameter("profile",   profile)
        }.body()

        g.features.firstOrNull()
            ?.geometry?.coordinates
            ?.map { (lon, lat) -> lat to lon } ?: emptyList()
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }

    // DTOs para GeoJSON (solo si sigues usando este endpoint)
    @Serializable private data class GeoJsonDTO(val features: List<FeatureDTO>)
    @Serializable private data class FeatureDTO(val geometry: GeometryDTO)
    @Serializable private data class GeometryDTO(val coordinates: List<List<Double>>)  // [lon, lat]

    // ─────────────────────── 4) LISTA de rutas guardadas ──────────────────
    suspend fun getRoutesList(): List<String> = try {
        client.get("$BASE_URL/routes").body()
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }

    // ───────────────────── 5) Puntos de una ruta por ID ───────────────────
    suspend fun getRouteById(id: String): List<Pair<Double, Double>> = try {
        val resp: RouteOutDTO = client
            .get("$BASE_URL/routes/$id")
            .body()
        // Aquí mapeamos directamente el array "points" que devuelve tu backend
        resp.points.map { it.lat to it.lon }
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }

    // DTOs para getRouteById
    @Serializable
    private data class RouteOutDTO(
        val points: List<PointDTO> = emptyList()
    )
    @Serializable
    private data class PointDTO(
        val lat: Double,
        val lon: Double
        // Puedes añadir timestamp, accuracy, altitude… si los necesitas
    )
}
