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
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.nmims.careconnect.ui.common.LoadingIndicator
import com.nmims.careconnect.ui.common.SectionCard
import com.nmims.careconnect.ui.patient.components.EmptyStatePanel
import com.nmims.careconnect.ui.patient.components.PatientHeroCard
import com.nmims.careconnect.ui.patient.components.PatientSectionHeader
import com.nmims.careconnect.ui.portfolio.components.PatientProfileCard
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
                    EmptyStatePanel(
                        icon = Icons.Default.PersonOutline,
                        title = "No Profile Created",
                        message = "Create your comprehensive medical profile to enable fast hospital bookings and expedited admission.",
                        actionText = "Create Medical Profile",
                        onActionClick = onNavigateToEditProfile
                    )
                } else {
                    PatientProfileCard(
                        profile = currentProfile,
                        onEditClick = onNavigateToEditProfile
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                PatientSectionHeader(
                    title = "Clinical Records & Documents",
                    subtitle = "History, diagnostic reports and imaging"
                )

                // Stat Tiles: Records & Documents Count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PortfolioStatTile(
                        title = "Medical Records",
                        count = recordsCount,
                        subtitle = "Diagnoses & Rx",
                        icon = Icons.Default.HistoryEdu,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToRecords
                    )
                    PortfolioStatTile(
                        title = "Documents",
                        count = documentsCount,
                        subtitle = "Scans & Reports",
                        icon = Icons.Default.Description,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToDocuments
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Navigation Buttons to Submodules
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateToRecords,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("View Medical History")
                    }
                    OutlinedButton(
                        onClick = onNavigateToDocuments,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("View Documents")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Share Preview Section Card
                SectionCard(title = "Forwardable Medical Portfolio Summary") {
                    Text(
                        text = "This live clinical summary is forwarded automatically when you book consultations, lab tests, or hospital admissions—eliminating repeated manual paperwork.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
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
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun PortfolioStatTile(
    title: String,
    count: Int,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "$count",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
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
