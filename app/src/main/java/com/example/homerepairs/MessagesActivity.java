package com.example.homerepairs;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.FrameLayout;

import com.airbnb.lottie.LottieAnimationView;
import com.example.homerepairs.adapters.ConversationAdapter;
import com.example.homerepairs.models.Conversation;
import com.example.homerepairs.services.FirebaseMessageService;
import com.example.homerepairs.utils.AuthHelper;
import com.example.homerepairs.utils.MessageTestHelper;
import com.example.homerepairs.utils.NetworkMonitor;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.airbnb.lottie.LottieAnimationView;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class MessagesActivity extends AppCompatActivity {

    private RecyclerView rvConversations;
    private LinearLayoutManager layoutManager;
    private ConversationAdapter adapter;
    private FirebaseMessageService messageService;
    private View llEmptyState;
    private ListenerRegistration conversationListener;
    private String currentUserId;
    private MaterialButton btnTestData;
    private MaterialButton btnSelect;
    private MessageTestHelper testHelper;
    private NetworkMonitor networkMonitor;

    // UI Elements
    private TextView tvUnreadCount;
    private EditText etSearch;
    private Chip chipAll, chipUnread, chipPinned, chipStarred;
    private FrameLayout llScreenLoading;
    private LottieAnimationView ivScreenLoading;
    private long loadingStartTime = 0;
    private static final long MIN_LOADING_DURATION = 1500; // Minimum 1.5 seconds
    private LinearLayout llSelectionBar;
    private TextView tvSelectedCount;
    private MaterialButton btnMarkRead, btnDeleteSelected;

    // State
    private String currentFilter = "All";
    private boolean isSelectionMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            // User not authenticated, redirect to login in sign in mode
            Intent intent = new Intent(this, LoginActivity.class);
            intent.putExtra("mode", "sign_in");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
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

        // Wait for layout to be ready before hiding loading overlay
        waitForLayoutReady();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (networkMonitor != null) {
            networkMonitor.startMonitoring();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (networkMonitor != null) {
            networkMonitor.stopMonitoring();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (networkMonitor != null) {
            networkMonitor.cleanup();
        }
        if (conversationListener != null) {
            conversationListener.remove();
        }
    }

    private void initializeViews() {
        rvConversations = findViewById(R.id.rvConversations);
        llEmptyState = findViewById(R.id.llEmptyState);
        btnTestData = findViewById(R.id.btnTestData);
        btnSelect = findViewById(R.id.btnSelect);
        tvUnreadCount = findViewById(R.id.tvUnreadCount);
        etSearch = findViewById(R.id.etSearch);
        chipAll = findViewById(R.id.chipAll);
        chipUnread = findViewById(R.id.chipUnread);
        chipPinned = findViewById(R.id.chipPinned);
        chipStarred = findViewById(R.id.chipStarred);
        llSelectionBar = findViewById(R.id.llSelectionBar);
        tvSelectedCount = findViewById(R.id.tvSelectedCount);
        btnMarkRead = findViewById(R.id.btnMarkRead);
        btnDeleteSelected = findViewById(R.id.btnDeleteSelected);
        llScreenLoading = findViewById(R.id.llScreenLoading);
        ivScreenLoading = findViewById(R.id.ivScreenLoading);
        testHelper = new MessageTestHelper();

        // Setup test data button - only visible in debug builds
        if (btnTestData != null) {
            boolean isDebug = (getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0;
            btnTestData.setVisibility(isDebug ? View.VISIBLE : View.GONE);
            btnTestData.setOnClickListener(v -> createTestData());
        }

        // Setup Browse Services button
        MaterialButton btnBrowseServices = findViewById(R.id.btnBrowseServices);
        if (btnBrowseServices != null) {
            btnBrowseServices.setOnClickListener(v -> {
                Intent intent = new Intent(MessagesActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            });
        }
    }

    private void setupNetworkMonitoring() {
        networkMonitor = new NetworkMonitor(this);
        FrameLayout llInternetLoading = findViewById(R.id.llInternetLoading);
        TextView tvInternetMessage = findViewById(R.id.tvInternetMessage);
        LottieAnimationView ivNoConnection = findViewById(R.id.ivNoConnection);
        networkMonitor.initialize(llInternetLoading, tvInternetMessage, ivNoConnection);
        networkMonitor.startMonitoring();
    }

    private void setupRecyclerView() {
        layoutManager = new LinearLayoutManager(this);
        rvConversations.setLayoutManager(layoutManager);

        adapter = new ConversationAdapter(new ArrayList<>(),
                conversation -> {
                    if (!isSelectionMode) {
                        // Navigate to chat activity
                        Intent intent = new Intent(MessagesActivity.this, ChatActivity.class);
                        intent.putExtra("conversationId", conversation.getId());
                        intent.putExtra("providerId", conversation.getProviderId());
                        intent.putExtra("providerName", conversation.getProviderName());
                        startActivity(intent);
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    }
                },
                new ConversationAdapter.OnConversationActionListener() {
                    @Override
                    public void onMarkAsRead(List<Conversation> conversations) {
                        markConversationsAsRead(conversations);
                    }

                    @Override
                    public void onMarkAsUnread(List<Conversation> conversations) {
                        markConversationsAsUnread(conversations);
                    }

                    @Override
                    public void onPin(Conversation conversation) {
                        pinConversation(conversation, true);
                    }

                    @Override
                    public void onUnpin(Conversation conversation) {
                        pinConversation(conversation, false);
                    }

                    @Override
                    public void onStar(Conversation conversation) {
                        starConversation(conversation, true);
                    }

                    @Override
                    public void onUnstar(Conversation conversation) {
                        starConversation(conversation, false);
                    }

                    @Override
                    public void onArchive(Conversation conversation) {
                        // TODO: Implement archive functionality
                        android.widget.Toast.makeText(MessagesActivity.this,
                                getString(R.string.msg_archive_soon),
                                android.widget.Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onDelete(List<Conversation> conversations) {
                        deleteConversations(conversations);
                    }

                    @Override
                    public void onSelectionChanged(int count) {
                        updateSelectionBar(count);
                    }
                });
        rvConversations.setAdapter(adapter);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterConversations(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupFilterChips() {
        chipAll.setOnClickListener(v -> {
            currentFilter = "All";
            updateFilterChips();
            adapter.filterBy("All");
            updateFilterCounts();
        });

        chipUnread.setOnClickListener(v -> {
            currentFilter = "Unread";
            updateFilterChips();
            adapter.filterBy("Unread");
            updateFilterCounts();
        });

        chipPinned.setOnClickListener(v -> {
            currentFilter = "Pinned";
            updateFilterChips();
            adapter.filterBy("Pinned");
            updateFilterCounts();
        });

        chipStarred.setOnClickListener(v -> {
            currentFilter = "Starred";
            updateFilterChips();
            adapter.filterBy("Starred");
            updateFilterCounts();
        });
    }

    private void updateFilterChips() {
        chipAll.setChecked(currentFilter.equals("All"));
        chipUnread.setChecked(currentFilter.equals("Unread"));
        chipPinned.setChecked(currentFilter.equals("Pinned"));
        chipStarred.setChecked(currentFilter.equals("Starred"));

        // Update chip backgrounds
        updateChipStyle(chipAll, currentFilter.equals("All"));
        updateChipStyle(chipUnread, currentFilter.equals("Unread"));
        updateChipStyle(chipPinned, currentFilter.equals("Pinned"));
        updateChipStyle(chipStarred, currentFilter.equals("Starred"));
    }

    private void updateChipStyle(Chip chip, boolean isSelected) {
        if (isSelected) {
            chip.setChipBackgroundColorResource(R.color.deep_royal_blue);
            chip.setTextColor(getResources().getColor(R.color.text_white));
        } else {
            chip.setChipBackgroundColorResource(R.color.background_gray);
            chip.setTextColor(getResources().getColor(R.color.text_primary));
        }
    }

    private void setupSelectionMode() {
        btnSelect.setOnClickListener(v -> {
            isSelectionMode = !isSelectionMode;
            adapter.setSelectionMode(isSelectionMode);

            if (isSelectionMode) {
                btnSelect.setText(getString(R.string.btn_cancel_selection));
                llSelectionBar.setVisibility(View.VISIBLE);
            } else {
                btnSelect.setText(getString(R.string.btn_select));
                llSelectionBar.setVisibility(View.GONE);
                adapter.clearSelection();
            }
        });

        btnMarkRead.setOnClickListener(v -> {
            List<Conversation> selected = adapter.getSelectedConversations();
            if (!selected.isEmpty()) {
                markConversationsAsRead(selected);
                exitSelectionMode();
            }
        });

        btnDeleteSelected.setOnClickListener(v -> {
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
        btnSelect.setText(getString(R.string.btn_select));
        llSelectionBar.setVisibility(View.GONE);
    }

    private void updateSelectionBar(int count) {
        if (count > 0) {
            tvSelectedCount.setText(getString(R.string.msg_selected_count, count));
            llSelectionBar.setVisibility(View.VISIBLE);
        } else {
            llSelectionBar.setVisibility(View.GONE);
        }
    }

    private void setupMessageService() {
        messageService = new FirebaseMessageService();
    }

    private void loadConversations() {
        llEmptyState.setVisibility(View.GONE);

        // Set up real-time listener
        conversationListener = messageService.listenToConversations(currentUserId,
                new FirebaseMessageService.ConversationListCallback() {
                    @Override
                    public void onSuccess(List<Conversation> conversations) {
                        if (conversations != null && !conversations.isEmpty()) {
                            adapter.updateConversations(conversations);
                            rvConversations.setVisibility(View.VISIBLE);
                            llEmptyState.setVisibility(View.GONE);
                            updateUnreadCount();
                            updateFilterCounts();
                        } else {
                            rvConversations.setVisibility(View.GONE);
                            llEmptyState.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        android.util.Log.e("MessagesActivity", "Error loading conversations: " + error);

                        if (error != null && error.contains("PERMISSION_DENIED")) {
                            android.widget.Toast.makeText(MessagesActivity.this,
                                    getString(R.string.msg_firebase_permission_error),
                                    android.widget.Toast.LENGTH_LONG).show();
                        }

                        rvConversations.setVisibility(View.GONE);
                        llEmptyState.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void filterConversations(String query) {
        // This would filter the conversations by search query
        // For now, we'll just reapply the current filter
        adapter.filterBy(currentFilter);
    }

    private void updateUnreadCount() {
        int unreadCount = adapter.getUnreadCount();
        if (unreadCount > 0) {
            tvUnreadCount.setText(getString(R.string.msg_unread_count, unreadCount));
            tvUnreadCount.setVisibility(View.VISIBLE);
        } else {
            tvUnreadCount.setText(getString(R.string.msg_no_unread));
            tvUnreadCount.setVisibility(View.VISIBLE);
        }
    }

    private void updateFilterCounts() {
        int allCount = adapter.getAllCount();
        int unreadCount = adapter.getUnreadCount();
        int pinnedCount = adapter.getPinnedCount();
        int starredCount = adapter.getStarredCount();

        chipAll.setText(getString(R.string.filter_all_count, allCount));
        chipUnread.setText(getString(R.string.filter_unread_count, unreadCount));
        chipPinned.setText(getString(R.string.filter_pinned_count, pinnedCount));
        chipStarred.setText(getString(R.string.filter_star));
    }

    private void markConversationsAsRead(List<Conversation> conversations) {
        for (Conversation conv : conversations) {
            conv.setRead(true);
            conv.setUnreadCount(0);
            // TODO: Update in Firebase
        }
        adapter.notifyDataSetChanged();
        updateUnreadCount();
        updateFilterCounts();
    }

    private void markConversationsAsUnread(List<Conversation> conversations) {
        for (Conversation conv : conversations) {
            conv.setRead(false);
            conv.setUnreadCount(1);
            // TODO: Update in Firebase
        }
        adapter.notifyDataSetChanged();
        updateUnreadCount();
        updateFilterCounts();
    }

    private void pinConversation(Conversation conversation, boolean pin) {
        conversation.setPinned(pin);
        // TODO: Update in Firebase
        adapter.notifyDataSetChanged();
        updateFilterCounts();
    }

    private void starConversation(Conversation conversation, boolean star) {
        conversation.setStarred(star);
        // TODO: Update in Firebase
        adapter.notifyDataSetChanged();
        updateFilterCounts();
    }

    private void deleteConversations(List<Conversation> conversations) {
        // Show confirmation dialog
        new android.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_delete_title))
                .setMessage(getString(R.string.dialog_delete_message, conversations.size()))
                .setPositiveButton(getString(R.string.dialog_delete_confirm), (dialog, which) -> {
                    // TODO: Delete from Firebase
                    List<Conversation> allConvs = adapter.getAllConversations();
                    for (Conversation conv : conversations) {
                        allConvs.remove(conv);
                    }
                    adapter.updateConversations(allConvs);
                    updateFilterCounts();
                    android.widget.Toast.makeText(this,
                            getString(R.string.msg_conversations_deleted),
                            android.widget.Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(getString(R.string.btn_cancel_selection), null)
                .show();
    }

    private String getCurrentUserId() {
        return AuthHelper.getCurrentUserId(this);
    }

    private String getCurrentUserName() {
        return AuthHelper.getCurrentUserName(this);
    }

    private void createTestData() {
        btnTestData.setEnabled(false);
        btnTestData.setText(getString(R.string.btn_creating));
        android.widget.Toast
                .makeText(this, getString(R.string.msg_creating_test_data), android.widget.Toast.LENGTH_SHORT).show();

        testHelper.createTestConversations(currentUserId, getCurrentUserName(),
                new FirebaseMessageService.ConversationListCallback() {
                    @Override
                    public void onSuccess(List<Conversation> conversations) {
                        btnTestData.setEnabled(true);
                        btnTestData.setText(getString(R.string.btn_test));
                        android.widget.Toast.makeText(MessagesActivity.this,
                                getString(R.string.msg_test_data_created),
                                android.widget.Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String error) {
                        btnTestData.setEnabled(true);
                        btnTestData.setText(getString(R.string.btn_test));
                        android.util.Log.e("MessagesActivity", "Error creating test data: " + error);
                        android.widget.Toast.makeText(MessagesActivity.this,
                                getString(R.string.error_prefix, error),
                                android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            bottomNavigation.setElevation(0f);
        }

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                showScreenLoading(true);
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish();
                return true;
            } else if (itemId == R.id.nav_bookings) {
                showScreenLoading(true);
                Intent intent = new Intent(this, BookingsActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish();
                return true;
            } else if (itemId == R.id.nav_messages) {
                return true;
            } else if (itemId == R.id.nav_profile) {
                showScreenLoading(true);
                Intent intent = new Intent(this, UserProfileActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
                return true;
            }
            return false;
        });
        bottomNavigation.setSelectedItemId(R.id.nav_messages);
    }

    /**
     * Show or hide screen transition loading overlay
     * 
     * @param show true to show, false to hide
     */
    private void showScreenLoading(boolean show) {
        if (llScreenLoading == null) {
            return;
        }

        if (show) {
            llScreenLoading.setVisibility(View.VISIBLE);
            llScreenLoading.setAlpha(0f);
            llScreenLoading.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
            // Start Lottie animation with smooth settings
            if (ivScreenLoading != null) {
                ivScreenLoading.setAnimation(R.raw.loading);
                ivScreenLoading.setSpeed(1.0f);
                ivScreenLoading.setRenderMode(com.airbnb.lottie.RenderMode.HARDWARE);
                ivScreenLoading.enableMergePathsForKitKatAndAbove(true);
                ivScreenLoading.playAnimation();
            }
        } else {
            llScreenLoading.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() -> llScreenLoading.setVisibility(View.GONE))
                    .start();
        }
    }

    /**
     * Wait for layout to be fully rendered before hiding loading overlay
     * Ensures loading shows for minimum duration
     */
    private void waitForLayoutReady() {
        if (llScreenLoading == null) {
            return;
        }

        // Record start time
        loadingStartTime = System.currentTimeMillis();

        // Show loading overlay if it's not already visible
        if (llScreenLoading.getVisibility() != View.VISIBLE) {
            llScreenLoading.setVisibility(View.VISIBLE);
            llScreenLoading.setAlpha(0f);
            llScreenLoading.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
            // Start Lottie animation with smooth settings
            if (ivScreenLoading != null) {
                ivScreenLoading.setAnimation(R.raw.loading);
                ivScreenLoading.setSpeed(1.0f);
                ivScreenLoading.setRenderMode(com.airbnb.lottie.RenderMode.HARDWARE);
                ivScreenLoading.enableMergePathsForKitKatAndAbove(true);
                ivScreenLoading.playAnimation();
            }
        }

        // Get root view
        View rootView = findViewById(android.R.id.content);
        if (rootView == null) {
            // Fallback: hide after minimum duration
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> hideScreenLoading(),
                    MIN_LOADING_DURATION);
            return;
        }

        // Wait for layout to be measured and laid out
        rootView.getViewTreeObserver()
                .addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        // Check if layout is ready (has dimensions)
                        if (rootView.getWidth() > 0 && rootView.getHeight() > 0) {
                            // Remove listener to avoid multiple calls
                            rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                            // Calculate remaining time to meet minimum duration
                            long elapsedTime = System.currentTimeMillis() - loadingStartTime;
                            long remainingTime = MIN_LOADING_DURATION - elapsedTime;

                            // Wait for minimum duration or additional 200ms, whichever is longer
                            long delayTime = Math.max(remainingTime, 200);
                            rootView.postDelayed(() -> hideScreenLoading(), delayTime);
                        }
                    }
                });
    }

    /**
     * Hide screen loading overlay with animation
     */
    private void hideScreenLoading() {
        if (llScreenLoading == null || llScreenLoading.getVisibility() != View.VISIBLE) {
            return;
        }

        // Stop Lottie animation
        if (ivScreenLoading != null) {
            ivScreenLoading.cancelAnimation();
        }

        llScreenLoading.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> llScreenLoading.setVisibility(View.GONE))
                .start();
    }
}
