package com.example.botondepanico.ui.theme.alarm

import android.app.*
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.example.botondepanico.R

/**
 * Servicio de primer plano que reproduce sonido de ALARMA en STREAM_ALARM,
 * vibra y lanza una Full-Screen Activity aun si el teléfono está bloqueado.
 *
 * Declarar en AndroidManifest:
 * <service
 *   android:name=".ui.theme.alarm.AlarmNoLocationService"
 *   android:exported="false" />
 */
class AlarmNoLocationService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    private val channelId = "alarm_fullscreen_channel"
    private val notificationId = 2020

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Iniciamos Vibrator
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        // Construimos el MediaPlayer de forma manual para usar STREAM_ALARM
        mediaPlayer = MediaPlayer().apply {
            val afd = resources.openRawResourceFd(R.raw.sirena)
            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()

            setAudioStreamType(AudioManager.STREAM_ALARM) // Suena incluso en silencio normal
            isLooping = true
            prepare()
        }
    }

    override fun onStartCommand(intent: Intent?, serviceFlags: Int, startId: Int): Int {
        // Preparamos un Intent para la Activity full-screen
        val fullScreenIntent = Intent(this, AlarmFullScreenActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            0,
            fullScreenIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            else
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Creamos la notificación con categoría ALARM y prioridad máxima
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("¡Alerta de Pánico!")
            .setContentText("Sonando y vibrando...")
            .setSmallIcon(R.drawable.ic_notification)
            .setCategory(Notification.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX) // Para < Android 8
            .setFullScreenIntent(fullScreenPendingIntent, true) // Inicia la Activity en pantalla completa
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .build()

        // Arrancamos en primer plano para que no lo maten en background
        startForeground(notificationId, notification)

        // Arrancamos sirena y vibración
        startAlarm()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Inicia el sonido de sirena (STREAM_ALARM) y vibración en bucle.
     */
    private fun startAlarm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = longArrayOf(0, 1000, 1000) // vibra 1s, pausa 1s
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 1000, 1000), 0)
        }

        mediaPlayer?.start()
    }

    /**
     * Detiene sonido y vibración.
     */
    private fun stopAlarm() {
        vibrator?.cancel()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    /**
     * Crea un canal con IMPORTANCE_HIGH para permitir heads-up y full-screen
     * en dispositivos con Android 8+.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alerta Full-Screen",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Servicio de alarma y pantalla completa"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
