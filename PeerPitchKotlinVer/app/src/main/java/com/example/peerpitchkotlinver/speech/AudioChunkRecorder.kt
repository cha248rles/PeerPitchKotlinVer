package com.example.peerpitchkotlinver.speech

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

/**
 * Records short fixed-length audio clips from the mic into the app cache. Each clip is an
 * AAC/ADTS file ("audio/aac") ready to hand to [com.example.peerpitchkotlinver.ai.GeminiTranscriber].
 *
 * Usage per clip: [startClip] → wait → [stopClip] (returns the file). Call [release] when done.
 * Requires the RECORD_AUDIO permission.
 */
class AudioChunkRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var current: File? = null
    private var index = 0

    /** Begin recording a new clip. Returns false if the recorder failed to start. */
    fun startClip(): Boolean {
        val file = File(context.cacheDir, "clip_${index++}.aac")
        val rec = newRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(16_000)
            setAudioEncodingBitRate(32_000)
            setOutputFile(file.absolutePath)
        }
        return runCatching {
            rec.prepare()
            rec.start()
            recorder = rec
            current = file
            true
        }.onFailure {
            Log.w(TAG, "failed to start clip", it)
            runCatching { rec.release() }
        }.getOrDefault(false)
    }

    /**
     * Peak amplitude (0..32767) since the previous call, or 0 if not recording. Poll this
     * while a clip records to decide whether it actually contains speech vs. silence.
     */
    fun peakAmplitude(): Int = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)

    /** Stop the current clip and return its file, or null if nothing usable was recorded. */
    fun stopClip(): File? {
        val rec = recorder ?: return null
        val file = current
        recorder = null
        current = null
        return runCatching {
            rec.stop()
            rec.release()
            file
        }.onFailure {
            Log.w(TAG, "failed to stop clip", it)
            runCatching { rec.release() }
        }.getOrNull()
    }

    fun release() {
        runCatching { recorder?.release() }
        recorder = null
        current = null
    }

    private fun newRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context)
        else @Suppress("DEPRECATION") MediaRecorder()

    private companion object {
        const val TAG = "PeerPitchAudio"
    }
}