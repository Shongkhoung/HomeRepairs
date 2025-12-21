package com.example.homerepairs.adapters;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.homerepairs.R;
import com.example.homerepairs.models.Provider;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class ProviderListAdapter extends RecyclerView.Adapter<ProviderListAdapter.ProviderViewHolder> {
    private List<Provider> providers;
    private OnProviderClickListener listener;

    public interface OnProviderClickListener {
        void onProviderClick(Provider provider);
    }

    public ProviderListAdapter(List<Provider> providers, OnProviderClickListener listener) {
        this.providers = providers;
        this.listener = listener;
    }

    public void updateProviders(List<Provider> newProviders) {
        this.providers = newProviders;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProviderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_provider_list, parent, false);
        return new ProviderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProviderViewHolder holder, int position) {
        if (providers == null || position >= providers.size()) {
            android.util.Log.e("ProviderListAdapter", "Invalid position: " + position + ", providers size: " + (providers != null ? providers.size() : 0));
            return;
        }
        Provider provider = providers.get(position);
        holder.bind(provider, position);
    }

    @Override
    public int getItemCount() {
        return providers != null ? providers.size() : 0;
    }

    class ProviderViewHolder extends RecyclerView.ViewHolder {
        private MaterialCardView cardProvider;
        private ImageView ivProviderPhoto;
        private View llVerified;
        private ImageView ivVerifiedIcon;
        private TextView tvVerified;
        private TextView tvProviderName;
        private TextView tvServiceType;
        private TextView tvRating;
        private TextView tvReviewCount;
        private TextView tvAvailability;
        private TextView tvPrice;
        private View vAvailabilityDot;

        ProviderViewHolder(@NonNull View itemView) {
            super(itemView);
            cardProvider = itemView.findViewById(R.id.cardProvider);
            ivProviderPhoto = itemView.findViewById(R.id.ivProviderPhoto);
            llVerified = itemView.findViewById(R.id.llVerified);
            ivVerifiedIcon = itemView.findViewById(R.id.ivVerifiedIcon);
            tvVerified = itemView.findViewById(R.id.tvVerified);
            tvProviderName = itemView.findViewById(R.id.tvProviderName);
            tvServiceType = itemView.findViewById(R.id.tvServiceType);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvReviewCount = itemView.findViewById(R.id.tvReviewCount);
            tvAvailability = itemView.findViewById(R.id.tvAvailability);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            vAvailabilityDot = itemView.findViewById(R.id.vAvailabilityDot);
        }

        void bind(Provider provider, int position) {
            if (provider == null) {
                android.util.Log.e("ProviderListAdapter", "Provider is null in bind method");
                return;
            }
            
            android.util.Log.d("ProviderListAdapter", "Binding provider at position " + position + ": " + provider.getName());
            
            // CRITICAL: Set up click listener FIRST, before any animation
            // This ensures clicks work immediately, even during animation
            setupClickListener(provider, position);
            
            // CRITICAL: Ensure view is always visible and clickable for first click to work
            itemView.setAlpha(1f);
            itemView.setClickable(true);
            itemView.setEnabled(true);
            itemView.setTranslationY(0f); // Reset translation first
            
            // Add smooth slide-up animation for items (only on first bind)
            // Use a tag to track if this view has been animated before
            Boolean hasAnimated = (Boolean) itemView.getTag();
            if (hasAnimated == null || !hasAnimated) {
                itemView.setTag(true);
                // Start from slightly below, but keep alpha at 1 so clicks work
                itemView.setTranslationY(30f);
                // Animate only translation, keep alpha at 1 to maintain clickability
                // Remove withLayer() as it can interfere with touch events
                itemView.animate()
                        .translationY(0f)
                        .setDuration(400)
                        .setStartDelay(position * 50) // Staggered animation
                        .setInterpolator(new android.view.animation.DecelerateInterpolator(1.2f))
                        .start();
            } else {
                // If already animated, ensure it's in final position
                itemView.setTranslationY(0f);
            }
            
            // Load profile image - prioritize URL from Firebase, fallback to local resource
            if (ivProviderPhoto != null) {
                String imageUrl = provider.getProfileImageUrl();
                if (imageUrl != null && !imageUrl.isEmpty() && !imageUrl.equals("null")) {
                    // Load image from URL using Glide - high quality, fills entire circle
                    android.util.Log.d("ProviderListAdapter", "Loading image from URL for " + provider.getName() + ": " + imageUrl);
                    Glide.with(ivProviderPhoto.getContext())
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_profile)
                            .error(R.drawable.ic_profile)
                            .circleCrop()
                            .into(ivProviderPhoto);
                } else if (provider.getProfileImageResId() != 0) {
                    // Fallback to local resource - also use Glide for consistent circular cropping
                    Glide.with(ivProviderPhoto.getContext())
                            .load(provider.getProfileImageResId())
                            .circleCrop()
                            .into(ivProviderPhoto);
                } else {
                    // Default placeholder - use Glide for consistent circular cropping
                    Glide.with(ivProviderPhoto.getContext())
                            .load(R.drawable.ic_profile)
                            .circleCrop()
                            .into(ivProviderPhoto);
                }
            }
            if (tvProviderName != null) {
                tvProviderName.setText(provider.getName());
            }
            if (tvServiceType != null) {
                tvServiceType.setText(provider.getService());
            }

            // Rating and Reviews
            if (tvRating != null) {
                tvRating.setText(String.format("%.1f", provider.getRating()));
            }
            if (tvReviewCount != null) {
                tvReviewCount.setText("(" + provider.getReviewCount() + " reviews)");
            }

            // Availability
            if (tvAvailability != null) {
                if (provider.isAvailableNow()) {
                    tvAvailability.setText("Available Now");
                    tvAvailability.setTextColor(0xFF10B981); // Green
                    if (vAvailabilityDot != null) {
                        vAvailabilityDot.setBackgroundColor(0xFF10B981);
                    }
                } else {
                    tvAvailability.setText("Unavailable Now");
                    tvAvailability.setTextColor(0xFFF59E0B); // Orange
                    if (vAvailabilityDot != null) {
                        vAvailabilityDot.setBackgroundColor(0xFFF59E0B);
                    }
                }
            }

            // Price
            if (tvPrice != null) {
                tvPrice.setText(provider.getPrice());
            }

            // Verified Badge
            if (llVerified != null) {
                if (provider.isVerified()) {
                    llVerified.setVisibility(View.VISIBLE);
                    if (ivVerifiedIcon != null) {
                        ivVerifiedIcon.setVisibility(View.VISIBLE);
                    }
                    if (tvVerified != null) {
                    tvVerified.setVisibility(View.VISIBLE);
                    }
                } else {
                    llVerified.setVisibility(View.GONE);
                    if (ivVerifiedIcon != null) {
                        ivVerifiedIcon.setVisibility(View.GONE);
                    }
                    if (tvVerified != null) {
                    tvVerified.setVisibility(View.GONE);
                    }
                }
            }

            // Click listener is set up in setupClickListener() method called at start of bind()
        }
        
        /**
         * Set up click listener for the provider card
         * Called at the start of bind() to ensure clicks work immediately
         */
        private void setupClickListener(Provider provider, int position) {
            // Use itemView directly since cardProvider is the root view (same reference)
            View targetView = cardProvider != null ? cardProvider : itemView;
            
            if (targetView != null) {
                // Clear any previous click listener
                targetView.setOnClickListener(null);
                
                // Use the provider from bind directly to avoid position lookup issues
                final Provider currentProvider = provider;
                final int currentPosition = position;
                
                // Ensure view is clickable and can receive touch events immediately
                targetView.setClickable(true);
                targetView.setFocusable(true);
                targetView.setEnabled(true);
                targetView.setAlpha(1f); // Ensure view is visible for clicks
                targetView.setVisibility(View.VISIBLE); // Ensure view is visible
                
                // Recursively ensure ALL child views don't block touch events
                makeChildrenNonClickable(targetView);
                
                // Also set up touch listener for debugging
                targetView.setOnTouchListener((v, event) -> {
                    if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                        android.util.Log.d("ProviderListAdapter", "Touch DOWN detected on card for: " + provider.getName());
                    }
                    // Return false to allow click listener to work
                    return false;
                });
                
                // Set click listener immediately (synchronously)
                targetView.setOnClickListener(v -> {
                    int adapterPosition = getAdapterPosition();
                    android.util.Log.d("ProviderListAdapter", "Card clicked! ViewHolder position: " + currentPosition + ", Adapter position: " + adapterPosition);
                    
                    if (listener != null) {
                        // Use the provider from bind, but verify with adapter position if valid
                        Provider clickedProvider = currentProvider;
                        if (adapterPosition != RecyclerView.NO_POSITION && 
                            adapterPosition >= 0 && adapterPosition < providers.size()) {
                            // Use provider from adapter position if valid
                            clickedProvider = providers.get(adapterPosition);
                        }
                        
                        if (clickedProvider != null) {
                            android.util.Log.d("ProviderListAdapter", "Calling listener for provider: " + clickedProvider.getName());
                            animateClick(targetView);
                            listener.onProviderClick(clickedProvider);
                        } else {
                            android.util.Log.e("ProviderListAdapter", "Provider is null");
                        }
                    } else {
                        android.util.Log.e("ProviderListAdapter", "Listener is null");
                    }
                });
                
                android.util.Log.d("ProviderListAdapter", "Click listener set up for provider: " + provider.getName() + ", view clickable: " + targetView.isClickable());
            } else {
                android.util.Log.e("ProviderListAdapter", "Both cardProvider and itemView are null!");
            }
        }
        
        /**
         * Recursively make all child views non-clickable to ensure parent receives touch events
         */
        private void makeChildrenNonClickable(View view) {
            if (view instanceof android.view.ViewGroup) {
                android.view.ViewGroup group = (android.view.ViewGroup) view;
                for (int i = 0; i < group.getChildCount(); i++) {
                    View child = group.getChildAt(i);
                    child.setClickable(false);
                    child.setFocusable(false);
                    child.setFocusableInTouchMode(false);
                    child.setLongClickable(false);
                    // Recursively process nested children
                    makeChildrenNonClickable(child);
                }
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

