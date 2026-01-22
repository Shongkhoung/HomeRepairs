package com.example.homerepairs;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.homerepairs.databinding.ActivityViewBookingBinding;
import com.example.homerepairs.models.Booking;
import com.example.homerepairs.services.FirebaseBookingService;

public class ViewBookingActivity extends BaseActivity {
    private static final String TAG = "ViewBookingActivity";

    private ActivityViewBookingBinding binding;
    private FirebaseBookingService bookingService;
    private Booking currentBooking;
    private String bookingId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityViewBookingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        bookingService = new FirebaseBookingService();
        bookingId = getIntent().getStringExtra("booking_id");

        if (bookingId == null || bookingId.isEmpty()) {
            Toast.makeText(this, "Invalid ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupListeners();
        loadDetails();
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnCallProvider.setOnClickListener(v -> {
            if (currentBooking != null && currentBooking.getPhoneNumber() != null) {
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + currentBooking.getPhoneNumber())));
            } else {
                Toast.makeText(this, "Missing phone", Toast.LENGTH_SHORT).show();
            }
        });
        binding.btnCancelBooking.setOnClickListener(v -> confirmCancel());
    }

    private void loadDetails() {
        bookingService.getBookingById(bookingId, new FirebaseBookingService.BookingCallback() {
            @Override
            public void onSuccess(Booking b) {
                currentBooking = b;
                display(b);
            }

            @Override
            public void onError(String e) {
                Toast.makeText(ViewBookingActivity.this, "Error: " + e, Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void display(Booking b) {
        binding.tvBookingReference.setText("#" + (b.getBookingReference() != null ? b.getBookingReference() : "N/A"));

        String status = b.getStatus() != null ? b.getStatus() : "Pending";
        binding.chipStatus.setText(status);
        setStatusStyle(status);

        binding.tvServiceName.setText(b.getServiceName() != null ? b.getServiceName()
                : (b.getServiceCategory() != null ? b.getServiceCategory() : "Service"));
        binding.tvServiceCategory.setText(b.getServiceCategory() != null ? b.getServiceCategory() : "General");
        binding.ivServiceIcon.setImageResource(getIcon(b.getServiceCategory()));
        binding.tvIssueDescription
                .setText(b.getIssueDescription() != null ? b.getIssueDescription() : "No description");
        binding.tvProviderName.setText(b.getProviderName() != null ? b.getProviderName() : "Provider");
        binding.tvPhoneNumber.setText(b.getPhoneNumber() != null ? b.getPhoneNumber() : "N/A");

        if (b.getServiceDateTime() != null && b.getServiceDateTime().contains(", ")) {
            String[] parts = b.getServiceDateTime().split(", ");
            binding.tvServiceDate.setText(parts[0]);
            binding.tvServiceTime.setText(parts[1]);
        } else {
            binding.tvServiceDate.setText(b.getServiceDateTime() != null ? b.getServiceDateTime() : "N/A");
            binding.tvServiceTime.setText("N/A");
        }

        binding.tvAddress.setText(b.getPropertyLocation() != null ? b.getPropertyLocation() : "No address");
        binding.btnCancelBooking
                .setVisibility(status.equalsIgnoreCase("completed") || status.equalsIgnoreCase("cancelled") ? View.GONE
                        : View.VISIBLE);
    }

    private void setStatusStyle(String status) {
        int color;
        switch (status.toLowerCase()) {
            case "completed":
                color = R.color.success;
                break;
            case "in progress":
                color = R.color.warning;
                break;
            case "confirmed":
            case "scheduled":
                color = R.color.primary_blue;
                break;
            case "cancelled":
                color = R.color.error;
                break;
            default:
                color = R.color.text_secondary;
        }
        binding.chipStatus.setChipBackgroundColorResource(color);
    }

    private int getIcon(String cat) {
        if (cat == null)
            return R.drawable.handyman;
        switch (cat.toLowerCase()) {
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

    private void confirmCancel() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Booking")
                .setMessage("Confirm cancellation?")
                .setPositiveButton("Yes", (d, w) -> cancel())
                .setNegativeButton("No", null)
                .show();
    }

    private void cancel() {
        if (currentBooking == null)
            return;
        Toast.makeText(this, "Canceling...", Toast.LENGTH_SHORT).show();
        bookingService.deleteBooking(currentBooking.getId(), new FirebaseBookingService.BookingCallback() {
            @Override
            public void onSuccess(Booking b) {
                Toast.makeText(ViewBookingActivity.this, "Canceled", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String e) {
                Toast.makeText(ViewBookingActivity.this, "Error: " + e, Toast.LENGTH_LONG).show();
            }
        });
    }
}
