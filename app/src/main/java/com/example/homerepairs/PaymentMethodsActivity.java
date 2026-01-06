package com.example.homerepairs;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class PaymentMethodsActivity extends AppCompatActivity {

    private android.widget.LinearLayout llPaymentList;
    private com.google.firebase.firestore.FirebaseFirestore db;
    private com.google.firebase.auth.FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_methods);

        db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        auth = com.google.firebase.auth.FirebaseAuth.getInstance();
        llPaymentList = findViewById(R.id.llPaymentList);

        // Status bar configuration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getWindow().getDecorView().setSystemUiVisibility(
                        getWindow().getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }

        setupClickListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPaymentMethods();
    }

    private void setupClickListeners() {
        // Back Button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Add Payment Method
        findViewById(R.id.btnAddPayment).setOnClickListener(
                v -> startActivity(new android.content.Intent(this, AddPaymentMethodActivity.class)));
    }

    private void loadPaymentMethods() {
        if (auth.getCurrentUser() == null)
            return;

        db.collection("users")
                .document(auth.getCurrentUser().getUid())
                .collection("payment_methods")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    llPaymentList.removeAllViews();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        addPaymentMethodView(document);
                    }
                })
                .addOnFailureListener(e -> Toast
                        .makeText(this, "Error loading payments: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void addPaymentMethodView(com.google.firebase.firestore.QueryDocumentSnapshot document) {
        View view = android.view.LayoutInflater.from(this).inflate(R.layout.item_payment_method, llPaymentList, false);

        String cardNumber = document.getString("cardNumber");
        String expiry = document.getString("expiryDate");

        // Mask card number
        String maskedCard = "Card ending in " + (cardNumber != null && cardNumber.length() >= 4
                ? cardNumber.substring(cardNumber.length() - 4)
                : "****");

        ((android.widget.TextView) view.findViewById(R.id.tvCardName)).setText(maskedCard);
        ((android.widget.TextView) view.findViewById(R.id.tvCardExpiry)).setText("Expires: " + expiry);

        view.findViewById(R.id.btnDelete).setOnClickListener(v -> confirmDelete(document.getId(), maskedCard));

        llPaymentList.addView(view);
    }

    private void confirmDelete(String docId, String cardName) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.delete))
                .setMessage(getString(R.string.msg_delete_payment, cardName))
                .setPositiveButton(getString(R.string.delete), (dialog, which) -> deletePaymentMethod(docId))
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void deletePaymentMethod(String docId) {
        if (auth.getCurrentUser() == null)
            return;

        db.collection("users")
                .document(auth.getCurrentUser().getUid())
                .collection("payment_methods")
                .document(docId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Payment method deleted", Toast.LENGTH_SHORT).show();
                    loadPaymentMethods();
                })
                .addOnFailureListener(
                        e -> Toast.makeText(this, "Error deleting: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
