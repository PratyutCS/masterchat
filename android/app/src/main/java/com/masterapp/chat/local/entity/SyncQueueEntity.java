package com.masterapp.chat.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Sync Queue for Upstream Operations (Client -> Server)
 * Supports CREATE_MSG, UPDATE_MSG, DELETE_CONV
 */
@Entity(tableName = "sync_queue")
public class SyncQueueEntity {
    
    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    public String entityId; // msgUuid or conversationId

    @NonNull
    public String operation; // "CREATE_MSG", "UPDATE_MSG", "DELETE_CONV"

    public int retryCount;

    public long createdAt;

    public SyncQueueEntity(@NonNull String entityId, @NonNull String operation, long createdAt) {
        this.entityId = entityId;
        this.operation = operation;
        this.retryCount = 0;
        this.createdAt = createdAt;
    }
}
