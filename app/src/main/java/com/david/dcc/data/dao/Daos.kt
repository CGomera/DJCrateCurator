package com.david.dcc.data.dao

import androidx.room.*
import com.david.dcc.data.model.*

@Dao
interface TrackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(track: Track): Long

    @Query("SELECT * FROM tracks WHERE id=:id")
    suspend fun byId(id: Long): Track?

    @Query("SELECT * FROM tracks WHERE title LIKE :q OR artist LIKE :q ORDER BY title")
    suspend fun search(q: String): List<Track>

    @Query("SELECT * FROM tracks ORDER BY title")
    suspend fun all(): List<Track>
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

    @Query("SELECT * FROM setlists WHERE id = :setlistId LIMIT 1")
    suspend fun byId(setlistId: Long): Setlist?

    @Query("SELECT s.*, COUNT(si.trackId) AS trackCount FROM setlists s LEFT JOIN setlist_items si ON s.id = si.setlistId GROUP BY s.id ORDER BY s.name")
    suspend fun allWithCounts(): List<SetlistWithCount>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItem(item: SetlistItem)

    @Query("DELETE FROM setlist_items WHERE setlistId=:setlistId")
    suspend fun clearItems(setlistId: Long)

    @Query("SELECT si.position, t.* FROM setlist_items si LEFT JOIN tracks t ON si.trackId = t.id WHERE si.setlistId = :setlistId ORDER BY si.position")
    suspend fun tracksForSetlist(setlistId: Long): List<SetlistTrackView>
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