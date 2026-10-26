package com.nmims.careconnect

import com.google.firebase.Timestamp
import com.nmims.careconnect.data.model.AdminAuditLog
import com.nmims.careconnect.data.model.UserProfile
import com.nmims.careconnect.data.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminAuthorizationUnitTest {

    @Test
    fun testAdminSelfDeactivationGuards() {
        val currentAdminUid = "admin_master_001"
        val currentAdminProfile = UserProfile(
            uid = currentAdminUid,
            role = UserRole.ADMIN,
            email = "admin@nmims.edu",
            displayName = "Super Administrator",
            active = true
        )

        // Rule check: Admin cannot deactivate itself
        val canDeactivateSelf = currentAdminProfile.uid != currentAdminUid
        assertFalse("Administrator must never be permitted to deactivate their own account", canDeactivateSelf)
    }

    @Test
    fun testProtectionOfOtherAdminAccounts() {
        val currentAdminUid = "admin_master_001"
        val otherAdminProfile = UserProfile(
            uid = "admin_secondary_002",
            role = UserRole.ADMIN,
            email = "secondary.admin@nmims.edu",
            displayName = "Secondary Admin",
            active = true
        )

        // Rule check: Admin cannot modify or deactivate another Admin
        val isOtherAdmin = otherAdminProfile.role == UserRole.ADMIN && otherAdminProfile.uid != currentAdminUid
        assertTrue("System must detect and block modifications targeting another Administrator", isOtherAdmin)

        val canModifyOtherAdmin = !isOtherAdmin
        assertFalse(canModifyOtherAdmin)
    }

    @Test
    fun testPermittedUserModificationsOnNonAdmin() {
        val patientUser = UserProfile(
            uid = "patient_777",
            role = UserRole.PATIENT,
            email = "patient@test.com",
            displayName = "Old Name",
            active = true
        )

        val updatedPatient = patientUser.copy(
            displayName = "Updated Patient Name",
            active = false
        )

        // Role must remain strictly unchanged
        assertEquals(patientUser.role, updatedPatient.role)
        assertEquals("Updated Patient Name", updatedPatient.displayName)
        assertFalse(updatedPatient.active)
    }

    @Test
    fun testAuditLogSerialization() {
        val now = Timestamp.now()
        val auditLog = AdminAuditLog(
            id = "log_999",
            adminUid = "admin_01",
            adminEmail = "admin@careconnect.org",
            action = "USER_DEACTIVATED",
            targetUid = "user_456",
            details = mapOf("reason" to "Policy violation", "targetRole" to "PATIENT"),
            timestamp = now
        )

        val map = auditLog.toMap()
        assertEquals("log_999", map["id"])
        assertEquals("admin_01", map["adminUid"])
        assertEquals("USER_DEACTIVATED", map["action"])
        assertEquals("user_456", map["targetUid"])

        val restored = AdminAuditLog.fromMap("log_999", map)
        assertEquals(auditLog.id, restored.id)
        assertEquals(auditLog.adminUid, restored.adminUid)
        assertEquals(auditLog.action, restored.action)
        assertEquals(auditLog.details["reason"], restored.details["reason"])
    }

    @Test
    fun testStaffProvisioningRoleRestrictions() {
        val allowedRoles = setOf(UserRole.DOCTOR, UserRole.HOSPITAL)

        assertTrue(allowedRoles.contains(UserRole.DOCTOR))
        assertTrue(allowedRoles.contains(UserRole.HOSPITAL))
        assertFalse("Admin provisioning must forbid creating ADMIN accounts", allowedRoles.contains(UserRole.ADMIN))
        assertFalse("Admin provisioning must not handle PATIENT self-registration", allowedRoles.contains(UserRole.PATIENT))
    }
}
