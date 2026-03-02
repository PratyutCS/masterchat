package com.masterapp.chat.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.masterapp.chat.R;
import com.masterapp.chat.models.Message;

import java.util.List;

/**
 * RecyclerView adapter for chat messages.
 * Uses ListAdapter for efficient DiffUtil-based updates.
 */
public class MessageAdapter extends ListAdapter<Message, MessageAdapter.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private String currentUserId;

    public MessageAdapter(String currentUserId) {
        super(new MessageDiffCallback());
        this.currentUserId = currentUserId;
    }

    public void setMessages(List<Message> messages) {
        submitList(messages);
    }

    @Override
    public int getItemViewType(int position) {
        Message message = getItem(position);
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
        Message message = getItem(position);
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

    private String formatMessageTime(Message message) {
        String timestamp = message.getSentAt() != null ? message.getSentAt() : message.getCreatedAt();
        if (timestamp == null) return "";

        try {
            // Backend returns ISO 8601 strings (UTC)
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US);
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            
            java.util.Date date = sdf.parse(timestamp);
            if (date == null) {
                // Try alternate format without milliseconds
                java.text.SimpleDateFormat sdfAlt = new java.text.SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US);
                sdfAlt.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                date = sdfAlt.parse(timestamp);
            }
            
            if (date != null) {
                java.text.SimpleDateFormat displayFormat = new java.text.SimpleDateFormat(
                        "HH:mm", java.util.Locale.getDefault());
                return displayFormat.format(date);
            }
        } catch (Exception e) {
            return "";
        }
        return "";
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

    /**
     * DiffUtil callback for Message list updates.
     */
    public static class MessageDiffCallback extends DiffUtil.ItemCallback<Message> {
        @Override
        public boolean areItemsTheSame(@NonNull Message oldItem, @NonNull Message newItem) {
            return oldItem.getId() != null && oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Message oldItem, @NonNull Message newItem) {
            // Status and text are the main things that change
            return (oldItem.getStatus() != null && oldItem.getStatus().equals(newItem.getStatus())) &&
                   (oldItem.getText() != null && oldItem.getText().equals(newItem.getText()));
        }
    }
}
