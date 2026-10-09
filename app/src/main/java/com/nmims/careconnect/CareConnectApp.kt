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

@Composable
fun CareConnectApp(
    navController: AppNavController = rememberAppNavController()
) {
    val context = LocalContext.current
    var isAppReady by remember { mutableStateOf(HospitalRepository.isLoaded) }

    LaunchedEffect(Unit) {
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
                    text = "Loading hospitals...",
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
                }
            )
        }
        is Screen.Login -> {
            LoginScreen(
                role = current.role,
                onLoginSuccess = {
                    when (current.role) {
                        UserRole.PATIENT -> navController.resetTo(Screen.PatientHome)
                        UserRole.DOCTOR -> navController.resetTo(Screen.DoctorHome)
                        UserRole.HOSPITAL_ADMIN -> navController.resetTo(Screen.AdminHome)
                    }
                },
                onBack = { navController.back() }
            )
        }
        is Screen.PatientHome -> {
            PatientHomeScreen(
                onNavigateToPortfolio = { navController.navigate(Screen.Portfolio) },
                onNavigateToSearch = { navController.navigate(Screen.Search) },
                onLogout = { navController.resetTo(Screen.RoleSelection) }
            )
        }
        is Screen.DoctorHome -> {
            DoctorHomeScreen(
                onLogout = { navController.resetTo(Screen.RoleSelection) }
            )
        }
        is Screen.AdminHome -> {
            AdminHomeScreen(
                onLogout = { navController.resetTo(Screen.RoleSelection) }
            )
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
                onDoctorClick = { doctorId -> navController.navigate(Screen.DoctorDetail(doctorId)) }
            )
        }
        is Screen.DoctorDetail -> {
            DoctorDetailScreen(
                doctorId = current.doctorId,
                onNavigateBack = { navController.back() },
                onHospitalClick = { hospitalId -> navController.navigate(Screen.HospitalDetail(hospitalId)) }
            )
        }
        is Screen.BookAppointment -> {
            BookAppointmentScreen(
                doctorId = current.doctorId,
                onNavigateBack = { navController.back() }
            )
        }
        is Screen.Compare -> {
            CompareScreen(
                doctorIds = current.doctorIds,
                onNavigateBack = { navController.back() }
            )
        }
    }
}
