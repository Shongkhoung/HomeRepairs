package com.example.homerepairs.adapters;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.homerepairs.R;
import com.example.homerepairs.models.Provider;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class ProviderListAdapter extends RecyclerView.Adapter<ProviderListAdapter.ProviderViewHolder> {
    private final java.util.Set<Integer> animatedPositions = new java.util.HashSet<>();
    private List<Provider> providers;
    private OnProviderClickListener listener;

    public interface OnProviderClickListener {
        void onProviderClick(Provider provider);

        void onBookClick(Provider provider);

        void onFavoriteClick(Provider provider);
    }

    public ProviderListAdapter(List<Provider> providers, OnProviderClickListener listener) {
        this.providers = providers;
        this.listener = listener;
    }

    public void updateProviders(List<Provider> newProviders) {
        this.providers = newProviders;
        animatedPositions.clear(); // Clear tracking on data refresh
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
        private TextView tvInitial;

        private TextView tvProviderName;
        private TextView tvRating;
        private TextView tvReviewCount;
        private ImageButton btnFavorite;
        private TextView tvServiceTag;
        private TextView tvStatusTag;
        private TextView tvPrice;
        private View btnBook; // AppCompactButton is a View

        ProviderViewHolder(@NonNull View itemView) {
            super(itemView);
            cardProvider = itemView.findViewById(R.id.cardProvider);
            ivProviderPhoto = itemView.findViewById(R.id.ivProviderPhoto);
            tvInitial = itemView.findViewById(R.id.tvInitial);

            tvProviderName = itemView.findViewById(R.id.tvProviderName);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvReviewCount = itemView.findViewById(R.id.tvReviewCount);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
            tvServiceTag = itemView.findViewById(R.id.tvServiceTag);
            tvStatusTag = itemView.findViewById(R.id.tvStatusTag);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            btnBook = itemView.findViewById(R.id.btnBook);
        }

        void bind(Provider provider, int position) {
            if (provider == null)
                return;

            // 1. Initial / Image Logic
            String name = provider.getName();
            String initial = (name != null && !name.isEmpty()) ? String.valueOf(name.charAt(0)).toUpperCase() : "?";

            String imageUrl = provider.getProfileImageUrl();
            boolean hasImage = (imageUrl != null && !imageUrl.isEmpty() && !imageUrl.equals("null")) ||
                    (provider.getProfileImageResId() != 0
                            && provider.getProfileImageResId() != R.drawable.no_profile_image);

            if (hasImage) {
                tvInitial.setVisibility(View.GONE);
                if (imageUrl != null && !imageUrl.isEmpty() && !imageUrl.equals("null")) {
                    Glide.with(itemView.getContext())
                            .load(imageUrl)
                            .centerCrop()
                            .placeholder(R.drawable.no_profile_image)
                            .error(R.drawable.no_profile_image)
                            .override(200, 200)
                            .transition(com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
                                    .withCrossFade(300))
                            .circleCrop()
                            .into(ivProviderPhoto);
                } else {
                    Glide.with(itemView.getContext())
                            .load(provider.getProfileImageResId())
                            .centerCrop()
                            .override(200, 200)
                            .circleCrop()
                            .into(ivProviderPhoto);
                }
            } else {
                tvInitial.setText(initial);
                tvInitial.setVisibility(View.VISIBLE);
                ivProviderPhoto.setImageDrawable(null);
            }

            // 3. Name & Rating
            tvProviderName.setText(name);
            tvRating.setText(String.format("%.1f", provider.getRating()));
            tvReviewCount.setText("(" + provider.getReviewCount() + ")");

            // 4. Favorite Icon
            if (provider.isFavorite()) {
                btnFavorite.setImageResource(R.drawable.heart_filled);
                btnFavorite.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.favorite_pink)); // Assuming
                                                                                                                  // color
                                                                                                                  // exists,
                                                                                                                  // or
                                                                                                                  // use
                                                                                                                  // #EF4444
                                                                                                                  // (red-500)
                // If favorite_pink doesn't exist, use red
                // btnFavorite.setColorFilter(0xFFEF4444);
            } else {
                btnFavorite.setImageResource(R.drawable.heart_outline);
                btnFavorite.setColorFilter(0xFF9CA3AF); // gray-400
            }

            // 5. Tags
            tvServiceTag.setText(provider.getService());
            // Status
            if (provider.isAvailableNow()) {
                tvStatusTag.setText("Available"); // "Available" based on design, model says "Available Now"
                tvStatusTag.setBackgroundTintList(ColorStateList.valueOf(0xFFECFDF5)); // green-50
                tvStatusTag.setTextColor(0xFF047857); // green-700
            } else {
                tvStatusTag.setText("Busy");
                tvStatusTag.setBackgroundTintList(ColorStateList.valueOf(0xFFFFF7ED)); // orange-50
                tvStatusTag.setTextColor(0xFFC2410C); // orange-700
            }

            // 6. Price
            String price = provider.getPrice();
            if (price != null && !price.trim().isEmpty() && !price.startsWith("$")) {
                tvPrice.setText("$" + price);
            } else {
                tvPrice.setText(price);
            }

            // 7. Click Listeners

            // Card Click
            if (cardProvider != null) {
                cardProvider.setOnClickListener(v -> {
                    if (listener != null)
                        listener.onProviderClick(provider);
                });
            }

            // Favorite Click
            btnFavorite.setOnClickListener(v -> {
                // Animate
                animateClick(v);
                if (listener != null)
                    listener.onFavoriteClick(provider);
            });

            // Book Click
            btnBook.setOnClickListener(v -> {
                animateClick(v);
                if (listener != null)
                    listener.onBookClick(provider);
            });

            // Entry Animation
            setAnimation(itemView, position);
        }

        private void setAnimation(View viewToAnimate, int position) {
            if (position != RecyclerView.NO_POSITION && !animatedPositions.contains(position)) {
                animatedPositions.add(position);
                viewToAnimate.setTranslationY(100f);
                viewToAnimate.setAlpha(0f);
                viewToAnimate.animate()
                        .translationY(0f)
                        .alpha(1f)
                        .setDuration(400)
                        .setStartDelay(position * 30)
                        .setInterpolator(new android.view.animation.DecelerateInterpolator(2.0f))
                        .start();
            }
        }

        private void animateClick(View view) {
            ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 1.0f, 0.9f);
            ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 1.0f, 0.9f);
            scaleDownX.setDuration(80);
            scaleDownY.setDuration(80);
            scaleDownX.setRepeatCount(1);
            scaleDownX.setRepeatMode(ObjectAnimator.REVERSE);
            scaleDownY.setRepeatCount(1);
            scaleDownY.setRepeatMode(ObjectAnimator.REVERSE);
            scaleDownX.start();
            scaleDownY.start();
        }
    }
}
