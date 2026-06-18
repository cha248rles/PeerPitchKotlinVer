package com.example.peerpitchkotlinver.session

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.peerpitchkotlinver.vision.EyeContact

/** Immutable snapshot of the live metrics, assembled on an interval for the LLM. */
data class MetricsSnapshot(
    val eyeContact: EyeContact,
    val fillerWordCount: Int,
    val wordsPerMinute: Int,
    val transcript: String
)

/**
 * Holds the live metrics for one practice session as Compose state. The camera/vision
 * layer pushes [eyeContact]; the speech layer pushes transcript text via [addFinal] and
 * [updatePartial].
 *
 * Filler words are a running session total; speaking pace ([wordsPerMinute]) is computed
 * over a rolling [WINDOW_MS] window so it tracks current speech and eases back toward zero
 * during silence (call [tick] periodically to refresh it without new speech).
 */
class SessionState {

    var eyeContact by mutableStateOf(EyeContact.NONE)
    var transcript by mutableStateOf("")
        private set
    var partial by mutableStateOf("")
        private set
    var fillerWordCount by mutableStateOf(0)
        private set
    var wordsPerMinute by mutableStateOf(0)
        private set

    private var startMs = 0L
    private val bursts = ArrayDeque<WordBurst>()

    fun begin() {
        startMs = SystemClock.elapsedRealtime()
        transcript = ""
        partial = ""
        fillerWordCount = 0
        wordsPerMinute = 0
        bursts.clear()
    }

    /** Append a finalized speech segment and refresh derived metrics. */
    fun addFinal(text: String) {
        transcript = "$transcript $text".trim()
        partial = ""
        val newWords = text.split(WHITESPACE).count { it.isNotBlank() }
        if (newWords > 0) bursts.addLast(WordBurst(SystemClock.elapsedRealtime(), newWords))
        recompute()
    }

    /** Update the in-progress (not yet committed) tail of speech. */
    fun updatePartial(text: String) {
        partial = text
    }

    /** Recompute time-based metrics (pace) without new speech; safe to call on a timer. */
    fun tick() = recompute()

    fun snapshot(): MetricsSnapshot {
        recompute()
        return MetricsSnapshot(eyeContact, fillerWordCount, wordsPerMinute, transcript)
    }

    private fun recompute() {
        val now = SystemClock.elapsedRealtime()
        while (bursts.isNotEmpty() && now - bursts.first().atMs > WINDOW_MS) bursts.removeFirst()
        val wordsInWindow = bursts.sumOf { it.count }
        val elapsedMs = (now - startMs).coerceAtLeast(1L)
        val windowMs = minOf(elapsedMs, WINDOW_MS)
        val minutes = (windowMs / 60_000.0).coerceAtLeast(MIN_MINUTES)
        wordsPerMinute = (wordsInWindow / minutes).toInt()
        fillerWordCount = countFillers(transcript)
    }

    private fun countFillers(text: String): Int {
        val haystack = " ${text.lowercase()} "
        return FILLER_WORDS.sumOf { filler ->
            Regex("\\b${Regex.escape(filler)}\\b").findAll(haystack).count()
        }
    }

    private data class WordBurst(val atMs: Long, val count: Int)

    companion object {
        private val WHITESPACE = Regex("\\s+")
        private const val WINDOW_MS = 30_000L
        private const val MIN_MINUTES = 1.0 / 60.0 // avoid divide-by-zero in the first second
        private val FILLER_WORDS = listOf(
            "um", "uh", "uhm", "umm", "er", "ah", "hmm",
            "like", "you know", "i mean", "kind of", "sort of"
        )
    }
}
