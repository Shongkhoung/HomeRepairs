package com.example.homerepairs;

import android.os.Bundle;
import android.widget.Toast;

import com.example.homerepairs.databinding.ActivityAddAddressBinding;
import com.example.homerepairs.models.Address;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AddAddressActivity extends BaseActivity {

    private ActivityAddAddressBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddAddressBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSaveAddress.setOnClickListener(v -> saveAddress());
    }

    private void saveAddress() {
        String name = binding.edtAddressName.getText().toString().trim();
        String street = binding.edtStreetAddress.getText().toString().trim();
        String city = binding.edtCity.getText().toString().trim();
        String zip = binding.edtZipCode.getText().toString().trim();

        if (name.isEmpty() || street.isEmpty() || city.isEmpty() || zip.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Address address = new Address(name, street, city, zip);
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .collection("addresses")
                .add(address)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, getString(R.string.msg_address_saved), Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
