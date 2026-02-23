package com.masterapp.chat.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manages JWT token storage using SharedPreferences.
 * Provides methods to save, retrieve, and clear auth data.
 */
public class TokenManager {

    private final SharedPreferences prefs;

    public TokenManager(Context context) {
        prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** Save token and user info after login/register */
    public void saveToken(String token, String userId, String username) {
        prefs.edit()
                .putString(Constants.KEY_TOKEN, token)
                .putString(Constants.KEY_USER_ID, userId)
                .putString(Constants.KEY_USERNAME, username)
                .apply();
    }

    /** Get the stored JWT token, or null if not logged in */
    public String getToken() {
        return prefs.getString(Constants.KEY_TOKEN, null);
    }

    /** Get the stored user ID */
    public String getUserId() {
        return prefs.getString(Constants.KEY_USER_ID, null);
    }

    /** Get the stored username */
    public String getUsername() {
        return prefs.getString(Constants.KEY_USERNAME, null);
    }

    /** Check if user is logged in */
    public boolean isLoggedIn() {
        return getToken() != null;
    }

    /** Clear all stored auth data (logout) */
    public void clearToken() {
        prefs.edit().clear().apply();
    }
}
