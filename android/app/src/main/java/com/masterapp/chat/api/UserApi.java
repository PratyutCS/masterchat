package com.masterapp.chat.api;

import com.masterapp.chat.models.User;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Retrofit interface for user endpoints.
 */
public interface UserApi {

    /** Get all users except self */
    @GET("/api/users")
    Call<List<User>> getUsers();

    /** Get a single user by ID */
    @GET("/api/users/{id}")
    Call<User> getUser(@Path("id") String userId);
}
