package com.masterapp.chat.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Read Outbox — durable persistence of read events.
 *
 * Primary key is conversationId because we only need the latest watermark
 * per conversation. INSERT OR REPLACE with a higher maxSequenceId naturally
 * compresses multiple reads into one watermark.
 */
@Entity(tableName = "read_outbox")
public class ReadOutbox {

    @PrimaryKey
    @NonNull
    public String conversationId;

    /** Highest sequenceId the user has read in this conversation */
    public long maxSequenceId;

    /** Client timestamp when the read event was recorded */
    public long localTimestamp;

    /** 0 = pending (needs sync), 1 = acked by server */
    public int synced;

    public ReadOutbox(@NonNull String conversationId, long maxSequenceId, long localTimestamp, int synced) {
        this.conversationId = conversationId;
        this.maxSequenceId = maxSequenceId;
        this.localTimestamp = localTimestamp;
        this.synced = synced;
    }
}
