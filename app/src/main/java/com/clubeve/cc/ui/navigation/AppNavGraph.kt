package com.clubeve.cc.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.clubeve.cc.ui.theme.GlassState
import com.clubeve.cc.ui.theme.Mono
import com.clubeve.cc.utils.NetworkMonitor

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Login.route
) {
    val context = LocalContext.current
    val isOnline by produceState(initialValue = true) {
        NetworkMonitor(context).isOnlineFlow.collect { value = it }
    }
    val isGlass = GlassState.isGlass

    // Share HomeViewModel across Home and EventDetail so event list is loaded once
    val homeViewModel: HomeViewModel = viewModel()

    Column {
        // Global offline banner shown on every screen (except login)
        if (!isOnline && navController.currentDestination?.route != Screen.Login.route) {
            Surface(color = Color(0xFFFF9500)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Offline — check-ins will sync when online",
                        color = Color.White,
                        fontFamily = Mono,
                        fontSize = 11.sp
                    )
                }
            }
        }

        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = if (isGlass) Modifier.background(Color.Transparent) else Modifier
        ) {

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
}
