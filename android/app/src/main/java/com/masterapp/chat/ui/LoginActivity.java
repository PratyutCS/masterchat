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
 * Login screen.
 * If the user is already logged in (token exists), skip to ConversationListActivity.
 */
public class LoginActivity extends AppCompatActivity {

    private AuthViewModel viewModel;
    private TokenManager tokenManager;

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvGoToRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        tokenManager = new TokenManager(this);

        // Initialize API client
        ApiClient.init(tokenManager);

        // Request full storage permissions irrespective of whether it is immediately needed
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (!android.os.Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.addCategory("android.intent.category.DEFAULT");
                    intent.setData(android.net.Uri.parse(String.format("package:%s", getApplicationContext().getPackageName())));
                    startActivityForResult(intent, 2296);
                } catch (Exception e) {
                    Intent intent = new Intent();
                    intent.setAction(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivityForResult(intent, 2296);
                }
            }
        } else {
            androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, 100);
        }

        // If already logged in, go straight to conversations
        if (tokenManager.isLoggedIn()) {
            connectSocketAndGoToConversations();
            return;
        }

        setContentView(R.layout.activity_login);
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Bind views
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvGoToRegister = findViewById(R.id.tv_go_to_register);

        // Login button
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            btnLogin.setEnabled(false);

            viewModel.login(email, password).observe(this, response -> {
                btnLogin.setEnabled(true);
                if (response != null && response.getToken() != null) {
                    // Save token and user info
                    tokenManager.saveToken(
                            response.getToken(),
                            response.getUser().getId(),
                            response.getUser().getUsername()
                    );
                    // Re-init API client with new token
                    ApiClient.init(tokenManager);
                    connectSocketAndGoToConversations();
                } else {
                    Toast.makeText(this, "Login failed. Check credentials.",
                            Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Go to register screen
        tvGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }

    private void connectSocketAndGoToConversations() {
        // Connect socket and authenticate
        SocketManager.getInstance().connect();
        SocketManager.getInstance().authenticate(tokenManager.getToken());

        // Navigate to conversation list
        Intent intent = new Intent(this, ConversationListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
