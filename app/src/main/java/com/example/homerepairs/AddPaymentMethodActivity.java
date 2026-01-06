package com.example.homerepairs;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class AddPaymentMethodActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_payment_method);

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

    private void setupClickListeners() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnSavePayment).setOnClickListener(v -> savePaymentMethod());
    }

    private void savePaymentMethod() {
        EditText edtCardNumber = findViewById(R.id.edtCardNumber);
        EditText edtCardHolder = findViewById(R.id.edtCardHolder);
        EditText edtExpiry = findViewById(R.id.edtExpiry);
        EditText edtCvv = findViewById(R.id.edtCvv);

        String cardNumber = edtCardNumber.getText().toString().trim();
        String cardHolder = edtCardHolder.getText().toString().trim();
        String expiry = edtExpiry.getText().toString().trim();
        String cvv = edtCvv.getText().toString().trim();

        if (cardNumber.isEmpty() || cardHolder.isEmpty() || expiry.isEmpty() || cvv.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance()
                .getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "You must be logged in to save payment methods", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        com.example.homerepairs.models.PaymentMethod paymentMethod = new com.example.homerepairs.models.PaymentMethod(
                cardNumber, cardHolder, expiry, cvv);

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .collection("payment_methods")
                .add(paymentMethod)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, getString(R.string.msg_payment_saved), Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast
                        .makeText(this, "Error saving payment method: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
