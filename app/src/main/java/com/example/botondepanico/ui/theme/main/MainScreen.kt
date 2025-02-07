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

@Composable
fun MainScreen(parentNavController: NavController) {
    // Controlador de navegación local para Home, Panic, etc.
    val bottomNavController = rememberNavController()

    Scaffold(
        // Menú inferior
        bottomBar = {
            BottomNavBar(bottomNavController)
        }
    ) { innerPadding ->
        // Contenido principal con un NavHost anidado
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navController = bottomNavController,
                startDestination = "home"
            ) {
                composable("home") { HomeScreen() }
                composable("panic") { PanicScreen() }
            }
        }
    }
}
