package com.nmims.careconnect.data.auth

/**
 * Standardized authentication errors for CareConnect.
 */
sealed class AuthError(val message: String) {
    data class InvalidEmail(val msg: String = "Please enter a valid email address") : AuthError(msg)
    data class WeakPassword(val msg: String = "Password must be at least 6 characters long and include numbers or special characters") : AuthError(msg)
    data class UserNotFound(val msg: String = "No account found with this email address") : AuthError(msg)
    data class WrongPassword(val msg: String = "Incorrect password. Please try again") : AuthError(msg)
    data class EmailAlreadyInUse(val msg: String = "An account with this email already exists") : AuthError(msg)
    data class UserDisabled(val msg: String = "This user account has been disabled") : AuthError(msg)
    data class TooManyRequests(val msg: String = "Too many attempts. Please try again later") : AuthError(msg)
    data class NetworkError(val msg: String = "Network error. Please check your internet connection") : AuthError(msg)
    data class UnauthorizedRole(val msg: String = "You are not authorized to access this role") : AuthError(msg)
    data class AccountPendingApproval(val msg: String = "Your account is pending administrator approval") : AuthError(msg)
    data class Unknown(val msg: String = "An unexpected authentication error occurred") : AuthError(msg)

    companion object {
        fun fromException(exception: Throwable): AuthError {
            val message = exception.message.orEmpty()
            return when {
                message.contains("ERROR_INVALID_EMAIL", ignoreCase = true) ||
                message.contains("badly formatted", ignoreCase = true) -> InvalidEmail()

                message.contains("ERROR_WRONG_PASSWORD", ignoreCase = true) ||
                message.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ||
                message.contains("invalid credential", ignoreCase = true) -> WrongPassword()

                message.contains("ERROR_USER_NOT_FOUND", ignoreCase = true) ||
                message.contains("no user record", ignoreCase = true) -> UserNotFound()

                message.contains("ERROR_EMAIL_ALREADY_IN_USE", ignoreCase = true) ||
                message.contains("email address is already in use", ignoreCase = true) -> EmailAlreadyInUse()

                message.contains("ERROR_WEAK_PASSWORD", ignoreCase = true) ||
                message.contains("at least 6 characters", ignoreCase = true) -> WeakPassword()

                message.contains("ERROR_USER_DISABLED", ignoreCase = true) -> UserDisabled()

                message.contains("ERROR_TOO_MANY_REQUESTS", ignoreCase = true) ||
                message.contains("unusual activity", ignoreCase = true) -> TooManyRequests()

                message.contains("network error", ignoreCase = true) ||
                message.contains("unable to resolve host", ignoreCase = true) -> NetworkError()

                else -> Unknown(exception.localizedMessage ?: "Authentication failed")
            }
        }
    }
}
