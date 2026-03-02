package com.masterapp.chat.local.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.room.util.StringUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.masterapp.chat.local.entity.SyncQueueEntity;
import java.lang.Class;
import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class SyncQueueDao_Impl implements SyncQueueDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<SyncQueueEntity> __insertionAdapterOfSyncQueueEntity;

  private final SharedSQLiteStatement __preparedStmtOfIncrementRetryCount;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  public SyncQueueDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSyncQueueEntity = new EntityInsertionAdapter<SyncQueueEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `sync_queue` (`id`,`entityId`,`operation`,`retryCount`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final SyncQueueEntity entity) {
        statement.bindLong(1, entity.id);
        if (entity.entityId == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.entityId);
        }
        if (entity.operation == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.operation);
        }
        statement.bindLong(4, entity.retryCount);
        statement.bindLong(5, entity.createdAt);
      }
    };
    this.__preparedStmtOfIncrementRetryCount = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE sync_queue SET retryCount = retryCount + 1 WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM sync_queue WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public void insert(final SyncQueueEntity syncQueueEntity) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfSyncQueueEntity.insert(syncQueueEntity);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void incrementRetryCount(final int id) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfIncrementRetryCount.acquire();
    int _argIndex = 1;
    _stmt.bindLong(_argIndex, id);
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfIncrementRetryCount.release(_stmt);
    }
  }

  @Override
  public void deleteById(final int id) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
    int _argIndex = 1;
    _stmt.bindLong(_argIndex, id);
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfDeleteById.release(_stmt);
    }
  }

  @Override
  public List<SyncQueueEntity> getPendingSyncQueue(final int max) {
    final String _sql = "SELECT * FROM sync_queue ORDER BY createdAt ASC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, max);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfEntityId = CursorUtil.getColumnIndexOrThrow(_cursor, "entityId");
      final int _cursorIndexOfOperation = CursorUtil.getColumnIndexOrThrow(_cursor, "operation");
      final int _cursorIndexOfRetryCount = CursorUtil.getColumnIndexOrThrow(_cursor, "retryCount");
      final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
      final List<SyncQueueEntity> _result = new ArrayList<SyncQueueEntity>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final SyncQueueEntity _item;
        final String _tmpEntityId;
        if (_cursor.isNull(_cursorIndexOfEntityId)) {
          _tmpEntityId = null;
        } else {
          _tmpEntityId = _cursor.getString(_cursorIndexOfEntityId);
        }
        final String _tmpOperation;
        if (_cursor.isNull(_cursorIndexOfOperation)) {
          _tmpOperation = null;
        } else {
          _tmpOperation = _cursor.getString(_cursorIndexOfOperation);
        }
        final long _tmpCreatedAt;
        _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
        _item = new SyncQueueEntity(_tmpEntityId,_tmpOperation,_tmpCreatedAt);
        _item.id = _cursor.getInt(_cursorIndexOfId);
        _item.retryCount = _cursor.getInt(_cursorIndexOfRetryCount);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public SyncQueueEntity getById(final int id) {
    final String _sql = "SELECT * FROM sync_queue WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfEntityId = CursorUtil.getColumnIndexOrThrow(_cursor, "entityId");
      final int _cursorIndexOfOperation = CursorUtil.getColumnIndexOrThrow(_cursor, "operation");
      final int _cursorIndexOfRetryCount = CursorUtil.getColumnIndexOrThrow(_cursor, "retryCount");
      final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
      final SyncQueueEntity _result;
      if (_cursor.moveToFirst()) {
        final String _tmpEntityId;
        if (_cursor.isNull(_cursorIndexOfEntityId)) {
          _tmpEntityId = null;
        } else {
          _tmpEntityId = _cursor.getString(_cursorIndexOfEntityId);
        }
        final String _tmpOperation;
        if (_cursor.isNull(_cursorIndexOfOperation)) {
          _tmpOperation = null;
        } else {
          _tmpOperation = _cursor.getString(_cursorIndexOfOperation);
        }
        final long _tmpCreatedAt;
        _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
        _result = new SyncQueueEntity(_tmpEntityId,_tmpOperation,_tmpCreatedAt);
        _result.id = _cursor.getInt(_cursorIndexOfId);
        _result.retryCount = _cursor.getInt(_cursorIndexOfRetryCount);
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
  public SyncQueueEntity getByEntityId(final String entityId) {
    final String _sql = "SELECT * FROM sync_queue WHERE entityId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (entityId == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, entityId);
    }
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfEntityId = CursorUtil.getColumnIndexOrThrow(_cursor, "entityId");
      final int _cursorIndexOfOperation = CursorUtil.getColumnIndexOrThrow(_cursor, "operation");
      final int _cursorIndexOfRetryCount = CursorUtil.getColumnIndexOrThrow(_cursor, "retryCount");
      final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
      final SyncQueueEntity _result;
      if (_cursor.moveToFirst()) {
        final String _tmpEntityId;
        if (_cursor.isNull(_cursorIndexOfEntityId)) {
          _tmpEntityId = null;
        } else {
          _tmpEntityId = _cursor.getString(_cursorIndexOfEntityId);
        }
        final String _tmpOperation;
        if (_cursor.isNull(_cursorIndexOfOperation)) {
          _tmpOperation = null;
        } else {
          _tmpOperation = _cursor.getString(_cursorIndexOfOperation);
        }
        final long _tmpCreatedAt;
        _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
        _result = new SyncQueueEntity(_tmpEntityId,_tmpOperation,_tmpCreatedAt);
        _result.id = _cursor.getInt(_cursorIndexOfId);
        _result.retryCount = _cursor.getInt(_cursorIndexOfRetryCount);
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
  public void deleteSyncItems(final List<Integer> ids) {
    __db.assertNotSuspendingTransaction();
    final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
    _stringBuilder.append("DELETE FROM sync_queue WHERE id IN (");
    final int _inputSize = ids == null ? 1 : ids.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(")");
    final String _sql = _stringBuilder.toString();
    final SupportSQLiteStatement _stmt = __db.compileStatement(_sql);
    int _argIndex = 1;
    if (ids == null) {
      _stmt.bindNull(_argIndex);
    } else {
      for (Integer _item : ids) {
        if (_item == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindLong(_argIndex, _item);
        }
        _argIndex++;
      }
    }
    __db.beginTransaction();
    try {
      _stmt.executeUpdateDelete();
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
