package com.example.senderos.model

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val error: String
)
