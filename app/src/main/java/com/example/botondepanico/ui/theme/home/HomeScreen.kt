package com.example.botondepanico.ui.theme.home

import android.content.Intent
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.botondepanico.ui.theme.alarm.AlarmNoLocationService
import com.example.botondepanico.ui.theme.alarm.TriggerLocationService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

@Composable
fun HomeScreen() {
    val context = LocalContext.current

    // UID del usuario actual
    val miUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "SIN_UID"
    // Nombre del usuario actual
    val miUserName = FirebaseAuth.getInstance().currentUser?.displayName ?: "Desconocido"

    // Estado para animación al activar la alerta
    var isPressed by remember { mutableStateOf(false) }

    // Estado para mostrar el diálogo de confirmación
    var showConfirmDialog by remember { mutableStateOf(false) }

    // Estados para reflejar lo que hay en Realtime Database
    var active by remember { mutableStateOf(false) }
    var triggeredBy by remember { mutableStateOf("") }
    var triggeredByName by remember { mutableStateOf("Alguien") }

    // Animación del botón de “Activar Alerta”
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.8f else 1f,
        animationSpec = tween(durationMillis = 150)
    )

    // Referencia en Realtime Database
    val dbRef = FirebaseDatabase.getInstance().getReference("alarmState")

    // Escuchamos cambios en /alarmState y actualizamos 'active', 'triggeredBy', etc.
    LaunchedEffect(Unit) {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                active = snapshot.child("active").getValue(Boolean::class.java) ?: false
                triggeredBy = snapshot.child("triggeredBy").getValue(String::class.java) ?: ""
                triggeredByName =
                    snapshot.child("triggeredByName").getValue(String::class.java) ?: "Alguien"

                if (active) {
                    // ALARMA ACTIVA
                    if (triggeredBy == miUserId) {
                        // Soy el emisor => Iniciar TriggerLocationService (ubicación silenciosa)
                        val intent = Intent(context, TriggerLocationService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                    } else {
                        // Otro usuario la activó => Iniciar AlarmNoLocationService (sirena/vibración)
                        val intent = Intent(context, AlarmNoLocationService::class.java).apply {
                            putExtra("triggeredByName", triggeredByName)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                    }
                } else {
                    // ALARMA INACTIVA => Detenemos AMBOS servicios (por si acaso)
                    val intentLocation = Intent(context, TriggerLocationService::class.java)
                    context.stopService(intentLocation)

                    val intentNoLoc = Intent(context, AlarmNoLocationService::class.java)
                    context.stopService(intentNoLoc)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // UI principal
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // Diálogo de confirmación para activar alerta
            if (showConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showConfirmDialog = false },
                    title = { Text("¿Activar Alerta?") },
                    text = { Text("¿Estás seguro de que quieres activar la alarma?") },
                    confirmButton = {
                        TextButton(onClick = {
                            showConfirmDialog = false
                            // Si confirma, disparamos la animación
                            isPressed = true
                        }) {
                            Text("Sí")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            // Cancela
                            showConfirmDialog = false
                        }) {
                            Text("No")
                        }
                    }
                )
            }

            // Botón para ACTIVAR la alarma
            Button(
                onClick = {
                    // En lugar de activar de inmediato, primero se pide confirmación
                    showConfirmDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                shape = CircleShape,
                modifier = Modifier
                    .size(250.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = CircleShape,
                        ambientColor = Color.Red,
                        spotColor = Color.Red
                    )
                    .scale(scale),
                // DESACTIVADO si la alarma ya está activa
                enabled = !active
            ) {
                Text(
                    text = "¡Activar Alerta!",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botón para DESACTIVAR la alarma:
            // Solo lo muestra si la alarma está activa Y YO soy quien la activó.
            if (active && triggeredBy == miUserId) {
                Button(
                    onClick = {
                        // Poner active = false en /alarmState (solo el emisor puede)
                        dbRef.child("active").setValue(false)
                    },
                    shape = CircleShape
                ) {
                    Text(text = "Desactivar Alarma", fontSize = 16.sp)
                }
            }
        }
    }

    // Cuando el usuario pulsó “Activar Alerta!” y confirmó => isPressed = true
    // Este efecto ejecuta la animación y luego sube a la DB.
    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(150)
            isPressed = false

            val alarmData = mapOf(
                "active" to true,
                "triggeredBy" to miUserId,
                "triggeredByName" to miUserName,
                "timestamp" to System.currentTimeMillis()
            )
            dbRef.setValue(alarmData)
        }
    }
}
