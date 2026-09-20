package com.nmims.careconnect.ui.auth

/*
 * RoleSelectionScreen:
 * Displays the CareConnect branding, logo, and lets the user select their role:
 * - Patient: Manage medical portfolio, book appointments, hospital admission.
 * - Doctor: View scheduled appointments and patient medical records.
 * - Hospital Admin: Manage admissions, emergency requests, and hospital resources.
 * Also offers a quick navigation button to the course "About" screen.
 */

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nmims.careconnect.data.model.UserRole
import com.nmims.careconnect.ui.theme.CareConnectTheme

@Composable
fun RoleSelectionScreen(
    onRoleSelected: (UserRole) -> Unit,
    onAboutClick: () -> Unit
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // App Brand Header
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(80.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalHospital,
                    contentDescription = "CareConnect Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(18.dp)
                        .fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "CareConnect",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Hospital Appointment Booking & Healthcare Document Manager",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Select your role to continue",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Patient Card
            RoleCard(
                title = "Patient",
                subtitle = "Manage your health portfolio, book appointments & admissions",
                icon = Icons.Default.Person,
                onClick = { onRoleSelected(UserRole.PATIENT) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Doctor Card
            RoleCard(
                title = "Doctor",
                subtitle = "Consultations, view patient portfolios & clinical notes",
                icon = Icons.Default.MedicalServices,
                onClick = { onRoleSelected(UserRole.DOCTOR) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Hospital Admin Card
            RoleCard(
                title = "Hospital Admin",
                subtitle = "Manage hospital admissions, beds & emergency requests",
                icon = Icons.Default.AdminPanelSettings,
                onClick = { onRoleSelected(UserRole.HOSPITAL_ADMIN) }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // About App Button
            TextButton(
                onClick = onAboutClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "About the App & Project Team",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RoleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Proceed",
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RoleSelectionScreenPreview() {
    CareConnectTheme {
        RoleSelectionScreen(
            onRoleSelected = {},
            onAboutClick = {}
        )
    }
}
