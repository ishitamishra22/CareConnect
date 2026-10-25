package com.nmims.careconnect.ui.booking

/*
 * BookingSuccessScreen:
 * Displays confirmation details upon successful OPD or Lab Test booking.
 * Displays booking reference code, doctor/test details, hospital, date, time slot, and status.
 * Provides direct navigation options to My Bookings and Patient Home.
 * Polished with clinical minimalism, theme-aware tokens, and StatusChip.
 */

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
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
import com.nmims.careconnect.data.model.Booking
import com.nmims.careconnect.data.model.BookingStatus
import com.nmims.careconnect.data.model.BookingType
import com.nmims.careconnect.data.repository.BookingRepositoryProvider
import com.nmims.careconnect.data.repository.HospitalRepository
import com.nmims.careconnect.data.repository.LabTestRepository
import com.nmims.careconnect.ui.common.CareTopBar
import com.nmims.careconnect.ui.common.DateUtils
import com.nmims.careconnect.ui.common.InfoRow
import com.nmims.careconnect.ui.common.LoadingIndicator
import com.nmims.careconnect.ui.common.PrimaryButton
import com.nmims.careconnect.ui.common.SectionCard
import com.nmims.careconnect.ui.common.StatusChip
import com.nmims.careconnect.ui.patient.components.EmptyStatePanel
import com.nmims.careconnect.ui.patient.components.PatientHeroCard
import com.nmims.careconnect.ui.theme.CareConnectTheme

@Composable
fun BookingSuccessScreen(
    bookingId: String,
    onNavigateToMyBookings: () -> Unit = {},
    onNavigateToHome: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val repository = remember(context) { BookingRepositoryProvider.get(context) }

    var booking by remember { mutableStateOf<Booking?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }

    LaunchedEffect(bookingId) {
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
                title = "Booking Confirmation"
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
                    EmptyStatePanel(
                        icon = Icons.Default.ErrorOutline,
                        title = "Booking Details Unavailable",
                        message = "We could not find the requested booking record.",
                        actionText = "Back to Home",
                        onActionClick = onNavigateToHome,
                        modifier = Modifier.align(Alignment.Center)
                    )
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

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
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

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Booking Confirmed!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Your healthcare appointment has been secured and dispatched to the clinician.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Reference Code Highlight Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "BOOKING REFERENCE CODE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = currentBooking.referenceCode(),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(currentBooking.referenceCode()))
                                            Toast.makeText(context, "Reference code copied", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy code",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                StatusChip(
                                    text = currentBooking.status.label,
                                    containerColor = statusColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Details Section Card
                        SectionCard(title = "Appointment Summary") {
                            InfoRow(
                                label = "Service Type",
                                value = currentBooking.type.label
                            )

                            if (currentBooking.type == BookingType.OPD_APPOINTMENT) {
                                InfoRow(
                                    label = "Doctor",
                                    value = doctor?.let { "Dr. ${it.name}" } ?: "Specialist Doctor"
                                )
                                if (doctor != null) {
                                    InfoRow(
                                        label = "Specialty",
                                        value = doctor.specialty
                                    )
                                }
                            } else {
                                InfoRow(
                                    label = "Lab Test / Scan",
                                    value = labTest?.name ?: currentBooking.reason
                                )
                                if (labTest != null) {
                                    InfoRow(
                                        label = "Category",
                                        value = labTest.category
                                    )
                                }
                            }

                            InfoRow(
                                label = "Hospital / Facility",
                                value = hospital?.name ?: currentBooking.hospitalId
                            )
                            if (hospital != null) {
                                InfoRow(
                                    label = "Location",
                                    value = "${hospital.area}, ${hospital.city}"
                                )
                            }

                            InfoRow(
                                label = "Date",
                                value = DateUtils.formatWithDay(currentBooking.dateMillis)
                            )
                            InfoRow(
                                label = "Time Slot",
                                value = currentBooking.timeSlot
                            )
                            InfoRow(
                                label = "Patient Name",
                                value = currentBooking.patientName
                            )
                            if (currentBooking.reason.isNotBlank()) {
                                InfoRow(
                                    label = "Reason / Notes",
                                    value = currentBooking.reason
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        PrimaryButton(
                            text = "View in My Bookings",
                            onClick = onNavigateToMyBookings
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = onNavigateToHome,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "Back to Home",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BookingSuccessScreenPreview() {
    CareConnectTheme {
        BookingSuccessScreen(
            bookingId = "preview_123"
        )
    }
}
