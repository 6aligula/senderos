// Archivo: app/src/main/java/com/example/senderos/network/ApiClient.kt
package com.example.senderos.network

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.example.senderos.model.RegisterRequest
import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(val error: String)

suspend fun registerUser(email: String, password: String): String {
    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }
    return try {
        val registerRequest = RegisterRequest(email, password)
        val requestJson = Json.encodeToString(registerRequest)
        println("JSON serializado: $requestJson")

        val response: HttpResponse = client.post("http://192.168.1.180:5000/register") {
            contentType(ContentType.Application.Json)
            setBody(registerRequest)
        }

        println("Estado de la respuesta: ${response.status}")

        val responseBody = response.bodyAsText()
        if (response.status == HttpStatusCode.OK) {
            "Registro exitoso"
        } else {
            // Parsear solo el mensaje de error del JSON
            try {
                val errorResponse = Json.decodeFromString<ErrorResponse>(responseBody)
                errorResponse.error
            } catch (e: Exception) {
                "Error en el registro"



            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        "Error de conexión: ${e.localizedMessage}"
    } finally {
        client.close()
    }
}
