package com.clubeve.cc.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.clubeve.cc.ui.attendance.AttendanceListScreen
import com.clubeve.cc.ui.events.EventListScreen
import com.clubeve.cc.ui.login.LoginScreen
import com.clubeve.cc.ui.result.ScanResultScreen
import com.clubeve.cc.ui.scanner.ScannerScreen

@Composable
fun AppNavGraph(navController: NavHostController, startDestination: String = Screen.Login.route) {
    NavHost(navController = navController, startDestination = startDestination) {

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Events.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Events.route) {
            EventListScreen(
                onEventSelected = { eventId ->
                    navController.navigate(Screen.Scanner.createRoute(eventId))
                },
                onViewAttendance = { eventId ->
                    navController.navigate(Screen.AttendanceList.createRoute(eventId))
                }
            )
        }

        composable(
            Screen.Scanner.route,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStack ->
            val eventId = backStack.arguments?.getString("eventId")!!
            ScannerScreen(
                eventId = eventId,
                onScanned = { usn ->
                    navController.navigate(Screen.ScanResult.createRoute(eventId, usn))
                },
                onViewList = {
                    navController.navigate(Screen.AttendanceList.createRoute(eventId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Screen.ScanResult.route,
            arguments = listOf(
                navArgument("eventId") { type = NavType.StringType },
                navArgument("usn") { type = NavType.StringType }
            )
        ) { backStack ->
            val eventId = backStack.arguments?.getString("eventId")!!
            val usn = backStack.arguments?.getString("usn")!!
            ScanResultScreen(
                eventId = eventId,
                usn = usn,
                onRescan = {
                    navController.navigate(Screen.Scanner.createRoute(eventId)) {
                        popUpTo(Screen.Scanner.createRoute(eventId)) { inclusive = true }
                    }
                },
                onViewList = {
                    navController.navigate(Screen.AttendanceList.createRoute(eventId)) {
                        popUpTo(Screen.Scanner.createRoute(eventId))
                    }
                }
            )
        }

        composable(
            Screen.AttendanceList.route,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStack ->
            val eventId = backStack.arguments?.getString("eventId")!!
            AttendanceListScreen(
                eventId = eventId,
                onScan = {
                    navController.navigate(Screen.Scanner.createRoute(eventId)) {
                        popUpTo(Screen.AttendanceList.createRoute(eventId)) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
