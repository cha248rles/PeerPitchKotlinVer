package com.example.peerpitchkotlinver.session

import android.content.Context
import com.example.peerpitchkotlinver.vision.EyeContact
import org.json.JSONObject
import java.io.File
import java.util.concurrent.Executors

/**
 * Persists the current session metrics to a single JSON file in the app's internal
 * storage (`filesDir/session.json`) so other parts of the app can read the latest
 * snapshot off disk.
 *
 * The file holds only the current run: [reset] overwrites it at the start of each
 * session, so it "refreshes" rather than accumulating history. Writes happen on a
 * background thread to keep them off the UI.
 */
class SessionStore(context: Context) {

    private val file = File(context.filesDir, FILE_NAME)
    private val io = Executors.newSingleThreadExecutor()

    /** Absolute path of the session file, for whatever else needs to read it. */
    val path: String get() = file.absolutePath

    /** Clear the file back to an empty session (call at session start). */
    fun reset() = write(MetricsSnapshot(EyeContact.NONE, 0, 0, ""))

    /** Overwrite the file with the latest metrics. */
    fun save(snapshot: MetricsSnapshot) = write(snapshot)

    /** Read the raw JSON back (empty string if nothing has been written yet). */
    fun read(): String =
        runCatching { if (file.exists()) file.readText() else "" }.getOrDefault("")

    private fun write(snapshot: MetricsSnapshot) {
        io.execute {
            val json = JSONObject().apply {
                put("eyeContact", snapshot.eyeContact.name)
                put("fillerWordCount", snapshot.fillerWordCount)
                put("wordsPerMinute", snapshot.wordsPerMinute)
                put("transcript", snapshot.transcript)
                put("updatedAt", System.currentTimeMillis())
            }
            runCatching { file.writeText(json.toString()) }
        }
    }

    private companion object {
        const val FILE_NAME = "session.json"
    }
}
