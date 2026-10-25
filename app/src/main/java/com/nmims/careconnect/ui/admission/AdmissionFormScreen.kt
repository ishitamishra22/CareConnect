package com.nmims.careconnect.ui.admission

/*
 * AdmissionFormScreen:
 * Four-step wizard for automated hospital admission requests:
 * 1. Hospital & Reason: Partner hospital dropdown selection, reason for admission (min 10 chars), optional referring doctor.
 * 2. Patient Details: Demographics, emergency contact, and clinical history auto-filled from Medical Portfolio.
 * 3. Room & Dates: Room category selection, preferred admission date picker, stay duration, and live cost calculation.
 * 4. Insurance & Review: Optional health insurance details with self-pay opt-out, policy validity date picker, and comprehensive final admission review.
 */

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nmims.careconnect.data.model.InsuranceDetails
import com.nmims.careconnect.data.model.RoomType
import com.nmims.careconnect.data.repository.HospitalRepository
import com.nmims.careconnect.data.repository.PortfolioRepositoryProvider
import com.nmims.careconnect.ui.common.CareTopBar
import com.nmims.careconnect.ui.common.DateUtils
import com.nmims.careconnect.ui.common.SectionCard
import com.nmims.careconnect.ui.theme.CareConnectTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdmissionFormScreen(
    initialHospitalId: String? = null,
    onNavigateBack: () -> Unit,
    onCreateProfile: () -> Unit = {},
    onAdmissionSuccess: (String) -> Unit = {}
) {
    // Current wizard step state (1 to 4), preserved across configuration changes
    var currentStep by rememberSaveable { mutableIntStateOf(1) }

    // Step 1: Hospital and Reason Form State
    val hospitals = remember { HospitalRepository.getHospitals() }
    var selectedHospitalId by rememberSaveable {
        mutableStateOf(initialHospitalId ?: hospitals.firstOrNull()?.id ?: "")
    }
    var hospitalDropdownExpanded by remember { mutableStateOf(false) }
    var reason by rememberSaveable { mutableStateOf("") }
    var referringDoctor by rememberSaveable { mutableStateOf("") }
    var step1Touched by rememberSaveable { mutableStateOf(false) }

    val selectedHospital = hospitals.find { it.id == selectedHospitalId }

    // Step 1 Validation Logic
    val isHospitalValid = selectedHospital != null && selectedHospitalId.isNotBlank()
    val isReasonValid = reason.trim().length >= 10
    val isStep1Valid = isHospitalValid && isReasonValid

    val hospitalError: String? = if (step1Touched && !isHospitalValid) {
        "Please select a partner hospital."
    } else null

    val reasonError: String? = when {
        !step1Touched -> null
        reason.trim().isEmpty() -> "Reason for admission is required."
        reason.trim().length < 10 -> "Please provide at least 10 characters (${reason.trim().length}/10)."
        else -> null
    }

    // Step 2: Patient Details Form State
    val context = LocalContext.current
    val portfolioRepo = remember { PortfolioRepositoryProvider.get(context) }
    val isPreviewMode = LocalInspectionMode.current

    var patientName by rememberSaveable { mutableStateOf("") }
    var patientAgeText by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf("") }
    var bloodGroup by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var emergencyContactName by rememberSaveable { mutableStateOf("") }
    var emergencyContactPhone by rememberSaveable { mutableStateOf("") }
    var allergiesText by rememberSaveable { mutableStateOf("") }
    var chronicConditionsText by rememberSaveable { mutableStateOf("") }
    var medicationsText by rememberSaveable { mutableStateOf("") }

    var genderDropdownExpanded by remember { mutableStateOf(false) }
    var bloodGroupDropdownExpanded by remember { mutableStateOf(false) }
    var isLoadingProfile by rememberSaveable { mutableStateOf(false) }
    var hasLoadedProfile by rememberSaveable { mutableStateOf(false) }
    var showMissingProfileDialog by rememberSaveable { mutableStateOf(false) }
    var profileLoadError by rememberSaveable { mutableStateOf<String?>(null) }
    var profileRetryTrigger by rememberSaveable { mutableIntStateOf(0) }
    var step2Touched by rememberSaveable { mutableStateOf(false) }

    val genderOptions = listOf("Male", "Female", "Other")
    val bloodGroupOptions = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")

    // Auto-fill profile from PortfolioRepository when Step 2 is shown
    LaunchedEffect(currentStep, profileRetryTrigger) {
        if (currentStep == 2 && !hasLoadedProfile) {
            if (isPreviewMode) {
                patientName = "Rahul Sharma"
                patientAgeText = "32"
                gender = "Male"
                bloodGroup = "B+"
                phone = "9876543210"
                address = "Flat 402, Sunshine Apts, Bandra West, Mumbai"
                emergencyContactName = "Priya Sharma"
                emergencyContactPhone = "9876543211"
                allergiesText = "Penicillin, Dust"
                chronicConditionsText = "Asthma"
                medicationsText = "Inhaler (as needed)"
                hasLoadedProfile = true
            } else {
                try {
                    isLoadingProfile = true
                    profileLoadError = null
                    val profile = portfolioRepo.getProfile()
                    if (profile != null) {
                        patientName = profile.name
                        patientAgeText = if (profile.age > 0) profile.age.toString() else ""
                        val matchedGender = genderOptions.firstOrNull { it.equals(profile.gender.trim(), ignoreCase = true) }
                        gender = matchedGender ?: ""
                        val matchedBloodGroup = bloodGroupOptions.firstOrNull { it.equals(profile.bloodGroup.trim(), ignoreCase = true) }
                        bloodGroup = matchedBloodGroup ?: ""
                        phone = profile.phone
                        address = profile.address
                        emergencyContactName = profile.emergencyContactName
                        emergencyContactPhone = profile.emergencyContactPhone
                        allergiesText = profile.allergies.joinToString(", ")
                        chronicConditionsText = profile.chronicConditions.joinToString(", ")
                        medicationsText = profile.currentMedications.joinToString(", ")
                        hasLoadedProfile = true
                    } else {
                        showMissingProfileDialog = true
                    }
                } catch (e: Exception) {
                    profileLoadError = e.localizedMessage ?: "Failed to load medical portfolio. Please check your connection and try again."
                } finally {
                    isLoadingProfile = false
                }
            }
        }
    }

    // Step 2 Validation Logic (identical rules to EditProfileScreen)
    val trimmedPatientName = patientName.trim()
    val isPatientNameValid = trimmedPatientName.length >= 3 && trimmedPatientName.all { it.isLetter() || it.isWhitespace() }

    val patientAgeInt = patientAgeText.toIntOrNull()
    val isPatientAgeValid = patientAgeInt != null && patientAgeInt in 1..120

    val isGenderValid = gender.isNotBlank() && gender in genderOptions

    val isBloodGroupValid = bloodGroup.isNotBlank() && bloodGroup in bloodGroupOptions

    val trimmedPhone = phone.trim()
    val isPhoneValid = trimmedPhone.length == 10 && trimmedPhone.all { it.isDigit() }

    val trimmedAddress = address.trim()
    val isAddressValid = trimmedAddress.isNotBlank()

    val trimmedEmergName = emergencyContactName.trim()
    val isEmergNameValid = trimmedEmergName.isNotBlank()

    val trimmedEmergPhone = emergencyContactPhone.trim()
    val isEmergPhoneValid = trimmedEmergPhone.length == 10 && trimmedEmergPhone.all { it.isDigit() }

    val isStep2Valid = isPatientNameValid && isPatientAgeValid && isGenderValid &&
            isBloodGroupValid && isPhoneValid && isAddressValid &&
            isEmergNameValid && isEmergPhoneValid

    val patientNameError: String? = when {
        patientName.isNotBlank() && !isPatientNameValid -> {
            if (trimmedPatientName.length < 3) "Min 3 characters required."
            else "Letters and spaces only."
        }
        step2Touched && trimmedPatientName.isEmpty() -> "Full name is required."
        else -> null
    }

    val patientAgeError: String? = when {
        patientAgeText.isNotBlank() && !isPatientAgeValid -> "Age must be between 1 and 120."
        step2Touched && patientAgeText.isEmpty() -> "Age is required."
        else -> null
    }

    val genderError: String? = if (step2Touched && !isGenderValid) {
        "Please select a gender."
    } else null

    val bloodGroupError: String? = if (step2Touched && !isBloodGroupValid) {
        "Please select a valid blood group."
    } else null

    val phoneError: String? = when {
        phone.isNotBlank() && !isPhoneValid -> "Exactly 10 digits required."
        step2Touched && trimmedPhone.isEmpty() -> "Phone number is required."
        else -> null
    }

    val addressError: String? = if (step2Touched && !isAddressValid) {
        "Residential address is required."
    } else null

    val emergNameError: String? = if (step2Touched && !isEmergNameValid) {
        "Emergency contact name is required."
    } else null

    val emergPhoneError: String? = when {
        emergencyContactPhone.isNotBlank() && !isEmergPhoneValid -> "Exactly 10 digits required."
        step2Touched && trimmedEmergPhone.isEmpty() -> "Emergency contact phone is required."
        else -> null
    }

    // Step 3: Room & Dates Form State
    val todayStartMillis = remember { DateUtils.todayStartMillis() }
    var selectedRoomTypeName by rememberSaveable { mutableStateOf("") }
    var preferredDateMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var expectedStayDaysText by rememberSaveable { mutableStateOf("") }
    var showDatePickerDialog by rememberSaveable { mutableStateOf(false) }
    var step3Touched by rememberSaveable { mutableStateOf(false) }

    val availableRoomTypes = selectedHospital?.roomTypes ?: emptyList()
    val selectedRoomType = availableRoomTypes.firstOrNull { it.name == selectedRoomTypeName }

    // Clear room selection if hospital changes and selected room is not available in new hospital
    LaunchedEffect(selectedHospitalId) {
        if (selectedRoomTypeName.isNotBlank() && availableRoomTypes.none { it.name == selectedRoomTypeName }) {
            selectedRoomTypeName = ""
        }
    }

    // Step 3 Validation Logic
    val isRoomTypeValid = selectedRoomType != null
    val isDateValid = preferredDateMillis != null && preferredDateMillis!! >= todayStartMillis
    val stayDaysInt = expectedStayDaysText.toIntOrNull()
    val isStayDaysValid = stayDaysInt != null && stayDaysInt in 1..365
    val isStep3Valid = isRoomTypeValid && isDateValid && isStayDaysValid

    val roomTypeError: String? = if (step3Touched && !isRoomTypeValid) {
        "Please select a room category."
    } else null

    val preferredDateError: String? = when {
        step3Touched && preferredDateMillis == null -> "Please select a preferred admission date."
        preferredDateMillis != null && preferredDateMillis!! < todayStartMillis -> "Admission date cannot be in the past."
        else -> null
    }

    val stayDaysError: String? = when {
        expectedStayDaysText.isNotBlank() && !isStayDaysValid -> "Stay duration must be between 1 and 365 days."
        step3Touched && expectedStayDaysText.isBlank() -> "Expected stay duration is required (1 to 365 days)."
        else -> null
    }

    // Live estimated cost calculation guarded against arithmetic overflow
    val pricePerDay = selectedRoomType?.pricePerDay ?: 0
    val stayDays = stayDaysInt ?: 0
    val estimatedCostLong = pricePerDay.toLong() * stayDays.toLong()
    val estimatedCostInt = if (estimatedCostLong > Int.MAX_VALUE) Int.MAX_VALUE else estimatedCostLong.toInt()

    // Step 4: Insurance & Review Form State
    var noInsurance by rememberSaveable { mutableStateOf(false) }
    var insuranceProvider by rememberSaveable { mutableStateOf("") }
    var policyNumber by rememberSaveable { mutableStateOf("") }
    var policyHolderName by rememberSaveable { mutableStateOf("") }
    var insuranceValidTillMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var sumInsuredText by rememberSaveable { mutableStateOf("") }
    var showInsuranceDatePickerDialog by rememberSaveable { mutableStateOf(false) }
    var step4Touched by rememberSaveable { mutableStateOf(false) }
    var showReviewReadyDialog by rememberSaveable { mutableStateOf(false) }

    // Pre-fill policyholder name with patient name when entering Step 4 if not yet set
    LaunchedEffect(currentStep, patientName) {
        if (currentStep == 4 && policyHolderName.isBlank() && patientName.isNotBlank() && !noInsurance) {
            policyHolderName = patientName.trim()
        }
    }

    // Step 4 Validation Logic
    val trimmedProvider = insuranceProvider.trim()
    val isProviderValid = noInsurance || trimmedProvider.length >= 2

    val trimmedPolicyNumber = policyNumber.trim()
    val isPolicyNumberValid = noInsurance || trimmedPolicyNumber.length >= 3

    val trimmedPolicyHolder = policyHolderName.trim()
    val isPolicyHolderValid = noInsurance || (trimmedPolicyHolder.length >= 3 && trimmedPolicyHolder.all { it.isLetter() || it.isWhitespace() })

    val isInsuranceDateValid = noInsurance || (insuranceValidTillMillis != null && insuranceValidTillMillis!! >= todayStartMillis)

    val sumInsuredInt = sumInsuredText.toIntOrNull()
    val isSumInsuredValid = noInsurance || (sumInsuredInt != null && sumInsuredInt > 0)

    val isInsuranceValid = if (noInsurance) {
        true
    } else {
        isProviderValid && isPolicyNumberValid && isPolicyHolderValid && isInsuranceDateValid && isSumInsuredValid
    }

    val isStep4Valid = isStep1Valid && isStep2Valid && isStep3Valid && isInsuranceValid

    val providerError: String? = when {
        noInsurance -> null
        insuranceProvider.isNotBlank() && !isProviderValid -> "Provider name must be at least 2 characters."
        step4Touched && trimmedProvider.isEmpty() -> "Insurance provider is required."
        else -> null
    }

    val policyNumberError: String? = when {
        noInsurance -> null
        policyNumber.isNotBlank() && !isPolicyNumberValid -> "Policy number must be at least 3 characters."
        step4Touched && trimmedPolicyNumber.isEmpty() -> "Policy number is required."
        else -> null
    }

    val policyHolderError: String? = when {
        noInsurance -> null
        policyHolderName.isNotBlank() && !isPolicyHolderValid -> {
            if (trimmedPolicyHolder.length < 3) "Min 3 characters required."
            else "Letters and spaces only."
        }
        step4Touched && trimmedPolicyHolder.isEmpty() -> "Policyholder name is required."
        else -> null
    }

    val insuranceDateError: String? = when {
        noInsurance -> null
        step4Touched && insuranceValidTillMillis == null -> "Policy validity date is required."
        insuranceValidTillMillis != null && insuranceValidTillMillis!! < todayStartMillis -> "Policy validity date cannot be in the past."
        else -> null
    }

    val sumInsuredError: String? = when {
        noInsurance -> null
        sumInsuredText.isNotBlank() && !isSumInsuredValid -> "Sum insured must be greater than 0."
        step4Touched && sumInsuredText.isBlank() -> "Sum insured amount is required."
        else -> null
    }

    // Custom back handler for multi-step wizard navigation
    BackHandler(enabled = true) {
        if (currentStep > 1) {
            currentStep -= 1
        } else {
            onNavigateBack()
        }
    }

    // Step validation gate: verifies current step requirements before advancing
    fun validateCurrentStep(step: Int): Boolean {
        return when (step) {
            1 -> isStep1Valid
            2 -> isStep2Valid
            3 -> isStep3Valid
            4 -> isStep4Valid
            else -> false
        }
    }

    // Missing Profile Alert Dialog
    if (showMissingProfileDialog) {
        AlertDialog(
            onDismissRequest = {
                showMissingProfileDialog = false
                currentStep = 1
            },
            title = {
                Text("Medical Portfolio Required", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Create your Medical Portfolio first so we can auto-fill your admission form")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showMissingProfileDialog = false
                        onCreateProfile()
                    }
                ) {
                    Text("Create Profile")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showMissingProfileDialog = false
                        currentStep = 1
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Material 3 Date Picker Dialog for Step 3
    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return DateUtils.normalizeToDayStart(utcTimeMillis) >= DateUtils.todayStartMillis()
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            val localMidnight = DateUtils.normalizeToDayStart(utcMillis)
                            if (localMidnight >= DateUtils.todayStartMillis()) {
                                preferredDateMillis = localMidnight
                            }
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Material 3 Date Picker Dialog for Policy Validity
    if (showInsuranceDatePickerDialog) {
        val insDatePickerState = rememberDatePickerState(
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return DateUtils.normalizeToDayStart(utcTimeMillis) >= DateUtils.todayStartMillis()
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showInsuranceDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        insDatePickerState.selectedDateMillis?.let { utcMillis ->
                            val localMidnight = DateUtils.normalizeToDayStart(utcMillis)
                            if (localMidnight >= DateUtils.todayStartMillis()) {
                                insuranceValidTillMillis = localMidnight
                            }
                        }
                        showInsuranceDatePickerDialog = false
                    }
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showInsuranceDatePickerDialog = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = insDatePickerState)
        }
    }

    // Step 4 Review Ready Confirmation Dialog (validates form without writing to Firestore or triggering broadcasts)
    if (showReviewReadyDialog) {
        AlertDialog(
            onDismissRequest = { showReviewReadyDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Admission Review Complete",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "All admission details for ${trimmedPatientName.ifBlank { "the patient" }} at ${selectedHospital?.name ?: "the hospital"} have been verified and are ready for submission.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(onClick = { showReviewReadyDialog = false }) {
                    Text("OK")
                }
            }
        )
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
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = { currentStep -= 1 },
                            modifier = Modifier.weight(1f),
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

                    Button(
                        onClick = {
                            if (currentStep == 1) {
                                step1Touched = true
                            } else if (currentStep == 2) {
                                step2Touched = true
                            } else if (currentStep == 3) {
                                step3Touched = true
                            } else if (currentStep == 4) {
                                step4Touched = true
                            }
                            if (currentStep < 4) {
                                if (validateCurrentStep(currentStep)) {
                                    currentStep += 1
                                }
                            } else {
                                step4Touched = true
                                if (validateCurrentStep(4)) {
                                    showReviewReadyDialog = true
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = validateCurrentStep(currentStep)
                    ) {
                        if (currentStep < 4) {
                            Text("Next")
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Text("Review & Submit")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Step Wizard Indicator
            WizardStepIndicator(currentStep = currentStep)

            // Step Content Area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                when (currentStep) {
                    1 -> {
                        // STEP 1: Hospital & Reason
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
                                    label = { Text("Partner Hospital *") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = hospitalDropdownExpanded)
                                    },
                                    isError = hospitalError != null,
                                    supportingText = if (hospitalError != null) {
                                        { Text(hospitalError, color = MaterialTheme.colorScheme.error) }
                                    } else null,
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
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = selectedHospital.address,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Phone: ${selectedHospital.phone} • Rating: ${selectedHospital.rating} ★",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        SectionCard(title = "Admission Details") {
                            OutlinedTextField(
                                value = reason,
                                onValueChange = { reason = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Reason for Admission *") },
                                placeholder = { Text("Describe diagnosis, symptoms, or recommended surgery...") },
                                minLines = 3,
                                maxLines = 5,
                                isError = reasonError != null,
                                supportingText = {
                                    if (reasonError != null) {
                                        Text(reasonError, color = MaterialTheme.colorScheme.error)
                                    } else {
                                        Text(
                                            text = "${reason.trim().length} characters (min 10 required)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = referringDoctor,
                                onValueChange = { referringDoctor = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Referring Doctor (Optional)") },
                                placeholder = { Text("e.g. Dr. A. Sharma (Cardiologist)") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.MedicalServices,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                supportingText = {
                                    Text("Doctor who recommended this hospital admission, if applicable")
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                    2 -> {
                        // STEP 2: Patient Details
                        if (isLoadingProfile) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(36.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 3.dp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Loading patient portfolio...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else if (profileLoadError != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Unable to Load Portfolio",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = profileLoadError ?: "Failed to load medical portfolio.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        OutlinedButton(onClick = { currentStep = 1 }) {
                                            Text("Back")
                                        }
                                        Button(
                                            onClick = {
                                                profileLoadError = null
                                                profileRetryTrigger++
                                            }
                                        ) {
                                            Text("Retry")
                                        }
                                    }
                                }
                            }
                        } else if (!hasLoadedProfile) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Medical Portfolio Required",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Create your Medical Portfolio first so we can auto-fill your admission form",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        OutlinedButton(onClick = { currentStep = 1 }) {
                                            Text("Back")
                                        }
                                        Button(
                                            onClick = {
                                                showMissingProfileDialog = false
                                                onCreateProfile()
                                            }
                                        ) {
                                            Text("Create Profile")
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(
                                        onClick = {
                                            profileLoadError = null
                                            profileRetryTrigger++
                                        }
                                    ) {
                                        Text("Check Again")
                                    }
                                }
                            }
                        } else {
                            // Highlighted information banner
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Auto-filled from your Medical Portfolio — review and edit if needed",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Personal Information Section
                            SectionCard(title = "Personal Information") {
                                OutlinedTextField(
                                    value = patientName,
                                    onValueChange = { patientName = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Full Name *") },
                                    isError = patientNameError != null,
                                    supportingText = if (patientNameError != null) {
                                        { Text(patientNameError, color = MaterialTheme.colorScheme.error) }
                                    } else null,
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedTextField(
                                        value = patientAgeText,
                                        onValueChange = { if (it.length <= 3 && it.all { ch -> ch.isDigit() }) patientAgeText = it },
                                        modifier = Modifier.weight(1f),
                                        label = { Text("Age *") },
                                        isError = patientAgeError != null,
                                        supportingText = if (patientAgeError != null) {
                                            { Text(patientAgeError, color = MaterialTheme.colorScheme.error) }
                                        } else {
                                            { Text("1-120 yrs") }
                                        },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    ExposedDropdownMenuBox(
                                        expanded = genderDropdownExpanded,
                                        onExpandedChange = { genderDropdownExpanded = it },
                                        modifier = Modifier.weight(1.2f)
                                    ) {
                                        OutlinedTextField(
                                            value = gender,
                                            onValueChange = {},
                                            readOnly = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                            label = { Text("Gender *") },
                                            placeholder = { Text("Select Gender") },
                                            trailingIcon = {
                                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderDropdownExpanded)
                                            },
                                            isError = genderError != null,
                                            supportingText = if (genderError != null) {
                                                { Text(genderError, color = MaterialTheme.colorScheme.error) }
                                            } else null,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        ExposedDropdownMenu(
                                            expanded = genderDropdownExpanded,
                                            onDismissRequest = { genderDropdownExpanded = false }
                                        ) {
                                            genderOptions.forEach { option ->
                                                DropdownMenuItem(
                                                    text = { Text(option) },
                                                    onClick = {
                                                        gender = option
                                                        genderDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                ExposedDropdownMenuBox(
                                    expanded = bloodGroupDropdownExpanded,
                                    onExpandedChange = { bloodGroupDropdownExpanded = it },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = bloodGroup,
                                        onValueChange = {},
                                        readOnly = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                        label = { Text("Blood Group *") },
                                        placeholder = { Text("Select Blood Group") },
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = bloodGroupDropdownExpanded)
                                        },
                                        isError = bloodGroupError != null,
                                        supportingText = if (bloodGroupError != null) {
                                            { Text(bloodGroupError, color = MaterialTheme.colorScheme.error) }
                                        } else null,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = bloodGroupDropdownExpanded,
                                        onDismissRequest = { bloodGroupDropdownExpanded = false }
                                    ) {
                                        bloodGroupOptions.forEach { option ->
                                            DropdownMenuItem(
                                                text = { Text(option) },
                                                onClick = {
                                                    bloodGroup = option
                                                    bloodGroupDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Contact & Residential Details Section
                            SectionCard(title = "Contact & Residential Details") {
                                OutlinedTextField(
                                    value = phone,
                                    onValueChange = { if (it.length <= 10 && it.all { ch -> ch.isDigit() }) phone = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Phone Number (10 digits) *") },
                                    isError = phoneError != null,
                                    supportingText = if (phoneError != null) {
                                        { Text(phoneError, color = MaterialTheme.colorScheme.error) }
                                    } else {
                                        { Text("10-digit mobile number") }
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = address,
                                    onValueChange = { address = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Residential Address *") },
                                    placeholder = { Text("House/Flat No., Street, City, Pincode") },
                                    minLines = 2,
                                    maxLines = 3,
                                    isError = addressError != null,
                                    supportingText = if (addressError != null) {
                                        { Text(addressError, color = MaterialTheme.colorScheme.error) }
                                    } else null,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Emergency Contact Section
                            SectionCard(title = "Emergency Contact") {
                                OutlinedTextField(
                                    value = emergencyContactName,
                                    onValueChange = { emergencyContactName = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Emergency Contact Name *") },
                                    isError = emergNameError != null,
                                    supportingText = if (emergNameError != null) {
                                        { Text(emergNameError, color = MaterialTheme.colorScheme.error) }
                                    } else null,
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = emergencyContactPhone,
                                    onValueChange = { if (it.length <= 10 && it.all { ch -> ch.isDigit() }) emergencyContactPhone = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Emergency Contact Phone (10 digits) *") },
                                    isError = emergPhoneError != null,
                                    supportingText = if (emergPhoneError != null) {
                                        { Text(emergPhoneError, color = MaterialTheme.colorScheme.error) }
                                    } else {
                                        { Text("10-digit mobile number") }
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Medical Background Section
                            SectionCard(title = "Medical Background (Optional)") {
                                OutlinedTextField(
                                    value = allergiesText,
                                    onValueChange = { allergiesText = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Allergies") },
                                    placeholder = { Text("e.g. Penicillin, Peanuts, Pollen") },
                                    supportingText = { Text("Separate multiple allergies with commas") },
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = chronicConditionsText,
                                    onValueChange = { chronicConditionsText = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Chronic Conditions") },
                                    placeholder = { Text("e.g. Asthma, Hypertension, Diabetes") },
                                    supportingText = { Text("Separate multiple conditions with commas") },
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = medicationsText,
                                    onValueChange = { medicationsText = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Current Medications") },
                                    placeholder = { Text("e.g. Metformin 500mg, Inhaler") },
                                    supportingText = { Text("Separate multiple medications with commas") },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                    3 -> {
                        // STEP 3: Room & Dates
                        SectionCard(title = "Room Category") {
                            Text(
                                text = "Choose an accommodation category offered by ${selectedHospital?.name ?: "the hospital"}:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            if (availableRoomTypes.isEmpty()) {
                                Text(
                                    text = "No room categories available for this hospital.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    availableRoomTypes.forEach { room ->
                                        val isSelected = selectedRoomTypeName == room.name
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedRoomTypeName = room.name
                                                    step3Touched = true
                                                },
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.outlineVariant
                                            ),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                                else MaterialTheme.colorScheme.surface
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(14.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                RadioButton(
                                                    selected = isSelected,
                                                    onClick = {
                                                        selectedRoomTypeName = room.name
                                                        step3Touched = true
                                                    },
                                                    modifier = Modifier.padding(top = 2.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = room.name,
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = "₹${room.pricePerDay} / day",
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                    if (room.description.isNotBlank()) {
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
                            }

                            if (roomTypeError != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = roomTypeError,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        SectionCard(title = "Preferred Admission Date") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        step3Touched = true
                                        showDatePickerDialog = true
                                    }
                            ) {
                                OutlinedTextField(
                                    value = preferredDateMillis?.let { DateUtils.formatWithDay(it) } ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    enabled = false,
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Admission Date *") },
                                    placeholder = { Text("Select preferred admission date") },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = "Pick admission date",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    isError = preferredDateError != null,
                                    supportingText = if (preferredDateError != null) {
                                        { Text(preferredDateError, color = MaterialTheme.colorScheme.error) }
                                    } else {
                                        { Text("Select today or a future date") }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                        disabledBorderColor = if (preferredDateError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                                        disabledLabelColor = if (preferredDateError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        disabledTrailingIconColor = MaterialTheme.colorScheme.primary,
                                        disabledSupportingTextColor = if (preferredDateError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        SectionCard(title = "Stay Duration & Cost Estimation") {
                            OutlinedTextField(
                                value = expectedStayDaysText,
                                onValueChange = { input ->
                                    step3Touched = true
                                    if (input.length <= 3 && input.all { it.isDigit() }) {
                                        expectedStayDaysText = input
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Expected Stay (Days) *") },
                                placeholder = { Text("e.g. 3") },
                                isError = stayDaysError != null,
                                supportingText = if (stayDaysError != null) {
                                    { Text(stayDaysError, color = MaterialTheme.colorScheme.error) }
                                } else {
                                    { Text("Enter 1 to 365 days") }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Estimated Cost Breakdown",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Room Category",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = selectedRoomType?.name ?: "None selected",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Daily Room Rate",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = if (selectedRoomType != null) "₹${selectedRoomType.pricePerDay} / day" else "₹0 / day",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Stay Duration",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = if (isStayDaysValid) "$stayDays ${if (stayDays == 1) "day" else "days"}" else "—",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Estimated Total Cost",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (selectedRoomType != null && isStayDaysValid) "₹$estimatedCostInt" else "₹0",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                    4 -> {
                        // STEP 4: Insurance & Review
                        // 1. Insurance Section
                        SectionCard(title = "Health Insurance (Optional)") {
                            Text(
                                text = "Provide medical insurance coverage details to facilitate cashless hospitalization or reimbursement.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Self-Pay / No Insurance Toggle Card
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        noInsurance = !noInsurance
                                        step4Touched = true
                                    },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (noInsurance) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (noInsurance) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = noInsurance,
                                        onCheckedChange = {
                                            noInsurance = it
                                            step4Touched = true
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "I do not have health insurance",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (noInsurance) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Proceed as a self-paying patient. No insurance policy details required.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (!noInsurance) {
                                Spacer(modifier = Modifier.height(16.dp))

                                OutlinedTextField(
                                    value = insuranceProvider,
                                    onValueChange = {
                                        insuranceProvider = it
                                        step4Touched = true
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Insurance Provider *") },
                                    placeholder = { Text("e.g. Star Health, HDFC ERGO, Care Insurance") },
                                    isError = providerError != null,
                                    supportingText = if (providerError != null) {
                                        { Text(providerError, color = MaterialTheme.colorScheme.error) }
                                    } else null,
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = policyNumber,
                                    onValueChange = {
                                        policyNumber = it
                                        step4Touched = true
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Policy / TPA Card Number *") },
                                    placeholder = { Text("e.g. POL-98765432") },
                                    isError = policyNumberError != null,
                                    supportingText = if (policyNumberError != null) {
                                        { Text(policyNumberError, color = MaterialTheme.colorScheme.error) }
                                    } else null,
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = policyHolderName,
                                    onValueChange = {
                                        policyHolderName = it
                                        step4Touched = true
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Policyholder Name *") },
                                    placeholder = { Text("Full name as per insurance policy") },
                                    trailingIcon = {
                                        if (patientName.isNotBlank() && policyHolderName.trim() != patientName.trim()) {
                                            TextButton(
                                                onClick = {
                                                    policyHolderName = patientName.trim()
                                                    step4Touched = true
                                                }
                                            ) {
                                                Text("Same as patient", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    },
                                    isError = policyHolderError != null,
                                    supportingText = if (policyHolderError != null) {
                                        { Text(policyHolderError, color = MaterialTheme.colorScheme.error) }
                                    } else null,
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            step4Touched = true
                                            showInsuranceDatePickerDialog = true
                                        }
                                    ) {
                                    OutlinedTextField(
                                        value = insuranceValidTillMillis?.let { DateUtils.format(it) } ?: "",
                                        onValueChange = {},
                                        readOnly = true,
                                        enabled = false,
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("Policy Valid Till *") },
                                        placeholder = { Text("Select policy expiration date") },
                                        trailingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.CalendarMonth,
                                                contentDescription = "Pick validity date",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        },
                                        isError = insuranceDateError != null,
                                        supportingText = if (insuranceDateError != null) {
                                            { Text(insuranceDateError, color = MaterialTheme.colorScheme.error) }
                                        } else {
                                            { Text("Must be a currently active policy (future date)") }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                            disabledBorderColor = if (insuranceDateError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                                            disabledLabelColor = if (insuranceDateError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            disabledTrailingIconColor = MaterialTheme.colorScheme.primary,
                                            disabledSupportingTextColor = if (insuranceDateError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = sumInsuredText,
                                    onValueChange = { input ->
                                        step4Touched = true
                                        if (input.length <= 9 && input.all { it.isDigit() }) {
                                            sumInsuredText = input
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Sum Insured (INR) *") },
                                    placeholder = { Text("e.g. 500000") },
                                    isError = sumInsuredError != null,
                                    supportingText = if (sumInsuredError != null) {
                                        { Text(sumInsuredError, color = MaterialTheme.colorScheme.error) }
                                    } else {
                                        { Text("Enter total medical cover limit in INR") }
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 2. Final Admission Review Section
                        SectionCard(title = "Final Admission Review") {
                            Text(
                                text = "Please verify all details carefully before submitting your inpatient admission request:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Hospital & Reason Review Card
                            ReviewSubCard(
                                title = "Hospital & Admission Reason",
                                icon = Icons.Default.LocalHospital,
                                onEditClick = { currentStep = 1 }
                            ) {
                                ReviewDetailRow("Partner Hospital", selectedHospital?.name ?: "—")
                                ReviewDetailRow("Location", "${selectedHospital?.area ?: ""}, ${selectedHospital?.city ?: "Mumbai"}")
                                ReviewDetailRow("Reason for Admission", reason.trim())
                                if (referringDoctor.isNotBlank()) {
                                    ReviewDetailRow("Referring Doctor", referringDoctor.trim())
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Patient Information Review Card
                            ReviewSubCard(
                                title = "Patient Details",
                                icon = Icons.Default.Person,
                                onEditClick = { currentStep = 2 }
                            ) {
                                ReviewDetailRow("Full Name", trimmedPatientName)
                                ReviewDetailRow("Age & Gender", "${patientAgeInt ?: 0} yrs • $gender")
                                ReviewDetailRow("Blood Group", bloodGroup)
                                ReviewDetailRow("Phone", trimmedPhone)
                                ReviewDetailRow("Residential Address", trimmedAddress)
                                ReviewDetailRow("Emergency Contact", "$trimmedEmergName ($trimmedEmergPhone)")
                                if (allergiesText.isNotBlank()) {
                                    ReviewDetailRow("Allergies", allergiesText.trim())
                                }
                                if (chronicConditionsText.isNotBlank()) {
                                    ReviewDetailRow("Chronic Conditions", chronicConditionsText.trim())
                                }
                                if (medicationsText.isNotBlank()) {
                                    ReviewDetailRow("Current Medications", medicationsText.trim())
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Room & Dates Review Card
                            ReviewSubCard(
                                title = "Room Category & Stay",
                                icon = Icons.Default.CalendarMonth,
                                onEditClick = { currentStep = 3 }
                            ) {
                                ReviewDetailRow("Room Category", selectedRoomType?.name ?: selectedRoomTypeName.ifBlank { "—" })
                                ReviewDetailRow("Daily Rate", if (selectedRoomType != null) "₹${selectedRoomType.pricePerDay} / day" else "—")
                                ReviewDetailRow("Preferred Date", preferredDateMillis?.let { DateUtils.formatWithDay(it) } ?: "—")
                                ReviewDetailRow("Expected Stay", if (isStayDaysValid) "$stayDays ${if (stayDays == 1) "day" else "days"}" else "—")
                                ReviewDetailRow("Estimated Cost", "₹$estimatedCostInt", isHighlighted = true)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Insurance Coverage Review Card
                            ReviewSubCard(
                                title = "Insurance Coverage",
                                icon = Icons.Default.Shield,
                                onEditClick = null
                            ) {
                                if (noInsurance) {
                                    ReviewDetailRow("Status", "Self-Paying Patient")
                                    ReviewDetailRow("Coverage Details", "No health insurance provided")
                                } else if (isInsuranceValid && trimmedProvider.isNotBlank()) {
                                    ReviewDetailRow("Provider", trimmedProvider)
                                    ReviewDetailRow("Policy Number", trimmedPolicyNumber)
                                    ReviewDetailRow("Policyholder", trimmedPolicyHolder)
                                    ReviewDetailRow("Valid Till", insuranceValidTillMillis?.let { DateUtils.format(it) } ?: "—")
                                    ReviewDetailRow("Sum Insured", "₹${sumInsuredInt ?: 0}")
                                } else {
                                    ReviewDetailRow("Status", "Incomplete insurance details")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Visual four-step indicator displaying progress across the wizard stages.
 */
@Composable
private fun WizardStepIndicator(currentStep: Int) {
    val steps = listOf(
        "Hospital & Reason",
        "Patient Details",
        "Room & Dates",
        "Insurance & Review"
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
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
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

/**
 * Placeholder card rendered for each wizard step during scaffolding.
 */
@Composable
private fun StepPlaceholderCard(
    stepNumber: Int,
    title: String,
    description: String,
    icon: ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Step $stepNumber: $title",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ReviewSubCard(
    title: String,
    icon: ImageVector,
    onEditClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
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
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (onEditClick != null) {
                    TextButton(
                        onClick = onEditClick,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit $title",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Edit",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            content()
        }
    }
}

@Composable
private fun ReviewDetailRow(
    label: String,
    value: String,
    isHighlighted: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1.1f)
        )
        Text(
            text = value,
            style = if (isHighlighted) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
            color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.7f)
        )
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
