package com.masterapp.chat;

import android.app.Application;
import android.content.Context;

import com.masterapp.chat.api.ApiClient;
import com.masterapp.chat.util.TokenManager;

/**
 * Application class for global initialization.
 */
public class ChatApplication extends Application {
    private static Context context;

    public static Context getAppContext() {
        return context;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        // Global initialization: ensure ApiClient is always initialized,
        // even if Android resurrects the app directly to ConversationListActivity.
        ApiClient.init(new TokenManager(this));
        com.masterapp.chat.sync.StatusUpdateManager.init(this);

        // 1. Master Linear Sequential Sync (Upstream + Downstream)
        androidx.work.PeriodicWorkRequest linearSyncRequest = new androidx.work.PeriodicWorkRequest.Builder(
                com.masterapp.chat.sync.LinearSyncWorker.class, 
                15, java.util.concurrent.TimeUnit.MINUTES
        ).setConstraints(new androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()
        ).build();

        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "LinearMasterSync",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                linearSyncRequest
        );
    }
}
