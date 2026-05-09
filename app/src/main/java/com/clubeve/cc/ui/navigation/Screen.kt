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
}
