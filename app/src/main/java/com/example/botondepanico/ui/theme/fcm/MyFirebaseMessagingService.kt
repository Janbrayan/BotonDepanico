package com.example.botondepanico.ui.theme.fcm

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.content.Intent
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
                // Muestra notificación local (opcional) con sonido/vibración si el usuario lo permite
                showNotification("Alerta Activada", "Alguien encendió la alarma.")

                // Inicia el servicio que reproduce sirena/vibración
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

                // Detiene el servicio (sirena y vibración)
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
        // Chequeamos el permiso de POST_NOTIFICATIONS en Android 13+ (API 33)
        val canNotify = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true

        if (!canNotify) return

        val channelId = "fcm_alert_channel"
        val channelName = "Alerta de FCM"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de alerta con sonido/vibración"

                // Definir un sonido para el canal
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                setSound(
                    uri,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )

                // Vibración sencilla
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        // Construimos la notificación
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_MAX) // Para < Android 8
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .build()

        // Mostramos la notificación con un ID único (System.currentTimeMillis().toInt())
        NotificationManagerCompat.from(this).notify(
            System.currentTimeMillis().toInt(),
            notification
        )
    }
}
