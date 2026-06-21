/*
 * What: Welcome/landing screen — the app's first screen, showing branding and a "Get Started" call to action.
 * Who:  Charles O'Connell and Anish Machiraju
 * When: 2026-06-21
 */
package com.example.peerpitchkotlinver.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.peerpitchkotlinver.ui.components.OutlinedPillButton
import com.example.peerpitchkotlinver.ui.components.PeerPitchLogo
import com.example.peerpitchkotlinver.ui.components.PresentationIllustration
import com.example.peerpitchkotlinver.ui.theme.PitchGold

/**
 * Renders the welcome landing screen: a gold-backed column with a "Welcome" heading, the
 * PeerPitch logo, a presentation illustration, and a "Get Started" pill button.
 */
@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchGold)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Text("Welcome", color = Color.White, fontSize = 16.sp, fontFamily = FontFamily.Serif)
        Spacer(modifier = Modifier.height(56.dp))
        PeerPitchLogo()
        Spacer(modifier = Modifier.height(56.dp))
        PresentationIllustration(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        )
        Spacer(modifier = Modifier.weight(1f))
        OutlinedPillButton(text = "Get Started", onClick = onGetStarted)
        Spacer(modifier = Modifier.height(56.dp))
    }
}

/** Design-time preview of the welcome screen with a no-op get-started callback. */
@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    WelcomeScreen(onGetStarted = {})
}
