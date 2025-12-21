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

public class MainActivity extends AppCompatActivity {

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
    private static final long WEATHER_REFRESH_INTERVAL = 60000; // 1 minute for real-time sync
    
    // Debounce mechanism to prevent duplicate weather fetches
    private android.os.Handler weatherDebounceHandler;
    private Runnable weatherDebounceRunnable;
    private static final long WEATHER_DEBOUNCE_DELAY = 2000; // 2 seconds debounce
    private long lastWeatherFetchTime = 0;

    // Track network state to prevent excessive weather fetches
    private boolean wasInternetAvailable = false;
    
    // User data
    private FirebaseUserService userService;
    private ListenerRegistration userProfileListener;
    private String currentUserName = null;

    // Views
    private TextView tvWeather;
    private LottieAnimationView ivWeatherIcon;
    private TextView tvLocation;
    private TextView tvGreeting;
    private EditText etSearch;
    private RecyclerView rvCategories;
    private RecyclerView rvFeaturedProviders;
    private RecyclerView rvRecentActivity;
    private ProgressBar progressBar;
    private ImageButton btnNotification;
    private View btnEmergency;
    private View btnScheduleLater;
    private View btnDocument;
    private LottieAnimationView ivEmergencyIcon;
    private ImageView ivBookingLaterIcon;
    private ImageView ivFavoriteIcon;
    private FrameLayout llInternetLoading;
    private de.hdodenhof.circleimageview.CircleImageView ivProfileIcon;
    private TextView tvProfileLetter;
    private View cardProfileIcon;
    private TextView tvInternetMessage;
    private LottieAnimationView ivNoConnection;
    private android.os.Handler offlineDelayHandler;
    private TextView tvViewAllServices;
    private TextView tvViewAllActivity;
    private Runnable offlineDelayRunnable;
    private FrameLayout llScreenLoading;
    private LottieAnimationView ivScreenLoading;
    private long loadingStartTime = 0;
    private static final long MIN_LOADING_DURATION = 1500; // Minimum 1.5 seconds

    // Adapters
    private ServiceCategoryAdapter categoryAdapter;
    private ProviderAdapter providerAdapter;
    private RecentActivityAdapterEnhanced activityAdapter;
    
    // Category display state
    private boolean showingAllCategories = false;
    private List<ServiceCategory> allCategories = new java.util.ArrayList<>();
    
    // Recent Activity display state
    private boolean showingAllActivities = false;
    private List<RecentActivity> allActivities = new java.util.ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check if user is authenticated
        if (!AuthHelper.isAuthenticated()) {
            // Redirect to login screen in sign in mode (user likely needs to sign in, not create account)
            Intent intent = new Intent(this, LoginActivity.class);
            intent.putExtra("mode", "sign_in");
            startActivity(intent);
            finish();
            return;
        }
        
        setContentView(R.layout.activity_main);
        
        // Set status bar color to white to match screen background
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));
            // Set dark status bar icons (dark icons on light background)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(
                    getWindow().getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            );
            }
        }

        // Initialize Firebase Authentication (anonymous sign-in for testing)
        initializeFirebaseAuth();
        
        // Initialize user service for loading user data
        userService = new FirebaseUserService();
        
        initializeViews();
        setupViewModel();
        setupAdapters();
        setupRecyclerViews();
        setupButtons();
        setupBottomNavigation();
        requestLocationPermission();
        setupNetworkCallback();
        
        // Load user data from Firebase
        loadUserData();
        
        // Wait for layout to be ready before hiding loading overlay
        waitForLayoutReady();
        // Initialize debounce handler
        weatherDebounceHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        setupWeatherRefresh();
        
        // Check network status after a short delay to ensure views are ready
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
        tvWeather = findViewById(R.id.tvWeather);
        ivWeatherIcon = findViewById(R.id.ivWeatherIcon);
        tvLocation = findViewById(R.id.tvLocation);
        tvGreeting = findViewById(R.id.tvGreeting);
        etSearch = findViewById(R.id.etSearch);
        rvCategories = findViewById(R.id.rvCategories);
        rvFeaturedProviders = findViewById(R.id.rvFeaturedProviders);
        rvRecentActivity = findViewById(R.id.rvRecentActivity);
        progressBar = findViewById(R.id.progressBar);
        btnNotification = findViewById(R.id.btnNotification);
        btnEmergency = findViewById(R.id.btnEmergency);
        btnScheduleLater = findViewById(R.id.btnScheduleLater);
        btnDocument = findViewById(R.id.btnDocument);
        ivEmergencyIcon = findViewById(R.id.ivEmergencyIcon);
        ivBookingLaterIcon = findViewById(R.id.ivBookingLaterIcon);
        ivFavoriteIcon = findViewById(R.id.ivFavoriteIcon);
        llInternetLoading = findViewById(R.id.llInternetLoading);
        tvInternetMessage = findViewById(R.id.tvInternetMessage);
        ivNoConnection = findViewById(R.id.ivNoConnection);
        tvViewAllServices = findViewById(R.id.tvViewAllServices);
        tvViewAllActivity = findViewById(R.id.tvViewAllActivity);
        llScreenLoading = findViewById(R.id.llScreenLoading);
        ivScreenLoading = findViewById(R.id.ivScreenLoading);
        ivProfileIcon = findViewById(R.id.ivProfileIcon);
        tvProfileLetter = findViewById(R.id.tvProfileLetter);
        cardProfileIcon = findViewById(R.id.cardProfileIcon);
        offlineDelayHandler = new android.os.Handler();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        // Set default values (will be updated with real data)
        updateGreeting();
        updateProfileIcon(); // Initialize profile icon
        tvLocation.setText("Phnom Penh");
        tvWeather.setText("22°C Sunny");
        if (ivWeatherIcon != null) {
            // Use sunny_weather.json for weather icon (sunny.json is for time-based greeting icon)
            ivWeatherIcon.setAnimation(R.raw.sunny_weather);
            ivWeatherIcon.playAnimation();
        }
        
        // Initialize button icons
        if (ivEmergencyIcon != null) {
            // Emergency uses Lottie animation
            ivEmergencyIcon.setAnimation(R.raw.amercency);
            ivEmergencyIcon.playAnimation();
        }
        // Booking Later and Favorite icons are set in XML layout using drawable resources
        
        // Initialize internet loading indicator as hidden
        showInternetLoading(false);
        
    }

    /**
     * Get greeting message based on current time of day
     * @return Greeting string (Good Morning/Afternoon/Evening)
     * Time frames:
     * - Good Morning: 12:00 a.m. to 12:00 p.m. (noon)
     * - Good Afternoon: 12:00 p.m. to 6:00 p.m.
     * - Good Evening: 6:00 p.m. until midnight
     */
    private String getTimeBasedGreeting() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int hourOfDay = calendar.get(java.util.Calendar.HOUR_OF_DAY);
        
        String greeting;
        if (hourOfDay >= 0 && hourOfDay < 12) {
            // 12:00 a.m. to 12:00 p.m. (noon)
            greeting = "Good Morning";
        } else if (hourOfDay >= 12 && hourOfDay < 18) {
            // 12:00 p.m. to 6:00 p.m.
            greeting = "Good Afternoon";
        } else {
            // 6:00 p.m. until midnight (18:00 to 23:59)
            greeting = "Good Evening";
        }
        
        // Use real user name if available, otherwise use fallback
        String userName = currentUserName != null ? currentUserName : AuthHelper.getCurrentUserName(this);
        if (userName == null || userName.isEmpty() || userName.equals("You")) {
            userName = "User"; // Fallback
        }
        
        return greeting + ", " + userName + " 👋";
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
     * @param name User's name (from Firestore or Firebase Auth)
     * @param email User's email (for fallback letter)
     */
    private void updateProfileIcon(String name, String email) {
        if (ivProfileIcon == null || tvProfileLetter == null) {
            return;
        }
        
        // Always show letter, never load images (matching profile screen behavior)
        ivProfileIcon.setVisibility(View.GONE);
        tvProfileLetter.setVisibility(View.VISIBLE);
        
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
        
        tvProfileLetter.setText(firstLetter);
    }
    
    /**
     * Update greeting text with current user name
     */
    private void updateGreeting() {
        if (tvGreeting != null) {
            tvGreeting.setText(getTimeBasedGreeting());
        }
    }


    /**
     * Convert Lottie file name to R.raw resource ID
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
                allCategories = new java.util.ArrayList<>(categories);
                // Show only first 6 categories initially
                List<ServiceCategory> initialCategories;
                if (categories.size() > 6) {
                    initialCategories = new java.util.ArrayList<>(categories.subList(0, 6));
                } else {
                    initialCategories = new java.util.ArrayList<>(categories);
                }
                categoryAdapter.updateCategories(initialCategories);
                showingAllCategories = false;
                if (tvViewAllServices != null) {
                    tvViewAllServices.setText("View all >");
                }
            }
        });

        // Observe featured providers
        viewModel.getFeaturedProviders().observe(this, providers -> {
            if (providers != null && !providers.isEmpty()) {
                providerAdapter.updateProviders(providers);
            }
        });

        // Observe recent activities
        viewModel.getRecentActivities().observe(this, activities -> {
            if (activities != null && !activities.isEmpty()) {
                allActivities = new java.util.ArrayList<>(activities);
                // Show only first 2 activities by default
                List<RecentActivity> displayActivities;
                if (!showingAllActivities && activities.size() > 2) {
                    displayActivities = new java.util.ArrayList<>(activities.subList(0, 2));
                } else {
                    displayActivities = new java.util.ArrayList<>(activities);
                }
                activityAdapter.updateActivities(displayActivities);
                
                // Update "View All" button text
                updateViewAllActivityButton();
            }
        });

        // Observe weather
        viewModel.getWeatherData().observe(this, weather -> {
            if (weather != null) {
                android.util.Log.d("MainActivity", "Weather data updated: " + weather);
                tvWeather.setText(weather);
            }
        });

        // Observe weather icon (Lottie animation)
        viewModel.getWeatherIconFileName().observe(this, fileName -> {
            android.util.Log.d("MainActivity", "Weather icon observer triggered, fileName: " + fileName + ", ivWeatherIcon null: " + (ivWeatherIcon == null));
            if (fileName != null && !fileName.isEmpty() && ivWeatherIcon != null) {
                android.util.Log.d("MainActivity", "Setting weather icon to Lottie file: " + fileName);
                int resId = getLottieResourceId(fileName);
                if (resId != 0) {
                    ivWeatherIcon.setAnimation(resId);
                    ivWeatherIcon.playAnimation();
                    android.util.Log.d("MainActivity", "Weather icon updated successfully");
                } else {
                    android.util.Log.e("MainActivity", "Invalid Lottie file name: " + fileName);
                }
            } else {
                android.util.Log.w("MainActivity", "Cannot update weather icon - fileName: " + fileName + ", ivWeatherIcon: " + ivWeatherIcon);
            }
        });

        // Observe location
        viewModel.getUserLocation().observe(this, location -> {
            if (location != null) {
                tvLocation.setText(location);
            }
        });

        // Observe loading state
        viewModel.getIsLoading().observe(this, isLoading -> {
            if (progressBar != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
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
                        Toast.makeText(this, "Unable to fetch weather data", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        // Load data
        viewModel.loadCategories();
        viewModel.loadFeaturedProviders();
        // Load recent activity with current user ID
        String userId = AuthHelper.getCurrentUserId(this);
        if (userId != null) {
            viewModel.loadRecentActivity(userId);
        } else {
            // This should not happen as we check authentication in onCreate
            android.util.Log.e("MainActivity", "getCurrentUserId returned null - user should be authenticated");
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
                        Toast.makeText(MainActivity.this, "Opening " + category.getName(), Toast.LENGTH_SHORT).show();
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
                }
        );

        // Provider adapter
        providerAdapter = new ProviderAdapter(
                viewModel.getFeaturedProviders().getValue(),
                provider -> {
                    Intent intent = new Intent(MainActivity.this, ProviderProfileActivity.class);
                    intent.putExtra("provider_name", provider.getName());
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                }
        );

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
                            Toast.makeText(MainActivity.this, "Provider not found", Toast.LENGTH_SHORT).show();
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
                }
        );
    }

    private void setupRecyclerViews() {
        // Categories - Grid layout (3 columns for better fit on 6.43-inch screen)
        if (rvCategories != null && categoryAdapter != null) {
            GridLayoutManager categoryLayoutManager = new GridLayoutManager(this, 3);
            rvCategories.setLayoutManager(categoryLayoutManager);
            rvCategories.setAdapter(categoryAdapter);
            rvCategories.setHasFixedSize(true);
            
            // Set up smooth animations for item changes
            DefaultItemAnimator animator = new DefaultItemAnimator();
            animator.setAddDuration(600);
            animator.setRemoveDuration(600);
            animator.setMoveDuration(600);
            animator.setChangeDuration(600);
            rvCategories.setItemAnimator(animator);
            
            android.util.Log.d("MainActivity", "RecyclerView setup complete, adapter item count: " + categoryAdapter.getItemCount());
        } else {
            android.util.Log.e("MainActivity", "RecyclerView or adapter is null!");
        }

        // Featured providers - Horizontal layout
        // Horizontal layout for Featured Pro cards (scrollable cards)
        LinearLayoutManager providerLayoutManager = new LinearLayoutManager(
                this, LinearLayoutManager.HORIZONTAL, false);
        rvFeaturedProviders.setLayoutManager(providerLayoutManager);
        rvFeaturedProviders.setAdapter(providerAdapter);
        
        // Calculate card width to match recent activity card width
        // Recent activity: RecyclerView width = screen width - 40dp (container padding 20dp each side)
        //                 Card width = RecyclerView width - 32dp (16dp margin each side) = screen width - 72dp
        // Featured provider: RecyclerView width = screen width - 40dp (container padding 20dp each side)
        //                    Card width should match recent activity = screen width - 72dp
        //                    Card margins = 16dp + 8dp = 24dp
        rvFeaturedProviders.post(() -> {
            int recyclerViewWidth = rvFeaturedProviders.getWidth();
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
                rvFeaturedProviders.setPadding(0, 0, paddingEnd, 0);
                rvFeaturedProviders.setClipToPadding(false);
            }
        });

        // Recent activity - Vertical layout
        LinearLayoutManager activityLayoutManager = new LinearLayoutManager(this);
        rvRecentActivity.setLayoutManager(activityLayoutManager);
        rvRecentActivity.setAdapter(activityAdapter);
    }

    private void setupButtons() {
        // Profile icon click - navigate to Profile screen
        if (cardProfileIcon != null) {
            cardProfileIcon.setOnClickListener(v -> {
                Intent intent = new Intent(this, UserProfileActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            });
        }
        
        btnNotification.setOnClickListener(v -> {
            Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show();
        });

        btnEmergency.setOnClickListener(v -> navigateToBooking("Emergency"));
        btnScheduleLater.setOnClickListener(v -> navigateToBooking("Schedule Later"));

        btnDocument.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FavoritesActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        // View all services click handler
        if (tvViewAllServices != null) {
            tvViewAllServices.setOnClickListener(v -> {
                toggleCategoriesView();
            });
        }

        // View all activity click handler - toggle expand/collapse
        if (tvViewAllActivity != null) {
            tvViewAllActivity.setOnClickListener(v -> {
                toggleActivitiesView();
            });
        }

        etSearch.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // Could open search activity
                Toast.makeText(this, "Search functionality", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Toggle between showing 6 categories and all categories with smooth animation
     */
    private void toggleCategoriesView() {
        if (allCategories == null || allCategories.isEmpty()) {
            return;
        }
        
        // Disable clicks during animation
        if (tvViewAllServices != null) {
            tvViewAllServices.setEnabled(false);
        }
        
        if (showingAllCategories) {
            // Show only first 6 with smooth animation
            List<ServiceCategory> limitedCategories;
            if (allCategories.size() > 6) {
                limitedCategories = new java.util.ArrayList<>(allCategories.subList(0, 6));
            } else {
                limitedCategories = new java.util.ArrayList<>(allCategories);
            }
            
            categoryAdapter.updateCategories(limitedCategories);
            showingAllCategories = false;
            if (tvViewAllServices != null) {
                tvViewAllServices.setText("View all >");
            }
            
            // Re-enable after animation starts (shorter delay for better UX)
            rvCategories.postDelayed(() -> {
                if (tvViewAllServices != null) {
                    tvViewAllServices.setEnabled(true);
                }
            }, 300);
        } else {
            // Show all categories with smooth animation
            categoryAdapter.updateCategories(new java.util.ArrayList<>(allCategories));
            showingAllCategories = true;
            if (tvViewAllServices != null) {
                tvViewAllServices.setText("Show less >");
            }
            
            // Re-enable after animation starts (shorter delay for better UX)
            rvCategories.postDelayed(() -> {
                if (tvViewAllServices != null) {
                    tvViewAllServices.setEnabled(true);
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
        if (tvViewAllActivity != null) {
            tvViewAllActivity.setEnabled(false);
        }
        
        if (showingAllActivities) {
            // Collapse: Show only first 2 activities
            List<RecentActivity> limitedActivities;
            if (allActivities.size() > 2) {
                limitedActivities = new java.util.ArrayList<>(allActivities.subList(0, 2));
            } else {
                limitedActivities = new java.util.ArrayList<>(allActivities);
            }
            
            activityAdapter.updateActivities(limitedActivities);
            showingAllActivities = false;
            updateViewAllActivityButton();
            
            // Re-enable after update
            rvRecentActivity.postDelayed(() -> {
                if (tvViewAllActivity != null) {
                    tvViewAllActivity.setEnabled(true);
                }
            }, 100);
        } else {
            // Expand: Show all activities
            activityAdapter.updateActivities(new java.util.ArrayList<>(allActivities));
            showingAllActivities = true;
            updateViewAllActivityButton();
            
            // Re-enable after update
            rvRecentActivity.postDelayed(() -> {
                if (tvViewAllActivity != null) {
                    tvViewAllActivity.setEnabled(true);
                }
            }, 100);
        }
    }
    
    /**
     * Update "View All" button text for activities based on current state
     */
    private void updateViewAllActivityButton() {
        if (tvViewAllActivity == null) {
            return;
        }
        
        if (showingAllActivities) {
            tvViewAllActivity.setText("Show less");
            tvViewAllActivity.setVisibility(View.VISIBLE);
        } else {
            // Only show "View All" if there are more than 2 activities
            if (allActivities != null && allActivities.size() > 2) {
                tvViewAllActivity.setText("View All");
                tvViewAllActivity.setVisibility(View.VISIBLE);
            } else {
                // Hide button if 2 or fewer activities
                tvViewAllActivity.setVisibility(View.GONE);
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
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        
        // Remove elevation/shadow to eliminate divider - keep the navigation bar background
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bottomNavigation.setElevation(0f);
        }
        
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                // Already on home
                return true;
            } else if (itemId == R.id.nav_bookings) {
                showScreenLoading(true);
                Intent intent = new Intent(this, BookingsActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
                return true;
            } else if (itemId == R.id.nav_messages) {
                showScreenLoading(true);
                Intent intent = new Intent(this, MessagesActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                showScreenLoading(true);
                Intent intent = new Intent(this, UserProfileActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
                return true;
            }
            return false;
        });
        bottomNavigation.setSelectedItemId(R.id.nav_home);
    }

    /**
     * Show or hide screen transition loading overlay
     * @param show true to show, false to hide
     */
    private void showScreenLoading(boolean show) {
        if (llScreenLoading == null) {
            return;
        }
        
        if (show) {
            llScreenLoading.setVisibility(View.VISIBLE);
            llScreenLoading.setAlpha(0f);
            llScreenLoading.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
            // Start Lottie animation with smooth settings
            if (ivScreenLoading != null) {
                ivScreenLoading.setAnimation(R.raw.loading);
                ivScreenLoading.setSpeed(1.0f);
                ivScreenLoading.setRenderMode(com.airbnb.lottie.RenderMode.HARDWARE);
                ivScreenLoading.enableMergePathsForKitKatAndAbove(true);
                ivScreenLoading.playAnimation();
            }
        } else {
            llScreenLoading.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() -> llScreenLoading.setVisibility(View.GONE))
                    .start();
        }
    }

    /**
     * Wait for layout to be fully rendered before hiding loading overlay
     * Ensures loading shows for minimum duration
     */
    private void waitForLayoutReady() {
        if (llScreenLoading == null) {
            return;
        }
        
        // Record start time
        loadingStartTime = System.currentTimeMillis();
        
        // Show loading overlay if it's not already visible
        if (llScreenLoading.getVisibility() != View.VISIBLE) {
            llScreenLoading.setVisibility(View.VISIBLE);
            llScreenLoading.setAlpha(0f);
            llScreenLoading.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
            // Start Lottie animation with smooth settings
            if (ivScreenLoading != null) {
                ivScreenLoading.setAnimation(R.raw.loading);
                ivScreenLoading.setSpeed(1.0f);
                ivScreenLoading.setRenderMode(com.airbnb.lottie.RenderMode.HARDWARE);
                ivScreenLoading.enableMergePathsForKitKatAndAbove(true);
                ivScreenLoading.playAnimation();
            }
        }
        
        // Get root view
        View rootView = findViewById(android.R.id.content);
        if (rootView == null) {
            // Fallback: hide after minimum duration
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> hideScreenLoading(), MIN_LOADING_DURATION);
            return;
        }
        
        // Wait for layout to be measured and laid out
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // Check if layout is ready (has dimensions)
                if (rootView.getWidth() > 0 && rootView.getHeight() > 0) {
                    // Remove listener to avoid multiple calls
                    rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    
                    // Calculate remaining time to meet minimum duration
                    long elapsedTime = System.currentTimeMillis() - loadingStartTime;
                    long remainingTime = MIN_LOADING_DURATION - elapsedTime;
                    
                    // Wait for minimum duration or additional 200ms, whichever is longer
                    long delayTime = Math.max(remainingTime, 200);
                    rootView.postDelayed(() -> hideScreenLoading(), delayTime);
                }
            }
        });
    }

    /**
     * Hide screen loading overlay with animation
     */
    private void hideScreenLoading() {
        if (llScreenLoading == null || llScreenLoading.getVisibility() != View.VISIBLE) {
            return;
        }
        
        // Stop Lottie animation
        if (ivScreenLoading != null) {
            ivScreenLoading.cancelAnimation();
        }
        
        llScreenLoading.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> llScreenLoading.setVisibility(View.GONE))
                .start();
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            boolean hasNetwork = NetworkUtils.isNetworkAvailable(this);
            showInternetLoading(!hasNetwork);
            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
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
     * @param show true to show, false to hide
     */
    private void showInternetLoading(boolean show) {
        if (llInternetLoading == null) {
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
                    if (llInternetLoading == null) {
                        return;
                    }
                    
                    // Show with smooth fade-in animation after 2 second delay
                    llInternetLoading.setVisibility(View.VISIBLE);
                    llInternetLoading.setAlpha(0f);
                    
                    // Fade in overlay background
                    llInternetLoading.animate()
                            .alpha(1f)
                            .setDuration(400)
                            .setStartDelay(100)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator())
                            .start();
                    
                    // Animate loading content with delay
                    if (tvInternetMessage != null && ivNoConnection != null) {
                        tvInternetMessage.setAlpha(0f);
                        ivNoConnection.setAlpha(0f);
                        ivNoConnection.setScaleX(0.8f);
                        ivNoConnection.setScaleY(0.8f);
                        
                        // Start Lottie animation
                        if (ivNoConnection != null) {
                            ivNoConnection.setAnimation(R.raw.no_connection);
                            ivNoConnection.playAnimation();
                        }
                        
                        // Animate text first
                        tvInternetMessage.animate()
                                .alpha(1f)
                                .setDuration(400)
                                .setStartDelay(300)
                                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                                .start();
                        
                        // Animate Lottie animation with scale effect
                        ivNoConnection.animate()
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
            llInternetLoading.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .setInterpolator(new android.view.animation.AccelerateInterpolator())
                    .withEndAction(() -> {
                        if (llInternetLoading != null) {
                            llInternetLoading.setVisibility(View.GONE);
                        }
                        if (ivNoConnection != null) {
                            ivNoConnection.pauseAnimation();
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
                        // Fetch real weather data when internet is restored (with delay to avoid duplicates)
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
                        android.util.Log.d("MainActivity", "Network capabilities changed - hasInternet: " + hasInternet);
                        showInternetLoading(!hasInternet);
                        
                        // Only fetch weather if internet just became available (transition from offline to online)
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
        if (connectivityManager != null && networkCallback != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
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
        if (llInternetLoading == null) {
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
            android.util.Log.d("MainActivity", "Debouncing weather fetch - too soon since last fetch, delaying by " + remainingDelay + "ms");
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
        android.util.Log.d("MainActivity", "Refetching weather data with location: " + lastKnownLatitude + ", " + lastKnownLongitude);
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
     * Note: If anonymous auth fails, app will use device ID fallback (see AuthHelper.getCurrentUserId)
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


