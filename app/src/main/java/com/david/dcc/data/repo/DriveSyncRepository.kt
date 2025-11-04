package com.david.dcc.data.repo

import android.content.Context
import com.david.dcc.data.db.AppDb
import com.david.dcc.data.model.CrateTrack
import com.david.dcc.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale

sealed interface DriveSyncResult {
    data class Success(val message: String, val trackCount: Int) : DriveSyncResult
    data class NotFound(val reason: String) : DriveSyncResult
    data class Failure(val throwable: Throwable) : DriveSyncResult
}

class DriveSyncRepository(private val context: Context, private val db: AppDb) {

    private val driveFolder: File by lazy {
        File(context.filesDir, "drive_sync").apply { mkdirs() }
    }

    suspend fun uploadCrate(crateId: Long): DriveSyncResult = withContext(Dispatchers.IO) {
        runCatching {
            val crate = db.crateDao().byId(crateId) ?: return@withContext DriveSyncResult.NotFound("Crate no encontrado")
            val tracks = db.crateDao().tracksInCrate(crateId)
            val payload = JSONObject().apply {
                put("crateId", crate.id)
                put("crateName", crate.name)
                put("generatedAt", System.currentTimeMillis())
                put("tracks", JSONArray().apply {
                    tracks.forEach { track ->
                        put(track.toJson())
                    }
                })
            }
            val file = File(driveFolder, "crate_${crate.id}.json")
            file.writeText(payload.toString(2))
            DriveSyncResult.Success("Backup creado en ${file.absolutePath}", tracks.size)
        }.getOrElse { error ->
            DriveSyncResult.Failure(error)
        }
    }

    suspend fun downloadCrate(crateId: Long): DriveSyncResult = withContext(Dispatchers.IO) {
        val file = File(driveFolder, "crate_${crateId}.json")
        if (!file.exists()) {
            return@withContext DriveSyncResult.NotFound("No existe copia en Drive local para el crate $crateId")
        }
        runCatching {
            val payload = JSONObject(file.readText())
            val tracksArray = payload.optJSONArray("tracks") ?: JSONArray()
            var imported = 0
            db.runInTransaction {
                for (i in 0 until tracksArray.length()) {
                    val entry = tracksArray.getJSONObject(i)
                    val title = entry.optString("title")
                    val artist = entry.optString("artist")
                    if (title.isBlank() && artist.isBlank()) continue
                    val existing = db.trackDao().findByTitleAndArtist(title, artist)
                    val track = Track(
                        id = existing?.id ?: 0,
                        title = title,
                        artist = artist,
                        bpm = entry.optDoubleOrNull("bpm"),
                        musicalKey = entry.optStringOrNull("key"),
                        genre = entry.optStringOrNull("genre"),
                        rating = entry.optIntOrNull("rating"),
                        energy = entry.optIntOrNull("energy"),
                        comment = entry.optStringOrNull("comment"),
                        path = entry.optStringOrNull("path"),
                        durationSeconds = entry.optIntOrNull("duration"),
                        colorHex = entry.optStringOrNull("color"),
                        coverArtUri = entry.optStringOrNull("coverArt"),
                    )
                    val newId = db.trackDao().upsert(track)
                    db.crateDao().addTrack(CrateTrack(crateId, newId))
                    imported++
                }
            }
            DriveSyncResult.Success("Sincronizados $imported temas desde Drive", imported)
        }.getOrElse { error ->
            DriveSyncResult.Failure(error)
        }
    }

    suspend fun availableSnapshots(): List<File> = withContext(Dispatchers.IO) {
        driveFolder.listFiles()?.sortedBy { it.name.lowercase(Locale.getDefault()) } ?: emptyList()
    }
}

private fun Track.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("title", title)
    put("artist", artist)
    putOpt("bpm", bpm)
    putOpt("key", musicalKey)
    putOpt("genre", genre)
    putOpt("rating", rating)
    putOpt("energy", energy)
    putOpt("comment", comment)
    putOpt("path", path)
    putOpt("duration", durationSeconds)
    putOpt("color", colorHex)
    putOpt("coverArt", coverArtUri)
}

private fun JSONObject.optDoubleOrNull(name: String): Double? =
    if (has(name) && !isNull(name)) getDouble(name) else null

private fun JSONObject.optStringOrNull(name: String): String? =
    optString(name).takeIf { it.isNotBlank() }

private fun JSONObject.optIntOrNull(name: String): Int? =
    if (has(name) && !isNull(name)) getInt(name) else null
