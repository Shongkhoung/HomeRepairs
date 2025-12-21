package com.example.homerepairs.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Helper class for Firebase Authentication
 * Provides centralized user authentication management
 */
public class AuthHelper {
    private static final String TAG = "AuthHelper";
    private static final String PREF_NAME = "user_prefs";
    private static final String PREF_USER_NAME = "user_name";
    
    private static FirebaseAuth auth;
    
    static {
        try {
            auth = FirebaseAuth.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase Auth", e);
        }
    }
    
    /**
     * Get current authenticated user ID
     * Returns null if user is not authenticated (app should redirect to LoginActivity)
     * 
     * @param context Application context
     * @return Firebase user ID if authenticated, null otherwise
     */
    public static String getCurrentUserId(Context context) {
        FirebaseUser user = getCurrentUser();
        if (user != null) {
            return user.getUid();
        }
        
        // User not authenticated - app should redirect to LoginActivity
        Log.e(TAG, "getCurrentUserId called but user is not authenticated. " +
                "This should not happen as MainActivity redirects unauthenticated users. " +
                "Please ensure user is logged in before calling this method.");
        return null;
    }
    
    /**
     * Get current authenticated user
     */
    public static FirebaseUser getCurrentUser() {
        try {
            return auth != null ? auth.getCurrentUser() : null;
        } catch (Exception e) {
            Log.e(TAG, "Error getting current user", e);
            return null;
        }
    }
    
    /**
     * Get current user name
     * Returns authenticated user's display name, or saved name, or default
     */
    public static String getCurrentUserName(Context context) {
        FirebaseUser user = getCurrentUser();
        if (user != null) {
            if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                return user.getDisplayName();
            }
            if (user.getEmail() != null) {
                // Extract name from email
                String email = user.getEmail();
                int atIndex = email.indexOf('@');
                if (atIndex > 0) {
                    return email.substring(0, atIndex);
                }
            }
        }
        
        // Try to get from SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String savedName = prefs.getString(PREF_USER_NAME, null);
        if (savedName != null && !savedName.isEmpty()) {
            return savedName;
        }
        
        // Default fallback
        return "You";
    }
    
    /**
     * Save user name locally
     */
    public static void saveUserName(Context context, String userName) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(PREF_USER_NAME, userName).apply();
    }
    
    /**
     * Check if user is authenticated
     */
    public static boolean isAuthenticated() {
        return getCurrentUser() != null;
    }
    
    /**
     * Sign in anonymously (for quick testing without registration)
     * Note: Anonymous authentication must be enabled in Firebase Console
     * If it fails, the app will use device ID as fallback (see getCurrentUserId)
     */
    public static void signInAnonymously(OnAuthCompleteListener listener) {
        if (auth == null) {
            Log.w(TAG, "Firebase Auth not initialized - will use device ID fallback");
            if (listener != null) {
                listener.onError("Firebase Auth not initialized");
            }
            return;
        }
        
        auth.signInAnonymously()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        Log.d(TAG, "Anonymous sign-in successful: " + (user != null ? user.getUid() : "null"));
                        if (listener != null) {
                            listener.onSuccess();
                        }
                    } else {
                        Exception exception = task.getException();
                        String errorMessage = exception != null ? exception.getMessage() : "Sign-in failed";
                        
                        // Check for specific configuration errors
                        if (exception != null && exception.getMessage() != null) {
                            if (exception.getMessage().contains("CONFIGURATION_NOT_FOUND")) {
                                Log.w(TAG, "Anonymous authentication not enabled in Firebase Console. " +
                                        "App will continue using device ID fallback. " +
                                        "To enable: Firebase Console > Authentication > Sign-in method > Anonymous > Enable");
                                errorMessage = "Anonymous auth not configured (using device ID fallback)";
                            } else if (exception.getMessage().contains("NETWORK_ERROR") || 
                                      exception.getMessage().contains("network")) {
                                Log.w(TAG, "Network error during anonymous sign-in. Will retry later.");
                                errorMessage = "Network error (using device ID fallback)";
                            }
                        }
                        
                        Log.w(TAG, "Anonymous sign-in failed: " + errorMessage + 
                                ". App will use device ID fallback for user identification.");
                        
                        if (listener != null) {
                            listener.onError(errorMessage);
                        }
                    }
                });
    }
    
    /**
     * Sign out
     */
    public static void signOut() {
        if (auth != null) {
            auth.signOut();
            Log.d(TAG, "User signed out");
        }
    }
    
    public interface OnAuthCompleteListener {
        void onSuccess();
        void onError(String error);
    }
}

