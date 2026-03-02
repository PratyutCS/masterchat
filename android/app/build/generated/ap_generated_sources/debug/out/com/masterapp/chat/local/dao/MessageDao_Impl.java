package com.masterapp.chat.local.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.room.util.StringUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.masterapp.chat.local.entity.MessageEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class MessageDao_Impl implements MessageDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<MessageEntity> __insertionAdapterOfMessageEntity;

  private final EntityInsertionAdapter<MessageEntity> __insertionAdapterOfMessageEntity_1;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  private final SharedSQLiteStatement __preparedStmtOfSafeUpsertFromServer;

  private final SharedSQLiteStatement __preparedStmtOfUpdateMessageStatusAndSeq;

  private final SharedSQLiteStatement __preparedStmtOfUpdateOtherUserReadWatermark;

  private final SharedSQLiteStatement __preparedStmtOfUpdateMyOwnReadWatermark;

  private final SharedSQLiteStatement __preparedStmtOfDeleteMessagesByConversation;

  private final SharedSQLiteStatement __preparedStmtOfUpdateMessageStatus;

  private final SharedSQLiteStatement __preparedStmtOfDeleteByUuid;

  public MessageDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfMessageEntity = new EntityInsertionAdapter<MessageEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `messages` (`msgUuid`,`conversationId`,`text`,`senderId`,`sequenceId`,`status`,`localTimestamp`,`sentAt`,`readAt`,`receiverId`,`content`,`deliveredAt`,`seenAt`,`isRead`,`updatedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final MessageEntity entity) {
        if (entity.msgUuid == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.msgUuid);
        }
        if (entity.conversationId == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.conversationId);
        }
        if (entity.text == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.text);
        }
        if (entity.senderId == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.senderId);
        }
        if (entity.sequenceId == null) {
          statement.bindNull(5);
        } else {
          statement.bindLong(5, entity.sequenceId);
        }
        if (entity.status == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.status);
        }
        statement.bindLong(7, entity.localTimestamp);
        if (entity.sentAt == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.sentAt);
        }
        if (entity.readAt == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.readAt);
        }
        if (entity.receiverId == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.receiverId);
        }
        if (entity.content == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.content);
        }
        if (entity.deliveredAt == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.deliveredAt);
        }
        if (entity.seenAt == null) {
          statement.bindNull(13);
        } else {
          statement.bindString(13, entity.seenAt);
        }
        final int _tmp = entity.isRead ? 1 : 0;
        statement.bindLong(14, _tmp);
        if (entity.updatedAt == null) {
          statement.bindNull(15);
        } else {
          statement.bindString(15, entity.updatedAt);
        }
      }
    };
    this.__insertionAdapterOfMessageEntity_1 = new EntityInsertionAdapter<MessageEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR IGNORE INTO `messages` (`msgUuid`,`conversationId`,`text`,`senderId`,`sequenceId`,`status`,`localTimestamp`,`sentAt`,`readAt`,`receiverId`,`content`,`deliveredAt`,`seenAt`,`isRead`,`updatedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final MessageEntity entity) {
        if (entity.msgUuid == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.msgUuid);
        }
        if (entity.conversationId == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.conversationId);
        }
        if (entity.text == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.text);
        }
        if (entity.senderId == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.senderId);
        }
        if (entity.sequenceId == null) {
          statement.bindNull(5);
        } else {
          statement.bindLong(5, entity.sequenceId);
        }
        if (entity.status == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.status);
        }
        statement.bindLong(7, entity.localTimestamp);
        if (entity.sentAt == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.sentAt);
        }
        if (entity.readAt == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.readAt);
        }
        if (entity.receiverId == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.receiverId);
        }
        if (entity.content == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.content);
        }
        if (entity.deliveredAt == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.deliveredAt);
        }
        if (entity.seenAt == null) {
          statement.bindNull(13);
        } else {
          statement.bindString(13, entity.seenAt);
        }
        final int _tmp = entity.isRead ? 1 : 0;
        statement.bindLong(14, _tmp);
        if (entity.updatedAt == null) {
          statement.bindNull(15);
        } else {
          statement.bindString(15, entity.updatedAt);
        }
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM messages";
        return _query;
      }
    };
    this.__preparedStmtOfSafeUpsertFromServer = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE messages SET sequenceId = ?, sentAt = ?, readAt = ?, deliveredAt = ?, status = CASE   WHEN status = 'read' THEN 'read'   WHEN status = 'delivered' AND ? IN ('sent', 'PENDING') THEN 'delivered'   WHEN status = 'sent' AND ? = 'PENDING' THEN 'sent'   ELSE ? END WHERE msgUuid = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateMessageStatusAndSeq = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE messages SET status = ?, sequenceId = ? WHERE msgUuid = ? AND status != 'read'";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateOtherUserReadWatermark = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE messages SET status = ?, readAt = CASE WHEN ? = 'read' AND readAt IS NULL THEN ? ELSE readAt END, deliveredAt = CASE WHEN ? IN ('read', 'delivered') AND deliveredAt IS NULL THEN ? ELSE deliveredAt END WHERE conversationId = ? AND sequenceId <= ? AND senderId = ? AND status != 'read'";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateMyOwnReadWatermark = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE messages SET status = 'read', readAt = ? WHERE conversationId = ? AND sequenceId <= ? AND senderId != ? AND status != 'read'";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteMessagesByConversation = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM messages WHERE conversationId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateMessageStatus = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE messages SET status = ? WHERE msgUuid = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteByUuid = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM messages WHERE msgUuid = ?";
        return _query;
      }
    };
  }

  @Override
  public void insertOrReplace(final MessageEntity message) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfMessageEntity.insert(message);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void insertOrReplaceAll(final List<MessageEntity> messages) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfMessageEntity.insert(messages);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void insertIfAbsent(final MessageEntity message) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfMessageEntity_1.insert(message);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void insertIfAbsentAll(final List<MessageEntity> messages) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfMessageEntity_1.insert(messages);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
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
  public void safeUpsertFromServer(final String msgUuid, final Long sequenceId,
      final String newStatus, final String sentAt, final String readAt, final String deliveredAt) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfSafeUpsertFromServer.acquire();
    int _argIndex = 1;
    if (sequenceId == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindLong(_argIndex, sequenceId);
    }
    _argIndex = 2;
    if (sentAt == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, sentAt);
    }
    _argIndex = 3;
    if (readAt == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, readAt);
    }
    _argIndex = 4;
    if (deliveredAt == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, deliveredAt);
    }
    _argIndex = 5;
    if (newStatus == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, newStatus);
    }
    _argIndex = 6;
    if (newStatus == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, newStatus);
    }
    _argIndex = 7;
    if (newStatus == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, newStatus);
    }
    _argIndex = 8;
    if (msgUuid == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, msgUuid);
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
      __preparedStmtOfSafeUpsertFromServer.release(_stmt);
    }
  }

  @Override
  public void updateMessageStatusAndSeq(final String msgUuid, final String newStatus,
      final Long sequenceId) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateMessageStatusAndSeq.acquire();
    int _argIndex = 1;
    if (newStatus == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, newStatus);
    }
    _argIndex = 2;
    if (sequenceId == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindLong(_argIndex, sequenceId);
    }
    _argIndex = 3;
    if (msgUuid == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, msgUuid);
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
      __preparedStmtOfUpdateMessageStatusAndSeq.release(_stmt);
    }
  }

  @Override
  public void updateOtherUserReadWatermark(final String convId, final long maxSeq,
      final String status, final String timeStr, final String myUserId) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateOtherUserReadWatermark.acquire();
    int _argIndex = 1;
    if (status == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, status);
    }
    _argIndex = 2;
    if (status == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, status);
    }
    _argIndex = 3;
    if (timeStr == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, timeStr);
    }
    _argIndex = 4;
    if (status == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, status);
    }
    _argIndex = 5;
    if (timeStr == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, timeStr);
    }
    _argIndex = 6;
    if (convId == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, convId);
    }
    _argIndex = 7;
    _stmt.bindLong(_argIndex, maxSeq);
    _argIndex = 8;
    if (myUserId == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, myUserId);
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
      __preparedStmtOfUpdateOtherUserReadWatermark.release(_stmt);
    }
  }

  @Override
  public void updateMyOwnReadWatermark(final String convId, final long maxSeq, final String readAt,
      final String myUserId) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateMyOwnReadWatermark.acquire();
    int _argIndex = 1;
    if (readAt == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, readAt);
    }
    _argIndex = 2;
    if (convId == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, convId);
    }
    _argIndex = 3;
    _stmt.bindLong(_argIndex, maxSeq);
    _argIndex = 4;
    if (myUserId == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, myUserId);
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
      __preparedStmtOfUpdateMyOwnReadWatermark.release(_stmt);
    }
  }

  @Override
  public void deleteMessagesByConversation(final String convId) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteMessagesByConversation.acquire();
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
      __preparedStmtOfDeleteMessagesByConversation.release(_stmt);
    }
  }

  @Override
  public void updateMessageStatus(final String msgUuid, final String status) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateMessageStatus.acquire();
    int _argIndex = 1;
    if (status == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, status);
    }
    _argIndex = 2;
    if (msgUuid == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, msgUuid);
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
      __preparedStmtOfUpdateMessageStatus.release(_stmt);
    }
  }

  @Override
  public void deleteByUuid(final String msgUuid) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteByUuid.acquire();
    int _argIndex = 1;
    if (msgUuid == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, msgUuid);
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
      __preparedStmtOfDeleteByUuid.release(_stmt);
    }
  }

  @Override
  public LiveData<List<MessageEntity>> getMessagesForConversation(final String convId) {
    final String _sql = "SELECT * FROM messages WHERE conversationId = ? ORDER BY COALESCE(sequenceId, 999999999) DESC, localTimestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (convId == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, convId);
    }
    return __db.getInvalidationTracker().createLiveData(new String[] {"messages"}, false, new Callable<List<MessageEntity>>() {
      @Override
      @Nullable
      public List<MessageEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfMsgUuid = CursorUtil.getColumnIndexOrThrow(_cursor, "msgUuid");
          final int _cursorIndexOfConversationId = CursorUtil.getColumnIndexOrThrow(_cursor, "conversationId");
          final int _cursorIndexOfText = CursorUtil.getColumnIndexOrThrow(_cursor, "text");
          final int _cursorIndexOfSenderId = CursorUtil.getColumnIndexOrThrow(_cursor, "senderId");
          final int _cursorIndexOfSequenceId = CursorUtil.getColumnIndexOrThrow(_cursor, "sequenceId");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfLocalTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "localTimestamp");
          final int _cursorIndexOfSentAt = CursorUtil.getColumnIndexOrThrow(_cursor, "sentAt");
          final int _cursorIndexOfReadAt = CursorUtil.getColumnIndexOrThrow(_cursor, "readAt");
          final int _cursorIndexOfReceiverId = CursorUtil.getColumnIndexOrThrow(_cursor, "receiverId");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfDeliveredAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deliveredAt");
          final int _cursorIndexOfSeenAt = CursorUtil.getColumnIndexOrThrow(_cursor, "seenAt");
          final int _cursorIndexOfIsRead = CursorUtil.getColumnIndexOrThrow(_cursor, "isRead");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<MessageEntity> _result = new ArrayList<MessageEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MessageEntity _item;
            final String _tmpMsgUuid;
            if (_cursor.isNull(_cursorIndexOfMsgUuid)) {
              _tmpMsgUuid = null;
            } else {
              _tmpMsgUuid = _cursor.getString(_cursorIndexOfMsgUuid);
            }
            final String _tmpConversationId;
            if (_cursor.isNull(_cursorIndexOfConversationId)) {
              _tmpConversationId = null;
            } else {
              _tmpConversationId = _cursor.getString(_cursorIndexOfConversationId);
            }
            final String _tmpText;
            if (_cursor.isNull(_cursorIndexOfText)) {
              _tmpText = null;
            } else {
              _tmpText = _cursor.getString(_cursorIndexOfText);
            }
            final String _tmpSenderId;
            if (_cursor.isNull(_cursorIndexOfSenderId)) {
              _tmpSenderId = null;
            } else {
              _tmpSenderId = _cursor.getString(_cursorIndexOfSenderId);
            }
            final Long _tmpSequenceId;
            if (_cursor.isNull(_cursorIndexOfSequenceId)) {
              _tmpSequenceId = null;
            } else {
              _tmpSequenceId = _cursor.getLong(_cursorIndexOfSequenceId);
            }
            final String _tmpStatus;
            if (_cursor.isNull(_cursorIndexOfStatus)) {
              _tmpStatus = null;
            } else {
              _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            }
            final long _tmpLocalTimestamp;
            _tmpLocalTimestamp = _cursor.getLong(_cursorIndexOfLocalTimestamp);
            final String _tmpSentAt;
            if (_cursor.isNull(_cursorIndexOfSentAt)) {
              _tmpSentAt = null;
            } else {
              _tmpSentAt = _cursor.getString(_cursorIndexOfSentAt);
            }
            final String _tmpReadAt;
            if (_cursor.isNull(_cursorIndexOfReadAt)) {
              _tmpReadAt = null;
            } else {
              _tmpReadAt = _cursor.getString(_cursorIndexOfReadAt);
            }
            final String _tmpReceiverId;
            if (_cursor.isNull(_cursorIndexOfReceiverId)) {
              _tmpReceiverId = null;
            } else {
              _tmpReceiverId = _cursor.getString(_cursorIndexOfReceiverId);
            }
            final String _tmpContent;
            if (_cursor.isNull(_cursorIndexOfContent)) {
              _tmpContent = null;
            } else {
              _tmpContent = _cursor.getString(_cursorIndexOfContent);
            }
            final String _tmpDeliveredAt;
            if (_cursor.isNull(_cursorIndexOfDeliveredAt)) {
              _tmpDeliveredAt = null;
            } else {
              _tmpDeliveredAt = _cursor.getString(_cursorIndexOfDeliveredAt);
            }
            final String _tmpSeenAt;
            if (_cursor.isNull(_cursorIndexOfSeenAt)) {
              _tmpSeenAt = null;
            } else {
              _tmpSeenAt = _cursor.getString(_cursorIndexOfSeenAt);
            }
            final boolean _tmpIsRead;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRead);
            _tmpIsRead = _tmp != 0;
            final String _tmpUpdatedAt;
            if (_cursor.isNull(_cursorIndexOfUpdatedAt)) {
              _tmpUpdatedAt = null;
            } else {
              _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt);
            }
            _item = new MessageEntity(_tmpMsgUuid,_tmpConversationId,_tmpText,_tmpSenderId,_tmpSequenceId,_tmpStatus,_tmpLocalTimestamp,_tmpSentAt,_tmpReadAt,_tmpReceiverId,_tmpContent,_tmpDeliveredAt,_tmpSeenAt,_tmpIsRead,_tmpUpdatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Long getHighestSequenceId(final String convId) {
    final String _sql = "SELECT MAX(sequenceId) FROM messages WHERE conversationId = ?";
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
        final Long _tmp;
        if (_cursor.isNull(0)) {
          _tmp = null;
        } else {
          _tmp = _cursor.getLong(0);
        }
        _result = _tmp;
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
  public List<MessageEntity> getPendingMessages() {
    final String _sql = "SELECT * FROM messages WHERE status = 'PENDING' ORDER BY localTimestamp ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfMsgUuid = CursorUtil.getColumnIndexOrThrow(_cursor, "msgUuid");
      final int _cursorIndexOfConversationId = CursorUtil.getColumnIndexOrThrow(_cursor, "conversationId");
      final int _cursorIndexOfText = CursorUtil.getColumnIndexOrThrow(_cursor, "text");
      final int _cursorIndexOfSenderId = CursorUtil.getColumnIndexOrThrow(_cursor, "senderId");
      final int _cursorIndexOfSequenceId = CursorUtil.getColumnIndexOrThrow(_cursor, "sequenceId");
      final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
      final int _cursorIndexOfLocalTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "localTimestamp");
      final int _cursorIndexOfSentAt = CursorUtil.getColumnIndexOrThrow(_cursor, "sentAt");
      final int _cursorIndexOfReadAt = CursorUtil.getColumnIndexOrThrow(_cursor, "readAt");
      final int _cursorIndexOfReceiverId = CursorUtil.getColumnIndexOrThrow(_cursor, "receiverId");
      final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
      final int _cursorIndexOfDeliveredAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deliveredAt");
      final int _cursorIndexOfSeenAt = CursorUtil.getColumnIndexOrThrow(_cursor, "seenAt");
      final int _cursorIndexOfIsRead = CursorUtil.getColumnIndexOrThrow(_cursor, "isRead");
      final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
      final List<MessageEntity> _result = new ArrayList<MessageEntity>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final MessageEntity _item;
        final String _tmpMsgUuid;
        if (_cursor.isNull(_cursorIndexOfMsgUuid)) {
          _tmpMsgUuid = null;
        } else {
          _tmpMsgUuid = _cursor.getString(_cursorIndexOfMsgUuid);
        }
        final String _tmpConversationId;
        if (_cursor.isNull(_cursorIndexOfConversationId)) {
          _tmpConversationId = null;
        } else {
          _tmpConversationId = _cursor.getString(_cursorIndexOfConversationId);
        }
        final String _tmpText;
        if (_cursor.isNull(_cursorIndexOfText)) {
          _tmpText = null;
        } else {
          _tmpText = _cursor.getString(_cursorIndexOfText);
        }
        final String _tmpSenderId;
        if (_cursor.isNull(_cursorIndexOfSenderId)) {
          _tmpSenderId = null;
        } else {
          _tmpSenderId = _cursor.getString(_cursorIndexOfSenderId);
        }
        final Long _tmpSequenceId;
        if (_cursor.isNull(_cursorIndexOfSequenceId)) {
          _tmpSequenceId = null;
        } else {
          _tmpSequenceId = _cursor.getLong(_cursorIndexOfSequenceId);
        }
        final String _tmpStatus;
        if (_cursor.isNull(_cursorIndexOfStatus)) {
          _tmpStatus = null;
        } else {
          _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
        }
        final long _tmpLocalTimestamp;
        _tmpLocalTimestamp = _cursor.getLong(_cursorIndexOfLocalTimestamp);
        final String _tmpSentAt;
        if (_cursor.isNull(_cursorIndexOfSentAt)) {
          _tmpSentAt = null;
        } else {
          _tmpSentAt = _cursor.getString(_cursorIndexOfSentAt);
        }
        final String _tmpReadAt;
        if (_cursor.isNull(_cursorIndexOfReadAt)) {
          _tmpReadAt = null;
        } else {
          _tmpReadAt = _cursor.getString(_cursorIndexOfReadAt);
        }
        final String _tmpReceiverId;
        if (_cursor.isNull(_cursorIndexOfReceiverId)) {
          _tmpReceiverId = null;
        } else {
          _tmpReceiverId = _cursor.getString(_cursorIndexOfReceiverId);
        }
        final String _tmpContent;
        if (_cursor.isNull(_cursorIndexOfContent)) {
          _tmpContent = null;
        } else {
          _tmpContent = _cursor.getString(_cursorIndexOfContent);
        }
        final String _tmpDeliveredAt;
        if (_cursor.isNull(_cursorIndexOfDeliveredAt)) {
          _tmpDeliveredAt = null;
        } else {
          _tmpDeliveredAt = _cursor.getString(_cursorIndexOfDeliveredAt);
        }
        final String _tmpSeenAt;
        if (_cursor.isNull(_cursorIndexOfSeenAt)) {
          _tmpSeenAt = null;
        } else {
          _tmpSeenAt = _cursor.getString(_cursorIndexOfSeenAt);
        }
        final boolean _tmpIsRead;
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfIsRead);
        _tmpIsRead = _tmp != 0;
        final String _tmpUpdatedAt;
        if (_cursor.isNull(_cursorIndexOfUpdatedAt)) {
          _tmpUpdatedAt = null;
        } else {
          _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt);
        }
        _item = new MessageEntity(_tmpMsgUuid,_tmpConversationId,_tmpText,_tmpSenderId,_tmpSequenceId,_tmpStatus,_tmpLocalTimestamp,_tmpSentAt,_tmpReadAt,_tmpReceiverId,_tmpContent,_tmpDeliveredAt,_tmpSeenAt,_tmpIsRead,_tmpUpdatedAt);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public List<MessageEntity> getPendingMessagesForConversation(final String convId) {
    final String _sql = "SELECT * FROM messages WHERE status = 'PENDING' AND conversationId = ? ORDER BY localTimestamp ASC";
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
      final int _cursorIndexOfMsgUuid = CursorUtil.getColumnIndexOrThrow(_cursor, "msgUuid");
      final int _cursorIndexOfConversationId = CursorUtil.getColumnIndexOrThrow(_cursor, "conversationId");
      final int _cursorIndexOfText = CursorUtil.getColumnIndexOrThrow(_cursor, "text");
      final int _cursorIndexOfSenderId = CursorUtil.getColumnIndexOrThrow(_cursor, "senderId");
      final int _cursorIndexOfSequenceId = CursorUtil.getColumnIndexOrThrow(_cursor, "sequenceId");
      final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
      final int _cursorIndexOfLocalTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "localTimestamp");
      final int _cursorIndexOfSentAt = CursorUtil.getColumnIndexOrThrow(_cursor, "sentAt");
      final int _cursorIndexOfReadAt = CursorUtil.getColumnIndexOrThrow(_cursor, "readAt");
      final int _cursorIndexOfReceiverId = CursorUtil.getColumnIndexOrThrow(_cursor, "receiverId");
      final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
      final int _cursorIndexOfDeliveredAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deliveredAt");
      final int _cursorIndexOfSeenAt = CursorUtil.getColumnIndexOrThrow(_cursor, "seenAt");
      final int _cursorIndexOfIsRead = CursorUtil.getColumnIndexOrThrow(_cursor, "isRead");
      final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
      final List<MessageEntity> _result = new ArrayList<MessageEntity>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final MessageEntity _item;
        final String _tmpMsgUuid;
        if (_cursor.isNull(_cursorIndexOfMsgUuid)) {
          _tmpMsgUuid = null;
        } else {
          _tmpMsgUuid = _cursor.getString(_cursorIndexOfMsgUuid);
        }
        final String _tmpConversationId;
        if (_cursor.isNull(_cursorIndexOfConversationId)) {
          _tmpConversationId = null;
        } else {
          _tmpConversationId = _cursor.getString(_cursorIndexOfConversationId);
        }
        final String _tmpText;
        if (_cursor.isNull(_cursorIndexOfText)) {
          _tmpText = null;
        } else {
          _tmpText = _cursor.getString(_cursorIndexOfText);
        }
        final String _tmpSenderId;
        if (_cursor.isNull(_cursorIndexOfSenderId)) {
          _tmpSenderId = null;
        } else {
          _tmpSenderId = _cursor.getString(_cursorIndexOfSenderId);
        }
        final Long _tmpSequenceId;
        if (_cursor.isNull(_cursorIndexOfSequenceId)) {
          _tmpSequenceId = null;
        } else {
          _tmpSequenceId = _cursor.getLong(_cursorIndexOfSequenceId);
        }
        final String _tmpStatus;
        if (_cursor.isNull(_cursorIndexOfStatus)) {
          _tmpStatus = null;
        } else {
          _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
        }
        final long _tmpLocalTimestamp;
        _tmpLocalTimestamp = _cursor.getLong(_cursorIndexOfLocalTimestamp);
        final String _tmpSentAt;
        if (_cursor.isNull(_cursorIndexOfSentAt)) {
          _tmpSentAt = null;
        } else {
          _tmpSentAt = _cursor.getString(_cursorIndexOfSentAt);
        }
        final String _tmpReadAt;
        if (_cursor.isNull(_cursorIndexOfReadAt)) {
          _tmpReadAt = null;
        } else {
          _tmpReadAt = _cursor.getString(_cursorIndexOfReadAt);
        }
        final String _tmpReceiverId;
        if (_cursor.isNull(_cursorIndexOfReceiverId)) {
          _tmpReceiverId = null;
        } else {
          _tmpReceiverId = _cursor.getString(_cursorIndexOfReceiverId);
        }
        final String _tmpContent;
        if (_cursor.isNull(_cursorIndexOfContent)) {
          _tmpContent = null;
        } else {
          _tmpContent = _cursor.getString(_cursorIndexOfContent);
        }
        final String _tmpDeliveredAt;
        if (_cursor.isNull(_cursorIndexOfDeliveredAt)) {
          _tmpDeliveredAt = null;
        } else {
          _tmpDeliveredAt = _cursor.getString(_cursorIndexOfDeliveredAt);
        }
        final String _tmpSeenAt;
        if (_cursor.isNull(_cursorIndexOfSeenAt)) {
          _tmpSeenAt = null;
        } else {
          _tmpSeenAt = _cursor.getString(_cursorIndexOfSeenAt);
        }
        final boolean _tmpIsRead;
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfIsRead);
        _tmpIsRead = _tmp != 0;
        final String _tmpUpdatedAt;
        if (_cursor.isNull(_cursorIndexOfUpdatedAt)) {
          _tmpUpdatedAt = null;
        } else {
          _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt);
        }
        _item = new MessageEntity(_tmpMsgUuid,_tmpConversationId,_tmpText,_tmpSenderId,_tmpSequenceId,_tmpStatus,_tmpLocalTimestamp,_tmpSentAt,_tmpReadAt,_tmpReceiverId,_tmpContent,_tmpDeliveredAt,_tmpSeenAt,_tmpIsRead,_tmpUpdatedAt);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public List<String> getConversationsWithPendingMessages() {
    final String _sql = "SELECT DISTINCT conversationId FROM messages WHERE status = 'PENDING'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final List<String> _result = new ArrayList<String>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final String _item;
        if (_cursor.isNull(0)) {
          _item = null;
        } else {
          _item = _cursor.getString(0);
        }
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public int messageExists(final String msgUuid) {
    final String _sql = "SELECT COUNT(*) FROM messages WHERE msgUuid = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (msgUuid == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, msgUuid);
    }
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _result;
      if (_cursor.moveToFirst()) {
        _result = _cursor.getInt(0);
      } else {
        _result = 0;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public MessageEntity getMessageByUuid(final String msgUuid) {
    final String _sql = "SELECT * FROM messages WHERE msgUuid = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (msgUuid == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, msgUuid);
    }
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfMsgUuid = CursorUtil.getColumnIndexOrThrow(_cursor, "msgUuid");
      final int _cursorIndexOfConversationId = CursorUtil.getColumnIndexOrThrow(_cursor, "conversationId");
      final int _cursorIndexOfText = CursorUtil.getColumnIndexOrThrow(_cursor, "text");
      final int _cursorIndexOfSenderId = CursorUtil.getColumnIndexOrThrow(_cursor, "senderId");
      final int _cursorIndexOfSequenceId = CursorUtil.getColumnIndexOrThrow(_cursor, "sequenceId");
      final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
      final int _cursorIndexOfLocalTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "localTimestamp");
      final int _cursorIndexOfSentAt = CursorUtil.getColumnIndexOrThrow(_cursor, "sentAt");
      final int _cursorIndexOfReadAt = CursorUtil.getColumnIndexOrThrow(_cursor, "readAt");
      final int _cursorIndexOfReceiverId = CursorUtil.getColumnIndexOrThrow(_cursor, "receiverId");
      final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
      final int _cursorIndexOfDeliveredAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deliveredAt");
      final int _cursorIndexOfSeenAt = CursorUtil.getColumnIndexOrThrow(_cursor, "seenAt");
      final int _cursorIndexOfIsRead = CursorUtil.getColumnIndexOrThrow(_cursor, "isRead");
      final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
      final MessageEntity _result;
      if (_cursor.moveToFirst()) {
        final String _tmpMsgUuid;
        if (_cursor.isNull(_cursorIndexOfMsgUuid)) {
          _tmpMsgUuid = null;
        } else {
          _tmpMsgUuid = _cursor.getString(_cursorIndexOfMsgUuid);
        }
        final String _tmpConversationId;
        if (_cursor.isNull(_cursorIndexOfConversationId)) {
          _tmpConversationId = null;
        } else {
          _tmpConversationId = _cursor.getString(_cursorIndexOfConversationId);
        }
        final String _tmpText;
        if (_cursor.isNull(_cursorIndexOfText)) {
          _tmpText = null;
        } else {
          _tmpText = _cursor.getString(_cursorIndexOfText);
        }
        final String _tmpSenderId;
        if (_cursor.isNull(_cursorIndexOfSenderId)) {
          _tmpSenderId = null;
        } else {
          _tmpSenderId = _cursor.getString(_cursorIndexOfSenderId);
        }
        final Long _tmpSequenceId;
        if (_cursor.isNull(_cursorIndexOfSequenceId)) {
          _tmpSequenceId = null;
        } else {
          _tmpSequenceId = _cursor.getLong(_cursorIndexOfSequenceId);
        }
        final String _tmpStatus;
        if (_cursor.isNull(_cursorIndexOfStatus)) {
          _tmpStatus = null;
        } else {
          _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
        }
        final long _tmpLocalTimestamp;
        _tmpLocalTimestamp = _cursor.getLong(_cursorIndexOfLocalTimestamp);
        final String _tmpSentAt;
        if (_cursor.isNull(_cursorIndexOfSentAt)) {
          _tmpSentAt = null;
        } else {
          _tmpSentAt = _cursor.getString(_cursorIndexOfSentAt);
        }
        final String _tmpReadAt;
        if (_cursor.isNull(_cursorIndexOfReadAt)) {
          _tmpReadAt = null;
        } else {
          _tmpReadAt = _cursor.getString(_cursorIndexOfReadAt);
        }
        final String _tmpReceiverId;
        if (_cursor.isNull(_cursorIndexOfReceiverId)) {
          _tmpReceiverId = null;
        } else {
          _tmpReceiverId = _cursor.getString(_cursorIndexOfReceiverId);
        }
        final String _tmpContent;
        if (_cursor.isNull(_cursorIndexOfContent)) {
          _tmpContent = null;
        } else {
          _tmpContent = _cursor.getString(_cursorIndexOfContent);
        }
        final String _tmpDeliveredAt;
        if (_cursor.isNull(_cursorIndexOfDeliveredAt)) {
          _tmpDeliveredAt = null;
        } else {
          _tmpDeliveredAt = _cursor.getString(_cursorIndexOfDeliveredAt);
        }
        final String _tmpSeenAt;
        if (_cursor.isNull(_cursorIndexOfSeenAt)) {
          _tmpSeenAt = null;
        } else {
          _tmpSeenAt = _cursor.getString(_cursorIndexOfSeenAt);
        }
        final boolean _tmpIsRead;
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfIsRead);
        _tmpIsRead = _tmp != 0;
        final String _tmpUpdatedAt;
        if (_cursor.isNull(_cursorIndexOfUpdatedAt)) {
          _tmpUpdatedAt = null;
        } else {
          _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt);
        }
        _result = new MessageEntity(_tmpMsgUuid,_tmpConversationId,_tmpText,_tmpSenderId,_tmpSequenceId,_tmpStatus,_tmpLocalTimestamp,_tmpSentAt,_tmpReadAt,_tmpReceiverId,_tmpContent,_tmpDeliveredAt,_tmpSeenAt,_tmpIsRead,_tmpUpdatedAt);
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
  public List<String> getFailedMessageUuids() {
    final String _sql = "SELECT msgUuid FROM messages WHERE status = 'FAILED'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final List<String> _result = new ArrayList<String>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final String _item;
        if (_cursor.isNull(0)) {
          _item = null;
        } else {
          _item = _cursor.getString(0);
        }
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public void markMessagesAsReadLocal(final List<String> msgUuids) {
    __db.assertNotSuspendingTransaction();
    final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
    _stringBuilder.append("UPDATE messages SET status = 'read' WHERE msgUuid IN (");
    final int _inputSize = msgUuids == null ? 1 : msgUuids.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(")");
    final String _sql = _stringBuilder.toString();
    final SupportSQLiteStatement _stmt = __db.compileStatement(_sql);
    int _argIndex = 1;
    if (msgUuids == null) {
      _stmt.bindNull(_argIndex);
    } else {
      for (String _item : msgUuids) {
        if (_item == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, _item);
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

  @Override
  public void deleteOrphanedMessages(final String convId, final List<String> serverUuids) {
    __db.assertNotSuspendingTransaction();
    final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
    _stringBuilder.append("DELETE FROM messages WHERE conversationId = ");
    _stringBuilder.append("?");
    _stringBuilder.append(" AND msgUuid NOT IN (");
    final int _inputSize = serverUuids == null ? 1 : serverUuids.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(")");
    final String _sql = _stringBuilder.toString();
    final SupportSQLiteStatement _stmt = __db.compileStatement(_sql);
    int _argIndex = 1;
    if (convId == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, convId);
    }
    _argIndex = 2;
    if (serverUuids == null) {
      _stmt.bindNull(_argIndex);
    } else {
      for (String _item : serverUuids) {
        if (_item == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, _item);
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
