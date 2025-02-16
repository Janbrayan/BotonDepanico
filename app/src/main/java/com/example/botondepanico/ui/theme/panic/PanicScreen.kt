package com.example.botondepanico.ui.theme.panic

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.os.Looper
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted
import com.google.android.gms.location.*
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*

/**
 * Pantalla "PanicScreen" con Material 3 y Accompanist Permissions,
 * recibiendo la ubicación en TIEMPO REAL con requestLocationUpdates,
 * con tilt=45° solo la PRIMERA vez que obtenemos la ubicación,
 * y un zoom más cercano (18f) en esa primera localización.
 * Después no re-centramos la cámara, para no "alejar" al usuario.
 */
@OptIn(
    ExperimentalPermissionsApi::class,   // Para recordar permisos con Accompanist
    ExperimentalMaterial3Api::class     // Para usar SmallTopAppBar
)
@Composable
fun PanicScreen(
    userName: String = "Mi Usuario"
) {
    val context = LocalContext.current

    // Permiso de ubicación (FINE_LOCATION)
    val fineLocationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val hasLocationPermission = fineLocationPermissionState.status.isGranted

    // Coordenadas en tiempo real
    var lat by remember { mutableStateOf(19.432608) }  // CDMX por defecto
    var lng by remember { mutableStateOf(-99.133209) }

    // Control de la cámara en Google Maps
    val cameraPositionState = rememberCameraPositionState {
        // Cámara inicial (CDMX), tilt=45°, bearing=0°, zoom=15
        position = CameraPosition(
            LatLng(lat, lng),
            15f,
            45f,
            0f
        )
    }

    // Flag para saber si ya re-centramos la cámara UNA sola vez
    var isFirstLocationSet by remember { mutableStateOf(false) }

    // Suscripción a actualizaciones si hay permiso
    if (hasLocationPermission) {
        DisposableEffect(Unit) {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val locationRequest = LocationRequest.create().apply {
                interval = 4000L
                fastestInterval = 2000L
                priority = Priority.PRIORITY_HIGH_ACCURACY
            }

            // Callback para actualizar lat/lng
            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { loc ->
                        lat = loc.latitude
                        lng = loc.longitude

                        // Solo la primera vez, re-centramos con un zoom más cercano (18f)
                        if (!isFirstLocationSet) {
                            isFirstLocationSet = true
                            cameraPositionState.position = CameraPosition(
                                LatLng(lat, lng),
                                18f,  // Zoom más cercano
                                45f,  // Tilt
                                0f    // Bearing
                            )
                        }
                        // En actualizaciones posteriores, solo movemos el Marker.
                    }
                }
            }

            // Verificar permiso en tiempo de ejecución
            if (
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
            ) {
                fusedClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            }

            // Al salir de la Composable, remover updates
            onDispose {
                fusedClient.removeLocationUpdates(locationCallback)
            }
        }
    }

    // Interfaz
    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Pánico") },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Si NO hay permiso => Botón para pedirlo
            if (!hasLocationPermission) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Necesitamos permiso de Ubicación para mostrar el mapa.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        fineLocationPermissionState.launchPermissionRequest()
                    }) {
                        Text("Solicitar Permiso")
                    }
                }
            } else {
                // Mapa: con cámara inicial en CDMX (tilt=45°),
                // y un Marker que se actualiza en tiempo real
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isMyLocationEnabled = false),
                    uiSettings = MapUiSettings(myLocationButtonEnabled = false)
                ) {
                    val myPosition = LatLng(lat, lng)
                    val markerIcon = bitmapDescriptorFromVector(
                        context = context,
                        vectorResId = com.example.botondepanico.R.drawable.alerta_panico
                    )
                    Marker(
                        state = MarkerState(position = myPosition),
                        title = userName,
                        snippet = "Aquí estoy",
                        icon = markerIcon
                    )
                }
            }
        }
    }
}

/**
 * Ejemplo original de "última ubicación" (ya no lo usamos en tiempo real).
 */
@SuppressLint("MissingPermission")
fun getLastKnownLocation(context: Context, onResult: (Location?) -> Unit) {
    val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    val locationTask: Task<Location> = fusedClient.lastLocation
    locationTask.addOnSuccessListener(OnSuccessListener { loc ->
        onResult(loc)
    })
    locationTask.addOnFailureListener {
        onResult(null)
    }
}

/**
 * Convierte un vector drawable a un BitmapDescriptor (pin personalizado).
 */
fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor {
    val vectorDrawable = context.getDrawable(vectorResId)
        ?: return BitmapDescriptorFactory.defaultMarker()
    val width = vectorDrawable.intrinsicWidth
    val height = vectorDrawable.intrinsicHeight
    vectorDrawable.setBounds(0, 0, width, height)

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    vectorDrawable.draw(canvas)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}
