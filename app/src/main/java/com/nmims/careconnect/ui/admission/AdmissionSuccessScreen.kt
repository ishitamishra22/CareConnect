package com.nmims.careconnect.ui.admission

/*
 * AdmissionSuccessScreen:
 * Confirmation destination displayed immediately following an admission request submission:
 * - Success check icon and "Request Submitted" banner
 * - Admission reference code (first 8 chars of ID in uppercase)
 * - Notice explaining hospital review and room assignment workflow
 * - Summary card showing hospital, room category, admission date, estimated cost, and status chip
 * - "My Admissions" and "Back to Home" action buttons
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.nmims.careconnect.data.repository.AdmissionRepositoryProvider
import com.nmims.careconnect.data.repository.HospitalRepository
import com.nmims.careconnect.ui.common.CareTopBar
import com.nmims.careconnect.ui.common.DateUtils
import com.nmims.careconnect.ui.common.LoadingIndicator
import com.nmims.careconnect.ui.common.SectionCard
import com.nmims.careconnect.ui.common.StatusChip
import com.nmims.careconnect.ui.theme.CareConnectTheme

@Composable
fun AdmissionSuccessScreen(
    admissionId: String,
    onNavigateToMyAdmissions: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember(context) { AdmissionRepositoryProvider.get(context) }

    var admission by remember { mutableStateOf<Admission?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(admissionId) {
        isLoading = true
        admission = repository.getById(admissionId)
        isLoading = false
    }

    Scaffold(
        topBar = {
            CareTopBar(
                title = "Request Confirmation",
                onBack = onNavigateToHome
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator()
            }
        } else {
            val record = admission
            val hospital = record?.let { HospitalRepository.getHospital(it.hospitalId) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
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
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Request Submitted",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (record != null) {
                    Text(
                        text = "Reference Code: ${record.referenceCode()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Your admission request has been sent to the hospital desk. The administration team will review your health summary, confirm bed availability, and assign your room number.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (record != null) {
                    SectionCard(title = "Admission Details") {
                        DetailRow("Hospital", hospital?.name ?: record.hospitalId)
                        DetailRow("Room Category", record.roomType)
                        DetailRow("Preferred Date", DateUtils.formatWithDay(record.preferredDateMillis))
                        DetailRow("Expected Stay", "${record.expectedStayDays} days")
                        DetailRow("Estimated Cost", "₹${record.estimatedCost}")

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Current Status",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val statusColor = when (record.status) {
                                AdmissionStatus.PENDING -> Color(0xFFFFF3CD)
                                AdmissionStatus.APPROVED -> Color(0xFFD1E7DD)
                                AdmissionStatus.REJECTED -> Color(0xFFF8D7DA)
                                AdmissionStatus.DISCHARGED -> Color(0xFFE2E3E5)
                            }
                            StatusChip(
                                text = record.status.label,
                                containerColor = statusColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onNavigateToMyAdmissions,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("My Admissions")
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onNavigateToHome,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Back to Home")
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AdmissionSuccessScreenPreview() {
    CareConnectTheme {
        AdmissionSuccessScreen(
            admissionId = "preview123",
            onNavigateToMyAdmissions = {},
            onNavigateToHome = {}
        )
    }
}
