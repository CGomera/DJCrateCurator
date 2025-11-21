package com.david.dcc.data.model

import java.util.Locale

/**
 * Represents the supported CSV formats when exporting a saved setlist so it can be
 * imported in DJ software like Rekordbox, Serato or Traktor.
 */
enum class SetlistExportFormat(
    val displayName: String,
    val fileSuffix: String,
) {
    REKORDBOX(
        displayName = "Rekordbox",
        fileSuffix = "rekordbox",
    ) {
        override fun headers(): List<String> = listOf("Position", "Title", "Artist", "BPM", "Key", "Comment")

        override fun rowFor(position: Int, track: ExportableTrack): List<String> = listOf(
            String.format(Locale.US, "%d", position + 1),
            track.title,
            track.artist,
            track.bpm?.let { String.format(Locale.US, "%.2f", it) } ?: "",
            track.key.orEmpty(),
            track.comment.orEmpty(),
        )
    },
    SERATO(
        displayName = "Serato",
        fileSuffix = "serato",
    ) {
        override fun headers(): List<String> = listOf("Track", "Title", "Artist", "BPM", "Key", "Comment")

        override fun rowFor(position: Int, track: ExportableTrack): List<String> = listOf(
            String.format(Locale.US, "%03d", position + 1),
            track.title,
            track.artist,
            track.bpm?.let { String.format(Locale.US, "%.2f", it) } ?: "",
            track.key.orEmpty(),
            track.comment.orEmpty(),
        )
    },
    TRAKTOR(
        displayName = "Traktor",
        fileSuffix = "traktor",
    ) {
        override fun headers(): List<String> = listOf("Order", "Title", "Artist", "BPM", "Key", "Notes")

        override fun rowFor(position: Int, track: ExportableTrack): List<String> = listOf(
            (position + 1).toString(),
            track.title,
            track.artist,
            track.bpm?.let { String.format(Locale.US, "%.2f", it) } ?: "",
            track.key.orEmpty(),
            track.comment.orEmpty(),
        )
    };

    abstract fun headers(): List<String>
    abstract fun rowFor(position: Int, track: ExportableTrack): List<String>

    data class ExportableTrack(
        val title: String,
        val artist: String,
        val bpm: Double?,
        val key: String?,
        val comment: String?,
    )
}