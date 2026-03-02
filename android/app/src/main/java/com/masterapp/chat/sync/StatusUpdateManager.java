package com.masterapp.chat.sync;

import android.content.Context;
import android.util.Log;

import com.masterapp.chat.local.AppDatabase;
import com.masterapp.chat.local.dao.MessageDao;
import com.masterapp.chat.socket.SocketManager;
import com.masterapp.chat.util.TokenManager;

import org.json.JSONObject;

/**
 * Global manager for real-time status updates via Socket.IO.
 * Updates the local Room database when 'delivered' or 'read' events arrive.
 */
public class StatusUpdateManager {

    private static final String TAG = "StatusUpdateManager";
    private static StatusUpdateManager instance;
    private final TokenManager tokenManager;
    private final Context context;

    private StatusUpdateManager(Context context) {
        this.context = context.getApplicationContext();
        this.tokenManager = new TokenManager(this.context);
    }

    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new StatusUpdateManager(context);
            instance.startObserving();
        }
    }

    private void startObserving() {
        Log.d(TAG, "Starting socket status update observers...");

        // 1. Observed 'delivered' status
        SocketManager.getInstance().getMessageDeliveredEvents().observeForever(data -> {
            if (data == null) return;
            handleStatusUpdate(data, "delivered");
        });

        // 2. Observed 'read' status
        SocketManager.getInstance().getMessageReadEvents().observeForever(data -> {
            if (data == null) return;
            handleStatusUpdate(data, "read");
        });
        
        // 3. Observed 'new_message_available'
        SocketManager.getInstance().getNewMessageEvents().observeForever(data -> {
            if (data == null) return;
            Log.d(TAG, "Global Trigger: New message detected. Syncing...");
            
            androidx.work.OneTimeWorkRequest syncRequest = new androidx.work.OneTimeWorkRequest.Builder(
                    LinearSyncWorker.class).build();
            androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
                    "GlobalNotificationSync",
                    androidx.work.ExistingWorkPolicy.REPLACE,
                    syncRequest
            );
        });

        // 4. Observed 'global_sync_required' (from admin or system)
        SocketManager.getInstance().getGlobalSyncEvents().observeForever(data -> {
            if (data == null) return;
            Log.d(TAG, "Admin Trigger: Global sync required. Reason: " + data.optString("reason"));
            
            androidx.work.OneTimeWorkRequest syncRequest = new androidx.work.OneTimeWorkRequest.Builder(
                    LinearSyncWorker.class).build();
            androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
                    "AdminGlobalSync",
                    androidx.work.ExistingWorkPolicy.REPLACE,
                    syncRequest
            );
        });

        // 5. Observed 'message_deleted_from_admin'
        SocketManager.getInstance().getMessageDeletedFromAdminEvents().observeForever(data -> {
            if (data == null) return;
            handleAdminMessageDeletion(data);
        });
    }

    private void handleAdminMessageDeletion(JSONObject data) {
        new Thread(() -> {
            try {
                String messageId = data.getString("messageId");
                Log.d(TAG, "Admin Trigger: Deleting message locally: " + messageId);
                
                AppDatabase db = AppDatabase.getDatabase(context);
                MessageDao dao = db.messageDao();
                
                // We need a delete query by ID in MessageDao
                dao.deleteByUuid(messageId);
                
            } catch (Exception e) {
                Log.e(TAG, "Error handling admin msg delete: " + e.getMessage());
            }
        }).start();
    }

    private void handleStatusUpdate(JSONObject data, String status) {
        new Thread(() -> {
            try {
                String myUserId = tokenManager.getUserId();
                if (myUserId == null) return;

                String convId = data.getString("conversationId");
                long maxSeq = data.optLong("sequenceId", -1);
                
                if (maxSeq == -1) return;

                Log.d(TAG, "Updating " + status + " for " + convId + " up to seq " + maxSeq);
                
                String timeStr = String.valueOf(System.currentTimeMillis());
                
                // Fetch DAO lazily to avoid startup race conditions
                MessageDao dao = AppDatabase.getDatabase(context).messageDao();
                dao.updateOtherUserReadWatermark(
                    convId, maxSeq, status, timeStr, myUserId
                );
            } catch (Exception e) {
                Log.e(TAG, "Status internal error: " + e.getMessage());
            }
        }).start();
    }
}
