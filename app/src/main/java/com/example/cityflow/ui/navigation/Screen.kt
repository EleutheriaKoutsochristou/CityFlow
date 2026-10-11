package com.example.cityflow.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object CitizenHome : Screen("citizen_home")
    object SubmitReport : Screen("submit_report")
    object MyReports : Screen("my_reports")
    object EmployeeDashboard : Screen("employee_dashboard")
    object AdminDashboard : Screen("admin_dashboard")
    object Stats : Screen("stats")
    object Profile : Screen("profile")
}
