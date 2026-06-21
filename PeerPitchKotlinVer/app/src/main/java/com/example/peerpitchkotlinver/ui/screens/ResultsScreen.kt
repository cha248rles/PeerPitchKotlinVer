/*
 * What: Results screen — displays a completed pitch's score, metrics, transcription, and suggestions.
 * Who:  Charles O'Connell and Anish Machiraju
 * When: 2026-06-21
 */
package com.example.peerpitchkotlinver.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.peerpitchkotlinver.ui.components.BulletLine
import com.example.peerpitchkotlinver.ui.components.MetricCard
import com.example.peerpitchkotlinver.ui.components.OutlinedPillButton
import com.example.peerpitchkotlinver.ui.components.PitchTopBar
import com.example.peerpitchkotlinver.ui.components.ScoreBadge
import com.example.peerpitchkotlinver.ui.components.SectionCard
import com.example.peerpitchkotlinver.ui.theme.PitchGold

/**
 * Summary of a completed pitch. Defaults provide dummy data so the screen can be
 * previewed and navigated to before any analysis backend is wired in.
 */
data class PitchResult(
    val score: Int,
    val eyeContact: String,
    val fillerWordCount: Int,
    val durationLabel: String,
    val transcription: String,
    val suggestions: List<String>
)

val sampleResult = PitchResult(
    score = 82,
    eyeContact = "Good",
    fillerWordCount = 7,
    durationLabel = "1:42",
    transcription = "The main reason why we decided to do this is uhm because we like " +
        "the idea of helping people practice their presentations. Our app gives " +
        "instant feedback so you can, like, improve before the real thing.",
    suggestions = listOf(
        "Reduce filler words like \"uhm\" and \"like\".",
        "Maintain eye contact with the camera a bit longer.",
        "Slow down slightly during the opening sentence."
    )
)

/**
 * Renders the pitch results screen: a scrollable, gold-backed layout with a score badge,
 * a row of metric cards (eye contact, filler words, duration), a transcription section,
 * a suggestions section, and a Done button.
 */
@Composable
fun ResultsScreen(
    result: PitchResult = sampleResult,
    onBack: () -> Unit = {},
    onDone: () -> Unit = {}
) {
    Scaffold(
        containerColor = PitchGold,
        topBar = { PitchTopBar(title = "Your Results", onBack = onBack, useHomeIcon = true) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            ScoreBadge(score = result.score)
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Eye Contact",
                    value = result.eyeContact,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Filler Words",
                    value = "${result.fillerWordCount}",
                    detail = "times",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Duration",
                    value = result.durationLabel,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            SectionCard(title = "Transcription") {
                Text(result.transcription, color = Color(0xFF333333), fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
            SectionCard(title = "Suggestions") {
                result.suggestions.forEach { BulletLine(it) }
            }

            Spacer(modifier = Modifier.height(28.dp))
            OutlinedPillButton(text = "Done", onClick = onDone)
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

/** Design-time preview of the results screen using the built-in sample result. */
@Preview(showBackground = true)
@Composable
fun ResultsScreenPreview() {
    ResultsScreen()
}
