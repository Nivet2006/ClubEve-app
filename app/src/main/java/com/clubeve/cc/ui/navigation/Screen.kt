package com.clubeve.cc.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object EventDetail : Screen("event_detail/{eventId}") {
        fun createRoute(eventId: String) = "event_detail/$eventId"
    }
    object Scanner : Screen("scanner/{eventId}") {
        fun createRoute(eventId: String) = "scanner/$eventId"
    }
    object AttendeeList : Screen("attendee_list/{eventId}") {
        fun createRoute(eventId: String) = "attendee_list/$eventId"
    }
    // ── Student flow ──────────────────────────────────────────────────────────
    object StudentHome : Screen("student_home")
    object StudentQr : Screen("student_qr/{registrationId}") {
        fun createRoute(registrationId: String) = "student_qr/$registrationId"
    }
    // ── CC (Club Coordinator) flow ────────────────────────────────────────────
    object CcDashboard : Screen("cc_dashboard")
    object CcEventDetail : Screen("cc_event_detail/{eventId}") {
        fun createRoute(eventId: String) = "cc_event_detail/$eventId"
    }
    object CcReport : Screen("cc_report/{eventId}") {
        fun createRoute(eventId: String) = "cc_report/$eventId"
    }
    object CcLiveView : Screen("cc_live_view/{eventId}") {
        fun createRoute(eventId: String) = "cc_live_view/$eventId"
    }
}
