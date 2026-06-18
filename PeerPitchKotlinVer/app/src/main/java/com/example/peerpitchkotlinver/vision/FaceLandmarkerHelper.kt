package com.example.peerpitchkotlinver.vision

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult

/** Coarse eye-contact state derived from face landmarks. */
enum class EyeContact { NONE, GOOD, POOR }

/**
 * Wraps MediaPipe Face Landmarker in LIVE_STREAM mode. Feed it camera frames with
 * [detect]; results are interpreted into an [EyeContact] value and delivered on the
 * main thread via the [onResult] callback.
 *
 * The model file `face_landmarker.task` must live in `app/src/main/assets/`.
 */
class FaceLandmarkerHelper(
    context: Context,
    private val onResult: (EyeContact) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var faceLandmarker: FaceLandmarker? = null

    init {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(MODEL_PATH)
            .build()
        val options = FaceLandmarker.FaceLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumFaces(1)
            .setOutputFaceBlendshapes(true)
            .setResultListener { result, _ ->
                val state = interpret(result)
                mainHandler.post { onResult(state) }
            }
            .setErrorListener { /* swallow transient frame errors */ }
            .build()
        faceLandmarker = FaceLandmarker.createFromOptions(context, options)
    }

    /** Submit a frame for async detection. [timestampMs] must strictly increase. */
    fun detect(bitmap: Bitmap, timestampMs: Long) {
        val mpImage = BitmapImageBuilder(bitmap).build()
        faceLandmarker?.detectAsync(mpImage, timestampMs)
    }

    fun close() {
        faceLandmarker?.close()
        faceLandmarker = null
    }

    /**
     * Uses the gaze blendshapes (how far each eye looks in/out/up/down). When the user
     * looks at the camera all gaze scores stay low; a high score means they're looking away.
     */
    private fun interpret(result: FaceLandmarkerResult): EyeContact {
        val blendshapes = result.faceBlendshapes()
        if (!blendshapes.isPresent || blendshapes.get().isEmpty()) {
            Log.d(TAG, "no face detected")
            return EyeContact.NONE
        }
        val faceShapes = blendshapes.get()[0]
        val maxGaze = faceShapes
            .filter { it.categoryName() in GAZE_SHAPES }
            .maxOfOrNull { it.score() } ?: 0f
        val state = if (maxGaze < GAZE_THRESHOLD) EyeContact.GOOD else EyeContact.POOR
        Log.d(TAG, "face found · maxGaze=%.2f · eyeContact=%s".format(maxGaze, state))
        return state
    }

    companion object {
        private const val TAG = "PeerPitchVision"
        private const val MODEL_PATH = "face_landmarker.task"
        private const val GAZE_THRESHOLD = 0.45f
        private val GAZE_SHAPES = setOf(
            "eyeLookInLeft", "eyeLookOutLeft", "eyeLookUpLeft", "eyeLookDownLeft",
            "eyeLookInRight", "eyeLookOutRight", "eyeLookUpRight", "eyeLookDownRight"
        )
    }
}
