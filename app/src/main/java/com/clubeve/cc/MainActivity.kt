package com.clubeve.cc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.clubeve.cc.data.remote.SupabaseClientProvider
import com.clubeve.cc.ui.navigation.AppNavGraph
import com.clubeve.cc.ui.navigation.Screen
import com.clubeve.cc.ui.theme.BackgroundPrimary
import com.clubeve.cc.ui.theme.ClubEveTheme
import io.github.jan.supabase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val startDest = if (SupabaseClientProvider.client.auth.currentUserOrNull() != null)
            Screen.Events.route else Screen.Login.route

        setContent {
            ClubEveTheme {
                Surface(Modifier.fillMaxSize(), color = BackgroundPrimary) {
                    val navController = rememberNavController()
                    AppNavGraph(navController = navController, startDestination = startDest)
                }
            }
        }
    }
}
