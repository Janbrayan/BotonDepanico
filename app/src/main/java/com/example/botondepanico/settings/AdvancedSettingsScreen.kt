package com.example.botondepanico.ui.theme.settings

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.botondepanico.utils.BatteryOptimizationHelper

@Composable
fun AdvancedSettingsScreen() {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Configuraciones Avanzadas", style = MaterialTheme.typography.headlineSmall)

        // Botón: superposición (overlay)
        Button(onClick = { requestOverlayPermission(context) }) {
            Text("Permitir superposición (Overlay)")
        }

        // Botón: no molestar
        Button(onClick = { requestNotificationPolicyAccess(context) }) {
            Text("Permitir modificar No Molestar")
        }

        // Botón: ignorar optimizaciones de batería
        Button(onClick = {
            BatteryOptimizationHelper.handleBatteryOptimization(context)
        }) {
            Text("Desactivar optimizaciones de batería")
        }
    }
}

private fun requestOverlayPermission(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        if (!Settings.canDrawOverlays(context)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "Ya tienes permiso de superposición.", Toast.LENGTH_SHORT).show()
        }
    }
}

// Verificar acceso para cambiar No Molestar
private fun requestNotificationPolicyAccess(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        if (notificationManager != null && !notificationManager.isNotificationPolicyAccessGranted) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "Ya tienes permiso para modificar DND.", Toast.LENGTH_SHORT).show()
        }
    }
}
