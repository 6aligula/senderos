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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object LocationClient {

    private const val BASE_URL = BuildConfig.SERVER_IP   // gradleField existente ✅

    // ────────────────────────── HTTP client común ──────────────────────────
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint       = false
                ignoreUnknownKeys = true
            })
        }
        // timeouts, retry, logging… si ya los tenías
    }

    // ──────────────────────── 1) Enviar coordenada (ya estaba) ─────────────
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

    // ─────────────────────── 2) Descargar track guardado ───────────────────
    /** Devuelve la lista (lat, lon) tal cual está guardada en Firestore */
    suspend fun getTrack(deviceId: String): List<Pair<Double, Double>> = try {
        val resp: LocationOutDTO = client
            .get("$BASE_URL/location/$deviceId")
            .body()
        resp.locations.map { it.lat to it.lon }
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }

    // DTOs mínimos para mapear el JSON de LocationOut ----------------------
    @Serializable
    private data class LocationOutDTO(
        val locations: List<LocationEntryDTO> = emptyList()
    )
    @Serializable
    private data class LocationEntryDTO(
        val lat: Double,
        val lon: Double
    )

    // ────────────────── 3) Pedir ruta “snap-to-road” ──────────────────────
    /**
     * Llama a GET /route?start_lat=…&start_lon=…&end_lat=…&end_lon=…
     * y decodifica el GeoJSON devuelto por el backend.
     *
     * @param profile  mismo parámetro que en el backend (foot-walking, driving-car…)
     */
    suspend fun getRoute(
        start: Pair<Double, Double>,
        end:   Pair<Double, Double>,
        profile: String = "foot-walking"
    ): List<Pair<Double, Double>> = try {
        val g: GeoJsonDTO = client.get("$BASE_URL/route") {
            parameter("start_lat", start.first)
            parameter("start_lon", start.second)
            parameter("end_lat",   end.first)
            parameter("end_lon",   end.second)
            parameter("profile",   profile)
        }.body()

        g.features
            .firstOrNull()
            ?.geometry
            ?.coordinates
            ?.map { (lon, lat) -> lat to lon }   // ← (lat, lon)
            ?: emptyList()
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }

    // DTO mínimos para GeoJSON --------------------------------------------
    @Serializable
    private data class GeoJsonDTO(val features: List<FeatureDTO>)
    @Serializable
    private data class FeatureDTO(val geometry: GeometryDTO)
    @Serializable
    private data class GeometryDTO(
        /** ORS devuelve [lon, lat] */
        val coordinates: List<List<Double>>
    )
}
