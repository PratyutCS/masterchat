package com.masterapp.chat.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.masterapp.chat.R;
import com.masterapp.chat.models.Conversation;
import com.masterapp.chat.models.User;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for the conversation list screen.
 * Shows the other user's name and the last message preview.
 */
public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {

    private List<Conversation> conversations = new ArrayList<>();
    private String currentUserId;
    private OnConversationClickListener listener;
    private OnConversationLongClickListener longClickListener;

    public interface OnConversationClickListener {
        void onConversationClick(Conversation conversation);
    }

    public interface OnConversationLongClickListener {
        void onConversationLongClick(Conversation conversation);
    }

    public ConversationAdapter(String currentUserId, OnConversationClickListener listener) {
        this(currentUserId, listener, null);
    }

    public ConversationAdapter(String currentUserId, OnConversationClickListener clickListener,
                               OnConversationLongClickListener longClickListener) {
        this.currentUserId = currentUserId;
        this.listener = clickListener;
        this.longClickListener = longClickListener;
    }

    public void setConversations(List<Conversation> conversations) {
        this.conversations = conversations;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Conversation conversation = conversations.get(position);

        // Show the other member's name
        User other = conversation.getOtherMember(currentUserId);
        String username = other != null ? other.getUsername() : "Unknown";
        holder.tvUsername.setText(username);
        
        // Extract first initial for avatar
        if (username != null && !username.isEmpty()) {
            holder.tvAvatarInitial.setText(username.substring(0, 1).toUpperCase());
        } else {
            holder.tvAvatarInitial.setText("U");
        }

        // Show last message preview
        if (conversation.getLastMessage() != null) {
            String lastText = conversation.getLastMessage().getText();
            android.util.Log.d("ConvAdapter", "Pos " + position + " lastMsg: " + lastText);
            holder.tvLastMessage.setText(lastText);
            holder.tvLastMessage.setVisibility(View.VISIBLE);
        } else {
            android.util.Log.d("ConvAdapter", "Pos " + position + " lastMsg: NULL");
            holder.tvLastMessage.setText("");
            holder.tvLastMessage.setVisibility(View.GONE);
        }

        // Unread Badge
        int unreadCount = conversation.getUnreadCount();
        android.util.Log.d("ConvAdapter", "Pos " + position + " unreadCount: " + unreadCount);
        if (unreadCount > 0) {
            holder.tvUnreadBadge.setText(String.valueOf(unreadCount));
            holder.tvUnreadBadge.setVisibility(View.VISIBLE);
        } else {
            holder.tvUnreadBadge.setVisibility(View.GONE);
        }

        // Click to open chat
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onConversationClick(conversation);
            }
        });

        // Long-press to delete
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onConversationLongClick(conversation);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername;
        TextView tvLastMessage;
        TextView tvAvatarInitial;
        TextView tvUnreadBadge;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tv_username);
            tvLastMessage = itemView.findViewById(R.id.tv_last_message);
            tvAvatarInitial = itemView.findViewById(R.id.tv_avatar_initial);
            tvUnreadBadge = itemView.findViewById(R.id.tv_unread_badge);
        }
    }
}
