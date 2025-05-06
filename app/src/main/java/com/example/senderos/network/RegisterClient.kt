// Archivo: app/src/main/java/com/example/senderos/network/RegisterClient.kt
package com.example.senderos.network

import com.example.senderos.BuildConfig
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import com.example.senderos.model.RegisterRequest

@Serializable
data class ErrorResponse(val error: String)

object RegisterClient {
    // URL base inyectada en tiempo de compilación
    private const val BASE_URL = BuildConfig.SERVER_IP

    // Cliente único para todas las peticiones
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint       = true
                isLenient         = true
                ignoreUnknownKeys = true
            })
        }
    }

    /**
     * Registra un usuario con email y password.
     * Retorna mensaje de éxito o el error recibido.
     */
    suspend fun registerUser(email: String, password: String): String {
        return try {
            val request = RegisterRequest(email, password)
            val response: HttpResponse = client.post("$BASE_URL/register") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            val bodyText = response.bodyAsText()
            if (response.status == HttpStatusCode.OK) {
                "Registro exitoso"
            } else {
                // Intentar parsear mensaje de error
                val err = try {
                    Json.decodeFromString<ErrorResponse>(bodyText)
                } catch (_: Exception) {
                    ErrorResponse("Error en el registro")
                }
                err.error
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Error de conexión: ${e.localizedMessage}"
        }
    }

    // Cerrar cliente si es necesario
    fun close() {
        client.close()
    }
}
