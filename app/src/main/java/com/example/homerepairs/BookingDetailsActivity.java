package com.example.homerepairs;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class BookingDetailsActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private EditText etLocationDetails;
    private EditText etGateCode;
    private EditText etParkingInfo;
    private EditText etPetInfo;
    private EditText etAdditionalInstructions;
    private Button btnUseCurrentLocation;
    
    // Contact preference views
    private LinearLayout llCallWhenArriving;
    private LinearLayout llRingDoorbell;
    private LinearLayout llTextMessage;
    private View radioCall;
    private View radioDoorbell;
    private View radioText;
    
    private String selectedContactPreference = "doorbell"; // Default to "Ring doorbell"
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_details);

        // Set status bar color to white
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getWindow().getDecorView().setSystemUiVisibility(
                        getWindow().getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                );
            }
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initializeViews();
        setupBackButton();
        setupUseCurrentLocation();
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
        btnUseCurrentLocation = findViewById(R.id.btnUseCurrentLocation);
        
        // Contact preference views
        llCallWhenArriving = findViewById(R.id.llCallWhenArriving);
        llRingDoorbell = findViewById(R.id.llRingDoorbell);
        llTextMessage = findViewById(R.id.llTextMessage);
        radioCall = findViewById(R.id.radioCall);
        radioDoorbell = findViewById(R.id.radioDoorbell);
        radioText = findViewById(R.id.radioText);
    }

    private void setupBackButton() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupUseCurrentLocation() {
        btnUseCurrentLocation.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
            } else {
                getCurrentLocation();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            etLocationDetails.setText("Getting your location...");
            btnUseCurrentLocation.setEnabled(false);
            
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            getAddressFromLocation(location);
                        } else {
                            Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show();
                            etLocationDetails.setText("");
                            btnUseCurrentLocation.setEnabled(true);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error getting location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        etLocationDetails.setText("");
                        btnUseCurrentLocation.setEnabled(true);
                    });
        }
    }

    private void getAddressFromLocation(Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    1
            );
            
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                StringBuilder addressString = new StringBuilder();
                
                // Build address string
                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    if (i > 0) addressString.append(", ");
                    addressString.append(address.getAddressLine(i));
                }
                
                etLocationDetails.setText(addressString.toString());
            } else {
                // Fallback to coordinates
                etLocationDetails.setText(String.format(Locale.getDefault(),
                        "%.6f, %.6f", location.getLatitude(), location.getLongitude()));
            }
        } catch (IOException e) {
            // Fallback to coordinates if geocoding fails
            etLocationDetails.setText(String.format(Locale.getDefault(),
                    "%.6f, %.6f", location.getLatitude(), location.getLongitude()));
        } finally {
            btnUseCurrentLocation.setEnabled(true);
        }
    }

    private void setupContactPreference() {
        // Set default selection to "Ring doorbell"
        updateContactPreferenceSelection("doorbell");

        llCallWhenArriving.setOnClickListener(v -> {
            updateContactPreferenceSelection("call");
        });

        llRingDoorbell.setOnClickListener(v -> {
            updateContactPreferenceSelection("doorbell");
        });

        llTextMessage.setOnClickListener(v -> {
            updateContactPreferenceSelection("text");
        });
    }

    private void updateContactPreferenceSelection(String preference) {
        selectedContactPreference = preference;

        // Reset all backgrounds and radio buttons
        llCallWhenArriving.setBackgroundResource(R.drawable.payment_method_card_background);
        llRingDoorbell.setBackgroundResource(R.drawable.payment_method_card_background);
        llTextMessage.setBackgroundResource(R.drawable.payment_method_card_background);

        radioCall.setBackgroundResource(R.drawable.radio_button_unselected);
        radioDoorbell.setBackgroundResource(R.drawable.radio_button_unselected);
        radioText.setBackgroundResource(R.drawable.radio_button_unselected);

        // Update selected preference
        switch (preference) {
            case "call":
                llCallWhenArriving.setBackgroundResource(R.drawable.payment_method_card_selected);
                radioCall.setBackgroundResource(R.drawable.radio_button_selected);
                selectedContactPreference = "Call when arriving";
                break;
            case "doorbell":
                llRingDoorbell.setBackgroundResource(R.drawable.payment_method_card_selected);
                radioDoorbell.setBackgroundResource(R.drawable.radio_button_selected);
                selectedContactPreference = "Ring doorbell";
                break;
            case "text":
                llTextMessage.setBackgroundResource(R.drawable.payment_method_card_selected);
                radioText.setBackgroundResource(R.drawable.radio_button_selected);
                selectedContactPreference = "Text message";
                break;
        }
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
            String serviceName = currentIntent.getStringExtra("serviceType");
            String serviceCategory = currentIntent.getStringExtra("serviceCategory");
            String serviceDate = currentIntent.getStringExtra("serviceDate");
            String serviceTime = currentIntent.getStringExtra("serviceTime");

            // Navigate to ReviewConfirmActivity
            Intent intent = new Intent(this, ReviewConfirmActivity.class);
            
            // Pass booking information
            if (serviceName != null) intent.putExtra("serviceType", serviceName);
            if (serviceCategory != null) intent.putExtra("serviceCategory", serviceCategory);
            if (serviceDate != null) intent.putExtra("serviceDate", serviceDate);
            if (serviceTime != null) intent.putExtra("serviceTime", serviceTime);
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
