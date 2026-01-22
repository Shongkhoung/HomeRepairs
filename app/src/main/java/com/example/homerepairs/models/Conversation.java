package com.example.homerepairs.models;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class Conversation {
    private String id; // Firebase document ID
    private String userId; // User ID (customer)
    private String userName; // User name
    private String providerId; // Provider ID
    private String providerName; // Provider name
    private String lastMessage; // Last message text
    private Date lastMessageTime; // Timestamp of last message
    private int unreadCount; // Number of unread messages for current user
    private Date createdAt; // When conversation was created
    private Date updatedAt; // When conversation was last updated
    private String bookingId; // Optional: related booking ID
    private String bookingReference; // Optional: booking reference for context
    private boolean isPinned; // Whether conversation is pinned
    private boolean isStarred; // Whether conversation is starred
    private boolean isRead; // Whether last message is read

    // Default constructor required for Firebase
    public Conversation() {
        // Empty constructor for Firebase
    }

    // Constructor for creating a new conversation
    public Conversation(String userId, String userName, String providerId, String providerName) {
        this.userId = userId;
        this.userName = userName;
        this.providerId = providerId;
        this.providerName = providerName;
        this.lastMessage = "";
        this.unreadCount = 0;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    @ServerTimestamp
    public Date getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(Date lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    @ServerTimestamp
    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    @ServerTimestamp
    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getBookingReference() {
        return bookingReference;
    }

    public void setBookingReference(String bookingReference) {
        this.bookingReference = bookingReference;
    }

    public boolean isPinned() {
        return isPinned;
    }

    public void setPinned(boolean pinned) {
        isPinned = pinned;
    }

    public boolean isStarred() {
        return isStarred;
    }

    public void setStarred(boolean starred) {
        isStarred = starred;
    }

    @com.google.firebase.firestore.PropertyName("isRead")
    public boolean isRead() {
        return isRead;
    }

    @com.google.firebase.firestore.PropertyName("isRead")
    public void setRead(boolean read) {
        isRead = read;
    }
}
