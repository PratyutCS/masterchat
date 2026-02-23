package com.masterapp.chat.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.masterapp.chat.local.dao.MessageDao;
import com.masterapp.chat.local.dao.ReadOutboxDao;
import com.masterapp.chat.local.dao.SyncCheckpointDao;
import com.masterapp.chat.local.dao.ConversationDao;
import com.masterapp.chat.local.entity.MessageEntity;
import com.masterapp.chat.local.entity.ReadOutbox;
import com.masterapp.chat.local.entity.SyncCheckpoint;
import com.masterapp.chat.local.entity.ConversationEntity;

@Database(entities = {MessageEntity.class, SyncCheckpoint.class, ReadOutbox.class, ConversationEntity.class}, version = 6, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract MessageDao messageDao();
    public abstract SyncCheckpointDao syncCheckpointDao();
    public abstract ReadOutboxDao readOutboxDao();
    public abstract ConversationDao conversationDao();

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "chat_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}

