package com.example.homerepairs.models;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class Message {
    private String id; // Firebase document ID
    private String conversationId; // ID of the conversation this message belongs to
    private String senderId; // User ID of the sender
    private String senderName; // Name of the sender
    private String receiverId; // User ID of the receiver
    private String receiverName; // Name of the receiver
    private String text; // Message text content
    private Date timestamp; // When the message was sent
    private boolean isRead; // Whether the message has been read
    private String type; // "text", "image", "system"
    
    // Default constructor required for Firebase
    public Message() {
        // Empty constructor for Firebase
    }
    
    // Constructor for creating a new message
    public Message(String conversationId, String senderId, String senderName, 
                   String receiverId, String receiverName, String text) {
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.receiverId = receiverId;
        this.receiverName = receiverName;
        this.text = text;
        this.type = "text";
        this.isRead = false;
        this.timestamp = new Date();
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getConversationId() {
        return conversationId;
    }
    
    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
    
    public String getSenderId() {
        return senderId;
    }
    
    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }
    
    public String getSenderName() {
        return senderName;
    }
    
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
    
    public String getReceiverId() {
        return receiverId;
    }
    
    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }
    
    public String getReceiverName() {
        return receiverName;
    }
    
    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    @ServerTimestamp
    public Date getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
    
    public boolean isRead() {
        return isRead;
    }
    
    public void setRead(boolean read) {
        isRead = read;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
}
