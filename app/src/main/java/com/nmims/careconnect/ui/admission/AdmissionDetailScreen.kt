package com.nmims.careconnect.ui.admission

/*
 * AdmissionDetailScreen:
 * Comprehensive view of a single admission request for the patient or hospital desk:
 * - Loads admission record asynchronously by ID from AdmissionRepository
 * - Validates patient ownership and hospital admin role authorization
 * - Distinguishes unauthenticated, unauthorized, loading, missing-record, and error states
 * - Displays prominent status banner (Pending, Approved with assigned room, Rejected with remark, Discharged)
 * - Hospital information: Name, area/city, full address, contact telephone, and 24/7 emergency tag
 * - Admission & accommodation details: Room category, daily rate, preferred date, stay duration, and cost breakdown
 * - Insurance coverage summary or self-pay indication
 * - Patient demographics, emergency contact, clinical history (allergies, chronic conditions, medications with explicit "None reported")
 * - Forwarded Medical Portfolio summary
 * - Extracted stateless AdmissionDetailContent with isolated Compose Previews
 */

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
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
import com.nmims.careconnect.ui.common.admissionStatusContainerColor
import com.nmims.careconnect.ui.theme.CareConnectTheme

sealed interface AdmissionDetailUiState {
    object Loading : AdmissionDetailUiState
    object Unauthenticated : AdmissionDetailUiState
    object Unauthorized : AdmissionDetailUiState
    object NotFound : AdmissionDetailUiState
    data class Error(val message: String) : AdmissionDetailUiState
    data class Success(val admission: Admission) : AdmissionDetailUiState
}

@Composable
fun AdmissionDetailScreen(
    admissionId: String,
    onNavigateBack: () -> Unit,
    onHospitalClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember(context) { AdmissionRepositoryProvider.get(context) }

    var uiState by remember { mutableStateOf<AdmissionDetailUiState>(AdmissionDetailUiState.Loading) }
    var retryTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(admissionId, retryTrigger) {
        if (admissionId.isBlank()) {
            uiState = AdmissionDetailUiState.NotFound
            return@LaunchedEffect
        }

        uiState = AdmissionDetailUiState.Loading

        try {
            val record = repository.getById(admissionId)
            if (record == null) {
                uiState = AdmissionDetailUiState.NotFound
            } else {
                val currentPatientId = AppSession.currentPatientId
                val currentRole = AppSession.currentRole
                val currentHospitalId = AppSession.currentHospitalId

                // Exact identity verification (preserves established project convention)
                val isPatientOwner = currentPatientId.isNotBlank() && record.patientId == currentPatientId

                // Hospital-admin access: role is HOSPITAL_ADMIN and assigned hospital matches admission hospital
                val isAdminAuthorized = currentRole == UserRole.HOSPITAL_ADMIN &&
                        !currentHospitalId.isNullOrBlank() &&
                        record.hospitalId == currentHospitalId

                when {
                    isPatientOwner || isAdminAuthorized -> {
                        uiState = AdmissionDetailUiState.Success(record)
                    }
                    currentPatientId.isBlank() && currentRole == null && currentHospitalId.isNullOrBlank() -> {
                        uiState = AdmissionDetailUiState.Unauthenticated
                    }
                    else -> {
                        uiState = AdmissionDetailUiState.Unauthorized
                    }
                }
            }
        } catch (e: Exception) {
            uiState = AdmissionDetailUiState.Error(
                e.localizedMessage ?: "Unable to load admission details. Please check your network connection and try again."
            )
        }
    }

    AdmissionDetailContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onRetry = { retryTrigger++ },
        onHospitalClick = onHospitalClick,
        hospitalLookup = { hospitalId -> HospitalRepository.getHospital(hospitalId) }
    )
}

@Composable
fun AdmissionDetailContent(
    uiState: AdmissionDetailUiState,
    onNavigateBack: () -> Unit,
    onRetry: () -> Unit,
    onHospitalClick: (String) -> Unit = {},
    hospitalLookup: (String) -> Hospital? = { HospitalRepository.getHospital(it) },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    Scaffold(
        topBar = {
            CareTopBar(
                title = "Admission Details",
                onBack = onNavigateBack
            )
        },
        modifier = modifier
    ) { innerPadding ->
        when (uiState) {
            is AdmissionDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator()
                }
            }
            is AdmissionDetailUiState.Unauthenticated -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = Icons.Default.AccountCircle,
                        message = "Patient Sign-In Required\n\nPlease sign in with an authorized patient or hospital account to view this inpatient admission request.",
                        action = {
                            Button(onClick = onNavigateBack) {
                                Text("Go Back")
                            }
                        }
                    )
                }
            }
            is AdmissionDetailUiState.Unauthorized -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = Icons.Default.Block,
                        message = "Access Restricted\n\nYou are not authorized to view this admission request. Please ensure you are signed in with the correct account.",
                        action = {
                            Button(onClick = onNavigateBack) {
                                Text("Go Back")
                            }
                        }
                    )
                }
            }
            is AdmissionDetailUiState.NotFound -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = Icons.Default.SearchOff,
                        message = "Admission Record Not Found\n\nThe requested inpatient admission request could not be found. It may have been removed or the ID is invalid.",
                        action = {
                            Button(onClick = onNavigateBack) {
                                Text("Back to Admissions")
                            }
                        }
                    )
                }
            }
            is AdmissionDetailUiState.Error -> {
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
            is AdmissionDetailUiState.Success -> {
                val record = uiState.admission
                val hospital = remember(record.hospitalId) { hospitalLookup(record.hospitalId) }
                val statusColor = admissionStatusContainerColor(record.status)

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
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = hospital?.name ?: record.hospitalId,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val hospitalLocation = if (hospital != null && hospital.area.isNotBlank()) {
                                    "${hospital.area}, ${hospital.city}"
                                } else {
                                    hospital?.city ?: ""
                                }
                                if (hospitalLocation.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = hospitalLocation,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Ref: ${record.referenceCode()} • Submitted ${DateUtils.format(record.createdAtMillis)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(record.referenceCode()))
                                            Toast.makeText(
                                                context,
                                                "Reference code copied to clipboard",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy Reference Code",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            StatusChip(
                                text = record.status.label,
                                containerColor = statusColor
                            )
                        }

                        // Lifecycle Status Banners
                        when (record.status) {
                            AdmissionStatus.APPROVED -> {
                                if (!record.assignedRoom.isNullOrBlank()) {
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
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
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
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            AdmissionStatus.PENDING -> {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.HourglassTop,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Admission Under Review",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                            Text(
                                                text = "Hospital administration is reviewing clinical details and preparing bed allotment.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                                            )
                                        }
                                    }
                                }
                            }
                            AdmissionStatus.REJECTED -> {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ErrorOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier
                                                .size(22.dp)
                                                .padding(top = 2.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Admission Request Rejected",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                            if (!record.adminRemark.isNullOrBlank()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Reason: ${record.adminRemark}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            } else {
                                                Text(
                                                    text = "This admission request could not be accommodated by hospital administration.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            AdmissionStatus.DISCHARGED -> {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Patient Discharged",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Inpatient stay concluded and discharge formalities completed.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Hospital Remark for non-rejected statuses if present
                        if (record.status != AdmissionStatus.REJECTED && !record.adminRemark.isNullOrBlank()) {
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
                                        text = record.adminRemark!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Hospital Facility Information
                    SectionCard(title = "Hospital Facility & Contact") {
                        if (hospital != null) {
                            DetailItem("Hospital Name", hospital.name)
                            DetailItem("Location", "${hospital.area}, ${hospital.city}")
                            if (hospital.address.isNotBlank()) {
                                DetailItem("Address", hospital.address)
                            }
                            if (hospital.phone.isNotBlank()) {
                                DetailItem("Contact Phone", hospital.phone)
                            }
                            if (hospital.emergency24x7) {
                                DetailItem("Emergency Care", "24/7 Emergency Available")
                            }

                            val cleanPhone = hospital.phone.replace(Regex("[^0-9+]"), "")
                            val hasValidPhone = cleanPhone.count { it.isDigit() } >= 7

                            Spacer(modifier = Modifier.height(12.dp))

                            if (hasValidPhone) {
                                Button(
                                    onClick = { launchDialer(context, cleanPhone) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "Call Desk",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Call Desk", style = MaterialTheme.typography.labelLarge)
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            OutlinedButton(
                                onClick = { onHospitalClick(record.hospitalId) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalHospital,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("View Hospital Profile", style = MaterialTheme.typography.labelLarge)
                            }
                        } else {
                            DetailItem("Hospital ID", record.hospitalId)
                            DetailItem("Facility Status", "Directory information unavailable offline")

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = { onHospitalClick(record.hospitalId) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalHospital,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("View Hospital Profile", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Admission & Room Details
                    SectionCard(title = "Admission & Accommodation") {
                        DetailItem("Reason for Admission", record.reason)
                        if (!record.referringDoctor.isNullOrBlank()) {
                            DetailItem("Referring Doctor", record.referringDoctor!!)
                        }
                        DetailItem("Room Category", record.roomType)
                        DetailItem("Daily Room Rate", "₹${record.roomPricePerDay} / day")
                        DetailItem("Preferred Date", DateUtils.formatWithDay(record.preferredDateMillis))
                        DetailItem("Expected Stay", "${record.expectedStayDays} ${if (record.expectedStayDays == 1) "day" else "days"}")

                        // Present calculation breakdown only when it accurately represents the persisted total
                        if (record.roomPricePerDay * record.expectedStayDays == record.estimatedCost) {
                            DetailItem("Stay Calculation", "₹${record.roomPricePerDay} × ${record.expectedStayDays} days")
                        }
                        DetailItem("Estimated Cost", "₹${record.estimatedCost}")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Insurance Details
                    SectionCard(title = "Insurance & Billing") {
                        val ins = record.insuranceDetails
                        if (ins != null) {
                            DetailItem("Billing Mode", "Health Insurance Claim")
                            DetailItem("Insurance Provider", ins.providerName)
                            DetailItem("Policy Number", ins.policyNumber)
                            DetailItem("Policyholder", ins.policyholderName)
                            DetailItem("Valid Till", DateUtils.format(ins.validTillMillis))
                            DetailItem("Sum Insured", "₹${ins.sumInsured}")
                        } else {
                            DetailItem("Billing Mode", "Self-Pay Inpatient")
                            DetailItem("Insurance Policy", "No health insurance attached. Patient self-pay.")
                        }
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

                        val validEmergencyPhone = remember(record.emergencyContactPhone) {
                            sanitizeAndValidatePhone(record.emergencyContactPhone)
                        }
                        val validPatientPhone = remember(record.patientPhone) {
                            sanitizeAndValidatePhone(record.patientPhone)
                        }

                        if (validEmergencyPhone != null || validPatientPhone != null) {
                            Spacer(modifier = Modifier.height(12.dp))

                            if (validEmergencyPhone != null) {
                                Button(
                                    onClick = { launchDialer(context, validEmergencyPhone) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "Call Emergency Contact",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Call Emergency Contact", style = MaterialTheme.typography.labelLarge)
                                }
                            }

                            if (validEmergencyPhone != null && validPatientPhone != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            if (validPatientPhone != null) {
                                OutlinedButton(
                                    onClick = { launchDialer(context, validPatientPhone) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "Call Patient",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Call Patient", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Clinical Background (Explicitly indicates "None reported" when empty)
                    SectionCard(title = "Clinical Background") {
                        DetailItem(
                            label = "Known Allergies",
                            value = if (record.allergies.isNotEmpty()) record.allergies.joinToString(", ") else "None reported"
                        )
                        DetailItem(
                            label = "Chronic Conditions",
                            value = if (record.chronicConditions.isNotEmpty()) record.chronicConditions.joinToString(", ") else "None reported"
                        )
                        DetailItem(
                            label = "Current Medications",
                            value = if (record.currentMedications.isNotEmpty()) record.currentMedications.joinToString(", ") else "None reported"
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Shared Portfolio Summary
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

                    Spacer(modifier = Modifier.height(20.dp))

                    // Back to Admissions Action
                    OutlinedButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Back to Admissions")
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
    val cleaned = raw.trim().replace(Regex("[\\s\\-().]"), "")
    if (!cleaned.all { it.isDigit() || it == '+' }) return null
    val plusCount = cleaned.count { it == '+' }
    if (plusCount > 1 || (plusCount == 1 && !cleaned.startsWith("+"))) return null
    val digitCount = cleaned.count { it.isDigit() }
    if (digitCount < 7) return null
    if (cleaned.startsWith("+") && !cleaned.substring(1).all { it.isDigit() }) return null
    return cleaned
}

/**
 * Dispatches an implicit Intent.ACTION_DIAL intent with tel: URI.
 * Safely catches ActivityNotFoundException and other execution failures without crashing.
 */
private fun launchDialer(context: Context, phoneNumber: String) {
    try {
        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }
        context.startActivity(dialIntent)
    } catch (_: Exception) {
        Toast.makeText(
            context,
            "Unable to launch phone dialer",
            Toast.LENGTH_SHORT
        ).show()
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

private val previewApprovedAdmission = Admission(
    id = "ADM001-XYZ",
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
    status = AdmissionStatus.APPROVED,
    assignedRoomNumber = "Bed 402-A",
    adminRemark = "Admitted under Dr. Amit Roy. Pre-procedure fasting required from midnight.",
    createdAtMillis = System.currentTimeMillis() - 86400000L
)

private val previewPendingAdmission = previewApprovedAdmission.copy(
    id = "ADM002-PND",
    status = AdmissionStatus.PENDING,
    assignedRoomNumber = null,
    adminRemark = null
)

private val previewRejectedAdmission = previewApprovedAdmission.copy(
    id = "ADM003-REJ",
    status = AdmissionStatus.REJECTED,
    assignedRoomNumber = null,
    adminRemark = "Requested Deluxe accommodation is fully occupied for the chosen date. Please select another date or room type."
)

@Preview(showBackground = true, name = "Approved Admission")
@Composable
fun AdmissionDetailScreenApprovedPreview() {
    CareConnectTheme {
        AdmissionDetailContent(
            uiState = AdmissionDetailUiState.Success(previewApprovedAdmission),
            onNavigateBack = {},
            onRetry = {},
            hospitalLookup = { previewMockHospital }
        )
    }
}

@Preview(showBackground = true, name = "Pending Admission")
@Composable
fun AdmissionDetailScreenPendingPreview() {
    CareConnectTheme {
        AdmissionDetailContent(
            uiState = AdmissionDetailUiState.Success(previewPendingAdmission),
            onNavigateBack = {},
            onRetry = {},
            hospitalLookup = { previewMockHospital }
        )
    }
}

@Preview(showBackground = true, name = "Rejected Admission")
@Composable
fun AdmissionDetailScreenRejectedPreview() {
    CareConnectTheme {
        AdmissionDetailContent(
            uiState = AdmissionDetailUiState.Success(previewRejectedAdmission),
            onNavigateBack = {},
            onRetry = {},
            hospitalLookup = { previewMockHospital }
        )
    }
}

@Preview(showBackground = true, name = "Record Not Found")
@Composable
fun AdmissionDetailScreenNotFoundPreview() {
    CareConnectTheme {
        AdmissionDetailContent(
            uiState = AdmissionDetailUiState.NotFound,
            onNavigateBack = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true, name = "Access Restricted")
@Composable
fun AdmissionDetailScreenUnauthorizedPreview() {
    CareConnectTheme {
        AdmissionDetailContent(
            uiState = AdmissionDetailUiState.Unauthorized,
            onNavigateBack = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true, name = "Sign-In Required")
@Composable
fun AdmissionDetailScreenUnauthenticatedPreview() {
    CareConnectTheme {
        AdmissionDetailContent(
            uiState = AdmissionDetailUiState.Unauthenticated,
            onNavigateBack = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true, name = "Default Preview")
@Composable
fun AdmissionDetailScreenPreview() {
    AdmissionDetailScreenApprovedPreview()
}
