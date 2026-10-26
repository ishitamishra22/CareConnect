package com.nmims.careconnect.data.auth

/**
 * Reusable validation utility for authentication forms across all roles.
 */
object AuthValidators {

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val PHONE_REGEX = Regex("^[+]?[0-9]{10,13}$")

    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null
    )

    fun validateEmail(email: String?): ValidationResult {
        val trimmed = email?.trim().orEmpty()
        return when {
            trimmed.isBlank() -> ValidationResult(false, "Email address is required")
            !EMAIL_REGEX.matches(trimmed) -> ValidationResult(false, "Please enter a valid email address")
            else -> ValidationResult(true)
        }
    }

    fun validatePassword(password: String?): ValidationResult {
        val pass = password.orEmpty()
        return when {
            pass.isBlank() -> ValidationResult(false, "Password is required")
            pass.length < 6 -> ValidationResult(false, "Password must be at least 6 characters")
            else -> ValidationResult(true)
        }
    }

    fun validateName(name: String?): ValidationResult {
        val trimmed = name?.trim().orEmpty()
        return when {
            trimmed.isBlank() -> ValidationResult(false, "Full name is required")
            trimmed.length < 2 -> ValidationResult(false, "Name must be at least 2 characters")
            !trimmed.all { it.isLetter() || it.isWhitespace() || it == '.' || it == '-' } ->
                ValidationResult(false, "Name contains invalid characters")
            else -> ValidationResult(true)
        }
    }

    fun validatePhone(phone: String?): ValidationResult {
        val trimmed = phone?.trim().orEmpty()
        if (trimmed.isBlank()) return ValidationResult(true) // optional
        return if (!PHONE_REGEX.matches(trimmed)) {
            ValidationResult(false, "Please enter a valid phone number (10-13 digits)")
        } else {
            ValidationResult(true)
        }
    }
}
