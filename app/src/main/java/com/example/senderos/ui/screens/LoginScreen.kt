package com.example.senderos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import com.example.senderos.network.AuthClient

@Composable
fun LoginScreen(navController: NavHostController) {
    var email        by remember { mutableStateOf("") }
    var password     by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    val context        = LocalContext.current

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Iniciar sesión", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value           = email,
            onValueChange   = { email = it },
            label           = { Text("Correo electrónico") },
            singleLine      = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction    = ImeAction.Next
            ),
            modifier        = Modifier.fillMaxWidth(0.9f)
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value           = password,
            onValueChange   = { password = it },
            label           = { Text("Contraseña") },
            singleLine      = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction    = ImeAction.Done
            ),
            modifier        = Modifier.fillMaxWidth(0.9f)
        )
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    errorMessage = "Completa todos los campos."
                } else {
                    coroutineScope.launch {
                        // Llamamos al método de AuthClient
                        val result = AuthClient.loginUser(email, password, context)
                        if (result == "Login exitoso") {
                            navController.navigate("profile")
                        } else {
                            errorMessage = result
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Text("Ingresar")
        }
        Spacer(Modifier.height(16.dp))

        if (errorMessage.isNotEmpty()) {
            Text(
                text  = errorMessage,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(16.dp))
        }

        TextButton(onClick = { navController.navigate("register") }) {
            Text("¿No tienes una cuenta? Regístrate")
        }
    }
}
