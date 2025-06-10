package com.example.senderos.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(val token: String, val user_id: Int, val message: String)
