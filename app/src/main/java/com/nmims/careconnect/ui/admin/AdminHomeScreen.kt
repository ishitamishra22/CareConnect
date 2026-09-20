package com.nmims.careconnect.ui.admin

/*
 * AdminHomeScreen:
 * Placeholder dashboard for the Hospital Administrator role:
 * - TopBar with "Hospital Admin" and Logout action (ConfirmDialog -> logout -> RoleSelection)
 * - Header: Administrator name
 * - EmptyState: "Admissions and bookings will appear here"
 * Prepared cleanly for Phase 4 teammate integration.
 */

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nmims.careconnect.session.AppSession
import com.nmims.careconnect.ui.common.CareTopBar
import com.nmims.careconnect.ui.common.ConfirmDialog
import com.nmims.careconnect.ui.common.EmptyState
import com.nmims.careconnect.ui.theme.CareConnectTheme

@Composable
fun AdminHomeScreen(
    onLogout: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    val adminName = if (AppSession.currentUserName.isNotBlank()) AppSession.currentUserName else "Hospital Admin"

    Scaffold(
        topBar = {
            CareTopBar(
                title = "Hospital Admin",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = adminName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Hospital Management & Resource Allotment Console",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            EmptyState(
                icon = Icons.Default.LocalHospital,
                message = "Inpatient admissions, emergency requests, bed occupancy, and hospital bookings will appear here."
            )
        }

        if (showLogoutDialog) {
            ConfirmDialog(
                title = "Logout",
                message = "Are you sure you want to log out of the Hospital Admin portal?",
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

@Preview(showBackground = true)
@Composable
fun AdminHomeScreenPreview() {
    CareConnectTheme {
        AdminHomeScreen(onLogout = {})
    }
}
