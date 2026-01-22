package com.example.homerepairs.services;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.Map;
import java.util.HashMap;

/**
 * Service for managing user profile data in Firestore
 */
public class FirebaseUserService {
    private static final String TAG = "FirebaseUserService";
    private static final String COLLECTION_USERS = "users";

    private FirebaseFirestore db;

    public FirebaseUserService() {
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Callback interface for user profile operations
     */
    public interface UserProfileCallback {
        void onSuccess(Map<String, Object> userProfile);

        void onError(String error);
    }

    /**
     * Get user profile from Firestore by user ID
     * 
     * @param userId   Firebase user ID
     * @param callback Callback for result
     */
    public void getUserProfile(String userId, UserProfileCallback callback) {
        if (userId == null || userId.isEmpty()) {
            if (callback != null) {
                callback.onError("User ID is required");
            }
            return;
        }

        Log.d(TAG, "Fetching user profile for userId: " + userId);

        db.collection(COLLECTION_USERS).document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> userProfile = documentSnapshot.getData();
                        Log.d(TAG, "User profile loaded successfully");
                        if (callback != null) {
                            callback.onSuccess(userProfile);
                        }
                    } else {
                        Log.w(TAG, "User profile document does not exist for userId: " + userId);
                        // Return empty map if document doesn't exist
                        if (callback != null) {
                            callback.onSuccess(new HashMap<>());
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user profile", e);
                    if (callback != null) {
                        callback.onError("Failed to load user profile: " + e.getMessage());
                    }
                });
    }

    /**
     * Set up real-time listener for user profile changes
     * 
     * @param userId   Firebase user ID
     * @param callback Callback for result
     * @return ListenerRegistration to remove listener when done
     */
    public ListenerRegistration listenToUserProfile(String userId, UserProfileCallback callback) {
        if (userId == null || userId.isEmpty()) {
            if (callback != null) {
                callback.onError("User ID is required");
            }
            return null;
        }

        Log.d(TAG, "Setting up real-time listener for userId: " + userId);

        return db.collection(COLLECTION_USERS).document(userId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error listening to user profile", e);
                        if (callback != null) {
                            callback.onError("Failed to listen to user profile: " + e.getMessage());
                        }
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        Map<String, Object> userProfile = documentSnapshot.getData();
                        Log.d(TAG, "User profile updated");
                        if (callback != null) {
                            callback.onSuccess(userProfile);
                        }
                    } else {
                        Log.w(TAG, "User profile document does not exist");
                        if (callback != null) {
                            callback.onSuccess(new HashMap<>());
                        }
                    }
                });
    }

    /**
     * Update user profile in Firestore
     * If document doesn't exist, creates it instead of updating
     * 
     * @param userId   Firebase user ID
     * @param updates  Map of fields to update
     * @param callback Callback for result
     */
    public void updateUserProfile(String userId, Map<String, Object> updates, UserProfileCallback callback) {
        if (userId == null || userId.isEmpty()) {
            if (callback != null) {
                callback.onError("User ID is required");
            }
            return;
        }

        // Add updated timestamp
        updates.put("updatedAt", com.google.firebase.Timestamp.now());

        Log.d(TAG, "Updating user profile for userId: " + userId);

        // First check if document exists
        db.collection(COLLECTION_USERS).document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Document exists, update it
                        db.collection(COLLECTION_USERS).document(userId)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "User profile updated successfully");
                                    // Fetch updated profile
                                    getUserProfile(userId, callback);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error updating user profile", e);
                                    if (callback != null) {
                                        callback.onError("Failed to update user profile: " + e.getMessage());
                                    }
                                });
                    } else {
                        // Document doesn't exist, create it
                        Log.d(TAG, "User profile document doesn't exist, creating new one");
                        // Add userId and createdAt to the updates
                        updates.put("userId", userId);
                        updates.put("createdAt", com.google.firebase.Timestamp.now());

                        // Get email from Firebase Auth if available
                        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance()
                                .getCurrentUser();
                        if (user != null && user.getEmail() != null) {
                            updates.put("email", user.getEmail());
                        }

                        db.collection(COLLECTION_USERS).document(userId)
                                .set(updates)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "User profile created successfully");
                                    // Fetch the created profile
                                    getUserProfile(userId, callback);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error creating user profile", e);
                                    if (callback != null) {
                                        callback.onError("Failed to create user profile: " + e.getMessage());
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking if user profile exists", e);
                    // If check fails, try to create the document anyway
                    Log.d(TAG, "Attempting to create user profile document");
                    updates.put("userId", userId);
                    updates.put("createdAt", com.google.firebase.Timestamp.now());

                    com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance()
                            .getCurrentUser();
                    if (user != null && user.getEmail() != null) {
                        updates.put("email", user.getEmail());
                    }

                    db.collection(COLLECTION_USERS).document(userId)
                            .set(updates)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "User profile created successfully");
                                getUserProfile(userId, callback);
                            })
                            .addOnFailureListener(createError -> {
                                Log.e(TAG, "Error creating user profile", createError);
                                if (callback != null) {
                                    callback.onError("Failed to create user profile: " + createError.getMessage());
                                }
                            });
                });
    }

    /**
     * Get user's booking count from Firestore
     * Queries the bookings collection and counts documents where userId matches
     * 
     * @param userId   Firebase user ID
     * @param callback Callback with booking count result
     */
    public void getBookingCount(String userId, UserProfileCallback callback) {
        if (userId == null || userId.isEmpty()) {
            if (callback != null) {
                callback.onError("User ID is required");
            }
            return;
        }

        Log.d(TAG, "Fetching booking count for userId: " + userId);

        // Query bookings collection where userId matches
        db.collection("bookings")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int bookingCount = querySnapshot.size();
                    Log.d(TAG, "Found " + bookingCount + " bookings for user");

                    if (callback != null) {
                        Map<String, Object> result = new HashMap<>();
                        result.put("bookingCount", bookingCount);
                        callback.onSuccess(result);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching booking count", e);
                    if (callback != null) {
                        // Return 0 on error instead of failing completely
                        Map<String, Object> result = new HashMap<>();
                        result.put("bookingCount", 0);
                        callback.onSuccess(result);
                    }
                });
    }
}
