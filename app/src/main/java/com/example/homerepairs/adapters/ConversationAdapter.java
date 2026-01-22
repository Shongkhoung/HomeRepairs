package com.example.homerepairs.adapters;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.homerepairs.R;
import com.example.homerepairs.models.Conversation;
import com.example.homerepairs.services.FirebaseProviderService;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder> {
    private List<Conversation> allConversations;
    private List<Conversation> filteredConversations;
    private final OnConversationClickListener listener;
    private final OnConversationActionListener actionListener;
    private boolean isSelectionMode = false;
    private Set<String> selectedConversationIds = new HashSet<>();
    private String currentFilter = "All";

    public interface OnConversationClickListener {
        void onConversationClick(Conversation conversation);
    }

    public interface OnConversationActionListener {
        void onMarkAsRead(List<Conversation> conversations);

        void onMarkAsUnread(List<Conversation> conversations);

        void onPin(Conversation conversation);

        void onUnpin(Conversation conversation);

        void onStar(Conversation conversation);

        void onUnstar(Conversation conversation);

        void onArchive(Conversation conversation);

        void onDelete(List<Conversation> conversations);

        void onSelectionChanged(int count);
    }

    public ConversationAdapter(List<Conversation> conversations,
            OnConversationClickListener listener,
            OnConversationActionListener actionListener) {
        this.allConversations = conversations != null ? new ArrayList<>(conversations) : new ArrayList<>();
        this.filteredConversations = new ArrayList<>(this.allConversations);
        this.listener = listener;
        this.actionListener = actionListener;
    }

    public void updateConversations(List<Conversation> newConversations) {
        this.allConversations = newConversations != null ? new ArrayList<>(newConversations) : new ArrayList<>();
        applyFilter(currentFilter);
    }

    public void setSelectionMode(boolean enabled) {
        isSelectionMode = enabled;
        if (!enabled) {
            selectedConversationIds.clear();
            if (actionListener != null) {
                actionListener.onSelectionChanged(0);
            }
        }
        notifyDataSetChanged();
    }

    public boolean isSelectionMode() {
        return isSelectionMode;
    }

    public int getSelectedCount() {
        return selectedConversationIds.size();
    }

    public List<Conversation> getSelectedConversations() {
        List<Conversation> selected = new ArrayList<>();
        for (Conversation conv : allConversations) {
            if (selectedConversationIds.contains(conv.getId())) {
                selected.add(conv);
            }
        }
        return selected;
    }

    public void selectAll() {
        for (Conversation conv : filteredConversations) {
            selectedConversationIds.add(conv.getId());
        }
        notifyDataSetChanged();
        if (actionListener != null) {
            actionListener.onSelectionChanged(selectedConversationIds.size());
        }
    }

    public void clearSelection() {
        selectedConversationIds.clear();
        notifyDataSetChanged();
        if (actionListener != null) {
            actionListener.onSelectionChanged(0);
        }
    }

    public void filterBy(String filterType) {
        currentFilter = filterType != null ? filterType : "All";
        applyFilter(currentFilter);
    }

    private void applyFilter(String filterType) {
        filteredConversations.clear();

        if (filterType == null || filterType.equals("All")) {
            filteredConversations.addAll(allConversations);
        } else {
            for (Conversation conv : allConversations) {
                switch (filterType) {
                    case "Unread":
                        if (conv.getUnreadCount() > 0) {
                            filteredConversations.add(conv);
                        }
                        break;
                    case "Pinned":
                        if (conv.isPinned()) {
                            filteredConversations.add(conv);
                        }
                        break;
                    case "Starred":
                        if (conv.isStarred()) {
                            filteredConversations.add(conv);
                        }
                        break;
                }
            }
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation, parent, false);
        return new ConversationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        if (filteredConversations == null || position >= filteredConversations.size()) {
            return;
        }
        Conversation conversation = filteredConversations.get(position);
        holder.bind(conversation);
    }

    @Override
    public int getItemCount() {
        return filteredConversations != null ? filteredConversations.size() : 0;
    }

    public List<Conversation> getAllConversations() {
        return allConversations != null ? new ArrayList<>(allConversations) : new ArrayList<>();
    }

    public int getAllCount() {
        return allConversations != null ? allConversations.size() : 0;
    }

    public int getUnreadCount() {
        int count = 0;
        for (Conversation conv : allConversations) {
            if (conv.getUnreadCount() > 0) {
                count++;
            }
        }
        return count;
    }

    public int getPinnedCount() {
        int count = 0;
        for (Conversation conv : allConversations) {
            if (conv.isPinned()) {
                count++;
            }
        }
        return count;
    }

    public int getStarredCount() {
        int count = 0;
        for (Conversation conv : allConversations) {
            if (conv.isStarred()) {
                count++;
            }
        }
        return count;
    }

    class ConversationViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout layoutContent;
        private MaterialCheckBox checkboxSelect;
        private ImageView ivProfileImage;
        private TextView tvInitials;

        private TextView tvProviderName;
        private TextView tvTime;
        private TextView tvLastMessage;
        private TextView tvUnreadBadge;
        private ImageButton btnOptions;

        ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutContent = itemView.findViewById(R.id.layoutContent);
            checkboxSelect = itemView.findViewById(R.id.checkboxSelect);
            ivProfileImage = itemView.findViewById(R.id.ivProfileImage);
            tvInitials = itemView.findViewById(R.id.tvInitials);

            tvProviderName = itemView.findViewById(R.id.tvProviderName);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvUnreadBadge = itemView.findViewById(R.id.tvUnreadBadge);
            btnOptions = itemView.findViewById(R.id.btnOptions);
        }

        void bind(Conversation conversation) {
            if (conversation == null) {
                return;
            }

            // Unread state - background and bold text
            boolean isUnread = conversation.getUnreadCount() > 0;
            if (isUnread) {
                layoutContent.setBackgroundResource(R.drawable.bg_unread_message);
                tvLastMessage.setTextColor(itemView.getContext().getResources().getColor(R.color.gray_900));
                tvLastMessage.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                layoutContent.setBackgroundResource(android.R.color.white);
                tvLastMessage.setTextColor(itemView.getContext().getResources().getColor(R.color.gray_600));
                tvLastMessage.setTypeface(null, android.graphics.Typeface.NORMAL);
            }

            // Selection mode
            if (isSelectionMode) {
                checkboxSelect.setVisibility(View.VISIBLE);
                checkboxSelect.setChecked(selectedConversationIds.contains(conversation.getId()));
                btnOptions.setVisibility(View.GONE);

                checkboxSelect.setOnCheckedChangeListener(null);
                checkboxSelect.setChecked(selectedConversationIds.contains(conversation.getId()));
                checkboxSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        selectedConversationIds.add(conversation.getId());
                    } else {
                        selectedConversationIds.remove(conversation.getId());
                    }
                    if (actionListener != null) {
                        actionListener.onSelectionChanged(selectedConversationIds.size());
                    }
                });

                // Highlight selected items
                if (selectedConversationIds.contains(conversation.getId())) {
                    layoutContent.setBackgroundResource(R.color.ripple_light);
                }
            } else {
                checkboxSelect.setVisibility(View.GONE);
                btnOptions.setVisibility(View.VISIBLE);
            }

            tvProviderName.setText(conversation.getProviderName());

            // Last message
            String lastMessage = conversation.getLastMessage();
            if (lastMessage != null && !lastMessage.isEmpty()) {
                tvLastMessage.setText(lastMessage);
            } else {
                tvLastMessage.setText(itemView.getContext().getString(R.string.msg_start_conversation));
            }

            // Load profile image or set initials
            loadProviderImageAndInitials(conversation, ivProfileImage, tvInitials);

            // Format time
            if (conversation.getLastMessageTime() != null) {
                tvTime.setText(formatTime(conversation.getLastMessageTime()));
            } else {
                tvTime.setText("");
            }

            // Unread badge
            int unreadCount = conversation.getUnreadCount();
            if (unreadCount > 0) {
                tvUnreadBadge.setVisibility(View.VISIBLE);
                tvUnreadBadge.setText(String.valueOf(unreadCount > 99 ? "99+" : unreadCount));
            } else {
                tvUnreadBadge.setVisibility(View.GONE);
            }

            // Click listeners
            if (!isSelectionMode) {
                itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onConversationClick(conversation);
                    }
                });
            } else {
                itemView.setOnClickListener(v -> {
                    checkboxSelect.setChecked(!checkboxSelect.isChecked());
                });
            }

            // Options menu
            btnOptions.setOnClickListener(v -> showOptionsMenu(conversation, v));
        }

        private void showOptionsMenu(Conversation conversation, View anchor) {
            try {
                androidx.fragment.app.FragmentActivity activity = (androidx.fragment.app.FragmentActivity) anchor
                        .getContext();

                com.example.homerepairs.bottomsheets.ConversationOptionsBottomSheet bottomSheet = new com.example.homerepairs.bottomsheets.ConversationOptionsBottomSheet();

                bottomSheet.setConversation(conversation);
                bottomSheet.setActionListener(actionListener);
                bottomSheet.show(activity.getSupportFragmentManager(), "ConversationOptions");

            } catch (ClassCastException e) {
                // Fallback for context that isn't a FragmentActivity, though in this app it
                // should be.
                e.printStackTrace();
            }
        }

        private void loadProviderImageAndInitials(Conversation conversation, ImageView imageView, TextView tvInitials) {
            String providerId = conversation.getProviderId();
            String providerName = conversation.getProviderName();

            if (imageView == null || tvInitials == null)
                return;

            // Set initial state: show image view, hide initials
            tvInitials.setVisibility(View.GONE);
            imageView.setVisibility(View.VISIBLE);

            if (providerId == null) {
                imageView.setImageResource(R.drawable.no_profile_image);
                return;
            }

            FirebaseProviderService providerService = new FirebaseProviderService();
            providerService.getProviderById(providerId, new FirebaseProviderService.ProviderCallback() {
                @Override
                public void onSuccess(List<com.example.homerepairs.models.Provider> providers) {
                    if (providers != null && !providers.isEmpty()) {
                        com.example.homerepairs.models.Provider provider = providers.get(0);
                        if (provider != null) {
                            String imageUrl = provider.getProfileImageUrl();
                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                Glide.with(imageView.getContext())
                                        .load(imageUrl)
                                        .placeholder(R.drawable.no_profile_image)
                                        .error(R.drawable.no_profile_image)
                                        .circleCrop()
                                        .into(imageView);
                            } else {
                                // Show initials as fallback
                                imageView.setVisibility(View.GONE);
                                tvInitials.setVisibility(View.VISIBLE);
                                tvInitials.setText(getInitials(providerName));
                            }
                        }
                    } else {
                        imageView.setImageResource(R.drawable.no_profile_image);
                    }
                }

                @Override
                public void onError(String error) {
                    imageView.setImageResource(R.drawable.no_profile_image);
                }
            });
        }

        private String getInitials(String name) {
            if (name == null || name.trim().isEmpty())
                return "??";
            String[] parts = name.trim().split("\\s+");
            StringBuilder initials = new StringBuilder();
            for (int i = 0; i < Math.min(parts.length, 2); i++) {
                if (!parts[i].isEmpty()) {
                    initials.append(parts[i].charAt(0));
                }
            }
            return initials.toString().toUpperCase();
        }

        private String formatTime(Date date) {
            if (date == null) {
                return "";
            }

            Date now = new Date();
            long diffInMillis = now.getTime() - date.getTime();
            long diffInMinutes = diffInMillis / (1000 * 60);
            long diffInHours = diffInMinutes / 60;
            long diffInDays = diffInHours / 24;

            android.content.Context context = itemView.getContext();

            if (diffInMinutes < 1) {
                return "Just now"; // Keep "Just now" or add a resource if strictly needed, but common apps often
                                   // keep it or "Now"
            } else if (diffInMinutes < 60) {
                return context.getString(R.string.time_min_ago, diffInMinutes);
            } else if (diffInHours < 24) {
                return context.getString(R.string.time_hour_ago_short, diffInHours);
            } else if (diffInDays < 7) {
                return context.getString(R.string.time_day_ago_short, diffInDays);
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
                return sdf.format(date);
            }
        }
    }
}
