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
import com.example.cityflow.ui.stats.StatsScreen

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()

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

        composable(Screen.CitizenHome.route) {
            CitizenHomeScreen(
                currentUser = currentUser,
                onNavigateToNewReport = {
                    navController.navigate(Screen.SubmitReport.route)
                },
                onNavigateToStats = {
                    navController.navigate(Screen.Stats.route)
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

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

        composable(Screen.EmployeeDashboard.route) {
            EmployeeDashboardScreen(
                currentUser = currentUser,
                onNavigateToAdmin = {
                    navController.navigate(Screen.AdminDashboard.route)
                },
                onNavigateToStats = {
                    navController.navigate(Screen.Stats.route)
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.AdminDashboard.route) {
            AdminDashboardScreen(
                currentUser = currentUser,
                onNavigateToEmployeeDashboard = {
                    navController.navigate(Screen.EmployeeDashboard.route)
                },
                onNavigateToStats = {
                    navController.navigate(Screen.Stats.route)
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Stats.route) {
            StatsScreen(
                currentUser = currentUser,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
