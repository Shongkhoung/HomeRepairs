package com.example.homerepairs;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class AddAddressActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_address);

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

        findViewById(R.id.btnSaveAddress).setOnClickListener(v -> saveAddress());
    }

    private void saveAddress() {
        EditText edtAddressName = findViewById(R.id.edtAddressName);
        EditText edtStreetAddress = findViewById(R.id.edtStreetAddress);
        EditText edtCity = findViewById(R.id.edtCity);
        EditText edtZipCode = findViewById(R.id.edtZipCode);

        String name = edtAddressName.getText().toString().trim();
        String street = edtStreetAddress.getText().toString().trim();
        String city = edtCity.getText().toString().trim();
        String zip = edtZipCode.getText().toString().trim();

        if (name.isEmpty() || street.isEmpty() || city.isEmpty() || zip.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance()
                .getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "You must be logged in to save addresses", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        com.example.homerepairs.models.Address address = new com.example.homerepairs.models.Address(name, street, city,
                zip);

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .collection("addresses")
                .add(address)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, getString(R.string.msg_address_saved), Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast
                        .makeText(this, "Error saving address: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
