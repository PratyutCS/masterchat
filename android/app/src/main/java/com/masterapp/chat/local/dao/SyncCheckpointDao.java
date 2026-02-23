package com.masterapp.chat.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.masterapp.chat.local.entity.SyncCheckpoint;

/**
 * DAO for managing sync checkpoints.
 * 
 * Each conversation has a checkpoint tracking the last successfully
 * synced sequence ID. This survives local message deletions and
 * is more reliable than MAX(sequenceId).
 */
@Dao
public interface SyncCheckpointDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(SyncCheckpoint checkpoint);

    @Query("SELECT lastPulledSeq FROM sync_checkpoints WHERE conversationId = :convId")
    Long getLastPulledSeq(String convId);

    @Query("UPDATE sync_checkpoints SET lastPulledSeq = :seq, updatedAt = :now WHERE conversationId = :convId")
    void updatePulledSeq(String convId, long seq, long now);

    @Query("UPDATE sync_checkpoints SET lastPushedAt = :now, updatedAt = :now WHERE conversationId = :convId")
    void updatePushedAt(String convId, long now);

    /** Initialize a checkpoint for a conversation if it doesn't exist */
    @Query("INSERT OR IGNORE INTO sync_checkpoints (conversationId, lastPulledSeq, lastPushedAt, updatedAt) VALUES (:convId, 0, 0, :now)")
    void ensureExists(String convId, long now);

    @Query("DELETE FROM sync_checkpoints WHERE conversationId = :convId")
    void deleteCheckpoint(String convId);
}
