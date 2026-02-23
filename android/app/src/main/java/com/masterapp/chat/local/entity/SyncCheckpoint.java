package com.masterapp.chat.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Tracks the last successfully synced sequence ID per conversation.
 * 
 * This is more robust than recalculating MAX(sequenceId) on every sync,
 * because it survives local message deletions and partial syncs.
 */
@Entity(tableName = "sync_checkpoints")
public class SyncCheckpoint {

    @PrimaryKey
    @NonNull
    public String conversationId;

    /** The highest sequenceId we have successfully pulled from the server */
    public long lastPulledSeq;

    /** Epoch millis of the last successful upstream push for this conversation */
    public long lastPushedAt;

    /** Epoch millis of when this checkpoint was last updated */
    public long updatedAt;

    public SyncCheckpoint(@NonNull String conversationId, long lastPulledSeq, long lastPushedAt, long updatedAt) {
        this.conversationId = conversationId;
        this.lastPulledSeq = lastPulledSeq;
        this.lastPushedAt = lastPushedAt;
        this.updatedAt = updatedAt;
    }
}
