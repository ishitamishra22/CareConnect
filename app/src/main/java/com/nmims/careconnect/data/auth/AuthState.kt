package com.nmims.careconnect.data.auth

import com.nmims.careconnect.data.model.UserProfile

/**
 * Observable authentication state representing the user's current session.
 * Contract: Loading, SignedOut, SignedIn, Blocked.
 */
sealed class AuthState {
    /** Session restoration or credential authentication is in progress */
    data object Loading : AuthState()

    /** No authenticated user session exists */
    data object SignedOut : AuthState()

    /** User is successfully authenticated with an active profile */
    data class SignedIn(val user: UserProfile) : AuthState()

    /** User account is disabled, pending approval, or blocked */
    data class Blocked(val reason: String = "Your account is currently suspended or inactive. Please contact an administrator.") : AuthState()

    // Backward compatibility aliases
    val isAuthenticated: Boolean get() = this is SignedIn
}
