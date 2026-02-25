package com.tensorlabs.amaanat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tensorlabs.amaanat.ui.theme.AmaanatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Status bar aur Navigation bar transparent (Pixel look)
        enableEdgeToEdge()

        setContent {
            AmaanatTheme {
                // Surface container using background color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // Navigation Host
                    NavHost(
                        navController = navController,
                        startDestination = "splash_screen"
                    ) {
                        // Screen 1: Splash
                        composable("splash_screen") {
                            // Ensure AmaanatSplashScreen is defined in SplashScreen.kt
                            AmaanatSplashScreen(navController = navController)
                        }

                        // Screen 2: Dashboard (Main App)
                        composable("dashboard_screen") {
                            // Ensure DashboardScreen is defined in DashboardScreen.kt
                            DashboardScreen(navController = navController)
                        }
                    }
                }
            }
        }
    }
}