package com.nmims.careconnect.ui.patient

/*
 * PatientHomeScreen:
 * Patient dashboard containing:
 * - TopBar with "CareConnect" brand title and a Logout icon (with ConfirmDialog -> AppSession.logout() -> resetTo(RoleSelection))
 * - Greeting: "Hello, <name>"
 * - Real-time observation of patient's inpatient hospital admission requests via AdmissionRepository.observeForPatient()
 * - Active Inpatient Admission Status Card:
 *   - Displayed when an active admission (PENDING or APPROVED) exists
 *   - Shows resolved hospital name (with "Partner Hospital" safe fallback), semantic status chip, preferred date, stay duration
 *   - Highlights assigned room/bed allotment (admission.assignedRoom) when approved
 *   - Tap action opens My Admissions
 * - Non-blocking stream failure handling with a compact Retry banner, preserving previous valid data
 * - Six fully-integrated feature cards:
 *   1. My Medical Portfolio (wired to Screen.Portfolio)
 *   2. Find Hospitals & Doctors (wired to Screen.Search)
 *   3. Book Appointment / Lab Test (wired to Screen.BookingHub)
 *   4. My Bookings (wired to Screen.MyBookings)
 *   5. Hospital Admission (wired to Screen.AdmissionForm)
 *   6. My Admissions (dynamically reflects active admissions count)
 * - Extracted stateless PatientHomeContent and Firebase-independent Compose Previews
 */

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
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
import com.nmims.careconnect.ui.common.ConfirmDialog
import com.nmims.careconnect.ui.common.DateUtils
import com.nmims.careconnect.ui.common.StatusChip
import com.nmims.careconnect.ui.theme.CareConnectTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch

data class FeatureItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun PatientHomeScreen(
    onNavigateToPortfolio: () -> Unit,
    onNavigateToSearch: () -> Unit = {},
    onNavigateToBookingHub: () -> Unit = {},
    onNavigateToMyBookings: () -> Unit = {},
    onNavigateToAdmissionForm: () -> Unit = {},
    onNavigateToMyAdmissions: () -> Unit = {},
    onAdmissionClick: (String) -> Unit = {},
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember(context) { AdmissionRepositoryProvider.get(context) }
    val patientId = AppSession.currentPatientId
    val userName = if (AppSession.currentUserName.isNotBlank()) AppSession.currentUserName else "Patient"

    var admissions by remember { mutableStateOf<List<Admission>?>(null) }
    var isStreamError by remember { mutableStateOf(false) }
    var retryTrigger by remember { mutableIntStateOf(0) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(patientId, retryTrigger) {
        if (patientId.isBlank()) {
            admissions = emptyList()
            isStreamError = false
            return@LaunchedEffect
        }

        try {
            repository.observeForPatient(patientId)
                .catch { e ->
                    if (e is CancellationException) throw e
                    isStreamError = true
                }
                .collect { list ->
                    admissions = list
                    isStreamError = false
                }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            isStreamError = true
        }
    }

    val activeAdmissions = remember(admissions) {
        (admissions ?: emptyList())
            .filter { it.status == AdmissionStatus.PENDING || it.status == AdmissionStatus.APPROVED }
            .sortedByDescending { it.createdAtMillis }
    }
    val latestActiveAdmission = activeAdmissions.firstOrNull()
    val activeCount = activeAdmissions.size

    PatientHomeContent(
        userName = userName,
        activeAdmission = latestActiveAdmission,
        activeCount = activeCount,
        isStreamError = isStreamError,
        onRetryStream = { retryTrigger++ },
        onNavigateToPortfolio = onNavigateToPortfolio,
        onNavigateToSearch = onNavigateToSearch,
        onNavigateToBookingHub = onNavigateToBookingHub,
        onNavigateToMyBookings = onNavigateToMyBookings,
        onNavigateToAdmissionForm = onNavigateToAdmissionForm,
        onNavigateToMyAdmissions = onNavigateToMyAdmissions,
        onAdmissionClick = onAdmissionClick,
        onLogoutClick = { showLogoutDialog = true }
    )

    if (showLogoutDialog) {
        ConfirmDialog(
            title = "Logout",
            message = "Are you sure you want to log out of CareConnect?",
            confirmText = "Logout",
            onConfirm = {
                AppSession.logout()
                onLogout()
            },
            onDismiss = { showLogoutDialog = false }
        )
    }
}

@Composable
fun PatientHomeContent(
    userName: String,
    activeAdmission: Admission?,
    activeCount: Int,
    isStreamError: Boolean,
    onRetryStream: () -> Unit,
    onNavigateToPortfolio: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToBookingHub: () -> Unit,
    onNavigateToMyBookings: () -> Unit,
    onNavigateToAdmissionForm: () -> Unit,
    onNavigateToMyAdmissions: () -> Unit,
    onLogoutClick: () -> Unit,
    onAdmissionClick: (String) -> Unit = {},
    hospitalLookup: (String) -> Hospital? = { HospitalRepository.getHospital(it) },
    modifier: Modifier = Modifier
) {
    val admissionsSubtitle = when {
        activeCount > 0 -> "$activeCount active admission request${if (activeCount > 1) "s" else ""} • Tap to view tracking"
        else -> "View current inpatient status, room allotment & discharge summaries"
    }

    val features = listOf(
        FeatureItem(
            title = "My Medical Portfolio",
            subtitle = "Manage health profile, records, uploaded reports & view forwardable summary",
            icon = Icons.Default.FolderShared,
            onClick = onNavigateToPortfolio
        ),
        FeatureItem(
            title = "Find Hospitals & Doctors",
            subtitle = "Search specialists, compare facilities, reviews & consultation fees",
            icon = Icons.Default.Search,
            onClick = onNavigateToSearch
        ),
        FeatureItem(
            title = "Book Appointment / Lab Test",
            subtitle = "Schedule in-person or video consultations and diagnostic lab tests",
            icon = Icons.Default.CalendarMonth,
            onClick = onNavigateToBookingHub
        ),
        FeatureItem(
            title = "My Bookings",
            subtitle = "Track active tokens, appointment slots, test results & past visits",
            icon = Icons.Default.CheckCircleOutline,
            onClick = onNavigateToMyBookings
        ),
        FeatureItem(
            title = "Hospital Admission",
            subtitle = "Direct admission request, bed allotment & emergency room triage",
            icon = Icons.Default.Hotel,
            onClick = onNavigateToAdmissionForm
        ),
        FeatureItem(
            title = "My Admissions",
            subtitle = admissionsSubtitle,
            icon = Icons.Default.LocalHospital,
            onClick = onNavigateToMyAdmissions
        )
    )

    Scaffold(
        topBar = {
            CareTopBar(
                title = "CareConnect",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = "Hello, $userName 👋",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your healthcare hub. Book appointments, manage records & admissions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Non-blocking stream error banner with retry option
            if (isStreamError) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
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
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Unable to update live admission status",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            TextButton(
                                onClick = onRetryStream,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Retry",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            // Interactive Active Inpatient Status Card (Only when activeAdmission != null)
            if (activeAdmission != null) {
                item {
                    ActiveAdmissionCard(
                        admission = activeAdmission,
                        hospitalLookup = hospitalLookup,
                        onClick = { onAdmissionClick(activeAdmission.id) }
                    )
                }
            }

            items(features) { feature ->
                FeatureCard(item = feature)
            }
        }
    }
}

@Composable
fun ActiveAdmissionCard(
    admission: Admission,
    hospitalLookup: (String) -> Hospital?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val hospitalName = remember(admission.hospitalId) {
        try {
            hospitalLookup(admission.hospitalId)?.name?.takeIf { it.isNotBlank() } ?: "Partner Hospital"
        } catch (_: Exception) {
            "Partner Hospital"
        }
    }

    val statusColor = when (admission.status) {
        AdmissionStatus.APPROVED -> MaterialTheme.colorScheme.primaryContainer
        AdmissionStatus.PENDING -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Hospital Name & StatusChip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalHospital,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = hospitalName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                StatusChip(
                    text = admission.status.label,
                    containerColor = statusColor
                )
            }

            // Room type, daily rate, stay duration & preferred date
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "${admission.roomType} • ₹${admission.roomPricePerDay}/day",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Preferred Date: ${DateUtils.format(admission.preferredDateMillis)} • Stay: ${admission.expectedStayDays} ${if (admission.expectedStayDays == 1) "day" else "days"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Assigned Bed Allotment Banner (When APPROVED and assignedRoom is present)
            if (admission.status == AdmissionStatus.APPROVED && !admission.assignedRoom.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MeetingRoom,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Assigned Bed / Room: ${admission.assignedRoom}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            // Footer Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = "Ref: ${admission.referenceCode()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(admission.referenceCode()))
                            Toast.makeText(
                                context,
                                "Reference code copied to clipboard",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Reference Code",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "View Details",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FeatureCard(
    item: FeatureItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { item.onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ---------------------------------------------------------
// Previews (Decoupled from Firebase and Repositories)
// ---------------------------------------------------------

private val mockHospitalForPreview = Hospital(
    id = "hosp_1",
    name = "Lilavati Hospital & Research Centre",
    area = "Bandra West",
    city = "Mumbai",
    address = "A-791, Bandra Reclamation, Bandra West, Mumbai 400050",
    phone = "+91 22 2675 1000",
    rating = 4.7,
    reviewCount = 312,
    specialties = listOf("Cardiology", "Neurology"),
    facilities = listOf("ICU", "Cath Lab"),
    emergency24x7 = true,
    roomTypes = emptyList(),
    aboutHtml = "<p>About</p>",
    accentColorHex = "#1F7A8C"
)

private val mockPendingAdmissionForPreview = Admission(
    id = "adm-preview-pending-12345",
    patientId = "patient_1",
    patientName = "Rahul Sharma",
    hospitalId = "hosp_1",
    reasonForAdmission = "Severe respiratory distress requiring inpatient observation",
    referringDoctor = "Dr. Amit Verma",
    preferredDateMillis = System.currentTimeMillis() + 86400000L,
    expectedStayDays = 3,
    roomTypeName = "Deluxe Room",
    pricePerDay = 6000,
    estimatedCost = 18000,
    patientAge = 34,
    gender = "Male",
    bloodGroup = "O+",
    phone = "9876543210",
    address = "123 Marine Drive, Mumbai",
    emergencyContactName = "Sunita Sharma",
    emergencyContactPhone = "9876543211",
    allergies = emptyList(),
    chronicConditions = emptyList(),
    currentMedications = emptyList(),
    sharedPortfolioText = "Sample portfolio summary",
    insurance = null,
    status = AdmissionStatus.PENDING,
    assignedRoomNumber = null,
    adminRemark = null,
    createdAtMillis = System.currentTimeMillis()
)

private val mockApprovedAdmissionForPreview = Admission(
    id = "adm-preview-approved-67890",
    patientId = "patient_1",
    patientName = "Rahul Sharma",
    hospitalId = "hosp_1",
    reasonForAdmission = "Cardiac monitoring and post-operative evaluation",
    referringDoctor = "Dr. Priya Desai",
    preferredDateMillis = System.currentTimeMillis() + 172800000L,
    expectedStayDays = 5,
    roomTypeName = "ICU Bed",
    pricePerDay = 12000,
    estimatedCost = 60000,
    patientAge = 34,
    gender = "Male",
    bloodGroup = "O+",
    phone = "9876543210",
    address = "123 Marine Drive, Mumbai",
    emergencyContactName = "Sunita Sharma",
    emergencyContactPhone = "9876543211",
    allergies = emptyList(),
    chronicConditions = emptyList(),
    currentMedications = emptyList(),
    sharedPortfolioText = "Sample portfolio summary",
    insurance = null,
    status = AdmissionStatus.APPROVED,
    assignedRoomNumber = "Ward 3, Bed 305-B",
    adminRemark = "Admitted to Ward 3",
    createdAtMillis = System.currentTimeMillis()
)

@Preview(showBackground = true)
@Composable
fun PatientHomeDefaultPreview() {
    CareConnectTheme {
        PatientHomeContent(
            userName = "Rahul Sharma",
            activeAdmission = null,
            activeCount = 0,
            isStreamError = false,
            onRetryStream = {},
            onNavigateToPortfolio = {},
            onNavigateToSearch = {},
            onNavigateToBookingHub = {},
            onNavigateToMyBookings = {},
            onNavigateToAdmissionForm = {},
            onNavigateToMyAdmissions = {},
            onAdmissionClick = {},
            onLogoutClick = {},
            hospitalLookup = { mockHospitalForPreview }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PatientHomePendingPreview() {
    CareConnectTheme {
        PatientHomeContent(
            userName = "Rahul Sharma",
            activeAdmission = mockPendingAdmissionForPreview,
            activeCount = 1,
            isStreamError = false,
            onRetryStream = {},
            onNavigateToPortfolio = {},
            onNavigateToSearch = {},
            onNavigateToBookingHub = {},
            onNavigateToMyBookings = {},
            onNavigateToAdmissionForm = {},
            onNavigateToMyAdmissions = {},
            onAdmissionClick = {},
            onLogoutClick = {},
            hospitalLookup = { mockHospitalForPreview }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PatientHomeApprovedPreview() {
    CareConnectTheme {
        PatientHomeContent(
            userName = "Rahul Sharma",
            activeAdmission = mockApprovedAdmissionForPreview,
            activeCount = 1,
            isStreamError = false,
            onRetryStream = {},
            onNavigateToPortfolio = {},
            onNavigateToSearch = {},
            onNavigateToBookingHub = {},
            onNavigateToMyBookings = {},
            onNavigateToAdmissionForm = {},
            onNavigateToMyAdmissions = {},
            onAdmissionClick = {},
            onLogoutClick = {},
            hospitalLookup = { mockHospitalForPreview }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PatientHomeErrorPreview() {
    CareConnectTheme {
        PatientHomeContent(
            userName = "Rahul Sharma",
            activeAdmission = mockApprovedAdmissionForPreview,
            activeCount = 1,
            isStreamError = true,
            onRetryStream = {},
            onNavigateToPortfolio = {},
            onNavigateToSearch = {},
            onNavigateToBookingHub = {},
            onNavigateToMyBookings = {},
            onNavigateToAdmissionForm = {},
            onNavigateToMyAdmissions = {},
            onAdmissionClick = {},
            onLogoutClick = {},
            hospitalLookup = { mockHospitalForPreview }
        )
    }
}
