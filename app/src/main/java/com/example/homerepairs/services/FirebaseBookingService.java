package com.example.homerepairs.services;

import android.util.Log;

import com.example.homerepairs.models.Booking;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class FirebaseBookingService {
    private static final String TAG = "FirebaseBookingService";
    private static final String COLLECTION_BOOKINGS = "bookings";
    private FirebaseFirestore db;

    // Callback interfaces for booking operations
    public interface BookingCallback {
        void onSuccess(Booking booking);

        void onError(String error);
    }

    public interface BookingListCallback {
        void onSuccess(List<Booking> bookings);

        void onError(String error);
    }

    public FirebaseBookingService() {
        try {
            db = FirebaseFirestore.getInstance();
            Log.d(TAG, "FirebaseBookingService initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing FirebaseBookingService", e);
            throw e;
        }
    }

    /**
     * Create a new booking in Firebase
     */
    public void createBooking(Booking booking, BookingCallback callback) {
        // Generate booking reference if not provided
        if (booking.getBookingReference() == null || booking.getBookingReference().isEmpty()) {
            booking.setBookingReference(generateBookingReference());
        }

        // Set timestamps
        Date now = new Date();
        if (booking.getCreatedAt() == null) {
            booking.setCreatedAt(now);
        }
        booking.setUpdatedAt(now);

        // Set default status if not provided
        if (booking.getStatus() == null || booking.getStatus().isEmpty()) {
            booking.setStatus("Pending");
        }

        Log.d(TAG, "Creating booking with reference: " + booking.getBookingReference());
        Log.d(TAG, "Booking details - UserId: " + booking.getUserId() + ", ProviderId: " + booking.getProviderId()
                + ", ServiceCategory: " + booking.getServiceCategory());

        if (db == null) {
            Log.e(TAG, "FirebaseFirestore instance is null!");
            callback.onError("Firebase is not initialized. Please check your Firebase configuration.");
            return;
        }

        try {
            db.collection(COLLECTION_BOOKINGS)
                    .add(booking)
                    .addOnSuccessListener(documentReference -> {
                        Log.d(TAG, "Booking created successfully with ID: " + documentReference.getId());
                        booking.setId(documentReference.getId());  // Set the Firestore document ID
                        callback.onSuccess(booking);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error creating booking", e);
                        String errorMessage = "Failed to create booking";
                        if (e.getMessage() != null) {
                            errorMessage += ": " + e.getMessage();
                        }
                        if (e.getCause() != null) {
                            errorMessage += " (Cause: " + e.getCause().getMessage() + ")";
                        }
                        callback.onError(errorMessage);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Exception while creating booking", e);
            callback.onError("Exception: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }

    /**
     * Get booking by ID
     */
    public void getBookingById(String bookingId, BookingCallback callback) {
        db.collection(COLLECTION_BOOKINGS)
                .document(bookingId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        Booking booking = task.getResult().toObject(Booking.class);
                        if (booking != null) {
                            booking.setId(task.getResult().getId());
                            Log.d(TAG, "Booking loaded: " + booking.getBookingReference());
                            callback.onSuccess(booking);
                        } else {
                            callback.onError("Booking data is null");
                        }
                    } else {
                        Log.e(TAG, "Error getting booking by ID", task.getException());
                        callback.onError("Failed to load booking: " +
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                });
    }

    /**
     * Get all bookings for a specific user
     */
    public void getBookingsByUserId(String userId, BookingListCallback callback) {
        db.collection(COLLECTION_BOOKINGS)
                .whereEqualTo("userId", userId)
                // Removed orderBy to avoid requiring Firebase composite index
                // .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        List<Booking> bookings = new ArrayList<>();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            for (QueryDocumentSnapshot document : querySnapshot) {
                                try {
                                    Booking booking = document.toObject(Booking.class);
                                    if (booking != null) {
                                        booking.setId(document.getId());
                                        bookings.add(booking);
                                        Log.d(TAG, "Booking loaded: " + booking.getBookingReference());
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error converting document to Booking: " + e.getMessage(), e);
                                }
                            }
                        }

                        // Sort bookings by createdAt in memory (newest first)
                        bookings.sort((b1, b2) -> {
                            if (b1.getCreatedAt() == null)
                                return 1;
                            if (b2.getCreatedAt() == null)
                                return -1;
                            return b2.getCreatedAt().compareTo(b1.getCreatedAt());
                        });

                        Log.d(TAG, "Loaded " + bookings.size() + " bookings for user: " + userId);
                        callback.onSuccess(bookings);
                    } else {
                        Log.e(TAG, "Error getting bookings by user ID", task.getException());
                        callback.onError("Failed to load bookings: " +
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                });
    }

    /**
     * Get all bookings for a specific provider
     */
    public void getBookingsByProviderId(String providerId, BookingListCallback callback) {
        db.collection(COLLECTION_BOOKINGS)
                .whereEqualTo("providerId", providerId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        List<Booking> bookings = new ArrayList<>();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            for (QueryDocumentSnapshot document : querySnapshot) {
                                try {
                                    Booking booking = document.toObject(Booking.class);
                                    if (booking != null) {
                                        booking.setId(document.getId());
                                        bookings.add(booking);
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error converting document to Booking: " + e.getMessage(), e);
                                }
                            }
                        }
                        Log.d(TAG, "Loaded " + bookings.size() + " bookings for provider: " + providerId);
                        callback.onSuccess(bookings);
                    } else {
                        Log.e(TAG, "Error getting bookings by provider ID", task.getException());
                        callback.onError("Failed to load bookings: " +
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                });
    }

    /**
     * Update booking status
     */
    public void updateBookingStatus(String bookingId, String status, BookingCallback callback) {
        db.collection(COLLECTION_BOOKINGS)
                .document(bookingId)
                .update("status", status, "updatedAt", new Date())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Booking status updated: " + bookingId + " -> " + status);
                    // Fetch updated booking
                    getBookingById(bookingId, callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating booking status", e);
                    callback.onError("Failed to update booking: " +
                            (e.getMessage() != null ? e.getMessage() : "Unknown error"));
                });
    }

    /**
     * Update booking details
     */
    public void updateBooking(String bookingId, Booking booking, BookingCallback callback) {
        booking.setUpdatedAt(new Date());
        booking.setId(bookingId);

        db.collection(COLLECTION_BOOKINGS)
                .document(bookingId)
                .set(booking)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Booking updated: " + bookingId);
                    callback.onSuccess(booking);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating booking", e);
                    callback.onError("Failed to update booking: " +
                            (e.getMessage() != null ? e.getMessage() : "Unknown error"));
                });
    }

    /**
     * Delete booking
     */
    public void deleteBooking(String bookingId, BookingCallback callback) {
        db.collection(COLLECTION_BOOKINGS)
                .document(bookingId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Booking deleted: " + bookingId);
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting booking", e);
                    callback.onError("Failed to delete booking: " +
                            (e.getMessage() != null ? e.getMessage() : "Unknown error"));
                });
    }

    /**
     * Generate a unique booking reference number
     */
    private String generateBookingReference() {
        Random random = new Random();
        long reference = 1000000000L + random.nextInt(900000000);
        return String.valueOf(reference);
    }
}
