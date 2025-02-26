// Archivo: app/src/main/java/com/example/senderos/ui/screens/RegisterScreen.kt
package com.example.senderos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Data class para enviar los datos en formato JSON
@Serializable

data class RegisterRequest(val email: String, val password: String)

// Función que realiza la petición POST a la API Flask
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
        val response: HttpResponse = client.post("http://192.168.18.252:5000/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(email, password))
        }
        if (response.status == HttpStatusCode.OK) {
            "Registro exitoso"
        } else {
            "Error en el registro: ${response.status}"
        }
    } catch (e: Exception) {
        "Error de conexión: ${e.localizedMessage}"
    } finally {
        client.close()
    }
}

@Composable
fun RegisterScreen(navController: NavHostController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    // Scope para lanzar corrutinas
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Registro", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo electrónico") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth(0.9f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth(0.9f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirmar contraseña") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier.fillMaxWidth(0.9f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                if (email.isNotBlank() && password.isNotBlank() && password == confirmPassword) {
                    // Lanza una corrutina para llamar a la API de registro
                    coroutineScope.launch {
                        val result = registerUser(email, password)
                        if (result == "Registro exitoso") {
                            // Si el registro es exitoso, vuelve a la pantalla anterior (login)
                            navController.popBackStack()
                        } else {
                            errorMessage = result
                        }
                    }
                } else {
                    errorMessage = "Verifica que los campos estén completos y que las contraseñas coincidan."
                }
            },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Text("Registrarse")
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (errorMessage.isNotEmpty()) {
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(16.dp))
        }
        TextButton(onClick = { navController.popBackStack() }) {
            Text("Volver a iniciar sesión")
        }
    }
}
