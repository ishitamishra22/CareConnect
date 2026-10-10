package com.nmims.careconnect.ui.booking

/* Lab booking: catalogue, hospital, date/time, preparation advice and portfolio consent. */

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.nmims.careconnect.ui.common.CareTopBar
import com.nmims.careconnect.ui.common.DateUtils
import com.nmims.careconnect.ui.common.SectionCard
import com.nmims.careconnect.ui.theme.CareConnectTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
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
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard(title = "Diagnostic Test / Scan") {
                Box {
                    OutlinedButton(onClick = { testMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(test?.name ?: "Select a test or scan")
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
                    Text(test.description)
                    Text("Category: ${test.category} | Price: ₹${test.price}")
                    Text("Estimated report time: ${test.reportTimeHours} hours")
                    Text(if (test.requiresFasting) "Preparation: fasting may be required" else "No fasting requirement listed")
                    TextButton(onClick = { showPreparation = true }) { Text("View preparation instructions") }
                }
            }
            SectionCard(title = "Hospital") {
                Box {
                    OutlinedButton(onClick = { hospitalMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(hospital?.name ?: "Select hospital")
                    }
                    DropdownMenu(expanded = hospitalMenu, onDismissRequest = { hospitalMenu = false }) {
                        hospitals.forEach { item ->
                            DropdownMenuItem(text = { Text(item.name) }, onClick = {
                                hospitalId = item.id
                                hospitalMenu = false
                            })
                        }
                    }
                }
                Text("Availability of the selected test at this hospital is not verified.", style = MaterialTheme.typography.bodySmall)
            }
            SectionCard(title = "Date and Time") {
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(date?.let(DateUtils::formatWithDay) ?: "Select date")
                }
                Text("Preferred time slot")
                @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    slots.forEach { item ->
                        FilterChip(selected = slot == item, onClick = { slot = item }, label = { Text(item) })
                    }
                }
                Text("Time slots are requests; real-time lab capacity is not checked.", style = MaterialTheme.typography.bodySmall)
            }
            SectionCard(title = "Medical Portfolio Sharing") {
                when {
                    loadingSummary -> CircularProgressIndicator()
                    profileMissing -> {
                        Text("Create your patient profile before booking.")
                        TextButton(onClick = onCreateProfile) { Text("Create profile") }
                    }
                    summary == null -> Text("Medical summary unavailable", color = MaterialTheme.colorScheme.error)
                    else -> {
                        Text("Your medical summary will be shared with the selected hospital.")
                        Row {
                            Checkbox(checked = consent, onCheckedChange = { consent = it })
                            Text("I consent to sharing my medical portfolio.", modifier = Modifier.padding(top = 12.dp))
                        }
                    }
                }
            }
            Button(onClick = { showConfirmation = true }, enabled = canBook, modifier = Modifier.fillMaxWidth()) {
                Text("Review Booking")
            }
        }
    }

    if (showDatePicker) {
        val picker = rememberDatePickerState(selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) =
                DateUtils.normalizeToDayStart(utcTimeMillis) >= DateUtils.todayStartMillis()
        })
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = {
                picker.selectedDateMillis?.let {
                    val normalized = DateUtils.normalizeToDayStart(it)
                    if (normalized >= DateUtils.todayStartMillis()) date = normalized
                }
                showDatePicker = false
            }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = picker) }
    }

    if (showPreparation && test != null) {
        AlertDialog(
            onDismissRequest = { showPreparation = false },
            title = { Text("Preparation: ${test.name}") },
            text = { Text(if (test.requiresFasting)
                "This test may require fasting. Confirm the fasting duration and any medication instructions with the laboratory before your visit."
                else "No fasting requirement is listed for this test. Follow any additional instructions provided by the laboratory.") },
            confirmButton = { TextButton(onClick = { showPreparation = false }) { Text("Understood") } }
        )
    }

    if (showConfirmation && test != null && hospital != null) {
        AlertDialog(
            onDismissRequest = { if (!saving) showConfirmation = false },
            title = { Text("Confirm Lab Booking") },
            text = { Text("Test: ${test.name}\nHospital: ${hospital.name}\nDate: ${date?.let(DateUtils::formatWithDay)}\nTime: $slot\nPrice: ₹${test.price}${if (test.requiresFasting) "\nFasting may be required" else ""}") },
            confirmButton = {
                TextButton(enabled = canBook, onClick = {
                    val chosenDate = date ?: return@TextButton
                    val chosenSlot = slot ?: return@TextButton
                    val medicalSummary = summary ?: return@TextButton
                    val patientId = AppSession.currentPatientId
                    if (patientId.isBlank()) return@TextButton
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
                                onBookingSuccess(booking.id)
                            } else Toast.makeText(context, "Could not save booking", Toast.LENGTH_LONG).show()
                        } catch (_: Exception) {
                            Toast.makeText(context, "Could not save booking", Toast.LENGTH_LONG).show()
                        } finally {
                            saving = false
                        }
                    }
                }) { Text(if (saving) "Saving..." else "Book Now") }
            },
            dismissButton = { TextButton(enabled = !saving, onClick = { showConfirmation = false }) { Text("Back") } }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BookLabTestScreenPreview() {
    CareConnectTheme { BookLabTestScreen() }
}
