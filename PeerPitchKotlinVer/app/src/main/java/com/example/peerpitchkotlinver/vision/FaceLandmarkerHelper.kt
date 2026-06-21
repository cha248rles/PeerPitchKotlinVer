/*
 * What: Wraps MediaPipe Face Landmarker in live-stream mode to derive a coarse eye-contact
 *       state (NONE/GOOD/POOR) from head turn and gaze, smoothed and delivered via callback.
 * Who:  Charles O'Connell and Anish Machiraju
 * When: 2026-06-21
 */
package com.example.peerpitchkotlinver.vision

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import kotlin.math.abs

/** Coarse eye-contact state derived from face landmarks. */
enum class EyeContact { NONE, GOOD, POOR }

/**
 * Wraps MediaPipe Face Landmarker in LIVE_STREAM mode. Feed it camera frames with
 * [detect]; results are interpreted into an [EyeContact] value and delivered on the
 * main thread via the [onResult] callback.
 *
 * Eye contact combines two signals — how far the head is turned (from the face
 * landmarks) and where the eyes are looking (from the gaze blendshapes) — then smooths
 * the result over a short window so it doesn't flicker frame to frame.
 *
 * The model file `face_landmarker.task` must live in `app/src/main/assets/`.
 */
class FaceLandmarkerHelper(
    context: Context,
    private val onResult: (EyeContact) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val recent = ArrayDeque<EyeContact>()
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

    /** Release the underlying Face Landmarker; call when the camera/vision pipeline shuts down. */
    fun close() {
        faceLandmarker?.close()
        faceLandmarker = null
    }

    private fun interpret(result: FaceLandmarkerResult): EyeContact {
        val faces = result.faceLandmarks()
        if (faces.isEmpty() || faces[0].isEmpty()) {
            Log.d(TAG, "no face detected")
            return smooth(EyeContact.NONE)
        }
        val face = faces[0]
        val yaw = headYaw(face)
        val maxGaze = maxGazeScore(result)

        val raw = when {
            abs(yaw) > YAW_THRESHOLD -> EyeContact.POOR   // head turned away
            maxGaze > GAZE_THRESHOLD -> EyeContact.POOR   // eyes looking away
            else -> EyeContact.GOOD
        }
        val smoothed = smooth(raw)
        Log.d(TAG, "yaw=%.3f maxGaze=%.2f raw=%s -> %s".format(yaw, maxGaze, raw, smoothed))
        return smoothed
    }

    /** Horizontal head turn: 0 ≈ facing camera, magnitude grows as the head rotates. */
    private fun headYaw(face: List<NormalizedLandmark>): Float {
        val nose = face[NOSE_TIP]
        val left = face[LEFT_CHEEK]
        val right = face[RIGHT_CHEEK]
        val centerX = (left.x() + right.x()) / 2f
        val width = abs(right.x() - left.x()).coerceAtLeast(1e-4f)
        return (nose.x() - centerX) / width
    }

    /** Largest eye-gaze blendshape score; low when looking straight ahead. */
    private fun maxGazeScore(result: FaceLandmarkerResult): Float {
        val blendshapes = result.faceBlendshapes()
        if (!blendshapes.isPresent || blendshapes.get().isEmpty()) return 0f
        return blendshapes.get()[0]
            .filter { it.categoryName() in GAZE_SHAPES }
            .maxOfOrNull { it.score() } ?: 0f
    }

    /** Majority vote over the last [SMOOTH_WINDOW] frames; resets when the face is lost. */
    private fun smooth(raw: EyeContact): EyeContact {
        if (raw == EyeContact.NONE) {
            recent.clear()
            return EyeContact.NONE
        }
        recent.addLast(raw)
        while (recent.size > SMOOTH_WINDOW) recent.removeFirst()
        val good = recent.count { it == EyeContact.GOOD }
        return if (good * 2 >= recent.size) EyeContact.GOOD else EyeContact.POOR
    }

    companion object {
        private const val TAG = "PeerPitchVision"
        private const val MODEL_PATH = "face_landmarker.task"
        private const val GAZE_THRESHOLD = 0.45f
        private const val YAW_THRESHOLD = 0.08f
        private const val SMOOTH_WINDOW = 5

        // Face-mesh landmark indices (478-point model).
        private const val NOSE_TIP = 1
        private const val LEFT_CHEEK = 234
        private const val RIGHT_CHEEK = 454

        private val GAZE_SHAPES = setOf(
            "eyeLookInLeft", "eyeLookOutLeft", "eyeLookUpLeft", "eyeLookDownLeft",
            "eyeLookInRight", "eyeLookOutRight", "eyeLookUpRight", "eyeLookDownRight"
        )
    }
}
