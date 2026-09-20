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
import com.nmims.careconnect.ui.auth.LoginScreen
import com.nmims.careconnect.ui.auth.RoleSelectionScreen
import com.nmims.careconnect.ui.portfolio.EditProfileScreen
import com.nmims.careconnect.ui.portfolio.PortfolioScreen

@Composable
fun CareConnectApp(
    navController: AppNavController = rememberAppNavController()
) {
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
            PlaceholderRouteScreen(
                title = "Patient Home",
                onBack = { navController.back() }
            )
        }
        is Screen.DoctorHome -> {
            PlaceholderRouteScreen(
                title = "Doctor Home",
                onBack = { navController.back() }
            )
        }
        is Screen.AdminHome -> {
            PlaceholderRouteScreen(
                title = "Admin Home",
                onBack = { navController.back() }
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
            PlaceholderRouteScreen(
                title = "Medical Records",
                onBack = { navController.back() }
            )
        }
        is Screen.AddEditRecord -> {
            PlaceholderRouteScreen(
                title = "Add/Edit Record",
                onBack = { navController.back() }
            )
        }
        is Screen.Documents -> {
            PlaceholderRouteScreen(
                title = "Documents",
                onBack = { navController.back() }
            )
        }
        is Screen.AddDocument -> {
            PlaceholderRouteScreen(
                title = "Add Document",
                onBack = { navController.back() }
            )
        }
        is Screen.DocumentViewer -> {
            PlaceholderRouteScreen(
                title = "Document Viewer",
                onBack = { navController.back() }
            )
        }
        is Screen.About -> {
            PlaceholderRouteScreen(
                title = "About",
                onBack = { navController.back() }
            )
        }
    }
}

@Composable
private fun PlaceholderRouteScreen(
    title: String,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("$title Screen Placeholder")
    }
}
