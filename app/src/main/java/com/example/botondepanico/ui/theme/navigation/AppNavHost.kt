package com.example.botondepanico.ui.theme.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.botondepanico.ui.theme.auth.GoogleAuthViewModel
import com.example.botondepanico.ui.theme.auth.GoogleLoginScreen
import com.example.botondepanico.ui.theme.main.MainScreen
import com.example.botondepanico.ui.theme.permissions.PermissionsScreen
import com.example.botondepanico.utils.BatteryOptimizationHelper
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AppNavHost(
    navController: NavHostController,
    authViewModel: GoogleAuthViewModel
) {
    NavHost(
        navController = navController,
        startDestination = "permissions"
    ) {
        // Pantalla de Permisos
        composable("permissions") {
            val context = LocalContext.current
            PermissionsScreen(
                onAllPermissionsGranted = {
                    // 1) Llamamos a BatteryOptimizationHelper para desactivar optimización
                    BatteryOptimizationHelper.handleBatteryOptimization(context)

                    // 2) Revisamos si hay usuario logueado
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    if (currentUser != null) {
                        // Navegar a "main"
                        navController.navigate("main") {
                            popUpTo("permissions") { inclusive = true }
                        }
                    } else {
                        // Navegar a "login"
                        navController.navigate("login") {
                            popUpTo("permissions") { inclusive = true }
                        }
                    }
                }
            )
        }

        // Pantalla de Login
        composable("login") {
            GoogleLoginScreen(navController, authViewModel)
        }

        // Pantalla principal
        composable("main") {
            MainScreen(navController)
        }
    }
}
