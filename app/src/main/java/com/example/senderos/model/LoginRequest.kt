package com.example.senderos.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(val email: String, val password: String)
