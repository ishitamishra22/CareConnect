package com.nmims.careconnect.ui.booking

/*
 * BookingHubScreen:
 * Central navigation hub for healthcare bookings:
 * - Option 1: Doctor Appointment booking (browse specialists or pick doctor)
 * - Option 2: Diagnostic Lab Test & Scan booking
 * - Option 3: Quick access to My Bookings
 * Matches CareConnectTheme and uses SectionCard and CareTopBar.
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Schedule Healthcare",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Book OPD doctor consultations with hospital specialists or schedule laboratory diagnostics & scans.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                BookingHubCard(
                    title = "Doctor Consultation",
                    subtitle = "Book an in-person appointment with verified specialists across partner hospitals",
                    icon = Icons.Default.MedicalServices,
                    onClick = onNavigateToDoctorSearch
                )
            }

            item {
                BookingHubCard(
                    title = "Lab Tests & Scans",
                    subtitle = "Book diagnostic pathology tests (CBC, Lipid, HbA1c, Thyroid) and radiological scans",
                    icon = Icons.Default.Biotech,
                    onClick = onNavigateToLabBooking
                )
            }

            item {
                BookingHubCard(
                    title = "My Bookings",
                    subtitle = "Review active tokens, upcoming schedules, consult notes and past visit records",
                    icon = Icons.Default.CheckCircleOutline,
                    onClick = onNavigateToMyBookings
                )
            }
        }
    }
}

@Composable
private fun BookingHubCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
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
