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
import com.masterapp.chat.local.dao.ConversationDao;
import com.masterapp.chat.local.dao.MessageDao;
import com.masterapp.chat.local.dao.SyncCheckpointDao;
import com.masterapp.chat.local.entity.ConversationEntity;
import com.masterapp.chat.local.entity.MessageEntity;
import com.masterapp.chat.models.Conversation;
import com.masterapp.chat.models.User;
import com.masterapp.chat.util.TokenManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Response;

/**
 * Background Downstream Sync Worker
 *
 * Pulls ALL conversations and their new messages from the server,
 * independent of which chat screen is open. Runs:
 *   - Periodically (every 15 min via ChatApplication)
 *   - On Socket.IO new_message_available events
 *   - On FCM push notification wake-up
 *   - On network connectivity restored
 *
 * For each conversation:
 *   1. Fetches conversation list from server → upserts into local conversations table
 *   2. For each conversation, pulls messages after its sync checkpoint
 *   3. Uses conflict-safe inserts (insertIfAbsent + safeUpsertFromServer)
 *   4. Advances checkpoints
 *   5. Sends delivery ack (two-phase)
 *   6. Fetches read watermarks and applies them locally
 *
 * Per-conversation fault isolation: failure in one conversation
 * does NOT block syncing others.
 */
public class DownstreamSyncWorker extends Worker {

    private static final String TAG = "DownstreamSyncWorker";
    private static final int MAX_PULL_BATCH = 200;

    public DownstreamSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting full downstream sync...");

        AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
        ConversationDao conversationDao = db.conversationDao();
        MessageDao messageDao = db.messageDao();
        SyncCheckpointDao checkpointDao = db.syncCheckpointDao();

        TokenManager tokenManager = new TokenManager(getApplicationContext());
        String myUserId = tokenManager.getUserId();

        if (myUserId == null || !tokenManager.isLoggedIn()) {
            Log.w(TAG, "Not logged in, skipping downstream sync");
            return Result.success();
        }

        SyncApi syncApi = ApiClient.getSyncApi();

        // ──────────────────────────────────────────────────────────
        // PHASE 1: Sync conversation list from server → local Room
        // ──────────────────────────────────────────────────────────
        List<Conversation> serverConversations = null;
        try {
            Response<List<Conversation>> convResponse = ApiClient.getConversationApi()
                    .getConversations()
                    .execute();

            if (convResponse.isSuccessful() && convResponse.body() != null) {
                serverConversations = convResponse.body();

                // Convert API models to Room entities and upsert
                List<ConversationEntity> localEntities = new ArrayList<>();
                for (Conversation conv : serverConversations) {
                    String title = "";
                    String otherUserIdFromConv = null;
                    if (conv.getMembers() != null) {
                        for (User member : conv.getMembers()) {
                            String memberId = member.getId();
                            if (memberId != null && !memberId.equals(myUserId)) {
                                title = member.getUsername();
                                otherUserIdFromConv = memberId;
                                break;
                            }
                        }
                    }

                    String lastMsg = "";
                    if (conv.getLastMessage() != null && conv.getLastMessage().getText() != null) {
                        lastMsg = conv.getLastMessage().getText();
                    }

                    long updatedAt = System.currentTimeMillis();
                    if (conv.getUpdatedAt() != null) {
                        try {
                            // Backend returns ISO 8601 strings (e.g. 2024-02-28T12:34:56.789Z)
                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US);
                            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                            java.util.Date date = sdf.parse(conv.getUpdatedAt());
                            if (date != null) updatedAt = date.getTime();
                        } catch (Exception ignore) {
                            // Fallback for different ISO formats (some might omit .SSS)
                            try {
                                java.text.SimpleDateFormat sdfAlt = new java.text.SimpleDateFormat(
                                        "yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US);
                                sdfAlt.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                                java.util.Date date = sdfAlt.parse(conv.getUpdatedAt());
                                if (date != null) updatedAt = date.getTime();
                            } catch (Exception ignored) {}
                        }
                    }

                    localEntities.add(new ConversationEntity(
                            conv.getId(), title, otherUserIdFromConv, lastMsg, conv.getUnreadCount(), updatedAt
                    ));
                }

                if (!localEntities.isEmpty()) {
                    conversationDao.insertOrReplaceAll(localEntities);
                    Log.d(TAG, "Synced " + localEntities.size() + " conversations to local DB");
                }

                // --- RECONCILIATION: Remove local conversations that no longer exist on server ---
                List<String> serverIds = new ArrayList<>();
                for (Conversation conv : serverConversations) {
                    serverIds.add(conv.getId());
                }
                
                if (!serverIds.isEmpty()) {
                    conversationDao.deleteConversationsNotInList(serverIds);
                } else {
                    // If server says 0 conversations, clear all local
                    conversationDao.deleteAll();
                    messageDao.deleteAll();
                    checkpointDao.deleteAll();
                }
                Log.d(TAG, "Conversation reconciliation complete");
            } else {
                Log.w(TAG, "Conversation fetch failed: " +
                        (convResponse.code()) + " " + convResponse.message());
            }
        } catch (IOException e) {
            Log.e(TAG, "Conversation sync IO error: " + e.getMessage());
            // Continue to message sync using locally cached conversations
        }

        // ──────────────────────────────────────────────────────────
        // PHASE 2: For each conversation, pull new messages
        // ──────────────────────────────────────────────────────────
        List<String> conversationIds = new ArrayList<>();

        // Use server conversations if available, otherwise fall back to local
        if (serverConversations != null) {
            for (Conversation conv : serverConversations) {
                conversationIds.add(conv.getId());
            }
        }

        if (conversationIds.isEmpty()) {
            Log.d(TAG, "No conversations to sync messages for");
            return Result.success();
        }

        boolean anyFailure = false;

        for (String convId : conversationIds) {
            try {
                // Ensure checkpoint exists
                checkpointDao.ensureExists(convId, System.currentTimeMillis());

                // Get last pulled sequence ID
                Long checkpointSeq = checkpointDao.getLastPulledSeq(convId);
                Long checkpointPulledAt = checkpointDao.getLastPulledAt(convId);
                long afterSeq = checkpointSeq != null ? checkpointSeq : 0;
                String updatedAfter = formatIso8601(checkpointPulledAt != null ? checkpointPulledAt : 0);

                // Pull messages from server
                Response<List<MessageEntity>> msgResponse = syncApi
                        .pullMessages(convId, afterSeq, updatedAfter)
                        .execute();

                if (msgResponse.isSuccessful() && msgResponse.body() != null) {
                    List<MessageEntity> serverMessages = msgResponse.body();

                    if (serverMessages != null && !serverMessages.isEmpty()) {
                        // Conflict-safe insert
                        messageDao.insertIfAbsentAll(serverMessages);

                        // Safe upsert for existing messages
                        for (MessageEntity msg : serverMessages) {
                            messageDao.safeUpsertFromServer(
                                    msg.msgUuid, msg.sequenceId,
                                    msg.status, msg.sentAt, msg.readAt, msg.deliveredAt
                            );
                        }

                        // Advance checkpoint
                        long maxSeq = 0;
                        for (MessageEntity msg : serverMessages) {
                            if (msg.sequenceId != null && msg.sequenceId > maxSeq) {
                                maxSeq = msg.sequenceId;
                            }
                        }

                        if (maxSeq > 0) {
                            checkpointDao.updatePulledCheckpoint(convId, maxSeq, System.currentTimeMillis(), System.currentTimeMillis());

                            // Two-phase delivery ack
                            try {
                                syncApi.acknowledgeDelivery(
                                        new SyncApi.AckRequest(convId, maxSeq)
                                ).execute();
                            } catch (Exception ackErr) {
                                Log.w(TAG, "Delivery ack failed for " + convId + ": " + ackErr.getMessage());
                            }
                        }

                        Log.d(TAG, "Pulled " + serverMessages.size() +
                                " messages for conversation " + convId);
                    }
                } else {
                    Log.w(TAG, "Message pull failed for " + convId +
                            ": " + msgResponse.code());
                    anyFailure = true;
                }

                // ──────────────────────────────────────────────────
                // PHASE 3: Fetch and apply read watermarks
                // ──────────────────────────────────────────────────
                try {
                    Response<List<SyncApi.Watermark>> wmResponse =
                            syncApi.getWatermarks(convId).execute();

                    if (wmResponse.isSuccessful() && wmResponse.body() != null) {
                        for (SyncApi.Watermark wm : wmResponse.body()) {
                            if (wm.userId != null && !wm.userId.equals(myUserId)) {
                                // Other user's read watermark → update MY sent messages
                                messageDao.updateOtherUserReadWatermark(
                                        convId, wm.readUpToSeq, "read", wm.updatedAt, myUserId
                                );
                            } else if (wm.userId != null && wm.userId.equals(myUserId)) {
                                // My own read watermark from server → apply to incoming
                                messageDao.updateMyOwnReadWatermark(
                                        convId, wm.readUpToSeq, wm.updatedAt, myUserId
                                );
                            }
                        }
                    }
                } catch (Exception wmErr) {
                    Log.w(TAG, "Watermark sync failed for " + convId + ": " + wmErr.getMessage());
                }

            } catch (IOException e) {
                Log.e(TAG, "IO error syncing conversation " + convId + ": " + e.getMessage());
                anyFailure = true;
                // Continue to next conversation — per-conversation fault isolation
            }
        }

        if (anyFailure) {
            Log.w(TAG, "Some conversations had sync failures");
            // Still return success — we don't want to block periodic scheduling
            // Individual conversation failures will be retried on next periodic run
        }

        Log.d(TAG, "Downstream sync complete");
        return Result.success();
    }

    private String formatIso8601(long millis) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US);
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return sdf.format(new java.util.Date(millis));
    }
}
