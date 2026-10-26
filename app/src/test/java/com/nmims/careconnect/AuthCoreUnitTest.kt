package com.nmims.careconnect

import com.google.firebase.Timestamp
import com.nmims.careconnect.data.auth.AuthError
import com.nmims.careconnect.data.auth.AuthState
import com.nmims.careconnect.data.model.UserProfile
import com.nmims.careconnect.data.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthCoreUnitTest {

    @Test
    fun testUserRoleMigrationCompatibility() {
        assertEquals(UserRole.HOSPITAL, UserRole.HOSPITAL_ADMIN)
        assertEquals(UserRole.ADMIN, UserRole.fromString("ADMIN"))
        assertEquals(UserRole.PATIENT, UserRole.fromString("PATIENT"))
        assertEquals(UserRole.DOCTOR, UserRole.fromString("DOCTOR"))
        assertEquals(UserRole.HOSPITAL, UserRole.fromString("HOSPITAL"))
        assertEquals(UserRole.HOSPITAL, UserRole.fromString("HOSPITAL_ADMIN"))
    }

    @Test
    fun testUserProfileContractFieldsAndLegacyCompatibility() {
        val now = Timestamp.now()
        val profile = UserProfile(
            uid = "user-12345",
            role = UserRole.DOCTOR,
            email = "dr.smith@careconnect.org",
            phone = "+919876543210",
            displayName = "Dr. Suresh Kulkarni",
            active = true,
            linkedDoctorId = "doc_01",
            linkedHospitalId = "hosp_01",
            createdAt = now,
            createdBy = "admin-uid",
            lastLoginAt = now
        )

        val map = profile.toMap()
        assertEquals("user-12345", map["uid"])
        assertEquals("DOCTOR", map["role"])
        assertEquals("+919876543210", map["phone"])
        assertEquals("Dr. Suresh Kulkarni", map["displayName"])
        assertEquals(true, map["active"])
        assertEquals("doc_01", map["linkedDoctorId"])
        assertEquals("hosp_01", map["linkedHospitalId"])
        assertEquals("admin-uid", map["createdBy"])

        // Test backward compatibility getters
        assertEquals("doc_01", profile.doctorId)
        assertEquals("hosp_01", profile.hospitalId)
        assertEquals("+919876543210", profile.phoneNumber)
        assertEquals(true, profile.isApproved)

        // Test deserialization from map
        val restored = UserProfile.fromMap("user-12345", map)
        assertEquals(profile.uid, restored.uid)
        assertEquals(profile.role, restored.role)
        assertEquals(profile.phone, restored.phone)
        assertEquals(profile.linkedDoctorId, restored.linkedDoctorId)
        assertEquals(profile.linkedHospitalId, restored.linkedHospitalId)
        assertEquals(profile.createdBy, restored.createdBy)
        assertEquals(profile.active, restored.active)
    }

    @Test
    fun testInactiveUserStatus() {
        val blockedProfile = UserProfile(
            uid = "user-blocked",
            role = UserRole.PATIENT,
            email = "blocked@example.com",
            displayName = "Suspended User",
            active = false
        )

        assertFalse(blockedProfile.active)
        assertFalse(blockedProfile.isApproved)

        val blockedState = AuthState.Blocked("Account suspended")
        assertFalse(blockedState.isAuthenticated)
        assertEquals("Account suspended", blockedState.reason)
    }

    @Test
    fun testAuthErrorMapping() {
        val invalidEmailEx = Exception("The email address is badly formatted.")
        val error1 = AuthError.fromException(invalidEmailEx)
        assertTrue(error1 is AuthError.InvalidEmail)

        val wrongPassEx = Exception("INVALID_LOGIN_CREDENTIALS")
        val error2 = AuthError.fromException(wrongPassEx)
        assertTrue(error2 is AuthError.WrongPassword)

        val inUseEx = Exception("The email address is already in use by another account.")
        val error3 = AuthError.fromException(inUseEx)
        assertTrue(error3 is AuthError.EmailAlreadyInUse)
    }

    @Test
    fun testAuthStateContract() {
        val loading: AuthState = AuthState.Loading
        val signedOut: AuthState = AuthState.SignedOut
        val blocked: AuthState = AuthState.Blocked()
        val signedIn: AuthState = AuthState.SignedIn(
            UserProfile(uid = "uid1", role = UserRole.ADMIN, email = "admin@nmims.edu")
        )

        assertNotNull(loading)
        assertNotNull(signedOut)
        assertNotNull(blocked)
        assertTrue(signedIn is AuthState.SignedIn)
        assertEquals(UserRole.ADMIN, (signedIn as AuthState.SignedIn).user.role)
        assertTrue(signedIn.isAuthenticated)
        assertFalse(signedOut.isAuthenticated)
        assertFalse(blocked.isAuthenticated)
    }
}
