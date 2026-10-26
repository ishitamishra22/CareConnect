package com.nmims.careconnect.data.model

import com.google.firebase.Timestamp

/**
 * Shared User Profile stored in Firestore under `users/{uid}`.
 * Strictly adheres to the CareConnect Shared Authentication Contract:
 * - uid: Unique identifier matching Firebase Auth UID
 * - role: UserRole enum (ADMIN, PATIENT, DOCTOR, HOSPITAL)
 * - email: Primary account email address
 * - phone: Contact phone number (nullable)
 * - displayName: Full user or organization name
 * - active: Account status flag (true = active, false = disabled/blocked)
 * - linkedDoctorId: Associated doctor ID (for DOCTOR role, nullable)
 * - linkedHospitalId: Associated hospital ID (for HOSPITAL role, nullable)
 * - createdAt: Timestamp of account creation
 * - createdBy: UID of the creator ("self" for self-registered patient, or admin UID)
 * - lastLoginAt: Timestamp of the most recent authentication session
 */
data class UserProfile(
    val uid: String = "",
    val role: UserRole = UserRole.PATIENT,
    val email: String = "",
    val phone: String? = null,
    val displayName: String = "",
    val active: Boolean = true,
    val linkedDoctorId: String? = null,
    val linkedHospitalId: String? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val createdBy: String? = "self",
    val lastLoginAt: Timestamp? = Timestamp.now()
) {
    // Backward compatibility properties for existing modules
    val doctorId: String? get() = linkedDoctorId
    val hospitalId: String? get() = linkedHospitalId
    val phoneNumber: String? get() = phone
    val isApproved: Boolean get() = active

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "uid" to uid,
            "role" to role.name,
            "email" to email,
            "phone" to phone,
            "displayName" to displayName,
            "active" to active,
            "linkedDoctorId" to linkedDoctorId,
            "linkedHospitalId" to linkedHospitalId,
            "createdAt" to createdAt,
            "createdBy" to createdBy,
            "lastLoginAt" to lastLoginAt,
            // Retain legacy keys for database backward-compatibility
            "doctorId" to linkedDoctorId,
            "hospitalId" to linkedHospitalId,
            "phoneNumber" to phone,
            "isApproved" to active
        )
    }

    companion object {
        fun fromMap(uid: String, map: Map<String, Any?>): UserProfile {
            val roleVal = UserRole.fromString(map["role"] as? String)

            // Resolve fields with legacy fallbacks
            val phoneVal = (map["phone"] as? String) ?: (map["phoneNumber"] as? String)
            val doctorIdVal = (map["linkedDoctorId"] as? String) ?: (map["doctorId"] as? String)
            val hospitalIdVal = (map["linkedHospitalId"] as? String) ?: (map["hospitalId"] as? String)
            val activeVal = (map["active"] as? Boolean) ?: (map["isApproved"] as? Boolean) ?: true

            return UserProfile(
                uid = uid,
                role = roleVal,
                email = (map["email"] as? String).orEmpty(),
                phone = phoneVal,
                displayName = (map["displayName"] as? String).orEmpty(),
                active = activeVal,
                linkedDoctorId = doctorIdVal,
                linkedHospitalId = hospitalIdVal,
                createdAt = (map["createdAt"] as? Timestamp) ?: Timestamp.now(),
                createdBy = map["createdBy"] as? String ?: "self",
                lastLoginAt = map["lastLoginAt"] as? Timestamp
            )
        }
    }
}
