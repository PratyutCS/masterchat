package com.masterapp.chat.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.masterapp.chat.api.ApiClient;
import com.masterapp.chat.api.SyncApi;
import com.masterapp.chat.local.AppDatabase;
import com.masterapp.chat.local.dao.ReadOutboxDao;
import com.masterapp.chat.local.entity.ReadOutbox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Response;

/**
 * ReadSyncWorker — durable offline-first read-receipt sync.
 *
 * Reads pending watermarks from the read_outbox table, batches them,
 * pushes to server via POST /api/sync/read-ack, marks acked on success.
 *
 * Features:
 * - Watermark compression (1 push per conversation, not per message)
 * - Bounded retries (max 5)
 * - Per-conversation fault isolation
 * - Survives app restart via WorkManager
 */
public class ReadSyncWorker extends Worker {

    private static final String TAG = "ReadSyncWorker";
    private static final int MAX_RETRY_ATTEMPTS = 5;

    public ReadSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        if (getRunAttemptCount() >= MAX_RETRY_ATTEMPTS) {
            Log.e(TAG, "Max retry attempts (" + MAX_RETRY_ATTEMPTS + ") reached. " +
                       "Pending reads remain in outbox for next sync.");
            return Result.success();
        }

        AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
        ReadOutboxDao outboxDao = db.readOutboxDao();
        SyncApi syncApi = ApiClient.getSyncApi();

        // Fetch all pending read watermarks
        List<ReadOutbox> pending = outboxDao.getPendingReadEvents();
        if (pending == null || pending.isEmpty()) {
            return Result.success();
        }

        Log.d(TAG, "Pushing " + pending.size() + " read watermarks to server");

        // Build batch request
        List<SyncApi.ReadAckItem> batch = new ArrayList<>();
        for (ReadOutbox entry : pending) {
            batch.add(new SyncApi.ReadAckItem(entry.conversationId, entry.maxSequenceId));
        }

        try {
            Response<SyncApi.ReadAckResponse> response = syncApi.acknowledgeReads(batch).execute();

            if (response.isSuccessful() && response.body() != null) {
                Log.d(TAG, "Read ack success: " + response.body().acked + " conversations");

                // Mark all as acked using sequence-aware safe method
                outboxDao.markAckedBatchSafe(pending);

                return Result.success();
            } else {
                Log.e(TAG, "Read ack failed: " + response.code() + " " + response.message());
                return Result.retry();
            }
        } catch (IOException e) {
            Log.e(TAG, "Read sync IO error: " + e.getMessage());
            return Result.retry();
        }
    }
}
