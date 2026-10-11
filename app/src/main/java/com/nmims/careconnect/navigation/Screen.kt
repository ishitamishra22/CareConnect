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

    // Phase 2: Hospital & Doctor Search and Comparison
    open class Search(val initialTab: Int = 0) : Screen() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Search) return false
            return initialTab == other.initialTab
        }
        override fun hashCode(): Int = initialTab.hashCode()
        override fun toString(): String = "Search(initialTab=$initialTab)"
        companion object : Search(0)
    }
    data class HospitalDetail(val hospitalId: String) : Screen()
    data class DoctorDetail(val doctorId: String) : Screen()
    data class BookAppointment(val doctorId: String) : Screen()
    data object BookLabTest : Screen()
    data object BookingHub : Screen()
    data class BookingSuccess(val bookingId: String) : Screen()
    data object MyBookings : Screen()
    data class BookingDetail(val bookingId: String) : Screen()
    data class DoctorAppointmentDetail(val bookingId: String) : Screen()
    data class Compare(val doctorIds: List<String>) : Screen()

    // Phase 4: Automated Hospital Admission & Admin Dashboard
    data object AdmissionForm : Screen()
    data class AdmissionSuccess(val admissionId: String) : Screen()
    data object MyAdmissions : Screen()
    data class AdmissionDetail(val admissionId: String) : Screen()
    data class AdminAdmissionReview(val admissionId: String) : Screen()
}
