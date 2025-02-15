package com.example.senderos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.senderos.model.Spot

import android.util.Log
import androidx.compose.runtime.LaunchedEffect

@Composable
fun ParkingGrid(
    spots: List<Spot>,
    onSpotClick: (Spot) -> Unit
) {
    // Cada vez que spots cambie, se lanza este efecto
    LaunchedEffect(spots) {
        Log.d("ParkingGrid", "Se están creando ${spots.size} cards.")
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 80.dp),
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(spots) { spot ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.7f)
                    .clickable { onSpotClick(spot) },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFD1FAE5))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "ID: ${spot.id}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = spot.plate)
                }
            }
        }
    }
}

