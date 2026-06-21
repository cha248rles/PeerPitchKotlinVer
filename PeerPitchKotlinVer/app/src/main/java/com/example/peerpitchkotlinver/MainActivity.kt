/*
 * What: App entry point. Defines the navigation routes, the MainActivity that hosts the
 *       Compose UI, and the PeerPitchApp NavHost that wires together all app screens.
 * Who:  Charles O'Connell and Anish Machiraju
 * When: 2026-06-21
 */
package com.example.peerpitchkotlinver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.peerpitchkotlinver.ui.screens.ActiveVideoFeedScreen
import com.example.peerpitchkotlinver.ui.screens.LoginScreen
import com.example.peerpitchkotlinver.ui.screens.PitchResult
import com.example.peerpitchkotlinver.ui.screens.ResultsScreen
import com.example.peerpitchkotlinver.ui.screens.SignUpScreen
import com.example.peerpitchkotlinver.ui.screens.StartVideoFeedScreen
import com.example.peerpitchkotlinver.ui.screens.WelcomeScreen
import com.example.peerpitchkotlinver.ui.screens.sampleResult
import com.example.peerpitchkotlinver.ui.theme.PeerPitchKotlinVerTheme

/** Central definition of the navigation route names used by the [PeerPitchApp] NavHost. */
object Routes {
    const val WELCOME = "welcome"
    const val SIGN_UP = "signup"
    const val LOGIN = "login"
    const val START_FEED = "start_feed"
    const val ACTIVE_FEED = "active_feed"
    const val RESULTS = "results"
}

/**
 * Single-activity entry point for the app. Enables edge-to-edge rendering and sets the
 * Compose content to the themed [PeerPitchApp] root composable.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PeerPitchKotlinVerTheme {
                PeerPitchApp()
            }
        }
    }
}

/**
 * Root composable hosting the app's navigation graph. Sets up the NavHost, holds the latest
 * pitch result in state, and defines navigation between Welcome, Sign Up, Login, Start Feed,
 * Active Feed, and Results screens.
 */
@Composable
fun PeerPitchApp() {
    val navController = rememberNavController()
    // Holds the most recent pitch result so Results can render it after Active Feed computes it.
    var pitchResult by remember { mutableStateOf<PitchResult?>(null) }
    // App opens on Welcome and goes through login first. After that, the
    // "Start Video Feed" screen acts as the home hub the home buttons return to.
    val goHome: () -> Unit = {
        navController.navigate(Routes.START_FEED) {
            popUpTo(Routes.START_FEED) { inclusive = false }
            launchSingleTop = true
        }
    }
    NavHost(navController = navController, startDestination = Routes.WELCOME) {
        composable(Routes.WELCOME) {
            WelcomeScreen(onGetStarted = { navController.navigate(Routes.SIGN_UP) })
        }
        composable(Routes.SIGN_UP) {
            SignUpScreen(
                onBack = { navController.popBackStack() },
                onLoginInstead = { navController.navigate(Routes.LOGIN) },
                onNext = { navController.navigate(Routes.START_FEED) }
            )
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate(Routes.START_FEED) }
            )
        }
        composable(Routes.START_FEED) {
            StartVideoFeedScreen(onStartRecording = { navController.navigate(Routes.ACTIVE_FEED) })
        }
        composable(Routes.ACTIVE_FEED) {
            ActiveVideoFeedScreen(
                onEnd = { result ->
                    pitchResult = result
                    navController.navigate(Routes.RESULTS)
                },
                onHome = goHome
            )
        }
        composable(Routes.RESULTS) {
            ResultsScreen(
                result = pitchResult ?: sampleResult,
                onBack = goHome,
                onDone = goHome
            )
        }
    }
}

/** IDE design-time preview of the app's navigation root, [PeerPitchApp]. */
@Preview
@Composable
private fun PeerPitchAppPreview() {
    PeerPitchApp()
}
