package com.masterapp.chat.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.masterapp.chat.R;
import com.masterapp.chat.models.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for chat messages.
 * Uses two view types: sent messages (right-aligned) and received messages (left-aligned).
 */
public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private List<Message> messages = new ArrayList<>();
    private String currentUserId;

    public MessageAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        if (message.isSentBy(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_SENT) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);
        }
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Message message = messages.get(position);
        holder.tvMessageText.setText(message.getText());

        // Format and display time
        String timeStr = formatMessageTime(message);
        if (holder.tvTime != null) {
            holder.tvTime.setText(timeStr);
        }

        // Show status indicator for sent messages
        if (holder.tvStatus != null && message.isSentBy(currentUserId)) {
            String statusText;
            switch (message.getStatus() != null ? message.getStatus() : "PENDING") {
                case "delivered":
                    statusText = "✓✓";
                    holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#B0FFFFFF")); // Grey
                    break;
                case "read":
                    statusText = "✓✓";
                    holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#34B7F1")); // WhatsApp Blue
                    break;
                case "sent":
                    statusText = "✓";
                    holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#B0FFFFFF")); // Grey
                    break;
                default:
                    // 'PENDING' (Offline/Sending)
                    statusText = "🕒"; // Clock icon for pending
                    holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#B0FFFFFF"));
                    break;
            }
            holder.tvStatus.setText(statusText);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    private String formatMessageTime(Message message) {
        String timestamp = message.getSentAt() != null ? message.getSentAt() : message.getCreatedAt();
        if (timestamp == null) return "";

        try {
            // Backend returns ISO 8601 strings
            java.time.OffsetDateTime odt = java.time.OffsetDateTime.parse(timestamp);
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
            return odt.format(formatter);
        } catch (Throwable t) {
            return "";
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessageText;
        TextView tvStatus; // only present in sent message layout
        TextView tvTime;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessageText = itemView.findViewById(R.id.tv_message_text);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvTime = itemView.findViewById(R.id.tv_time);
        }
    }
}
