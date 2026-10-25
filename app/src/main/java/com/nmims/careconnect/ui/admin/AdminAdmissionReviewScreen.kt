package com.nmims.careconnect.ui.admin

/*
 * AdminAdmissionReviewScreen:
 * Hospital administrator interface for evaluating and adjudicating admission requests:
 * - Restricts access strictly to authenticated UserRole.HOSPITAL_ADMIN sessions with matching hospitalId
 * - Distinguishes unauthenticated, unauthorized, loading, missing-record, and error states
 * - Displays complete patient profile, emergency contacts, allergies, chronic conditions, current medications
 * - Displays requested room accommodation, daily rate, stay duration, preferred date, and estimated stay cost
 * - Displays health insurance claim coverage details or self-pay designation
 * - Displays patient's shareable medical portfolio summary
 * - Adjudication Actions:
 *   - Approve: Dialog requiring trimmed room/bed number (min 2 chars) and optional remark, updates status to APPROVED
 *   - Reject: Dialog requiring trimmed rejection remark (min 5 chars), updates status to REJECTED
 *   - Mark Discharged: Confirmation dialog visible only for APPROVED requests, updates status to DISCHARGED
 * - Enforces submit-locks, error recovery, and proper cancellation handling
 * - Extracted stateless AdminAdmissionReviewContent with isolated Compose Previews
 */

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nmims.careconnect.data.model.Admission
import com.nmims.careconnect.data.model.AdmissionStatus
import com.nmims.careconnect.data.model.Hospital
import com.nmims.careconnect.data.model.InsuranceDetails
import com.nmims.careconnect.data.model.UserRole
import com.nmims.careconnect.data.repository.AdmissionRepositoryProvider
import com.nmims.careconnect.data.repository.HospitalRepository
import com.nmims.careconnect.session.AppSession
import com.nmims.careconnect.ui.common.CareTopBar
import com.nmims.careconnect.ui.common.DateUtils
import com.nmims.careconnect.ui.common.EmptyState
import com.nmims.careconnect.ui.common.LoadingIndicator
import com.nmims.careconnect.ui.common.SectionCard
import com.nmims.careconnect.ui.common.StatusChip
import com.nmims.careconnect.ui.theme.CareConnectTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

sealed interface AdminAdmissionReviewUiState {
    object Loading : AdminAdmissionReviewUiState
    object Unauthenticated : AdminAdmissionReviewUiState
    object Unauthorized : AdminAdmissionReviewUiState
    object NotFound : AdminAdmissionReviewUiState
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
            title = { Text("Approve Admission & Assign Bed") },
            text = {
                Column {
                    Text(
                        text = "Assign a room and bed number for ${currentRecord.patientName}. Requested accommodation: ${currentRecord.roomType} (₹${currentRecord.roomPricePerDay}/day).",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
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
                                Text("Room number must be between 2 and 50 characters")
                            } else {
                                Text("Required. Minimum 2 characters.")
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isProcessing
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = adminRemarkInput,
                        onValueChange = { adminRemarkInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Admin Remark (Optional)") },
                        placeholder = { Text("e.g. Admitted under Dr. Sharma, fasting required") },
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isProcessing
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
                    enabled = isRoomValid && !isProcessing
                ) {
                    Text(if (isProcessing) "Saving..." else "Confirm Approval")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showApproveDialog = false },
                    enabled = !isProcessing
                ) {
                    Text("Cancel")
                }
            }
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
            title = { Text("Reject Admission Request") },
            text = {
                Column {
                    Text(
                        text = "Provide a clinical or administrative reason for rejecting this admission request for ${currentRecord.patientName}.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
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
                                Text("Rejection remark must be at least 5 characters (${trimmedRemark.length}/5)")
                            } else {
                                Text("Mandatory. Minimum 5 characters.")
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isProcessing
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(if (isProcessing) "Rejecting..." else "Reject Request")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRejectDialog = false },
                    enabled = !isProcessing
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // -----------------------------------------------------------------------
    // Discharge Confirmation Dialog
    // -----------------------------------------------------------------------
    if (showDischargeDialog && currentRecord != null) {
        AlertDialog(
            onDismissRequest = { if (!isProcessing) showDischargeDialog = false },
            title = { Text("Mark Patient Discharged?") },
            text = {
                Text(
                    text = "Are you sure you want to mark patient ${currentRecord.patientName} as DISCHARGED? This will conclude inpatient accommodation in room ${currentRecord.assignedRoom ?: currentRecord.roomType} and finalize the record.",
                    style = MaterialTheme.typography.bodyMedium
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
                    enabled = !isProcessing
                ) {
                    Text(if (isProcessing) "Discharging..." else "Confirm Discharge")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDischargeDialog = false },
                    enabled = !isProcessing
                ) {
                    Text("Cancel")
                }
            }
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
                    LoadingIndicator()
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
                    EmptyState(
                        icon = Icons.Default.AccountCircle,
                        message = "Hospital Admin Sign-In Required\n\nPlease sign in with an authorized hospital administrator account to evaluate inpatient admission requests.",
                        action = {
                            Button(onClick = onNavigateBack) {
                                Text("Go Back")
                            }
                        }
                    )
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
                    EmptyState(
                        icon = Icons.Default.Block,
                        message = "Access Restricted\n\nYou are not authorized to review this admission request. Administrators can review admissions for their designated hospital only.",
                        action = {
                            Button(onClick = onNavigateBack) {
                                Text("Go Back")
                            }
                        }
                    )
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
                    EmptyState(
                        icon = Icons.Default.SearchOff,
                        message = "Admission Request Not Found\n\nThe requested admission record could not be found. It may have been deleted or the ID is invalid.",
                        action = {
                            Button(onClick = onNavigateBack) {
                                Text("Go Back")
                            }
                        }
                    )
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
                    EmptyState(
                        icon = Icons.Default.ErrorOutline,
                        message = "Unable to Load Admission\n\n${uiState.message}",
                        action = {
                            Button(onClick = onRetry) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Retry")
                            }
                        }
                    )
                }
            }
            is AdminAdmissionReviewUiState.Success -> {
                val record = uiState.admission
                val hospital = remember(record.hospitalId) { hospitalLookup(record.hospitalId) }
                val statusColor = when (record.status) {
                    AdmissionStatus.PENDING -> Color(0xFFFFF3CD)
                    AdmissionStatus.APPROVED -> Color(0xFFD1E7DD)
                    AdmissionStatus.REJECTED -> Color(0xFFF8D7DA)
                    AdmissionStatus.DISCHARGED -> Color(0xFFE2E3E5)
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header card
                    SectionCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = record.patientName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Ref: ${record.referenceCode()} • ${hospital?.name ?: record.hospitalId}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Submitted ${DateUtils.format(record.createdAtMillis)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusChip(
                                text = record.status.label,
                                containerColor = statusColor
                            )
                        }

                        // Assigned Room Banner (Only when assignedRoom is non-blank)
                        if (!record.assignedRoom.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MeetingRoom,
                                        contentDescription = "Room",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Assigned Room: ${record.assignedRoom}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Text(
                                            text = "Bed confirmed by hospital administration",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                            }
                        }

                        // Administrative Remarks / Rejection Reason
                        if (!record.adminRemark.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val isRejected = record.status == AdmissionStatus.REJECTED
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isRejected) {
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = if (isRejected) "Rejection Reason:" else "Hospital Admin Remark:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isRejected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = record.adminRemark!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

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

                    Spacer(modifier = Modifier.height(16.dp))

                    // Patient Details & Emergency Contacts
                    SectionCard(title = "Patient Information & Contacts") {
                        DetailItemRow("Full Name", record.patientName)
                        DetailItemRow("Age / Gender", "${record.patientAge} years, ${record.patientGender}")
                        DetailItemRow("Blood Group", record.patientBloodGroup)
                        DetailItemRow("Primary Phone", record.patientPhone)
                        DetailItemRow("Address", record.patientAddress)
                        DetailItemRow("Emergency Contact", "${record.emergencyContactName} (${record.emergencyContactPhone})")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

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

                    Spacer(modifier = Modifier.height(16.dp))

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

                    Spacer(modifier = Modifier.height(16.dp))

                    // Forwarded Medical Summary
                    SectionCard(title = "Forwarded Medical Portfolio") {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = record.sharedPortfolioSummary.ifBlank { "No previous medical summary forwarded." },
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

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
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Reject")
                                }

                                Button(
                                    onClick = onApproveClick,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Approve")
                                }
                            }
                        }
                        AdmissionStatus.APPROVED -> {
                            Button(
                                onClick = onDischargeClick,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Mark Discharged")
                            }
                        }
                        AdmissionStatus.REJECTED -> {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
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
                                shape = RoundedCornerShape(8.dp),
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

@Composable
private fun DetailItemRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
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
    specialties = listOf("Cardiology", "Orthopedics"),
    facilities = listOf("24/7 ICU", "Pharmacy"),
    emergency24x7 = true,
    roomTypes = emptyList(),
    aboutHtml = "<p>Lilavati Hospital</p>",
    accentColorHex = "#1E88E5"
)

private val previewPendingAdmission = Admission(
    id = "ADM001-PND",
    patientId = "P001",
    patientName = "Rahul Sharma",
    hospitalId = "HOSP001",
    reasonForAdmission = "Cardiac evaluation and elective stenting procedure",
    referringDoctor = "Dr. Amit Roy",
    preferredDateMillis = System.currentTimeMillis() + 86400000L * 2,
    expectedStayDays = 3,
    roomTypeName = "Deluxe Room",
    pricePerDay = 6500,
    estimatedCost = 19500,
    patientAge = 42,
    gender = "Male",
    bloodGroup = "O+",
    phone = "+91 98200 12345",
    address = "402 Palm Heights, Andheri West, Mumbai",
    emergencyContactName = "Priya Sharma",
    emergencyContactPhone = "+91 98200 54321",
    allergies = listOf("Penicillin", "Sulfa drugs"),
    chronicConditions = listOf("Hypertension"),
    currentMedications = listOf("Amlodipine 5mg", "Aspirin 75mg"),
    sharedPortfolioText = "Patient has a 5-year history of controlled hypertension. Recent ECG showed mild ST-T changes. Routine follow-up scheduled.",
    insurance = InsuranceDetails(
        providerName = "Star Health Insurance",
        policyNumber = "SH12345678",
        policyHolderName = "Rahul Sharma",
        validTillMillis = System.currentTimeMillis() + 86400000L * 300,
        sumInsured = 500000
    ),
    status = AdmissionStatus.PENDING,
    assignedRoomNumber = null,
    adminRemark = null,
    createdAtMillis = System.currentTimeMillis() - 86400000L
)

private val previewApprovedAdmission = previewPendingAdmission.copy(
    id = "ADM002-APP",
    status = AdmissionStatus.APPROVED,
    assignedRoomNumber = "Bed 402-A",
    adminRemark = "Admitted under Dr. Amit Roy. Pre-procedure fasting required from midnight."
)

private val previewRejectedAdmission = previewPendingAdmission.copy(
    id = "ADM003-REJ",
    status = AdmissionStatus.REJECTED,
    assignedRoomNumber = null,
    adminRemark = "Requested Deluxe accommodation is fully occupied for the chosen date. Please select another date or room type."
)

private val previewDischargedAdmission = previewApprovedAdmission.copy(
    id = "ADM004-DIS",
    status = AdmissionStatus.DISCHARGED
)

@Preview(showBackground = true, name = "Pending Admission (Adjudication Actions)")
@Composable
fun AdminAdmissionReviewScreenPendingPreview() {
    CareConnectTheme {
        AdminAdmissionReviewContent(
            uiState = AdminAdmissionReviewUiState.Success(previewPendingAdmission),
            onNavigateBack = {},
            onRetry = {},
            hospitalLookup = { previewMockHospital }
        )
    }
}

@Preview(showBackground = true, name = "Approved Admission (Discharge Action)")
@Composable
fun AdminAdmissionReviewScreenApprovedPreview() {
    CareConnectTheme {
        AdminAdmissionReviewContent(
            uiState = AdminAdmissionReviewUiState.Success(previewApprovedAdmission),
            onNavigateBack = {},
            onRetry = {},
            hospitalLookup = { previewMockHospital }
        )
    }
}

@Preview(showBackground = true, name = "Rejected Admission")
@Composable
fun AdminAdmissionReviewScreenRejectedPreview() {
    CareConnectTheme {
        AdminAdmissionReviewContent(
            uiState = AdminAdmissionReviewUiState.Success(previewRejectedAdmission),
            onNavigateBack = {},
            onRetry = {},
            hospitalLookup = { previewMockHospital }
        )
    }
}

@Preview(showBackground = true, name = "Discharged Admission")
@Composable
fun AdminAdmissionReviewScreenDischargedPreview() {
    CareConnectTheme {
        AdminAdmissionReviewContent(
            uiState = AdminAdmissionReviewUiState.Success(previewDischargedAdmission),
            onNavigateBack = {},
            onRetry = {},
            hospitalLookup = { previewMockHospital }
        )
    }
}

@Preview(showBackground = true, name = "Unauthorized Access")
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

@Preview(showBackground = true, name = "Unauthenticated Session")
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

@Preview(showBackground = true, name = "Record Not Found")
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

@Preview(showBackground = true, name = "Default Preview")
@Composable
fun AdminAdmissionReviewScreenPreview() {
    AdminAdmissionReviewScreenPendingPreview()
}
