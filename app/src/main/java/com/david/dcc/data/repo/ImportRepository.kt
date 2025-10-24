package com.david.dcc.data.repo

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.david.dcc.data.db.AppDb
import com.david.dcc.data.model.CrateTrack
import com.david.dcc.data.model.Track
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class ImportRepository(private val context: Context, private val db: AppDb) {
    suspend fun importCsvToCrate(uri: Uri, crateId: Long): Int = withContext(Dispatchers.IO) {
        var count = 0
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
            val headerLine = reader.readLine()?.trimStart('\uFEFF') ?: return@use
            val separator = if (headerLine.contains(';')) ';' else ','
            val headers = parseCsvLine(headerLine, separator).map { it.trim().lowercase(Locale.getDefault()) }
            fun indexOf(column: String): Int = headers.indexOfFirst { it.contains(column) }

            val iTitle = indexOf("title")
            val iArtist = indexOf("artist")
            val iBpm = indexOf("bpm")
            val iKey = indexOf("key")
            val iGenre = indexOf("genre")
            val iRating = indexOf("rating")
            val iComment = indexOf("comment")
            val iPath = listOf("location", "path", "filename").firstNotNullOfOrNull { name ->
                indexOf(name).takeIf { it >= 0 }
            } ?: -1

            db.withTransaction {
                reader.lineSequence().forEach { rawLine ->
                    if (rawLine.isBlank()) return@forEach
                    val parts = parseCsvLine(rawLine, separator)
                    val title = parts.getOrNull(iTitle)?.trim().orEmpty()
                    val artist = parts.getOrNull(iArtist)?.trim().orEmpty()
                    if (title.isEmpty() && artist.isEmpty()) return@forEach

                    val track = Track(
                        title = title,
                        artist = artist,
                        bpm = parts.getOrNull(iBpm)?.replace(',', '.')?.toDoubleOrNull(),
                        musicalKey = parts.getOrNull(iKey)?.trim()?.ifBlank { null },
                        genre = parts.getOrNull(iGenre)?.trim()?.ifBlank { null },
                        rating = parts.getOrNull(iRating)?.trim()?.toIntOrNull(),
                        energy = null,
                        comment = parts.getOrNull(iComment)?.trim()?.ifBlank { null },
                        path = parts.getOrNull(iPath)?.trim()?.ifBlank { null }
                    )
                    val id = db.trackDao().upsert(track)
                    db.crateDao().addTrack(CrateTrack(crateId, id))
                    count++
                }
            }
        }
            count
    }

        suspend fun exportCrateToCsv(uri: Uri, crateId: Long): Int = withContext(Dispatchers.IO) {
            if (db.crateDao().byId(crateId) == null) return@withContext 0
            val tracks = db.crateDao().tracksInCrate(crateId)
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                writer.appendLine("Title,Artist,BPM,Key,Genre,Rating,Energy,Comment,Path")
                tracks.forEach { track ->
                    val row = encodeCsvRow(
                        listOf(
                            track.title,
                            track.artist,
                            track.bpm?.let { String.format(Locale.US, "%.2f", it) } ?: "",
                            track.musicalKey.orEmpty(),
                            track.genre.orEmpty(),
                            track.rating?.toString() ?: "",
                            track.energy?.toString() ?: "",
                            track.comment.orEmpty(),
                            track.path.orEmpty()
                        )
                    )
                    writer.appendLine(row)
                }
            }
            tracks.size
        }


    suspend fun exportCrateToJson(uri: Uri, crateId: Long): Int = withContext(Dispatchers.IO) {
        val crate = db.crateDao().byId(crateId) ?: return@withContext 0
        val tracks = db.crateDao().tracksInCrate(crateId)
        val exports = JSONObject()

        fun buildTrackArray(format: String): JSONArray {
            val array = JSONArray()
            tracks.forEach { track ->
                val entry = JSONObject().apply {
                    put("title", track.title)
                    put("artist", track.artist)
                    putOpt("bpm", track.bpm)
                    putOpt("genre", track.genre)
                    putOpt("key", track.musicalKey)
                    putOpt("comment", track.comment)
                    putOpt("path", track.path)
                }

                when (format) {
                    "rekordbox" -> entry.putOpt("rating", track.rating)
                    "serato" -> entry.putOpt("energy", track.energy)
                    "traktor" -> entry.putOpt("cue", track.comment)
                }
                array.put(entry)
            }
            return array
        }
        exports.put("rekordbox", buildTrackArray("rekordbox"))
        exports.put("serato", buildTrackArray("serato"))
        exports.put("traktor", buildTrackArray("traktor"))

        val root = JSONObject().apply {
            put("crate", JSONObject().apply {
                put("id", crate.id)
                put("name", crate.name)
                put("tracks", tracks.size)
            })
            put("exports", exports)
        }

        context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
            writer.write(root.toString(2))
        }

        tracks.size
    }

    suspend fun exportSetlistToRekordboxCsv(setlistId: Long, uri: Uri): Int = withContext(Dispatchers.IO) {
        if (db.setlistDao().byId(setlistId) == null) return@withContext 0
        val entries = db.setlistDao().tracksForSetlist(setlistId)
        var count = 0

        context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
            writer.appendLine("Position,Title,Artist,BPM,Key,Comment")
            entries.forEach { entry ->
                val track = entry.track ?: return@forEach
                val row = encodeCsvRow(
                    listOf(
                        (entry.position + 1).toString(),
                        track.title,
                        track.artist,
                        track.bpm?.let { String.format(Locale.US, "%.2f", it) } ?: "",
                        track.musicalKey.orEmpty(),
                        track.comment.orEmpty()
                    )
                )
                writer.appendLine(row)
                count++
            }
        }

        count
    }

    private fun parseCsvLine(line: String, separator: Char): List<String> {
        val result = mutableListOf<String>()
        if (line.isEmpty()) return result

        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            when (val char = line[i]) {
                '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                        result += current.toString()
                        current.clear()
                    }

            else -> current.append(char)
            }
        i++
    }
    result += current.toString()
    return result
}

private fun encodeCsvRow(values: List<String>): String = values.joinToString(",") { value ->
    if (value.contains(',') || value.contains('"') || value.contains('\n')) {
        "\"" + value.replace("\"", "\"\"") + "\""
    } else {
        value
    }
}
}