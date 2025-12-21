package com.example.homerepairs.models;

import com.google.firebase.firestore.PropertyName;
import java.util.List;
import java.util.Map;

public class ProviderDetail extends Provider {
    // Basic Info
    private int yearsInBusiness;
    private String jobsCompleted;
    private String responseTime;
    private String repeatCustomers;
    private String about;
    
    // Services offered (list of service names)
    private List<String> services;
    
    // Pricing details
    private String pricingRange; // e.g., "$75 - $125 / hr"
    private List<Map<String, String>> typicalJobCosts; // e.g., [{"job": "Leak Repair", "cost": "$100 - $200"}]
    
    // Payment methods
    private List<String> paymentMethods;
    
    // Service area
    private String serviceArea;
    
    // Languages spoken
    private List<String> languages;
    
    // Credentials
    private boolean hasLicense;
    private boolean hasInsurance;
    private boolean hasCertifications;
    private boolean hasBackgroundCheck;
    private boolean hasBusinessRegistration;
    
    // Reviews
    private List<Review> reviews;
    private Map<String, Integer> ratingDistribution; // e.g., {"5": 70, "4": 20, "3": 5, "2": 3, "1": 2}
    
    // Portfolio
    private List<PortfolioItem> portfolio;
    private String portfolioDescription;
    
    // Default constructor for Firebase
    public ProviderDetail() {
        super();
    }

    // Getters and Setters
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

    public String getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(String responseTime) {
        this.responseTime = responseTime;
    }

    public String getRepeatCustomers() {
        return repeatCustomers;
    }

    public void setRepeatCustomers(String repeatCustomers) {
        this.repeatCustomers = repeatCustomers;
    }

    public String getAbout() {
        return about;
    }

    public void setAbout(String about) {
        this.about = about;
    }

    public List<String> getServices() {
        return services;
    }

    public void setServices(List<String> services) {
        this.services = services;
    }

    public String getPricingRange() {
        return pricingRange;
    }

    public void setPricingRange(String pricingRange) {
        this.pricingRange = pricingRange;
    }

    public List<Map<String, String>> getTypicalJobCosts() {
        return typicalJobCosts;
    }

    public void setTypicalJobCosts(List<Map<String, String>> typicalJobCosts) {
        this.typicalJobCosts = typicalJobCosts;
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

    @PropertyName("hasLicense")
    public boolean isHasLicense() {
        return hasLicense;
    }

    @PropertyName("hasLicense")
    public void setHasLicense(boolean hasLicense) {
        this.hasLicense = hasLicense;
    }

    @PropertyName("hasInsurance")
    public boolean isHasInsurance() {
        return hasInsurance;
    }

    @PropertyName("hasInsurance")
    public void setHasInsurance(boolean hasInsurance) {
        this.hasInsurance = hasInsurance;
    }

    @PropertyName("hasCertifications")
    public boolean isHasCertifications() {
        return hasCertifications;
    }

    @PropertyName("hasCertifications")
    public void setHasCertifications(boolean hasCertifications) {
        this.hasCertifications = hasCertifications;
    }

    @PropertyName("hasBackgroundCheck")
    public boolean isHasBackgroundCheck() {
        return hasBackgroundCheck;
    }

    @PropertyName("hasBackgroundCheck")
    public void setHasBackgroundCheck(boolean hasBackgroundCheck) {
        this.hasBackgroundCheck = hasBackgroundCheck;
    }

    @PropertyName("hasBusinessRegistration")
    public boolean isHasBusinessRegistration() {
        return hasBusinessRegistration;
    }

    @PropertyName("hasBusinessRegistration")
    public void setHasBusinessRegistration(boolean hasBusinessRegistration) {
        this.hasBusinessRegistration = hasBusinessRegistration;
    }

    public List<Review> getReviews() {
        return reviews;
    }

    public void setReviews(List<Review> reviews) {
        this.reviews = reviews;
    }

    public Map<String, Integer> getRatingDistribution() {
        return ratingDistribution;
    }

    public void setRatingDistribution(Map<String, Integer> ratingDistribution) {
        this.ratingDistribution = ratingDistribution;
    }

    public List<PortfolioItem> getPortfolio() {
        return portfolio;
    }

    public void setPortfolio(List<PortfolioItem> portfolio) {
        this.portfolio = portfolio;
    }

    public String getPortfolioDescription() {
        return portfolioDescription;
    }

    public void setPortfolioDescription(String portfolioDescription) {
        this.portfolioDescription = portfolioDescription;
    }
}
