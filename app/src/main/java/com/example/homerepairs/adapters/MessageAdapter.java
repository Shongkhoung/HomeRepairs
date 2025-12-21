package com.example.homerepairs.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homerepairs.R;
import com.example.homerepairs.models.Message;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<Message> messages;
    private String currentUserId;
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;
    
    public MessageAdapter(List<Message> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
    }
    
    public void updateMessages(List<Message> newMessages) {
        android.util.Log.d("MessageAdapter", "updateMessages called with " + (newMessages != null ? newMessages.size() : 0) + " messages");
        this.messages = newMessages;
        notifyDataSetChanged();
        android.util.Log.d("MessageAdapter", "notifyDataSetChanged called, itemCount: " + getItemCount());
    }
    
    @Override
    public int getItemViewType(int position) {
        if (messages == null || position >= messages.size()) {
            return VIEW_TYPE_RECEIVED;
        }
        Message message = messages.get(position);
        return message.getSenderId().equals(currentUserId) ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_SENT) {
            View view = inflater.inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (messages == null || position >= messages.size()) {
            return;
        }
        Message message = messages.get(position);
        
        if (holder instanceof SentMessageViewHolder) {
            ((SentMessageViewHolder) holder).bind(message);
        } else if (holder instanceof ReceivedMessageViewHolder) {
            ((ReceivedMessageViewHolder) holder).bind(message);
        }
    }
    
    @Override
    public int getItemCount() {
        return messages != null ? messages.size() : 0;
    }
    
    class SentMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView tvMessageText;
        private TextView tvTime;
        
        SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessageText = itemView.findViewById(R.id.tvMessageText);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
        
        void bind(Message message) {
            tvMessageText.setText(message.getText());
            if (message.getTimestamp() != null) {
                tvTime.setText(formatTime(message.getTimestamp()));
            } else {
                tvTime.setText("");
            }
        }
    }
    
    class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView tvMessageText;
        private TextView tvSenderName;
        private TextView tvTime;
        
        ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessageText = itemView.findViewById(R.id.tvMessageText);
            tvSenderName = itemView.findViewById(R.id.tvSenderName);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
        
        void bind(Message message) {
            tvMessageText.setText(message.getText());
            tvSenderName.setText(message.getSenderName());
            if (message.getTimestamp() != null) {
                tvTime.setText(formatTime(message.getTimestamp()));
            } else {
                tvTime.setText("");
            }
        }
    }
    
    private String formatTime(Date date) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
        return sdf.format(date);
    }
}
