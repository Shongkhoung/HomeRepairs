package com.example.homerepairs;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.example.homerepairs.databinding.ActivityPaymentMethodsBinding;
import com.example.homerepairs.databinding.ItemPaymentMethodBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class PaymentMethodsActivity extends BaseActivity {

    private ActivityPaymentMethodsBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPaymentMethodsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPayments();
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnAddPayment.setOnClickListener(v -> startActivity(new Intent(this, AddPaymentMethodActivity.class)));
    }

    private void loadPayments() {
        if (auth.getCurrentUser() == null)
            return;
        db.collection("users").document(auth.getCurrentUser().getUid())
                .collection("payment_methods").get()
                .addOnSuccessListener(snaps -> {
                    binding.llPaymentList.removeAllViews();
                    for (int i = 0; i < snaps.size(); i++) {
                        addPaymentView(snaps.getDocuments().get(i));
                        if (i < snaps.size() - 1)
                            addDivider();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void addDivider() {
        View d = new View(this);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (1 * getResources().getDisplayMetrics().density));
        p.setMargins((int) (72 * getResources().getDisplayMetrics().density), 0, 0, 0);
        d.setLayoutParams(p);
        d.setBackgroundColor(ContextCompat.getColor(this, R.color.divider_gray));
        d.setAlpha(0.3f);
        binding.llPaymentList.addView(d);
    }

    private void addPaymentView(DocumentSnapshot doc) {
        ItemPaymentMethodBinding item = ItemPaymentMethodBinding.inflate(LayoutInflater.from(this),
                binding.llPaymentList, false);
        String card = doc.getString("cardNumber");
        String masked = "Card ending in "
                + (card != null && card.length() >= 4 ? card.substring(card.length() - 4) : "****");

        item.tvCardName.setText(masked);
        item.tvCardExpiry.setText("Expires: " + doc.getString("expiryDate"));
        item.btnDelete.setOnClickListener(v -> confirmDelete(doc.getId(), masked));

        binding.llPaymentList.addView(item.getRoot());
    }

    private void confirmDelete(String id, String name) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete)
                .setMessage(getString(R.string.msg_delete_payment, name))
                .setPositiveButton(R.string.delete, (d, w) -> delete(id))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void delete(String id) {
        if (auth.getCurrentUser() == null)
            return;
        db.collection("users").document(auth.getCurrentUser().getUid())
                .collection("payment_methods").document(id).delete()
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Deleted!", Toast.LENGTH_SHORT).show();
                    loadPayments();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
