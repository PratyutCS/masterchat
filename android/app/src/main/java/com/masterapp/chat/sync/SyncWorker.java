package com.masterapp.chat.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.masterapp.chat.api.ApiClient;
import com.masterapp.chat.api.SyncApi;
import com.masterapp.chat.local.AppDatabase;
import com.masterapp.chat.local.dao.MessageDao;
import com.masterapp.chat.local.dao.SyncCheckpointDao;
import com.masterapp.chat.local.entity.MessageEntity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Response;

/**
 * Production-grade SyncWorker with:
 * - Per-conversation partitioning (failure in conv A doesn't block conv B)
 * - Bounded retries (max 5 attempts via getRunAttemptCount)
 * - Conflict-safe upsert (server response never downgrades local status)
 * - Sync checkpoint integration (robust pull tracking)
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
        // Bounded retry: give up after MAX_RETRY_ATTEMPTS
        if (getRunAttemptCount() >= MAX_RETRY_ATTEMPTS) {
            Log.e(TAG, "Max retry attempts (" + MAX_RETRY_ATTEMPTS + ") exceeded. Giving up. " +
                       "Messages remain PENDING in local DB for manual recovery.");
            // Return success to stop retrying — messages stay PENDING for next user-initiated sync
            return Result.success();
        }

        AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
        MessageDao dao = db.messageDao();
        SyncCheckpointDao checkpointDao = db.syncCheckpointDao();
        SyncApi syncApi = ApiClient.getInstance(getApplicationContext()).create(SyncApi.class);

        // Per-conversation partitioning: get distinct conversations with pending messages
        List<String> conversationsWithPending = dao.getConversationsWithPendingMessages();

        if (conversationsWithPending == null || conversationsWithPending.isEmpty()) {
            return Result.success();
        }

        boolean anyFailure = false;

        // Process each conversation independently — failure in one doesn't block others
        for (String conversationId : conversationsWithPending) {
            try {
                List<MessageEntity> pending = dao.getPendingMessagesForConversation(conversationId);
                if (pending == null || pending.isEmpty()) continue;

                Log.d(TAG, "Pushing " + pending.size() + " messages for conversation " + conversationId);

                // Push batch to server
                Response<List<MessageEntity>> response = syncApi.pushMessages(pending).execute();

                if (response.isSuccessful() && response.body() != null) {
                    List<MessageEntity> serverMessages = response.body();
                    Log.d(TAG, "Push success for " + conversationId + ": " + serverMessages.size() + " messages");

                    // Conflict-safe update: use safeUpsertFromServer to prevent status downgrades
                    for (MessageEntity serverMsg : serverMessages) {
                        if (dao.messageExists(serverMsg.msgUuid) > 0) {
                            // Message exists locally — safely update without downgrading status
                            dao.safeUpsertFromServer(
                                serverMsg.msgUuid,
                                serverMsg.sequenceId,
                                serverMsg.status,
                                serverMsg.sentAt,
                                serverMsg.readAt
                            );
                        } else {
                            // New message (shouldn't happen for outbox, but handle gracefully)
                            dao.insertOrReplace(serverMsg);
                        }
                    }

                    // Update push checkpoint
                    checkpointDao.ensureExists(conversationId, System.currentTimeMillis());
                    checkpointDao.updatePushedAt(conversationId, System.currentTimeMillis());

                } else {
                    Log.e(TAG, "Push failed for " + conversationId + ": " +
                               response.code() + " " + response.message());
                    anyFailure = true;
                    // Continue to next conversation — don't let one failure block everything
                }
            } catch (IOException e) {
                Log.e(TAG, "IO error syncing " + conversationId + ": " + e.getMessage());
                anyFailure = true;
                // Continue to next conversation
            }
        }

        if (anyFailure) {
            Log.w(TAG, "Some conversations failed to sync. Retry attempt " +
                       (getRunAttemptCount() + 1) + "/" + MAX_RETRY_ATTEMPTS);
            return Result.retry();
        }

        return Result.success();
    }
}

