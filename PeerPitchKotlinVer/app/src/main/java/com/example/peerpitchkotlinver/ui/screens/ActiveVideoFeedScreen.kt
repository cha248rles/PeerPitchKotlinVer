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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.peerpitchkotlinver.ai.GeminiCoach
import com.example.peerpitchkotlinver.camera.CameraPreview
import com.example.peerpitchkotlinver.session.PitchScore
import com.example.peerpitchkotlinver.session.SessionState
import com.example.peerpitchkotlinver.session.SessionStore
import com.example.peerpitchkotlinver.speech.SpeechController
import com.example.peerpitchkotlinver.ui.components.OutlinedHomeButton
import com.example.peerpitchkotlinver.ui.components.OutlinedPillButton
import com.example.peerpitchkotlinver.ui.theme.PitchFeedDark
import com.example.peerpitchkotlinver.ui.theme.PitchGold
import com.example.peerpitchkotlinver.vision.EyeContact
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)

/** Interval at which a metrics sample (eye contact + pace) is captured for the summary. */
private const val SNAPSHOT_INTERVAL_MS = 10_000L

/**
 * TEMP DEBUG: when false, the camera preview streams but the MediaPipe Face Landmarker never
 * runs — isolates whether MediaPipe (not CameraX) is what silences the Vosk mic. Set true once
 * the camera+mic interaction is confirmed.
 */
private const val RUN_FACE_DETECTION = true

@Composable
fun ActiveVideoFeedScreen(onEnd: (PitchResult) -> Unit = {}, onHome: () -> Unit = {}) {
    val context = LocalContext.current
    val session = remember { SessionState(SessionStore(context)) }
    val coach = remember { GeminiCoach(context) }
    val scope = rememberCoroutineScope()
    var analyzing by remember { mutableStateOf(false) }

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

    // On "End": capture a final sample, ask Gemini for the summary (one call), assemble the
    // result from Gemini's feedback + the metrics we already track, then navigate to Results.
    fun finishSession() {
        if (analyzing) return
        analyzing = true
        scope.launch {
            session.sample()
            // Include `partial` — the sentence still in progress when End is pressed lives there
            // (Vosk only commits it to `transcript` on stop(), which fires later during dispose).
            val fullTranscript = "${session.transcript} ${session.partial}".trim()
            val samples = session.samplesSnapshot()
            val feedback = coach.summarize(
                samples, session.fillerWordCount, fullTranscript
            )
            // Final grade is computed deterministically from the metrics (eye tracking +
            // fillers in code, speech quality from Gemini) so the same pitch always scores
            // the same — see PitchScore. Gemini failing just zeroes the speech-quality slice.
            val score = PitchScore.total(
                samples = samples,
                fillerWordCount = session.fillerWordCount,
                speechQuality = feedback?.speechQuality ?: 0,
                transcript = fullTranscript
            )
            onEnd(
                PitchResult(
                    score = score,
                    eyeContact = feedback?.eyeContact ?: "—",
                    fillerWordCount = session.fillerWordCount,
                    durationLabel = formatDuration(session.elapsedMs),
                    transcription = fullTranscript.ifBlank { "(no speech captured)" },
                    suggestions = feedback?.suggestions?.ifEmpty { null }
                        ?: listOf("Couldn't generate feedback: ${coach.lastError ?: "no suggestions returned"}")
                )
            )
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
                // Camera (eye-contact tracking) and Vosk (mic) run together. CameraX here binds
                // only Preview + ImageAnalysis — no VideoCapture — so it never opens an audio
                // source; Vosk owns the mic alone. Any past "silent mic while camera on" was an
                // emulator host-audio routing quirk, not contention in app code.
                CameraPreview(
                    onEyeContact = { session.eyeContact = it },
                    modifier = Modifier.fillMaxSize(),
                    runFaceDetection = RUN_FACE_DETECTION
                )
                MetricsOverlay(session)
                if (analyzing) AnalyzingOverlay()
            } else {
                PermissionPrompt(onGrant = { permissionLauncher.launch(REQUIRED_PERMISSIONS) })
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedHomeButton(text = "Home", onClick = { if (!analyzing) onHome() })
            OutlinedPillButton(text = "End", onClick = { finishSession() })
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

/** Covers the feed while the end-of-session Gemini summary call runs. */
@Composable
private fun BoxScope.AnalyzingOverlay() {
    Column(
        modifier = Modifier
            .matchParentSize()
            .background(Color(0xCC000000)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Analyzing your pitch…",
            color = PitchGold,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

/** Format elapsed milliseconds as M:SS for the results screen. */
private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    return "${totalSeconds / 60}:${(totalSeconds % 60).toString().padStart(2, '0')}"
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
