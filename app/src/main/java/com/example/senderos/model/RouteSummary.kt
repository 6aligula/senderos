package com.example.senderos.model

import kotlinx.serialization.Serializable

@Serializable
data class RouteSummary(
    val id: String       // UUID de la ruta
)