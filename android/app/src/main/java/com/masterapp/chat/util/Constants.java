package com.masterapp.chat.util;

/**
 * App-wide constants.
 * BASE_URL is set via BuildConfig (see app/build.gradle).
 */
public class Constants {
    // Base URL for REST API and Socket.IO (from BuildConfig)
    public static final String BASE_URL = com.masterapp.chat.BuildConfig.BASE_URL;

    // SharedPreferences file name
    public static final String PREFS_NAME = "chat_prefs";

    // Keys for SharedPreferences
    public static final String KEY_TOKEN = "jwt_token";
    public static final String KEY_USER_ID = "user_id";
    public static final String KEY_USERNAME = "username";
}
