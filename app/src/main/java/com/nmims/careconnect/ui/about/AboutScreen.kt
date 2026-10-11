package com.nmims.careconnect.ui.about

/*
 * AboutScreen:
 * Information screen about the CareConnect project:
 * - App name + version 1.0
 * - One-paragraph mission description
 * - Academic course line: Mobile Application Development (702AI0E002) - SVKM's NMIMS, MBA-Tech CE Sem V, Batch D2
 * - Structured team members table: Person 1, Person 2, Person 3, Person 4
 */

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nmims.careconnect.ui.common.CareTopBar
import com.nmims.careconnect.ui.common.SectionCard
import com.nmims.careconnect.ui.theme.CareConnectTheme

data class TeamMember(
    val name: String,
    val role: String,
    val module: String
)

@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    val teamMembers = listOf(
        TeamMember("Person 1", "Project Lead & Developer", "Module 1: Medical Portfolio Management"),
        TeamMember("Person 2", "Developer", "Module 2: Hospital & Doctor Search"),
        TeamMember("Person 3", "Developer", "Module 3: Appointment & Lab Test Booking"),
        TeamMember("Person 4", "Developer", "Module 4: Automated Hospital Admission & Admin")
    )

    Scaffold(
        topBar = {
            CareTopBar(
                title = "About CareConnect",
                onBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // App Brand Banner
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalHospital,
                    contentDescription = "CareConnect Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "CareConnect",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Version 1.0 • Phase 1 Release",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Project Mission
            SectionCard(title = "About the Application") {
                Text(
                    text = "CareConnect is an all-in-one healthcare platform designed to bring every part of a patient's medical journey—comprehensive health records, document storage, hospital and doctor discovery, appointment and lab test scheduling, and direct hospital admissions—into a single unified ecosystem. Built with intelligent health portfolio sharing, CareConnect ensures patient medical summaries are automatically forwarded to appointments and admissions so repetitive paperwork is eliminated entirely.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // University & Course Information
            SectionCard(title = "Academic Details") {
                Text(
                    text = "Course: Mobile Application Development (702AI0E002)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Institution: SVKM's NMIMS, Mukesh Patel School of Technology Management & Engineering (MPSTME)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Program: MBA-Tech Computer Engineering, Semester V, Batch D2",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Modules Section
            SectionCard(title = "Modules") {
                val modules = listOf(
                    "1. Medical Portfolio Management — Person 1",
                    "2. Hospital & Doctor Search and Comparison — Person 2",
                    "3. OPD Appointment & Test Booking — Person 3",
                    "4. Automated Hospital Admission & Admin Dashboard — Person 4"
                )
                modules.forEachIndexed { index, moduleText ->
                    Text(
                        text = moduleText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    if (index < modules.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 2.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Team Table
            SectionCard(title = "Project Team") {
                teamMembers.forEachIndexed { index, member ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = member.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = member.role,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = member.module,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (index < teamMembers.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AboutScreenPreview() {
    CareConnectTheme {
        AboutScreen(onNavigateBack = {})
    }
}
