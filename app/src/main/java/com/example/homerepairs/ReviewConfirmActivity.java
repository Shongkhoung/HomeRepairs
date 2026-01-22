package com.example.homerepairs;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.homerepairs.databinding.ActivityReviewConfirmBinding;
import com.example.homerepairs.models.Booking;
import com.example.homerepairs.services.FirebaseBookingService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

public class ReviewConfirmActivity extends BaseActivity {

    private ActivityReviewConfirmBinding binding;
    private FirebaseBookingService bookingService;
    private double serviceFee = 120.0, bookingFee = 10.0, taxes = 5.0, total = 135.0;
    private String selectedPayment = "wallet";
    private boolean promoApplied = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReviewConfirmBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        bookingService = new FirebaseBookingService();

        setupData();
        setupListeners();
        updatePaymentSelection("wallet");
        updateConfirmState();
    }

    private void setupData() {
        Intent intent = getIntent();
        binding.tvServiceType.setText(
                intent.getStringExtra("serviceType") != null ? intent.getStringExtra("serviceType") : "Plumbing");

        String provider = intent.getStringExtra("providerName");
        binding.tvServiceProvider.setText("Service by " + (provider != null ? provider : "Alex Bennett"));

        String date = intent.getStringExtra("serviceDate");
        String time = intent.getStringExtra("serviceTime");
        binding.tvServiceDateTime.setText((date != null && time != null) ? date + ", " + time : "Today, 10:00 AM");

        String loc = intent.getStringExtra("location");
        binding.tvLocationValue.setText(loc != null ? loc : "123 Maple Street, Anytown");

        String dur = intent.getStringExtra("duration");
        binding.tvDuration.setText(dur != null ? dur : "2 hours");

        String desc = intent.getStringExtra("issueDescription");
        binding.tvIssueDescription.setText(desc != null ? desc : "Leaky faucet in the kitchen");

        serviceFee = intent.getDoubleExtra("serviceFee", 120.0);
        bookingFee = intent.getDoubleExtra("bookingFee", 10.0);
        taxes = intent.getDoubleExtra("taxes", 5.0);
        total = serviceFee + bookingFee + taxes;

        updatePriceDisplay();
    }

    private void updatePriceDisplay() {
        binding.tvServiceFee.setText("$" + String.format("%.0f", serviceFee));
        binding.tvBookingFee.setText("$" + String.format("%.0f", bookingFee));
        binding.tvTaxes.setText("$" + String.format("%.0f", taxes));
        binding.tvTotalEstimate.setText("$" + String.format("%.2f", total));
    }

    private void setupListeners() {
        binding.btnClose.setOnClickListener(v -> finish());
        binding.llPaymentCard.setOnClickListener(v -> {
            animateClick(v);
            updatePaymentSelection("card");
        });
        binding.llPaymentQR.setOnClickListener(v -> {
            animateClick(v);
            updatePaymentSelection("qr");
        });
        binding.llPaymentWallet.setOnClickListener(v -> {
            animateClick(v);
            updatePaymentSelection("wallet");
        });
        binding.llPaymentCash.setOnClickListener(v -> {
            animateClick(v);
            updatePaymentSelection("cash");
        });

        binding.etPromoCode.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus)
                applyPromo(binding.etPromoCode.getText().toString().trim());
        });

        binding.cbTermsOfService.setOnCheckedChangeListener((bv, checked) -> updateConfirmState());
        binding.btnCancel.setOnClickListener(v -> {
            animateClick(v);
            finish();
        });
        binding.btnConfirmBooking.setOnClickListener(v -> {
            if (!binding.cbTermsOfService.isChecked()) {
                Toast.makeText(this, "Agree to terms", Toast.LENGTH_SHORT).show();
                return;
            }
            animateClick(v);
            confirm();
        });
    }

    private void updatePaymentSelection(String method) {
        selectedPayment = method;
        binding.llPaymentCard.setBackgroundResource(R.drawable.payment_method_unselected);
        binding.llPaymentQR.setBackgroundResource(R.drawable.payment_method_unselected);
        binding.llPaymentWallet.setBackgroundResource(R.drawable.payment_method_unselected);
        binding.llPaymentCash.setBackgroundResource(R.drawable.payment_method_unselected);

        binding.radioCard.setImageResource(R.drawable.radio_button_unselected_gray);
        binding.radioQR.setImageResource(R.drawable.radio_button_unselected_gray);
        binding.radioWallet.setImageResource(R.drawable.radio_button_unselected_gray);
        binding.radioCash.setImageResource(R.drawable.radio_button_unselected_gray);

        switch (method) {
            case "card":
                binding.llPaymentCard.setBackgroundResource(R.drawable.payment_method_selected);
                binding.radioCard.setImageResource(R.drawable.radio_button_selected_teal);
                break;
            case "qr":
                binding.llPaymentQR.setBackgroundResource(R.drawable.payment_method_selected);
                binding.radioQR.setImageResource(R.drawable.radio_button_selected_teal);
                break;
            case "wallet":
                binding.llPaymentWallet.setBackgroundResource(R.drawable.payment_method_selected);
                binding.radioWallet.setImageResource(R.drawable.radio_button_selected_teal);
                break;
            case "cash":
                binding.llPaymentCash.setBackgroundResource(R.drawable.payment_method_selected);
                binding.radioCash.setImageResource(R.drawable.radio_button_selected_teal);
                break;
        }
    }

    private void applyPromo(String code) {
        if ((code.equalsIgnoreCase("SAVE10") || code.equalsIgnoreCase("DISCOUNT")) && !promoApplied) {
            promoApplied = true;
            total *= 0.90;
            updatePriceDisplay();
            Toast.makeText(this, "10% Discount applied!", Toast.LENGTH_SHORT).show();
        } else if (!code.isEmpty() && !promoApplied) {
            Toast.makeText(this, "Invalid code", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateConfirmState() {
        boolean enabled = binding.cbTermsOfService.isChecked();
        binding.btnConfirmBooking.setEnabled(enabled);
        binding.btnConfirmBooking.setAlpha(enabled ? 1.0f : 0.5f);
    }

    private void confirm() {
        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        if (api.isGooglePlayServicesAvailable(this) != ConnectionResult.SUCCESS) {
            Toast.makeText(this, "GPS error", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Toast.makeText(this, "Login required", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnConfirmBooking.setEnabled(false);
        binding.btnConfirmBooking.setText("Processing...");

        Intent intent = getIntent();
        String cat = intent.getStringExtra("serviceCategory");
        String name = intent.getStringExtra("serviceName");
        String desc = intent.getStringExtra("issueDescription");
        String date = intent.getStringExtra("serviceDate");
        String time = intent.getStringExtra("serviceTime");
        String loc = intent.getStringExtra("location");

        Booking booking = new Booking(uid, intent.getStringExtra("providerId"), intent.getStringExtra("providerName"),
                cat, name, desc, intent.getStringExtra("urgency"));
        booking.setServiceDateTime((date != null ? date : "Today") + ", " + (time != null ? time : "10:00 AM"));
        booking.setPropertyLocation(loc != null ? loc : "Not specified");
        booking.setPaymentMethod(selectedPayment);
        booking.setPrice(total);

        bookingService.createBooking(booking, new FirebaseBookingService.BookingCallback() {
            @Override
            public void onSuccess(Booking b) {
                Intent confirm = new Intent(ReviewConfirmActivity.this, BookingConfirmedActivity.class);
                confirm.putExtra("bookingReference", b.getBookingReference());
                confirm.putExtra("booking_id", b.getId());
                confirm.putExtra("serviceName", name);
                confirm.putExtra("paymentMethod", selectedPayment);
                confirm.putExtra("price", "$" + String.format("%.0f", total));
                startActivity(confirm);
                finish();
            }

            @Override
            public void onError(String err) {
                Toast.makeText(ReviewConfirmActivity.this, "Error: " + err, Toast.LENGTH_LONG).show();
                updateConfirmState();
                binding.btnConfirmBooking.setText("Confirm Booking");
            }
        });
    }
}
