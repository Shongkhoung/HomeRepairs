package com.example.homerepairs;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import java.io.IOException;
import java.io.FileNotFoundException;
import com.example.homerepairs.services.FirebaseStorageService;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.widget.FrameLayout;
import com.airbnb.lottie.LottieAnimationView;
import com.example.homerepairs.utils.AuthHelper;
import com.example.homerepairs.services.FirebaseUserService;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.Timestamp;
import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import android.net.Uri;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.widget.ImageView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;

public class UserProfileActivity extends AppCompatActivity {

    private FrameLayout llScreenLoading;
    private LottieAnimationView ivScreenLoading;
    private long loadingStartTime = 0;
    private static final long MIN_LOADING_DURATION = 1500; // Minimum 1.5 seconds

    private FirebaseUserService userService;
    private FirebaseStorageService storageService;
    private ListenerRegistration userProfileListener;

    // Current user data for editing
    private String currentUserName;
    private String currentUserEmail;
    private String currentUserPhone;
    private String currentUserLocation;
    private static final int CAMERA_REQUEST_CODE = 101;
    private static final int PICK_IMAGE_REQUEST = 102;
    private Uri imageUri;

    private ImageView ivProfileImage;
    private TextView tvAvatarLetter;

    private MaterialCardView cardAvatar;
    private Bitmap selectedBitmap;
    private String currentUserPhotoUrl;

    // Request codes
    private static final int REQUEST_IMAGE_PICK = 1001;
    private static final int REQUEST_PERMISSION_READ_MEDIA = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);
        ivProfileImage = findViewById(R.id.ivProfileImage);
        tvAvatarLetter = findViewById(R.id.tvAvatarLetter);
        cardAvatar = findViewById(R.id.cardAvatar);


        View btnPrivacy = findViewById(R.id.btnPrivacy);

        btnPrivacy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to PrivacyActivity
                Intent intent = new Intent(UserProfileActivity.this, PrivacyAndSecurityActivity.class);
                startActivity(intent);
                ;
            }
        });

        View btnTerms = findViewById(R.id.btnTerms);

        btnTerms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to PrivacyActivity
                Intent intent = new Intent(UserProfileActivity.this, TermsConditionsActivity.class);
                startActivity(intent);
                ;
            }
        });

        View btnHelpcenter = findViewById(R.id.btnHelpCenter);

        btnHelpcenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to PrivacyActivity
                Intent intent = new Intent(UserProfileActivity.this, HelpCenterActivity.class);
                startActivity(intent);
                ;
            }
        });


        // Check if user is authenticated
        String userId = AuthHelper.getCurrentUserId(this);
        if (userId == null) {
            // User not authenticated, redirect to login
            Intent intent = new Intent(this, SignInActivity.class);
            intent.putExtra("mode", "sign_in");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // Set status bar color to white to match screen background
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));
            // Set dark status bar icons (dark icons on light background)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getWindow().getDecorView().setSystemUiVisibility(
                        getWindow().getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                );
            }
        }

        userService = new FirebaseUserService();
        storageService = new FirebaseStorageService();

        initializeViews();
        setupClickListeners();
        setupBottomNavigation();
        loadUserData();

        // Wait for layout to be ready before hiding loading overlay
        waitForLayoutReady();
    }
    // Upload the selected profile photo to Firebase Storage
    private void uploadProfilePhotoToFirebase(Uri imageUri) {
        // Display loading message or progress
        Toast.makeText(this, "Uploading profile photo...", Toast.LENGTH_SHORT).show();

        // Get the current user's ID (you can replace this with the actual user ID)
        String userId = AuthHelper.getCurrentUserId(this);

        if (userId != null) {
            // Reference to Firebase Storage
            StorageReference storageReference = FirebaseStorage.getInstance().getReference()
                    .child("profile_photos/" + userId + ".jpg");

            // Upload the image to Firebase Storage
            storageReference.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        // Get the image URL from Firebase Storage
                        storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();
                            // Save the image URL to Firestore and Firebase Auth
                            saveProfileImageUrlToFirestore(imageUrl);
                            updateProfilePhotoInAuth(imageUrl);
                        });
                    })
                    .addOnFailureListener(e -> {
                        // Handle failure
                        Toast.makeText(UserProfileActivity.this, "Failed to upload profile photo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
        }
    }

    // Save the image URL to Firestore
    private void saveProfileImageUrlToFirestore(String imageUrl) {
        String userId = AuthHelper.getCurrentUserId(this);
        if (userId != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference userRef = db.collection("users").document(userId);

            // Update Firestore with the new profile image URL
            userRef.update("photoUrl", imageUrl)
                    .addOnSuccessListener(aVoid -> {
                        // Image URL has been updated successfully
                        Toast.makeText(UserProfileActivity.this, "Profile photo updated successfully!", Toast.LENGTH_SHORT).show();
                        loadUserData();  // Reload the profile data to reflect the new image
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(UserProfileActivity.this, "Failed to update profile photo in Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    // Update the user's profile photo in Firebase Authentication
    private void updateProfilePhotoInAuth(String imageUrl) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setPhotoUri(Uri.parse(imageUrl))  // Set the new photo URL
                    .build();

            firebaseUser.updateProfile(profileUpdates)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Profile photo updated successfully in Firebase Auth
                            Toast.makeText(UserProfileActivity.this, "Profile photo updated in Firebase Auth!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(UserProfileActivity.this, "Failed to update profile photo in Firebase Auth", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }


    // Handle permission request result (for camera)

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove Firestore listener when activity is destroyed
        if (userProfileListener != null) {
            userProfileListener.remove();
            userProfileListener = null;
        }
    }

    private void initializeViews() {
        // Views are already defined in XML, we'll just set up listeners
        llScreenLoading = findViewById(R.id.llScreenLoading);
        ivScreenLoading = findViewById(R.id.ivScreenLoading);
    }

    private void setupClickListeners() {
        // Edit button
        ImageButton btnEdit = findViewById(R.id.btnEdit);
        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> {
                // Open personal information edit
                openPersonalInfo();
            });
        }

        // Camera icon on avatar
        View cardAvatar = findViewById(R.id.cardAvatar);
        if (cardAvatar != null) {
            cardAvatar.setOnClickListener(v -> {
                openImagePicker();
            });
        }

        // Also make the avatar itself clickable
        View avatarContainer = findViewById(R.id.avatarContainer);
        if (avatarContainer != null) {
            avatarContainer.setOnClickListener(v -> {
                openImagePicker();
            });
        }

        // Menu items
        findViewById(R.id.btnPersonalInfo).setOnClickListener(v -> openPersonalInfo());
        findViewById(R.id.btnPaymentMethods).setOnClickListener(v -> {
            android.widget.Toast.makeText(this, "Payment Methods - Coming soon",
                    android.widget.Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.btnAddresses).setOnClickListener(v -> {
            android.widget.Toast.makeText(this, "Addresses - Coming soon",
                    android.widget.Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.btnNotifications).setOnClickListener(v -> {
            android.widget.Toast.makeText(this, "Notifications - Coming soon",
                    android.widget.Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.btnAppSettings).setOnClickListener(v -> {
            android.widget.Toast.makeText(this, "App Settings - Coming soon",
                    android.widget.Toast.LENGTH_SHORT).show();
        });



        findViewById(R.id.btnInviteFriends).setOnClickListener(v -> {
            android.widget.Toast.makeText(this, "Invite Friends - Coming soon",
                    android.widget.Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.btnLogout).setOnClickListener(v -> handleLogout());
    }

    private void openPersonalInfo() {
        // Create and show edit profile dialog
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_edit_profile);
        dialog.getWindow().setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        );
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        // Get dialog views
        com.google.android.material.textfield.TextInputEditText etFullName =
                dialog.findViewById(R.id.etFullName);
        com.google.android.material.textfield.TextInputEditText etEmail =
                dialog.findViewById(R.id.etEmail);
        com.google.android.material.textfield.TextInputEditText etPhone =
                dialog.findViewById(R.id.etPhone);
        com.google.android.material.textfield.TextInputEditText etLocation =
                dialog.findViewById(R.id.etLocation);
        ImageButton btnClose = dialog.findViewById(R.id.btnClose);
        com.google.android.material.button.MaterialButton btnCancel =
                dialog.findViewById(R.id.btnCancel);
        com.google.android.material.button.MaterialButton btnSave =
                dialog.findViewById(R.id.btnSave);

        // Pre-fill with current data
        if (etFullName != null) {
            etFullName.setText(currentUserName != null ? currentUserName : "");
        }
        if (etEmail != null) {
            etEmail.setText(currentUserEmail != null ? currentUserEmail : "");
        }
        if (etPhone != null) {
            etPhone.setText(currentUserPhone != null ? currentUserPhone : "");
        }
        if (etLocation != null) {
            etLocation.setText(currentUserLocation != null ? currentUserLocation : "");
        }

        // Close button
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        // Cancel button
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        // Save button
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                String name = etFullName != null ? etFullName.getText().toString().trim() : "";
                String phone = etPhone != null ? etPhone.getText().toString().trim() : "";
                String location = etLocation != null ? etLocation.getText().toString().trim() : "";

                // Validate
                if (name.isEmpty()) {
                    android.widget.Toast.makeText(this, "Please enter your full name",
                            android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }

                // Save changes
                saveProfileChanges(name, phone, location, dialog);
            });
        }

        dialog.show();
    }

    private void saveProfileChanges(String name, String phone, String location, android.app.Dialog dialog) {
        String userId = AuthHelper.getCurrentUserId(this);
        if (userId == null) {
            android.widget.Toast.makeText(this, "User not authenticated",
                    android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading
        android.widget.ProgressBar progressBar = new android.widget.ProgressBar(this);
        progressBar.setIndeterminate(true);

        // Prepare updates
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("name", name);
        updates.put("phone", phone);
        updates.put("location", location);
        updates.put("updatedAt", com.google.firebase.Timestamp.now());

        // Update Firestore
        userService.updateUserProfile(userId, updates, new FirebaseUserService.UserProfileCallback() {
            @Override
            public void onSuccess(java.util.Map<String, Object> userProfile) {
                // Update local variables
                currentUserName = name;
                currentUserPhone = phone;
                currentUserLocation = location;

                // Update Firebase Auth display name
                FirebaseUser user = AuthHelper.getCurrentUser();
                if (user != null) {
                    com.google.firebase.auth.UserProfileChangeRequest profileUpdates =
                            new com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();
                    user.updateProfile(profileUpdates);
                }

                // Save name locally
                AuthHelper.saveUserName(UserProfileActivity.this, name);

                // Dismiss dialog
                dialog.dismiss();

                // Show success message
                android.widget.Toast.makeText(UserProfileActivity.this, "Profile updated successfully",
                        android.widget.Toast.LENGTH_SHORT).show();

                // Reload user data to refresh UI
                loadUserData();
            }

            @Override
            public void onError(String error) {
                android.util.Log.e("UserProfileActivity", "Error updating profile: " + error);
                android.widget.Toast.makeText(UserProfileActivity.this,
                        "Failed to update profile: " + error,
                        android.widget.Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleLogout() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Log Out", (dialog, which) -> {
                    // Sign out from Firebase
                    AuthHelper.signOut();

                    // Navigate to login screen in sign in mode (not create account)
                    Intent intent = new Intent(this, SignInActivity.class);
                    intent.putExtra("mode", "sign_in");
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();

                    android.widget.Toast.makeText(this, "Logged out successfully",
                            android.widget.Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);

        // Remove elevation/shadow to eliminate divider
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bottomNavigation.setElevation(0f);
        }

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                showScreenLoading(true);
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish();
                return true;
            } else if (itemId == R.id.nav_bookings) {
                showScreenLoading(true);
                Intent intent = new Intent(this, BookingsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
                return true;
            } else if (itemId == R.id.nav_messages) {
                showScreenLoading(true);
                Intent intent = new Intent(this, MessagesActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                // Already on profile screen
                return true;
            }
            return false;
        });
        bottomNavigation.setSelectedItemId(R.id.nav_profile);
    }

    /**
     * Show or hide screen transition loading overlay
     * @param show true to show, false to hide
     */
    private void showScreenLoading(boolean show) {
        if (llScreenLoading == null) {
            return;
        }

        if (show) {
            llScreenLoading.setVisibility(View.VISIBLE);
            llScreenLoading.setAlpha(0f);
            llScreenLoading.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
            // Start Lottie animation with smooth settings
            if (ivScreenLoading != null) {
                ivScreenLoading.setAnimation(R.raw.loading);
                ivScreenLoading.setSpeed(1.0f);
                ivScreenLoading.setRenderMode(com.airbnb.lottie.RenderMode.HARDWARE);
                ivScreenLoading.enableMergePathsForKitKatAndAbove(true);
                ivScreenLoading.playAnimation();
            }
        } else {
            llScreenLoading.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() -> llScreenLoading.setVisibility(View.GONE))
                    .start();
        }
    }

    /**
     * Wait for layout to be fully rendered before hiding loading overlay
     * Ensures loading shows for minimum duration
     */
    private void waitForLayoutReady() {
        if (llScreenLoading == null) {
            return;
        }

        // Record start time
        loadingStartTime = System.currentTimeMillis();

        // Show loading overlay if it's not already visible
        if (llScreenLoading.getVisibility() != View.VISIBLE) {
            llScreenLoading.setVisibility(View.VISIBLE);
            llScreenLoading.setAlpha(0f);
            llScreenLoading.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
            // Start Lottie animation with smooth settings
            if (ivScreenLoading != null) {
                ivScreenLoading.setAnimation(R.raw.loading);
                ivScreenLoading.setSpeed(1.0f);
                ivScreenLoading.setRenderMode(com.airbnb.lottie.RenderMode.HARDWARE);
                ivScreenLoading.enableMergePathsForKitKatAndAbove(true);
                ivScreenLoading.playAnimation();
            }
        }

        // Get root view
        View rootView = findViewById(android.R.id.content);
        if (rootView == null) {
            // Fallback: hide after minimum duration
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> hideScreenLoading(), MIN_LOADING_DURATION);
            return;
        }

        // Wait for layout to be measured and laid out
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // Check if layout is ready (has dimensions)
                if (rootView.getWidth() > 0 && rootView.getHeight() > 0) {
                    // Remove listener to avoid multiple calls
                    rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    // Calculate remaining time to meet minimum duration
                    long elapsedTime = System.currentTimeMillis() - loadingStartTime;
                    long remainingTime = MIN_LOADING_DURATION - elapsedTime;

                    // Wait for minimum duration or additional 200ms, whichever is longer
                    long delayTime = Math.max(remainingTime, 200);
                    rootView.postDelayed(() -> hideScreenLoading(), delayTime);
                }
            }
        });
    }

    /**
     * Hide screen loading overlay with animation
     */
    private void hideScreenLoading() {
        if (llScreenLoading == null || llScreenLoading.getVisibility() != View.VISIBLE) {
            return;
        }

        // Stop Lottie animation
        if (ivScreenLoading != null) {
            ivScreenLoading.cancelAnimation();
        }

        llScreenLoading.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> llScreenLoading.setVisibility(View.GONE))
                .start();
    }

    private void loadUserData() {
        String userId = AuthHelper.getCurrentUserId(this);
        if (userId == null) {
            android.util.Log.e("UserProfileActivity", "Cannot load user data: user not authenticated");
            return;
        }

        // Get Firebase Auth user for fallback data
        FirebaseUser firebaseUser = AuthHelper.getCurrentUser();

        // Set up real-time listener for user profile
        userProfileListener = userService.listenToUserProfile(userId, new FirebaseUserService.UserProfileCallback() {
            @Override
            public void onSuccess(Map<String, Object> userProfile) {
                populateUserData(userProfile, firebaseUser);
            }

            @Override
            public void onError(String error) {
                android.util.Log.e("UserProfileActivity", "Error loading user profile: " + error);
                // Fallback to Firebase Auth data if Firestore fails
                // This handles cases where Firestore rules aren't deployed yet or permission errors
                if (firebaseUser != null) {
                    android.util.Log.d("UserProfileActivity", "Falling back to Firebase Auth data");
                    populateUserData(null, firebaseUser);
                } else {
                    android.util.Log.w("UserProfileActivity", "No Firebase Auth user available for fallback");
                }
            }
        });
    }

    /**
     * Populate UI with user data from Firestore and Firebase Auth
     */
    private void populateUserData(Map<String, Object> userProfile, FirebaseUser firebaseUser) {
        // Get all text views
        TextView tvUserName = findViewById(R.id.tvUserName);
        TextView tvEmail = findViewById(R.id.tvEmail);
        TextView tvPhone = findViewById(R.id.tvPhone);
        TextView tvLocation = findViewById(R.id.tvLocation);
        TextView tvMemberSince = findViewById(R.id.tvMemberSince);
        TextView tvBookingsCount = findViewById(R.id.tvBookingsCount);
        TextView tvReviewsCount = findViewById(R.id.tvReviewsCount);
        TextView tvFavoritesCount = findViewById(R.id.tvFavoritesCount);
        TextView tvAvatarLetter = findViewById(R.id.tvAvatarLetter);

        // Get data from Firestore (preferred) or Firebase Auth (fallback)
        String name = null;
        String email = null;
        String phone = null;
        String location = null;
        String photoUrl = null;
        Timestamp createdAt = null;

        if (userProfile != null && !userProfile.isEmpty()) {
            // Use Firestore data
            name = userProfile.get("name") != null ? userProfile.get("name").toString() : null;
            email = userProfile.get("email") != null ? userProfile.get("email").toString() : null;
            phone = userProfile.get("phone") != null ? userProfile.get("phone").toString() : null;
            location = userProfile.get("location") != null ? userProfile.get("location").toString() : null;
            photoUrl = userProfile.get("photoUrl") != null ? userProfile.get("photoUrl").toString() : null;
            createdAt = userProfile.get("createdAt") instanceof Timestamp ?
                    (Timestamp) userProfile.get("createdAt") : null;
        }

        // Store current values for editing
        currentUserName = name;
        currentUserEmail = email;
        currentUserPhone = phone;
        currentUserLocation = location;

        // Fallback to Firebase Auth if Firestore data is missing
        if (firebaseUser != null) {
            if (name == null || name.isEmpty()) {
                name = firebaseUser.getDisplayName();
            }
            if (email == null || email.isEmpty()) {
                email = firebaseUser.getEmail();
            }
            if (photoUrl == null || photoUrl.isEmpty()) {
                if (firebaseUser.getPhotoUrl() != null) {
                    photoUrl = firebaseUser.getPhotoUrl().toString();
                }
            }
        }

        // Set user name
        if (tvUserName != null) {
            tvUserName.setText(name != null && !name.isEmpty() ? name : "User");
        }

        // Set email
        if (tvEmail != null) {
            tvEmail.setText(email != null && !email.isEmpty() ? email : "No email");
        }

        // Set phone
        if (tvPhone != null) {
            tvPhone.setText(phone != null && !phone.isEmpty() ? phone : "No phone number");
        }

        // Set location
        if (tvLocation != null) {
            tvLocation.setText(location != null && !location.isEmpty() ? location : "Not set");
        }

        // Set member since date
        if (tvMemberSince != null) {
            if (createdAt != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
                String memberSince = "Member since " + dateFormat.format(createdAt.toDate());
                tvMemberSince.setText(memberSince);
            } else if (firebaseUser != null && firebaseUser.getMetadata() != null) {
                // Use Firebase Auth creation time as fallback
                long creationTime = firebaseUser.getMetadata().getCreationTimestamp();
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
                String memberSince = "Member since " + dateFormat.format(new java.util.Date(creationTime));
                tvMemberSince.setText(memberSince);
            } else {
                tvMemberSince.setText("Member");
            }
        }

        // Set booking count (placeholder - TODO: query bookings collection)
        if (tvBookingsCount != null) {
            tvBookingsCount.setText("0");
        }

        // Set reviews count (placeholder - TODO: query reviews)
        if (tvReviewsCount != null) {
            tvReviewsCount.setText("0");
        }

        // Set favorites count (placeholder - TODO: query favorites)
        if (tvFavoritesCount != null) {
            tvFavoritesCount.setText("0");
        }

        // Set avatar letter (first letter of name)
        if (tvAvatarLetter != null) {
            String firstLetter = "U";
            if (name != null && !name.isEmpty()) {
                firstLetter = name.substring(0, 1).toUpperCase();
            } else if (email != null && !email.isEmpty()) {
                firstLetter = email.substring(0, 1).toUpperCase();
            }
            tvAvatarLetter.setText(firstLetter);
        }

        // Load profile image if available
        ImageView ivProfileImage = findViewById(R.id.ivProfileImage);

        if (photoUrl != null && !photoUrl.isEmpty()) {
            // Show image, hide letter
            if (ivProfileImage != null) {
                ivProfileImage.setVisibility(View.VISIBLE);
                Glide.with(this)
                        .load(photoUrl)
                        .circleCrop()
                        .placeholder(R.color.deep_royal_blue)
                        .error(R.color.deep_royal_blue)
                        .into(ivProfileImage);
            }
            if (tvAvatarLetter != null) {
                tvAvatarLetter.setVisibility(View.GONE);
            }
            currentUserPhotoUrl = photoUrl;
        } else {
            // Show letter, hide image
            if (ivProfileImage != null) {
                ivProfileImage.setVisibility(View.GONE);
            }
            if (tvAvatarLetter != null) {
                tvAvatarLetter.setVisibility(View.VISIBLE);
            }
            currentUserPhotoUrl = null;
        }
    }

    /**
     * Open image picker to select profile picture
     */
    private void openImagePicker() {
        // Check permission for Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        REQUEST_PERMISSION_READ_MEDIA);
                return;
            }
        } else {
            // For Android 12 and below
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_PERMISSION_READ_MEDIA);
                return;
            }
        }

        // Create intent to pick image
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Profile Picture"), REQUEST_IMAGE_PICK);
    }


}

