package com.david.dcc.data.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.david.dcc.data.model.Crate;
import com.david.dcc.data.model.CrateTrack;
import com.david.dcc.data.model.Track;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class CrateDao_Impl implements CrateDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Crate> __insertionAdapterOfCrate;

  private final EntityInsertionAdapter<CrateTrack> __insertionAdapterOfCrateTrack;

  private final EntityDeletionOrUpdateAdapter<Crate> __deletionAdapterOfCrate;

  public CrateDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCrate = new EntityInsertionAdapter<Crate>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `crates` (`id`,`name`) VALUES (nullif(?, 0),?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Crate entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
      }
    };
    this.__insertionAdapterOfCrateTrack = new EntityInsertionAdapter<CrateTrack>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR IGNORE INTO `crate_tracks` (`crateId`,`trackId`) VALUES (?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CrateTrack entity) {
        statement.bindLong(1, entity.getCrateId());
        statement.bindLong(2, entity.getTrackId());
      }
    };
    this.__deletionAdapterOfCrate = new EntityDeletionOrUpdateAdapter<Crate>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `crates` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Crate entity) {
        statement.bindLong(1, entity.getId());
      }
    };
  }

  @Override
  public Object insert(final Crate crate, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfCrate.insertAndReturnId(crate);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object addTrack(final CrateTrack link, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCrateTrack.insert(link);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final Crate crate, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfCrate.handle(crate);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object all(final Continuation<? super List<Crate>> $completion) {
    final String _sql = "SELECT * FROM crates ORDER BY name";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Crate>>() {
      @Override
      @NonNull
      public List<Crate> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final List<Crate> _result = new ArrayList<Crate>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Crate _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            _item = new Crate(_tmpId,_tmpName);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object tracksInCrate(final long crateId,
      final Continuation<? super List<Track>> $completion) {
    final String _sql = "SELECT t.* FROM tracks t JOIN crate_tracks ct ON t.id=ct.trackId WHERE ct.crateId=? ORDER BY t.title";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, crateId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, true, _cancellationSignal, new Callable<List<Track>>() {
      @Override
      @NonNull
      public List<Track> call() throws Exception {
        __db.beginTransaction();
        try {
          final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
          try {
            final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
            final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
            final int _cursorIndexOfArtist = CursorUtil.getColumnIndexOrThrow(_cursor, "artist");
            final int _cursorIndexOfBpm = CursorUtil.getColumnIndexOrThrow(_cursor, "bpm");
            final int _cursorIndexOfMusicalKey = CursorUtil.getColumnIndexOrThrow(_cursor, "musicalKey");
            final int _cursorIndexOfGenre = CursorUtil.getColumnIndexOrThrow(_cursor, "genre");
            final int _cursorIndexOfRating = CursorUtil.getColumnIndexOrThrow(_cursor, "rating");
            final int _cursorIndexOfEnergy = CursorUtil.getColumnIndexOrThrow(_cursor, "energy");
            final int _cursorIndexOfComment = CursorUtil.getColumnIndexOrThrow(_cursor, "comment");
            final int _cursorIndexOfPath = CursorUtil.getColumnIndexOrThrow(_cursor, "path");
            final List<Track> _result = new ArrayList<Track>(_cursor.getCount());
            while (_cursor.moveToNext()) {
              final Track _item;
              final long _tmpId;
              _tmpId = _cursor.getLong(_cursorIndexOfId);
              final String _tmpTitle;
              _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
              final String _tmpArtist;
              _tmpArtist = _cursor.getString(_cursorIndexOfArtist);
              final Double _tmpBpm;
              if (_cursor.isNull(_cursorIndexOfBpm)) {
                _tmpBpm = null;
              } else {
                _tmpBpm = _cursor.getDouble(_cursorIndexOfBpm);
              }
              final String _tmpMusicalKey;
              if (_cursor.isNull(_cursorIndexOfMusicalKey)) {
                _tmpMusicalKey = null;
              } else {
                _tmpMusicalKey = _cursor.getString(_cursorIndexOfMusicalKey);
              }
              final String _tmpGenre;
              if (_cursor.isNull(_cursorIndexOfGenre)) {
                _tmpGenre = null;
              } else {
                _tmpGenre = _cursor.getString(_cursorIndexOfGenre);
              }
              final Integer _tmpRating;
              if (_cursor.isNull(_cursorIndexOfRating)) {
                _tmpRating = null;
              } else {
                _tmpRating = _cursor.getInt(_cursorIndexOfRating);
              }
              final Integer _tmpEnergy;
              if (_cursor.isNull(_cursorIndexOfEnergy)) {
                _tmpEnergy = null;
              } else {
                _tmpEnergy = _cursor.getInt(_cursorIndexOfEnergy);
              }
              final String _tmpComment;
              if (_cursor.isNull(_cursorIndexOfComment)) {
                _tmpComment = null;
              } else {
                _tmpComment = _cursor.getString(_cursorIndexOfComment);
              }
              final String _tmpPath;
              if (_cursor.isNull(_cursorIndexOfPath)) {
                _tmpPath = null;
              } else {
                _tmpPath = _cursor.getString(_cursorIndexOfPath);
              }
              _item = new Track(_tmpId,_tmpTitle,_tmpArtist,_tmpBpm,_tmpMusicalKey,_tmpGenre,_tmpRating,_tmpEnergy,_tmpComment,_tmpPath);
              _result.add(_item);
            }
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            _cursor.close();
            _statement.release();
          }
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
