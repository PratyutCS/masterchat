package com.masterapp.chat.api;

import com.masterapp.chat.models.Conversation;
import com.masterapp.chat.models.CreateConversationRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

/**
 * Retrofit interface for conversation endpoints.
 */
public interface ConversationApi {

    /** Create or find an existing 1-to-1 conversation */
    @POST("/api/conversations")
    Call<Conversation> createConversation(@Body CreateConversationRequest request);

    /** Get all conversations for the current user */
    @GET("/api/conversations")
    Call<List<Conversation>> getConversations();

    /** Delete a conversation and its messages */
    @retrofit2.http.DELETE("/api/conversations/{id}")
    Call<Void> deleteConversation(@retrofit2.http.Path("id") String conversationId);
}
