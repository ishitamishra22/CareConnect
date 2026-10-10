package com.nmims.careconnect.ui.doctor

/*
 * DoctorHomeScreen:
 * Doctor consultation management dashboard:
 * - Identifies doctor through AppSession.currentDoctorId
 * - Retrieves doctor's appointments via BookingRepository.getForDoctor()
 * - Excludes lab bookings from consultation dashboard
 * - Displays StatCards for Today, Upcoming, and Completed appointment counts
 * - TabRow with Today, Upcoming, and Completed tabs
 * - Appointment cards displaying patient name, date, time slot, reference code, status
 * - Navigation to DoctorAppointmentDetailScreen
 * - Logout confirmation clearing AppSession
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.nmims.careconnect.ui.common.LoadingIndicator
import com.nmims.careconnect.ui.common.StatCard
import com.nmims.careconnect.ui.common.StatusChip
import com.nmims.careconnect.ui.theme.CareConnectTheme

@Composable
fun DoctorHomeScreen(
    onLogout: () -> Unit,
    onAppointmentClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember(context) { BookingRepositoryProvider.get(context) }
    var showLogoutDialog by remember { mutableStateOf(false) }

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
            // Header with doctor and hospital info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Dr. $doctorName",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (doctor != null) {
                    Text(
                        text = "${doctor.specialty} • ${doctor.qualifications}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (hospital != null) {
                    Text(
                        text = "${hospital.name} (${hospital.area})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Stat Cards Row
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
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Upcoming",
                    value = upcomingAppointments.size.toString(),
                    icon = Icons.Default.Schedule,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Completed",
                    value = completedAppointments.size.toString(),
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tab Navigation
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
                            text = "Today (${todayAppointments.size})",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "Upcoming (${upcomingAppointments.size})",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Text(
                            text = "Completed (${completedAppointments.size})",
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
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
                        LoadingIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    selectedTab == 0 && todayAppointments.isEmpty() -> {
                        EmptyState(
                            icon = Icons.Default.CalendarMonth,
                            message = "No consultations scheduled for today."
                        )
                    }
                    selectedTab == 1 && upcomingAppointments.isEmpty() -> {
                        EmptyState(
                            icon = Icons.Default.Schedule,
                            message = "No upcoming future appointments scheduled."
                        )
                    }
                    selectedTab == 2 && completedAppointments.isEmpty() -> {
                        EmptyState(
                            icon = Icons.Default.History,
                            message = "No completed consultations recorded yet."
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

@Composable
private fun DoctorAppointmentCard(
    booking: Booking,
    onClick: () -> Unit
) {
    val statusColor = when (booking.status) {
        BookingStatus.CONFIRMED -> Color(0xFFE8F5E9)
        BookingStatus.COMPLETED -> Color(0xFFE3F2FD)
        BookingStatus.CANCELLED -> Color(0xFFFFEBEE)
    }

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
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TOKEN: ${booking.referenceCode()}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                StatusChip(
                    text = booking.status.label,
                    containerColor = statusColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = booking.patientName.ifBlank { "Patient" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (booking.reason.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Reason: ${booking.reason}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${DateUtils.formatWithDay(booking.dateMillis)} • ${booking.timeSlot}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (booking.doctorNote.isNotBlank()) {
                    Text(
                        text = "Notes saved ✓",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
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
