package com.nmims.careconnect.ui.patient

/*
 * PatientHomeScreen:
 * Redesigned Patient Home Dashboard:
 * - Personalized welcome header with InitialsAvatar
 * - Clinical health-focused hero card with fast summary
 * - Modern 2x2 quick action grid (Portfolio, Search/Doctors, Book Consultation, My Bookings)
 * - Hospital Admission & Inpatient Tracking shortcuts
 * - Live Inpatient Admission Status Card with room allotment banner
 * - Real-time upcoming appointment/lab booking card with fast token preview
 * - Comprehensive booking history shortcut
 * - Full backward-compatible parameters and callbacks
 */

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.nmims.careconnect.data.auth.AuthValidators
import com.nmims.careconnect.data.repository.AuthRepository
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Science
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
import com.nmims.careconnect.data.model.Booking
import com.nmims.careconnect.data.model.BookingStatus
import com.nmims.careconnect.data.model.BookingType
import com.nmims.careconnect.data.model.Doctor
import com.nmims.careconnect.data.model.Hospital
import com.nmims.careconnect.data.model.LabTest
import com.nmims.careconnect.data.repository.AdmissionRepositoryProvider
import com.nmims.careconnect.data.repository.BookingRepositoryProvider
import com.nmims.careconnect.data.repository.HospitalRepository
import com.nmims.careconnect.session.AppSession
import com.nmims.careconnect.ui.common.CareTopBar
import com.nmims.careconnect.ui.common.ConfirmDialog
import com.nmims.careconnect.ui.common.DateUtils
import com.nmims.careconnect.ui.common.StatusChip
import com.nmims.careconnect.ui.patient.components.PatientActionTile
import com.nmims.careconnect.ui.patient.components.PatientHeroCard
import com.nmims.careconnect.ui.patient.components.PatientQuickActionCard
import com.nmims.careconnect.ui.patient.components.PatientSectionHeader
import com.nmims.careconnect.ui.patient.components.PatientWelcomeHeader
import com.nmims.careconnect.ui.patient.components.UpcomingBookingCard
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
    onLogout: () -> Unit,
    onNavigateToEmailVerification: (() -> Unit)? = null,
    authRepository: AuthRepository = AuthRepository.getInstance()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val admissionRepo = remember(context) { AdmissionRepositoryProvider.get(context) }
    val bookingRepo = remember(context) { BookingRepositoryProvider.get(context) }
    val patientId = AppSession.currentPatientId
    val userName = if (AppSession.currentUserName.isNotBlank()) AppSession.currentUserName else "Patient"

    var admissions by remember { mutableStateOf<List<Admission>?>(null) }
    var upcomingBookings by remember { mutableStateOf<List<Booking>>(emptyList()) }
    var isStreamError by remember { mutableStateOf(false) }
    var retryTrigger by remember { mutableIntStateOf(0) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var isEmailVerified by remember { mutableStateOf(authRepository.isCurrentUserEmailVerified()) }

    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var oldPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmNewPasswordVisible by remember { mutableStateOf(false) }
    var changePasswordLoading by remember { mutableStateOf(false) }
    var changePasswordError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(patientId, retryTrigger) {
        if (patientId.isBlank()) {
            admissions = emptyList()
            upcomingBookings = emptyList()
            isStreamError = false
            return@LaunchedEffect
        }

        try {
            admissionRepo.observeForPatient(patientId)
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

    LaunchedEffect(patientId, retryTrigger) {
        if (patientId.isNotBlank()) {
            try {
                val list = bookingRepo.getForPatient(patientId)
                val now = System.currentTimeMillis() - (2 * 60 * 60 * 1000L) // allow ongoing day
                upcomingBookings = list.filter {
                    it.status == BookingStatus.CONFIRMED && it.dateMillis >= now
                }.sortedBy { it.dateMillis }
            } catch (_: Exception) {
                upcomingBookings = emptyList()
            }
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
        upcomingBookings = upcomingBookings,
        isStreamError = isStreamError,
        onRetryStream = { retryTrigger++ },
        onNavigateToPortfolio = onNavigateToPortfolio,
        onNavigateToSearch = onNavigateToSearch,
        onNavigateToBookingHub = onNavigateToBookingHub,
        onNavigateToMyBookings = onNavigateToMyBookings,
        onNavigateToAdmissionForm = onNavigateToAdmissionForm,
        onNavigateToMyAdmissions = onNavigateToMyAdmissions,
        onAdmissionClick = onAdmissionClick,
        onLogoutClick = { showLogoutDialog = true },
        onChangePasswordClick = { showChangePasswordDialog = true },
        isEmailVerified = isEmailVerified,
        onNavigateToEmailVerification = onNavigateToEmailVerification
    )

    if (showChangePasswordDialog) {
        val oldPassVal = AuthValidators.validatePassword(oldPassword)
        val newPassVal = AuthValidators.validatePassword(newPassword)
        val confirmPassVal = AuthValidators.validateConfirmPassword(newPassword, confirmNewPassword)

        AlertDialog(
            onDismissRequest = {
                if (!changePasswordLoading) {
                    showChangePasswordDialog = false
                    changePasswordError = null
                }
            },
            title = {
                Text(
                    text = "Change Password",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter your current password and choose a new secure password.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    if (changePasswordError != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = changePasswordError.orEmpty(),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = {
                            oldPassword = it
                            changePasswordError = null
                        },
                        label = { Text("Current Password") },
                        visualTransformation = if (oldPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { oldPasswordVisible = !oldPasswordVisible }) {
                                Icon(
                                    imageVector = if (oldPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !changePasswordLoading
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = {
                            newPassword = it
                            changePasswordError = null
                        },
                        label = { Text("New Password (min 6 chars)") },
                        visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                Icon(
                                    imageVector = if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !changePasswordLoading
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = confirmNewPassword,
                        onValueChange = {
                            confirmNewPassword = it
                            changePasswordError = null
                        },
                        label = { Text("Confirm New Password") },
                        visualTransformation = if (confirmNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { confirmNewPasswordVisible = !confirmNewPasswordVisible }) {
                                Icon(
                                    imageVector = if (confirmNewPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !changePasswordLoading
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!oldPassVal.isValid) {
                            changePasswordError = "Please enter your current password"
                            return@Button
                        }
                        if (!newPassVal.isValid) {
                            changePasswordError = newPassVal.errorMessage ?: "New password must be at least 6 characters"
                            return@Button
                        }
                        if (!confirmPassVal.isValid) {
                            changePasswordError = confirmPassVal.errorMessage ?: "Passwords do not match"
                            return@Button
                        }

                        changePasswordLoading = true
                        changePasswordError = null
                        coroutineScope.launch {
                            val res = authRepository.changePassword(oldPassword, newPassword)
                            changePasswordLoading = false
                            res.fold(
                                onSuccess = {
                                    Toast.makeText(context, "Password updated successfully!", Toast.LENGTH_SHORT).show()
                                    showChangePasswordDialog = false
                                    oldPassword = ""
                                    newPassword = ""
                                    confirmNewPassword = ""
                                },
                                onFailure = { err ->
                                    changePasswordError = err.message ?: "Failed to update password."
                                }
                            )
                        }
                    },
                    enabled = !changePasswordLoading
                ) {
                    if (changePasswordLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Update Password")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showChangePasswordDialog = false
                        changePasswordError = null
                    },
                    enabled = !changePasswordLoading
                ) {
                    Text("Cancel")
                }
            }
        )
    }

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
    onChangePasswordClick: () -> Unit = {},
    isEmailVerified: Boolean = true,
    onNavigateToEmailVerification: (() -> Unit)? = null,
    upcomingBookings: List<Booking> = emptyList(),
    onAdmissionClick: (String) -> Unit = {},
    hospitalLookup: (String) -> Hospital? = { HospitalRepository.getHospital(it) },
    doctorLookup: (String) -> Doctor? = { HospitalRepository.getDoctor(it) },
    modifier: Modifier = Modifier
) {
    val admissionsSubtitle = when {
        activeCount > 0 -> "$activeCount active admission request${if (activeCount > 1) "s" else ""} • Tap to view tracking"
        else -> "Inpatient status, room allotment & discharge summaries"
    }

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Personalized Welcome Header
            item {
                PatientWelcomeHeader(
                    userName = userName,
                    greetingSubtitle = "Your clinical care dashboard & health records"
                )
            }

            // 2. Health-Focused Hero Card
            item {
                PatientHeroCard(
                    title = "Complete Medical Portfolio",
                    description = "Consolidate your prescriptions, emergency profile, verified records & reports for direct care sharing.",
                    badgeText = "Clinical Record Hub",
                    actionText = "Open Portfolio",
                    onActionClick = onNavigateToPortfolio
                )
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

            // 3. Interactive Active Inpatient Status Card (if admission exists)
            if (activeAdmission != null) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Active Inpatient Admission",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        ActiveAdmissionCard(
                            admission = activeAdmission,
                            hospitalLookup = hospitalLookup,
                            onClick = { onAdmissionClick(activeAdmission.id) }
                        )
                    }
                }
            }

            // 4. Upcoming Appointments Section (if any)
            if (upcomingBookings.isNotEmpty()) {
                item {
                    PatientSectionHeader(
                        title = "Upcoming Appointments",
                        subtitle = "Scheduled visits & diagnostic appointments",
                        actionText = "View All",
                        onActionClick = onNavigateToMyBookings
                    )
                }

                items(upcomingBookings.take(2).size) { index ->
                    val booking = upcomingBookings[index]
                    val targetName = remember(booking) {
                        if (booking.type == BookingType.OPD_APPOINTMENT && !booking.doctorId.isNullOrBlank()) {
                            doctorLookup(booking.doctorId)?.name ?: "Consulting Specialist"
                        } else {
                            "Diagnostic Lab Test"
                        }
                    }
                    val hospName = remember(booking) {
                        hospitalLookup(booking.hospitalId)?.name ?: "Partner Hospital"
                    }

                    UpcomingBookingCard(
                        booking = booking,
                        targetName = targetName,
                        hospitalName = hospName,
                        onClick = onNavigateToMyBookings
                    )
                }
            }

            // 5. Quick Action Grid
            item {
                PatientSectionHeader(
                    title = "Quick Health Actions",
                    subtitle = "Instant access to key healthcare services"
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PatientQuickActionCard(
                            title = "Book Doctor",
                            subtitle = "OPD & video consults",
                            icon = Icons.Default.CalendarMonth,
                            onClick = onNavigateToBookingHub,
                            modifier = Modifier.weight(1f)
                        )
                        PatientQuickActionCard(
                            title = "Book Lab Test",
                            subtitle = "Scans & pathology tests",
                            icon = Icons.Default.Science,
                            onClick = onNavigateToBookingHub,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PatientQuickActionCard(
                            title = "Find Doctors",
                            subtitle = "Specialists & ratings",
                            icon = Icons.Default.Search,
                            onClick = onNavigateToSearch,
                            modifier = Modifier.weight(1f)
                        )
                        PatientQuickActionCard(
                            title = "Medical Portfolio",
                            subtitle = "Summary & reports",
                            icon = Icons.Default.FolderShared,
                            onClick = onNavigateToPortfolio,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 6. Hospital Admission & Inpatient Services
            item {
                PatientSectionHeader(
                    title = "Hospital & Admission Hub",
                    subtitle = "Request inpatient care or review active status"
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PatientActionTile(
                        title = "Hospital Admission Request",
                        subtitle = "Direct admission request, bed allotment & triage",
                        icon = Icons.Default.Hotel,
                        onClick = onNavigateToAdmissionForm
                    )

                    PatientActionTile(
                        title = "My Admissions",
                        subtitle = admissionsSubtitle,
                        icon = Icons.Default.LocalHospital,
                        trailingBadge = if (activeCount > 0) "$activeCount Active" else null,
                        onClick = onNavigateToMyAdmissions
                    )

                    PatientActionTile(
                        title = "Booking History & Receipts",
                        subtitle = "Track active tokens, appointment slots & past visits",
                        icon = Icons.Default.CheckCircleOutline,
                        onClick = onNavigateToMyBookings
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
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
