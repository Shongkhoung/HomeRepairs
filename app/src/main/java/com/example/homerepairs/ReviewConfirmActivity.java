package com.example.homerepairs;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ReviewConfirmActivity extends AppCompatActivity {

    private TextView tvServiceType;
    private TextView tvServiceProvider;
    private TextView tvLocationValue;
    private TextView tvDuration;
    private TextView tvIssueDescription;
    private TextView tvServiceFee;
    private TextView tvBookingFee;
    private TextView tvTaxes;
    private TextView tvTotalEstimate;
    private TextView tvPaymentMethod;
    private CheckBox cbPaymentMethod;
    private CheckBox cbTermsOfService;
    private EditText etPromoCode;
    private LinearLayout llPaymentMethod;
    private Button btnCancel;
    private Button btnConfirmBooking;

    private double serviceFee = 120.0;
    private double bookingFee = 10.0;
    private double taxes = 5.0;
    private double totalEstimate = 135.0;
    private String selectedPaymentMethod = "Visa •••• 42...";
    private boolean promoCodeApplied = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_confirm);

        initializeViews();
        setupData();
        setupPaymentMethod();
        setupPromoCode();
        setupButtons();
        setupBottomNavigation();
    }

    private void initializeViews() {
        tvServiceType = findViewById(R.id.tvServiceType);
        tvServiceProvider = findViewById(R.id.tvServiceProvider);
        tvLocationValue = findViewById(R.id.tvLocationValue);
        tvDuration = findViewById(R.id.tvDuration);
        tvIssueDescription = findViewById(R.id.tvIssueDescription);
        tvServiceFee = findViewById(R.id.tvServiceFee);
        tvBookingFee = findViewById(R.id.tvBookingFee);
        tvTaxes = findViewById(R.id.tvTaxes);
        tvTotalEstimate = findViewById(R.id.tvTotalEstimate);
        tvPaymentMethod = findViewById(R.id.tvPaymentMethod);
        cbPaymentMethod = findViewById(R.id.cbPaymentMethod);
        cbTermsOfService = findViewById(R.id.cbTermsOfService);
        etPromoCode = findViewById(R.id.etPromoCode);
        llPaymentMethod = findViewById(R.id.llPaymentMethod);
        btnCancel = findViewById(R.id.btnCancel);
        btnConfirmBooking = findViewById(R.id.btnConfirmBooking);

        ImageButton btnClose = findViewById(R.id.btnClose);
        btnClose.setOnClickListener(v -> finish());
    }

    private void setupData() {
        // Get data from intent or use defaults
        Intent intent = getIntent();
        
        // Service information
        String serviceType = intent.getStringExtra("serviceType");
        if (serviceType != null) {
            tvServiceType.setText(serviceType);
        }

        String providerName = intent.getStringExtra("providerName");
        String serviceDate = intent.getStringExtra("serviceDate");
        String serviceTime = intent.getStringExtra("serviceTime");
        if (providerName != null && serviceDate != null && serviceTime != null) {
            tvServiceProvider.setText("Service by " + providerName + " · " + serviceDate + ", " + serviceTime);
        } else {
            tvServiceProvider.setText("Service by Alex Bennett · Today, 10:00 AM");
        }

        String location = intent.getStringExtra("location");
        if (location != null) {
            tvLocationValue.setText(location);
        } else {
            tvLocationValue.setText("123 Maple Street, Anytown");
        }

        String duration = intent.getStringExtra("duration");
        if (duration != null) {
            tvDuration.setText(duration);
        } else {
            tvDuration.setText("2 hours");
        }

        String issueDescription = intent.getStringExtra("issueDescription");
        if (issueDescription != null) {
            tvIssueDescription.setText(issueDescription);
        } else {
            tvIssueDescription.setText("Leaky faucet in the kitchen");
        }

        // Pricing
        double serviceFeeValue = intent.getDoubleExtra("serviceFee", serviceFee);
        double bookingFeeValue = intent.getDoubleExtra("bookingFee", bookingFee);
        double taxesValue = intent.getDoubleExtra("taxes", taxes);
        
        serviceFee = serviceFeeValue;
        bookingFee = bookingFeeValue;
        taxes = taxesValue;
        totalEstimate = serviceFee + bookingFee + taxes;

        tvServiceFee.setText("$" + String.format("%.0f", serviceFee));
        tvBookingFee.setText("$" + String.format("%.0f", bookingFee));
        tvTaxes.setText("$" + String.format("%.0f", taxes));
        tvTotalEstimate.setText("$" + String.format("%.0f", totalEstimate));
    }

    private void setupPaymentMethod() {
        llPaymentMethod.setOnClickListener(v -> {
            animateButtonClick(v);
            showPaymentMethodDialog();
        });
    }

    private void showPaymentMethodDialog() {
        String[] paymentMethods = {
            "Visa •••• 42...",
            "Mastercard •••• 56...",
            "American Express •••• 78...",
            "Add new payment method"
        };

        new AlertDialog.Builder(this)
                .setTitle("Select Payment Method")
                .setItems(paymentMethods, (dialog, which) -> {
                    if (which == paymentMethods.length - 1) {
                        // Add new payment method
                        Toast.makeText(this, "Add new payment method feature coming soon", Toast.LENGTH_SHORT).show();
                    } else {
                        selectedPaymentMethod = paymentMethods[which];
                        tvPaymentMethod.setText(selectedPaymentMethod);
                        cbPaymentMethod.setChecked(true);
                    }
                })
                .show();
    }

    private void setupPromoCode() {
        etPromoCode.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String promoCode = etPromoCode.getText().toString().trim();
                if (!promoCode.isEmpty()) {
                    applyPromoCode(promoCode);
                }
            }
        });
    }

    private void applyPromoCode(String promoCode) {
        // Simulate promo code validation
        if (promoCode.equalsIgnoreCase("SAVE10") || promoCode.equalsIgnoreCase("DISCOUNT")) {
            if (!promoCodeApplied) {
                promoCodeApplied = true;
                double discount = totalEstimate * 0.10;
                totalEstimate = totalEstimate - discount;
                updateTotalEstimate();
                Toast.makeText(this, "Promo code applied! 10% discount", Toast.LENGTH_SHORT).show();
            }
        } else if (!promoCode.isEmpty()) {
            Toast.makeText(this, "Invalid promo code", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateTotalEstimate() {
        tvTotalEstimate.setText("$" + String.format("%.2f", totalEstimate));
    }

    private void setupButtons() {
        btnCancel.setOnClickListener(v -> {
            animateButtonClick(v);
            finish();
        });

        btnConfirmBooking.setOnClickListener(v -> {
            if (!cbTermsOfService.isChecked()) {
                Toast.makeText(this, "Please agree to the Terms of Service", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!cbPaymentMethod.isChecked()) {
                Toast.makeText(this, "Please select a payment method", Toast.LENGTH_SHORT).show();
                return;
            }

            animateButtonClick(v);
            confirmBooking();
        });
    }

    private void confirmBooking() {
        // Navigate to booking confirmed screen
        Intent intent = new Intent(this, BookingConfirmedActivity.class);
        
        // Pass booking data to confirmed screen
        intent.putExtra("bookingReference", generateBookingReference());
        intent.putExtra("serviceName", tvServiceType.getText().toString());
        intent.putExtra("serviceCategory", extractServiceCategory(tvServiceType.getText().toString()));
        intent.putExtra("serviceDateTime", getServiceDateTime());
        intent.putExtra("providerPhoneNumber", "+1234567890"); // Default phone number
        
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        finish();
    }

    private String generateBookingReference() {
        Random random = new Random();
        long reference = 1000000000L + random.nextInt(900000000);
        return String.valueOf(reference);
    }

    private String extractServiceCategory(String serviceType) {
        // Extract category from service type or use default
        if (serviceType != null && serviceType.toLowerCase().contains("plumb")) {
            return "Plumbing";
        }
        return "Plumbing"; // Default
    }

    private String getServiceDateTime() {
        // Get date and time from intent or use default
        Intent intent = getIntent();
        String serviceDate = intent.getStringExtra("serviceDate");
        String serviceTime = intent.getStringExtra("serviceTime");
        
        if (serviceDate != null && serviceTime != null) {
            return serviceDate + ", " + serviceTime;
        }
        return "Tomorrow, 10:00 AM - 11:00 AM"; // Default
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
}

