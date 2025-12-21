package com.example.homerepairs;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class BookingDetailsActivity extends AppCompatActivity {

    private EditText etLocationDetails;
    private EditText etGateCode;
    private EditText etParkingInfo;
    private EditText etPetInfo;
    private EditText etAdditionalInstructions;
    private RadioGroup rgContactPreference;
    private RadioButton rbCallWhenArriving;
    private RadioButton rbRingDoorbell;
    private RadioButton rbTextMessage;
    private String selectedContactPreference = "Call when arriving";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_details);

        initializeViews();
        setupBackButton();
        setupContactPreference();
        setupContinueButton();
        setupBottomNavigation();
    }

    private void initializeViews() {
        etLocationDetails = findViewById(R.id.etLocationDetails);
        etGateCode = findViewById(R.id.etGateCode);
        etParkingInfo = findViewById(R.id.etParkingInfo);
        etPetInfo = findViewById(R.id.etPetInfo);
        etAdditionalInstructions = findViewById(R.id.etAdditionalInstructions);
        rgContactPreference = findViewById(R.id.rgContactPreference);
        rbCallWhenArriving = findViewById(R.id.rbCallWhenArriving);
        rbRingDoorbell = findViewById(R.id.rbRingDoorbell);
        rbTextMessage = findViewById(R.id.rbTextMessage);
    }

    private void setupBackButton() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupContactPreference() {
        rgContactPreference.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbCallWhenArriving) {
                selectedContactPreference = "Call when arriving";
            } else if (checkedId == R.id.rbRingDoorbell) {
                selectedContactPreference = "Ring doorbell";
            } else if (checkedId == R.id.rbTextMessage) {
                selectedContactPreference = "Text message";
            }
        });
    }

    private void setupContinueButton() {
        Button btnContinue = findViewById(R.id.btnContinue);
        btnContinue.setOnClickListener(v -> {
            // Validate required fields
            String locationDetails = etLocationDetails.getText().toString().trim();
            
            if (locationDetails.isEmpty()) {
                Toast.makeText(this, "Please enter location details", Toast.LENGTH_SHORT).show();
                return;
            }

            // Collect all information
            String gateCode = etGateCode.getText().toString().trim();
            String parkingInfo = etParkingInfo.getText().toString().trim();
            String petInfo = etPetInfo.getText().toString().trim();
            String additionalInstructions = etAdditionalInstructions.getText().toString().trim();

            // Get booking data from intent
            Intent currentIntent = getIntent();
            String bookingReference = currentIntent.getStringExtra("bookingReference");
            String serviceName = currentIntent.getStringExtra("serviceName");
            String serviceCategory = currentIntent.getStringExtra("serviceCategory");
            String serviceDateTime = currentIntent.getStringExtra("serviceDateTime");

            // Navigate to ReviewConfirmActivity
            Intent intent = new Intent(this, ReviewConfirmActivity.class);
            
            // Pass booking information
            if (serviceName != null) intent.putExtra("serviceType", serviceName);
            if (serviceCategory != null) intent.putExtra("serviceCategory", serviceCategory);
            if (serviceDateTime != null) {
                // Parse date and time from serviceDateTime
                String[] parts = serviceDateTime.split(", ");
                if (parts.length >= 2) {
                    intent.putExtra("serviceDate", parts[0]);
                    intent.putExtra("serviceTime", parts[1]);
                }
            }
            if (locationDetails != null) intent.putExtra("location", locationDetails);
            if (additionalInstructions != null && !additionalInstructions.isEmpty()) {
                intent.putExtra("issueDescription", additionalInstructions);
            }
            
            // Pass additional details
            intent.putExtra("gateCode", gateCode);
            intent.putExtra("parkingInfo", parkingInfo);
            intent.putExtra("petInfo", petInfo);
            intent.putExtra("contactPreference", selectedContactPreference);
            
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
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
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
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
}

