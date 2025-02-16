package com.example.botondepanico.ui.theme.alarm

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.botondepanico.MainActivity
import com.example.botondepanico.R

/**
 * Servicio en primer plano que muestra una notificación fija ("Monitoreando Alertas")
 * sin reproducir audio ni vibración. Únicamente sirve para indicar que la app está
 * activa en segundo plano.
 *
 * Declarar en AndroidManifest.xml, por ejemplo:
 *
 * <service
 *   android:name=".ui.theme.alarm.FixedAlertService"
 *   android:exported="false"
 *   android:foregroundServiceType="dataSync" />
 *   (o el tipo que consideres; "mediaPlayback" si quieres usarlo con audio en el futuro)
 */
class FixedAlertService : Service() {

    private val channelId = "fixed_alert_channel"
    private val notificationId = 3030

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Construye la notificación fija
        val notification = buildPersistentNotification()
        // Inicia el servicio en primer plano
        startForeground(notificationId, notification)
        // No hay sirena ni vibración, solo notificación.
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // Aquí no detienes alarmas ni nada, pues no reproduces nada
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Crea la notificación fija (ongoing) que indica "Monitoreando Alertas".
     * Se oculta la hora con .setShowWhen(false) y .setWhen(0).
     */
    private fun buildPersistentNotification(): Notification {
        // Al pulsar la notificación, abrir MainActivity
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            else
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Monitoreando Alertas")
            .setContentText("La app está en segundo plano y lista para recibir eventos.")
            .setOngoing(true) // Notificación fija (no deslizable)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)  // Baja prioridad para no molestar
            .setCategory(Notification.CATEGORY_SERVICE)
            // Ocultar la hora en la notificación
            .setShowWhen(false)
            .setWhen(0)
            .build()
    }

    /**
     * Crea un canal con importancia baja (IMPORTANCE_LOW)
     * para no ser intrusivo.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Monitoreo de Alertas",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificación fija para indicar que la app está activa"
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }
}
