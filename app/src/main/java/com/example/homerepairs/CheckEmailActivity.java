package com.example.homerepairs;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.example.homerepairs.databinding.ActivityCheckEmailBinding;
import com.google.firebase.auth.FirebaseAuth;

public class CheckEmailActivity extends BaseActivity {

    private ActivityCheckEmailBinding binding;
    private FirebaseAuth auth;
    private String email;
    private CountDownTimer timer;
    private boolean canResend = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCheckEmailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        email = getIntent().getStringExtra("email");

        setupUI();
        setupListeners();
        startTimer();
    }

    private void setupUI() {
        if (!TextUtils.isEmpty(email))
            binding.tvEmailAddress.setText(email);
        binding.btnEnterResetCode.setText("Resend Email");
        binding.tvResendEmail.setVisibility(View.GONE);
    }

    private void setupListeners() {
        binding.btnEnterResetCode.setOnClickListener(v -> {
            if (canResend)
                resend();
            else
                Toast.makeText(this, "Wait please", Toast.LENGTH_SHORT).show();
        });
        binding.tvBackToSignIn.setOnClickListener(v -> navLogin());
        binding.btnBack.setOnClickListener(v -> navLogin());
    }

    private void resend() {
        if (TextUtils.isEmpty(email))
            return;
        showLoading(true);
        auth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            showLoading(false);
            if (task.isSuccessful()) {
                Toast.makeText(this, "Sent!", Toast.LENGTH_SHORT).show();
                startTimer();
            } else {
                Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startTimer() {
        if (timer != null)
            timer.cancel();
        canResend = false;
        binding.btnEnterResetCode.setEnabled(false);
        binding.btnEnterResetCode.setAlpha(0.6f);

        timer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long ms) {
                binding.btnEnterResetCode.setText("Resend Email (" + (ms / 1000) + "s)");
            }

            @Override
            public void onFinish() {
                canResend = true;
                binding.btnEnterResetCode.setText("Resend Email");
                binding.btnEnterResetCode.setEnabled(true);
                binding.btnEnterResetCode.setAlpha(1.0f);
            }
        }.start();
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            binding.btnEnterResetCode.setEnabled(false);
            binding.btnEnterResetCode.setAlpha(0.6f);
        }
    }

    private void navLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("mode", "sign_in");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null)
            timer.cancel();
    }

    @Override
    public void onBackPressed() {
        navLogin();
    }
}
