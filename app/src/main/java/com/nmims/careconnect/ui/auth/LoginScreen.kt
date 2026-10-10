package com.nmims.careconnect.ui.auth

/*
 * LoginScreen:
 * Shared onboarding/login composable configured per selected UserRole.
 * - For PATIENT and HOSPITAL_ADMIN: Collects full name with student-friendly validation.
 * - For DOCTOR: Displays searchable/selectable doctor identity picker from the catalogue,
 *   populating AppSession.currentDoctorId and AppSession.currentHospitalId upon selection.
 * Clears and resets session properly and preserves existing Material 3 styling.
 */

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.nmims.careconnect.data.model.Doctor
import com.nmims.careconnect.data.model.UserRole
import com.nmims.careconnect.data.repository.HospitalRepository
import com.nmims.careconnect.session.AppSession
import com.nmims.careconnect.ui.common.CareTopBar
import com.nmims.careconnect.ui.common.PrimaryButton
import com.nmims.careconnect.ui.common.SectionCard
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

    // Doctor selection state
    val allDoctors = remember { HospitalRepository.getDoctors() }
    var selectedDoctor by remember { mutableStateOf<Doctor?>(null) }
    var doctorDropdownExpanded by remember { mutableStateOf(false) }

    // Validation rules:
    // For PATIENT/ADMIN: not blank, letters & spaces only, min 3 chars
    // For DOCTOR: doctor selected from catalogue
    val trimmed = fullName.trim()
    val isLettersAndSpaces = trimmed.all { it.isLetter() || it.isWhitespace() }
    val isNameValid = trimmed.length >= 3 && isLettersAndSpaces
    val isValid = if (role == UserRole.DOCTOR) {
        selectedDoctor != null
    } else {
        isNameValid
    }

    val errorMessage = when {
        role == UserRole.DOCTOR -> {
            if (touched && selectedDoctor == null) "Please select your doctor identity" else null
        }
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
        UserRole.DOCTOR -> "Select your verified doctor profile from the hospital directory"
        UserRole.HOSPITAL_ADMIN -> "Enter administrator name to access admissions and beds"
    }

    fun handleContinue() {
        touched = true
        if (role == UserRole.DOCTOR) {
            val doc = selectedDoctor ?: return
            AppSession.currentRole = UserRole.DOCTOR
            AppSession.currentUserName = doc.name
            AppSession.currentDoctorId = doc.id
            AppSession.currentHospitalId = doc.hospitalId
            focusManager.clearFocus()
            onLoginSuccess()
        } else if (isValid) {
            AppSession.currentRole = role
            AppSession.currentUserName = trimmed
            if (role == UserRole.PATIENT) {
                AppSession.currentPatientId = trimmed.lowercase().replace(Regex("\\s+"), "_")
            }
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

            if (role == UserRole.DOCTOR) {
                // Doctor Identity Picker
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedDoctor?.let { "Dr. ${it.name} (${it.specialty})" } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                touched = true
                                doctorDropdownExpanded = true
                            },
                        label = { Text("Select Doctor Profile") },
                        placeholder = { Text("Tap to choose your doctor profile") },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown"
                            )
                        },
                        isError = errorMessage != null,
                        supportingText = {
                            if (errorMessage != null) {
                                Text(
                                    text = errorMessage,
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else {
                                Text("Select your identity from the catalogue")
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        enabled = true
                    )

                    // Overlay invisible click catcher to ensure tap opens dropdown
                    Surface(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(bottom = 20.dp)
                            .clickable {
                                touched = true
                                doctorDropdownExpanded = true
                            },
                        color = androidx.compose.ui.graphics.Color.Transparent
                    ) {}

                    DropdownMenu(
                        expanded = doctorDropdownExpanded,
                        onDismissRequest = { doctorDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        allDoctors.forEach { doctor ->
                            val hospital = HospitalRepository.getHospital(doctor.hospitalId)
                            val isSelected = selectedDoctor?.id == doctor.id
                            DropdownMenuItem(
                                text = {
                                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Dr. ${doctor.name}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${doctor.specialty} • ${hospital?.name ?: ""}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    selectedDoctor = doctor
                                    doctorDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                if (selectedDoctor != null) {
                    val doctor = selectedDoctor!!
                    val hospital = HospitalRepository.getHospital(doctor.hospitalId)
                    Spacer(modifier = Modifier.height(14.dp))
                    SectionCard(title = "Selected Profile") {
                        Text(
                            text = "Dr. ${doctor.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${doctor.specialty} • ${doctor.qualifications}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = hospital?.name ?: doctor.hospitalId,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = {
                        fullName = it
                        touched = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Full Name") },
                    placeholder = {
                        Text("e.g. Rahul Verma")
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
            }

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
            role = UserRole.DOCTOR,
            onLoginSuccess = {},
            onBack = {}
        )
    }
}
