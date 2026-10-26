package com.nmims.careconnect.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

/**
 * Shared Firebase Emulator configuration helper for local development and integration testing.
 */
object EmulatorConfig {

    private const val DEFAULT_EMULATOR_HOST = "10.0.2.2" // Android emulator loopback
    private const val AUTH_PORT = 9099
    private const val FIRESTORE_PORT = 8080
    private const val STORAGE_PORT = 9199

    var isEmulatorEnabled: Boolean = false
        private set

    /**
     * Connects all Firebase services to local emulators.
     * Call this in Application.onCreate() or during test setup if running against emulators.
     */
    fun useEmulator(
        host: String = DEFAULT_EMULATOR_HOST,
        auth: FirebaseAuth = FirebaseProvider.auth,
        db: FirebaseFirestore = FirebaseProvider.db,
        storage: FirebaseStorage = FirebaseProvider.storage
    ) {
        if (isEmulatorEnabled) return
        try {
            auth.useEmulator(host, AUTH_PORT)
            db.useEmulator(host, FIRESTORE_PORT)
            storage.useEmulator(host, STORAGE_PORT)
            isEmulatorEnabled = true
        } catch (_: Exception) {
            // Emulators may already be connected or not reachable in certain test environments
        }
    }
}
