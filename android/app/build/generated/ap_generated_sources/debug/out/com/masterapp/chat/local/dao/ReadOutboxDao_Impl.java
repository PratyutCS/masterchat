package com.masterapp.chat.local.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.masterapp.chat.local.entity.ReadOutbox;
import java.lang.Class;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ReadOutboxDao_Impl implements ReadOutboxDao {
  private final RoomDatabase __db;

  private final SharedSQLiteStatement __preparedStmtOfUpsertReadWatermark;

  private final SharedSQLiteStatement __preparedStmtOfMarkAcked;

  private final SharedSQLiteStatement __preparedStmtOfDeleteForConversation;

  public ReadOutboxDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__preparedStmtOfUpsertReadWatermark = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "INSERT INTO read_outbox (conversationId, maxSequenceId, localTimestamp, synced) VALUES (?, ?, ?, 0) ON CONFLICT(conversationId) DO UPDATE SET maxSequenceId = MAX(read_outbox.maxSequenceId, ?), localTimestamp = ?, synced = 0";
        return _query;
      }
    };
    this.__preparedStmtOfMarkAcked = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE read_outbox SET synced = 1 WHERE conversationId = ? AND maxSequenceId <= ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteForConversation = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM read_outbox WHERE conversationId = ?";
        return _query;
      }
    };
  }

  @Override
  public void upsertReadWatermark(final String convId, final long maxSeqId, final long timestamp) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfUpsertReadWatermark.acquire();
    int _argIndex = 1;
    if (convId == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, convId);
    }
    _argIndex = 2;
    _stmt.bindLong(_argIndex, maxSeqId);
    _argIndex = 3;
    _stmt.bindLong(_argIndex, timestamp);
    _argIndex = 4;
    _stmt.bindLong(_argIndex, maxSeqId);
    _argIndex = 5;
    _stmt.bindLong(_argIndex, timestamp);
    try {
      __db.beginTransaction();
      try {
        _stmt.executeInsert();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfUpsertReadWatermark.release(_stmt);
    }
  }

  @Override
  public void markAcked(final String convId, final long ackSeqId) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfMarkAcked.acquire();
    int _argIndex = 1;
    if (convId == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, convId);
    }
    _argIndex = 2;
    _stmt.bindLong(_argIndex, ackSeqId);
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfMarkAcked.release(_stmt);
    }
  }

  @Override
  public void deleteForConversation(final String convId) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteForConversation.acquire();
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
      __preparedStmtOfDeleteForConversation.release(_stmt);
    }
  }

  @Override
  public List<ReadOutbox> getPendingReadEvents() {
    final String _sql = "SELECT * FROM read_outbox WHERE synced = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfConversationId = CursorUtil.getColumnIndexOrThrow(_cursor, "conversationId");
      final int _cursorIndexOfMaxSequenceId = CursorUtil.getColumnIndexOrThrow(_cursor, "maxSequenceId");
      final int _cursorIndexOfLocalTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "localTimestamp");
      final int _cursorIndexOfSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "synced");
      final List<ReadOutbox> _result = new ArrayList<ReadOutbox>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final ReadOutbox _item;
        final String _tmpConversationId;
        if (_cursor.isNull(_cursorIndexOfConversationId)) {
          _tmpConversationId = null;
        } else {
          _tmpConversationId = _cursor.getString(_cursorIndexOfConversationId);
        }
        final long _tmpMaxSequenceId;
        _tmpMaxSequenceId = _cursor.getLong(_cursorIndexOfMaxSequenceId);
        final long _tmpLocalTimestamp;
        _tmpLocalTimestamp = _cursor.getLong(_cursorIndexOfLocalTimestamp);
        final int _tmpSynced;
        _tmpSynced = _cursor.getInt(_cursorIndexOfSynced);
        _item = new ReadOutbox(_tmpConversationId,_tmpMaxSequenceId,_tmpLocalTimestamp,_tmpSynced);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public Long getWatermark(final String convId) {
    final String _sql = "SELECT maxSequenceId FROM read_outbox WHERE conversationId = ?";
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
