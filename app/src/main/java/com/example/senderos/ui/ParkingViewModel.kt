package com.example.senderos.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.senderos.model.Spot
import com.example.senderos.network.ParkingApiClient
import kotlinx.coroutines.launch

class ParkingViewModel : ViewModel() {

    // Lista de plazas (spots)
    private val _spots = mutableStateOf<List<Spot>>(emptyList())
    val spots: State<List<Spot>> get() = _spots

    // Indica si está cargando datos
    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> get() = _isLoading

    // Para mensajes de error o alertas
    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> get() = _errorMessage

    /**
     * Función para obtener las plazas desde el servidor.
     */
    fun fetchSpots() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = ParkingApiClient.getSpots()
                _spots.value = response
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Error al obtener plazas: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Retorna la plaza que coincida con la matrícula dada (ignorando mayúsculas).
     */
    fun getSpotByPlate(plate: String): Spot? {
        return _spots.value.find { it.plate.equals(plate, ignoreCase = true) }
    }
}
