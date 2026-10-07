package com.example.data.sync

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class FirebaseAuthService(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val currentUid: String?
        get() = auth.currentUser?.uid

    suspend fun getOrCreateAnonymousUser(): String {
        val existing = auth.currentUser
        if (existing != null) {
            return existing.uid
        }
        val result = auth.signInAnonymously().await()
        return result.user?.uid ?: throw IllegalStateException("Failed to sign in anonymously with Firebase")
    }
}
