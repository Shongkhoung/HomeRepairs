package com.example.homerepairs;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.FrameLayout;

import com.airbnb.lottie.LottieAnimationView;
import com.example.homerepairs.adapters.MessageAdapter;
import com.example.homerepairs.models.Message;
import com.example.homerepairs.services.FirebaseMessageService;
import com.example.homerepairs.utils.AuthHelper;
import com.example.homerepairs.utils.MessageTestHelper;
import com.example.homerepairs.utils.NetworkMonitor;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView rvMessages;
    private LinearLayoutManager layoutManager;
    private MessageAdapter adapter;
    private EditText etMessageInput;
    private ImageButton btnSend;
    private TextView tvProviderName;
    private TextView tvProviderStatus;
    private ProgressBar progressBar;
    private View llEmptyState;
    
    private FirebaseMessageService messageService;
    private ListenerRegistration messageListener;
    
    private String conversationId;
    private String providerId;
    private String providerName;
    private String currentUserId;
    private String currentUserName;
    private MessageTestHelper testHelper;
    private NetworkMonitor networkMonitor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Get intent data
        conversationId = getIntent().getStringExtra("conversationId");
        providerId = getIntent().getStringExtra("providerId");
        providerName = getIntent().getStringExtra("providerName");
        
        currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            // User not authenticated, redirect to login in sign in mode
            Intent intent = new Intent(this, LoginActivity.class);
            intent.putExtra("mode", "sign_in");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
        currentUserName = getCurrentUserName();

        initializeViews();
        setupRecyclerView();
        setupMessageService();
        setupMessageInput();
        loadMessages();
    }

    private void initializeViews() {
        rvMessages = findViewById(R.id.rvMessages);
        etMessageInput = findViewById(R.id.etMessageInput);
        btnSend = findViewById(R.id.btnSend);
        tvProviderName = findViewById(R.id.tvProviderName);
        tvProviderStatus = findViewById(R.id.tvProviderStatus);
        progressBar = findViewById(R.id.progressBar);
        llEmptyState = findViewById(R.id.llEmptyState);
        
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
        
        if (providerName != null) {
            tvProviderName.setText(providerName);
        }
        tvProviderStatus.setText("Online");
    }

    private void setupRecyclerView() {
        layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Start from bottom
        rvMessages.setLayoutManager(layoutManager);
        
        adapter = new MessageAdapter(new ArrayList<>(), currentUserId);
        rvMessages.setAdapter(adapter);
    }

    private void setupMessageService() {
        messageService = new FirebaseMessageService();
        testHelper = new MessageTestHelper();
    }

    private void setupMessageInput() {
        // Enable/disable send button based on input
        etMessageInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnSend.setEnabled(s != null && s.toString().trim().length() > 0);
                btnSend.setAlpha(btnSend.isEnabled() ? 1.0f : 0.5f);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnSend.setOnClickListener(v -> sendMessage());
        
        // Send on Enter key
        etMessageInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
    }

    private void sendMessage() {
        String messageText = etMessageInput.getText().toString().trim();
        if (messageText.isEmpty()) {
            return;
        }

        // Disable send button while sending
        btnSend.setEnabled(false);
        etMessageInput.setText("");

        // If no conversation ID, create/get conversation first
        if (conversationId == null || conversationId.isEmpty()) {
            createConversationAndSendMessage(messageText);
        } else {
            sendMessageToConversation(messageText);
        }
    }

    private void createConversationAndSendMessage(String messageText) {
        messageService.getOrCreateConversation(currentUserId, currentUserName, 
                providerId, providerName, 
                new FirebaseMessageService.ConversationCallback() {
                    @Override
                    public void onSuccess(com.example.homerepairs.models.Conversation conversation) {
                        conversationId = conversation.getId();
                        sendMessageToConversation(messageText);
                    }

                    @Override
                    public void onError(String error) {
                        android.util.Log.e("ChatActivity", "Error creating conversation: " + error);
                        btnSend.setEnabled(true);
                        android.widget.Toast.makeText(ChatActivity.this, 
                                "Failed to send message: " + error, 
                                android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendMessageToConversation(String messageText) {
        messageService.sendMessage(conversationId, currentUserId, currentUserName,
                providerId, providerName, messageText,
                new FirebaseMessageService.MessageCallback() {
                    @Override
                    public void onSuccess(Message message) {
                        btnSend.setEnabled(true);
                        // Scroll to bottom
                        rvMessages.post(() -> {
                            if (adapter.getItemCount() > 0) {
                                rvMessages.smoothScrollToPosition(adapter.getItemCount() - 1);
                            }
                        });
                        
                        // Simulate provider response after 2 seconds (for testing)
                        simulateProviderResponse();
                    }

                    @Override
                    public void onError(String error) {
                        android.util.Log.e("ChatActivity", "Error sending message: " + error);
                        btnSend.setEnabled(true);
                        
                        // Show user-friendly error message
                        String errorMessage = "Failed to send message";
                        if (error != null && error.contains("PERMISSION_DENIED")) {
                            errorMessage = "Permission denied. Please check Firebase Security Rules.";
                        } else if (error != null) {
                            errorMessage = "Error: " + error;
                        }
                        
                        android.widget.Toast.makeText(ChatActivity.this, 
                                errorMessage, 
                                android.widget.Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void loadMessages() {
        if (conversationId == null || conversationId.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            llEmptyState.setVisibility(View.VISIBLE);
            rvMessages.setVisibility(View.GONE);
            android.util.Log.d("ChatActivity", "No conversationId provided");
            return;
        }

        android.util.Log.d("ChatActivity", "Loading messages for conversation: " + conversationId);
        progressBar.setVisibility(View.VISIBLE);
        llEmptyState.setVisibility(View.GONE);
        rvMessages.setVisibility(View.VISIBLE); // Keep RecyclerView visible while loading

        // Set up real-time listener for messages
        messageListener = messageService.listenToMessages(conversationId,
                new FirebaseMessageService.MessageListCallback() {
                    @Override
                    public void onSuccess(List<Message> messages) {
                        progressBar.setVisibility(View.GONE);
                        android.util.Log.d("ChatActivity", "Messages received: " + (messages != null ? messages.size() : 0));
                        
                        if (messages != null && !messages.isEmpty()) {
                            android.util.Log.d("ChatActivity", "Updating adapter with " + messages.size() + " messages");
                            adapter.updateMessages(messages);
                            rvMessages.setVisibility(View.VISIBLE);
                            llEmptyState.setVisibility(View.GONE);
                            
                            // Scroll to bottom when new messages arrive
                            rvMessages.post(() -> {
                                if (adapter.getItemCount() > 0) {
                                    android.util.Log.d("ChatActivity", "Scrolling to position: " + (adapter.getItemCount() - 1));
                                    rvMessages.smoothScrollToPosition(adapter.getItemCount() - 1);
                                }
                            });
                        } else {
                            android.util.Log.d("ChatActivity", "No messages, showing empty state");
                            // Only show empty state if we truly have no messages
                            if (adapter.getItemCount() == 0) {
                                rvMessages.setVisibility(View.GONE);
                                llEmptyState.setVisibility(View.VISIBLE);
                            }
                        }
                    }

                    @Override
                    public void onError(String error) {
                        progressBar.setVisibility(View.GONE);
                        android.util.Log.e("ChatActivity", "Error loading messages: " + error);
                        
                        // For index errors, don't hide the RecyclerView - messages might still come through
                        // and the index will be created automatically
                        if (error != null && error.contains("FAILED_PRECONDITION") && error.contains("index")) {
                            android.widget.Toast.makeText(ChatActivity.this, 
                                "Creating database index... Messages will appear soon.", 
                                android.widget.Toast.LENGTH_LONG).show();
                            // Keep RecyclerView visible - messages may still load after index is created
                            // Don't hide it yet
                            return;
                        }
                        
                        // Show user-friendly error message for permission errors
                        if (error != null && error.contains("PERMISSION_DENIED")) {
                            android.widget.Toast.makeText(ChatActivity.this, 
                                "Firebase permissions not configured. Please check Security Rules.", 
                                android.widget.Toast.LENGTH_LONG).show();
                        }
                        
                        // Only hide RecyclerView for other errors
                        // Check if we already have messages displayed
                        if (adapter.getItemCount() == 0) {
                            rvMessages.setVisibility(View.GONE);
                            llEmptyState.setVisibility(View.VISIBLE);
                        }
                    }
                });

        // Mark messages as read
        messageService.markMessagesAsRead(conversationId, currentUserId);
    }

    /**
     * Get current user ID using AuthHelper
     */
    private String getCurrentUserId() {
        return AuthHelper.getCurrentUserId(this);
    }

    /**
     * Get current user name using AuthHelper
     */
    private String getCurrentUserName() {
        return AuthHelper.getCurrentUserName(this);
    }

    /**
     * Simulate a provider response (for testing purposes)
     * In production, this would be sent by the actual provider
     */
    private void simulateProviderResponse() {
        // Only simulate if we have a valid conversation
        if (conversationId == null || conversationId.isEmpty() || providerId == null) {
            return;
        }
        
        // Wait 2 seconds then send a simulated response
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            testHelper.addProviderResponse(conversationId, providerId, providerName, 
                    currentUserId, currentUserName);
        }, 2000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove listener when activity is destroyed
        if (messageListener != null) {
            messageListener.remove();
        }
        if (networkMonitor != null) {
            networkMonitor.cleanup();
        }
    }
    
    private void setupNetworkMonitoring() {
        networkMonitor = new NetworkMonitor(this);
        FrameLayout llInternetLoading = findViewById(R.id.llInternetLoading);
        TextView tvInternetMessage = findViewById(R.id.tvInternetMessage);
        LottieAnimationView ivNoConnection = findViewById(R.id.ivNoConnection);
        networkMonitor.initialize(llInternetLoading, tvInternetMessage, ivNoConnection);
        networkMonitor.startMonitoring();
    }
}
