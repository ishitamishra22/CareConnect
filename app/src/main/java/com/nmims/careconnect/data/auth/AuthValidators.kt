package com.nmims.careconnect.data.auth

/**
 * Reusable validation utility for authentication forms across all roles.
 */
object AuthValidators {

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val PHONE_REGEX = Regex("^[+]?[0-9]{10,13}$")
    private val INDIAN_MOBILE_REGEX = Regex("^(?:\\+91|91)?[6-9]\\d{9}$")

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

    fun validateConfirmPassword(password: String?, confirmPassword: String?): ValidationResult {
        val pass = password.orEmpty()
        val confirm = confirmPassword.orEmpty()
        return when {
            confirm.isBlank() -> ValidationResult(false, "Please confirm your password")
            pass != confirm -> ValidationResult(false, "Passwords do not match")
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

    fun validateIndianMobile(phone: String?): ValidationResult {
        val cleaned = phone?.trim()?.replace(" ", "")?.replace("-", "").orEmpty()
        return when {
            cleaned.isBlank() -> ValidationResult(false, "Mobile number is required")
            !INDIAN_MOBILE_REGEX.matches(cleaned) ->
                ValidationResult(false, "Enter a valid 10-digit Indian mobile number starting with 6-9")
            else -> ValidationResult(true)
        }
    }

    fun formatIndianMobile(phone: String): String {
        val cleaned = phone.trim().replace(" ", "").replace("-", "")
        return when {
            cleaned.startsWith("+91") -> cleaned
            cleaned.startsWith("91") && cleaned.length == 12 -> "+$cleaned"
            cleaned.length == 10 -> "+91$cleaned"
            else -> cleaned
        }
    }

    fun validateConsent(agreed: Boolean): ValidationResult {
        return if (!agreed) {
            ValidationResult(false, "You must agree to the Terms of Service & Privacy Policy")
        } else {
            ValidationResult(true)
        }
    }
}
