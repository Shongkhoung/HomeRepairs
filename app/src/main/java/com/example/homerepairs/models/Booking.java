package com.example.homerepairs.models;

import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
import java.util.List;

public class Booking {
    private String id; // Firebase document ID
    private String userId; // User who created the booking
    private String providerId; // Provider ID (if selected)
    private String providerName; // Provider name
    private String serviceCategory; // e.g., "Plumbing"
    private String serviceName; // e.g., "Leaky faucet"
    private String issueDescription; // Detailed description
    private String urgency; // "Emergency", "Same Day", "Schedule Later"
    private String status; // "Pending", "Confirmed", "In Progress", "Completed", "Cancelled"
    private String bookingReference; // Unique booking reference number
    private String serviceDateTime; // Scheduled date and time
    private String propertyLocation; // Property address
    private String phoneNumber; // Contact phone number
    private String propertyName; // e.g., "Home"
    private List<String> photoUrls; // URLs of uploaded photos
    private String paymentMethod; // Payment method selected (e.g., "wallet", "card", "cash")
    private Date createdAt; // Timestamp when booking was created
    private Date updatedAt; // Timestamp when booking was last updated
    private String additionalNotes; // Any additional notes or instructions

    // Default constructor required for Firebase
    public Booking() {
        // Empty constructor for Firebase
    }

    // Constructor for creating a new booking
    public Booking(String userId, String providerId, String providerName, String serviceCategory,
            String serviceName, String issueDescription, String urgency) {
        this.userId = userId;
        this.providerId = providerId;
        this.providerName = providerName;
        this.serviceCategory = serviceCategory;
        this.serviceName = serviceName;
        this.issueDescription = issueDescription;
        this.urgency = urgency;
        this.status = "Pending";
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

    public String getServiceCategory() {
        return serviceCategory;
    }

    public void setServiceCategory(String serviceCategory) {
        this.serviceCategory = serviceCategory;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getIssueDescription() {
        return issueDescription;
    }

    public void setIssueDescription(String issueDescription) {
        this.issueDescription = issueDescription;
    }

    public String getUrgency() {
        return urgency;
    }

    public void setUrgency(String urgency) {
        this.urgency = urgency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBookingReference() {
        return bookingReference;
    }

    public void setBookingReference(String bookingReference) {
        this.bookingReference = bookingReference;
    }

    public String getServiceDateTime() {
        return serviceDateTime;
    }

    public void setServiceDateTime(String serviceDateTime) {
        this.serviceDateTime = serviceDateTime;
    }

    public String getPropertyLocation() {
        return propertyLocation;
    }

    public void setPropertyLocation(String propertyLocation) {
        this.propertyLocation = propertyLocation;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public List<String> getPhotoUrls() {
        return photoUrls;
    }

    public void setPhotoUrls(List<String> photoUrls) {
        this.photoUrls = photoUrls;
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

    public String getAdditionalNotes() {
        return additionalNotes;
    }

    public void setAdditionalNotes(String additionalNotes) {
        this.additionalNotes = additionalNotes;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
