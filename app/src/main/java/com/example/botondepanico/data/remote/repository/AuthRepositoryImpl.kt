package com.example.botondepanico.data.remote.repository

import com.example.botondepanico.data.remote.GoogleAuthRemoteDataSource
import com.example.botondepanico.domain.model.User
import com.example.botondepanico.domain.model.repository.AuthRepository

class AuthRepositoryImpl(
    private val remoteDataSource: GoogleAuthRemoteDataSource
) : AuthRepository {

    override suspend fun loginWithGoogle(idToken: String): User? {
        return remoteDataSource.signInWithGoogle(idToken)
    }

    override suspend fun logout() {
        remoteDataSource.signOut()
    }

    override fun getCurrentUser(): User? {
        return remoteDataSource.getCurrentUser()
    }
}
