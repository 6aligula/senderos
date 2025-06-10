package com.example.senderos.model

import kotlinx.serialization.Serializable

@Serializable
data class ProfileResponse(
    val name: String,
    val description: String,
    val imageUrl: String?
)