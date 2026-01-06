package com.example.homerepairs;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Calendar;
import java.util.Random;

public class BookingConfirmedActivity extends AppCompatActivity {

    private TextView tvBookingReference;
    private TextView tvServiceCategory;
    private TextView tvServiceName;
    private TextView tvServiceDateTime;
    private Button btnViewBookingDetails;
    private TextView tvReturnToHome;

    private String providerPhoneNumber = "+1234567890"; // Default phone number
    private String bookingReference;
    private String bookingId;
    private String serviceName;
    private String serviceCategory;
    private String serviceDateTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_confirmed);

        // Get data from intent
        Intent intent = getIntent();
        bookingReference = intent.getStringExtra("bookingReference");
        bookingId = intent.getStringExtra("booking_id");
        serviceName = intent.getStringExtra("serviceName");
        serviceCategory = intent.getStringExtra("serviceCategory");
        serviceDateTime = intent.getStringExtra("serviceDateTime");
        providerPhoneNumber = intent.getStringExtra("providerPhoneNumber");

        // Generate booking reference if not provided
        if (bookingReference == null || bookingReference.isEmpty()) {
            bookingReference = generateBookingReference();
        }

        // Set default values if not provided
        if (serviceName == null || serviceName.isEmpty()) {
            serviceName = "Leaky Faucet Repair";
        }
        if (serviceCategory == null || serviceCategory.isEmpty()) {
            serviceCategory = "Plumbing";
        }
        if (serviceDateTime == null || serviceDateTime.isEmpty()) {
            serviceDateTime = "Tomorrow, 10:00 AM - 11:00 AM";
        }

        initializeViews();
        setupData();
        setupButtons();

    }

    private void initializeViews() {
        tvBookingReference = findViewById(R.id.tvBookingReference);
        tvServiceCategory = findViewById(R.id.tvServiceCategory);
        tvServiceName = findViewById(R.id.tvServiceName);
        tvServiceDateTime = findViewById(R.id.tvServiceDateTime);
        btnViewBookingDetails = findViewById(R.id.btnViewBookingDetails);
    }

    private void setupData() {
        if (bookingReference != null) {
            tvBookingReference.setText("Booking reference: #" + bookingReference);
        }
        tvServiceCategory.setText(serviceCategory);
        tvServiceName.setText(serviceName);
        tvServiceDateTime.setText(serviceDateTime);
    }

    private void setupButtons() {
        // View Booking Details Button
        btnViewBookingDetails.setOnClickListener(v -> {
            animateButtonClick(v);
            viewBookingDetails();
        });

        // Return to Home Button
        Button btnReturnToHome = findViewById(R.id.btnReturnToHome);
        if (btnReturnToHome != null) {
            btnReturnToHome.setOnClickListener(v -> {
                animateButtonClick(v);
                returnToHome();
            });
        }
    }

    private void viewBookingDetails() {
        // Navigate to booking details screen
        Intent intent = new Intent(this, ViewBookingActivity.class); // Updated to ViewBookingActivity
        intent.putExtra("bookingReference", bookingReference);
        intent.putExtra("booking_id", bookingId);
        intent.putExtra("serviceName", serviceName);
        intent.putExtra("serviceType", serviceName); // Map for compatibility
        intent.putExtra("serviceCategory", serviceCategory);
        intent.putExtra("serviceDateTime", serviceDateTime);

        // Split date time for compatibility
        if (serviceDateTime != null && serviceDateTime.contains(",")) {
            int lastCommaIndex = serviceDateTime.lastIndexOf(",");
            if (lastCommaIndex != -1) {
                intent.putExtra("serviceDate", serviceDateTime.substring(0, lastCommaIndex).trim());
                intent.putExtra("serviceTime", serviceDateTime.substring(lastCommaIndex + 1).trim());
            } else {
                intent.putExtra("serviceDate", serviceDateTime);
            }
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

    private void animateButtonClick(View button) {
        float originalScaleX = button.getScaleX();
        float originalScaleY = button.getScaleY();

        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(button, "scaleX", originalScaleX, 0.85f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(button, "scaleY", originalScaleY, 0.85f);

        scaleDownX.setDuration(100);
        scaleDownY.setDuration(100);

        scaleDownX.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(button, "scaleX", 0.85f, originalScaleX);
                ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(button, "scaleY", 0.85f, originalScaleY);

                scaleUpX.setDuration(100);
                scaleUpY.setDuration(100);

                OvershootInterpolator springInterpolator = new OvershootInterpolator(1.5f);
                scaleUpX.setInterpolator(springInterpolator);
                scaleUpY.setInterpolator(springInterpolator);

                scaleUpX.start();
                scaleUpY.start();
            }
        });

        scaleDownX.start();
        scaleDownY.start();
    }

    private String generateBookingReference() {
        Random random = new Random();
        long reference = 1000000000L + random.nextInt(900000000);
        return String.valueOf(reference);
    }
}
