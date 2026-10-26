package com.nmims.careconnect

import com.google.firebase.Timestamp
import com.nmims.careconnect.data.auth.AuthError
import com.nmims.careconnect.data.auth.AuthState
import com.nmims.careconnect.data.auth.AuthValidators
import com.nmims.careconnect.data.model.PatientProfile
import com.nmims.careconnect.data.model.UserProfile
import com.nmims.careconnect.data.model.UserRole
import com.nmims.careconnect.session.AppSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PatientAuthUnitTest {

    @Before
    fun setup() {
        AppSession.logout()
    }

    @Test
    fun testPatientRegistrationContractProfileCreation() {
        val now = Timestamp.now()
        val patientProfile = UserProfile(
            uid = "patient_uid_123",
            role = UserRole.PATIENT,
            email = "patient.sharma@example.com",
            phone = "+919876543210",
            displayName = "Rahul Sharma",
            active = true,
            linkedDoctorId = null,
            linkedHospitalId = null,
            createdAt = now,
            createdBy = "self",
            lastLoginAt = now
        )

        // Strict role verification: must be PATIENT
        assertEquals(UserRole.PATIENT, patientProfile.role)
        assertEquals("patient_uid_123", patientProfile.uid)
        assertEquals("Rahul Sharma", patientProfile.displayName)
        assertEquals("+919876543210", patientProfile.phone)
        assertTrue(patientProfile.active)
        assertNull(patientProfile.linkedDoctorId)
        assertNull(patientProfile.linkedHospitalId)
        assertEquals("self", patientProfile.createdBy)

        // Verify Map serialization adheres to firestore rules
        val map = patientProfile.toMap()
        assertEquals("PATIENT", map["role"])
        assertEquals("patient_uid_123", map["uid"])
        assertEquals(true, map["active"])
        assertNull(map["linkedDoctorId"])
        assertNull(map["linkedHospitalId"])
        assertEquals("self", map["createdBy"])
    }

    @Test
    fun testPatientSessionRestorationAndUidLinkage() {
        val profile = UserProfile(
            uid = "patient_uid_777",
            role = UserRole.PATIENT,
            email = "anita.verma@example.com",
            phone = "+919123456780",
            displayName = "Anita Verma",
            active = true
        )

        // Simulate AuthRepository.syncToAppSession(profile)
        AppSession.currentRole = profile.role
        AppSession.currentUserName = profile.displayName
        AppSession.currentPatientId = if (profile.role == UserRole.PATIENT) profile.uid else ""
        AppSession.currentDoctorId = profile.linkedDoctorId
        AppSession.currentHospitalId = profile.linkedHospitalId

        // Verify AppSession reflects patient's UID
        assertEquals("patient_uid_777", AppSession.currentPatientId)
        assertEquals("Anita Verma", AppSession.currentUserName)
        assertEquals(UserRole.PATIENT, AppSession.currentRole)
        assertNull(AppSession.currentDoctorId)
        assertNull(AppSession.currentHospitalId)

        // Verify Patient demographic profile creation mapping
        val patientDemographic = PatientProfile(
            name = profile.displayName,
            age = 28,
            gender = "Female",
            bloodGroup = "O+",
            phone = profile.phone.orEmpty(),
            email = profile.email,
            address = "Mumbai, Maharashtra",
            emergencyContactName = "Ramesh Verma",
            emergencyContactPhone = "+919876500000"
        )
        val demoMap = patientDemographic.toMap()
        assertEquals("Anita Verma", demoMap["name"])
        assertEquals("+919123456780", demoMap["phone"])
        assertEquals("anita.verma@example.com", demoMap["email"])
    }

    @Test
    fun testIndianMobileNumberValidationAndFormatting() {
        // Valid 10-digit Indian numbers starting with 6, 7, 8, 9
        assertTrue(AuthValidators.validateIndianMobile("9876543210").isValid)
        assertTrue(AuthValidators.validateIndianMobile("8888888888").isValid)
        assertTrue(AuthValidators.validateIndianMobile("7777777777").isValid)
        assertTrue(AuthValidators.validateIndianMobile("6666666666").isValid)

        // Valid with prefix
        assertTrue(AuthValidators.validateIndianMobile("+919876543210").isValid)
        assertTrue(AuthValidators.validateIndianMobile("+91 98765 43210").isValid)
        assertTrue(AuthValidators.validateIndianMobile("919876543210").isValid)

        // Invalid numbers
        assertFalse(AuthValidators.validateIndianMobile("").isValid)
        assertFalse(AuthValidators.validateIndianMobile(null).isValid)
        assertFalse(AuthValidators.validateIndianMobile("1234567890").isValid) // starts with 1
        assertFalse(AuthValidators.validateIndianMobile("5555555555").isValid) // starts with 5
        assertFalse(AuthValidators.validateIndianMobile("987654").isValid) // too short
        assertFalse(AuthValidators.validateIndianMobile("9876543210987").isValid) // too long
        assertFalse(AuthValidators.validateIndianMobile("abcdefghij").isValid) // letters

        // Canonical formatting
        assertEquals("+919876543210", AuthValidators.formatIndianMobile("9876543210"))
        assertEquals("+919876543210", AuthValidators.formatIndianMobile("+919876543210"))
        assertEquals("+919876543210", AuthValidators.formatIndianMobile("919876543210"))
        assertEquals("+919876543210", AuthValidators.formatIndianMobile("+91 98765 43210"))
    }

    @Test
    fun testPasswordAndConfirmPasswordMatching() {
        assertFalse(AuthValidators.validatePassword("123").isValid)
        assertTrue(AuthValidators.validatePassword("secret123").isValid)

        // Confirm password
        assertFalse(AuthValidators.validateConfirmPassword("secret123", "").isValid)
        assertFalse(AuthValidators.validateConfirmPassword("secret123", "secret456").isValid)
        assertTrue(AuthValidators.validateConfirmPassword("secret123", "secret123").isValid)
    }

    @Test
    fun testHealthcareDataConsentRequirement() {
        assertFalse(AuthValidators.validateConsent(false).isValid)
        assertTrue(AuthValidators.validateConsent(true).isValid)
    }

    @Test
    fun testDuplicateAccountAndErrorHandling() {
        val inUseError = AuthError.fromException(Exception("The email address is already in use by another account."))
        assertTrue(inUseError is AuthError.EmailAlreadyInUse)
        assertEquals("An account with this email already exists", inUseError.message)

        val weakPassError = AuthError.fromException(Exception("The given password is weak and at least 6 characters are required."))
        assertTrue(weakPassError is AuthError.WeakPassword)

        val networkError = AuthError.fromException(Exception("A network error occurred. Please verify host."))
        assertTrue(networkError is AuthError.NetworkError)
    }
}
