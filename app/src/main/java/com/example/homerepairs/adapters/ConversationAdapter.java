package com.example.homerepairs.adapters;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
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
        private MaterialCardView cardConversation;
        private MaterialCheckBox checkboxSelect;
        private ImageView ivProfileImage;
        private ImageView ivPinnedBadge;
        private ImageView ivStarredBadge;
        private View viewOnlineStatus;
        private TextView tvProviderName;
        private ImageView ivStarIcon;
        private TextView tvTime;
        private ImageView ivReadCheckmark;
        private TextView tvLastMessage;
        private View viewUnreadIndicator;
        private TextView tvUnreadBadge;
        private ImageButton btnOptions;
        
        ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
            cardConversation = itemView.findViewById(R.id.cardConversation);
            checkboxSelect = itemView.findViewById(R.id.checkboxSelect);
            ivProfileImage = itemView.findViewById(R.id.ivProfileImage);
            ivPinnedBadge = itemView.findViewById(R.id.ivPinnedBadge);
            ivStarredBadge = itemView.findViewById(R.id.ivStarredBadge);
            viewOnlineStatus = itemView.findViewById(R.id.viewOnlineStatus);
            tvProviderName = itemView.findViewById(R.id.tvProviderName);
            ivStarIcon = itemView.findViewById(R.id.ivStarIcon);
            tvTime = itemView.findViewById(R.id.tvTime);
            ivReadCheckmark = itemView.findViewById(R.id.ivReadCheckmark);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            viewUnreadIndicator = itemView.findViewById(R.id.viewUnreadIndicator);
            tvUnreadBadge = itemView.findViewById(R.id.tvUnreadBadge);
            btnOptions = itemView.findViewById(R.id.btnOptions);
        }
        
        void bind(Conversation conversation) {
            if (conversation == null) {
                return;
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
                    cardConversation.setCardBackgroundColor(itemView.getContext().getResources().getColor(R.color.ripple_light));
                    cardConversation.setStrokeWidth(2);
                    cardConversation.setStrokeColor(itemView.getContext().getResources().getColor(R.color.bright_periwinkle_blue));
                } else {
                    cardConversation.setCardBackgroundColor(itemView.getContext().getResources().getColor(R.color.white));
                    cardConversation.setStrokeWidth(0);
                }
            } else {
                checkboxSelect.setVisibility(View.GONE);
                btnOptions.setVisibility(View.VISIBLE);
                cardConversation.setCardBackgroundColor(itemView.getContext().getResources().getColor(R.color.white));
                cardConversation.setStrokeWidth(0);
            }
            
            tvProviderName.setText(conversation.getProviderName());
            
            // Last message
            String lastMessage = conversation.getLastMessage();
            if (lastMessage != null && !lastMessage.isEmpty()) {
                tvLastMessage.setText(lastMessage);
            } else {
                tvLastMessage.setText("Start conversation...");
            }
            
            // Read checkmark
            if (conversation.isRead() && conversation.getUnreadCount() == 0) {
                ivReadCheckmark.setVisibility(View.VISIBLE);
            } else {
                ivReadCheckmark.setVisibility(View.GONE);
            }
            
            // Load profile image
            loadProviderImage(conversation.getProviderId(), ivProfileImage);
            
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
                viewUnreadIndicator.setVisibility(View.VISIBLE);
            } else {
                tvUnreadBadge.setVisibility(View.GONE);
                viewUnreadIndicator.setVisibility(View.GONE);
            }
            
            // Pinned badge
            if (conversation.isPinned()) {
                ivPinnedBadge.setVisibility(View.VISIBLE);
                ivStarredBadge.setVisibility(View.GONE);
            } else {
                ivPinnedBadge.setVisibility(View.GONE);
            }
            
            // Starred badge and icon
            if (conversation.isStarred()) {
                ivStarredBadge.setVisibility(conversation.isPinned() ? View.GONE : View.VISIBLE);
                ivStarIcon.setVisibility(View.VISIBLE);
            } else {
                ivStarredBadge.setVisibility(View.GONE);
                ivStarIcon.setVisibility(View.GONE);
            }
            
            // Online status (mock - you can implement real status later)
            viewOnlineStatus.setVisibility(View.VISIBLE);
            
            // Click listeners
            if (!isSelectionMode) {
                cardConversation.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onConversationClick(conversation);
                    }
                });
            } else {
                cardConversation.setOnClickListener(v -> {
                    checkboxSelect.setChecked(!checkboxSelect.isChecked());
                });
            }
            
            // Options menu
            btnOptions.setOnClickListener(v -> showOptionsMenu(conversation, v));
        }
        
        private void showOptionsMenu(Conversation conversation, View anchor) {
            PopupMenu popupMenu = new PopupMenu(anchor.getContext(), anchor);
            popupMenu.getMenuInflater().inflate(R.menu.conversation_options, popupMenu.getMenu());
            
            // Force icons to show in PopupMenu (works on API 29+)
            try {
                java.lang.reflect.Field[] fields = popupMenu.getClass().getDeclaredFields();
                for (java.lang.reflect.Field field : fields) {
                    if ("mPopup".equals(field.getName())) {
                        field.setAccessible(true);
                        Object menuPopupHelper = field.get(popupMenu);
                        Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
                        java.lang.reflect.Method setForceIcons = classPopupHelper.getMethod("setForceShowIcon", boolean.class);
                        setForceIcons.invoke(menuPopupHelper, true);
                        break;
                    }
                }
            } catch (Exception e) {
                // Try alternative method for older Android versions
                try {
                    java.lang.reflect.Field mPopup = popupMenu.getClass().getDeclaredField("mPopup");
                    mPopup.setAccessible(true);
                    Object menuPopupHelper = mPopup.get(popupMenu);
                    java.lang.reflect.Method setForceShowIcon = menuPopupHelper.getClass().getDeclaredMethod("setForceShowIcon", boolean.class);
                    setForceShowIcon.invoke(menuPopupHelper, true);
                } catch (Exception ex) {
                    android.util.Log.e("ConversationAdapter", "Error forcing icons to show", ex);
                }
            }
            
            // Update menu items based on conversation state
            MenuItem markReadItem = popupMenu.getMenu().findItem(R.id.action_mark_read);
            MenuItem markUnreadItem = popupMenu.getMenu().findItem(R.id.action_mark_unread);
            MenuItem pinItem = popupMenu.getMenu().findItem(R.id.action_pin);
            MenuItem unpinItem = popupMenu.getMenu().findItem(R.id.action_unpin);
            MenuItem starItem = popupMenu.getMenu().findItem(R.id.action_star);
            MenuItem unstarItem = popupMenu.getMenu().findItem(R.id.action_unstar);
            
            // Show "Mark as Read" if there are unread messages, otherwise show "Mark as Unread"
            if (conversation.getUnreadCount() > 0) {
                markReadItem.setVisible(true);
                markUnreadItem.setVisible(false);
            } else {
                markReadItem.setVisible(false);
                markUnreadItem.setVisible(true);
            }
            
            if (conversation.isPinned()) {
                pinItem.setVisible(false);
                unpinItem.setVisible(true);
            } else {
                pinItem.setVisible(true);
                unpinItem.setVisible(false);
            }
            
            if (conversation.isStarred()) {
                starItem.setVisible(false);
                unstarItem.setVisible(true);
            } else {
                starItem.setVisible(true);
                unstarItem.setVisible(false);
            }
            
            // Style delete item with red text and icon color
            MenuItem deleteItem = popupMenu.getMenu().findItem(R.id.action_delete);
            if (deleteItem != null) {
                // Set red text color
                android.text.SpannableString deleteText = new android.text.SpannableString(deleteItem.getTitle());
                int errorColor = anchor.getContext().getResources().getColor(R.color.error);
                deleteText.setSpan(new android.text.style.ForegroundColorSpan(errorColor), 
                    0, deleteText.length(), 0);
                deleteItem.setTitle(deleteText);
                
                // Set red icon tint
                if (deleteItem.getIcon() != null) {
                    android.graphics.drawable.Drawable deleteIcon = deleteItem.getIcon();
                    deleteIcon = deleteIcon.mutate();
                    deleteIcon.setColorFilter(errorColor, android.graphics.PorterDuff.Mode.SRC_IN);
                    deleteItem.setIcon(deleteIcon);
                }
            }
            
            popupMenu.setOnMenuItemClickListener(item -> {
                if (actionListener == null) return false;
                
                int itemId = item.getItemId();
                if (itemId == R.id.action_mark_read) {
                    actionListener.onMarkAsRead(java.util.Collections.singletonList(conversation));
                } else if (itemId == R.id.action_mark_unread) {
                    actionListener.onMarkAsUnread(java.util.Collections.singletonList(conversation));
                } else if (itemId == R.id.action_pin) {
                    actionListener.onPin(conversation);
                } else if (itemId == R.id.action_unpin) {
                    actionListener.onUnpin(conversation);
                } else if (itemId == R.id.action_star) {
                    actionListener.onStar(conversation);
                } else if (itemId == R.id.action_unstar) {
                    actionListener.onUnstar(conversation);
                } else if (itemId == R.id.action_archive) {
                    actionListener.onArchive(conversation);
                } else if (itemId == R.id.action_delete) {
                    actionListener.onDelete(java.util.Collections.singletonList(conversation));
                }
                return true;
            });
            
            popupMenu.show();
        }
        
        private void loadProviderImage(String providerId, ImageView imageView) {
            if (providerId == null || imageView == null) {
                imageView.setImageResource(R.drawable.ic_profile);
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
                                        .placeholder(R.drawable.ic_profile)
                                        .error(R.drawable.ic_profile)
                                        .circleCrop()
                                        .into(imageView);
                            } else {
                                imageView.setImageResource(R.drawable.ic_profile);
                            }
                        } else {
                            imageView.setImageResource(R.drawable.ic_profile);
                        }
                    } else {
                        imageView.setImageResource(R.drawable.ic_profile);
                    }
                }
                
                @Override
                public void onError(String error) {
                    android.util.Log.e("ConversationAdapter", "Error loading provider image: " + error);
                    imageView.setImageResource(R.drawable.ic_profile);
                }
            });
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
            
            if (diffInMinutes < 1) {
                return "Just now";
            } else if (diffInMinutes < 60) {
                return diffInMinutes + "m ago";
            } else if (diffInHours < 24) {
                return diffInHours + "h ago";
            } else if (diffInDays < 7) {
                return diffInDays + "d ago";
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
                return sdf.format(date);
            }
        }
    }
}
