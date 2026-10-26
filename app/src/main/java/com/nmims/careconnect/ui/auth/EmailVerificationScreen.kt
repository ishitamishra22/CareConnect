package com.nmims.careconnect.ui.auth

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nmims.careconnect.data.repository.AuthRepository
import com.nmims.careconnect.ui.common.CareTopBar
import com.nmims.careconnect.ui.common.PrimaryButton
import com.nmims.careconnect.ui.theme.CareConnectTheme
import com.nmims.careconnect.ui.theme.LocalElevatedSurface
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Dedicated Email Verification Screen.
 * Allows users to review sent verification emails, check verification status,
 * resend links with a cooldown timer, or proceed to the patient dashboard.
 */
@Composable
fun EmailVerificationScreen(
    onVerificationSuccess: () -> Unit,
    onContinueToDashboard: () -> Unit,
    onSignOut: () -> Unit,
    authRepository: AuthRepository = AuthRepository.getInstance()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val userEmail = authRepository.currentUserEmail ?: "your email address"

    var isChecking by remember { mutableStateOf(false) }
    var isResending by remember { mutableStateOf(false) }
    var resendCooldown by remember { mutableIntStateOf(0) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    // Cooldown countdown timer
    LaunchedEffect(resendCooldown) {
        if (resendCooldown > 0) {
            delay(1000L)
            resendCooldown -= 1
        }
    }

    Scaffold(
        topBar = {
            CareTopBar(
                title = "Email Verification",
                onBack = onContinueToDashboard
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                shape = RoundedCornerShape(26.dp),
                color = LocalElevatedSurface.current,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.MarkEmailRead,
                        contentDescription = "Email Verification",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Verify Your Email Address",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "We sent a verification link to:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = userEmail,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {

                    if (statusMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = statusMessage.orEmpty(),
                                color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    Text(
                        text = "Click the link in the email to activate full account recovery and security features.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Check Verification Button
                    PrimaryButton(
                        text = if (isChecking) "Checking Status..." else "I've Verified My Email",
                        onClick = {
                            isChecking = true
                            statusMessage = null
                            coroutineScope.launch {
                                val result = authRepository.checkEmailVerification()
                                isChecking = false
                                result.fold(
                                    onSuccess = { isVerified ->
                                        if (isVerified) {
                                            isError = false
                                            statusMessage = "Email verified successfully!"
                                            Toast.makeText(context, "Email verified!", Toast.LENGTH_SHORT).show()
                                            onVerificationSuccess()
                                        } else {
                                            isError = true
                                            statusMessage = "Email is not verified yet. Please click the link in your inbox."
                                        }
                                    },
                                    onFailure = { err ->
                                        isError = true
                                        statusMessage = err.message ?: "Could not check verification status."
                                    }
                                )
                            }
                        },
                        enabled = !isChecking
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Resend Email Button
                    OutlinedButton(
                        onClick = {
                            if (resendCooldown == 0 && !isResending) {
                                isResending = true
                                statusMessage = null
                                coroutineScope.launch {
                                    val res = authRepository.sendEmailVerification()
                                    isResending = false
                                    res.fold(
                                        onSuccess = {
                                            isError = false
                                            statusMessage = "New verification link sent to $userEmail"
                                            resendCooldown = 30
                                        },
                                        onFailure = { err ->
                                            isError = true
                                            statusMessage = err.message ?: "Failed to resend verification email."
                                        }
                                    )
                                }
                            }
                        },
                        enabled = resendCooldown == 0 && !isResending,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isResending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = if (resendCooldown > 0) "Resend in ${resendCooldown}s" else "Resend Verification Email"
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Continue to Dashboard
                    TextButton(
                        onClick = onContinueToDashboard,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Continue to Patient Dashboard",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            TextButton(onClick = onSignOut) {
                Text(
                    text = "Sign out and use another account",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EmailVerificationScreenPreview() {
    CareConnectTheme {
        EmailVerificationScreen(
            onVerificationSuccess = {},
            onContinueToDashboard = {},
            onSignOut = {}
        )
    }
}
