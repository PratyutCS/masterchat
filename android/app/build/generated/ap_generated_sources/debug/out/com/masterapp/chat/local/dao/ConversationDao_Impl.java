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
import androidx.sqlite.db.SupportSQLiteStatement;
import com.masterapp.chat.local.entity.ConversationEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ConversationDao_Impl implements ConversationDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ConversationEntity> __insertionAdapterOfConversationEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  public ConversationDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfConversationEntity = new EntityInsertionAdapter<ConversationEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `conversations` (`id`,`title`,`lastMessage`,`updatedAt`) VALUES (?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final ConversationEntity entity) {
        if (entity.id == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.id);
        }
        if (entity.title == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.title);
        }
        if (entity.lastMessage == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.lastMessage);
        }
        statement.bindLong(4, entity.updatedAt);
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM conversations WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public void insertOrReplaceAll(final List<ConversationEntity> conversations) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfConversationEntity.insert(conversations);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void deleteById(final String id) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
    int _argIndex = 1;
    if (id == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, id);
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
      __preparedStmtOfDeleteById.release(_stmt);
    }
  }

  @Override
  public LiveData<List<ConversationEntity>> getAllConversations() {
    final String _sql = "SELECT * FROM conversations ORDER BY updatedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return __db.getInvalidationTracker().createLiveData(new String[] {"conversations"}, false, new Callable<List<ConversationEntity>>() {
      @Override
      @Nullable
      public List<ConversationEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfLastMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "lastMessage");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<ConversationEntity> _result = new ArrayList<ConversationEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ConversationEntity _item;
            final String _tmpId;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmpId = null;
            } else {
              _tmpId = _cursor.getString(_cursorIndexOfId);
            }
            final String _tmpTitle;
            if (_cursor.isNull(_cursorIndexOfTitle)) {
              _tmpTitle = null;
            } else {
              _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            }
            final String _tmpLastMessage;
            if (_cursor.isNull(_cursorIndexOfLastMessage)) {
              _tmpLastMessage = null;
            } else {
              _tmpLastMessage = _cursor.getString(_cursorIndexOfLastMessage);
            }
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new ConversationEntity(_tmpId,_tmpTitle,_tmpLastMessage,_tmpUpdatedAt);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
