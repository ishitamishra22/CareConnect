package com.nmims.careconnect.ui.admin

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
import androidx.compose.ui.unit.dp
import com.nmims.careconnect.data.auth.AuthValidators
import com.nmims.careconnect.data.model.UserRole
import com.nmims.careconnect.data.repository.AuthRepository
import com.nmims.careconnect.ui.common.CareTopBar
import com.nmims.careconnect.ui.common.PrimaryButton
import com.nmims.careconnect.ui.theme.LocalElevatedSurface
import kotlinx.coroutines.launch

/**
 * System Administrator Login Screen.
 * Dedicated strictly to credential authentication with role UserRole.ADMIN.
 * No public Admin sign-up or registration is available.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemAdminLoginScreen(
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

    fun handleAdminLogin() {
        touched = true
        generalError = null
        if (!isFormValid || isLoading) return

        focusManager.clearFocus()
        isLoading = true

        coroutineScope.launch {
            val result = authRepository.signInWithEmail(
                email = email.trim(),
                pass = password,
                expectedRole = UserRole.ADMIN
            )

            isLoading = false
            result.fold(
                onSuccess = { profile ->
                    if (profile.role == UserRole.ADMIN) {
                        Toast.makeText(context, "Welcome, System Administrator", Toast.LENGTH_SHORT).show()
                        onLoginSuccess()
                    } else {
                        authRepository.signOut()
                        generalError = "Unauthorized: Only system administrators may log in through this portal."
                    }
                },
                onFailure = {
                    generalError = "Invalid administrator credentials or unauthorized account."
                }
            )
        }
    }

    Scaffold(
        topBar = {
            CareTopBar(
                title = "System Administrator Portal",
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
            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = LocalElevatedSurface.current,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = "System Admin",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "System Administration",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Secure portal for system administration, catalogue maintenance, and staff provisioning",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
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

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Admin Email *") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Email, contentDescription = null) },
                        isError = touched && !emailValidation.isValid,
                        supportingText = {
                            if (touched && !emailValidation.isValid) {
                                Text(emailValidation.errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Admin Password *") },
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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { handleAdminLogin() }),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                val emailVal = AuthValidators.validateEmail(email)
                                if (!emailVal.isValid) {
                                    Toast.makeText(context, "Enter your administrator email to reset password", Toast.LENGTH_SHORT).show()
                                } else {
                                    coroutineScope.launch {
                                        authRepository.sendPasswordReset(email).fold(
                                            onSuccess = {
                                                Toast.makeText(context, "Password reset link sent to your admin email", Toast.LENGTH_LONG).show()
                                            },
                                            onFailure = {
                                                Toast.makeText(context, "Failed to send reset link", Toast.LENGTH_SHORT).show()
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
                        text = if (isLoading) "Authenticating..." else "Sign In as System Admin",
                        onClick = { handleAdminLogin() },
                        enabled = !isLoading
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

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
                    text = "Requires privileged console-provisioned administrator account",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
