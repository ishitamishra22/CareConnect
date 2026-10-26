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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nmims.careconnect.data.auth.AuthValidators
import com.nmims.careconnect.data.model.UserRole
import com.nmims.careconnect.data.repository.AuthRepository
import com.nmims.careconnect.ui.common.CareTopBar
import com.nmims.careconnect.ui.common.PrimaryButton
import com.nmims.careconnect.ui.theme.CareConnectTheme
import com.nmims.careconnect.ui.theme.LocalElevatedSurface
import kotlinx.coroutines.launch

/**
 * Shared credential-based authentication screen configured per selected UserRole.
 * Dedicated strictly to authenticated sign-in.
 * Public account creation and self-registration are disallowed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    role: UserRole,
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit,
    authRepository: AuthRepository = AuthRepository.getInstance()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    var touched by rememberSaveable { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var generalError by remember { mutableStateOf<String?>(null) }

    val emailValidation = AuthValidators.validateEmail(email)
    val passwordValidation = AuthValidators.validatePassword(password)
    val isFormValid = emailValidation.isValid && passwordValidation.isValid

    val roleIcon = when (role) {
        UserRole.PATIENT -> Icons.Default.Person
        UserRole.DOCTOR -> Icons.Default.MedicalServices
        UserRole.HOSPITAL, UserRole.ADMIN -> Icons.Default.AdminPanelSettings
    }

    val roleSubtitle = when (role) {
        UserRole.PATIENT -> "Sign in with your registered email and password to access medical records and bookings"
        UserRole.DOCTOR -> "Sign in with your verified clinical credentials to access your doctor dashboard"
        UserRole.HOSPITAL, UserRole.ADMIN -> "Sign in with your authorized administrator credentials to manage hospital operations"
    }

    fun handleSignInSubmit() {
        touched = true
        generalError = null
        if (!isFormValid || isLoading) return

        focusManager.clearFocus()
        isLoading = true

        coroutineScope.launch {
            val result = authRepository.signInWithEmail(
                email = email.trim(),
                pass = password,
                expectedRole = role
            )

            isLoading = false
            result.fold(
                onSuccess = {
                    Toast.makeText(context, "Signed in successfully!", Toast.LENGTH_SHORT).show()
                    onLoginSuccess()
                },
                onFailure = { err ->
                    generalError = err.message ?: "Authentication failed. Please verify your credentials."
                }
            )
        }
    }

    Scaffold(
        topBar = {
            CareTopBar(
                title = "Login as ${role.label}",
                onBack = onBack
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Role Badge Surface
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = LocalElevatedSurface.current,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                modifier = Modifier.size(76.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = roleIcon,
                        contentDescription = role.label,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Welcome, ${role.label}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = roleSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Credential Form Container Card
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {

                    if (generalError != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = generalError.orEmpty(),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Email Address Input
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address *") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Email, contentDescription = null) },
                        isError = touched && !emailValidation.isValid,
                        supportingText = {
                            if (touched && !emailValidation.isValid) {
                                Text(emailValidation.errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Password Input
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password *") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        isError = touched && !passwordValidation.isValid,
                        supportingText = {
                            if (touched && !passwordValidation.isValid) {
                                Text(passwordValidation.errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { handleSignInSubmit() }),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Forgot Password Link
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                val emailVal = AuthValidators.validateEmail(email)
                                if (!emailVal.isValid) {
                                    Toast.makeText(context, "Enter your email address to reset password", Toast.LENGTH_SHORT).show()
                                } else {
                                    coroutineScope.launch {
                                        val res = authRepository.sendPasswordReset(email)
                                        res.fold(
                                            onSuccess = {
                                                Toast.makeText(context, "Password reset link sent to your email", Toast.LENGTH_LONG).show()
                                            },
                                            onFailure = {
                                                Toast.makeText(context, it.message ?: "Failed to send reset link", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                            }
                        ) {
                            Text(
                                text = "Forgot password?",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    PrimaryButton(
                        text = if (isLoading) "Signing in..." else "Sign In as ${role.label}",
                        onClick = { handleSignInSubmit() },
                        enabled = !isLoading
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Security Notice
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Role-verified Firebase Authentication",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    CareConnectTheme {
        LoginScreen(
            role = UserRole.DOCTOR,
            onLoginSuccess = {},
            onBack = {}
        )
    }
}
