package com.masterapp.chat.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.masterapp.chat.local.entity.ConversationEntity;

import java.util.List;

@Dao
public interface ConversationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrReplaceAll(List<ConversationEntity> conversations);

    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    LiveData<List<ConversationEntity>> getAllConversations();

    @Query("DELETE FROM conversations WHERE id = :id")
    void deleteById(String id);
}
