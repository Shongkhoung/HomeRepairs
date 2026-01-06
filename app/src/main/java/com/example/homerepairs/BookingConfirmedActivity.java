package com.example.homerepairs;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
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

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Calendar;
import java.util.Random;

public class BookingConfirmedActivity extends AppCompatActivity {

    // Views
    private TextView tvBookingReference;
    private TextView tvServiceCategory;
    private TextView tvServiceName;
    private TextView tvServiceDateTime;
    private Button btnCall;
    private Button btnMessage;
    private TextView btnAddToCalendar;
    private Button btnViewBookingDetails;
    private TextView btnBookAnotherService; // TextView styled as button
    private TextView tvReturnToHome;        // TextView styled as button

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

        // Fallbacks
        if (bookingReference == null || bookingReference.isEmpty()) {
            bookingReference = generateBookingReference();
        }
        if (serviceName == null || serviceName.isEmpty()) serviceName = "Leaky Faucet Repair";
        if (serviceCategory == null || serviceCategory.isEmpty()) serviceCategory = "Plumbing";
        if (serviceDateTime == null || serviceDateTime.isEmpty()) serviceDateTime = "Tomorrow, 10:00 AM - 11:00 AM";
        if (providerPhoneNumber == null || providerPhoneNumber.isEmpty()) providerPhoneNumber = "+1234567890";

        initializeViews();
        setupData();
        setupBackButton();
        setupButtons();
        setupBottomNavigation();
    }

    private void initializeViews() {
//        tvBookingReference = findViewById(R.id.tvBookingReference);
        tvServiceCategory = findViewById(R.id.tvServiceCategory);
        tvServiceName = findViewById(R.id.tvServiceName);
        tvServiceDateTime = findViewById(R.id.tvServiceDateTime);

        btnCall = findViewById(R.id.btnCall);
        btnMessage = findViewById(R.id.btnMessage);
        btnAddToCalendar = findViewById(R.id.btnAddToCalendar);
        btnViewBookingDetails = findViewById(R.id.btnViewBookingDetails);

        btnBookAnotherService = findViewById(R.id.btnBookAnotherService); // TextView
        tvReturnToHome = findViewById(R.id.tvReturnToHome);               // TextView
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
        btnCall.setOnClickListener(v -> {
            animateButtonClick(v);
            makePhoneCall();
        });

        btnMessage.setOnClickListener(v -> {
            animateButtonClick(v);
            sendMessage();
        });

        btnAddToCalendar.setOnClickListener(v -> {
            animateButtonClick(v);
            addToCalendar();
        });

        btnViewBookingDetails.setOnClickListener(v -> {
            animateButtonClick(v);
            viewBookingDetails();
        });

        btnBookAnotherService.setOnClickListener(v -> {
            animateButtonClick(v);
            bookAnotherService();
        });

        tvReturnToHome.setOnClickListener(v -> {
            animateButtonClick(v);
            returnToHome();
        });
    }

    private void makePhoneCall() {
        Intent callIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + providerPhoneNumber));
        try { startActivity(callIntent); }
        catch (Exception e) { Toast.makeText(this, "Unable to make call", Toast.LENGTH_SHORT).show(); }
    }

    private void sendMessage() {
        Intent smsIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + providerPhoneNumber));
        smsIntent.putExtra("sms_body", "Hello, I have a booking for " + serviceName + " (Ref: #" + bookingReference + ")");
        try { startActivity(smsIntent); }
        catch (Exception e) { Toast.makeText(this, "Unable to send message", Toast.LENGTH_SHORT).show(); }
    }

    private void addToCalendar() {
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 10);
            calendar.set(Calendar.MINUTE, 0);

            long startTime = calendar.getTimeInMillis();
            calendar.add(Calendar.HOUR, 1);
            long endTime = calendar.getTimeInMillis();

            Intent calendarIntent = new Intent(Intent.ACTION_INSERT)
                    .setData(CalendarContract.Events.CONTENT_URI)
                    .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime)
                    .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime)
                    .putExtra(CalendarContract.Events.TITLE, serviceName)
                    .putExtra(CalendarContract.Events.DESCRIPTION,
                            "Service: " + serviceName + "\nCategory: " + serviceCategory + "\nBooking Ref: #" + bookingReference)
                    .putExtra(CalendarContract.Events.EVENT_LOCATION, "Service Location");

            startActivity(calendarIntent);
            Toast.makeText(this, "Adding to calendar...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Unable to add to calendar", Toast.LENGTH_SHORT).show();
        }
    }

    private void viewBookingDetails() {
        Intent intent = new Intent(this, BookingDetailsActivity.class);
        intent.putExtra("bookingReference", bookingReference);
        intent.putExtra("serviceName", serviceName);
        intent.putExtra("serviceCategory", serviceCategory);
        intent.putExtra("serviceDateTime", serviceDateTime);
        startActivity(intent);
    }

    private void bookAnotherService() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void returnToHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) startActivity(new Intent(this, MainActivity.class));
            else if (id == R.id.nav_bookings) startActivity(new Intent(this, BookingsActivity.class));
            else if (id == R.id.nav_messages) startActivity(new Intent(this, MessagesActivity.class));
            else if (id == R.id.nav_profile) startActivity(new Intent(this, UserProfileActivity.class));
            return true;
        });
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

                OvershootInterpolator interpolator = new OvershootInterpolator(1.5f);
                scaleUpX.setInterpolator(interpolator);
                scaleUpY.setInterpolator(interpolator);

                scaleUpX.start();
                scaleUpY.start();
            }
        });

        scaleDownX.start();
        scaleDownY.start();
    }

    private String generateBookingReference() {
        Random random = new Random();
        return String.valueOf(1000000000L + (long) random.nextInt(900_000_000));
    }
}
