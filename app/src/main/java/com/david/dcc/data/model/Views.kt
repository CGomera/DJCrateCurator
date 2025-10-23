package com.david.dcc.data.model

import androidx.room.ColumnInfo
import androidx.room.Embedded

/**
 * Projection helpers for richer Room queries.
 */
data class CrateWithTrackCount(
    @Embedded val crate: Crate,
    @ColumnInfo(name = "trackCount") val trackCount: Int
)

data class SetlistWithCount(
    @Embedded val setlist: Setlist,
    @ColumnInfo(name = "trackCount") val trackCount: Int
)

data class SetlistTrackView(
    @ColumnInfo(name = "position") val position: Int,
    @Embedded val track: Track?
)

data class TrackTagAssignment(
    @ColumnInfo(name = "trackId") val trackId: Long,
    @Embedded val tag: Tag
)