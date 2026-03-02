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
import com.masterapp.chat.local.entity.SyncQueueEntity;
import com.masterapp.chat.models.Conversation;
import com.masterapp.chat.models.User;
import com.masterapp.chat.util.TokenManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Response;

/**
 * Linear Sequential Master Synchronization Worker.
 * 
 * Logic Flow:
 * 1. Read everything from offline files (Done by Activities/ViewModels automatically)
 * 2. Push local updates (Upstream) to Server.
 * 3. Fetch server data in batches (Downstream).
 * 4. Update Database + Signal UI after each batch.
 * 5. Wait for UI to settle.
 * 6. Repeat in a linear loop.
 */
public class LinearSyncWorker extends Worker {

    private static final String TAG = "LinearSyncWorker";
    private static final int BATCH_SIZE = 50;
    private static final long STEP_PAUSE_MS = 500; // Fast UI settle

    public LinearSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting Linear Sequential Sync Cycle...");

        AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
        TokenManager tokenManager = new TokenManager(getApplicationContext());
        String myUserId = tokenManager.getUserId();

        if (myUserId == null || !tokenManager.isLoggedIn()) {
            return Result.success();
        }

        try {
            // STEP 1: UPSTREAM SYNC (Client -> Server)
            performUpstreamSync(db);
            pauseForUI();

            // STEP 2: DOWNSTREAM CONVERSATIONS (Server -> Client)
            List<String> convIds = performDownstreamConversationSync(db, myUserId);
            pauseForUI();

            // STEP 3: DOWNSTREAM MESSAGES (Batching for each conversation)
            if (convIds != null && !convIds.isEmpty()) {
                for (String convId : convIds) {
                    performDownstreamMessageSync(db, convId, myUserId);
                    pauseForUI(); // Allow UI to render after each conversation's batch
                }
            }

            Log.d(TAG, "Linear Sync Cycle Complete. Scheduling next tick in " + SyncScheduler.TICK_INTERVAL_MS + "ms");
            SyncScheduler.scheduleNextTick(getApplicationContext(), SyncScheduler.TICK_INTERVAL_MS);
            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "Sync Loop Error: " + e.getMessage());
            return Result.retry();
        }
    }

    private void performUpstreamSync(AppDatabase db) {
        Log.d(TAG, "[Upstream] Processing outbox...");
        SyncApi syncApi = ApiClient.getSyncApi();
        ConversationApi convApi = ApiClient.getConversationApi();
        DeadLetterManager deadLetterManager = new DeadLetterManager(getApplicationContext());

        List<SyncQueueEntity> items = db.syncQueueDao().getPendingSyncQueue(BATCH_SIZE);
        if (items == null || items.isEmpty()) return;

        List<Integer> successIds = new ArrayList<>();
        for (SyncQueueEntity item : items) {
            try {
                if (item.operation.equals("CREATE_MSG")) {
                    MessageEntity msg = db.messageDao().getMessageByUuid(item.entityId);
                    if (msg == null) {
                        successIds.add(item.id);
                        continue;
                    }
                    List<MessageEntity> batch = new ArrayList<>();
                    batch.add(msg);
                    Response<List<MessageEntity>> res = syncApi.pushMessages(batch).execute();
                    if (res.isSuccessful() && res.body() != null) {
                        MessageEntity serv = res.body().get(0);
                        db.messageDao().safeUpsertFromServer(serv.msgUuid, serv.sequenceId, serv.status, serv.sentAt, serv.readAt, serv.deliveredAt);
                        successIds.add(item.id);
                    } else {
                        deadLetterManager.incrementAndCheck(item.id);
                    }
                } else if (item.operation.equals("DELETE_CONV")) {
                    if (convApi.deleteConversation(item.entityId).execute().isSuccessful()) {
                        successIds.add(item.id);
                    } else {
                        deadLetterManager.incrementAndCheck(item.id);
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Upstream IO Error: " + e.getMessage());
            }
        }
        if (!successIds.isEmpty()) db.syncQueueDao().deleteSyncItems(successIds);
    }

    private List<String> performDownstreamConversationSync(AppDatabase db, String myUserId) throws IOException {
        Log.d(TAG, "[Downstream] Syncing conversations...");
        Response<List<Conversation>> res = ApiClient.getConversationApi().getConversations().execute();
        
        if (!res.isSuccessful() || res.body() == null) return null;

        List<Conversation> serverConvs = res.body();
        List<ConversationEntity> localEntities = new ArrayList<>();
        List<String> serverIds = new ArrayList<>();

        for (Conversation conv : serverConvs) {
            if (conv.getId() == null) continue;
            serverIds.add(conv.getId());

            String title = "Chat";
            String otherUserId = null;
            if (conv.getMembers() != null) {
                for (User m : conv.getMembers()) {
                    if (m != null && m.getId() != null && !m.getId().equals(myUserId)) {
                        title = m.getUsername() != null ? m.getUsername() : "User";
                        otherUserId = m.getId();
                        break;
                    }
                }
            }
            String lastText = (conv.getLastMessage() != null) ? conv.getLastMessage().getText() : "";
            localEntities.add(new ConversationEntity(conv.getId(), title, otherUserId, lastText, conv.getUnreadCount(), System.currentTimeMillis()));
        }

        db.conversationDao().insertOrReplaceAll(localEntities);
        db.conversationDao().deleteConversationsNotInList(serverIds);
        
        return serverIds;
    }

    private void performDownstreamMessageSync(AppDatabase db, String convId, String myUserId) throws IOException {
        Log.d(TAG, "[Downstream] Syncing messages for: " + convId);
        SyncApi syncApi = ApiClient.getSyncApi();
        SyncCheckpointDao checkpointDao = db.syncCheckpointDao();
        MessageDao messageDao = db.messageDao();

        checkpointDao.ensureExists(convId, System.currentTimeMillis());
        Long seq = checkpointDao.getLastPulledSeq(convId);
        Long lastPulledAt = checkpointDao.getLastPulledAt(convId);
        
        long afterSeq = (seq != null) ? seq : 0;
        String updatedAfter = formatIso8601(lastPulledAt != null ? lastPulledAt : 0);

        Response<List<MessageEntity>> res = syncApi.pullMessages(convId, afterSeq, updatedAfter).execute();
        if (res.isSuccessful() && res.body() != null) {
            List<MessageEntity> messages = res.body();
            if (!messages.isEmpty()) {
                messageDao.insertIfAbsentAll(messages);
                for (MessageEntity m : messages) {
                    if (m.msgUuid == null) continue;
                    messageDao.safeUpsertFromServer(m.msgUuid, m.sequenceId, m.status, m.sentAt, m.readAt, m.deliveredAt);
                }
            }
        }

        // Update local checkpoint and server ACK based on absolute local highest sequence
        Long localMax = messageDao.getHighestSequenceId(convId);
        if (localMax != null && localMax > 0) {
            checkpointDao.updatePulledCheckpoint(convId, localMax, System.currentTimeMillis(), System.currentTimeMillis());
            syncApi.acknowledgeDelivery(new SyncApi.AckRequest(convId, localMax)).execute();
        }

        // Always fetch watermarks
        {
            Response<List<SyncApi.Watermark>> wmRes = syncApi.getWatermarks(convId).execute();
            if (wmRes.isSuccessful() && wmRes.body() != null) {
                for (SyncApi.Watermark wm : wmRes.body()) {
                    if (wm.userId == null) continue;
                    if (!wm.userId.equals(myUserId)) {
                        messageDao.updateOtherUserReadWatermark(convId, wm.readUpToSeq, "read", wm.updatedAt, myUserId);
                    } else {
                        messageDao.updateMyOwnReadWatermark(convId, wm.readUpToSeq, wm.updatedAt, myUserId);
                    }
                }
            }
        }

        // --- STEP 4: Reconciliation (Hard-Delete Sync) ---
        performReconciliation(db, convId);
    }

    private void performReconciliation(AppDatabase db, String convId) {
        try {
            Log.d(TAG, "[Reconciliation] Verifying IDs for: " + convId);
            Response<SyncApi.ReconcileResponse> res = ApiClient.getSyncApi().reconcileIds(convId).execute();
            if (res.isSuccessful() && res.body() != null) {
                List<String> serverUuids = res.body().msgUuids;
                if (serverUuids != null) {
                    // Delete local messages that are NOT in the server's list
                    db.messageDao().deleteOrphanedMessages(convId, serverUuids);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Reconciliation Error: " + e.getMessage());
        }
    }

    private String formatIso8601(long millis) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US);
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return sdf.format(new java.util.Date(millis));
    }

    private void pauseForUI() {
        try {
            Thread.sleep(STEP_PAUSE_MS);
        } catch (InterruptedException ignored) {}
    }
}
