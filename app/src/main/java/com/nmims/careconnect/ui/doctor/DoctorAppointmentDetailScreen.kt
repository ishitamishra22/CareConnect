package com.nmims.careconnect.ui.doctor

/*
 * DoctorAppointmentDetailScreen:
 * Displays appointment details for the logged-in doctor:
 * - Verifies that booking.doctorId matches AppSession.currentDoctorId
 * - Shows patient identity hero card with InitialsAvatar, token badge, and theme-adaptive StatusChip
 * - Structured Schedule card with date, time slot, and hospital facility details
 * - Shared medical portfolio summary displaying patient-consented medical history
 * - Consultation note editor allowing doctors to compose and persist clinical notes
 * - Action buttons to mark confirmed appointments as Completed or Cancelled with confirmation dialogs
 * - Clear success/error feedback banners and state recovery
 * - Preserves all validation and security access checks
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.nmims.careconnect.ui.common.LabelChip
import com.nmims.careconnect.ui.common.LoadingIndicator
import com.nmims.careconnect.ui.common.SectionCard
import com.nmims.careconnect.ui.common.StatusChip
import com.nmims.careconnect.ui.common.bookingStatusContainerColor
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
    var noteSaveSuccess by remember { mutableStateOf(false) }

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
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LoadingIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Loading consultation details...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (errorMessage.contains("Access Denied", ignoreCase = true)) {
                                        Icons.Default.Lock
                                    } else {
                                        Icons.Default.ErrorOutline
                                    },
                                    contentDescription = "Error",
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Please check your assigned consultations or contact the hospital administrator.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onNavigateBack,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Back to Dashboard")
                        }
                    }
                }
                else -> {
                    val currentBooking = booking!!
                    val hospital = remember(currentBooking.hospitalId) {
                        HospitalRepository.getHospital(currentBooking.hospitalId)
                    }

                    val statusContainer = bookingStatusContainerColor(currentBooking.status)
                    val canModifyStatus = currentBooking.status == BookingStatus.CONFIRMED

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        // Patient Hero Header Card
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    LabelChip(
                                        text = "TOKEN: ${currentBooking.referenceCode()}",
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    StatusChip(
                                        text = currentBooking.status.label,
                                        containerColor = statusContainer
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = currentBooking.patientName.ifBlank { "Unspecified Patient" },
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Outpatient Consultation",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (currentBooking.reason.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "Patient Reason for Visit",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = currentBooking.reason,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Schedule & Facility Card
                        SectionCard(title = "Schedule & Facility") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Date",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = DateUtils.formatWithDay(currentBooking.dateMillis),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Time Slot",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = currentBooking.timeSlot,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (hospital != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalHospital,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Hospital",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${hospital.name} (${hospital.area})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Shared Medical Portfolio (read-only consent summary)
                        SectionCard(title = "Shared Medical Portfolio") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderShared,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Patient-consented medical history shared for this consultation",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (currentBooking.sharedPortfolioText.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = currentBooking.sharedPortfolioText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "No additional medical portfolio records were attached by the patient for this appointment.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Doctor Consultation Note (Editable)
                        SectionCard(title = "Clinical Consultation Notes") {
                            Text(
                                text = "Document diagnosis, clinical observations, prescribed medications, and follow-up guidance.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = noteText,
                                onValueChange = {
                                    noteText = it
                                    noteSaveSuccess = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = {
                                    Text("Enter diagnosis, prescription, clinical observations, or discharge instructions...")
                                },
                                minLines = 4,
                                maxLines = 8,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (noteSaveSuccess || currentBooking.doctorNote.isNotBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (noteSaveSuccess) "Notes saved successfully" else "Clinical notes recorded",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.width(1.dp))
                                }

                                Button(
                                    onClick = {
                                        isSavingNote = true
                                        noteSaveSuccess = false
                                        coroutineScope.launch {
                                            try {
                                                val success = repository.updateStatus(
                                                    id = currentBooking.id,
                                                    status = currentBooking.status,
                                                    doctorNote = noteText.trim()
                                                )
                                                if (success) {
                                                    noteSaveSuccess = true
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
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
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
                                        .height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Complete",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Mark Completed", fontWeight = FontWeight.SemiBold)
                                }

                                OutlinedButton(
                                    onClick = { showCancelDialog = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    border = BorderStroke(
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
                                    Text("Cancel", fontWeight = FontWeight.SemiBold)
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
                title = "Complete Consultation",
                message = "Mark consultation for ${booking?.patientName} as completed? Any clinical notes entered will be permanently saved to the consultation record.",
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
                                Toast.makeText(context, "Consultation marked as completed", Toast.LENGTH_SHORT).show()
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
                title = "Cancel Consultation",
                message = "Are you sure you want to cancel the appointment for ${booking?.patientName}? This action cannot be undone.",
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
