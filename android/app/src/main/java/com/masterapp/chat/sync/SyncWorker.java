package com.masterapp.chat.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.masterapp.chat.api.ApiClient;
import com.masterapp.chat.api.ConversationApi;
import com.masterapp.chat.api.SyncApi;
import com.masterapp.chat.local.AppDatabase;
import com.masterapp.chat.local.dao.MessageDao;
import com.masterapp.chat.local.entity.MessageEntity;
import com.masterapp.chat.local.entity.SyncQueueEntity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Response;

/**
 * Upstream Sync Worker (Client -> Server)
 * Handles batched message sends and conversation deletes.
 */
public class SyncWorker extends Worker {

    private static final String TAG = "SyncWorker";
    private static final int MAX_RETRY_ATTEMPTS = 5;

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        if (getRunAttemptCount() >= MAX_RETRY_ATTEMPTS) {
            return Result.success();
        }

        AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
        MessageDao messageDao = db.messageDao();
        SyncApi syncApi = ApiClient.getSyncApi();
        ConversationApi convApi = ApiClient.getConversationApi();
        DeadLetterManager deadLetterManager = new DeadLetterManager(getApplicationContext());

        // Process up to 50 items from the sync queue
        List<SyncQueueEntity> queueItems = db.syncQueueDao().getPendingSyncQueue(50);
        if (queueItems == null || queueItems.isEmpty()) {
            return Result.success();
        }

        boolean anyFailure = false;
        List<Integer> successIds = new ArrayList<>();

        for (SyncQueueEntity item : queueItems) {
            try {
                if (item.operation.equals("CREATE_MSG") || item.operation.equals("CREATE")) {
                    // Process message creation
                    MessageEntity msg = messageDao.getMessageByUuid(item.entityId);
                    if (msg == null) {
                        successIds.add(item.id); // orphaned queue item
                        continue;
                    }

                    List<MessageEntity> batch = new ArrayList<>();
                    batch.add(msg);
                    Response<List<MessageEntity>> response = syncApi.pushMessages(batch).execute();

                    if (response.isSuccessful() && response.body() != null) {
                        MessageEntity serverMsg = response.body().get(0);
                        messageDao.safeUpsertFromServer(
                            serverMsg.msgUuid, serverMsg.sequenceId, serverMsg.status, 
                            serverMsg.sentAt, serverMsg.readAt, serverMsg.deliveredAt
                        );
                        successIds.add(item.id);
                    } else {
                        deadLetterManager.incrementAndCheck(item.id);
                        anyFailure = true;
                    }
                } 
                else if (item.operation.equals("DELETE_CONV")) {
                    // Process conversation deletion
                    Response<Void> response = convApi.deleteConversation(item.entityId).execute();
                    if (response.isSuccessful()) {
                        successIds.add(item.id);
                        Log.d(TAG, "Successfully deleted conversation on server: " + item.entityId);
                    } else if (response.code() == 404) {
                        // Already deleted or not found
                        successIds.add(item.id);
                    } else {
                        deadLetterManager.incrementAndCheck(item.id);
                        anyFailure = true;
                    }
                }
                else {
                    // Other operations (UPDATE_MSG etc)
                    successIds.add(item.id);
                }
            } catch (IOException e) {
                Log.e(TAG, "Sync error for item " + item.id + ": " + e.getMessage());
                anyFailure = true;
            }
        }

        if (!successIds.isEmpty()) {
            db.syncQueueDao().deleteSyncItems(successIds);
        }

        if (anyFailure) {
            return Result.retry();
        }

        return Result.success();
    }
}
