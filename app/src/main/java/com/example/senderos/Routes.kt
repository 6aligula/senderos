// Archivo: app/src/main/java/com/example/senderos/Routes.kt
package com.example.senderos

sealed class Routes(val route: String) {
    object Login : Routes("login")
    object Register : Routes("register")
    object Profile : Routes("profile")
    object ProfileDisplay : Routes("profileDisplay")
    object Map : Routes("map") // Nueva ruta para el map
    // Nuevas rutas para tu app de parking:
    object Home : Routes("home")
    object Parking : Routes("parking")

    // Ruta con argumento "plate"
    object VehicleInfo : Routes("vehicleInfo/{plate}") {
        fun createRoute(plate: String) = "vehicleInfo/$plate"
    }

}
