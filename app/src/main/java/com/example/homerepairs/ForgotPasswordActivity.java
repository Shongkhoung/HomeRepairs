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
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText etEmail;
    private MaterialButton btnSendResetLink;
    private TextView tvError;
    private TextView tvBackToSignIn;
    private ProgressBar progressBar;
    private ImageButton btnBack;
    private ImageView ivLockIcon;

    private FirebaseAuth auth;
    private String emailAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

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

        // Get email from intent if available
        if (getIntent() != null && getIntent().hasExtra("email")) {
            emailAddress = getIntent().getStringExtra("email");
            if (etEmail != null && !TextUtils.isEmpty(emailAddress)) {
                etEmail.setText(emailAddress);
            }
        }
    }

    private void initializeViews() {
        etEmail = findViewById(R.id.etEmail);
        btnSendResetLink = findViewById(R.id.btnSendResetLink);
        tvError = findViewById(R.id.tvError);
        tvBackToSignIn = findViewById(R.id.tvBackToSignIn);
        progressBar = findViewById(R.id.progressBar);
        btnBack = findViewById(R.id.btnBack);
        ivLockIcon = findViewById(R.id.ivLockIcon);

        auth = FirebaseAuth.getInstance();
    }

    private void setupClickListeners() {
        btnSendResetLink.setOnClickListener(v -> sendResetLink());

        tvBackToSignIn.setOnClickListener(v -> navigateToLogin());

        btnBack.setOnClickListener(v -> onBackPressed());
    }

    private void sendResetLink() {
        String email = etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            showError("Please enter your email address");
            etEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Please enter a valid email address");
            etEmail.requestFocus();
            return;
        }

        showLoading(true);
        hideError();

        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        // Navigate to check email screen
                        emailAddress = email;
                        navigateToCheckEmail(email);
                    } else {
                        String errorMessage = "Failed to send reset email";
                        if (task.getException() != null && task.getException().getMessage() != null) {
                            errorMessage = getErrorMessage(task.getException().getMessage());
                        }
                        showError(errorMessage);
                    }
                });
    }

    private String getErrorMessage(String error) {
        if (error == null) {
            return "An error occurred";
        }

        if (error.contains("user-not-found")) {
            return "No account found with this email address";
        } else if (error.contains("invalid-email")) {
            return "Invalid email address";
        } else if (error.contains("network")) {
            return "Network error. Please check your internet connection";
        } else {
            return error;
        }
    }

    private void showError(String message) {
        if (tvError != null) {
            tvError.setText(message);
            tvError.setVisibility(View.VISIBLE);
        }
    }

    private void hideError() {
        if (tvError != null) {
            tvError.setVisibility(View.GONE);
        }
    }

    private void showLoading(boolean show) {
        if (show) {
            if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
            if (btnSendResetLink != null) {
                btnSendResetLink.setEnabled(false);
                btnSendResetLink.setAlpha(0.6f);
            }
        } else {
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            if (btnSendResetLink != null) {
                btnSendResetLink.setEnabled(true);
                btnSendResetLink.setAlpha(1.0f);
            }
        }
    }

    private void navigateToCheckEmail(String email) {
        Intent intent = new Intent(this, CheckEmailActivity.class);
        intent.putExtra("email", email);
        startActivity(intent);
        finish();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("mode", "sign_in"); // Navigate to sign-in mode (Welcome Back)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        navigateToLogin();
    }
}

