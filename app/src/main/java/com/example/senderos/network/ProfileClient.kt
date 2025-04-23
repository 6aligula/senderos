package com.example.senderos.network

import android.content.Context
import com.example.senderos.model.ProfileRequest
import com.example.senderos.utils.getAuthToken
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import com.example.senderos.BuildConfig

object ProfileClient {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    suspend fun createProfile(context: Context, profileRequest: ProfileRequest): String {
        return try {
            // Obtenemos el token desde DataStore
            val token = getAuthToken(context)

            // Hacemos la petición con el token en el header
            val response: HttpResponse = client.post("http://192.168.18.253:5000/create_profile") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(profileRequest)
            }

            val responseBody = response.bodyAsText()
            if (response.status == HttpStatusCode.OK) {
                "Perfil creado exitosamente"
            } else {
                responseBody
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Error de conexión: ${e.localizedMessage}"
        }
    }
}
