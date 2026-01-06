package com.example.homerepairs.models;

import com.google.firebase.firestore.PropertyName;

public class Provider {
    private String id; // Firebase document ID
    private String name;
    private String service;
    private double rating;
    private int reviewCount;
    private String availability;
    private String price;
    private String profileImageUrl; // Firebase Storage URL instead of resource ID
    private int profileImageResId; // Fallback to local resource
    private boolean isVerified;
    private boolean isAvailableNow;

    // Default constructor required for Firebase
    public Provider() {
        // Empty constructor for Firebase
    }

    // Constructor for local/mock data
    public Provider(String name, String service, double rating, int reviewCount,
            String availability, String price, int profileImageResId, boolean isVerified, boolean isAvailableNow) {
        this.name = name;
        this.service = service;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.availability = availability;
        this.price = price;
        this.profileImageResId = profileImageResId;
        this.isVerified = isVerified;
        this.isAvailableNow = isAvailableNow;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public int getProfileImageResId() {
        return profileImageResId;
    }

    public void setProfileImageResId(int profileImageResId) {
        this.profileImageResId = profileImageResId;
    }

    @PropertyName("isVerified")
    public boolean isVerified() {
        return isVerified;
    }

    @PropertyName("isVerified")
    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    @PropertyName("isAvailableNow")
    public boolean isAvailableNow() {
        return isAvailableNow;
    }

    @PropertyName("isAvailableNow")
    public void setAvailableNow(boolean availableNow) {
        isAvailableNow = availableNow;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
}
