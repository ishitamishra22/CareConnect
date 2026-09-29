package com.nmims.careconnect.ui.portfolio

/*
 * EditProfileScreen:
 * Form to create or update the patient's medical profile.
 * Fields:
 * - Full Name (required, min 3 chars)
 * - Age (number, 1-120)
 * - Gender (ExposedDropdownMenu: Male, Female, Other)
 * - Blood Group (Dropdown: A+, A-, B+, B-, AB+, AB-, O+, O-)
 * - Phone (exactly 10 digits)
 * - Email (optional, standard email regex)
 * - Residential Address (required)
 * - Emergency Contact Name & Phone (10 digits)
 * - Allergies, Chronic Conditions, Current Medications (comma-separated strings)
 * Features live validation errors; Save button is disabled until the form is completely valid.
 */

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nmims.careconnect.data.model.PatientProfile
import com.nmims.careconnect.data.repository.PortfolioRepositoryProvider
import com.nmims.careconnect.session.AppSession
import com.nmims.careconnect.ui.common.CareTopBar
import com.nmims.careconnect.ui.common.LoadingIndicator
import com.nmims.careconnect.ui.common.PrimaryButton
import com.nmims.careconnect.ui.common.SectionCard
import com.nmims.careconnect.ui.theme.CareConnectTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = PortfolioRepositoryProvider.get(context)

    var isLoading by rememberSaveable { mutableStateOf(true) }
    var isSaving by rememberSaveable { mutableStateOf(false) }

    // Form states
    var name by rememberSaveable { mutableStateOf(AppSession.currentUserName) }
    var ageText by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf("Male") }
    var bloodGroup by rememberSaveable { mutableStateOf("B+") }
    var phone by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var emergencyContactName by rememberSaveable { mutableStateOf("") }
    var emergencyContactPhone by rememberSaveable { mutableStateOf("") }
    var allergiesText by rememberSaveable { mutableStateOf("") }
    var chronicConditionsText by rememberSaveable { mutableStateOf("") }
    var medicationsText by rememberSaveable { mutableStateOf("") }

    // Dropdown expanded states
    var genderExpanded by rememberSaveable { mutableStateOf(false) }
    var bloodGroupExpanded by rememberSaveable { mutableStateOf(false) }

    val genderOptions = listOf("Male", "Female", "Other")
    val bloodGroupOptions = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")

    // Load existing profile if available
    LaunchedEffect(Unit) {
        val existing = repository.getProfile()
        if (existing != null) {
            name = existing.name
            ageText = if (existing.age > 0) existing.age.toString() else ""
            if (existing.gender.isNotBlank()) gender = existing.gender
            if (existing.bloodGroup.isNotBlank()) bloodGroup = existing.bloodGroup
            phone = existing.phone
            email = existing.email
            address = existing.address
            emergencyContactName = existing.emergencyContactName
            emergencyContactPhone = existing.emergencyContactPhone
            allergiesText = existing.allergies.joinToString(", ")
            chronicConditionsText = existing.chronicConditions.joinToString(", ")
            medicationsText = existing.currentMedications.joinToString(", ")
        } else if (name.isBlank()) {
            name = AppSession.currentUserName
        }
        isLoading = false
    }

    // Validations
    val trimmedName = name.trim()
    val isNameValid = trimmedName.length >= 3 && trimmedName.all { it.isLetter() || it.isWhitespace() }

    val ageInt = ageText.toIntOrNull()
    val isAgeValid = ageInt != null && ageInt in 1..120

    val trimmedPhone = phone.trim()
    val isPhoneValid = trimmedPhone.length == 10 && trimmedPhone.all { it.isDigit() }

    val trimmedEmail = email.trim()
    val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$")
    val isEmailValid = trimmedEmail.isEmpty() || emailRegex.matches(trimmedEmail)

    val trimmedAddress = address.trim()
    val isAddressValid = trimmedAddress.isNotBlank()

    val trimmedEmergName = emergencyContactName.trim()
    val isEmergNameValid = trimmedEmergName.isNotBlank()

    val trimmedEmergPhone = emergencyContactPhone.trim()
    val isEmergPhoneValid = trimmedEmergPhone.length == 10 && trimmedEmergPhone.all { it.isDigit() }

    val isFormValid = isNameValid && isAgeValid && isPhoneValid && isEmailValid &&
            isAddressValid && isEmergNameValid && isEmergPhoneValid

    fun parseCommaSeparated(input: String): List<String> {
        return input.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    fun handleSave() {
        if (!isFormValid || isSaving) return
        isSaving = true

        val profile = PatientProfile(
            name = trimmedName,
            age = ageInt ?: 0,
            gender = gender,
            bloodGroup = bloodGroup,
            phone = trimmedPhone,
            email = trimmedEmail,
            address = trimmedAddress,
            emergencyContactName = trimmedEmergName,
            emergencyContactPhone = trimmedEmergPhone,
            allergies = parseCommaSeparated(allergiesText),
            chronicConditions = parseCommaSeparated(chronicConditionsText),
            currentMedications = parseCommaSeparated(medicationsText)
        )

        coroutineScope.launch {
            val success = repository.saveProfile(profile)
            if (success) {
                AppSession.currentUserName = trimmedName
                Toast.makeText(context, "Profile saved successfully", Toast.LENGTH_SHORT).show()
                onNavigateBack()
            } else {
                Toast.makeText(context, "Could not reach the server. Check your internet connection.", Toast.LENGTH_SHORT).show()
                isSaving = false
            }
        }
    }

    Scaffold(
        topBar = {
            CareTopBar(
                title = "Edit Medical Profile",
                onBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            LoadingIndicator(modifier = Modifier.padding(innerPadding))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Basic Identity Section
                SectionCard(title = "Personal Information") {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Full Name *") },
                        isError = name.isNotBlank() && !isNameValid,
                        supportingText = {
                            if (name.isNotBlank() && !isNameValid) {
                                Text("Min 3 characters, letters only", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = ageText,
                            onValueChange = { if (it.length <= 3 && it.all { ch -> ch.isDigit() }) ageText = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Age *") },
                            isError = ageText.isNotBlank() && !isAgeValid,
                            supportingText = {
                                if (ageText.isNotBlank() && !isAgeValid) {
                                    Text("1-120", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // Gender Dropdown
                        ExposedDropdownMenuBox(
                            expanded = genderExpanded,
                            onExpandedChange = { genderExpanded = !genderExpanded },
                            modifier = Modifier.weight(1.2f)
                        ) {
                            OutlinedTextField(
                                value = gender,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Gender *") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded)
                                },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = genderExpanded,
                                onDismissRequest = { genderExpanded = false }
                            ) {
                                genderOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            gender = option
                                            genderExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Blood Group Dropdown
                    ExposedDropdownMenuBox(
                        expanded = bloodGroupExpanded,
                        onExpandedChange = { bloodGroupExpanded = !bloodGroupExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = bloodGroup,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Blood Group *") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = bloodGroupExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = bloodGroupExpanded,
                            onDismissRequest = { bloodGroupExpanded = false }
                        ) {
                            bloodGroupOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        bloodGroup = option
                                        bloodGroupExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Contact Details Section
                SectionCard(title = "Contact & Residential Details") {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { if (it.length <= 10 && it.all { ch -> ch.isDigit() }) phone = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Phone Number (10 digits) *") },
                        isError = phone.isNotBlank() && !isPhoneValid,
                        supportingText = {
                            if (phone.isNotBlank() && !isPhoneValid) {
                                Text("Exactly 10 digits required", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Email Address (Optional)") },
                        isError = email.isNotBlank() && !isEmailValid,
                        supportingText = {
                            if (email.isNotBlank() && !isEmailValid) {
                                Text("Enter a valid email address", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Residential Address *") },
                        isError = address.isNotBlank() && !isAddressValid,
                        supportingText = {
                            if (address.isNotBlank() && !isAddressValid) {
                                Text("Address is required", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
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
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = emergencyContactPhone,
                        onValueChange = { if (it.length <= 10 && it.all { ch -> ch.isDigit() }) emergencyContactPhone = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Emergency Contact Phone *") },
                        isError = emergencyContactPhone.isNotBlank() && !isEmergPhoneValid,
                        supportingText = {
                            if (emergencyContactPhone.isNotBlank() && !isEmergPhoneValid) {
                                Text("Exactly 10 digits required", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Clinical History Section
                SectionCard(title = "Medical Background (Comma-separated)") {
                    OutlinedTextField(
                        value = allergiesText,
                        onValueChange = { allergiesText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Allergies") },
                        placeholder = { Text("e.g. Penicillin, Peanuts, Pollen") },
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = chronicConditionsText,
                        onValueChange = { chronicConditionsText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Chronic Conditions") },
                        placeholder = { Text("e.g. Asthma, Hypertension, Diabetes") },
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = medicationsText,
                        onValueChange = { medicationsText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Current Medications") },
                        placeholder = { Text("e.g. Metformin 500mg, Inhaler") },
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                PrimaryButton(
                    text = if (isSaving) "Saving Medical Profile..." else "Save Medical Profile",
                    onClick = { handleSave() },
                    enabled = isFormValid && !isSaving
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EditProfileScreenPreview() {
    CareConnectTheme {
        EditProfileScreen(onNavigateBack = {})
    }
}
