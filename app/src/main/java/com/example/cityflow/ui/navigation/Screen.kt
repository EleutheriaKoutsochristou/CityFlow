package com.example.cityflow.ui.navigation

/**
 * Sealed hierarchy defining unique navigation destinations and their route strings
 * for the Jetpack Compose Navigation framework in CityFlow.
 */
sealed class Screen(val route: String) {
    /** Authentication login destination */
    object Login : Screen("login")
    /** User registration destination */
    object Register : Screen("register")
    /** Main citizen feed and map overview */
    object CitizenHome : Screen("citizen_home")
    /** Report submission form for citizens */
    object SubmitReport : Screen("submit_report")
    /** Citizen's submitted reports history */
    object MyReports : Screen("my_reports")
    /** Municipality employee management dashboard */
    object EmployeeDashboard : Screen("employee_dashboard")
    /** System administrator ranking & parameters configuration screen */
    object AdminDashboard : Screen("admin_dashboard")
    /** City-wide incident analytics and statistics charts */
    object Stats : Screen("stats")
    /** User account profile management and password update */
    object Profile : Screen("profile")
}
