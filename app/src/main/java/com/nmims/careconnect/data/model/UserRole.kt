package com.nmims.careconnect.data.model

/**
 * User roles supported in CareConnect.
 */
enum class UserRole(val label: String) {
    PATIENT("Patient"),
    DOCTOR("Doctor"),
    HOSPITAL_ADMIN("Hospital Admin")
}
