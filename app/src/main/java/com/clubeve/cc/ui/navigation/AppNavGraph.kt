package com.clubeve.cc.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.clubeve.cc.ui.attendance.AttendeeListScreen
import com.clubeve.cc.ui.events.EventDetailScreen
import com.clubeve.cc.ui.events.HomeScreen
import com.clubeve.cc.ui.events.HomeViewModel
import com.clubeve.cc.ui.login.LoginScreen
import com.clubeve.cc.ui.scanner.ScannerScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Login.route
) {
    // Share HomeViewModel across Home and EventDetail so event list is loaded once
    val homeViewModel: HomeViewModel = viewModel()

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onEventClick = { eventId ->
                    navController.navigate(Screen.EventDetail.createRoute(eventId))
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                vm = homeViewModel
            )
        }

        composable(
            route = Screen.EventDetail.route,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStack ->
            val eventId = backStack.arguments?.getString("eventId")!!
            EventDetailScreen(
                eventId = eventId,
                onBack = { navController.popBackStack() },
                onScanQR = { navController.navigate(Screen.Scanner.createRoute(eventId)) },
                onViewAttendees = { navController.navigate(Screen.AttendeeList.createRoute(eventId)) },
                vm = homeViewModel
            )
        }

        composable(
            route = Screen.Scanner.route,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStack ->
            val eventId = backStack.arguments?.getString("eventId")!!
            ScannerScreen(
                eventId = eventId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AttendeeList.route,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStack ->
            val eventId = backStack.arguments?.getString("eventId")!!
            // Pass event title from cached events if available
            val eventTitle = homeViewModel.state.value.events
                .find { it.id == eventId }?.title ?: ""
            AttendeeListScreen(
                eventId = eventId,
                eventTitle = eventTitle,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
