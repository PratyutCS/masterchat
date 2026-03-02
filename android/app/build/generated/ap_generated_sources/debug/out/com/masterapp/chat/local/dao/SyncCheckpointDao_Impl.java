package com.masterapp.chat.local.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.masterapp.chat.local.entity.SyncCheckpoint;
import java.lang.Class;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class SyncCheckpointDao_Impl implements SyncCheckpointDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<SyncCheckpoint> __insertionAdapterOfSyncCheckpoint;

  private final SharedSQLiteStatement __preparedStmtOfUpdatePulledCheckpoint;

  private final SharedSQLiteStatement __preparedStmtOfUpdatePushedAt;

  private final SharedSQLiteStatement __preparedStmtOfEnsureExists;

  private final SharedSQLiteStatement __preparedStmtOfDeleteCheckpoint;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public SyncCheckpointDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSyncCheckpoint = new EntityInsertionAdapter<SyncCheckpoint>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `sync_checkpoints` (`conversationId`,`lastPulledSeq`,`lastPulledAt`,`lastPushedAt`,`updatedAt`) VALUES (?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final SyncCheckpoint entity) {
        if (entity.conversationId == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.conversationId);
        }
        statement.bindLong(2, entity.lastPulledSeq);
        statement.bindLong(3, entity.lastPulledAt);
        statement.bindLong(4, entity.lastPushedAt);
        statement.bindLong(5, entity.updatedAt);
      }
    };
    this.__preparedStmtOfUpdatePulledCheckpoint = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE sync_checkpoints SET lastPulledSeq = ?, lastPulledAt = ?, updatedAt = ? WHERE conversationId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdatePushedAt = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE sync_checkpoints SET lastPushedAt = ?, updatedAt = ? WHERE conversationId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfEnsureExists = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "INSERT OR IGNORE INTO sync_checkpoints (conversationId, lastPulledSeq, lastPulledAt, lastPushedAt, updatedAt) VALUES (?, 0, 0, 0, ?)";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteCheckpoint = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM sync_checkpoints WHERE conversationId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM sync_checkpoints";
        return _query;
      }
    };
  }

  @Override
  public void upsert(final SyncCheckpoint checkpoint) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfSyncCheckpoint.insert(checkpoint);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void updatePulledCheckpoint(final String convId, final long seq, final long pulledAt,
      final long now) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfUpdatePulledCheckpoint.acquire();
    int _argIndex = 1;
    _stmt.bindLong(_argIndex, seq);
    _argIndex = 2;
    _stmt.bindLong(_argIndex, pulledAt);
    _argIndex = 3;
    _stmt.bindLong(_argIndex, now);
    _argIndex = 4;
    if (convId == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, convId);
    }
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfUpdatePulledCheckpoint.release(_stmt);
    }
  }

  @Override
  public void updatePushedAt(final String convId, final long now) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfUpdatePushedAt.acquire();
    int _argIndex = 1;
    _stmt.bindLong(_argIndex, now);
    _argIndex = 2;
    _stmt.bindLong(_argIndex, now);
    _argIndex = 3;
    if (convId == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, convId);
    }
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfUpdatePushedAt.release(_stmt);
    }
  }

  @Override
  public void ensureExists(final String convId, final long now) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfEnsureExists.acquire();
    int _argIndex = 1;
    if (convId == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, convId);
    }
    _argIndex = 2;
    _stmt.bindLong(_argIndex, now);
    try {
      __db.beginTransaction();
      try {
        _stmt.executeInsert();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfEnsureExists.release(_stmt);
    }
  }

  @Override
  public void deleteCheckpoint(final String convId) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteCheckpoint.acquire();
    int _argIndex = 1;
    if (convId == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, convId);
    }
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfDeleteCheckpoint.release(_stmt);
    }
  }

  @Override
  public void deleteAll() {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfDeleteAll.release(_stmt);
    }
  }

  @Override
  public Long getLastPulledSeq(final String convId) {
    final String _sql = "SELECT lastPulledSeq FROM sync_checkpoints WHERE conversationId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (convId == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, convId);
    }
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final Long _result;
      if (_cursor.moveToFirst()) {
        if (_cursor.isNull(0)) {
          _result = null;
        } else {
          _result = _cursor.getLong(0);
        }
      } else {
        _result = null;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public Long getLastPulledAt(final String convId) {
    final String _sql = "SELECT lastPulledAt FROM sync_checkpoints WHERE conversationId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (convId == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, convId);
    }
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final Long _result;
      if (_cursor.moveToFirst()) {
        if (_cursor.isNull(0)) {
          _result = null;
        } else {
          _result = _cursor.getLong(0);
        }
      } else {
        _result = null;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
