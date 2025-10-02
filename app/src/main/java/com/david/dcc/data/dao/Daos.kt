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
}

@Dao
interface CrateDao {
    @Insert suspend fun insert(crate: Crate): Long
    @Delete suspend fun delete(crate: Crate)
    @Query("SELECT * FROM crates ORDER BY name")
    suspend fun all(): List<Crate>
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTrack(link: CrateTrack)
    @Transaction
    @Query("SELECT t.* FROM tracks t JOIN crate_tracks ct ON t.id=ct.trackId WHERE ct.crateId=:crateId ORDER BY t.title")
    suspend fun tracksInCrate(crateId: Long): List<Track>
}
