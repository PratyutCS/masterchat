package com.masterapp.chat.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.masterapp.chat.api.ApiClient;
import com.masterapp.chat.local.AppDatabase;
import com.masterapp.chat.local.entity.ConversationEntity;
import com.masterapp.chat.local.entity.SyncQueueEntity;
import com.masterapp.chat.models.Conversation;
import com.masterapp.chat.models.CreateConversationRequest;
import com.masterapp.chat.models.User;
import com.masterapp.chat.sync.DownstreamSyncWorker;
import com.masterapp.chat.sync.SyncWorker;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository for conversation operations.
 * Bridges ViewModel ↔ Retrofit API ↔ Room.
 */
public class ConversationRepository {

    private final com.masterapp.chat.local.dao.ConversationDao conversationDao;
    private final AppDatabase db;

    public ConversationRepository(android.content.Context context) {
        this.db = AppDatabase.getDatabase(context);
        this.conversationDao = db.conversationDao();
    }

    public LiveData<List<ConversationEntity>> getConversations() {
        return conversationDao.getAllConversations();
    }

    public void refreshConversations() {
        OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(com.masterapp.chat.sync.LinearSyncWorker.class).build();
        WorkManager.getInstance(com.masterapp.chat.ChatApplication.getAppContext())
                .enqueueUniqueWork("LinearMasterSyncManual", ExistingWorkPolicy.REPLACE, syncRequest);
    }

    public LiveData<Conversation> createConversation(String recipientId) {
        MutableLiveData<Conversation> result = new MutableLiveData<>();
        ApiClient.getConversationApi().createConversation(new CreateConversationRequest(recipientId))
                .enqueue(new Callback<Conversation>() {
                    @Override
                    public void onResponse(Call<Conversation> call, Response<Conversation> response) {
                        result.postValue(response.isSuccessful() ? response.body() : null);
                    }
                    @Override
                    public void onFailure(Call<Conversation> call, Throwable t) {
                        result.postValue(null);
                    }
                });
        return result;
    }

    public LiveData<List<User>> getUsers() {
        MutableLiveData<List<User>> result = new MutableLiveData<>();
        ApiClient.getUserApi().getUsers().enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                result.postValue(response.isSuccessful() ? response.body() : null);
            }
            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                result.postValue(null);
            }
        });
        return result;
    }

    /**
     * Offline-First Deletion:
     * 1. Remove from local Room immediately.
     * 2. Add 'DELETE_CONV' to sync_queue.
     * 3. Trigger SyncWorker.
     */
    public LiveData<Boolean> deleteConversation(android.content.Context context, String conversationId) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();

        new Thread(() -> {
            try {
                db.runInTransaction(() -> {
                    // 1. Local cleanup
                    db.messageDao().deleteMessagesByConversation(conversationId);
                    db.syncCheckpointDao().deleteCheckpoint(conversationId);
                    db.conversationDao().deleteById(conversationId);

                    // 2. Queue for server sync
                    SyncQueueEntity syncEntry = new SyncQueueEntity(conversationId, "DELETE_CONV", System.currentTimeMillis());
                    db.syncQueueDao().insert(syncEntry);
                });

                // 3. Trigger Master Sync
                OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(com.masterapp.chat.sync.LinearSyncWorker.class).build();
                WorkManager.getInstance(context).enqueueUniqueWork("LinearMasterSyncUpstream", ExistingWorkPolicy.REPLACE, syncRequest);

                result.postValue(true);
            } catch (Exception e) {
                result.postValue(false);
            }
        }).start();

        return result;
    }

    public void resetSync() {
        db.runInTransaction(() -> {
            db.messageDao().deleteAll();
            db.conversationDao().deleteAll();
            db.syncCheckpointDao().deleteAll();
            refreshConversations();
        });
    }

    public void fetchUsers(LoadCallback<List<User>> callback) {
        ApiClient.getUserApi().getUsers().enqueue(new Callback<List<User>>() {
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

    public interface LoadCallback<T> {
        void onLoaded(T data);
    }
}
