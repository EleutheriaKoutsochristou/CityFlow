package com.example.cityflow.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cityflow.data.model.User
import com.example.cityflow.data.model.UserRole
import com.example.cityflow.ui.admin.AdminDashboardScreen
import com.example.cityflow.ui.auth.AuthViewModel
import com.example.cityflow.ui.auth.LoginScreen
import com.example.cityflow.ui.auth.RegisterScreen
import com.example.cityflow.ui.citizen.CitizenHomeScreen
import com.example.cityflow.ui.citizen.SubmitReportScreen
import com.example.cityflow.ui.employee.EmployeeDashboardScreen
import com.example.cityflow.ui.profile.ProfileScreen
import com.example.cityflow.ui.stats.StatsScreen

/**
 * Root navigation graph host for the CityFlow application.
 *
 * Configures the Compose Navigation [NavHost], defines destination routes,
 * manages back-stack transitions, and routes users to appropriate landing screens
 * depending on their authorization role (Admin, Employee, Citizen).
 *
 * @param navController The Navigation controller managing destination transitions.
 * @param authViewModel Shared ViewModel managing authentication state and current user info.
 */
@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()

    /**
     * Redirects the user to their corresponding home dashboard based on role:
     * - Admins -> Admin Dashboard
     * - Municipality Employees -> Employee Dashboard
     * - Citizens -> Citizen Home Feed & Map
     * Clears the Login screen from the back-stack so pressing 'Back' exits or behaves correctly.
     */
    fun navigateForUser(user: User) {
        when {
            user.isAdmin -> {
                navController.navigate(Screen.AdminDashboard.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            }
            user.userRole == UserRole.EMPLOYEE -> {
                navController.navigate(Screen.EmployeeDashboard.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            }
            else -> {
                navController.navigate(Screen.CitizenHome.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        // --- Login Screen ---
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = {
                    authViewModel.resetState()
                    navController.navigate(Screen.Register.route)
                },
                onLoginSuccess = { user ->
                    navigateForUser(user)
                }
            )
        }

        // --- Registration Screen ---
        composable(Screen.Register.route) {
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateBack = {
                    authViewModel.resetState()
                    navController.popBackStack()
                },
                onRegisterSuccess = { user ->
                    navigateForUser(user)
                }
            )
        }

        // --- Citizen Main Feed & Map ---
        composable(Screen.CitizenHome.route) {
            CitizenHomeScreen(
                currentUser = currentUser,
                onNavigateToNewReport = {
                    navController.navigate(Screen.SubmitReport.route)
                },
                onNavigateToStats = {
                    navController.navigate(Screen.Stats.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // --- Report Submission Screen ---
        composable(Screen.SubmitReport.route) {
            SubmitReportScreen(
                currentUser = currentUser,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSubmitSuccess = {
                    navController.popBackStack()
                }
            )
        }

        // --- Municipality Employee Dashboard ---
        composable(Screen.EmployeeDashboard.route) {
            EmployeeDashboardScreen(
                currentUser = currentUser,
                onNavigateToAdmin = {
                    navController.navigate(Screen.AdminDashboard.route)
                },
                onNavigateToStats = {
                    navController.navigate(Screen.Stats.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // --- Administrator Configuration Dashboard ---
        composable(Screen.AdminDashboard.route) {
            AdminDashboardScreen(
                currentUser = currentUser,
                onNavigateToEmployeeDashboard = {
                    navController.navigate(Screen.EmployeeDashboard.route)
                },
                onNavigateToStats = {
                    navController.navigate(Screen.Stats.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // --- Analytics & Statistics Screen ---
        composable(Screen.Stats.route) {
            StatsScreen(
                currentUser = currentUser,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // --- User Profile & Account Settings Screen ---
        composable(Screen.Profile.route) {
            ProfileScreen(
                currentUser = currentUser,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToStats = {
                    navController.navigate(Screen.Stats.route)
                },
                onUpdateProfile = { newName ->
                    authViewModel.updateProfile(newName)
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
