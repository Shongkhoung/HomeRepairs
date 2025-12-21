package com.example.homerepairs;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homerepairs.adapters.FavoritesAdapter;
import com.example.homerepairs.models.FeaturedProvider;
import com.example.homerepairs.models.Provider;
import com.example.homerepairs.services.FirebaseProviderService;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import android.widget.Button;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FavoritesActivity extends AppCompatActivity {

    private RecyclerView rvFavorites;
    private LinearLayout llEmptyState;
    private TextView tvProviderCount;
    private TextView tvEmptyMessage;
    private Button btnBrowseServices;
    private LinearLayout llCategoryFilters;
    private LinearLayout llSelectionBar;
    private TextView tvSelectedCount;
    private Button btnDeleteSelected;
    private MaterialButton btnSelect;
    private boolean isSelectionMode = false;
    private String searchQuery = "";
    
    private FavoritesAdapter favoritesAdapter;
    private List<FeaturedProvider> allProviders;
    private List<FeaturedProvider> filteredProviders;
    private boolean isGridView = true;
    private String currentCategory = "All";
    private FirebaseProviderService firebaseProviderService;
    private Set<String> favoriteProviderIds; // Store favorite provider IDs
    
    private String[] categories = {"All", "Handyman", "Electrical", "HVAC", "Carpentry", "Plumbing", "Painting", "Cleaning"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        // Initialize Firebase service
        firebaseProviderService = new FirebaseProviderService();
        
        // Load favorite provider IDs from SharedPreferences
        loadFavoriteIds();

        initializeViews();
        setupRecyclerView();
        setupCategoryFilters();
        setupSelectionMode();
        setupBottomNavigation();
        loadFavorites();
    }

    private void initializeViews() {
        rvFavorites = findViewById(R.id.rvFavorites);
        llEmptyState = findViewById(R.id.llEmptyState);
        tvProviderCount = findViewById(R.id.tvProviderCount);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);
        btnBrowseServices = findViewById(R.id.btnBrowseServices);
        llCategoryFilters = findViewById(R.id.llCategoryFilters);
        llSelectionBar = findViewById(R.id.llSelectionBar);
        tvSelectedCount = findViewById(R.id.tvSelectedCount);
        btnDeleteSelected = findViewById(R.id.btnDeleteSelected);
        btnSelect = findViewById(R.id.btnSelect);
        
        if (btnSelect == null) {
            android.util.Log.e("FavoritesActivity", "btnSelect not found in layout!");
        }
        if (llSelectionBar == null) {
            android.util.Log.e("FavoritesActivity", "llSelectionBar not found in layout!");
        }
        
        allProviders = new ArrayList<>();
        filteredProviders = new ArrayList<>();
        
        // Search button
        findViewById(R.id.btnSearch).setOnClickListener(v -> {
            showSearchDialog();
        });
        
        // Browse Services button
        btnBrowseServices.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void setupSelectionMode() {
        if (btnSelect == null) {
            android.util.Log.e("FavoritesActivity", "btnSelect is null!");
            return;
        }
        
        btnSelect.setOnClickListener(v -> {
            if (favoritesAdapter == null) {
                android.util.Log.e("FavoritesActivity", "favoritesAdapter is null!");
                return;
            }
            
            isSelectionMode = !isSelectionMode;
            favoritesAdapter.setSelectionMode(isSelectionMode);
            
            if (isSelectionMode) {
                btnSelect.setText("CANCEL");
                if (llSelectionBar != null) {
                    llSelectionBar.setVisibility(View.VISIBLE);
                }
            } else {
                btnSelect.setText("SELECT");
                if (llSelectionBar != null) {
                    llSelectionBar.setVisibility(View.GONE);
                }
                favoritesAdapter.clearSelection();
            }
            updateSelectionCount();
        });
        
        if (favoritesAdapter != null) {
            favoritesAdapter.setSelectionListener(count -> updateSelectionCount());
        }
        
        if (btnDeleteSelected != null) {
            btnDeleteSelected.setOnClickListener(v -> {
                if (favoritesAdapter != null) {
                    List<FeaturedProvider> selected = favoritesAdapter.getSelectedProviders();
                    if (!selected.isEmpty()) {
                        deleteSelectedFavorites(selected);
                    }
                }
            });
        }
    }
    
    private void updateSelectionCount() {
        int count = favoritesAdapter.getSelectedProviderIds().size();
        tvSelectedCount.setText(count + " selected");
    }
    
    private void deleteSelectedFavorites(List<FeaturedProvider> selectedProviders) {
        // Remove from favorites
        for (FeaturedProvider provider : selectedProviders) {
            if (provider.getId() != null) {
                favoriteProviderIds.remove(provider.getId());
            }
        }
        
        // Save updated favorites
        saveFavoriteIds();
        
        // Remove from allProviders and filteredProviders
        Set<String> selectedIds = favoritesAdapter.getSelectedProviderIds();
        allProviders.removeIf(p -> p.getId() != null && selectedIds.contains(p.getId()));
        filteredProviders.removeIf(p -> p.getId() != null && selectedIds.contains(p.getId()));
        
        // Update adapter
        favoritesAdapter.updateProviders(filteredProviders);
        updateEmptyState();
        updateProviderCount();
        
        // Exit selection mode
        isSelectionMode = false;
        favoritesAdapter.setSelectionMode(false);
        btnSelect.setText("SELECT");
        llSelectionBar.setVisibility(View.GONE);
        
        Toast.makeText(this, selectedProviders.size() + " provider(s) removed from favorites", Toast.LENGTH_SHORT).show();
    }

    private void setupRecyclerView() {
        favoritesAdapter = new FavoritesAdapter(filteredProviders, isGridView, new FavoritesAdapter.OnFavoriteClickListener() {
            @Override
            public void onMessageClick(FeaturedProvider provider) {
                // Navigate to ChatActivity with provider information
                Intent intent = new Intent(FavoritesActivity.this, ChatActivity.class);
                if (provider.getName() != null) {
                    intent.putExtra("providerName", provider.getName());
                }
                if (provider.getId() != null) {
                    intent.putExtra("providerId", provider.getId());
                }
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }

            @Override
            public void onBookClick(FeaturedProvider provider) {
                // Navigate to ProviderProfileActivity
                Intent intent = new Intent(FavoritesActivity.this, ProviderProfileActivity.class);
                if (provider.getName() != null) {
                    intent.putExtra("provider_name", provider.getName());
                }
                if (provider.getService() != null) {
                    intent.putExtra("service_category", provider.getService());
                }
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }

            @Override
            public void onProviderClick(FeaturedProvider provider) {
                // Navigate to ProviderProfileActivity
                Intent intent = new Intent(FavoritesActivity.this, ProviderProfileActivity.class);
                if (provider.getName() != null) {
                    intent.putExtra("provider_name", provider.getName());
                }
                if (provider.getService() != null) {
                    intent.putExtra("service_category", provider.getService());
                }
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });
        
        updateLayoutManager();
        rvFavorites.setAdapter(favoritesAdapter);
    }

    private void updateLayoutManager() {
        if (isGridView) {
            GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
            rvFavorites.setLayoutManager(gridLayoutManager);
        } else {
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
            rvFavorites.setLayoutManager(linearLayoutManager);
        }
        favoritesAdapter.setGridView(isGridView);
    }

    private void setupCategoryFilters() {
        llCategoryFilters.removeAllViews();
        
        for (String category : categories) {
            Button button = new Button(this);
            button.setText(category.toUpperCase());
            button.setTextSize(14);
            button.setTypeface(button.getTypeface(), android.graphics.Typeface.BOLD);
            button.setAllCaps(true);
            button.setPadding(32, 18, 32, 18);
            button.setMinHeight(60);
            button.setMinimumHeight(60);
            button.setLetterSpacing(0);
            button.setBackground(null);
            button.setTransformationMethod(null);
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 12, 0);
            button.setLayoutParams(params);
            
            // Set initial state
            if (category.equals("All")) {
                button.setBackground(ContextCompat.getDrawable(this, R.drawable.category_filter_button_selected));
                button.setTextColor(ContextCompat.getColor(this, R.color.text_white));
            } else {
                button.setBackground(ContextCompat.getDrawable(this, R.drawable.category_filter_button_unselected));
                button.setTextColor(ContextCompat.getColor(this, R.color.gray_700));
            }
            
            button.setOnClickListener(v -> {
                // Reset all buttons
                for (int i = 0; i < llCategoryFilters.getChildCount(); i++) {
                    View child = llCategoryFilters.getChildAt(i);
                    if (child instanceof Button) {
                        Button btn = (Button) child;
                        btn.setBackground(ContextCompat.getDrawable(FavoritesActivity.this, R.drawable.category_filter_button_unselected));
                        btn.setTextColor(ContextCompat.getColor(FavoritesActivity.this, R.color.gray_700));
                    }
                }
                
                // Set selected button
                button.setBackground(ContextCompat.getDrawable(this, R.drawable.category_filter_button_selected));
                button.setTextColor(ContextCompat.getColor(this, R.color.text_white));
                
                currentCategory = category;
                filterProviders();
            });
            
            llCategoryFilters.addView(button);
        }
    }


    private void filterProviders() {
        filteredProviders.clear();
        
        // Filter by category
        List<FeaturedProvider> categoryFiltered = new ArrayList<>();
        if (currentCategory.equals("All")) {
            categoryFiltered.addAll(allProviders);
        } else {
            for (FeaturedProvider provider : allProviders) {
                if (provider.getService() != null && provider.getService().equalsIgnoreCase(currentCategory)) {
                    categoryFiltered.add(provider);
                }
            }
        }
        
        // Filter by search query
        if (searchQuery == null || searchQuery.trim().isEmpty()) {
            filteredProviders.addAll(categoryFiltered);
        } else {
            String query = searchQuery.toLowerCase().trim();
            for (FeaturedProvider provider : categoryFiltered) {
                boolean matches = false;
                if (provider.getName() != null && provider.getName().toLowerCase().contains(query)) {
                    matches = true;
                } else if (provider.getService() != null && provider.getService().toLowerCase().contains(query)) {
                    matches = true;
                }
                if (matches) {
                    filteredProviders.add(provider);
                }
            }
        }
        
        if (favoritesAdapter != null) {
            favoritesAdapter.updateProviders(filteredProviders);
        }
        updateEmptyState();
        updateProviderCount();
        
        // Update empty message
        if (filteredProviders.isEmpty()) {
            if (!searchQuery.isEmpty()) {
                tvEmptyMessage.setText("No providers found matching \"" + searchQuery + "\"");
            } else {
                String message = "No " + currentCategory.toLowerCase() + " providers in your favorites";
                tvEmptyMessage.setText(message);
            }
        }
    }
    
    private void showSearchDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Search Favorites");
        
        // Create EditText for search input
        final EditText input = new EditText(this);
        input.setHint("Enter provider name or service...");
        input.setText(searchQuery);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        input.setPadding(50, 20, 50, 20);
        
        builder.setView(input);
        
        builder.setPositiveButton("Search", (dialog, which) -> {
            searchQuery = input.getText().toString();
            filterProviders();
        });
        
        builder.setNegativeButton("Clear", (dialog, which) -> {
            searchQuery = "";
            filterProviders();
        });
        
        builder.setNeutralButton("Cancel", null);
        
        android.app.AlertDialog dialog = builder.create();
        dialog.show();
        
        // Focus and show keyboard
        input.requestFocus();
        input.postDelayed(() -> {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(input, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        }, 100);
    }

    private void loadFavorites() {
        allProviders.clear();
        
        // Load providers from Firebase
        firebaseProviderService.getAllProviders(new FirebaseProviderService.ProviderCallback() {
            @Override
            public void onSuccess(List<Provider> providers) {
                android.util.Log.d("FavoritesActivity", "Loaded " + providers.size() + " providers from Firebase");
                android.util.Log.d("FavoritesActivity", "Favorite IDs count: " + favoriteProviderIds.size());
                
                // Convert Provider objects to FeaturedProvider
                for (Provider provider : providers) {
                    // If favorites list is empty, show all providers (for demo/testing)
                    // Otherwise, only show favorited providers
                    if (favoriteProviderIds.isEmpty() || favoriteProviderIds.contains(provider.getId())) {
                        FeaturedProvider featuredProvider = convertToFeaturedProvider(provider);
                        android.util.Log.d("FavoritesActivity", "Adding provider: " + provider.getName() + 
                            ", ImageURL: " + (provider.getProfileImageUrl() != null ? provider.getProfileImageUrl() : "null"));
                        allProviders.add(featuredProvider);
                    }
                }
                
                // If no providers from Firebase, use sample data as fallback
                if (allProviders.isEmpty()) {
                    android.util.Log.d("FavoritesActivity", "No providers found, loading sample data");
                    loadSampleFavorites();
                }
                
                android.util.Log.d("FavoritesActivity", "Total providers to display: " + allProviders.size());
                filterProviders();
                updateProviderCount();
            }
            
            @Override
            public void onError(String error) {
                android.util.Log.e("FavoritesActivity", "Error loading providers from Firebase: " + error);
                // Fallback to sample data on error
                loadSampleFavorites();
                filterProviders();
                updateProviderCount();
            }
        });
    }
    
    /**
     * Convert Provider (from Firebase) to FeaturedProvider
     */
    private FeaturedProvider convertToFeaturedProvider(Provider provider) {
        // Get service icon resource ID based on service type
        int serviceIconResId = getServiceIconResId(provider.getService());
        
        // Get profile image URL from Firebase
        String profileImageUrl = provider.getProfileImageUrl();
        android.util.Log.d("FavoritesActivity", "Converting provider: " + provider.getName() + 
            ", ProfileImageUrl: " + (profileImageUrl != null ? profileImageUrl : "null"));
        
        // Create FeaturedProvider with Firebase data
        FeaturedProvider featuredProvider = new FeaturedProvider(
            provider.getName(),
            provider.getService(),
            provider.getRating(),
            provider.getReviewCount(),
            provider.getAvailability() != null ? provider.getAvailability() : "Available Now",
            provider.getPrice() != null ? provider.getPrice() : "$0/hr",
            serviceIconResId,
            profileImageUrl, // Firebase Storage URL - pass it explicitly
            provider.isAvailableNow(),
            provider.isVerified(),
            null, // responseTime - can be added if available in Provider model
            null  // jobsCompleted - can be added if available in Provider model
        );
        // Set the provider ID for favorites management
        featuredProvider.setId(provider.getId());
        
        // Verify the URL was set correctly
        android.util.Log.d("FavoritesActivity", "FeaturedProvider created for " + featuredProvider.getName() + 
            ", ProfileImageUrl: " + (featuredProvider.getProfileImageUrl() != null ? featuredProvider.getProfileImageUrl() : "null"));
        
        return featuredProvider;
    }
    
    /**
     * Get service icon resource ID based on service category
     */
    private int getServiceIconResId(String serviceCategory) {
        if (serviceCategory == null) {
            return R.drawable.handyman;
        }
        
        String serviceLower = serviceCategory.toLowerCase();
        
        if (serviceLower.contains("plumb")) {
            return R.drawable.plumbing;
        } else if (serviceLower.contains("electric")) {
            return R.drawable.electrical;
        } else if (serviceLower.contains("hvac") || serviceLower.contains("heating") || serviceLower.contains("cooling")) {
            return R.drawable.hvac;
        } else if (serviceLower.contains("carpent")) {
            return R.drawable.carpentry;
        } else if (serviceLower.contains("paint")) {
            return R.drawable.painting;
        } else if (serviceLower.contains("clean")) {
            return R.drawable.cleaning;
        } else if (serviceLower.contains("roof")) {
            return R.drawable.roofing;
        } else if (serviceLower.contains("landscap")) {
            return R.drawable.landscaping;
        } else if (serviceLower.contains("appliance")) {
            return R.drawable.appliance_repair;
        } else {
            return R.drawable.handyman;
        }
    }
    
    /**
     * Load sample favorites as fallback
     */
    private void loadSampleFavorites() {
        FeaturedProvider provider1 = new FeaturedProvider(
            "Pulu", "Handyman", 4.8, 234, 
            "Available Now", "$55/hr", 
            R.drawable.handyman, null, true
        );
        provider1.setId("sample_pulu");
        allProviders.add(provider1);
        
        FeaturedProvider provider2 = new FeaturedProvider(
            "Minea", "Electrical", 4.9, 189, 
            "Available Now", "$65/hr", 
            R.drawable.electrical, null, true
        );
        provider2.setId("sample_minea");
        allProviders.add(provider2);
        
        FeaturedProvider provider3 = new FeaturedProvider(
            "Expert Plus", "HVAC", 4.7, 145, 
            "Busy", "$70/hr", 
            R.drawable.hvac, null, false
        );
        provider3.setId("sample_expert_plus");
        allProviders.add(provider3);
        
        FeaturedProvider provider4 = new FeaturedProvider(
            "Pasha", "Carpentry", 4.9, 203, 
            "Available Now", "$60/hr", 
            R.drawable.carpentry, null, true
        );
        provider4.setId("sample_pasha");
        allProviders.add(provider4);
    }
    
    /**
     * Load favorite provider IDs from SharedPreferences
     */
    private void loadFavoriteIds() {
        favoriteProviderIds = new HashSet<>();
        android.content.SharedPreferences prefs = getSharedPreferences("favorites", MODE_PRIVATE);
        Set<String> savedIds = prefs.getStringSet("provider_ids", new HashSet<>());
        if (savedIds != null) {
            favoriteProviderIds.addAll(savedIds);
        }
    }
    
    /**
     * Save favorite provider IDs to SharedPreferences
     */
    private void saveFavoriteIds() {
        android.content.SharedPreferences prefs = getSharedPreferences("favorites", MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet("provider_ids", favoriteProviderIds);
        editor.apply();
    }

    private void updateEmptyState() {
        if (filteredProviders.isEmpty()) {
            llEmptyState.setVisibility(View.VISIBLE);
            rvFavorites.setVisibility(View.GONE);
        } else {
            llEmptyState.setVisibility(View.GONE);
            rvFavorites.setVisibility(View.VISIBLE);
        }
    }

    private void updateProviderCount() {
        int count = allProviders.size();
        tvProviderCount.setText(count + " saved providers");
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        if (bottomNavigation != null) {
            bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                
                if (itemId == R.id.nav_home) {
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                    return true;
                } else if (itemId == R.id.nav_bookings) {
                    Intent intent = new Intent(this, BookingsActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                    return true;
                } else if (itemId == R.id.nav_messages) {
                    Intent intent = new Intent(this, MessagesActivity.class);
                    startActivity(intent);
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
        }
    }
}

