package com.example.botondepanico.ui.theme.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavBar(navController: NavController) {
    val currentBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry.value?.destination?.route

    NavigationBar {
        // Item HOME
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Boton") },
            selected = currentRoute == "home",
            onClick = {
                if (currentRoute != "home") {
                    navController.navigate("home")
                }
            }
        )
        // Item PANIC
        NavigationBarItem(
            icon = { Icon(Icons.Default.Warning, contentDescription = "Panic") },
            label = { Text("Alertas") },
            selected = currentRoute == "panic",
            onClick = {
                if (currentRoute != "panic") {
                    navController.navigate("panic")
                }
            }
        )
        // Item SETTINGS
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Configuraciones") },
            selected = currentRoute == "settings",
            onClick = {
                if (currentRoute != "settings") {
                    navController.navigate("settings")
                }
            }
        )
    }
}
