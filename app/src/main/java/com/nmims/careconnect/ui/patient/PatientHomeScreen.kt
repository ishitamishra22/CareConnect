package com.nmims.careconnect.ui.patient

/*
 * PatientHomeScreen:
 * Patient dashboard containing:
 * - TopBar with "CareConnect" brand title and a Logout icon (with ConfirmDialog -> AppSession.logout() -> resetTo(RoleSelection))
 * - Greeting: "Hello, <name>"
 * - LazyColumn of 6 feature cards:
 *   1. My Medical Portfolio (wired to Screen.Portfolio)
 *   2. Find Hospitals & Doctors (coming soon toast)
 *   3. Book Appointment / Lab Test (coming soon toast)
 *   4. My Bookings (coming soon toast)
 *   5. Hospital Admission (coming soon toast)
 *   6. My Admissions (coming soon toast)
 * Built with a single FeatureCard composable and FeatureItem model for easy extension by teammates.
 */

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nmims.careconnect.session.AppSession
import com.nmims.careconnect.ui.common.CareTopBar
import com.nmims.careconnect.ui.common.ConfirmDialog
import com.nmims.careconnect.ui.theme.CareConnectTheme

data class FeatureItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun PatientHomeScreen(
    onNavigateToPortfolio: () -> Unit,
    onNavigateToSearch: () -> Unit = {},
    onNavigateToBookingHub: () -> Unit = {},
    onNavigateToMyBookings: () -> Unit = {},
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var showLogoutDialog by remember { mutableStateOf(false) }

    val userName = if (AppSession.currentUserName.isNotBlank()) AppSession.currentUserName else "Patient"

    val comingSoonToast: (String) -> Unit = { moduleName ->
        Toast.makeText(context, "$moduleName module is coming soon in later phases", Toast.LENGTH_SHORT).show()
    }

    val features = listOf(
        FeatureItem(
            title = "My Medical Portfolio",
            subtitle = "Manage health profile, records, uploaded reports & view forwardable summary",
            icon = Icons.Default.FolderShared,
            onClick = onNavigateToPortfolio
        ),
        FeatureItem(
            title = "Find Hospitals & Doctors",
            subtitle = "Search specialists, compare facilities, reviews & consultation fees",
            icon = Icons.Default.Search,
            onClick = onNavigateToSearch
        ),
        FeatureItem(
            title = "Book Appointment / Lab Test",
            subtitle = "Schedule in-person or video consultations and diagnostic lab tests",
            icon = Icons.Default.CalendarMonth,
            onClick = onNavigateToBookingHub
        ),
        FeatureItem(
            title = "My Bookings",
            subtitle = "Track active tokens, appointment slots, test results & past visits",
            icon = Icons.Default.CheckCircleOutline,
            onClick = onNavigateToMyBookings
        ),
        FeatureItem(
            title = "Hospital Admission",
            subtitle = "Direct admission request, bed allotment & emergency room triage",
            icon = Icons.Default.Hotel,
            onClick = { comingSoonToast("Hospital Admission") }
        ),
        FeatureItem(
            title = "My Admissions",
            subtitle = "View current inpatient status, room allotment & discharge summaries",
            icon = Icons.Default.LocalHospital,
            onClick = { comingSoonToast("My Admissions") }
        )
    )

    Scaffold(
        topBar = {
            CareTopBar(
                title = "CareConnect",
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        text = "Hello, $userName 👋",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your healthcare hub. Book appointments, manage records & admissions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(features) { feature ->
                FeatureCard(item = feature)
            }
        }

        if (showLogoutDialog) {
            ConfirmDialog(
                title = "Logout",
                message = "Are you sure you want to log out of CareConnect?",
                confirmText = "Logout",
                onConfirm = {
                    AppSession.logout()
                    onLogout()
                },
                onDismiss = { showLogoutDialog = false }
            )
        }
    }
}

@Composable
fun FeatureCard(
    item: FeatureItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { item.onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.subtitle,
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
fun PatientHomeScreenPreview() {
    CareConnectTheme {
        PatientHomeScreen(
            onNavigateToPortfolio = {},
            onLogout = {}
        )
    }
}
