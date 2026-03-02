package com.masterapp.chat.fcm;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.masterapp.chat.sync.DownstreamSyncWorker;
import com.masterapp.chat.util.TokenManager;

import java.util.Map;

/**
 * Firebase Cloud Messaging Service for background sync triggers.
 *
 * Receives data-only push notifications from the server containing:
 *   { conversationId: "abc", newSeqId: "95" }
 *
 * On receiving a push:
 *   1. Validates the user is logged in
 *   2. Enqueues a DownstreamSyncWorker to pull new messages
 *   3. Does NOT show a notification directly — the sync worker
 *      persists messages to Room, which triggers LiveData/UI updates
 *
 * This approach is:
 *   - E2EE-compatible (message content never goes through FCM)
 *   - Size-safe (FCM data payloads have a 4KB limit)
 *   - Reliable (actual content comes from the authoritative REST API)
 *
 * Token lifecycle:
 *   - onNewToken() fires when FCM assigns/rotates the device token
 *   - Token is sent to server so it can target this device for pushes
 */
public class ChatFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "ChatFCMService";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Map<String, String> data = remoteMessage.getData();
        Log.d(TAG, "FCM data received: " + data);

        // Validate user is logged in
        TokenManager tokenManager = new TokenManager(this);
        if (!tokenManager.isLoggedIn()) {
            Log.w(TAG, "FCM received but user not logged in, ignoring");
            return;
        }

        String conversationId = data.get("conversationId");
        String newSeqId = data.get("newSeqId");

        Log.d(TAG, "New message notification — conv: " + conversationId + ", seq: " + newSeqId);

        // Trigger full downstream sync (pulls ALL conversations, not just the notified one)
        // This ensures we catch any other missed messages too
        OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(DownstreamSyncWorker.class)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .build();

        WorkManager.getInstance(this).enqueueUniqueWork(
                "FCMTriggeredDownstreamSync",
                ExistingWorkPolicy.REPLACE,
                syncRequest
        );

        Log.d(TAG, "Enqueued DownstreamSyncWorker from FCM");
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "FCM token refreshed: " + token);

        // Store the token locally
        getSharedPreferences(com.masterapp.chat.util.Constants.PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString("fcm_token", token)
                .apply();

        // Send to server (if logged in)
        TokenManager tokenManager = new TokenManager(this);
        if (tokenManager.isLoggedIn()) {
            sendTokenToServer(token);
        }
    }

    /**
     * Send the FCM token to the backend so it can target this device.
     * Uses a fire-and-forget pattern — if it fails, it will be retried
     * on next token refresh or app login.
     */
    private void sendTokenToServer(String fcmToken) {
        new Thread(() -> {
            try {
                retrofit2.Response<okhttp3.ResponseBody> response =
                        com.masterapp.chat.api.ApiClient.getAuthApi()
                                .registerFcmToken(new com.masterapp.chat.api.AuthApi.FcmTokenRequest(fcmToken))
                                .execute();

                if (response.isSuccessful()) {
                    Log.d(TAG, "FCM token registered with server");
                } else {
                    Log.w(TAG, "FCM token registration failed: " + response.code());
                }
            } catch (Exception e) {
                Log.e(TAG, "FCM token registration error: " + e.getMessage());
            }
        }).start();
    }
}
