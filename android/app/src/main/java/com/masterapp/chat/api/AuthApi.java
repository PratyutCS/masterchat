package com.masterapp.chat.api;

import com.masterapp.chat.models.AuthResponse;
import com.masterapp.chat.models.LoginRequest;
import com.masterapp.chat.models.RegisterRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Retrofit interface for authentication endpoints.
 */
public interface AuthApi {

    /** Register a new user */
    @POST("/api/auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);

    /** Login with email + password */
    @POST("/api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    /** Register FCM token for push notifications */
    @POST("/api/auth/fcm-token")
    Call<okhttp3.ResponseBody> registerFcmToken(@Body FcmTokenRequest request);

    class FcmTokenRequest {
        public String token;
        public FcmTokenRequest(String token) { this.token = token; }
    }
}
