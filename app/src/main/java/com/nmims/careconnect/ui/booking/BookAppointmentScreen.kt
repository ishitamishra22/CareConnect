package com.nmims.careconnect.ui.booking

/*
 * BookAppointmentScreen:
 * Appointment booking with date/slot validation, portfolio consent and confirmation.
 * Redesigned with BookingProgressHeader, Selected-state slot chips, PortfolioConsentCard,
 * and high-contrast confirm actions.
 */

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nmims.careconnect.data.firebase.Collections
import com.nmims.careconnect.data.firebase.FirebaseProvider
import com.nmims.careconnect.data.model.Booking
import com.nmims.careconnect.data.model.BookingStatus
import com.nmims.careconnect.data.model.BookingType
import com.nmims.careconnect.data.model.PatientProfile
import com.nmims.careconnect.data.repository.BookingRepositoryProvider
import com.nmims.careconnect.data.repository.HospitalRepository
import com.nmims.careconnect.data.repository.PortfolioRepositoryProvider
import com.nmims.careconnect.data.repository.SlotTakenException
import com.nmims.careconnect.session.AppSession
import com.nmims.careconnect.ui.booking.components.BookingProgressHeader
import com.nmims.careconnect.ui.booking.components.PortfolioConsentCard
import com.nmims.careconnect.ui.common.CareTopBar
import com.nmims.careconnect.ui.common.DateUtils
import com.nmims.careconnect.ui.common.InitialsAvatar
import com.nmims.careconnect.ui.common.PrimaryButton
import com.nmims.careconnect.ui.common.SectionCard
import com.nmims.careconnect.ui.patient.components.EmptyStatePanel
import com.nmims.careconnect.ui.patient.components.PatientHeroCard
import com.nmims.careconnect.ui.theme.CareConnectTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BookAppointmentScreen(
    doctorId: String,
    onNavigateBack: () -> Unit = {},
    onCreateProfile: () -> Unit = {},
    onBookingSuccess: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val doctor = remember(doctorId) { HospitalRepository.getDoctor(doctorId) }
    val hospital = remember(doctor?.hospitalId) { doctor?.let { HospitalRepository.getHospital(it.hospitalId) } }
    val bookings = remember(context) { BookingRepositoryProvider.get(context) }
    val portfolio = remember(context) { PortfolioRepositoryProvider.get(context) }

    var selectedDate by remember { mutableStateOf<Long?>(null) }
    var selectedSlot by remember { mutableStateOf<String?>(null) }
    var bookedSlots by remember { mutableStateOf<List<String>>(emptyList()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var slotsLoading by remember { mutableStateOf(false) }
    var slotsError by remember { mutableStateOf(false) }
    var refreshSlots by remember { mutableIntStateOf(0) }
    var reason by remember { mutableStateOf("") }
    var consent by remember { mutableStateOf(false) }
    var summary by remember { mutableStateOf<String?>(null) }
    var profileMissing by remember { mutableStateOf(false) }
    var summaryLoading by remember { mutableStateOf(true) }
    var showConfirmation by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    val dayName = selectedDate?.let { SimpleDateFormat("EEE", Locale.ENGLISH).format(Date(it)) }
    val availableDay = dayName != null && doctor?.availableDays?.any {
        it.equals(dayName, ignoreCase = true) || it.take(3).equals(dayName, ignoreCase = true)
    } == true

    LaunchedEffect(AppSession.currentPatientId) {
        summaryLoading = true
        summary = null
        profileMissing = false
        consent = false

        val patientId = AppSession.currentPatientId

        if (patientId.isNotBlank()) {
            try {
                val result = FirebaseProvider.safeCall {
                    val patientRef = FirebaseProvider.db
                        .collection(Collections.PATIENTS)
                        .document(patientId)

                    val profileSnapshot = patientRef.get().await()

                    if (!profileSnapshot.exists()) {
                        null
                    } else {
                        val profileData = profileSnapshot.data
                            ?: error("Patient profile data unavailable")

                        PatientProfile.fromMap(profileData)

                        patientRef.collection(Collections.RECORDS)
                            .get().await()

                        patientRef.collection(Collections.DOCUMENTS)
                            .get().await()

                        true
                    }
                }

                if (result.isSuccess) {
                    if (result.getOrNull() == null) {
                        profileMissing = true
                    } else {
                        val generated = portfolio.getSummary()

                        if (generated.patientName.isNotBlank()) {
                            summary = generated.toShareableText()
                        }
                    }
                }
            } catch (_: Exception) {
                summary = null
            }
        }

        summaryLoading = false
    }

    LaunchedEffect(doctorId, selectedDate, availableDay, refreshSlots) {
        bookedSlots = emptyList()
        selectedSlot = null
        slotsError = false
        slotsLoading = false
        val date = selectedDate
        if (date != null && availableDay) {
            slotsLoading = true
            try {
                bookedSlots = bookings.getBookedSlots(doctorId, date)
            } catch (_: Exception) {
                slotsError = true
            } finally {
                slotsLoading = false
            }
        }
    }

    val validReason = reason.trim().length in 10..200
    val canConfirm = doctor != null && hospital != null &&
        selectedDate != null && selectedDate!! >= DateUtils.todayStartMillis() &&
        availableDay && !slotsLoading && !slotsError && selectedSlot != null &&
        validReason && consent && summary != null && !saving

    val currentStep = when {
        selectedSlot == null -> 0
        !consent || !validReason -> 1
        else -> 2
    }

    Scaffold(topBar = { CareTopBar(title = "Book Appointment", onBack = onNavigateBack) }) { padding ->
        if (doctor == null) {
            EmptyStatePanel(
                icon = Icons.Default.Person,
                title = "Doctor Not Found",
                message = "The doctor you are looking for is unavailable or invalid.",
                actionText = "Back",
                onActionClick = onNavigateBack,
                modifier = Modifier.padding(padding)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                BookingProgressHeader(
                    currentStep = currentStep,
                    steps = listOf("Schedule", "Details", "Confirm")
                )

                // Doctor Details Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InitialsAvatar(
                            name = doctor.name,
                            colorHex = hospital?.accentColorHex ?: "#087F8C",
                            size = 54.dp
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = doctor.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = doctor.specialty,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = hospital?.name ?: "Partner Hospital",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "₹${doctor.consultationFee}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Appointment Date Selection
                SectionCard(title = "1. Appointment Date") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedDate?.let(DateUtils::formatWithDay) ?: "Select appointment date",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selectedDate != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Pick date",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Practicing days: ${doctor.availableDays.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (selectedDate != null && !availableDay) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Doctor is not available on this day. Please pick a listed practicing day.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Available Time Slots
                if (selectedDate != null && availableDay) {
                    SectionCard(title = "2. Consultation Slot") {
                        when {
                            slotsLoading -> {
                                Row(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Verifying real-time slot availability...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            slotsError -> {
                                Text(
                                    text = "Unable to check slot availability. Tap to retry.",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            else -> {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    doctor.availableSlots.forEach { slot ->
                                        val taken = bookedSlots.any { it.equals(slot, ignoreCase = true) }
                                        FilterChip(
                                            selected = selectedSlot == slot,
                                            onClick = { selectedSlot = slot },
                                            enabled = !taken,
                                            label = {
                                                Text(
                                                    text = slot,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    textDecoration = if (taken) TextDecoration.LineThrough else TextDecoration.None
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                            )
                                        )
                                    }
                                }
                                if (doctor.availableSlots.isEmpty()) {
                                    Text(
                                        text = "No appointment slots configured for this date.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Reason for Visit
                SectionCard(title = "3. Reason for Visit") {
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { if (it.length <= 200) reason = it },
                        label = { Text("Clinical symptoms or reason *") },
                        placeholder = { Text("Describe primary symptoms or consultation goal") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        supportingText = {
                            Text(
                                text = "${reason.trim().length}/200 characters (minimum 10 required)",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        isError = reason.isNotEmpty() && !validReason
                    )
                }

                // Medical Portfolio Consent Card
                PortfolioConsentCard(
                    hasSummary = summary != null,
                    consentChecked = consent,
                    onConsentChange = { consent = it },
                    onCreateProfileClick = onCreateProfile
                )

                // Confirm Button
                PrimaryButton(
                    text = if (saving) "Securing Slot..." else "Review & Confirm Appointment",
                    onClick = { showConfirmation = true },
                    enabled = canConfirm
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showDatePicker) {
        val picker = rememberDatePickerState(selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                DateUtils.normalizeToDayStart(utcTimeMillis) >= DateUtils.todayStartMillis()
        })
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let {
                        val date = DateUtils.normalizeToDayStart(it)
                        if (date >= DateUtils.todayStartMillis()) selectedDate = date
                    }
                    showDatePicker = false
                }) { Text("Confirm", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = picker) }
    }

    if (showConfirmation && doctor != null) {
        AlertDialog(
            onDismissRequest = { if (!saving) showConfirmation = false },
            title = {
                Text(
                    text = "Confirm Consultation Booking",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Review your appointment details:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "Doctor: ${doctor.name}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text(text = "Hospital: ${hospital?.name ?: "Partner Hospital"}", style = MaterialTheme.typography.bodySmall)
                            Text(text = "Date: ${selectedDate?.let(DateUtils::formatWithDay)}", style = MaterialTheme.typography.bodySmall)
                            Text(text = "Time: $selectedSlot", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                            Text(text = "Fee: ₹${doctor.consultationFee}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = canConfirm,
                    onClick = {
                        val date = selectedDate ?: return@Button
                        val slot = selectedSlot ?: return@Button
                        val medicalSummary = summary ?: return@Button
                        val patientId = AppSession.currentPatientId
                        if (patientId.isBlank()) return@Button
                        saving = true
                        scope.launch {
                            try {
                                val booking = Booking(
                                    id = UUID.randomUUID().toString(),
                                    type = BookingType.OPD_APPOINTMENT,
                                    patientId = patientId,
                                    patientName = AppSession.currentUserName,
                                    hospitalId = doctor.hospitalId,
                                    doctorId = doctor.id,
                                    labTestId = null,
                                    dateMillis = date,
                                    timeSlot = slot,
                                    reason = reason.trim(),
                                    sharedPortfolioText = medicalSummary,
                                    status = BookingStatus.CONFIRMED,
                                    createdAtMillis = System.currentTimeMillis()
                                )
                                if (bookings.add(booking)) {
                                    showConfirmation = false
                                    com.nmims.careconnect.receiver.BookingBroadcastReceiver.sendBroadcast(
                                        context = context,
                                        title = "Appointment Confirmed",
                                        message = "Dr. ${doctor.name} at ${hospital?.name ?: ""}".trim()
                                    )
                                    onBookingSuccess(booking.id)
                                } else {
                                    Toast.makeText(context, "Could not save booking", Toast.LENGTH_LONG).show()
                                }
                            } catch (_: SlotTakenException) {
                                showConfirmation = false
                                refreshSlots++
                                Toast.makeText(context, "That slot was just booked - please pick another", Toast.LENGTH_LONG).show()
                            } catch (_: Exception) {
                                Toast.makeText(context, "Could not save booking", Toast.LENGTH_LONG).show()
                            } finally {
                                saving = false
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text(if (saving) "Booking..." else "Confirm & Book") }
            },
            dismissButton = {
                TextButton(enabled = !saving, onClick = { showConfirmation = false }) { Text("Back") }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BookAppointmentScreenPreview() {
    CareConnectTheme { BookAppointmentScreen(doctorId = "preview") }
}
