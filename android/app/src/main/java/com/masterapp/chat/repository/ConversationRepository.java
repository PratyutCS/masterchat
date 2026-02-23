package com.masterapp.chat.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.masterapp.chat.api.ApiClient;
import com.masterapp.chat.models.Conversation;
import com.masterapp.chat.models.CreateConversationRequest;
import com.masterapp.chat.models.User;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository for conversation operations.
 * Bridges ViewModel ↔ Retrofit API.
 */
public class ConversationRepository {

    /**
     * Get all conversations for the current user.
     */
    public LiveData<List<Conversation>> getConversations() {
        MutableLiveData<List<Conversation>> result = new MutableLiveData<>();

        ApiClient.getConversationApi()
                .getConversations()
                .enqueue(new Callback<List<Conversation>>() {
                    @Override
                    public void onResponse(Call<List<Conversation>> call,
                                           Response<List<Conversation>> response) {
                        if (response.isSuccessful()) {
                            result.postValue(response.body());
                        } else {
                            result.postValue(null);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Conversation>> call, Throwable t) {
                        result.postValue(null);
                    }
                });

        return result;
    }

    /**
     * Create or find a 1-to-1 conversation with another user.
     */
    public LiveData<Conversation> createConversation(String recipientId) {
        MutableLiveData<Conversation> result = new MutableLiveData<>();

        ApiClient.getConversationApi()
                .createConversation(new CreateConversationRequest(recipientId))
                .enqueue(new Callback<Conversation>() {
                    @Override
                    public void onResponse(Call<Conversation> call,
                                           Response<Conversation> response) {
                        if (response.isSuccessful()) {
                            result.postValue(response.body());
                        } else {
                            result.postValue(null);
                        }
                    }

                    @Override
                    public void onFailure(Call<Conversation> call, Throwable t) {
                        result.postValue(null);
                    }
                });

        return result;
    }

    /**
     * Get all users (for starting new conversations).
     */
    public LiveData<List<User>> getUsers() {
        MutableLiveData<List<User>> result = new MutableLiveData<>();

        ApiClient.getUserApi()
                .getUsers()
                .enqueue(new Callback<List<User>>() {
                    @Override
                    public void onResponse(Call<List<User>> call,
                                           Response<List<User>> response) {
                        if (response.isSuccessful()) {
                            result.postValue(response.body());
                        } else {
                            result.postValue(null);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<User>> call, Throwable t) {
                        result.postValue(null);
                    }
                });
        return result;
    }

    public interface LoadCallback<T> {
        void onLoaded(T data);
    }

    public void fetchConversations(LoadCallback<List<Conversation>> callback) {
        ApiClient.getConversationApi()
                .getConversations()
                .enqueue(new Callback<List<Conversation>>() {
                    @Override
                    public void onResponse(Call<List<Conversation>> call, Response<List<Conversation>> response) {
                        callback.onLoaded(response.isSuccessful() ? response.body() : null);
                    }

                    @Override
                    public void onFailure(Call<List<Conversation>> call, Throwable t) {
                        callback.onLoaded(null);
                    }
                });
    }

    public void fetchUsers(LoadCallback<List<User>> callback) {
        ApiClient.getUserApi()
                .getUsers()
                .enqueue(new Callback<List<User>>() {
                    @Override
                    public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                        callback.onLoaded(response.isSuccessful() ? response.body() : null);
                    }

                    @Override
                    public void onFailure(Call<List<User>> call, Throwable t) {
                        callback.onLoaded(null);
                    }
                });
    }

    /**
     * Deletes a conversation from the server and cleans up local message cache.
     */
    public LiveData<Boolean> deleteConversation(android.content.Context context, String conversationId) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();

        ApiClient.getConversationApi()
                .deleteConversation(conversationId)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            // Server deletion success — now clean up local SQLite
                            new Thread(() -> {
                                com.masterapp.chat.local.AppDatabase db = 
                                    com.masterapp.chat.local.AppDatabase.getDatabase(context);
                                
                                // 1. Remove all local messages for this room
                                db.messageDao().deleteMessagesByConversation(conversationId);
                                
                                // 2. Remove sync checkpoint
                                db.syncCheckpointDao().deleteCheckpoint(conversationId);
                                
                                result.postValue(true);
                            }).start();
                        } else {
                            result.postValue(false);
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        result.postValue(false);
                    }
                });

        return result;
    }
}
