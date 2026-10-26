package com.nmims.careconnect.ui.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nmims.careconnect.data.auth.AuthState
import com.nmims.careconnect.data.model.UserRole
import com.nmims.careconnect.data.repository.AuthRepository
import com.nmims.careconnect.ui.common.LoadingIndicator

/**
 * Reusable Jetpack Compose Security Gate.
 * Enforces authenticated sessions and verifies user roles before granting access to protected routes.
 * Handles Loading, SignedOut, SignedIn, and Blocked states.
 *
 * @param authRepository Shared authentication repository instance.
 * @param requiredRoles Optional set of allowed roles (e.g. Set(UserRole.PATIENT)). If empty, any authenticated user is allowed.
 * @param onNavigateToLogin Callback invoked when the user is unauthenticated or needs to log in.
 * @param onRoleMismatch Callback optionally invoked if the authenticated user does not have permission for the target route.
 * @param content The protected composable content to render when authorization succeeds.
 */
@Composable
fun AuthGate(
    authRepository: AuthRepository = AuthRepository.getInstance(),
    requiredRoles: Set<UserRole> = emptySet(),
    onNavigateToLogin: () -> Unit,
    onRoleMismatch: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val authState by authRepository.authState.collectAsState()

    when (val state = authState) {
        is AuthState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LoadingIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Verifying security session...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        is AuthState.SignedOut -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Authentication Required",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Please sign in to access this protected healthcare area.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(onClick = onNavigateToLogin) {
                        Text("Sign In")
                    }
                }
            }
        }
        is AuthState.Blocked -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Account Inactive",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(onClick = onNavigateToLogin) {
                        Text("Return to Sign In")
                    }
                }
            }
        }
        is AuthState.SignedIn -> {
            val userProfile = state.user
            if (!userProfile.active) {
                // Inactive user fail-closed guard
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Access Blocked",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your account is not active. Please contact support or an administrator.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(onClick = onNavigateToLogin) {
                            Text("Return to Sign In")
                        }
                    }
                }
                return
            }

            val userRole = userProfile.role
            val isAuthorized = requiredRoles.isEmpty() ||
                    requiredRoles.contains(userRole) ||
                    userRole == UserRole.ADMIN

            if (isAuthorized) {
                content()
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Access Restricted",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your account (${userRole.label}) does not have permission to view this section.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        OutlinedButton(
                            onClick = {
                                if (onRoleMismatch != null) {
                                    onRoleMismatch()
                                } else {
                                    authRepository.signOut()
                                    onNavigateToLogin()
                                }
                            }
                        ) {
                            Text("Switch Account")
                        }
                    }
                }
            }
        }
    }
}
