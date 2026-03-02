package com.masterapp.chat.api;

import com.masterapp.chat.local.entity.MessageEntity;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface SyncApi {

    @POST("/api/sync/messages")
    Call<List<MessageEntity>> pushMessages(@Body List<MessageEntity> pendingMessages);

    @GET("/api/sync/messages")
    Call<List<MessageEntity>> pullMessages(
            @Query("conversationId") String conversationId,
            @Query("afterSequenceId") long afterSequenceId,
            @Query("updatedAfter") String updatedAfter
    );

    @GET("/api/sync/reconcile-ids")
    Call<ReconcileResponse> reconcileIds(@Query("conversationId") String conversationId);

    class ReconcileResponse {
        public List<String> msgUuids;
    }

    /**
     * Delivery acknowledgment: confirm that pulled messages have been
     * persisted locally. Server will then mark them as 'delivered'
     * and notify the sender.
     */
    @POST("/api/sync/ack")
    Call<AckResponse> acknowledgeDelivery(@Body AckRequest request);

    /** Request body for /api/sync/ack */
    class AckRequest {
        public String conversationId;
        public long maxSequenceId;

        public AckRequest(String conversationId, long maxSequenceId) {
            this.conversationId = conversationId;
            this.maxSequenceId = maxSequenceId;
        }
    }

    /** Response from /api/sync/ack */
    class AckResponse {
        public int acknowledged;
    }

    // ── Read-Receipt Sync ─────────────────────────────────────────

    /**
     * Batch push read watermarks to server.
     * Server will advance per-(user, conversation) watermarks and notify senders.
     */
    @POST("/api/sync/read-ack")
    Call<ReadAckResponse> acknowledgeReads(@Body List<ReadAckItem> readWatermarks);

    /** Single read watermark item in with batch */
    class ReadAckItem {
        public String conversationId;
        public long maxSequenceId;

        public ReadAckItem(String conversationId, long maxSequenceId) {
            this.conversationId = conversationId;
            this.maxSequenceId = maxSequenceId;
        }
    }

    /** Response from /api/sync/read-ack */
    class ReadAckResponse {
        public int acked;
    }

    @GET("/api/sync/watermarks")
    Call<List<Watermark>> getWatermarks(@Query("conversationId") String conversationId);

    class Watermark {
        public String conversationId;
        public String userId;
        public long readUpToSeq;
        public String updatedAt;
    }
}
