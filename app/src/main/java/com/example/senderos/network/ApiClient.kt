package com.example.senderos.network

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import com.example.senderos.model.Profile
import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse(val message: String)

object ApiClient {
    private const val BASE_URL = "http://192.168.18.253:5000/"

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    suspend fun registerProfile(profile: Profile): String {
        return try {
            val response: HttpResponse = client.post("${BASE_URL}register_profile") {
                contentType(ContentType.Application.Json)
                setBody(profile)
            }

            if (response.status == HttpStatusCode.OK) {
                "Registro exitoso"
            } else {
                val errorResponse = Json.decodeFromString<ApiResponse>(response.bodyAsText())
                errorResponse.message
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Error de conexión: ${e.localizedMessage}"
        }
    }
}
