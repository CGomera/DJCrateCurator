package com.david.dcc.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.david.dcc.data.dao.CrateDao
import com.david.dcc.data.dao.FilterPresetDao
import com.david.dcc.data.dao.SetlistDao
import com.david.dcc.data.dao.TagDao
import com.david.dcc.data.dao.TrackDao
import com.david.dcc.data.model.*

@Database(
    entities = [Track::class, Crate::class, CrateTrack::class, Setlist::class, SetlistItem::class, Tag::class, TrackTag::class, FilterPreset::class],
    version = 4,
    exportSchema = false
)
abstract class AppDb : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun crateDao(): CrateDao
    abstract fun setlistDao(): SetlistDao
    abstract fun tagDao(): TagDao
    abstract fun filterPresetDao(): FilterPresetDao


    companion object {
        @Volatile private var INSTANCE: AppDb? = null
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tracks ADD COLUMN durationSeconds INTEGER")
                db.execSQL("ALTER TABLE tracks ADD COLUMN colorHex TEXT")
                db.execSQL("ALTER TABLE tracks ADD COLUMN coverArtUri TEXT")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS filter_presets (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "name TEXT NOT NULL, " +
                            "query TEXT, " +
                            "genre TEXT, " +
                            "artist TEXT, " +
                            "key TEXT, " +
                            "bpmMin REAL, " +
                            "bpmMax REAL, " +
                            "ratingMin INTEGER, " +
                            "ratingMax INTEGER, " +
                            "energyMin INTEGER, " +
                            "energyMax INTEGER, " +
                            "tagIds TEXT"
                            + ")"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE crates ADD COLUMN colorCategory TEXT")
            }
        }


        fun get(context: Context): AppDb = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, AppDb::class.java, "dcc.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
                .also { INSTANCE = it }
        }
    }
}