package com.nmims.careconnect.ui.search

/*
 * CompareScreen:
 * Side-by-side comparative analysis of 2 to 3 selected specialist doctors:
 * - CareTopBar with "Compare Doctors" title and back navigation
 * - Sticky first column showing row metrics and attributes
 * - Horizontally scrollable doctor comparison columns
 * - Metrics compared:
 *   1. Doctor (InitialsAvatar & Name)
 *   2. Hospital (Hospital name and locality)
 *   3. Specialty
 *   4. Qualifications
 *   5. Experience (Years) — highlights highest with "Best" badge
 *   6. Consultation fee (INR) — highlights lowest with "Best" badge
 *   7. Rating — highlights highest with "Best" badge
 *   8. Reviews count
 *   9. Available days
 *   10. Languages
 * - Action row underneath each column with a "Book" button (placeholder Toast for Phase 3)
 */

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nmims.careconnect.data.model.Doctor
import com.nmims.careconnect.data.model.Hospital
import com.nmims.careconnect.data.repository.HospitalRepository
import com.nmims.careconnect.ui.common.CareTopBar
import com.nmims.careconnect.ui.common.EmptyState
import com.nmims.careconnect.ui.common.InitialsAvatar
import com.nmims.careconnect.ui.common.LabelChip
import com.nmims.careconnect.ui.common.RatingBar
import com.nmims.careconnect.ui.theme.CareConnectTheme

@Composable
fun CompareScreen(
    doctorIds: List<String>,
    onNavigateBack: () -> Unit = {},
    onBookClick: (String) -> Unit = {}
) {
    val context = LocalContext.current

    val doctors = remember(doctorIds) {
        doctorIds.mapNotNull { HospitalRepository.getDoctor(it) }
    }

    val hospitals = remember(doctors) {
        doctors.associate { it.id to HospitalRepository.getHospital(it.hospitalId) }
    }

    if (doctors.size < 2) {
        Scaffold(
            topBar = { CareTopBar(title = "Compare Doctors", onBack = onNavigateBack) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.CompareArrows,
                    message = "Please select at least 2 doctors to view comparison."
                )
            }
        }
        return
    }

    // Determine best numeric metrics for highlighting
    val minFee = doctors.minOfOrNull { it.consultationFee }
    val maxRating = doctors.maxOfOrNull { it.rating }
    val maxExperience = doctors.maxOfOrNull { it.experienceYears }

    val labelColumnWidth = 125.dp
    val doctorColumnWidth = 175.dp

    Scaffold(
        topBar = {
            CareTopBar(
                title = "Compare Doctors (${doctors.size})",
                onBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                ),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                ) {
                    // Sticky Left Column (Labels)
                    Column(
                        modifier = Modifier
                            .width(labelColumnWidth)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        CompareLabelCell(text = "Doctor", minHeight = 110.dp, isHeader = true)
                        CompareDivider()
                        CompareLabelCell(text = "Hospital", minHeight = 70.dp)
                        CompareDivider()
                        CompareLabelCell(text = "Specialty", minHeight = 50.dp)
                        CompareDivider()
                        CompareLabelCell(text = "Qualifications", minHeight = 65.dp)
                        CompareDivider()
                        CompareLabelCell(text = "Experience", minHeight = 55.dp)
                        CompareDivider()
                        CompareLabelCell(text = "Consultation Fee", minHeight = 55.dp)
                        CompareDivider()
                        CompareLabelCell(text = "Rating", minHeight = 55.dp)
                        CompareDivider()
                        CompareLabelCell(text = "Reviews", minHeight = 45.dp)
                        CompareDivider()
                        CompareLabelCell(text = "Available Days", minHeight = 55.dp)
                        CompareDivider()
                        CompareLabelCell(text = "Languages", minHeight = 55.dp)
                        CompareDivider()
                        CompareLabelCell(text = "Action", minHeight = 65.dp)
                    }

                    // Horizontally Scrollable Doctor Columns
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState())
                    ) {
                        doctors.forEachIndexed { index, doctor ->
                            val hospital = hospitals[doctor.id]

                            Column(
                                modifier = Modifier
                                    .width(doctorColumnWidth)
                                    .fillMaxHeight()
                            ) {
                                // 1. Doctor Header Cell
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        InitialsAvatar(
                                            name = doctor.name,
                                            colorHex = hospital?.accentColorHex ?: "#1F7A8C",
                                            size = 46.dp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = doctor.name,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2
                                        )
                                    }
                                }
                                CompareDivider()

                                // 2. Hospital Cell
                                CompareValueCell(
                                    text = "${hospital?.name.orEmpty()}\n(${hospital?.area.orEmpty()})",
                                    minHeight = 70.dp,
                                    bold = false
                                )
                                CompareDivider()

                                // 3. Specialty Cell
                                CompareValueCell(
                                    text = doctor.specialty,
                                    minHeight = 50.dp,
                                    bold = true,
                                    textColor = MaterialTheme.colorScheme.primary
                                )
                                CompareDivider()

                                // 4. Qualifications Cell
                                CompareValueCell(
                                    text = doctor.qualifications,
                                    minHeight = 65.dp,
                                    bold = false
                                )
                                CompareDivider()

                                // 5. Experience Cell (Best Highlight)
                                val isBestExperience = doctor.experienceYears == maxExperience
                                CompareValueCell(
                                    text = "${doctor.experienceYears} Years",
                                    minHeight = 55.dp,
                                    isBest = isBestExperience,
                                    bold = true
                                )
                                CompareDivider()

                                // 6. Consultation Fee Cell (Best Highlight)
                                val isBestFee = doctor.consultationFee == minFee
                                CompareValueCell(
                                    text = "₹${doctor.consultationFee}",
                                    minHeight = 55.dp,
                                    isBest = isBestFee,
                                    bold = true
                                )
                                CompareDivider()

                                // 7. Rating Cell (Best Highlight)
                                val isBestRating = doctor.rating == maxRating
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(55.dp)
                                        .background(
                                            if (isBestRating) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                            else Color.Transparent
                                        )
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        RatingBar(rating = doctor.rating, size = 13.dp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${doctor.rating}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isBestRating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isBestRating) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            LabelChip(
                                                text = "Best",
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }
                                }
                                CompareDivider()

                                // 8. Reviews Cell
                                CompareValueCell(
                                    text = "${doctor.reviewCount} reviews",
                                    minHeight = 45.dp,
                                    bold = false
                                )
                                CompareDivider()

                                // 9. Available Days Cell
                                CompareValueCell(
                                    text = doctor.availableDays.joinToString(", "),
                                    minHeight = 55.dp,
                                    bold = false
                                )
                                CompareDivider()

                                // 10. Languages Cell
                                CompareValueCell(
                                    text = doctor.languages.joinToString(", "),
                                    minHeight = 55.dp,
                                    bold = false
                                )
                                CompareDivider()

                                // 11. Book Action Button
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(65.dp)
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Button(
                                        onClick = { onBookClick(doctor.id) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Text(
                                            text = "Book",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            // Vertical divider between doctor columns
                            if (index < doctors.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompareLabelCell(
    text: String,
    minHeight: androidx.compose.ui.unit.Dp,
    isHeader: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(minHeight)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            style = if (isHeader) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CompareValueCell(
    text: String,
    minHeight: androidx.compose.ui.unit.Dp,
    bold: Boolean = false,
    textColor: Color = Color.Unspecified,
    isBest: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(minHeight)
            .background(
                if (isBest) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                else Color.Transparent
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (bold || isBest) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                color = if (isBest) MaterialTheme.colorScheme.primary
                else if (textColor != Color.Unspecified) textColor
                else MaterialTheme.colorScheme.onSurface
            )

            if (isBest) {
                Spacer(modifier = Modifier.width(6.dp))
                LabelChip(
                    text = "Best",
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun CompareDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
}

@Preview(showBackground = true)
@Composable
fun CompareScreenPreview() {
    CareConnectTheme {
        CompareScreen(doctorIds = listOf("doc_andheri_1", "doc_andheri_2", "doc_bandra_1"))
    }
}
