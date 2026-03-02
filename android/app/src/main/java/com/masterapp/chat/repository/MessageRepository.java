package com.masterapp.chat.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.masterapp.chat.local.AppDatabase;
import com.masterapp.chat.local.entity.MessageEntity;
import com.masterapp.chat.local.entity.SyncQueueEntity;
import com.masterapp.chat.sync.SyncWorker;

import java.util.List;
import java.util.UUID;

/**
 * MessageRepository Refactored for Offline-First
 */
public class MessageRepository {

    private final AppDatabase db;
    private final WorkManager workManager;

    public MessageRepository(Context context) {
        this.db = AppDatabase.getDatabase(context);
        this.workManager = WorkManager.getInstance(context);
    }

    // Purely Offline-First Insert
    public void sendMessage(String conversationId, String senderId, String receiverId, String text) {
        new Thread(() -> {
            String msgUuid = UUID.randomUUID().toString();
            long now = System.currentTimeMillis();

            // 1. Save locally with status PENDING
            MessageEntity msg = new MessageEntity(
                msgUuid, conversationId, text, senderId, null, "PENDING", 
                now, String.valueOf(now), null, receiverId, text, null, null, false, String.valueOf(now)
            );

            // 2. Outbox operation item
            SyncQueueEntity syncEntry = new SyncQueueEntity(msgUuid, "CREATE_MSG", now);

            // 3. Insert both in the same rigid DB Transaction to guarantee local consistency
            db.runInTransaction(() -> {
                db.messageDao().insertOrReplace(msg);
                db.syncQueueDao().insert(syncEntry);
            });

            // 4. Instantly attempt trigger 
            scheduleSync();
        }).start();
    }

    // Observe local SQLite directly
    public LiveData<List<MessageEntity>> getLocalMessages(String conversationId) {
        return db.messageDao().getMessagesForConversation(conversationId);
    }

    private void scheduleSync() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(com.masterapp.chat.sync.LinearSyncWorker.class)
                .setConstraints(constraints)
                .build();

        workManager.enqueueUniqueWork(
                "LinearMasterSyncMsg",
                ExistingWorkPolicy.REPLACE,
                syncRequest
        );
    }
}
