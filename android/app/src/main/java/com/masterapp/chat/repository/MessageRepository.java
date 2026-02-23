package com.masterapp.chat.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.masterapp.chat.api.ApiClient;
import com.masterapp.chat.models.MessageListResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository for message operations.
 * Bridges ViewModel ↔ Retrofit API.
 */
public class MessageRepository {

    /**
     * Get paginated messages for a conversation.
     */
    public LiveData<MessageListResponse> getMessages(String conversationId, int page, int limit) {
        MutableLiveData<MessageListResponse> result = new MutableLiveData<>();

        ApiClient.getMessageApi()
                .getMessages(conversationId, page, limit)
                .enqueue(new Callback<MessageListResponse>() {
                    @Override
                    public void onResponse(Call<MessageListResponse> call,
                                           Response<MessageListResponse> response) {
                        if (response.isSuccessful()) {
                            result.postValue(response.body());
                        } else {
                            result.postValue(null);
                        }
                    }

                    @Override
                    public void onFailure(Call<MessageListResponse> call, Throwable t) {
                        result.postValue(null);
                    }
                });

        return result;
    }
}
