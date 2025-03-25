package com.example.senderos.network

import android.content.Context
import android.content.SharedPreferences
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.example.senderos.model.LoginRequest
import com.example.senderos.model.LoginResponse
import kotlinx.serialization.Serializable



suspend fun loginUser(email: String, password: String, context: Context): String {
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
        val loginRequest = LoginRequest(email, password)
        // Conexión con el servidor Flask para login
        val response: HttpResponse = client.post("http://192.168.18.253:5000/login") {
            contentType(ContentType.Application.Json)
            setBody(loginRequest)
        }

        val responseBody = response.bodyAsText()
        if (response.status == HttpStatusCode.OK) {
            val loginResponse = Json.decodeFromString<LoginResponse>(responseBody)
            saveToken(context, loginResponse.token)
            "Login exitoso"
        } else {
            val errorResponse = Json.decodeFromString<ErrorResponse>(responseBody)
            errorResponse.error
        }
    } catch (e: Exception) {
        e.printStackTrace()
        "Error de conexión: ${e.localizedMessage}"
    } finally {
        client.close()
    }
}

private fun saveToken(context: Context, token: String) {
    val sharedPreferences: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    sharedPreferences.edit().putString("auth_token", token).apply()
}
