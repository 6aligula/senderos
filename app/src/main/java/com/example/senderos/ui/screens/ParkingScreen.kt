package com.example.senderos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.senderos.Routes
import com.example.senderos.model.Spot
import com.example.senderos.ui.ParkingViewModel
import com.example.senderos.ui.components.ParkingGrid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParkingScreen(
    navController: NavController,
    viewModel: ParkingViewModel
) {
    val spots by viewModel.spots

    val isLoading by viewModel.isLoading
    val errorMessage by viewModel.errorMessage

    // State para la búsqueda de la matrícula
    var searchPlate by remember { mutableStateOf(TextFieldValue("")) }

    // Cargamos las plazas al entrar en la pantalla (similar a useEffect en RN)
    LaunchedEffect(Unit) {
        viewModel.fetchSpots()
    }

    // Manejo de error con AlertDialog (si quieres)
    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { /* Cerrar el diálogo */ },
            title = { Text("Error") },
            text = { Text(errorMessage ?: "") },
            confirmButton = {
                Button(onClick = { /* Podrías limpiar el error si quieres */ }) {
                    Text("OK")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(26.dp)
    ) {
        Text(text = "Plazas del Parking")
        // Añadimos un espacio para bajar el grid
        Spacer(modifier = Modifier.height(50.dp))

        // Grid con las plazas
        ParkingGrid(spots = spots) { spot ->
            // Navegar a VehicleInfo con la placa como parámetro
            navController.navigate("vehicleInfo/${spot.plate}")
        }

        // Botón para recargar
        Button(
            onClick = {
                viewModel.fetchSpots()
            },
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text(text = "Actualizar plazas")
        }

        // Barra de búsqueda
        OutlinedTextField(
            value = searchPlate,
            onValueChange = { searchPlate = it },
            label = { Text("Buscar Matrícula") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                val plateToFind = searchPlate.text.trim().uppercase()
                if (plateToFind.isEmpty()) {
                    // Puedes mostrar un AlertDialog o un Toast
                    return@Button
                }

                // Buscar la plaza
                val foundSpot = spots.find {
                    it.plate.equals(plateToFind, ignoreCase = true) && it.status == "occupied"
                }

                if (foundSpot != null) {
                    navController.navigate("vehicleInfo/${foundSpot.plate}")
                } else {
                    // Mostrar alerta
                }
            },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(text = "Buscar")
        }

        // Mostrar un indicador si está cargando
        if (isLoading) {
            Text(text = "Cargando plazas...")
        }
    }
}
