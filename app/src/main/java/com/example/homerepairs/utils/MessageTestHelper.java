package com.example.homerepairs.utils;

import android.util.Log;
import android.widget.Toast;

import com.example.homerepairs.models.Conversation;
import com.example.homerepairs.models.Message;
import com.example.homerepairs.services.FirebaseMessageService;

import java.util.Date;

/**
 * Helper class to create test conversations and messages for testing purposes
 * This allows testing the messaging system with a single user account
 */
public class MessageTestHelper {
    private static final String TAG = "MessageTestHelper";
    private FirebaseMessageService messageService;

    // Test provider data
    private static final String[] TEST_PROVIDERS = {
        "Ethan Carter", "Sophia Bennett", "Liam Harper"
    };
    
    private static final String[] TEST_PROVIDER_IDS = {
        "provider_ethan", "provider_sophia", "provider_liam"
    };

    public MessageTestHelper() {
        messageService = new FirebaseMessageService();
    }

    /**
     * Create test conversations with sample messages
     * This simulates providers sending messages to the user
     */
    public void createTestConversations(String userId, String userName, 
                                       FirebaseMessageService.ConversationListCallback callback) {
        Log.d(TAG, "Creating test conversations for user: " + userId);
        
        // Create conversations for each test provider
        for (int i = 0; i < TEST_PROVIDERS.length; i++) {
            final String providerName = TEST_PROVIDERS[i];
            final String providerId = TEST_PROVIDER_IDS[i];
            final int index = i;
            
            messageService.getOrCreateConversation(userId, userName, providerId, providerName,
                    new FirebaseMessageService.ConversationCallback() {
                        @Override
                        public void onSuccess(Conversation conversation) {
                            Log.d(TAG, "Created conversation with " + providerName);
                            // Add a test message from provider
                            addTestMessageFromProvider(conversation.getId(), providerId, providerName, userId, userName, index);
                            
                            // If this is the last provider, call callback
                            if (index == TEST_PROVIDERS.length - 1 && callback != null) {
                                messageService.getConversationsForUser(userId, callback);
                            }
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Error creating conversation with " + providerName + ": " + error);
                        }
                    });
        }
    }

    /**
     * Add a test message from a provider to simulate receiving messages
     */
    private void addTestMessageFromProvider(String conversationId, String providerId, 
                                          String providerName, String userId, String userName, int providerIndex) {
        String[] testMessages = {
            "Hi! Thanks for reaching out. I'm available to help with your plumbing needs. When would be a good time?",
            "Hello! I received your message about electrical work. I can schedule you in today. Would 2pm work?",
            "Hi there! I'm available for HVAC services. I have slots open this week. What's your preferred time?"
        };
        
        String messageText = testMessages[providerIndex % testMessages.length];
        
        // Send message as if from provider
        messageService.sendMessage(conversationId, providerId, providerName, 
                userId, userName, messageText,
                new FirebaseMessageService.MessageCallback() {
                    @Override
                    public void onSuccess(Message message) {
                        Log.d(TAG, "Added test message from " + providerName);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Error adding test message: " + error);
                    }
                });
    }

    /**
     * Create a single test conversation (for testing from ChatActivity)
     */
    public void createSingleTestConversation(String userId, String userName, 
                                            String providerId, String providerName,
                                            FirebaseMessageService.ConversationCallback callback) {
        messageService.getOrCreateConversation(userId, userName, providerId, providerName, callback);
    }

    /**
     * Add a test message from provider to an existing conversation
     */
    public void addProviderResponse(String conversationId, String providerId, 
                                   String providerName, String userId, String userName) {
        String[] responses = {
            "I understand. Let me know more details about the issue.",
            "I can help with that! When would be convenient for you?",
            "Great! I'll be there at the scheduled time.",
            "Thanks for the information. I'll prepare everything needed."
        };
        
        String messageText = responses[(int)(Math.random() * responses.length)];
        
        messageService.sendMessage(conversationId, providerId, providerName,
                userId, userName, messageText,
                new FirebaseMessageService.MessageCallback() {
                    @Override
                    public void onSuccess(Message message) {
                        Log.d(TAG, "Added provider response");
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Error adding provider response: " + error);
                    }
                });
    }
}
