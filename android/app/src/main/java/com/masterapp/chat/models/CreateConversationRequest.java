package com.masterapp.chat.models;

/**
 * Request body for POST /api/conversations
 */
public class CreateConversationRequest {
    private String recipientId;

    public CreateConversationRequest(String recipientId) {
        this.recipientId = recipientId;
    }

    public String getRecipientId() { return recipientId; }
}
