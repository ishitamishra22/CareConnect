package com.nmims.careconnect.ui.admission

/*
 * AdmissionSuccessScreen:
 * Confirmation destination displayed immediately following an admission request submission:
 * - Success check icon and "Request Submitted" banner
 * - Prominent admission reference code card (first 8 chars of ID in uppercase) and live status chip
 * - Comprehensive admission details: patient, hospital, location, room category & daily rate,
 *   preferred date, stay duration, estimated cost, insurance/self-pay summary, and referring doctor
 * - Notice explaining hospital review and room assignment workflow
 * - Explicit loading, error, and retry states with graceful fallback for missing metadata
 * - "View in My Admissions" and "Back to Home" navigation actions
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nmims.careconnect.data.model.Admission
import com.nmims.careconnect.data.model.AdmissionStatus
import com.nmims.careconnect.data.model.InsuranceDetails
import com.nmims.careconnect.data.repository.AdmissionRepositoryProvider
import com.nmims.careconnect.data.repository.HospitalRepository
import com.nmims.careconnect.ui.common.CareTopBar
import com.nmims.careconnect.ui.common.DateUtils
import com.nmims.careconnect.ui.common.InfoRow
import com.nmims.careconnect.ui.common.LoadingIndicator
import com.nmims.careconnect.ui.common.PrimaryButton
import com.nmims.careconnect.ui.common.SectionCard
import com.nmims.careconnect.ui.common.StatusChip
import com.nmims.careconnect.ui.common.admissionStatusContainerColor
import com.nmims.careconnect.ui.theme.CareConnectTheme

@Composable
fun AdmissionSuccessScreen(
    admissionId: String,
    onNavigateToMyAdmissions: () -> Unit,
    onNavigateToHome: () -> Unit,
    onViewAdmissionDetail: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val repository = remember(context) { AdmissionRepositoryProvider.get(context) }
    val isPreview = LocalInspectionMode.current

    var admission by remember { mutableStateOf<Admission?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }
    var retryTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(admissionId, retryTrigger) {
        if (isPreview) {
            admission = Admission(
                id = "adm-preview-12345",
                patientId = "patient_1",
                patientName = "Rahul Sharma",
                hospitalId = "hosp_1",
                reasonForAdmission = "Severe respiratory distress requiring inpatient observation",
                referringDoctor = "Dr. Amit Verma",
                preferredDateMillis = System.currentTimeMillis() + 86400000L,
                expectedStayDays = 3,
                roomTypeName = "Deluxe Room",
                pricePerDay = 6000,
                estimatedCost = 18000,
                patientAge = 32,
                gender = "Male",
                bloodGroup = "B+",
                phone = "9876543210",
                address = "Bandra West, Mumbai",
                emergencyContactName = "Priya Sharma",
                emergencyContactPhone = "9876543211",
                insurance = InsuranceDetails(
                    providerName = "Star Health Insurance",
                    policyNumber = "POL-98765432",
                    policyHolderName = "Rahul Sharma",
                    validTillMillis = System.currentTimeMillis() + 31536000000L,
                    sumInsured = 500000
                ),
                status = AdmissionStatus.PENDING,
                createdAtMillis = System.currentTimeMillis()
            )
            isLoading = false
            isError = false
            return@LaunchedEffect
        }

        if (admissionId.isBlank()) {
            isLoading = false
            isError = true
            return@LaunchedEffect
        }

        isLoading = true
        isError = false
        try {
            val record = repository.getById(admissionId)
            if (record != null) {
                admission = record
            } else {
                isError = true
            }
        } catch (_: Exception) {
            isError = true
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            CareTopBar(
                title = "Admission Confirmation",
                onBack = onNavigateToHome
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isLoading -> {
                    LoadingIndicator(modifier = Modifier.align(Alignment.Center))
                }
                isError || admission == null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Error",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Admission Details Unavailable",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "We could not find or load the requested admission record. Please check your connection and try again.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        PrimaryButton(
                            text = "Retry",
                            onClick = {
                                if (!isLoading) {
                                    retryTrigger++
                                }
                            },
                            enabled = !isLoading
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onNavigateToHome,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = "Back to Home",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                else -> {
                    val record = admission!!
                    val hospital = remember(record.hospitalId) {
                        HospitalRepository.getHospital(record.hospitalId)
                    }

                    val statusColor = admissionStatusContainerColor(record.status)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Success Confirmation Icon Banner
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(52.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Request Submitted!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Your inpatient hospital admission request has been sent to the hospital desk.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Reference Code Highlight Card
                        SectionCard {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "ADMISSION REFERENCE CODE",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = record.referenceCode(),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(record.referenceCode()))
                                            Toast.makeText(
                                                context,
                                                "Reference code copied to clipboard",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy Reference Code",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                StatusChip(
                                    text = record.status.label,
                                    containerColor = statusColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Persisted Admission Details Section
                        SectionCard(title = "Admission Details") {
                            InfoRow(
                                label = "Patient Name",
                                value = record.patientName
                            )
                            InfoRow(
                                label = "Partner Hospital",
                                value = hospital?.name ?: record.hospitalId
                            )
                            if (hospital != null) {
                                InfoRow(
                                    label = "Location",
                                    value = "${hospital.area}, ${hospital.city}"
                                )
                            }
                            InfoRow(
                                label = "Room Category",
                                value = "${record.roomType} (₹${record.roomPricePerDay}/day)"
                            )
                            InfoRow(
                                label = "Admission Date",
                                value = DateUtils.formatWithDay(record.preferredDateMillis)
                            )
                            InfoRow(
                                label = "Expected Stay",
                                value = "${record.expectedStayDays} ${if (record.expectedStayDays == 1) "day" else "days"}"
                            )
                            InfoRow(
                                label = "Estimated Total Cost",
                                value = "₹${record.estimatedCost}"
                            )

                            val insurance = record.insuranceDetails
                            val insuranceDisplay = if (insurance != null) {
                                "${insurance.providerName} (Pol: ${insurance.policyNumber})"
                            } else {
                                "Self-Paying Patient"
                            }
                            InfoRow(
                                label = "Insurance Coverage",
                                value = insuranceDisplay
                            )

                            if (!record.referringDoctor.isNullOrBlank()) {
                                InfoRow(
                                    label = "Referring Doctor",
                                    value = record.referringDoctor!!
                                )
                            }

                            if (record.reason.isNotBlank()) {
                                InfoRow(
                                    label = "Reason for Admission",
                                    value = record.reason
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Hospital Review Workflow Notice Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "The hospital administration team will review your medical summary, verify bed availability, and assign your room. You can track real-time bed allotment and approval status in My Admissions.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        PrimaryButton(
                            text = "View Admission Details",
                            onClick = { onViewAdmissionDetail(record.id) }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = onNavigateToMyAdmissions,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = "View in My Admissions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = onNavigateToHome,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = "Back to Home",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AdmissionSuccessScreenPreview() {
    CareConnectTheme {
        AdmissionSuccessScreen(
            admissionId = "preview123",
            onNavigateToMyAdmissions = {},
            onNavigateToHome = {},
            onViewAdmissionDetail = {}
        )
    }
}
