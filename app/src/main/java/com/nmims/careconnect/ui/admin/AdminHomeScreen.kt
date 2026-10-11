package com.nmims.careconnect.ui.admin

/*
 * AdminHomeScreen:
 * Hospital Management & Resource Allotment Console:
 * - Restricted strictly to the hospital assigned to AppSession.currentHospitalId
 * - Header displaying "<Hospital Name> Admin" with top bar logout action
 * - Real-time statistics: Pending Admissions, Approved Admissions, Appointments Today, Lab Tests Today
 * - Three-tab layout via TabRow:
 *   1. Admissions: Real-time list of hospital admission requests via AdmissionRepository.observeForHospital(),
 *      sorted with pending first, navigating to AdminAdmissionReviewScreen.
 *   2. Appointments: Hospital OPD bookings grouped and sorted by date with doctor, patient, slot, and status.
 *   3. Lab Tests: Diagnostic scan bookings with "Mark Completed" action for confirmed bookings via BookingRepository.updateStatus().
 * Enforces ownership checks to prevent unauthorized cross-hospital data access.
 */

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.nmims.careconnect.data.model.Admission
import com.nmims.careconnect.data.model.AdmissionStatus
import com.nmims.careconnect.data.model.Booking
import com.nmims.careconnect.data.model.BookingStatus
import com.nmims.careconnect.data.model.BookingType
import com.nmims.careconnect.data.repository.AdmissionRepositoryProvider
import com.nmims.careconnect.data.repository.BookingRepositoryProvider
import com.nmims.careconnect.data.repository.HospitalRepository
import com.nmims.careconnect.data.repository.LabTestRepository
import com.nmims.careconnect.session.AppSession
import com.nmims.careconnect.ui.common.CareTopBar
import com.nmims.careconnect.ui.common.ConfirmDialog
import com.nmims.careconnect.ui.common.DateUtils
import com.nmims.careconnect.ui.common.EmptyState
import com.nmims.careconnect.ui.common.LoadingIndicator
import com.nmims.careconnect.ui.common.StatCard
import com.nmims.careconnect.ui.common.StatusChip
import com.nmims.careconnect.ui.theme.CareConnectTheme
import kotlinx.coroutines.launch

@Composable
fun AdminHomeScreen(
    onLogout: () -> Unit,
    onAdmissionClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val admissionRepo = remember(context) { AdmissionRepositoryProvider.get(context) }
    val bookingRepo = remember(context) { BookingRepositoryProvider.get(context) }

    var showLogoutDialog by remember { mutableStateOf(false) }
    val hospitalId = AppSession.currentHospitalId ?: ""
    val hospital = remember(hospitalId) { HospitalRepository.getHospital(hospitalId) }
    val adminName = if (AppSession.currentUserName.isNotBlank()) AppSession.currentUserName else "Administrator"

    // 0: Admissions, 1: Appointments, 2: Lab Tests
    var selectedTab by remember { mutableIntStateOf(0) }

    // Admissions state observed from Firestore
    var admissions by remember { mutableStateOf<List<Admission>?>(null) }

    // Bookings state
    var bookings by remember { mutableStateOf<List<Booking>?>(null) }

    fun refreshBookings() {
        if (hospitalId.isNotBlank()) {
            coroutineScope.launch {
                bookings = bookingRepo.getForHospital(hospitalId)
            }
        } else {
            bookings = emptyList()
        }
    }

    LaunchedEffect(hospitalId) {
        if (hospitalId.isNotBlank()) {
            admissionRepo.observeForHospital(hospitalId).collect { list ->
                admissions = list
            }
        } else {
            admissions = emptyList()
        }
    }

    LaunchedEffect(hospitalId) {
        refreshBookings()
    }

    // Statistics computation
    val pendingAdmissionsCount = admissions?.count { it.status == AdmissionStatus.PENDING } ?: 0
    val approvedAdmissionsCount = admissions?.count { it.status == AdmissionStatus.APPROVED } ?: 0

    val todayMillis = DateUtils.todayStartMillis()
    val appointmentsTodayCount = bookings?.count {
        it.type == BookingType.OPD_APPOINTMENT &&
                DateUtils.normalizeToDayStart(it.dateMillis) == todayMillis &&
                it.status != BookingStatus.CANCELLED
    } ?: 0

    val labTestsTodayCount = bookings?.count {
        it.type == BookingType.LAB_TEST &&
                DateUtils.normalizeToDayStart(it.dateMillis) == todayMillis &&
                it.status != BookingStatus.CANCELLED
    } ?: 0

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
        ) {
            // Header Info
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "${hospital?.name ?: "Hospital"} Admin",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$adminName • Inpatient admissions & resource desk",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Stat Cards Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    label = "Pending",
                    value = pendingAdmissionsCount.toString(),
                    icon = Icons.Default.HourglassTop,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Approved",
                    value = approvedAdmissionsCount.toString(),
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "OPD Today",
                    value = appointmentsTodayCount.toString(),
                    icon = Icons.Default.CalendarMonth,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Lab Today",
                    value = labTestsTodayCount.toString(),
                    icon = Icons.Default.Biotech,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tab Navigation
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Admissions (${admissions?.size ?: 0})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        val opdCount = bookings?.count { it.type == BookingType.OPD_APPOINTMENT } ?: 0
                        Text("Appointments ($opdCount)")
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        val labCount = bookings?.count { it.type == BookingType.LAB_TEST } ?: 0
                        Text("Lab Tests ($labCount)")
                    }
                )
            }

            // Tab Content
            when (selectedTab) {
                0 -> {
                    // TAB 1: ADMISSIONS
                    val list = admissions
                    if (list == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            LoadingIndicator()
                        }
                    } else if (list.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            EmptyState(
                                icon = Icons.Default.Hotel,
                                message = "No hospital admission requests for this facility yet."
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(list, key = { it.id }) { admission ->
                                AdminAdmissionCard(
                                    admission = admission,
                                    onClick = { onAdmissionClick(admission.id) }
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 2: APPOINTMENTS
                    val list = bookings?.filter { it.type == BookingType.OPD_APPOINTMENT }
                    if (bookings == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            LoadingIndicator()
                        }
                    } else if (list.isNullOrEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            EmptyState(
                                icon = Icons.Default.CalendarMonth,
                                message = "No OPD doctor appointments scheduled for this hospital."
                            )
                        }
                    } else {
                        // Group appointments by date, sorting dates in Kotlin
                        val grouped = list.groupBy { it.dateMillis }
                            .toSortedMap(compareByDescending { it })

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            grouped.forEach { (dateMillis, appts) ->
                                item(key = "header_$dateMillis") {
                                    Text(
                                        text = DateUtils.formatWithDay(dateMillis),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }

                                items(appts, key = { it.id }) { booking ->
                                    AdminAppointmentCard(booking = booking)
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 3: LAB TESTS
                    val list = bookings?.filter { it.type == BookingType.LAB_TEST }
                        ?.sortedByDescending { it.dateMillis }

                    if (bookings == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            LoadingIndicator()
                        }
                    } else if (list.isNullOrEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            EmptyState(
                                icon = Icons.Default.Biotech,
                                message = "No diagnostic lab tests scheduled for this hospital."
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(list, key = { it.id }) { booking ->
                                AdminLabTestCard(
                                    booking = booking,
                                    onMarkCompleted = {
                                        coroutineScope.launch {
                                            val ok = bookingRepo.updateStatus(booking.id, BookingStatus.COMPLETED)
                                            if (ok) {
                                                Toast.makeText(context, "Lab test marked as completed", Toast.LENGTH_SHORT).show()
                                                refreshBookings()
                                            } else {
                                                Toast.makeText(context, "Could not update status", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
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

@Composable
private fun AdminAdmissionCard(
    admission: Admission,
    onClick: () -> Unit
) {
    val statusColor = when (admission.status) {
        AdmissionStatus.PENDING -> Color(0xFFFFF3CD)
        AdmissionStatus.APPROVED -> Color(0xFFD1E7DD)
        AdmissionStatus.REJECTED -> Color(0xFFF8D7DA)
        AdmissionStatus.DISCHARGED -> Color(0xFFE2E3E5)
    }

    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = admission.patientName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                StatusChip(
                    text = admission.status.label,
                    containerColor = statusColor
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${admission.patientAge} yrs, ${admission.patientGender} • Blood: ${admission.patientBloodGroup} • Phone: ${admission.patientPhone}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Reason: ${admission.reason}",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${admission.roomType} • ₹${admission.roomPricePerDay}/day",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Date: ${DateUtils.format(admission.preferredDateMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (admission.status == AdmissionStatus.APPROVED && !admission.assignedRoom.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MeetingRoom,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Assigned Room: ${admission.assignedRoom}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ref: ${admission.referenceCode()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Review Request",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminAppointmentCard(booking: Booking) {
    val doctor = booking.doctorId?.let { HospitalRepository.getDoctor(it) }
    val statusColor = when (booking.status) {
        BookingStatus.CONFIRMED -> Color(0xFFD1E7DD)
        BookingStatus.COMPLETED -> Color(0xFFE2E3E5)
        BookingStatus.CANCELLED -> Color(0xFFF8D7DA)
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dr. ${doctor?.name ?: "Doctor"}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                StatusChip(
                    text = booking.status.label,
                    containerColor = statusColor
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Patient: ${booking.patientName}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Slot: ${booking.timeSlot}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Date: ${DateUtils.format(booking.dateMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (booking.reason.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Reason: ${booking.reason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AdminLabTestCard(
    booking: Booking,
    onMarkCompleted: () -> Unit
) {
    val test = booking.labTestId?.let { LabTestRepository.getTest(it) }
    val statusColor = when (booking.status) {
        BookingStatus.CONFIRMED -> Color(0xFFD1E7DD)
        BookingStatus.COMPLETED -> Color(0xFFE2E3E5)
        BookingStatus.CANCELLED -> Color(0xFFF8D7DA)
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = test?.name ?: "Diagnostic Test",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                StatusChip(
                    text = booking.status.label,
                    containerColor = statusColor
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Patient: ${booking.patientName}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Slot: ${booking.timeSlot}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Date: ${DateUtils.format(booking.dateMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (booking.status == BookingStatus.CONFIRMED) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onMarkCompleted,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Mark Completed")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AdminHomeScreenPreview() {
    CareConnectTheme {
        AdminHomeScreen(
            onLogout = {},
            onAdmissionClick = {}
        )
    }
}
