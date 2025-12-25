package com.example.homerepairs;

import android.Manifest;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
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

    private static final int REQUEST_CAMERA = 1;
    private static final int REQUEST_GALLERY = 2;
    private static final int REQUEST_PERMISSIONS = 100;

    private EditText etIssueDescription;
    private Button btnEmergency, btnSameDay, btnScheduleLater;
    private Button btnTakePhoto, btnUploadFromGallery;
    private Button btnNext;
    
    private MaterialCardView cardPhoto1, cardPhoto2, cardPhoto3;
    private ImageView ivPhoto1, ivPhoto2, ivPhoto3;
    private ImageView ivAddPhoto1, ivAddPhoto2, ivAddPhoto3;
    
    private String selectedUrgency = "";
    private List<Uri> selectedPhotos = new ArrayList<>();
    private List<Bitmap> selectedPhotoBitmaps = new ArrayList<>(); // Store Bitmaps for upload
    private int currentPhotoIndex = 0;
    private FirebaseBookingService bookingService;
    private FirebaseStorageService storageService;
    private TextView tvServiceName;
    private TextView tvServiceCategory;
    private TextView tvLocationName;
    private TextView tvLocationAddress;
    private boolean isInitializingBottomNav = true; // Flag to prevent navigation during initialization

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_new_booking);

            // Initialize services
            bookingService = new FirebaseBookingService();
            storageService = new FirebaseStorageService();

            initializeViews();
            setupBackButton();
            setupUrgencyButtons();
            setupPhotoButtons();
            setupNextButton();
            setupBottomNavigation();
            
            android.util.Log.d("NewBookingActivity", "Activity created successfully");
        } catch (Exception e) {
            android.util.Log.e("NewBookingActivity", "Error in onCreate", e);
            Toast.makeText(this, "Error loading booking screen: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish(); // Close activity if there's an error
        }
    }

    private void initializeViews() {
        try {
            etIssueDescription = findViewById(R.id.etIssueDescription);
            
            btnEmergency = findViewById(R.id.btnEmergency);
            btnSameDay = findViewById(R.id.btnSameDay);
            btnScheduleLater = findViewById(R.id.btnScheduleLater);
            
            btnTakePhoto = findViewById(R.id.btnTakePhoto);
            btnUploadFromGallery = findViewById(R.id.btnUploadFromGallery);
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
            
            // Service display views
            tvServiceName = findViewById(R.id.tvServiceName);
            tvServiceCategory = findViewById(R.id.tvServiceCategory);
            
            // Property location views
            tvLocationName = findViewById(R.id.tvLocationName);
            tvLocationAddress = findViewById(R.id.tvLocationAddress);
            
            // Initialize photo bitmaps list
            selectedPhotoBitmaps = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                selectedPhotoBitmaps.add(null);
            }
            
            // Check for null views
            if (btnNext == null) {
                android.util.Log.e("NewBookingActivity", "btnNext is null - check layout file");
            }
            if (etIssueDescription == null) {
                android.util.Log.e("NewBookingActivity", "etIssueDescription is null - check layout file");
            }
            
            // Populate service information from intent
            populateServiceInfo();
        } catch (Exception e) {
            android.util.Log.e("NewBookingActivity", "Error initializing views", e);
            throw e; // Re-throw to be caught by onCreate
        }
    }
    
    private void populateServiceInfo() {
        // Get service category from intent (this is always passed from ProviderProfileActivity)
        String serviceCategory = getIntent().getStringExtra("service_category");
        if (tvServiceCategory != null) {
            if (serviceCategory != null && !serviceCategory.isEmpty()) {
                tvServiceCategory.setText(serviceCategory);
            } else {
                // Default category if not provided
                tvServiceCategory.setText("General");
            }
        }
        
        // Get service name from intent (if provided)
        // If not provided, use a default based on the category or leave as default from layout
        String serviceName = getIntent().getStringExtra("service_name");
        if (tvServiceName != null) {
            if (serviceName != null && !serviceName.isEmpty()) {
                tvServiceName.setText(serviceName);
            }
            // If no service name provided, keep the default from layout (e.g., "Leaky faucet")
            // or set a generic one
            else if (tvServiceName.getText().toString().trim().isEmpty()) {
                tvServiceName.setText("Service Request");
            }
        }
    }

    private void setupBackButton() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupUrgencyButtons() {
        // Reset all buttons to default state
        resetUrgencyButtons();
        
        btnEmergency.setOnClickListener(v -> {
            selectedUrgency = "Emergency";
            selectUrgencyButton(btnEmergency);
            deselectUrgencyButton(btnSameDay);
            deselectUrgencyButton(btnScheduleLater);
        });

        btnSameDay.setOnClickListener(v -> {
            selectedUrgency = "Same Day";
            selectUrgencyButton(btnSameDay);
            deselectUrgencyButton(btnEmergency);
            deselectUrgencyButton(btnScheduleLater);
        });

        btnScheduleLater.setOnClickListener(v -> {
            selectedUrgency = "Schedule Later";
            selectUrgencyButton(btnScheduleLater);
            deselectUrgencyButton(btnEmergency);
            deselectUrgencyButton(btnSameDay);
        });
    }

    private void resetUrgencyButtons() {
        deselectUrgencyButton(btnEmergency);
        deselectUrgencyButton(btnSameDay);
        deselectUrgencyButton(btnScheduleLater);
    }

    private void selectUrgencyButton(Button button) {
        button.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.primary_blue));
        button.setTextColor(getResources().getColor(R.color.text_white));
    }

    private void deselectUrgencyButton(Button button) {
        button.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.background_gray));
        button.setTextColor(getResources().getColor(R.color.text_primary));
    }

    private void setupPhotoButtons() {
        btnTakePhoto.setOnClickListener(v -> {
            if (checkCameraPermission()) {
                openCamera();
            } else {
                requestCameraPermission();
            }
        });

        btnUploadFromGallery.setOnClickListener(v -> {
            if (checkStoragePermission()) {
                openGallery();
            } else {
                requestStoragePermission();
            }
        });

        // Set click listeners for photo cards
        cardPhoto1.setOnClickListener(v -> {
            currentPhotoIndex = 0;
            showPhotoOptions();
        });

        cardPhoto2.setOnClickListener(v -> {
            currentPhotoIndex = 1;
            showPhotoOptions();
        });

        cardPhoto3.setOnClickListener(v -> {
            currentPhotoIndex = 2;
            showPhotoOptions();
        });
    }

    private void showPhotoOptions() {
        String[] options = {"Take Photo", "Choose from Gallery", "Remove Photo"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Photo Options")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        if (checkCameraPermission()) {
                            openCamera();
                        } else {
                            requestCameraPermission();
                        }
                    } else if (which == 1) {
                        if (checkStoragePermission()) {
                            openGallery();
                        } else {
                            requestStoragePermission();
                        }
                    } else if (which == 2) {
                        removePhoto(currentPhotoIndex);
                    }
                })
                .show();
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean checkStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSIONS);
    }

    private void requestStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, REQUEST_PERMISSIONS);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS);
        }
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_CAMERA);
        } else {
            Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

            if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CAMERA && data != null) {
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                if (photo != null) {
                    setPhoto(currentPhotoIndex, photo);
                    // Store bitmap for upload
                    if (currentPhotoIndex < selectedPhotoBitmaps.size()) {
                        selectedPhotoBitmaps.set(currentPhotoIndex, photo);
                    } else {
                        // Expand list if needed
                        while (selectedPhotoBitmaps.size() <= currentPhotoIndex) {
                            selectedPhotoBitmaps.add(null);
                        }
                        selectedPhotoBitmaps.set(currentPhotoIndex, photo);
                    }
                }
            } else if (requestCode == REQUEST_GALLERY && data != null) {
                Uri selectedImage = data.getData();
                if (selectedImage != null) {
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImage);
                        setPhoto(currentPhotoIndex, bitmap);
                        selectedPhotos.add(selectedImage);
                        // Store bitmap for upload
                        if (currentPhotoIndex < selectedPhotoBitmaps.size()) {
                            selectedPhotoBitmaps.set(currentPhotoIndex, bitmap);
                        } else {
                            // Expand list if needed
                            while (selectedPhotoBitmaps.size() <= currentPhotoIndex) {
                                selectedPhotoBitmaps.add(null);
                            }
                            selectedPhotoBitmaps.set(currentPhotoIndex, bitmap);
                        }
                    } catch (IOException e) {
                        android.util.Log.e("NewBookingActivity", "Error loading image", e);
                        Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    private void setPhoto(int index, Bitmap bitmap) {
        ImageView photoView = null;
        ImageView addIcon = null;
        MaterialCardView card = null;

        switch (index) {
            case 0:
                photoView = ivPhoto1;
                addIcon = ivAddPhoto1;
                card = cardPhoto1;
                break;
            case 1:
                photoView = ivPhoto2;
                addIcon = ivAddPhoto2;
                card = cardPhoto2;
                break;
            case 2:
                photoView = ivPhoto3;
                addIcon = ivAddPhoto3;
                card = cardPhoto3;
                break;
        }

        if (photoView != null && addIcon != null && card != null) {
            photoView.setImageBitmap(bitmap);
            photoView.setVisibility(View.VISIBLE);
            addIcon.setVisibility(View.GONE);
            card.setCardBackgroundColor(getResources().getColor(R.color.white));
        }
    }

    private void removePhoto(int index) {
        ImageView photoView = null;
        ImageView addIcon = null;
        MaterialCardView card = null;

        switch (index) {
            case 0:
                photoView = ivPhoto1;
                addIcon = ivAddPhoto1;
                card = cardPhoto1;
                break;
            case 1:
                photoView = ivPhoto2;
                addIcon = ivAddPhoto2;
                card = cardPhoto2;
                break;
            case 2:
                photoView = ivPhoto3;
                addIcon = ivAddPhoto3;
                card = cardPhoto3;
                break;
        }

        if (photoView != null && addIcon != null && card != null) {
            photoView.setVisibility(View.GONE);
            addIcon.setVisibility(View.VISIBLE);
            card.setCardBackgroundColor(getResources().getColor(R.color.background_gray));
            
            // Remove from both lists
            if (index < selectedPhotos.size()) {
                selectedPhotos.remove(index);
            }
            if (index < selectedPhotoBitmaps.size()) {
                selectedPhotoBitmaps.set(index, null);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (permissions[0].equals(Manifest.permission.CAMERA)) {
                    openCamera();
                } else {
                    openGallery();
                }
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupNextButton() {
        btnNext.setOnClickListener(v -> {
            String issueDescription = etIssueDescription.getText().toString().trim();
            
            if (issueDescription.isEmpty()) {
                Toast.makeText(this, "Please describe the issue", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (selectedUrgency.isEmpty()) {
                Toast.makeText(this, "Please select urgency", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Get data from intent
            String serviceCategoryTemp = getIntent().getStringExtra("service_category");
            final String serviceCategory = (serviceCategoryTemp == null || serviceCategoryTemp.isEmpty()) 
                    ? "Plumbing" : serviceCategoryTemp; // Default category
            
            final String providerName = getIntent().getStringExtra("provider_name");
            final String providerId = getIntent().getStringExtra("provider_id");
            
            // Get service name
            String serviceNameTemp = getIntent().getStringExtra("service_name");
            final String serviceName = (serviceNameTemp == null || serviceNameTemp.isEmpty()) 
                    ? (tvServiceName != null ? tvServiceName.getText().toString() : "Service Request") 
                    : serviceNameTemp;
            
            // Get property location data
            String propertyName = tvLocationName != null ? tvLocationName.getText().toString() : "Home";
            String propertyAddress = tvLocationAddress != null ? tvLocationAddress.getText().toString() : "";
            final String propertyLocation = propertyAddress.isEmpty() ? propertyName : propertyName + ", " + propertyAddress;
            
            // Get current user ID
            final String userId = getCurrentUserId();
            if (userId == null) {
                // User not authenticated, redirect to login in sign in mode
                android.widget.Toast.makeText(this, "Please log in to create a booking", 
                    android.widget.Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, SignInActivity.class);
                intent.putExtra("mode", "sign_in");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return;
            }
            
            // Show loading
            btnNext.setEnabled(false);
            btnNext.setText("Creating...");
            
            android.util.Log.d("NewBookingActivity", "Attempting to create booking...");
            android.util.Log.d("NewBookingActivity", "Booking details - UserId: " + userId + ", ProviderId: " + providerId + ", ServiceCategory: " + serviceCategory);
            
            // Check services
            if (bookingService == null) {
                android.util.Log.e("NewBookingActivity", "BookingService is null!");
                btnNext.setEnabled(true);
                btnNext.setText("Next");
                Toast.makeText(this, "Booking service not initialized. Please try again.", Toast.LENGTH_LONG).show();
                return;
            }
            
            if (storageService == null) {
                android.util.Log.e("NewBookingActivity", "StorageService is null!");
                btnNext.setEnabled(true);
                btnNext.setText("Next");
                Toast.makeText(this, "Storage service not initialized. Please try again.", Toast.LENGTH_LONG).show();
                return;
            }
            
            // Collect valid photo bitmaps
            final List<Bitmap> photosToUpload = new ArrayList<>();
            for (Bitmap bitmap : selectedPhotoBitmaps) {
                if (bitmap != null) {
                    photosToUpload.add(bitmap);
                }
            }
            
            // Create booking first, then upload images with the actual booking ID
            createBookingWithData(userId, providerId, providerName, serviceCategory, serviceName, 
                    issueDescription, selectedUrgency, propertyLocation, propertyName, photosToUpload);
        });
    }
    
    private void createBookingWithData(String userId, String providerId, String providerName, 
            String serviceCategory, String serviceName, String issueDescription, String urgency,
            String propertyLocation, String propertyName, List<Bitmap> photosToUpload) {
        
        // Create booking object (without images first)
        Booking booking = new Booking(userId, providerId, providerName, serviceCategory, 
                serviceName, issueDescription, urgency);
        
        // Set additional booking details
        booking.setServiceDateTime("Tomorrow, 10:00 AM - 11:00 AM"); // Default time slot
        booking.setPropertyLocation(propertyLocation);
        booking.setPropertyName(propertyName);
        booking.setPhotoUrls(new ArrayList<>()); // Will be updated after image upload
        
        // Update button text
        btnNext.setText("Creating booking...");
        
        android.util.Log.d("NewBookingActivity", "Creating booking with all data...");
        android.util.Log.d("NewBookingActivity", "Property Location: " + propertyLocation);
        android.util.Log.d("NewBookingActivity", "Photos to upload: " + photosToUpload.size());
        
        // Create booking in Firebase first
        bookingService.createBooking(booking, new FirebaseBookingService.BookingCallback() {
            @Override
            public void onSuccess(Booking createdBooking) {
                // Booking created successfully
                android.util.Log.d("NewBookingActivity", "Booking created successfully with ID: " + createdBooking.getId());
                android.util.Log.d("NewBookingActivity", "Booking Reference: " + createdBooking.getBookingReference());
                
                final String bookingId = createdBooking.getId();
                
                // Upload images with the actual booking ID
                if (!photosToUpload.isEmpty() && bookingId != null) {
                    android.util.Log.d("NewBookingActivity", "Uploading " + photosToUpload.size() + " images with booking ID: " + bookingId);
                    btnNext.setText("Uploading images...");
                    
                    storageService.uploadMultipleImages(photosToUpload, bookingId, new FirebaseStorageService.MultipleImageUploadCallback() {
                        @Override
                        public void onSuccess(List<String> imageUrls) {
                            android.util.Log.d("NewBookingActivity", "All images uploaded successfully. URLs: " + imageUrls.size());
                            
                            // Update booking with image URLs
                            createdBooking.setPhotoUrls(imageUrls);
                            bookingService.updateBooking(bookingId, createdBooking, new FirebaseBookingService.BookingCallback() {
                                @Override
                                public void onSuccess(Booking updatedBooking) {
                                    android.util.Log.d("NewBookingActivity", "Booking updated with image URLs");
                                    navigateToConfirmation(updatedBooking, issueDescription, urgency, serviceName, 
                                            serviceCategory, propertyLocation, providerName);
                                }
                                
                                @Override
                                public void onError(String error) {
                                    android.util.Log.e("NewBookingActivity", "Error updating booking with images: " + error);
                                    // Continue anyway - booking is created, images are uploaded
                                    Toast.makeText(NewBookingActivity.this, "Booking created, but failed to save image links", Toast.LENGTH_SHORT).show();
                                    navigateToConfirmation(createdBooking, issueDescription, urgency, serviceName, 
                                            serviceCategory, propertyLocation, providerName);
                                }
                            });
                        }
                        
                        @Override
                        public void onError(String error) {
                            android.util.Log.e("NewBookingActivity", "Image upload error: " + error);
                            // Continue with booking even if image upload fails
                            Toast.makeText(NewBookingActivity.this, "Booking created, but some images failed to upload", Toast.LENGTH_LONG).show();
                            navigateToConfirmation(createdBooking, issueDescription, urgency, serviceName, 
                                    serviceCategory, propertyLocation, providerName);
                        }
                        
                        @Override
                        public void onProgress(int uploaded, int total) {
                            android.util.Log.d("NewBookingActivity", "Image upload progress: " + uploaded + "/" + total);
                            btnNext.setText("Uploading images... " + uploaded + "/" + total);
                        }
                    });
                } else {
                    // No images to upload, navigate directly
                    navigateToConfirmation(createdBooking, issueDescription, urgency, serviceName, 
                            serviceCategory, propertyLocation, providerName);
                }
            }
            
            @Override
            public void onError(String error) {
                // Booking creation failed
                android.util.Log.e("NewBookingActivity", "Booking creation error: " + error);
                btnNext.setEnabled(true);
                btnNext.setText("Next");
                
                // Show user-friendly error message
                String userMessage = "Failed to create booking";
                if (error != null && (error.contains("SecurityException") || error.contains("Unknown calling package"))) {
                    userMessage = "Firebase configuration issue. Please check your internet connection and try again.";
                } else if (error != null) {
                    userMessage = "Error: " + error;
                }
                
                Toast.makeText(NewBookingActivity.this, userMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void navigateToConfirmation(Booking booking, String issueDescription, String urgency, 
            String serviceName, String serviceCategory, String propertyLocation, String providerName) {
        Toast.makeText(NewBookingActivity.this, "Booking created successfully!", Toast.LENGTH_SHORT).show();
        
        // Navigate to booking confirmation
        Intent intent = new Intent(NewBookingActivity.this, BookingConfirmedActivity.class);
        intent.putExtra("issueDescription", issueDescription);
        intent.putExtra("urgency", urgency);
        intent.putExtra("serviceName", serviceName);
        intent.putExtra("serviceCategory", serviceCategory);
        intent.putExtra("bookingReference", booking.getBookingReference());
        intent.putExtra("bookingId", booking.getId());
        intent.putExtra("propertyLocation", propertyLocation);
        
        if (providerName != null && !providerName.isEmpty()) {
            intent.putExtra("provider_name", providerName);
        }
        
        intent.putExtra("serviceDateTime", booking.getServiceDateTime());
        intent.putExtra("hasPhotos", booking.getPhotoUrls() != null && !booking.getPhotoUrls().isEmpty());
        
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        finish(); // Close this activity after navigating
    }
    
    /**
     * Get current user ID using AuthHelper
     */
    private String getCurrentUserId() {
        return com.example.homerepairs.utils.AuthHelper.getCurrentUserId(this);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        if (bottomNavigation == null) {
            android.util.Log.w("NewBookingActivity", "BottomNavigationView not found");
            return;
        }
        
        bottomNavigation.setOnItemSelectedListener(item -> {
            // Ignore selections during initialization to prevent auto-navigation
            if (isInitializingBottomNav) {
                android.util.Log.d("NewBookingActivity", "Ignoring bottom nav selection during initialization");
                return false;
            }
            
            int itemId = item.getItemId();
            android.util.Log.d("NewBookingActivity", "Bottom nav item selected: " + itemId);
            
            if (itemId == R.id.nav_home) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish();
                return true;
            } else if (itemId == R.id.nav_bookings) {
                Intent intent = new Intent(this, BookingsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
                return true;
            } else if (itemId == R.id.nav_messages) {
                Intent intent = new Intent(this, MessagesActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                Intent intent = new Intent(this, UserProfileActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
                return true;
            }
            return false;
        });
        
        // Don't set selected item - it would trigger navigation to MainActivity
        // Mark initialization as complete after a short delay to allow UI to settle
        bottomNavigation.post(() -> {
            isInitializingBottomNav = false;
            android.util.Log.d("NewBookingActivity", "Bottom navigation initialization complete");
        });
    }
}




