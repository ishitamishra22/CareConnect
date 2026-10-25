package com.nmims.careconnect.ui.admin

/*
 * AdminHomeScreen:
 * Hospital Management & Resource Allotment Console:
 * - Restricted strictly to authenticated UserRole.HOSPITAL_ADMIN sessions with nonblank AppSession.currentHospitalId
 * - Enforces Unauthenticated and Unauthorized security gates before starting or exposing any data streams
 * - Real-time statistics: Pending Admissions, Approved Admissions, Appointments Today, Lab Tests Today
 * - Quick-filter activation by tapping Pending or Approved statistic cards
 * - Three-tab layout via TabRow:
 *   1. Admissions: Real-time list of hospital admission requests via AdmissionRepository.observeForHospital(),
 *      supporting status filter chips (All, Pending, Approved, Rejected, Discharged) with dynamic item counts.
 *   2. Appointments: Hospital OPD bookings grouped and sorted by date with doctor, patient, slot, and status.
 *   3. Lab Tests: Diagnostic scan bookings with guarded "Mark Completed" action protected by confirmation dialog
 *      and duplicate-submission lock.
 * - Enriched admission request cards displaying demographics, room type, stay duration, estimated cost,
 *   insurance coverage / self-pay designation, assigned room banner, and administrative rejection remarks.
 * - Independent, recoverable loading and error states with functional retry mechanisms.
 * - Stateless AdminHomeContent with isolated Compose Previews using in-memory mock data.
 */

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nmims.careconnect.data.model.Admission
import com.nmims.careconnect.data.model.AdmissionStatus
import com.nmims.careconnect.data.model.Booking
import com.nmims.careconnect.data.model.BookingStatus
import com.nmims.careconnect.data.model.BookingType
import com.nmims.careconnect.data.model.Doctor
import com.nmims.careconnect.data.model.Hospital
import com.nmims.careconnect.data.model.InsuranceDetails
import com.nmims.careconnect.data.model.LabTest
import com.nmims.careconnect.data.model.RoomType
import com.nmims.careconnect.data.model.UserRole
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Filter categories for inpatient hospital admissions.
 */
enum class AdmissionFilter(val label: String) {
    ALL("All"),
    PENDING("Pending"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    DISCHARGED("Discharged")
}

/**
 * Authorization state for the hospital administrator portal.
 */
sealed interface AdminHomeSecurityState {
    data object Authorized : AdminHomeSecurityState
    data object Unauthenticated : AdminHomeSecurityState
    data object Unauthorized : AdminHomeSecurityState
}

@Composable
fun AdminHomeScreen(
    onLogout: () -> Unit,
    onAdmissionClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val admissionRepo = remember(context) { AdmissionRepositoryProvider.get(context) }
    val bookingRepo = remember(context) { BookingRepositoryProvider.get(context) }

    val currentRole = AppSession.currentRole
    val currentHospitalId = AppSession.currentHospitalId
    val currentUserName = AppSession.currentUserName

    // Strict authorization: must be HOSPITAL_ADMIN role with nonblank assigned hospitalId
    val securityState = remember(currentRole, currentHospitalId) {
        when {
            currentRole == UserRole.HOSPITAL_ADMIN && !currentHospitalId.isNullOrBlank() ->
                AdminHomeSecurityState.Authorized
            currentRole == null && currentHospitalId.isNullOrBlank() && AppSession.currentPatientId.isBlank() ->
                AdminHomeSecurityState.Unauthenticated
            else ->
                AdminHomeSecurityState.Unauthorized
        }
    }

    when (securityState) {
        is AdminHomeSecurityState.Unauthenticated -> {
            AdminUnauthenticatedGate(
                onLoginClick = onLogout
            )
        }
        is AdminHomeSecurityState.Unauthorized -> {
            AdminUnauthorizedGate(
                onSwitchAccountClick = {
                    AppSession.logout()
                    onLogout()
                }
            )
        }
        is AdminHomeSecurityState.Authorized -> {
            val hospitalId = currentHospitalId!!
            val hospital = remember(hospitalId) { HospitalRepository.getHospital(hospitalId) }
            val adminDisplayName = if (currentUserName.isNotBlank()) currentUserName else "Administrator"

            var showLogoutDialog by remember { mutableStateOf(false) }

            // 0: Admissions, 1: Appointments, 2: Lab Tests
            var selectedTab by remember { mutableIntStateOf(0) }
            var selectedAdmissionFilter by remember { mutableStateOf(AdmissionFilter.ALL) }

            // Admissions state & stream observation
            var admissions by remember { mutableStateOf<List<Admission>?>(null) }
            var admissionsLoading by remember { mutableStateOf(true) }
            var admissionsError by remember { mutableStateOf<String?>(null) }
            var admissionsRetryTrigger by remember { mutableIntStateOf(0) }

            // Bookings state & retrieval
            var bookings by remember { mutableStateOf<List<Booking>?>(null) }
            var bookingsLoading by remember { mutableStateOf(true) }
            var bookingsError by remember { mutableStateOf<String?>(null) }
            var bookingsRetryTrigger by remember { mutableIntStateOf(0) }

            // Lab test action states
            var labTestToComplete by remember { mutableStateOf<Booking?>(null) }
            var isProcessingLabTest by remember { mutableStateOf(false) }

            // Real-time Firestore stream observation for admissions
            LaunchedEffect(hospitalId, admissionsRetryTrigger) {
                admissionsLoading = true
                admissionsError = null
                try {
                    admissionRepo.observeForHospital(hospitalId)
                        .catch { e ->
                            if (e is CancellationException) throw e
                            admissionsError = e.localizedMessage ?: "Failed to connect to admissions stream."
                            admissionsLoading = false
                        }
                        .collect { list ->
                            admissions = list
                            admissionsLoading = false
                            admissionsError = null
                        }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    admissionsError = e.localizedMessage ?: "Failed to load hospital admissions."
                    admissionsLoading = false
                }
            }

            // Bookings retrieval
            LaunchedEffect(hospitalId, bookingsRetryTrigger) {
                bookingsLoading = true
                bookingsError = null
                try {
                    bookings = bookingRepo.getForHospital(hospitalId)
                    bookingsLoading = false
                    bookingsError = null
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    bookingsError = e.localizedMessage ?: "Failed to load hospital bookings."
                    bookingsLoading = false
                }
            }

            fun refreshBookings() {
                coroutineScope.launch {
                    try {
                        bookings = bookingRepo.getForHospital(hospitalId)
                        bookingsError = null
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // Preserve existing bookings list on background refresh failure
                    }
                }
            }

            AdminHomeContent(
                hospital = hospital,
                adminName = adminDisplayName,
                selectedTab = selectedTab,
                onTabSelect = { selectedTab = it },
                selectedAdmissionFilter = selectedAdmissionFilter,
                onFilterSelect = { selectedAdmissionFilter = it },
                admissionsLoading = admissionsLoading,
                admissionsError = admissionsError,
                onRetryAdmissions = { admissionsRetryTrigger++ },
                admissions = admissions,
                bookingsLoading = bookingsLoading,
                bookingsError = bookingsError,
                onRetryBookings = { bookingsRetryTrigger++ },
                bookings = bookings,
                onAdmissionClick = onAdmissionClick,
                onMarkLabTestCompleted = { booking ->
                    if (!isProcessingLabTest) {
                        labTestToComplete = booking
                    }
                },
                onLogoutClick = { showLogoutDialog = true },
                isProcessingLabAction = isProcessingLabTest
            )

            // Logout Confirmation Dialog
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

            // Lab Test Completion Confirmation Dialog
            if (labTestToComplete != null) {
                val booking = labTestToComplete!!
                val test = booking.labTestId?.let { LabTestRepository.getTest(it) }
                val testTitle = test?.name ?: "diagnostic lab test"

                ConfirmDialog(
                    title = "Complete Lab Test",
                    message = "Mark ${booking.patientName}'s $testTitle (${booking.timeSlot}) as COMPLETED?",
                    confirmText = if (isProcessingLabTest) "Processing..." else "Mark Completed",
                    dismissText = "Cancel",
                    onConfirm = {
                        if (isProcessingLabTest) return@ConfirmDialog
                        isProcessingLabTest = true
                        coroutineScope.launch {
                            try {
                                val ok = bookingRepo.updateStatus(booking.id, BookingStatus.COMPLETED)
                                if (ok) {
                                    Toast.makeText(context, "Lab test marked as completed.", Toast.LENGTH_SHORT).show()
                                    refreshBookings()
                                } else {
                                    Toast.makeText(context, "Failed to update lab test status.", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_LONG).show()
                            } finally {
                                isProcessingLabTest = false
                                labTestToComplete = null
                            }
                        }
                    },
                    onDismiss = {
                        if (!isProcessingLabTest) {
                            labTestToComplete = null
                        }
                    }
                )
            }
        }
    }
}

/**
 * Stateless presentation composable for the Hospital Admin Management Console.
 * Enables 100% isolated Compose Previews without runtime Firebase dependencies.
 */
@Composable
fun AdminHomeContent(
    hospital: Hospital?,
    adminName: String,
    selectedTab: Int,
    onTabSelect: (Int) -> Unit,
    selectedAdmissionFilter: AdmissionFilter,
    onFilterSelect: (AdmissionFilter) -> Unit,
    admissionsLoading: Boolean,
    admissionsError: String?,
    onRetryAdmissions: () -> Unit,
    admissions: List<Admission>?,
    bookingsLoading: Boolean,
    bookingsError: String?,
    onRetryBookings: () -> Unit,
    bookings: List<Booking>?,
    onAdmissionClick: (String) -> Unit,
    onMarkLabTestCompleted: (Booking) -> Unit,
    onLogoutClick: () -> Unit,
    isProcessingLabAction: Boolean = false,
    doctorLookup: (String) -> Doctor? = { HospitalRepository.getDoctor(it) },
    labTestLookup: (String) -> LabTest? = { LabTestRepository.getTest(it) },
    modifier: Modifier = Modifier
) {
    // Statistics computations
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
                    IconButton(onClick = onLogoutClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        modifier = modifier
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

            // Stat Cards Row with interactive filter activation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    label = "Pending",
                    value = if (admissionsLoading && admissions == null) "—" else pendingAdmissionsCount.toString(),
                    icon = Icons.Default.HourglassTop,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            onTabSelect(0)
                            onFilterSelect(AdmissionFilter.PENDING)
                        }
                )
                StatCard(
                    label = "Approved",
                    value = if (admissionsLoading && admissions == null) "—" else approvedAdmissionsCount.toString(),
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            onTabSelect(0)
                            onFilterSelect(AdmissionFilter.APPROVED)
                        }
                )
                StatCard(
                    label = "OPD Today",
                    value = if (bookingsLoading && bookings == null) "—" else appointmentsTodayCount.toString(),
                    icon = Icons.Default.CalendarMonth,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            onTabSelect(1)
                        }
                )
                StatCard(
                    label = "Lab Today",
                    value = if (bookingsLoading && bookings == null) "—" else labTestsTodayCount.toString(),
                    icon = Icons.Default.Biotech,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            onTabSelect(2)
                        }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main Tab Navigation
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { onTabSelect(0) },
                    text = { Text("Admissions (${admissions?.size ?: 0})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { onTabSelect(1) },
                    text = {
                        val opdCount = bookings?.count { it.type == BookingType.OPD_APPOINTMENT } ?: 0
                        Text("Appointments ($opdCount)")
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { onTabSelect(2) },
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
                    AdmissionsTabContent(
                        admissions = admissions,
                        isLoading = admissionsLoading,
                        errorMessage = admissionsError,
                        selectedFilter = selectedAdmissionFilter,
                        onFilterSelect = onFilterSelect,
                        onRetry = onRetryAdmissions,
                        onAdmissionClick = onAdmissionClick
                    )
                }

                1 -> {
                    // TAB 2: APPOINTMENTS
                    AppointmentsTabContent(
                        bookings = bookings,
                        isLoading = bookingsLoading,
                        errorMessage = bookingsError,
                        onRetry = onRetryBookings,
                        doctorLookup = doctorLookup
                    )
                }

                2 -> {
                    // TAB 3: LAB TESTS
                    LabTestsTabContent(
                        bookings = bookings,
                        isLoading = bookingsLoading,
                        errorMessage = bookingsError,
                        onRetry = onRetryBookings,
                        onMarkCompleted = onMarkLabTestCompleted,
                        isProcessing = isProcessingLabAction,
                        labTestLookup = labTestLookup
                    )
                }
            }
        }
    }
}

@Composable
private fun AdmissionsTabContent(
    admissions: List<Admission>?,
    isLoading: Boolean,
    errorMessage: String?,
    selectedFilter: AdmissionFilter,
    onFilterSelect: (AdmissionFilter) -> Unit,
    onRetry: () -> Unit,
    onAdmissionClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Status Filter Chips Row with dynamic live counts
        val allCount = admissions?.size ?: 0
        val pendingCount = admissions?.count { it.status == AdmissionStatus.PENDING } ?: 0
        val approvedCount = admissions?.count { it.status == AdmissionStatus.APPROVED } ?: 0
        val rejectedCount = admissions?.count { it.status == AdmissionStatus.REJECTED } ?: 0
        val dischargedCount = admissions?.count { it.status == AdmissionStatus.DISCHARGED } ?: 0

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AdmissionFilter.entries.forEach { filter ->
                val count = when (filter) {
                    AdmissionFilter.ALL -> allCount
                    AdmissionFilter.PENDING -> pendingCount
                    AdmissionFilter.APPROVED -> approvedCount
                    AdmissionFilter.REJECTED -> rejectedCount
                    AdmissionFilter.DISCHARGED -> dischargedCount
                }
                val isSelected = selectedFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { onFilterSelect(filter) },
                    label = { Text("${filter.label} ($count)") },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        // Error banner if stream encountered an error but data was partially loaded
        if (errorMessage != null && admissions != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Admissions stream interrupted: $errorMessage",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    TextButton(onClick = onRetry) {
                        Text("Retry", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        when {
            isLoading && admissions == null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
            }

            errorMessage != null && admissions == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = Icons.Default.ErrorOutline,
                        message = "Unable to load admissions.\n\n$errorMessage",
                        action = {
                            Button(onClick = onRetry) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry")
                            }
                        }
                    )
                }
            }

            admissions != null -> {
                val filteredList = when (selectedFilter) {
                    AdmissionFilter.ALL -> admissions
                    AdmissionFilter.PENDING -> admissions.filter { it.status == AdmissionStatus.PENDING }
                    AdmissionFilter.APPROVED -> admissions.filter { it.status == AdmissionStatus.APPROVED }
                    AdmissionFilter.REJECTED -> admissions.filter { it.status == AdmissionStatus.REJECTED }
                    AdmissionFilter.DISCHARGED -> admissions.filter { it.status == AdmissionStatus.DISCHARGED }
                }

                if (admissions.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            icon = Icons.Default.Hotel,
                            message = "No hospital admission requests for this facility yet."
                        )
                    }
                } else if (filteredList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            icon = Icons.Default.FilterList,
                            message = "No admissions matching the '${selectedFilter.label}' filter.",
                            action = {
                                OutlinedButton(onClick = { onFilterSelect(AdmissionFilter.ALL) }) {
                                    Text("Show All Admissions")
                                }
                            }
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredList, key = { it.id }) { admission ->
                            AdminAdmissionCard(
                                admission = admission,
                                onClick = { onAdmissionClick(admission.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppointmentsTabContent(
    bookings: List<Booking>?,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    doctorLookup: (String) -> Doctor?
) {
    when {
        isLoading && bookings == null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
        }

        errorMessage != null && bookings == null -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    icon = Icons.Default.ErrorOutline,
                    message = "Unable to load appointments.\n\n$errorMessage",
                    action = {
                        Button(onClick = onRetry) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry")
                        }
                    }
                )
            }
        }

        bookings != null -> {
            val opdList = bookings.filter { it.type == BookingType.OPD_APPOINTMENT }
            if (opdList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = Icons.Default.CalendarMonth,
                        message = "No OPD doctor appointments scheduled for this hospital."
                    )
                }
            } else {
                // Group appointments by date, sorting dates descending
                val grouped = opdList.groupBy { it.dateMillis }
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
                            AdminAppointmentCard(
                                booking = booking,
                                doctorLookup = doctorLookup
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LabTestsTabContent(
    bookings: List<Booking>?,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    onMarkCompleted: (Booking) -> Unit,
    isProcessing: Boolean,
    labTestLookup: (String) -> LabTest?
) {
    when {
        isLoading && bookings == null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
        }

        errorMessage != null && bookings == null -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    icon = Icons.Default.ErrorOutline,
                    message = "Unable to load diagnostic lab tests.\n\n$errorMessage",
                    action = {
                        Button(onClick = onRetry) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry")
                        }
                    }
                )
            }
        }

        bookings != null -> {
            val labList = bookings.filter { it.type == BookingType.LAB_TEST }
                .sortedByDescending { it.dateMillis }

            if (labList.isEmpty()) {
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
                    items(labList, key = { it.id }) { booking ->
                        AdminLabTestCard(
                            booking = booking,
                            onMarkCompleted = { onMarkCompleted(booking) },
                            isProcessing = isProcessing,
                            labTestLookup = labTestLookup
                        )
                    }
                }
            }
        }
    }
}

/**
 * Enhanced inpatient admission card with demographics, room type, stay duration,
 * estimated stay cost, insurance coverage badge, assigned room, and rejection remarks.
 */
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
            // Header Row: Patient Name & Status Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = admission.patientName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                StatusChip(
                    text = admission.status.label,
                    containerColor = statusColor
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Demographics: Age, Gender, Blood Group, Phone
            Text(
                text = "${admission.patientAge} yrs, ${admission.patientGender} • Blood: ${admission.patientBloodGroup} • Phone: ${admission.patientPhone}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Admission Reason
            Text(
                text = "Reason: ${admission.reason}",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Room Type & Preferred Date
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

            Spacer(modifier = Modifier.height(4.dp))

            // Stay Duration, Estimated Cost, and Insurance Coverage Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stay: ${admission.expectedStayDays} ${if (admission.expectedStayDays == 1) "day" else "days"} • Est: ₹${admission.estimatedCost}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val insurance = admission.insuranceDetails
                if (insurance != null && insurance.policyNumber.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = "Insurance: ${insurance.providerName}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "Self-Pay",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // APPROVED State: Assigned Bed / Room Banner
            if (admission.status == AdmissionStatus.APPROVED && !admission.assignedRoom.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
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
                            text = "Assigned Bed / Room: ${admission.assignedRoom}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // REJECTED State: Rejection Remark Banner
            if (admission.status == AdmissionStatus.REJECTED && !admission.adminRemark.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Rejection Remark: ${admission.adminRemark}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // DISCHARGED State: Discharge Banner
            if (admission.status == AdmissionStatus.DISCHARGED) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Patient Discharged${if (!admission.assignedRoom.isNullOrBlank()) " • Concluded Room: ${admission.assignedRoom}" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer: Reference Code & Review Request Navigation
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
private fun AdminAppointmentCard(
    booking: Booking,
    doctorLookup: (String) -> Doctor?
) {
    val doctor = booking.doctorId?.let { doctorLookup(it) }
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
    onMarkCompleted: () -> Unit,
    isProcessing: Boolean,
    labTestLookup: (String) -> LabTest?
) {
    val test = booking.labTestId?.let { labTestLookup(it) }
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
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isProcessing
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isProcessing) "Updating..." else "Mark Completed")
                }
            }
        }
    }
}

/**
 * Gate displayed when no active Hospital Admin session is authenticated.
 */
@Composable
private fun AdminUnauthenticatedGate(
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            CareTopBar(title = "Hospital Admin")
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            EmptyState(
                icon = Icons.Default.AccountCircle,
                message = "Hospital Admin Session Required\n\nPlease sign in with your hospital administrator credentials to manage bed allotments, admissions, and hospital resources.",
                action = {
                    Button(onClick = onLoginClick) {
                        Text("Sign In as Hospital Admin")
                    }
                }
            )
        }
    }
}

/**
 * Gate displayed when the active session lacks hospital administrative authorization.
 */
@Composable
private fun AdminUnauthorizedGate(
    onSwitchAccountClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            CareTopBar(title = "Hospital Admin")
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            EmptyState(
                icon = Icons.Default.Block,
                message = "Access Restricted\n\nThis management console is restricted exclusively to authorized Hospital Administrators. Your active session does not have administrative privileges for this hospital.",
                action = {
                    Button(onClick = onSwitchAccountClick) {
                        Text("Switch Account / Logout")
                    }
                }
            )
        }
    }
}

// =============================================================================
// Isolated Compose Previews with Mock Data (Zero Firebase Dependencies)
// =============================================================================

private val previewMockHospital = Hospital(
    id = "hosp_kokilaben",
    name = "Kokilaben Dhirubhai Ambani Hospital",
    area = "Andheri West",
    city = "Mumbai",
    address = "Rao Saheb Achutrao Patwardhan Marg, Four Bungalows, Andheri West, Mumbai, Maharashtra 400053",
    phone = "+91 22 4269 6969",
    rating = 4.8,
    reviewCount = 1420,
    specialties = listOf("Cardiology", "Neurology", "Oncology", "Orthopedics"),
    facilities = listOf("24x7 Emergency", "ICU", "Blood Bank", "Diagnostics", "Cafeteria"),
    emergency24x7 = true,
    roomTypes = listOf(
        RoomType("General Ward", 2500, "Shared accommodation with nursing support"),
        RoomType("Semi-Private", 5500, "Twin-sharing with attached washroom"),
        RoomType("Private Deluxe", 12000, "Single patient room with attendant bed")
    ),
    aboutHtml = "Kokilaben Hospital is Mumbai's premier quaternary-care medical institution.",
    accentColorHex = "#0D6EFD"
)

private val previewMockAdmissions = listOf(
    Admission(
        id = "adm_101",
        patientId = "pat_rahul",
        hospitalId = "hosp_kokilaben",
        patientName = "Rahul Sharma",
        patientAge = 45,
        patientGender = "Male",
        patientBloodGroup = "O+",
        patientPhone = "9820112233",
        patientAddress = "102 Palm Residency, Andheri West, Mumbai",
        emergencyContactName = "Sunita Sharma",
        emergencyContactPhone = "9820112244",
        reason = "Elective laparoscopic cholecystectomy for symptomatic gallstones.",
        referringDoctor = "Dr. Sameer Joshi",
        roomType = "Private Deluxe",
        roomPricePerDay = 12000,
        preferredDateMillis = System.currentTimeMillis() + 86400000L,
        expectedStayDays = 3,
        estimatedCost = 36000,
        allergies = listOf("Penicillin"),
        chronicConditions = listOf("Hypertension"),
        currentMedications = listOf("Amlodipine 5mg"),
        insuranceDetails = InsuranceDetails(
            providerName = "Star Health Insurance",
            policyNumber = "SH-78901234",
            policyholderName = "Rahul Sharma",
            validityDateMillis = System.currentTimeMillis() + 31536000000L,
            sumInsured = 500000
        ),
        status = AdmissionStatus.PENDING,
        createdAtMillis = System.currentTimeMillis() - 7200000L
    ),
    Admission(
        id = "adm_102",
        patientId = "pat_priya",
        hospitalId = "hosp_kokilaben",
        patientName = "Priya Mehta",
        patientAge = 32,
        patientGender = "Female",
        patientBloodGroup = "B+",
        patientPhone = "9876543210",
        patientAddress = "Flat 401 Sunrise Towers, Juhu, Mumbai",
        emergencyContactName = "Vikram Mehta",
        emergencyContactPhone = "9876543211",
        reason = "Scheduled surgical delivery with perinatal monitoring.",
        referringDoctor = "Dr. Anita Desai",
        roomType = "Semi-Private",
        roomPricePerDay = 5500,
        preferredDateMillis = System.currentTimeMillis() + 172800000L,
        expectedStayDays = 4,
        estimatedCost = 22000,
        insuranceDetails = null,
        status = AdmissionStatus.APPROVED,
        assignedRoom = "Maternity Ward 2, Bed 204-B",
        createdAtMillis = System.currentTimeMillis() - 14400000L
    ),
    Admission(
        id = "adm_103",
        patientId = "pat_amit",
        hospitalId = "hosp_kokilaben",
        patientName = "Amit Patel",
        patientAge = 58,
        patientGender = "Male",
        patientBloodGroup = "A+",
        patientPhone = "9819001122",
        patientAddress = "B-14 Green Park, Versova, Mumbai",
        emergencyContactName = "Rekha Patel",
        emergencyContactPhone = "9819001133",
        reason = "Observation for acute transient ischemic episode.",
        roomType = "Private Deluxe",
        roomPricePerDay = 12000,
        preferredDateMillis = System.currentTimeMillis() - 86400000L,
        expectedStayDays = 2,
        estimatedCost = 24000,
        status = AdmissionStatus.REJECTED,
        adminRemark = "Neurology ICU suites currently at 100% capacity. Transferred to Lilavati Emergency.",
        createdAtMillis = System.currentTimeMillis() - 86400000L
    ),
    Admission(
        id = "adm_104",
        patientId = "pat_kavita",
        hospitalId = "hosp_kokilaben",
        patientName = "Kavita Rao",
        patientAge = 67,
        patientGender = "Female",
        patientBloodGroup = "AB+",
        patientPhone = "9820556677",
        patientAddress = "704 Silver Sands, Bandra West, Mumbai",
        emergencyContactName = "Girish Rao",
        emergencyContactPhone = "9820556688",
        reason = "Total knee arthroplasty rehabilitation.",
        roomType = "Semi-Private",
        roomPricePerDay = 5500,
        preferredDateMillis = System.currentTimeMillis() - 259200000L,
        expectedStayDays = 5,
        estimatedCost = 27500,
        status = AdmissionStatus.DISCHARGED,
        assignedRoom = "Ortho Ward 4, Bed 410",
        createdAtMillis = System.currentTimeMillis() - 259200000L
    )
)

private val previewMockBookings = listOf(
    Booking(
        id = "bk_001",
        type = BookingType.OPD_APPOINTMENT,
        patientId = "pat_rahul",
        patientName = "Rahul Sharma",
        hospitalId = "hosp_kokilaben",
        doctorId = "doc_sameer",
        labTestId = null,
        dateMillis = DateUtils.todayStartMillis(),
        timeSlot = "10:30 AM",
        reason = "Routine cardiology follow-up",
        sharedPortfolioText = "",
        status = BookingStatus.CONFIRMED,
        createdAtMillis = System.currentTimeMillis() - 86400000L
    ),
    Booking(
        id = "bk_002",
        type = BookingType.OPD_APPOINTMENT,
        patientId = "pat_priya",
        patientName = "Priya Mehta",
        hospitalId = "hosp_kokilaben",
        doctorId = "doc_sameer",
        labTestId = null,
        dateMillis = DateUtils.todayStartMillis(),
        timeSlot = "11:30 AM",
        reason = "Pre-operative evaluation",
        sharedPortfolioText = "",
        status = BookingStatus.CONFIRMED,
        createdAtMillis = System.currentTimeMillis() - 86400000L
    ),
    Booking(
        id = "bk_003",
        type = BookingType.LAB_TEST,
        patientId = "pat_rahul",
        patientName = "Rahul Sharma",
        hospitalId = "hosp_kokilaben",
        doctorId = null,
        labTestId = "test_lipid",
        dateMillis = DateUtils.todayStartMillis(),
        timeSlot = "08:00 AM",
        reason = "Fasting Lipid Profile",
        sharedPortfolioText = "",
        status = BookingStatus.CONFIRMED,
        createdAtMillis = System.currentTimeMillis() - 86400000L
    ),
    Booking(
        id = "bk_004",
        type = BookingType.LAB_TEST,
        patientId = "pat_priya",
        patientName = "Priya Mehta",
        hospitalId = "hosp_kokilaben",
        doctorId = null,
        labTestId = "test_cbc",
        dateMillis = DateUtils.todayStartMillis(),
        timeSlot = "09:00 AM",
        reason = "Complete Blood Count",
        sharedPortfolioText = "",
        status = BookingStatus.COMPLETED,
        createdAtMillis = System.currentTimeMillis() - 86400000L
    )
)

private val previewDoctorLookup: (String) -> Doctor? = {
    Doctor(
        id = "doc_sameer",
        hospitalId = "hosp_kokilaben",
        name = "Sameer Joshi",
        specialty = "Cardiology",
        qualifications = "MBBS, MD, DM (Cardiology)",
        experienceYears = 16,
        consultationFee = 1500,
        rating = 4.9,
        reviewCount = 310,
        availableDays = listOf("Mon", "Wed", "Fri"),
        availableSlots = listOf("10:00 AM", "10:30 AM", "11:00 AM"),
        languages = listOf("English", "Hindi", "Marathi"),
        about = "Senior Consultant Interventional Cardiologist."
    )
}

private val previewLabTestLookup: (String) -> LabTest? = { id ->
    when (id) {
        "test_lipid" -> LabTest(
            id = "test_lipid",
            name = "Lipid Profile Panel",
            category = "Biochemistry",
            price = 1200,
            requiresFasting = true,
            reportTimeHours = 24,
            description = "Measures cholesterol levels and triglycerides."
        )
        else -> LabTest(
            id = "test_cbc",
            name = "Complete Blood Count (CBC)",
            category = "Hematology",
            price = 450,
            requiresFasting = false,
            reportTimeHours = 12,
            description = "Analyzes red/white blood cells and platelets."
        )
    }
}

@Preview(showBackground = true, name = "Populated Console (All Admissions)")
@Composable
fun AdminHomeScreenPopulatedPreview() {
    CareConnectTheme {
        AdminHomeContent(
            hospital = previewMockHospital,
            adminName = "Dr. Suresh Kulkarni",
            selectedTab = 0,
            onTabSelect = {},
            selectedAdmissionFilter = AdmissionFilter.ALL,
            onFilterSelect = {},
            admissionsLoading = false,
            admissionsError = null,
            onRetryAdmissions = {},
            admissions = previewMockAdmissions,
            bookingsLoading = false,
            bookingsError = null,
            onRetryBookings = {},
            bookings = previewMockBookings,
            onAdmissionClick = {},
            onMarkLabTestCompleted = {},
            onLogoutClick = {},
            doctorLookup = previewDoctorLookup,
            labTestLookup = previewLabTestLookup
        )
    }
}

@Preview(showBackground = true, name = "Pending Admissions Filter")
@Composable
fun AdminHomeScreenPendingFilterPreview() {
    CareConnectTheme {
        AdminHomeContent(
            hospital = previewMockHospital,
            adminName = "Dr. Suresh Kulkarni",
            selectedTab = 0,
            onTabSelect = {},
            selectedAdmissionFilter = AdmissionFilter.PENDING,
            onFilterSelect = {},
            admissionsLoading = false,
            admissionsError = null,
            onRetryAdmissions = {},
            admissions = previewMockAdmissions,
            bookingsLoading = false,
            bookingsError = null,
            onRetryBookings = {},
            bookings = previewMockBookings,
            onAdmissionClick = {},
            onMarkLabTestCompleted = {},
            onLogoutClick = {},
            doctorLookup = previewDoctorLookup,
            labTestLookup = previewLabTestLookup
        )
    }
}

@Preview(showBackground = true, name = "Appointments Tab")
@Composable
fun AdminHomeScreenAppointmentsTabPreview() {
    CareConnectTheme {
        AdminHomeContent(
            hospital = previewMockHospital,
            adminName = "Dr. Suresh Kulkarni",
            selectedTab = 1,
            onTabSelect = {},
            selectedAdmissionFilter = AdmissionFilter.ALL,
            onFilterSelect = {},
            admissionsLoading = false,
            admissionsError = null,
            onRetryAdmissions = {},
            admissions = previewMockAdmissions,
            bookingsLoading = false,
            bookingsError = null,
            onRetryBookings = {},
            bookings = previewMockBookings,
            onAdmissionClick = {},
            onMarkLabTestCompleted = {},
            onLogoutClick = {},
            doctorLookup = previewDoctorLookup,
            labTestLookup = previewLabTestLookup
        )
    }
}

@Preview(showBackground = true, name = "Lab Tests Tab")
@Composable
fun AdminHomeScreenLabTestsTabPreview() {
    CareConnectTheme {
        AdminHomeContent(
            hospital = previewMockHospital,
            adminName = "Dr. Suresh Kulkarni",
            selectedTab = 2,
            onTabSelect = {},
            selectedAdmissionFilter = AdmissionFilter.ALL,
            onFilterSelect = {},
            admissionsLoading = false,
            admissionsError = null,
            onRetryAdmissions = {},
            admissions = previewMockAdmissions,
            bookingsLoading = false,
            bookingsError = null,
            onRetryBookings = {},
            bookings = previewMockBookings,
            onAdmissionClick = {},
            onMarkLabTestCompleted = {},
            onLogoutClick = {},
            doctorLookup = previewDoctorLookup,
            labTestLookup = previewLabTestLookup
        )
    }
}

@Preview(showBackground = true, name = "Loading State")
@Composable
fun AdminHomeScreenLoadingPreview() {
    CareConnectTheme {
        AdminHomeContent(
            hospital = previewMockHospital,
            adminName = "Administrator",
            selectedTab = 0,
            onTabSelect = {},
            selectedAdmissionFilter = AdmissionFilter.ALL,
            onFilterSelect = {},
            admissionsLoading = true,
            admissionsError = null,
            onRetryAdmissions = {},
            admissions = null,
            bookingsLoading = true,
            bookingsError = null,
            onRetryBookings = {},
            bookings = null,
            onAdmissionClick = {},
            onMarkLabTestCompleted = {},
            onLogoutClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Empty State")
@Composable
fun AdminHomeScreenEmptyPreview() {
    CareConnectTheme {
        AdminHomeContent(
            hospital = previewMockHospital,
            adminName = "Administrator",
            selectedTab = 0,
            onTabSelect = {},
            selectedAdmissionFilter = AdmissionFilter.ALL,
            onFilterSelect = {},
            admissionsLoading = false,
            admissionsError = null,
            onRetryAdmissions = {},
            admissions = emptyList(),
            bookingsLoading = false,
            bookingsError = null,
            onRetryBookings = {},
            bookings = emptyList(),
            onAdmissionClick = {},
            onMarkLabTestCompleted = {},
            onLogoutClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Error State")
@Composable
fun AdminHomeScreenErrorPreview() {
    CareConnectTheme {
        AdminHomeContent(
            hospital = previewMockHospital,
            adminName = "Administrator",
            selectedTab = 0,
            onTabSelect = {},
            selectedAdmissionFilter = AdmissionFilter.ALL,
            onFilterSelect = {},
            admissionsLoading = false,
            admissionsError = "Network connection timeout while connecting to Cloud Firestore.",
            onRetryAdmissions = {},
            admissions = null,
            bookingsLoading = false,
            bookingsError = "Failed to load hospital bookings.",
            onRetryBookings = {},
            bookings = null,
            onAdmissionClick = {},
            onMarkLabTestCompleted = {},
            onLogoutClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Unauthenticated Security Gate")
@Composable
fun AdminHomeScreenUnauthenticatedPreview() {
    CareConnectTheme {
        AdminUnauthenticatedGate(
            onLoginClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Unauthorized Security Gate")
@Composable
fun AdminHomeScreenUnauthorizedPreview() {
    CareConnectTheme {
        AdminUnauthorizedGate(
            onSwitchAccountClick = {}
        )
    }
}
