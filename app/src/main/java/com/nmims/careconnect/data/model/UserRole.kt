package com.nmims.careconnect.data.model

/**
 * User roles supported in CareConnect.
 * Standard roles: ADMIN, PATIENT, DOCTOR, HOSPITAL.
 * HOSPITAL_ADMIN is retained as a backward-compatible alias for HOSPITAL.
 */
enum class UserRole(val label: String) {
    ADMIN("Admin"),
    PATIENT("Patient"),
    DOCTOR("Doctor"),
    HOSPITAL("Hospital Admin");

    companion object {
        /** Backward compatibility alias for existing screens */
        val HOSPITAL_ADMIN: UserRole = HOSPITAL

        fun fromString(value: String?): UserRole {
            return when (value?.trim()?.uppercase()) {
                "ADMIN" -> ADMIN
                "PATIENT" -> PATIENT
                "DOCTOR" -> DOCTOR
                "HOSPITAL", "HOSPITAL_ADMIN" -> HOSPITAL
                else -> PATIENT
            }
        }
    }
}
