package com.nmims.careconnect.ui.booking

/* Appointment booking with date/slot validation, portfolio consent and confirmation. */

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.nmims.careconnect.data.repository.BookingRepositoryProvider
import com.nmims.careconnect.data.repository.HospitalRepository
import com.nmims.careconnect.data.repository.PortfolioRepositoryProvider
import com.nmims.careconnect.data.repository.SlotTakenException
import com.nmims.careconnect.session.AppSession
import com.nmims.careconnect.ui.common.CareTopBar
import com.nmims.careconnect.ui.common.DateUtils
import com.nmims.careconnect.ui.common.SectionCard
import com.nmims.careconnect.ui.theme.CareConnectTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import com.nmims.careconnect.data.model.PatientProfile


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

                        // Validate that the stored profile can be read.
                        PatientProfile.fromMap(profileData)

                        // Ensure both collections are accessible.
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

    Scaffold(topBar = { CareTopBar(title = "Book Appointment", onBack = onNavigateBack) }) { padding ->
        if (doctor == null) {
            Text("Doctor profile not found", modifier = Modifier.padding(padding).padding(16.dp))
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding)
                    .verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SectionCard(title = "Doctor Details") {
                    Text(doctor.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(doctor.specialty)
                    Text(hospital?.name ?: "Hospital unavailable")
                    Text("Consultation fee INR ${doctor.consultationFee}", fontWeight = FontWeight.SemiBold)
                }
                SectionCard(title = "Appointment Date") {
                    TextButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selectedDate?.let(DateUtils::formatWithDay) ?: "Select appointment date")
                    }
                    Text("Available days: ${doctor.availableDays.joinToString(", ")}")
                    if (selectedDate != null && !availableDay) {
                        Text("Doctor is not available on this day", color = MaterialTheme.colorScheme.error)
                    }
                }
                if (selectedDate != null && availableDay) {
                    SectionCard(title = "Available Time Slots") {
                        when {
                            slotsLoading -> Text("Checking available slots...")
                            slotsError -> Text("Unable to check slot availability", color = MaterialTheme.colorScheme.error)
                            else -> {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    doctor.availableSlots.forEach { slot ->
                                        val taken = bookedSlots.any { it.equals(slot, ignoreCase = true) }
                                        FilterChip(
                                            selected = selectedSlot == slot,
                                            onClick = { selectedSlot = slot },
                                            enabled = !taken,
                                            label = { Text(slot, textDecoration = if (taken) TextDecoration.LineThrough else TextDecoration.None) }
                                        )
                                    }
                                }
                                if (doctor.availableSlots.isEmpty()) Text("No appointment slots configured.")
                            }
                        }
                    }
                }
                SectionCard(title = "Reason for Visit") {
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { if (it.length <= 200) reason = it },
                        label = { Text("Reason for appointment") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = { Text("${reason.trim().length}/200 characters (minimum 10)") },
                        isError = reason.isNotEmpty() && !validReason
                    )
                }
                SectionCard(title = "Medical Portfolio Sharing") {
                    when {
                        summaryLoading -> CircularProgressIndicator()
                        profileMissing -> {
                            Text("Please create your patient profile before booking.")
                            TextButton(onClick = onCreateProfile) { Text("Create profile") }
                        }
                        summary == null -> Text("Medical summary unavailable", color = MaterialTheme.colorScheme.error)
                        else -> {
                            Text("Your medical summary will be shared with the doctor for this appointment.")
                            Row {
                                Checkbox(checked = consent, onCheckedChange = { consent = it })
                                Text("I consent to sharing my medical portfolio with the doctor.", modifier = Modifier.padding(top = 12.dp))
                            }
                        }
                    }
                }
                Button(
                    onClick = { showConfirmation = true },
                    enabled = canConfirm,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (saving) "Saving..." else "Confirm Booking") }
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
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = picker) }
    }

    if (showConfirmation && doctor != null) {
        AlertDialog(
            onDismissRequest = { if (!saving) showConfirmation = false },
            title = { Text("Confirm Appointment") },
            text = {
                Text("Doctor: ${doctor.name}\nDate: ${selectedDate?.let(DateUtils::formatWithDay)}\nTime: $selectedSlot\nConsultation fee INR ${doctor.consultationFee}")
            },
            confirmButton = {
                TextButton(enabled = canConfirm, onClick = {
                    val date = selectedDate ?: return@TextButton
                    val slot = selectedSlot ?: return@TextButton
                    val medicalSummary = summary ?: return@TextButton
                    val patientId = AppSession.currentPatientId
                    if (patientId.isBlank()) return@TextButton
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
                }) { Text("Book Now") }
            },
            dismissButton = {
                TextButton(enabled = !saving, onClick = { showConfirmation = false }) { Text("Back") }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BookAppointmentScreenPreview() {
    CareConnectTheme { BookAppointmentScreen(doctorId = "preview") }
}
