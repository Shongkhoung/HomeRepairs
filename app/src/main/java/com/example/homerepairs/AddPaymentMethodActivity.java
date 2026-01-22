package com.example.homerepairs;

import android.os.Bundle;
import android.widget.Toast;

import com.example.homerepairs.databinding.ActivityAddPaymentMethodBinding;
import com.example.homerepairs.models.PaymentMethod;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AddPaymentMethodActivity extends BaseActivity {

    private ActivityAddPaymentMethodBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddPaymentMethodBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSavePayment.setOnClickListener(v -> savePaymentMethod());
    }

    private void savePaymentMethod() {
        String cardNumber = binding.edtCardNumber.getText().toString().trim();
        String cardHolder = binding.edtCardHolder.getText().toString().trim();
        String expiry = binding.edtExpiry.getText().toString().trim();
        String cvv = binding.edtCvv.getText().toString().trim();

        if (cardNumber.isEmpty() || cardHolder.isEmpty() || expiry.isEmpty() || cvv.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        PaymentMethod method = new PaymentMethod(cardNumber, cardHolder, expiry, cvv);
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .collection("payment_methods")
                .add(method)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, getString(R.string.msg_payment_saved), Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
