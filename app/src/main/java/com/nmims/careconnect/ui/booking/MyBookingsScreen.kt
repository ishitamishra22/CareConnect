package com.nmims.careconnect.ui.booking

/*
 * MyBookingsScreen:
 * Displays a list of the patient's OPD and Lab Test bookings divided into Upcoming and Past tabs.
 * Allows viewing booking details and cancelling eligible upcoming bookings with a confirmation dialog.
 * Redesigned with BookingSummaryCard, PatientHeroCard, and EmptyStatePanel.
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.nmims.careconnect.data.repository.LabTestRepository
import com.nmims.careconnect.session.AppSession
import com.nmims.careconnect.ui.booking.components.BookingSummaryCard
import com.nmims.careconnect.ui.common.CareTopBar
import com.nmims.careconnect.ui.common.ConfirmDialog
import com.nmims.careconnect.ui.common.DateUtils
import com.nmims.careconnect.ui.common.LoadingIndicator
import com.nmims.careconnect.ui.common.StatusChip
import com.nmims.careconnect.ui.patient.components.EmptyStatePanel
import com.nmims.careconnect.ui.patient.components.PatientHeroCard
import com.nmims.careconnect.ui.theme.CareConnectTheme
import kotlinx.coroutines.launch

@Composable
fun MyBookingsScreen(
    onNavigateBack: () -> Unit = {},
    onBookingClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember(context) { BookingRepositoryProvider.get(context) }

    var selectedTab by remember { mutableIntStateOf(0) }
    var allBookings by remember { mutableStateOf<List<Booking>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var bookingToCancel by remember { mutableStateOf<Booking?>(null) }
    var isCancelling by remember { mutableStateOf(false) }

    val patientId = AppSession.currentPatientId

    LaunchedEffect(patientId, refreshTrigger) {
        isLoading = true
        try {
            if (patientId.isNotBlank()) {
                allBookings = repository.getForPatient(patientId)
            } else {
                allBookings = emptyList()
            }
        } catch (_: Exception) {
            allBookings = emptyList()
        } finally {
            isLoading = false
        }
    }

    val todayStart = remember { DateUtils.todayStartMillis() }

    // Upcoming: Date is today or in future AND status is CONFIRMED
    val upcomingBookings = remember(allBookings, todayStart) {
        allBookings.filter { booking ->
            booking.status == BookingStatus.CONFIRMED && booking.dateMillis >= todayStart
        }.sortedWith(compareBy({ it.dateMillis }, { it.timeSlot }))
    }

    // Past: Cancelled, Completed, or date in past
    val pastBookings = remember(allBookings, todayStart) {
        allBookings.filter { booking ->
            booking.status != BookingStatus.CONFIRMED || booking.dateMillis < todayStart
        }.sortedByDescending { it.dateMillis }
    }

    Scaffold(
        topBar = {
            CareTopBar(
                title = "My Bookings",
                onBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
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
                            text = "Upcoming (${upcomingBookings.size})",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "Past (${pastBookings.size})",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
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
                    selectedTab == 0 && upcomingBookings.isEmpty() -> {
                        EmptyStatePanel(
                            icon = Icons.Default.CalendarMonth,
                            title = "No Upcoming Bookings",
                            message = "You have no active doctor appointments or diagnostic tests scheduled. Browse specialists or book laboratory tests anytime."
                        )
                    }
                    selectedTab == 1 && pastBookings.isEmpty() -> {
                        EmptyStatePanel(
                            icon = Icons.Default.History,
                            title = "No Past Records",
                            message = "No past bookings, completed consultations, or cancellation records found."
                        )
                    }
                    else -> {
                        val displayList = if (selectedTab == 0) upcomingBookings else pastBookings
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                PatientHeroCard(
                                    title = if (selectedTab == 0) "${upcomingBookings.size} Active Healthcare Bookings" else "${pastBookings.size} Recorded Consultation Entries",
                                    description = if (selectedTab == 0) "Show your reference code at hospital check-in for expedited admission and queue priority." else "Consultation history, laboratory reports, and prescription advice from previous visits.",
                                    badgeText = if (selectedTab == 0) "Upcoming Visits" else "Booking History"
                                )
                            }

                            items(displayList, key = { it.id }) { booking ->
                                val hospital = remember(booking.hospitalId) {
                                    HospitalRepository.getHospital(booking.hospitalId)
                                }
                                val doctor = remember(booking.doctorId) {
                                    booking.doctorId?.let { HospitalRepository.getDoctor(it) }
                                }
                                val labTest = remember(booking.labTestId) {
                                    booking.labTestId?.let { LabTestRepository.getTest(it) }
                                }

                                val targetName = when (booking.type) {
                                    BookingType.OPD_APPOINTMENT -> doctor?.let { "${it.name} (${it.specialty})" } ?: "Doctor Appointment"
                                    BookingType.LAB_TEST -> labTest?.name ?: booking.reason
                                }
                                val hospitalName = hospital?.name ?: "Partner Hospital"

                                val canCancel = booking.status == BookingStatus.CONFIRMED &&
                                        booking.dateMillis >= todayStart

                                BookingSummaryCard(
                                    booking = booking,
                                    targetName = targetName,
                                    hospitalName = hospitalName,
                                    onClick = { onBookingClick(booking.id) },
                                    onCancelClick = if (canCancel) {
                                        { bookingToCancel = booking }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }
        }

        if (bookingToCancel != null) {
            ConfirmDialog(
                title = "Cancel Booking",
                message = "Are you sure you want to cancel booking Ref: ${bookingToCancel?.referenceCode()}? This cannot be undone.",
                confirmText = if (isCancelling) "Cancelling..." else "Cancel Booking",
                dismissText = "Keep Booking",
                onConfirm = {
                    val idToCancel = bookingToCancel?.id ?: return@ConfirmDialog
                    isCancelling = true
                    coroutineScope.launch {
                        try {
                            repository.cancel(idToCancel)
                            refreshTrigger++
                        } finally {
                            isCancelling = false
                            bookingToCancel = null
                        }
                    }
                },
                onDismiss = {
                    if (!isCancelling) {
                        bookingToCancel = null
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MyBookingsScreenPreview() {
    CareConnectTheme {
        MyBookingsScreen()
    }
}
