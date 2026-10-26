package com.nmims.careconnect

import com.nmims.careconnect.data.auth.AuthValidators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidatorsTest {

    @Test
    fun testValidEmails() {
        assertTrue(AuthValidators.validateEmail("user@example.com").isValid)
        assertTrue(AuthValidators.validateEmail("doctor.smith@careconnect.org").isValid)
        assertTrue(AuthValidators.validateEmail("admin+test@hospital.in").isValid)
    }

    @Test
    fun testInvalidEmails() {
        assertFalse(AuthValidators.validateEmail("").isValid)
        assertFalse(AuthValidators.validateEmail(null).isValid)
        assertFalse(AuthValidators.validateEmail("invalid-email").isValid)
        assertFalse(AuthValidators.validateEmail("missing@domain").isValid)
        assertFalse(AuthValidators.validateEmail("@nodomain.com").isValid)
    }

    @Test
    fun testPasswordValidation() {
        assertFalse(AuthValidators.validatePassword("").isValid)
        assertFalse(AuthValidators.validatePassword(null).isValid)
        assertFalse(AuthValidators.validatePassword("12345").isValid) // < 6 chars
        assertTrue(AuthValidators.validatePassword("123456").isValid)
        assertTrue(AuthValidators.validatePassword("StrongPass#2026").isValid)
    }

    @Test
    fun testConfirmPasswordValidation() {
        assertFalse(AuthValidators.validateConfirmPassword("password123", "").isValid)
        assertFalse(AuthValidators.validateConfirmPassword("password123", "mismatch").isValid)
        assertTrue(AuthValidators.validateConfirmPassword("password123", "password123").isValid)
    }

    @Test
    fun testNameValidation() {
        assertFalse(AuthValidators.validateName("").isValid)
        assertFalse(AuthValidators.validateName("A").isValid)
        assertTrue(AuthValidators.validateName("Dr. Suresh Kulkarni").isValid)
        assertTrue(AuthValidators.validateName("Rahul Verma").isValid)
        assertTrue(AuthValidators.validateName("Mary-Jane Watson").isValid)
        assertFalse(AuthValidators.validateName("Invalid123!").isValid)
    }

    @Test
    fun testPhoneValidation() {
        assertTrue(AuthValidators.validatePhone("").isValid) // Optional
        assertTrue(AuthValidators.validatePhone(null).isValid) // Optional
        assertTrue(AuthValidators.validatePhone("9876543210").isValid)
        assertTrue(AuthValidators.validatePhone("+919876543210").isValid)
        assertFalse(AuthValidators.validatePhone("123").isValid)
        assertFalse(AuthValidators.validatePhone("invalid-phone").isValid)
    }

    @Test
    fun testIndianMobileValidation() {
        assertFalse(AuthValidators.validateIndianMobile("").isValid)
        assertFalse(AuthValidators.validateIndianMobile(null).isValid)
        assertFalse(AuthValidators.validateIndianMobile("1234567890").isValid) // doesn't start with 6-9
        assertFalse(AuthValidators.validateIndianMobile("98765").isValid) // too short
        assertFalse(AuthValidators.validateIndianMobile("9876543210123").isValid) // too long
        assertTrue(AuthValidators.validateIndianMobile("9876543210").isValid)
        assertTrue(AuthValidators.validateIndianMobile("8123456789").isValid)
        assertTrue(AuthValidators.validateIndianMobile("7000000000").isValid)
        assertTrue(AuthValidators.validateIndianMobile("6999999999").isValid)
        assertTrue(AuthValidators.validateIndianMobile("+919876543210").isValid)
        assertTrue(AuthValidators.validateIndianMobile("+91 98765 43210").isValid)
        assertTrue(AuthValidators.validateIndianMobile("919876543210").isValid)

        assertEquals("+919876543210", AuthValidators.formatIndianMobile("9876543210"))
        assertEquals("+919876543210", AuthValidators.formatIndianMobile("+919876543210"))
        assertEquals("+919876543210", AuthValidators.formatIndianMobile("919876543210"))
        assertEquals("+919876543210", AuthValidators.formatIndianMobile("+91 98765 43210"))
    }

    @Test
    fun testConsentValidation() {
        assertFalse(AuthValidators.validateConsent(false).isValid)
        assertTrue(AuthValidators.validateConsent(true).isValid)
    }
}
