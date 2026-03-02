package com.masterapp.chat.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.masterapp.chat.local.entity.ReadOutbox;

import java.util.List;

@Dao
public interface ReadOutboxDao {

    /**
     * Upsert a read watermark. If the conversation already exists in the outbox,
     * only update if the new maxSequenceId is higher (monotonic advance).
     */
    @Query("INSERT INTO read_outbox (conversationId, maxSequenceId, localTimestamp, synced) " +
           "VALUES (:convId, :maxSeqId, :timestamp, 0) " +
           "ON CONFLICT(conversationId) DO UPDATE SET " +
           "maxSequenceId = MAX(read_outbox.maxSequenceId, :maxSeqId), " +
           "localTimestamp = :timestamp, " +
           "synced = 0")
    void upsertReadWatermark(String convId, long maxSeqId, long timestamp);

    /** Get all pending (un-acked) read watermarks for sync */
    @Query("SELECT * FROM read_outbox WHERE synced = 0")
    List<ReadOutbox> getPendingReadEvents();

    /** Mark a conversation's read watermark as acked by server up to a specific sequenceId */
    @Query("UPDATE read_outbox SET synced = 1 WHERE conversationId = :convId AND maxSequenceId <= :ackSeqId")
    void markAcked(String convId, long ackSeqId);

    /** Mark multiple conversations as acked up to their respective values */
    default void markAckedBatchSafe(List<ReadOutbox> items) {
        for (ReadOutbox item : items) {
            markAcked(item.conversationId, item.maxSequenceId);
        }
    }

    /** Get the current watermark for a conversation (for UI display) */
    @Query("SELECT maxSequenceId FROM read_outbox WHERE conversationId = :convId")
    Long getWatermark(String convId);

    /** Delete outbox entry for a conversation (when conversation is deleted) */
    @Query("DELETE FROM read_outbox WHERE conversationId = :convId")
    void deleteForConversation(String convId);
}
