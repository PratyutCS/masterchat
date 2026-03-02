package com.masterapp.chat.sync;

import android.content.Context;
import android.util.Log;

import com.masterapp.chat.local.AppDatabase;
import com.masterapp.chat.local.dao.SyncQueueDao;
import com.masterapp.chat.local.entity.SyncQueueEntity;

import java.util.List;

/**
 * Dead-Letter Queue Manager
 *
 * Manages failed sync queue items that have exceeded their retry budget.
 * After MAX_RETRIES attempts, items are moved to FAILED status and removed
 * from the automated retry queue. Users can manually retry failed messages
 * via the UI ("Tap to retry").
 *
 * Lifecycle:
 *   1. SyncWorker processes sync_queue entries
 *   2. On failure, incrementRetryCount() is called
 *   3. If retryCount >= MAX_RETRIES, markAsFailed() pulls it from rotation
 *   4. UI shows a red ❌ with "Tap to retry" for FAILED messages
 *   5. User tap calls retryFailedMessage() → resets retry count → re-enqueues
 */
public class DeadLetterManager {

    private static final String TAG = "DeadLetterManager";
    public static final int MAX_RETRIES = 10;

    private final AppDatabase db;

    public DeadLetterManager(Context context) {
        this.db = AppDatabase.getDatabase(context);
    }

    /**
     * Increment retry count for a sync queue item.
     * Returns true if the item should be retried, false if it hit the dead-letter limit.
     */
    public boolean incrementAndCheck(int syncQueueId) {
        SyncQueueDao dao = db.syncQueueDao();
        dao.incrementRetryCount(syncQueueId);

        SyncQueueEntity item = dao.getById(syncQueueId);
        if (item != null && item.retryCount >= MAX_RETRIES) {
            Log.w(TAG, "Message " + item.entityId + " exceeded max retries (" +
                    MAX_RETRIES + "). Moving to dead-letter (FAILED).");
            // Mark the message itself as FAILED
            db.messageDao().updateMessageStatus(item.entityId, "FAILED");
            // Remove from sync queue so it doesn't block other messages
            dao.deleteById(syncQueueId);
            return false;
        }
        return true;
    }

    /**
     * Get all messages currently in FAILED status (dead-lettered).
     * Used by the UI to show "Tap to retry" indicators.
     */
    public List<String> getFailedMessageUuids() {
        return db.messageDao().getFailedMessageUuids();
    }

    /**
     * Retry a specific failed message.
     * Resets its status to PENDING and re-inserts into the sync queue.
     */
    public void retryFailedMessage(String msgUuid) {
        db.runInTransaction(() -> {
            // Reset message status back to PENDING
            db.messageDao().updateMessageStatus(msgUuid, "PENDING");

            // Re-insert into sync queue with fresh retry count
            SyncQueueEntity newEntry = new SyncQueueEntity(msgUuid, "CREATE_MSG", System.currentTimeMillis());
            newEntry.retryCount = 0;
            db.syncQueueDao().insert(newEntry);

            Log.d(TAG, "Re-queued FAILED message " + msgUuid + " for retry");
        });
    }

    /**
     * Retry ALL failed messages at once.
     */
    public void retryAllFailed() {
        List<String> failedUuids = getFailedMessageUuids();
        if (failedUuids != null) {
            for (String uuid : failedUuids) {
                retryFailedMessage(uuid);
            }
            Log.d(TAG, "Re-queued " + (failedUuids != null ? failedUuids.size() : 0) + " failed messages");
        }
    }
}
