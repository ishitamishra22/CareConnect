package com.nmims.careconnect

/*
 * CareConnectApp:
 * Root composable managing the application navigation back stack via AppNavController.
 * Handles system back presses via BackHandler and dispatches to appropriate screens.
 */

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.nmims.careconnect.data.model.UserRole
import com.nmims.careconnect.navigation.AppNavController
import com.nmims.careconnect.navigation.Screen
import com.nmims.careconnect.navigation.rememberAppNavController
import com.nmims.careconnect.ui.about.AboutScreen
import com.nmims.careconnect.ui.admin.AdminHomeScreen
import com.nmims.careconnect.ui.auth.ApprovalPendingScreen
import com.nmims.careconnect.ui.auth.DoctorSignupScreen
import com.nmims.careconnect.ui.auth.HospitalStaffSignupScreen
import com.nmims.careconnect.ui.auth.LoginScreen
import com.nmims.careconnect.ui.auth.RoleSelectionScreen
import com.nmims.careconnect.ui.doctor.DoctorHomeScreen
import com.nmims.careconnect.ui.patient.PatientHomeScreen
import com.nmims.careconnect.ui.portfolio.AddDocumentScreen
import com.nmims.careconnect.ui.portfolio.AddEditRecordScreen
import com.nmims.careconnect.ui.portfolio.DocumentViewerScreen
import com.nmims.careconnect.ui.portfolio.DocumentsScreen
import com.nmims.careconnect.ui.portfolio.EditProfileScreen
import com.nmims.careconnect.ui.portfolio.MedicalRecordsScreen
import com.nmims.careconnect.ui.portfolio.PortfolioScreen
import com.nmims.careconnect.ui.search.CompareScreen
import com.nmims.careconnect.ui.search.DoctorDetailScreen
import com.nmims.careconnect.ui.search.HospitalDetailScreen
import com.nmims.careconnect.ui.search.SearchScreen

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nmims.careconnect.data.repository.HospitalRepository
import com.nmims.careconnect.ui.common.LoadingIndicator
import com.nmims.careconnect.ui.booking.BookAppointmentScreen
import com.nmims.careconnect.ui.booking.BookLabTestScreen

import com.nmims.careconnect.data.repository.AuthRepository
import com.nmims.careconnect.ui.auth.AuthGate

@Composable
fun CareConnectApp(
    navController: AppNavController = rememberAppNavController(),
    authRepository: AuthRepository = AuthRepository.getInstance()
) {
    val context = LocalContext.current
    var isAppReady by remember { mutableStateOf(HospitalRepository.isLoaded) }

    LaunchedEffect(Unit) {
        // Initialize session and hospital repository
        authRepository.initialize()
        if (!HospitalRepository.isLoaded) {
            val success = HospitalRepository.load()
            if (!success) {
                Toast.makeText(context, "Offline - showing built-in hospital data", Toast.LENGTH_SHORT).show()
            }
            isAppReady = true
        }
    }

    if (!isAppReady) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LoadingIndicator()
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Loading CareConnect...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    // Intercept system back button to manage in-app back stack
    BackHandler(enabled = true) {
        val handled = navController.back()
        if (!handled) {
            // Reached the bottom of the stack; system activity finish can happen naturally
        }
    }

    when (val current = navController.current) {
        is Screen.RoleSelection -> {
            RoleSelectionScreen(
                onRoleSelected = { role ->
                    navController.navigate(Screen.Login(role))
                },
                onAboutClick = {
                    navController.navigate(Screen.About)
                },
                onSystemAdminClick = {
                    navController.navigate(Screen.SystemAdminLogin)
                }
            )
        }
        is Screen.Login -> {
            LoginScreen(
                role = current.role,
                authRepository = authRepository,
                onLoginSuccess = {
                    when (current.role) {
                        UserRole.PATIENT -> navController.resetTo(Screen.PatientHome)
                        UserRole.DOCTOR -> navController.resetTo(Screen.DoctorHome)
                        UserRole.HOSPITAL -> navController.resetTo(Screen.AdminHome)
                        UserRole.ADMIN -> navController.resetTo(Screen.SystemAdminDashboard)
                    }
                },
                onNavigateToSignup = when (current.role) {
                    UserRole.DOCTOR -> { { navController.navigate(Screen.DoctorSignup) } }
                    UserRole.HOSPITAL -> { { navController.navigate(Screen.HospitalStaffSignup) } }
                    else -> null
                },
                onNavigateToSignUp = if (current.role == UserRole.PATIENT) {
                    { navController.navigate(Screen.PatientSignUp) }
                } else null,
                onBack = { navController.back() }
            )
        }
        is Screen.PatientSignUp -> {
            com.nmims.careconnect.ui.auth.PatientSignUpScreen(
                onSignUpSuccess = {
                    navController.resetTo(Screen.PatientHome)
                    navController.navigate(Screen.EmailVerification)
                },
                onNavigateToLogin = {
                    navController.resetTo(Screen.Login(UserRole.PATIENT))
                },
                onBack = { navController.back() },
                authRepository = authRepository
            )
        }
        is Screen.DoctorSignup -> {
            DoctorSignupScreen(
                onSignupSubmitted = { email ->
                    navController.resetTo(Screen.RoleSelection)
                    navController.navigate(
                        Screen.ApprovalPending(email = email, role = UserRole.DOCTOR)
                    )
                },
                onNavigateToLogin = { navController.back() },
                onBack = { navController.back() },
                authRepository = authRepository
            )
        }
        is Screen.HospitalStaffSignup -> {
            HospitalStaffSignupScreen(
                onSignupSubmitted = { email ->
                    navController.resetTo(Screen.RoleSelection)
                    navController.navigate(
                        Screen.ApprovalPending(email = email, role = UserRole.HOSPITAL)
                    )
                },
                onNavigateToLogin = { navController.back() },
                onBack = { navController.back() },
                authRepository = authRepository
            )
        }
        is Screen.ApprovalPending -> {
            ApprovalPendingScreen(
                title = if (current.role == UserRole.DOCTOR)
                    "Doctor Verification Pending"
                else "Hospital Staff Verification Pending",
                message = if (current.role == UserRole.DOCTOR) {
                    "Your doctor application has been submitted and is currently pending verification of credentials and approval by a System Administrator."
                } else {
                    "Your hospital staff registration has been submitted and is pending verification of hospital association and approval by a System Administrator."
                },
                email = current.email,
                onBackToLogin = { navController.resetTo(Screen.RoleSelection) }
            )
        }
        is Screen.EmailVerification -> {
            AuthGate(
                authRepository = authRepository,
                requiredRoles = setOf(UserRole.PATIENT),
                onNavigateToLogin = { navController.resetTo(Screen.RoleSelection) }
            ) {
                com.nmims.careconnect.ui.auth.EmailVerificationScreen(
                    onVerificationSuccess = {
                        navController.resetTo(Screen.PatientHome)
                    },
                    onContinueToDashboard = {
                        navController.resetTo(Screen.PatientHome)
                    },
                    onSignOut = {
                        authRepository.signOut()
                        navController.resetTo(Screen.RoleSelection)
                    },
                    authRepository = authRepository
                )
            }
        }
        is Screen.PatientHome -> {
            AuthGate(
                authRepository = authRepository,
                requiredRoles = setOf(UserRole.PATIENT),
                onNavigateToLogin = { navController.resetTo(Screen.RoleSelection) }
            ) {
                PatientHomeScreen(
                    onNavigateToPortfolio = { navController.navigate(Screen.Portfolio) },
                    onNavigateToSearch = { navController.navigate(Screen.Search) },
                    onNavigateToBookingHub = { navController.navigate(Screen.BookingHub) },
                    onNavigateToMyBookings = { navController.navigate(Screen.MyBookings) },
                    onNavigateToAdmissionForm = { navController.navigate(Screen.AdmissionForm()) },
                    onNavigateToMyAdmissions = { navController.navigate(Screen.MyAdmissions) },
                    onAdmissionClick = { admissionId ->
                        navController.navigate(Screen.AdmissionDetail(admissionId))
                    },
                    onNavigateToEmailVerification = {
                        navController.navigate(Screen.EmailVerification)
                    },
                    onLogout = {
                        authRepository.signOut()
                        navController.resetTo(Screen.RoleSelection)
                    },
                    authRepository = authRepository
                )
            }
        }
        is Screen.BookingHub -> {
            com.nmims.careconnect.ui.booking.BookingHubScreen(
                onNavigateBack = { navController.back() },
                onNavigateToDoctorSearch = {
                    navController.navigate(Screen.Search(initialTab = 1))
                },
                onNavigateToLabBooking = {
                    navController.navigate(Screen.BookLabTest)
                },
                onNavigateToMyBookings = {
                    navController.navigate(Screen.MyBookings)
                }
            )
        }
        is Screen.DoctorHome -> {
            AuthGate(
                authRepository = authRepository,
                requiredRoles = setOf(UserRole.DOCTOR),
                onNavigateToLogin = { navController.resetTo(Screen.RoleSelection) }
            ) {
                DoctorHomeScreen(
                    onLogout = {
                        authRepository.signOut()
                        navController.resetTo(Screen.RoleSelection)
                    },
                    onAppointmentClick = { bookingId ->
                        navController.navigate(Screen.DoctorAppointmentDetail(bookingId))
                    }
                )
            }
        }
        is Screen.DoctorAppointmentDetail -> {
            com.nmims.careconnect.ui.doctor.DoctorAppointmentDetailScreen(
                bookingId = current.bookingId,
                onNavigateBack = { navController.back() }
            )
        }
        is Screen.AdminHome -> {
            AuthGate(
                authRepository = authRepository,
                requiredRoles = setOf(UserRole.HOSPITAL, UserRole.ADMIN),
                onNavigateToLogin = { navController.resetTo(Screen.RoleSelection) }
            ) {
                AdminHomeScreen(
                    onLogout = {
                        authRepository.signOut()
                        navController.resetTo(Screen.RoleSelection)
                    },
                    onAdmissionClick = { admissionId ->
                        navController.navigate(Screen.AdminAdmissionReview(admissionId))
                    }
                )
            }
        }
        is Screen.SystemAdminLogin -> {
            com.nmims.careconnect.ui.admin.SystemAdminLoginScreen(
                onLoginSuccess = { navController.resetTo(Screen.SystemAdminDashboard) },
                onBack = { navController.back() },
                authRepository = authRepository
            )
        }
        is Screen.SystemAdminDashboard -> {
            AuthGate(
                authRepository = authRepository,
                requiredRoles = setOf(UserRole.ADMIN),
                onNavigateToLogin = { navController.resetTo(Screen.RoleSelection) }
            ) {
                com.nmims.careconnect.ui.admin.SystemAdminDashboard(
                    onNavigateToUserManagement = { navController.navigate(Screen.UserManagement) },
                    onNavigateToProvisionStaff = { navController.navigate(Screen.ProvisionStaff) },
                    onSignOut = { navController.resetTo(Screen.RoleSelection) },
                    authRepository = authRepository
                )
            }
        }
        is Screen.UserManagement -> {
            AuthGate(
                authRepository = authRepository,
                requiredRoles = setOf(UserRole.ADMIN),
                onNavigateToLogin = { navController.resetTo(Screen.RoleSelection) }
            ) {
                com.nmims.careconnect.ui.admin.UserManagementScreen(
                    onNavigateBack = { navController.back() },
                    onUserClick = { uid -> navController.navigate(Screen.UserDetail(uid)) }
                )
            }
        }
        is Screen.UserDetail -> {
            AuthGate(
                authRepository = authRepository,
                requiredRoles = setOf(UserRole.ADMIN),
                onNavigateToLogin = { navController.resetTo(Screen.RoleSelection) }
            ) {
                com.nmims.careconnect.ui.admin.UserDetailScreen(
                    userId = current.userId,
                    onNavigateBack = { navController.back() }
                )
            }
        }
        is Screen.ProvisionStaff -> {
            AuthGate(
                authRepository = authRepository,
                requiredRoles = setOf(UserRole.ADMIN),
                onNavigateToLogin = { navController.resetTo(Screen.RoleSelection) }
            ) {
                com.nmims.careconnect.ui.admin.ProvisionStaffScreen(
                    onNavigateBack = { navController.back() }
                )
            }
        }
        is Screen.Portfolio -> {
            PortfolioScreen(
                onNavigateToEditProfile = { navController.navigate(Screen.EditProfile) },
                onNavigateToRecords = { navController.navigate(Screen.MedicalRecords) },
                onNavigateToDocuments = { navController.navigate(Screen.Documents) },
                onNavigateBack = { navController.back() }
            )
        }
        is Screen.EditProfile -> {
            EditProfileScreen(
                onNavigateBack = { navController.back() }
            )
        }
        is Screen.MedicalRecords -> {
            MedicalRecordsScreen(
                onNavigateToAddRecord = { navController.navigate(Screen.AddEditRecord(null)) },
                onNavigateToEditRecord = { id -> navController.navigate(Screen.AddEditRecord(id)) },
                onNavigateBack = { navController.back() }
            )
        }
        is Screen.AddEditRecord -> {
            AddEditRecordScreen(
                recordId = current.recordId,
                onNavigateBack = { navController.back() }
            )
        }
        is Screen.Documents -> {
            DocumentsScreen(
                onNavigateToAddDocument = { navController.navigate(Screen.AddDocument) },
                onNavigateToViewer = { id -> navController.navigate(Screen.DocumentViewer(id)) },
                onNavigateBack = { navController.back() }
            )
        }
        is Screen.AddDocument -> {
            AddDocumentScreen(
                onNavigateBack = { navController.back() }
            )
        }
        is Screen.DocumentViewer -> {
            DocumentViewerScreen(
                documentId = current.documentId,
                onNavigateBack = { navController.back() }
            )
        }
        is Screen.About -> {
            AboutScreen(
                onNavigateBack = { navController.back() }
            )
        }
        is Screen.Search -> {
            SearchScreen(
                initialTab = current.initialTab,
                onNavigateBack = { navController.back() },
                onDoctorClick = { doctorId -> navController.navigate(Screen.DoctorDetail(doctorId)) },
                onHospitalClick = { hospitalId -> navController.navigate(Screen.HospitalDetail(hospitalId)) },
                onCompareClick = { doctorIds -> navController.navigate(Screen.Compare(doctorIds)) }
            )
        }
        is Screen.HospitalDetail -> {
            HospitalDetailScreen(
                hospitalId = current.hospitalId,
                onNavigateBack = { navController.back() },
                onDoctorClick = { doctorId -> navController.navigate(Screen.DoctorDetail(doctorId)) },
                onRequestAdmission = { hospitalId -> navController.navigate(Screen.AdmissionForm(hospitalId)) }
            )
        }
        is Screen.DoctorDetail -> {
            DoctorDetailScreen(
                doctorId = current.doctorId,
                onNavigateBack = { navController.back() },
                onHospitalClick = { hospitalId -> navController.navigate(Screen.HospitalDetail(hospitalId)) },
                onBookClick = { doctorId -> navController.navigate(Screen.BookAppointment(doctorId)) }
            )
        }
        is Screen.BookAppointment -> {
            BookAppointmentScreen(
                doctorId = current.doctorId,
                onNavigateBack = { navController.back() },
                onCreateProfile = {
                    navController.navigate(Screen.EditProfile)
                },
                onBookingSuccess = { bookingId ->
                    navController.navigate(Screen.BookingSuccess(bookingId))
                }
            )
        }
        is Screen.BookLabTest -> {
            BookLabTestScreen(
                onNavigateBack = { navController.back() },
                onCreateProfile = {
                    navController.navigate(Screen.EditProfile)
                },
                onBookingSuccess = { bookingId ->
                    navController.navigate(Screen.BookingSuccess(bookingId))
                }
            )
        }
        is Screen.BookingSuccess -> {
            com.nmims.careconnect.ui.booking.BookingSuccessScreen(
                bookingId = current.bookingId,
                onNavigateToMyBookings = {
                    navController.navigate(Screen.MyBookings)
                },
                onNavigateToHome = {
                    navController.resetTo(Screen.PatientHome)
                }
            )
        }
        is Screen.MyBookings -> {
            com.nmims.careconnect.ui.booking.MyBookingsScreen(
                onNavigateBack = { navController.back() },
                onBookingClick = { bookingId ->
                    navController.navigate(Screen.BookingDetail(bookingId))
                }
            )
        }
        is Screen.BookingDetail -> {
            com.nmims.careconnect.ui.booking.BookingDetailScreen(
                bookingId = current.bookingId,
                onNavigateBack = { navController.back() }
            )
        }
        is Screen.Compare -> {
            CompareScreen(
                doctorIds = current.doctorIds,
                onNavigateBack = { navController.back() },
                onBookClick = { doctorId -> navController.navigate(Screen.BookAppointment(doctorId)) }
            )
        }
        is Screen.AdmissionForm -> {
            com.nmims.careconnect.ui.admission.AdmissionFormScreen(
                initialHospitalId = current.hospitalId,
                onNavigateBack = { navController.back() },
                onCreateProfile = { navController.navigate(Screen.EditProfile) },
                onAdmissionSuccess = { admissionId ->
                    navController.resetTo(Screen.PatientHome)
                    navController.navigate(Screen.AdmissionSuccess(admissionId))
                }
            )
        }
        is Screen.AdmissionSuccess -> {
            com.nmims.careconnect.ui.admission.AdmissionSuccessScreen(
                admissionId = current.admissionId,
                onViewAdmissionDetail = { admissionId ->
                    navController.resetTo(Screen.PatientHome)
                    navController.navigate(Screen.AdmissionDetail(admissionId))
                },
                onNavigateToMyAdmissions = {
                    navController.resetTo(Screen.PatientHome)
                    navController.navigate(Screen.MyAdmissions)
                },
                onNavigateToHome = {
                    navController.resetTo(Screen.PatientHome)
                }
            )
        }
        is Screen.MyAdmissions -> {
            com.nmims.careconnect.ui.admission.MyAdmissionsScreen(
                onNavigateBack = { navController.back() },
                onAdmissionClick = { admissionId ->
                    navController.navigate(Screen.AdmissionDetail(admissionId))
                },
                onRequestAdmission = {
                    navController.navigate(Screen.AdmissionForm())
                }
            )
        }
        is Screen.AdmissionDetail -> {
            com.nmims.careconnect.ui.admission.AdmissionDetailScreen(
                admissionId = current.admissionId,
                onNavigateBack = { navController.back() },
                onHospitalClick = { hospitalId ->
                    navController.navigate(Screen.HospitalDetail(hospitalId))
                }
            )
        }
        is Screen.AdminAdmissionReview -> {
            com.nmims.careconnect.ui.admin.AdminAdmissionReviewScreen(
                admissionId = current.admissionId,
                onNavigateBack = { navController.back() }
            )
        }
    }
}
