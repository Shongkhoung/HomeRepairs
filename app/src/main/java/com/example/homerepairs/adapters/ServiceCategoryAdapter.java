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
    
    public void updateCategories(List<ServiceCategory> newCategories) {
        this.categories = newCategories;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_service_category, parent, false);
        
        // Skip fade-in animation during initial load to improve performance
        // Animation can be added later if needed, but it causes frame drops during initial rendering
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        if (categories == null || position >= categories.size()) {
            android.util.Log.e("ServiceCategoryAdapter", "Invalid position: " + position + ", categories size: " + (categories != null ? categories.size() : 0));
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
            
            // Set solid background color first - all categories use the same bright periwinkle blue
            if (cardCategory != null) {
                cardCategory.setCardBackgroundColor(CATEGORY_COLOR_INT);
                android.util.Log.d("ServiceCategoryAdapter", "Set card background color for " + category.getName() + " to: #4D55CC");
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
            
            // Set icon - Display PNG icons directly first to verify they load, then apply white tint if needed
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
                
                cardCategory.setOnClickListener(v -> {
                    int adapterPosition = getAdapterPosition();
                    android.util.Log.d("ServiceCategoryAdapter", "Card clicked! ViewHolder position: " + currentPosition + ", Adapter position: " + adapterPosition);
                    
                    if (listener != null && adapterPosition != RecyclerView.NO_POSITION) {
                        // Use the category from bind to avoid position issues
                        ServiceCategory clickedCategory = currentCategory;
                        if (adapterPosition >= 0 && adapterPosition < categories.size()) {
                            clickedCategory = categories.get(adapterPosition);
                        }
                        
                        if (clickedCategory != null) {
                            android.util.Log.d("ServiceCategoryAdapter", "Calling listener for: " + clickedCategory.getName());
                            animateClick(cardCategory);
                            listener.onCategoryClick(clickedCategory);
                        } else {
                            android.util.Log.e("ServiceCategoryAdapter", "Category is null at position: " + adapterPosition);
                        }
                    } else {
                        android.util.Log.e("ServiceCategoryAdapter", "Click conditions not met - listener: " + (listener != null) + 
                            ", position: " + adapterPosition);
                    }
                });
                
                // Ensure card is clickable
                cardCategory.setClickable(true);
                cardCategory.setFocusable(true);
            }
        }
        
        /**
         * Smooth scale animation on click: scale to 0.96 for 80ms, then back to 1.0 with overshoot
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

