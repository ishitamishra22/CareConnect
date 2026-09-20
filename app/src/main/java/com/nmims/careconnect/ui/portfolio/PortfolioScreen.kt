package com.nmims.careconnect.ui.portfolio

/*
 * PortfolioScreen:
 * Displays an overview of the patient's medical portfolio:
 * - Patient details section card with edit button
 * - Empty state if no profile exists
 * - Two quick stat tiles: Medical records count & Document count
 * - Direct navigation buttons: "Medical History" & "My Documents"
 * - Share Preview SectionCard: Displays PortfolioSummary.toShareableText()
 *   with an explanatory caption explaining how later modules forward this automatically.
 */

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nmims.careconnect.data.model.PatientProfile
import com.nmims.careconnect.data.repository.PortfolioRepositoryProvider
import com.nmims.careconnect.ui.common.CareTopBar
import com.nmims.careconnect.ui.common.EmptyState
import com.nmims.careconnect.ui.common.InfoRow
import com.nmims.careconnect.ui.common.LabelChip
import com.nmims.careconnect.ui.common.LoadingIndicator
import com.nmims.careconnect.ui.common.SectionCard
import com.nmims.careconnect.ui.theme.CareConnectTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PortfolioScreen(
    onNavigateToEditProfile: () -> Unit,
    onNavigateToRecords: () -> Unit,
    onNavigateToDocuments: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = PortfolioRepositoryProvider.get(context)

    var isLoading by remember { mutableStateOf(true) }
    var profile by remember { mutableStateOf<PatientProfile?>(null) }
    var recordsCount by remember { mutableIntStateOf(0) }
    var documentsCount by remember { mutableIntStateOf(0) }
    var shareableSummary by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        profile = repository.getProfile()
        recordsCount = repository.getRecords().size
        documentsCount = repository.getDocuments().size
        shareableSummary = repository.getSummary().toShareableText()
        isLoading = false
    }

    Scaffold(
        topBar = {
            CareTopBar(
                title = "My Medical Portfolio",
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
                Spacer(modifier = Modifier.height(12.dp))

                // Patient Profile Section
                val currentProfile = profile
                if (currentProfile == null) {
                    EmptyState(
                        icon = Icons.Default.PersonOutline,
                        message = "No profile created yet. Create your medical profile to enable fast hospital bookings and admission.",
                        action = {
                            FilledTonalButton(onClick = onNavigateToEditProfile) {
                                Text("Create Medical Profile")
                            }
                        }
                    )
                } else {
                    SectionCard(title = "Patient Information") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentProfile.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${currentProfile.age} yrs • ${currentProfile.gender} • Blood Group: ${currentProfile.bloodGroup}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = onNavigateToEditProfile) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Profile",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        InfoRow(label = "Contact Phone", value = currentProfile.phone)
                        if (currentProfile.email.isNotBlank()) {
                            InfoRow(label = "Email", value = currentProfile.email)
                        }
                        InfoRow(label = "Emergency Contact", value = "${currentProfile.emergencyContactName} (${currentProfile.emergencyContactPhone})")
                        InfoRow(label = "Address", value = currentProfile.address)

                        // Allergies Chip Row
                        if (currentProfile.allergies.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Allergies:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                currentProfile.allergies.forEach { allergy ->
                                    LabelChip(
                                        text = allergy,
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stat Tiles: Records & Documents Count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatTile(
                        title = "Medical Records",
                        count = recordsCount,
                        icon = Icons.Default.HistoryEdu,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToRecords
                    )
                    StatTile(
                        title = "Documents",
                        count = documentsCount,
                        icon = Icons.Default.Description,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToDocuments
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Navigation Buttons to Submodules
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateToRecords,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Medical History")
                    }
                    OutlinedButton(
                        onClick = onNavigateToDocuments,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("My Documents")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Share Preview Section Card
                SectionCard(title = "Forwardable Medical Portfolio Summary") {
                    Text(
                        text = "This live summary is what gets forwarded automatically when you book consultations, lab tests, or hospital admissions in later modules—eliminating repeated paperwork.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = shareableSummary,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun StatTile(
    title: String,
    count: Int,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "$count",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PortfolioScreenPreview() {
    CareConnectTheme {
        PortfolioScreen(
            onNavigateToEditProfile = {},
            onNavigateToRecords = {},
            onNavigateToDocuments = {},
            onNavigateBack = {}
        )
    }
}
