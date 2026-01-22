package com.example.homerepairs.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.homerepairs.R;
import com.example.homerepairs.models.FeaturedProvider;
import com.google.android.material.button.MaterialButton;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Enhanced Provider Adapter with new Featured Pro design
 * Features: Profile image with verification badge, response time, availability,
 * jobs completed
 */
public class ProviderAdapter extends RecyclerView.Adapter<ProviderAdapter.ProviderViewHolder> {
    private final java.util.Set<Integer> animatedPositions = new java.util.HashSet<>();
    private List<FeaturedProvider> providers;
    private OnProviderClickListener listener;
    private int cardWidth = -1; // -1 means use default from XML
    private String searchQuery = ""; // Current search query for highlighting
    private static final android.view.animation.DecelerateInterpolator INTERPOLATOR = new android.view.animation.DecelerateInterpolator(
            2.0f);

    public interface OnProviderClickListener {
        void onBookProvider(FeaturedProvider provider);
    }

    public ProviderAdapter(List<FeaturedProvider> providers, OnProviderClickListener listener) {
        this.providers = providers;
        this.listener = listener;
    }

    /**
     * Set the card width programmatically to match recent activity card width
     * 
     * @param width Width in pixels
     */
    public void setCardWidth(int width) {
        this.cardWidth = width;
        notifyDataSetChanged();
    }

    public void updateProviders(List<FeaturedProvider> newProviders) {
        this.providers = newProviders;
        animatedPositions.clear(); // Clear tracking on data refresh
        notifyDataSetChanged();
    }

    /**
     * Set search query to highlight matching providers
     * 
     * @param query Search query (empty string to show all normally)
     */
    public void setSearchQuery(String query) {
        this.searchQuery = query != null ? query.toLowerCase().trim() : "";
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProviderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_provider, parent, false);
        ProviderViewHolder holder = new ProviderViewHolder(view);
        // Set card width if specified
        if (cardWidth > 0 && holder.cardProvider != null) {
            ViewGroup.LayoutParams params = holder.cardProvider.getLayoutParams();
            if (params != null) {
                params.width = cardWidth;
                holder.cardProvider.setLayoutParams(params);
            }
        }
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ProviderViewHolder holder, int position) {
        FeaturedProvider provider = providers.get(position);
        // Ensure card width is set (in case it was set after view holder creation)
        if (cardWidth > 0 && holder.cardProvider != null) {
            ViewGroup.LayoutParams params = holder.cardProvider.getLayoutParams();
            if (params != null && params.width != cardWidth) {
                params.width = cardWidth;
                holder.cardProvider.setLayoutParams(params);
            }
        }
        holder.bind(provider);
    }

    @Override
    public int getItemCount() {
        return providers != null ? providers.size() : 0;
    }

    class ProviderViewHolder extends RecyclerView.ViewHolder {
        private CircleImageView ivProviderPhoto;
        private ImageView ivVerifiedBadge;
        private ImageView ivFavorite;
        private TextView tvProviderName;
        private TextView tvServiceType;
        private TextView tvRating;
        private TextView tvJobsCompleted;
        private TextView tvResponseTime;
        private TextView tvAvailability;
        private TextView tvAvailabilityTime;
        private TextView tvPrice;
        private MaterialButton btnBookNow;
        private View cardProvider;
        private FeaturedProvider currentProvider; // Store current provider for click handling

        ProviderViewHolder(@NonNull View itemView) {
            super(itemView);
            cardProvider = itemView.findViewById(R.id.cardProvider);
            ivProviderPhoto = itemView.findViewById(R.id.ivProviderPhoto);
            ivVerifiedBadge = itemView.findViewById(R.id.ivVerifiedBadge);
            ivFavorite = itemView.findViewById(R.id.ivFavorite);
            tvProviderName = itemView.findViewById(R.id.tvProviderName);
            tvServiceType = itemView.findViewById(R.id.tvServiceType);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvJobsCompleted = itemView.findViewById(R.id.tvJobsCompleted);
            tvResponseTime = itemView.findViewById(R.id.tvResponseTime);
            tvAvailability = itemView.findViewById(R.id.tvAvailability);
            tvAvailabilityTime = itemView.findViewById(R.id.tvAvailabilityTime);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            btnBookNow = itemView.findViewById(R.id.btnBookNow);

            // Make entire card clickable to view provider profile
            if (cardProvider != null) {
                cardProvider.setOnClickListener(v -> {
                    // Re-check match status dynamically to be safe
                    boolean isMatch = true;
                    if (searchQuery != null && !searchQuery.isEmpty()) {
                        String providerName = currentProvider.getName() != null
                                ? currentProvider.getName().toLowerCase()
                                : "";
                        String providerService = currentProvider.getService() != null
                                ? currentProvider.getService().toLowerCase()
                                : "";
                        isMatch = providerName.contains(searchQuery) || providerService.contains(searchQuery);
                    }

                    if (currentProvider != null) {
                        // Always navigate since we are not hiding/dimming anymore
                        if (listener != null) {
                            android.util.Log.d("ProviderAdapter",
                                    "✓ Navigating to provider: " + currentProvider.getName());
                            listener.onBookProvider(currentProvider);
                        }
                    }
                });
            }

            // Book Now button also triggers the same action
            btnBookNow.setOnClickListener(v -> {
                if (currentProvider != null) {
                    // Always navigate
                    if (listener != null) {
                        listener.onBookProvider(currentProvider);
                    }
                }
            });

            // Favorite icon click (optional - can add favorite functionality later)
            ivFavorite.setOnClickListener(v -> {
                // Toggle favorite state (can implement later)
            });
        }

        void bind(FeaturedProvider provider) {
            // Store current provider for click handling
            currentProvider = provider;

            // Animation removed as per user request
            itemView.setTranslationY(0f);
            itemView.setAlpha(1f);

            // Determine if this provider matches the search query
            boolean isMatch = true;
            if (searchQuery != null && !searchQuery.isEmpty()) {
                String providerName = provider.getName() != null ? provider.getName().toLowerCase() : "";
                String providerService = provider.getService() != null ? provider.getService().toLowerCase() : "";

                isMatch = providerName.contains(searchQuery) || providerService.contains(searchQuery);
            }

            // Keep all items at full opacity - DISABLED dimming as per user request
            if (cardProvider != null) {
                cardProvider.setAlpha(1.0f);
            }

            // Load profile image - prefer URL from Firebase, fallback to no_profile_image
            if (provider.getProfileImageUrl() != null && !provider.getProfileImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(provider.getProfileImageUrl())
                        .centerCrop() // Crop image to fill the circle perfectly
                        .placeholder(R.drawable.no_profile_image)
                        .error(R.drawable.no_profile_image)
                        .override(200, 200) // Load at higher resolution for better quality
                        .transition(
                                com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade(300))
                        .into(ivProviderPhoto);
            } else {
                // Use no_profile_image if no URL
                ivProviderPhoto.setImageResource(R.drawable.no_profile_image);
            }
            // Show/hide verification badge
            if (ivVerifiedBadge != null) {
                ivVerifiedBadge.setVisibility(provider.isVerified() ? View.VISIBLE : View.GONE);
            }

            // Set name and service
            tvProviderName.setText(provider.getName());
            tvServiceType.setText(provider.getService());

            // Set rating: "5.0 (142)" format (show one decimal place)
            String ratingText = String.format("%.1f (%d)", provider.getRating(), provider.getReviewCount());
            tvRating.setText(ratingText);

            // Set jobs completed: "328 jobs" format
            String jobsText = provider.getJobsCompleted();
            if (jobsText == null || jobsText.isEmpty()) {
                // Fallback: use review count as jobs if not provided
                jobsText = provider.getReviewCount() + " jobs";
            } else if (!jobsText.contains("jobs") && !jobsText.contains("job")) {
                // If just a number, add "jobs"
                jobsText = jobsText + " jobs";
            }
            tvJobsCompleted.setText(jobsText);

            // Set response time: "~10 min" format
            String responseTime = provider.getResponseTime();
            if (responseTime == null || responseTime.isEmpty()) {
                responseTime = "~10 min"; // Default
            }
            tvResponseTime.setText(responseTime);

            // Set availability
            if (provider.isAvailableNow()) {
                tvAvailability.setText("Available");
                tvAvailabilityTime.setText("Right now");
                tvAvailability.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.available_green));
                tvAvailabilityTime.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.available_green));
            } else {
                tvAvailability.setText("Unavailable");
                tvAvailabilityTime.setText(provider.getAvailability() != null ? provider.getAvailability() : "Later");
                tvAvailability.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.warning));
                tvAvailabilityTime.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.warning));
            }

            // Set price: Extract number from price string (e.g., "$55/hr" -> "$55" or
            // "$10-$30/hr" -> "$10-$30")
            String price = provider.getPrice();
            if (price != null && !price.isEmpty()) {
                // Remove "/hr" or "/hour" if present
                price = price.replace("/hr", "").replace("/hour", "").trim();
                // If it doesn't start with $, add it (but preserve ranges like "10-30")
                if (!price.startsWith("$")) {
                    // Check if it's a range (contains "-")
                    if (price.contains("-")) {
                        // Split and add $ to each part
                        String[] parts = price.split("-");
                        if (parts.length == 2) {
                            price = "$" + parts[0].trim() + "-$" + parts[1].trim();
                        } else {
                            price = "$" + price;
                        }
                    } else {
                        price = "$" + price;
                    }
                }
            } else {
                price = "$55"; // Default
            }
            tvPrice.setText(price);
        }
    }
}
