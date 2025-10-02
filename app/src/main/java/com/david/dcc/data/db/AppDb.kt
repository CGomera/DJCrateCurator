package com.david.dcc.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.david.dcc.data.dao.CrateDao
import com.david.dcc.data.dao.TrackDao
import com.david.dcc.data.model.*

@Database(
    entities = [Track::class, Crate::class, CrateTrack::class, Setlist::class, SetlistItem::class, Tag::class, TrackTag::class],
    version = 1,
    exportSchema = false
)
abstract class AppDb : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun crateDao(): CrateDao

    companion object {
        @Volatile private var INSTANCE: AppDb? = null
        fun get(context: Context): AppDb = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, AppDb::class.java, "dcc.db").build().also { INSTANCE = it }
        }
    }
}
