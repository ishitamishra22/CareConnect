package com.nmims.careconnect.ui.booking

/*
 * BookingDetailScreen:
 * Displays full details of an individual OPD appointment or Lab Test booking.
 * Enables cancellation for eligible upcoming bookings using BookingRepository.cancel().
 * Redesigned with clinical minimalism, status chips, structured detail cards,
 * copyable booking reference code, and doctor consultation notes.
 */

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nmims.careconnect.data.model.Booking
import com.nmims.careconnect.data.model.BookingStatus
import com.nmims.careconnect.data.model.BookingType
import com.nmims.careconnect.data.repository.BookingRepositoryProvider
import com.nmims.careconnect.data.repository.HospitalRepository
import com.nmims.careconnect.data.repository.LabTestRepository
import com.nmims.careconnect.ui.booking.components.FastingNoticeCard
import com.nmims.careconnect.ui.common.CareTopBar
import com.nmims.careconnect.ui.common.ConfirmDialog
import com.nmims.careconnect.ui.common.DateUtils
import com.nmims.careconnect.ui.common.InfoRow
import com.nmims.careconnect.ui.common.LoadingIndicator
import com.nmims.careconnect.ui.common.SectionCard
import com.nmims.careconnect.ui.common.StatusChip
import com.nmims.careconnect.ui.theme.CareConnectTheme
import kotlinx.coroutines.launch

@Composable
fun BookingDetailScreen(
    bookingId: String,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember(context) { BookingRepositoryProvider.get(context) }

    var booking by remember { mutableStateOf<Booking?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var isCancelling by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(bookingId, refreshTrigger) {
        isLoading = true
        isError = false
        try {
            val result = repository.getById(bookingId)
            if (result != null) {
                booking = result
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
                title = "Booking Details",
                onBack = onNavigateBack
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
                isError || booking == null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "Error",
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Booking Not Found",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Could not locate this booking. It may have been cancelled or the ID is invalid.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        OutlinedButton(
                            onClick = onNavigateBack,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Return Back")
                        }
                    }
                }
                else -> {
                    val currentBooking = booking!!
                    val hospital = remember(currentBooking.hospitalId) {
                        HospitalRepository.getHospital(currentBooking.hospitalId)
                    }
                    val doctor = remember(currentBooking.doctorId) {
                        currentBooking.doctorId?.let { HospitalRepository.getDoctor(it) }
                    }
                    val labTest = remember(currentBooking.labTestId) {
                        currentBooking.labTestId?.let { LabTestRepository.getTest(it) }
                    }

                    val statusColor = when (currentBooking.status) {
                        BookingStatus.CONFIRMED -> MaterialTheme.colorScheme.primaryContainer
                        BookingStatus.COMPLETED -> MaterialTheme.colorScheme.tertiaryContainer
                        BookingStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer
                    }

                    val canCancel = currentBooking.status == BookingStatus.CONFIRMED &&
                            currentBooking.dateMillis >= DateUtils.todayStartMillis()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header Reference & Status Hero Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "REFERENCE CODE",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = currentBooking.referenceCode(),
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            IconButton(
                                                onClick = {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    val clip = ClipData.newPlainText("Booking Ref", currentBooking.referenceCode())
                                                    clipboard.setPrimaryClip(clip)
                                                    Toast.makeText(context, "Copied reference code", Toast.LENGTH_SHORT).show()
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
                                        text = currentBooking.status.label,
                                        containerColor = statusColor
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CalendarMonth,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = DateUtils.formatWithDay(currentBooking.dateMillis),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.AccessTime,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = currentBooking.timeSlot,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Fasting Notice if lab test requires it
                        if (currentBooking.type == BookingType.LAB_TEST && labTest != null) {
                            FastingNoticeCard(
                                requiresFasting = labTest.requiresFasting
                            )
                        }

                        // Service Information
                        SectionCard(title = "Clinical Service Details") {
                            InfoRow(label = "Booking Type", value = currentBooking.type.label)
                            if (currentBooking.type == BookingType.OPD_APPOINTMENT) {
                                InfoRow(
                                    label = "Attending Doctor",
                                    value = doctor?.let { "Dr. ${it.name}" } ?: "Doctor Specialist"
                                )
                                if (doctor != null) {
                                    InfoRow(label = "Specialty", value = doctor.specialty)
                                    InfoRow(label = "Consultation Fee", value = "₹${doctor.consultationFee}")
                                }
                            } else {
                                InfoRow(
                                    label = "Diagnostic Test",
                                    value = labTest?.name ?: currentBooking.reason
                                )
                                if (labTest != null) {
                                    InfoRow(label = "Category", value = labTest.category)
                                    InfoRow(label = "Standard Price", value = "₹${labTest.price}")
                                    InfoRow(
                                        label = "Fasting Mandate",
                                        value = if (labTest.requiresFasting) "Required (10-12h)" else "Not Required"
                                    )
                                    InfoRow(
                                        label = "Turnaround Duration",
                                        value = "${labTest.reportTimeHours} hours"
                                    )
                                }
                            }
                        }

                        // Healthcare Facility Information
                        SectionCard(title = "Healthcare Facility") {
                            InfoRow(
                                label = "Hospital Name",
                                value = hospital?.name ?: currentBooking.hospitalId
                            )
                            if (hospital != null) {
                                InfoRow(
                                    label = "Area & City",
                                    value = "${hospital.area}, ${hospital.city}"
                                )
                                InfoRow(label = "Full Address", value = hospital.address)
                                InfoRow(label = "Helpdesk Phone", value = hospital.phone)
                            }
                        }

                        // Patient & Booking Meta
                        SectionCard(title = "Patient & Registration") {
                            InfoRow(label = "Patient Full Name", value = currentBooking.patientName)
                            if (currentBooking.reason.isNotBlank()) {
                                InfoRow(label = "Visit Symptom / Reason", value = currentBooking.reason)
                            }
                            InfoRow(
                                label = "Registered On",
                                value = DateUtils.format(currentBooking.createdAtMillis)
                            )
                        }

                        // Doctor Notes if present
                        if (currentBooking.doctorNote.isNotBlank()) {
                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Notes,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Doctor Consultation Note",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = currentBooking.doctorNote,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // Shared Medical Portfolio if present
                        if (currentBooking.sharedPortfolioText.isNotBlank()) {
                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.MedicalServices,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Shared Medical Summary",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = currentBooking.sharedPortfolioText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Cancel Button for eligible bookings
                        if (canCancel) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { showCancelDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Cancel Booking",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }

        if (showCancelDialog && booking != null) {
            ConfirmDialog(
                title = "Cancel Booking",
                message = "Are you sure you want to cancel booking ${booking?.referenceCode()}? This action cannot be reversed.",
                confirmText = if (isCancelling) "Cancelling..." else "Cancel Booking",
                dismissText = "Keep Booking",
                onConfirm = {
                    val idToCancel = booking?.id ?: return@ConfirmDialog
                    isCancelling = true
                    coroutineScope.launch {
                        try {
                            repository.cancel(idToCancel)
                            refreshTrigger++
                        } finally {
                            isCancelling = false
                            showCancelDialog = false
                        }
                    }
                },
                onDismiss = {
                    if (!isCancelling) {
                        showCancelDialog = false
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BookingDetailScreenPreview() {
    CareConnectTheme {
        BookingDetailScreen(bookingId = "preview_123")
    }
}
