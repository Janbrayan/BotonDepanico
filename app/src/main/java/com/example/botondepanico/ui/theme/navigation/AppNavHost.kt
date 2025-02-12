package com.example.botondepanico.ui.theme.navigation

import android.content.Intent
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.botondepanico.ui.theme.alarm.FixedAlertService
import com.example.botondepanico.ui.theme.auth.GoogleAuthViewModel
import com.example.botondepanico.ui.theme.auth.GoogleLoginScreen
import com.example.botondepanico.ui.theme.main.MainScreen
import com.example.botondepanico.ui.theme.permissions.PermissionsScreen
import com.example.botondepanico.settings.AdvancedSettingsScreen
import com.example.botondepanico.utils.BatteryOptimizationHelper
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AppNavHost(
    navController: NavHostController,
    authViewModel: GoogleAuthViewModel
) {
    val context = LocalContext.current
    val currentUser = FirebaseAuth.getInstance().currentUser

    // Decide la ruta inicial
    val startDestination = if (currentUser != null) {
        // Arrancamos servicio de monitoreo (notificación fija)
        LaunchedEffect(Unit) {
            val serviceIntent = Intent(context, FixedAlertService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
        "main"
    } else {
        "permissions"
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("permissions") {
            PermissionsScreen(
                onAllPermissionsGranted = {
                    // Desactivar optimizaciones de batería, etc.
                    BatteryOptimizationHelper.handleBatteryOptimization(context)

                    // Checamos si hay user
                    val user = FirebaseAuth.getInstance().currentUser
                    if (user != null) {
                        // Aquí también podrías iniciar el servicio
                        // si no lo has iniciado antes:
                        val serviceIntent = Intent(context, FixedAlertService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }

                        navController.navigate("main") {
                            popUpTo("permissions") { inclusive = true }
                        }
                    } else {
                        navController.navigate("login") {
                            popUpTo("permissions") { inclusive = true }
                        }
                    }
                }
            )
        }

        composable("login") {
            GoogleLoginScreen(navController, authViewModel)
        }

        composable("main") {
            MainScreen(navController)
        }

        composable("advancedSettings") {
            AdvancedSettingsScreen()
        }
    }
}
