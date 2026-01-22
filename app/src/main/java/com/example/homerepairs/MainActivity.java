package com.example.homerepairs;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homerepairs.adapters.ProviderAdapter;
import com.example.homerepairs.adapters.RecentActivityAdapterEnhanced;
import com.example.homerepairs.adapters.ServiceCategoryAdapter;
import com.example.homerepairs.models.FeaturedProvider;
import com.example.homerepairs.models.RecentActivity;
import com.example.homerepairs.models.ServiceCategory;
import com.example.homerepairs.utils.AuthHelper;
import com.example.homerepairs.utils.NetworkUtils;
import com.example.homerepairs.viewmodel.HomeViewModel;
import com.example.homerepairs.databinding.ActivityMainBinding;
import com.example.homerepairs.services.FirebaseUserService;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.airbnb.lottie.LottieAnimationView;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends BaseActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private HomeViewModel viewModel;
    private FusedLocationProviderClient fusedLocationClient;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    // Store last known location for refetching when internet is restored
    private double lastKnownLatitude = 11.5564; // Default: Phnom Penh
    private double lastKnownLongitude = 104.9282;

    // Handler for periodic weather refresh
    private android.os.Handler weatherRefreshHandler;
    private Runnable weatherRefreshRunnable;
    private static final long WEATHER_REFRESH_INTERVAL = 900000; // 15 minutes (was 1 minute)

    // Debounce mechanism to prevent duplicate weather fetches
    private android.os.Handler weatherDebounceHandler;
    private Runnable weatherDebounceRunnable;
    private static final long WEATHER_DEBOUNCE_DELAY = 2000; // 2 seconds debounce
    private long lastWeatherFetchTime = 0;

    // Search Debounce
    private android.os.Handler searchDebounceHandler = new android.os.Handler();
    private Runnable searchDebounceRunnable;
    private static final long SEARCH_DEBOUNCE_DELAY = 300; // 300ms debounce

    // Track network state to prevent excessive weather fetches
    private boolean wasInternetAvailable = false;

    // User data
    private FirebaseUserService userService;
    private ListenerRegistration userProfileListener;
    private String currentUserName = null;

    private ActivityMainBinding binding;
    private android.os.Handler offlineDelayHandler;
    private Runnable offlineDelayRunnable;

    // Adapters
    private ServiceCategoryAdapter categoryAdapter;
    private ProviderAdapter providerAdapter;
    private RecentActivityAdapterEnhanced activityAdapter;

    // Category display state
    private boolean showingAllCategories = false;
    private List<ServiceCategory> allCategories = new java.util.ArrayList<>();
    private List<ServiceCategory> originalCategories = new java.util.ArrayList<>(); // Store original for search

    // Provider display state
    private List<FeaturedProvider> originalProviders = new java.util.ArrayList<>(); // Store original for search

    // Recent Activity display state
    private boolean showingAllActivities = false;
    private List<RecentActivity> allActivities = new java.util.ArrayList<>();

    // Search state
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if user is authenticated
        if (!AuthHelper.isAuthenticated()) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.putExtra("mode", "sign_in");
            startActivity(intent);
            finish();
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase Authentication
        initializeFirebaseAuth();

        // Initialize user service
        userService = new FirebaseUserService();

        initializeViews();
        setupViewModel();
        setupAdapters();
        setupRecyclerViews();
        setupButtons();
        setupBottomNavigation();
        requestLocationPermission();
        setupNetworkCallback();

        loadUserData();

        weatherDebounceHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        setupWeatherRefresh();

        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            checkNetworkStatus();
        }, 500);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update greeting in case time of day has changed
        updateGreeting();
        // Check network status immediately when app resumes
        checkNetworkStatus();
        // Refresh weather data when app resumes
        refreshWeatherIfOnline();
        // Start periodic weather refresh
        startWeatherRefresh();

        // Clear search text and focus when returning to the screen
        if (binding.etSearch != null) {
            binding.etSearch.setText("");
            binding.etSearch.clearFocus();
            // Hide keyboard if visible
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(
                    android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(binding.etSearch.getWindowToken(), 0);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister network callback when app goes to background
        unregisterNetworkCallback();
        // Stop periodic weather refresh
        stopWeatherRefresh();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up network callback
        unregisterNetworkCallback();

        // Remove user profile listener
        if (userProfileListener != null) {
            userProfileListener.remove();
            userProfileListener = null;
        }

        // Clean up offline delay handler
        if (offlineDelayHandler != null && offlineDelayRunnable != null) {
            offlineDelayHandler.removeCallbacks(offlineDelayRunnable);
            offlineDelayRunnable = null;
        }
        // Clean up weather refresh
        stopWeatherRefresh();
        if (weatherDebounceHandler != null && weatherDebounceRunnable != null) {
            weatherDebounceHandler.removeCallbacks(weatherDebounceRunnable);
        }
    }

    private void initializeViews() {
        offlineDelayHandler = new android.os.Handler();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        updateGreeting();
        updateProfileIcon();
        binding.tvLocation.setText(getString(R.string.default_location));
        binding.tvWeather.setText(getString(R.string.default_weather));

        binding.ivWeatherIcon.setAnimation(R.raw.sunny_weather);
        binding.ivWeatherIcon.playAnimation();

        binding.ivEmergencyIcon.setAnimation(R.raw.amercency);
        binding.ivEmergencyIcon.playAnimation();

        showInternetLoading(false);
    }

    /**
     * Get greeting message based on current time of day
     * 
     * @return Greeting string (Good Morning/Afternoon/Evening)
     *         Time frames:
     *         - Good Morning: 12:00 a.m. to 12:00 p.m. (noon)
     *         - Good Afternoon: 12:00 p.m. to 6:00 p.m.
     *         - Good Evening: 6:00 p.m. until midnight
     */
    private String getTimeBasedGreeting() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int hourOfDay = calendar.get(java.util.Calendar.HOUR_OF_DAY);

        String greeting;
        if (hourOfDay >= 0 && hourOfDay < 12) {
            // 12:00 a.m. to 12:00 p.m. (noon)
            greeting = getString(R.string.greeting_morning);
        } else if (hourOfDay >= 12 && hourOfDay < 18) {
            // 12:00 p.m. to 6:00 p.m.
            greeting = getString(R.string.greeting_afternoon);
        } else {
            // 6:00 p.m. until midnight (18:00 to 23:59)
            greeting = getString(R.string.greeting_evening);
        }

        // Use real user name if available, otherwise use fallback
        String userName = currentUserName != null ? currentUserName : AuthHelper.getCurrentUserName(this);
        if (userName == null || userName.isEmpty() || userName.equals("You")) {
            userName = getString(R.string.default_user); // Fallback
        }

        return String.format("%s, %s \uD83D\uDC4B", greeting, userName);
    }

    /**
     * Load user data from Firestore and set up real-time listener
     */
    private void loadUserData() {
        String userId = AuthHelper.getCurrentUserId(this);
        if (userId == null) {
            // User not authenticated, use default greeting
            updateGreeting();
            return;
        }

        // Set up real-time listener for user profile
        userProfileListener = userService.listenToUserProfile(userId, new FirebaseUserService.UserProfileCallback() {
            @Override
            public void onSuccess(java.util.Map<String, Object> userProfile) {
                // Extract user name and email from profile (same as UserProfileActivity)
                String name = null;
                String email = null;

                if (userProfile != null && !userProfile.isEmpty()) {
                    name = userProfile.get("name") != null ? userProfile.get("name").toString() : null;
                    email = userProfile.get("email") != null ? userProfile.get("email").toString() : null;

                    if (name != null && !name.isEmpty()) {
                        currentUserName = name;
                        // Save to SharedPreferences for quick access
                        AuthHelper.saveUserName(MainActivity.this, name);
                    }
                }

                // Fallback to Firebase Auth if Firestore doesn't have data
                com.google.firebase.auth.FirebaseUser user = AuthHelper.getCurrentUser();
                if (user != null) {
                    if (currentUserName == null || currentUserName.isEmpty()) {
                        if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                            currentUserName = user.getDisplayName();
                        }
                    }

                    if (email == null || email.isEmpty()) {
                        email = user.getEmail();
                    }
                }

                // Update greeting with real user name
                updateGreeting();

                // Update profile icon with letter only (no image loading)
                updateProfileIcon(name, email);
            }

            @Override
            public void onError(String error) {
                android.util.Log.e("MainActivity", "Error loading user profile: " + error);
                // Fallback to AuthHelper for user name
                currentUserName = AuthHelper.getCurrentUserName(MainActivity.this);

                // Fallback to Firebase Auth for email
                com.google.firebase.auth.FirebaseUser user = AuthHelper.getCurrentUser();
                String email = null;
                if (user != null) {
                    email = user.getEmail();
                }

                updateGreeting();
                updateProfileIcon(currentUserName, email);
            }
        });
    }

    /**
     * Update profile icon with initial letter only (no image loading)
     * Uses the same logic as UserProfileActivity to ensure consistency
     */
    private void updateProfileIcon() {
        // Get current user data for letter calculation
        String name = currentUserName;
        String email = null;
        com.google.firebase.auth.FirebaseUser user = AuthHelper.getCurrentUser();
        if (user != null) {
            email = user.getEmail();
        }
        updateProfileIcon(name, email);
    }

    /**
     * Update profile icon with initial letter only (no image loading)
     * 
     * @param name  User's name (from Firestore or Firebase Auth)
     * @param email User's email (for fallback letter)
     */
    private void updateProfileIcon(String name, String email) {
        if (binding.ivProfileIcon == null || binding.tvProfileLetter == null) {
            return;
        }

        // Always show letter, never load images (matching profile screen behavior)
        binding.ivProfileIcon.setVisibility(View.GONE);
        binding.tvProfileLetter.setVisibility(View.VISIBLE);

        // Get first letter - same priority as profile screen:
        // 1. First letter of name
        // 2. First letter of email
        // 3. Default "U"
        String firstLetter = "U";
        if (name != null && !name.isEmpty()) {
            firstLetter = name.substring(0, 1).toUpperCase();
        } else if (email != null && !email.isEmpty()) {
            firstLetter = email.substring(0, 1).toUpperCase();
        }

        binding.tvProfileLetter.setText(firstLetter);
    }

    /**
     * Update greeting text with current user name
     */
    private void updateGreeting() {
        if (binding.tvGreeting != null) {
            binding.tvGreeting.setText(getTimeBasedGreeting());
        }
    }

    /**
     * Convert Lottie file name to R.raw resource ID
     * 
     * @param fileName Lottie JSON file name (e.g., "sunny_weather.json")
     * @return Resource ID from R.raw, or 0 if not found
     */
    private int getLottieResourceId(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return 0;
        }

        // Remove .json extension if present
        String name = fileName.replace(".json", "");

        // Map file names to R.raw resource IDs
        switch (name) {
            case "sunny":
                return R.raw.sunny;
            case "night":
                return R.raw.night;
            case "sunny_weather":
                return R.raw.sunny_weather;
            case "day_rain":
                return R.raw.day_rain;
            case "night_rain":
                return R.raw.night_rain;
            case "thunder":
                return R.raw.thunder;
            case "fog":
                return R.raw.fog;
            default:
                android.util.Log.w("MainActivity", "Unknown Lottie file: " + fileName);
                return 0;
        }
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        // Observe categories
        viewModel.getCategories().observe(this, categories -> {
            if (categories != null && !categories.isEmpty()) {
                // Show ALL categories immediately (User Request: Show all, no see more/less)
                allCategories = new java.util.ArrayList<>(categories);
                originalCategories = new java.util.ArrayList<>(categories);

                categoryAdapter.updateCategories(allCategories);
                categoryAdapter.setSearchQuery(currentSearchQuery); // Apply search highlighting
                showingAllCategories = true;

                // Hide the View All button since we are showing everything
                if (binding.tvViewAllServices != null) {
                    binding.tvViewAllServices.setVisibility(View.GONE);
                }
            }
        });

        // Observe featured providers
        viewModel.getFeaturedProviders().observe(this, providers -> {
            if (providers != null && !providers.isEmpty()) {
                // Store original providers for search highlighting
                originalProviders = new java.util.ArrayList<>(providers);
                providerAdapter.updateProviders(providers);
                providerAdapter.setSearchQuery(currentSearchQuery); // Apply search highlighting
                // Also set providers in category adapter for search matching by provider name
                categoryAdapter.setProviders(originalProviders);
            }
        });

        // Observe recent activities
        viewModel.getRecentActivities().observe(this, activities -> {
            if (activities != null && !activities.isEmpty()) {
                allActivities = new java.util.ArrayList<>(activities);
                // Show ALL activities immediately (User Request: Show all, no see more/less)
                activityAdapter.updateActivities(allActivities);
                showingAllActivities = true;

                // Hide View All button
                if (binding.tvViewAllActivity != null) {
                    binding.tvViewAllActivity.setVisibility(View.GONE);
                }
            }
        });

        // Observe weather
        viewModel.getWeatherData().observe(this, weather -> {
            if (weather != null) {
                android.util.Log.d("MainActivity", "Weather data updated: " + weather);
                binding.tvWeather.setText(weather);
            }
        });

        // Observe weather icon (Lottie animation)
        viewModel.getWeatherIconFileName().observe(this, fileName -> {
            android.util.Log.d("MainActivity", "Weather icon observer triggered, fileName: " + fileName
                    + ", binding.ivWeatherIcon null: " + (binding.ivWeatherIcon == null));
            if (fileName != null && !fileName.isEmpty() && binding.ivWeatherIcon != null) {
                android.util.Log.d("MainActivity", "Setting weather icon to Lottie file: " + fileName);
                int resId = getLottieResourceId(fileName);
                if (resId != 0) {
                    binding.ivWeatherIcon.setAnimation(resId);
                    binding.ivWeatherIcon.playAnimation();
                    android.util.Log.d("MainActivity", "Weather icon updated successfully");
                } else {
                    android.util.Log.e("MainActivity", "Invalid Lottie file name: " + fileName);
                }
            } else {
                android.util.Log.w("MainActivity",
                        "Cannot update weather icon - fileName: " + fileName + ", binding.ivWeatherIcon: "
                                + binding.ivWeatherIcon);
            }
        });

        // Observe location
        viewModel.getUserLocation().observe(this, location -> {
            if (location != null) {
                binding.tvLocation.setText(location);
            }
        });

        // Observe loading state
        viewModel.getIsLoading().observe(this, isLoading -> {
            if (binding.progressBar != null) {
                binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        });

        // Observe error messages
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                // Only show toast for network-related errors, not for offline mode
                if (error.contains("Failed to fetch weather") || error.contains("Weather API")) {
                    // Check if it's a network issue
                    boolean hasNetwork = NetworkUtils.isNetworkAvailable(this);
                    showInternetLoading(!hasNetwork);
                    if (hasNetwork) {
                        Toast.makeText(this, getString(R.string.error_weather_fetch), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        // Load data
        // Load optimized dashboard data (Categories, Featured, Recent)
        String userId = AuthHelper.getCurrentUserId(this);
        if (userId != null) {
            viewModel.loadDashboardData(userId);
        } else {
            // This should not happen as we check authentication in onCreate
            android.util.Log.e("MainActivity", "getCurrentUserId returned null - user should be authenticated");
            // Still try to load data without user ID context if needed
            viewModel.loadDashboardData(null);
        }
    }

    private void setupAdapters() {
        // Category adapter - initialize with empty list if categories not loaded yet
        List<ServiceCategory> initialCategories = viewModel.getCategories().getValue();
        if (initialCategories == null) {
            initialCategories = new java.util.ArrayList<>();
        }
        categoryAdapter = new ServiceCategoryAdapter(
                initialCategories,
                category -> {
                    if (category != null) {
                        android.util.Log.d("MainActivity", "Category clicked: " + category.getName());
                        Toast.makeText(MainActivity.this, getString(R.string.opening_category, category.getName()),
                                Toast.LENGTH_SHORT).show();
                        try {
                            Intent intent = new Intent(MainActivity.this, PlumbingActivity.class);
                            intent.putExtra("category_name", category.getName());
                            startActivity(intent);
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                        } catch (Exception e) {
                            android.util.Log.e("MainActivity", "Error starting PlumbingActivity", e);
                            Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        android.util.Log.e("MainActivity", "Category is null in click listener");
                        Toast.makeText(MainActivity.this, "Error: Category is null", Toast.LENGTH_SHORT).show();
                    }
                });

        // Provider adapter
        providerAdapter = new ProviderAdapter(
                viewModel.getFeaturedProviders().getValue(),
                provider -> {
                    Intent intent = new Intent(MainActivity.this, ProviderProfileActivity.class);
                    intent.putExtra("provider_name", provider.getName());
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                });

        // Activity adapter - now shows providers from Firebase
        activityAdapter = new RecentActivityAdapterEnhanced(
                viewModel.getRecentActivities().getValue(),
                new RecentActivityAdapterEnhanced.OnActivityClickListener() {
                    @Override
                    public void onBookAgain(RecentActivity activity) {
                        // Navigate to provider profile or booking screen
                        // Activity title contains provider name
                        String providerName = activity.getTitle();
                        if (providerName != null && !providerName.isEmpty()) {
                            // Remove "..." if present
                            providerName = providerName.replace("...", "");
                            Intent intent = new Intent(MainActivity.this, ProviderProfileActivity.class);
                            intent.putExtra("provider_name", providerName.trim());
                            startActivity(intent);
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                        } else {
                            // Fallback to booking screen
                            Intent intent = new Intent(MainActivity.this, NewBookingActivity.class);
                            startActivity(intent);
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                        }
                    }

                    @Override
                    public void onViewDetails(RecentActivity activity) {
                        // Navigate to provider profile
                        String providerName = activity.getTitle();
                        if (providerName != null && !providerName.isEmpty()) {
                            // Remove "..." if present
                            providerName = providerName.replace("...", "");
                            Intent intent = new Intent(MainActivity.this, ProviderProfileActivity.class);
                            intent.putExtra("provider_name", providerName.trim());
                            startActivity(intent);
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                        } else {
                            Toast.makeText(MainActivity.this, getString(R.string.error_provider_not_found),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancel(RecentActivity activity) {
                        // For providers, cancel doesn't apply - navigate to profile instead
                        String providerName = activity.getTitle();
                        if (providerName != null && !providerName.isEmpty()) {
                            providerName = providerName.replace("...", "");
                            Intent intent = new Intent(MainActivity.this, ProviderProfileActivity.class);
                            intent.putExtra("provider_name", providerName.trim());
                            startActivity(intent);
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                        }
                    }
                });
    }

    private void setupRecyclerViews() {
        // Categories - Grid layout (3 columns for better fit on 6.43-inch screen)
        if (binding.rvCategories != null && categoryAdapter != null) {
            GridLayoutManager categoryLayoutManager = new GridLayoutManager(this, 3);
            binding.rvCategories.setLayoutManager(categoryLayoutManager);
            binding.rvCategories.setAdapter(categoryAdapter);
            binding.rvCategories.setHasFixedSize(true);

            // Disable individual item animator to prevent "random" flight during layout
            // changes
            binding.rvCategories.setItemAnimator(null);

            android.util.Log.d("MainActivity",
                    "RecyclerView setup complete, adapter item count: " + categoryAdapter.getItemCount());
        } else {
            android.util.Log.e("MainActivity", "RecyclerView or adapter is null!");
        }

        // Featured providers - Horizontal layout
        // Horizontal layout for Featured Pro cards (scrollable cards)
        LinearLayoutManager providerLayoutManager = new LinearLayoutManager(
                this, LinearLayoutManager.HORIZONTAL, false);
        binding.rvFeaturedProviders.setLayoutManager(providerLayoutManager);
        binding.rvFeaturedProviders.setAdapter(providerAdapter);
        binding.rvFeaturedProviders.setItemAnimator(null);

        // Calculate card width to match recent activity card width
        // Recent activity: RecyclerView width = screen width - 40dp (container padding
        // 20dp each side)
        // Card width = RecyclerView width - 32dp (16dp margin each side) = screen width
        // - 72dp
        // Featured provider: RecyclerView width = screen width - 40dp (container
        // padding 20dp each side)
        // Card width should match recent activity = screen width - 72dp
        // Card margins = 16dp + 8dp = 24dp
        binding.rvFeaturedProviders.post(() -> {
            int recyclerViewWidth = binding.rvFeaturedProviders.getWidth();
            float density = getResources().getDisplayMetrics().density;
            // Recent activity card width = screen width - 72dp
            // Since RecyclerView width = screen width - 40dp, we can calculate:
            // card width = recyclerViewWidth - 32dp (to match recent activity card width)
            int cardWidth = recyclerViewWidth - (int) (32 * density);

            // Set the card width on the adapter
            if (providerAdapter != null) {
                providerAdapter.setCardWidth(cardWidth);
            }

            // Add padding to center the last card on screen
            // Account for card margins: 16dp left + 8dp right = 24dp total
            int cardWithMargins = cardWidth + (int) (24 * density);
            int paddingEnd = (recyclerViewWidth - cardWithMargins) / 2;
            if (paddingEnd > 0) {
                binding.rvFeaturedProviders.setPadding(0, 0, paddingEnd, 0);
                binding.rvFeaturedProviders.setClipToPadding(false);
            }
        });

        // Recent activity - Vertical layout
        LinearLayoutManager activityLayoutManager = new LinearLayoutManager(this);
        binding.rvRecentActivity.setLayoutManager(activityLayoutManager);
        binding.rvRecentActivity.setAdapter(activityAdapter);
    }

    private void setupButtons() {
        // Profile icon click - navigate to Profile screen
        if (binding.cardProfileIcon != null) {
            binding.cardProfileIcon.setOnClickListener(v -> {
                Intent intent = new Intent(this, UserProfileActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            });
        }

        binding.btnNotification.setOnClickListener(v -> {
            Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show();
        });

        binding.btnEmergency.setOnClickListener(v -> navigateToBooking("Emergency"));
        binding.btnScheduleLater.setOnClickListener(v -> navigateToBooking("Schedule Later"));

        binding.btnDocument.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FavoritesActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        // View all services click handler
        if (binding.tvViewAllServices != null) {
            binding.tvViewAllServices.setOnClickListener(v -> {
                toggleCategoriesView();
            });
        }

        // View all activity click handler - toggle expand/collapse
        if (binding.tvViewAllActivity != null) {
            binding.tvViewAllActivity.setOnClickListener(v -> {
                toggleActivitiesView();
            });
        }

        // Setup search functionality with real-time filtering
        // Setup Search Actions
        setupSearch();

        // Seed Firebase Data (One-time) in background thread
        // Seed Firebase Data (One-time) in background thread
        new Thread(() -> {
            // Seed providers and update counts
            com.example.homerepairs.utils.FirebaseSeeder.seedProviders(this);

            // Check if images already seeded to prevent re-randomizing on every launch
            android.content.SharedPreferences prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
            if (prefs.getBoolean("images_seeded_v1", false)) {
                return;
            }

            // Update all provider profiles with image URL
            // Wait a bit for seeding to complete before updating images
            try {
                Thread.sleep(3000); // Wait 3 seconds for seeding to complete
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Update all providers with random images from the list
            java.util.List<String> imageUrls = new java.util.ArrayList<>();
            imageUrls.add("https://i.pinimg.com/1200x/e0/cc/4b/e0cc4b56a0ae8eff72afa090591abfbe.jpg");
            imageUrls.add("https://i.pinimg.com/736x/c1/9e/8c/c19e8c264bdb02463f66c04e3bf97204.jpg");
            imageUrls.add("https://i.pinimg.com/736x/76/36/6c/76366cca75463a631ef27a91c9966a34.jpg");
            imageUrls.add("https://i.pinimg.com/736x/88/6c/4b/886c4b6802d3092892b528186d50694f.jpg");
            imageUrls.add("https://i.pinimg.com/736x/f2/38/f9/f238f9ba5fa9a3f2b0fc4a8e2aa6a38b.jpg");

            com.example.homerepairs.utils.ProviderImageUpdater.updateProvidersWithRandomImages(this, imageUrls);

            // Mark images as seeded
            prefs.edit().putBoolean("images_seeded_v1", true).apply();
        }).start();
    }

    /**
     * Setup search functionality to filter categories
     */
    private void setupSearch() {
        if (binding.etSearch == null) {
            return;
        }

        // Clear focus when touching the background/scroll view
        if (binding.nsvMain != null) {
            binding.nsvMain.setOnTouchListener((v, event) -> {
                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    if (binding.etSearch != null && binding.etSearch.hasFocus()) {
                        binding.etSearch.clearFocus();
                        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(
                                android.content.Context.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.hideSoftInputFromWindow(binding.etSearch.getWindowToken(), 0);
                        }
                    }
                }
                return false; // Let scrolling happen
            });
        }

        // SCROLL OPTIMIZATION: Scroll search bar to top when focused
        binding.etSearch.setOnFocusChangeListener((v, hasFocus) -> {
            if (binding.bottomNavigation != null) {
                binding.bottomNavigation.setVisibility(hasFocus ? View.GONE : View.VISIBLE);
            }

            // Adjust padding: 0 when typing (so content goes to keyboard), 80dp when not
            // (to clear nav bar)
            if (binding.nsvMain != null) {
                int paddingBottom = hasFocus ? 0 : (int) (80 * getResources().getDisplayMetrics().density);
                binding.nsvMain.setPadding(0, 0, 0, paddingBottom);
            }

            if (hasFocus) {
                // Determine the scroll position to bring the search bar to the top
                // We need to account for any toolbar/header height if necessary
                android.view.View searchCard = binding.cvSearchBar;

                if (searchCard != null && binding.nsvMain != null) {
                    // Post to message queue to ensure layout is measured
                    binding.nsvMain.post(() -> {
                        // Get text location
                        int top = searchCard.getTop();
                        // Smooth scroll to that position
                        binding.nsvMain.smoothScrollTo(0, top - 40); // 40px buffer for padding
                    });
                }
            }
        });

        // Add text change listener for real-time search with debounce
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Remove pending search runnable
                if (searchDebounceHandler != null && searchDebounceRunnable != null) {
                    searchDebounceHandler.removeCallbacks(searchDebounceRunnable);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Post new search runnable with delay
                searchDebounceRunnable = () -> {
                    currentSearchQuery = s.toString().toLowerCase().trim();
                    filterBySearchQuery(currentSearchQuery);
                };
                searchDebounceHandler.postDelayed(searchDebounceRunnable, SEARCH_DEBOUNCE_DELAY);
            }
        });

        // Handle keyboard "Search" or "Enter" action
        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                    actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER
                            && event.getAction() == android.view.KeyEvent.ACTION_DOWN)) {

                String query = binding.etSearch.getText().toString().trim();
                if (!query.isEmpty()) {
                    android.util.Log.d("MainActivity", "Search action triggering navigation for: " + query);

                    // Hide keyboard
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(
                            android.content.Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(binding.etSearch.getWindowToken(), 0);
                    }

                    // Clear focus to show nav bar
                    binding.etSearch.clearFocus();

                    // Show searching animation as full screen loader
                    // showSearchLoading(true); // REMOVED

                    // Navigate to results screen (reusing PlumbingActivity as generic list)
                    Intent intent = new Intent(MainActivity.this, PlumbingActivity.class);
                    intent.putExtra("category_name", "Search Results");
                    intent.putExtra("search_query", query);
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);

                    return true;
                }
            }
            return false;
        });
    }

    /**
     * Highlight matching categories and providers based on search query
     * All items remain visible, but matches are highlighted and non-matches are
     * dimmed
     */
    /**
     * Filter categories and providers based on search query
     * Non-matching items are removed from the view
     */
    private void filterBySearchQuery(String query) {
        currentSearchQuery = query;
        android.util.Log.d("MainActivity", "filterBySearchQuery called with query: '" + query + "'");

        if (query.isEmpty()) {
            // RESTORE DEFAULT VIEW
            // Categories
            List<ServiceCategory> categoriesToShow;
            if (showingAllCategories) {
                categoriesToShow = new java.util.ArrayList<>(originalCategories);
            } else {
                if (originalCategories.size() > 6) {
                    categoriesToShow = new java.util.ArrayList<>(originalCategories.subList(0, 6));
                } else {
                    categoriesToShow = new java.util.ArrayList<>(originalCategories);
                }
            }
            categoryAdapter.updateCategories(categoriesToShow);
            // Hide View All button (User Request)
            if (binding.tvViewAllServices != null) {
                binding.tvViewAllServices.setVisibility(View.GONE);
            }

            // Providers
            providerAdapter.updateProviders(new java.util.ArrayList<>(originalProviders));

            // Activities
            // Always show all (User Request)
            activityAdapter.updateActivities(new java.util.ArrayList<>(allActivities));

            // Hide View All button
            if (binding.tvViewAllActivity != null) {
                binding.tvViewAllActivity.setVisibility(View.GONE);
            }

            // Hide No Results
            // if (llNoSearchResults != null)
            // llNoSearchResults.setVisibility(View.GONE);

        } else {
            // SEARCH MODE
            String queryLower = query.toLowerCase();

            // 1. Do NOT Filter Categories (User Request: All provider/category cards must
            // still be visible)
            // We kept the search highlighting logic in the adapter, but we pass ALL
            // categories now.
            List<ServiceCategory> filteredCategories = new java.util.ArrayList<>();
            for (ServiceCategory cat : originalCategories) {
                // Add all categories, regardless of match
                filteredCategories.add(cat);
            }

            // 2. Do NOT Filter Providers (User Request: All provider cards must still be
            // visible)
            // We kept the original logic for categories and activities, but providers
            // remain unfiltered
            List<FeaturedProvider> filteredProviders = new java.util.ArrayList<>(originalProviders);

            // 3. Filter Activities
            List<RecentActivity> filteredActivities = new java.util.ArrayList<>();
            for (RecentActivity act : allActivities) {
                if (act.getTitle() != null && act.getTitle().toLowerCase().contains(queryLower)) {
                    filteredActivities.add(act);
                }
            }

            // boolean hasResults = !filteredCategories.isEmpty() ||
            // !filteredProviders.isEmpty()
            // || !filteredActivities.isEmpty();

            // if (!hasResults) {
            // Show Animation
            // if (llNoSearchResults != null) {
            // llNoSearchResults.setVisibility(View.VISIBLE);
            // // Play animation if not already playing
            // if (ivSearchingAnimation != null && !ivSearchingAnimation.isAnimating()) {
            // ivSearchingAnimation.playAnimation();
            // }
            // }

            // Update adapters with empty lists
            // categoryAdapter.updateCategories(new java.util.ArrayList<>());
            // providerAdapter.updateProviders(new java.util.ArrayList<>());
            // activityAdapter.updateActivities(new java.util.ArrayList<>());

            // } else {
            // Show Matches
            // if (llNoSearchResults != null) {
            // llNoSearchResults.setVisibility(View.GONE);
            // }

            categoryAdapter.updateCategories(filteredCategories);
            providerAdapter.updateProviders(filteredProviders);
            activityAdapter.updateActivities(filteredActivities);
            // }

            // Hide "View All" buttons during search
            if (binding.tvViewAllServices != null) {
                binding.tvViewAllServices.setVisibility(View.INVISIBLE);
            }
            if (binding.tvViewAllActivity != null) {
                binding.tvViewAllActivity.setVisibility(View.GONE);
            }
        }

        // Pass query to adapters for highlighting
        categoryAdapter.setSearchQuery(query);
        providerAdapter.setSearchQuery(query);
        activityAdapter.setSearchQuery(query);
    }

    /**
     * Toggle between showing 6 categories and all categories with smooth animation
     */
    private void toggleCategoriesView() {
        if (originalCategories == null || originalCategories.isEmpty()) {
            return;
        }

        // Disable clicks during animation
        if (binding.tvViewAllServices != null) {
            binding.tvViewAllServices.setEnabled(false);
        }

        if (showingAllCategories) {
            // Animate layout changes
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                android.transition.TransitionManager.beginDelayedTransition(binding.llMainContent,
                        new android.transition.ChangeBounds().setDuration(300));
            }
            // Show only first 6 with smooth animation
            List<ServiceCategory> limitedCategories;
            if (originalCategories.size() > 6) {
                limitedCategories = new java.util.ArrayList<>(originalCategories.subList(0, 6));
            } else {
                limitedCategories = new java.util.ArrayList<>(originalCategories);
            }

            categoryAdapter.updateCategoriesGranular(limitedCategories, false);
            categoryAdapter.setSearchQuery(currentSearchQuery); // Maintain search highlighting
            showingAllCategories = false;
            if (binding.tvViewAllServices != null) {
                binding.tvViewAllServices.setText(getString(R.string.view_all_arrow));
            }

            // Re-enable after animation starts (shorter delay for better UX)
            binding.rvCategories.postDelayed(() -> {
                if (binding.tvViewAllServices != null) {
                    binding.tvViewAllServices.setEnabled(true);
                }
            }, 300);
        } else {
            // Animate layout changes - REMOVED to prevent card "rotation" effect
            /*
             * if (android.os.Build.VERSION.SDK_INT >=
             * android.os.Build.VERSION_CODES.KITKAT) {
             * android.transition.TransitionManager.beginDelayedTransition(binding.
             * llMainContent,
             * new android.transition.ChangeBounds().setDuration(300));
             * }
             */
            // Show all categories with smooth animation
            categoryAdapter.updateCategoriesGranular(new java.util.ArrayList<>(originalCategories), true);
            categoryAdapter.setSearchQuery(currentSearchQuery); // Maintain search highlighting
            showingAllCategories = true;
            if (binding.tvViewAllServices != null) {
                binding.tvViewAllServices.setText(getString(R.string.view_less_arrow));
            }

            // Re-enable after animation starts (shorter delay for better UX)
            binding.rvCategories.postDelayed(() -> {
                if (binding.tvViewAllServices != null) {
                    binding.tvViewAllServices.setEnabled(true);
                }
            }, 300);
        }
    }

    /**
     * Toggle between showing 2 activities and all activities
     */
    private void toggleActivitiesView() {
        if (allActivities == null || allActivities.isEmpty()) {
            return;
        }

        // Disable clicks during update
        if (binding.tvViewAllActivity != null) {
            binding.tvViewAllActivity.setEnabled(false);
        }

        if (showingAllActivities) {
            // Animate layout changes - REMOVED to prevent card "rotation" effect
            /*
             * if (android.os.Build.VERSION.SDK_INT >=
             * android.os.Build.VERSION_CODES.KITKAT) {
             * android.transition.TransitionManager.beginDelayedTransition(binding.
             * llMainContent,
             * new android.transition.ChangeBounds().setDuration(300));
             * }
             */
            // Collapse: Show only first 2 activities
            List<RecentActivity> limitedActivities;
            if (allActivities.size() > 2) {
                limitedActivities = new java.util.ArrayList<>(allActivities.subList(0, 2));
            } else {
                limitedActivities = new java.util.ArrayList<>(allActivities);
            }

            activityAdapter.updateActivitiesGranular(limitedActivities, false);
            showingAllActivities = false;
            updateViewAllActivityButton();

            // Re-enable after update
            binding.rvRecentActivity.postDelayed(() -> {
                if (binding.tvViewAllActivity != null) {
                    binding.tvViewAllActivity.setEnabled(true);
                }
            }, 100);
        } else {
            // Animate layout changes - REMOVED to prevent card "rotation" effect
            /*
             * if (android.os.Build.VERSION.SDK_INT >=
             * android.os.Build.VERSION_CODES.KITKAT) {
             * android.transition.TransitionManager.beginDelayedTransition(binding.
             * llMainContent,
             * new android.transition.ChangeBounds().setDuration(300));
             * }
             */
            // Expand: Show all activities
            activityAdapter.updateActivitiesGranular(new java.util.ArrayList<>(allActivities), true);
            showingAllActivities = true;
            updateViewAllActivityButton();

            // Re-enable after update
            binding.rvRecentActivity.postDelayed(() -> {
                if (binding.tvViewAllActivity != null) {
                    binding.tvViewAllActivity.setEnabled(true);
                }
            }, 100);
        }
    }

    /**
     * Update "View All" button text for activities based on current state
     */
    private void updateViewAllActivityButton() {
        if (binding.tvViewAllActivity == null) {
            return;
        }

        if (showingAllActivities) {
            binding.tvViewAllActivity.setText(getString(R.string.view_less));
            binding.tvViewAllActivity.setVisibility(View.VISIBLE);
        } else {
            // Only show "View All" if there are more than 2 activities
            if (allActivities != null && allActivities.size() > 2) {
                binding.tvViewAllActivity.setText(getString(R.string.view_all_caps));
                binding.tvViewAllActivity.setVisibility(View.VISIBLE);
            } else {
                // Hide button if 2 or fewer activities
                binding.tvViewAllActivity.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Helper method to navigate to booking screen with urgency level
     */
    private void navigateToBooking(String urgency) {
        try {
            Intent intent = new Intent(MainActivity.this, NewBookingActivity.class);
            intent.putExtra("urgency", urgency);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error navigating to NewBookingActivity", e);
            Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupBottomNavigation() {
        // Remove elevation/shadow to eliminate divider
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            binding.bottomNavigation.setElevation(0f);
        }

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                // Already on home
                return true;
            } else if (itemId == R.id.nav_bookings) {
                Intent intent = new Intent(this, BookingsActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
                return true;
            } else if (itemId == R.id.nav_messages) {
                Intent intent = new Intent(this, MessagesActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                Intent intent = new Intent(this, UserProfileActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
                return true;
            }
            return false;
        });
        binding.bottomNavigation.setSelectedItemId(R.id.nav_home);
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                // Use default location (Phnom Penh)
                lastKnownLatitude = 11.5564;
                lastKnownLongitude = 104.9282;
                viewModel.setUserLocation("Phnom Penh");
                boolean hasNetwork = NetworkUtils.isNetworkAvailable(this);
                viewModel.fetchWeather(lastKnownLatitude, lastKnownLongitude, hasNetwork); // Phnom Penh coordinates
                showInternetLoading(!hasNetwork);
            }
        }
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            boolean hasNetwork = NetworkUtils.isNetworkAvailable(this);
            showInternetLoading(!hasNetwork);
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();
                            // Store location for later use
                            lastKnownLatitude = latitude;
                            lastKnownLongitude = longitude;
                            viewModel.fetchWeather(latitude, longitude, hasNetwork);
                            // Get city name from coordinates using reverse geocoding
                            if (hasNetwork) {
                                getCityNameFromLocation(latitude, longitude);
                            } else {
                                viewModel.setUserLocation("Current Location");
                            }
                        } else {
                            // Try to get current location if last location is null
                            requestLocationUpdates();
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Fallback to default
                        lastKnownLatitude = 11.5564;
                        lastKnownLongitude = 104.9282;
                        viewModel.setUserLocation("Phnom Penh");
                        viewModel.fetchWeather(11.5564, 104.9282, hasNetwork);
                    });
        } else {
            // No permission - use default
            boolean hasNetwork = NetworkUtils.isNetworkAvailable(this);
            lastKnownLatitude = 11.5564;
            lastKnownLongitude = 104.9282;
            viewModel.setUserLocation("Phnom Penh");
            viewModel.fetchWeather(11.5564, 104.9282, hasNetwork);
            showInternetLoading(!hasNetwork);
        }
    }

    private void requestLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            boolean hasNetwork = NetworkUtils.isNetworkAvailable(this);
            showInternetLoading(!hasNetwork);
            LocationRequest locationRequest = LocationRequest.create();
            // Use balanced power accuracy instead of high accuracy for better battery life
            locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
            locationRequest.setNumUpdates(1);
            locationRequest.setInterval(10000);
            locationRequest.setMaxWaitTime(15000); // Max wait time for location

            fusedLocationClient.requestLocationUpdates(locationRequest,
                    new LocationCallback() {
                        @Override
                        public void onLocationResult(LocationResult locationResult) {
                            if (locationResult != null && locationResult.getLastLocation() != null) {
                                Location location = locationResult.getLastLocation();
                                double latitude = location.getLatitude();
                                double longitude = location.getLongitude();
                                // Store location for later use
                                lastKnownLatitude = latitude;
                                lastKnownLongitude = longitude;
                                viewModel.fetchWeather(latitude, longitude, hasNetwork);
                                if (hasNetwork) {
                                    getCityNameFromLocation(latitude, longitude);
                                } else {
                                    viewModel.setUserLocation("Current Location");
                                }
                                fusedLocationClient.removeLocationUpdates(this);
                            } else {
                                // Fallback to default
                                lastKnownLatitude = 11.5564;
                                lastKnownLongitude = 104.9282;
                                viewModel.setUserLocation("Phnom Penh");
                                viewModel.fetchWeather(11.5564, 104.9282, hasNetwork);
                            }
                        }
                    },
                    getMainLooper());
        }
    }

    /**
     * Show or hide internet loading indicator with message
     * 
     * @param show true to show, false to hide
     */
    private void showInternetLoading(boolean show) {
        if (binding.llInternetLoading == null) {
            return;
        }

        // Cancel any pending delay
        if (offlineDelayHandler != null && offlineDelayRunnable != null) {
            offlineDelayHandler.removeCallbacks(offlineDelayRunnable);
            offlineDelayRunnable = null;
        }

        if (show) {
            // Create delay runnable for 2 seconds
            offlineDelayRunnable = new Runnable() {
                @Override
                public void run() {
                    if (binding.llInternetLoading == null) {
                        return;
                    }

                    // Show with smooth fade-in animation after 2 second delay
                    binding.llInternetLoading.setVisibility(View.VISIBLE);
                    binding.llInternetLoading.setAlpha(0f);

                    // Fade in overlay background
                    binding.llInternetLoading.animate()
                            .alpha(1f)
                            .setDuration(400)
                            .setStartDelay(100)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator())
                            .start();

                    // Animate loading content with delay
                    if (binding.tvInternetMessage != null && binding.ivNoConnection != null) {
                        binding.tvInternetMessage.setAlpha(0f);
                        binding.ivNoConnection.setAlpha(0f);
                        binding.ivNoConnection.setScaleX(0.8f);
                        binding.ivNoConnection.setScaleY(0.8f);

                        // Start Lottie animation
                        if (binding.ivNoConnection != null) {
                            binding.ivNoConnection.setAnimation(R.raw.no_connection);
                            binding.ivNoConnection.playAnimation();
                        }

                        // Animate text first
                        binding.tvInternetMessage.animate()
                                .alpha(1f)
                                .setDuration(400)
                                .setStartDelay(300)
                                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                                .start();

                        // Animate Lottie animation with scale effect
                        binding.ivNoConnection.animate()
                                .alpha(1f)
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(400)
                                .setStartDelay(400)
                                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                                .start();
                    }
                }
            };

            // Post delay of 2 seconds (2000 milliseconds)
            offlineDelayHandler.postDelayed(offlineDelayRunnable, 2000);
        } else {
            // Hide immediately when internet is restored
            binding.llInternetLoading.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .setInterpolator(new android.view.animation.AccelerateInterpolator())
                    .withEndAction(() -> {
                        if (binding.llInternetLoading != null) {
                            binding.llInternetLoading.setVisibility(View.GONE);
                        }
                        if (binding.ivNoConnection != null) {
                            binding.ivNoConnection.pauseAnimation();
                        }
                    })
                    .start();
        }
    }

    /**
     * Setup network callback to monitor network state changes in real-time
     */
    private void setupNetworkCallback() {
        if (connectivityManager == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    runOnUiThread(() -> {
                        android.util.Log.d("MainActivity", "Network available - hiding loading and fetching real data");
                        showInternetLoading(false);
                        // Only fetch if we didn't already have internet (prevent duplicate fetches)
                        if (!wasInternetAvailable) {
                            wasInternetAvailable = true;
                            // Fetch real weather data when internet is restored (with delay to avoid
                            // duplicates)
                            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                                refetchWeatherData();
                            }, 1000);
                        }
                    });
                }

                @Override
                public void onLost(Network network) {
                    runOnUiThread(() -> {
                        android.util.Log.d("MainActivity", "Network lost - showing loading");
                        showInternetLoading(true);
                        wasInternetAvailable = false;
                    });
                }

                @Override
                public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                    runOnUiThread(() -> {
                        boolean hasInternet = networkCapabilities != null &&
                                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                        android.util.Log.d("MainActivity",
                                "Network capabilities changed - hasInternet: " + hasInternet);
                        showInternetLoading(!hasInternet);

                        // Only fetch weather if internet just became available (transition from offline
                        // to online)
                        // This prevents excessive fetches when capabilities change while already online
                        if (hasInternet && !wasInternetAvailable) {
                            wasInternetAvailable = true;
                            android.util.Log.d("MainActivity", "Internet just became available - fetching weather");
                            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                                refetchWeatherData();
                            }, 1000);
                        } else if (!hasInternet) {
                            wasInternetAvailable = false;
                        }
                        // If already had internet and still has it, don't refetch
                    });
                }
            };

            NetworkRequest networkRequest = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    .build();

            try {
                connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
            } catch (Exception e) {
                android.util.Log.e("MainActivity", "Error registering network callback", e);
            }
        }
    }

    /**
     * Unregister network callback
     */
    private void unregisterNetworkCallback() {
        if (connectivityManager != null && networkCallback != null
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception e) {
                android.util.Log.e("MainActivity", "Error unregistering network callback", e);
            }
        }
    }

    /**
     * Check network status in real-time and update loading indicator
     */
    private void checkNetworkStatus() {
        if (binding.llInternetLoading == null) {
            return; // Views not initialized yet
        }

        // Initialize network state tracking
        boolean hasNetwork = NetworkUtils.isNetworkAvailable(this);
        wasInternetAvailable = hasNetwork;
        android.util.Log.d("MainActivity", "Network available: " + hasNetwork);
        showInternetLoading(!hasNetwork);
    }

    /**
     * Refetch weather data when internet is restored (with debouncing)
     */
    private void refetchWeatherData() {
        if (weatherDebounceHandler == null) {
            // Handler not initialized yet, perform fetch immediately
            performWeatherFetch();
            return;
        }

        long currentTime = System.currentTimeMillis();
        long timeSinceLastFetch = currentTime - lastWeatherFetchTime;

        // If a fetch happened recently, cancel the pending one and schedule a new one
        if (weatherDebounceRunnable != null) {
            weatherDebounceHandler.removeCallbacks(weatherDebounceRunnable);
        }

        // Only fetch if enough time has passed since last fetch
        if (lastWeatherFetchTime > 0 && timeSinceLastFetch < WEATHER_DEBOUNCE_DELAY) {
            long remainingDelay = WEATHER_DEBOUNCE_DELAY - timeSinceLastFetch;
            android.util.Log.d("MainActivity",
                    "Debouncing weather fetch - too soon since last fetch, delaying by " + remainingDelay + "ms");
            weatherDebounceRunnable = () -> {
                performWeatherFetch();
            };
            weatherDebounceHandler.postDelayed(weatherDebounceRunnable, remainingDelay);
        } else {
            performWeatherFetch();
        }
    }

    /**
     * Actually perform the weather fetch
     */
    private void performWeatherFetch() {
        lastWeatherFetchTime = System.currentTimeMillis();
        android.util.Log.d("MainActivity",
                "Refetching weather data with location: " + lastKnownLatitude + ", " + lastKnownLongitude);
        // Fetch real weather data using stored location
        viewModel.fetchWeather(lastKnownLatitude, lastKnownLongitude, true);
        // Update location name if we have valid coordinates
        if (lastKnownLatitude != 0 && lastKnownLongitude != 0) {
            getCityNameFromLocation(lastKnownLatitude, lastKnownLongitude);
        }
    }

    /**
     * Setup periodic weather refresh to keep data in sync
     */
    /**
     * Initialize Firebase Authentication
     * Signs in anonymously if not already authenticated
     * Note: If anonymous auth fails, app will use device ID fallback (see
     * AuthHelper.getCurrentUserId)
     */
    private void initializeFirebaseAuth() {
        if (!AuthHelper.isAuthenticated()) {
            AuthHelper.signInAnonymously(new AuthHelper.OnAuthCompleteListener() {
                @Override
                public void onSuccess() {
                    android.util.Log.d("MainActivity", "Firebase Auth: Anonymous sign-in successful");
                }

                @Override
                public void onError(String error) {
                    // Log as warning instead of error since we have a fallback mechanism
                    android.util.Log.w("MainActivity", "Firebase Auth: Sign-in failed: " + error +
                            ". App will continue using device ID for user identification.");
                    // App continues normally with device ID fallback - no user action needed
                }
            });
        } else {
            android.util.Log.d("MainActivity", "Firebase Auth: User already authenticated");
        }
    }

    private void setupWeatherRefresh() {
        weatherRefreshHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        weatherRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                refreshWeatherIfOnline();
                // Schedule next refresh
                if (weatherRefreshHandler != null) {
                    weatherRefreshHandler.postDelayed(this, WEATHER_REFRESH_INTERVAL);
                }
            }
        };
    }

    /**
     * Start periodic weather refresh
     */
    private void startWeatherRefresh() {
        if (weatherRefreshHandler != null && weatherRefreshRunnable != null) {
            weatherRefreshHandler.removeCallbacks(weatherRefreshRunnable);
            weatherRefreshHandler.postDelayed(weatherRefreshRunnable, WEATHER_REFRESH_INTERVAL);
        }
    }

    /**
     * Stop periodic weather refresh
     */
    private void stopWeatherRefresh() {
        if (weatherRefreshHandler != null && weatherRefreshRunnable != null) {
            weatherRefreshHandler.removeCallbacks(weatherRefreshRunnable);
        }
    }

    /**
     * Refresh weather data if online (with debouncing)
     */
    private void refreshWeatherIfOnline() {
        if (NetworkUtils.isNetworkAvailable(this) && lastKnownLatitude != 0 && lastKnownLongitude != 0) {
            long currentTime = System.currentTimeMillis();
            // Only refresh if enough time has passed since last fetch
            if (currentTime - lastWeatherFetchTime >= WEATHER_DEBOUNCE_DELAY) {
                android.util.Log.d("MainActivity", "Periodic weather refresh - fetching fresh data");
                performWeatherFetch();
            } else {
                android.util.Log.d("MainActivity", "Skipping periodic refresh - too soon since last fetch");
            }
        }
    }

    private void getCityNameFromLocation(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String cityName = null;

                // Check if we're in Phnom Penh area (coordinates roughly within Phnom Penh)
                // Phnom Penh coordinates: approximately 11.55°N, 104.92°E
                if (latitude >= 11.40 && latitude <= 11.70 &&
                        longitude >= 104.75 && longitude <= 105.10) {
                    // We're in Phnom Penh area, use "Phnom Penh" as city name
                    cityName = "Phnom Penh";
                } else {
                    // Try to get the city name from address
                    // First try locality (city/district)
                    cityName = address.getLocality();

                    // If locality is null or seems like a district, try admin area (province)
                    if (cityName == null || cityName.isEmpty()) {
                        cityName = address.getAdminArea();
                    } else {
                        // Check if locality is a district within Phnom Penh
                        String adminArea = address.getAdminArea();
                        if (adminArea != null && adminArea.contains("Phnom Penh")) {
                            cityName = "Phnom Penh";
                        }
                    }

                    // If still null, try sub-admin area or feature name
                    if (cityName == null || cityName.isEmpty()) {
                        cityName = address.getSubAdminArea();
                    }
                }

                if (cityName != null && !cityName.isEmpty()) {
                    viewModel.setUserLocation(cityName);
                } else {
                    viewModel.setUserLocation("Current Location");
                }
            } else {
                viewModel.setUserLocation("Current Location");
            }
        } catch (IOException e) {
            // If geocoding fails, check if coordinates are in Phnom Penh area
            if (latitude >= 11.40 && latitude <= 11.70 &&
                    longitude >= 104.75 && longitude <= 105.10) {
                viewModel.setUserLocation("Phnom Penh");
            } else {
                viewModel.setUserLocation("Current Location");
            }
        }
    }
}
