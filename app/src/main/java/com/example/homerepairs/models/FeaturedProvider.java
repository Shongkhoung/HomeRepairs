package com.example.homerepairs.models;

public class FeaturedProvider {
    private String id; // Provider ID for favorites management
    private String name;
    private String service;
    private double rating;
    private int reviewCount;
    private String availability;
    private String price;
    private int profileImageResId;
    private String profileImageUrl; // Firebase Storage URL
    private boolean isAvailableNow;
    private boolean isVerified;
    private String responseTime; // e.g., "~10 min"
    private String jobsCompleted; // e.g., "328 jobs" or "328"
    private String phoneNumber;

    public FeaturedProvider(String name, String service, double rating, int reviewCount,
            String availability, String price, int profileImageResId, boolean isAvailableNow) {
        this.name = name;
        this.service = service;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.availability = availability;
        this.price = price;
        this.profileImageResId = profileImageResId;
        this.isAvailableNow = isAvailableNow;
        this.phoneNumber = "+85512345678"; // Default
    }

    // Constructor with image URL support
    public FeaturedProvider(String name, String service, double rating, int reviewCount,
            String availability, String price, int profileImageResId,
            String profileImageUrl, boolean isAvailableNow) {
        this.name = name;
        this.service = service;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.availability = availability;
        this.price = price;
        this.profileImageResId = profileImageResId;
        this.profileImageUrl = profileImageUrl;
        this.isAvailableNow = isAvailableNow;
        this.phoneNumber = "+85512345678"; // Default
    }

    // Full constructor with all fields
    public FeaturedProvider(String name, String service, double rating, int reviewCount,
            String availability, String price, int profileImageResId,
            String profileImageUrl, boolean isAvailableNow, boolean isVerified,
            String responseTime, String jobsCompleted) {
        this.name = name;
        this.service = service;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.availability = availability;
        this.price = price;
        this.profileImageResId = profileImageResId;
        this.profileImageUrl = profileImageUrl;
        this.isAvailableNow = isAvailableNow;
        this.isVerified = isVerified;
        this.responseTime = responseTime;
        this.jobsCompleted = jobsCompleted;
        this.phoneNumber = "+85512345678"; // Default
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

    public boolean isAvailableNow() {
        return isAvailableNow;
    }

    public void setAvailableNow(boolean availableNow) {
        isAvailableNow = availableNow;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        this.isVerified = verified;
    }

    public String getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(String responseTime) {
        this.responseTime = responseTime;
    }

    public String getJobsCompleted() {
        return jobsCompleted;
    }

    public void setJobsCompleted(String jobsCompleted) {
        this.jobsCompleted = jobsCompleted;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
