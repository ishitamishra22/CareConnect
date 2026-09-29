package com.nmims.careconnect.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * Singleton provider for Firebase Auth, Firestore, and Cloud Storage instances.
 * Provides anonymous sign-in management and a safeCall helper with timeout.
 */
object FirebaseProvider {
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    val storage: FirebaseStorage = FirebaseStorage.getInstance()

    suspend fun ensureSignedIn() {
        if (auth.currentUser == null) {
            auth.signInAnonymously().await()
        }
    }

    suspend fun <T> safeCall(timeoutMs: Long = 15000, block: suspend () -> T): Result<T> {
        return try {
            ensureSignedIn()
            withTimeout(timeoutMs) {
                Result.success(block())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
