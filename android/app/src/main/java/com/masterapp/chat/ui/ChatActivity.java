package com.masterapp.chat.ui;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.masterapp.chat.R;
import com.masterapp.chat.ui.adapter.MessageAdapter;
import com.masterapp.chat.util.TokenManager;
import com.masterapp.chat.viewmodel.ChatViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Chat screen for a specific conversation.
 * Displays messages with real-time updates and a text input for sending.
 */
public class ChatActivity extends AppCompatActivity {

    private ChatViewModel viewModel;
    private TokenManager tokenManager;
    private MessageAdapter adapter;
    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageButton btnSend;

    private String conversationId;
    private String otherUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        tokenManager = new TokenManager(this);
        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        // Get conversation info from intent
        conversationId = getIntent().getStringExtra("conversationId");
        otherUsername = getIntent().getStringExtra("otherUsername");

        if (conversationId == null) {
            Toast.makeText(this, "Error: no conversation ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Setup toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(otherUsername != null ? otherUsername : "Chat");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Bind views
        rvMessages = findViewById(R.id.rv_messages);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);

        // Setup RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // show newest messages at bottom
        rvMessages.setLayoutManager(layoutManager);

        adapter = new MessageAdapter(tokenManager.getUserId());
        rvMessages.setAdapter(adapter);

        // Initialize the chat BEFORE observing messages so LocalLiveData is initialized
        viewModel.initChat(conversationId);

        // Observe messages from ViewModel (which now provides MessageEntity from Room)
        viewModel.getMessages().observe(this, new androidx.lifecycle.Observer<List<com.masterapp.chat.local.entity.MessageEntity>>() {
            @Override
            public void onChanged(List<com.masterapp.chat.local.entity.MessageEntity> messageEntities) {
                if (messageEntities != null) {
                    List<com.masterapp.chat.models.Message> uiMessages = new ArrayList<>();
                    
                    // Convert Room Entities to UI Models
                    for (com.masterapp.chat.local.entity.MessageEntity entity : messageEntities) {
                        com.masterapp.chat.models.User sender = new com.masterapp.chat.models.User();
                        sender.setId(entity.senderId);
                        
                        com.masterapp.chat.models.Message msg = new com.masterapp.chat.models.Message();
                        msg.setId(entity.msgUuid);
                        msg.setConversationId(entity.conversationId);
                        msg.setText(entity.text);
                        msg.setSenderId(sender);
                        msg.setStatus(entity.status);
                        msg.setSentAt(entity.sentAt);
                        msg.setReadAt(entity.readAt);
                        
                        uiMessages.add(msg);
                    }

                    adapter.setMessages(uiMessages);
                    // Scroll to bottom when new messages arrive
                    if (!uiMessages.isEmpty()) {
                        rvMessages.scrollToPosition(uiMessages.size() - 1);
                    }

                    // Mark unread messages as read
                    markUnreadMessages(messageEntities);
                }
            }
        });

        // Send button click
        btnSend.setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                viewModel.sendMessage(text);
                etMessage.setText("");
            }
        });
    }

    /**
     * Mark received messages (from the other user) as read.
     * Use the entities fromRoom directly to get sequence IDs.
     */
    private void markUnreadMessages(List<com.masterapp.chat.local.entity.MessageEntity> entities) {
        List<String> unreadIds = new ArrayList<>();
        Long maxSeq = null;
        String myId = tokenManager.getUserId();

        for (com.masterapp.chat.local.entity.MessageEntity msg : entities) {
            if (msg.senderId != null && !msg.senderId.equals(myId) && !"read".equals(msg.status)) {
                unreadIds.add(msg.msgUuid);
                if (msg.sequenceId != null) {
                    if (maxSeq == null || msg.sequenceId > maxSeq) {
                        maxSeq = msg.sequenceId;
                    }
                }
            }
        }

        if (!unreadIds.isEmpty() || maxSeq != null) {
            viewModel.markMessagesAsRead(unreadIds.toArray(new String[0]), maxSeq);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        menu.add(0, 100, 0, "Delete Chat");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == 100) {
            // Delete chat confirmation
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Delete Chat")
                    .setMessage("Delete this entire conversation with " +
                            (otherUsername != null ? otherUsername : "this user") +
                            "?\nAll messages will be removed.")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        com.masterapp.chat.repository.ConversationRepository repo =
                                new com.masterapp.chat.repository.ConversationRepository();
                        repo.deleteConversation(this, conversationId).observe(this, success -> {
                            if (success != null && success) {
                                Toast.makeText(this, "Chat deleted", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(this, "Failed to delete chat", Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
