package com.example.botondepanico.ui.theme.panic

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState

/**
 * Pantalla "PanicScreen" que muestra:
 * - Permiso de ubicación
 * - Mapa centrado en la ubicación
 * - Botón de Pánico
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PanicScreen(
    onPanicClick: () -> Unit = {}
) {
    val context = LocalContext.current

    // Estado del permiso FINE_LOCATION
    val fineLocationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val hasLocationPermission = fineLocationPermissionState.status.isGranted

    // Estados para almacenar lat/lng si logramos obtener la ubicación
    var lat by remember { mutableStateOf(19.432608) }  // CDMX lat predeterminada
    var lng by remember { mutableStateOf(-99.133209) } // CDMX lng predeterminada

    // Manejo de la cámara de Google Maps
    val cameraPositionState = rememberCameraPositionState()

    // Al montar la Composable, si tenemos permiso, tratamos de obtener ubicación
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            getLastKnownLocation(context) { location ->
                location?.let {
                    lat = it.latitude
                    lng = it.longitude
                    // Movemos la cámara a esa posición
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(
                        LatLng(lat, lng), 15f
                    )
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // Si NO tenemos permiso, mostramos un mensaje y un botón
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
            // Si tenemos permiso, mostramos el mapa
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                // Activamos la capa "MyLocation" (punto azul)
                properties = MapProperties(isMyLocationEnabled = true),
                // Mostramos el botón "Mi ubicación" en la UI del mapa
                uiSettings = MapUiSettings(myLocationButtonEnabled = true)
            ) {
                // Opcional: un marcador en lat,lng
                Marker(
                    state = MarkerState(position = LatLng(lat, lng)),
                    title = "Mi ubicación"
                )
            }
        }

        // Botón de Pánico
        Button(
            onClick = onPanicClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Text("¡Botón de Pánico!")
        }
    }
}

/**
 * Función que obtiene la última ubicación conocida con FusedLocationProviderClient.
 * Retorna `null` si no logra obtenerla.
 * @param onResult Se llama con la ubicación o con null.
 */
@SuppressLint("MissingPermission") // Asumimos que ya pediste permission en Compose
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
