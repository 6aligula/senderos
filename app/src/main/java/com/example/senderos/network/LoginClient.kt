package com.example.senderos.network

import android.content.Context
import com.example.senderos.BuildConfig
import com.example.senderos.model.LoginRequest
import com.example.senderos.model.LoginResponse
import com.example.senderos.model.ErrorResponse
import com.example.senderos.utils.dataStore
import com.example.senderos.utils.PreferencesKeys.AUTH_TOKEN
import androidx.datastore.preferences.core.edit

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object AuthClient {
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

    // Función de login reutilizando el mismo cliente
    suspend fun loginUser(email: String, password: String, context: Context): String {
        return try {
            val response: HttpResponse = client.post("$BASE_URL/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(email, password))
            }
            val bodyText = response.bodyAsText()
            if (response.status == HttpStatusCode.OK) {
                val loginResp = Json.decodeFromString<LoginResponse>(bodyText)
                saveToken(context, loginResp.token)
                "Login exitoso"
            } else {
                val err = Json.decodeFromString<ErrorResponse>(bodyText)
                err.error
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Error de conexión: ${e.localizedMessage}"
        }
    }

    // Guardado de token en DataStore
    private suspend fun saveToken(context: Context, token: String) {
        context.dataStore.edit { prefs ->
            prefs[AUTH_TOKEN] = token
        }
    }
}
