package com.nmims.careconnect.ui.doctor

/*
 * DoctorHomeScreen:
 * Doctor consultation management dashboard:
 * - Identifies doctor through AppSession.currentDoctorId
 * - Retrieves doctor's appointments via BookingRepository.getForDoctor()
 * - Excludes lab bookings from consultation dashboard
 * - Displays interactive StatCards for Today, Upcoming, and Completed appointment counts
 * - TabRow with Today, Upcoming, and Completed tabs with dynamic item badges
 * - Redesigned appointment cards with patient avatar, information hierarchy, and theme-adaptive status
 * - Contextual empty states with supportive medical iconography and guidance
 * - TopBar with instant theme mode toggling and secure logout confirmation
 * - Navigation to DoctorAppointmentDetailScreen
 */

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nmims.careconnect.data.model.Booking
import com.nmims.careconnect.data.model.BookingStatus
import com.nmims.careconnect.data.model.BookingType
import com.nmims.careconnect.data.repository.BookingRepositoryProvider
import com.nmims.careconnect.data.repository.HospitalRepository
import com.nmims.careconnect.session.AppSession
import com.nmims.careconnect.ui.common.CareTopBar
import com.nmims.careconnect.ui.common.ConfirmDialog
import com.nmims.careconnect.ui.common.DateUtils
import com.nmims.careconnect.ui.common.EmptyState
import com.nmims.careconnect.ui.common.LabelChip
import com.nmims.careconnect.ui.common.LoadingIndicator
import com.nmims.careconnect.ui.common.StatCard
import com.nmims.careconnect.ui.common.StatusChip
import com.nmims.careconnect.ui.common.bookingStatusContainerColor
import com.nmims.careconnect.ui.theme.CareConnectTheme
import com.nmims.careconnect.ui.theme.LocalSetThemeMode
import com.nmims.careconnect.ui.theme.LocalThemeMode
import com.nmims.careconnect.ui.theme.ThemeMode

@Composable
fun DoctorHomeScreen(
    onLogout: () -> Unit,
    onAppointmentClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember(context) { BookingRepositoryProvider.get(context) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val currentThemeMode = LocalThemeMode.current
    val setThemeMode = LocalSetThemeMode.current

    val doctorId = AppSession.currentDoctorId.orEmpty()
    val doctor = remember(doctorId) {
        if (doctorId.isNotBlank()) HospitalRepository.getDoctor(doctorId) else null
    }
    val doctorName = doctor?.name ?: AppSession.currentUserName.ifBlank { "Doctor" }
    val hospital = remember(doctor?.hospitalId) {
        doctor?.hospitalId?.let { HospitalRepository.getHospital(it) }
    }

    var appointments by remember { mutableStateOf<List<Booking>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val todayStart = remember { DateUtils.todayStartMillis() }
    val tomorrowStart = remember(todayStart) { todayStart + (24 * 60 * 60 * 1000L) }

    LaunchedEffect(doctorId) {
        isLoading = true
        try {
            if (doctorId.isNotBlank()) {
                val list = repository.getForDoctor(doctorId)
                // Filter only OPD doctor appointments, excluding lab test bookings
                appointments = list.filter { it.type == BookingType.OPD_APPOINTMENT }
            } else {
                appointments = emptyList()
            }
        } catch (_: Exception) {
            appointments = emptyList()
        } finally {
            isLoading = false
        }
    }

    // Tab 0: Today (date >= todayStart && date < tomorrowStart && status != CANCELLED)
    val todayAppointments = remember(appointments, todayStart, tomorrowStart) {
        appointments.filter { booking ->
            booking.dateMillis in todayStart until tomorrowStart && booking.status != BookingStatus.CANCELLED
        }.sortedBy { it.timeSlot }
    }

    // Tab 1: Upcoming (date >= tomorrowStart && status == CONFIRMED)
    val upcomingAppointments = remember(appointments, tomorrowStart) {
        appointments.filter { booking ->
            booking.dateMillis >= tomorrowStart && booking.status == BookingStatus.CONFIRMED
        }.sortedWith(compareBy({ it.dateMillis }, { it.timeSlot }))
    }

    // Tab 2: Completed (status == COMPLETED)
    val completedAppointments = remember(appointments) {
        appointments.filter { booking ->
            booking.status == BookingStatus.COMPLETED
        }.sortedByDescending { it.dateMillis }
    }

    Scaffold(
        topBar = {
            CareTopBar(
                title = "Doctor Dashboard",
                actions = {
                    IconButton(
                        onClick = {
                            val nextMode = when (currentThemeMode) {
                                ThemeMode.LIGHT -> ThemeMode.DARK
                                ThemeMode.DARK -> ThemeMode.SYSTEM
                                ThemeMode.SYSTEM -> ThemeMode.LIGHT
                            }
                            setThemeMode(nextMode)
                        }
                    ) {
                        Icon(
                            imageVector = when (currentThemeMode) {
                                ThemeMode.LIGHT -> Icons.Default.LightMode
                                ThemeMode.DARK -> Icons.Default.DarkMode
                                ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                            },
                            contentDescription = "Switch theme (${currentThemeMode.label})",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
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
        ) {
            // Enhanced Doctor Identity Header
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    val cleanDoctorName = doctorName.removePrefix("Dr. ").removePrefix("Dr ").trim()
                    val formattedDoctorName = "Dr. $cleanDoctorName"

                    Text(
                        text = formattedDoctorName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (doctor != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MedicalServices,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${doctor.specialty} • ${doctor.qualifications}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (hospital != null) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalHospital,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${hospital.name} (${hospital.area})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Stat Cards Row with interactive filter activation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    label = "Today",
                    value = todayAppointments.size.toString(),
                    icon = Icons.Default.EventAvailable,
                    isSelected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Upcoming",
                    value = upcomingAppointments.size.toString(),
                    icon = Icons.Default.Schedule,
                    isSelected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Completed",
                    value = completedAppointments.size.toString(),
                    icon = Icons.Default.CheckCircle,
                    isSelected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Modern Tab Navigation
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "Today",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "Upcoming",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Text(
                            text = "Completed",
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when {
                    isLoading -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LoadingIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Loading consultations...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    selectedTab == 0 && todayAppointments.isEmpty() -> {
                        EmptyState(
                            icon = Icons.Default.CalendarMonth,
                            message = "No consultations scheduled for today.\n\nYou're all caught up! Patient bookings for today will appear here."
                        )
                    }
                    selectedTab == 1 && upcomingAppointments.isEmpty() -> {
                        EmptyState(
                            icon = Icons.Default.Schedule,
                            message = "No upcoming future appointments.\n\nFuture confirmed appointments booked by patients will be listed here."
                        )
                    }
                    selectedTab == 2 && completedAppointments.isEmpty() -> {
                        EmptyState(
                            icon = Icons.Default.History,
                            message = "No completed consultations yet.\n\nCompleted patient visits and clinical notes will be archived here."
                        )
                    }
                    else -> {
                        val currentList = when (selectedTab) {
                            0 -> todayAppointments
                            1 -> upcomingAppointments
                            else -> completedAppointments
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(currentList, key = { it.id }) { booking ->
                                DoctorAppointmentCard(
                                    booking = booking,
                                    onClick = { onAppointmentClick(booking.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showLogoutDialog) {
            ConfirmDialog(
                title = "Logout",
                message = "Are you sure you want to log out of the Doctor Dashboard?",
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

/**
 * Modern Doctor Appointment Card designed with clear visual hierarchy:
 * - Reference token and theme-adaptive StatusChip
 * - Patient avatar with full patient name
 * - Patient reason for visit in highlighted callout container
 * - Appointment date, time slot, clinical notes badge, and action chevron
 */
@Composable
private fun DoctorAppointmentCard(
    booking: Booking,
    onClick: () -> Unit
) {
    val statusContainer = bookingStatusContainerColor(booking.status)

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Reference Token & Theme-Adaptive StatusChip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LabelChip(
                    text = "TOKEN: ${booking.referenceCode()}",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                StatusChip(
                    text = booking.status.label,
                    containerColor = statusContainer
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Patient Identity Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = booking.patientName.ifBlank { "Unspecified Patient" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Outpatient Consultation",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "View Consultation Details",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Reason for Visit callout container
            if (booking.reason.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = booking.reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Schedule & Notes Saved footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${DateUtils.formatWithDay(booking.dateMillis)} • ${booking.timeSlot}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (booking.doctorNote.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "Notes recorded",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DoctorHomeScreenPreview() {
    CareConnectTheme {
        DoctorHomeScreen(onLogout = {})
    }
}
