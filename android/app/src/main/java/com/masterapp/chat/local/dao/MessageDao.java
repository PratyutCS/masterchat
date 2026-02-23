package com.masterapp.chat.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.masterapp.chat.local.entity.MessageEntity;

import java.util.List;

@Dao
public interface MessageDao {

    // Upsert message (on conflict, replace with server's truth)
    // WARNING: Use safeUpsertFromServer() for downstream sync to avoid status downgrades
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrReplace(MessageEntity message);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrReplaceAll(List<MessageEntity> messages);

    // Insert only if the message doesn't already exist (safe for batch pulls)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertIfAbsent(MessageEntity message);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertIfAbsentAll(List<MessageEntity> messages);

    /**
     * Conflict-safe upsert for downstream sync.
     * Updates sequenceId and text from server, but ONLY advances the status forward.
     * Status priority: PENDING(0) < sent(1) < delivered(2) < read(3)
     * If the server returns status='sent' but we already have status='read', we keep 'read'.
     */
    @Query("UPDATE messages SET " +
           "sequenceId = :sequenceId, " +
           "sentAt = :sentAt, " +
           "readAt = :readAt, " +
           "status = CASE " +
           "  WHEN status = 'read' THEN 'read' " +
           "  WHEN status = 'delivered' AND :newStatus IN ('sent', 'PENDING') THEN 'delivered' " +
           "  WHEN status = 'sent' AND :newStatus = 'PENDING' THEN 'sent' " +
           "  ELSE :newStatus " +
           "END " +
           "WHERE msgUuid = :msgUuid")
    void safeUpsertFromServer(String msgUuid, Long sequenceId, String newStatus, String sentAt, String readAt);

    // Get all messages for a conversation, perfectly ordered by sequence ID, falling back to local timestamp for pending
    @Query("SELECT * FROM messages WHERE conversationId = :convId ORDER BY COALESCE(sequenceId, 999999999) ASC, localTimestamp ASC")
    LiveData<List<MessageEntity>> getMessagesForConversation(String convId);

    // Get strictly the highest known sequence ID for this conversation to use in sync pulls
    @Query("SELECT MAX(sequenceId) FROM messages WHERE conversationId = :convId")
    Long getHighestSequenceId(String convId);

    // Fetch messages the user tried to send while offline
    @Query("SELECT * FROM messages WHERE status = 'PENDING' ORDER BY localTimestamp ASC")
    List<MessageEntity> getPendingMessages();

    // Fetch pending messages for a specific conversation only
    @Query("SELECT * FROM messages WHERE status = 'PENDING' AND conversationId = :convId ORDER BY localTimestamp ASC")
    List<MessageEntity> getPendingMessagesForConversation(String convId);

    // Get distinct conversation IDs that have pending messages
    @Query("SELECT DISTINCT conversationId FROM messages WHERE status = 'PENDING'")
    List<String> getConversationsWithPendingMessages();

    // Update statuses (e.g. from PENDING to SENT) — monotonic guard
    @Query("UPDATE messages SET status = :newStatus, sequenceId = :sequenceId WHERE msgUuid = :msgUuid AND status != 'read'")
    void updateMessageStatusAndSeq(String msgUuid, String newStatus, Long sequenceId);

    // Locally mark multiple messages as read instantly
    @Query("UPDATE messages SET status = 'read' WHERE msgUuid IN (:msgUuids)")
    void markMessagesAsReadLocal(List<String> msgUuids);

    @Query("UPDATE messages SET status = :status, readAt = :readAt " +
           "WHERE conversationId = :convId AND sequenceId <= :maxSeq AND senderId = :myUserId AND status != 'read'")
    void updateOtherUserReadWatermark(String convId, long maxSeq, String status, String readAt, String myUserId);

    @Query("UPDATE messages SET status = 'read', readAt = :readAt " +
           "WHERE conversationId = :convId AND sequenceId <= :maxSeq AND senderId != :myUserId AND status != 'read'")
    void updateMyOwnReadWatermark(String convId, long maxSeq, String readAt, String myUserId);

    // Check if a message exists locally
    @Query("SELECT COUNT(*) FROM messages WHERE msgUuid = :msgUuid")
    int messageExists(String msgUuid);

    // Delete all messages for a specific conversation
    @Query("DELETE FROM messages WHERE conversationId = :convId")
    void deleteMessagesByConversation(String convId);
}
