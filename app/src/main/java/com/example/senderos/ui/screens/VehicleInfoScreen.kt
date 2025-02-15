package com.example.senderos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import com.example.senderos.ui.ParkingViewModel

@Composable
fun VehicleInfoScreen(
    backStackEntry: NavBackStackEntry,
    viewModel: ParkingViewModel
) {
    val plate = backStackEntry.arguments?.getString("plate") ?: ""
    var spotFound by remember { mutableStateOf(viewModel.getSpotByPlate(plate)) }
    var showError by remember { mutableStateOf(false) }

    LaunchedEffect(plate) {
        // Si no encuentra el spot, forzamos un fetch para ver si hay nuevos datos
        if (spotFound == null) {
            viewModel.fetchSpots()
            spotFound = viewModel.getSpotByPlate(plate)
            if (spotFound == null) {
                showError = true
            }
        }
    }

    if (showError) {
        AlertDialog(
            onDismissRequest = { /* Maneja cierre */ },
            title = { Text("No se encontró") },
            text = { Text("No hay plaza con la matrícula: $plate") },
            confirmButton = {
                Button(onClick = { showError = false }) {
                    Text("OK")
                }
            }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (spotFound == null) {
            Text(text = "Cargando o no existe la plaza para $plate")
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = "Información del Vehículo")
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "ID: ${spotFound!!.id}")
                Text(text = "Matrícula: ${spotFound!!.plate}")
                Text(
                    text = "Estado: ${spotFound!!.status}",
                )
            }
        }
    }
}
