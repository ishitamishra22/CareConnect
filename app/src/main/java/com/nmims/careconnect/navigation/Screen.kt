package com.nmims.careconnect.navigation

import com.nmims.careconnect.data.model.UserRole

/**
 * Sealed hierarchy defining all navigation destinations in CareConnect.
 */
sealed class Screen {
    data object RoleSelection : Screen()
    data class Login(val role: UserRole) : Screen()
    data object PatientHome : Screen()
    data object DoctorHome : Screen()
    data object AdminHome : Screen()
    data object Portfolio : Screen()
    data object EditProfile : Screen()
    data object MedicalRecords : Screen()
    data class AddEditRecord(val recordId: String?) : Screen()
    data object Documents : Screen()
    data object AddDocument : Screen()
    data class DocumentViewer(val documentId: String) : Screen()
    data object About : Screen()
}
