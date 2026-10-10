package com.nmims.careconnect.ui.doctor

/*
 * DoctorAppointmentDetailScreen:
 * Displays appointment details for the logged-in doctor:
 * - Verifies that booking.doctorId matches AppSession.currentDoctorId
 * - Shows patient information, date, slot, and reason for visit
 * - Shows shared medical summary (sharedPortfolioText)
 * - Consultation note editor allowing doctors to save and update clinical notes
 * - Action buttons to mark confirmed appointments as Completed or Cancelled
 * - Uses BookingRepository.updateStatus() and validates lifecycle transitions
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.nmims.careconnect.data.repository.BookingRepositoryProvider
import com.nmims.careconnect.data.repository.HospitalRepository
import com.nmims.careconnect.session.AppSession
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
fun DoctorAppointmentDetailScreen(
    bookingId: String,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember(context) { BookingRepositoryProvider.get(context) }

    var booking by remember { mutableStateOf<Booking?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("Appointment Not Found") }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    // Consultation notes state
    var noteText by remember { mutableStateOf("") }
    var isSavingNote by remember { mutableStateOf(false) }

    // Dialog states
    var showCompleteDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var isUpdatingStatus by remember { mutableStateOf(false) }

    LaunchedEffect(bookingId, refreshTrigger) {
        isLoading = true
        isError = false
        try {
            val result = repository.getById(bookingId)
            if (result == null) {
                isError = true
                errorMessage = "Appointment could not be found."
            } else if (result.doctorId != AppSession.currentDoctorId) {
                // Security check: Only allow access to the assigned doctor's bookings
                isError = true
                errorMessage = "Access Denied: This appointment is not assigned to your profile."
            } else {
                booking = result
                noteText = result.doctorNote
            }
        } catch (_: Exception) {
            isError = true
            errorMessage = "Could not load appointment details."
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            CareTopBar(
                title = "Consultation Details",
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
                            text = errorMessage,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = onNavigateBack) {
                            Text("Back to Dashboard")
                        }
                    }
                }
                else -> {
                    val currentBooking = booking!!
                    val hospital = remember(currentBooking.hospitalId) {
                        HospitalRepository.getHospital(currentBooking.hospitalId)
                    }

                    val statusColor = when (currentBooking.status) {
                        BookingStatus.CONFIRMED -> Color(0xFFE8F5E9)
                        BookingStatus.COMPLETED -> Color(0xFFE3F2FD)
                        BookingStatus.CANCELLED -> Color(0xFFFFEBEE)
                    }

                    val canModifyStatus = currentBooking.status == BookingStatus.CONFIRMED

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        // Header Reference & Status
                        SectionCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "APPOINTMENT TOKEN",
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

                        // Patient & Schedule Details
                        SectionCard(title = "Patient & Schedule") {
                            InfoRow(
                                label = "Patient Name",
                                value = currentBooking.patientName.ifBlank { "Unspecified" }
                            )
                            InfoRow(
                                label = "Appointment Date",
                                value = DateUtils.formatWithDay(currentBooking.dateMillis)
                            )
                            InfoRow(
                                label = "Time Slot",
                                value = currentBooking.timeSlot
                            )
                            if (currentBooking.reason.isNotBlank()) {
                                InfoRow(
                                    label = "Patient Reason",
                                    value = currentBooking.reason
                                )
                            }
                            if (hospital != null) {
                                InfoRow(
                                    label = "Hospital",
                                    value = "${hospital.name} (${hospital.area})"
                                )
                            }
                        }

                        // Shared Medical Portfolio (read-only consent summary)
                        if (currentBooking.sharedPortfolioText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            SectionCard(title = "Patient Shared Medical Portfolio") {
                                Text(
                                    text = currentBooking.sharedPortfolioText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Doctor Consultation Note (Editable)
                        SectionCard(title = "Consultation & Clinical Notes") {
                            OutlinedTextField(
                                value = noteText,
                                onValueChange = { noteText = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = {
                                    Text("Enter diagnosis, treatment plan, prescription or observations...")
                                },
                                minLines = 4,
                                maxLines = 8,
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    isSavingNote = true
                                    coroutineScope.launch {
                                        try {
                                            val success = repository.updateStatus(
                                                id = currentBooking.id,
                                                status = currentBooking.status,
                                                doctorNote = noteText.trim()
                                            )
                                            if (success) {
                                                Toast.makeText(context, "Consultation note saved", Toast.LENGTH_SHORT).show()
                                                refreshTrigger++
                                            } else {
                                                Toast.makeText(context, "Could not save notes", Toast.LENGTH_SHORT).show()
                                            }
                                        } finally {
                                            isSavingNote = false
                                        }
                                    }
                                },
                                enabled = !isSavingNote,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                if (isSavingNote) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Save,
                                        contentDescription = "Save",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text("Save Note")
                            }
                        }

                        // Action Buttons: Mark Completed / Cancel
                        if (canModifyStatus) {
                            Spacer(modifier = Modifier.height(20.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { showCompleteDialog = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Complete",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Mark Completed")
                                }

                                OutlinedButton(
                                    onClick = { showCancelDialog = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancel",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Cancel")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }

        // Complete Dialog
        if (showCompleteDialog && booking != null) {
            ConfirmDialog(
                title = "Complete Appointment",
                message = "Mark consultation for ${booking?.patientName} as completed?",
                confirmText = if (isUpdatingStatus) "Saving..." else "Confirm Completed",
                dismissText = "Dismiss",
                onConfirm = {
                    val id = booking?.id ?: return@ConfirmDialog
                    isUpdatingStatus = true
                    coroutineScope.launch {
                        try {
                            val success = repository.updateStatus(
                                id = id,
                                status = BookingStatus.COMPLETED,
                                doctorNote = noteText.trim()
                            )
                            if (success) {
                                Toast.makeText(context, "Appointment marked as completed", Toast.LENGTH_SHORT).show()
                                refreshTrigger++
                            }
                        } finally {
                            isUpdatingStatus = false
                            showCompleteDialog = false
                        }
                    }
                },
                onDismiss = {
                    if (!isUpdatingStatus) showCompleteDialog = false
                }
            )
        }

        // Cancel Dialog
        if (showCancelDialog && booking != null) {
            ConfirmDialog(
                title = "Cancel Appointment",
                message = "Are you sure you want to cancel this appointment for ${booking?.patientName}?",
                confirmText = if (isUpdatingStatus) "Cancelling..." else "Confirm Cancel",
                dismissText = "Dismiss",
                onConfirm = {
                    val id = booking?.id ?: return@ConfirmDialog
                    isUpdatingStatus = true
                    coroutineScope.launch {
                        try {
                            val success = repository.updateStatus(
                                id = id,
                                status = BookingStatus.CANCELLED,
                                doctorNote = noteText.trim()
                            )
                            if (success) {
                                Toast.makeText(context, "Appointment cancelled", Toast.LENGTH_SHORT).show()
                                refreshTrigger++
                            }
                        } finally {
                            isUpdatingStatus = false
                            showCancelDialog = false
                        }
                    }
                },
                onDismiss = {
                    if (!isUpdatingStatus) showCancelDialog = false
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DoctorAppointmentDetailScreenPreview() {
    CareConnectTheme {
        DoctorAppointmentDetailScreen(bookingId = "preview_123")
    }
}
