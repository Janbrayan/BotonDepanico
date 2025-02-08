package com.example.botondepanico.ui.theme.alarm

import android.app.*
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.example.botondepanico.MainActivity
import com.example.botondepanico.R

/**
 * Servicio de primer plano que reproduce sonido de ALARMA (tipo llamada) en STREAM_ALARM,
 * vibra y lanza una Activity en pantalla completa aun si el teléfono está bloqueado.
 *
 * En tu AndroidManifest.xml:
 * <service
 *   android:name=".ui.theme.alarm.AlarmNoLocationService"
 *   android:exported="false"
 *   android:foregroundServiceType="mediaPlayback" />
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

        // Configuramos el MediaPlayer (preferible usar AudioAttributes en Android L+)
        mediaPlayer = MediaPlayer().apply {
            val afd = resources.openRawResourceFd(R.raw.sirena)
            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val audioAttrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                setAudioAttributes(audioAttrs)
            } else {
                @Suppress("DEPRECATION")
                setAudioStreamType(AudioManager.STREAM_ALARM)
            }

            isLooping = true
            prepare()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Preparamos un Intent para la Activity full-screen (tipo llamada)
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

        // Iniciamos el servicio en primer plano para que no sea matado en background
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
     * Inicia la sirena (AudioAttributes USAGE_ALARM si Lollipop+, sino STREAM_ALARM)
     * y vibra indefinidamente 1s, pausa 1s.
     */
    private fun startAlarm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = longArrayOf(0, 1000, 1000)
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 1000, 1000), 0)
        }
        mediaPlayer?.start()
    }

    /**
     * Detiene la sirena y la vibración.
     */
    private fun stopAlarm() {
        vibrator?.cancel()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    /**
     * Crea un canal IMPORTANCE_HIGH para permitir heads-up / full-screen en Android 8+.
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
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }
}
