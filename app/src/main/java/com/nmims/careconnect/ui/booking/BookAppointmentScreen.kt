package com.nmims.careconnect.ui.booking

/*
 * BookAppointmentScreen:
 * Displays doctor details, allows future-date selection,
 * validates available days and displays unbooked appointment slots.
 * Booking confirmation and portfolio consent are added in the next step.
 */

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nmims.careconnect.data.repository.BookingRepositoryProvider
import com.nmims.careconnect.data.repository.HospitalRepository
import com.nmims.careconnect.ui.common.CareTopBar
import com.nmims.careconnect.ui.common.DateUtils
import com.nmims.careconnect.ui.common.SectionCard
import com.nmims.careconnect.ui.theme.CareConnectTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun BookAppointmentScreen(
    doctorId: String,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val doctor = remember(doctorId) { HospitalRepository.getDoctor(doctorId) }
    val hospital = remember(doctor?.hospitalId) {
        doctor?.let { HospitalRepository.getHospital(it.hospitalId) }
    }
    val bookingRepository = remember(context) {
        BookingRepositoryProvider.get(context)
    }

    var selectedDate by remember { mutableStateOf<Long?>(null) }
    var selectedSlot by remember { mutableStateOf<String?>(null) }
    var bookedSlots by remember { mutableStateOf<List<String>>(emptyList()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var slotsLoading by remember { mutableStateOf(false) }
    var slotsError by remember { mutableStateOf(false) }

    val todayStart = DateUtils.todayStartMillis()
    val selectedDayName = selectedDate?.let {
        SimpleDateFormat("EEE", Locale.ENGLISH).format(Date(it))
    }
    val isAvailableDay = selectedDayName != null &&
        doctor?.availableDays?.any {
            it.equals(selectedDayName, ignoreCase = true) ||
                it.take(3).equals(selectedDayName, ignoreCase = true)
        } == true

    LaunchedEffect(doctorId, selectedDate, isAvailableDay) {
        bookedSlots = emptyList()
        selectedSlot = null
        slotsError = false
        slotsLoading = false

        val date = selectedDate
        if (date != null && isAvailableDay) {
            slotsLoading = true
            try {
                bookedSlots = bookingRepository.getBookedSlots(doctorId, date)
            } catch (_: Exception) {
                slotsError = true
            } finally {
                slotsLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            CareTopBar(
                title = "Book Appointment",
                onBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        if (doctor == null) {
            Text(
                text = "Doctor profile not found",
                modifier = Modifier.padding(innerPadding).padding(16.dp)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SectionCard(title = "Doctor Details") {
                    Text(
                        text = doctor.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = doctor.specialty)
                    Text(text = hospital?.name ?: "Hospital unavailable")
                    Text(
                        text = "Consultation fee INR ${doctor.consultationFee}",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                SectionCard(title = "Appointment Date") {
                    TextButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            selectedDate?.let { DateUtils.formatWithDay(it) }
                                ?: "Select appointment date"
                        )
                    }

                    Text(
                        text = "Available days: ${doctor.availableDays.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall
                    )

                    if (selectedDate != null && !isAvailableDay) {
                        Text(
                            text = "Doctor is not available on this day",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                if (selectedDate != null && isAvailableDay) {
                    SectionCard(title = "Available Time Slots") {
                        when {
                            slotsLoading -> Text("Checking available slots...")
                            slotsError -> Text(
                                "Unable to check slot availability. Please select another date.",
                                color = MaterialTheme.colorScheme.error
                            )
                            else -> {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    doctor.availableSlots.forEach { slot ->
                                        val taken = bookedSlots.any {
                                            it.equals(slot, ignoreCase = true)
                                        }
                                        FilterChip(
                                            selected = selectedSlot == slot,
                                            onClick = { selectedSlot = slot },
                                            enabled = !taken,
                                            label = {
                                                Text(
                                                    text = slot,
                                                    textDecoration = if (taken) {
                                                        TextDecoration.LineThrough
                                                    } else {
                                                        TextDecoration.None
                                                    }
                                                )
                                            }
                                        )
                                    }
                                }
                                if (doctor.availableSlots.isEmpty()) {
                                    Text("No appointment slots configured.")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return DateUtils.normalizeToDayStart(utcTimeMillis) >=
                        DateUtils.todayStartMillis()
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val normalized = DateUtils.normalizeToDayStart(millis)
                            if (normalized >= DateUtils.todayStartMillis()) {
                                selectedDate = normalized
                            }
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BookAppointmentScreenPreview() {
    CareConnectTheme {
        BookAppointmentScreen(doctorId = "preview")
    }
}
