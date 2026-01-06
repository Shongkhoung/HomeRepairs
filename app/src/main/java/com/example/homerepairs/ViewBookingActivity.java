package com.example.homerepairs;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.homerepairs.models.Booking;
import com.example.homerepairs.services.FirebaseBookingService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class ViewBookingActivity extends AppCompatActivity {
    private static final String TAG = "ViewBookingActivity";

    // UI Components
    private ImageView btnBack, ivServiceIcon;
    private TextView tvBookingReference, tvServiceName, tvServiceCategory;
    private TextView tvIssueDescription, tvProviderName, tvPhoneNumber;
    private TextView tvServiceDate, tvServiceTime, tvAddress;
    private Chip chipStatus;
    private MaterialCardView cardServiceIcon;
    private MaterialButton btnCallProvider, btnCancelBooking;

    // Data
    private FirebaseBookingService bookingService;
    private Booking currentBooking;
    private String bookingId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_booking);

        // Initialize service
        bookingService = new FirebaseBookingService();

        // Get booking ID from intent
        bookingId = getIntent().getStringExtra("booking_id");
        if (bookingId == null || bookingId.isEmpty()) {
            Toast.makeText(this, "Invalid booking ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupClickListeners();
        Log.d(TAG, "Starting to load booking details for ID: " + bookingId);
        loadBookingDetails();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        ivServiceIcon = findViewById(R.id.ivServiceIcon);
        tvBookingReference = findViewById(R.id.tvBookingReference);
        tvServiceName = findViewById(R.id.tvServiceName);
        tvServiceCategory = findViewById(R.id.tvServiceCategory);
        tvIssueDescription = findViewById(R.id.tvIssueDescription);
        tvProviderName = findViewById(R.id.tvProviderName);
        tvPhoneNumber = findViewById(R.id.tvPhoneNumber);
        tvServiceDate = findViewById(R.id.tvServiceDate);
        tvServiceTime = findViewById(R.id.tvServiceTime);
        tvAddress = findViewById(R.id.tvAddress);
        chipStatus = findViewById(R.id.chipStatus);
        cardServiceIcon = findViewById(R.id.cardServiceIcon);
        btnCallProvider = findViewById(R.id.btnCallProvider);
        btnCancelBooking = findViewById(R.id.btnCancelBooking);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnCallProvider.setOnClickListener(v -> {
            if (currentBooking != null && currentBooking.getPhoneNumber() != null) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + currentBooking.getPhoneNumber()));
                startActivity(intent);
            } else {
                Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancelBooking.setOnClickListener(v -> showDeleteConfirmation());
    }

    private void loadBookingDetails() {
        bookingService.getBookingById(bookingId, new FirebaseBookingService.BookingCallback() {
            @Override
            public void onSuccess(Booking booking) {
                currentBooking = booking;
                displayBookingDetails(booking);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading booking: " + error);
                Toast.makeText(ViewBookingActivity.this,
                        "Failed to load booking details: " + error,
                        Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void displayBookingDetails(Booking booking) {
        // Booking Reference
        tvBookingReference
                .setText("#" + (booking.getBookingReference() != null ? booking.getBookingReference() : "N/A"));

        // Status
        String status = booking.getStatus() != null ? booking.getStatus() : "Pending";
        chipStatus.setText(status);
        setStatusColor(status);

        // Service Information
        String serviceName = booking.getServiceName() != null ? booking.getServiceName()
                : (booking.getServiceCategory() != null ? booking.getServiceCategory() + " Service" : "Service");
        tvServiceName.setText(serviceName);

        tvServiceCategory.setText(booking.getServiceCategory() != null ? booking.getServiceCategory() : "General");

        // Set service icon
        int iconResId = getServiceIcon(booking.getServiceCategory());
        ivServiceIcon.setImageResource(iconResId);

        // Issue Description
        tvIssueDescription.setText(
                booking.getIssueDescription() != null ? booking.getIssueDescription() : "No description provided");

        // Provider Information
        tvProviderName.setText(booking.getProviderName() != null ? booking.getProviderName() : "Provider");

        tvPhoneNumber.setText(booking.getPhoneNumber() != null ? booking.getPhoneNumber() : "Not available");

        // Schedule
        String dateTime = booking.getServiceDateTime();
        if (dateTime != null && dateTime.contains(", ")) {
            String[] parts = dateTime.split(", ");
            tvServiceDate.setText(parts[0]);
            tvServiceTime.setText(parts[1]);
        } else if (dateTime != null) {
            tvServiceDate.setText(dateTime);
            tvServiceTime.setText("N/A");
        } else {
            tvServiceDate.setText("Not scheduled");
            tvServiceTime.setText("Not specified");
        }

        // Location
        tvAddress
                .setText(booking.getPropertyLocation() != null ? booking.getPropertyLocation() : "No address provided");

        // Update button visibility based on status
        updateButtonsForStatus(status);
    }

    private void setStatusColor(String status) {
        String statusLower = status.toLowerCase();
        if (statusLower.equals("completed")) {
            chipStatus.setChipBackgroundColorResource(R.color.success);
        } else if (statusLower.equals("in progress")) {
            chipStatus.setChipBackgroundColorResource(R.color.warning);
        } else if (statusLower.equals("confirmed") || statusLower.equals("scheduled")) {
            chipStatus.setChipBackgroundColorResource(R.color.primary_blue);
        } else if (statusLower.equals("cancelled")) {
            chipStatus.setChipBackgroundColorResource(R.color.error);
        } else {
            chipStatus.setChipBackgroundColorResource(R.color.text_secondary);
        }
    }

    private int getServiceIcon(String category) {
        if (category == null)
            return R.drawable.handyman;

        switch (category.toLowerCase()) {
            case "plumbing":
                return R.drawable.plumbing;
            case "electrical":
            case "electrical work":
                return R.drawable.electrical;
            case "hvac":
                return R.drawable.hvac;
            case "cleaning":
                return R.drawable.cleaning;
            case "carpentry":
                return R.drawable.carpentry;
            case "painting":
                return R.drawable.painting;
            case "roofing":
                return R.drawable.roofing;
            case "landscaping":
                return R.drawable.landscaping;
            case "appliance repair":
                return R.drawable.appliance_repair;
            default:
                return R.drawable.handyman;
        }
    }

    private void updateButtonsForStatus(String status) {
        String statusLower = status.toLowerCase();

        // Hide cancel button for completed or cancelled bookings
        if (statusLower.equals("completed") || statusLower.equals("cancelled")) {
            btnCancelBooking.setVisibility(View.GONE);
        } else {
            btnCancelBooking.setVisibility(View.VISIBLE);
        }
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Booking")
                .setMessage("Are you sure you want to cancel this booking?")
                .setPositiveButton("Yes, Cancel", (dialog, which) -> deleteBooking())
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteBooking() {
        if (currentBooking == null || currentBooking.getId() == null) {
            Toast.makeText(this, "Cannot delete booking: Invalid booking data",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Canceling booking...", Toast.LENGTH_SHORT).show();

        bookingService.deleteBooking(currentBooking.getId(),
                new FirebaseBookingService.BookingCallback() {
                    @Override
                    public void onSuccess(Booking deletedBooking) {
                        Log.d(TAG, "Booking deleted successfully");
                        Toast.makeText(ViewBookingActivity.this,
                                "Booking canceled successfully",
                                Toast.LENGTH_SHORT).show();

                        // Return to previous screen (BookingsActivity)
                        finish();
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Error deleting booking: " + error);
                        Toast.makeText(ViewBookingActivity.this,
                                "Failed to cancel booking: " + error,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
