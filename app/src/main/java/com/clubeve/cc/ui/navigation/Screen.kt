package com.clubeve.cc.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Events : Screen("events")
    object Scanner : Screen("scanner/{eventId}") {
        fun createRoute(eventId: String) = "scanner/$eventId"
    }
    object ScanResult : Screen("scan_result/{eventId}/{usn}") {
        fun createRoute(eventId: String, usn: String) = "scan_result/$eventId/$usn"
    }
    object AttendanceList : Screen("attendance/{eventId}") {
        fun createRoute(eventId: String) = "attendance/$eventId"
    }
}
