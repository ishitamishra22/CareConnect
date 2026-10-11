package com.nmims.careconnect.ui.admission

/*
 * AdmissionDetailScreen:
 * Comprehensive view of a single admission request for the patient or hospital desk:
 * - Loads admission record by ID from AdmissionRepository
 * - Validates patient ownership / role authorization before rendering
 * - Displays status chip, assigned room number, admin remark (if approved/rejected)
 * - Details: Patient bio, contact info, reason, referring doctor, room category, dates, cost breakdown
 * - Insurance coverage summary (provider, policy no, validity date, sum insured)
 * - Embedded shareable medical portfolio summary sent with request
 * - Comprehensive loading, missing record, and unauthorized access states
 */

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
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

@Composable
fun AdmissionDetailScreen(
    admissionId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember(context) { AdmissionRepositoryProvider.get(context) }

    var admission by remember { mutableStateOf<Admission?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isUnauthorized by remember { mutableStateOf(false) }

    LaunchedEffect(admissionId) {
        isLoading = true
        val item = repository.getById(admissionId)
        if (item != null) {
            // Patient authorization check: must match current patient or be hospital admin
            val isPatientOwner = item.patientId.equals(AppSession.currentPatientId, ignoreCase = true)
            val isAdmin = AppSession.currentRole == UserRole.HOSPITAL_ADMIN &&
                    (AppSession.currentHospitalId == null || item.hospitalId == AppSession.currentHospitalId)

            if (!isPatientOwner && !isAdmin) {
                isUnauthorized = true
            } else {
                admission = item
            }
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            CareTopBar(
                title = "Admission Details",
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
                        message = "You are not authorized to view this admission request."
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
                        message = "Admission record not found."
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
                    // Header card with Status and Reference Code
                    SectionCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = hospital?.name ?: record.hospitalId,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Ref: ${record.referenceCode()} • Created ${DateUtils.format(record.createdAtMillis)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusChip(
                                text = record.status.label,
                                containerColor = statusColor
                            )
                        }

                        // Highlight assigned room if approved
                        if (record.status == AdmissionStatus.APPROVED && !record.assignedRoom.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
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
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Assigned Room: ${record.assignedRoom}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Text(
                                            text = "Room allotment confirmed by hospital administration",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }

                        // Admin remark if present
                        if (!record.adminRemark.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "Hospital Admin Remark:",
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

                    // Admission & Room Details
                    SectionCard(title = "Admission & Accommodation") {
                        DetailItem("Reason for Admission", record.reason)
                        if (!record.referringDoctor.isNullOrBlank()) {
                            DetailItem("Referring Doctor", record.referringDoctor)
                        }
                        DetailItem("Room Category", record.roomType)
                        DetailItem("Room Rate", "₹${record.roomPricePerDay} / day")
                        DetailItem("Preferred Date", DateUtils.formatWithDay(record.preferredDateMillis))
                        DetailItem("Expected Stay", "${record.expectedStayDays} days")
                        DetailItem("Estimated Cost", "₹${record.estimatedCost}")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Patient Details
                    SectionCard(title = "Patient Information") {
                        DetailItem("Patient Name", record.patientName)
                        DetailItem("Age / Gender", "${record.patientAge} yrs, ${record.patientGender}")
                        DetailItem("Blood Group", record.patientBloodGroup)
                        DetailItem("Phone", record.patientPhone)
                        DetailItem("Address", record.patientAddress)
                        DetailItem("Emergency Contact", "${record.emergencyContactName} (${record.emergencyContactPhone})")
                        if (record.allergies.isNotEmpty()) {
                            DetailItem("Known Allergies", record.allergies.joinToString(", "))
                        }
                        if (record.chronicConditions.isNotEmpty()) {
                            DetailItem("Chronic Conditions", record.chronicConditions.joinToString(", "))
                        }
                        if (record.currentMedications.isNotEmpty()) {
                            DetailItem("Current Medications", record.currentMedications.joinToString(", "))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Insurance Details
                    SectionCard(title = "Insurance Coverage") {
                        val ins = record.insuranceDetails
                        if (ins != null) {
                            DetailItem("Provider", ins.providerName)
                            DetailItem("Policy Number", ins.policyNumber)
                            DetailItem("Policyholder", ins.policyholderName)
                            DetailItem("Valid Till", DateUtils.format(ins.validityDateMillis))
                            DetailItem("Sum Insured", "₹${ins.sumInsured}")
                        } else {
                            Text(
                                text = "No health insurance attached. Self-pay admission.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Shared Portfolio Summary
                    SectionCard(title = "Forwarded Medical Summary") {
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
                }
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
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
fun AdmissionDetailScreenPreview() {
    CareConnectTheme {
        AdmissionDetailScreen(
            admissionId = "preview123",
            onNavigateBack = {}
        )
    }
}
