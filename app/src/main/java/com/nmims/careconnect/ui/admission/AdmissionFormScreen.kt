package com.nmims.careconnect.ui.admission

/*
 * AdmissionFormScreen:
 * Four-step wizard for automated hospital admission requests:
 * 1. Hospital & Reason: Partner hospital dropdown selection, reason for admission (min 10 chars), optional referring doctor.
 * 2. Patient Details: Demographics and emergency contacts (placeholder for Milestone 8).
 * 3. Room & Dates: Room category and stay duration (placeholder for Milestone 9).
 * 4. Insurance & Review: Insurance details and final request review (placeholder for Milestone 10).
 */

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nmims.careconnect.data.repository.HospitalRepository
import com.nmims.careconnect.ui.common.CareTopBar
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
            2 -> true // Validated in Milestone 8
            3 -> true // Validated in Milestone 9
            4 -> true // Validated in Milestone 10
            else -> false
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
                            }
                            if (currentStep < 4) {
                                if (validateCurrentStep(currentStep)) {
                                    currentStep += 1
                                }
                            } else {
                                // Final submission trigger (wired in later milestone)
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
                    2 -> StepPlaceholderCard(
                        stepNumber = 2,
                        title = "Patient Details",
                        description = "Review patient identity, demographics, emergency contact numbers, and medical conditions.",
                        icon = Icons.Default.Person
                    )
                    3 -> StepPlaceholderCard(
                        stepNumber = 3,
                        title = "Room & Dates",
                        description = "Choose room category, preferred admission date, expected stay duration, and live cost calculation.",
                        icon = Icons.Default.CalendarMonth
                    )
                    4 -> StepPlaceholderCard(
                        stepNumber = 4,
                        title = "Insurance & Review",
                        description = "Provide health insurance coverage details and complete the final admission request review.",
                        icon = Icons.Default.Shield
                    )
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
