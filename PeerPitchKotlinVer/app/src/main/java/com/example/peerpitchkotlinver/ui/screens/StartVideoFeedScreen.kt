/*
 * What: Pre-recording screen — prompts the user to start recording their pitch video feed.
 * Who:  Charles O'Connell and Anish Machiraju
 * When: 2026-06-21
 */
package com.example.peerpitchkotlinver.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.peerpitchkotlinver.ui.components.OutlinedPillButton
import com.example.peerpitchkotlinver.ui.components.PeerPitchLogo
import com.example.peerpitchkotlinver.ui.theme.PitchBlue
import com.example.peerpitchkotlinver.ui.theme.PitchGold

/**
 * Renders the start-recording screen: a gold-backed column with a "Start Video Feed" heading,
 * a faded PeerPitch logo, and a transparent pill button that begins recording.
 */
@Composable
fun StartVideoFeedScreen(onStartRecording: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchGold)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text("Start Video Feed", color = Color.White, fontSize = 15.sp, fontFamily = FontFamily.Serif)
        Spacer(modifier = Modifier.weight(0.7f))
        PeerPitchLogo(modifier = Modifier.alpha(0.55f))
        Spacer(modifier = Modifier.weight(0.5f))
        OutlinedPillButton(
            text = "Click Here To Start\nRecording",
            onClick = onStartRecording,
            containerColor = Color.Transparent,
            contentColor = PitchBlue
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

/** Design-time preview of the start-recording screen with a no-op start callback. */
@Preview(showBackground = true)
@Composable
fun StartVideoFeedScreenPreview() {
    StartVideoFeedScreen(onStartRecording = {})
}
