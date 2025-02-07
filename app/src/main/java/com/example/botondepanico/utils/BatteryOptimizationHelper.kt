package com.example.botondepanico.utils

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import java.util.Locale

object BatteryOptimizationHelper {

    /**
     * Llama a esta función para que el usuario desactive
     * la optimización de batería / active autoarranque en su dispositivo,
     * en función del fabricante detectado.
     */
    fun handleBatteryOptimization(context: Context) {
        val manufacturer = Build.MANUFACTURER.lowercase(Locale.getDefault())

        when {
            manufacturer.contains("xiaomi") -> {
                openXiaomiAutoStart(context)
            }
            manufacturer.contains("huawei") -> {
                openHuaweiProtectedApps(context)
            }
            manufacturer.contains("oppo") || manufacturer.contains("realme") -> {
                openOppoAutoStart(context)
            }
            else -> {
                // Para Android puro u otros fabricantes
                requestIgnoreBatteryOptimization(context)
            }
        }
    }

    private fun openXiaomiAutoStart(context: Context) {
        try {
            val intent = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                putExtra("extra_pkgname", context.packageName)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo abrir AutoStart (Xiaomi).", Toast.LENGTH_LONG).show()
            requestIgnoreBatteryOptimization(context)
        }
    }

    private fun openHuaweiProtectedApps(context: Context) {
        try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                )
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo abrir Protected Apps (Huawei).", Toast.LENGTH_LONG).show()
            requestIgnoreBatteryOptimization(context)
        }
    }

    private fun openOppoAutoStart(context: Context) {
        try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                )
            }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No se pudo abrir AutoStart (Oppo/Realme).", Toast.LENGTH_LONG).show()
            requestIgnoreBatteryOptimization(context)
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo abrir AutoStart. Intenta manualmente.", Toast.LENGTH_LONG).show()
            requestIgnoreBatteryOptimization(context)
        }
    }

    /**
     * Pide al usuario que ignore las optimizaciones de batería
     * en Android puro (API >= 23).
     */
    private fun requestIgnoreBatteryOptimization(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(PowerManager::class.java)
            val packageName = context.packageName
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    context.startActivity(intent)
                } catch (ex: Exception) {
                    // Si falla, abrimos ajustes de optimizaciones
                    openBatteryOptimizationSettings(context)
                }
            }
        }
    }

    private fun openBatteryOptimizationSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            context.startActivity(intent)
        } catch (ex: Exception) {
            Toast.makeText(context, "No se pudo abrir ajustes de optimización de batería.", Toast.LENGTH_LONG).show()
        }
    }
}
