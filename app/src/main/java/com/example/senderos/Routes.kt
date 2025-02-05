package com.example.senderos

sealed class Routes(val route: String) {
    object Login : Routes("login")
    object Register : Routes("register")
}
