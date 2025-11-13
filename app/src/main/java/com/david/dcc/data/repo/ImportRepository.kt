package com.david.dcc.data.repo

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.david.dcc.data.db.AppDb
import com.david.dcc.data.model.CrateTrack
import com.david.dcc.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import com.david.dcc.data.model.TechnoStyle


class ImportRepository(private val context: Context, private val db: AppDb) {

    suspend fun importCsvToCrate(uri: Uri, crateId: Long): Int = withContext(Dispatchers.IO) {
        var imported = 0
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
            val headerLine = reader.readLine()?.trimStart('\uFEFF') ?: return@use
            val separator = if (';' in headerLine) ';' else ','
            val headers = parseCsvLine(headerLine, separator).map { it.trim().lowercase(Locale.getDefault()) }

            fun indexOf(column: String): Int = headers.indexOfFirst { it.contains(column) }

            val iTitle = indexOf("title")
            val iArtist = indexOf("artist")
            val iBpm = indexOf("bpm")
            val iKey = indexOf("key")
            val iGenre = indexOf("genre")
            val iRating = indexOf("rating")
            val iComment = indexOf("comment")
            val iPath = listOf("location", "path", "filename")
                .firstNotNullOfOrNull { name -> indexOf(name).takeIf { it >= 0 } }
                ?: -1

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
                        path = parts.getOrNull(iPath)?.trim()?.ifBlank { null },
                    )
                    val trackId = db.trackDao().upsert(track)
                    db.crateDao().addTrack(CrateTrack(crateId, trackId))
                    imported++
                }
            }
        }
        imported
    }

    suspend fun exportCrateToCsv(uri: Uri, crateId: Long): Int = withContext(Dispatchers.IO) {
        val crate = db.crateDao().byId(crateId) ?: return@withContext 0
        val tracks = db.crateDao().tracksInCrate(crateId)
        var exported = 0
        context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
            writer.write("Title,Artist,BPM,Key,Genre,Rating,Energy,Comment,Path")
            writer.newLine()
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
                        track.path.orEmpty(),
                    ),
                )
                writer.write(row)
                writer.newLine()
                exported++
            }
        }
        exported
    }

    suspend fun exportCrateToJson(uri: Uri, crateId: Long): Int = withContext(Dispatchers.IO) {
        val crate = db.crateDao().byId(crateId) ?: return@withContext 0
        val tracks = db.crateDao().tracksInCrate(crateId)
        val style = TechnoStyle.fromId(crate.colorCategory)

        val json = buildString {
            append("{\n")
            append("  \"crate\": {\n")
            append("    \"id\": ${crate.id},\n")
            append("    \"name\": ${jsonString(crate.name)},\n")
            crate.colorCategory?.let {
                append("    \"style\": ${jsonString(it)},\n")
            }
            style?.let {
                append("    \"styleName\": ${jsonString(it.displayName)},\n")
            }
            append("    \"tracks\": ${tracks.size}\n")
            append("  },\n")
            append("  \"exports\": {\n")
            append("    \"rekordbox\": ${buildTrackArrayJson(tracks, ExportFormat.REKORDBOX)},\n")
            append("    \"serato\": ${buildTrackArrayJson(tracks, ExportFormat.SERATO)},\n")
            append("    \"traktor\": ${buildTrackArrayJson(tracks, ExportFormat.TRAKTOR)}\n")
            append("  }\n")
            append('}')
        }

        context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
            writer.write(json)
        }

        tracks.size
    }

    suspend fun exportSetlistToRekordboxCsv(setlistId: Long, uri: Uri): Int = withContext(Dispatchers.IO) {
        val setlist = db.setlistDao().byId(setlistId) ?: return@withContext 0
        val entries = db.setlistDao().tracksForSetlist(setlist.id)
        var exported = 0

        context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
            writer.write("Position,Title,Artist,BPM,Key,Comment")
            writer.newLine()
            entries.forEach { entry ->
                val track = entry.track ?: return@forEach
                val row = encodeCsvRow(
                    listOf(
                        (entry.position + 1).toString(),
                        track.title,
                        track.artist,
                        track.bpm?.let { String.format(Locale.US, "%.2f", it) } ?: "",
                        track.musicalKey.orEmpty(),
                        track.comment.orEmpty(),
                    ),
                )
                writer.write(row)
                writer.newLine()
                exported++
            }
        }

            exported
        }

            private fun parseCsvLine(line: String, separator: Char): List<String> {
                if (line.isEmpty()) return emptyList()
                val values = mutableListOf<String>()
                val current = StringBuilder()
                var inQuotes = false
                var index = 0
                while (index < line.length) {
                    when (val ch = line[index]) {
                        '"' -> {
                            if (inQuotes && index + 1 < line.length && line[index + 1] == '"') {
                                current.append('"')
                                index++
                            } else {
                                inQuotes = !inQuotes
                            }

                        }
                            separator -> {
                            if (inQuotes) {
                                current.append(ch)
                            } else {
                                values += current.toString()
                                current.clear()
                            }

                        }

                        else -> current.append(ch)
                    }
                        index++
                    }
                    values += current.toString()
                    return values
                }

                private fun encodeCsvRow(values: List<String>): String = values.joinToString(",") { value ->
                    if (value.contains(',') || value.contains('"') || value.contains('\n')) {
                        val escaped = value.replace("\"", "\"\"")
                        "\"$escaped\""
                    } else {
                        value
                    }
                }

                private fun buildTrackArrayJson(tracks: List<Track>, format: ExportFormat): String {
                    if (tracks.isEmpty()) return "[]"
                    val builder = StringBuilder()
                    builder.append('[')
                    tracks.forEachIndexed { index, track ->
                        if (index > 0) builder.append(',')
                        builder.append('{')
                        builder.append("\"title\": ${jsonString(track.title)}")
                        builder.append(",\"artist\": ${jsonString(track.artist)}")
                        track.bpm?.let { builder.append(",\"bpm\": ${String.format(Locale.US, "%.2f", it)}") }
                        track.genre?.let { builder.append(",\"genre\": ${jsonString(it)}") }
                        track.musicalKey?.let { builder.append(",\"key\": ${jsonString(it)}") }
                        track.comment?.let { builder.append(",\"comment\": ${jsonString(it)}") }
                        track.path?.let { builder.append(",\"path\": ${jsonString(it)}") }
                        when (format) {
                            ExportFormat.REKORDBOX -> track.rating?.let { builder.append(",\"rating\": $it") }
                            ExportFormat.SERATO -> track.energy?.let { builder.append(",\"energy\": $it") }
                            ExportFormat.TRAKTOR -> track.comment?.let { builder.append(",\"cue\": ${jsonString(it)}") }
                        }
                        builder.append('}')
                    }
                    builder.append(']')
                    return builder.toString()
                }

                private fun jsonString(value: String): String {
                    val escaped = value
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
                        .replace("\t", "\\t")
                    return "\"$escaped\""
                }

                private enum class ExportFormat { REKORDBOX, SERATO, TRAKTOR }
            }