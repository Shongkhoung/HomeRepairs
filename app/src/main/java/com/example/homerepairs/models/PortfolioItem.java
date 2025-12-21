package com.example.homerepairs.models;

public class PortfolioItem {
    private String imageUrl;
    private String label; // e.g., "Bathroom Renovation Before"
    private String description;

    // Default constructor for Firebase
    public PortfolioItem() {
    }

    public PortfolioItem(String imageUrl, String label, String description) {
        this.imageUrl = imageUrl;
        this.label = label;
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
