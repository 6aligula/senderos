package com.example.senderos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    // Define tus colores aquí
)

private val DarkColors = darkColorScheme(
    // Define tus colores aquí
)

@Composable
fun SenderosTheme(
    darkTheme: Boolean = false, // o utiliza una lógica para detectar el modo oscuro
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
