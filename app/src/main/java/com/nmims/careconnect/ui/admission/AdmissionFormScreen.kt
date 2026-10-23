package com.nmims.careconnect.ui.admission

/*
 * AdmissionFormScreen:
 * Four-step wizard scaffold for automated hospital admission requests:
 * 1. Hospital & Reason
 * 2. Patient Details
 * 3. Room & Dates
 * 4. Insurance & Review
 *
 * Implements a single-screen wizard with a responsive step indicator,
 * Back/Next navigation controls, rememberSaveable state retention, and
 * step validation gates ready for detailed field implementation in subsequent milestones.
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nmims.careconnect.ui.common.CareTopBar
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
            1 -> true // Validated in Milestone 7
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
                    1 -> StepPlaceholderCard(
                        stepNumber = 1,
                        title = "Hospital & Reason",
                        description = "Select partner hospital, describe admission reason, and specify optional referring doctor.",
                        icon = Icons.Default.LocalHospital
                    )
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
