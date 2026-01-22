package com.example.homerepairs.models;

import com.google.firebase.firestore.PropertyName;
import java.util.List;
import java.util.Map;

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

    // New API Fields
    private Map<String, Integer> ratingDistribution;
    private String portfolioDescription;
    private boolean hasCertifications;
    private String about;
    private boolean hasBusinessRegistration;
    private boolean hasLicense;
    private List<Review> reviews;
    private List<PortfolioItem> portfolio;
    private List<String> paymentMethods;
    private String serviceArea; // Changed from List<String> to String to match ProviderDetail
    private List<String> languages;
    private List<Map<String, String>> typicalJobCosts; // Changed to match ProviderDetail
    private String responseTime;
    private List<String> services; // Detailed list of services
    private boolean hasBackgroundCheck;
    private int yearsInBusiness; // ProviderDetail uses int, matches
    private String jobsCompleted; // Changed to String to match ProviderDetail
    private String repeatCustomers; // Changed to String to match ProviderDetail
    private String pricingRange;
    private boolean hasInsurance;
    private boolean isFavorite;
    private String phoneNumber;
    private String email;

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

    // Handle typo in existing Firestore data
    @PropertyName("avialability")
    public void setAvialability(String avialability) {
        this.availability = avialability;
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

    public Map<String, Integer> getRatingDistribution() {
        return ratingDistribution;
    }

    public void setRatingDistribution(Map<String, Integer> ratingDistribution) {
        this.ratingDistribution = ratingDistribution;
    }

    public String getPortfolioDescription() {
        return portfolioDescription;
    }

    public void setPortfolioDescription(String portfolioDescription) {
        this.portfolioDescription = portfolioDescription;
    }

    public boolean isHasCertifications() {
        return hasCertifications;
    }

    public void setHasCertifications(boolean hasCertifications) {
        this.hasCertifications = hasCertifications;
    }

    public String getAbout() {
        return about;
    }

    public void setAbout(String about) {
        this.about = about;
    }

    public boolean isHasBusinessRegistration() {
        return hasBusinessRegistration;
    }

    public void setHasBusinessRegistration(boolean hasBusinessRegistration) {
        this.hasBusinessRegistration = hasBusinessRegistration;
    }

    public boolean isHasLicense() {
        return hasLicense;
    }

    public void setHasLicense(boolean hasLicense) {
        this.hasLicense = hasLicense;
    }

    public List<Review> getReviews() {
        return reviews;
    }

    public void setReviews(List<Review> reviews) {
        this.reviews = reviews;
    }

    public List<PortfolioItem> getPortfolio() {
        return portfolio;
    }

    public void setPortfolio(List<PortfolioItem> portfolio) {
        this.portfolio = portfolio;
    }

    public List<String> getPaymentMethods() {
        return paymentMethods;
    }

    public void setPaymentMethods(List<String> paymentMethods) {
        this.paymentMethods = paymentMethods;
    }

    public String getServiceArea() {
        return serviceArea;
    }

    public void setServiceArea(String serviceArea) {
        this.serviceArea = serviceArea;
    }

    public List<String> getLanguages() {
        return languages;
    }

    public void setLanguages(List<String> languages) {
        this.languages = languages;
    }

    public List<Map<String, String>> getTypicalJobCosts() {
        return typicalJobCosts;
    }

    public void setTypicalJobCosts(List<Map<String, String>> typicalJobCosts) {
        this.typicalJobCosts = typicalJobCosts;
    }

    public String getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(String responseTime) {
        this.responseTime = responseTime;
    }

    public List<String> getServices() {
        return services;
    }

    public void setServices(List<String> services) {
        this.services = services;
    }

    public boolean isHasBackgroundCheck() {
        return hasBackgroundCheck;
    }

    public void setHasBackgroundCheck(boolean hasBackgroundCheck) {
        this.hasBackgroundCheck = hasBackgroundCheck;
    }

    public int getYearsInBusiness() {
        return yearsInBusiness;
    }

    public void setYearsInBusiness(int yearsInBusiness) {
        this.yearsInBusiness = yearsInBusiness;
    }

    public String getJobsCompleted() {
        return jobsCompleted;
    }

    public void setJobsCompleted(String jobsCompleted) {
        this.jobsCompleted = jobsCompleted;
    }

    public String getRepeatCustomers() {
        return repeatCustomers;
    }

    public void setRepeatCustomers(String repeatCustomers) {
        this.repeatCustomers = repeatCustomers;
    }

    public String getPricingRange() {
        return pricingRange;
    }

    public void setPricingRange(String pricingRange) {
        this.pricingRange = pricingRange;
    }

    public boolean isHasInsurance() {
        return hasInsurance;
    }

    public void setHasInsurance(boolean hasInsurance) {
        this.hasInsurance = hasInsurance;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
