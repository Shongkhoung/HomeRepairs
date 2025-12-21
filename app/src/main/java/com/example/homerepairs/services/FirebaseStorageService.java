package com.example.homerepairs.services;

import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FirebaseStorageService {
    private static final String TAG = "FirebaseStorageService";
    private static final String BOOKING_PHOTOS_PATH = "booking_photos";
    private FirebaseStorage storage;

    public interface ImageUploadCallback {
        void onSuccess(String imageUrl);
        void onError(String error);
    }

    public interface MultipleImageUploadCallback {
        void onSuccess(List<String> imageUrls);
        void onError(String error);
        void onProgress(int uploaded, int total);
    }

    public FirebaseStorageService() {
        try {
            storage = FirebaseStorage.getInstance();
            Log.d(TAG, "FirebaseStorageService initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing FirebaseStorageService", e);
            throw e;
        }
    }

    /**
     * Upload a single image from Bitmap to Firebase Storage
     */
    public void uploadImage(Bitmap bitmap, String bookingId, ImageUploadCallback callback) {
        if (bitmap == null) {
            callback.onError("Bitmap is null");
            return;
        }

        if (storage == null) {
            Log.e(TAG, "FirebaseStorage instance is null!");
            callback.onError("Firebase Storage is not initialized");
            return;
        }

        try {
            // Generate unique filename
            String filename = UUID.randomUUID().toString() + ".jpg";
            String path = BOOKING_PHOTOS_PATH + "/" + bookingId + "/" + filename;
            
            StorageReference storageRef = storage.getReference().child(path);

            // Convert Bitmap to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos); // 85% quality
            byte[] imageData = baos.toByteArray();

            // Upload the image
            UploadTask uploadTask = storageRef.putBytes(imageData);
            
            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // Get download URL
                    storageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri downloadUri) {
                            String imageUrl = downloadUri.toString();
                            Log.d(TAG, "Image uploaded successfully: " + imageUrl);
                            callback.onSuccess(imageUrl);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "Error getting download URL", e);
                            callback.onError("Failed to get image URL: " + e.getMessage());
                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Error uploading image", e);
                    callback.onError("Failed to upload image: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"));
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception while uploading image", e);
            callback.onError("Exception: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }

    /**
     * Upload a single image from Uri to Firebase Storage
     */
    public void uploadImage(Uri imageUri, String bookingId, ImageUploadCallback callback) {
        if (imageUri == null) {
            callback.onError("Image URI is null");
            return;
        }

        if (storage == null) {
            Log.e(TAG, "FirebaseStorage instance is null!");
            callback.onError("Firebase Storage is not initialized");
            return;
        }

        try {
            // Generate unique filename
            String filename = UUID.randomUUID().toString() + ".jpg";
            String path = BOOKING_PHOTOS_PATH + "/" + bookingId + "/" + filename;
            
            StorageReference storageRef = storage.getReference().child(path);

            // Upload the image
            UploadTask uploadTask = storageRef.putFile(imageUri);
            
            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // Get download URL
                    storageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri downloadUri) {
                            String imageUrl = downloadUri.toString();
                            Log.d(TAG, "Image uploaded successfully: " + imageUrl);
                            callback.onSuccess(imageUrl);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "Error getting download URL", e);
                            callback.onError("Failed to get image URL: " + e.getMessage());
                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Error uploading image", e);
                    callback.onError("Failed to upload image: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"));
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception while uploading image", e);
            callback.onError("Exception: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }

    /**
     * Upload multiple images from Bitmaps
     */
    public void uploadMultipleImages(List<Bitmap> bitmaps, String bookingId, MultipleImageUploadCallback callback) {
        if (bitmaps == null || bitmaps.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        List<String> uploadedUrls = new ArrayList<>();
        final int[] uploadedCount = {0};
        final int totalCount = bitmaps.size();

        for (int i = 0; i < bitmaps.size(); i++) {
            final int index = i;
            Bitmap bitmap = bitmaps.get(i);
            
            if (bitmap != null) {
                uploadImage(bitmap, bookingId, new ImageUploadCallback() {
                    @Override
                    public void onSuccess(String imageUrl) {
                        uploadedUrls.add(imageUrl);
                        uploadedCount[0]++;
                        callback.onProgress(uploadedCount[0], totalCount);
                        
                        if (uploadedCount[0] == totalCount) {
                            callback.onSuccess(uploadedUrls);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Error uploading image " + index + ": " + error);
                        uploadedCount[0]++;
                        callback.onProgress(uploadedCount[0], totalCount);
                        
                        // Continue with other images even if one fails
                        if (uploadedCount[0] == totalCount) {
                            if (uploadedUrls.isEmpty()) {
                                callback.onError("Failed to upload any images");
                            } else {
                                // Return partial success
                                callback.onSuccess(uploadedUrls);
                            }
                        }
                    }
                });
            } else {
                uploadedCount[0]++;
                callback.onProgress(uploadedCount[0], totalCount);
                
                if (uploadedCount[0] == totalCount) {
                    if (uploadedUrls.isEmpty()) {
                        callback.onError("No valid images to upload");
                    } else {
                        callback.onSuccess(uploadedUrls);
                    }
                }
            }
        }
    }

    /**
     * Upload a profile photo from Uri to Firebase Storage
     */
    public void uploadProfilePhoto(Uri imageUri, String userId, ImageUploadCallback callback) {
        if (imageUri == null) {
            callback.onError("Image URI is null");
            return;
        }

        if (storage == null) {
            Log.e(TAG, "FirebaseStorage instance is null!");
            callback.onError("Firebase Storage is not initialized");
            return;
        }

        try {
            // Generate unique filename
            String filename = "profile_" + userId + "_" + UUID.randomUUID().toString() + ".jpg";
            String path = "profile_photos/" + userId + "/" + filename;
            
            StorageReference storageRef = storage.getReference().child(path);

            // Upload the image
            UploadTask uploadTask = storageRef.putFile(imageUri);
            
            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // Get download URL
                    storageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri downloadUri) {
                            String imageUrl = downloadUri.toString();
                            Log.d(TAG, "Profile photo uploaded successfully: " + imageUrl);
                            callback.onSuccess(imageUrl);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "Error getting download URL", e);
                            callback.onError("Failed to get image URL: " + e.getMessage());
                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Error uploading profile photo", e);
                    String errorMessage = "Failed to upload profile photo";
                    if (e instanceof com.google.firebase.storage.StorageException) {
                        com.google.firebase.storage.StorageException storageException = (com.google.firebase.storage.StorageException) e;
                        int errorCode = storageException.getErrorCode();
                        if (errorCode == com.google.firebase.storage.StorageException.ERROR_OBJECT_NOT_FOUND) {
                            errorMessage = "Firebase Storage is not set up. Please enable Storage in Firebase Console and deploy storage rules.";
                        } else if (errorCode == -13020 || errorCode == -13021) {
                            // -13020: ERROR_UNAUTHORIZED, -13021: ERROR_QUOTA_EXCEEDED
                            errorMessage = "Permission denied. Please check Firebase Storage security rules.";
                        } else {
                            errorMessage = "Upload failed: " + storageException.getMessage();
                        }
                    } else {
                        errorMessage = "Upload failed: " + (e.getMessage() != null ? e.getMessage() : "Unknown error");
                    }
                    callback.onError(errorMessage);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception while uploading profile photo", e);
            callback.onError("Exception: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }

    /**
     * Delete an image from Firebase Storage
     */
    public void deleteImage(String imageUrl, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            if (onFailure != null) {
                onFailure.onFailure(new Exception("Image URL is null or empty"));
            }
            return;
        }

        try {
            StorageReference storageRef = storage.getReferenceFromUrl(imageUrl);
            storageRef.delete()
                    .addOnSuccessListener(onSuccess)
                    .addOnFailureListener(onFailure);
        } catch (Exception e) {
            Log.e(TAG, "Error deleting image", e);
            if (onFailure != null) {
                onFailure.onFailure(e);
            }
        }
    }
}

