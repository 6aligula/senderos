package com.example.senderos.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.senderos.Routes
import com.example.senderos.ui.ParkingViewModel
import androidx.compose.runtime.remember
import androidx.navigation.navArgument


@Composable
fun AppNavigator() {
    val navController = rememberNavController()
    // Ejemplo: si quieres un único ViewModel para manejar plazas,
    // puedes crearlo una sola vez aquí y pasarlo a las pantallas.
    val parkingViewModel = remember { ParkingViewModel() }

    NavHost(navController = navController, startDestination = Routes.Home.route) {
        composable(Routes.Login.route) { LoginScreen(navController) }
        composable(Routes.Register.route) { RegisterScreen(navController) }
        composable(Routes.Profile.route) { ProfileScreen(navController) }
        composable(Routes.ProfileDisplay.route) { ProfileDisplayScreen(navController)}
        composable(Routes.Map.route){ MapScreen() }
        // Pantallas nuevas:
        composable(Routes.Home.route) {
            HomeScreen(navController)
        }
        composable(Routes.Parking.route) {
            ParkingScreen(navController, parkingViewModel)
        }

        // Pantalla con argumento "plate"
        composable(
            route = Routes.VehicleInfo.route,
            arguments = listOf(navArgument("plate") { type = NavType.StringType })
        ) { backStackEntry ->
            VehicleInfoScreen(
                backStackEntry = backStackEntry,
                viewModel = parkingViewModel
            )
        }
    }
}
