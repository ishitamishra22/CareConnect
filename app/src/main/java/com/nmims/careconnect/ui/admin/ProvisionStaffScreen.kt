package com.nmims.careconnect.ui.admin

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nmims.careconnect.data.auth.AuthValidators
import com.nmims.careconnect.data.model.Doctor
import com.nmims.careconnect.data.model.Hospital
import com.nmims.careconnect.data.model.UserRole
import com.nmims.careconnect.data.repository.AdminRepository
import com.nmims.careconnect.data.repository.HospitalRepository
import com.nmims.careconnect.ui.common.PrimaryButton
import kotlinx.coroutines.launch

/**
 * Screen for securely provisioning DOCTOR and HOSPITAL accounts.
 * Admins provision accounts out-of-band via a secondary FirebaseApp instance;
 * temporary random passwords are never exposed or stored, and a password-reset email is dispatched.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProvisionStaffScreen(
    onNavigateBack: () -> Unit,
    adminRepository: AdminRepository = AdminRepository.getInstance()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedRole by remember { mutableStateOf(UserRole.DOCTOR) }
    var email by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }

    val allDoctors = remember { HospitalRepository.getDoctors() }
    val allHospitals = remember { HospitalRepository.getHospitals() }

    var selectedDoctor by remember { mutableStateOf<Doctor?>(null) }
    var selectedHospital by remember { mutableStateOf<Hospital?>(allHospitals.firstOrNull()) }

    var doctorDropdownExpanded by remember { mutableStateOf(false) }
    var hospitalDropdownExpanded by remember { mutableStateOf(false) }

    var touched by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var generalError by remember { mutableStateOf<String?>(null) }

    val emailValidation = AuthValidators.validateEmail(email)
    val nameValidation = AuthValidators.validateName(displayName)
    val isFormValid = emailValidation.isValid && nameValidation.isValid

    fun handleProvision() {
        touched = true
        generalError = null
        if (!isFormValid || isSubmitting) return

        isSubmitting = true
        coroutineScope.launch {
            val assignedDoctorId = if (selectedRole == UserRole.DOCTOR) selectedDoctor?.id else null
            val assignedHospitalId = if (selectedRole == UserRole.DOCTOR) {
                selectedDoctor?.hospitalId ?: selectedHospital?.id
            } else {
                selectedHospital?.id
            }

            val res = adminRepository.provisionStaffAccount(
                context = context,
                email = email.trim(),
                displayName = displayName.trim(),
                role = selectedRole,
                linkedDoctorId = assignedDoctorId,
                linkedHospitalId = assignedHospitalId
            )

            isSubmitting = false
            res.fold(
                onSuccess = {
                    Toast.makeText(
                        context,
                        "Staff account provisioned! Password reset email dispatched to ${email.trim()}.",
                        Toast.LENGTH_LONG
                    ).show()
                    onNavigateBack()
                },
                onFailure = { err ->
                    generalError = err.message ?: "Failed to provision account."
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Provision Staff Account", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Notice
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Secure staff provisioning: Accounts are initialized without exposing credentials. The user receives a secure link to set their password.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
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

                    Text("Select Account Role", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedRole == UserRole.DOCTOR,
                            onClick = { selectedRole = UserRole.DOCTOR }
                        )
                        Text("Doctor", style = MaterialTheme.typography.bodyMedium)

                        Spacer(modifier = Modifier.width(20.dp))

                        RadioButton(
                            selected = selectedRole == UserRole.HOSPITAL,
                            onClick = { selectedRole = UserRole.HOSPITAL }
                        )
                        Text("Hospital Admin", style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Staff Email Address *") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        isError = touched && !emailValidation.isValid,
                        supportingText = {
                            if (touched && !emailValidation.isValid) {
                                Text(emailValidation.errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Full Name / Title *") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        isError = touched && !nameValidation.isValid,
                        supportingText = {
                            if (touched && !nameValidation.isValid) {
                                Text(nameValidation.errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (selectedRole == UserRole.DOCTOR) {
                        ExposedDropdownMenuBox(
                            expanded = doctorDropdownExpanded,
                            onExpandedChange = { doctorDropdownExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedDoctor?.let { "Dr. ${it.name} (${it.specialty})" } ?: "None (Unlinked)",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Link Doctor Directory Profile (Optional)") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = doctorDropdownExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = doctorDropdownExpanded,
                                onDismissRequest = { doctorDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("None") },
                                    onClick = {
                                        selectedDoctor = null
                                        doctorDropdownExpanded = false
                                    }
                                )
                                allDoctors.forEach { doc ->
                                    DropdownMenuItem(
                                        text = { Text("Dr. ${doc.name} - ${doc.specialty}") },
                                        onClick = {
                                            selectedDoctor = doc
                                            if (displayName.isBlank()) displayName = "Dr. ${doc.name}"
                                            doctorDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    ExposedDropdownMenuBox(
                        expanded = hospitalDropdownExpanded,
                        onExpandedChange = { hospitalDropdownExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedHospital?.name ?: "None",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Associated Hospital") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = hospitalDropdownExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = hospitalDropdownExpanded,
                            onDismissRequest = { hospitalDropdownExpanded = false }
                        ) {
                            allHospitals.forEach { hosp ->
                                DropdownMenuItem(
                                    text = { Text(hosp.name) },
                                    onClick = {
                                        selectedHospital = hosp
                                        hospitalDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    PrimaryButton(
                        text = if (isSubmitting) "Provisioning Staff..." else "Provision & Send Invitation",
                        onClick = { handleProvision() },
                        enabled = !isSubmitting
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
