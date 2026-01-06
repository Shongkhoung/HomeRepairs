package com.example.homerepairs;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class PrivacySecurityActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_security);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getWindow().getDecorView().setSystemUiVisibility(
                        getWindow().getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }

        setupClickListeners();
    }

    private void setupClickListeners() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnChangePassword).setOnClickListener(
                v -> Toast.makeText(this, getString(R.string.change_password), Toast.LENGTH_SHORT).show());

        findViewById(R.id.btnTwoFactor).setOnClickListener(
                v -> Toast.makeText(this, getString(R.string.two_factor_auth), Toast.LENGTH_SHORT).show());

        findViewById(R.id.btnPrivacySettings).setOnClickListener(
                v -> Toast.makeText(this, getString(R.string.privacy_settings), Toast.LENGTH_SHORT).show());

        findViewById(R.id.btnDeleteAccount).setOnClickListener(v -> confirmDeleteAccount());
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
