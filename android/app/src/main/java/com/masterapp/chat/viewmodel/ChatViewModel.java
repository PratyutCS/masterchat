package com.masterapp.chat.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.AndroidViewModel;

import com.google.gson.Gson;
import com.masterapp.chat.repository.MessageRepository;
import com.masterapp.chat.socket.SocketManager;

import java.util.List;

/**
 * ViewModel for the Chat screen.
 * 
 * Responsibilities:
 *   - Load message history via REST API
 *   - Listen for real-time messages via Socket.IO
 *   - Send messages via Socket.IO
 *   - Manage message list as LiveData for the UI
 */
public class ChatViewModel extends AndroidViewModel {

    private static final String TAG = "ChatViewModel";
    private final SocketManager socketManager = SocketManager.getInstance();
    private final Gson gson = new Gson();

    private final com.masterapp.chat.local.dao.MessageDao dao;
    private LiveData<List<com.masterapp.chat.local.entity.MessageEntity>> localMessages;
    private String conversationId;
    private String myUserId;

    private final androidx.lifecycle.Observer<org.json.JSONObject> newMessageObserver;
    private final androidx.lifecycle.Observer<org.json.JSONObject> messageReadObserver;
    private final androidx.lifecycle.Observer<org.json.JSONObject> messageDeliveredObserver;

    private MessageRepository messageRepository;
    private boolean isPolling = false;

    public ChatViewModel(android.app.Application application) {
        super(application);
        dao = com.masterapp.chat.local.AppDatabase.getDatabase(application).messageDao();
        messageRepository = new MessageRepository(application);
        
        // Retrieve current user ID from SharedPreferences for setting senderId
        myUserId = new com.masterapp.chat.util.TokenManager(application).getUserId();

        // Initialize observers AFTER dao is ready
        newMessageObserver = data -> {
            // A new message happened on the server. Pull it!
            triggerDownstreamSync();
        };

        // Status updates are now handled globally by StatusUpdateManager
        messageReadObserver = data -> {};
        messageDeliveredObserver = data -> {};
    }

    /** Expose messages as LiveData directly from Room! */
    public LiveData<List<com.masterapp.chat.local.entity.MessageEntity>> getMessages() {
        return localMessages;
    }

    public void initChat(String conversationId) {
        this.conversationId = conversationId;

        // 1. Observe strictly local SQLite data IMMEDIATELY
        // This ensures that even if user is offline, cached data shows up instantly
        localMessages = messageRepository.getLocalMessages(conversationId);

        // 2. Join Socket room for typing indicators & silent push triggers
        socketManager.joinConversation(conversationId);

        // 3. Observe global socket events via LiveData bus
        SocketManager.getInstance().getNewMessageEvents().observeForever(newMessageObserver);
        SocketManager.getInstance().getMessageReadEvents().observeForever(messageReadObserver);
        SocketManager.getInstance().getMessageDeliveredEvents().observeForever(messageDeliveredObserver);

        // Listen for typing events (Phase 2) - these are short lived, raw socket is okay
        socketManager.on("typing_start", args -> {
            // Typing indicators are short-lived UI state - using a simple flag
            // In a production app, we might use a LiveData<Boolean> isTyping
            Log.d(TAG, "Conversation partner started typing...");
        });
        
        socketManager.on("typing_stop", args -> {
            Log.d(TAG, "Conversation partner stopped typing");
        });

        // Load historical messages from server once on entry
        triggerDownstreamSync();
    }

    /**
     * PULL Downstream from Server to SQLite.
     * Uses sync checkpoints for robustness and conflict-safe inserts
     * to prevent status downgrades during re-sync.
     */
    public void triggerDownstreamSync() {
        new Thread(() -> {
            if (conversationId == null) return;

            try {
                // Use checkpoint table for robust tracking (falls back to MAX if no checkpoint)
                com.masterapp.chat.local.dao.SyncCheckpointDao checkpointDao =
                    com.masterapp.chat.local.AppDatabase.getDatabase(getApplication()).syncCheckpointDao();

                checkpointDao.ensureExists(conversationId, System.currentTimeMillis());
                Long checkpointSeq = checkpointDao.getLastPulledSeq(conversationId);
                Long checkpointPulledAt = checkpointDao.getLastPulledAt(conversationId);
                long afterSeq = checkpointSeq != null ? checkpointSeq : 0;
                String updatedAfter = formatIso8601(checkpointPulledAt != null ? checkpointPulledAt : 0);

                com.masterapp.chat.api.SyncApi api = com.masterapp.chat.api.ApiClient.getInstance(getApplication()).getSyncApi();
                retrofit2.Response<java.util.List<com.masterapp.chat.local.entity.MessageEntity>> response =
                    api.pullMessages(conversationId, afterSeq, updatedAfter).execute();

                if (response.isSuccessful()) {
                    // 1.5 Fetch and apply Read Watermarks (Crucial for offline catching up)
                    // Always do this if pull was successful, even if no new messages arrived.
                    try {
                        retrofit2.Response<java.util.List<com.masterapp.chat.api.SyncApi.Watermark>> wmResponse = 
                            api.getWatermarks(conversationId).execute();
                        if (wmResponse.isSuccessful() && wmResponse.body() != null) {
                            for (com.masterapp.chat.api.SyncApi.Watermark wm : wmResponse.body()) {
                                if (wm.userId != null && !wm.userId.equals(myUserId)) {
                                    // This is the OTHER user's read watermark. Apply it to MY sent messages.
                                    dao.updateOtherUserReadWatermark(conversationId, wm.readUpToSeq, "read", wm.updatedAt, myUserId);
                                } else if (wm.userId != null && wm.userId.equals(myUserId)) {
                                    // This is MY read watermark (from server). Apply it to INCOMING messages if local DB is behind.
                                    dao.updateMyOwnReadWatermark(conversationId, wm.readUpToSeq, wm.updatedAt, myUserId);
                                }
                            }
                        }
                    } catch (Exception wmErr) {
                        Log.w(TAG, "Watermark sync failed: " + wmErr.getMessage());
                    }

                    java.util.List<com.masterapp.chat.local.entity.MessageEntity> serverMessages = response.body();
                    if (serverMessages != null && !serverMessages.isEmpty()) {
                        // Conflict-safe insert: only adds messages that don't exist locally
                        dao.insertIfAbsentAll(serverMessages);

                        // For messages that DO already exist, safely update their sequenceId and status
                        for (com.masterapp.chat.local.entity.MessageEntity msg : serverMessages) {
                            dao.safeUpsertFromServer(msg.msgUuid, msg.sequenceId, msg.status, msg.sentAt, msg.readAt, msg.deliveredAt);
                        }

                        // Advance checkpoint to highest received sequenceId
                        long maxSeq = 0;
                        for (com.masterapp.chat.local.entity.MessageEntity msg : serverMessages) {
                            if (msg.sequenceId != null && msg.sequenceId > maxSeq) {
                                maxSeq = msg.sequenceId;
                            }
                        }
                        if (maxSeq > 0) {
                            checkpointDao.updatePulledCheckpoint(conversationId, maxSeq, System.currentTimeMillis(), System.currentTimeMillis());

                            // Two-phase delivery ack: tell server we successfully persisted these messages
                            try {
                                api.acknowledgeDelivery(
                                    new com.masterapp.chat.api.SyncApi.AckRequest(conversationId, maxSeq)
                                ).execute();
                            } catch (Exception ackErr) {
                                Log.w(TAG, "Delivery ack failed (will retry): " + ackErr.getMessage());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Pull Sync Error: " + e.getMessage());
            }

            // --- APPEND DELTA: Trigger Incremental Updates ---
            android.content.SharedPreferences prefs = getApplication().getSharedPreferences("ChatPrefs", android.content.Context.MODE_PRIVATE);
            long lastSyncTime = prefs.getLong("lastSyncTime", 0);
            
            try {
                retrofit2.Response<com.masterapp.chat.models.IncrementalPullResponse> pullResponse = com.masterapp.chat.api.ApiClient.getMessageApi()
                        .getIncrementalUpdates(lastSyncTime, 200)
                        .execute();
                        
                if (pullResponse.isSuccessful() && pullResponse.body() != null) {
                    List<com.masterapp.chat.local.entity.MessageEntity> updatedMessages = pullResponse.body().getData();
                    if (updatedMessages != null && !updatedMessages.isEmpty()) {
                        dao.insertOrReplaceAll(updatedMessages);
                        long maxUpdatedAt = lastSyncTime;
                        
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US);
                        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                        
                        for (com.masterapp.chat.local.entity.MessageEntity m : updatedMessages) {
                            if (m.updatedAt != null) {
                                try {
                                    java.util.Date date = sdf.parse(m.updatedAt);
                                    if (date != null && date.getTime() > maxUpdatedAt) {
                                        maxUpdatedAt = date.getTime();
                                    }
                                } catch (Exception ignored) {
                                    // Try alternate format
                                    try {
                                        java.text.SimpleDateFormat sdfAlt = new java.text.SimpleDateFormat(
                                                "yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US);
                                        sdfAlt.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                                        java.util.Date dateAlt = sdfAlt.parse(m.updatedAt);
                                        if (dateAlt != null && dateAlt.getTime() > maxUpdatedAt) {
                                            maxUpdatedAt = dateAlt.getTime();
                                        }
                                    } catch (Exception ignored2) {}
                                }
                            }
                        }
                        prefs.edit().putLong("lastSyncTime", maxUpdatedAt).apply();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Incremental Updates Sync Error", e);
            }
        }).start();
    }

    /**
     * Send a Message: Offline-First Approach
     */
    public void sendMessage(String text, String receiverId) {
        if (conversationId == null || text == null || text.trim().isEmpty()) return;

        // Delegate entire complex offline-first logic cleanly to Repository.
        // It injects locally securely, queues the sync Outbox, and handles WorkManager native scheduling securely.
        messageRepository.sendMessage(conversationId, myUserId, receiverId, text.trim());
    }

    /**
     * Mark messages as read: Offline-First Approach
     * 1. Write to local outbox (survives restart/offline)
     * 2. Trigger ReadSyncWorker
     * 3. Emit via socket (fast path for real-time)
     */
    public void markMessagesAsRead(String[] messageIds, Long maxSequenceId) {
        if (conversationId == null) return;

        // First update local SQLite DB to trigger instant UI update over LiveData
        new Thread(() -> {
            if (messageIds != null && messageIds.length > 0) {
                dao.markMessagesAsReadLocal(java.util.Arrays.asList(messageIds));
            }
            if (maxSequenceId != null) {
                dao.updateMyOwnReadWatermark(conversationId, maxSequenceId, null, myUserId);

                // --- NEW: Durable Read Outbox ---
                com.masterapp.chat.local.AppDatabase db = com.masterapp.chat.local.AppDatabase.getDatabase(getApplication());
                db.readOutboxDao().upsertReadWatermark(conversationId, maxSequenceId, System.currentTimeMillis());

                // Trigger background ReadSyncWorker
                androidx.work.OneTimeWorkRequest syncRequest = new androidx.work.OneTimeWorkRequest.Builder(com.masterapp.chat.sync.ReadSyncWorker.class)
                        .setConstraints(new androidx.work.Constraints.Builder()
                                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                                .build())
                        .build();

                androidx.work.WorkManager.getInstance(getApplication()).enqueueUniqueWork(
                        "read_sync_" + conversationId,
                        androidx.work.ExistingWorkPolicy.REPLACE,
                        syncRequest
                );
            }
        }).start();

        // Next securely tell the server (Fast Path)
        socketManager.markRead(conversationId, messageIds, maxSequenceId);
    }

    /**
     * Typing indicators
     */
    public void sendTypingStart() {
        if (conversationId != null) socketManager.emit("typing_start", conversationId);
    }

    public void sendTypingStop() {
        if (conversationId != null) socketManager.emit("typing_stop", conversationId);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        isPolling = false;
        // Remove stable observers
        SocketManager.getInstance().getNewMessageEvents().removeObserver(newMessageObserver);
        SocketManager.getInstance().getMessageReadEvents().removeObserver(messageReadObserver);
        SocketManager.getInstance().getMessageDeliveredEvents().removeObserver(messageDeliveredObserver);
        
        // Remove raw listeners
        socketManager.off("typing_start");
        socketManager.off("typing_stop");
    }

    private String formatIso8601(long millis) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US);
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return sdf.format(new java.util.Date(millis));
    }
}
