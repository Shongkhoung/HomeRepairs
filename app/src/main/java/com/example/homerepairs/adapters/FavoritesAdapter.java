package com.example.homerepairs.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.homerepairs.R;
import com.example.homerepairs.models.FeaturedProvider;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder> {

    private List<FeaturedProvider> providers;
    private OnFavoriteClickListener listener;
    // Removing selection mode as it wasn't requested in the new design

    public interface OnFavoriteClickListener {
        void onMessageClick(FeaturedProvider provider);

        void onBookClick(FeaturedProvider provider);

        void onProviderClick(FeaturedProvider provider);

        // Add call listener click
        void onCallClick(FeaturedProvider provider);

        void onFavoriteToggle(FeaturedProvider provider);
    }

    public FavoritesAdapter(List<FeaturedProvider> providers, boolean isGridView, OnFavoriteClickListener listener) {
        this.providers = providers;
        this.listener = listener;
    }

    public void setGridView(boolean isGridView) {
        // Ignored, always list view now
    }

    public void updateProviders(List<FeaturedProvider> newProviders) {
        this.providers = newProviders;
        notifyDataSetChanged();
    }

    // Removing selection logic methods for cleanup

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_favorite_provider_list, parent,
                false);
        return new FavoriteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        FeaturedProvider provider = providers.get(position);
        Context context = holder.itemView.getContext();

        // 1. Basic Info
        holder.tvProviderName.setText(provider.getName());
        holder.tvProfession.setText(provider.getService());

        // 2. Rating & Jobs
        holder.tvRating.setText(String.format("%.1f", provider.getRating()));
        holder.tvReviewCount.setText("(" + provider.getReviewCount() + ")");

        String jobs = provider.getJobsCompleted();
        if (jobs == null || jobs.isEmpty()) {
            jobs = "10+ jobs"; // Fallback
        } else if (!jobs.toLowerCase().contains("job")) {
            jobs += " jobs";
        }
        holder.tvJobsCompleted.setText(jobs);

        // 3. Avatar Logic
        configureAvatar(holder, provider, context);

        // 4. Status Badge
        configureStatusBadge(holder, provider, context);

        // 5. Last Booked Mock
        // In a real app, this would come from the model.
        holder.tvLastBooked.setText("Last: " + getRandomTimeAgo(position));

        // 6. Specialties Tags
        configureSpecialties(holder, provider, context);

        // 7. Click Listeners
        holder.ivFavorite.setOnClickListener(v -> {
            // Toggle logic usually handled by parent or a specific favorite listener
            // For now just animate
            v.animate().scaleX(0.8f).scaleY(0.8f).setDuration(100).withEndAction(() -> {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100);
            });
            if (listener != null) {
                listener.onFavoriteToggle(provider);
            }
        });

        holder.btnCall.setOnClickListener(v -> {
            if (listener != null)
                listener.onCallClick(provider);
        });

        holder.btnMessage.setOnClickListener(v -> {
            if (listener != null)
                listener.onMessageClick(provider);
        });

        holder.btnBook.setOnClickListener(v -> {
            if (listener != null)
                listener.onBookClick(provider);
        });

        holder.cardProvider.setOnClickListener(v -> {
            if (listener != null)
                listener.onProviderClick(provider);
        });
    }

    private void configureAvatar(FavoriteViewHolder holder, FeaturedProvider provider, Context context) {
        String profileImageUrl = provider.getProfileImageUrl();
        String initialLetter = (provider.getName() != null && !provider.getName().isEmpty())
                ? String.valueOf(provider.getName().charAt(0)).toUpperCase()
                : "?";

        holder.tvInitialLetter.setText(initialLetter);
        holder.tvInitialLetter.setVisibility(View.VISIBLE);
        holder.ivProviderIcon.setImageDrawable(null); // Clear image view

        if (profileImageUrl != null && !profileImageUrl.isEmpty() && !profileImageUrl.equals("null")) {
            // Load URL - Using circleCrop as requested
            Glide.with(context)
                    .load(profileImageUrl)
                    .circleCrop()
                    .into(holder.ivProviderIcon);
            holder.tvInitialLetter.setVisibility(View.GONE);
        } else if (provider.getProfileImageResId() != 0) {
            // Load Resource - Using circleCrop as requested
            Glide.with(context)
                    .load(provider.getProfileImageResId())
                    .circleCrop()
                    .into(holder.ivProviderIcon);
            holder.tvInitialLetter.setVisibility(View.GONE);
        }
        // If neither, existing gradient bg + initials are shown
    }

    private void configureStatusBadge(FavoriteViewHolder holder, FeaturedProvider provider, Context context) {
        boolean available = provider.isAvailableNow();

        int bgRes = available ? R.drawable.bg_status_available : R.drawable.bg_status_busy;
        int textColor = available ? R.color.badge_available_text : R.color.badge_busy_text;
        String text = available ? "Available Now" : "Busy";

        holder.llStatusBadge.setBackgroundResource(bgRes);

        holder.tvStatusText.setText(text);
        holder.tvStatusText.setTextColor(ContextCompat.getColor(context, textColor));
    }

    private void configureSpecialties(FavoriteViewHolder holder, FeaturedProvider provider, Context context) {
        holder.llSpecialties.removeAllViews();
        List<String> specialties = getMockSpecialties(provider.getService());

        for (String specialty : specialties) {
            TextView chip = new TextView(context);
            chip.setText(specialty);
            chip.setTextSize(11);
            chip.setTypeface(null, android.graphics.Typeface.BOLD);
            chip.setTextColor(ContextCompat.getColor(context, R.color.tag_text));
            chip.setBackgroundResource(R.drawable.bg_tag_blue);
            chip.setPadding(30, 12, 30, 12);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 16, 0); // Margin end
            chip.setLayoutParams(params);

            holder.llSpecialties.addView(chip);
        }
    }

    private List<String> getMockSpecialties(String service) {
        List<String> list = new ArrayList<>();
        if (service == null)
            return list;

        String s = service.toLowerCase();
        if (s.contains("plumb")) {
            list.add("Plumbing");
            list.add("Repairs");
            list.add("Drainage");
        } else if (s.contains("electric")) {
            list.add("Wiring");
            list.add("Lighting");
            list.add("Installations");
        } else if (s.contains("handy")) {
            list.add("Assembly");
            list.add("Painting");
            list.add("Repairs");
        } else if (s.contains("hvac")) {
            list.add("AC Repair");
            list.add("Heating");
            list.add("Maintenance");
        } else if (s.contains("roof")) {
            list.add("Roofing");
            list.add("Leak Repair");
            list.add("Insulation");
        } else {
            list.add(service);
            list.add("General");
            list.add("Consultation");
        }
        return list;
    }

    private String getRandomTimeAgo(int position) {
        String[] times = { "2 weeks ago", "1 month ago", "3 days ago", "1 week ago", "Yesterday", "5 days ago" };
        return times[position % times.length];
    }

    @Override
    public int getItemCount() {
        return providers != null ? providers.size() : 0;
    }

    static class FavoriteViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardProvider;
        ImageView ivProviderIcon;
        TextView tvInitialLetter;
        FrameLayout flAvatarContainer;

        TextView tvProviderName;
        TextView tvProfession;
        TextView tvRating;
        TextView tvReviewCount;
        TextView tvJobsCompleted;
        ImageView ivFavorite;

        LinearLayout llStatusBadge;

        TextView tvStatusText;
        TextView tvLastBooked;

        LinearLayout llSpecialties;

        View btnCall;
        View btnMessage;
        View btnBook;

        FavoriteViewHolder(@NonNull View itemView) {
            super(itemView);
            cardProvider = itemView.findViewById(R.id.cardProvider);
            ivProviderIcon = itemView.findViewById(R.id.ivProviderIcon);
            tvInitialLetter = itemView.findViewById(R.id.tvInitialLetter);
            flAvatarContainer = itemView.findViewById(R.id.flAvatarContainer);

            tvProviderName = itemView.findViewById(R.id.tvProviderName);
            tvProfession = itemView.findViewById(R.id.tvProfession);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvReviewCount = itemView.findViewById(R.id.tvReviewCount);
            tvJobsCompleted = itemView.findViewById(R.id.tvJobsCompleted);
            ivFavorite = itemView.findViewById(R.id.ivFavorite);

            llStatusBadge = itemView.findViewById(R.id.llStatusBadge);

            tvStatusText = itemView.findViewById(R.id.tvStatusText);
            tvLastBooked = itemView.findViewById(R.id.tvLastBooked);

            llSpecialties = itemView.findViewById(R.id.llSpecialties);

            btnCall = itemView.findViewById(R.id.btnCall);
            btnMessage = itemView.findViewById(R.id.btnMessage);
            btnBook = itemView.findViewById(R.id.btnBook);
        }
    }
}
