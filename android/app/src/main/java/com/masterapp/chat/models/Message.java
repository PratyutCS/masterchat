package com.masterapp.chat.models;

import com.google.gson.annotations.SerializedName;

/**
 * Message model matching the backend Message schema.
 */
public class Message {
    @SerializedName("_id")
    private String id;

    private String conversationId;

    // senderId can be either a string (ID) or a populated User object.
    // When populated, Gson will parse it as a User.
    private User senderId;

    private String text;
    private String status; // "sent", "delivered", "read"
    private String createdAt;
    private String sentAt;
    private String readAt;

    // ----- Getters -----
    public String getId() { return id; }
    public String getConversationId() { return conversationId; }
    public User getSenderId() { return senderId; }
    public String getText() { return text; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
    public String getSentAt() { return sentAt; }
    public String getReadAt() { return readAt; }

    // ----- Setters -----
    public void setId(String id) { this.id = id; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public void setSenderId(User senderId) { this.senderId = senderId; }
    public void setText(String text) { this.text = text; }
    public void setStatus(String status) { this.status = status; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public void setSentAt(String sentAt) { this.sentAt = sentAt; }
    public void setReadAt(String readAt) { this.readAt = readAt; }

    /**
     * Helper: check if this message was sent by the given user.
     */
    public boolean isSentBy(String userId) {
        return senderId != null && senderId.getId() != null && senderId.getId().equals(userId);
    }
}
