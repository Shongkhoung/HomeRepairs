package com.example.homerepairs;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Build;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.homerepairs.models.Booking;
import com.example.homerepairs.services.FirebaseBookingService;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Random;

public class ReviewConfirmActivity extends AppCompatActivity {

    private TextView tvServiceType;
    private TextView tvServiceProvider;
    private TextView tvServiceDateTime;
    private TextView tvLocationValue;
    private TextView tvDuration;
    private TextView tvIssueDescription;
    private TextView tvServiceFee;
    private TextView tvBookingFee;
    private TextView tvTaxes;
    private TextView tvTotalEstimate;
    private EditText etPromoCode;
    private CheckBox cbTermsOfService;
    private Button btnCancel;
    private Button btnConfirmBooking;

    // Payment method views
    private LinearLayout llPaymentCard;
    private LinearLayout llPaymentQR;
    private LinearLayout llPaymentWallet;
    private LinearLayout llPaymentCash;
    private View radioCard;
    private View radioQR;
    private View radioWallet;
    private View radioCash;

    private double serviceFee = 120.0;
    private double bookingFee = 10.0;
    private double taxes = 5.0;
    private double totalEstimate = 135.0;
    private String selectedPaymentMethod = "wallet"; // Default to wallet
    private boolean promoCodeApplied = false;
    private FirebaseBookingService bookingService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_confirm);

        // Set status bar color to white
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getWindow().getDecorView().setSystemUiVisibility(
                        getWindow().getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }

        // Initialize Firebase service
        bookingService = new FirebaseBookingService();

        initializeViews();
        setupData();
        setupPaymentMethods();
        setupPromoCode();
        setupButtons();

        updateConfirmButtonState();
    }

    private void initializeViews() {
        tvServiceType = findViewById(R.id.tvServiceType);
        tvServiceProvider = findViewById(R.id.tvServiceProvider);
        tvServiceDateTime = findViewById(R.id.tvServiceDateTime);
        tvLocationValue = findViewById(R.id.tvLocationValue);
        tvDuration = findViewById(R.id.tvDuration);
        tvIssueDescription = findViewById(R.id.tvIssueDescription);
        tvServiceFee = findViewById(R.id.tvServiceFee);
        tvBookingFee = findViewById(R.id.tvBookingFee);
        tvTaxes = findViewById(R.id.tvTaxes);
        tvTotalEstimate = findViewById(R.id.tvTotalEstimate);
        etPromoCode = findViewById(R.id.etPromoCode);
        cbTermsOfService = findViewById(R.id.cbTermsOfService);
        btnCancel = findViewById(R.id.btnCancel);
        btnConfirmBooking = findViewById(R.id.btnConfirmBooking);

        // Payment method views
        llPaymentCard = findViewById(R.id.llPaymentCard);
        llPaymentQR = findViewById(R.id.llPaymentQR);
        llPaymentWallet = findViewById(R.id.llPaymentWallet);
        llPaymentCash = findViewById(R.id.llPaymentCash);
        radioCard = findViewById(R.id.radioCard);
        radioQR = findViewById(R.id.radioQR);
        radioWallet = findViewById(R.id.radioWallet);
        radioCash = findViewById(R.id.radioCash);

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
        if (providerName != null) {
            tvServiceProvider.setText("Service by " + providerName);
        } else {
            tvServiceProvider.setText("Service by Alex Bennett");
        }

        if (serviceDate != null && serviceTime != null) {
            tvServiceDateTime.setText(serviceDate + ", " + serviceTime);
        } else {
            tvServiceDateTime.setText("Today, 10:00 AM");
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

    private void setupPaymentMethods() {
        // Set default selection to wallet
        updatePaymentMethodSelection("wallet");

        llPaymentCard.setOnClickListener(v -> {
            animateButtonClick(v);
            updatePaymentMethodSelection("card");
        });

        llPaymentQR.setOnClickListener(v -> {
            animateButtonClick(v);
            updatePaymentMethodSelection("qr");
        });

        llPaymentWallet.setOnClickListener(v -> {
            animateButtonClick(v);
            updatePaymentMethodSelection("wallet");
        });

        llPaymentCash.setOnClickListener(v -> {
            animateButtonClick(v);
            updatePaymentMethodSelection("cash");
        });
    }

    private void updatePaymentMethodSelection(String method) {
        selectedPaymentMethod = method;

        // Reset all payment methods
        llPaymentCard.setBackgroundResource(R.drawable.payment_method_card_background);
        llPaymentQR.setBackgroundResource(R.drawable.payment_method_card_background);
        llPaymentWallet.setBackgroundResource(R.drawable.payment_method_card_background);
        llPaymentCash.setBackgroundResource(R.drawable.payment_method_card_background);

        radioCard.setBackgroundResource(R.drawable.radio_button_unselected);
        radioQR.setBackgroundResource(R.drawable.radio_button_unselected);
        radioWallet.setBackgroundResource(R.drawable.radio_button_unselected);
        radioCash.setBackgroundResource(R.drawable.radio_button_unselected);

        // Update selected payment method
        switch (method) {
            case "card":
                llPaymentCard.setBackgroundResource(R.drawable.payment_method_card_selected);
                radioCard.setBackgroundResource(R.drawable.radio_button_selected);
                break;
            case "qr":
                llPaymentQR.setBackgroundResource(R.drawable.payment_method_card_selected);
                radioQR.setBackgroundResource(R.drawable.radio_button_selected);
                break;
            case "wallet":
                llPaymentWallet.setBackgroundResource(R.drawable.payment_method_card_selected);
                radioWallet.setBackgroundResource(R.drawable.radio_button_selected);
                break;
            case "cash":
                llPaymentCash.setBackgroundResource(R.drawable.payment_method_card_selected);
                radioCash.setBackgroundResource(R.drawable.radio_button_selected);
                break;
        }
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
        cbTermsOfService.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateConfirmButtonState();
        });

        btnCancel.setOnClickListener(v -> {
            animateButtonClick(v);
            finish();
        });

        btnConfirmBooking.setOnClickListener(v -> {
            if (!cbTermsOfService.isChecked()) {
                Toast.makeText(this, "Please agree to the Terms of Service", Toast.LENGTH_SHORT).show();
                return;
            }

            animateButtonClick(v);
            confirmBooking();
        });
    }

    private void updateConfirmButtonState() {
        boolean isEnabled = cbTermsOfService.isChecked();
        btnConfirmBooking.setEnabled(isEnabled);
        btnConfirmBooking.setAlpha(isEnabled ? 1.0f : 0.5f);
    }

    private void confirmBooking() {
        // Get all data from intent
        Intent currentIntent = getIntent();
        String userId = currentIntent.getStringExtra("userId");
        String providerId = currentIntent.getStringExtra("providerId");
        String providerName = currentIntent.getStringExtra("providerName");
        String serviceCategory = currentIntent.getStringExtra("serviceCategory");
        String serviceName = currentIntent.getStringExtra("serviceName");
        String issueDescription = currentIntent.getStringExtra("issueDescription");
        String urgency = currentIntent.getStringExtra("urgency");
        String propertyLocation = currentIntent.getStringExtra("location");
        String propertyName = currentIntent.getStringExtra("propertyName");
        String serviceDate = currentIntent.getStringExtra("serviceDate");
        String serviceTime = currentIntent.getStringExtra("serviceTime");
        ArrayList<String> photoUris = currentIntent.getStringArrayListExtra("photoUris");

        // Validate user is logged in
        if (userId == null) {
            userId = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                    : null;
        }

        if (userId == null) {
            Toast.makeText(this, "Please log in to confirm booking", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state
        btnConfirmBooking.setEnabled(false);
        btnConfirmBooking.setText("Creating booking...");

        // Create booking object
        Booking booking = new Booking(
                userId,
                providerId != null ? providerId : "default_provider",
                providerName != null ? providerName : "Service Provider",
                serviceCategory != null ? serviceCategory : "General Service",
                serviceName != null ? serviceName : "Service Request",
                issueDescription != null ? issueDescription : "",
                urgency != null ? urgency : "Normal");

        // Set additional details
        String dateTime = (serviceDate != null ? serviceDate : "Today") + ", " +
                (serviceTime != null ? serviceTime : "10:00 AM");
        booking.setServiceDateTime(dateTime);
        booking.setPropertyLocation(propertyLocation != null ? propertyLocation : "Not specified");
        booking.setPropertyName(propertyName != null ? propertyName : "Home");
        booking.setPhotoUrls(photoUris != null ? photoUris : new ArrayList<>());
        booking.setPaymentMethod(selectedPaymentMethod);

        // Create booking in Firebase
        bookingService.createBooking(booking, new FirebaseBookingService.BookingCallback() {
            @Override
            public void onSuccess(Booking createdBooking) {
                // Navigate to confirmation screen
                Intent intent = new Intent(ReviewConfirmActivity.this, BookingConfirmedActivity.class);

                intent.putExtra("bookingReference", createdBooking.getBookingReference());
                intent.putExtra("booking_id", createdBooking.getId());
                intent.putExtra("serviceName", serviceName);
                intent.putExtra("serviceCategory", serviceCategory);
                intent.putExtra("serviceDateTime", dateTime);
                intent.putExtra("providerPhoneNumber", "+1234567890");
                intent.putExtra("paymentMethod", selectedPaymentMethod);

                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(ReviewConfirmActivity.this,
                        "Failed to create booking: " + error,
                        Toast.LENGTH_LONG).show();
                btnConfirmBooking.setEnabled(true);
                btnConfirmBooking.setText("Confirm Booking");
            }
        });
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
        return tvServiceDateTime.getText().toString();
    }

    private void animateButtonClick(View button) {
        float originalScaleX = button.getScaleX();
        float originalScaleY = button.getScaleY();

        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(button, "scaleX", originalScaleX, 0.95f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(button, "scaleY", originalScaleY, 0.95f);

        scaleDownX.setDuration(100);
        scaleDownY.setDuration(100);

        scaleDownX.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(button, "scaleX", 0.95f, originalScaleX);
                ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(button, "scaleY", 0.95f, originalScaleY);

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
