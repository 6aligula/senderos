// Archivo: app/src/main/java/com/example/senderos/Routes.kt
package com.example.senderos

sealed class Routes(val route: String) {
    object Login : Routes("login")
    object Register : Routes("register")
    object Profile : Routes("profile")
    object ProfileDisplay : Routes("profileDisplay")
}
