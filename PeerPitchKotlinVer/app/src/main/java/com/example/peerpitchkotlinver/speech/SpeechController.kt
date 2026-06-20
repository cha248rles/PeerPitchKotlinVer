package com.example.peerpitchkotlinver.speech

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService

/**
 * Continuous speech-to-text using Vosk — an OFFLINE, on-device recognizer. Unlike Android's
 * [android.speech.SpeechRecognizer], it has no per-utterance server session: it captures the
 * mic itself and streams results indefinitely, so it survives a full multi-minute pitch with
 * no restart loop, no ~60s cap, and no quota. (Android's recognizer died after one session on
 * the emulator with ERROR_SERVER_DISCONNECTED — see git history.)
 *
 * [onPartial] fires with the live in-progress text; [onFinal] fires with a finalized segment.
 * The bundled English model lives in `assets/model-en-us`. Requires RECORD_AUDIO. Start/stop
 * on the main thread; result callbacks arrive on the main thread.
 */
class SpeechController(
    context: Context,
    private val onPartial: (String) -> Unit,
    private val onFinal: (String) -> Unit
) : RecognitionListener {

    private val appContext = context.applicationContext
    private var model: Model? = null
    private var speechService: SpeechService? = null
    private var started = false

    /** Begins loading the model (first-run unpack) and then listening. Always returns true. */
    fun start(): Boolean {
        started = true
        // Unpack the model from assets to internal storage (cached after the first run), then
        // start listening once it's ready. Loading happens off the main thread.
        StorageService.unpack(
            appContext, MODEL_ASSET, "model",
            { loaded ->
                if (!started) { loaded.close(); return@unpack }
                model = loaded
                beginListening()
            },
            { e -> Log.e(TAG, "model load failed", e) }
        )
        return true
    }

    private fun beginListening() {
        runCatching {
            val recognizer = Recognizer(model, SAMPLE_RATE)
            speechService = SpeechService(recognizer, SAMPLE_RATE)
            speechService?.startListening(this)
            Log.d(TAG, "vosk listening started")
        }.onFailure { Log.e(TAG, "failed to start vosk", it) }
    }

    fun stop() {
        started = false
        runCatching {
            speechService?.stop()
            speechService?.shutdown()
        }
        speechService = null
        model?.close()
        model = null
}

    override fun onPartialResult(hypothesis: String?) {
        val text = fieldOf(hypothesis, "partial")
        if (text.isNotBlank()) onPartial(text)
    }

    override fun onResult(hypothesis: String?) {
        val text = fieldOf(hypothesis, "text")
        if (text.isNotBlank()) {
            Log.d(TAG, "final: $text")
            onFinal(text)
        }
    }

    override fun onFinalResult(hypothesis: String?) {
        // The tail flushed when listening stops; append anything not already emitted.
        val text = fieldOf(hypothesis, "text")
        if (text.isNotBlank()) onFinal(text)
    }

    override fun onError(exception: Exception?) {
        Log.w(TAG, "vosk error", exception)
    }

    override fun onTimeout() {}

    private fun fieldOf(json: String?, key: String): String =
        runCatching { JSONObject(json ?: "{}").optString(key) }.getOrDefault("")

    private companion object {
        const val TAG = "PeerPitchSpeech"
        const val MODEL_ASSET = "model-en-us"
        const val SAMPLE_RATE = 16000.0f
    }
}
