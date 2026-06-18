package com.example.peerpitchkotlinver.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

/**
 * Thin wrapper around Android's built-in [SpeechRecognizer] that keeps listening
 * continuously by restarting itself whenever a result or timeout occurs.
 *
 * [onPartial] fires with the live (in-progress) text; [onFinal] fires with a committed
 * segment once the recognizer finalizes it. Requires the RECORD_AUDIO permission and a
 * speech-recognition service on the device. All methods must be called on the main thread.
 */
class SpeechController(
    private val context: Context,
    private val onPartial: (String) -> Unit,
    private val onFinal: (String) -> Unit
) : RecognitionListener {

    private var recognizer: SpeechRecognizer? = null
    private var listening = false
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    /** Returns false if no recognition service is available (e.g. a bare emulator). */
    fun start(): Boolean {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(TAG, "no speech recognition service available on this device")
            return false
        }
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(this@SpeechController)
        }
        listening = true
        listen()
        Log.d(TAG, "listening started")
        return true
    }

    fun stop() {
        listening = false
        handler.removeCallbacksAndMessages(null)
        recognizer?.destroy()
        recognizer = null
    }

    private fun listen() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        recognizer?.startListening(intent)
    }

    override fun onPartialResults(partialResults: Bundle?) {
        firstResult(partialResults)?.let {
            if (it.isNotBlank()) {
                Log.d(TAG, "partial: $it")
                onPartial(it)
            }
        }
    }

    override fun onResults(results: Bundle?) {
        firstResult(results)?.let {
            if (it.isNotBlank()) {
                Log.d(TAG, "final: $it")
                onFinal(it)
            }
        }
        if (listening) listen() // restart for continuous transcription
    }

    override fun onError(error: Int) {
        // Common during pauses (ERROR_NO_MATCH / ERROR_SPEECH_TIMEOUT); just keep going.
        Log.d(TAG, "error code=$error (restarting)")
        if (listening) handler.postDelayed({ if (listening) listen() }, 600L)
    }

    private fun firstResult(bundle: Bundle?): String? =
        bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()

    private companion object {
        const val TAG = "PeerPitchSpeech"
    }

    // Unused callbacks.
    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onEvent(eventType: Int, params: Bundle?) {}
}
