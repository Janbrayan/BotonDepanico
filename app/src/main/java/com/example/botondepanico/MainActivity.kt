package com.example.botondepanico

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.example.botondepanico.data.remote.GoogleAuthRemoteDataSource
import com.example.botondepanico.data.remote.repository.AuthRepositoryImpl
import com.example.botondepanico.domain.model.repository.AuthRepository
import com.example.botondepanico.domain.model.usecase.LoginWithGoogleUseCase
import com.example.botondepanico.ui.theme.MyApplicationTheme
import com.example.botondepanico.ui.theme.auth.GoogleAuthViewModel
import com.example.botondepanico.ui.theme.navigation.AppNavHost

class MainActivity : ComponentActivity() {

    private val remoteDataSource = GoogleAuthRemoteDataSource()
    private val authRepository: AuthRepository = AuthRepositoryImpl(remoteDataSource)
    private val loginUseCase = LoginWithGoogleUseCase(authRepository)
    private val authViewModel = GoogleAuthViewModel(loginUseCase)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                // Montamos el NavHost con nuestro ViewModel de autenticación
                AppNavHost(
                    navController = navController,
                    authViewModel = authViewModel
                )
            }
        }
    }
}
