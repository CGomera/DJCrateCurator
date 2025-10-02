package com.david.dcc.data.repo

import android.content.Context
import android.net.Uri
import com.david.dcc.data.db.AppDb
import com.david.dcc.data.model.CrateTrack
import com.david.dcc.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImportRepository(private val context: Context, private val db: AppDb) {
    suspend fun importCsvToCrate(uri: Uri, crateId: Long): Int = withContext(Dispatchers.IO) {
        var count = 0
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { br ->
            val header = br.readLine() ?: return@use
            val sep = if (header.contains(';')) ';' else ','
            val cols = header.split(sep).map { it.trim().lowercase() }
            fun idx(name: String) = cols.indexOfFirst { it.contains(name) }
            val iTitle = idx("title"); val iArtist = idx("artist"); val iBpm = idx("bpm")
            val iKey = idx("key"); val iGenre = idx("genre"); val iRating = idx("rating")
            val iComment = idx("comment"); val iPath = listOf("location","path","filename").firstNotNullOfOrNull { n ->
                val x = idx(n); if (x>=0) x else null
            } ?: -1

            br.lineSequence().forEach { line ->
                if (line.isBlank()) return@forEach
                val parts = line.split(sep)
                val track = Track(
                    title = parts.getOrNull(iTitle)?.trim().orEmpty(),
                    artist = parts.getOrNull(iArtist)?.trim().orEmpty(),
                    bpm = parts.getOrNull(iBpm)?.replace(",",".")?.toDoubleOrNull(),
                    musicalKey = parts.getOrNull(iKey)?.trim(),
                    genre = parts.getOrNull(iGenre)?.trim(),
                    rating = parts.getOrNull(iRating)?.toIntOrNull(),
                    energy = null,
                    comment = parts.getOrNull(iComment)?.trim(),
                    path = parts.getOrNull(iPath)?.trim()
                )
                val id = db.trackDao().upsert(track)
                db.crateDao().addTrack(CrateTrack(crateId, id))
                count++
            }
        }
        count
    }
}
