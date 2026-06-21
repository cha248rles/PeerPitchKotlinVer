/*
 * What: Deterministically computes the 0..100 pitch grade from session metrics, combining
 *       eye-tracking, Gemini-judged speech quality, and filler-word sections.
 * Who:  Charles O'Connell and Anish Machiraju
 * When: 2026-06-21
 */
package com.example.peerpitchkotlinver.session

import com.example.peerpitchkotlinver.session.SessionState.TimedSample
import com.example.peerpitchkotlinver.vision.EyeContact
import kotlin.math.roundToInt

/**
 * Computes the 0..100 pitch grade deterministically from the session metrics, so the
 * same pitch always yields the same number (no LLM round-number guessing). Three sections:
 *
 *  - Eye tracking (0..30): share of samples with GOOD eye contact, POOR at half credit.
 *    With the camera disabled every sample is NONE, so this scores 0 until vision is
 *    wired up — then it earns points automatically from real samples.
 *  - Speech quality (0..40): the only subjective part — judged by Gemini.
 *  - Filler words (0..30): full marks with none, -10 per filler word, floored at 0.
 *
 * No speech captured at all => 0 overall, regardless of the other sections.
 */
object PitchScore {
    const val EYE_CONTACT_MAX = 30
    const val SPEECH_QUALITY_MAX = 40
    const val FILLER_MAX = 30
    const val FILLER_PENALTY_PER_WORD = 10

    /** Eye-tracking points (0..30) from the GOOD/POOR/NONE mix; 0 with no samples. */
    fun eyeTrackingPoints(samples: List<TimedSample>): Int {
        if (samples.isEmpty()) return 0
        val credit = samples.sumOf { s ->
            when (s.eyeContact) {
                EyeContact.GOOD -> 1.0
                EyeContact.POOR -> 0.5
                EyeContact.NONE -> 0.0
            }
        }
        return (credit / samples.size * EYE_CONTACT_MAX).roundToInt()
    }

    /** Filler points (0..30): 30 minus 10 per filler word, never below 0. */
    fun fillerPoints(fillerWordCount: Int): Int =
        (FILLER_MAX - fillerWordCount * FILLER_PENALTY_PER_WORD).coerceAtLeast(0)

    /**
     * Final 0..100 grade. [speechQuality] is Gemini's 0..40 score (clamped defensively).
     * Returns 0 when nothing was said.
     */
    fun total(
        samples: List<TimedSample>,
        fillerWordCount: Int,
        speechQuality: Int,
        transcript: String
    ): Int {
        if (transcript.isBlank()) return 0
        return eyeTrackingPoints(samples) +
            fillerPoints(fillerWordCount) +
            speechQuality.coerceIn(0, SPEECH_QUALITY_MAX)
    }
}