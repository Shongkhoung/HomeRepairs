package com.example.homerepairs.adapters;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homerepairs.R;
import com.example.homerepairs.models.ServiceCategory;

import java.util.List;

/**
 * Enhanced Service Category Adapter with solid color backgrounds
 * Features: Solid color cards, white icons/text, scale animations
 */
public class ServiceCategoryAdapter extends RecyclerView.Adapter<ServiceCategoryAdapter.CategoryViewHolder> {
    private List<ServiceCategory> categories;
    private final OnCategoryClickListener listener;
    private String searchQuery = ""; // Current search query for highlighting
    private List<com.example.homerepairs.models.FeaturedProvider> allProviders = new java.util.ArrayList<>(); // All
                                                                                                              // providers
                                                                                                              // for
                                                                                                              // search
                                                                                                              // matching

    // All service category cards use the same bright periwinkle blue color
    // Parse colors once and cache them for performance
    private static final int CATEGORY_COLOR_INT = Color.parseColor("#4D55CC");
    private static final int PROVIDER_COUNT_TEXT_COLOR = Color.parseColor("#E6FFFFFF"); // 90% opacity white

    public interface OnCategoryClickListener {
        void onCategoryClick(ServiceCategory category);
    }

    public ServiceCategoryAdapter(List<ServiceCategory> categories, OnCategoryClickListener listener) {
        this.categories = categories;
        this.listener = listener;
    }

    /**
     * Set all providers for search matching by provider name
     * 
     * @param providers List of all providers
     */
    public void setProviders(List<com.example.homerepairs.models.FeaturedProvider> providers) {
        this.allProviders = providers != null ? new java.util.ArrayList<>(providers) : new java.util.ArrayList<>();
        android.util.Log.d("ServiceCategoryAdapter", "Providers set: " + this.allProviders.size());
    }

    public void updateCategories(List<ServiceCategory> newCategories) {
        this.categories = newCategories;
        android.util.Log.d("ServiceCategoryAdapter",
                "updateCategories (fallback) called with " + (newCategories != null ? newCategories.size() : 0)
                        + " categories");
        notifyDataSetChanged();
    }

    /**
     * Update categories with granular notifications to avoid re-binding existing
     * items.
     * Use this for See More / See Less toggles.
     */
    public void updateCategoriesGranular(List<ServiceCategory> newCategories, boolean expanded) {
        if (this.categories == null || newCategories == null) {
            updateCategories(newCategories);
            return;
        }

        int oldSize = this.categories.size();
        int newSize = newCategories.size();
        this.categories = newCategories;

        if (expanded && newSize > oldSize) {
            // Addition: Items were added at the end
            notifyItemRangeInserted(oldSize, newSize - oldSize);
        } else if (!expanded && newSize < oldSize) {
            // Removal: Items were removed from the end
            notifyItemRangeRemoved(newSize, oldSize - newSize);
        } else {
            // Fallback for search or other complex changes
            notifyDataSetChanged();
        }
    }

    /**
     * Set search query to highlight matching categories
     * 
     * @param query Search query (empty string to show all normally)
     */
    public void setSearchQuery(String query) {
        this.searchQuery = query != null ? query.toLowerCase().trim() : "";
        android.util.Log.d("ServiceCategoryAdapter", "Search query updated to: '" + this.searchQuery + "'");
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_service_category, parent, false);

        // Skip fade-in animation during initial load to improve performance
        // Animation can be added later if needed, but it causes frame drops during
        // initial rendering
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        if (categories == null || position >= categories.size()) {
            android.util.Log.e("ServiceCategoryAdapter", "Invalid position: " + position + ", categories size: "
                    + (categories != null ? categories.size() : 0));
            return;
        }
        ServiceCategory category = categories.get(position);
        holder.bind(category, position);
    }

    @Override
    public int getItemCount() {
        return categories != null ? categories.size() : 0;
    }

    class CategoryViewHolder extends RecyclerView.ViewHolder {
        private CardView cardCategory;
        private ImageView ivCategoryIcon;
        private TextView tvCategoryName;
        private TextView tvProviderCount;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            cardCategory = itemView.findViewById(R.id.cardCategory);
            ivCategoryIcon = itemView.findViewById(R.id.ivCategoryIcon);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            tvProviderCount = itemView.findViewById(R.id.tvProviderCount);
        }

        void bind(ServiceCategory category, int position) {
            if (category == null) {
                android.util.Log.e("ServiceCategoryAdapter", "Category is null in bind method");
                return;
            }

            android.util.Log.d("ServiceCategoryAdapter", "Binding category: '" + category.getName() + "' at position "
                    + position + ", searchQuery: '" + searchQuery + "'");

            // Determine if this category matches the search query
            boolean isMatch = true;
            if (searchQuery != null && !searchQuery.isEmpty()) {
                String categoryNameLower = category.getName().toLowerCase();
                // Check if category name matches
                boolean categoryMatches = categoryNameLower.contains(searchQuery);

                // Check if any provider in this category matches
                boolean providerMatches = false;
                if (!categoryMatches && allProviders != null) {
                    for (com.example.homerepairs.models.FeaturedProvider provider : allProviders) {
                        if (provider != null) {
                            String providerService = provider.getService() != null ? provider.getService().toLowerCase()
                                    : "";
                            String providerName = provider.getName() != null ? provider.getName().toLowerCase() : "";

                            // Check if provider belongs to this category and matches search
                            if (providerService.equals(categoryNameLower)) {
                                if (providerName.contains(searchQuery) || providerService.contains(searchQuery)) {
                                    providerMatches = true;
                                    break;
                                }
                            }
                        }
                    }
                }
                isMatch = categoryMatches || providerMatches;
            }

            // Set solid background color first - all categories use the same bright
            // periwinkle blue
            if (cardCategory != null) {
                cardCategory.setCardBackgroundColor(CATEGORY_COLOR_INT);
                // Dim if not a match - DISABLED as per user request
                cardCategory.setAlpha(1.0f);

                android.util.Log.d("ServiceCategoryAdapter", "Set card background color for " + category.getName()
                        + " to: #4D55CC, Alpha: " + (isMatch ? 1.0f : 0.3f));
            } else {
                android.util.Log.e("ServiceCategoryAdapter", "cardCategory is NULL for " + category.getName());
            }

            // Set text - ensure it's always set and visible
            if (tvCategoryName != null) {
                tvCategoryName.setText(category.getName());
                tvCategoryName.setTextColor(Color.WHITE);
                tvCategoryName.setVisibility(View.VISIBLE);
            }

            if (tvProviderCount != null) {
                tvProviderCount.setText(category.getProviderCount() + " providers");
                tvProviderCount.setTextColor(PROVIDER_COUNT_TEXT_COLOR);
                tvProviderCount.setVisibility(View.VISIBLE);
            }

            // Set icon - Display PNG icons directly first to verify they load, then apply
            // white tint if needed
            int iconResId = category.getIconResId();

            android.util.Log.d("ServiceCategoryAdapter", "Setting icon for " + category.getName() +
                    " - iconResId: " + iconResId + ", ImageView null: " + (ivCategoryIcon == null));

            if (iconResId != 0 && ivCategoryIcon != null) {
                try {
                    // Set the icon resource
                    ivCategoryIcon.setImageResource(iconResId);

                    // Apply white color filter to make icons white on blue background
                    ivCategoryIcon.setColorFilter(Color.WHITE);

                    // Ensure visibility
                    ivCategoryIcon.setVisibility(View.VISIBLE);
                    ivCategoryIcon.setAlpha(1.0f);

                    android.util.Log.d("ServiceCategoryAdapter", "Icon set successfully for " +
                            category.getName() + " with resource ID: " + iconResId + " (white tinted)");

                } catch (android.content.res.Resources.NotFoundException e) {
                    android.util.Log.e("ServiceCategoryAdapter", "Icon resource NOT FOUND for " +
                            category.getName() + " - resource ID: " + iconResId, e);
                    ivCategoryIcon.setVisibility(View.GONE);
                } catch (Exception e) {
                    android.util.Log.e("ServiceCategoryAdapter", "Error setting icon for " +
                            category.getName() + ": " + e.getMessage(), e);
                    ivCategoryIcon.setVisibility(View.GONE);
                }
            } else {
                android.util.Log.w("ServiceCategoryAdapter", "Cannot set icon - iconResId: " + iconResId +
                        ", ImageView null: " + (ivCategoryIcon == null));
                if (ivCategoryIcon != null) {
                    ivCategoryIcon.setVisibility(View.GONE);
                }
            }

            // Set click listener in bind to ensure correct position and data
            if (cardCategory != null) {
                // Clear any previous click listener
                cardCategory.setOnClickListener(null);

                // Set new click listener with current position
                final int currentPosition = position;
                final ServiceCategory currentCategory = category;
                final boolean finalIsMatch = isMatch;

                cardCategory.setOnClickListener(v -> {
                    android.util.Log.d("ServiceCategoryAdapter", "=== CLICK DETECTED ===");
                    android.util.Log.d("ServiceCategoryAdapter", "Category: '" + currentCategory.getName() + "'");

                    // Always navigate since we are not hiding/dimming anymore
                    if (listener != null) {
                        android.util.Log.d("ServiceCategoryAdapter",
                                "✓✓✓ NAVIGATING to category: " + currentCategory.getName());
                        animateClick(cardCategory);
                        listener.onCategoryClick(currentCategory);
                    }
                });

                android.util.Log.d("ServiceCategoryAdapter", "Click listener set for category: " + category.getName());

                // Ensure card is clickable
                cardCategory.setClickable(true);
            }
        }

        /**
         * Smooth scale animation on click: scale to 0.96 for 80ms, then back to 1.0
         * with overshoot
         */
        private void animateClick(View view) {
            ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 1.0f, 0.96f);
            ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 1.0f, 0.96f);

            scaleDownX.setDuration(80);
            scaleDownY.setDuration(80);
            scaleDownX.setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f));
            scaleDownY.setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f));

            scaleDownX.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(view, "scaleX", 0.96f, 1.0f);
                    ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 0.96f, 1.0f);

                    scaleUpX.setDuration(120);
                    scaleUpY.setDuration(120);
                    scaleUpX.setInterpolator(new OvershootInterpolator(1.1f));
                    scaleUpY.setInterpolator(new OvershootInterpolator(1.1f));

                    scaleUpX.start();
                    scaleUpY.start();
                }
            });

            scaleDownX.start();
            scaleDownY.start();
        }
    }
}
