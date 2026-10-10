package com.nmims.careconnect.ui.booking

/*
 * BookingDetailScreen:
 * Displays full details of an individual OPD appointment or Lab Test booking.
 * Enables cancellation for eligible upcoming bookings using BookingRepository.cancel().
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Error",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Booking Not Found",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Could not locate this booking. It may have been removed or the ID is invalid.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
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
                        BookingStatus.CONFIRMED -> Color(0xFFE8F5E9)
                        BookingStatus.COMPLETED -> Color(0xFFE3F2FD)
                        BookingStatus.CANCELLED -> Color(0xFFFFEBEE)
                    }

                    val canCancel = currentBooking.status == BookingStatus.CONFIRMED &&
                            currentBooking.dateMillis >= DateUtils.todayStartMillis()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        // Header Reference & Status Card
                        SectionCard {
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
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = currentBooking.referenceCode(),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                StatusChip(
                                    text = currentBooking.status.label,
                                    containerColor = statusColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Service Information
                        SectionCard(title = "Service Details") {
                            InfoRow(label = "Booking Type", value = currentBooking.type.label)
                            if (currentBooking.type == BookingType.OPD_APPOINTMENT) {
                                InfoRow(
                                    label = "Doctor",
                                    value = doctor?.let { "Dr. ${it.name}" } ?: "Doctor Specialist"
                                )
                                if (doctor != null) {
                                    InfoRow(label = "Specialty", value = doctor.specialty)
                                    InfoRow(label = "Fee", value = "₹${doctor.consultationFee}")
                                }
                            } else {
                                InfoRow(
                                    label = "Lab Test / Scan",
                                    value = labTest?.name ?: currentBooking.reason
                                )
                                if (labTest != null) {
                                    InfoRow(label = "Category", value = labTest.category)
                                    InfoRow(label = "Price", value = "₹${labTest.price}")
                                    InfoRow(
                                        label = "Fasting Required",
                                        value = if (labTest.requiresFasting) "Yes" else "No"
                                    )
                                    InfoRow(
                                        label = "Report Turnaround",
                                        value = "${labTest.reportTimeHours} hours"
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Hospital Information
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
                                InfoRow(label = "Address", value = hospital.address)
                                InfoRow(label = "Contact Phone", value = hospital.phone)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Schedule & Patient Information
                        SectionCard(title = "Schedule & Patient") {
                            InfoRow(
                                label = "Appointment Date",
                                value = DateUtils.formatWithDay(currentBooking.dateMillis)
                            )
                            InfoRow(label = "Time Slot", value = currentBooking.timeSlot)
                            InfoRow(label = "Patient Name", value = currentBooking.patientName)
                            if (currentBooking.reason.isNotBlank()) {
                                InfoRow(label = "Reason for Visit", value = currentBooking.reason)
                            }
                            InfoRow(
                                label = "Booked On",
                                value = DateUtils.format(currentBooking.createdAtMillis)
                            )
                        }

                        // Doctor Notes if present
                        if (currentBooking.doctorNote.isNotBlank()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            SectionCard(title = "Doctor Consultation Note") {
                                Text(
                                    text = currentBooking.doctorNote,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Shared Medical Portfolio if present
                        if (currentBooking.sharedPortfolioText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            SectionCard(title = "Shared Medical Summary") {
                                Text(
                                    text = currentBooking.sharedPortfolioText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Cancel Button for eligible bookings
                        if (canCancel) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { showCancelDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                )
                            ) {
                                Text(
                                    text = "Cancel Booking",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }

        if (showCancelDialog && booking != null) {
            ConfirmDialog(
                title = "Cancel Booking",
                message = "Are you sure you want to cancel booking ${booking?.referenceCode()}? This cannot be undone.",
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
