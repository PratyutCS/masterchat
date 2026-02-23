package com.masterapp.chat.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.masterapp.chat.R;
import com.masterapp.chat.models.Conversation;
import com.masterapp.chat.models.User;
import com.masterapp.chat.socket.SocketManager;
import com.masterapp.chat.ui.adapter.ConversationAdapter;
import com.masterapp.chat.util.TokenManager;
import com.masterapp.chat.viewmodel.ConversationListViewModel;

import java.util.List;

/**
 * Conversation list screen.
 * Shows all conversations for the current user.
 * FAB or menu to start a new conversation.
 */
public class ConversationListActivity extends AppCompatActivity
        implements ConversationAdapter.OnConversationClickListener,
                   ConversationAdapter.OnConversationLongClickListener {

    private ConversationListViewModel viewModel;
    private TokenManager tokenManager;
    private ConversationAdapter adapter;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation_list);

        tokenManager = new TokenManager(this);
        viewModel = new ViewModelProvider(this).get(ConversationListViewModel.class);

        // Ensure Socket is connected and authenticated when reopening the app directly to this screen
        if (!SocketManager.getInstance().isConnected()) {
            SocketManager.getInstance().connect();
            SocketManager.getInstance().authenticate(tokenManager.getToken());
        }

        // Setup RecyclerView
        recyclerView = findViewById(R.id.rv_conversations);
        progressBar = findViewById(R.id.progress_bar);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ConversationAdapter(tokenManager.getUserId(), this, this);
        recyclerView.setAdapter(adapter);

        // Setup toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Chats");
        }

        // New chat button
        findViewById(R.id.fab_new_chat).setOnClickListener(v -> showNewChatDialog());

        // Observe live data from server fetch
        viewModel.getConversations().observe(this, newData -> {
            progressBar.setVisibility(View.GONE);
            if (newData != null) {
                adapter.setConversations(newData);
            }
        });

        viewModel.refreshConversations();
        
        // Listen for global socket events via stable LiveData bus
        SocketManager.getInstance().getNewMessageEvents().observe(this, data -> {
            android.util.Log.d("ConvList", "Dashboard refreshing: New message available");
            viewModel.refreshConversations();
        });

        SocketManager.getInstance().getMessageReadEvents().observe(this, data -> {
            android.util.Log.d("ConvList", "Dashboard refreshing: Messages read");
            viewModel.refreshConversations();
        });

        SocketManager.getInstance().getMessageDeliveredEvents().observe(this, data -> {
            android.util.Log.d("ConvList", "Dashboard refreshing: Messages delivered");
            viewModel.refreshConversations();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.refreshConversations();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void loadConversations() {
        progressBar.setVisibility(View.VISIBLE);
        viewModel.refreshConversations();
    }

    /**
     * Show a dialog listing all users to start a new conversation with.
     */
    private void showNewChatDialog() {
        // Fetch users fresh from the server each time the dialog is opened
        viewModel.loadAndGetUsers().observe(this, users -> {
            if (users != null && !users.isEmpty()) {
                String[] usernames = new String[users.size()];
                for (int i = 0; i < users.size(); i++) {
                    usernames[i] = users.get(i).getUsername();
                }

                new AlertDialog.Builder(this)
                        .setTitle("Start new chat with...")
                        .setItems(usernames, (dialog, which) -> {
                            User selectedUser = users.get(which);
                            createConversationAndOpen(selectedUser);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            } else {
                Toast.makeText(this, "No other users found", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createConversationAndOpen(User user) {
        viewModel.createConversation(user.getId()).observe(this, conversation -> {
            if (conversation != null) {
                openChat(conversation, user.getUsername());
            } else {
                Toast.makeText(this, "Failed to create conversation", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onConversationClick(Conversation conversation) {
        User other = conversation.getOtherMember(tokenManager.getUserId());
        String otherName = other != null ? other.getUsername() : "Chat";
        openChat(conversation, otherName);
    }

    private void openChat(Conversation conversation, String otherUsername) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("conversationId", conversation.getId());
        intent.putExtra("otherUsername", otherUsername);
        startActivity(intent);
    }

    @Override
    public void onConversationLongClick(Conversation conversation) {
        User other = conversation.getOtherMember(tokenManager.getUserId());
        String name = other != null ? other.getUsername() : "this user";

        new AlertDialog.Builder(this)
                .setTitle("Delete Chat")
                .setMessage("Delete conversation with " + name + "?\nThis will remove all messages from both devices.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteConversation(conversation.getId(), name);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteConversation(String conversationId, String name) {
        viewModel.deleteConversation(this, conversationId).observe(this, success -> {
            if (success != null && success) {
                Toast.makeText(this, "Chat with " + name + " deleted", Toast.LENGTH_SHORT).show();
                viewModel.refreshConversations();
            } else {
                Toast.makeText(this, "Failed to delete conversation", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Options menu for logout
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "Logout");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            // Logout
            SocketManager.getInstance().disconnect();
            tokenManager.clearToken();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
