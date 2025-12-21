package com.example.homerepairs.services;

import android.util.Log;

import com.example.homerepairs.models.Conversation;
import com.example.homerepairs.models.Message;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseMessageService {
    private static final String TAG = "FirebaseMessageService";
    private static final String COLLECTION_CONVERSATIONS = "conversations";
    private static final String COLLECTION_MESSAGES = "messages";
    private FirebaseFirestore db;

    public interface ConversationCallback {
        void onSuccess(Conversation conversation);
        void onError(String error);
    }

    public interface ConversationListCallback {
        void onSuccess(List<Conversation> conversations);
        void onError(String error);
    }

    public interface MessageCallback {
        void onSuccess(Message message);
        void onError(String error);
    }

    public interface MessageListCallback {
        void onSuccess(List<Message> messages);
        void onError(String error);
    }

    public FirebaseMessageService() {
        try {
            db = FirebaseFirestore.getInstance();
            Log.d(TAG, "FirebaseMessageService initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing FirebaseMessageService", e);
            throw e;
        }
    }

    /**
     * Create or get existing conversation between user and provider
     */
    public void getOrCreateConversation(String userId, String userName, String providerId, 
                                       String providerName, ConversationCallback callback) {
        // Check if conversation already exists
        db.collection(COLLECTION_CONVERSATIONS)
                .whereEqualTo("userId", userId)
                .whereEqualTo("providerId", providerId)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            // Conversation exists
                            DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                            Conversation conversation = document.toObject(Conversation.class);
                            if (conversation != null) {
                                conversation.setId(document.getId());
                                Log.d(TAG, "Found existing conversation: " + document.getId());
                                callback.onSuccess(conversation);
                            } else {
                                callback.onError("Failed to parse conversation");
                            }
                        } else {
                            // Create new conversation
                            createConversation(userId, userName, providerId, providerName, callback);
                        }
                    } else {
                        Log.e(TAG, "Error checking for existing conversation", task.getException());
                        callback.onError("Failed to check conversation: " + 
                            (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                });
    }

    /**
     * Create a new conversation
     */
    public void createConversation(String userId, String userName, String providerId, 
                                  String providerName, ConversationCallback callback) {
        Conversation conversation = new Conversation(userId, userName, providerId, providerName);
        
        db.collection(COLLECTION_CONVERSATIONS)
                .add(conversation)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Conversation created successfully with ID: " + documentReference.getId());
                    conversation.setId(documentReference.getId());
                    callback.onSuccess(conversation);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating conversation", e);
                    callback.onError("Failed to create conversation: " + 
                        (e.getMessage() != null ? e.getMessage() : "Unknown error"));
                });
    }

    /**
     * Get all conversations for a user
     */
    public void getConversationsForUser(String userId, ConversationListCallback callback) {
        db.collection(COLLECTION_CONVERSATIONS)
                .whereEqualTo("userId", userId)
                .orderBy("lastMessageTime", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        List<Conversation> conversations = new ArrayList<>();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            for (QueryDocumentSnapshot document : querySnapshot) {
                                try {
                                    Conversation conversation = document.toObject(Conversation.class);
                                    if (conversation != null) {
                                        conversation.setId(document.getId());
                                        conversations.add(conversation);
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error converting document to Conversation", e);
                                }
                            }
                        }
                        Log.d(TAG, "Loaded " + conversations.size() + " conversations for user: " + userId);
                        callback.onSuccess(conversations);
                    } else {
                        Log.e(TAG, "Error getting conversations", task.getException());
                        callback.onError("Failed to load conversations: " + 
                            (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                });
    }

    /**
     * Get all conversations for a provider
     */
    public void getConversationsForProvider(String providerId, ConversationListCallback callback) {
        db.collection(COLLECTION_CONVERSATIONS)
                .whereEqualTo("providerId", providerId)
                .orderBy("lastMessageTime", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        List<Conversation> conversations = new ArrayList<>();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            for (QueryDocumentSnapshot document : querySnapshot) {
                                try {
                                    Conversation conversation = document.toObject(Conversation.class);
                                    if (conversation != null) {
                                        conversation.setId(document.getId());
                                        conversations.add(conversation);
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error converting document to Conversation", e);
                                }
                            }
                        }
                        Log.d(TAG, "Loaded " + conversations.size() + " conversations for provider: " + providerId);
                        callback.onSuccess(conversations);
                    } else {
                        Log.e(TAG, "Error getting conversations", task.getException());
                        callback.onError("Failed to load conversations: " + 
                            (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                });
    }

    /**
     * Send a message
     */
    public void sendMessage(String conversationId, String senderId, String senderName,
                           String receiverId, String receiverName, String text, MessageCallback callback) {
        Message message = new Message(conversationId, senderId, senderName, receiverId, receiverName, text);
        
        // Use batch write to update conversation and add message atomically
        WriteBatch batch = db.batch();
        
        // Add message to messages collection
        DocumentReference messageRef = db.collection(COLLECTION_MESSAGES).document();
        batch.set(messageRef, message);
        
        // Update conversation with last message and timestamp
        DocumentReference conversationRef = db.collection(COLLECTION_CONVERSATIONS).document(conversationId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessage", text);
        updates.put("lastMessageTime", new Date());
        updates.put("updatedAt", new Date());
        
        // Increment unread count for receiver
        // Note: In a real app, you'd need to query current unread count or use FieldValue.increment()
        updates.put("unreadCount", 1); // Simplified - should track per user
        batch.update(conversationRef, updates);
        
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Message sent successfully");
                    message.setId(messageRef.getId());
                    callback.onSuccess(message);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error sending message", e);
                    callback.onError("Failed to send message: " + 
                        (e.getMessage() != null ? e.getMessage() : "Unknown error"));
                });
    }

    /**
     * Get all messages for a conversation
     */
    public void getMessagesForConversation(String conversationId, MessageListCallback callback) {
        db.collection(COLLECTION_MESSAGES)
                .whereEqualTo("conversationId", conversationId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        List<Message> messages = new ArrayList<>();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            for (QueryDocumentSnapshot document : querySnapshot) {
                                try {
                                    Message message = document.toObject(Message.class);
                                    if (message != null) {
                                        message.setId(document.getId());
                                        messages.add(message);
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error converting document to Message", e);
                                }
                            }
                        }
                        Log.d(TAG, "Loaded " + messages.size() + " messages for conversation: " + conversationId);
                        callback.onSuccess(messages);
                    } else {
                        Log.e(TAG, "Error getting messages", task.getException());
                        callback.onError("Failed to load messages: " + 
                            (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                });
    }

    /**
     * Set up real-time listener for messages in a conversation
     */
    public ListenerRegistration listenToMessages(String conversationId, MessageListCallback callback) {
        return db.collection(COLLECTION_MESSAGES)
                .whereEqualTo("conversationId", conversationId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to messages", error);
                        callback.onError("Failed to listen to messages: " + error.getMessage());
                        return;
                    }
                    
                    if (querySnapshot != null) {
                        List<Message> messages = new ArrayList<>();
                        for (QueryDocumentSnapshot document : querySnapshot) {
                            try {
                                Message message = document.toObject(Message.class);
                                if (message != null) {
                                    message.setId(document.getId());
                                    messages.add(message);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error converting document to Message", e);
                            }
                        }
                        Log.d(TAG, "Real-time update: " + messages.size() + " messages");
                        callback.onSuccess(messages);
                    }
                });
    }

    /**
     * Set up real-time listener for conversations
     */
    public ListenerRegistration listenToConversations(String userId, ConversationListCallback callback) {
        return db.collection(COLLECTION_CONVERSATIONS)
                .whereEqualTo("userId", userId)
                .orderBy("lastMessageTime", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to conversations", error);
                        callback.onError("Failed to listen to conversations: " + error.getMessage());
                        return;
                    }
                    
                    if (querySnapshot != null) {
                        List<Conversation> conversations = new ArrayList<>();
                        for (QueryDocumentSnapshot document : querySnapshot) {
                            try {
                                Conversation conversation = document.toObject(Conversation.class);
                                if (conversation != null) {
                                    conversation.setId(document.getId());
                                    conversations.add(conversation);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error converting document to Conversation", e);
                            }
                        }
                        Log.d(TAG, "Real-time update: " + conversations.size() + " conversations");
                        callback.onSuccess(conversations);
                    }
                });
    }

    /**
     * Mark messages as read
     */
    public void markMessagesAsRead(String conversationId, String userId) {
        db.collection(COLLECTION_MESSAGES)
                .whereEqualTo("conversationId", conversationId)
                .whereEqualTo("receiverId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            WriteBatch batch = db.batch();
                            for (QueryDocumentSnapshot document : querySnapshot) {
                                batch.update(document.getReference(), "isRead", true);
                            }
                            batch.commit()
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Messages marked as read");
                                        // Update conversation unread count to 0 and isRead to true
                                        db.collection(COLLECTION_CONVERSATIONS)
                                                .document(conversationId)
                                                .update("unreadCount", 0, "isRead", true)
                                                .addOnSuccessListener(aVoid1 -> 
                                                    Log.d(TAG, "Conversation marked as read"))
                                                .addOnFailureListener(e -> 
                                                    Log.e(TAG, "Error updating conversation read status", e));
                                    })
                                    .addOnFailureListener(e -> 
                                        Log.e(TAG, "Error marking messages as read", e));
                        } else {
                            // Even if no unread messages, update conversation to mark as read
                            db.collection(COLLECTION_CONVERSATIONS)
                                    .document(conversationId)
                                    .update("unreadCount", 0, "isRead", true)
                                    .addOnSuccessListener(aVoid -> 
                                        Log.d(TAG, "Conversation marked as read (no unread messages)"))
                                    .addOnFailureListener(e -> 
                                        Log.e(TAG, "Error updating conversation read status", e));
                        }
                    } else {
                        Log.e(TAG, "Error querying messages", task.getException());
                    }
                });
    }
}
