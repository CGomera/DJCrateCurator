package com.david.dcc.data.dao

import androidx.room.*
import com.david.dcc.data.model.*

@Dao
interface TrackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(track: Track): Long

    @Update
    suspend fun update(track: Track)

    @Query("SELECT * FROM tracks WHERE id=:id")
    suspend fun byId(id: Long): Track?

    @Query("SELECT * FROM tracks WHERE title LIKE :q OR artist LIKE :q ORDER BY title")
    suspend fun search(q: String): List<Track>

    @Query("SELECT * FROM tracks ORDER BY title")
    suspend fun all(): List<Track>

    @Query("SELECT * FROM tracks WHERE LOWER(title) = LOWER(:title) AND LOWER(artist) = LOWER(:artist) LIMIT 1")
    suspend fun findByTitleAndArtist(title: String, artist: String): Track?

    @Query("UPDATE tracks SET colorHex = :colorHex WHERE id = :trackId")
    suspend fun updateColor(trackId: Long, colorHex: String?)

    @Query("UPDATE tracks SET energy = :energy WHERE id = :trackId")
    suspend fun updateEnergy(trackId: Long, energy: Int?)

    @Query("UPDATE tracks SET rating = :rating WHERE id = :trackId")
    suspend fun updateRating(trackId: Long, rating: Int?)
}

@Dao
interface CrateDao {
    @Insert suspend fun insert(crate: Crate): Long
    @Delete suspend fun delete(crate: Crate)
    @Query("SELECT * FROM crates ORDER BY name")
    suspend fun all(): List<Crate>
    @Query("SELECT c.*, COUNT(ct.trackId) AS trackCount FROM crates c LEFT JOIN crate_tracks ct ON c.id = ct.crateId GROUP BY c.id ORDER BY c.name")
    suspend fun allWithCounts(): List<CrateWithTrackCount>
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTrack(link: CrateTrack)
    @Transaction
    @Query("SELECT t.* FROM tracks t JOIN crate_tracks ct ON t.id=ct.trackId WHERE ct.crateId=:crateId ORDER BY t.title")
    suspend fun tracksInCrate(crateId: Long): List<Track>
    @Query("SELECT * FROM crates WHERE id=:crateId LIMIT 1")
    suspend fun byId(crateId: Long): Crate?
}

@Dao
interface SetlistDao {
    @Insert
    suspend fun insert(setlist: Setlist): Long
    fun byId(setlistId: Long)
    fun tracksForSetlist(setlistId: Long)

    //@Query("SELECT * FROM setlists WHERE id = :setlistId LIMIT 1")

}
    @Dao
    interface TagDao {
        @Query("SELECT * FROM tags ORDER BY name")
        suspend fun all(): List<Tag>

        @Query("SELECT * FROM tags WHERE LOWER(name) = LOWER(:name) LIMIT 1")
        suspend fun findByName(name: String): Tag?

        @Insert(onConflict = OnConflictStrategy.IGNORE)
        suspend fun insert(tag: Tag): Long

        @Insert(onConflict = OnConflictStrategy.IGNORE)
        suspend fun insertTrackTag(link: TrackTag)

        @Query("DELETE FROM track_tags WHERE trackId = :trackId AND tagId = :tagId")
        suspend fun deleteTrackTag(trackId: Long, tagId: Long)

        @Query(
            "SELECT tt.trackId AS trackId, t.id AS id, t.name AS name " +
                    "FROM track_tags tt INNER JOIN tags t ON t.id = tt.tagId " +
                    "WHERE tt.trackId IN (:trackIds)"
        )
        suspend fun tagsForTrackIds(trackIds: List<Long>): List<TrackTagAssignment>
    }

    @Dao
    interface FilterPresetDao {
        @Query("SELECT * FROM filter_presets ORDER BY name")
        suspend fun all(): List<FilterPreset>

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        suspend fun insert(preset: FilterPreset): Long

        @Query("DELETE FROM filter_presets WHERE id = :presetId")
        suspend fun delete(presetId: Long)
    }