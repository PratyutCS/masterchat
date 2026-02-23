package com.masterapp.chat.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.masterapp.chat.models.AuthResponse;
import com.masterapp.chat.repository.AuthRepository;

/**
 * ViewModel for Login and Register screens.
 * Exposes LiveData for auth results.
 */
public class AuthViewModel extends ViewModel {

    private final AuthRepository authRepository = new AuthRepository();

    /** Attempt login with email and password */
    public LiveData<AuthResponse> login(String email, String password) {
        return authRepository.login(email, password);
    }

    /** Attempt registration with username, email, and password */
    public LiveData<AuthResponse> register(String username, String email, String password) {
        return authRepository.register(username, email, password);
    }
}
