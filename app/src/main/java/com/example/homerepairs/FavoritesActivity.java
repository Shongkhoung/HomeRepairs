package com.example.homerepairs;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.homerepairs.adapters.FavoritesAdapter;
import com.example.homerepairs.databinding.ActivityFavoritesBinding;
import com.example.homerepairs.models.FeaturedProvider;
import com.example.homerepairs.models.Provider;
import com.example.homerepairs.services.FirebaseProviderService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FavoritesActivity extends BaseActivity {

    private ActivityFavoritesBinding binding;
    private FavoritesAdapter favoritesAdapter;
    private List<FeaturedProvider> allProviders = new ArrayList<>();
    private List<FeaturedProvider> filteredProviders = new ArrayList<>();
    private String currentCategory = "ALL";
    private String searchQuery = "";
    private FirebaseProviderService firebaseProviderService;
    private Set<String> favoriteProviderIds;

    private final String[] categories = { "ALL", "HANDYMAN", "ELECTRICAL", "ROOFING", "PLUMBING", "HVAC", "PAINTING",
            "CLEANING" };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFavoritesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseProviderService = new FirebaseProviderService();
        loadFavoriteIds();
        initializeViews();
        setupRecyclerView();
        setupCategoryFilters();
        setupBottomNavigation();
        loadFavorites();
    }

    private void initializeViews() {
        binding.btnSearch.setOnClickListener(v -> showSearchDialog());
    }

    private void setupRecyclerView() {
        favoritesAdapter = new FavoritesAdapter(filteredProviders, false,
                new FavoritesAdapter.OnFavoriteClickListener() {
                    @Override
                    public void onMessageClick(FeaturedProvider provider) {
                        Intent intent = new Intent(FavoritesActivity.this, ChatActivity.class);
                        if (provider.getName() != null)
                            intent.putExtra("providerName", provider.getName());
                        if (provider.getId() != null)
                            intent.putExtra("providerId", provider.getId());
                        startActivity(intent);
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    }

                    @Override
                    public void onBookClick(FeaturedProvider provider) {
                        navigateToProviderProfile(provider);
                    }

                    @Override
                    public void onProviderClick(FeaturedProvider provider) {
                        navigateToProviderProfile(provider);
                    }

                    @Override
                    public void onCallClick(FeaturedProvider provider) {
                        String phone = provider.getPhoneNumber() != null && !provider.getPhoneNumber().isEmpty()
                                ? provider.getPhoneNumber()
                                : "+85512345678";
                        startActivity(new Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:" + phone)));
                    }

                    @Override
                    public void onFavoriteToggle(FeaturedProvider provider) {
                        removeProviderFromFavorites(provider);
                    }
                });

        binding.rvFavorites.setLayoutManager(new LinearLayoutManager(this));
        binding.rvFavorites.setAdapter(favoritesAdapter);
    }

    private void navigateToProviderProfile(FeaturedProvider provider) {
        Intent intent = new Intent(this, ProviderProfileActivity.class);
        if (provider.getName() != null)
            intent.putExtra("provider_name", provider.getName());
        if (provider.getService() != null)
            intent.putExtra("service_category", provider.getService());
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void setupCategoryFilters() {
        binding.llCategoryFilters.removeAllViews();
        for (String category : categories) {
            TextView chip = new TextView(this);
            chip.setText(getCategoryDisplayName(category));
            chip.setTextSize(14);
            chip.setTypeface(null, android.graphics.Typeface.BOLD);
            chip.setPadding(40, 20, 40, 20);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 16, 0);
            chip.setLayoutParams(params);

            if (category.equals(currentCategory)) {
                chip.setBackgroundResource(R.drawable.bg_pill_selected);
                chip.setTextColor(ContextCompat.getColor(this, R.color.text_white));
                chip.setElevation(4f);
            } else {
                chip.setBackgroundResource(R.drawable.bg_pill_unselected);
                chip.setTextColor(ContextCompat.getColor(this, R.color.gray_700));
                chip.setElevation(0f);
            }

            chip.setOnClickListener(v -> {
                currentCategory = category;
                setupCategoryFilters();
                filterProviders();
            });
            binding.llCategoryFilters.addView(chip);
        }
    }

    private String getCategoryDisplayName(String key) {
        switch (key) {
            case "ALL":
                return getString(R.string.category_all);
            case "HANDYMAN":
                return getString(R.string.category_handyman);
            case "ELECTRICAL":
                return getString(R.string.category_electrical);
            case "ROOFING":
                return getString(R.string.category_roofing);
            case "PLUMBING":
                return getString(R.string.category_plumbing);
            case "HVAC":
                return getString(R.string.category_hvac);
            case "PAINTING":
                return getString(R.string.category_painting);
            case "CLEANING":
                return getString(R.string.category_cleaning);
            default:
                return key;
        }
    }

    private void filterProviders() {
        filteredProviders.clear();
        List<FeaturedProvider> catFiltered = new ArrayList<>();
        if (currentCategory.equals("ALL"))
            catFiltered.addAll(allProviders);
        else {
            for (FeaturedProvider p : allProviders) {
                if (p.getService() != null && p.getService().equalsIgnoreCase(currentCategory))
                    catFiltered.add(p);
            }
        }

        if (searchQuery == null || searchQuery.trim().isEmpty())
            filteredProviders.addAll(catFiltered);
        else {
            String q = searchQuery.toLowerCase().trim();
            for (FeaturedProvider p : catFiltered) {
                if ((p.getName() != null && p.getName().toLowerCase().contains(q))
                        || (p.getService() != null && p.getService().toLowerCase().contains(q)))
                    filteredProviders.add(p);
            }
        }

        if (favoritesAdapter != null)
            favoritesAdapter.updateProviders(filteredProviders);
        updateEmptyState();
        updateProviderCount();
    }

    private void showSearchDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.title_search_favorites));
        final EditText input = new EditText(this);
        input.setHint(getString(R.string.hint_search_favorites));
        input.setText(searchQuery);
        input.setPadding(50, 40, 50, 40);
        builder.setView(input);
        builder.setPositiveButton(getString(R.string.btn_search), (d, w) -> {
            searchQuery = input.getText().toString();
            filterProviders();
        });
        builder.setNegativeButton(getString(R.string.btn_clear), (d, w) -> {
            searchQuery = "";
            filterProviders();
        });
        builder.setNeutralButton(getString(R.string.cancel), null);
        builder.show();
    }

    private void loadFavorites() {
        allProviders.clear();
        if (favoriteProviderIds == null || favoriteProviderIds.isEmpty()) {
            filterProviders();
            updateProviderCount();
            return;
        }
        firebaseProviderService.getAllProviders(new FirebaseProviderService.ProviderCallback() {
            @Override
            public void onSuccess(List<Provider> providers) {
                for (Provider p : providers) {
                    if (p.getId() != null && favoriteProviderIds.contains(p.getId()))
                        allProviders.add(convertToFeaturedProvider(p));
                }
                filterProviders();
                updateProviderCount();
            }

            @Override
            public void onError(String e) {
                Toast.makeText(FavoritesActivity.this, "Error: " + e, Toast.LENGTH_SHORT).show();
                filterProviders();
                updateProviderCount();
            }
        });
    }

    private FeaturedProvider convertToFeaturedProvider(Provider p) {
        FeaturedProvider fp = new FeaturedProvider(p.getName(), p.getService(), p.getRating(), p.getReviewCount(),
                p.getAvailability() != null ? p.getAvailability() : getString(R.string.filter_option_available_now),
                p.getPrice() != null ? p.getPrice() : "$0/hr", R.drawable.no_profile_image, p.getProfileImageUrl(),
                p.isAvailableNow(), p.isVerified(), null,
                p.getJobsCompleted() != null ? p.getJobsCompleted() : getString(R.string.default_job_count));
        fp.setId(p.getId());
        return fp;
    }

    private void removeProviderFromFavorites(FeaturedProvider p) {
        if (favoriteProviderIds.contains(p.getId())) {
            favoriteProviderIds.remove(p.getId());
            saveFavoriteIds();
            allProviders.remove(p);
            filterProviders();
            Toast.makeText(this, getString(R.string.msg_removed_from_favorites), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveFavoriteIds() {
        getSharedPreferences("favorites", MODE_PRIVATE).edit().putStringSet("provider_ids", favoriteProviderIds)
                .apply();
    }

    private void loadFavoriteIds() {
        favoriteProviderIds = new HashSet<>(
                getSharedPreferences("favorites", MODE_PRIVATE).getStringSet("provider_ids", new HashSet<>()));
    }

    private void updateEmptyState() {
        binding.llEmptyState.setVisibility(filteredProviders.isEmpty() ? View.VISIBLE : View.GONE);
        binding.rvFavorites.setVisibility(filteredProviders.isEmpty() ? View.GONE : View.VISIBLE);

        if (filteredProviders.isEmpty()) {
            String msg;
            if (!searchQuery.isEmpty()) {
                msg = getString(R.string.msg_no_providers_matching, searchQuery);
            } else {
                msg = getString(R.string.msg_no_category_providers, getCategoryDisplayName(currentCategory));
            }
            binding.tvEmptyMessage.setText(msg);
        }
    }

    private void updateProviderCount() {
        binding.tvProviderCount.setText(getString(R.string.label_saved_providers, filteredProviders.size()));
    }

    private void setupBottomNavigation() {
        binding.bottomNavigation.setSelectedItemId(R.id.nav_profile);
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home)
                navigateTo(MainActivity.class);
            else if (id == R.id.nav_bookings)
                navigateTo(BookingsActivity.class);
            else if (id == R.id.nav_messages)
                navigateTo(MessagesActivity.class);
            else if (id == R.id.nav_profile)
                navigateTo(UserProfileActivity.class);
            return true;
        });
    }

    private void navigateTo(Class<?> cls) {
        Intent intent = new Intent(this, cls);
        if (cls == MainActivity.class || cls == BookingsActivity.class)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
