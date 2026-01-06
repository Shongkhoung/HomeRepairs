package com.example.homerepairs;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class AddressesActivity extends AppCompatActivity {

    private android.widget.LinearLayout llAddressesList;
    private com.google.firebase.firestore.FirebaseFirestore db;
    private com.google.firebase.auth.FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_addresses);

        db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        auth = com.google.firebase.auth.FirebaseAuth.getInstance();
        llAddressesList = findViewById(R.id.llAddressesList);

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
        loadAddresses();
    }

    private void setupClickListeners() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnAddAddress)
                .setOnClickListener(
                        v -> startActivity(new android.content.Intent(this, AddAddressActivity.class)));
    }

    private void loadAddresses() {
        if (auth.getCurrentUser() == null)
            return;

        db.collection("users")
                .document(auth.getCurrentUser().getUid())
                .collection("addresses")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    llAddressesList.removeAllViews();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        addAddressView(document);
                    }
                })
                .addOnFailureListener(e -> Toast
                        .makeText(this, "Error loading addresses: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void addAddressView(com.google.firebase.firestore.QueryDocumentSnapshot document) {
        View view = android.view.LayoutInflater.from(this).inflate(R.layout.item_address, llAddressesList, false);

        String name = document.getString("name"); // e.g. Home, Work
        String street = document.getString("streetAddress");
        String city = document.getString("city");

        ((android.widget.TextView) view.findViewById(R.id.tvAddressName)).setText(name != null ? name : "Address");
        ((android.widget.TextView) view.findViewById(R.id.tvAddressDetails)).setText(
                String.format("%s, %s", street != null ? street : "", city != null ? city : ""));

        view.findViewById(R.id.btnDelete).setOnClickListener(v -> confirmDelete(document.getId(), name));

        llAddressesList.addView(view);
    }

    private void confirmDelete(String docId, String addressName) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.delete))
                .setMessage(getString(R.string.msg_delete_address, addressName))
                .setPositiveButton(getString(R.string.delete), (dialog, which) -> deleteAddress(docId))
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void deleteAddress(String docId) {
        if (auth.getCurrentUser() == null)
            return;

        db.collection("users")
                .document(auth.getCurrentUser().getUid())
                .collection("addresses")
                .document(docId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Address deleted", Toast.LENGTH_SHORT).show();
                    loadAddresses();
                })
                .addOnFailureListener(
                        e -> Toast.makeText(this, "Error deleting: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
