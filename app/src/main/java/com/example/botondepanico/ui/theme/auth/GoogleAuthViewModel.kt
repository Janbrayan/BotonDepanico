package com.example.botondepanico.ui.theme.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.botondepanico.domain.model.User
import com.example.botondepanico.domain.model.usecase.LoginWithGoogleUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GoogleAuthViewModel(
    private val loginWithGoogleUseCase: LoginWithGoogleUseCase
) : ViewModel() {

    private val _userState = MutableStateFlow<User?>(null)
    val userState = _userState.asStateFlow()

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState = _errorState.asStateFlow()

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            try {
                val user = loginWithGoogleUseCase(idToken)
                if (user != null) {
                    _userState.value = user
                } else {
                    _errorState.value = "Falló el inicio de sesión."
                }
            } catch (e: Exception) {
                _errorState.value = e.message
            }
        }
    }
}
