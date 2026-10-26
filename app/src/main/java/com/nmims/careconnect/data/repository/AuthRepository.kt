package com.nmims.careconnect.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.nmims.careconnect.data.auth.AuthError
import com.nmims.careconnect.data.auth.AuthState
import com.nmims.careconnect.data.firebase.FirebaseProvider
import com.nmims.careconnect.data.model.UserProfile
import com.nmims.careconnect.data.model.UserRole
import com.nmims.careconnect.session.AppSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Shared Authentication Repository for CareConnect.
 * Manages Firebase Email/Password Authentication, User Profiles in Firestore (`users/{uid}`),
 * and session state.
 *
 * Implements the contract:
 * - signInWithEmail(email, password, expectedRole)
 * - signOut()
 * - sendPasswordReset(email)
 * - reauthenticate(password)
 * - changePassword(oldPassword, newPassword)
 * - loadProfile(uid)
 */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseProvider.auth,
    private val db: FirebaseFirestore = FirebaseProvider.db,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        // Observe Firebase Auth state transitions
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user == null) {
                _authState.value = AuthState.SignedOut
                AppSession.logout()
            } else {
                externalScope.launch {
                    refreshCurrentUserProfile(user)
                }
            }
        }
    }

    /**
     * Initializes and restores the session on app launch.
     */
    suspend fun initialize(): AuthState = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) {
            _authState.value = AuthState.SignedOut
            AuthState.SignedOut
        } else {
            refreshCurrentUserProfile(user)
        }
    }

    /**
     * Authenticates an existing user via Email & Password and validates role match and active status.
     * Never stores passwords in Firestore, logs, or local storage.
     */
    suspend fun signInWithEmail(
        email: String,
        pass: String,
        expectedRole: UserRole? = null
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        _authState.value = AuthState.Loading
        try {
            val authResult = auth.signInWithEmailAndPassword(email.trim(), pass).await()
            val firebaseUser = authResult.user
                ?: throw IllegalStateException("Firebase user was null after successful sign-in")

            val profile = fetchProfileFromFirestore(firebaseUser.uid)
                ?: createFallbackProfile(firebaseUser, expectedRole ?: UserRole.PATIENT)

            // Account status validation: inactive / blocked accounts fail closed
            if (!profile.active) {
                auth.signOut()
                val isPending = profile.createdBy == "self_registration" || profile.createdBy == "self"
                val blockedReason = if (isPending && (profile.role == UserRole.DOCTOR || profile.role == UserRole.HOSPITAL)) {
                    "Your application is currently pending approval by a System Administrator."
                } else {
                    "Your account has been deactivated or blocked by an administrator."
                }
                _authState.value = AuthState.Blocked(blockedReason)
                return@withContext Result.failure(Exception(blockedReason))
            }

            // Role match validation
            if (expectedRole != null && profile.role != expectedRole && profile.role != UserRole.ADMIN) {
                auth.signOut()
                val err = AuthError.UnauthorizedRole(
                    "This account is registered as ${profile.role.label}, but you are signing in as ${expectedRole.label}."
                )
                _authState.value = AuthState.SignedOut
                return@withContext Result.failure(Exception(err.message))
            }

            // Update lastLoginAt timestamp
            try {
                db.collection(USERS_COLLECTION).document(firebaseUser.uid)
                    .update("lastLoginAt", Timestamp.now())
                    .await()
            } catch (e: Exception) {
                Log.w(TAG, "Non-critical: could not update lastLoginAt: ${e.message}")
            }

            val updatedProfile = profile.copy(lastLoginAt = Timestamp.now())
            syncToAppSession(updatedProfile)
            _authState.value = AuthState.SignedIn(updatedProfile)
            Result.success(updatedProfile)
        } catch (e: Throwable) {
            val error = AuthError.fromException(e)
            _authState.value = AuthState.SignedOut
            Result.failure(Exception(error.message, e))
        }
    }

    /** Backward compatibility alias for existing callers */
    suspend fun signIn(
        email: String,
        pass: String,
        expectedRole: UserRole? = null
    ): Result<UserProfile> = signInWithEmail(email, pass, expectedRole)

    /**
     * Loads a user profile by UID directly from Firestore.
     */
    suspend fun loadProfile(uid: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val profile = fetchProfileFromFirestore(uid)
            if (profile != null) {
                Result.success(profile)
            } else {
                Result.failure(Exception("Profile not found for UID: $uid"))
            }
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    /**
     * Sends a password reset email to the given address.
     */
    suspend fun sendPasswordReset(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Throwable) {
            val error = AuthError.fromException(e)
            Result.failure(Exception(error.message, e))
        }
    }

    /** Backward compatibility alias for sendPasswordReset */
    suspend fun resetPassword(email: String): Result<Unit> = sendPasswordReset(email)

    /**
     * Re-authenticates the current user using their current password before sensitive changes.
     */
    suspend fun reauthenticate(password: String): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
            ?: return@withContext Result.failure(Exception("No authenticated user to reauthenticate"))
        val email = user.email
            ?: return@withContext Result.failure(Exception("User email is not available"))

        try {
            val credential = EmailAuthProvider.getCredential(email, password)
            user.reauthenticate(credential).await()
            Result.success(Unit)
        } catch (e: Throwable) {
            val error = AuthError.fromException(e)
            Result.failure(Exception(error.message, e))
        }
    }

    /**
     * Changes the current user's password following reauthentication.
     */
    suspend fun changePassword(oldPass: String, newPass: String): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
            ?: return@withContext Result.failure(Exception("No authenticated user"))

        // Reauthenticate first
        val reauthRes = reauthenticate(oldPass)
        if (reauthRes.isFailure) {
            return@withContext Result.failure(reauthRes.exceptionOrNull() ?: Exception("Reauthentication failed"))
        }

        try {
            user.updatePassword(newPass).await()
            Result.success(Unit)
        } catch (e: Throwable) {
            val error = AuthError.fromException(e)
            Result.failure(Exception(error.message, e))
        }
    }

    /**
     * Registers a new Doctor applicant in a restricted pending-approval state:
     * - Creates Firebase Authentication credentials
     * - Dispatches email verification
     * - Records a pending registration profile in `users/{uid}` with `active = false` and `createdBy = "self_registration"`
     * - Does NOT assign active privileges or linkedDoctorId (assigned only upon Admin verification)
     * - Signs the user out immediately so unapproved applicants cannot access dashboards.
     */
    suspend fun registerDoctorPending(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        registrationNumber: String,
        specialization: String,
        hospitalName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val authResult = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val firebaseUser = authResult.user
                ?: throw IllegalStateException("Firebase user was null after registration")

            // Send verification email
            try {
                firebaseUser.sendEmailVerification().await()
            } catch (e: Exception) {
                Log.w(TAG, "Email verification dispatch notice: ${e.message}")
            }

            // Create restricted pending profile (active = false)
            val profile = UserProfile(
                uid = firebaseUser.uid,
                role = UserRole.DOCTOR,
                email = email.trim(),
                phone = phone.trim(),
                displayName = fullName.trim(),
                active = false, // Restricted pending approval
                linkedDoctorId = null, // Set only by Admin upon identity verification
                linkedHospitalId = null,
                createdAt = Timestamp.now(),
                createdBy = "self_registration",
                lastLoginAt = null
            )

            val profileMap = profile.toMap().toMutableMap().apply {
                put("registrationNumber", registrationNumber.trim())
                put("specialization", specialization.trim())
                put("requestedHospital", hospitalName.trim())
                put("approvalStatus", "PENDING")
            }

            db.collection(USERS_COLLECTION).document(firebaseUser.uid)
                .set(profileMap)
                .await()

            // Sign out to enforce pending gate
            auth.signOut()
            _authState.value = AuthState.SignedOut
            Result.success(Unit)
        } catch (e: Throwable) {
            val error = AuthError.fromException(e)
            Result.failure(Exception(error.message, e))
        }
    }

    /**
     * Registers a new Hospital Staff applicant in a restricted pending-approval state:
     * - Creates Firebase Authentication credentials
     * - Dispatches email verification
     * - Records a pending registration profile in `users/{uid}` with `active = false` and `createdBy = "self_registration"`
     * - Does NOT assign active privileges or linkedHospitalId (assigned only upon Admin verification)
     * - Signs the user out immediately so unapproved applicants cannot access dashboards.
     */
    suspend fun registerHospitalStaffPending(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        designation: String,
        hospitalName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val authResult = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val firebaseUser = authResult.user
                ?: throw IllegalStateException("Firebase user was null after registration")

            // Send verification email
            try {
                firebaseUser.sendEmailVerification().await()
            } catch (e: Exception) {
                Log.w(TAG, "Email verification dispatch notice: ${e.message}")
            }

            // Create restricted pending profile (active = false)
            val profile = UserProfile(
                uid = firebaseUser.uid,
                role = UserRole.HOSPITAL,
                email = email.trim(),
                phone = phone.trim(),
                displayName = fullName.trim(),
                active = false, // Restricted pending approval
                linkedDoctorId = null,
                linkedHospitalId = null, // Admin establishes valid linkedHospitalId before activation
                createdAt = Timestamp.now(),
                createdBy = "self_registration",
                lastLoginAt = null
            )

            val profileMap = profile.toMap().toMutableMap().apply {
                put("designation", designation.trim())
                put("requestedHospital", hospitalName.trim())
                put("approvalStatus", "PENDING")
            }

            db.collection(USERS_COLLECTION).document(firebaseUser.uid)
                .set(profileMap)
                .await()

            // Sign out to enforce pending gate
            auth.signOut()
            _authState.value = AuthState.SignedOut
            Result.success(Unit)
        } catch (e: Throwable) {
            val error = AuthError.fromException(e)
            Result.failure(Exception(error.message, e))
        }
    }

    /**
     * Signs out the current user and clears session state.
     */
    fun signOut() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            Log.e(TAG, "Error during signOut", e)
        }
        AppSession.logout()
        _authState.value = AuthState.SignedOut
    }

    /**
     * Fetches user profile from Firestore or handles blocked/fallback states.
     */
    private suspend fun refreshCurrentUserProfile(user: FirebaseUser): AuthState {
        return try {
            val profile = fetchProfileFromFirestore(user.uid)
                ?: createFallbackProfile(user, UserRole.PATIENT)

            if (!profile.active) {
                auth.signOut()
                AppSession.logout()
                val blockedState = AuthState.Blocked()
                _authState.value = blockedState
                blockedState
            } else {
                syncToAppSession(profile)
                val signedInState = AuthState.SignedIn(profile)
                _authState.value = signedInState
                signedInState
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to refresh profile for uid ${user.uid}: ${e.message}")
            val errorState = AuthState.SignedOut
            _authState.value = errorState
            errorState
        }
    }

    private suspend fun fetchProfileFromFirestore(uid: String): UserProfile? {
        val snap = db.collection(USERS_COLLECTION).document(uid).get().await()
        return if (snap.exists() && snap.data != null) {
            UserProfile.fromMap(uid, snap.data!!)
        } else {
            null
        }
    }

    private suspend fun createFallbackProfile(user: FirebaseUser, fallbackRole: UserRole): UserProfile {
        val profile = UserProfile(
            uid = user.uid,
            role = fallbackRole,
            email = user.email.orEmpty(),
            displayName = user.displayName ?: user.email?.substringBefore("@").orEmpty().ifBlank { "User" },
            active = true,
            createdBy = "self"
        )
        try {
            db.collection(USERS_COLLECTION).document(user.uid).set(profile.toMap()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Could not persist fallback profile: ${e.message}")
        }
        return profile
    }

    private fun syncToAppSession(profile: UserProfile) {
        AppSession.currentRole = profile.role
        AppSession.currentUserName = profile.displayName
        AppSession.currentPatientId = if (profile.role == UserRole.PATIENT) profile.uid else ""
        AppSession.currentDoctorId = profile.linkedDoctorId
        AppSession.currentHospitalId = profile.linkedHospitalId
    }

    companion object {
        private const val TAG = "AuthRepository"
        const val USERS_COLLECTION = "users"

        @Volatile
        private var INSTANCE: AuthRepository? = null

        fun getInstance(
            auth: FirebaseAuth = FirebaseProvider.auth,
            db: FirebaseFirestore = FirebaseProvider.db
        ): AuthRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthRepository(auth, db).also { INSTANCE = it }
            }
        }
    }
}
