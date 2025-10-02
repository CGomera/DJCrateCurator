package com.david.dcc.data.model

import androidx.room.*

@Entity(tableName = "tracks")
data class Track(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val artist: String,
    val bpm: Double?,
    val musicalKey: String?,
    val genre: String?,
    val rating: Int?,
    val energy: Int?,
    val comment: String?,
    val path: String?
)

@Entity(tableName = "crates")
data class Crate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(
    tableName = "crate_tracks",
    primaryKeys = ["crateId", "trackId"],
    indices = [Index("trackId")],
    foreignKeys = [
        ForeignKey(entity = Crate::class, parentColumns = ["id"], childColumns = ["crateId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Track::class, parentColumns = ["id"], childColumns = ["trackId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class CrateTrack(
    val crateId: Long,
    val trackId: Long
)

@Entity(tableName = "setlists")
data class Setlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(
    tableName = "setlist_items",
    primaryKeys = ["setlistId", "position"],
    indices = [Index("trackId")],
    foreignKeys = [
        ForeignKey(entity = Setlist::class, parentColumns = ["id"], childColumns = ["setlistId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Track::class, parentColumns = ["id"], childColumns = ["trackId"], onDelete = ForeignKey.SET_NULL)
    ]
)
data class SetlistItem(
    val setlistId: Long,
    val position: Int,
    val trackId: Long?
)

@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(
    tableName = "track_tags",
    primaryKeys = ["trackId", "tagId"],
    foreignKeys = [
        ForeignKey(entity = Track::class, parentColumns = ["id"], childColumns = ["trackId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Tag::class, parentColumns = ["id"], childColumns = ["tagId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class TrackTag(
    val trackId: Long,
    val tagId: Long
)
