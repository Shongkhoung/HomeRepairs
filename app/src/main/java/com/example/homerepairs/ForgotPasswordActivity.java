package com.example.homerepairs;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import com.example.homerepairs.databinding.ActivityForgotPasswordBinding;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends BaseActivity {

    private ActivityForgotPasswordBinding binding;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        setupListeners();

        String email = getIntent().getStringExtra("email");
        if (!TextUtils.isEmpty(email))
            binding.etEmail.setText(email);
    }

    private void setupListeners() {
        binding.btnSendResetLink.setOnClickListener(v -> sendResetLink());
        binding.tvBackToSignIn.setOnClickListener(v -> navigateToLogin());
        binding.btnBack.setOnClickListener(v -> onBackPressed());
    }

    private void sendResetLink() {
        String email = binding.etEmail.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            showError("Please enter email");
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Invalid email");
            return;
        }

        showLoading(true);
        binding.tvError.setVisibility(View.GONE);

        auth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            showLoading(false);
            if (task.isSuccessful()) {
                Intent intent = new Intent(this, CheckEmailActivity.class);
                intent.putExtra("email", email);
                startActivity(intent);
                finish();
            } else {
                showError(getErrorMessage(task.getException() != null ? task.getException().getMessage() : "Failed"));
            }
        });
    }

    private String getErrorMessage(String error) {
        if (error == null)
            return "Unknown error";
        if (error.contains("user-not-found"))
            return "No account found";
        if (error.contains("invalid-email"))
            return "Invalid email address";
        if (error.contains("network"))
            return "Network error";
        return error;
    }

    private void showError(String msg) {
        binding.tvError.setText(msg);
        binding.tvError.setVisibility(View.VISIBLE);
        binding.etEmail.requestFocus();
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.btnSendResetLink.setEnabled(!show);
        binding.btnSendResetLink.setAlpha(show ? 0.6f : 1.0f);
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("mode", "sign_in");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        navigateToLogin();
    }
}
