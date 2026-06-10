package com.example.peerpitchkotlinver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.peerpitchkotlinver.ui.screens.ActiveVideoFeedScreen
import com.example.peerpitchkotlinver.ui.screens.LoginScreen
import com.example.peerpitchkotlinver.ui.screens.SignUpScreen
import com.example.peerpitchkotlinver.ui.screens.StartVideoFeedScreen
import com.example.peerpitchkotlinver.ui.screens.WelcomeScreen
import com.example.peerpitchkotlinver.ui.theme.PeerPitchKotlinVerTheme

object Routes {
    const val WELCOME = "welcome"
    const val SIGN_UP = "signup"
    const val LOGIN = "login"
    const val START_FEED = "start_feed"
    const val ACTIVE_FEED = "active_feed"
}

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

@Composable
fun PeerPitchApp() {
    val navController = rememberNavController()
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
            ActiveVideoFeedScreen()
        }
    }
}
