package com.example.senderos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.senderos.ui.theme.SenderosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SenderosTheme {
                // Puedes usar Scaffold o cualquier otro layout base
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        AppNavigator()
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigator() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.Login.route) {
        composable(Routes.Login.route) {
            LoginScreen(navController)
        }
        composable(Routes.Register.route) {
            RegisterScreen(navController)
        }
    }
}

@Composable
fun LoginScreen(navController: NavHostController) {
    // Ejemplo básico de pantalla de login
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Iniciar sesión", style = MaterialTheme.typography.headlineLarge)

        // Aquí podrías definir TextFields para usuario y contraseña
        // y un Button para iniciar sesión.
        // Por simplicidad, se muestra solo un botón para navegar a la pantalla de registro.
        Button(onClick = { navController.navigate(Routes.Register.route) }) {
            Text("Ir a registro")
        }
    }
}

@Composable
fun RegisterScreen(navController: NavHostController) {
    // Ejemplo básico de pantalla de registro
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Registro", style = MaterialTheme.typography.headlineLarge)

        // Aquí incluirías campos para nombre, correo, contraseña, etc.
        // y un botón para confirmar el registro.
        Button(onClick = { navController.popBackStack() }) {
            Text("Volver a login")
        }
    }
}
