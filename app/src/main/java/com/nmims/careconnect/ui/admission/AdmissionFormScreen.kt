package com.nmims.careconnect.ui.admission

/*
 * AdmissionFormScreen:
 * Four-step wizard for automated hospital admission requests:
 * - Step 1: Hospital selection (from catalogue), admission reason (min 10 chars), optional referring doctor.
 * - Step 2: Patient details auto-filled from Medical Portfolio (editable), missing profile gate with alert dialog.
 * - Step 3: Room category selection (from hospital room types), preferred admission date (today or later), expected stay (1-60 days), live cost estimation.
 * - Step 4: Insurance switch & validation (policy 8-20 alphanumeric, validity > admission date), final review card, portfolio forwardable summary.
 * Enforces strict input validation, prevents duplicate submission, handles loading/saving states,
 * dispatches booking confirmation broadcast upon persistence, and navigates to AdmissionSuccess.
 */

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nmims.careconnect.data.model.Admission
import com.nmims.careconnect.data.model.AdmissionStatus
import com.nmims.careconnect.data.model.Hospital
import com.nmims.careconnect.data.model.InsuranceDetails
import com.nmims.careconnect.data.model.PatientProfile
import com.nmims.careconnect.data.model.RoomType
import com.nmims.careconnect.data.repository.AdmissionRepositoryProvider
import com.nmims.careconnect.data.repository.HospitalRepository
import com.nmims.careconnect.data.repository.PortfolioRepositoryProvider
import com.nmims.careconnect.receiver.BookingBroadcastReceiver
import com.nmims.careconnect.session.AppSession
import com.nmims.careconnect.ui.common.CareTopBar
import com.nmims.careconnect.ui.common.ConfirmDialog
import com.nmims.careconnect.ui.common.DateUtils
import com.nmims.careconnect.ui.common.LoadingIndicator
import com.nmims.careconnect.ui.common.PrimaryButton
import com.nmims.careconnect.ui.common.SectionCard
import com.nmims.careconnect.ui.theme.CareConnectTheme
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdmissionFormScreen(
    initialHospitalId: String? = null,
    onNavigateBack: () -> Unit,
    onCreateProfile: () -> Unit,
    onAdmissionSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val admissionRepo = remember(context) { AdmissionRepositoryProvider.get(context) }
    val portfolioRepo = remember(context) { PortfolioRepositoryProvider.get(context) }

    val hospitals = remember { HospitalRepository.getHospitals() }

    // Step state: 1 to 4
    var currentStep by rememberSaveable { mutableIntStateOf(1) }

    // Step 1: Hospital and Reason
    var selectedHospitalId by rememberSaveable {
        mutableStateOf(initialHospitalId ?: hospitals.firstOrNull()?.id ?: "")
    }
    var hospitalDropdownExpanded by remember { mutableStateOf(false) }
    var reason by rememberSaveable { mutableStateOf("") }
    var referringDoctor by rememberSaveable { mutableStateOf("") }
    var step1Touched by rememberSaveable { mutableStateOf(false) }

    val selectedHospital = hospitals.find { it.id == selectedHospitalId }

    // Step 2: Patient Details
    var isProfileLoading by rememberSaveable { mutableStateOf(true) }
    var isProfileMissing by rememberSaveable { mutableStateOf(false) }
    var profileLoadedOnce by rememberSaveable { mutableStateOf(false) }

    var patientName by rememberSaveable { mutableStateOf(AppSession.currentUserName) }
    var ageText by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf("Male") }
    var bloodGroup by rememberSaveable { mutableStateOf("B+") }
    var phone by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var emergencyContactName by rememberSaveable { mutableStateOf("") }
    var emergencyContactPhone by rememberSaveable { mutableStateOf("") }
    var allergiesText by rememberSaveable { mutableStateOf("") }
    var chronicConditionsText by rememberSaveable { mutableStateOf("") }
    var medicationsText by rememberSaveable { mutableStateOf("") }
    var genderDropdownExpanded by remember { mutableStateOf(false) }
    var bloodGroupDropdownExpanded by remember { mutableStateOf(false) }
    var step2Touched by rememberSaveable { mutableStateOf(false) }

    val genderOptions = listOf("Male", "Female", "Other")
    val bloodGroupOptions = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")

    // Portfolio shareable summary text
    var portfolioSummaryText by rememberSaveable { mutableStateOf("") }

    // Step 3: Room and Dates
    val roomTypes = selectedHospital?.roomTypes ?: emptyList()
    var selectedRoomName by rememberSaveable { mutableStateOf("") }
    var preferredDateMillis by rememberSaveable { mutableLongStateOf(0L) }
    var expectedStayDaysText by rememberSaveable { mutableStateOf("3") }
    var showAdmissionDatePicker by remember { mutableStateOf(false) }
    var step3Touched by rememberSaveable { mutableStateOf(false) }

    // Keep selectedRoom updated with available hospital room types
    LaunchedEffect(selectedHospitalId) {
        if (selectedHospital != null && (selectedRoomName.isBlank() || roomTypes.none { it.name == selectedRoomName })) {
            selectedRoomName = roomTypes.firstOrNull()?.name ?: ""
        }
    }

    val selectedRoom = roomTypes.find { it.name == selectedRoomName } ?: roomTypes.firstOrNull()
    val stayDaysInt = expectedStayDaysText.toIntOrNull() ?: 0
    val estimatedCost = (selectedRoom?.pricePerDay ?: 0) * stayDaysInt

    // Step 4: Insurance
    var hasInsurance by rememberSaveable { mutableStateOf(false) }
    var insuranceProvider by rememberSaveable { mutableStateOf("") }
    var insurancePolicyNumber by rememberSaveable { mutableStateOf("") }
    var insurancePolicyholder by rememberSaveable { mutableStateOf("") }
    var insuranceValidityDateMillis by rememberSaveable { mutableLongStateOf(0L) }
    var insuranceSumInsuredText by rememberSaveable { mutableStateOf("") }
    var showInsuranceDatePicker by remember { mutableStateOf(false) }
    var step4Touched by rememberSaveable { mutableStateOf(false) }

    // Submission states
    var showConfirmSubmissionDialog by remember { mutableStateOf(false) }
    var isSubmitting by rememberSaveable { mutableStateOf(false) }

    // Load patient profile on start
    LaunchedEffect(Unit) {
        if (!profileLoadedOnce) {
            isProfileLoading = true
            val profile = portfolioRepo.getProfile()
            if (profile == null) {
                isProfileMissing = true
            } else {
                patientName = profile.name.ifBlank { AppSession.currentUserName }
                if (profile.age > 0) ageText = profile.age.toString()
                if (profile.gender.isNotBlank()) gender = profile.gender
                if (profile.bloodGroup.isNotBlank()) bloodGroup = profile.bloodGroup
                phone = profile.phone
                address = profile.address
                emergencyContactName = profile.emergencyContactName
                emergencyContactPhone = profile.emergencyContactPhone
                allergiesText = profile.allergies.joinToString(", ")
                chronicConditionsText = profile.chronicConditions.joinToString(", ")
                medicationsText = profile.currentMedications.joinToString(", ")

                // Load portfolio summary
                val summary = portfolioRepo.getSummary()
                portfolioSummaryText = summary.toShareableText()
                profileLoadedOnce = true
            }
            isProfileLoading = false
        }
    }

    // Custom back handler for the multi-step wizard
    BackHandler(enabled = true) {
        if (currentStep > 1) {
            currentStep -= 1
        } else {
            onNavigateBack()
        }
    }

    // Step 1 Validation
    val isStep1Valid = selectedHospital != null && reason.trim().length >= 10

    // Step 2 Validation
    val ageInt = ageText.toIntOrNull()
    val isAgeValid = ageInt != null && ageInt in 1..120
    val isPhoneValid = phone.trim().length == 10 && phone.trim().all { it.isDigit() }
    val isEmergencyPhoneValid = emergencyContactPhone.trim().length == 10 && emergencyContactPhone.trim().all { it.isDigit() }
    val isStep2Valid = patientName.trim().length >= 3 &&
            isAgeValid &&
            isPhoneValid &&
            address.trim().isNotBlank() &&
            emergencyContactName.trim().isNotBlank() &&
            isEmergencyPhoneValid &&
            bloodGroup in bloodGroupOptions

    // Step 3 Validation
    val isStayDaysValid = stayDaysInt in 1..60
    val isDateValid = preferredDateMillis >= DateUtils.todayStartMillis()
    val isStep3Valid = selectedRoom != null && isDateValid && isStayDaysValid

    // Step 4 Validation
    val policyRegex = remember { Regex("^[a-zA-Z0-9]{8,20}$") }
    val sumInsuredInt = insuranceSumInsuredText.toIntOrNull()
    val isInsuranceValid = if (!hasInsurance) {
        true
    } else {
        insuranceProvider.trim().isNotBlank() &&
                policyRegex.matches(insurancePolicyNumber.trim()) &&
                insurancePolicyholder.trim().isNotBlank() &&
                insuranceValidityDateMillis > preferredDateMillis &&
                (sumInsuredInt != null && sumInsuredInt > 0)
    }
    val isStep4Valid = isInsuranceValid

    fun handleSubmitAdmission() {
        if (isSubmitting) return
        val hospital = selectedHospital ?: return
        val room = selectedRoom ?: return
        val patientId = AppSession.currentPatientId
        if (patientId.isBlank()) {
            Toast.makeText(context, "Patient session invalid. Please log in again.", Toast.LENGTH_SHORT).show()
            return
        }

        isSubmitting = true
        coroutineScope.launch {
            val allergiesList = allergiesText.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
            val conditionsList = chronicConditionsText.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
            val medicationsList = medicationsText.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }

            val insurance = if (hasInsurance) {
                InsuranceDetails(
                    providerName = insuranceProvider.trim(),
                    policyNumber = insurancePolicyNumber.trim().uppercase(),
                    policyholderName = insurancePolicyholder.trim(),
                    validityDateMillis = insuranceValidityDateMillis,
                    sumInsured = sumInsuredInt ?: 0
                )
            } else null

            val admissionId = UUID.randomUUID().toString()
            val admission = Admission(
                id = admissionId,
                patientId = patientId,
                hospitalId = hospital.id,
                patientName = patientName.trim(),
                patientAge = ageInt ?: 0,
                patientGender = gender,
                patientBloodGroup = bloodGroup,
                patientPhone = phone.trim(),
                patientAddress = address.trim(),
                emergencyContactName = emergencyContactName.trim(),
                emergencyContactPhone = emergencyContactPhone.trim(),
                reason = reason.trim(),
                referringDoctor = referringDoctor.trim().takeIf { it.isNotBlank() },
                roomType = room.name,
                roomPricePerDay = room.pricePerDay,
                preferredDateMillis = preferredDateMillis,
                expectedStayDays = stayDaysInt,
                estimatedCost = estimatedCost,
                allergies = allergiesList,
                chronicConditions = conditionsList,
                currentMedications = medicationsList,
                sharedPortfolioSummary = portfolioSummaryText,
                insuranceDetails = insurance,
                status = AdmissionStatus.PENDING,
                createdAtMillis = System.currentTimeMillis()
            )

            val success = admissionRepo.add(admission)
            isSubmitting = false
            showConfirmSubmissionDialog = false

            if (success) {
                // Trigger existing broadcast notification
                val formattedDate = DateUtils.format(preferredDateMillis)
                BookingBroadcastReceiver.sendBroadcast(
                    context = context,
                    title = "Admission request sent",
                    message = "${hospital.name} • ${room.name} on $formattedDate"
                )
                onAdmissionSuccess(admissionId)
            } else {
                Toast.makeText(
                    context,
                    "Could not reach the server. Check your internet connection.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    Scaffold(
        topBar = {
            CareTopBar(
                title = "Hospital Admission",
                onBack = {
                    if (currentStep > 1) {
                        currentStep -= 1
                    } else {
                        onNavigateBack()
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Step Wizard Indicator
            WizardStepIndicator(currentStep = currentStep)

            if (isProfileLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    when (currentStep) {
                        1 -> {
                            // STEP 1: Hospital and Reason
                            SectionCard(title = "Select Hospital") {
                                ExposedDropdownMenuBox(
                                    expanded = hospitalDropdownExpanded,
                                    onExpandedChange = { hospitalDropdownExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = selectedHospital?.name ?: "Select Hospital",
                                        onValueChange = {},
                                        readOnly = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                        label = { Text("Partner Hospital") },
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = hospitalDropdownExpanded)
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = hospitalDropdownExpanded,
                                        onDismissRequest = { hospitalDropdownExpanded = false }
                                    ) {
                                        hospitals.forEach { hosp ->
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(hosp.name, fontWeight = FontWeight.Bold)
                                                        Text(
                                                            "${hosp.area}, ${hosp.city}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    selectedHospitalId = hosp.id
                                                    hospitalDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                if (selectedHospital != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "${selectedHospital.address} • Phone: ${selectedHospital.phone}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            SectionCard(title = "Reason for Admission") {
                                OutlinedTextField(
                                    value = reason,
                                    onValueChange = {
                                        reason = it
                                        step1Touched = true
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Reason for Admission *") },
                                    placeholder = { Text("e.g. Scheduled knee replacement surgery, acute chest observation...") },
                                    minLines = 3,
                                    maxLines = 5,
                                    isError = step1Touched && reason.trim().length < 10,
                                    supportingText = {
                                        if (step1Touched && reason.trim().length < 10) {
                                            Text("Minimum 10 characters required (${reason.trim().length}/10)")
                                        } else {
                                            Text("Please describe symptoms, planned procedures, or emergency reasons")
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            SectionCard(title = "Referring Doctor (Optional)") {
                                OutlinedTextField(
                                    value = referringDoctor,
                                    onValueChange = { referringDoctor = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Referring Doctor Name") },
                                    placeholder = { Text("e.g. Dr. Rajesh Sharma") },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }

                        2 -> {
                            // STEP 2: Patient Details and Auto-Fill
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Auto-filled",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Auto-filled from your Medical Portfolio - review and edit if needed",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            SectionCard(title = "Personal & Health Details") {
                                OutlinedTextField(
                                    value = patientName,
                                    onValueChange = {
                                        patientName = it
                                        step2Touched = true
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Full Name *") },
                                    isError = step2Touched && patientName.trim().length < 3,
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedTextField(
                                        value = ageText,
                                        onValueChange = {
                                            if (it.length <= 3 && it.all { ch -> ch.isDigit() }) ageText = it
                                            step2Touched = true
                                        },
                                        modifier = Modifier.weight(1f),
                                        label = { Text("Age (1-120) *") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        isError = step2Touched && !isAgeValid,
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    // Gender dropdown
                                    ExposedDropdownMenuBox(
                                        expanded = genderDropdownExpanded,
                                        onExpandedChange = { genderDropdownExpanded = it },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        OutlinedTextField(
                                            value = gender,
                                            onValueChange = {},
                                            readOnly = true,
                                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                            label = { Text("Gender *") },
                                            trailingIcon = {
                                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderDropdownExpanded)
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        ExposedDropdownMenu(
                                            expanded = genderDropdownExpanded,
                                            onDismissRequest = { genderDropdownExpanded = false }
                                        ) {
                                            genderOptions.forEach { opt ->
                                                DropdownMenuItem(
                                                    text = { Text(opt) },
                                                    onClick = {
                                                        gender = opt
                                                        genderDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Blood Group dropdown
                                    ExposedDropdownMenuBox(
                                        expanded = bloodGroupDropdownExpanded,
                                        onExpandedChange = { bloodGroupDropdownExpanded = it },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        OutlinedTextField(
                                            value = bloodGroup,
                                            onValueChange = {},
                                            readOnly = true,
                                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                            label = { Text("Blood Group *") },
                                            trailingIcon = {
                                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = bloodGroupDropdownExpanded)
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        ExposedDropdownMenu(
                                            expanded = bloodGroupDropdownExpanded,
                                            onDismissRequest = { bloodGroupDropdownExpanded = false }
                                        ) {
                                            bloodGroupOptions.forEach { bg ->
                                                DropdownMenuItem(
                                                    text = { Text(bg) },
                                                    onClick = {
                                                        bloodGroup = bg
                                                        bloodGroupDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = phone,
                                        onValueChange = {
                                            if (it.length <= 10 && it.all { ch -> ch.isDigit() }) phone = it
                                            step2Touched = true
                                        },
                                        modifier = Modifier.weight(1.5f),
                                        label = { Text("Phone (10 digits) *") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        isError = step2Touched && !isPhoneValid,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = address,
                                    onValueChange = {
                                        address = it
                                        step2Touched = true
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Residential Address *") },
                                    isError = step2Touched && address.trim().isBlank(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            SectionCard(title = "Emergency Contact") {
                                OutlinedTextField(
                                    value = emergencyContactName,
                                    onValueChange = {
                                        emergencyContactName = it
                                        step2Touched = true
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Emergency Contact Name *") },
                                    isError = step2Touched && emergencyContactName.trim().isBlank(),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = emergencyContactPhone,
                                    onValueChange = {
                                        if (it.length <= 10 && it.all { ch -> ch.isDigit() }) emergencyContactPhone = it
                                        step2Touched = true
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Emergency Contact Phone (10 digits) *") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    isError = step2Touched && !isEmergencyPhoneValid,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            SectionCard(title = "Medical Background") {
                                OutlinedTextField(
                                    value = allergiesText,
                                    onValueChange = { allergiesText = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Known Allergies (comma-separated)") },
                                    placeholder = { Text("e.g. Penicillin, Peanuts, Sulfa") },
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = chronicConditionsText,
                                    onValueChange = { chronicConditionsText = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Chronic Conditions (comma-separated)") },
                                    placeholder = { Text("e.g. Hypertension, Diabetes Type 2") },
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = medicationsText,
                                    onValueChange = { medicationsText = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Current Medications (comma-separated)") },
                                    placeholder = { Text("e.g. Metformin 500mg, Amlodipine 5mg") },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }

                        3 -> {
                            // STEP 3: Room and Dates
                            SectionCard(title = "Select Room Category") {
                                if (roomTypes.isEmpty()) {
                                    Text(
                                        text = "No room categories listed for this hospital.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    roomTypes.forEach { room ->
                                        val isSelected = selectedRoom?.name == room.name
                                        OutlinedCard(
                                            onClick = { selectedRoomName = room.name },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.outlinedCardColors(
                                                containerColor = if (isSelected) {
                                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                                } else {
                                                    MaterialTheme.colorScheme.surface
                                                }
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = isSelected,
                                                    onClick = { selectedRoomName = room.name }
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = room.name,
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = "₹${room.pricePerDay} / day",
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = room.description,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            SectionCard(title = "Dates & Duration") {
                                OutlinedCard(
                                    onClick = { showAdmissionDatePicker = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Preferred Admission Date *",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = if (preferredDateMillis > 0) {
                                                    DateUtils.formatWithDay(preferredDateMillis)
                                                } else {
                                                    "Tap to select date (Today or later)"
                                                },
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (preferredDateMillis > 0) {
                                                    MaterialTheme.colorScheme.onSurface
                                                } else {
                                                    MaterialTheme.colorScheme.primary
                                                }
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = "Pick Date",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                if (step3Touched && !isDateValid) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Please choose a valid admission date (today or later)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                OutlinedTextField(
                                    value = expectedStayDaysText,
                                    onValueChange = {
                                        if (it.length <= 2 && it.all { ch -> ch.isDigit() }) expectedStayDaysText = it
                                        step3Touched = true
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Expected Stay Duration (1-60 Days) *") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    isError = step3Touched && !isStayDaysValid,
                                    supportingText = {
                                        if (step3Touched && !isStayDaysValid) {
                                            Text("Duration must be between 1 and 60 days", color = MaterialTheme.colorScheme.error)
                                        } else {
                                            Text("Used for bed reservation and initial deposit calculation")
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Live Estimated Cost Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Estimated Stay Cost",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "${selectedRoom?.name ?: "Room"} • $stayDaysInt day(s)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                    Text(
                                        text = "₹$estimatedCost",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        4 -> {
                            // STEP 4: Insurance and Review
                            SectionCard(title = "Health Insurance Coverage") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "I have health insurance",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "Enable cashless or reimbursement claim processing",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = hasInsurance,
                                        onCheckedChange = { hasInsurance = it }
                                    )
                                }

                                if (hasInsurance) {
                                    Spacer(modifier = Modifier.height(16.dp))

                                    OutlinedTextField(
                                        value = insuranceProvider,
                                        onValueChange = {
                                            insuranceProvider = it
                                            step4Touched = true
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("TPA / Insurance Provider Name *") },
                                        placeholder = { Text("e.g. Star Health, HDFC ERGO, ICICI Lombard") },
                                        isError = step4Touched && insuranceProvider.trim().isBlank(),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = insurancePolicyNumber,
                                        onValueChange = {
                                            if (it.length <= 20) insurancePolicyNumber = it.uppercase()
                                            step4Touched = true
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("Policy Number (8-20 alphanumeric) *") },
                                        placeholder = { Text("e.g. POL12345678") },
                                        isError = step4Touched && !policyRegex.matches(insurancePolicyNumber.trim()),
                                        supportingText = {
                                            if (step4Touched && !policyRegex.matches(insurancePolicyNumber.trim())) {
                                                Text("8 to 20 alphanumeric characters required", color = MaterialTheme.colorScheme.error)
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = insurancePolicyholder,
                                        onValueChange = {
                                            insurancePolicyholder = it
                                            step4Touched = true
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("Policyholder Name *") },
                                        placeholder = { Text("e.g. Rahul Verma") },
                                        isError = step4Touched && insurancePolicyholder.trim().isBlank(),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedCard(
                                        onClick = { showInsuranceDatePicker = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Policy Validity Expiry Date *",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = if (insuranceValidityDateMillis > 0) {
                                                        DateUtils.format(insuranceValidityDateMillis)
                                                    } else {
                                                        "Tap to select expiry date (after admission date)"
                                                    },
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (insuranceValidityDateMillis > 0) {
                                                        MaterialTheme.colorScheme.onSurface
                                                    } else {
                                                        MaterialTheme.colorScheme.primary
                                                    }
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Default.CalendarMonth,
                                                contentDescription = "Pick Validity Date",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    if (step4Touched && insuranceValidityDateMillis <= preferredDateMillis) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Policy validity must expire after the preferred admission date",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = insuranceSumInsuredText,
                                        onValueChange = {
                                            if (it.length <= 9 && it.all { ch -> ch.isDigit() }) insuranceSumInsuredText = it
                                            step4Touched = true
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("Sum Insured in INR *") },
                                        placeholder = { Text("e.g. 500000") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        isError = step4Touched && (sumInsuredInt == null || sumInsuredInt <= 0),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Review Section Card
                            SectionCard(title = "Review Admission Request") {
                                ReviewItemRow("Hospital", "${selectedHospital?.name} (${selectedHospital?.area})")
                                ReviewItemRow("Reason", reason)
                                if (referringDoctor.isNotBlank()) {
                                    ReviewItemRow("Referring Doctor", referringDoctor)
                                }
                                ReviewItemRow("Patient", "$patientName ($ageText yrs, $gender, $bloodGroup)")
                                ReviewItemRow("Contact", "$phone (Emergency: $emergencyContactName, $emergencyContactPhone)")
                                ReviewItemRow("Room Type", "${selectedRoom?.name} @ ₹${selectedRoom?.pricePerDay}/day")
                                ReviewItemRow("Preferred Date", DateUtils.formatWithDay(preferredDateMillis))
                                ReviewItemRow("Expected Stay", "$stayDaysInt days")
                                ReviewItemRow("Estimated Cost", "₹$estimatedCost")
                                ReviewItemRow(
                                    "Insurance",
                                    if (hasInsurance) "$insuranceProvider • No: $insurancePolicyNumber • Cover: ₹$insuranceSumInsuredText" else "No Insurance (Self-pay)"
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Shareable Medical Summary",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Only this summary is forwarded to the hospital",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = portfolioSummaryText.ifBlank { "No previous medical summary found. Form values will be forwarded." },
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(12.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Bottom Action Bar: Back and Next / Submit
                Surface(
                    tonalElevation = 3.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentStep > 1) {
                            OutlinedButton(
                                onClick = { currentStep -= 1 },
                                modifier = Modifier.weight(1f),
                                enabled = !isSubmitting,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Back")
                            }
                        }

                        if (currentStep < 4) {
                            Button(
                                onClick = {
                                    when (currentStep) {
                                        1 -> {
                                            step1Touched = true
                                            if (isStep1Valid) currentStep = 2
                                        }
                                        2 -> {
                                            step2Touched = true
                                            if (isStep2Valid) currentStep = 3
                                        }
                                        3 -> {
                                            step3Touched = true
                                            if (isStep3Valid) currentStep = 4
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                enabled = when (currentStep) {
                                    1 -> isStep1Valid
                                    2 -> isStep2Valid
                                    3 -> isStep3Valid
                                    else -> false
                                }
                            ) {
                                Text("Next")
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            PrimaryButton(
                                text = if (isSubmitting) "Submitting..." else "Submit Request",
                                onClick = {
                                    step4Touched = true
                                    if (isStep4Valid) {
                                        showConfirmSubmissionDialog = true
                                    }
                                },
                                enabled = isStep4Valid && !isSubmitting,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }

    // Step 2 Gate: Medical Portfolio Missing Alert Dialog
    if (isProfileMissing) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(
                    text = "Medical Portfolio Required",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("To request automated hospital admission, you must first create your Medical Portfolio so your basic health details and emergency contacts can be forwarded.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        isProfileMissing = false
                        onCreateProfile()
                    }
                ) {
                    Text("Create Portfolio")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        isProfileMissing = false
                        onNavigateBack()
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Date Picker for Preferred Admission Date
    if (showAdmissionDatePicker) {
        val today = DateUtils.todayStartMillis()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = if (preferredDateMillis > 0) preferredDateMillis else today,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return DateUtils.normalizeToDayStart(utcTimeMillis) >= today
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showAdmissionDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            val norm = DateUtils.normalizeToDayStart(it)
                            if (norm >= today) {
                                preferredDateMillis = norm
                            }
                        }
                        showAdmissionDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdmissionDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Date Picker for Insurance Validity
    if (showInsuranceDatePicker) {
        val minDate = if (preferredDateMillis > 0) preferredDateMillis + 86400000L else DateUtils.todayStartMillis()
        val insurancePickerState = rememberDatePickerState(
            initialSelectedDateMillis = if (insuranceValidityDateMillis > 0) insuranceValidityDateMillis else minDate
        )
        DatePickerDialog(
            onDismissRequest = { showInsuranceDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        insurancePickerState.selectedDateMillis?.let {
                            insuranceValidityDateMillis = DateUtils.normalizeToDayStart(it)
                        }
                        showInsuranceDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInsuranceDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = insurancePickerState)
        }
    }

    // Confirmation dialog before submitting admission request
    if (showConfirmSubmissionDialog && selectedHospital != null && selectedRoom != null) {
        ConfirmDialog(
            title = "Submit Admission Request?",
            message = "Your request for a ${selectedRoom.name} at ${selectedHospital.name} on ${DateUtils.format(preferredDateMillis)} will be forwarded to the hospital desk for review and bed assignment.",
            confirmText = "Submit",
            onConfirm = { handleSubmitAdmission() },
            onDismiss = { if (!isSubmitting) showConfirmSubmissionDialog = false }
        )
    }
}

@Composable
private fun ReviewItemRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(2f)
        )
    }
}

@Composable
private fun WizardStepIndicator(currentStep: Int) {
    val steps = listOf("Hospital", "Patient", "Room & Stay", "Review")
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEachIndexed { index, title ->
                val stepNum = index + 1
                val isCompleted = stepNum < currentStep
                val isCurrent = stepNum == currentStep

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isCompleted -> MaterialTheme.colorScheme.primary
                                    isCurrent -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text(
                                text = "$stepNum",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrent) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCurrent) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AdmissionFormScreenPreview() {
    CareConnectTheme {
        AdmissionFormScreen(
            onNavigateBack = {},
            onCreateProfile = {},
            onAdmissionSuccess = {}
        )
    }
}
