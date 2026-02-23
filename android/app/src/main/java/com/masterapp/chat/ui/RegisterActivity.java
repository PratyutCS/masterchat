package com.masterapp.chat.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.masterapp.chat.R;
import com.masterapp.chat.api.ApiClient;
import com.masterapp.chat.socket.SocketManager;
import com.masterapp.chat.util.TokenManager;
import com.masterapp.chat.viewmodel.AuthViewModel;

/**
 * Registration screen.
 * After successful registration, saves token and navigates to ConversationListActivity.
 */
public class RegisterActivity extends AppCompatActivity {

    private AuthViewModel viewModel;
    private TokenManager tokenManager;

    private EditText etUsername, etEmail, etPassword;
    private Button btnRegister;
    private TextView tvGoToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        tokenManager = new TokenManager(this);
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Bind views
        etUsername = findViewById(R.id.et_username);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnRegister = findViewById(R.id.btn_register);
        tvGoToLogin = findViewById(R.id.tv_go_to_login);

        // Register button
        btnRegister.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            btnRegister.setEnabled(false);

            viewModel.register(username, email, password).observe(this, response -> {
                btnRegister.setEnabled(true);
                if (response != null && response.getToken() != null) {
                    // Save token
                    tokenManager.saveToken(
                            response.getToken(),
                            response.getUser().getId(),
                            response.getUser().getUsername()
                    );
                    // Re-init API client with new token
                    ApiClient.init(tokenManager);

                    // Connect socket
                    SocketManager.getInstance().connect();
                    SocketManager.getInstance().authenticate(tokenManager.getToken());

                    // Navigate to conversations
                    Intent intent = new Intent(this, ConversationListActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, "Registration failed. Try different credentials.",
                            Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Go back to login
        tvGoToLogin.setOnClickListener(v -> finish());
    }
}
