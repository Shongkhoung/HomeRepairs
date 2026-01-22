package com.example.homerepairs;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.homerepairs.adapters.MessageAdapter;
import com.example.homerepairs.databinding.ActivityChatBinding;
import com.example.homerepairs.models.Conversation;
import com.example.homerepairs.models.Message;
import com.example.homerepairs.services.FirebaseMessageService;
import com.example.homerepairs.utils.AuthHelper;

import com.example.homerepairs.utils.NetworkMonitor;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends BaseActivity {

    private ActivityChatBinding binding;
    private MessageAdapter adapter;
    private FirebaseMessageService messageService;
    private ListenerRegistration messageListener;
    private String conversationId, providerId, providerName, currentUserId, currentUserName;
    private NetworkMonitor networkMonitor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        extractIntentData();
        if (checkAuthentication()) {
            setupUI();
            setupRecyclerView();
            setupMessageService();
            setupMessageInput();
            setupNetworkMonitoring();
            loadMessages();
        }
    }

    private void extractIntentData() {
        conversationId = getIntent().getStringExtra("conversationId");
        providerId = getIntent().getStringExtra("providerId");
        providerName = getIntent().getStringExtra("providerName");
    }

    private boolean checkAuthentication() {
        currentUserId = AuthHelper.getCurrentUserId(this);
        if (currentUserId == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.putExtra("mode", "sign_in");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return false;
        }
        currentUserName = AuthHelper.getCurrentUserName(this);
        return true;
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> finish());
        if (providerName != null)
            binding.tvProviderName.setText(providerName);
        binding.tvProviderStatus.setText(R.string.status_online);
    }

    private void setupRecyclerView() {
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        binding.rvMessages.setLayoutManager(lm);
        adapter = new MessageAdapter(new ArrayList<>(), currentUserId);
        binding.rvMessages.setAdapter(adapter);
    }

    private void setupMessageService() {
        messageService = new FirebaseMessageService();
    }

    private void setupMessageInput() {
        binding.etMessageInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean enabled = s != null && s.toString().trim().length() > 0;
                binding.btnSend.setEnabled(enabled);
                binding.btnSend.setAlpha(enabled ? 1.0f : 0.5f);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        binding.btnSend.setOnClickListener(v -> sendMessage());
        binding.etMessageInput.setOnEditorActionListener((v, id, e) -> {
            if (id == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
    }

    private void sendMessage() {
        String text = binding.etMessageInput.getText().toString().trim();
        if (text.isEmpty())
            return;

        binding.btnSend.setEnabled(false);
        binding.etMessageInput.setText("");

        if (conversationId == null || conversationId.isEmpty()) {
            createAndSend(text);
        } else {
            sendToConv(text);
        }
    }

    private void createAndSend(String text) {
        messageService.getOrCreateConversation(currentUserId, currentUserName, providerId, providerName,
                new FirebaseMessageService.ConversationCallback() {
                    @Override
                    public void onSuccess(Conversation conv) {
                        conversationId = conv.getId();
                        loadMessages();
                        sendToConv(text);
                    }

                    @Override
                    public void onError(String err) {
                        Log.e("ChatActivity", "Error: " + err);
                        binding.btnSend.setEnabled(true);
                        Toast.makeText(ChatActivity.this, getString(R.string.msg_send_failed, err), Toast.LENGTH_SHORT)
                                .show();
                    }
                });
    }

    private void sendToConv(String text) {
        messageService.sendMessage(conversationId, currentUserId, currentUserName, providerId, providerName, text,
                new FirebaseMessageService.MessageCallback() {
                    @Override
                    public void onSuccess(Message msg) {
                        binding.btnSend.setEnabled(true);
                        binding.rvMessages.post(() -> {
                            if (adapter.getItemCount() > 0)
                                binding.rvMessages.smoothScrollToPosition(adapter.getItemCount() - 1);
                        });
                        // Response simulation removed for real data
                    }

                    @Override
                    public void onError(String err) {
                        Log.e("ChatActivity", "Error: " + err);
                        binding.btnSend.setEnabled(true);
                        Toast.makeText(ChatActivity.this, "Send failed: " + err, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void loadMessages() {
        if (conversationId == null || conversationId.isEmpty()) {
            binding.progressBar.setVisibility(View.GONE);
            binding.llEmptyState.setVisibility(View.VISIBLE);
            binding.rvMessages.setVisibility(View.GONE);
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.llEmptyState.setVisibility(View.GONE);
        binding.rvMessages.setVisibility(View.VISIBLE);

        messageListener = messageService.listenToMessages(conversationId,
                new FirebaseMessageService.MessageListCallback() {

                    @Override
                    public void onSuccess(List<Message> messages) {
                        binding.progressBar.setVisibility(View.GONE);
                        if (messages != null && !messages.isEmpty()) {
                            adapter.updateMessages(messages);
                            binding.rvMessages.setVisibility(View.VISIBLE);
                            binding.llEmptyState.setVisibility(View.GONE);
                            binding.rvMessages.post(() -> {
                                if (adapter.getItemCount() > 0)
                                    binding.rvMessages.smoothScrollToPosition(adapter.getItemCount() - 1);
                            });
                        } else if (adapter.getItemCount() == 0) {
                            binding.rvMessages.setVisibility(View.GONE);
                            binding.llEmptyState.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onError(String err) {
                        binding.progressBar.setVisibility(View.GONE);
                        Log.e("ChatActivity", "Error: " + err);
                        if (adapter.getItemCount() == 0) {
                            binding.rvMessages.setVisibility(View.GONE);
                            binding.llEmptyState.setVisibility(View.VISIBLE);
                        }
                    }

                });

        messageService.markMessagesAsRead(conversationId, currentUserId);
    }

    private void setupNetworkMonitoring() {
        networkMonitor = new NetworkMonitor(this);
        networkMonitor.initialize(binding.llInternetLoading, binding.tvInternetMessage, binding.ivNoConnection);
        networkMonitor.startMonitoring();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener != null)
            messageListener.remove();
        if (networkMonitor != null)
            networkMonitor.cleanup();
    }
}
