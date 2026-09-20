package com.nmims.careconnect.ui.auth

/*
 * LoginScreen:
 * Shared onboarding/login composable configured per selected UserRole.
 * Collects full name with student-friendly validation (not blank, letters & spaces only, min 3 chars).
 * Saves to AppSession and redirects to the respective home screen via resetTo.
 * Designed cleanly to allow future extension for doctor IDs / admin hospital selection.
 */

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nmims.careconnect.data.model.UserRole
import com.nmims.careconnect.session.AppSession
import com.nmims.careconnect.ui.common.CareTopBar
import com.nmims.careconnect.ui.common.PrimaryButton
import com.nmims.careconnect.ui.theme.CareConnectTheme

@Composable
fun LoginScreen(
    role: UserRole,
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var fullName by rememberSaveable { mutableStateOf("") }
    var touched by rememberSaveable { mutableStateOf(false) }

    // Validation rules: not blank, letters and spaces only, min 3 characters
    val trimmed = fullName.trim()
    val isLettersAndSpaces = trimmed.all { it.isLetter() || it.isWhitespace() }
    val isValid = trimmed.length >= 3 && isLettersAndSpaces

    val errorMessage = when {
        !touched -> null
        trimmed.isBlank() -> "Full name is required"
        trimmed.length < 3 -> "Name must be at least 3 characters long"
        !isLettersAndSpaces -> "Only letters and spaces are allowed"
        else -> null
    }

    val roleIcon = when (role) {
        UserRole.PATIENT -> Icons.Default.Person
        UserRole.DOCTOR -> Icons.Default.MedicalServices
        UserRole.HOSPITAL_ADMIN -> Icons.Default.AdminPanelSettings
    }

    val roleSubtitle = when (role) {
        UserRole.PATIENT -> "Enter your name to access your medical portfolio and bookings"
        UserRole.DOCTOR -> "Enter your name to access your appointments and consult notes"
        UserRole.HOSPITAL_ADMIN -> "Enter administrator name to access admissions and beds"
    }

    fun handleContinue() {
        touched = true
        if (isValid) {
            AppSession.currentRole = role
            AppSession.currentUserName = trimmed
            focusManager.clearFocus()
            onLoginSuccess()
        }
    }

    Scaffold(
        topBar = {
            CareTopBar(
                title = "Login as ${role.label}",
                onBack = onBack
            )
        }
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
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector = roleIcon,
                    contentDescription = role.label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Welcome, ${role.label}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = roleSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = fullName,
                onValueChange = {
                    fullName = it
                    touched = true
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Full Name") },
                placeholder = {
                    Text(if (role == UserRole.DOCTOR) "e.g. Dr. Priya Sharma" else "e.g. Rahul Verma")
                },
                isError = errorMessage != null,
                supportingText = {
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text("Letters and spaces only, min 3 characters")
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        handleContinue()
                    }
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            PrimaryButton(
                text = "Continue to Dashboard",
                onClick = { handleContinue() },
                enabled = isValid
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    CareConnectTheme {
        LoginScreen(
            role = UserRole.PATIENT,
            onLoginSuccess = {},
            onBack = {}
        )
    }
}
