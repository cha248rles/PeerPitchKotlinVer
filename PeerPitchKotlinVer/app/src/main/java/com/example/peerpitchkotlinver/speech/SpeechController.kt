/*
 * What: Continuous offline speech-to-text using Vosk; captures mic audio directly into the
 *       recognizer and reports live partial and finalized transcript segments via callbacks.
 * Who:  Charles O'Connell and Anish Machiraju
 * When: 2026-06-21
 */
package com.example.peerpitchkotlinver.speech

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.StorageService
import kotlin.concurrent.thread

/**
 * Continuous speech-to-text using Vosk — an OFFLINE, on-device recognizer.
 *
 * Unlike the Vosk-provided `SpeechService`, this owns its OWN [AudioRecord] and pumps PCM into
 * the [Recognizer] directly. That's deliberate: `SpeechService` opens the mic with
 * `AudioSource.VOICE_RECOGNITION`, which on the emulator (and some devices) is bound to the
 * camera-associated audio path — so the moment CameraX opens the camera, that source is
 * rerouted and the recognizer is fed pure silence. Capturing from [AudioSource.MIC] ourselves
 * keeps us on the primary built-in mic, which is independent of the camera. (See git history for
 * the camera+mic silence investigation.)
 *
 * [onPartial] fires with the live in-progress text; [onFinal] fires with a finalized segment.
 * The bundled English model lives in `assets/model-en-us`. Requires RECORD_AUDIO. Start/stop on
 * the main thread; result callbacks are posted to the main thread.
 */
class SpeechController(
    context: Context,
    private val onPartial: (String) -> Unit,
    private val onFinal: (String) -> Unit
) {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var model: Model? = null
    private var recorder: AudioRecord? = null
    private var captureThread: Thread? = null
    @Volatile private var running = false

    /** Begins loading the model (first-run unpack) and then listening. Always returns true. */
    fun start(): Boolean {
        running = true
        // Unpack the model from assets to internal storage (cached after the first run), then
        // start capturing once it's ready. Loading happens off the main thread.
        StorageService.unpack(
            appContext, MODEL_ASSET, "model",
            { loaded ->
                if (!running) { loaded.close(); return@unpack }
                model = loaded
                beginCapture()
            },
            { e -> Log.e(TAG, "model load failed", e) }
        )
        return true
    }

    private fun beginCapture() {
        val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING)
        if (minBuffer <= 0) {
            Log.e(TAG, "AudioRecord.getMinBufferSize failed: $minBuffer")
            return
        }
        val bufferBytes = maxOf(minBuffer, SAMPLE_RATE) // ~1s of headroom
        val record = runCatching {
            @Suppress("MissingPermission") // RECORD_AUDIO is checked by the caller before start()
            AudioRecord(AUDIO_SOURCE, SAMPLE_RATE, CHANNEL, ENCODING, bufferBytes)
        }.getOrElse {
            Log.e(TAG, "failed to create AudioRecord", it)
            return
        }
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord not initialized (state=${record.state})")
            record.release()
            return
        }
        val recognizer = runCatching { Recognizer(model, SAMPLE_RATE.toFloat()) }.getOrElse {
            Log.e(TAG, "failed to create recognizer", it)
            record.release()
            return
        }
        recorder = record
        record.startRecording()
        Log.d(TAG, "vosk capture started (source=$AUDIO_SOURCE)")

        captureThread = thread(name = "vosk-capture") {
            val buffer = ShortArray(bufferBytes / 2)
            recognizer.use { rec ->
                while (running) {
                    val n = record.read(buffer, 0, buffer.size)
                    if (n <= 0) continue
                    if (rec.acceptWaveForm(buffer, n)) {
                        emit(fieldOf(rec.result, "text"), onFinal)
                    } else {
                        emit(fieldOf(rec.partialResult, "partial"), onPartial)
                    }
                }
                // Flush the tail captured before stop().
                emit(fieldOf(rec.finalResult, "text"), onFinal)
            }
            runCatching {
                record.stop()
                record.release()
            }
        }
    }

    /** Stop listening, flush the final segment, and release the recorder and model. */
    fun stop() {
        running = false
        captureThread?.join(STOP_TIMEOUT_MS)
        captureThread = null
        recorder = null
        model?.close()
        model = null
    }

    private fun emit(text: String, callback: (String) -> Unit) {
        if (text.isNotBlank()) mainHandler.post { callback(text) }
    }

    private fun fieldOf(json: String?, key: String): String =
        runCatching { JSONObject(json ?: "{}").optString(key) }.getOrDefault("")

    private companion object {
        const val TAG = "PeerPitchSpeech"
        const val MODEL_ASSET = "model-en-us"
        const val SAMPLE_RATE = 16000
        const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
        // MIC = the primary built-in mic, independent of the camera audio path. Vosk's own
        // SpeechService uses VOICE_RECOGNITION, which the camera silences on the emulator.
        const val AUDIO_SOURCE = MediaRecorder.AudioSource.MIC
        const val STOP_TIMEOUT_MS = 1000L
    }
}