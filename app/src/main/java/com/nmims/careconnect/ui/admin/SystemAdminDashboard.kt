package com.nmims.careconnect.ui.admin

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nmims.careconnect.data.firebase.CatalogueSeeder
import com.nmims.careconnect.data.repository.AdminRepository
import com.nmims.careconnect.data.repository.AuthRepository
import com.nmims.careconnect.data.repository.HospitalRepository
import com.nmims.careconnect.data.repository.SystemCounts
import com.nmims.careconnect.ui.common.PrimaryButton
import com.nmims.careconnect.ui.theme.LocalElevatedSurface
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * System Administrator Dashboard.
 * Displays counts for registered users and active catalogue entries (Hospitals, Doctors).
 * Never reads or displays patient medical records, documents, bookings, or admissions.
 * Enforces a 15-minute inactivity session timeout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemAdminDashboard(
    onNavigateToUserManagement: () -> Unit,
    onNavigateToProvisionStaff: () -> Unit,
    onSignOut: () -> Unit,
    adminRepository: AdminRepository = AdminRepository.getInstance(),
    authRepository: AuthRepository = AuthRepository.getInstance()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var systemCounts by remember { mutableStateOf<SystemCounts?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshingCatalogue by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Inactivity timeout: 15 minutes (900,000 ms)
    val inactivityTimeoutMs = 15 * 60 * 1000L
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        // Fetch counts
        val countsRes = adminRepository.getSystemCounts()
        systemCounts = countsRes.getOrNull()
        isLoading = false

        // Inactivity watchdog loop
        while (true) {
            delay(30_000L) // Check every 30 seconds
            val now = System.currentTimeMillis()
            if (now - lastInteractionTime > inactivityTimeoutMs) {
                authRepository.signOut()
                Toast.makeText(context, "Session expired due to 15 minutes of inactivity.", Toast.LENGTH_LONG).show()
                onSignOut()
                break
            }
        }
    }

    fun recordInteraction() {
        lastInteractionTime = System.currentTimeMillis()
    }

    fun refreshCatalogueData() {
        recordInteraction()
        isRefreshingCatalogue = true
        coroutineScope.launch {
            CatalogueSeeder.seedIfEmpty()
            HospitalRepository.load()
            val countsRes = adminRepository.getSystemCounts()
            systemCounts = countsRes.getOrNull()
            isRefreshingCatalogue = false
            Toast.makeText(context, "Catalogue verified & refreshed.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "System Administration",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "CareConnect Central Portal",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign Out",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Security Banner
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Privileged Access Enforced",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "15-minute inactivity timeout active. Medical records are restricted from Admin access.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // System Metrics Section
            Text(
                text = "System Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Total Users",
                        value = "${systemCounts?.totalUsers ?: 0}",
                        icon = Icons.Default.Group,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Hospitals",
                        value = "${systemCounts?.totalHospitals ?: 0}",
                        icon = Icons.Default.LocalHospital,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Doctors",
                        value = "${systemCounts?.totalDoctors ?: 0}",
                        icon = Icons.Default.MedicalServices,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Quick Links
            Text(
                text = "Administrative Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 1. Manage Users
            AdminActionCard(
                title = "Manage Users",
                subtitle = "Inspect accounts, activate/deactivate, link catalogues, reset passwords",
                icon = Icons.Default.Group,
                onClick = {
                    recordInteraction()
                    onNavigateToUserManagement()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Provision Staff
            AdminActionCard(
                title = "Create Doctor / Hospital Account",
                subtitle = "Securely provision verified clinical & hospital administrator credentials",
                icon = Icons.Default.PersonAdd,
                onClick = {
                    recordInteraction()
                    onNavigateToProvisionStaff()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Refresh Catalogue
            AdminActionCard(
                title = "Seed / Refresh Catalogue",
                subtitle = if (isRefreshingCatalogue) "Refreshing catalogue..." else "Sync hospital and doctor directory data in Firestore",
                icon = Icons.Default.Refresh,
                onClick = { refreshCatalogueData() },
                enabled = !isRefreshingCatalogue
            )

            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sign Out of System Admin?") },
            text = { Text("Are you sure you want to exit the system administration portal?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        authRepository.signOut()
                        onSignOut()
                    }
                ) {
                    Text("Sign Out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AdminActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    OutlinedCard(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = LocalElevatedSurface.current,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
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
        }
    }
}
