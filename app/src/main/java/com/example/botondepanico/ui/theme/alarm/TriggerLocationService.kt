package com.example.botondepanico.ui.theme.alarm

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.botondepanico.R
import com.google.android.gms.location.*
import com.google.firebase.database.FirebaseDatabase

class TriggerLocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val channelId = "trigger_location_channel"
    private val notificationId = 9999

    // Se llama cada vez que se reciben coordenadas nuevas
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            for (loc in result.locations) {
                uploadLocationToFirebase(loc)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        // Crea una notificación silenciosa y con prioridad mínima
        val notification = createStealthNotification(
            title = "Obteniendo ubicación",
            content = "Servicio en ejecución..."
        )

        // Inicia el servicio en primer plano
        startForeground(notificationId, notification)

        // Inicia la lógica de ubicación
        requestLocationUpdates()

        // Si el sistema mata el servicio, se reintentará crearlo
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }

    override fun onBind(intent: android.content.Intent?): IBinder? = null

    private fun requestLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 10_000L       // Ajusta según tu necesidad (10s)
            fastestInterval = 5_000L // Máxima frecuencia
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }

        // Verifica que el usuario tenga ACCESS_FINE_LOCATION concedido
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun uploadLocationToFirebase(location: Location) {
        val dbRef = FirebaseDatabase.getInstance().getReference("alertLocations")
        val data = mapOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "timestamp" to System.currentTimeMillis()
        )
        dbRef.push().setValue(data)
    }

    private fun createStealthNotification(title: String, content: String): Notification {
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setVisibility(NotificationCompat.VISIBILITY_SECRET)
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        // IMPORTANCE_LOW para minimizar ruido; Android NO permite IMPORTANCE_NONE en foreground
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Ubicación Silenciosa",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Servicio de ubicación discreto"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
