package com.masterapp.chat.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "conversations")
public class ConversationEntity {
    @PrimaryKey
    @NonNull
    public String id;
    public String title;
    public String otherUserId;
    public String lastMessage;
    public int unreadCount;
    public long updatedAt;

    public ConversationEntity(@NonNull String id, String title, String otherUserId, String lastMessage, int unreadCount, long updatedAt) {
        this.id = id;
        this.title = title;
        this.otherUserId = otherUserId;
        this.lastMessage = lastMessage;
        this.unreadCount = unreadCount;
        this.updatedAt = updatedAt;
    }
}
