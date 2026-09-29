package com.nmims.careconnect.session

import com.nmims.careconnect.data.model.UserRole

/**
 * In-memory active user session.
 * Tracks the current role and user details across the application.
 */
object AppSession {
    var currentRole: UserRole? = null
    var currentUserName: String = ""
    var currentPatientId: String = ""
    var currentDoctorId: String? = null
    var currentHospitalId: String? = null

    fun logout() {
        currentRole = null
        currentUserName = ""
        currentPatientId = ""
        currentDoctorId = null
        currentHospitalId = null
    }
}
