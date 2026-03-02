package com.masterapp.chat.sync;

import android.content.Context;
import android.util.Log;

import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

/**
 * Clock-ticking Heartbeat for the Sync System.
 * Ensures the LinearSyncWorker stays alive and active indefinitely.
 */
public class SyncScheduler {
    private static final String TAG = "SyncScheduler";
    private static final String SYNC_WORK_NAME = "MasterLinearSync";
    public static final long TICK_INTERVAL_MS = 5000; // 5 seconds high-frequency tick

    public static void startHeartbeat(Context context) {
        Log.d(TAG, "Initiating Sync Heartbeat...");
        scheduleNextTick(context, 0); // Start immediately
    }

    public static void scheduleNextTick(Context context, long delayMillis) {
        OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(LinearSyncWorker.class)
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .addTag("HeartbeatTick")
                .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                SYNC_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                syncRequest
        );
    }
}
