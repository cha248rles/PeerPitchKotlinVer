package com.example.peerpitchkotlinver.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.util.Log
import com.example.peerpitchkotlinver.camera.CameraPreview
import com.example.peerpitchkotlinver.session.SessionState
import com.example.peerpitchkotlinver.session.SessionStore
import com.example.peerpitchkotlinver.speech.SpeechController
import com.example.peerpitchkotlinver.ui.components.OutlinedHomeButton
import com.example.peerpitchkotlinver.ui.components.OutlinedPillButton
import com.example.peerpitchkotlinver.ui.theme.PitchFeedDark
import com.example.peerpitchkotlinver.ui.theme.PitchGold
import com.example.peerpitchkotlinver.vision.EyeContact
import kotlinx.coroutines.delay

private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)

/** Interval at which a metrics sample (eye contact + pace) is captured for the summary. */
private const val SNAPSHOT_INTERVAL_MS = 10_000L

/**
 * TEMP DEBUG: when true, the camera is not started, so SpeechRecognizer runs alone. Used to
 * test whether CameraX + speech recognition contend for the emulator mic. Set back to false.
 */
private const val DISABLE_CAMERA_FOR_SPEECH_TEST = true

@Composable
fun ActiveVideoFeedScreen(onEnd: () -> Unit = {}, onHome: () -> Unit = {}) {
    val context = LocalContext.current
    val session = remember { SessionState(SessionStore(context)) }

    var granted by remember {
        mutableStateOf(
            REQUIRED_PERMISSIONS.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        granted = result.values.all { it }
    }
    LaunchedEffect(Unit) {
        if (!granted) permissionLauncher.launch(REQUIRED_PERMISSIONS)
    }

    // Live transcription via Android's built-in SpeechRecognizer — the same online Google
    // service that powers Gboard voice typing (which works on the emulator). Partial results
    // feed the live transcript; finalized segments commit into the session metrics.
    DisposableEffect(granted) {
        if (!granted) return@DisposableEffect onDispose { }
        session.begin()
        val speech = SpeechController(
            context,
            onPartial = { session.updatePartial(it) },
            onFinal = { session.addFinal(it) }
        )
        if (!speech.start()) {
            Log.w("PeerPitchSpeech", "speech recognition unavailable on this device")
        }
        onDispose { speech.stop() }
    }

    // Capture an eye-contact + pace sample on an interval to build the summary time-series.
    LaunchedEffect(granted) {
        if (!granted) return@LaunchedEffect
        while (true) {
            delay(SNAPSHOT_INTERVAL_MS)
            session.sample()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchGold)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Text(
            text = "Active Video Feed",
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            color = Color.White,
            fontSize = 15.sp,
            fontFamily = FontFamily.Serif,
            textAlign = TextAlign.Center
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PitchFeedDark)
        ) {
            if (granted) {
                if (!DISABLE_CAMERA_FOR_SPEECH_TEST) {
                    CameraPreview(
                        onEyeContact = { session.eyeContact = it },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                MetricsOverlay(session)
            } else {
                PermissionPrompt(onGrant = { permissionLauncher.launch(REQUIRED_PERMISSIONS) })
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedHomeButton(text = "Home", onClick = onHome)
            OutlinedPillButton(text = "End", onClick = onEnd)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun BoxScope.MetricsOverlay(session: SessionState) {
    val eyeLabel = when (session.eyeContact) {
        EyeContact.GOOD -> "Good"
        EyeContact.POOR -> "Look at camera"
        EyeContact.NONE -> "No face detected"
    }
    val dotColor = when (session.eyeContact) {
        EyeContact.GOOD -> Color(0xFF4CAF50)
        EyeContact.POOR -> Color(0xFFE57373)
        EyeContact.NONE -> Color(0xFF9E9E9E)
    }
    Column(
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Eye Contact: $eyeLabel",
                color = PitchGold,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text("Filler Words: ${session.fillerWordCount}", color = Color.White, fontSize = 12.sp)
        Text("Pace: ${session.wordsPerMinute} WPM", color = Color.White, fontSize = 12.sp)
    }
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Chat Transcription:", color = Color.White, fontSize = 14.sp)
        val live = "${session.transcript} ${session.partial}".trim()
        Text(
            text = live.ifBlank { "Listening…" },
            color = Color.White,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun BoxScope.PermissionPrompt(onGrant: () -> Unit) {
    Column(
        modifier = Modifier
            .align(Alignment.Center)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Camera and microphone access are needed for the live feed.",
            color = Color.White,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedPillButton(
            text = "Grant Access",
            onClick = onGrant,
            containerColor = Color.Transparent,
            contentColor = Color.White
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ActiveVideoFeedScreenPreview() {
    ActiveVideoFeedScreen()
}
