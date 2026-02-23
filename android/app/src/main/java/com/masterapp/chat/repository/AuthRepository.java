package com.masterapp.chat.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.masterapp.chat.api.ApiClient;
import com.masterapp.chat.models.AuthResponse;
import com.masterapp.chat.models.LoginRequest;
import com.masterapp.chat.models.RegisterRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository for authentication operations.
 * Bridges ViewModel ↔ Retrofit API.
 */
public class AuthRepository {

    /**
     * Register a new user.
     * @return LiveData that emits the AuthResponse on success or null on failure.
     */
    public LiveData<AuthResponse> register(String username, String email, String password) {
        MutableLiveData<AuthResponse> result = new MutableLiveData<>();

        ApiClient.getAuthApi()
                .register(new RegisterRequest(username, email, password))
                .enqueue(new Callback<AuthResponse>() {
                    @Override
                    public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            result.postValue(response.body());
                        } else {
                            result.postValue(null);
                        }
                    }

                    @Override
                    public void onFailure(Call<AuthResponse> call, Throwable t) {
                        result.postValue(null);
                    }
                });

        return result;
    }

    /**
     * Login with email and password.
     * @return LiveData that emits the AuthResponse on success or null on failure.
     */
    public LiveData<AuthResponse> login(String email, String password) {
        MutableLiveData<AuthResponse> result = new MutableLiveData<>();

        ApiClient.getAuthApi()
                .login(new LoginRequest(email, password))
                .enqueue(new Callback<AuthResponse>() {
                    @Override
                    public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            result.postValue(response.body());
                        } else {
                            result.postValue(null);
                        }
                    }

                    @Override
                    public void onFailure(Call<AuthResponse> call, Throwable t) {
                        result.postValue(null);
                    }
                });

        return result;
    }
}
