package com.example.botondepanico.domain.model.repository

import com.example.botondepanico.domain.model.User

interface AuthRepository {
    suspend fun loginWithGoogle(idToken: String): User?
    suspend fun logout()
    fun getCurrentUser(): User?
}
