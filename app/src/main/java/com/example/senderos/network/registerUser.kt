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

suspend fun registerUser(email: String, password: String): String {
    // Inicializa el cliente Ktor con el engine CIO y configuración para JSON
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
        // Crea el objeto de solicitud
        val registerRequest = RegisterRequest(email, password)
        // Serializa manualmente para verificar el JSON generado
        val requestJson = Json.encodeToString(registerRequest)
        println("JSON serializado: $requestJson")

        // Realiza la petición POST usando el objeto, para que Ktor use el serializer generado automáticamente
        val response: HttpResponse = client.post("http://192.168.1.186:5000/register") {
            contentType(ContentType.Application.Json)
            setBody(registerRequest)
        }

        // Imprime el estado de la respuesta para ver qué retorna el servidor
        println("Estado de la respuesta: ${response.status}")

        if (response.status == HttpStatusCode.OK) {
            "Registro exitoso"
        } else {
            "Error en el registro: ${response.status}"
        }
    } catch (e: Exception) {
        e.printStackTrace()
        "Error de conexión: ${e.localizedMessage}"
    } finally {
        client.close()
    }
}
