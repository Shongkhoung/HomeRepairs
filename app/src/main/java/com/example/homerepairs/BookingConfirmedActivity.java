package com.example.homerepairs;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import com.example.homerepairs.databinding.ActivityBookingConfirmedBinding;

import java.util.Random;

public class BookingConfirmedActivity extends BaseActivity {

    private ActivityBookingConfirmedBinding binding;
    private String bookingReference, bookingId, serviceName, serviceCategory, serviceDateTime, providerName, location;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBookingConfirmedBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        extractIntentData();
        setupUI();
        setupListeners();
    }

    private void extractIntentData() {
        Intent intent = getIntent();
        bookingReference = intent.getStringExtra("bookingReference");
        bookingId = intent.getStringExtra("booking_id");
        serviceName = intent.getStringExtra("serviceName");
        serviceCategory = intent.getStringExtra("serviceCategory");
        serviceDateTime = intent.getStringExtra("serviceDateTime");
        providerName = intent.getStringExtra("providerName");
        location = intent.getStringExtra("location");

        if (bookingReference == null || bookingReference.isEmpty())
            bookingReference = generateBookingReference();
        if (serviceName == null || serviceName.isEmpty())
            serviceName = "Service Request";
        if (providerName == null || providerName.isEmpty())
            providerName = "Service Provider";
        if (location == null || location.isEmpty())
            location = "Home";
        if (serviceDateTime == null || serviceDateTime.isEmpty())
            serviceDateTime = "Tomorrow, 10:00 AM";
    }

    private void setupUI() {
        binding.tvBookingReference.setText(getString(R.string.booking_reference_label, "#" + bookingReference));
        binding.tvBookingIdValue.setText("#" + bookingReference);
        binding.tvServiceProviderDetail.setText(providerName);
        binding.tvLocationDetailValue.setText(location);

        String datePart = serviceDateTime;
        String timePart = "10:00 AM";
        if (serviceDateTime.contains(",")) {
            int idx = serviceDateTime.lastIndexOf(",");
            datePart = serviceDateTime.substring(0, idx).trim();
            timePart = serviceDateTime.substring(idx + 1).trim();
        }
        binding.tvDateValue.setText(datePart);
        binding.tvTimeValue.setText(timePart);
    }

    private void setupListeners() {
        binding.btnViewBookingDetails.setOnClickListener(v -> {
            animateClick(v);
            viewBookingDetails();
        });
        binding.btnReturnToHome.setOnClickListener(v -> {
            animateClick(v);
            returnToHome();
        });
    }

    private void viewBookingDetails() {
        Intent intent = new Intent(this, ViewBookingActivity.class);
        intent.putExtra("bookingReference", bookingReference);
        intent.putExtra("booking_id", bookingId);
        intent.putExtra("serviceName", serviceName);
        intent.putExtra("serviceType", serviceName);
        intent.putExtra("serviceCategory", serviceCategory);
        intent.putExtra("serviceDateTime", serviceDateTime);

        if (serviceDateTime.contains(",")) {
            int idx = serviceDateTime.lastIndexOf(",");
            intent.putExtra("serviceDate", serviceDateTime.substring(0, idx).trim());
            intent.putExtra("serviceTime", serviceDateTime.substring(idx + 1).trim());
        } else {
            intent.putExtra("serviceDate", serviceDateTime);
            intent.putExtra("serviceTime", "");
        }
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void returnToHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        finish();
    }

    private String generateBookingReference() {
        return String.valueOf(1000000000L + new Random().nextInt(900000000));
    }
}
