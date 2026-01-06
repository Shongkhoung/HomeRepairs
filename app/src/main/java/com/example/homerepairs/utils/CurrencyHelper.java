package com.example.homerepairs.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import java.text.NumberFormat;
import java.util.Locale;

public class CurrencyHelper {

    private static final String SELECTED_CURRENCY = "CurrencyHelper.Selected.Currency";
    public static final String USD = "USD";
    public static final String KHR = "KHR";
    // Fixed exchange rate for now: 1 USD = 4000 KHR
    private static final double EXCHANGE_RATE = 4000.0;

    public static String getCurrency(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString(SELECTED_CURRENCY, USD);
    }

    public static void setCurrency(Context context, String currencyCode) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(SELECTED_CURRENCY, currencyCode);
        editor.apply();
    }

    /**
     * Formats a price (base is always USD) into the selected currency format
     * string.
     * 
     * @param context    Context to access preferences
     * @param priceInUsd Price in US Dollars
     * @return Formatted price string (e.g., "$10.00" or "40,000 ៛")
     */
    public static String formatPrice(Context context, double priceInUsd) {
        String currency = getCurrency(context);

        if (KHR.equals(currency)) {
            double priceInKhr = priceInUsd * EXCHANGE_RATE;
            NumberFormat format = NumberFormat.getInstance(new Locale("km", "KH"));
            return format.format((long) priceInKhr) + " ៛";
        } else {
            NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
            return format.format(priceInUsd);
        }
    }

    /**
     * Parsing a price string back to double (assuming USD input usually)
     * This is a utility if needed, but for now we assume input data is USD.
     */
}
