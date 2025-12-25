package com.example.homerepairs;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

public class CheckEmailActivity extends AppCompatActivity {

    private TextView tvEmailAddress;
    private MaterialButton btnEnterResetCode;
    private TextView tvResendEmail;
    private TextView tvBackToSignIn;
    private ProgressBar progressBar;
    private ImageButton btnBack;
    private TextView ivCheckIcon;

    private FirebaseAuth auth;
    private String emailAddress;
    
    // Resend timer variables
    private android.os.CountDownTimer resendTimer;
    private static final int RESEND_COOLDOWN_SECONDS = 60;
    private boolean canResend = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_email);

        // Set status bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getWindow().getDecorView().setSystemUiVisibility(
                        getWindow().getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                );
            }
        }

        initializeViews();
        setupClickListeners();

        // Get email from intent
        if (getIntent() != null && getIntent().hasExtra("email")) {
            emailAddress = getIntent().getStringExtra("email");
            if (tvEmailAddress != null && !TextUtils.isEmpty(emailAddress)) {
                tvEmailAddress.setText(emailAddress);
            }
        }
        
        // Update button text to "Resend Email" instead of "Enter Reset Code"
        if (btnEnterResetCode != null) {
            btnEnterResetCode.setText("Resend Email");
        }
        
        // Start countdown timer for resend button
        startResendTimer();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cancel timer if activity is destroyed
        if (resendTimer != null) {
            resendTimer.cancel();
        }
    }

    private void initializeViews() {
        tvEmailAddress = findViewById(R.id.tvEmailAddress);
        btnEnterResetCode = findViewById(R.id.btnEnterResetCode);
//        tvResendEmail = findViewById(R.id.tvResendEmail);
        tvBackToSignIn = findViewById(R.id.tvBackToSignIn);
        progressBar = findViewById(R.id.progressBar);
        btnBack = findViewById(R.id.btnBack);
        ivCheckIcon = findViewById(R.id.ivCheckIcon);

        auth = FirebaseAuth.getInstance();
    }

    private void setupClickListeners() {
        // Button now resends email instead of entering reset code
        btnEnterResetCode.setOnClickListener(v -> {
            if (canResend) {
                resendEmail();
            } else {
                Toast.makeText(this, "Please wait before resending", Toast.LENGTH_SHORT).show();
            }
        });

        // Remove resend email text view click listener since button handles it now
        // Keep tvResendEmail for display purposes only (or hide it)

        tvBackToSignIn.setOnClickListener(v -> navigateToLogin());

        btnBack.setOnClickListener(v -> onBackPressed());
    }

    private void resendEmail() {
        if (TextUtils.isEmpty(emailAddress)) {
            Toast.makeText(this, "Email address not found", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!canResend) {
            Toast.makeText(this, "Please wait before resending", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        auth.sendPasswordResetEmail(emailAddress)
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Reset email sent successfully!", Toast.LENGTH_SHORT).show();
                        // Restart the countdown timer after successful resend
                        startResendTimer();
                    } else {
                        String errorMessage = "Failed to resend email";
                        if (task.getException() != null && task.getException().getMessage() != null) {
                            errorMessage = task.getException().getMessage();
                        }
                        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    /**
     * Start countdown timer for resend button (60 seconds)
     */
    private void startResendTimer() {
        // Cancel existing timer if any
        if (resendTimer != null) {
            resendTimer.cancel();
        }
        
        canResend = false;
        
        // Disable resend button initially
        if (btnEnterResetCode != null) {
            btnEnterResetCode.setEnabled(false);
            btnEnterResetCode.setAlpha(0.6f);
        }
        
        // Hide the text view resend link since button handles it now
        if (tvResendEmail != null) {
            tvResendEmail.setVisibility(View.GONE);
        }
        
        // Create countdown timer
        resendTimer = new android.os.CountDownTimer(RESEND_COOLDOWN_SECONDS * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsRemaining = (int) (millisUntilFinished / 1000);
                if (btnEnterResetCode != null) {
                    btnEnterResetCode.setText("Resend Email (" + secondsRemaining + "s)");
                }
            }

            @Override
            public void onFinish() {
                canResend = true;
                if (btnEnterResetCode != null) {
                    btnEnterResetCode.setText("Resend Email");
                    btnEnterResetCode.setEnabled(true);
                    btnEnterResetCode.setAlpha(1.0f);
                }
            }
        };
        
        resendTimer.start();
    }

    private void showLoading(boolean show) {
        if (show) {
            if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
            if (btnEnterResetCode != null) {
                btnEnterResetCode.setEnabled(false);
                btnEnterResetCode.setAlpha(0.6f);
            }
        } else {
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            // Button state is controlled by countdown timer, not here
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, SignInActivity.class);
        intent.putExtra("mode", "sign_in"); // Navigate to sign-in mode (Welcome Back)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

}

