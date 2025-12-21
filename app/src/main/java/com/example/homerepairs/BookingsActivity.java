package com.example.homerepairs;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homerepairs.adapters.BookingCardAdapter;
import com.example.homerepairs.models.Booking;
import com.example.homerepairs.services.FirebaseBookingService;
import com.example.homerepairs.utils.AuthHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.airbnb.lottie.LottieAnimationView;

import java.util.ArrayList;
import java.util.List;

public class BookingsActivity extends AppCompatActivity {

    private RecyclerView rvBookings;
    private TextView tvEmptyState;
    private TextView tvBookingCount;
    private BookingCardAdapter bookingAdapter;
    private FirebaseBookingService bookingService;
    private List<Booking> allBookings = new ArrayList<>();
    private String currentFilter = "All";
    
    // Filter chips
    private Chip chipAll;
    private Chip chipActive;
    private Chip chipScheduled;
    private Chip chipCompleted;
    private FrameLayout llScreenLoading;
    private LottieAnimationView ivScreenLoading;
    private long loadingStartTime = 0;
    private static final long MIN_LOADING_DURATION = 1500; // Minimum 1.5 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookings);

        initializeViews();
        setupRecyclerView();
        setupFilterTabs();
        setupBottomNavigation();
        loadBookings();
        
        // Wait for layout to be ready before hiding loading overlay
        waitForLayoutReady();
    }

    private void initializeViews() {
        rvBookings = findViewById(R.id.rvBookings);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvBookingCount = findViewById(R.id.tvBookingCount);
        chipAll = findViewById(R.id.chipAll);
        chipActive = findViewById(R.id.chipActive);
        chipScheduled = findViewById(R.id.chipScheduled);
        chipCompleted = findViewById(R.id.chipCompleted);
        llScreenLoading = findViewById(R.id.llScreenLoading);
        ivScreenLoading = findViewById(R.id.ivScreenLoading);
        
        bookingService = new FirebaseBookingService();
        
        // Filter button click handler
        findViewById(R.id.btnFilter).setOnClickListener(v -> {
            // Show filter dialog or bottom sheet (can be implemented later)
            android.widget.Toast.makeText(this, "Filter options coming soon", android.widget.Toast.LENGTH_SHORT).show();
        });
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvBookings.setLayoutManager(layoutManager);

        bookingAdapter = new BookingCardAdapter(
                new ArrayList<>(),
                new BookingCardAdapter.OnBookingActionListener() {
                    @Override
                    public void onBookAgain(Booking booking) {
                        // Navigate to provider profile or new booking
                            Intent intent = new Intent(BookingsActivity.this, ProviderProfileActivity.class);
                        if (booking.getProviderName() != null) {
                            intent.putExtra("provider_name", booking.getProviderName());
                        }
                            startActivity(intent);
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                        }

                    @Override
                    public void onReview(Booking booking) {
                        // Navigate to review screen
                        Intent intent = new Intent(BookingsActivity.this, ReviewConfirmActivity.class);
                        intent.putExtra("booking_id", booking.getId());
                        intent.putExtra("provider_name", booking.getProviderName());
                        startActivity(intent);
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    }

                    @Override
                    public void onCallNow(Booking booking) {
                        // Make phone call
                        if (booking.getPhoneNumber() != null && !booking.getPhoneNumber().isEmpty()) {
                            Intent intent = new Intent(Intent.ACTION_DIAL);
                            intent.setData(android.net.Uri.parse("tel:" + booking.getPhoneNumber()));
                            startActivity(intent);
                        } else {
                            android.widget.Toast.makeText(BookingsActivity.this, "Phone number not available", android.widget.Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onTrack(Booking booking) {
                        // Navigate to tracking screen or booking details
                        Intent intent = new Intent(BookingsActivity.this, BookingDetailsActivity.class);
                        intent.putExtra("booking_id", booking.getId());
                            startActivity(intent);
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                        }

                    @Override
                    public void onViewDetails(Booking booking) {
                        // Navigate to booking details
                        Intent intent = new Intent(BookingsActivity.this, BookingDetailsActivity.class);
                        intent.putExtra("booking_id", booking.getId());
                        startActivity(intent);
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    }
                }
        );
        rvBookings.setAdapter(bookingAdapter);
    }

    private void setupFilterTabs() {
        chipAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "All";
                updateFilterSelection();
                filterBookings("All");
            }
        });

        chipActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "Active";
                updateFilterSelection();
                filterBookings("Active");
            }
        });

        chipScheduled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "Scheduled";
                updateFilterSelection();
                filterBookings("Scheduled");
            }
        });

        chipCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "Completed";
                updateFilterSelection();
                filterBookings("Completed");
            }
        });
    }

    private void updateFilterSelection() {
        chipAll.setChecked(currentFilter.equals("All"));
        chipActive.setChecked(currentFilter.equals("Active"));
        chipScheduled.setChecked(currentFilter.equals("Scheduled"));
        chipCompleted.setChecked(currentFilter.equals("Completed"));

        // Update chip styles
        updateChipStyle(chipAll, currentFilter.equals("All"));
        updateChipStyle(chipActive, currentFilter.equals("Active"));
        updateChipStyle(chipScheduled, currentFilter.equals("Scheduled"));
        updateChipStyle(chipCompleted, currentFilter.equals("Completed"));
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

    private void filterBookings(String filter) {
        List<Booking> filteredBookings = new ArrayList<>();
        
        for (Booking booking : allBookings) {
            String status = booking.getStatus() != null ? booking.getStatus() : "Pending";
            
            switch (filter) {
                case "All":
                    filteredBookings.add(booking);
                    break;
                case "Active":
                    // Active includes In Progress and Confirmed bookings
                    if (status.equalsIgnoreCase("In Progress") || status.equalsIgnoreCase("Confirmed")) {
                        filteredBookings.add(booking);
                    }
                    break;
                case "Scheduled":
                    if (status.equalsIgnoreCase("Scheduled")) {
                        filteredBookings.add(booking);
                    }
                    break;
                case "Completed":
                    if (status.equalsIgnoreCase("Completed")) {
                        filteredBookings.add(booking);
                    }
                    break;
            }
        }
        
        bookingAdapter.updateBookings(filteredBookings);
        updateBookingCount(filteredBookings.size());
        
        if (filteredBookings.isEmpty()) {
            rvBookings.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);
            } else {
            rvBookings.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.GONE);
            }
    }

    private void loadBookings() {
        String userId = AuthHelper.getCurrentUserId(this);
        if (userId == null || userId.isEmpty()) {
            // Use sample data for testing
            allBookings = getSampleBookings();
            updateBookingCount(allBookings.size());
            updateFilterChipCounts();
            filterBookings(currentFilter);
            return;
        }

        bookingService.getBookingsByUserId(userId, new FirebaseBookingService.BookingListCallback() {
            @Override
            public void onSuccess(List<Booking> bookings) {
                if (bookings == null || bookings.isEmpty()) {
                    // Use sample data if no bookings found
                    allBookings = getSampleBookings();
                } else {
                    allBookings = bookings;
                }
                updateBookingCount(allBookings.size());
                updateFilterChipCounts();
                filterBookings(currentFilter);
            }

            @Override
            public void onError(String error) {
                android.util.Log.e("BookingsActivity", "Error loading bookings: " + error);
                // Use sample data on error
                allBookings = getSampleBookings();
                updateBookingCount(allBookings.size());
                updateFilterChipCounts();
                filterBookings(currentFilter);
            }
        });
    }

    private List<Booking> getSampleBookings() {
        List<Booking> sampleBookings = new ArrayList<>();
        java.util.Calendar calendar = java.util.Calendar.getInstance();

        // Sample 1: Completed - Plumbing Service
        Booking booking1 = new Booking();
        booking1.setId("sample1");
        booking1.setServiceCategory("Plumbing");
        booking1.setServiceName("Plumbing Service");
        booking1.setProviderName("Parta Wiliama");
        booking1.setStatus("Completed");
        booking1.setBookingReference("BK001");
        calendar.add(java.util.Calendar.DAY_OF_MONTH, -3);
        booking1.setCreatedAt(calendar.getTime());
        sampleBookings.add(booking1);

        // Sample 2: In Progress - Electrical Work
        Booking booking2 = new Booking();
        booking2.setId("sample2");
        booking2.setServiceCategory("Electrical");
        booking2.setServiceName("Electrical Work");
        booking2.setProviderName("Sk Thompson");
        booking2.setStatus("In Progress");
        booking2.setBookingReference("BK002");
        calendar = java.util.Calendar.getInstance();
        calendar.add(java.util.Calendar.MINUTE, -20);
        booking2.setCreatedAt(calendar.getTime());
        sampleBookings.add(booking2);

        // Sample 3: Completed - HVAC Service
        Booking booking3 = new Booking();
        booking3.setId("sample3");
        booking3.setServiceCategory("HVAC");
        booking3.setServiceName("HVAC Service");
        booking3.setProviderName("John Smith");
        booking3.setStatus("Completed");
        booking3.setBookingReference("BK003");
        calendar = java.util.Calendar.getInstance();
        calendar.add(java.util.Calendar.DAY_OF_MONTH, -3);
        booking3.setCreatedAt(calendar.getTime());
        sampleBookings.add(booking3);

        // Sample 4: Scheduled - Cleaning Service
        Booking booking4 = new Booking();
        booking4.setId("sample4");
        booking4.setServiceCategory("Cleaning");
        booking4.setServiceName("Deep Cleaning");
        booking4.setProviderName("Janet Johnson");
        booking4.setStatus("Scheduled");
        booking4.setBookingReference("BK004");
        calendar = java.util.Calendar.getInstance();
        booking4.setCreatedAt(calendar.getTime());
        sampleBookings.add(booking4);

        // Sample 5: Completed - Carpentry
        Booking booking5 = new Booking();
        booking5.setId("sample5");
        booking5.setServiceCategory("Carpentry");
        booking5.setServiceName("Cabinet Installation");
        booking5.setProviderName("Mike Davis");
        booking5.setStatus("Completed");
        booking5.setBookingReference("BK005");
        calendar = java.util.Calendar.getInstance();
        calendar.add(java.util.Calendar.DAY_OF_MONTH, -7);
        booking5.setCreatedAt(calendar.getTime());
        sampleBookings.add(booking5);

        // Sample 6: Scheduled - Painting
        Booking booking6 = new Booking();
        booking6.setId("sample6");
        booking6.setServiceCategory("Painting");
        booking6.setServiceName("Interior Painting");
        booking6.setProviderName("Emily Brown");
        booking6.setStatus("Scheduled");
        booking6.setBookingReference("BK006");
        calendar = java.util.Calendar.getInstance();
        booking6.setCreatedAt(calendar.getTime());
        sampleBookings.add(booking6);

        // Sample 7: In Progress - Appliance Repair
        Booking booking7 = new Booking();
        booking7.setId("sample7");
        booking7.setServiceCategory("Appliance Repair");
        booking7.setServiceName("Refrigerator Repair");
        booking7.setProviderName("David Wilson");
        booking7.setStatus("In Progress");
        booking7.setBookingReference("BK007");
        calendar = java.util.Calendar.getInstance();
        calendar.add(java.util.Calendar.HOUR_OF_DAY, -2);
        booking7.setCreatedAt(calendar.getTime());
        sampleBookings.add(booking7);

        // Sample 8: In Progress - Additional (to match Active count of 3)
        Booking booking8 = new Booking();
        booking8.setId("sample8");
        booking8.setServiceCategory("Plumbing");
        booking8.setServiceName("Pipe Repair");
        booking8.setProviderName("Robert Lee");
        booking8.setStatus("In Progress");
        booking8.setBookingReference("BK008");
        calendar = java.util.Calendar.getInstance();
        calendar.add(java.util.Calendar.HOUR_OF_DAY, -1);
        booking8.setCreatedAt(calendar.getTime());
        sampleBookings.add(booking8);

        return sampleBookings;
    }

    private void updateBookingCount(int count) {
        tvBookingCount.setText(count + " total bookings");
    }

    private void updateFilterChipCounts() {
        int allCount = allBookings.size();
        int activeCount = 0;
        int scheduledCount = 0;
        int completedCount = 0;

        for (Booking booking : allBookings) {
            String status = booking.getStatus() != null ? booking.getStatus() : "Pending";
            // Active includes In Progress and Confirmed
            if (status.equalsIgnoreCase("In Progress") || status.equalsIgnoreCase("Confirmed")) {
                activeCount++;
            }
            if (status.equalsIgnoreCase("Scheduled")) {
                scheduledCount++;
            }
            if (status.equalsIgnoreCase("Completed")) {
                completedCount++;
            }
        }

        chipAll.setText("All (" + allCount + ")");
        chipActive.setText("Active (" + activeCount + ")");
        chipScheduled.setText("Scheduled (" + scheduledCount + ")");
        chipCompleted.setText("Completed (" + completedCount + ")");
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
                return true;
            } else if (itemId == R.id.nav_messages) {
                showScreenLoading(true);
                Intent intent = new Intent(this, MessagesActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
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
        bottomNavigation.setSelectedItemId(R.id.nav_bookings);
    }

    /**
     * Show or hide screen transition loading overlay
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
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> hideScreenLoading(), MIN_LOADING_DURATION);
            return;
        }
        
        // Wait for layout to be measured and laid out
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
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
