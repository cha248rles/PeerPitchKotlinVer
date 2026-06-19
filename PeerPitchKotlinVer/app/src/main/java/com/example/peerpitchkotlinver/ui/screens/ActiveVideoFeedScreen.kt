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
import kotlinx.coroutines.launch
import android.util.Log
import com.example.peerpitchkotlinver.ai.GeminiTranscriber
import com.example.peerpitchkotlinver.camera.CameraPreview
import com.example.peerpitchkotlinver.session.SessionState
import com.example.peerpitchkotlinver.session.SessionStore
import com.example.peerpitchkotlinver.speech.AudioChunkRecorder
import com.example.peerpitchkotlinver.ui.components.OutlinedHomeButton
import com.example.peerpitchkotlinver.ui.components.OutlinedPillButton
import com.example.peerpitchkotlinver.ui.theme.PitchFeedDark
import com.example.peerpitchkotlinver.ui.theme.PitchGold
import com.example.peerpitchkotlinver.vision.EyeContact
import kotlinx.coroutines.delay

private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)

/** Interval at which a metrics snapshot is assembled for the LLM. */
private const val SNAPSHOT_INTERVAL_MS = 10_000L

/** Length of each audio clip sent to Gemini for transcription. */
private const val CLIP_MS = 10_000L

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

    // Transcribe mic audio with Gemini in short clips (Android's on-device recognizer is
    // unavailable on the emulator). Record a clip, then transcribe it in the background and
    // append the text to the running transcript while the next clip records.
    LaunchedEffect(granted) {
        if (!granted) return@LaunchedEffect
        session.begin()
        val recorder = AudioChunkRecorder(context)
        val transcriber = GeminiTranscriber()
        try {
            while (true) {
                if (!recorder.startClip()) {
                    delay(1_000L)
                    continue
                }
                delay(CLIP_MS)
                val file = recorder.stopClip() ?: continue
                launch {
                    val bytes = runCatching { file.readBytes() }.getOrNull()
                    if (bytes != null) {
                        val text = transcriber.transcribe(bytes, "audio/aac")
                        Log.d("PeerPitchTranscribe", "clip -> ${text ?: "(none)"}")
                        if (!text.isNullOrBlank()) session.addFinal(text)
                    }
                    runCatching { file.delete() }
                }
            }
        } finally {
            recorder.release()
        }
    }

    // Refresh pace + flush the latest metrics to the session file on an interval.
    LaunchedEffect(granted) {
        if (!granted) return@LaunchedEffect
        while (true) {
            delay(SNAPSHOT_INTERVAL_MS)
            session.tick()
            // TODO(Gemini): send session.snapshot() / samples for live tips + final summary.
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
                CameraPreview(
                    onEyeContact = { session.eyeContact = it },
                    modifier = Modifier.fillMaxSize()
                )
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
