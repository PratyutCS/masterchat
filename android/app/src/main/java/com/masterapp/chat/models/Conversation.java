package com.masterapp.chat.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Conversation model matching the backend Conversation schema.
 * Includes populated members and optional lastMessage.
 */
public class Conversation {
    @SerializedName("_id")
    private String id;

    private List<User> members;
    private Message lastMessage;
    private int unreadCount;
    private String createdAt;
    private String updatedAt;

    // ----- Getters -----
    public String getId() { return id; }
    public List<User> getMembers() { return members; }
    public Message getLastMessage() { return lastMessage; }
    public int getUnreadCount() { return unreadCount; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }

    // ----- Setters -----
    public void setId(String id) { this.id = id; }
    public void setMembers(List<User> members) { this.members = members; }
    public void setLastMessage(Message lastMessage) { this.lastMessage = lastMessage; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    /**
     * Helper: get the other user in a 1-to-1 conversation.
     * @param myUserId the current user's ID
     * @return the other User, or null if not found
     */
    public User getOtherMember(String myUserId) {
        if (members == null || myUserId == null) return null;
        for (User member : members) {
            if (member == null) continue;
            String memberId = member.getId();
            if (memberId != null && !memberId.equals(myUserId)) {
                return member;
            }
        }
        return null;
    }
}
