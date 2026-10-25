package com.nmims.careconnect.ui.admin

/*
 * AdminAdmissionReviewScreen:
 * Hospital administrator interface for evaluating and adjudicating admission requests:
 * - Restricts access strictly to authenticated UserRole.HOSPITAL_ADMIN sessions with matching hospitalId
 * - Distinguishes unauthenticated, unauthorized, loading, missing-record, and error states
 * - Displays complete patient profile with InitialsAvatar, emergency contacts, allergies, chronic conditions, current medications
 * - Phone dialer integration for rapid patient and emergency contact phone calling
 * - Displays requested room accommodation, daily rate, stay duration, preferred date, and estimated stay cost
 * - Displays health insurance claim coverage details or self-pay designation
 * - Displays patient's shareable medical portfolio summary
 * - Adjudication Actions:
 *   - Approve: Dialog requiring trimmed room/bed number (min 2 chars) and optional remark, updates status to APPROVED
 *   - Reject: Dialog requiring trimmed rejection remark (min 5 chars), updates status to REJECTED
 *   - Mark Discharged: Confirmation dialog visible only for APPROVED requests, updates status to DISCHARGED
 * - Enforces submit-locks, error recovery, theme-adaptive status colors, and proper cancellation handling
 * - Extracted stateless AdminAdmissionReviewContent with isolated Compose Previews
 */

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.nmims.careconnect.data.model.Admission
import com.nmims.careconnect.data.model.AdmissionStatus
import com.nmims.careconnect.data.model.Hospital
import com.nmims.careconnect.data.model.InsuranceDetails
import com.nmims.careconnect.data.model.RoomType
import com.nmims.careconnect.data.model.UserRole
import com.nmims.careconnect.data.repository.AdmissionRepositoryProvider
import com.nmims.careconnect.data.repository.HospitalRepository
import com.nmims.careconnect.session.AppSession
import com.nmims.careconnect.ui.common.CareTopBar
import com.nmims.careconnect.ui.common.DateUtils
import com.nmims.careconnect.ui.common.EmptyState
import com.nmims.careconnect.ui.common.LabelChip
import com.nmims.careconnect.ui.common.LoadingIndicator
import com.nmims.careconnect.ui.common.SectionCard
import com.nmims.careconnect.ui.common.StatusChip
import com.nmims.careconnect.ui.common.admissionStatusContainerColor
import com.nmims.careconnect.ui.theme.CareConnectTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

sealed interface AdminAdmissionReviewUiState {
    data object Loading : AdminAdmissionReviewUiState
    data object Unauthenticated : AdminAdmissionReviewUiState
    data object Unauthorized : AdminAdmissionReviewUiState
    data object NotFound : AdminAdmissionReviewUiState
    data class Error(val message: String) : AdminAdmissionReviewUiState
    data class Success(val admission: Admission) : AdminAdmissionReviewUiState
}

@Composable
fun AdminAdmissionReviewScreen(
    admissionId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember(context) { AdmissionRepositoryProvider.get(context) }

    var uiState by remember { mutableStateOf<AdminAdmissionReviewUiState>(AdminAdmissionReviewUiState.Loading) }
    var retryTrigger by remember { mutableIntStateOf(0) }

    // Dialog & processing states
    var showApproveDialog by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var showDischargeDialog by remember { mutableStateOf(false) }

    var roomNumberInput by remember { mutableStateOf("") }
    var adminRemarkInput by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    suspend fun fetchAdmission() {
        val record = repository.getById(admissionId)
        if (record == null) {
            uiState = AdminAdmissionReviewUiState.NotFound
        } else {
            val currentRole = AppSession.currentRole
            val currentHospitalId = AppSession.currentHospitalId

            // Strict Hospital Admin verification: role must be HOSPITAL_ADMIN and assigned hospital must match record
            val isAdminAuthorized = currentRole == UserRole.HOSPITAL_ADMIN &&
                    !currentHospitalId.isNullOrBlank() &&
                    record.hospitalId == currentHospitalId

            when {
                isAdminAuthorized -> {
                    uiState = AdminAdmissionReviewUiState.Success(record)
                }
                currentRole == null && currentHospitalId.isNullOrBlank() && AppSession.currentPatientId.isBlank() -> {
                    uiState = AdminAdmissionReviewUiState.Unauthenticated
                }
                else -> {
                    uiState = AdminAdmissionReviewUiState.Unauthorized
                }
            }
        }
    }

    LaunchedEffect(admissionId, retryTrigger) {
        if (admissionId.isBlank()) {
            uiState = AdminAdmissionReviewUiState.NotFound
            return@LaunchedEffect
        }

        uiState = AdminAdmissionReviewUiState.Loading

        try {
            fetchAdmission()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            uiState = AdminAdmissionReviewUiState.Error(
                e.localizedMessage ?: "Unable to load admission request. Please check your network connection and try again."
            )
        }
    }

    AdminAdmissionReviewContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onRetry = { retryTrigger++ },
        onApproveClick = {
            roomNumberInput = ""
            adminRemarkInput = ""
            showApproveDialog = true
        },
        onRejectClick = {
            adminRemarkInput = ""
            showRejectDialog = true
        },
        onDischargeClick = {
            showDischargeDialog = true
        },
        hospitalLookup = { hospitalId -> HospitalRepository.getHospital(hospitalId) }
    )

    val currentRecord = (uiState as? AdminAdmissionReviewUiState.Success)?.admission

    // -----------------------------------------------------------------------
    // Approve & Bed Assignment Dialog
    // -----------------------------------------------------------------------
    if (showApproveDialog && currentRecord != null) {
        val trimmedRoom = roomNumberInput.trim()
        val isRoomValid = trimmedRoom.length in 2..50

        AlertDialog(
            onDismissRequest = { if (!isProcessing) showApproveDialog = false },
            title = {
                Text(
                    text = "Approve Admission & Assign Bed",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column {
                    Text(
                        text = "Assign a room and bed number for ${currentRecord.patientName}. Requested accommodation: ${currentRecord.roomType} (₹${currentRecord.roomPricePerDay}/day).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = roomNumberInput,
                        onValueChange = { roomNumberInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Assigned Room / Bed Number *") },
                        placeholder = { Text("e.g. Ward 3, Bed 305-B") },
                        singleLine = true,
                        isError = roomNumberInput.isNotBlank() && !isRoomValid,
                        supportingText = {
                            if (roomNumberInput.isNotBlank() && !isRoomValid) {
                                Text("Room number must be between 2 and 50 characters", color = MaterialTheme.colorScheme.error)
                            } else {
                                Text("Required. Minimum 2 characters.")
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isProcessing,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = adminRemarkInput,
                        onValueChange = { adminRemarkInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Admin Remark (Optional)") },
                        placeholder = { Text("e.g. Admitted under Dr. Sharma, fasting required") },
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isProcessing,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!isRoomValid || isProcessing) return@Button
                        isProcessing = true
                        coroutineScope.launch {
                            try {
                                val ok = repository.updateStatus(
                                    id = currentRecord.id,
                                    status = AdmissionStatus.APPROVED,
                                    roomNumber = trimmedRoom,
                                    remark = adminRemarkInput.trim().takeIf { it.isNotBlank() }
                                )
                                if (ok) {
                                    showApproveDialog = false
                                    Toast.makeText(context, "Admission approved. Room assigned.", Toast.LENGTH_SHORT).show()
                                    fetchAdmission()
                                } else {
                                    Toast.makeText(context, "Failed to approve admission. Server error or network disconnected.", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_LONG).show()
                            } finally {
                                isProcessing = false
                            }
                        }
                    },
                    enabled = isRoomValid && !isProcessing,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(if (isProcessing) "Saving..." else "Confirm Approval", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showApproveDialog = false },
                    enabled = !isProcessing
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // -----------------------------------------------------------------------
    // Rejection Dialog
    // -----------------------------------------------------------------------
    if (showRejectDialog && currentRecord != null) {
        val trimmedRemark = adminRemarkInput.trim()
        val isRemarkValid = trimmedRemark.length >= 5

        AlertDialog(
            onDismissRequest = { if (!isProcessing) showRejectDialog = false },
            title = {
                Text(
                    text = "Reject Admission Request",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Column {
                    Text(
                        text = "Provide a clinical or administrative reason for rejecting this admission request for ${currentRecord.patientName}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = adminRemarkInput,
                        onValueChange = { adminRemarkInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Rejection Remark *") },
                        placeholder = { Text("e.g. Requested ICU suite unavailable. Patient redirected to emergency triage.") },
                        minLines = 3,
                        isError = adminRemarkInput.isNotBlank() && !isRemarkValid,
                        supportingText = {
                            if (adminRemarkInput.isNotBlank() && !isRemarkValid) {
                                Text("Rejection remark must be at least 5 characters (${trimmedRemark.length}/5)", color = MaterialTheme.colorScheme.error)
                            } else {
                                Text("Mandatory. Minimum 5 characters.")
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isProcessing,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.error,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!isRemarkValid || isProcessing) return@Button
                        isProcessing = true
                        coroutineScope.launch {
                            try {
                                val ok = repository.updateStatus(
                                    id = currentRecord.id,
                                    status = AdmissionStatus.REJECTED,
                                    remark = trimmedRemark
                                )
                                if (ok) {
                                    showRejectDialog = false
                                    Toast.makeText(context, "Admission request rejected.", Toast.LENGTH_SHORT).show()
                                    fetchAdmission()
                                } else {
                                    Toast.makeText(context, "Failed to reject admission. Server error or network disconnected.", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_LONG).show()
                            } finally {
                                isProcessing = false
                            }
                        }
                    },
                    enabled = isRemarkValid && !isProcessing,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(if (isProcessing) "Rejecting..." else "Reject Request", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRejectDialog = false },
                    enabled = !isProcessing
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // -----------------------------------------------------------------------
    // Discharge Confirmation Dialog
    // -----------------------------------------------------------------------
    if (showDischargeDialog && currentRecord != null) {
        AlertDialog(
            onDismissRequest = { if (!isProcessing) showDischargeDialog = false },
            title = {
                Text(
                    text = "Mark Patient Discharged?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to mark patient ${currentRecord.patientName} as DISCHARGED? This will conclude inpatient accommodation in room ${currentRecord.assignedRoom ?: currentRecord.roomType} and finalize the admission record.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isProcessing) return@Button
                        isProcessing = true
                        coroutineScope.launch {
                            try {
                                val ok = repository.updateStatus(
                                    id = currentRecord.id,
                                    status = AdmissionStatus.DISCHARGED
                                )
                                if (ok) {
                                    showDischargeDialog = false
                                    Toast.makeText(context, "Patient marked as discharged.", Toast.LENGTH_SHORT).show()
                                    fetchAdmission()
                                } else {
                                    Toast.makeText(context, "Failed to update discharge status. Check your internet connection.", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_LONG).show()
                            } finally {
                                isProcessing = false
                            }
                        }
                    },
                    enabled = !isProcessing,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(if (isProcessing) "Discharging..." else "Confirm Discharge", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDischargeDialog = false },
                    enabled = !isProcessing
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
fun AdminAdmissionReviewContent(
    uiState: AdminAdmissionReviewUiState,
    onNavigateBack: () -> Unit,
    onRetry: () -> Unit,
    onApproveClick: () -> Unit = {},
    onRejectClick: () -> Unit = {},
    onDischargeClick: () -> Unit = {},
    hospitalLookup: (String) -> Hospital? = { HospitalRepository.getHospital(it) },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            CareTopBar(
                title = "Review Admission",
                onBack = onNavigateBack
            )
        },
        modifier = modifier
    ) { innerPadding ->
        when (uiState) {
            is AdminAdmissionReviewUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LoadingIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Loading admission details...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            is AdminAdmissionReviewUiState.Unauthenticated -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Hospital Admin Sign-In Required",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Please sign in with an authorized hospital administrator account to evaluate inpatient admission requests.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = onNavigateBack,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Go Back", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
            is AdminAdmissionReviewUiState.Unauthorized -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Block,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Access Restricted",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "You are not authorized to review this admission request. Administrators can review admissions for their designated hospital only.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = onNavigateBack,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Go Back", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
            is AdminAdmissionReviewUiState.NotFound -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.SearchOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Admission Request Not Found",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "The requested admission record could not be found. It may have been deleted or the ID is invalid.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = onNavigateBack,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Go Back", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
            is AdminAdmissionReviewUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Unable to Load Admission",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = uiState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = onRetry,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Retry", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
            is AdminAdmissionReviewUiState.Success -> {
                val record = uiState.admission
                val hospital = remember(record.hospitalId) { hospitalLookup(record.hospitalId) }
                val statusContainer = admissionStatusContainerColor(record.status)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header Hero Card
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LabelChip(
                                    text = "REF: ${record.referenceCode()}",
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                StatusChip(
                                    text = record.status.label,
                                    containerColor = statusContainer
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = record.patientName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${hospital?.name ?: record.hospitalId} • Submitted ${DateUtils.format(record.createdAtMillis)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Assigned Room Banner (Only when assignedRoom is non-blank)
                            if (!record.assignedRoom.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MeetingRoom,
                                            contentDescription = "Room",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Assigned Bed / Room: ${record.assignedRoom}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                            Text(
                                                text = "Bed allocation confirmed by hospital administration",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                                            )
                                        }
                                    }
                                }
                            }

                            // Administrative Remarks / Rejection Reason
                            if (!record.adminRemark.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                val isRejected = record.status == AdmissionStatus.REJECTED
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isRejected) {
                                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = if (isRejected) "Rejection Reason:" else "Hospital Admin Remark:",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isRejected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = record.adminRemark!!,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Reason & Requested Accommodation
                    SectionCard(title = "Reason & Requested Accommodation") {
                        DetailItemRow("Reason for Admission", record.reason)
                        if (!record.referringDoctor.isNullOrBlank()) {
                            DetailItemRow("Referring Doctor", record.referringDoctor!!)
                        }
                        DetailItemRow("Requested Room Type", record.roomType)
                        DetailItemRow("Daily Room Rate", "₹${record.roomPricePerDay} / day")
                        DetailItemRow("Preferred Date", DateUtils.formatWithDay(record.preferredDateMillis))
                        DetailItemRow("Expected Stay", "${record.expectedStayDays} ${if (record.expectedStayDays == 1) "day" else "days"}")

                        // Present calculation breakdown only when it accurately represents the persisted total
                        if (record.roomPricePerDay * record.expectedStayDays == record.estimatedCost) {
                            DetailItemRow("Stay Calculation", "₹${record.roomPricePerDay} × ${record.expectedStayDays} days")
                        }
                        DetailItemRow("Estimated Stay Cost", "₹${record.estimatedCost}")
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Patient Details & Emergency Contacts
                    SectionCard(title = "Patient Information & Contacts") {
                        DetailItemRow("Full Name", record.patientName)
                        DetailItemRow("Age / Gender", "${record.patientAge} years, ${record.patientGender}")
                        DetailItemRow("Blood Group", record.patientBloodGroup)
                        DetailItemRow("Primary Phone", record.patientPhone)
                        DetailItemRow("Address", record.patientAddress)
                        DetailItemRow("Emergency Contact", "${record.emergencyContactName} (${record.emergencyContactPhone})")

                        val validPatientPhone = remember(record.patientPhone) {
                            sanitizeAndValidatePhone(record.patientPhone)
                        }
                        val validEmergencyPhone = remember(record.emergencyContactPhone) {
                            sanitizeAndValidatePhone(record.emergencyContactPhone)
                        }

                        if (validPatientPhone != null || validEmergencyPhone != null) {
                            Spacer(modifier = Modifier.height(12.dp))

                            if (validPatientPhone != null) {
                                Button(
                                    onClick = { launchDialer(context, validPatientPhone) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "Call Patient",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Call Patient (${record.patientPhone})", style = MaterialTheme.typography.labelLarge)
                                }
                            }

                            if (validPatientPhone != null && validEmergencyPhone != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            if (validEmergencyPhone != null) {
                                OutlinedButton(
                                    onClick = { launchDialer(context, validEmergencyPhone) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "Call Emergency Contact",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Call Emergency Contact (${record.emergencyContactPhone})", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Clinical Background (Explicitly indicates "None reported" when empty)
                    SectionCard(title = "Clinical Background") {
                        DetailItemRow(
                            label = "Known Allergies",
                            value = if (record.allergies.isNotEmpty()) record.allergies.joinToString(", ") else "None reported"
                        )
                        DetailItemRow(
                            label = "Chronic Conditions",
                            value = if (record.chronicConditions.isNotEmpty()) record.chronicConditions.joinToString(", ") else "None reported"
                        )
                        DetailItemRow(
                            label = "Current Medications",
                            value = if (record.currentMedications.isNotEmpty()) record.currentMedications.joinToString(", ") else "None reported"
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Insurance Details
                    SectionCard(title = "Insurance Coverage") {
                        val ins = record.insuranceDetails
                        if (ins != null) {
                            DetailItemRow("Billing Mode", "Health Insurance Claim")
                            DetailItemRow("Insurance Provider", ins.providerName)
                            DetailItemRow("Policy Number", ins.policyNumber)
                            DetailItemRow("Policyholder", ins.policyholderName)
                            DetailItemRow("Valid Till", DateUtils.format(ins.validTillMillis))
                            DetailItemRow("Sum Insured", "₹${ins.sumInsured}")
                        } else {
                            DetailItemRow("Billing Mode", "Self-Pay Inpatient")
                            DetailItemRow("Insurance Policy", "No health insurance attached. Patient self-pay.")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Forwarded Medical Summary
                    SectionCard(title = "Forwarded Medical Portfolio") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderShared,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Patient-consented portfolio records attached during admission request",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = record.sharedPortfolioSummary.ifBlank { "No previous medical summary forwarded." },
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(14.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // -------------------------------------------------------
                    // Adjudication Action Buttons based on Status
                    // -------------------------------------------------------
                    when (record.status) {
                        AdmissionStatus.PENDING -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onRejectClick,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Reject", fontWeight = FontWeight.SemiBold)
                                }

                                Button(
                                    onClick = onApproveClick,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Approve", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        AdmissionStatus.APPROVED -> {
                            Button(
                                onClick = onDischargeClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Mark Discharged", fontWeight = FontWeight.SemiBold)
                            }
                        }
                        AdmissionStatus.REJECTED -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "This admission request has been rejected by hospital administration.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                        AdmissionStatus.DISCHARGED -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "This admission has concluded and the patient has been discharged.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

/**
 * Sanitizes and validates a phone number string for telephony actions.
 * - Strips common formatting separators (spaces, hyphens, parentheses, periods).
 * - Requires at least 7 numeric digits.
 * - Rejects malformed values: multiple '+' characters, or '+' appearing anywhere other than index 0.
 * - Rejects non-numeric characters.
 * - Returns sanitized dialable string (e.g. "+919820012345" or "02226751000") or null if invalid.
 */
private fun sanitizeAndValidatePhone(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    val stripped = raw.replace("[\\s\\-().]".toRegex(), "")
    if (!stripped.matches("^\\+?[0-9]{7,15}$".toRegex())) return null
    val digitsOnly = stripped.replace("+", "")
    if (digitsOnly.length < 7) return null
    return stripped
}

private fun launchDialer(context: Context, phoneNumber: String) {
    try {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phoneNumber")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "Unable to launch phone dialer.", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun DetailItemRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.8f)
        )
    }
}

// ---------------------------------------------------------------------------
// Previews (Isolated from runtime Firebase and Android Context)
// ---------------------------------------------------------------------------

private val previewMockHospital = Hospital(
    id = "HOSP001",
    name = "Lilavati Hospital & Research Centre",
    area = "Bandra West",
    city = "Mumbai",
    address = "A-791, Bandra Reclamation, Bandra West, Mumbai",
    phone = "+91 22 2675 1000",
    rating = 4.7,
    reviewCount = 320,
    specialties = listOf("Cardiology", "Neurology"),
    facilities = listOf("ICU", "Diagnostics"),
    emergency24x7 = true,
    roomTypes = listOf(
        RoomType("General Ward", 2000, "Shared accommodation with 24/7 nursing support"),
        RoomType("Semi-Private", 4500, "Two-bed sharing with partition and attached washroom"),
        RoomType("Private Deluxe", 8500, "Individual room with attendant sofa, TV, and en-suite")
    ),
    aboutHtml = "Premier healthcare facility in Western Mumbai.",
    accentColorHex = "#0D6EFD"
)

private val previewMockPendingAdmission = Admission(
    id = "ADM-20241025-001",
    patientId = "PAT001",
    hospitalId = "HOSP001",
    patientName = "Suresh Patel",
    patientAge = 58,
    patientGender = "Male",
    patientBloodGroup = "B+",
    patientPhone = "+91 98200 12345",
    patientAddress = "Flat 402, Sea Breeze Apts, Bandra West, Mumbai 400050",
    emergencyContactName = "Kavita Patel (Spouse)",
    emergencyContactPhone = "+91 98200 54321",
    reason = "Severe chest tightness and episodic shortness of breath requiring monitoring",
    referringDoctor = "Dr. Rajesh Shah",
    roomType = "Private Deluxe",
    roomPricePerDay = 8500,
    preferredDateMillis = System.currentTimeMillis() + 86400000L,
    expectedStayDays = 4,
    estimatedCost = 34000,
    allergies = listOf("Penicillin", "Sulfa drugs"),
    chronicConditions = listOf("Type 2 Diabetes", "Hypertension"),
    currentMedications = listOf("Metformin 500mg", "Amlodipine 5mg"),
    insuranceDetails = InsuranceDetails(
        providerName = "Star Health Insurance",
        policyNumber = "SH-MED-2024-88412",
        policyholderName = "Suresh Patel",
        validityDateMillis = System.currentTimeMillis() + 31536000000L,
        sumInsured = 500000
    ),
    sharedPortfolioSummary = "Diagnosed with Type 2 Diabetes (2018). Last HbA1c: 7.2%. No known adverse cardiac events prior to current presentation.",
    status = AdmissionStatus.PENDING,
    createdAtMillis = System.currentTimeMillis() - 3600000L
)

private val previewMockApprovedAdmission = previewMockPendingAdmission.copy(
    id = "ADM-20241025-002",
    status = AdmissionStatus.APPROVED,
    assignedRoomNumber = "Deluxe Ward 4, Bed 412",
    adminRemark = "Admitted under Dr. Rajesh Shah. Fasting blood work scheduled for 08:00 AM."
)

private val previewMockRejectedAdmission = previewMockPendingAdmission.copy(
    id = "ADM-20241025-003",
    status = AdmissionStatus.REJECTED,
    adminRemark = "Private Deluxe wing currently under scheduled maintenance. Advised rescheduling or choosing Semi-Private accommodation."
)

private val previewMockDischargedAdmission = previewMockPendingAdmission.copy(
    id = "ADM-20241025-004",
    status = AdmissionStatus.DISCHARGED,
    assignedRoomNumber = "Deluxe Ward 4, Bed 412",
    adminRemark = "Discharge summary handed over. Follow-up consultation in 10 days."
)

@Preview(showBackground = true, name = "Pending Request")
@Composable
fun AdminAdmissionReviewScreenPendingPreview() {
    CareConnectTheme {
        AdminAdmissionReviewContent(
            uiState = AdminAdmissionReviewUiState.Success(previewMockPendingAdmission),
            onNavigateBack = {},
            onRetry = {},
            hospitalLookup = { previewMockHospital }
        )
    }
}

@Preview(showBackground = true, name = "Approved Request")
@Composable
fun AdminAdmissionReviewScreenApprovedPreview() {
    CareConnectTheme {
        AdminAdmissionReviewContent(
            uiState = AdminAdmissionReviewUiState.Success(previewMockApprovedAdmission),
            onNavigateBack = {},
            onRetry = {},
            hospitalLookup = { previewMockHospital }
        )
    }
}

@Preview(showBackground = true, name = "Rejected Request")
@Composable
fun AdminAdmissionReviewScreenRejectedPreview() {
    CareConnectTheme {
        AdminAdmissionReviewContent(
            uiState = AdminAdmissionReviewUiState.Success(previewMockRejectedAdmission),
            onNavigateBack = {},
            onRetry = {},
            hospitalLookup = { previewMockHospital }
        )
    }
}

@Preview(showBackground = true, name = "Discharged Request")
@Composable
fun AdminAdmissionReviewScreenDischargedPreview() {
    CareConnectTheme {
        AdminAdmissionReviewContent(
            uiState = AdminAdmissionReviewUiState.Success(previewMockDischargedAdmission),
            onNavigateBack = {},
            onRetry = {},
            hospitalLookup = { previewMockHospital }
        )
    }
}

@Preview(showBackground = true, name = "Unauthorized State")
@Composable
fun AdminAdmissionReviewScreenUnauthorizedPreview() {
    CareConnectTheme {
        AdminAdmissionReviewContent(
            uiState = AdminAdmissionReviewUiState.Unauthorized,
            onNavigateBack = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true, name = "Unauthenticated State")
@Composable
fun AdminAdmissionReviewScreenUnauthenticatedPreview() {
    CareConnectTheme {
        AdminAdmissionReviewContent(
            uiState = AdminAdmissionReviewUiState.Unauthenticated,
            onNavigateBack = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true, name = "Not Found State")
@Composable
fun AdminAdmissionReviewScreenNotFoundPreview() {
    CareConnectTheme {
        AdminAdmissionReviewContent(
            uiState = AdminAdmissionReviewUiState.NotFound,
            onNavigateBack = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true, name = "Generic Preview")
@Composable
fun AdminAdmissionReviewScreenPreview() {
    CareConnectTheme {
        AdminAdmissionReviewContent(
            uiState = AdminAdmissionReviewUiState.Success(previewMockPendingAdmission),
            onNavigateBack = {},
            onRetry = {}
        )
    }
}
