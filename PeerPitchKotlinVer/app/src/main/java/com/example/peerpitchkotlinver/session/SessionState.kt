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
 * [setPartial], which recomputes the filler-word count and speaking pace.
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

    fun begin() {
        startMs = SystemClock.elapsedRealtime()
        transcript = ""
        partial = ""
        fillerWordCount = 0
        wordsPerMinute = 0
    }

    /** Append a finalized speech segment and refresh derived metrics. */
    fun addFinal(text: String) {
        transcript = "$transcript $text".trim()
        partial = ""
        recompute()
    }

    /** Update the in-progress (not yet committed) tail of speech. */
    fun updatePartial(text: String) {
        partial = text
    }

    fun snapshot(): MetricsSnapshot =
        MetricsSnapshot(eyeContact, fillerWordCount, wordsPerMinute, transcript)

    private fun recompute() {
        val words = transcript.split(WHITESPACE).filter { it.isNotBlank() }
        val elapsedMin = ((SystemClock.elapsedRealtime() - startMs) / 60_000.0).coerceAtLeast(MIN_MINUTES)
        wordsPerMinute = (words.size / elapsedMin).toInt()
        fillerWordCount = countFillers(transcript)
    }

    private fun countFillers(text: String): Int {
        val haystack = " ${text.lowercase()} "
        return FILLER_WORDS.sumOf { filler ->
            Regex("\\b${Regex.escape(filler)}\\b").findAll(haystack).count()
        }
    }

    companion object {
        private val WHITESPACE = Regex("\\s+")
        private const val MIN_MINUTES = 1.0 / 60.0 // avoid divide-by-zero in the first second
        private val FILLER_WORDS = listOf("um", "uh", "uhm", "umm", "er", "ah", "like", "you know")
    }
}
