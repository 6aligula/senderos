package com.example.senderos.model

import kotlinx.serialization.Serializable

@Serializable
data class ProfileRequest(
    val name: String,
    val description: String,
    val imageUrl: String? = null,

)
