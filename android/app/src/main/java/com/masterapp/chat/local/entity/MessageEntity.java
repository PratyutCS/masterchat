package com.masterapp.chat.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages")
public class MessageEntity {
    
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

    public MessageEntity(@NonNull String msgUuid, String conversationId, String text, String senderId, Long sequenceId, String status, long localTimestamp, String sentAt, String readAt) {
        this.msgUuid = msgUuid;
        this.conversationId = conversationId;
        this.text = text;
        this.senderId = senderId;
        this.sequenceId = sequenceId;
        this.status = status;
        this.localTimestamp = localTimestamp;
        this.sentAt = sentAt;
        this.readAt = readAt;
    }
}
