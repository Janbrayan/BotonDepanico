package com.example.botondepanico.data.remote

import com.example.botondepanico.domain.model.User
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class GoogleAuthRemoteDataSource {

    private val auth = Firebase.auth

    suspend fun signInWithGoogle(idToken: String): User? {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        val firebaseUser = result.user ?: return null

        return User(
            uid = firebaseUser.uid,
            displayName = firebaseUser.displayName,
            email = firebaseUser.email,
            photoUrl = firebaseUser.photoUrl?.toString()
        )
    }

    fun signOut() {
        auth.signOut()
    }

    fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser ?: return null
        return User(
            uid = firebaseUser.uid,
            displayName = firebaseUser.displayName,
            email = firebaseUser.email,
            photoUrl = firebaseUser.photoUrl?.toString()
        )
    }
}
