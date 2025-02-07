package com.example.botondepanico.ui.theme.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavBar(navController: NavController) {
    // Detectamos la ruta actual para marcar el ítem seleccionado
    val currentRoute = currentRoute(navController)

    NavigationBar {
        // Ítem "home"
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
            label = { Text("Inicio") },
            selected = currentRoute == "home",
            onClick = {
                // Evitamos recargar "home" si ya estamos en "home"
                if (currentRoute != "home") {
                    navController.navigate("home")
                }
            }
        )
        // Ítem "panic"
        NavigationBarItem(
            icon = { Icon(Icons.Default.Warning, contentDescription = "Pánico") },
            label = { Text("Pánico") },
            selected = currentRoute == "panic",
            onClick = {
                // Evitamos recargar "panic" si ya estamos en "panic"
                if (currentRoute != "panic") {
                    navController.navigate("panic")
                }
            }
        )
    }
}

/**
 * Retorna la ruta activa en el [navController].
 */
@Composable
fun currentRoute(navController: NavController): String? {
    val backStackEntry by navController.currentBackStackEntryAsState()
    return backStackEntry?.destination?.route
}
