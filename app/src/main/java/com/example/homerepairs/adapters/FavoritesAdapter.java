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
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder> {
    
    private List<FeaturedProvider> providers;
    private boolean isGridView;
    private OnFavoriteClickListener listener;
    private boolean isSelectionMode = false;
    private Set<String> selectedProviderIds = new HashSet<>();
    private OnSelectionChangedListener selectionListener;

    public interface OnFavoriteClickListener {
        void onMessageClick(FeaturedProvider provider);
        void onBookClick(FeaturedProvider provider);
        void onProviderClick(FeaturedProvider provider);
    }
    
    public interface OnSelectionChangedListener {
        void onSelectionChanged(int count);
    }

    public FavoritesAdapter(List<FeaturedProvider> providers, boolean isGridView, OnFavoriteClickListener listener) {
        this.providers = providers;
        this.isGridView = isGridView;
        this.listener = listener;
    }

    public void setGridView(boolean isGridView) {
        if (this.isGridView != isGridView) {
            this.isGridView = isGridView;
            notifyDataSetChanged();
        }
    }

    public void updateProviders(List<FeaturedProvider> newProviders) {
        this.providers = newProviders;
        notifyDataSetChanged();
    }
    
    public void setSelectionMode(boolean enabled) {
        isSelectionMode = enabled;
        if (!enabled) {
            selectedProviderIds.clear();
            if (selectionListener != null) {
                selectionListener.onSelectionChanged(0);
            }
        }
        notifyDataSetChanged();
    }
    
    public boolean isSelectionMode() {
        return isSelectionMode;
    }
    
    public void setSelectionListener(OnSelectionChangedListener listener) {
        this.selectionListener = listener;
    }
    
    public Set<String> getSelectedProviderIds() {
        return new HashSet<>(selectedProviderIds);
    }
    
    public List<FeaturedProvider> getSelectedProviders() {
        List<FeaturedProvider> selected = new ArrayList<>();
        for (FeaturedProvider provider : providers) {
            if (provider.getId() != null && selectedProviderIds.contains(provider.getId())) {
                selected.add(provider);
            }
        }
        return selected;
    }
    
    public void clearSelection() {
        selectedProviderIds.clear();
        notifyDataSetChanged();
        if (selectionListener != null) {
            selectionListener.onSelectionChanged(0);
        }
    }

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = isGridView ? R.layout.item_favorite_provider_grid : R.layout.item_favorite_provider_list;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new FavoriteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        FeaturedProvider provider = providers.get(position);
        
        // Set provider name
        holder.tvProviderName.setText(provider.getName());
        
        // Set profession/service (simplified - just use service name)
        String profession = provider.getService() != null ? provider.getService() : "Service Provider";
        // Remove prefixes like "Master", "Expert", "Professional" for display
        profession = profession.replace("Master ", "").replace("Expert ", "").replace("Professional ", "");
        holder.tvProfession.setText(profession);
        
        // Set rating
        if (provider.getRating() > 0) {
            holder.tvRating.setText(String.format("%.1f", provider.getRating()));
            holder.tvRating.setVisibility(View.VISIBLE);
        } else {
            holder.tvRating.setVisibility(View.GONE);
        }
        
        // Set review count
        if (provider.getReviewCount() > 0) {
            holder.tvReviewCount.setText("(" + provider.getReviewCount() + ")");
            holder.tvReviewCount.setVisibility(View.VISIBLE);
        } else {
            holder.tvReviewCount.setVisibility(View.GONE);
        }
        
        // Set availability with dot
        if (holder.vAvailabilityDot != null) {
            if (provider.isAvailableNow()) {
                holder.vAvailabilityDot.setBackgroundResource(R.drawable.circle_green);
                holder.vAvailabilityDot.setVisibility(View.VISIBLE);
                holder.tvAvailability.setText("Available Now");
                holder.tvAvailability.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.available_green));
            } else {
                holder.vAvailabilityDot.setBackgroundResource(R.drawable.circle_red);
                holder.vAvailabilityDot.setVisibility(View.VISIBLE);
                holder.tvAvailability.setText("Busy");
                holder.tvAvailability.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_secondary));
            }
        } else {
            // Fallback for list view or if dot not available
            if (provider.isAvailableNow()) {
                holder.tvAvailability.setText("Available Now");
                holder.tvAvailability.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_white));
                holder.tvAvailability.setBackgroundResource(R.drawable.availability_badge_background);
            } else {
                holder.tvAvailability.setText("Busy");
                holder.tvAvailability.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_primary));
                holder.tvAvailability.setBackgroundResource(R.drawable.availability_badge_unavailable);
            }
        }
        
        // Set price
        if (provider.getPrice() != null && !provider.getPrice().isEmpty()) {
            holder.tvPrice.setText(provider.getPrice());
            holder.tvPrice.setVisibility(View.VISIBLE);
        } else {
            holder.tvPrice.setVisibility(View.GONE);
        }
        
        // Load profile image - prioritize URL, then use service-specific icon, then default
        String profileImageUrl = provider.getProfileImageUrl();
        android.util.Log.d("FavoritesAdapter", "Loading image for " + provider.getName() + 
            ", URL: " + (profileImageUrl != null ? profileImageUrl : "null"));
        
        // Get the container FrameLayout to manage background visibility
        android.view.ViewGroup container = (android.view.ViewGroup) holder.ivProviderIcon.getParent();
        
        // Get initial letter TextView if it exists (for grid view)
        TextView tvInitialLetter = holder.itemView.findViewById(R.id.tvInitialLetter);
        String initialLetter = provider.getName() != null && !provider.getName().isEmpty() 
            ? String.valueOf(provider.getName().charAt(0)).toUpperCase() 
            : "?";
        
        if (profileImageUrl != null && !profileImageUrl.isEmpty() && !profileImageUrl.equals("null")) {
            // Load from URL - image will fill the entire container
            android.util.Log.d("FavoritesAdapter", "Loading image from URL: " + profileImageUrl);
            
            // Hide gradient background and initial letter when loading real image
            if (container != null) {
                container.setBackground(null);
            }
            if (tvInitialLetter != null) {
                tvInitialLetter.setVisibility(View.GONE);
            }
            
            // Make ImageView background transparent to show the image
            holder.ivProviderIcon.setBackground(null);
            
            try {
                Glide.with(holder.itemView.getContext())
                        .load(profileImageUrl)
                        .placeholder(R.drawable.profile_icon_background_rounded)
                        .error(R.drawable.profile_icon_background_rounded)
                        .centerCrop()
                        .into(holder.ivProviderIcon);
            } catch (Exception e) {
                android.util.Log.e("FavoritesAdapter", "Error loading image: " + e.getMessage(), e);
                // Fallback - show gradient background with initial letter
                showInitialLetterWithBackground(container, tvInitialLetter, initialLetter, holder);
            }
        } else if (provider.getProfileImageResId() != 0) {
            // Use provided resource ID
            android.util.Log.d("FavoritesAdapter", "Using resource ID: " + provider.getProfileImageResId());
            // Show gradient background for resource images
            if (container != null) {
                container.setBackgroundResource(R.drawable.profile_icon_background_rounded);
            }
            if (tvInitialLetter != null) {
                tvInitialLetter.setVisibility(View.GONE);
            }
            Glide.with(holder.itemView.getContext())
                    .load(provider.getProfileImageResId())
                    .centerCrop()
                    .into(holder.ivProviderIcon);
        } else {
            // Show initial letter with gradient background (like React design)
            showInitialLetterWithBackground(container, tvInitialLetter, initialLetter, holder);
        }
        
        // Set verified badge
        if (holder.ivVerifiedBadge != null) {
            holder.ivVerifiedBadge.setVisibility(provider.isVerified() ? View.VISIBLE : View.GONE);
        }
        
        // Set favorite icon (always filled for favorites screen)
        holder.ivFavorite.setImageResource(R.drawable.heart_filled);
        holder.ivFavorite.setColorFilter(0xFFFF4081); // Pink color
        
        // Handle selection mode
        ImageView ivSelectionCheck = holder.itemView.findViewById(R.id.ivSelectionCheck);
        if (ivSelectionCheck != null) {
            if (isSelectionMode) {
                ivSelectionCheck.setVisibility(View.VISIBLE);
                String providerId = provider.getId();
                boolean isSelected = providerId != null && selectedProviderIds.contains(providerId);
                if (isSelected) {
                    ivSelectionCheck.setImageResource(R.drawable.ic_check_circle);
                    ivSelectionCheck.setColorFilter(0xFF2196F3); // Blue
                } else {
                    ivSelectionCheck.setImageResource(R.drawable.ic_check_circle);
                    ivSelectionCheck.setColorFilter(0xFFE0E0E0); // Light gray
                    ivSelectionCheck.setAlpha(0.5f);
                }
            } else {
                ivSelectionCheck.setVisibility(View.GONE);
            }
        }
        
        // Set click listeners
        if (holder.btnMessage != null) {
            holder.btnMessage.setOnClickListener(v -> {
                if (!isSelectionMode && listener != null) {
                    listener.onMessageClick(provider);
                }
            });
        }
        
        if (holder.btnBook != null) {
            holder.btnBook.setOnClickListener(v -> {
                if (!isSelectionMode && listener != null) {
                    listener.onBookClick(provider);
                }
            });
        }
        
        holder.cardProvider.setOnClickListener(v -> {
            if (isSelectionMode) {
                // Toggle selection
                String providerId = provider.getId();
                if (providerId != null) {
                    if (selectedProviderIds.contains(providerId)) {
                        selectedProviderIds.remove(providerId);
                    } else {
                        selectedProviderIds.add(providerId);
                    }
                    notifyItemChanged(position);
                    if (selectionListener != null) {
                        selectionListener.onSelectionChanged(selectedProviderIds.size());
                    }
                }
            } else {
                if (listener != null) {
                    listener.onProviderClick(provider);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return providers != null ? providers.size() : 0;
    }
    
    /**
     * Show initial letter with gradient background
     */
    private void showInitialLetterWithBackground(android.view.ViewGroup container, TextView tvInitialLetter, 
                                                  String initialLetter, FavoriteViewHolder holder) {
        // Show gradient background
        if (container != null) {
            container.setBackgroundResource(R.drawable.profile_icon_background_rounded);
        }
        
        // Show initial letter
        if (tvInitialLetter != null) {
            tvInitialLetter.setText(initialLetter);
            tvInitialLetter.setVisibility(View.VISIBLE);
        }
        
        // Hide or clear the image view
        holder.ivProviderIcon.setImageDrawable(null);
        holder.ivProviderIcon.setBackground(null);
    }
    
    /**
     * Get service icon resource ID based on service category name
     */
    private int getServiceIcon(String serviceCategory) {
        if (serviceCategory == null) {
            return R.drawable.handyman;
        }
        
        String serviceLower = serviceCategory.toLowerCase();
        
        // Handle variations and common names
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
        } else if (serviceLower.contains("handyman") || serviceLower.contains("handy")) {
            return R.drawable.handyman;
        } else {
            // Default to handyman icon
            return R.drawable.handyman;
        }
    }

    static class FavoriteViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardProvider;
        ImageView ivProviderIcon;
        ImageView ivFavorite;
        ImageView ivVerifiedBadge;
        TextView tvProviderName;
        TextView tvProfession;
        TextView tvRating;
        TextView tvReviewCount;
        View vAvailabilityDot;
        TextView tvAvailability;
        TextView tvPrice;
        android.view.View btnMessage;
        android.view.View btnBook;

        FavoriteViewHolder(@NonNull View itemView) {
            super(itemView);
            cardProvider = itemView.findViewById(R.id.cardProvider);
            ivProviderIcon = itemView.findViewById(R.id.ivProviderIcon);
            ivFavorite = itemView.findViewById(R.id.ivFavorite);
            ivVerifiedBadge = itemView.findViewById(R.id.ivVerifiedBadge);
            tvProviderName = itemView.findViewById(R.id.tvProviderName);
            tvProfession = itemView.findViewById(R.id.tvProfession);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvReviewCount = itemView.findViewById(R.id.tvReviewCount);
            vAvailabilityDot = itemView.findViewById(R.id.vAvailabilityDot);
            tvAvailability = itemView.findViewById(R.id.tvAvailability);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            btnMessage = itemView.findViewById(R.id.btnMessage);
            btnBook = itemView.findViewById(R.id.btnBook);
        }
    }
}

