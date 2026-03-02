package com.masterapp.chat.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.masterapp.chat.local.entity.SyncQueueEntity;

import java.util.List;

@Dao
public interface SyncQueueDao {

    @Insert
    void insert(SyncQueueEntity syncQueueEntity);

    @Query("SELECT * FROM sync_queue ORDER BY createdAt ASC LIMIT :max")
    List<SyncQueueEntity> getPendingSyncQueue(int max);

    @Query("DELETE FROM sync_queue WHERE id IN (:ids)")
    void deleteSyncItems(List<Integer> ids);

    @Query("SELECT * FROM sync_queue WHERE id = :id")
    SyncQueueEntity getById(int id);

    @Query("UPDATE sync_queue SET retryCount = retryCount + 1 WHERE id = :id")
    void incrementRetryCount(int id);

    @Query("DELETE FROM sync_queue WHERE id = :id")
    void deleteById(int id);

    @Query("SELECT * FROM sync_queue WHERE entityId = :entityId")
    SyncQueueEntity getByEntityId(String entityId);
}
