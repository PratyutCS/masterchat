package com.masterapp.chat.api;

import com.masterapp.chat.models.MessageListResponse;

import retrofit2.Call;
import retrofit2.http.GET;
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
}
