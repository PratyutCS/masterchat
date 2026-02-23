package com.masterapp.chat.local;

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
import com.masterapp.chat.local.dao.ConversationDao;
import com.masterapp.chat.local.dao.ConversationDao_Impl;
import com.masterapp.chat.local.dao.MessageDao;
import com.masterapp.chat.local.dao.MessageDao_Impl;
import com.masterapp.chat.local.dao.ReadOutboxDao;
import com.masterapp.chat.local.dao.ReadOutboxDao_Impl;
import com.masterapp.chat.local.dao.SyncCheckpointDao;
import com.masterapp.chat.local.dao.SyncCheckpointDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile MessageDao _messageDao;

  private volatile SyncCheckpointDao _syncCheckpointDao;

  private volatile ReadOutboxDao _readOutboxDao;

  private volatile ConversationDao _conversationDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(6) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `messages` (`msgUuid` TEXT NOT NULL, `conversationId` TEXT, `text` TEXT, `senderId` TEXT, `sequenceId` INTEGER, `status` TEXT, `localTimestamp` INTEGER NOT NULL, `sentAt` TEXT, `readAt` TEXT, PRIMARY KEY(`msgUuid`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `sync_checkpoints` (`conversationId` TEXT NOT NULL, `lastPulledSeq` INTEGER NOT NULL, `lastPushedAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`conversationId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `read_outbox` (`conversationId` TEXT NOT NULL, `maxSequenceId` INTEGER NOT NULL, `localTimestamp` INTEGER NOT NULL, `synced` INTEGER NOT NULL, PRIMARY KEY(`conversationId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `conversations` (`id` TEXT NOT NULL, `title` TEXT, `lastMessage` TEXT, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '235b0dc4121bcd9adb6b7bae2c836ad3')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `messages`");
        db.execSQL("DROP TABLE IF EXISTS `sync_checkpoints`");
        db.execSQL("DROP TABLE IF EXISTS `read_outbox`");
        db.execSQL("DROP TABLE IF EXISTS `conversations`");
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
        final HashMap<String, TableInfo.Column> _columnsMessages = new HashMap<String, TableInfo.Column>(9);
        _columnsMessages.put("msgUuid", new TableInfo.Column("msgUuid", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("conversationId", new TableInfo.Column("conversationId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("text", new TableInfo.Column("text", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("senderId", new TableInfo.Column("senderId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("sequenceId", new TableInfo.Column("sequenceId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("status", new TableInfo.Column("status", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("localTimestamp", new TableInfo.Column("localTimestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("sentAt", new TableInfo.Column("sentAt", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("readAt", new TableInfo.Column("readAt", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMessages = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMessages = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoMessages = new TableInfo("messages", _columnsMessages, _foreignKeysMessages, _indicesMessages);
        final TableInfo _existingMessages = TableInfo.read(db, "messages");
        if (!_infoMessages.equals(_existingMessages)) {
          return new RoomOpenHelper.ValidationResult(false, "messages(com.masterapp.chat.local.entity.MessageEntity).\n"
                  + " Expected:\n" + _infoMessages + "\n"
                  + " Found:\n" + _existingMessages);
        }
        final HashMap<String, TableInfo.Column> _columnsSyncCheckpoints = new HashMap<String, TableInfo.Column>(4);
        _columnsSyncCheckpoints.put("conversationId", new TableInfo.Column("conversationId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSyncCheckpoints.put("lastPulledSeq", new TableInfo.Column("lastPulledSeq", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSyncCheckpoints.put("lastPushedAt", new TableInfo.Column("lastPushedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSyncCheckpoints.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSyncCheckpoints = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSyncCheckpoints = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSyncCheckpoints = new TableInfo("sync_checkpoints", _columnsSyncCheckpoints, _foreignKeysSyncCheckpoints, _indicesSyncCheckpoints);
        final TableInfo _existingSyncCheckpoints = TableInfo.read(db, "sync_checkpoints");
        if (!_infoSyncCheckpoints.equals(_existingSyncCheckpoints)) {
          return new RoomOpenHelper.ValidationResult(false, "sync_checkpoints(com.masterapp.chat.local.entity.SyncCheckpoint).\n"
                  + " Expected:\n" + _infoSyncCheckpoints + "\n"
                  + " Found:\n" + _existingSyncCheckpoints);
        }
        final HashMap<String, TableInfo.Column> _columnsReadOutbox = new HashMap<String, TableInfo.Column>(4);
        _columnsReadOutbox.put("conversationId", new TableInfo.Column("conversationId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReadOutbox.put("maxSequenceId", new TableInfo.Column("maxSequenceId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReadOutbox.put("localTimestamp", new TableInfo.Column("localTimestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReadOutbox.put("synced", new TableInfo.Column("synced", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysReadOutbox = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesReadOutbox = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoReadOutbox = new TableInfo("read_outbox", _columnsReadOutbox, _foreignKeysReadOutbox, _indicesReadOutbox);
        final TableInfo _existingReadOutbox = TableInfo.read(db, "read_outbox");
        if (!_infoReadOutbox.equals(_existingReadOutbox)) {
          return new RoomOpenHelper.ValidationResult(false, "read_outbox(com.masterapp.chat.local.entity.ReadOutbox).\n"
                  + " Expected:\n" + _infoReadOutbox + "\n"
                  + " Found:\n" + _existingReadOutbox);
        }
        final HashMap<String, TableInfo.Column> _columnsConversations = new HashMap<String, TableInfo.Column>(4);
        _columnsConversations.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsConversations.put("title", new TableInfo.Column("title", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsConversations.put("lastMessage", new TableInfo.Column("lastMessage", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsConversations.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysConversations = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesConversations = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoConversations = new TableInfo("conversations", _columnsConversations, _foreignKeysConversations, _indicesConversations);
        final TableInfo _existingConversations = TableInfo.read(db, "conversations");
        if (!_infoConversations.equals(_existingConversations)) {
          return new RoomOpenHelper.ValidationResult(false, "conversations(com.masterapp.chat.local.entity.ConversationEntity).\n"
                  + " Expected:\n" + _infoConversations + "\n"
                  + " Found:\n" + _existingConversations);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "235b0dc4121bcd9adb6b7bae2c836ad3", "83432a8af93b9644ae07b2a34c552007");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "messages","sync_checkpoints","read_outbox","conversations");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `messages`");
      _db.execSQL("DELETE FROM `sync_checkpoints`");
      _db.execSQL("DELETE FROM `read_outbox`");
      _db.execSQL("DELETE FROM `conversations`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
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
    _typeConvertersMap.put(MessageDao.class, MessageDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SyncCheckpointDao.class, SyncCheckpointDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ReadOutboxDao.class, ReadOutboxDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ConversationDao.class, ConversationDao_Impl.getRequiredConverters());
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
  public MessageDao messageDao() {
    if (_messageDao != null) {
      return _messageDao;
    } else {
      synchronized(this) {
        if(_messageDao == null) {
          _messageDao = new MessageDao_Impl(this);
        }
        return _messageDao;
      }
    }
  }

  @Override
  public SyncCheckpointDao syncCheckpointDao() {
    if (_syncCheckpointDao != null) {
      return _syncCheckpointDao;
    } else {
      synchronized(this) {
        if(_syncCheckpointDao == null) {
          _syncCheckpointDao = new SyncCheckpointDao_Impl(this);
        }
        return _syncCheckpointDao;
      }
    }
  }

  @Override
  public ReadOutboxDao readOutboxDao() {
    if (_readOutboxDao != null) {
      return _readOutboxDao;
    } else {
      synchronized(this) {
        if(_readOutboxDao == null) {
          _readOutboxDao = new ReadOutboxDao_Impl(this);
        }
        return _readOutboxDao;
      }
    }
  }

  @Override
  public ConversationDao conversationDao() {
    if (_conversationDao != null) {
      return _conversationDao;
    } else {
      synchronized(this) {
        if(_conversationDao == null) {
          _conversationDao = new ConversationDao_Impl(this);
        }
        return _conversationDao;
      }
    }
  }
}
