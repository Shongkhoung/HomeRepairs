package com.example.homerepairs.models;

public class ServiceCategory {
    private String name;
    private int providerCount;
    private int iconResId;

    public ServiceCategory() {
        // Required for Firestore
    }

    public ServiceCategory(String name, int providerCount, int iconResId) {
        this.name = name;
        this.providerCount = providerCount;
        this.iconResId = iconResId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getProviderCount() {
        return providerCount;
    }

    public void setProviderCount(int providerCount) {
        this.providerCount = providerCount;
    }

    public int getIconResId() {
        return iconResId;
    }

    public void setIconResId(int iconResId) {
        this.iconResId = iconResId;
    }
}
