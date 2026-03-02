package com.masterapp.chat.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.AndroidViewModel;

import com.masterapp.chat.models.Conversation;
import com.masterapp.chat.models.User;
import com.masterapp.chat.repository.ConversationRepository;

import java.util.List;

/**
 * ViewModel for the ConversationList screen.
 * Manages conversation list and user list for starting new chats.
 */
public class ConversationListViewModel extends AndroidViewModel {

    private final ConversationRepository repository;
    private final LiveData<List<com.masterapp.chat.local.entity.ConversationEntity>> localConversations;

    private final MutableLiveData<List<Conversation>> conversations = new MutableLiveData<>();
    private final MutableLiveData<List<User>> users = new MutableLiveData<>();
    private boolean isPolling = false;

    public ConversationListViewModel(android.app.Application application) {
        super(application);
        this.repository = new ConversationRepository(application);
        this.localConversations = repository.getConversations();
        // Repository and WorkManager handle background sync. 
        // No manual polling needed here anymore.
    }

    /** Load conversations for the current user (Offline-First) */
    public LiveData<List<com.masterapp.chat.local.entity.ConversationEntity>> getLocalConversations() {
        return localConversations;
    }

    /** Load conversations for the current user */
    public LiveData<List<Conversation>> getConversations() {
        return conversations;
    }

    public void refreshConversations() {
        repository.refreshConversations();
    }

    /** Load all users (for new conversation) */
    public LiveData<List<User>> getUsers() {
        return users;
    }

    /**
     * Fetch users from server and return a fresh LiveData.
     * Unlike getUsers(), this actually triggers the API call every time.
     */
    public LiveData<List<User>> loadAndGetUsers() {
        return repository.getUsers();
    }

    public void refreshUsers() {
        repository.fetchUsers(newData -> {
            if (newData != null) {
                users.postValue(newData);
            }
        });
    }

    /** Create or find a conversation with a specific user */
    public LiveData<Conversation> createConversation(String recipientId) {
        return repository.createConversation(recipientId);
    }

    /** Delete a conversation and all its messages from server + local DB */
    public LiveData<Boolean> deleteConversation(android.content.Context context, String conversationId) {
        return repository.deleteConversation(context, conversationId);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        isPolling = false;
    }
}
