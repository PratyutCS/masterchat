package com.masterapp.chat.models;

import com.masterapp.chat.local.entity.MessageEntity;
import java.util.List;

/**
 * Response wrapper for incremental pull.
 */
public class IncrementalPullResponse {
    private boolean success;
    private List<MessageEntity> data;

    public boolean isSuccess() { return success; }
    public List<MessageEntity> getData() { return data; }
}
