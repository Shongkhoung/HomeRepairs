package com.example.homerepairs;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.homerepairs.adapters.ProviderListAdapter;
import com.example.homerepairs.databinding.ActivityPlumbingBinding;
import com.example.homerepairs.models.Provider;
import com.example.homerepairs.services.FirebaseProviderService;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PlumbingActivity extends BaseActivity {

    private ActivityPlumbingBinding binding;
    private ProviderListAdapter adapter;
    private List<Provider> allProviders;
    private int currentFilterIndex = 0; // 0: All, 1: Available Now, 2: Highest Rated, 3: Lowest Price
    private boolean isInitializingBottomNav = true;
    private FirebaseProviderService firebaseProviderService;
    private ListenerRegistration providerListener;
    private boolean isPickMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPlumbingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeViews();
        setupRecyclerView();
        setupButtons();
        setupBottomNavigation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (providerListener != null) {
            providerListener.remove();
        }
    }

    private void initializeViews() {
        binding.btnBack.setOnClickListener(v -> finish());

        String categoryName = getIntent().getStringExtra("category_name");
        if (categoryName == null || categoryName.isEmpty()) {
            categoryName = "Plumbing";
        }

        if ("Plumbing".equals(categoryName)) {
            binding.tvTitle.setText(getString(R.string.category_plumbing));
        } else if ("Search Results".equals(categoryName)) {
            binding.tvTitle.setText(getString(R.string.title_search_results));
        } else {
            binding.tvTitle.setText(categoryName);
        }

        isPickMode = getIntent().getBooleanExtra("ACTION_PICK_PROVIDER", false);
        firebaseProviderService = new FirebaseProviderService();
    }

    private void setupRecyclerView() {
        binding.rvProviders.setLayoutManager(new LinearLayoutManager(this));
        binding.rvProviders.setNestedScrollingEnabled(false);

        DefaultItemAnimator animator = new DefaultItemAnimator();
        animator.setAddDuration(0);
        binding.rvProviders.setItemAnimator(animator);

        adapter = new ProviderListAdapter(new ArrayList<>(), new ProviderListAdapter.OnProviderClickListener() {
            @Override
            public void onProviderClick(Provider p) {
                handleProviderClick(p);
            }

            @Override
            public void onBookClick(Provider p) {
                handleProviderClick(p);
            }

            @Override
            public void onFavoriteClick(Provider p) {
                boolean newStatus = !p.isFavorite();
                p.setFavorite(newStatus);
                adapter.notifyDataSetChanged();
                updateFavoriteStatus(p.getId(), newStatus);
            }
        });

        binding.rvProviders.setAdapter(adapter);
        loadProvidersFromFirebase();
    }

    private void updateFavoriteStatus(String providerId, boolean isFavorite) {
        if (providerId == null)
            return;
        Set<String> ids = getSharedPreferences("favorites", MODE_PRIVATE).getStringSet("provider_ids",
                new java.util.HashSet<>());
        Set<String> newIds = new java.util.HashSet<>(ids);
        if (isFavorite)
            newIds.add(providerId);
        else
            newIds.remove(providerId);
        getSharedPreferences("favorites", MODE_PRIVATE).edit().putStringSet("provider_ids", newIds).apply();
    }

    private void syncFavoritesState(List<Provider> providers) {
        Set<String> savedIds = getSharedPreferences("favorites", MODE_PRIVATE).getStringSet("provider_ids",
                new java.util.HashSet<>());
        for (Provider p : providers) {
            p.setFavorite(p.getId() != null && savedIds.contains(p.getId()));
        }
    }

    private void handleProviderClick(Provider provider) {
        if (isPickMode) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("provider_id", provider.getId());
            resultIntent.putExtra("provider_name", provider.getName());
            resultIntent.putExtra("service_category", provider.getService());
            setResult(RESULT_OK, resultIntent);
            finish();
            return;
        }

        Intent intent = new Intent(this, ProviderProfileActivity.class);
        intent.putExtra("provider_name", provider.getName());
        intent.putExtra("service_category", provider.getService());
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void loadProvidersFromFirebase() {
        if (providerListener != null)
            providerListener.remove();
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.rvProviders.setVisibility(View.GONE);

        String serviceCategory = getIntent().getStringExtra("category_name");
        if (serviceCategory == null || serviceCategory.isEmpty())
            serviceCategory = "Plumbing";

        FirebaseProviderService.ProviderCallback callback = new FirebaseProviderService.ProviderCallback() {
            @Override
            public void onSuccess(List<Provider> providers) {
                allProviders = new ArrayList<>(providers);
                syncFavoritesState(allProviders);

                // Ensure at least 12 providers (pad with mock if needed)
                if (allProviders.size() < 12) {
                    List<Provider> mocks = createMockProviders();
                    for (Provider m : mocks) {
                        if (allProviders.stream()
                                .noneMatch(existing -> existing.getName().equalsIgnoreCase(m.getName()))) {
                            allProviders.add(m);
                        }
                        if (allProviders.size() >= 12)
                            break;
                    }
                }

                for (Provider p : allProviders) {
                    if (p.getProfileImageResId() == 0 || p.getProfileImageResId() == R.drawable.profile) {
                        p.setProfileImageResId(R.drawable.no_profile_image);
                    }
                }

                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.rvProviders.setVisibility(View.VISIBLE);
                    adapter.updateProviders(allProviders);

                    String initialQuery = getIntent().getStringExtra("search_query");
                    if (initialQuery != null && !initialQuery.isEmpty()) {
                        binding.etSearch.setText(initialQuery);
                        filterProviders(initialQuery);
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e("PlumbingActivity", "Error: " + error);
                allProviders = createMockProviders();
                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.rvProviders.setVisibility(View.VISIBLE);
                    adapter.updateProviders(allProviders);
                });
            }
        };

        if ("Search Results".equals(serviceCategory) || "All".equals(serviceCategory)) {
            firebaseProviderService.getAllProviders(callback);
        } else {
            providerListener = firebaseProviderService.listenToProviders(serviceCategory, callback);
        }
    }

    private List<Provider> createMockProviders() {
        List<Provider> p = new ArrayList<>();
        p.add(new Provider("Expert Plumber", "Plumbing", 4.8, 123, "Available Now", "$60/hr",
                R.drawable.no_profile_image, true, true));
        p.add(new Provider("Reliable Plumber", "Plumbing", 4.7, 98, "Available Now", "$55/hr",
                R.drawable.no_profile_image, true, true));
        p.add(new Provider("Professional Plumber", "Plumbing", 4.9, 156, "Next available: 2pm", "$70/hr",
                R.drawable.no_profile_image, true, false));
        p.add(new Provider("Skilled Plumber", "Plumbing", 4.6, 87, "Available Now", "$50/hr",
                R.drawable.no_profile_image, true, true));
        p.add(new Provider("Certified Plumber", "Plumbing", 4.8, 134, "Available Now", "$65/hr",
                R.drawable.no_profile_image, true, true));
        p.add(new Provider("Master Plumber", "Plumbing", 4.9, 210, "Available Now", "$80/hr",
                R.drawable.no_profile_image, true, true));
        p.add(new Provider("Express Plumbing", "Plumbing", 4.5, 76, "Available in 1h", "$55/hr",
                R.drawable.no_profile_image, false, true));
        p.add(new Provider("City Plumbers", "Plumbing", 4.4, 45, "Available Now", "$45/hr", R.drawable.no_profile_image,
                true, false));
        p.add(new Provider("Golden Hand Service", "Plumbing", 4.7, 112, "Next available: Tomorrow", "$60/hr",
                R.drawable.no_profile_image, false, true));
        p.add(new Provider("ProFix Plumbing", "Plumbing", 4.6, 90, "Available Now", "$50/hr",
                R.drawable.no_profile_image, true, true));
        p.add(new Provider("Elite Rooter", "Plumbing", 4.8, 180, "Available Now", "$90/hr", R.drawable.no_profile_image,
                true, true));
        p.add(new Provider("A+ Plumbing", "Plumbing", 4.9, 300, "Available Now", "$75/hr", R.drawable.no_profile_image,
                true, true));
        for (int i = 0; i < p.size(); i++)
            p.get(i).setId("mock_" + (i + 1));
        return p;
    }

    private void setupButtons() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
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
        binding.btnFilter.setOnClickListener(v -> showFilterDialog());
    }

    private void filterProviders(String query) {
        if (allProviders == null)
            return;
        List<Provider> filtered = allProviders.stream()
                .filter(p -> {
                    boolean matchesSearch = query == null || query.isEmpty()
                            || p.getName().toLowerCase().contains(query.toLowerCase())
                            || p.getService().toLowerCase().contains(query.toLowerCase());
                    boolean matchesFilter = true;
                    if (currentFilterIndex == 1) // Available Now
                        matchesFilter = p.isAvailableNow();
                    return matchesSearch && matchesFilter;
                })
                .collect(Collectors.toList());

        if (currentFilterIndex == 2) // Highest Rated
            filtered.sort((p1, p2) -> Double.compare(p2.getRating(), p1.getRating()));
        else if (currentFilterIndex == 3) // Lowest Price
            filtered.sort((p1, p2) -> Double.compare(extractPrice(p1.getPrice()), extractPrice(p2.getPrice())));

        adapter.updateProviders(filtered);
        binding.rvProviders.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
        binding.tvNoResults.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private double extractPrice(String price) {
        try {
            return Double.parseDouble(price.replaceAll("[^0-9.]", ""));
        } catch (Exception e) {
            return 0.0;
        }
    }

    private void showFilterDialog() {
        String[] options = {
                getString(R.string.filter_option_all),
                getString(R.string.filter_option_available_now),
                getString(R.string.filter_option_highest_rated),
                getString(R.string.filter_option_lowest_price)
        };

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.title_filter_providers))
                .setSingleChoiceItems(options, currentFilterIndex, (d, w) -> {
                    currentFilterIndex = w;
                    filterProviders(binding.etSearch.getText().toString());
                    d.dismiss();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            if (isInitializingBottomNav)
                return false;
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish();
                return true;
            } else if (id == R.id.nav_bookings) {
                startActivity(new Intent(this, BookingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, UserProfileActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
                return true;
            }
            return false;
        });
        isInitializingBottomNav = false;
    }
}
