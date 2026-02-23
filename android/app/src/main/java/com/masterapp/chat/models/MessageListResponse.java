package com.masterapp.chat.models;

import java.util.List;

/**
 * Response wrapper for GET /api/messages/:conversationId
 * Contains paginated messages.
 */
public class MessageListResponse {
    private List<Message> messages;
    private int page;
    private int totalPages;
    private int totalMessages;

    public List<Message> getMessages() { return messages; }
    public int getPage() { return page; }
    public int getTotalPages() { return totalPages; }
    public int getTotalMessages() { return totalMessages; }
}
