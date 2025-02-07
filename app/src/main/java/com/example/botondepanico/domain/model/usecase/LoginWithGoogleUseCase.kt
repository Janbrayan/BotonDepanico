package com.example.botondepanico.domain.model.usecase

import com.example.botondepanico.domain.model.User
import com.example.botondepanico.domain.model.repository.AuthRepository

class LoginWithGoogleUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(idToken: String): User? {
        return authRepository.loginWithGoogle(idToken)
    }
}
