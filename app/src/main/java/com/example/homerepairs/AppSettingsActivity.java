package com.example.homerepairs;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class AppSettingsActivity extends BaseActivity {

    private TextView tvSelectedLanguage;
    private TextView tvSelectedTheme;
    private TextView tvSelectedCurrency;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_settings);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getWindow().getDecorView().setSystemUiVisibility(
                        getWindow().getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        tvSelectedLanguage = findViewById(R.id.tvSelectedLanguage);
        tvSelectedTheme = findViewById(R.id.tvSelectedTheme);
        tvSelectedCurrency = findViewById(R.id.tvSelectedCurrency);
    }

    private void setupClickListeners() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.cardLanguage).setOnClickListener(v -> showLanguageDialog());

        findViewById(R.id.cardTheme).setOnClickListener(v -> showSelectionDialog(getString(R.string.select_theme),
                new String[] { "Light", "Dark", "Auto" }, tvSelectedTheme));

        findViewById(R.id.cardCurrency).setOnClickListener(v -> showCurrencyDialog());
    }

    private void showLanguageDialog() {
        String[] languages = { "English", "Khmer" };
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.select_language))
                .setItems(languages, (dialog, which) -> {
                    String selectedLang = "en";
                    if (which == 1) {
                        selectedLang = "km";
                    }

                    if (!selectedLang.equals(com.example.homerepairs.utils.LocaleHelper.getLanguage(this))) {
                        com.example.homerepairs.utils.LocaleHelper.setLocale(this, selectedLang);
                        restartApp();
                    }
                })
                .show();
    }

    private void showCurrencyDialog() {
        String[] currencies = { "USD ($)", "Riel (៛)" };
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.select_currency))
                .setItems(currencies, (dialog, which) -> {
                    String selectedCurrency = which == 0 ? com.example.homerepairs.utils.CurrencyHelper.USD
                            : com.example.homerepairs.utils.CurrencyHelper.KHR;
                    com.example.homerepairs.utils.CurrencyHelper.setCurrency(this, selectedCurrency);
                    updateCurrencyUI(selectedCurrency);
                    // Recreate to apply changes if needed, or just update UI
                })
                .show();
    }

    private void restartApp() {
        android.content.Intent intent = new android.content.Intent(this, MainActivity.class);
        intent.setFlags(
                android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    // saveCurrencyPreference removed as we use CurrencyHelper now

    private void updateCurrencyUI(String currencyCode) {
        if (com.example.homerepairs.utils.CurrencyHelper.USD.equals(currencyCode)) {
            tvSelectedCurrency.setText("USD");
        } else {
            tvSelectedCurrency.setText("Riel");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    private void updateUI() {
        String langCode = com.example.homerepairs.utils.LocaleHelper.getLanguage(this);
        if ("km".equals(langCode)) {
            if (tvSelectedLanguage != null)
                tvSelectedLanguage.setText("Khmer");
        } else {
            if (tvSelectedLanguage != null)
                tvSelectedLanguage.setText("English");
        }

        String currency = com.example.homerepairs.utils.CurrencyHelper.getCurrency(this);
        updateCurrencyUI(currency);
    }

    private void showSelectionDialog(String title, String[] items, TextView targetView) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setItems(items, (dialog, which) -> {
                    if (targetView != null) {
                        targetView.setText(items[which]);
                    }
                })
                .show();
    }
}
