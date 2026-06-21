package com.example.peerpitchkotlinver.camera

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.peerpitchkotlinver.vision.EyeContact
import com.example.peerpitchkotlinver.vision.FaceLandmarkerHelper

/**
 * Front-camera preview wired to MediaPipe. Shows the live feed and runs each frame
 * (throttled) through [FaceLandmarkerHelper], reporting eye-contact state via [onEyeContact].
 */
@Composable
fun CameraPreview(
    onEyeContact: (EyeContact) -> Unit,
    modifier: Modifier = Modifier,
    runFaceDetection: Boolean = true
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnEyeContact = rememberUpdatedState(onEyeContact)

    // TEMP DEBUG: when runFaceDetection is false, the preview still streams but MediaPipe is
    // never created or fed a frame — isolates whether the Face Landmarker (not CameraX itself)
    // is what silences the Vosk mic on the emulator.
    val faceHelper = remember(runFaceDetection) {
        if (runFaceDetection) {
            FaceLandmarkerHelper(context) { state -> latestOnEyeContact.value(state) }
        } else null
    }
    DisposableEffect(faceHelper) {
        onDispose { faceHelper?.close() }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val providerFuture = ProcessCameraProvider.getInstance(ctx)
            providerFuture.addListener({
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                var lastDetectMs = 0L
                analysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { proxy ->
                    val now = SystemClock.uptimeMillis()
                    if (faceHelper != null && now - lastDetectMs >= DETECT_INTERVAL_MS) {
                        lastDetectMs = now
                        runCatching { faceHelper.detect(proxy.toUprightBitmap(), now) }
                    }
                    proxy.close()
                }

                runCatching {
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        preview,
                        analysis
                    )
                }.onSuccess {
                    Log.d(TAG, "front camera bound, streaming frames")
                }.onFailure {
                    Log.e(TAG, "failed to bind camera", it)
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}

private const val DETECT_INTERVAL_MS = 100L
private const val TAG = "PeerPitchCamera"

/** Convert an analysis frame to an upright bitmap, applying the sensor rotation. */
private fun ImageProxy.toUprightBitmap(): Bitmap {
    val bitmap = toBitmap()
    val rotation = imageInfo.rotationDegrees
    if (rotation == 0) return bitmap
    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
