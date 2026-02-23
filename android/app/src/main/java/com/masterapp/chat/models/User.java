package com.masterapp.chat.models;

import com.google.gson.annotations.SerializedName;

/**
 * User model matching the backend User schema.
 */
public class User {
    @SerializedName("_id")
    private String id;

    private String username;
    private String email;
    private String lastSeen;
    private String createdAt;

    // ----- Getters -----
    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getLastSeen() { return lastSeen; }
    public String getCreatedAt() { return createdAt; }

    // ----- Setters -----
    public void setId(String id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setLastSeen(String lastSeen) { this.lastSeen = lastSeen; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
