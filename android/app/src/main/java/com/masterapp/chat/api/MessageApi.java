package com.masterapp.chat.api;

import com.masterapp.chat.models.MessageListResponse;
import com.masterapp.chat.local.entity.MessageEntity;

import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Retrofit interface for message endpoints.
 */
public interface MessageApi {

    /** Get paginated messages for a conversation */
    @GET("/api/messages/{conversationId}")
    Call<MessageListResponse> getMessages(
            @Path("conversationId") String conversationId,
            @Query("page") int page,
            @Query("limit") int limit
    );

    /** Batch sync messages via Outbox Pattern */
    @POST("/api/messages/batch")
    Call<ResponseBody> syncBatchMessages(
            @Body Map<String, List<MessageEntity>> batchPayload
    );

    /** Incremental pull of updated messages */
    @GET("/api/messages/updates")
    Call<com.masterapp.chat.models.IncrementalPullResponse> getIncrementalUpdates(
            @Query("since") long sinceTimestamp,
            @Query("limit") int limit
    );
}
