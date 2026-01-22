package com.example.homerepairs;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.homerepairs.databinding.ActivityUserProfileBinding;
import com.example.homerepairs.databinding.DialogEditProfileBinding;
import com.example.homerepairs.services.FirebaseStorageService;
import com.example.homerepairs.services.FirebaseUserService;
import com.example.homerepairs.utils.AuthHelper;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;

public class UserProfileActivity extends BaseActivity {

    private ActivityUserProfileBinding binding;
    private long loadingStartTime = 0;
    private static final long MIN_LOADING_DURATION = 1500;

    private FirebaseUserService userService;
    private FirebaseStorageService storageService;
    private ListenerRegistration userProfileListener;

    private String currentUserName;
    private String currentUserEmail;
    private String currentUserPhone;
    private String currentUserLocation;
    private String currentUserPhotoUrl;

    private static final int REQUEST_IMAGE_PICK = 1001;
    private static final int REQUEST_PERMISSION_READ_MEDIA = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String userId = AuthHelper.getCurrentUserId(this);
        if (userId == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.putExtra("mode", "sign_in");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        userService = new FirebaseUserService();
        storageService = new FirebaseStorageService();

        setupClickListeners();
        setupBottomNavigation();
        loadUserData();
        waitForLayoutReady();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userProfileListener != null) {
            userProfileListener.remove();
            userProfileListener = null;
        }
    }

    private void setupClickListeners() {
        binding.btnEdit.setOnClickListener(v -> openPersonalInfo());
        binding.ivAvatar.setOnClickListener(v -> openImagePicker());

        binding.btnPaymentMethods.setOnClickListener(v -> {
            startActivity(new Intent(this, PaymentMethodsActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
        binding.btnAddresses.setOnClickListener(v -> {
            startActivity(new Intent(this, AddressesActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
        binding.btnNotifications.setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationsActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
        binding.btnAppSettings.setOnClickListener(v -> {
            startActivity(new Intent(this, AppSettingsActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
        binding.btnPrivacy.setOnClickListener(v -> {
            startActivity(new Intent(this, PrivacySecurityActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
        binding.btnLogout.setOnClickListener(v -> handleLogout());
    }

    private void openPersonalInfo() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        DialogEditProfileBinding dialogBinding = DialogEditProfileBinding.inflate(getLayoutInflater());
        dialog.setContentView(dialogBinding.getRoot());
        dialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        dialogBinding.etFullName.setText(currentUserName != null ? currentUserName : "");
        dialogBinding.etEmail.setText(currentUserEmail != null ? currentUserEmail : "");
        dialogBinding.etPhone.setText(currentUserPhone != null ? currentUserPhone : "");
        dialogBinding.etLocation.setText(currentUserLocation != null ? currentUserLocation : "");

        dialogBinding.btnClose.setOnClickListener(v -> dialog.dismiss());
        dialogBinding.btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialogBinding.btnSave.setOnClickListener(v -> {
            String name = dialogBinding.etFullName.getText().toString().trim();
            String phone = dialogBinding.etPhone.getText().toString().trim();
            String location = dialogBinding.etLocation.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(this, getString(R.string.msg_enter_full_name), Toast.LENGTH_SHORT).show();
                return;
            }
            saveProfileChanges(name, phone, location, dialog);
        });

        dialog.show();
    }

    private void saveProfileChanges(String name, String phone, String location, android.app.Dialog dialog) {
        String userId = AuthHelper.getCurrentUserId(this);
        if (userId == null)
            return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("phone", phone);
        updates.put("location", location);
        updates.put("updatedAt", Timestamp.now());

        userService.updateUserProfile(userId, updates, new FirebaseUserService.UserProfileCallback() {
            @Override
            public void onSuccess(Map<String, Object> userProfile) {
                currentUserName = name;
                currentUserPhone = phone;
                currentUserLocation = location;

                FirebaseUser user = AuthHelper.getCurrentUser();
                if (user != null) {
                    user.updateProfile(new com.google.firebase.auth.UserProfileChangeRequest.Builder()
                            .setDisplayName(name).build());
                }
                AuthHelper.saveUserName(UserProfileActivity.this, name);
                dialog.dismiss();
                Toast.makeText(UserProfileActivity.this, getString(R.string.msg_profile_updated), Toast.LENGTH_SHORT)
                        .show();
                loadUserData();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(UserProfileActivity.this, getString(R.string.error_update_profile, error),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleLogout() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.title_logout))
                .setMessage(getString(R.string.msg_logout_confirm))
                .setPositiveButton(getString(R.string.btn_logout), (dialog, which) -> {
                    AuthHelper.signOut();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.putExtra("mode", "sign_in");
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void setupBottomNavigation() {
        binding.bottomNavigation.setElevation(0f);
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                showScreenLoading(true);
                startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                finish();
                return true;
            } else if (itemId == R.id.nav_bookings) {
                showScreenLoading(true);
                startActivity(new Intent(this, BookingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                finish();
                return true;
            } else if (itemId == R.id.nav_messages) {
                showScreenLoading(true);
                startActivity(new Intent(this, MessagesActivity.class));
                finish();
                return true;
            }
            return itemId == R.id.nav_profile;
        });
        binding.bottomNavigation.setSelectedItemId(R.id.nav_profile);
    }

    private void showScreenLoading(boolean show) {
        if (show) {
            binding.llScreenLoading.setVisibility(View.VISIBLE);
            binding.llScreenLoading.setAlpha(0f);
            binding.llScreenLoading.animate().alpha(1f).setDuration(300).start();
            binding.ivScreenLoading.setAnimation(R.raw.loading);
            binding.ivScreenLoading.playAnimation();
        } else {
            binding.llScreenLoading.animate().alpha(0f).setDuration(200)
                    .withEndAction(() -> binding.llScreenLoading.setVisibility(View.GONE)).start();
        }
    }

    private void waitForLayoutReady() {
        loadingStartTime = System.currentTimeMillis();
        if (binding.llScreenLoading.getVisibility() != View.VISIBLE) {
            showScreenLoading(true);
        }

        View rootView = findViewById(android.R.id.content);
        rootView.getViewTreeObserver()
                .addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (rootView.getWidth() > 0 && rootView.getHeight() > 0) {
                            rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            long delay = Math
                                    .max(MIN_LOADING_DURATION - (System.currentTimeMillis() - loadingStartTime), 200);
                            rootView.postDelayed(() -> hideScreenLoading(), delay);
                        }
                    }
                });
    }

    private void hideScreenLoading() {
        binding.ivScreenLoading.cancelAnimation();
        binding.llScreenLoading.animate().alpha(0f).setDuration(200)
                .withEndAction(() -> binding.llScreenLoading.setVisibility(View.GONE)).start();
    }

    private void loadUserData() {
        String userId = AuthHelper.getCurrentUserId(this);
        if (userId == null)
            return;

        FirebaseUser firebaseUser = AuthHelper.getCurrentUser();
        userProfileListener = userService.listenToUserProfile(userId, new FirebaseUserService.UserProfileCallback() {
            @Override
            public void onSuccess(Map<String, Object> userProfile) {
                populateUserData(userProfile, firebaseUser);
            }

            @Override
            public void onError(String error) {
                if (firebaseUser != null)
                    populateUserData(null, firebaseUser);
            }
        });
    }

    private void populateUserData(Map<String, Object> userProfile, FirebaseUser firebaseUser) {
        String name = null, email = null, phone = null, location = null, photoUrl = null;

        if (userProfile != null && !userProfile.isEmpty()) {
            name = (String) userProfile.get("name");
            email = (String) userProfile.get("email");
            phone = (String) userProfile.get("phone");
            location = (String) userProfile.get("location");
            photoUrl = (String) userProfile.get("photoUrl");
        }

        currentUserName = name;
        currentUserEmail = email;
        currentUserPhone = phone;
        currentUserLocation = location;

        if (firebaseUser != null) {
            if (name == null || name.isEmpty())
                name = firebaseUser.getDisplayName();
            if (email == null || email.isEmpty())
                email = firebaseUser.getEmail();
            if (photoUrl == null || photoUrl.isEmpty())
                photoUrl = firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : null;
        }

        binding.tvUserName.setText(name != null && !name.isEmpty() ? name : getString(R.string.default_user));
        binding.tvEmail.setText(email != null && !email.isEmpty() ? email : getString(R.string.msg_no_email));

        binding.tvBookingsCount.setText("0");
        binding.tvReviewsCount.setText("0");
        binding.tvFavoritesCount.setText("0");

        String letter = "U";
        if (name != null && !name.isEmpty())
            letter = name.substring(0, 1).toUpperCase();
        else if (email != null && !email.isEmpty())
            letter = email.substring(0, 1).toUpperCase();
        binding.tvAvatarLetter.setText(letter);

        // Always show letter avatar as per user request
        binding.ivProfileImage.setVisibility(View.GONE);
        binding.tvAvatarLetter.setVisibility(View.VISIBLE);
        /*
         * if (photoUrl != null && !photoUrl.isEmpty()) {
         * binding.ivProfileImage.setVisibility(View.VISIBLE);
         * binding.tvAvatarLetter.setVisibility(View.GONE);
         * Glide.with(this).load(photoUrl).circleCrop().into(binding.ivProfileImage);
         * currentUserPhotoUrl = photoUrl;
         * } else {
         * binding.ivProfileImage.setVisibility(View.GONE);
         * binding.tvAvatarLetter.setVisibility(View.VISIBLE);
         * currentUserPhotoUrl = null;
         * }
         */
    }

    private void openImagePicker() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { permission }, REQUEST_PERMISSION_READ_MEDIA);
            return;
        }
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, getString(R.string.title_select_profile_pic)),
                REQUEST_IMAGE_PICK);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_READ_MEDIA && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openImagePicker();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null)
                uploadProfileImage(imageUri);
        }
    }

    private void uploadProfileImage(Uri imageUri) {
        String userId = AuthHelper.getCurrentUserId(this);
        if (userId == null)
            return;

        android.app.ProgressDialog pd = new android.app.ProgressDialog(this);
        pd.setMessage(getString(R.string.msg_uploading_profile_pic));
        pd.setCancelable(false);
        pd.show();

        storageService.uploadProfilePhoto(imageUri, userId, new FirebaseStorageService.ImageUploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                pd.dismiss();
                Map<String, Object> updates = new HashMap<>();
                updates.put("photoUrl", imageUrl);
                updates.put("updatedAt", Timestamp.now());
                userService.updateUserProfile(userId, updates, new FirebaseUserService.UserProfileCallback() {
                    @Override
                    public void onSuccess(Map<String, Object> userProfile) {
                        FirebaseUser user = AuthHelper.getCurrentUser();
                        if (user != null)
                            user.updateProfile(new com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                    .setPhotoUri(Uri.parse(imageUrl)).build());
                        currentUserPhotoUrl = imageUrl;
                        Toast.makeText(UserProfileActivity.this, getString(R.string.msg_profile_pic_updated),
                                Toast.LENGTH_SHORT).show();
                        loadUserData();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(UserProfileActivity.this, getString(R.string.error_update_profile, error),
                                Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                pd.dismiss();
                Toast.makeText(UserProfileActivity.this, getString(R.string.error_upload_image, error),
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}
