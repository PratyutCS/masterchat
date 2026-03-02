package com.masterapp.chat.socket;

import android.util.Log;

import com.masterapp.chat.util.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * Singleton manager for Socket.IO connection.
 * 
 * Lifecycle:
 *   1. Call connect() after login to establish connection
 *   2. Call authenticate(token) to verify identity on server
 *   3. Call joinConversation(conversationId) when entering a chat
 *   4. Call sendMessage() to send messages
 *   5. Call disconnect() on logout
 * 
 * Register listeners via on() / off() for events like:
 *   - receive_message
 *   - message_delivered
 *   - message_read
 *   - user_online / user_offline
 */
public class SocketManager {

    private static final String TAG = "SocketManager";
    private static SocketManager instance;
    private Socket socket;
    private String authToken;

    // LiveData Event Bus for global notifications
    private final androidx.lifecycle.MutableLiveData<org.json.JSONObject> newMessageEvents = new androidx.lifecycle.MutableLiveData<>();
    private final androidx.lifecycle.MutableLiveData<org.json.JSONObject> messageReadEvents = new androidx.lifecycle.MutableLiveData<>();
    private final androidx.lifecycle.MutableLiveData<org.json.JSONObject> messageDeliveredEvents = new androidx.lifecycle.MutableLiveData<>();
    private final androidx.lifecycle.MutableLiveData<org.json.JSONObject> conversationDeletedEvents = new androidx.lifecycle.MutableLiveData<>();
    private final androidx.lifecycle.MutableLiveData<org.json.JSONObject> globalSyncEvents = new androidx.lifecycle.MutableLiveData<>();
    private final androidx.lifecycle.MutableLiveData<org.json.JSONObject> messageDeletedFromAdminEvents = new androidx.lifecycle.MutableLiveData<>();

    private SocketManager() {
        // Private constructor
    }

    public static synchronized SocketManager getInstance() {
        if (instance == null) {
            instance = new SocketManager();
        }
        return instance;
    }

    public androidx.lifecycle.LiveData<org.json.JSONObject> getNewMessageEvents() { return newMessageEvents; }
    public androidx.lifecycle.LiveData<org.json.JSONObject> getMessageReadEvents() { return messageReadEvents; }
    public androidx.lifecycle.LiveData<org.json.JSONObject> getMessageDeliveredEvents() { return messageDeliveredEvents; }
    public androidx.lifecycle.LiveData<org.json.JSONObject> getConversationDeletedEvents() { return conversationDeletedEvents; }
    public androidx.lifecycle.LiveData<org.json.JSONObject> getGlobalSyncEvents() { return globalSyncEvents; }
    public androidx.lifecycle.LiveData<org.json.JSONObject> getMessageDeletedFromAdminEvents() { return messageDeletedFromAdminEvents; }


    /**
     * Connect to the Socket.IO server.
     * Call this once after login.
     */
    public void connect() {
        if (socket != null) {
            if (!socket.connected()) {
                Log.d(TAG, "Socket exists but disconnected, calling connect()");
                socket.connect();
            }
            return;
        }

        try {
            IO.Options options = new IO.Options();
            options.forceNew = false; // Don't force new if we want stable object
            options.reconnection = true;
            options.reconnectionAttempts = 100;
            options.reconnectionDelay = 2000;

            socket = IO.socket(Constants.BASE_URL, options);

            socket.on(Socket.EVENT_CONNECT, args -> {
                Log.d(TAG, "Socket connected. Session: " + socket.id());
                if (authToken != null) {
                    authenticate(authToken);
                }
            });

            // Register global event listeners once
            socket.on("new_message_available", args -> {
                if (args.length > 0 && args[0] instanceof org.json.JSONObject) {
                    newMessageEvents.postValue((org.json.JSONObject) args[0]);
                }
            });

            socket.on("message_read", args -> {
                if (args.length > 0 && args[0] instanceof org.json.JSONObject) {
                    messageReadEvents.postValue((org.json.JSONObject) args[0]);
                }
            });

            socket.on("message_delivered", args -> {
                if (args.length > 0 && args[0] instanceof org.json.JSONObject) {
                    messageDeliveredEvents.postValue((org.json.JSONObject) args[0]);
                }
            });

            socket.on("conversation_deleted", args -> {
                if (args.length > 0 && args[0] instanceof org.json.JSONObject) {
                    conversationDeletedEvents.postValue((org.json.JSONObject) args[0]);
                }
            });

            socket.on("global_sync_required", args -> {
                if (args.length > 0 && args[0] instanceof org.json.JSONObject) {
                    globalSyncEvents.postValue((org.json.JSONObject) args[0]);
                }
            });

            socket.on("message_deleted_from_admin", args -> {
                if (args.length > 0 && args[0] instanceof org.json.JSONObject) {
                    messageDeletedFromAdminEvents.postValue((org.json.JSONObject) args[0]);
                }
            });

            socket.on(Socket.EVENT_DISCONNECT, args -> {
                Log.d(TAG, "Socket disconnected");
            });
            socket.on(Socket.EVENT_CONNECT_ERROR, args -> {
                Log.e(TAG, "Socket connection error: " +
                        (args.length > 0 ? args[0].toString() : "unknown"));
            });

            socket.connect();
            Log.d(TAG, "Connecting to " + Constants.BASE_URL);

        } catch (URISyntaxException e) {
            Log.e(TAG, "Invalid socket URL: " + e.getMessage());
        }
    }

    /**
     * Authenticate the socket connection with a JWT token.
     * Call this right after connect().
     */
    public void authenticate(String token) {
        if (token == null) return;
        this.authToken = token; // Store for reconnection
        
        if (socket == null) {
            connect(); // Ensure we have a socket
            return;
        }

        try {
            JSONObject data = new JSONObject();
            data.put("token", token);
            if (socket != null) {
                socket.emit("authenticate", data);
                Log.d(TAG, "Sent authenticate event");
            } else {
                Log.w(TAG, "Socket is null, cannot authenticate");
            }
        } catch (JSONException e) {
            Log.e(TAG, "Auth JSON error: " + e.getMessage());
        }
    }

    /**
     * Join a conversation room.
     * Call this when the user opens a chat screen.
     */
    public void joinConversation(String conversationId) {
        if (socket == null) return;
        try {
            JSONObject data = new JSONObject();
            data.put("conversationId", conversationId);
            socket.emit("join_conversation", data);
            Log.d(TAG, "Joined conversation: " + conversationId);
        } catch (JSONException e) {
            Log.e(TAG, "Join JSON error: " + e.getMessage());
        }
    }

    /**
     * Send a message to a conversation.
     */
    public void sendMessage(String conversationId, String text) {
        if (socket == null) return;
        try {
            JSONObject data = new JSONObject();
            data.put("conversationId", conversationId);
            data.put("text", text);
            socket.emit("send_message", data);
            Log.d(TAG, "Sent message to: " + conversationId);
        } catch (JSONException e) {
            Log.e(TAG, "Send JSON error: " + e.getMessage());
        }
    }

    /**
     * Emit a raw event with simple string data (like typing indicators)
     */
    public void emit(String event, String data) {
        if (socket == null) return;
        try {
            JSONObject payload = new JSONObject();
            payload.put("conversationId", data); // Special casing since we only map this to conversationId currently
            socket.emit(event, payload);
        } catch (JSONException e) {
            Log.e(TAG, "Emit JSON error: " + e.getMessage());
        }
    }

    /**
     * Mark messages as read in a conversation.
     */
    public void markRead(String conversationId, String[] messageIds, Long sequenceId) {
        if (socket == null) return;
        try {
            JSONObject data = new JSONObject();
            data.put("conversationId", conversationId);
            
            if (messageIds != null && messageIds.length > 0) {
                org.json.JSONArray ids = new org.json.JSONArray();
                for (String id : messageIds) {
                    ids.put(id);
                }
                data.put("messageIds", ids);
            }

            if (sequenceId != null) {
                data.put("sequenceId", sequenceId);
            }

            socket.emit("mark_read", data);
            Log.d(TAG, "Marked read in " + conversationId + " (seq: " + sequenceId + ")");
        } catch (JSONException e) {
            Log.e(TAG, "Mark read JSON error: " + e.getMessage());
        }
    }

    /**
     * Register a listener for a socket event.
     */
    public void on(String event, Emitter.Listener listener) {
        if (socket != null) {
            socket.on(event, listener);
        }
    }

    /**
     * Remove a listener for a socket event.
     */
    public void off(String event, Emitter.Listener listener) {
        if (socket != null) {
            socket.off(event, listener);
        }
    }

    /**
     * Remove all listeners for a socket event.
     */
    public void off(String event) {
        if (socket != null) {
            socket.off(event);
        }
    }

    /**
     * Disconnect from the server.
     * Call this on logout.
     */
    public void disconnect() {
        if (socket != null) {
            socket.disconnect();
            socket.off();
            socket = null;
            Log.d(TAG, "Socket disconnected and cleaned up");
        }
    }

    /** Check if currently connected */
    public boolean isConnected() {
        return socket != null && socket.connected();
    }
}
