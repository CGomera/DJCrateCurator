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
    val path: String?,
    @ColumnInfo(defaultValue = "NULL") val durationSeconds: Int? = null,
    @ColumnInfo(defaultValue = "NULL") val colorHex: String? = null,
    @ColumnInfo(defaultValue = "NULL") val coverArtUri: String? = null
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

@Entity(tableName = "filter_presets")
data class FilterPreset(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val query: String?,
    val genre: String?,
    val artist: String?,
    val key: String?,
    val bpmMin: Float?,
    val bpmMax: Float?,
    val ratingMin: Int?,
    val ratingMax: Int?,
    val energyMin: Int?,
    val energyMax: Int?,
    @ColumnInfo(name = "tagIds") val tagIdsCsv: String?
)