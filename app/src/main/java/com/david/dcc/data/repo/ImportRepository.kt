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

class ImportRepository(private val context: Context, private val db: AppDb) {
    suspend fun importCsvToCrate(uri: Uri, crateId: Long): Int = withContext(Dispatchers.IO) {
        var count = 0
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { br ->

            val headerLine = br.readLine()?.trimStart('\uFEFF') ?: return@use
            val sep = if (headerLine.contains(';')) ';' else ','
            val cols = parseCsvLine(headerLine, sep).map { it.trim().lowercase() }
            fun idx(name: String) = cols.indexOfFirst { it.contains(name) }
            val iTitle = idx("title"); val iArtist = idx("artist"); val iBpm = idx("bpm")
            val iKey = idx("key"); val iGenre = idx("genre"); val iRating = idx("rating")
            val iComment = idx("comment")
            val iPath = listOf("location", "path", "filename").firstNotNullOfOrNull { column ->
                idx(column).takeIf { it >= 0 }
            } ?: -1

                db.withTransaction {
                    br.lineSequence().forEach { rawLine ->
                        if (rawLine.isBlank()) return@forEach
                        val parts = parseCsvLine(rawLine, sep)
                        val title = parts.getOrNull(iTitle)?.trim().orEmpty()
                        val artist = parts.getOrNull(iArtist)?.trim().orEmpty()
                        if (title.isEmpty() && artist.isEmpty()) return@forEach

                        val track = Track(
                            title = title,
                            artist = artist,
                            bpm = parts.getOrNull(iBpm)?.replace(",", ".")?.toDoubleOrNull(),
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
                val crate = db.crateDao().byId(crateId) ?: return@withContext 0
                val tracks = db.crateDao().tracksInCrate(crateId)
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                    writer.appendLine("Title,Artist,BPM,Key,Genre,Rating,Energy,Comment,Path")
                    tracks.forEach { track ->
                        val line = listOf(
                            track.title,
                            track.artist,
                            track.bpm?.let { String.format("%.2f", it) } ?: "",
                            track.musicalKey.orEmpty(),
                            track.genre.orEmpty(),
                            track.rating?.toString() ?: "",
                            track.energy?.toString() ?: "",
                            track.comment.orEmpty(),
                            track.path.orEmpty()
                        ).joinToString(",") { part ->
                            if (part.contains(",") || part.contains('"')) {
                                "\"" + part.replace("\"", "\"\"") + "\""
                            } else {
                                part
                            }
                        }
                        writer.appendLine(line)
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
        }

        private fun parseCsvLine(line: String, separator: Char): List<String> {
            val result = mutableListOf<String>()
            if (line.isEmpty()) return result

            val current = StringBuilder()
            var inQuotes = false
            var i = 0
            while (i < line.length) {
                val char = line[i]
                when {
                    char == '"' -> {
                        if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                            current.append('"')
                            i++
                        } else {
                            inQuotes = !inQuotes
                        }
                    }
                    char == separator && !inQuotes -> {
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