package com.masterapp.chat.models;

/**
 * Response body from POST /api/auth/login and /api/auth/register
 */
public class AuthResponse {
    private String token;
    private User user;

    public String getToken() { return token; }
    public User getUser() { return user; }
}
