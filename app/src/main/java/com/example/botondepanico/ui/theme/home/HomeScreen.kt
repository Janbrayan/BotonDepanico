package com.example.botondepanico.ui.theme.home

import android.content.Intent
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
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

    // Animación del botón de “Activar Alerta”
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.8f else 1f,
        animationSpec = tween(durationMillis = 150)
    )

    // Referencia en Realtime Database
    val dbRef = FirebaseDatabase.getInstance().getReference("alarmState")

    // Escuchamos cambios en /alarmState
    LaunchedEffect(Unit) {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val active = snapshot.child("active").getValue(Boolean::class.java) ?: false
                val triggeredBy = snapshot.child("triggeredBy").getValue(String::class.java) ?: ""
                val triggeredByName = snapshot.child("triggeredByName").getValue(String::class.java) ?: "Alguien"

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

            // Botón para ACTIVAR la alarma
            Button(
                onClick = { isPressed = true },
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
                    .scale(scale)
            ) {
                Text(
                    text = "¡Activar Alerta!",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botón para DESACTIVAR la alarma (solo tendría efecto si YO soy el emisor)
            Button(
                onClick = {
                    // Poner active = false en /alarmState
                    dbRef.child("active").setValue(false)
                },
                shape = CircleShape
            ) {
                Text(text = "Desactivar Alarma", fontSize = 16.sp)
            }
        }
    }

    // Cuando el usuario pulsa “Activar Alerta!”, ejecutamos la animación
    // y luego seteamos la DB con active = true
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
