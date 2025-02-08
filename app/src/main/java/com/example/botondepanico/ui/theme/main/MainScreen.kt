package com.example.botondepanico.ui.theme.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.botondepanico.ui.theme.home.HomeScreen
import com.example.botondepanico.ui.theme.panic.PanicScreen
import com.example.botondepanico.ui.theme.settings.AdvancedSettingsScreen

@Composable
fun MainScreen(parentNavController: NavController) {
    // Controlador de navegación local para Home, Panic, Settings
    val bottomNavController = rememberNavController()

    Scaffold(
        bottomBar = {
            BottomNavBar(bottomNavController)
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navController = bottomNavController,
                startDestination = "home"
            ) {
                composable("home") {
                    HomeScreen()
                }
                composable("panic") {
                    PanicScreen()
                }
                // Agregamos la nueva ruta "settings"
                composable("settings") {
                    AdvancedSettingsScreen()
                }
            }
        }
    }
}
