package com.example.homerepairs;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;

import com.example.homerepairs.databinding.ActivityAppSettingsBinding;
import com.example.homerepairs.utils.CurrencyHelper;
import com.example.homerepairs.utils.LocaleHelper;

public class AppSettingsActivity extends BaseActivity {

    private ActivityAppSettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAppSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupListeners();
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.cardLanguage.setOnClickListener(v -> showLanguageDialog());
        binding.cardTheme.setOnClickListener(
                v -> showSelectionDialog(getString(R.string.select_theme), new String[] { "Light", "Dark", "Auto" }));
        binding.cardCurrency.setOnClickListener(v -> showCurrencyDialog());
    }

    private void showLanguageDialog() {
        String[] langs = { "English", "Khmer" };
        new AlertDialog.Builder(this).setTitle(R.string.select_language).setItems(langs, (d, w) -> {
            String sel = (w == 1) ? "km" : "en";
            if (!sel.equals(LocaleHelper.getLanguage(this))) {
                LocaleHelper.setLocale(this, sel);
                restartApp();
            }
        }).show();
    }

    private void showCurrencyDialog() {
        String[] curs = { "USD ($)", "Riel (៛)" };
        new AlertDialog.Builder(this).setTitle(R.string.select_currency).setItems(curs, (d, w) -> {
            String sel = (w == 0) ? CurrencyHelper.USD : CurrencyHelper.KHR;
            CurrencyHelper.setCurrency(this, sel);
            updateUI();
        }).show();
    }

    private void restartApp() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    private void updateUI() {
        binding.tvSelectedLanguage.setText("km".equals(LocaleHelper.getLanguage(this)) ? "Khmer" : "English");
        String cur = CurrencyHelper.getCurrency(this);
        binding.tvSelectedCurrency.setText(CurrencyHelper.USD.equals(cur) ? "USD" : "Riel");
    }

    private void showSelectionDialog(String title, String[] items) {
        new AlertDialog.Builder(this).setTitle(title)
                .setItems(items, (d, w) -> binding.tvSelectedTheme.setText(items[w])).show();
    }
}
