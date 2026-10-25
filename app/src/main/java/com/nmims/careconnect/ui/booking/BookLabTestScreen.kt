package com.nmims.careconnect.ui.booking

/*
 * BookLabTestScreen:
 * Lab booking: catalogue, hospital, date/time, preparation advice and portfolio consent.
 * Redesigned with BookingProgressHeader, FastingNoticeCard, and PortfolioConsentCard.
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
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.nmims.careconnect.data.repository.LabTestRepository
import com.nmims.careconnect.data.repository.PortfolioRepositoryProvider
import com.nmims.careconnect.session.AppSession
import com.nmims.careconnect.ui.booking.components.BookingProgressHeader
import com.nmims.careconnect.ui.booking.components.FastingNoticeCard
import com.nmims.careconnect.ui.booking.components.PortfolioConsentCard
import com.nmims.careconnect.ui.common.CareTopBar
import com.nmims.careconnect.ui.common.DateUtils
import com.nmims.careconnect.ui.common.PrimaryButton
import com.nmims.careconnect.ui.common.SectionCard
import com.nmims.careconnect.ui.theme.CareConnectTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BookLabTestScreen(
    onNavigateBack: () -> Unit = {},
    onCreateProfile: () -> Unit = {},
    onBookingSuccess: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember(context) { BookingRepositoryProvider.get(context) }
    val portfolio = remember(context) { PortfolioRepositoryProvider.get(context) }
    val tests = remember { LabTestRepository.getAll() }
    val hospitals = remember { HospitalRepository.getHospitals() }

    var testId by remember { mutableStateOf<String?>(null) }
    var hospitalId by remember { mutableStateOf<String?>(null) }
    var date by remember { mutableStateOf<Long?>(null) }
    var slot by remember { mutableStateOf<String?>(null) }
    var testMenu by remember { mutableStateOf(false) }
    var hospitalMenu by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showPreparation by remember { mutableStateOf(false) }
    var showConfirmation by remember { mutableStateOf(false) }
    var consent by remember { mutableStateOf(false) }
    var summary by remember { mutableStateOf<String?>(null) }
    var profileMissing by remember { mutableStateOf(false) }
    var loadingSummary by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }

    val test = tests.find { it.id == testId }
    val hospital = hospitals.find { it.id == hospitalId }
    val slots = listOf("09:00 AM", "10:00 AM", "11:00 AM", "12:00 PM", "02:00 PM", "03:00 PM", "04:00 PM")
    val canBook = test != null && hospital != null && date != null &&
        date!! >= DateUtils.todayStartMillis() && slot != null && consent &&
        summary != null && !loadingSummary && !saving && AppSession.currentPatientId.isNotBlank()

    val currentStep = when {
        test == null || hospital == null || date == null || slot == null -> 0
        !consent -> 1
        else -> 2
    }

    LaunchedEffect(AppSession.currentPatientId) {
        loadingSummary = true
        summary = null
        profileMissing = false
        consent = false
        val patientId = AppSession.currentPatientId
        if (patientId.isNotBlank()) {
            try {
                val result = FirebaseProvider.safeCall {
                    val ref = FirebaseProvider.db.collection(Collections.PATIENTS).document(patientId)
                    val snapshot = ref.get().await()
                    if (!snapshot.exists()) null else {
                        PatientProfile.fromMap(snapshot.data ?: error("Missing profile data"))
                        ref.collection(Collections.RECORDS).get().await()
                        ref.collection(Collections.DOCUMENTS).get().await()
                        true
                    }
                }
                if (result.isSuccess) {
                    if (result.getOrNull() == null) profileMissing = true
                    else {
                        val generated = portfolio.getSummary()
                        if (generated.patientName.isNotBlank()) summary = generated.toShareableText()
                    }
                }
            } catch (_: Exception) {
                summary = null
            }
        }
        loadingSummary = false
    }

    Scaffold(topBar = { CareTopBar(title = "Book Lab Test", onBack = onNavigateBack) }) { padding ->
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
                steps = listOf("Test & Center", "Consent", "Confirm")
            )

            // 1. Diagnostic Test Selection
            SectionCard(title = "1. Diagnostic Test / Scan") {
                Box {
                    OutlinedButton(
                        onClick = { testMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(test?.name ?: "Select a diagnostic test or scan")
                    }
                    DropdownMenu(expanded = testMenu, onDismissRequest = { testMenu = false }) {
                        tests.forEach { item ->
                            DropdownMenuItem(
                                text = { Text("${item.name} • ₹${item.price}") },
                                onClick = {
                                    testId = item.id
                                    testMenu = false
                                    if (item.requiresFasting) showPreparation = true
                                }
                            )
                        }
                    }
                }

                if (test != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = test.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "Category: ${test.category} • TAT: ${test.reportTimeHours} hours", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                            Text(text = "Tariff: ₹${test.price}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    FastingNoticeCard(
                        requiresFasting = test.requiresFasting,
                        preparationInstructions = if (test.requiresFasting) "Fasting may be required (10-12 hours overnight) for accurate lipid/glucose values." else "No dietary restrictions listed for this test."
                    )
                }
            }

            // 2. Partner Hospital Selection
            SectionCard(title = "2. Laboratory / Hospital") {
                Box {
                    OutlinedButton(
                        onClick = { hospitalMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(hospital?.name ?: "Select partner hospital")
                    }
                    DropdownMenu(expanded = hospitalMenu, onDismissRequest = { hospitalMenu = false }) {
                        hospitals.forEach { item ->
                            DropdownMenuItem(
                                text = { Text("${item.name} (${item.area})") },
                                onClick = {
                                    hospitalId = item.id
                                    hospitalMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // 3. Date & Slot Selection
            SectionCard(title = "3. Date & Sample Slot") {
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
                            text = date?.let(DateUtils::formatWithDay) ?: "Select diagnostic date",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (date != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Pick date",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "Sample Collection Slot", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    slots.forEach { item ->
                        FilterChip(
                            selected = slot == item,
                            onClick = { slot = item },
                            label = { Text(item, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            // 4. Portfolio Consent
            PortfolioConsentCard(
                hasSummary = summary != null,
                consentChecked = consent,
                onConsentChange = { consent = it },
                onCreateProfileClick = onCreateProfile
            )

            // Submit Button
            PrimaryButton(
                text = if (saving) "Securing Booking..." else "Review & Confirm Lab Booking",
                onClick = { showConfirmation = true },
                enabled = canBook
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showDatePicker) {
        val picker = rememberDatePickerState(selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) =
                DateUtils.normalizeToDayStart(utcTimeMillis) >= DateUtils.todayStartMillis()
        })
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let {
                        val normalized = DateUtils.normalizeToDayStart(it)
                        if (normalized >= DateUtils.todayStartMillis()) date = normalized
                    }
                    showDatePicker = false
                }) { Text("Confirm", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = picker) }
    }

    if (showPreparation && test != null) {
        AlertDialog(
            onDismissRequest = { showPreparation = false },
            title = { Text("Preparation: ${test.name}", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (test.requiresFasting)
                        "This test requires 10-12 hours overnight fasting. Water intake is permissible. Please follow any specific medication instructions from your prescribing doctor."
                    else
                        "No fasting requirement is listed for this diagnostic procedure. Please arrive 10 minutes prior to your scheduled slot."
                )
            },
            confirmButton = {
                TextButton(onClick = { showPreparation = false }) { Text("Understood", fontWeight = FontWeight.Bold) }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showConfirmation && test != null && hospital != null) {
        AlertDialog(
            onDismissRequest = { if (!saving) showConfirmation = false },
            title = { Text("Confirm Lab Test Booking", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "Test: ${test.name}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text(text = "Hospital: ${hospital.name}", style = MaterialTheme.typography.bodySmall)
                            Text(text = "Date: ${date?.let(DateUtils::formatWithDay)}", style = MaterialTheme.typography.bodySmall)
                            Text(text = "Time: $slot", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                            Text(text = "Price: ₹${test.price}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            if (test.requiresFasting) {
                                Text(text = "Note: Fasting may be required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = canBook,
                    onClick = {
                        val chosenDate = date ?: return@Button
                        val chosenSlot = slot ?: return@Button
                        val medicalSummary = summary ?: return@Button
                        val patientId = AppSession.currentPatientId
                        if (patientId.isBlank()) return@Button
                        saving = true
                        scope.launch {
                            try {
                                val booking = Booking(
                                    id = UUID.randomUUID().toString(),
                                    type = BookingType.LAB_TEST,
                                    patientId = patientId,
                                    patientName = AppSession.currentUserName,
                                    hospitalId = hospital.id,
                                    doctorId = null,
                                    labTestId = test.id,
                                    dateMillis = chosenDate,
                                    timeSlot = chosenSlot,
                                    reason = test.name,
                                    sharedPortfolioText = medicalSummary,
                                    status = BookingStatus.CONFIRMED,
                                    createdAtMillis = System.currentTimeMillis()
                                )
                                if (repository.add(booking)) {
                                    showConfirmation = false
                                    com.nmims.careconnect.receiver.BookingBroadcastReceiver.sendBroadcast(
                                        context = context,
                                        title = "Lab Test Confirmed",
                                        message = "${test.name} at ${hospital.name}"
                                    )
                                    onBookingSuccess(booking.id)
                                } else Toast.makeText(context, "Could not save booking", Toast.LENGTH_LONG).show()
                            } catch (_: Exception) {
                                Toast.makeText(context, "Could not save booking", Toast.LENGTH_LONG).show()
                            } finally {
                                saving = false
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text(if (saving) "Saving..." else "Confirm & Book") }
            },
            dismissButton = { TextButton(enabled = !saving, onClick = { showConfirmation = false }) { Text("Back") } },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BookLabTestScreenPreview() {
    CareConnectTheme { BookLabTestScreen() }
}
