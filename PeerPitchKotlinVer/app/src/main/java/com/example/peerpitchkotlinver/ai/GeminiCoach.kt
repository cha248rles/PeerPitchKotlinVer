package com.example.peerpitchkotlinver.ai

import android.content.Context
import android.util.Log
import com.example.peerpitchkotlinver.BuildConfig
import com.example.peerpitchkotlinver.session.SessionState.TimedSample
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * End-of-session coaching feedback parsed from Gemini's JSON response. Gemini only judges
 * the subjective [speechQuality] (0..40); the app combines it with the eye-tracking and
 * filler sections in [com.example.peerpitchkotlinver.session.PitchScore] for the final grade.
 */
data class CoachFeedback(
    val speechQuality: Int,
    val eyeContact: String,
    val suggestions: List<String>
)

/**
 * Generates the end-of-session pitch summary with Gemini. Makes ONE call per session: it
 * sends the metrics time-series + transcript and gets back a score, an eye-contact label,
 * and three suggestions (see `assets/gemini_summary_coach_prompt.txt`).
 *
 * Uses `gemini-2.5-flash-lite` — it has free-tier quota and works on the (deprecated) SDK.
 */
class GeminiCoach(context: Context) {

    private val appContext = context.applicationContext

    init {
        val key = BuildConfig.GEMINI_API_KEY
        Log.d(TAG, "key in use: ...${key.takeLast(4)} (length ${key.length})")
    }

    private val model = GenerativeModel(modelName = MODEL, apiKey = BuildConfig.GEMINI_API_KEY)

    private val systemPrompt: String by lazy {
        runCatching {
            appContext.assets.open(PROMPT_ASSET).bufferedReader().use { it.readText() }
        }.getOrDefault("")
    }

    /** Last failure reason, surfaced to the UI when [summarize] returns null. */
    var lastError: String? = null
        private set

    /**
     * Returns parsed feedback, or null on failure (reason in [lastError]). On a free-tier
     * rate-limit hit it waits out the cooldown the error reports ("retry in Ns") and tries
     * once more — one summary call per session won't normally hit it, but rapid testing does.
     */
    suspend fun summarize(
        samples: List<TimedSample>,
        fillerWordCount: Int,
        transcript: String
    ): CoachFeedback? = withContext(Dispatchers.IO) {
        lastError = null
        val payload = "$systemPrompt\n\n== SESSION ==\n${buildSessionJson(samples, fillerWordCount, transcript)}"

        repeat(MAX_ATTEMPTS) { attempt ->
            try {
                val raw = model.generateContent(content { text(payload) }).text?.trim().orEmpty()
                Log.d(TAG, "summary raw -> $raw")
                if (raw.isBlank()) {
                    lastError = "Gemini returned an empty response"
                    return@withContext null
                }
                return@withContext parse(stripFences(raw))
            } catch (e: Exception) {
                val retryMs = retryDelayMs(e.message)
                if (attempt < MAX_ATTEMPTS - 1 && retryMs != null) {
                    Log.d(TAG, "rate-limited; retrying in ${retryMs}ms")
                    delay(retryMs)
                } else {
                    lastError = e.message ?: e.javaClass.simpleName
                    Log.w(TAG, "summary failed", e)
                    return@withContext null
                }
            }
        }
        null
    }

    private fun buildSessionJson(
        samples: List<TimedSample>, fillerWordCount: Int, transcript: String
    ): JSONObject = JSONObject().apply {
        put("samples", JSONArray().apply {
            samples.forEach { s ->
                put(JSONObject().apply {
                    put("tSec", s.tSec)
                    put("eyeContact", s.eyeContact.name)
                    put("wpm", s.wpm)
                })
            }
        })
        put("fillerWordCount", fillerWordCount)
        put("transcript", transcript)
    }

    /** Parse the "retry in Ns" hint from a quota error; null if not a rate-limit error. */
    private fun retryDelayMs(message: String?): Long? {
        if (message == null) return null
        val m = Regex("retry in (\\d+(?:\\.\\d+)?)\\s*s", RegexOption.IGNORE_CASE).find(message)
            ?: return null
        val seconds = m.groupValues[1].toDoubleOrNull() ?: return null
        return ((seconds + 1.0) * 1000).toLong().coerceAtMost(40_000L)
    }

    private fun parse(json: String): CoachFeedback {
        val obj = JSONObject(json)
        val tips = obj.getJSONArray("suggestions")
        return CoachFeedback(
            speechQuality = obj.getInt("speechQuality"),
            eyeContact = obj.getString("eyeContact"),
            suggestions = (0 until tips.length()).map { tips.getString(it) }
        )
    }

    /** Strip ```json ... ``` fences if the model adds them despite instructions. */
    private fun stripFences(text: String): String =
        text.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

    private companion object {
        const val TAG = "PeerPitchCoach"
        const val MODEL = "gemini-2.5-flash-lite"
        const val PROMPT_ASSET = "gemini_summary_coach_prompt.txt"
        const val MAX_ATTEMPTS = 2 // initial try + one retry after the rate-limit cooldown
    }
}