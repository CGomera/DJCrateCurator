package com.david.dcc.data.db;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.david.dcc.data.dao.CrateDao;
import com.david.dcc.data.dao.CrateDao_Impl;
import com.david.dcc.data.dao.TrackDao;
import com.david.dcc.data.dao.TrackDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDb_Impl extends AppDb {
  private volatile TrackDao _trackDao;

  private volatile CrateDao _crateDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `tracks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `artist` TEXT NOT NULL, `bpm` REAL, `musicalKey` TEXT, `genre` TEXT, `rating` INTEGER, `energy` INTEGER, `comment` TEXT, `path` TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `crates` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `crate_tracks` (`crateId` INTEGER NOT NULL, `trackId` INTEGER NOT NULL, PRIMARY KEY(`crateId`, `trackId`), FOREIGN KEY(`crateId`) REFERENCES `crates`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`trackId`) REFERENCES `tracks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_crate_tracks_trackId` ON `crate_tracks` (`trackId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `setlists` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `setlist_items` (`setlistId` INTEGER NOT NULL, `position` INTEGER NOT NULL, `trackId` INTEGER, PRIMARY KEY(`setlistId`, `position`), FOREIGN KEY(`setlistId`) REFERENCES `setlists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`trackId`) REFERENCES `tracks`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_setlist_items_trackId` ON `setlist_items` (`trackId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `tags` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `track_tags` (`trackId` INTEGER NOT NULL, `tagId` INTEGER NOT NULL, PRIMARY KEY(`trackId`, `tagId`), FOREIGN KEY(`trackId`) REFERENCES `tracks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '35366a4f3fd7a873a1c39156864a414a')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `tracks`");
        db.execSQL("DROP TABLE IF EXISTS `crates`");
        db.execSQL("DROP TABLE IF EXISTS `crate_tracks`");
        db.execSQL("DROP TABLE IF EXISTS `setlists`");
        db.execSQL("DROP TABLE IF EXISTS `setlist_items`");
        db.execSQL("DROP TABLE IF EXISTS `tags`");
        db.execSQL("DROP TABLE IF EXISTS `track_tags`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsTracks = new HashMap<String, TableInfo.Column>(10);
        _columnsTracks.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTracks.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTracks.put("artist", new TableInfo.Column("artist", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTracks.put("bpm", new TableInfo.Column("bpm", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTracks.put("musicalKey", new TableInfo.Column("musicalKey", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTracks.put("genre", new TableInfo.Column("genre", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTracks.put("rating", new TableInfo.Column("rating", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTracks.put("energy", new TableInfo.Column("energy", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTracks.put("comment", new TableInfo.Column("comment", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTracks.put("path", new TableInfo.Column("path", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysTracks = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesTracks = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoTracks = new TableInfo("tracks", _columnsTracks, _foreignKeysTracks, _indicesTracks);
        final TableInfo _existingTracks = TableInfo.read(db, "tracks");
        if (!_infoTracks.equals(_existingTracks)) {
          return new RoomOpenHelper.ValidationResult(false, "tracks(com.david.dcc.data.model.Track).\n"
                  + " Expected:\n" + _infoTracks + "\n"
                  + " Found:\n" + _existingTracks);
        }
        final HashMap<String, TableInfo.Column> _columnsCrates = new HashMap<String, TableInfo.Column>(2);
        _columnsCrates.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCrates.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCrates = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCrates = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCrates = new TableInfo("crates", _columnsCrates, _foreignKeysCrates, _indicesCrates);
        final TableInfo _existingCrates = TableInfo.read(db, "crates");
        if (!_infoCrates.equals(_existingCrates)) {
          return new RoomOpenHelper.ValidationResult(false, "crates(com.david.dcc.data.model.Crate).\n"
                  + " Expected:\n" + _infoCrates + "\n"
                  + " Found:\n" + _existingCrates);
        }
        final HashMap<String, TableInfo.Column> _columnsCrateTracks = new HashMap<String, TableInfo.Column>(2);
        _columnsCrateTracks.put("crateId", new TableInfo.Column("crateId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCrateTracks.put("trackId", new TableInfo.Column("trackId", "INTEGER", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCrateTracks = new HashSet<TableInfo.ForeignKey>(2);
        _foreignKeysCrateTracks.add(new TableInfo.ForeignKey("crates", "CASCADE", "NO ACTION", Arrays.asList("crateId"), Arrays.asList("id")));
        _foreignKeysCrateTracks.add(new TableInfo.ForeignKey("tracks", "CASCADE", "NO ACTION", Arrays.asList("trackId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesCrateTracks = new HashSet<TableInfo.Index>(1);
        _indicesCrateTracks.add(new TableInfo.Index("index_crate_tracks_trackId", false, Arrays.asList("trackId"), Arrays.asList("ASC")));
        final TableInfo _infoCrateTracks = new TableInfo("crate_tracks", _columnsCrateTracks, _foreignKeysCrateTracks, _indicesCrateTracks);
        final TableInfo _existingCrateTracks = TableInfo.read(db, "crate_tracks");
        if (!_infoCrateTracks.equals(_existingCrateTracks)) {
          return new RoomOpenHelper.ValidationResult(false, "crate_tracks(com.david.dcc.data.model.CrateTrack).\n"
                  + " Expected:\n" + _infoCrateTracks + "\n"
                  + " Found:\n" + _existingCrateTracks);
        }
        final HashMap<String, TableInfo.Column> _columnsSetlists = new HashMap<String, TableInfo.Column>(2);
        _columnsSetlists.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSetlists.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSetlists = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSetlists = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSetlists = new TableInfo("setlists", _columnsSetlists, _foreignKeysSetlists, _indicesSetlists);
        final TableInfo _existingSetlists = TableInfo.read(db, "setlists");
        if (!_infoSetlists.equals(_existingSetlists)) {
          return new RoomOpenHelper.ValidationResult(false, "setlists(com.david.dcc.data.model.Setlist).\n"
                  + " Expected:\n" + _infoSetlists + "\n"
                  + " Found:\n" + _existingSetlists);
        }
        final HashMap<String, TableInfo.Column> _columnsSetlistItems = new HashMap<String, TableInfo.Column>(3);
        _columnsSetlistItems.put("setlistId", new TableInfo.Column("setlistId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSetlistItems.put("position", new TableInfo.Column("position", "INTEGER", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSetlistItems.put("trackId", new TableInfo.Column("trackId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSetlistItems = new HashSet<TableInfo.ForeignKey>(2);
        _foreignKeysSetlistItems.add(new TableInfo.ForeignKey("setlists", "CASCADE", "NO ACTION", Arrays.asList("setlistId"), Arrays.asList("id")));
        _foreignKeysSetlistItems.add(new TableInfo.ForeignKey("tracks", "SET NULL", "NO ACTION", Arrays.asList("trackId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesSetlistItems = new HashSet<TableInfo.Index>(1);
        _indicesSetlistItems.add(new TableInfo.Index("index_setlist_items_trackId", false, Arrays.asList("trackId"), Arrays.asList("ASC")));
        final TableInfo _infoSetlistItems = new TableInfo("setlist_items", _columnsSetlistItems, _foreignKeysSetlistItems, _indicesSetlistItems);
        final TableInfo _existingSetlistItems = TableInfo.read(db, "setlist_items");
        if (!_infoSetlistItems.equals(_existingSetlistItems)) {
          return new RoomOpenHelper.ValidationResult(false, "setlist_items(com.david.dcc.data.model.SetlistItem).\n"
                  + " Expected:\n" + _infoSetlistItems + "\n"
                  + " Found:\n" + _existingSetlistItems);
        }
        final HashMap<String, TableInfo.Column> _columnsTags = new HashMap<String, TableInfo.Column>(2);
        _columnsTags.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTags.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysTags = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesTags = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoTags = new TableInfo("tags", _columnsTags, _foreignKeysTags, _indicesTags);
        final TableInfo _existingTags = TableInfo.read(db, "tags");
        if (!_infoTags.equals(_existingTags)) {
          return new RoomOpenHelper.ValidationResult(false, "tags(com.david.dcc.data.model.Tag).\n"
                  + " Expected:\n" + _infoTags + "\n"
                  + " Found:\n" + _existingTags);
        }
        final HashMap<String, TableInfo.Column> _columnsTrackTags = new HashMap<String, TableInfo.Column>(2);
        _columnsTrackTags.put("trackId", new TableInfo.Column("trackId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrackTags.put("tagId", new TableInfo.Column("tagId", "INTEGER", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysTrackTags = new HashSet<TableInfo.ForeignKey>(2);
        _foreignKeysTrackTags.add(new TableInfo.ForeignKey("tracks", "CASCADE", "NO ACTION", Arrays.asList("trackId"), Arrays.asList("id")));
        _foreignKeysTrackTags.add(new TableInfo.ForeignKey("tags", "CASCADE", "NO ACTION", Arrays.asList("tagId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesTrackTags = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoTrackTags = new TableInfo("track_tags", _columnsTrackTags, _foreignKeysTrackTags, _indicesTrackTags);
        final TableInfo _existingTrackTags = TableInfo.read(db, "track_tags");
        if (!_infoTrackTags.equals(_existingTrackTags)) {
          return new RoomOpenHelper.ValidationResult(false, "track_tags(com.david.dcc.data.model.TrackTag).\n"
                  + " Expected:\n" + _infoTrackTags + "\n"
                  + " Found:\n" + _existingTrackTags);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "35366a4f3fd7a873a1c39156864a414a", "b1c92ab5ca7fea96b153f725ce775935");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "tracks","crates","crate_tracks","setlists","setlist_items","tags","track_tags");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `tracks`");
      _db.execSQL("DELETE FROM `crates`");
      _db.execSQL("DELETE FROM `crate_tracks`");
      _db.execSQL("DELETE FROM `setlists`");
      _db.execSQL("DELETE FROM `setlist_items`");
      _db.execSQL("DELETE FROM `tags`");
      _db.execSQL("DELETE FROM `track_tags`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(TrackDao.class, TrackDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(CrateDao.class, CrateDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public TrackDao trackDao() {
    if (_trackDao != null) {
      return _trackDao;
    } else {
      synchronized(this) {
        if(_trackDao == null) {
          _trackDao = new TrackDao_Impl(this);
        }
        return _trackDao;
      }
    }
  }

  @Override
  public CrateDao crateDao() {
    if (_crateDao != null) {
      return _crateDao;
    } else {
      synchronized(this) {
        if(_crateDao == null) {
          _crateDao = new CrateDao_Impl(this);
        }
        return _crateDao;
      }
    }
  }
}
