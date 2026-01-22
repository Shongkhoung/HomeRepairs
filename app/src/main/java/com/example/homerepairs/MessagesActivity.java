package com.example.homerepairs;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.airbnb.lottie.LottieAnimationView;
import com.example.homerepairs.adapters.ConversationAdapter;
import com.example.homerepairs.databinding.ActivityMessagesBinding;
import com.example.homerepairs.models.Conversation;
import com.example.homerepairs.services.FirebaseMessageService;
import com.example.homerepairs.utils.AuthHelper;

import com.example.homerepairs.utils.NetworkMonitor;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class MessagesActivity extends BaseActivity {

    private ActivityMessagesBinding binding;
    private ConversationAdapter adapter;
    private FirebaseMessageService messageService;
    private ListenerRegistration conversationListener;
    private String currentUserId;
    private NetworkMonitor networkMonitor;

    private long loadingStartTime = 0;
    private static final long MIN_LOADING_DURATION = 1500;
    private String currentFilter = "All";
    private boolean isSelectionMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMessagesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        currentUserId = AuthHelper.getCurrentUserId(this);
        if (currentUserId == null) {
            startActivity(new Intent(this, LoginActivity.class).putExtra("mode", "sign_in")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
            return;
        }

        initializeViews();
        setupNetworkMonitoring();
        setupRecyclerView();
        setupMessageService();
        setupSearch();
        setupFilterChips();
        setupSelectionMode();
        loadConversations();
        setupBottomNavigation();

        waitForLayoutReady();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (networkMonitor != null)
            networkMonitor.startMonitoring();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (networkMonitor != null)
            networkMonitor.stopMonitoring();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (networkMonitor != null)
            networkMonitor.cleanup();
        if (conversationListener != null)
            conversationListener.remove();
    }

    private void initializeViews() {

        boolean isDebug = (getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0;

        binding.btnBrowseServices.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });
    }

    private void setupNetworkMonitoring() {
        networkMonitor = new NetworkMonitor(this);
        networkMonitor.initialize(binding.llInternetLoading, binding.tvInternetMessage, binding.ivNoConnection);
        networkMonitor.startMonitoring();
    }

    private void setupRecyclerView() {
        binding.rvConversations.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ConversationAdapter(new ArrayList<>(),
                conversation -> {
                    if (!isSelectionMode) {
                        Intent intent = new Intent(this, ChatActivity.class);
                        intent.putExtra("conversationId", conversation.getId());
                        intent.putExtra("providerId", conversation.getProviderId());
                        intent.putExtra("providerName", conversation.getProviderName());
                        startActivity(intent);
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    }
                },
                new ConversationAdapter.OnConversationActionListener() {
                    @Override
                    public void onMarkAsRead(List<Conversation> convs) {
                        markConversationsAsRead(convs);
                    }

                    @Override
                    public void onMarkAsUnread(List<Conversation> convs) {
                        markConversationsAsUnread(convs);
                    }

                    @Override
                    public void onPin(Conversation conv) {
                        pinConversation(conv, true);
                    }

                    @Override
                    public void onUnpin(Conversation conv) {
                        pinConversation(conv, false);
                    }

                    @Override
                    public void onStar(Conversation conv) {
                        starConversation(conv, true);
                    }

                    @Override
                    public void onUnstar(Conversation conv) {
                        starConversation(conv, false);
                    }

                    @Override
                    public void onArchive(Conversation conv) {
                        Toast.makeText(MessagesActivity.this, getString(R.string.msg_archive_soon), Toast.LENGTH_SHORT)
                                .show();
                    }

                    @Override
                    public void onDelete(List<Conversation> convs) {
                        deleteConversations(convs);
                    }

                    @Override
                    public void onSelectionChanged(int count) {
                        updateSelectionBar(count);
                    }
                });
        binding.rvConversations.setAdapter(adapter);
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filterBy(currentFilter);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupFilterChips() {
        binding.btnAll.setOnClickListener(v -> handleFilterChange("All"));
        binding.btnUnread.setOnClickListener(v -> handleFilterChange("Unread"));
        binding.btnPinned.setOnClickListener(v -> handleFilterChange("Pinned"));
    }

    private void handleFilterChange(String filter) {
        currentFilter = filter;
        updateFilterChips();
        adapter.filterBy(filter);
        updateFilterCounts();
    }

    private void updateFilterChips() {
        updateButtonStyle(binding.btnAll, currentFilter.equals("All"));
        updateButtonStyle(binding.btnUnread, currentFilter.equals("Unread"));
        updateButtonStyle(binding.btnPinned, currentFilter.equals("Pinned"));
    }

    private void updateButtonStyle(com.google.android.material.button.MaterialButton button, boolean isSelected) {
        if (isSelected) {
            button.setBackgroundResource(R.drawable.bg_pill_selected);
            button.setTextColor(getResources().getColor(R.color.white));
        } else {
            button.setBackgroundResource(R.drawable.bg_pill_unselected);
            button.setTextColor(getResources().getColor(R.color.gray_700));
        }
    }

    private void setupSelectionMode() {
        binding.btnMarkRead.setOnClickListener(v -> {
            List<Conversation> selected = adapter.getSelectedConversations();
            if (!selected.isEmpty()) {
                markConversationsAsRead(selected);
                exitSelectionMode();
            }
        });

        binding.btnDeleteSelected.setOnClickListener(v -> {
            List<Conversation> selected = adapter.getSelectedConversations();
            if (!selected.isEmpty()) {
                deleteConversations(selected);
                exitSelectionMode();
            }
        });
    }

    private void exitSelectionMode() {
        isSelectionMode = false;
        adapter.setSelectionMode(false);
        binding.llSelectionBar.setVisibility(View.GONE);
    }

    private void updateSelectionBar(int count) {
        if (count > 0) {
            binding.tvSelectedCount.setText(getString(R.string.msg_selected_count, count));
            binding.llSelectionBar.setVisibility(View.VISIBLE);
        } else {
            binding.llSelectionBar.setVisibility(View.GONE);
        }
    }

    private void setupMessageService() {
        messageService = new FirebaseMessageService();
    }

    private void loadConversations() {
        binding.llEmptyState.setVisibility(View.GONE);
        conversationListener = messageService.listenToConversations(currentUserId,
                new FirebaseMessageService.ConversationListCallback() {
                    @Override
                    public void onSuccess(List<Conversation> conversations) {
                        if (conversations != null && !conversations.isEmpty()) {
                            adapter.updateConversations(conversations);
                            binding.rvConversations.setVisibility(View.VISIBLE);
                            binding.llEmptyState.setVisibility(View.GONE);
                            updateUnreadCount();
                            updateFilterCounts();
                        } else {
                            binding.rvConversations.setVisibility(View.GONE);
                            binding.llEmptyState.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Log.e("MessagesActivity", "Error: " + error);
                        binding.rvConversations.setVisibility(View.GONE);
                        binding.llEmptyState.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void updateUnreadCount() {
        int count = adapter.getUnreadCount();
        binding.tvUnreadCount
                .setText(count > 0 ? getString(R.string.msg_unread_count, count) : getString(R.string.msg_no_unread));
        binding.tvUnreadCount.setVisibility(View.VISIBLE);
    }

    private void updateFilterCounts() {
        binding.btnAll.setText(getString(R.string.filter_all_count, adapter.getAllCount()));
        binding.btnUnread.setText(getString(R.string.filter_unread_count, adapter.getUnreadCount()));
        binding.btnPinned.setText(getString(R.string.filter_pinned_count, adapter.getPinnedCount()));
    }

    private void markConversationsAsRead(List<Conversation> convs) {
        for (Conversation c : convs) {
            c.setRead(true);
            c.setUnreadCount(0);
        }
        adapter.notifyDataSetChanged();
        updateUnreadCount();
        updateFilterCounts();
    }

    private void markConversationsAsUnread(List<Conversation> convs) {
        for (Conversation c : convs) {
            c.setRead(false);
            c.setUnreadCount(1);
        }
        adapter.notifyDataSetChanged();
        updateUnreadCount();
        updateFilterCounts();
    }

    private void pinConversation(Conversation conv, boolean pin) {
        conv.setPinned(pin);
        adapter.notifyDataSetChanged();
        updateFilterCounts();
    }

    private void starConversation(Conversation conv, boolean star) {
        conv.setStarred(star);
        adapter.notifyDataSetChanged();
        updateFilterCounts();
    }

    private void deleteConversations(List<Conversation> convs) {
        new android.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_delete_title))
                .setMessage(getString(R.string.dialog_delete_message, convs.size()))
                .setPositiveButton(getString(R.string.dialog_delete_confirm), (d, w) -> {
                    // Show loading or progress
                    showScreenLoading(true);

                    int total = convs.size();
                    final int[] completed = { 0 };
                    final int[] errors = { 0 };

                    for (Conversation c : convs) {
                        messageService.deleteConversation(c.getId(),
                                (aVoid) -> {
                                    completed[0]++;
                                    checkDeletionCompletion(total, completed[0], errors[0]);
                                },
                                (e) -> {
                                    completed[0]++;
                                    errors[0]++;
                                    Log.e("MessagesActivity", "Error deleting conversation " + c.getId(), e);
                                    checkDeletionCompletion(total, completed[0], errors[0]);
                                });
                    }
                })
                .setNegativeButton(getString(R.string.btn_cancel_selection), null)
                .show();
    }

    private void checkDeletionCompletion(int total, int completed, int errors) {
        if (completed == total) {
            hideScreenLoading();
            if (errors > 0) {
                Toast.makeText(this, getString(R.string.msg_delete_error, errors), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, getString(R.string.msg_conversations_deleted), Toast.LENGTH_SHORT).show();
            }
            // Exit selection mode if active
            if (isSelectionMode) {
                exitSelectionMode();
            }
            // Note: UI will update automatically via Firestore snapshot listener
        }
    }

    private void setupBottomNavigation() {
        binding.bottomNavigation.setElevation(0f);
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                showScreenLoading(true);
                startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                finish();
                return true;
            } else if (id == R.id.nav_bookings) {
                showScreenLoading(true);
                startActivity(new Intent(this, BookingsActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_messages)
                return true;
            else if (id == R.id.nav_profile) {
                showScreenLoading(true);
                startActivity(new Intent(this, UserProfileActivity.class));
                finish();
                return true;
            }
            return false;
        });
        binding.bottomNavigation.setSelectedItemId(R.id.nav_messages);
    }

    private void showScreenLoading(boolean show) {
        if (show) {
            binding.llScreenLoading.setVisibility(View.VISIBLE);
            binding.llScreenLoading.setAlpha(0f);
            binding.llScreenLoading.animate().alpha(1f).setDuration(300).start();
            binding.ivScreenLoading.setAnimation(R.raw.loading);
            binding.ivScreenLoading.playAnimation();
        } else {
            binding.llScreenLoading.animate().alpha(0f).setDuration(200)
                    .withEndAction(() -> binding.llScreenLoading.setVisibility(View.GONE)).start();
        }
    }

    private void waitForLayoutReady() {
        loadingStartTime = System.currentTimeMillis();
        if (binding.llScreenLoading.getVisibility() != View.VISIBLE)
            showScreenLoading(true);
        View root = findViewById(android.R.id.content);
        root.getViewTreeObserver()
                .addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (root.getWidth() > 0 && root.getHeight() > 0) {
                            root.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            long delay = Math
                                    .max(MIN_LOADING_DURATION - (System.currentTimeMillis() - loadingStartTime), 200);
                            root.postDelayed(() -> hideScreenLoading(), delay);
                        }
                    }
                });
    }

    private void hideScreenLoading() {
        binding.ivScreenLoading.cancelAnimation();
        binding.llScreenLoading.animate().alpha(0f).setDuration(200)
                .withEndAction(() -> binding.llScreenLoading.setVisibility(View.GONE)).start();
    }
}
