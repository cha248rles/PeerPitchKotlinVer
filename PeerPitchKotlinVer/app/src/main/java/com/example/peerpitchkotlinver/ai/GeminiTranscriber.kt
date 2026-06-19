package com.example.peerpitchkotlinver.ai

import android.util.Log
import com.example.peerpitchkotlinver.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content

/**
 * Transcribes short audio clips with Gemini. Used because Android's on-device
 * [android.speech.SpeechRecognizer] has no working speech engine on the emulator, so we
 * record the mic and let Gemini turn the audio into text instead.
 *
 * Reads the key from [BuildConfig.GEMINI_API_KEY] (set in local.properties).
 */
class GeminiTranscriber {

    init {
        val key = BuildConfig.GEMINI_API_KEY
        Log.d(TAG, "key in use: ...${key.takeLast(4)} (length ${key.length})")
    }

    private val model = GenerativeModel(
        modelName = MODEL,
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    /** Returns the spoken text in [audioBytes], or null on failure / no speech. */
    suspend fun transcribe(audioBytes: ByteArray, mimeType: String): String? =
        runCatching {
            val response = model.generateContent(
                content {
                    blob(mimeType, audioBytes)
                    text(PROMPT)
                }
            )
            response.text?.trim()?.takeIf { it.isNotBlank() }
        }.onFailure { Log.w(TAG, "transcription failed", it) }.getOrNull()

    private companion object {
        const val TAG = "PeerPitchTranscribe"
        // If this 404s, the model name is the thing to change.
        const val MODEL = "gemini-2.5-flash-lite"
        const val PROMPT =
            "Transcribe the spoken words in this audio clip to plain text. " +
                "Return ONLY the transcription, with no quotes and no commentary. " +
                "If there is no clear speech, return nothing at all."
    }
}
