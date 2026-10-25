package com.nmims.careconnect.ui.auth

/*
 * LoginScreen:
 * Shared onboarding/login composable configured per selected UserRole.
 * - For PATIENT and HOSPITAL_ADMIN: Collects full name with student-friendly validation.
 * - For DOCTOR: Displays searchable/selectable doctor identity picker from the catalogue,
 *   populating AppSession.currentDoctorId and AppSession.currentHospitalId upon selection.
 * Clears and resets session properly and preserves existing Material 3 styling.
 */

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.nmims.careconnect.data.model.Hospital
import com.nmims.careconnect.data.model.UserRole
import com.nmims.careconnect.data.repository.HospitalRepository
import com.nmims.careconnect.session.AppSession
import com.nmims.careconnect.ui.common.CareTopBar
import com.nmims.careconnect.ui.common.PrimaryButton
import com.nmims.careconnect.ui.common.SectionCard
import com.nmims.careconnect.ui.theme.CareConnectTheme
import com.nmims.careconnect.ui.theme.LocalElevatedSurface

@OptIn(ExperimentalMaterial3Api::class)
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

    // Hospital Admin selection state
    val allHospitals = remember { HospitalRepository.getHospitals() }
    var selectedHospital by remember { mutableStateOf<Hospital?>(allHospitals.firstOrNull()) }
    var hospitalDropdownExpanded by remember { mutableStateOf(false) }

    // Validation rules:
    // For PATIENT: not blank, letters & spaces only, min 3 chars
    // For DOCTOR: doctor selected from catalogue
    // For HOSPITAL_ADMIN: name valid and hospital selected
    val trimmed = fullName.trim()
    val isLettersAndSpaces = trimmed.all { it.isLetter() || it.isWhitespace() }
    val isNameValid = trimmed.length >= 3 && isLettersAndSpaces
    val isValid = when (role) {
        UserRole.DOCTOR -> selectedDoctor != null
        UserRole.HOSPITAL_ADMIN -> isNameValid && selectedHospital != null
        UserRole.PATIENT -> isNameValid
    }

    val errorMessage = when {
        role == UserRole.DOCTOR -> {
            if (touched && selectedDoctor == null) "Please select your doctor identity" else null
        }
        role == UserRole.HOSPITAL_ADMIN && touched && selectedHospital == null -> {
            "Please select a hospital to administer"
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
        UserRole.HOSPITAL_ADMIN -> "Select your hospital and enter admin name to access dashboard"
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
        } else if (role == UserRole.HOSPITAL_ADMIN) {
            val hosp = selectedHospital ?: return
            if (isNameValid) {
                AppSession.currentRole = UserRole.HOSPITAL_ADMIN
                AppSession.currentUserName = trimmed
                AppSession.currentHospitalId = hosp.id
                focusManager.clearFocus()
                onLoginSuccess()
            }
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
            Spacer(modifier = Modifier.height(16.dp))

            // Role Badge with Elevated Surface
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
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Credential Form Container Card
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    if (role == UserRole.DOCTOR) {
                        // Doctor Identity Picker
                        Text(
                            text = "Doctor Identity",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Choose your verified credential from the roster",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

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
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.MedicalServices,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
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
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                ),
                                enabled = true
                            )

                            // Overlay invisible click catcher to ensure tap opens dropdown reliably
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
                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = LocalElevatedSurface.current,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Selected Profile",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Dr. ${doctor.name}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${doctor.specialty} • ${doctor.qualifications}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = hospital?.name ?: doctor.hospitalId,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        if (role == UserRole.HOSPITAL_ADMIN) {
                            Text(
                                text = "Hospital Administration",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Select hospital branch and enter your name",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            ExposedDropdownMenuBox(
                                expanded = hospitalDropdownExpanded,
                                onExpandedChange = { hospitalDropdownExpanded = it },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = selectedHospital?.name ?: "Select Hospital",
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                    label = { Text("Select Managed Hospital *") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.LocalHospital,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = hospitalDropdownExpanded)
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    )
                                )
                                ExposedDropdownMenu(
                                    expanded = hospitalDropdownExpanded,
                                    onDismissRequest = { hospitalDropdownExpanded = false }
                                ) {
                                    allHospitals.forEach { hosp ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(hosp.name, fontWeight = FontWeight.Bold)
                                                    Text(
                                                        "${hosp.area}, ${hosp.city}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            },
                                            onClick = {
                                                selectedHospital = hosp
                                                hospitalDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        OutlinedTextField(
                            value = fullName,
                            onValueChange = {
                                fullName = it
                                touched = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text(if (role == UserRole.HOSPITAL_ADMIN) "Administrator Name *" else "Full Name *")
                            },
                            placeholder = {
                                Text(if (role == UserRole.HOSPITAL_ADMIN) "e.g. Dr. Suresh Kulkarni" else "e.g. Rahul Verma")
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (role == UserRole.HOSPITAL_ADMIN) Icons.Default.Badge else Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
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
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    PrimaryButton(
                        text = "Continue to Dashboard",
                        onClick = { handleContinue() },
                        enabled = isValid
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Subtle Security Notice
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
                    text = "Encrypted session persistence",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
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
