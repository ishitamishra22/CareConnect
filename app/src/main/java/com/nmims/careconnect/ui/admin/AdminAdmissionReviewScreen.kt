package com.nmims.careconnect.ui.admin

/*
 * AdminAdmissionReviewScreen:
 * Hospital administrator interface for evaluating and adjudicating admission requests:
 * - Restricts access strictly to the hospital assigned to AppSession.currentHospitalId
 * - Displays complete patient profile, emergency contacts, allergies, chronic conditions, current medications
 * - Displays room accommodation preference, dates, estimated cost, and health insurance claim details
 * - Displays patient's shareable medical portfolio summary
 * - Actions:
 *   - Approve: opens dialog requiring room number (e.g. "Bed 402-A") and optional remark, updates status to APPROVED
 *   - Reject: opens dialog requiring rejection remark, updates status to REJECTED
 *   - Mark Discharged: visible for APPROVED requests, updates status to DISCHARGED
 * - Interacts via AdmissionRepository.updateStatus() with error handling and confirmation feedback
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.NoMeetingRoom
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.nmims.careconnect.data.repository.AdmissionRepositoryProvider
import com.nmims.careconnect.data.repository.HospitalRepository
import com.nmims.careconnect.session.AppSession
import com.nmims.careconnect.ui.common.CareTopBar
import com.nmims.careconnect.ui.common.ConfirmDialog
import com.nmims.careconnect.ui.common.DateUtils
import com.nmims.careconnect.ui.common.EmptyState
import com.nmims.careconnect.ui.common.LoadingIndicator
import com.nmims.careconnect.ui.common.SectionCard
import com.nmims.careconnect.ui.common.StatusChip
import com.nmims.careconnect.ui.theme.CareConnectTheme
import kotlinx.coroutines.launch

@Composable
fun AdminAdmissionReviewScreen(
    admissionId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember(context) { AdmissionRepositoryProvider.get(context) }

    var admission by remember { mutableStateOf<Admission?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isUnauthorized by remember { mutableStateOf(false) }

    // Dialog states
    var showApproveDialog by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var showDischargeDialog by remember { mutableStateOf(false) }

    var roomNumberInput by remember { mutableStateOf("") }
    var adminRemarkInput by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    fun refreshAdmission() {
        coroutineScope.launch {
            val record = repository.getById(admissionId)
            if (record != null) {
                val adminHospitalId = AppSession.currentHospitalId
                if (adminHospitalId != null && record.hospitalId != adminHospitalId) {
                    isUnauthorized = true
                } else {
                    admission = record
                }
            }
        }
    }

    LaunchedEffect(admissionId) {
        isLoading = true
        val record = repository.getById(admissionId)
        if (record != null) {
            val adminHospitalId = AppSession.currentHospitalId
            if (adminHospitalId != null && record.hospitalId != adminHospitalId) {
                isUnauthorized = true
            } else {
                admission = record
            }
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            CareTopBar(
                title = "Review Admission",
                onBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator()
                }
            }
            isUnauthorized -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = Icons.Default.Block,
                        message = "Unauthorized: You can only review admissions assigned to your hospital."
                    )
                }
            }
            admission == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = Icons.Default.SearchOff,
                        message = "Admission request not found."
                    )
                }
            }
            else -> {
                val record = admission!!
                val hospital = HospitalRepository.getHospital(record.hospitalId)
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
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
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
                            }
                            StatusChip(
                                text = record.status.label,
                                containerColor = statusColor
                            )
                        }

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
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Assigned Room: ${record.assignedRoom}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }

                        if (!record.adminRemark.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "Admin Remark:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = record.adminRemark,
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
                            DetailItemRow("Referring Doctor", record.referringDoctor)
                        }
                        DetailItemRow("Requested Room Type", record.roomType)
                        DetailItemRow("Room Price", "₹${record.roomPricePerDay} / day")
                        DetailItemRow("Preferred Date", DateUtils.formatWithDay(record.preferredDateMillis))
                        DetailItemRow("Expected Stay", "${record.expectedStayDays} days")
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
                        if (record.allergies.isNotEmpty()) {
                            DetailItemRow("Known Allergies", record.allergies.joinToString(", "))
                        }
                        if (record.chronicConditions.isNotEmpty()) {
                            DetailItemRow("Chronic Conditions", record.chronicConditions.joinToString(", "))
                        }
                        if (record.currentMedications.isNotEmpty()) {
                            DetailItemRow("Current Medications", record.currentMedications.joinToString(", "))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Insurance Details
                    SectionCard(title = "Insurance Coverage") {
                        val ins = record.insuranceDetails
                        if (ins != null) {
                            DetailItemRow("Provider", ins.providerName)
                            DetailItemRow("Policy Number", ins.policyNumber)
                            DetailItemRow("Policyholder", ins.policyholderName)
                            DetailItemRow("Valid Till", DateUtils.format(ins.validityDateMillis))
                            DetailItemRow("Sum Insured", "₹${ins.sumInsured}")
                        } else {
                            Text(
                                text = "Self-paying patient (No insurance details provided)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Forwarded Medical Summary
                    SectionCard(title = "Forwarded Medical Summary") {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = record.sharedPortfolioSummary.ifBlank { "No previous medical summary available." },
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action Buttons based on Status
                    when (record.status) {
                        AdmissionStatus.PENDING -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        adminRemarkInput = ""
                                        showRejectDialog = true
                                    },
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
                                    onClick = {
                                        roomNumberInput = ""
                                        adminRemarkInput = ""
                                        showApproveDialog = true
                                    },
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
                                onClick = { showDischargeDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Mark Discharged")
                            }
                        }
                        AdmissionStatus.REJECTED, AdmissionStatus.DISCHARGED -> {
                            // Already in final lifecycle state
                            Text(
                                text = "This admission request is ${record.status.label.lowercase()}.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Approve Dialog
    if (showApproveDialog) {
        AlertDialog(
            onDismissRequest = { if (!isProcessing) showApproveDialog = false },
            title = { Text("Approve Admission") },
            text = {
                Column {
                    Text(
                        text = "Assign a room / bed number to confirm this patient's admission request.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = roomNumberInput,
                        onValueChange = { roomNumberInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Room / Bed Number *") },
                        placeholder = { Text("e.g. Ward 3, Bed 305-B") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = adminRemarkInput,
                        onValueChange = { adminRemarkInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Admin Remark (Optional)") },
                        placeholder = { Text("e.g. Admitted under Dr. Sharma") },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (roomNumberInput.trim().isBlank()) {
                            Toast.makeText(context, "Please enter an assigned room number", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isProcessing = true
                        coroutineScope.launch {
                            val ok = repository.updateStatus(
                                id = admissionId,
                                status = AdmissionStatus.APPROVED,
                                roomNumber = roomNumberInput.trim(),
                                remark = adminRemarkInput.trim().takeIf { it.isNotBlank() }
                            )
                            isProcessing = false
                            showApproveDialog = false
                            if (ok) {
                                Toast.makeText(context, "Admission approved. Room assigned.", Toast.LENGTH_SHORT).show()
                                refreshAdmission()
                            } else {
                                Toast.makeText(context, "Could not reach the server. Check your internet connection.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = roomNumberInput.trim().isNotBlank() && !isProcessing
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

    // Reject Dialog
    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { if (!isProcessing) showRejectDialog = false },
            title = { Text("Reject Admission") },
            text = {
                Column {
                    Text(
                        text = "Provide a reason or remark for rejecting this admission request.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = adminRemarkInput,
                        onValueChange = { adminRemarkInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Rejection Remark *") },
                        placeholder = { Text("e.g. Bed unavailable in ICU, emergency triage redirect") },
                        minLines = 2,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (adminRemarkInput.trim().isBlank()) {
                            Toast.makeText(context, "Please provide a rejection remark", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isProcessing = true
                        coroutineScope.launch {
                            val ok = repository.updateStatus(
                                id = admissionId,
                                status = AdmissionStatus.REJECTED,
                                remark = adminRemarkInput.trim()
                            )
                            isProcessing = false
                            showRejectDialog = false
                            if (ok) {
                                Toast.makeText(context, "Admission rejected.", Toast.LENGTH_SHORT).show()
                                refreshAdmission()
                            } else {
                                Toast.makeText(context, "Could not reach the server. Check your internet connection.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = adminRemarkInput.trim().isNotBlank() && !isProcessing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(if (isProcessing) "Saving..." else "Reject Request")
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

    // Discharge Confirm Dialog
    if (showDischargeDialog) {
        ConfirmDialog(
            title = "Mark Patient Discharged?",
            message = "This will update the inpatient record to DISCHARGED and mark the stay completed.",
            confirmText = "Mark Discharged",
            onConfirm = {
                isProcessing = true
                coroutineScope.launch {
                    val ok = repository.updateStatus(
                        id = admissionId,
                        status = AdmissionStatus.DISCHARGED
                    )
                    isProcessing = false
                    showDischargeDialog = false
                    if (ok) {
                        Toast.makeText(context, "Patient marked as discharged.", Toast.LENGTH_SHORT).show()
                        refreshAdmission()
                    } else {
                        Toast.makeText(context, "Could not reach the server. Check your internet connection.", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onDismiss = { showDischargeDialog = false }
        )
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

@Preview(showBackground = true)
@Composable
fun AdminAdmissionReviewScreenPreview() {
    CareConnectTheme {
        AdminAdmissionReviewScreen(
            admissionId = "preview123",
            onNavigateBack = {}
        )
    }
}
