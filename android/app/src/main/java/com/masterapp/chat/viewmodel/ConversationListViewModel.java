package com.masterapp.chat.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.masterapp.chat.models.Conversation;
import com.masterapp.chat.models.User;
import com.masterapp.chat.repository.ConversationRepository;

import java.util.List;

/**
 * ViewModel for the ConversationList screen.
 * Manages conversation list and user list for starting new chats.
 */
public class ConversationListViewModel extends ViewModel {

    private final ConversationRepository repository = new ConversationRepository();
    private final MutableLiveData<List<Conversation>> conversations = new MutableLiveData<>();
    private final MutableLiveData<List<User>> users = new MutableLiveData<>();

    /** Load conversations for the current user */
    public LiveData<List<Conversation>> getConversations() {
        return conversations;
    }

    public void refreshConversations() {
        repository.fetchConversations(newData -> {
            if (newData != null) {
                conversations.postValue(newData);
            }
        });
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
}
