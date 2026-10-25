package com.nmims.careconnect.ui.booking

/*
 * BookingHubScreen:
 * Central navigation hub for healthcare bookings:
 * - Option 1: Doctor Appointment booking (browse specialists or pick doctor)
 * - Option 2: Diagnostic Lab Test & Scan booking
 * - Option 3: Quick access to My Bookings
 * Matches CareConnectTheme and uses SectionCard, CareTopBar, and PatientHeroCard.
 */

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nmims.careconnect.ui.common.CareTopBar
import com.nmims.careconnect.ui.patient.components.PatientActionTile
import com.nmims.careconnect.ui.patient.components.PatientHeroCard
import com.nmims.careconnect.ui.patient.components.PatientSectionHeader
import com.nmims.careconnect.ui.theme.CareConnectTheme

@Composable
fun BookingHubScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToDoctorSearch: () -> Unit = {},
    onNavigateToLabBooking: () -> Unit = {},
    onNavigateToMyBookings: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            CareTopBar(
                title = "Book Services",
                onBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                PatientHeroCard(
                    title = "Healthcare Consultation & Diagnostics",
                    description = "Schedule in-person specialist consultations across partner hospital networks or arrange pathology and radiology tests.",
                    badgeText = "Clinical Booking Hub"
                )
            }

            item {
                PatientSectionHeader(
                    title = "Available Services",
                    subtitle = "Select consultation or diagnostic service"
                )
            }

            item {
                PatientActionTile(
                    title = "Doctor Consultation",
                    subtitle = "Book in-person appointment with verified hospital specialists",
                    icon = Icons.Default.MedicalServices,
                    onClick = onNavigateToDoctorSearch
                )
            }

            item {
                PatientActionTile(
                    title = "Lab Tests & Scans",
                    subtitle = "Schedule blood tests (CBC, Lipid, HbA1c), pathology & scans",
                    icon = Icons.Default.Biotech,
                    onClick = onNavigateToLabBooking
                )
            }

            item {
                PatientSectionHeader(
                    title = "Booking Records",
                    subtitle = "Track active tokens & review history"
                )
            }

            item {
                PatientActionTile(
                    title = "My Bookings & Receipts",
                    subtitle = "Review active tokens, schedules, doctor notes & past visits",
                    icon = Icons.Default.CheckCircleOutline,
                    onClick = onNavigateToMyBookings
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BookingHubScreenPreview() {
    CareConnectTheme {
        BookingHubScreen()
    }
}
