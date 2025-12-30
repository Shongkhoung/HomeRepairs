package com.example.homerepairs;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.example.homerepairs.models.Booking;
import com.example.homerepairs.services.FirebaseBookingService;
import com.example.homerepairs.services.FirebaseStorageService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NewBookingActivity extends AppCompatActivity {

    private EditText etIssueDescription;
    private Button btnEmergency, btnSameDay, btnScheduleLater, btnNext;
    private MaterialCardView cardPhoto1, cardPhoto2, cardPhoto3;
    private ImageView ivPhoto1, ivPhoto2, ivPhoto3;
    private ImageView ivAddPhoto1, ivAddPhoto2, ivAddPhoto3;
    private TextView tvServiceName, tvServiceCategory, tvLocationName, tvLocationAddress;

    private String selectedUrgency = "";
    private List<Bitmap> selectedPhotoBitmaps = new ArrayList<>();
    private List<String> uploadedPhotoUrls = new ArrayList<>();
    private int currentPhotoIndex = 0;

    private FirebaseBookingService bookingService;
    private FirebaseStorageService storageService;

    private Uri cameraImageUri;

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_booking);

        bookingService = new FirebaseBookingService();
        storageService = new FirebaseStorageService();

        initializeViews();
        setupBackButton();
        setupUrgencyButtons();
        setupPhotoCards();
        setupNextButton();
        setupBottomNavigation();
        setupActivityResultLaunchers();
    }

    private void initializeViews() {
        etIssueDescription = findViewById(R.id.etIssueDescription);

        btnEmergency = findViewById(R.id.btnEmergency);
        btnScheduleLater = findViewById(R.id.btnScheduleLater);
        btnNext = findViewById(R.id.btnNext);

        cardPhoto1 = findViewById(R.id.cardPhoto1);
        cardPhoto2 = findViewById(R.id.cardPhoto2);
        cardPhoto3 = findViewById(R.id.cardPhoto3);

        ivPhoto1 = findViewById(R.id.ivPhoto1);
        ivPhoto2 = findViewById(R.id.ivPhoto2);
        ivPhoto3 = findViewById(R.id.ivPhoto3);

        ivAddPhoto1 = findViewById(R.id.ivAddPhoto1);
        ivAddPhoto2 = findViewById(R.id.ivAddPhoto2);
        ivAddPhoto3 = findViewById(R.id.ivAddPhoto3);

        tvServiceName = findViewById(R.id.tvServiceName);
        tvServiceCategory = findViewById(R.id.tvServiceCategory);
        tvLocationName = findViewById(R.id.tvLocationName);
        tvLocationAddress = findViewById(R.id.tvLocationAddress);

        // Initialize bitmaps with null
        for (int i = 0; i < 3; i++) selectedPhotoBitmaps.add(null);

        populateServiceInfo();
    }

    private void populateServiceInfo() {
        String serviceCategory = getIntent().getStringExtra("service_category");
        tvServiceCategory.setText(serviceCategory != null ? serviceCategory : "General");

        String serviceName = getIntent().getStringExtra("service_name");
        tvServiceName.setText((serviceName != null && !serviceName.isEmpty()) ? serviceName : "Service Request");
    }

    private void setupBackButton() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }

    private void setupUrgencyButtons() {
        deselectAllUrgency();
        if (btnEmergency != null) btnEmergency.setOnClickListener(v -> selectUrgency(btnEmergency, "Emergency"));
        if (btnSameDay != null) btnSameDay.setOnClickListener(v -> selectUrgency(btnSameDay, "Same Day"));
        if (btnScheduleLater != null) btnScheduleLater.setOnClickListener(v -> selectUrgency(btnScheduleLater, "Schedule Later"));
    }

    private void deselectAllUrgency() {
        deselectUrgencyButton(btnEmergency);
        deselectUrgencyButton(btnSameDay);
        deselectUrgencyButton(btnScheduleLater);
    }

    private void selectUrgency(Button btn, String urgency) {
        selectedUrgency = urgency;
        deselectAllUrgency();
        selectUrgencyButton(btn);
    }

    private void selectUrgencyButton(Button button) {
        if (button != null) {
            button.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.primary_blue));
            button.setTextColor(getResources().getColor(R.color.text_white));
        }
    }

    private void deselectUrgencyButton(Button button) {
        if (button != null) {
            button.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.background_gray));
            button.setTextColor(getResources().getColor(R.color.text_primary));
        }
    }

    private void setupPhotoCards() {
        if (cardPhoto1 != null) cardPhoto1.setOnClickListener(v -> openPhotoOptions(0));
        if (cardPhoto2 != null) cardPhoto2.setOnClickListener(v -> openPhotoOptions(1));
        if (cardPhoto3 != null) cardPhoto3.setOnClickListener(v -> openPhotoOptions(2));
    }

    private void openPhotoOptions(int index) {
        currentPhotoIndex = index;
        String[] options = {"Take Photo", "Choose from Gallery"};
        if (selectedPhotoBitmaps.get(index) != null) options = new String[]{"Take Photo", "Choose from Gallery", "Remove Photo"};

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Photo Options")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: checkCameraPermissionAndTakePhoto(); break;
                        case 1: openGallery(); break;
                        case 2: removePhoto(index); break;
                    }
                }).show();
    }

    private void setupActivityResultLaunchers() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), cameraImageUri);
                            setPhoto(currentPhotoIndex, bitmap);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                            setPhoto(currentPhotoIndex, bitmap);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    private void checkCameraPermissionAndTakePhoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 101);
        } else takePhoto();
    }

    private void takePhoto() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera");
        cameraImageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
        cameraLauncher.launch(cameraIntent);
    }

    private void openGallery() {
        galleryLauncher.launch("image/*");
    }

    private void setPhoto(int index, Bitmap bitmap) {
        ImageView photoView = null;
        ImageView addIcon = null;

        switch (index) {
            case 0: photoView = ivPhoto1; addIcon = ivAddPhoto1; break;
            case 1: photoView = ivPhoto2; addIcon = ivAddPhoto2; break;
            case 2: photoView = ivPhoto3; addIcon = ivAddPhoto3; break;
        }

        if (photoView != null && addIcon != null) {
            photoView.setImageBitmap(bitmap);
            photoView.setVisibility(View.VISIBLE);
            addIcon.setVisibility(View.GONE);
            selectedPhotoBitmaps.set(index, bitmap);
        }
    }

    private void removePhoto(int index) {
        ImageView photoView = null;
        ImageView addIcon = null;

        switch (index) {
            case 0: photoView = ivPhoto1; addIcon = ivAddPhoto1; break;
            case 1: photoView = ivPhoto2; addIcon = ivAddPhoto2; break;
            case 2: photoView = ivPhoto3; addIcon = ivAddPhoto3; break;
        }

        if (photoView != null && addIcon != null) {
            photoView.setVisibility(View.GONE);
            addIcon.setVisibility(View.VISIBLE);
            selectedPhotoBitmaps.set(index, null);
        }
    }

    private void setupNextButton() {
        btnNext.setOnClickListener(v -> {
            String issue = etIssueDescription.getText().toString().trim();
            if (issue.isEmpty()) {
                Toast.makeText(this, "Please describe the issue", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedUrgency.isEmpty()) {
                Toast.makeText(this, "Please select urgency", Toast.LENGTH_SHORT).show();
                return;
            }

            uploadedPhotoUrls.clear();
            uploadPhotosAndSaveBooking(issue);
        });
    }

    private void uploadPhotosAndSaveBooking(String issue) {
        List<Bitmap> photosToUpload = new ArrayList<>();
        for (Bitmap b : selectedPhotoBitmaps) if (b != null) photosToUpload.add(b);

        if (photosToUpload.isEmpty()) {
            saveBooking(issue);
            return;
        }

        final int total = photosToUpload.size();
        final int[] count = {0};

        for (Bitmap bitmap : photosToUpload) {
            storageService.uploadImage(bitmap, "booking_" + System.currentTimeMillis(), new FirebaseStorageService.ImageUploadCallback() {
                @Override
                public void onSuccess(String imageUrl) {
                    uploadedPhotoUrls.add(imageUrl);
                    count[0]++;
                    if (count[0] == total) saveBooking(issue);
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(NewBookingActivity.this, "Error uploading image: " + error, Toast.LENGTH_SHORT).show();
                    count[0]++;
                    if (count[0] == total) saveBooking(issue);
                }
            });
        }
    }

    private void saveBooking(String issue) {
        Booking booking = new Booking();
        booking.setIssueDescription(issue);
        booking.setUrgency(selectedUrgency);
        booking.setPhotoUrls(uploadedPhotoUrls);

        // Use createBooking instead of saveBooking
        bookingService.createBooking(booking, new FirebaseBookingService.BookingCallback() {
            @Override
            public void onSuccess(Booking booking) {
                Toast.makeText(NewBookingActivity.this, "Booking created successfully!", Toast.LENGTH_SHORT).show();
                finish();  // Close the activity or go back
            }

            @Override
            public void onError(String error) {
                Toast.makeText(NewBookingActivity.this, "Failed to create booking: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        if (bottomNavigation == null) return;

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            else if (itemId == R.id.nav_bookings) startActivity(new Intent(this, BookingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            else if (itemId == R.id.nav_messages) startActivity(new Intent(this, MessagesActivity.class));
            else if (itemId == R.id.nav_profile) startActivity(new Intent(this, UserProfileActivity.class));
            return true;
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) takePhoto();
            else Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
        }
    }
}
