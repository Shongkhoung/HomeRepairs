package com.example.homerepairs;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.homerepairs.databinding.ActivityBookingDetailsBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class BookingDetailsActivity extends BaseActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private ActivityBookingDetailsBinding binding;
    private String selectedContactPreference = "Ring doorbell";
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBookingDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupListeners();
        setupBottomNavigation();
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnUseCurrentLocation.setOnClickListener(v -> checkLocationPermission());

        binding.llCallWhenArriving.setOnClickListener(v -> updateContactPreference("call"));
        binding.llRingDoorbell.setOnClickListener(v -> updateContactPreference("doorbell"));
        binding.llTextMessage.setOnClickListener(v -> updateContactPreference("text"));

        binding.btnContinue.setOnClickListener(v -> validateAndProceed());
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            binding.etLocationDetails.setText("Getting location...");
            binding.btnUseCurrentLocation.setEnabled(false);
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null)
                    getAddress(location);
                else {
                    binding.etLocationDetails.setText("");
                    binding.btnUseCurrentLocation.setEnabled(true);
                }
            }).addOnFailureListener(e -> {
                binding.etLocationDetails.setText("");
                binding.btnUseCurrentLocation.setEnabled(true);
            });
        }
    }

    private void getAddress(Location loc) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address addr = addresses.get(0);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i <= addr.getMaxAddressLineIndex(); i++) {
                    if (i > 0)
                        sb.append(", ");
                    sb.append(addr.getAddressLine(i));
                }
                binding.etLocationDetails.setText(sb.toString());
            } else {
                binding.etLocationDetails.setText(
                        String.format(Locale.getDefault(), "%.6f, %.6f", loc.getLatitude(), loc.getLongitude()));
            }
        } catch (IOException e) {
            binding.etLocationDetails
                    .setText(String.format(Locale.getDefault(), "%.6f, %.6f", loc.getLatitude(), loc.getLongitude()));
        } finally {
            binding.btnUseCurrentLocation.setEnabled(true);
        }
    }

    private void updateContactPreference(String pref) {
        binding.llCallWhenArriving.setBackgroundResource(R.drawable.payment_method_card_background);
        binding.llRingDoorbell.setBackgroundResource(R.drawable.payment_method_card_background);
        binding.llTextMessage.setBackgroundResource(R.drawable.payment_method_card_background);
        binding.radioCall.setBackgroundResource(R.drawable.radio_button_unselected);
        binding.radioDoorbell.setBackgroundResource(R.drawable.radio_button_unselected);
        binding.radioText.setBackgroundResource(R.drawable.radio_button_unselected);

        switch (pref) {
            case "call":
                binding.llCallWhenArriving.setBackgroundResource(R.drawable.payment_method_card_selected);
                binding.radioCall.setBackgroundResource(R.drawable.radio_button_selected);
                selectedContactPreference = "Call when arriving";
                break;
            case "doorbell":
                binding.llRingDoorbell.setBackgroundResource(R.drawable.payment_method_card_selected);
                binding.radioDoorbell.setBackgroundResource(R.drawable.radio_button_selected);
                selectedContactPreference = "Ring doorbell";
                break;
            case "text":
                binding.llTextMessage.setBackgroundResource(R.drawable.payment_method_card_selected);
                binding.radioText.setBackgroundResource(R.drawable.radio_button_selected);
                selectedContactPreference = "Text message";
                break;
        }
    }

    private void validateAndProceed() {
        String loc = binding.etLocationDetails.getText().toString().trim();
        if (loc.isEmpty()) {
            Toast.makeText(this, "Enter location", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, ReviewConfirmActivity.class);
        Intent current = getIntent();
        intent.putExtra("serviceType", current.getStringExtra("serviceType"));
        intent.putExtra("serviceCategory", current.getStringExtra("serviceCategory"));
        intent.putExtra("serviceDate", current.getStringExtra("serviceDate"));
        intent.putExtra("serviceTime", current.getStringExtra("serviceTime"));
        intent.putExtra("location", loc);

        String instr = binding.etAdditionalInstructions.getText().toString().trim();
        if (!instr.isEmpty())
            intent.putExtra("issueDescription", instr);

        intent.putExtra("gateCode", binding.etGateCode.getText().toString().trim());
        intent.putExtra("parkingInfo", binding.etParkingInfo.getText().toString().trim());
        intent.putExtra("petInfo", binding.etPetInfo.getText().toString().trim());
        intent.putExtra("contactPreference", selectedContactPreference);

        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish();
                return true;
            } else if (id == R.id.nav_bookings) {
                startActivity(new Intent(this, BookingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish();
                return true;
            } else if (id == R.id.nav_messages) {
                startActivity(new Intent(this, MessagesActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, UserProfileActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
                return true;
            }
            return false;
        });
        binding.bottomNavigation.setSelectedItemId(R.id.nav_bookings);
    }
}
