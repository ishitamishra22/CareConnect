package com.nmims.careconnect.ui.admission

/*
 * MyAdmissionsScreen:
 * Live list of hospital admissions for the currently logged-in patient:
 * - Observes patient admission requests in real-time via AdmissionRepository.observeForPatient()
 * - Divides admissions into Active (PENDING, APPROVED) and Past (REJECTED, DISCHARGED) tabs
 * - Displays live item count badges in tab headers
 * - Sorts each list by createdAtMillis descending
 * - Handles unauthenticated patient session with an explicit sign-in empty state
 * - Provides resilient error handling with a retry mechanism
 * - Admission cards display hospital name and location, room category, daily rate, expected stay,
 *   preferred date, estimated total cost, reference code, and semantic status chips
 * - Highlights assigned room allotment for approved admissions when available
 * - Displays administrative remarks or rejection reasons when provided
 * - Extracted stateless MyAdmissionsContent with isolated Compose Previews
 */

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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.nmims.careconnect.data.model.Hospital
import com.nmims.careconnect.data.repository.AdmissionRepositoryProvider
import com.nmims.careconnect.data.repository.HospitalRepository
import com.nmims.careconnect.session.AppSession
import com.nmims.careconnect.ui.common.CareTopBar
import com.nmims.careconnect.ui.common.DateUtils
import com.nmims.careconnect.ui.common.EmptyState
import com.nmims.careconnect.ui.common.LoadingIndicator
import com.nmims.careconnect.ui.common.StatusChip
import com.nmims.careconnect.ui.theme.CareConnectTheme
import kotlinx.coroutines.flow.catch

@Composable
fun MyAdmissionsScreen(
    onNavigateBack: () -> Unit,
    onAdmissionClick: (String) -> Unit
) {
    val context = LocalContext.current
    val repository = remember(context) { AdmissionRepositoryProvider.get(context) }
    val patientId = AppSession.currentPatientId

    var admissions by remember { mutableStateOf<List<Admission>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }
    var retryTrigger by remember { mutableIntStateOf(0) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val isUnauthenticated = patientId.isBlank()

    LaunchedEffect(patientId, retryTrigger) {
        if (patientId.isBlank()) {
            admissions = emptyList()
            isLoading = false
            isError = false
            return@LaunchedEffect
        }

        isLoading = true
        isError = false

        try {
            repository.observeForPatient(patientId)
                .catch {
                    isError = true
                    isLoading = false
                }
                .collect { list ->
                    admissions = list
                    isLoading = false
                    isError = false
                }
        } catch (_: Exception) {
            isError = true
            isLoading = false
        }
    }

    val allAdmissions = admissions ?: emptyList()

    val activeAdmissions = remember(admissions) {
        allAdmissions
            .filter { it.status == AdmissionStatus.PENDING || it.status == AdmissionStatus.APPROVED }
            .sortedByDescending { it.createdAtMillis }
    }

    val pastAdmissions = remember(admissions) {
        allAdmissions
            .filter { it.status == AdmissionStatus.REJECTED || it.status == AdmissionStatus.DISCHARGED }
            .sortedByDescending { it.createdAtMillis }
    }

    MyAdmissionsContent(
        selectedTab = selectedTab,
        onTabSelected = { selectedTab = it },
        activeAdmissions = activeAdmissions,
        pastAdmissions = pastAdmissions,
        isLoading = isLoading && admissions == null,
        isError = isError && admissions == null,
        isUnauthenticated = isUnauthenticated,
        onRetry = { retryTrigger++ },
        onNavigateBack = onNavigateBack,
        onAdmissionClick = onAdmissionClick,
        hospitalLookup = { hospitalId -> HospitalRepository.getHospital(hospitalId) }
    )
}

@Composable
fun MyAdmissionsContent(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    activeAdmissions: List<Admission>,
    pastAdmissions: List<Admission>,
    isLoading: Boolean,
    isError: Boolean,
    isUnauthenticated: Boolean,
    onRetry: () -> Unit,
    onNavigateBack: () -> Unit,
    onAdmissionClick: (String) -> Unit,
    hospitalLookup: (String) -> Hospital? = { HospitalRepository.getHospital(it) },
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            CareTopBar(
                title = "My Admissions",
                onBack = onNavigateBack
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!isUnauthenticated) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { onTabSelected(0) },
                        text = {
                            Text(
                                text = "Active (${activeAdmissions.size})",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { onTabSelected(1) },
                        text = {
                            Text(
                                text = "Past (${pastAdmissions.size})",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when {
                    isUnauthenticated -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyState(
                                icon = Icons.Default.AccountCircle,
                                message = "Patient Sign-In Required\n\nPlease sign in with a patient account to view and track your hospital admission requests."
                            )
                        }
                    }
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingIndicator()
                        }
                    }
                    isError -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyState(
                                icon = Icons.Default.ErrorOutline,
                                message = "Unable to load admissions.\n\nPlease check your internet connection and try again.",
                                action = {
                                    Button(onClick = onRetry) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Retry")
                                    }
                                }
                            )
                        }
                    }
                    selectedTab == 0 && activeAdmissions.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyState(
                                icon = Icons.Default.Hotel,
                                message = "No active admissions found.\n\nYou have no pending requests or active inpatient hospital stays at this time."
                            )
                        }
                    }
                    selectedTab == 1 && pastAdmissions.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyState(
                                icon = Icons.Default.History,
                                message = "No past admissions found.\n\nYour completed or discharged admission history will appear here."
                            )
                        }
                    }
                    else -> {
                        val currentList = if (selectedTab == 0) activeAdmissions else pastAdmissions
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(currentList, key = { it.id }) { admission ->
                                AdmissionCard(
                                    admission = admission,
                                    hospitalLookup = hospitalLookup,
                                    onClick = { onAdmissionClick(admission.id) }
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
private fun AdmissionCard(
    admission: Admission,
    hospitalLookup: (String) -> Hospital?,
    onClick: () -> Unit
) {
    val hospital = remember(admission.hospitalId) {
        hospitalLookup(admission.hospitalId)
    }

    val statusColor = when (admission.status) {
        AdmissionStatus.PENDING -> Color(0xFFFFF3CD)
        AdmissionStatus.APPROVED -> Color(0xFFD1E7DD)
        AdmissionStatus.REJECTED -> Color(0xFFF8D7DA)
        AdmissionStatus.DISCHARGED -> Color(0xFFE2E3E5)
    }

    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Hospital Name, Location, and StatusChip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = hospital?.name ?: admission.hospitalId,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val hospitalLocation = if (hospital != null && hospital.area.isNotBlank()) {
                        "${hospital.area}, ${hospital.city}"
                    } else {
                        hospital?.city ?: ""
                    }
                    if (hospitalLocation.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = hospitalLocation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                StatusChip(
                    text = admission.status.label,
                    containerColor = statusColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Room Type, Daily Rate, Expected Stay
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${admission.roomType} • ₹${admission.roomPricePerDay}/day",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${admission.expectedStayDays} ${if (admission.expectedStayDays == 1) "day" else "days"} stay",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Preferred Date & Estimated Total Cost
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Date: ${DateUtils.formatWithDay(admission.preferredDateMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Est. Total: ₹${admission.estimatedCost}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Assigned Room Banner (Only when APPROVED and assignedRoom is present)
            if (admission.status == AdmissionStatus.APPROVED && !admission.assignedRoom.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MeetingRoom,
                            contentDescription = "Assigned Room",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Assigned Room: ${admission.assignedRoom}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Administrative Remarks / Rejection Reason (Only when adminRemark is present)
            if (!admission.adminRemark.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                val isRejected = admission.status == AdmissionStatus.REJECTED
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isRejected) {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = if (isRejected) Icons.Default.ErrorOutline else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isRejected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = if (isRejected) "Rejection Reason:" else "Hospital Remark:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isRejected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = admission.adminRemark!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer: Reference Code and Navigation affordance
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
                        text = "View Details",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(2.dp))
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

// ---------------------------------------------------------------------------
// Previews (Completely isolated from runtime Firebase)
// ---------------------------------------------------------------------------

private val previewMockHospital = Hospital(
    id = "HOSP001",
    name = "Lilavati Hospital & Research Centre",
    area = "Bandra West",
    city = "Mumbai",
    address = "A-791, Bandra Reclamation, Bandra West, Mumbai",
    phone = "+91 22 2675 1000",
    rating = 4.7,
    reviewCount = 320,
    specialties = listOf("Cardiology", "Orthopedics"),
    facilities = listOf("24/7 ICU", "Pharmacy"),
    emergency24x7 = true,
    roomTypes = emptyList(),
    aboutHtml = "<p>Lilavati Hospital</p>",
    accentColorHex = "#1E88E5"
)

private val previewActiveAdmissions = listOf(
    Admission(
        id = "ADM001-ABC",
        patientId = "P001",
        patientName = "Rahul Sharma",
        hospitalId = "HOSP001",
        reasonForAdmission = "Cardiac evaluation and angiography procedure",
        preferredDateMillis = System.currentTimeMillis() + 86400000L * 2,
        expectedStayDays = 3,
        roomTypeName = "Deluxe Room",
        pricePerDay = 6500,
        estimatedCost = 19500,
        patientAge = 42,
        gender = "Male",
        bloodGroup = "O+",
        phone = "+91 98200 12345",
        address = "Andheri West, Mumbai",
        emergencyContactName = "Priya Sharma",
        emergencyContactPhone = "+91 98200 54321",
        status = AdmissionStatus.APPROVED,
        assignedRoomNumber = "Bed 402-A",
        adminRemark = "Please report to Inpatient Reception, 4th Floor at 8:00 AM",
        createdAtMillis = System.currentTimeMillis() - 86400000L
    ),
    Admission(
        id = "ADM002-DEF",
        patientId = "P001",
        patientName = "Rahul Sharma",
        hospitalId = "HOSP001",
        reasonForAdmission = "Observation for persistent abdominal discomfort",
        preferredDateMillis = System.currentTimeMillis() + 86400000L * 5,
        expectedStayDays = 2,
        roomTypeName = "Semi-Private",
        pricePerDay = 3500,
        estimatedCost = 7000,
        patientAge = 42,
        gender = "Male",
        bloodGroup = "O+",
        phone = "+91 98200 12345",
        address = "Andheri West, Mumbai",
        emergencyContactName = "Priya Sharma",
        emergencyContactPhone = "+91 98200 54321",
        status = AdmissionStatus.PENDING,
        createdAtMillis = System.currentTimeMillis() - 3600000L * 2
    )
)

private val previewPastAdmissions = listOf(
    Admission(
        id = "ADM003-GHI",
        patientId = "P001",
        patientName = "Rahul Sharma",
        hospitalId = "HOSP001",
        reasonForAdmission = "Knee arthroscopy and physical therapy rehabilitation",
        preferredDateMillis = System.currentTimeMillis() - 86400000L * 30,
        expectedStayDays = 4,
        roomTypeName = "General Ward",
        pricePerDay = 2000,
        estimatedCost = 8000,
        patientAge = 42,
        gender = "Male",
        bloodGroup = "O+",
        phone = "+91 98200 12345",
        address = "Andheri West, Mumbai",
        emergencyContactName = "Priya Sharma",
        emergencyContactPhone = "+91 98200 54321",
        status = AdmissionStatus.DISCHARGED,
        assignedRoomNumber = "Ward 12-Bed 4",
        createdAtMillis = System.currentTimeMillis() - 86400000L * 35
    ),
    Admission(
        id = "ADM004-JKL",
        patientId = "P001",
        patientName = "Rahul Sharma",
        hospitalId = "HOSP001",
        reasonForAdmission = "Elective cosmetic revision procedure",
        preferredDateMillis = System.currentTimeMillis() - 86400000L * 15,
        expectedStayDays = 1,
        roomTypeName = "ICU Suite",
        pricePerDay = 12000,
        estimatedCost = 12000,
        patientAge = 42,
        gender = "Male",
        bloodGroup = "O+",
        phone = "+91 98200 12345",
        address = "Andheri West, Mumbai",
        emergencyContactName = "Priya Sharma",
        emergencyContactPhone = "+91 98200 54321",
        status = AdmissionStatus.REJECTED,
        adminRemark = "Requested ICU suite unavailable on chosen date. Please consult OPD specialist first.",
        createdAtMillis = System.currentTimeMillis() - 86400000L * 18
    )
)

@Preview(showBackground = true, name = "Active Admissions")
@Composable
fun MyAdmissionsScreenActivePreview() {
    CareConnectTheme {
        MyAdmissionsContent(
            selectedTab = 0,
            onTabSelected = {},
            activeAdmissions = previewActiveAdmissions,
            pastAdmissions = previewPastAdmissions,
            isLoading = false,
            isError = false,
            isUnauthenticated = false,
            onRetry = {},
            onNavigateBack = {},
            onAdmissionClick = {},
            hospitalLookup = { previewMockHospital }
        )
    }
}

@Preview(showBackground = true, name = "Past Admissions")
@Composable
fun MyAdmissionsScreenPastPreview() {
    CareConnectTheme {
        MyAdmissionsContent(
            selectedTab = 1,
            onTabSelected = {},
            activeAdmissions = previewActiveAdmissions,
            pastAdmissions = previewPastAdmissions,
            isLoading = false,
            isError = false,
            isUnauthenticated = false,
            onRetry = {},
            onNavigateBack = {},
            onAdmissionClick = {},
            hospitalLookup = { previewMockHospital }
        )
    }
}

@Preview(showBackground = true, name = "Empty Active Tab")
@Composable
fun MyAdmissionsScreenEmptyPreview() {
    CareConnectTheme {
        MyAdmissionsContent(
            selectedTab = 0,
            onTabSelected = {},
            activeAdmissions = emptyList(),
            pastAdmissions = emptyList(),
            isLoading = false,
            isError = false,
            isUnauthenticated = false,
            onRetry = {},
            onNavigateBack = {},
            onAdmissionClick = {},
            hospitalLookup = { previewMockHospital }
        )
    }
}

@Preview(showBackground = true, name = "Unauthenticated Session")
@Composable
fun MyAdmissionsScreenUnauthenticatedPreview() {
    CareConnectTheme {
        MyAdmissionsContent(
            selectedTab = 0,
            onTabSelected = {},
            activeAdmissions = emptyList(),
            pastAdmissions = emptyList(),
            isLoading = false,
            isError = false,
            isUnauthenticated = true,
            onRetry = {},
            onNavigateBack = {},
            onAdmissionClick = {},
            hospitalLookup = { previewMockHospital }
        )
    }
}

@Preview(showBackground = true, name = "Default Preview")
@Composable
fun MyAdmissionsScreenPreview() {
    MyAdmissionsScreenActivePreview()
}
