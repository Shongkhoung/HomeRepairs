package com.example.homerepairs;

import android.os.Bundle;
import android.widget.Toast;
import com.example.homerepairs.databinding.ActivityPrivacySecurityBinding;

public class PrivacySecurityActivity extends BaseActivity {

    private ActivityPrivacySecurityBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPrivacySecurityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnChangePassword.setOnClickListener(
                v -> Toast.makeText(this, getString(R.string.change_password), Toast.LENGTH_SHORT).show());

        binding.btnTwoFactor.setOnClickListener(
                v -> Toast.makeText(this, getString(R.string.two_factor_auth), Toast.LENGTH_SHORT).show());

        binding.btnPrivacySettings.setOnClickListener(
                v -> Toast.makeText(this, getString(R.string.privacy_settings), Toast.LENGTH_SHORT).show());

        binding.btnDeleteAccount.setOnClickListener(v -> confirmDeleteAccount());
    }

    private void confirmDeleteAccount() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.delete_account))
                .setMessage(getString(R.string.delete_account_confirmation))
                .setPositiveButton(getString(R.string.delete),
                        (dialog, which) -> Toast
                                .makeText(this, getString(R.string.delete_account_initiated), Toast.LENGTH_SHORT)
                                .show())
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }
}
