package com.example.botondepanico.ui.theme.fcm

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.botondepanico.R
import com.example.botondepanico.ui.theme.alarm.AlarmNoLocationService
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data
        val type = data["type"] ?: ""

        when (type) {
            "ALARM_TRIGGERED" -> {
                // Muestra notificación local con sonido/vibración (si el usuario lo permite)
                showNotification("Alerta Activada", "Alguien encendió la alarma.")

                // Inicia el servicio que reproduce sirena/vibración (no usa ubicación)
                val intent = Intent(this, AlarmNoLocationService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            }

            "ALARM_STOPPED" -> {
                // Muestra notificación de “Alerta detenida”
                showNotification("Alerta Detenida", "La alarma fue detenida.")

                // Detiene la sirena/vibración
                val intent = Intent(this, AlarmNoLocationService::class.java)
                stopService(intent)
            }
        }
    }

    /**
     * Muestra una notificación local usando NotificationManagerCompat.
     * Para Android 13+ se requiere el permiso POST_NOTIFICATIONS en tiempo de ejecución.
     */
    private fun showNotification(title: String, message: String) {
        // Verificar permiso de notificaciones en Android 13+ (API 33)
        val canNotify = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            // En versiones anteriores no se requiere POST_NOTIFICATIONS
            true
        }

        if (!canNotify) return

        // Canal de notificaciones
        val channelId = "fcm_alert_channel"
        val channelName = "Alerta de FCM"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Configuramos IMPORTANCE_HIGH para notificaciones heads-up
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de alerta con sonido/vibración"

                // Asignar un sonido para el canal si deseas
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                setSound(
                    uri,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )

                // Habilitar vibración (pattern simple)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        // Construimos la notificación
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)  // ícono de tu app
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_MAX)   // Para < Android 8
            .setAutoCancel(true)
            // Para versiones < Android 8, usar defaults
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .build()

        // Mostrar la notificación
        NotificationManagerCompat.from(this).notify(
            System.currentTimeMillis().toInt(),
            notification
        )
    }
}
