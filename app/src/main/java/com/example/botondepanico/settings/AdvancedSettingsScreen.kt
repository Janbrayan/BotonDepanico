package com.example.botondepanico.settings

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size
import com.example.botondepanico.utils.BatteryOptimizationHelper
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.maps.android.PolyUtil
import com.google.maps.android.compose.*
import com.google.maps.android.data.kml.KmlContainer
import com.google.maps.android.data.kml.KmlLayer
import com.google.maps.android.data.kml.KmlPolygon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.net.URL

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AdvancedSettingsScreen(
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentUser = FirebaseAuth.getInstance().currentUser

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sección superior: Perfil
        ProfileSection(currentUser, onLogout)

        Divider()

        Text("Configuraciones Avanzadas", style = MaterialTheme.typography.headlineSmall)

        Button(onClick = { requestOverlayPermission(context) }) {
            Text("Permitir superposición (Overlay)")
        }
        Button(onClick = { requestNotificationPolicyAccess(context) }) {
            Text("Permitir modificar No Molestar")
        }
        Button(onClick = {
            BatteryOptimizationHelper.handleBatteryOptimization(context)
        }) {
            Text("Desactivar optimizaciones de batería")
        }

        Divider()

        Text("Registrar Zona", style = MaterialTheme.typography.headlineSmall)
        RegisterZoneSectionMultiKML()
    }
}

/**
 * Muestra el perfil (foto, nombre, correo) y un botón para cerrar sesión.
 */
@Composable
private fun ProfileSection(
    user: FirebaseUser?,
    onLogout: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (user == null) {
                Text("No hay usuario logueado")
            } else {
                val photoUrl = user.photoUrl
                if (photoUrl != null) {
                    val painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(LocalContext.current)
                            .data(photoUrl)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .size(Size.ORIGINAL)
                            .build()
                    )
                    Image(
                        painter = painter,
                        contentDescription = "Foto de perfil",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                    )
                }
                val displayName = user.displayName ?: "(sin nombre)"
                Text(
                    text = "Nombre: $displayName",
                    style = MaterialTheme.typography.titleMedium
                )

                val email = user.email ?: "(sin correo)"
                Text(
                    text = "Correo: $email",
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onLogout) {
                    Text("Cerrar Sesión")
                }
            }
        }
    }
}

/**
 * - Al presionar "Vincular Zona":
 *   1) Obtiene ubicación real (si hay permiso).
 *   2) Muestra mapa centrado en esa ubicación.
 *   3) Descarga & procesa KML en IO.
 *   4) Verifica si está dentro de un polígono => registra => oculta mapa,
 *     sino => "Fuera de todos los polígonos" => oculta mapa.
 * - Solo registra 1 vez (si 'seccionActual' existe, no vuelve a mostrar el botón).
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun RegisterZoneSectionMultiKML() {
    val context = LocalContext.current
    // Scope para corutinas
    val scope = rememberCoroutineScope()

    val fineLocationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val hasLocationPermission = fineLocationPermissionState.status.isGranted

    // Ubicación real
    var userLat by remember { mutableStateOf(19.432608) }
    var userLng by remember { mutableStateOf(-99.133209) }

    // KML
    var kmlUrlList by remember { mutableStateOf(emptyList<String>()) }

    // Sección del usuario
    var userInsideName by remember { mutableStateOf("") }

    // Ya está registrado
    var zoneAlreadyRegistered by remember { mutableStateOf(false) }

    // Control de cámara
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(userLat, userLng), 14f)
    }

    // Mostrar/ocultar mapa
    var showMap by remember { mutableStateOf(false) }
    // Evita múltiples detecciones
    var detectionDone by remember { mutableStateOf(false) }

    // Usuario actual
    val currentUser = FirebaseAuth.getInstance().currentUser

    // Revisar si ya tiene 'seccionActual'
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            val db = Firebase.firestore
            db.collection("users").document(user.uid)
                .get()
                .addOnSuccessListener { snapshot ->
                    val existing = snapshot.getString("seccionActual")
                    if (!existing.isNullOrEmpty()) {
                        zoneAlreadyRegistered = true
                        Log.d("PolygonCheck", "Usuario ya tiene seccionActual=$existing")
                    }
                }
        }
    }

    // Descarga la lista de URLs KML (1 vez)
    LaunchedEffect(true) {
        val storageRef = Firebase.storage.reference.child("kmlFiles")
        try {
            val listResult = storageRef.listAll().await()
            if (listResult.items.isEmpty()) {
                Toast.makeText(context, "No hay archivos KML en kmlFiles/", Toast.LENGTH_SHORT).show()
            } else {
                val tmpUrls = mutableListOf<String>()
                for (itemRef in listResult.items) {
                    try {
                        val uri = itemRef.downloadUrl.await()
                        tmpUrls.add(uri.toString())
                        Log.d("KML", "Archivo KML encontrado: $uri")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                if (tmpUrls.isNotEmpty()) {
                    kmlUrlList = tmpUrls
                    Log.d("KML", "Lista KML final: $kmlUrlList")
                } else {
                    Toast.makeText(context, "Error al obtener URLs de KML", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error al listar KML files: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // UI
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!hasLocationPermission) {
            Text("Se requiere permiso de Ubicación para vincular la zona.")
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { fineLocationPermissionState.launchPermissionRequest() }) {
                Text("Pedir Permiso")
            }

        } else {
            if (zoneAlreadyRegistered) {
                Text("Ya se ha registrado una zona.\nContacta al admin para cambiarla.")
            } else {
                // Botón "Vincular Zona"
                if (!showMap) {
                    Button(onClick = {
                        Log.d("PolygonCheck", "Botón 'Vincular Zona' presionado")
                        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                        fusedLocationClient.lastLocation
                            .addOnSuccessListener { loc: Location? ->
                                if (loc == null) {
                                    Toast.makeText(context, "No se pudo obtener ubicación real.", Toast.LENGTH_SHORT).show()
                                } else {
                                    userLat = loc.latitude
                                    userLng = loc.longitude
                                    Log.d("LocationCheck", "GPS => lat=${loc.latitude}, lng=${loc.longitude}")
                                    cameraPositionState.position = CameraPosition.fromLatLngZoom(
                                        LatLng(userLat, userLng),
                                        15f
                                    )
                                }
                                showMap = true
                                detectionDone = false
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Error obteniendo ubicación.", Toast.LENGTH_SHORT).show()
                            }
                    }) {
                        Text("Vincular Zona")
                    }

                } else {
                    // Mapa
                    Box(
                        modifier = Modifier
                            .height(300.dp)
                            .fillMaxWidth()
                    ) {
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraPositionState,
                            properties = MapProperties(isMyLocationEnabled = true),
                            uiSettings = MapUiSettings(myLocationButtonEnabled = true)
                        ) {
                            MapEffect(kmlUrlList, userLat, userLng) { map ->
                                scope.launch {
                                    if (kmlUrlList.isEmpty()) {
                                        Log.d("MapEffect", "No hay KML en la lista. Saliendo.")
                                        return@launch
                                    }
                                    if (detectionDone) {
                                        Log.d("MapEffect", "detectionDone = true, saliendo.")
                                        return@launch
                                    }
                                    detectionDone = true

                                    try {
                                        var foundName: String? = null

                                        // Procesar cada KML
                                        for ((index, kmlUrl) in kmlUrlList.withIndex()) {
                                            Log.d("MapEffect", "Procesando KML #${index+1}: $kmlUrl")

                                            // Descargar KML
                                            val kmlBytes = withContext(Dispatchers.IO) {
                                                URL(kmlUrl).openConnection().getInputStream().use { it.readBytes() }
                                            }

                                            // Crear & agregar KmlLayer
                                            withContext(Dispatchers.Main) {
                                                val inputStream = ByteArrayInputStream(kmlBytes)
                                                val kmlLayer = KmlLayer(map, inputStream, context)
                                                kmlLayer.addLayerToMap()
                                                Log.d("PolygonCheck", "KmlLayer agregado (#${index+1}). Revisando contenedores...")

                                                // Recorremos contenedores recursivamente
                                                for (container in kmlLayer.containers) {
                                                    foundName = checkContainerRecursively(container, userLat, userLng)
                                                    if (foundName != null) {
                                                        Log.d("PolygonCheck", "Encontrado polígono => $foundName")
                                                        break
                                                    }
                                                }
                                            }

                                            if (foundName != null) {
                                                Log.d("PolygonCheck", "Polígono encontrado => $foundName. Saliendo del bucle KML.")
                                                break
                                            }
                                        }

                                        // Al final
                                        if (foundName.isNullOrEmpty()) {
                                            Toast.makeText(context, "Fuera de todos los polígonos", Toast.LENGTH_SHORT).show()
                                            showMap = false
                                        } else {
                                            userInsideName = foundName as String
                                            val user = FirebaseAuth.getInstance().currentUser
                                            if (user == null) {
                                                Toast.makeText(context, "No hay usuario logueado", Toast.LENGTH_SHORT).show()
                                                showMap = false
                                                return@launch
                                            }
                                            // REGISTRAR EN FIRESTORE con set() (Crea si no existe)
                                            Firebase.firestore
                                                .collection("users")
                                                .document(user.uid)
                                                .set(mapOf("seccionActual" to userInsideName), SetOptions.merge())
                                                .addOnSuccessListener {
                                                    Toast.makeText(
                                                        context,
                                                        "Se registró la zona: $userInsideName",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    zoneAlreadyRegistered = true
                                                    showMap = false
                                                }
                                                .addOnFailureListener { e ->
                                                    Toast.makeText(
                                                        context,
                                                        "Error al registrar zona: ${e.message}",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    showMap = false
                                                }
                                        }

                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        Toast.makeText(context, "Error leyendo KML: ${e.message}", Toast.LENGTH_SHORT).show()
                                        showMap = false
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Recorre recursivamente un `KmlContainer` (y sub-contenedores)
 * para saber si (userLat, userLng) está dentro de un polígono.
 * Retorna el nombre si lo encuentra.
 */
private fun checkContainerRecursively(
    container: KmlContainer,
    userLat: Double,
    userLng: Double
): String? {
    // Revisamos placemarks del container
    Log.d("PolygonCheck", "Container '${container.containerId}' con placemarks: ${container.placemarks.count()}")
    for (placemark in container.placemarks) {
        val geometry = placemark.geometry
        if (geometry is KmlPolygon) {
            val coords = geometry.outerBoundaryCoordinates
            Log.d("PolygonCheck", "Polígono con coords=$coords")
            val inside = PolyUtil.containsLocation(userLat, userLng, coords, true)
            Log.d("PolygonCheck", "inside=$inside")
            if (inside) {
                // Devuelve el nombre
                return placemark.getProperty("name") ?: "Sección X"
            }
        }
    }

    // Revisar sub-contenedores recursivamente
    for (subContainer in container.containers) {
        val foundName = checkContainerRecursively(subContainer, userLat, userLng)
        if (foundName != null) return foundName
    }

    return null
}

/** Pedir permiso Overlay */
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

/** Permiso para modificar No Molestar */
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
