package com.nmims.careconnect

import com.nmims.careconnect.data.auth.AuthValidators
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
}
