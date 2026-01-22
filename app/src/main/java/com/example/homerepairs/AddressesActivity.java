package com.example.homerepairs;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.example.homerepairs.databinding.ActivityAddressesBinding;
import com.example.homerepairs.databinding.ItemAddressBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class AddressesActivity extends BaseActivity {

    private ActivityAddressesBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private boolean isPickMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddressesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        isPickMode = getIntent().getBooleanExtra("ACTION_PICK", false);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAddresses();
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnAddAddress.setOnClickListener(v -> startActivity(new Intent(this, AddAddressActivity.class)));
    }

    private void loadAddresses() {
        if (auth.getCurrentUser() == null)
            return;

        db.collection("users")
                .document(auth.getCurrentUser().getUid())
                .collection("addresses")
                .get()
                .addOnSuccessListener(snapshots -> {
                    binding.llAddressesList.removeAllViews();
                    for (int i = 0; i < snapshots.size(); i++) {
                        DocumentSnapshot doc = snapshots.getDocuments().get(i);
                        addAddressView(doc);
                        if (i < snapshots.size() - 1)
                            addDivider();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void addDivider() {
        View divider = new View(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (int) (1 * getResources().getDisplayMetrics().density));
        params.setMargins((int) (72 * getResources().getDisplayMetrics().density), 0, 0, 0);
        divider.setLayoutParams(params);
        divider.setBackgroundColor(ContextCompat.getColor(this, R.color.divider_gray));
        divider.setAlpha(0.3f);
        binding.llAddressesList.addView(divider);
    }

    private void addAddressView(DocumentSnapshot doc) {
        ItemAddressBinding itemBinding = ItemAddressBinding.inflate(LayoutInflater.from(this), binding.llAddressesList,
                false);

        String name = doc.getString("name");
        String street = doc.getString("streetAddress");
        String city = doc.getString("city");
        String full = String.format("%s, %s", street != null ? street : "", city != null ? city : "");

        itemBinding.tvAddressName.setText(name != null ? name : "Address");
        itemBinding.tvAddressDetails.setText(full);
        itemBinding.btnDelete.setOnClickListener(v -> confirmDelete(doc.getId(), name));

        if (isPickMode) {
            itemBinding.getRoot().setOnClickListener(v -> {
                Intent intent = new Intent();
                intent.putExtra("address_name", name);
                intent.putExtra("address_full", full);
                intent.putExtra("address_street", street);
                intent.putExtra("address_city", city);
                setResult(RESULT_OK, intent);
                finish();
            });
        }

        binding.llAddressesList.addView(itemBinding.getRoot());
    }

    private void confirmDelete(String docId, String name) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete)
                .setMessage(getString(R.string.msg_delete_address, name))
                .setPositiveButton(R.string.delete, (d, w) -> deleteAddress(docId))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteAddress(String docId) {
        if (auth.getCurrentUser() == null)
            return;
        db.collection("users").document(auth.getCurrentUser().getUid())
                .collection("addresses").document(docId).delete()
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                    loadAddresses();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
