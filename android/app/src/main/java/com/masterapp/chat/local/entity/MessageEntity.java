package com.masterapp.chat.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages")
public class MessageEntity {
    
    // --- EXISTING ---
    @PrimaryKey
    @NonNull
    public String msgUuid;
    
    public String conversationId;
    public String text;
    public String senderId;
    
    // Server-assigned ordering (nullable locally for PENDING messages)
    public Long sequenceId;
    
    // sent, delivered, read, or PENDING (for offline sends)
    public String status;
    
    public long localTimestamp;
    public String sentAt;
    public String readAt;

    // --- NEW ---
    public String receiverId;
    public String content;
    public String deliveredAt;
    public String seenAt;
    public boolean isRead;
    public String updatedAt;

    // --- EXISTING / MERGED Constructor ---
    public MessageEntity(@NonNull String msgUuid, String conversationId, String text, 
                         String senderId, Long sequenceId, String status, 
                         long localTimestamp, String sentAt, String readAt, 
                         String receiverId, String content, String deliveredAt, 
                         String seenAt, boolean isRead, String updatedAt) {
        this.msgUuid = msgUuid;
        this.conversationId = conversationId;
        this.text = text;
        this.senderId = senderId;
        this.sequenceId = sequenceId;
        this.status = status;
        this.localTimestamp = localTimestamp;
        this.sentAt = sentAt;
        this.readAt = readAt;
        
        // Bind new fields
        this.receiverId = receiverId;
        this.content = content;
        this.deliveredAt = deliveredAt;
        this.seenAt = seenAt;
        this.isRead = isRead;
        this.updatedAt = updatedAt;
    }
}
