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

import com.google.android.material.card.MaterialCardView;
import com.example.homerepairs.models.Booking;
import com.example.homerepairs.services.FirebaseBookingService;
import com.example.homerepairs.services.FirebaseStorageService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NewBookingActivity extends BaseActivity {

    private static final int REQUEST_CAMERA = 1;
    private static final int REQUEST_GALLERY = 2;
    private static final int REQUEST_DATE_TIME = 200;
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
    // private boolean isInitializingBottomNav = true; // Flag removed
    private String selectedServiceDateTime = "";

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
        // Get service category from intent
        String serviceCategory = getIntent().getStringExtra("service_category");
        String serviceName = getIntent().getStringExtra("service_name");

        boolean hasServiceInfo = serviceName != null && !serviceName.isEmpty();

        if (tvServiceName != null && tvServiceCategory != null) {
            if (hasServiceInfo) {
                // Pre-filled from Provider Profile
                tvServiceName.setText(serviceName);
                tvServiceCategory.setText(serviceCategory != null ? serviceCategory : "General");

                // Hide dropdown, disable click
                View dropdown = findViewById(R.id.ivServiceDropdown);
                if (dropdown != null)
                    dropdown.setVisibility(View.GONE);

                View cardService = findViewById(R.id.cardService);
                if (cardService != null) {
                    cardService.setClickable(false);
                    cardService.setFocusable(false);
                }
            } else {
                // Fresh Booking - User needs to select
                tvServiceName.setText(getString(R.string.select_service));
                tvServiceCategory.setText(getString(R.string.tap_to_choose));

                // Show dropdown, enable click
                View dropdown = findViewById(R.id.ivServiceDropdown);
                if (dropdown != null)
                    dropdown.setVisibility(View.VISIBLE);

                View cardService = findViewById(R.id.cardService);
                if (cardService != null) {
                    cardService.setOnClickListener(v -> {
                        // Launch PlumbingActivity to list all providers for selection
                        Intent intent = new Intent(NewBookingActivity.this, PlumbingActivity.class);
                        intent.putExtra("category_name", "All"); // Show all providers
                        startActivity(intent);
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    });
                }
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

            // Launch Date Time Picker
            Intent intent = new Intent(NewBookingActivity.this, ServiceDateTimeActivity.class);
            intent.putExtra("return_result", true);
            startActivityForResult(intent, REQUEST_DATE_TIME);
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
        String[] options = { getString(R.string.option_take_photo), getString(R.string.option_choose_gallery),
                getString(R.string.option_remove_photo) };
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.title_select_picture))
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
            return ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA }, REQUEST_PERMISSIONS);
    }

    private void requestStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.READ_MEDIA_IMAGES },
                    REQUEST_PERMISSIONS);
        } else {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.READ_EXTERNAL_STORAGE },
                    REQUEST_PERMISSIONS);
        }
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_CAMERA);
        } else {
            Toast.makeText(this, getString(R.string.camera_not_available), Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, getString(R.string.title_select_picture)), REQUEST_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_DATE_TIME && data != null) {
                String date = data.getStringExtra("serviceDate");
                String time = data.getStringExtra("serviceTime");
                if (date != null && time != null) {
                    selectedServiceDateTime = date + ", " + time;
                    // Format date for button: "Scheduled: Oct 12..."
                    String shortDate = date;
                    if (date.contains(",")) {
                        shortDate = date.split(",")[1].trim();
                    }
                    btnScheduleLater.setText(shortDate + " " + time);
                    Toast.makeText(this, "Selected: " + selectedServiceDateTime, Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == REQUEST_CAMERA && data != null) {
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
                        Toast.makeText(this, getString(R.string.error_loading_image), Toast.LENGTH_SHORT).show();
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (permissions[0].equals(Manifest.permission.CAMERA)) {
                    openCamera();
                } else {
                    openGallery();
                }
            } else {
                Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupNextButton() {
        btnNext.setOnClickListener(v -> {
            String issueDescription = etIssueDescription.getText().toString().trim();

            if (issueDescription.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_describe_issue), Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedUrgency.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_select_urgency), Toast.LENGTH_SHORT).show();
                return;
            }

            // Get data from intent
            String serviceCategoryTemp = getIntent().getStringExtra("service_category");
            final String serviceCategory = (serviceCategoryTemp == null || serviceCategoryTemp.isEmpty())
                    ? getString(R.string.default_service_category)
                    : serviceCategoryTemp; // Default category

            final String providerName = getIntent().getStringExtra("provider_name");
            final String providerId = getIntent().getStringExtra("provider_id");

            // Get service name
            String serviceNameTemp = getIntent().getStringExtra("service_name");
            final String serviceName = (serviceNameTemp == null || serviceNameTemp.isEmpty())
                    ? (tvServiceName != null ? tvServiceName.getText().toString()
                            : getString(R.string.title_service_request))
                    : serviceNameTemp;

            // Get property location data
            String propertyName = tvLocationName != null ? tvLocationName.getText().toString()
                    : getString(R.string.default_home);
            String propertyAddress = tvLocationAddress != null ? tvLocationAddress.getText().toString() : "";
            final String propertyLocation = propertyAddress.isEmpty() ? propertyName
                    : propertyName + ", " + propertyAddress;

            // Get current user ID
            final String userId = getCurrentUserId();
            if (userId == null) {
                // User not authenticated, redirect to login in sign in mode
                android.widget.Toast.makeText(this, getString(R.string.login_required_booking),
                        android.widget.Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.putExtra("mode", "sign_in");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return;
            }

            // Navigate to ServiceDateTimeActivity passing all data
            Intent intent = new Intent(NewBookingActivity.this, ServiceDateTimeActivity.class);

            // Pass User & Provider Info
            intent.putExtra("userId", userId);
            intent.putExtra("providerId", providerId);
            intent.putExtra("providerName", providerName);

            // Pass Service Details
            intent.putExtra("serviceCategory", serviceCategory);
            intent.putExtra("serviceName", serviceName);
            intent.putExtra("issueDescription", issueDescription);
            intent.putExtra("urgency", selectedUrgency);

            // Pass Location
            intent.putExtra("propertyLocation", propertyLocation);
            intent.putExtra("propertyName", propertyName);

            // Pass Photo URIs (convert to strings)
            ArrayList<String> photoUriStrings = new ArrayList<>();
            for (Uri uri : selectedPhotos) {
                photoUriStrings.add(uri.toString());
            }
            intent.putStringArrayListExtra("photoUris", photoUriStrings);

            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
    }

    private void createBookingWithData(String userId, String providerId, String providerName,
            String serviceCategory, String serviceName, String issueDescription, String urgency,
            String propertyLocation, String propertyName, List<Bitmap> photosToUpload) {

        // Create booking object (without images first)
        Booking booking = new Booking(userId, providerId, providerName, serviceCategory,
                serviceName, issueDescription, urgency);

        // Set additional booking details
        if (!selectedServiceDateTime.isEmpty()) {
            booking.setServiceDateTime(selectedServiceDateTime);
        } else {
            booking.setServiceDateTime(getString(R.string.default_time_slot)); // Default time slot
        }
        booking.setPropertyLocation(propertyLocation);
        booking.setPropertyName(propertyName);
        booking.setPhotoUrls(new ArrayList<>()); // Will be updated after image upload

        // Update button text
        btnNext.setText(getString(R.string.msg_creating_booking));

        android.util.Log.d("NewBookingActivity", "Creating booking with all data...");
        android.util.Log.d("NewBookingActivity", "Property Location: " + propertyLocation);
        android.util.Log.d("NewBookingActivity", "Photos to upload: " + photosToUpload.size());

        // Create booking in Firebase first
        bookingService.createBooking(booking, new FirebaseBookingService.BookingCallback() {
            @Override
            public void onSuccess(Booking createdBooking) {
                // Booking created successfully
                android.util.Log.d("NewBookingActivity",
                        "Booking created successfully with ID: " + createdBooking.getId());
                android.util.Log.d("NewBookingActivity", "Booking Reference: " + createdBooking.getBookingReference());

                final String bookingId = createdBooking.getId();

                // Upload images with the actual booking ID
                if (!photosToUpload.isEmpty() && bookingId != null) {
                    android.util.Log.d("NewBookingActivity",
                            "Uploading " + photosToUpload.size() + " images with booking ID: " + bookingId);
                    btnNext.setText(getString(R.string.msg_uploading_images));

                    storageService.uploadMultipleImages(photosToUpload, bookingId,
                            new FirebaseStorageService.MultipleImageUploadCallback() {
                                @Override
                                public void onSuccess(List<String> imageUrls) {
                                    android.util.Log.d("NewBookingActivity",
                                            "All images uploaded successfully. URLs: " + imageUrls.size());

                                    // Update booking with image URLs
                                    createdBooking.setPhotoUrls(imageUrls);
                                    bookingService.updateBooking(bookingId, createdBooking,
                                            new FirebaseBookingService.BookingCallback() {
                                                @Override
                                                public void onSuccess(Booking updatedBooking) {
                                                    android.util.Log.d("NewBookingActivity",
                                                            "Booking updated with image URLs");
                                                    navigateToConfirmation(updatedBooking, issueDescription, urgency,
                                                            serviceName,
                                                            serviceCategory, propertyLocation, providerName);
                                                }

                                                @Override
                                                public void onError(String error) {
                                                    android.util.Log.e("NewBookingActivity",
                                                            "Error updating booking with images: " + error);
                                                    // Continue anyway - booking is created, images are uploaded
                                                    Toast.makeText(NewBookingActivity.this,
                                                            getString(R.string.msg_booking_created_image_fail),
                                                            Toast.LENGTH_SHORT).show();
                                                    navigateToConfirmation(createdBooking, issueDescription, urgency,
                                                            serviceName,
                                                            serviceCategory, propertyLocation, providerName);
                                                }
                                            });
                                }

                                @Override
                                public void onError(String error) {
                                    android.util.Log.e("NewBookingActivity", "Image upload error: " + error);
                                    // Continue with booking even if image upload fails
                                    Toast.makeText(NewBookingActivity.this,
                                            getString(R.string.msg_booking_created_some_images_fail), Toast.LENGTH_LONG)
                                            .show();
                                    navigateToConfirmation(createdBooking, issueDescription, urgency, serviceName,
                                            serviceCategory, propertyLocation, providerName);
                                }

                                @Override
                                public void onProgress(int uploaded, int total) {
                                    android.util.Log.d("NewBookingActivity",
                                            "Image upload progress: " + uploaded + "/" + total);
                                    btnNext.setText(getString(R.string.msg_uploading_progress, uploaded, total));
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
                btnNext.setText(getString(R.string.btn_next));

                // Show user-friendly error message
                String userMessage = getString(R.string.error_create_booking);
                if (error != null
                        && (error.contains("SecurityException") || error.contains("Unknown calling package"))) {
                    userMessage = getString(R.string.error_firebase_config);
                } else if (error != null) {
                    userMessage = "Error: " + error;
                }

                Toast.makeText(NewBookingActivity.this, userMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void navigateToConfirmation(Booking booking, String issueDescription, String urgency,
            String serviceName, String serviceCategory, String propertyLocation, String providerName) {
        Toast.makeText(NewBookingActivity.this, getString(R.string.msg_booking_created_success), Toast.LENGTH_SHORT)
                .show();

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

}
