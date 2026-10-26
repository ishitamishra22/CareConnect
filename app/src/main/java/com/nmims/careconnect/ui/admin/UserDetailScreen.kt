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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.nmims.careconnect.data.firebase.FirebaseProvider
import com.nmims.careconnect.data.model.Doctor
import com.nmims.careconnect.data.model.Hospital
import com.nmims.careconnect.data.model.UserProfile
import com.nmims.careconnect.data.model.UserRole
import com.nmims.careconnect.data.repository.AdminRepository
import com.nmims.careconnect.data.repository.HospitalRepository
import com.nmims.careconnect.ui.common.InitialsAvatar
import com.nmims.careconnect.ui.common.PrimaryButton
import kotlinx.coroutines.launch

/**
 * User Detail & Inspection Screen.
 * Provides controls for:
 * - Edit display name
 * - Link/unlink existing Doctor/Hospital catalogue IDs
 * - Activate/deactivate account with confirmation dialog
 * - Send password reset email
 * - Enforces safety guards: Admin cannot deactivate itself or modify other Admins
 * - Disallows changing roles or deleting user accounts from the client UI
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    adminRepository: AdminRepository = AdminRepository.getInstance()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentAdminUid = FirebaseProvider.auth.currentUser?.uid

    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Edit fields
    var editName by remember { mutableStateOf("") }
    var selectedDoctorId by remember { mutableStateOf<String?>(null) }
    var selectedHospitalId by remember { mutableStateOf<String?>(null) }

    var doctorDropdownExpanded by remember { mutableStateOf(false) }
    var hospitalDropdownExpanded by remember { mutableStateOf(false) }

    // Dialog states
    var showStatusConfirmationDialog by remember { mutableStateOf(false) }
    var showPasswordResetDialog by remember { mutableStateOf(false) }
    var isProcessingAction by remember { mutableStateOf(false) }

    val allDoctors = remember { HospitalRepository.getDoctors() }
    val allHospitals = remember { HospitalRepository.getHospitals() }

    fun loadUserData() {
        isLoading = true
        coroutineScope.launch {
            val res = adminRepository.getUsers()
            val found = res.getOrNull()?.find { it.uid == userId }
            if (found != null) {
                userProfile = found
                editName = found.displayName
                selectedDoctorId = found.linkedDoctorId
                selectedHospitalId = found.linkedHospitalId
            } else {
                errorMessage = "User profile not found."
            }
            isLoading = false
        }
    }

    LaunchedEffect(userId) {
        loadUserData()
    }

    val isSelfAdmin = userProfile?.uid == currentAdminUid
    val isOtherAdmin = userProfile?.role == UserRole.ADMIN && !isSelfAdmin

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Account Details", fontWeight = FontWeight.Bold) },
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
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(36.dp))
            }
        } else if (userProfile == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(errorMessage ?: "User not found", color = MaterialTheme.colorScheme.error)
            }
        } else {
            val u = userProfile!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                InitialsAvatar(name = u.displayName.ifBlank { u.email }, size = 68.dp)

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = u.displayName.ifBlank { "User" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = u.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = u.role.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (u.active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = if (u.active) "Account Active" else "Account Inactive",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (u.active) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Section: Edit Profile Information
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Account Information",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Display Name") },
                            enabled = !isOtherAdmin && !isProcessingAction,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Catalogue link options for DOCTOR or HOSPITAL
                        if (u.role == UserRole.DOCTOR) {
                            ExposedDropdownMenuBox(
                                expanded = doctorDropdownExpanded && !isOtherAdmin,
                                onExpandedChange = { doctorDropdownExpanded = it && !isOtherAdmin },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = allDoctors.find { it.id == selectedDoctorId }?.let { "Dr. ${it.name} (${it.specialty})" } ?: "None (Unlinked)",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Linked Doctor Catalogue Entry") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = doctorDropdownExpanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = doctorDropdownExpanded,
                                    onDismissRequest = { doctorDropdownExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("None (Unlink)") },
                                        onClick = {
                                            selectedDoctorId = null
                                            doctorDropdownExpanded = false
                                        }
                                    )
                                    allDoctors.forEach { doc ->
                                        DropdownMenuItem(
                                            text = { Text("Dr. ${doc.name} - ${doc.specialty}") },
                                            onClick = {
                                                selectedDoctorId = doc.id
                                                doctorDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        if (u.role == UserRole.HOSPITAL || u.role == UserRole.DOCTOR) {
                            ExposedDropdownMenuBox(
                                expanded = hospitalDropdownExpanded && !isOtherAdmin,
                                onExpandedChange = { hospitalDropdownExpanded = it && !isOtherAdmin },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = allHospitals.find { it.id == selectedHospitalId }?.name ?: "None (Unlinked)",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Linked Hospital Catalogue Entry") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = hospitalDropdownExpanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = hospitalDropdownExpanded,
                                    onDismissRequest = { hospitalDropdownExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("None (Unlink)") },
                                        onClick = {
                                            selectedHospitalId = null
                                            hospitalDropdownExpanded = false
                                        }
                                    )
                                    allHospitals.forEach { hosp ->
                                        DropdownMenuItem(
                                            text = { Text(hosp.name) },
                                            onClick = {
                                                selectedHospitalId = hosp.id
                                                hospitalDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        if (!isOtherAdmin) {
                            Button(
                                onClick = {
                                    val nameValidation = AuthValidators.validateName(editName)
                                    if (!nameValidation.isValid) {
                                        Toast.makeText(context, nameValidation.errorMessage ?: "Invalid name", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }

                                    isProcessingAction = true
                                    coroutineScope.launch {
                                        val res = adminRepository.updateUserProfile(
                                            targetUser = u,
                                            newDisplayName = editName,
                                            newLinkedDoctorId = selectedDoctorId,
                                            newLinkedHospitalId = selectedHospitalId
                                        )
                                        isProcessingAction = false
                                        res.fold(
                                            onSuccess = {
                                                Toast.makeText(context, "Profile updated successfully.", Toast.LENGTH_SHORT).show()
                                                loadUserData()
                                            },
                                            onFailure = {
                                                Toast.makeText(context, it.message ?: "Failed to update profile", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                },
                                enabled = !isProcessingAction,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Save Profile Changes")
                            }
                        } else {
                            Text(
                                text = "Safety Notice: Modifying other administrator profiles is strictly forbidden.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section: Sensitive Account Actions
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Sensitive Operations",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Password Reset Button
                        OutlinedButton(
                            onClick = { showPasswordResetDialog = true },
                            enabled = !isProcessingAction,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.LockReset, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Send Password Reset Email")
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Activate / Deactivate Button
                        if (isSelfAdmin) {
                            Text(
                                text = "Safety Guard: You cannot deactivate your own administrator account.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        } else if (isOtherAdmin) {
                            Text(
                                text = "Safety Guard: Modifying another administrator account is forbidden.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        } else {
                            OutlinedButton(
                                onClick = { showStatusConfirmationDialog = true },
                                enabled = !isProcessingAction,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (u.active) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (u.active) Icons.Default.Block else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (u.active) "Deactivate Account" else "Activate Account")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Confirmation Dialog for Activate/Deactivate
    if (showStatusConfirmationDialog && userProfile != null) {
        val target = userProfile!!
        val actionText = if (target.active) "Deactivate" else "Activate"

        AlertDialog(
            onDismissRequest = { showStatusConfirmationDialog = false },
            title = { Text("$actionText User Account?") },
            text = {
                Text(
                    if (target.active)
                        "Deactivating this account (${target.email}) will immediately block the user from signing in across all CareConnect portals. This action is recorded in the administrator audit log."
                    else
                        "Activating this account (${target.email}) will restore full portal access. This action will be recorded in the audit log."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showStatusConfirmationDialog = false
                        isProcessingAction = true
                        coroutineScope.launch {
                            val res = adminRepository.setUserActiveStatus(target, !target.active)
                            isProcessingAction = false
                            res.fold(
                                onSuccess = {
                                    Toast.makeText(context, "Account status updated to ${if (!target.active) "Active" else "Inactive"}.", Toast.LENGTH_SHORT).show()
                                    loadUserData()
                                },
                                onFailure = {
                                    Toast.makeText(context, it.message ?: "Failed to update status", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                ) {
                    Text(
                        actionText,
                        color = if (target.active) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showStatusConfirmationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Password Reset Confirmation Dialog
    if (showPasswordResetDialog && userProfile != null) {
        val target = userProfile!!
        AlertDialog(
            onDismissRequest = { showPasswordResetDialog = false },
            title = { Text("Send Password Reset?") },
            text = { Text("A secure password reset email will be dispatched to ${target.email}. An entry will be saved to the audit log.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPasswordResetDialog = false
                        isProcessingAction = true
                        coroutineScope.launch {
                            val res = adminRepository.sendPasswordReset(target)
                            isProcessingAction = false
                            res.fold(
                                onSuccess = {
                                    Toast.makeText(context, "Password reset email dispatched to ${target.email}", Toast.LENGTH_LONG).show()
                                },
                                onFailure = {
                                    Toast.makeText(context, it.message ?: "Failed to dispatch reset email", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                ) {
                    Text("Send Email")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
