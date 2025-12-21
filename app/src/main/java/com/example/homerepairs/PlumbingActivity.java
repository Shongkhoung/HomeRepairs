package com.example.homerepairs;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homerepairs.adapters.ProviderListAdapter;
import com.example.homerepairs.models.Provider;
import com.example.homerepairs.services.FirebaseProviderService;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PlumbingActivity extends AppCompatActivity {

    private RecyclerView rvProviders;
    private ProviderListAdapter adapter;
    private EditText etSearch;
    private ImageButton btnFilter;
    private List<Provider> allProviders;
    private String currentFilter = "All"; // Current filter: All, Available Now, Highest Rated, Lowest Price
    private boolean isInitializingBottomNav = true;
    private FirebaseProviderService firebaseProviderService;
    private ProgressBar progressBar;
    private ListenerRegistration providerListener; // Real-time listener registration

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("PlumbingActivity", "=== onCreate START ===");
        setContentView(R.layout.activity_plumbing);
        android.util.Log.d("PlumbingActivity", "setContentView completed");

        initializeViews();
        android.util.Log.d("PlumbingActivity", "Views initialized");
        
        setupRecyclerView();
        android.util.Log.d("PlumbingActivity", "RecyclerView setup completed");
        
        setupButtons();
        android.util.Log.d("PlumbingActivity", "Buttons setup completed");
        
        setupBottomNavigation();
        android.util.Log.d("PlumbingActivity", "Bottom navigation setup completed");
        
        android.util.Log.d("PlumbingActivity", "=== onCreate END - PlumbingActivity setup complete ===");
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        android.util.Log.d("PlumbingActivity", "=== onStart called ===");
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        android.util.Log.d("PlumbingActivity", "=== onResume called ===");
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        android.util.Log.d("PlumbingActivity", "=== onPause called ===");
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        android.util.Log.d("PlumbingActivity", "=== onStop called ===");
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        android.util.Log.d("PlumbingActivity", "=== onDestroy called ===");
        // Remove real-time listener to prevent memory leaks
        if (providerListener != null) {
            providerListener.remove();
            providerListener = null;
            android.util.Log.d("PlumbingActivity", "Real-time listener removed");
        }
    }

    private void initializeViews() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        TextView tvTitle = findViewById(R.id.tvTitle);
        if (tvTitle != null) {
            String categoryName = getIntent().getStringExtra("category_name");
            if (categoryName != null && !categoryName.isEmpty()) {
                tvTitle.setText(categoryName);
            } else {
                tvTitle.setText("Plumbing"); // Default title
            }
        }

        etSearch = findViewById(R.id.etSearch);
        btnFilter = findViewById(R.id.btnFilter);
        rvProviders = findViewById(R.id.rvProviders);
        progressBar = findViewById(R.id.progressBar);
        
        // Initialize Firebase service
        firebaseProviderService = new FirebaseProviderService();
        
        // Check for null views
        if (rvProviders == null) {
            android.util.Log.e("PlumbingActivity", "RecyclerView not found in layout");
        }
    }

    private void setupRecyclerView() {
        if (rvProviders == null) {
            android.util.Log.e("PlumbingActivity", "Cannot setup RecyclerView - view is null");
            return;
        }
        
        // Setup layout manager
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rvProviders.setLayoutManager(layoutManager);
        
        // Ensure RecyclerView doesn't interfere with touch events
        rvProviders.setNestedScrollingEnabled(false);
        rvProviders.setHasFixedSize(false);
        
        // Use smooth item animator with better timing
        // Set add duration to 0 to prevent animation from blocking first click
        androidx.recyclerview.widget.DefaultItemAnimator animator = new androidx.recyclerview.widget.DefaultItemAnimator();
        animator.setAddDuration(0); // No animation on add to ensure immediate clickability
        animator.setRemoveDuration(200);
        animator.setMoveDuration(200);
        animator.setChangeDuration(200);
        rvProviders.setItemAnimator(animator);
        
        // Initialize adapter with empty list
        adapter = new ProviderListAdapter(new ArrayList<>(), provider -> {
            android.util.Log.d("PlumbingActivity", "=== PROVIDER CLICKED ===");
            android.util.Log.d("PlumbingActivity", "Provider name: " + provider.getName());
            android.util.Log.d("PlumbingActivity", "Provider service: " + provider.getService());
            
            try {
                Intent intent = new Intent(PlumbingActivity.this, ProviderProfileActivity.class);
                intent.putExtra("provider_name", provider.getName());
                intent.putExtra("service_category", provider.getService());
                android.util.Log.d("PlumbingActivity", "Starting ProviderProfileActivity...");
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                android.util.Log.d("PlumbingActivity", "ProviderProfileActivity started successfully");
            } catch (Exception e) {
                android.util.Log.e("PlumbingActivity", "Error starting ProviderProfileActivity", e);
                android.widget.Toast.makeText(PlumbingActivity.this, "Error: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
            }
        });
        
        // Set adapter
        rvProviders.setAdapter(adapter);
        
        // Load providers from Firebase
        loadProvidersFromFirebase();
    }
    
    private void loadProvidersFromFirebase() {
        // Remove existing listener if any
        if (providerListener != null) {
            providerListener.remove();
            providerListener = null;
        }
        
        // Show loading indicator
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        if (rvProviders != null) {
            rvProviders.setVisibility(View.GONE);
        }
        
        // Get service category from intent or use default
        String serviceCategory = getIntent().getStringExtra("category_name");
        if (serviceCategory == null || serviceCategory.isEmpty()) {
            serviceCategory = "Plumbing"; // Default
        }
        
        android.util.Log.d("PlumbingActivity", "Setting up real-time listener for category: " + serviceCategory);
        
        // Set up real-time listener that automatically updates when data changes in Firebase
        if (firebaseProviderService != null) {
            providerListener = firebaseProviderService.listenToProviders(serviceCategory, new FirebaseProviderService.ProviderCallback() {
                @Override
                public void onSuccess(List<Provider> providers) {
                    android.util.Log.d("PlumbingActivity", "Real-time update: " + providers.size() + " providers");
                    
                    if (providers.isEmpty()) {
                        // No providers in Firestore - use mock data and show helpful message
                        android.util.Log.w("PlumbingActivity", "No providers found in Firestore. Using mock data.");
                        android.widget.Toast.makeText(PlumbingActivity.this, 
                            "No providers in database. Using sample data. Add providers in Firebase Console.", 
                            android.widget.Toast.LENGTH_LONG).show();
                        allProviders = createMockProviders();
                        updateProviderList(allProviders);
                        return;
                    }
                    
                    // Set default image resource ID for providers without image URL
                    for (Provider provider : providers) {
                        if (provider.getProfileImageResId() == 0) {
                            provider.setProfileImageResId(R.drawable.ic_profile);
                        }
                    }
                    
                    allProviders = providers;
                    updateProviderList(providers);
                }
                
                @Override
                public void onError(String error) {
                    android.util.Log.e("PlumbingActivity", "Firebase real-time listener error: " + error);
                    // Fallback to mock data if Firebase fails
                    android.util.Log.d("PlumbingActivity", "Falling back to mock data");
                    allProviders = createMockProviders();
                    updateProviderList(allProviders);
                    android.widget.Toast.makeText(PlumbingActivity.this, 
                        "Using offline data. Check your internet connection.", 
                        android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Fallback to mock data if Firebase service is not available
            android.util.Log.w("PlumbingActivity", "Firebase service not initialized, using mock data");
            allProviders = createMockProviders();
            updateProviderList(allProviders);
        }
    }
    
    private void updateProviderList(List<Provider> providers) {
        runOnUiThread(() -> {
            // Hide loading indicator
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            if (rvProviders != null) {
                rvProviders.setVisibility(View.VISIBLE);
            }
            
            if (providers == null || providers.isEmpty()) {
                android.util.Log.w("PlumbingActivity", "No providers to display");
                if (adapter != null) {
                    adapter.updateProviders(new ArrayList<>());
                }
                return;
            }
            
            if (adapter != null) {
                adapter.updateProviders(providers);
                android.util.Log.d("PlumbingActivity", "RecyclerView updated with " + adapter.getItemCount() + " items");
            }
            
            // Force layout to ensure items are displayed
            if (rvProviders != null) {
                rvProviders.post(() -> {
                    if (adapter != null && rvProviders != null) {
                        android.util.Log.d("PlumbingActivity", "RecyclerView post - ensuring views are ready");
                        rvProviders.requestLayout();
                    }
                });
            }
        });
    }

    private List<Provider> createMockProviders() {
        List<Provider> providers = new ArrayList<>();
        providers.add(new Provider("Expert Plumber", "Plumbing", 4.8, 123,
                "Available Now", "$60/hr", R.drawable.ic_profile, true, true));
        providers.add(new Provider("Reliable Plumber", "Plumbing", 4.7, 98,
                "Available Now", "$55/hr", R.drawable.ic_profile, true, true));
        providers.add(new Provider("Professional Plumber", "Plumbing", 4.9, 156,
                "Next available: 2pm", "$70/hr", R.drawable.ic_profile, true, false));
        providers.add(new Provider("Skilled Plumber", "Plumbing", 4.6, 87,
                "Available Now", "$50/hr", R.drawable.ic_profile, true, true));
        providers.add(new Provider("Certified Plumber", "Plumbing", 4.8, 134,
                "Available Now", "$65/hr", R.drawable.ic_profile, true, true));
        return providers;
    }

    private void setupButtons() {
        // Setup search functionality
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterProviders(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }

        // Setup filter button
        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> showFilterDialog());
        }
    }

    private void filterProviders(String searchQuery) {
        if (allProviders == null) {
            return;
        }

        List<Provider> filtered = allProviders.stream()
                .filter(provider -> {
                    // Filter by search query
                    boolean matchesSearch = searchQuery == null || searchQuery.isEmpty() ||
                            provider.getName().toLowerCase().contains(searchQuery.toLowerCase()) ||
                            provider.getService().toLowerCase().contains(searchQuery.toLowerCase());

                    // Apply current filter
                    boolean matchesFilter = true;
                    switch (currentFilter) {
                        case "Available Now":
                            matchesFilter = provider.isAvailableNow();
                            break;
                        case "Highest Rated":
                            // Will sort after filtering
                            break;
                        case "Lowest Price":
                            // Will sort after filtering
                            break;
                    }

                    return matchesSearch && matchesFilter;
                })
                .collect(Collectors.toList());

        // Apply sorting if needed
        if ("Highest Rated".equals(currentFilter)) {
            filtered.sort((p1, p2) -> Double.compare(p2.getRating(), p1.getRating()));
        } else if ("Lowest Price".equals(currentFilter)) {
            filtered.sort((p1, p2) -> {
                // Extract numeric value from price string (e.g., "$60/hr" -> 60)
                double price1 = extractPrice(p1.getPrice());
                double price2 = extractPrice(p2.getPrice());
                return Double.compare(price1, price2);
            });
        }

        adapter.updateProviders(filtered);
    }

    private double extractPrice(String priceString) {
        try {
            // Remove "$", "/hr", and any other non-numeric characters except decimal point
            String cleaned = priceString.replaceAll("[^0-9.]", "");
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private void showFilterDialog() {
        String[] filterOptions = {"All", "Available Now", "Highest Rated", "Lowest Price"};
        int selectedIndex = 0;
        for (int i = 0; i < filterOptions.length; i++) {
            if (filterOptions[i].equals(currentFilter)) {
                selectedIndex = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Filter Providers")
                .setSingleChoiceItems(filterOptions, selectedIndex, (dialog, which) -> {
                    currentFilter = filterOptions[which];
                    String searchText = etSearch != null ? etSearch.getText().toString() : "";
                    filterProviders(searchText);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        if (bottomNavigation == null) {
            android.util.Log.w("PlumbingActivity", "BottomNavigationView not found");
            return;
        }
        
        bottomNavigation.setOnItemSelectedListener(item -> {
            // Ignore selections during initialization
            if (isInitializingBottomNav) {
                android.util.Log.d("PlumbingActivity", "Ignoring bottom nav selection during initialization");
                return false;
            }
            
            int itemId = item.getItemId();
            android.util.Log.d("PlumbingActivity", "Bottom nav item selected: " + itemId);
            
            if (itemId == R.id.nav_home) {
                android.util.Log.d("PlumbingActivity", "Navigating to MainActivity from bottom nav");
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish(); // Finish this activity when navigating to home
                return true;
            } else if (itemId == R.id.nav_bookings) {
                Intent intent = new Intent(this, BookingsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
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
        
        // Mark initialization as complete - now listener will work
        isInitializingBottomNav = false;
        android.util.Log.d("PlumbingActivity", "Bottom navigation listener setup complete");
    }
}

