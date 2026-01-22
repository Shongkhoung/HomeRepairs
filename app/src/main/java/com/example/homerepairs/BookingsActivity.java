package com.example.homerepairs;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.homerepairs.adapters.BookingCardAdapter;
import com.example.homerepairs.databinding.ActivityBookingsBinding;
import com.example.homerepairs.models.Booking;
import com.example.homerepairs.services.FirebaseBookingService;
import com.example.homerepairs.utils.AuthHelper;
import com.google.android.material.chip.Chip;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.switchmaterial.SwitchMaterial;
import android.widget.RadioGroup;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import java.util.ArrayList;
import java.util.List;

public class BookingsActivity extends BaseActivity {

    private ActivityBookingsBinding binding;
    private BookingCardAdapter bookingAdapter;
    private FirebaseBookingService bookingService;
    private List<Booking> allBookings = new ArrayList<>();
    private String currentFilter = "All";

    // Header Filter State
    private int currentSortId = -1;
    private boolean isEmergencyFilter = false;
    private boolean isSameDayFilter = false;

    private long loadingStartTime = 0;
    private static final long MIN_LOADING_DURATION = 1500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBookingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        bookingService = new FirebaseBookingService();

        setupRecyclerView();
        setupFilterTabs();
        setupBottomNavigation();
        loadBookings();
        setupClickListeners();

        waitForLayoutReady();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBookings();
    }

    private void setupClickListeners() {
        binding.btnFilter.setOnClickListener(v -> showFilterBottomSheet());
    }

    private void setupRecyclerView() {
        binding.rvBookings.setLayoutManager(new LinearLayoutManager(this));
        bookingAdapter = new BookingCardAdapter(new ArrayList<>(), new BookingCardAdapter.OnBookingActionListener() {
            @Override
            public void onBookAgain(Booking booking) {
                Intent intent = new Intent(BookingsActivity.this, ProviderProfileActivity.class);
                if (booking.getProviderName() != null)
                    intent.putExtra("provider_name", booking.getProviderName());
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }

            @Override
            public void onReview(Booking booking) {
                Intent intent = new Intent(BookingsActivity.this, ReviewConfirmActivity.class);
                intent.putExtra("booking_id", booking.getId());
                intent.putExtra("provider_name", booking.getProviderName());
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }

            @Override
            public void onCallNow(Booking booking) {
                if (booking.getPhoneNumber() != null && !booking.getPhoneNumber().isEmpty()) {
                    startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + booking.getPhoneNumber())));
                } else {
                    Toast.makeText(BookingsActivity.this, getString(R.string.phone_not_available), Toast.LENGTH_SHORT)
                            .show();
                }
            }

            @Override
            public void onTrack(Booking booking) {
                Intent intent = new Intent(BookingsActivity.this, BookingDetailsActivity.class);
                intent.putExtra("booking_id", booking.getId());
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }

            @Override
            public void onViewDetails(Booking booking) {
                Intent intent = new Intent(BookingsActivity.this, ViewBookingActivity.class);
                intent.putExtra("booking_id", booking.getId());
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }

            @Override
            public void onDelete(Booking booking) {
                new android.app.AlertDialog.Builder(BookingsActivity.this)
                        .setTitle(getString(R.string.dialog_cancel_booking_title))
                        .setMessage(getString(R.string.dialog_cancel_booking_message))
                        .setPositiveButton(getString(R.string.dialog_yes_cancel),
                                (dialog, which) -> deleteBooking(booking))
                        .setNegativeButton(getString(R.string.dialog_no), null)
                        .show();
            }
        });
        binding.rvBookings.setAdapter(bookingAdapter);
    }

    private void setupFilterTabs() {
        binding.chipAll.setOnCheckedChangeListener((bv, isChecked) -> {
            if (isChecked)
                handleFilterChange("All");
        });
        binding.chipActive.setOnCheckedChangeListener((bv, isChecked) -> {
            if (isChecked)
                handleFilterChange("Active");
        });
        binding.chipScheduled.setOnCheckedChangeListener((bv, isChecked) -> {
            if (isChecked)
                handleFilterChange("Scheduled");
        });
        binding.chipCompleted.setOnCheckedChangeListener((bv, isChecked) -> {
            if (isChecked)
                handleFilterChange("Completed");
        });
    }

    private void handleFilterChange(String filter) {
        currentFilter = filter;
        updateFilterSelection();
        filterBookings(filter);
    }

    private void updateFilterSelection() {
        binding.chipAll.setChecked(currentFilter.equals("All"));
        binding.chipActive.setChecked(currentFilter.equals("Active"));
        binding.chipScheduled.setChecked(currentFilter.equals("Scheduled"));
        binding.chipCompleted.setChecked(currentFilter.equals("Completed"));

        updateChipStyle(binding.chipAll, currentFilter.equals("All"));
        updateChipStyle(binding.chipActive, currentFilter.equals("Active"));
        updateChipStyle(binding.chipScheduled, currentFilter.equals("Scheduled"));
        updateChipStyle(binding.chipCompleted, currentFilter.equals("Completed"));
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
        List<Booking> filtered = new ArrayList<>();

        // 1. Status Filter (Chips)
        for (Booking b : allBookings) {
            String status = b.getStatus() != null ? b.getStatus() : "Pending";
            boolean matchesStatus = false;
            switch (filter) {
                case "All":
                    matchesStatus = true;
                    break;
                case "Active":
                    if (status.equalsIgnoreCase("In Progress") || status.equalsIgnoreCase("Confirmed")
                            || status.equalsIgnoreCase("Pending"))
                        matchesStatus = true;
                    break;
                case "Scheduled":
                    if (status.equalsIgnoreCase("Scheduled"))
                        matchesStatus = true;
                    break;
                case "Completed":
                    if (status.equalsIgnoreCase("Completed"))
                        matchesStatus = true;
                    break;
            }
            if (matchesStatus) {
                filtered.add(b);
            }
        }

        // 2. Urgency Filter (Bottom Sheet)
        // Only apply if user selected at least one urgency filter
        if (isEmergencyFilter || isSameDayFilter) {
            List<Booking> urgencyFiltered = new ArrayList<>();
            for (Booking b : filtered) {
                boolean matchesUrgency = false;
                String urgency = b.getUrgency();
                if (urgency != null) {
                    if (isEmergencyFilter && urgency.equalsIgnoreCase("Emergency"))
                        matchesUrgency = true;
                    if (isSameDayFilter && urgency.equalsIgnoreCase("Same Day"))
                        matchesUrgency = true;
                }

                if (matchesUrgency)
                    urgencyFiltered.add(b);
            }
            filtered = urgencyFiltered;
        }

        // 3. Sorting (Bottom Sheet)
        Collections.sort(filtered, (b1, b2) -> {
            if (currentSortId == R.id.rbOldest) {
                // Oldest First
                return compareDates(b1.getCreatedAt(), b2.getCreatedAt());
            } else if (currentSortId == R.id.rbPriceLow) {
                // Price Low to High
                return Double.compare(b1.getPrice() != null ? b1.getPrice() : 0,
                        b2.getPrice() != null ? b2.getPrice() : 0);
            } else if (currentSortId == R.id.rbPriceHigh) {
                // Price High to Low
                return Double.compare(b2.getPrice() != null ? b2.getPrice() : 0,
                        b1.getPrice() != null ? b1.getPrice() : 0);
            } else {
                // Newest First (Default)
                return compareDates(b2.getCreatedAt(), b1.getCreatedAt());
            }
        });

        bookingAdapter.updateBookings(filtered);
        updateBookingCount(filtered.size());
        binding.rvBookings.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
        binding.tvEmptyState.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private int compareDates(Date d1, Date d2) {
        if (d1 == null && d2 == null)
            return 0;
        if (d1 == null)
            return -1; // Null is "older" than any date (bottom of list if asc, top if desc?) Let's
                       // treat null as very old.
        if (d2 == null)
            return 1;
        return d1.compareTo(d2);
    }

    private void showFilterBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_filter, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        RadioGroup rgSort = bottomSheetView.findViewById(R.id.rgSort);
        SwitchMaterial switchEmergency = bottomSheetView.findViewById(R.id.switchEmergency);
        SwitchMaterial switchSameDay = bottomSheetView.findViewById(R.id.switchSameDay);

        // Restore state
        if (currentSortId != -1) {
            rgSort.check(currentSortId);
        } else {
            rgSort.check(R.id.rbNewest); // Default
        }

        switchEmergency.setChecked(isEmergencyFilter);
        switchSameDay.setChecked(isSameDayFilter);

        // Apply Button
        bottomSheetView.findViewById(R.id.btnApplyFilters).setOnClickListener(v -> {
            currentSortId = rgSort.getCheckedRadioButtonId();
            isEmergencyFilter = switchEmergency.isChecked();
            isSameDayFilter = switchSameDay.isChecked();

            handleFilterChange(currentFilter); // Re-run filter logic
            bottomSheetDialog.dismiss();
        });

        // Reset Button
        bottomSheetView.findViewById(R.id.btnReset).setOnClickListener(v -> {
            currentSortId = R.id.rbNewest;
            isEmergencyFilter = false;
            isSameDayFilter = false;

            rgSort.check(currentSortId);
            switchEmergency.setChecked(false);
            switchSameDay.setChecked(false);
        });

        bottomSheetDialog.show();
    }

    private void loadBookings() {
        String userId = AuthHelper.getCurrentUserId(this);
        if (userId == null || userId.isEmpty()) {
            allBookings = new ArrayList<>();
            updateUI();
            return;
        }

        bookingService.getBookingsByUserId(userId, new FirebaseBookingService.BookingListCallback() {
            @Override
            public void onSuccess(List<Booking> bookings) {
                allBookings = bookings != null ? bookings : new ArrayList<>();
                updateUI();
            }

            @Override
            public void onError(String error) {
                Log.e("BookingsActivity", "Error loading bookings: " + error);
                allBookings = new ArrayList<>();
                updateUI();
            }
        });
    }

    private void updateUI() {
        updateBookingCount(allBookings.size());
        updateFilterChipCounts();
        filterBookings(currentFilter);
    }

    private void deleteBooking(Booking booking) {
        if (booking == null || booking.getId() == null)
            return;
        Toast.makeText(this, getString(R.string.msg_canceling_booking), Toast.LENGTH_SHORT).show();
        bookingService.deleteBooking(booking.getId(), new FirebaseBookingService.BookingCallback() {
            @Override
            public void onSuccess(Booking deleted) {
                Toast.makeText(BookingsActivity.this, getString(R.string.msg_booking_canceled), Toast.LENGTH_SHORT)
                        .show();
                allBookings.remove(booking);
                updateUI();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(BookingsActivity.this, getString(R.string.error_cancel_booking, error),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateBookingCount(int count) {
        binding.tvBookingCount.setText(getString(R.string.booking_count_format, count));
    }

    private void updateFilterChipCounts() {
        int all = allBookings.size(), active = 0, scheduled = 0, completed = 0;
        for (Booking b : allBookings) {
            String s = b.getStatus() != null ? b.getStatus() : "Pending";
            if (s.equalsIgnoreCase("In Progress") || s.equalsIgnoreCase("Confirmed") || s.equalsIgnoreCase("Pending"))
                active++;
            if (s.equalsIgnoreCase("Scheduled"))
                scheduled++;
            if (s.equalsIgnoreCase("Completed"))
                completed++;
        }
        binding.chipAll.setText(getString(R.string.filter_all_count, all));
        binding.chipActive.setText(getString(R.string.filter_active_count, active));
        binding.chipScheduled.setText(getString(R.string.filter_scheduled_count, scheduled));
        binding.chipCompleted.setText(getString(R.string.filter_completed_count, completed));
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
            } else if (id == R.id.nav_bookings)
                return true;
            else if (id == R.id.nav_messages) {
                showScreenLoading(true);
                startActivity(new Intent(this, MessagesActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                showScreenLoading(true);
                startActivity(new Intent(this, UserProfileActivity.class));
                finish();
                return true;
            }
            return false;
        });
        binding.bottomNavigation.setSelectedItemId(R.id.nav_bookings);
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
