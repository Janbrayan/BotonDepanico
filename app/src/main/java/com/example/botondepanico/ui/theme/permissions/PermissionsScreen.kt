package com.example.botondepanico.ui.theme.permissions

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState

/**
 * Pantalla que pide todos los permisos necesarios para tu app:
 * - Ubicación en primer plano
 * - Foreground Service Location (Android 14+)
 * - Notificaciones (Android 13+)
 * - Vibración, etc.
 *
 * Cuando se otorgan todos los permisos, llamamos [onAllPermissionsGranted].
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionsScreen(
    onAllPermissionsGranted: () -> Unit
) {
    // Construimos la lista de permisos que necesitamos
    val permissionsList = buildList {
        // Ubicación fina
        add(Manifest.permission.ACCESS_FINE_LOCATION)

        // Foreground Service para localización (Android 14+, targetSdk=34)
        if (Build.VERSION.SDK_INT >= 34) {
            add("android.permission.FOREGROUND_SERVICE_LOCATION")
        }

        // Notificaciones (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Vibración (si tu app usa vibración)
        add(Manifest.permission.VIBRATE)
    }

    // Gestor de permisos múltiples
    val multiplePermissionsState: MultiplePermissionsState =
        rememberMultiplePermissionsState(permissionsList)

    // Verificamos si TODOS los permisos de la lista han sido concedidos
    val allGranted = multiplePermissionsState.allPermissionsGranted

    // Si ya están todos concedidos, invocamos onAllPermissionsGranted y salimos
    if (allGranted) {
        LaunchedEffect(Unit) {
            onAllPermissionsGranted()
        }
        // Retornamos para no dibujar la UI de pedir permisos
        return
    }

    // Mientras no se concedan todos, mostramos una UI que explique por qué los necesitamos
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "La app requiere los siguientes permisos para funcionar correctamente:",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Lista de permisos que aún no están concedidos
        val revokedPermissions = multiplePermissionsState.revokedPermissions
        revokedPermissions.forEach { revokedPerm ->
            Text(text = "- ${revokedPerm.permission}")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                // Lanza la solicitud de todos los permisos
                multiplePermissionsState.launchMultiplePermissionRequest()
            }
        ) {
            Text("Otorgar Permisos")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Por si el usuario negó algo, podrías mostrar un mensaje adicional
        if (multiplePermissionsState.shouldShowRationale) {
            Text(
                text = "Por favor concede los permisos, de lo contrario la app no podrá " +
                        "activar la alarma con localización y notificaciones."
            )
        }
    }
}
