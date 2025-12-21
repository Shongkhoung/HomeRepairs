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
    private Button btnCall;
    private Button btnMessage;
    private Button btnAddToCalendar;
    private Button btnViewBookingDetails;
    private Button btnBookAnotherService;
    private TextView tvReturnToHome;

    private String providerPhoneNumber = "+1234567890"; // Default phone number
    private String bookingReference;
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
        setupBackButton();
        setupButtons();
        setupBottomNavigation();
    }

    private void initializeViews() {
        tvBookingReference = findViewById(R.id.tvBookingReference);
        tvServiceCategory = findViewById(R.id.tvServiceCategory);
        tvServiceName = findViewById(R.id.tvServiceName);
        tvServiceDateTime = findViewById(R.id.tvServiceDateTime);
        btnCall = findViewById(R.id.btnCall);
        btnMessage = findViewById(R.id.btnMessage);
        btnAddToCalendar = findViewById(R.id.btnAddToCalendar);
        btnViewBookingDetails = findViewById(R.id.btnViewBookingDetails);
        btnBookAnotherService = findViewById(R.id.btnBookAnotherService);
        tvReturnToHome = findViewById(R.id.tvReturnToHome);
    }

    private void setupData() {
        tvBookingReference.setText("Booking reference: #" + bookingReference);
        tvServiceCategory.setText(serviceCategory);
        tvServiceName.setText(serviceName);
        tvServiceDateTime.setText(serviceDateTime);
    }

    private void setupBackButton() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            animateButtonClick(v);
            finish();
        });
    }

    private void setupButtons() {
        // Call Button
        btnCall.setOnClickListener(v -> {
            animateButtonClick(v);
            makePhoneCall();
        });

        // Message Button
        btnMessage.setOnClickListener(v -> {
            animateButtonClick(v);
            sendMessage();
        });

        // Add to Calendar Button
        btnAddToCalendar.setOnClickListener(v -> {
            animateButtonClick(v);
            addToCalendar();
        });

        // View Booking Details Button
        btnViewBookingDetails.setOnClickListener(v -> {
            animateButtonClick(v);
            viewBookingDetails();
        });

        // Book Another Service Button
        btnBookAnotherService.setOnClickListener(v -> {
            animateButtonClick(v);
            bookAnotherService();
        });

        // Return to Home Link
        tvReturnToHome.setOnClickListener(v -> {
            returnToHome();
        });
    }

    private void makePhoneCall() {
        Intent callIntent = new Intent(Intent.ACTION_DIAL);
        callIntent.setData(Uri.parse("tel:" + providerPhoneNumber));
        try {
            startActivity(callIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to make call", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendMessage() {
        Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
        smsIntent.setData(Uri.parse("smsto:" + providerPhoneNumber));
        smsIntent.putExtra("sms_body", "Hello, I have a booking for " + serviceName + " (Reference: #" + bookingReference + ")");
        try {
            startActivity(smsIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to send message", Toast.LENGTH_SHORT).show();
        }
    }

    private void addToCalendar() {
        try {
            Intent calendarIntent = new Intent(Intent.ACTION_INSERT);
            calendarIntent.setData(CalendarContract.Events.CONTENT_URI);

            // Parse date and time from serviceDateTime
            // Format: "Tomorrow, 10:00 AM - 11:00 AM"
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_YEAR, 1); // Tomorrow
            calendar.set(Calendar.HOUR_OF_DAY, 10);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);

            long startTime = calendar.getTimeInMillis();
            calendar.add(Calendar.HOUR, 1); // 1 hour duration
            long endTime = calendar.getTimeInMillis();

            calendarIntent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime);
            calendarIntent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime);
            calendarIntent.putExtra(CalendarContract.Events.TITLE, serviceName);
            calendarIntent.putExtra(CalendarContract.Events.DESCRIPTION, 
                "Service: " + serviceName + "\n" +
                "Category: " + serviceCategory + "\n" +
                "Booking Reference: #" + bookingReference);
            calendarIntent.putExtra(CalendarContract.Events.EVENT_LOCATION, "Service Location");

            startActivity(calendarIntent);
            Toast.makeText(this, "Adding to calendar...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Unable to add to calendar", Toast.LENGTH_SHORT).show();
        }
    }

    private void viewBookingDetails() {
        // Navigate to booking details screen
        Intent intent = new Intent(this, BookingDetailsActivity.class);
        intent.putExtra("bookingReference", bookingReference);
        intent.putExtra("serviceName", serviceName);
        intent.putExtra("serviceCategory", serviceCategory);
        intent.putExtra("serviceDateTime", serviceDateTime);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void bookAnotherService() {
        // Navigate back to main activity or service selection
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        finish();
    }

    private void returnToHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        finish();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish();
                return true;
            } else if (itemId == R.id.nav_bookings) {
                Intent intent = new Intent(this, BookingsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
                return true;
            } else if (itemId == R.id.nav_messages) {
                Intent intent = new Intent(this, MessagesActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
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

