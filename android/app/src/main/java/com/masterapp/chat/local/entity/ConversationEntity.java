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
    public String lastMessage;
    public long updatedAt;

    public ConversationEntity(@NonNull String id, String title, String lastMessage, long updatedAt) {
        this.id = id;
        this.title = title;
        this.lastMessage = lastMessage;
        this.updatedAt = updatedAt;
    }
}
