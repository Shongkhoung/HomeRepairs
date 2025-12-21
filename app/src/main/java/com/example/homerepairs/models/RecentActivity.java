package com.example.homerepairs.models;

public class RecentActivity {
    private String title;
    private String status;
    private String details;
    private String timeAgo;
    private String actionText;
    private int iconResId;
    private String profileImageUrl; // Firebase Storage URL for provider profile image

    public RecentActivity(String title, String status, String details, String timeAgo, String actionText, int iconResId) {
        this.title = title;
        this.status = status;
        this.details = details;
        this.timeAgo = timeAgo;
        this.actionText = actionText;
        this.iconResId = iconResId;
    }
    
    // Constructor with profile image URL support
    public RecentActivity(String title, String status, String details, String timeAgo, String actionText, int iconResId, String profileImageUrl) {
        this.title = title;
        this.status = status;
        this.details = details;
        this.timeAgo = timeAgo;
        this.actionText = actionText;
        this.iconResId = iconResId;
        this.profileImageUrl = profileImageUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getTimeAgo() {
        return timeAgo;
    }

    public void setTimeAgo(String timeAgo) {
        this.timeAgo = timeAgo;
    }

    public String getActionText() {
        return actionText;
    }

    public void setActionText(String actionText) {
        this.actionText = actionText;
    }

    public int getIconResId() {
        return iconResId;
    }

    public void setIconResId(int iconResId) {
        this.iconResId = iconResId;
    }
    
    public String getProfileImageUrl() {
        return profileImageUrl;
    }
    
    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
}

