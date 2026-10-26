package com.nmims.careconnect.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.nmims.careconnect.data.firebase.Collections
import com.nmims.careconnect.data.firebase.FirebaseProvider
import com.nmims.careconnect.data.model.AdminAuditLog
import com.nmims.careconnect.data.model.UserProfile
import com.nmims.careconnect.data.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import java.util.UUID

/**
 * Repository for System Administrator operations:
 * - Querying and filtering users
 * - Updating non-security user profile details
 * - Activating / deactivating accounts with safety guards
 * - Provisioning Doctor/Hospital accounts using a secondary Firebase app instance
 *   (guaranteeing the current Admin session is never signed out or replaced)
 * - Recording immutable audit logs to `adminAuditLogs`
 */
class AdminRepository(
    private val db: FirebaseFirestore = FirebaseProvider.db,
    private val auth: FirebaseAuth = FirebaseProvider.auth,
    private val authRepo: AuthRepository = AuthRepository.getInstance()
) {

    /**
     * Fetches user profiles from Firestore with optional filtering.
     */
    suspend fun getUsers(
        roleFilter: UserRole? = null,
        activeFilter: Boolean? = null,
        searchQuery: String? = null,
        limit: Long = 50
    ): Result<List<UserProfile>> = withContext(Dispatchers.IO) {
        try {
            var query: Query = db.collection(Collections.USERS)
                .orderBy("createdAt", Query.Direction.DESCENDING)

            if (roleFilter != null) {
                query = query.whereEqualTo("role", roleFilter.name)
            }
            if (activeFilter != null) {
                query = query.whereEqualTo("active", activeFilter)
            }
            query = query.limit(limit)

            val snapshot = query.get().await()
            var users = snapshot.documents.mapNotNull { doc ->
                doc.data?.let { UserProfile.fromMap(doc.id, it) }
            }

            if (!searchQuery.isNullOrBlank()) {
                val q = searchQuery.trim().lowercase()
                users = users.filter { u ->
                    u.displayName.lowercase().contains(q) ||
                    u.email.lowercase().contains(q) ||
                    (u.phone?.lowercase()?.contains(q) == true)
                }
            }

            Result.success(users)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching users: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Returns total counts for users and active catalogue items.
     * Note: Admins NEVER read patient medical records, admissions, or clinical documents.
     */
    suspend fun getSystemCounts(): Result<SystemCounts> = withContext(Dispatchers.IO) {
        try {
            val usersCountSnap = db.collection(Collections.USERS).get().await()
            val hospitalsCountSnap = db.collection(Collections.HOSPITALS).get().await()
            val doctorsCountSnap = db.collection(Collections.DOCTORS).get().await()

            val counts = SystemCounts(
                totalUsers = usersCountSnap.size().toLong(),
                totalHospitals = hospitalsCountSnap.size().toLong(),
                totalDoctors = doctorsCountSnap.size().toLong()
            )
            Result.success(counts)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching system counts: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Updates permitted user profile fields (displayName, linkedDoctorId, linkedHospitalId).
     * Rule guard: Admins cannot change a user's role or edit other Admin profiles.
     */
    suspend fun updateUserProfile(
        targetUser: UserProfile,
        newDisplayName: String,
        newLinkedDoctorId: String?,
        newLinkedHospitalId: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val currentAdmin = auth.currentUser
            ?: return@withContext Result.failure(Exception("Admin session not found"))

        if (targetUser.role == UserRole.ADMIN && targetUser.uid != currentAdmin.uid) {
            return@withContext Result.failure(Exception("Modifying other administrator profiles is strictly forbidden."))
        }

        try {
            val updates = mapOf(
                "displayName" to newDisplayName.trim(),
                "linkedDoctorId" to newLinkedDoctorId,
                "linkedHospitalId" to newLinkedHospitalId,
                // Backward compatibility keys
                "doctorId" to newLinkedDoctorId,
                "hospitalId" to newLinkedHospitalId
            )

            db.collection(Collections.USERS).document(targetUser.uid)
                .update(updates)
                .await()

            logAudit(
                adminUid = currentAdmin.uid,
                adminEmail = currentAdmin.email.orEmpty(),
                action = "USER_PROFILE_UPDATED",
                targetUid = targetUser.uid,
                details = mapOf(
                    "previousName" to targetUser.displayName,
                    "updatedName" to newDisplayName,
                    "linkedDoctorId" to newLinkedDoctorId,
                    "linkedHospitalId" to newLinkedHospitalId
                )
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Sets user active status (activate/deactivate).
     * Safety rules:
     * - Admin cannot deactivate itself.
     * - Admin cannot deactivate another Admin.
     */
    suspend fun setUserActiveStatus(
        targetUser: UserProfile,
        newActive: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val currentAdmin = auth.currentUser
            ?: return@withContext Result.failure(Exception("Admin session not found"))

        if (targetUser.uid == currentAdmin.uid) {
            return@withContext Result.failure(Exception("Administrators cannot deactivate their own account."))
        }
        if (targetUser.role == UserRole.ADMIN) {
            return@withContext Result.failure(Exception("Modifying another administrator account is forbidden."))
        }

        try {
            db.collection(Collections.USERS).document(targetUser.uid)
                .update(
                    mapOf(
                        "active" to newActive,
                        "isApproved" to newActive
                    )
                )
                .await()

            val actionName = if (newActive) "USER_ACTIVATED" else "USER_DEACTIVATED"
            logAudit(
                adminUid = currentAdmin.uid,
                adminEmail = currentAdmin.email.orEmpty(),
                action = actionName,
                targetUid = targetUser.uid,
                details = mapOf(
                    "targetEmail" to targetUser.email,
                    "targetRole" to targetUser.role.name,
                    "newActiveState" to newActive
                )
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user active status: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Sends password reset email to a managed user and writes an audit log.
     */
    suspend fun sendPasswordReset(targetUser: UserProfile): Result<Unit> = withContext(Dispatchers.IO) {
        val currentAdmin = auth.currentUser
            ?: return@withContext Result.failure(Exception("Admin session not found"))

        val resetResult = authRepo.sendPasswordReset(targetUser.email)
        if (resetResult.isSuccess) {
            logAudit(
                adminUid = currentAdmin.uid,
                adminEmail = currentAdmin.email.orEmpty(),
                action = "PASSWORD_RESET_DISPATCHED",
                targetUid = targetUser.uid,
                details = mapOf("targetEmail" to targetUser.email)
            )
        }
        resetResult
    }

    /**
     * Securely provisions a new DOCTOR or HOSPITAL account:
     * 1. Uses a secondary, isolated FirebaseApp instance so the current Admin session is NEVER disturbed or replaced.
     * 2. Generates a secure, temporary random password in memory; NEVER displays, stores, or logs it.
     * 3. Creates the matching `users/{uid}` profile with role and catalogue links.
     * 4. Sends a password-reset email to the newly provisioned staff member.
     * 5. Closes the secondary app instance and writes an audit log.
     */
    suspend fun provisionStaffAccount(
        context: Context,
        email: String,
        displayName: String,
        role: UserRole,
        linkedDoctorId: String?,
        linkedHospitalId: String?
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        val currentAdmin = auth.currentUser
            ?: return@withContext Result.failure(Exception("Admin session not found"))

        if (role != UserRole.DOCTOR && role != UserRole.HOSPITAL) {
            return@withContext Result.failure(Exception("Only DOCTOR and HOSPITAL accounts may be provisioned by an Administrator."))
        }

        val secondaryAppName = "StaffProvisioningApp_${UUID.randomUUID()}"
        var secondaryApp: FirebaseApp? = null

        try {
            val defaultApp = FirebaseApp.getInstance()
            val options: FirebaseOptions = defaultApp.options

            // Initialize isolated secondary FirebaseApp
            secondaryApp = FirebaseApp.initializeApp(context, options, secondaryAppName)
            val secondaryAuth = FirebaseAuth.getInstance(secondaryApp)

            // Generate strong internal random password
            val temporaryPassword = generateSecureRandomPassword()

            // Create Firebase Auth user in secondary app
            val authResult = secondaryAuth.createUserWithEmailAndPassword(email.trim(), temporaryPassword).await()
            val newUid = authResult.user?.uid
                ?: throw IllegalStateException("Firebase user was null after staff creation")

            // Create user profile in Firestore
            val profile = UserProfile(
                uid = newUid,
                role = role,
                email = email.trim(),
                displayName = displayName.trim(),
                active = true,
                linkedDoctorId = linkedDoctorId,
                linkedHospitalId = linkedHospitalId,
                createdAt = Timestamp.now(),
                createdBy = currentAdmin.uid,
                lastLoginAt = null
            )

            db.collection(Collections.USERS).document(newUid)
                .set(profile.toMap())
                .await()

            // Send password reset email to new staff member
            secondaryAuth.sendPasswordResetEmail(email.trim()).await()

            // Record audit log
            logAudit(
                adminUid = currentAdmin.uid,
                adminEmail = currentAdmin.email.orEmpty(),
                action = "STAFF_PROVISIONED",
                targetUid = newUid,
                details = mapOf(
                    "role" to role.name,
                    "email" to email.trim(),
                    "displayName" to displayName.trim(),
                    "linkedDoctorId" to linkedDoctorId,
                    "linkedHospitalId" to linkedHospitalId
                )
            )

            Result.success(profile)
        } catch (e: Exception) {
            Log.e(TAG, "Error provisioning staff account: ${e.message}", e)
            Result.failure(e)
        } finally {
            try {
                secondaryApp?.delete()
            } catch (e: Exception) {
                Log.w(TAG, "Error cleaning up secondary FirebaseApp: ${e.message}")
            }
        }
    }

    /**
     * Records an immutable entry in `adminAuditLogs/{id}`.
     */
    private suspend fun logAudit(
        adminUid: String,
        adminEmail: String,
        action: String,
        targetUid: String,
        details: Map<String, Any?> = emptyMap()
    ) {
        try {
            val logId = UUID.randomUUID().toString()
            val log = AdminAuditLog(
                id = logId,
                adminUid = adminUid,
                adminEmail = adminEmail,
                action = action,
                targetUid = targetUid,
                details = details,
                timestamp = Timestamp.now()
            )
            db.collection(Collections.ADMIN_AUDIT_LOGS).document(logId)
                .set(log.toMap())
                .await()
        } catch (e: Exception) {
            Log.w(TAG, "Non-critical: could not write admin audit log: ${e.message}")
        }
    }

    private fun generateSecureRandomPassword(length: Int = 20): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*-_"
        val random = SecureRandom()
        val sb = StringBuilder(length)
        for (i in 0 until length) {
            sb.append(chars[random.nextInt(chars.length)])
        }
        return sb.toString()
    }

    companion object {
        private const val TAG = "AdminRepository"

        @Volatile
        private var INSTANCE: AdminRepository? = null

        fun getInstance(): AdminRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AdminRepository().also { INSTANCE = it }
            }
        }
    }
}

data class SystemCounts(
    val totalUsers: Long = 0,
    val totalHospitals: Long = 0,
    val totalDoctors: Long = 0
)
