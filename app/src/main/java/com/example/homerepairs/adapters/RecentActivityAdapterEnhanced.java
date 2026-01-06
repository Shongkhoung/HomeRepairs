package com.example.homerepairs.adapters;

import android.graphics.Color;
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
import com.example.homerepairs.models.RecentActivity;
import com.google.android.material.chip.Chip;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enhanced Recent Activity Adapter with status chips and colored icons
 * Features: Status badges, action buttons, colored service icons
 */
public class RecentActivityAdapterEnhanced
        extends RecyclerView.Adapter<RecentActivityAdapterEnhanced.ActivityViewHolder> {
    private List<RecentActivity> activities;
    private OnActivityClickListener listener;

    // Map status to color
    private static final Map<String, Integer> STATUS_COLORS = new HashMap<>();

    static {
        STATUS_COLORS.put("Completed", R.color.success);
        STATUS_COLORS.put("Available", R.color.success);
        STATUS_COLORS.put("Cancelled", R.color.error);
        STATUS_COLORS.put("In Progress", R.color.warning);
        STATUS_COLORS.put("Scheduled", R.color.info);
        STATUS_COLORS.put("Upcoming", R.color.success);
        STATUS_COLORS.put("Unavailable", R.color.error);
    }

    // Map action text to background drawable
    private static final Map<String, Integer> ACTION_BACKGROUNDS = new HashMap<>();

    static {
        ACTION_BACKGROUNDS.put("Book Again", R.drawable.button_action_background);
        ACTION_BACKGROUNDS.put("View Details", R.drawable.button_action_info_background);
        ACTION_BACKGROUNDS.put("Cancel", R.drawable.button_action_error_background);
    }

    public interface OnActivityClickListener {
        void onBookAgain(RecentActivity activity);

        void onViewDetails(RecentActivity activity);

        void onCancel(RecentActivity activity);
    }

    public RecentActivityAdapterEnhanced(List<RecentActivity> activities, OnActivityClickListener listener) {
        this.activities = activities;
        this.listener = listener;
    }

    private String searchQuery = "";

    public void updateActivities(List<RecentActivity> newActivities) {
        this.activities = newActivities;
        notifyDataSetChanged();
    }

    /**
     * Update activities with granular notifications to avoid re-binding existing
     * items.
     */
    public void updateActivitiesGranular(List<RecentActivity> newActivities, boolean expanded) {
        if (this.activities == null || newActivities == null) {
            updateActivities(newActivities);
            return;
        }

        int oldSize = this.activities.size();
        int newSize = newActivities.size();
        this.activities = newActivities;

        if (expanded && newSize > oldSize) {
            notifyItemRangeInserted(oldSize, newSize - oldSize);
        } else if (!expanded && newSize < oldSize) {
            notifyItemRangeRemoved(newSize, oldSize - newSize);
        } else {
            notifyDataSetChanged();
        }
    }

    /**
     * Set search query to highlight matching activities
     * 
     * @param query Search query (empty string to show all normally)
     */
    public void setSearchQuery(String query) {
        this.searchQuery = query != null ? query.toLowerCase().trim() : "";
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_activity_enhanced, parent, false);
        return new ActivityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
        RecentActivity activity = activities.get(position);
        holder.bind(activity);
    }

    @Override
    public int getItemCount() {
        return activities != null ? activities.size() : 0;
    }

    class ActivityViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivActivityIcon;
        private TextView tvActivityTitle;
        private Chip chipStatus;
        private TextView tvProviderName;
        private TextView tvActivityDate;
        private View iconContainer;
        private View cardView;

        ActivityViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardActivity);
            ivActivityIcon = itemView.findViewById(R.id.ivActivityIcon);
            iconContainer = itemView.findViewById(R.id.flIconContainer);
            tvActivityTitle = itemView.findViewById(R.id.tvActivityTitle);
            chipStatus = itemView.findViewById(R.id.chipStatus);
            tvProviderName = itemView.findViewById(R.id.tvProviderName);
            tvActivityDate = itemView.findViewById(R.id.tvActivityDate);

            // Make the entire card clickable
            cardView.setOnClickListener(v -> {
                triggerAction();
            });
        }

        private void triggerAction() {
            if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                RecentActivity activity = activities.get(getAdapterPosition());
                String action = activity.getActionText();

                if (action.contains("Book Again")) {
                    listener.onBookAgain(activity);
                } else if (action.contains("View Details")) {
                    listener.onViewDetails(activity);
                } else if (action.contains("Cancel")) {
                    listener.onCancel(activity);
                }
            }
        }

        void bind(RecentActivity activity) {
            // Calculate match status
            boolean isMatch = true;
            if (searchQuery != null && !searchQuery.isEmpty()) {
                String title = activity.getTitle() != null ? activity.getTitle().toLowerCase() : "";
                String details = activity.getDetails() != null ? activity.getDetails().toLowerCase() : "";
                isMatch = title.contains(searchQuery) || details.contains(searchQuery);
            }

            // Apply dimming if not a match - DISABLED as per user request
            if (cardView != null) {
                cardView.setAlpha(1.0f);
            }
            // Always enable click
            if (cardView != null) {
                cardView.setEnabled(true);
            }

            String imageUrl = activity.getProfileImageUrl();
            boolean hasValidImageUrl = imageUrl != null &&
                    !imageUrl.isEmpty() &&
                    !imageUrl.equals("null") &&
                    !imageUrl.trim().isEmpty();

            // Load profile image from Firebase Storage URL if available
            if (hasValidImageUrl) {
                // Load provider profile image from Firebase Storage using Glide
                android.util.Log.d("RecentActivityAdapter",
                        "Loading profile image for " + activity.getTitle() + " from URL: " + imageUrl);

                // Remove any color filters
                ivActivityIcon.clearColorFilter();
                ivActivityIcon.setColorFilter(null);

                // Use center crop for proper circular display
                ivActivityIcon.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);

                // Load image with Glide - circular crop
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.no_profile_image) // Show no_profile_image while loading
                        .error(R.drawable.no_profile_image) // Show no_profile_image on error
                        .circleCrop() // Make it circular
                        .into(ivActivityIcon);
            } else {
                // No profile image - show default no_profile_image
                android.util.Log.d("RecentActivityAdapter",
                        "No profile image URL for " + activity.getTitle() + ", showing default");

                // Clear any color filters
                ivActivityIcon.clearColorFilter();
                ivActivityIcon.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);

                // Load default image with Glide for consistent circular crop
                Glide.with(itemView.getContext())
                        .load(R.drawable.no_profile_image)
                        .circleCrop()
                        .into(ivActivityIcon);
            }

            // Set text - Title is the activity/service name
            tvActivityTitle.setText(activity.getTitle());

            // Provider name - from details field
            tvProviderName.setText(activity.getDetails());

            // Time ago
            tvActivityDate.setText(activity.getTimeAgo());

            // Set status chip
            chipStatus.setText(activity.getStatus());
            Integer statusColorRes = STATUS_COLORS.get(activity.getStatus());
            if (statusColorRes != null) {
                chipStatus.setChipBackgroundColorResource(statusColorRes);
            } else {
                // Default to success green for "Available" status
                chipStatus.setChipBackgroundColorResource(R.color.success);
            }
        }

        private int getCategoryColor(int iconResId) {
            // Map icon resources to category colors
            // This is a simplified mapping - adjust based on your icons
            return Color.parseColor("#10B981"); // Default green
        }
    }
}
