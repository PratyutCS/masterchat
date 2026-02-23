package com.masterapp.chat;

import android.app.Application;

import com.masterapp.chat.api.ApiClient;
import com.masterapp.chat.util.TokenManager;

/**
 * Application class for global initialization.
 */
public class ChatApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Global initialization: ensure ApiClient is always initialized,
        // even if Android resurrects the app directly to ConversationListActivity.
        ApiClient.init(new TokenManager(this));
    }
}
