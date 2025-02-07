package com.example.botondepanico.ui.theme.auth

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Animatable
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.botondepanico.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun GoogleLoginScreen(
    navController: NavController,
    viewModel: GoogleAuthViewModel
) {
    // Observa el estado del usuario y el error
    val userState by viewModel.userState.collectAsState()
    val errorState by viewModel.errorState.collectAsState()

    // Si ya estamos logueados, navegar a Main
    LaunchedEffect(userState) {
        if (userState != null) {
            navController.navigate("main") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    // Preparar ActivityResult para Google Sign-In
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data: Intent? = result.data
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            // Obtenemos la cuenta y verificamos el idToken.
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (!idToken.isNullOrEmpty()) {
                viewModel.loginWithGoogle(idToken)
            }
        } catch (e: ApiException) {
            // El usuario canceló o hubo otro error -> No cerramos la app ni crasheamos.
            // Simplemente no hacemos nada y seguimos en la pantalla de login.
        }
    }

    // Configurar GoogleSignInOptions con tu Client ID real
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken("749541236547-i54bdt5dhkkk84blt19uq4c6vc2pidie.apps.googleusercontent.com")
        .requestEmail()
        .build()

    val context = LocalContext.current
    val googleSignInClient = GoogleSignIn.getClient(context, gso)

    // 1) Animación de degradado pulsante en el fondo
    val infiniteTransition = rememberInfiniteTransition()
    val colorTop by infiniteTransition.animateColor(
        initialValue = Color(0xFF512DA8), // Morado oscuro
        targetValue = Color(0xFF303F9F), // Indigo
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        )
    )
    val colorBottom by infiniteTransition.animateColor(
        initialValue = Color(0xFF303F9F),
        targetValue = Color(0xFF512DA8),
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        )
    )

    // 2) Fade-in suave de todo el contenido
    val alphaAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        alphaAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing)
        )
    }

    // 3) Fondo pulsante (gradient animado)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(colorTop, colorBottom)
                )
            )
    ) {
        // Contenido principal con fade-in
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .graphicsLayer {
                    alpha = alphaAnim.value
                },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Título
            Text(
                text = "Bienvenido a Colonias Unidas",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Texto extra (opcional)
            Text(
                text = "Inicia sesión para acceder a todas las funcionalidades",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.8f)
                ),
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Tarjeta sin sombra ni fondo blanco
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 4) Animación de "press" en el botón
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scaleAnim by animateFloatAsState(
                        targetValue = if (isPressed) 0.95f else 1f,
                        animationSpec = tween(durationMillis = 150)
                    )

                    // Botón personalizado
                    Button(
                        onClick = {
                            launcher.launch(googleSignInClient.signInIntent)
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF202020) // Fondo oscuro
                        ),
                        interactionSource = interactionSource,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .scale(scaleAnim)
                    ) {
                        // Icono de Google
                        Icon(
                            painter = painterResource(id = R.drawable.ic_google_logo),
                            contentDescription = "Google Logo",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Sign in with Google",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    // Separador
                    Spacer(modifier = Modifier.height(16.dp))

                    // Mensaje de error, si existe
                    if (errorState != null) {
                        Text(
                            text = "Error: $errorState",
                            color = Color.Red,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
