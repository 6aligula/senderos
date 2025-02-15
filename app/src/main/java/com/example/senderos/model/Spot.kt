package com.example.senderos.model
import kotlinx.serialization.Serializable

@Serializable
data class Spot(
    val id: Int,
    val plate: String,
    val status: String
)
