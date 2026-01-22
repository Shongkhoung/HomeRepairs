package com.example.homerepairs.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.homerepairs.R;
import com.example.homerepairs.models.Booking;
import com.example.homerepairs.models.FeaturedProvider;
import com.example.homerepairs.models.Provider;
import com.example.homerepairs.models.RecentActivity;
import com.example.homerepairs.models.ServiceCategory;
import com.example.homerepairs.models.WeatherData;
import com.example.homerepairs.services.FirebaseBookingService;
import com.example.homerepairs.services.FirebaseProviderService;
import com.example.homerepairs.services.WeatherService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * ViewModel for Home Screen
 * Manages all data for categories, providers, recent activity, weather, and
 * location
 */
public class HomeViewModel extends ViewModel {

    // LiveData for service categories
    private MutableLiveData<List<ServiceCategory>> categories = new MutableLiveData<>();

    // LiveData for featured providers
    private MutableLiveData<List<FeaturedProvider>> featuredProviders = new MutableLiveData<>();

    // LiveData for recent activities
    private MutableLiveData<List<RecentActivity>> recentActivities = new MutableLiveData<>();

    // LiveData for weather data
    private MutableLiveData<String> weatherData = new MutableLiveData<>();
    private MutableLiveData<WeatherData> weatherDataFull = new MutableLiveData<>();
    private MutableLiveData<String> weatherIconFileName = new MutableLiveData<>(); // Lottie JSON file name

    private WeatherService weatherService;
    private FirebaseProviderService firebaseProviderService;
    private FirebaseBookingService firebaseBookingService;

    // LiveData for user location
    private MutableLiveData<String> userLocation = new MutableLiveData<>();

    // LiveData for loading state
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    // Flag to prevent duplicate weather requests
    private boolean isWeatherRequestInProgress = false;

    // LiveData for error messages
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();

    // LiveData for empty state
    private MutableLiveData<Boolean> isEmpty = new MutableLiveData<>(false);

    public HomeViewModel() {
        // Initialize with empty lists
        categories.setValue(new ArrayList<>());
        featuredProviders.setValue(new ArrayList<>());
        recentActivities.setValue(new ArrayList<>());
        weatherService = new WeatherService();
        firebaseProviderService = new FirebaseProviderService();
        firebaseBookingService = new FirebaseBookingService();
    }

    // Getters for LiveData
    public LiveData<List<ServiceCategory>> getCategories() {
        return categories;
    }

    public LiveData<List<FeaturedProvider>> getFeaturedProviders() {
        return featuredProviders;
    }

    public LiveData<List<RecentActivity>> getRecentActivities() {
        return recentActivities;
    }

    public LiveData<String> getWeatherData() {
        return weatherData;
    }

    public LiveData<WeatherData> getWeatherDataFull() {
        return weatherDataFull;
    }

    public LiveData<String> getWeatherIconFileName() {
        return weatherIconFileName;
    }

    public LiveData<String> getUserLocation() {
        return userLocation;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getIsEmpty() {
        return isEmpty;
    }

    /**
     * Load service categories with real provider counts from Firebase
     */
    // Cache for all providers to avoid multiple network calls
    private List<Provider> allCachedProviders = null;

    /**
     * Load all dashboard data efficiently
     * Fetches providers once and populates all dependent lists
     */
    public void loadDashboardData(String userId) {
        isLoading.setValue(true);

        // Fetch pre-calculated categories from Firestore first
        firebaseProviderService.getCategories(new FirebaseProviderService.CategoryCallback() {
            @Override
            public void onSuccess(List<ServiceCategory> firestoreCategories) {
                if (!firestoreCategories.isEmpty()) {
                    android.util.Log.d("HomeViewModel",
                            "Loaded " + firestoreCategories.size() + " categories from Firestore");
                    // Map icons locally
                    for (ServiceCategory cat : firestoreCategories) {
                        cat.setIconResId(getServiceIcon(cat.getName()));
                    }
                    categories.setValue(firestoreCategories);
                }

                // Now load providers for other sections (and fallback for categories if needed)
                loadAllProvidersCombined(userId, firestoreCategories.isEmpty());
            }

            @Override
            public void onError(String error) {
                android.util.Log.e("HomeViewModel", "Error loading categories from Firestore: " + error);
                // Fallback: fetch all providers
                loadAllProvidersCombined(userId, true);
            }
        });
    }

    private void loadAllProvidersCombined(String userId, boolean calculateCategories) {
        firebaseProviderService.getAllProviders(new FirebaseProviderService.ProviderCallback() {
            @Override
            public void onSuccess(List<Provider> providers) {
                android.util.Log.d("HomeViewModel", "Loaded " + providers.size() + " providers (Combined Load)");
                allCachedProviders = providers;

                // 1. Calculate Categories (if needed)
                if (calculateCategories) {
                    List<ServiceCategory> categoryList = createCategoriesWithCounts(providers);
                    categories.setValue(categoryList);
                    isEmpty.setValue(categoryList.isEmpty());
                }

                // 2. Process Featured Providers
                processFeaturedProviders(providers);

                // 3. Process Recent Activity
                processRecentActivity(providers);

                isLoading.setValue(false);
            }

            @Override
            public void onError(String error) {
                android.util.Log.e("HomeViewModel", "Error loading providers: " + error);

                // Fallback to mock data on error
                if (calculateCategories) {
                    categories.setValue(createMockCategories());
                }
                featuredProviders.setValue(createMockProviders());
                recentActivities.setValue(createMockRecentActivities());

                isLoading.setValue(false);
            }
        });
    }

    /**
     * Deprecated: Use loadDashboardData instead
     */
    public void loadCategories() {
        // Kept for compatibility, redirects to dashboard load
        // Note: This might be called individually, so we handle it gracefully
        if (allCachedProviders != null) {
            // If we already have data, just recalculate (rarely needed)
            List<ServiceCategory> categoryList = createCategoriesWithCounts(allCachedProviders);
            categories.setValue(categoryList);
        }
    }

    /**
     * Deprecated: Use loadDashboardData instead
     */
    public void loadFeaturedProviders() {
        // Kept for compatibility
    }

    /**
     * Deprecated: Use loadDashboardData instead
     */
    public void loadRecentActivity(String userId) {
        // Kept for compatibility
    }

    private List<ServiceCategory> createCategoriesWithCounts(List<Provider> providers) {
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();

        for (Provider provider : providers) {
            String service = provider.getService();
            if (service != null && !service.isEmpty()) {
                counts.put(service, counts.getOrDefault(service, 0) + 1);
            }
        }

        List<ServiceCategory> categoryList = new ArrayList<>();
        for (java.util.Map.Entry<String, Integer> entry : counts.entrySet()) {
            categoryList.add(new ServiceCategory(
                    entry.getKey(),
                    entry.getValue(),
                    getServiceIcon(entry.getKey())));
        }

        // Sort categories by provider count (descending)
        Collections.sort(categoryList, (c1, c2) -> Integer.compare(c2.getProviderCount(), c1.getProviderCount()));

        return categoryList;
    }

    private List<FeaturedProvider> convertToFeaturedProviders(List<Provider> providers) {
        List<FeaturedProvider> featuredList = new ArrayList<>();
        for (Provider provider : providers) {
            String service = provider.getService() != null ? provider.getService() : "Service";

            // Use the constructor that accepts profileImageUrl
            FeaturedProvider featured = new FeaturedProvider(
                    provider.getName(),
                    service,
                    provider.getRating(),
                    provider.getReviewCount(),
                    provider.getAvailability(),
                    provider.getPrice(),
                    getServiceIcon(service), // Fallback icon ID
                    provider.getProfileImageUrl(), // Image URL
                    provider.isAvailableNow(),
                    provider.isVerified(),
                    provider.getResponseTime(),
                    provider.getJobsCompleted());

            featured.setId(provider.getId());
            featuredList.add(featured);
        }
        return featuredList;
    }

    private void processFeaturedProviders(List<Provider> providers) {
        // Convert Provider to FeaturedProvider and select top providers
        List<FeaturedProvider> featuredList = convertToFeaturedProviders(providers);

        // Sort by rating (descending), then by review count (descending)
        Collections.sort(featuredList, new Comparator<FeaturedProvider>() {
            @Override
            public int compare(FeaturedProvider p1, FeaturedProvider p2) {
                int ratingCompare = Double.compare(p2.getRating(), p1.getRating());
                if (ratingCompare != 0) {
                    return ratingCompare;
                }
                return Integer.compare(p2.getReviewCount(), p1.getReviewCount());
            }
        });

        // Limit to top 5 featured providers
        if (featuredList.size() > 5) {
            featuredList = featuredList.subList(0, 5);
        }

        // If no providers from Firebase, fall back to mock data
        if (featuredList.isEmpty()) {
            featuredList = createMockProviders();
        }

        featuredProviders.setValue(featuredList);
    }

    private void processRecentActivity(List<Provider> providers) {
        // Convert Provider to RecentActivity
        List<RecentActivity> activityList = convertProvidersToRecentActivities(providers);

        // Limit to top 5 providers (sorted by rating)
        if (activityList.size() > 5) {
            activityList = activityList.subList(0, 5);
        }

        // If no providers from Firebase, fall back to mock data
        if (activityList.isEmpty()) {
            activityList = createMockRecentActivities();
        }

        recentActivities.setValue(activityList);
    }

    /**
     * Convert Provider objects to RecentActivity objects
     */
    private List<RecentActivity> convertProvidersToRecentActivities(List<Provider> providers) {
        List<RecentActivity> activityList = new ArrayList<>();

        // Sort providers by rating (highest first) for better display
        Collections.sort(providers, new Comparator<Provider>() {
            @Override
            public int compare(Provider p1, Provider p2) {
                return Double.compare(p2.getRating(), p1.getRating());
            }
        });

        for (Provider provider : providers) {
            // Get service icon based on service category
            int iconResId = getServiceIcon(provider.getService());

            // Title: Service name (e.g., "Plumbing Service" or just service category)
            String serviceName = provider.getService() != null ? provider.getService() : "Service";
            String title = serviceName + " Service";
            if (title.length() > 25) {
                title = title.substring(0, 25) + "...";
            }

            // Status: Based on availability - show "Completed" for available providers
            // In the new design, we show "Completed" status
            String status = "Completed";

            // Provider name: Full provider name
            String providerName = provider.getName() != null ? provider.getName() : "Provider";

            // Time ago: Show "Recently" or calculate from provider data
            // Since we don't have booking dates, show "Recently" for all providers
            String timeAgo = "Recently";

            // Details: Provider name (will be shown in tvProviderName)
            String details = providerName;

            // Action text: Not used in new design but keep for compatibility
            String actionText = "";

            // Get provider profile image URL
            String profileImageUrl = provider.getProfileImageUrl();

            RecentActivity activity = new RecentActivity(
                    title, // Service name
                    status, // "Completed"
                    details, // Provider name
                    timeAgo, // "Recently"
                    actionText, // Empty (not shown)
                    iconResId,
                    profileImageUrl // Pass profile image URL
            );

            activityList.add(activity);
        }

        return activityList;
    }

    /**
     * Get service icon resource ID based on service category name
     */
    private int getServiceIcon(String serviceCategory) {
        if (serviceCategory == null) {
            return R.drawable.handyman;
        }

        switch (serviceCategory.toLowerCase()) {
            case "plumbing":
                return R.drawable.plumbing;
            case "electrical":
                return R.drawable.electrical;
            case "hvac":
                return R.drawable.hvac;
            case "carpentry":
                return R.drawable.carpentry;
            case "painting":
                return R.drawable.painting;
            case "appliance":
            case "appliance repair":
                return R.drawable.appliance_repair;
            case "roofing":
                return R.drawable.roofing;
            case "landscaping":
                return R.drawable.landscaping;
            case "cleaning":
                return R.drawable.cleaning;
            case "handyman":
                return R.drawable.handyman;
            default:
                return R.drawable.handyman;
        }
    }

    /**
     * Format time ago from Date
     */
    private String formatTimeAgo(Date date) {
        if (date == null) {
            return "Recently";
        }

        long now = System.currentTimeMillis();
        long then = date.getTime();
        long diff = now - then;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long weeks = days / 7;

        if (weeks > 0) {
            return weeks + (weeks == 1 ? " week ago" : " weeks ago");
        } else if (days > 0) {
            return days + (days == 1 ? " day ago" : " days ago");
        } else if (hours > 0) {
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        } else if (minutes > 0) {
            return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        } else {
            return "Just now";
        }
    }

    /**
     * Fetch weather data using WeatherService
     * 
     * @param latitude   Latitude coordinate
     * @param longitude  Longitude coordinate
     * @param hasNetwork true if network is available, false otherwise
     */
    public void fetchWeather(double latitude, double longitude, boolean hasNetwork) {
        if (!hasNetwork) {
            // No network - use default weather immediately
            weatherData.postValue("22°C Sunny");
            weatherIconFileName.postValue("sunny_weather.json");
            isLoading.postValue(false);
            isWeatherRequestInProgress = false;
            // Don't set error message for offline mode - it's expected behavior
            return;
        }

        // Prevent duplicate requests
        if (isWeatherRequestInProgress) {
            android.util.Log.d("HomeViewModel", "Weather request already in progress, skipping duplicate call");
            return;
        }

        isWeatherRequestInProgress = true;
        isLoading.setValue(true);
        android.util.Log.d("HomeViewModel", "Fetching weather for lat: " + latitude + ", lon: " + longitude);
        weatherService.getCurrentWeather(latitude, longitude, new WeatherService.WeatherCallback() {
            @Override
            public void onSuccess(WeatherData data) {
                android.util.Log.d("HomeViewModel", "Weather fetch successful: " + data.getDisplayText());
                // Determine if it's day time (5 AM to 9 PM) for rain icon selection
                java.util.Calendar calendar = java.util.Calendar.getInstance();
                int hourOfDay = calendar.get(java.util.Calendar.HOUR_OF_DAY);
                boolean isDayTime = hourOfDay >= 5 && hourOfDay < 21;

                String lottieFileName = data.getLottieFileName(isDayTime);
                android.util.Log.d("HomeViewModel", "Weather Lottie file: " + lottieFileName);
                android.util.Log.d("HomeViewModel", "Weather condition: " + data.getCondition());
                isWeatherRequestInProgress = false;
                weatherDataFull.postValue(data);
                weatherData.postValue(data.getDisplayText());
                weatherIconFileName.postValue(lottieFileName);
                isLoading.postValue(false);
            }

            @Override
            public void onError(String error) {
                android.util.Log.e("HomeViewModel", "Weather fetch error: " + error);
                isWeatherRequestInProgress = false;
                // Fallback to default weather on error
                weatherData.postValue("22°C Sunny");
                weatherIconFileName.postValue("sunny_weather.json");
                errorMessage.postValue(error);
                isLoading.postValue(false);
            }
        });
    }

    /**
     * Set user location
     */
    public void setUserLocation(String location) {
        userLocation.setValue(location);
    }

    /**
     * Set error message
     */
    public void setError(String error) {
        errorMessage.setValue(error);
        isLoading.setValue(false);
    }

    // Mock data creation methods
    private List<ServiceCategory> createMockCategories() {
        List<ServiceCategory> list = new ArrayList<>();
        // Using actual PNG icons from Sevice Icons folder
        list.add(new ServiceCategory("Plumbing", 12, R.drawable.plumbing));
        list.add(new ServiceCategory("Electrical", 10, R.drawable.electrical));
        list.add(new ServiceCategory("HVAC", 11, R.drawable.hvac));
        list.add(new ServiceCategory("Carpentry", 10, R.drawable.carpentry));
        list.add(new ServiceCategory("Painting", 10, R.drawable.painting));
        list.add(new ServiceCategory("Appliance", 10, R.drawable.appliance_repair));
        list.add(new ServiceCategory("Roofing", 10, R.drawable.roofing));
        list.add(new ServiceCategory("Landscaping", 10, R.drawable.landscaping));
        list.add(new ServiceCategory("Cleaning", 10, R.drawable.cleaning));
        list.add(new ServiceCategory("Handyman", 2, R.drawable.handyman));
        return list;
    }

    private List<FeaturedProvider> createMockProviders() {
        List<FeaturedProvider> list = new ArrayList<>();
        list.add(new FeaturedProvider("Ethan Carter", "Plumbing", 4.8, 123,
                "Available Now", "$60/hr", R.drawable.no_profile_image, true));
        list.add(new FeaturedProvider("Sophia Bennett", "Electrical", 4.9, 156,
                "Next available: 2pm", "$75/hr", R.drawable.no_profile_image, false));
        list.add(new FeaturedProvider("Liam Harper", "HVAC", 4.7, 89,
                "Available Now", "$65/hr", R.drawable.no_profile_image, true));
        return list;
    }

    private List<RecentActivity> createMockRecentActivities() {
        List<RecentActivity> list = new ArrayList<>();
        // Using actual PNG icons from Sevice Icons folder
        // profileImageUrl is null for mock data (will use service icon)
        list.add(new RecentActivity("Leaky Faucet Repair", "Completed",
                "Plumbing · Ethan Carter", "2 days ago", "Book Again",
                R.drawable.plumbing, null));
        list.add(new RecentActivity("Outlet Installation", "Upcoming",
                "Electrical · Sophia Bennett", "5 days ago", "View Details",
                R.drawable.electrical, null));
        list.add(new RecentActivity("AC Maintenance", "Cancelled",
                "HVAC · Liam Harper", "1 week ago", "Book Again",
                R.drawable.hvac, null));
        return list;
    }
}
