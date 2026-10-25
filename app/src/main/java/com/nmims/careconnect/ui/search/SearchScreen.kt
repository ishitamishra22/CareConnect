package com.nmims.careconnect.ui.search

/*
 * SearchScreen:
 * Primary discovery hub for finding and comparing healthcare providers:
 * - CareTopBar "Find Care" with back navigation
 * - TabRow switching between "Doctors" and "Hospitals"
 * - Live query OutlinedTextField with search & clear icons
 * - Horizontal FilterChips row for medical specialties with "All" preselected
 * - ExposedDropdownMenuBox for SortOption selection
 * - Result count banner and responsive LazyColumn
 * - Multi-doctor selection (2-3) with floating Compare bottom bar
 * - 4th selection blocked via Toast
 */

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nmims.careconnect.data.model.Doctor
import com.nmims.careconnect.data.model.Hospital
import com.nmims.careconnect.data.model.SortOption
import com.nmims.careconnect.data.model.Specialties
import com.nmims.careconnect.data.repository.HospitalRepository
import com.nmims.careconnect.ui.common.CareTopBar
import com.nmims.careconnect.ui.common.InitialsAvatar
import com.nmims.careconnect.ui.common.LabelChip
import com.nmims.careconnect.ui.common.PrimaryButton
import com.nmims.careconnect.ui.common.RatingBar
import com.nmims.careconnect.ui.patient.components.EmptyStatePanel
import com.nmims.careconnect.ui.search.components.DoctorListingCard
import com.nmims.careconnect.ui.search.components.HospitalListingCard
import com.nmims.careconnect.ui.theme.CareConnectTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    initialTab: Int = 0,
    onNavigateBack: () -> Unit = {},
    onDoctorClick: (String) -> Unit = {},
    onHospitalClick: (String) -> Unit = {},
    onCompareClick: (List<String>) -> Unit = {}
) {
    val context = LocalContext.current

    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(0, 1)) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedSpecialty by rememberSaveable { mutableStateOf("All") }
    var selectedSort by rememberSaveable { mutableStateOf(SortOption.RATING_DESC) }
    var sortDropdownExpanded by remember { mutableStateOf(false) }

    // List of selected doctor IDs for side-by-side comparison (max 3)
    val selectedDoctorIds = remember { mutableStateListOf<String>() }

    // Query filtered datasets
    val doctorResults = remember(searchQuery, selectedSpecialty, selectedSort) {
        HospitalRepository.searchDoctors(searchQuery, selectedSpecialty, selectedSort)
    }

    val hospitalResults = remember(searchQuery, selectedSpecialty, selectedSort) {
        HospitalRepository.searchHospitals(searchQuery, selectedSpecialty, selectedSort)
    }

    Scaffold(
        topBar = {
            CareTopBar(
                title = "Find Care",
                onBack = onNavigateBack
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = selectedDoctorIds.size in 2..3,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "${selectedDoctorIds.size} Doctors Selected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Compare fees, ratings & availability",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        PrimaryButton(
                            text = "Compare (${selectedDoctorIds.size})",
                            onClick = {
                                onCompareClick(selectedDoctorIds.toList())
                            },
                            modifier = Modifier.width(160.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tabs: Doctors | Hospitals
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "Doctors",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "Hospitals",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                placeholder = {
                    Text(
                        if (selectedTab == 0) "Search doctor, specialty, area or hospital..."
                        else "Search hospital name, specialty or area..."
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            )

            // Specialty Filter Chips Row
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val chipList = listOf("All") + Specialties.ALL
                items(chipList) { specialty ->
                    val isSelected = selectedSpecialty.equals(specialty, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedSpecialty = if (isSelected && specialty != "All") "All" else specialty
                        },
                        label = { Text(specialty) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sort Control & Result Count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val resultCountText = if (selectedTab == 0) {
                    "${doctorResults.size} doctors found"
                } else {
                    "${hospitalResults.size} hospitals found"
                }

                Text(
                    text = resultCountText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Exposed Dropdown Menu for Sort
                ExposedDropdownMenuBox(
                    expanded = sortDropdownExpanded,
                    onExpandedChange = { sortDropdownExpanded = !sortDropdownExpanded }
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .menuAnchor()
                            .clickable { sortDropdownExpanded = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Sort",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedSort.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = sortDropdownExpanded)
                        }
                    }

                    ExposedDropdownMenu(
                        expanded = sortDropdownExpanded,
                        onDismissRequest = { sortDropdownExpanded = false }
                    ) {
                        SortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    selectedSort = option
                                    sortDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Results List
            if (selectedTab == 0) {
                if (doctorResults.isEmpty()) {
                    EmptyStatePanel(
                        icon = Icons.Default.SearchOff,
                        title = "No Doctors Found",
                        message = "No doctors matched your search criteria. Try modifying your search term or selecting 'All' specialties."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(doctorResults, key = { it.id }) { doctor ->
                            val hospital = HospitalRepository.getHospital(doctor.hospitalId)
                            val isChecked = selectedDoctorIds.contains(doctor.id)

                            DoctorListingCard(
                                doctor = doctor,
                                hospitalName = hospital?.let { "${it.name} • ${it.area}" },
                                accentColorHex = hospital?.accentColorHex,
                                isSelectedForCompare = isChecked,
                                onCompareToggle = {
                                    if (isChecked) {
                                        selectedDoctorIds.remove(doctor.id)
                                    } else {
                                        if (selectedDoctorIds.size < 3) {
                                            selectedDoctorIds.add(doctor.id)
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "You can compare up to 3 doctors",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                },
                                onClick = { onDoctorClick(doctor.id) }
                            )
                        }
                    }
                }
            } else {
                if (hospitalResults.isEmpty()) {
                    EmptyStatePanel(
                        icon = Icons.Default.LocalHospital,
                        title = "No Hospitals Found",
                        message = "No partner hospitals matched your criteria. Try searching by city, specialty, or hospital facility."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(hospitalResults, key = { it.id }) { hospital ->
                            HospitalListingCard(
                                hospital = hospital,
                                onClick = { onHospitalClick(hospital.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Backward compatibility aliases for DoctorCard & HospitalCard
 */
@Composable
fun DoctorCard(
    doctor: Doctor,
    hospital: Hospital?,
    modifier: Modifier = Modifier,
    isChecked: Boolean = false,
    showCompareCheckbox: Boolean = true,
    onCardClick: () -> Unit,
    onCompareToggle: (() -> Unit)? = null
) {
    DoctorListingCard(
        doctor = doctor,
        hospitalName = hospital?.let { "${it.name} • ${it.area}" },
        accentColorHex = hospital?.accentColorHex,
        isSelectedForCompare = isChecked,
        onCompareToggle = { onCompareToggle?.invoke() },
        onClick = onCardClick,
        modifier = modifier
    )
}

@Composable
fun HospitalCard(
    hospital: Hospital,
    modifier: Modifier = Modifier,
    onCardClick: () -> Unit
) {
    HospitalListingCard(
        hospital = hospital,
        onClick = onCardClick,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun SearchScreenPreview() {
    CareConnectTheme {
        SearchScreen()
    }
}
