package com.example.homerepairs.services;

import android.util.Log;

import com.example.homerepairs.models.Provider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FirebaseProviderService {
    private static final String TAG = "FirebaseProviderService";
    private static final String COLLECTION_PROVIDERS = "providers";
    private FirebaseFirestore db;

    public interface ProviderCallback {
        void onSuccess(List<Provider> providers);
        void onError(String error);
    }

    public interface ProviderDetailCallback {
        void onSuccess(com.example.homerepairs.models.ProviderDetail providerDetail);
        void onError(String error);
    }

    public FirebaseProviderService() {
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Fetch all providers from Firebase
     */
    public void getAllProviders(ProviderCallback callback) {
        db.collection(COLLECTION_PROVIDERS)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        List<Provider> providers = new ArrayList<>();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            for (QueryDocumentSnapshot document : querySnapshot) {
                                try {
                                    Provider provider = document.toObject(Provider.class);
                                    if (provider != null) {
                                        provider.setId(document.getId());
                                        // Ensure profileImageUrl is loaded (check if automatic mapping missed it)
                                        if (document.contains("profileImageUrl") && (provider.getProfileImageUrl() == null || provider.getProfileImageUrl().isEmpty())) {
                                            provider.setProfileImageUrl(document.getString("profileImageUrl"));
                                            Log.d(TAG, "Set profileImageUrl for " + provider.getName() + ": " + document.getString("profileImageUrl"));
                                        }
                                        providers.add(provider);
                                        Log.d(TAG, "Provider loaded: " + provider.getName() + 
                                            ", profileImageUrl: " + (provider.getProfileImageUrl() != null ? provider.getProfileImageUrl() : "null"));
                                    } else {
                                        Log.w(TAG, "Provider object is null for document: " + document.getId());
                                        // Try manual mapping as fallback
                                        try {
                                            Provider manualProvider = new Provider();
                                            manualProvider.setId(document.getId());
                                            if (document.contains("name")) {
                                                manualProvider.setName(document.getString("name"));
                                            }
                                            if (document.contains("service")) {
                                                manualProvider.setService(document.getString("service"));
                                            }
                                            if (document.contains("rating")) {
                                                Object ratingObj = document.get("rating");
                                                if (ratingObj instanceof Number) {
                                                    manualProvider.setRating(((Number) ratingObj).doubleValue());
                                                }
                                            }
                                            if (document.contains("reviewCount")) {
                                                Object reviewObj = document.get("reviewCount");
                                                if (reviewObj instanceof Number) {
                                                    manualProvider.setReviewCount(((Number) reviewObj).intValue());
                                                }
                                            }
                                            if (document.contains("availability")) {
                                                manualProvider.setAvailability(document.getString("availability"));
                                            }
                                            if (document.contains("price")) {
                                                Object priceObj = document.get("price");
                                                if (priceObj instanceof String) {
                                                    manualProvider.setPrice((String) priceObj);
                                                } else if (priceObj instanceof Number) {
                                                    // Convert number to string format (e.g., 60 -> "$60/hr")
                                                    manualProvider.setPrice("$" + priceObj + "/hr");
                                                }
                                            }
                                            if (document.contains("isVerified")) {
                                                manualProvider.setVerified(document.getBoolean("isVerified"));
                                            }
                                            if (document.contains("isAvailableNow")) {
                                                manualProvider.setAvailableNow(document.getBoolean("isAvailableNow"));
                                            }
                                            if (document.contains("profileImageUrl")) {
                                                manualProvider.setProfileImageUrl(document.getString("profileImageUrl"));
                                            }
                                            providers.add(manualProvider);
                                            Log.d(TAG, "Manually mapped provider: " + manualProvider.getName());
                                        } catch (Exception e) {
                                            Log.e(TAG, "Failed to manually map provider: " + e.getMessage(), e);
                                        }
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Exception converting document to Provider: " + e.getMessage(), e);
                                    // Try manual mapping when automatic mapping fails
                                    try {
                                        Provider manualProvider = new Provider();
                                        manualProvider.setId(document.getId());
                                        if (document.contains("name")) {
                                            manualProvider.setName(document.getString("name"));
                                        }
                                        if (document.contains("service")) {
                                            manualProvider.setService(document.getString("service"));
                                        }
                                        if (document.contains("rating")) {
                                            Object ratingObj = document.get("rating");
                                            if (ratingObj instanceof Number) {
                                                manualProvider.setRating(((Number) ratingObj).doubleValue());
                                            }
                                        }
                                        if (document.contains("reviewCount")) {
                                            Object reviewObj = document.get("reviewCount");
                                            if (reviewObj instanceof Number) {
                                                manualProvider.setReviewCount(((Number) reviewObj).intValue());
                                            }
                                        }
                                        if (document.contains("availability")) {
                                            manualProvider.setAvailability(document.getString("availability"));
                                        }
                                        if (document.contains("price")) {
                                            Object priceObj = document.get("price");
                                            if (priceObj instanceof String) {
                                                manualProvider.setPrice((String) priceObj);
                                            } else if (priceObj instanceof Number) {
                                                // Convert number to string format (e.g., 60 -> "$60/hr")
                                                manualProvider.setPrice("$" + priceObj + "/hr");
                                            }
                                        }
                                        if (document.contains("isVerified")) {
                                            manualProvider.setVerified(document.getBoolean("isVerified"));
                                        }
                                        if (document.contains("isAvailableNow")) {
                                            manualProvider.setAvailableNow(document.getBoolean("isAvailableNow"));
                                        }
                                        if (document.contains("profileImageUrl")) {
                                            manualProvider.setProfileImageUrl(document.getString("profileImageUrl"));
                                        }
                                        providers.add(manualProvider);
                                        Log.d(TAG, "Manually mapped provider after exception: " + manualProvider.getName());
                                    } catch (Exception manualEx) {
                                        Log.e(TAG, "Failed to manually map provider after exception: " + manualEx.getMessage(), manualEx);
                                    }
                                }
                            }
                        }
                        Log.d(TAG, "Loaded " + providers.size() + " providers from Firebase");
                        callback.onSuccess(providers);
                    } else {
                        Log.e(TAG, "Error getting providers", task.getException());
                        callback.onError("Failed to load providers: " + 
                            (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                });
    }

    /**
     * Fetch providers filtered by service category
     */
    public void getProvidersByService(String serviceCategory, ProviderCallback callback) {
        Log.d(TAG, "Querying providers for service: '" + serviceCategory + "'");
        Log.d(TAG, "Collection: " + COLLECTION_PROVIDERS);
        
        db.collection(COLLECTION_PROVIDERS)
                .whereEqualTo("service", serviceCategory)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        Log.d(TAG, "Query successful. Total documents found: " + (querySnapshot != null ? querySnapshot.size() : 0));
                        
                        List<Provider> providers = new ArrayList<>();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            for (QueryDocumentSnapshot document : querySnapshot) {
                                Log.d(TAG, "Processing document ID: " + document.getId());
                                Log.d(TAG, "Document data: " + document.getData());
                                
                                // Log each field individually to debug mapping issues
                                if (document.contains("name")) {
                                    Log.d(TAG, "Field 'name' exists: " + document.get("name"));
                                } else {
                                    Log.w(TAG, "Field 'name' does NOT exist in document");
                                }
                                if (document.contains("service")) {
                                    Log.d(TAG, "Field 'service' exists: " + document.get("service"));
                                } else {
                                    Log.w(TAG, "Field 'service' does NOT exist in document");
                                }
                                
                                try {
                                    Provider provider = document.toObject(Provider.class);
                                    if (provider != null) {
                                        provider.setId(document.getId());
                                        providers.add(provider);
                                        Log.d(TAG, "Provider loaded: " + provider.getName() + " (service: " + provider.getService() + ")");
                                    } else {
                                        Log.e(TAG, "Failed to convert document to Provider object: " + document.getId() + " - result is null");
                                        // Try manual mapping as fallback
                                        try {
                                            Provider manualProvider = new Provider();
                                            manualProvider.setId(document.getId());
                                            if (document.contains("name")) {
                                                manualProvider.setName(document.getString("name"));
                                            }
                                            if (document.contains("service")) {
                                                manualProvider.setService(document.getString("service"));
                                            }
                                            if (document.contains("rating")) {
                                                Object ratingObj = document.get("rating");
                                                if (ratingObj instanceof Number) {
                                                    manualProvider.setRating(((Number) ratingObj).doubleValue());
                                                }
                                            }
                                            if (document.contains("reviewCount")) {
                                                Object reviewObj = document.get("reviewCount");
                                                if (reviewObj instanceof Number) {
                                                    manualProvider.setReviewCount(((Number) reviewObj).intValue());
                                                }
                                            }
                                            if (document.contains("availability")) {
                                                manualProvider.setAvailability(document.getString("availability"));
                                            }
                                            if (document.contains("price")) {
                                                Object priceObj = document.get("price");
                                                if (priceObj instanceof String) {
                                                    manualProvider.setPrice((String) priceObj);
                                                } else if (priceObj instanceof Number) {
                                                    // Convert number to string format (e.g., 60 -> "$60/hr")
                                                    manualProvider.setPrice("$" + priceObj + "/hr");
                                                }
                                            }
                                            if (document.contains("isVerified")) {
                                                manualProvider.setVerified(document.getBoolean("isVerified"));
                                            }
                                            if (document.contains("isAvailableNow")) {
                                                manualProvider.setAvailableNow(document.getBoolean("isAvailableNow"));
                                            }
                                            if (document.contains("profileImageUrl")) {
                                                manualProvider.setProfileImageUrl(document.getString("profileImageUrl"));
                                            }
                                            providers.add(manualProvider);
                                            Log.d(TAG, "Manually mapped provider: " + manualProvider.getName());
                                        } catch (Exception e) {
                                            Log.e(TAG, "Failed to manually map provider: " + e.getMessage(), e);
                                        }
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Exception converting document to Provider: " + e.getMessage(), e);
                                    // Try manual mapping when automatic mapping fails
                                    try {
                                        Provider manualProvider = new Provider();
                                        manualProvider.setId(document.getId());
                                        if (document.contains("name")) {
                                            manualProvider.setName(document.getString("name"));
                                        }
                                        if (document.contains("service")) {
                                            manualProvider.setService(document.getString("service"));
                                        }
                                        if (document.contains("rating")) {
                                            Object ratingObj = document.get("rating");
                                            if (ratingObj instanceof Number) {
                                                manualProvider.setRating(((Number) ratingObj).doubleValue());
                                            }
                                        }
                                        if (document.contains("reviewCount")) {
                                            Object reviewObj = document.get("reviewCount");
                                            if (reviewObj instanceof Number) {
                                                manualProvider.setReviewCount(((Number) reviewObj).intValue());
                                            }
                                        }
                                        if (document.contains("availability")) {
                                            manualProvider.setAvailability(document.getString("availability"));
                                        }
                                        if (document.contains("price")) {
                                            Object priceObj = document.get("price");
                                            if (priceObj instanceof String) {
                                                manualProvider.setPrice((String) priceObj);
                                            } else if (priceObj instanceof Number) {
                                                // Convert number to string format (e.g., 60 -> "$60/hr")
                                                manualProvider.setPrice("$" + priceObj + "/hr");
                                            }
                                        }
                                        if (document.contains("isVerified")) {
                                            manualProvider.setVerified(document.getBoolean("isVerified"));
                                        }
                                        if (document.contains("isAvailableNow")) {
                                            manualProvider.setAvailableNow(document.getBoolean("isAvailableNow"));
                                        }
                                        if (document.contains("profileImageUrl")) {
                                            manualProvider.setProfileImageUrl(document.getString("profileImageUrl"));
                                        }
                                        providers.add(manualProvider);
                                        Log.d(TAG, "Manually mapped provider after exception: " + manualProvider.getName());
                                    } catch (Exception manualEx) {
                                        Log.e(TAG, "Failed to manually map provider after exception: " + manualEx.getMessage(), manualEx);
                                    }
                                }
                            }
                        } else {
                            Log.w(TAG, "No documents found matching service: '" + serviceCategory + "'");
                            Log.d(TAG, "Checking if collection exists by fetching all providers...");
                            // Debug: Try to get all providers to see if collection exists
                            getAllProviders(new ProviderCallback() {
                                @Override
                                public void onSuccess(List<Provider> allProviders) {
                                    Log.d(TAG, "Total providers in collection: " + allProviders.size());
                                    if (!allProviders.isEmpty()) {
                                        Log.d(TAG, "Sample provider service field: '" + allProviders.get(0).getService() + "'");
                                        Log.d(TAG, "Note: Query was for: '" + serviceCategory + "'");
                                    }
                                }
                                
                                @Override
                                public void onError(String error) {
                                    Log.e(TAG, "Failed to fetch all providers for debugging: " + error);
                                }
                            });
                        }
                        Log.d(TAG, "Loaded " + providers.size() + " providers for service: " + serviceCategory);
                        callback.onSuccess(providers);
                    } else {
                        Exception exception = task.getException();
                        Log.e(TAG, "Error getting providers by service: " + serviceCategory, exception);
                        if (exception != null) {
                            Log.e(TAG, "Exception message: " + exception.getMessage());
                            Log.e(TAG, "Exception class: " + exception.getClass().getName());
                        }
                        callback.onError("Failed to load providers: " + 
                            (exception != null ? exception.getMessage() : "Unknown error"));
                    }
                });
    }

    /**
     * Fetch a single provider by ID
     */
    public void getProviderById(String providerId, ProviderCallback callback) {
        db.collection(COLLECTION_PROVIDERS)
                .document(providerId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        Provider provider = task.getResult().toObject(Provider.class);
                        if (provider != null) {
                            provider.setId(task.getResult().getId());
                            List<Provider> providers = new ArrayList<>();
                            providers.add(provider);
                            callback.onSuccess(providers);
                        } else {
                            callback.onError("Provider data is null");
                        }
                    } else {
                        Log.e(TAG, "Error getting provider by ID", task.getException());
                        callback.onError("Failed to load provider: " + 
                            (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                });
    }

    /**
     * Real-time listener for providers - updates automatically when data changes
     * Returns a ListenerRegistration that should be removed when no longer needed
     */
    public ListenerRegistration listenToProviders(String serviceCategory, ProviderCallback callback) {
        Log.d(TAG, "Setting up real-time listener for service: '" + serviceCategory + "'");
        
        return db.collection(COLLECTION_PROVIDERS)
                .whereEqualTo("service", serviceCategory)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to providers", error);
                        callback.onError("Failed to listen to providers: " + error.getMessage());
                        return;
                    }

                    if (snapshot != null && !snapshot.isEmpty()) {
                        Log.d(TAG, "Real-time update received. Total documents: " + snapshot.size());
                        List<Provider> providers = new ArrayList<>();
                        
                        for (QueryDocumentSnapshot document : snapshot) {
                            Log.d(TAG, "Processing document ID: " + document.getId());
                            
                            try {
                                Provider provider = document.toObject(Provider.class);
                                if (provider != null) {
                                    provider.setId(document.getId());
                                    providers.add(provider);
                                    Log.d(TAG, "Provider loaded: " + provider.getName() + " (service: " + provider.getService() + ")");
                                } else {
                                    Log.e(TAG, "Failed to convert document to Provider object: " + document.getId() + " - result is null");
                                    // Try manual mapping as fallback
                                    try {
                                        Provider manualProvider = new Provider();
                                        manualProvider.setId(document.getId());
                                        if (document.contains("name")) {
                                            manualProvider.setName(document.getString("name"));
                                        }
                                        if (document.contains("service")) {
                                            manualProvider.setService(document.getString("service"));
                                        }
                                        if (document.contains("rating")) {
                                            Object ratingObj = document.get("rating");
                                            if (ratingObj instanceof Number) {
                                                manualProvider.setRating(((Number) ratingObj).doubleValue());
                                            }
                                        }
                                        if (document.contains("reviewCount")) {
                                            Object reviewObj = document.get("reviewCount");
                                            if (reviewObj instanceof Number) {
                                                manualProvider.setReviewCount(((Number) reviewObj).intValue());
                                            }
                                        }
                                        if (document.contains("availability")) {
                                            manualProvider.setAvailability(document.getString("availability"));
                                        }
                                        if (document.contains("price")) {
                                            Object priceObj = document.get("price");
                                            if (priceObj instanceof String) {
                                                manualProvider.setPrice((String) priceObj);
                                            } else if (priceObj instanceof Number) {
                                                // Convert number to string format (e.g., 60 -> "$60/hr")
                                                manualProvider.setPrice("$" + priceObj + "/hr");
                                            }
                                        }
                                        if (document.contains("isVerified")) {
                                            manualProvider.setVerified(document.getBoolean("isVerified"));
                                        }
                                        if (document.contains("isAvailableNow")) {
                                            manualProvider.setAvailableNow(document.getBoolean("isAvailableNow"));
                                        }
                                        if (document.contains("profileImageUrl")) {
                                            manualProvider.setProfileImageUrl(document.getString("profileImageUrl"));
                                        }
                                        providers.add(manualProvider);
                                        Log.d(TAG, "Manually mapped provider: " + manualProvider.getName());
                                    } catch (Exception e) {
                                        Log.e(TAG, "Failed to manually map provider: " + e.getMessage(), e);
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Exception converting document to Provider: " + e.getMessage(), e);
                                // Try manual mapping when automatic mapping fails
                                try {
                                    Provider manualProvider = new Provider();
                                    manualProvider.setId(document.getId());
                                    if (document.contains("name")) {
                                        manualProvider.setName(document.getString("name"));
                                    }
                                    if (document.contains("service")) {
                                        manualProvider.setService(document.getString("service"));
                                    }
                                    if (document.contains("rating")) {
                                        Object ratingObj = document.get("rating");
                                        if (ratingObj instanceof Number) {
                                            manualProvider.setRating(((Number) ratingObj).doubleValue());
                                        }
                                    }
                                    if (document.contains("reviewCount")) {
                                        Object reviewObj = document.get("reviewCount");
                                        if (reviewObj instanceof Number) {
                                            manualProvider.setReviewCount(((Number) reviewObj).intValue());
                                        }
                                    }
                                    if (document.contains("availability")) {
                                        manualProvider.setAvailability(document.getString("availability"));
                                    }
                                    if (document.contains("price")) {
                                        Object priceObj = document.get("price");
                                        if (priceObj instanceof String) {
                                            manualProvider.setPrice((String) priceObj);
                                        } else if (priceObj instanceof Number) {
                                            // Convert number to string format (e.g., 60 -> "$60/hr")
                                            manualProvider.setPrice("$" + priceObj + "/hr");
                                        }
                                    }
                                    if (document.contains("isVerified")) {
                                        manualProvider.setVerified(document.getBoolean("isVerified"));
                                    }
                                    if (document.contains("isAvailableNow")) {
                                        manualProvider.setAvailableNow(document.getBoolean("isAvailableNow"));
                                    }
                                    if (document.contains("profileImageUrl")) {
                                        manualProvider.setProfileImageUrl(document.getString("profileImageUrl"));
                                    }
                                    providers.add(manualProvider);
                                    Log.d(TAG, "Manually mapped provider after exception: " + manualProvider.getName());
                                } catch (Exception manualEx) {
                                    Log.e(TAG, "Failed to manually map provider after exception: " + manualEx.getMessage(), manualEx);
                                }
                            }
                        }
                        Log.d(TAG, "Real-time update: Loaded " + providers.size() + " providers for service: " + serviceCategory);
                        callback.onSuccess(providers);
                    } else {
                        Log.d(TAG, "Real-time update: No documents found for service: '" + serviceCategory + "'");
                        callback.onSuccess(new ArrayList<>()); // Return empty list instead of error
                    }
                });
    }

    /**
     * Fetch a provider by name (specifically for Panha)
     */
    public void getProviderByName(String providerName, ProviderDetailCallback callback) {
        Log.d(TAG, "Fetching provider by name: '" + providerName + "'");
        
        db.collection(COLLECTION_PROVIDERS)
                .whereEqualTo("name", providerName)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            QueryDocumentSnapshot document = (QueryDocumentSnapshot) querySnapshot.getDocuments().get(0);
                            try {
                                com.example.homerepairs.models.ProviderDetail providerDetail = 
                                    document.toObject(com.example.homerepairs.models.ProviderDetail.class);
                                if (providerDetail != null) {
                                    providerDetail.setId(document.getId());
                                    // If ProviderDetail doesn't have all fields, try to map manually
                                    mapProviderDetailFields(document, providerDetail);
                                    Log.d(TAG, "Provider detail loaded: " + providerDetail.getName());
                                    callback.onSuccess(providerDetail);
                                } else {
                                    callback.onError("Provider data is null");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error converting to ProviderDetail", e);
                                // Fallback: create ProviderDetail manually
                                com.example.homerepairs.models.ProviderDetail providerDetail = 
                                    new com.example.homerepairs.models.ProviderDetail();
                                providerDetail.setId(document.getId());
                                mapProviderDetailFields(document, providerDetail);
                                callback.onSuccess(providerDetail);
                            }
                        } else {
                            Log.w(TAG, "No provider found with name: " + providerName);
                            callback.onError("Provider not found: " + providerName);
                        }
                    } else {
                        Log.e(TAG, "Error getting provider by name", task.getException());
                        callback.onError("Failed to load provider: " + 
                            (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                });
    }

    /**
     * Real-time listener for a specific provider by name
     */
    public ListenerRegistration listenToProviderByName(String providerName, ProviderDetailCallback callback) {
        Log.d(TAG, "Setting up real-time listener for provider: '" + providerName + "'");
        
        return db.collection(COLLECTION_PROVIDERS)
                .whereEqualTo("name", providerName)
                .limit(1)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to provider", error);
                        callback.onError("Failed to listen to provider: " + error.getMessage());
                        return;
                    }

                    if (snapshot != null && !snapshot.isEmpty()) {
                        QueryDocumentSnapshot document = (QueryDocumentSnapshot) snapshot.getDocuments().get(0);
                        try {
                            com.example.homerepairs.models.ProviderDetail providerDetail = 
                                document.toObject(com.example.homerepairs.models.ProviderDetail.class);
                            if (providerDetail != null) {
                                providerDetail.setId(document.getId());
                                mapProviderDetailFields(document, providerDetail);
                                Log.d(TAG, "Real-time update: Provider detail loaded: " + providerDetail.getName());
                                callback.onSuccess(providerDetail);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error converting to ProviderDetail in real-time", e);
                            com.example.homerepairs.models.ProviderDetail providerDetail = 
                                new com.example.homerepairs.models.ProviderDetail();
                            providerDetail.setId(document.getId());
                            mapProviderDetailFields(document, providerDetail);
                            callback.onSuccess(providerDetail);
                        }
                    }
                });
    }

    /**
     * Helper method to map all fields from Firestore document to ProviderDetail
     */
    private void mapProviderDetailFields(QueryDocumentSnapshot document, 
                                        com.example.homerepairs.models.ProviderDetail providerDetail) {
        try {
            // Basic Provider fields
            if (document.contains("name")) {
                providerDetail.setName(document.getString("name"));
            }
            if (document.contains("service")) {
                providerDetail.setService(document.getString("service"));
            }
            if (document.contains("rating")) {
                Object ratingObj = document.get("rating");
                if (ratingObj instanceof Number) {
                    providerDetail.setRating(((Number) ratingObj).doubleValue());
                }
            }
            if (document.contains("reviewCount")) {
                Object reviewObj = document.get("reviewCount");
                if (reviewObj instanceof Number) {
                    providerDetail.setReviewCount(((Number) reviewObj).intValue());
                }
            }
            if (document.contains("availability")) {
                providerDetail.setAvailability(document.getString("availability"));
            }
            if (document.contains("price")) {
                Object priceObj = document.get("price");
                if (priceObj instanceof String) {
                    providerDetail.setPrice((String) priceObj);
                } else if (priceObj instanceof Number) {
                    providerDetail.setPrice("$" + priceObj + "/hr");
                }
            }
            if (document.contains("profileImageUrl")) {
                providerDetail.setProfileImageUrl(document.getString("profileImageUrl"));
            }
            if (document.contains("isVerified")) {
                providerDetail.setVerified(document.getBoolean("isVerified"));
            }
            if (document.contains("isAvailableNow")) {
                providerDetail.setAvailableNow(document.getBoolean("isAvailableNow"));
            }

            // ProviderDetail specific fields
            if (document.contains("yearsInBusiness")) {
                Object yearsObj = document.get("yearsInBusiness");
                if (yearsObj instanceof Number) {
                    providerDetail.setYearsInBusiness(((Number) yearsObj).intValue());
                }
            }
            if (document.contains("jobsCompleted")) {
                Object jobsObj = document.get("jobsCompleted");
                if (jobsObj instanceof String) {
                    providerDetail.setJobsCompleted((String) jobsObj);
                } else if (jobsObj instanceof Number) {
                    providerDetail.setJobsCompleted(String.valueOf(jobsObj) + "+");
                }
            }
            if (document.contains("responseTime")) {
                providerDetail.setResponseTime(document.getString("responseTime"));
            }
            if (document.contains("repeatCustomers")) {
                Object repeatObj = document.get("repeatCustomers");
                if (repeatObj instanceof String) {
                    providerDetail.setRepeatCustomers((String) repeatObj);
                } else if (repeatObj instanceof Number) {
                    providerDetail.setRepeatCustomers(String.valueOf(repeatObj) + "%");
                }
            }
            if (document.contains("about")) {
                providerDetail.setAbout(document.getString("about"));
            }
            if (document.contains("services")) {
                providerDetail.setServices((List<String>) document.get("services"));
            }
            if (document.contains("pricingRange")) {
                providerDetail.setPricingRange(document.getString("pricingRange"));
            }
            if (document.contains("typicalJobCosts")) {
                providerDetail.setTypicalJobCosts((List<Map<String, String>>) document.get("typicalJobCosts"));
            }
            if (document.contains("paymentMethods")) {
                providerDetail.setPaymentMethods((List<String>) document.get("paymentMethods"));
            }
            if (document.contains("serviceArea")) {
                providerDetail.setServiceArea(document.getString("serviceArea"));
            }
            if (document.contains("languages")) {
                providerDetail.setLanguages((List<String>) document.get("languages"));
            }
            if (document.contains("hasLicense")) {
                providerDetail.setHasLicense(document.getBoolean("hasLicense"));
            }
            if (document.contains("hasInsurance")) {
                providerDetail.setHasInsurance(document.getBoolean("hasInsurance"));
            }
            if (document.contains("hasCertifications")) {
                providerDetail.setHasCertifications(document.getBoolean("hasCertifications"));
            }
            if (document.contains("hasBackgroundCheck")) {
                providerDetail.setHasBackgroundCheck(document.getBoolean("hasBackgroundCheck"));
            }
            if (document.contains("hasBusinessRegistration")) {
                providerDetail.setHasBusinessRegistration(document.getBoolean("hasBusinessRegistration"));
            }
            if (document.contains("reviews")) {
                // Try to convert reviews
                List<com.example.homerepairs.models.Review> reviews = 
                    (List<com.example.homerepairs.models.Review>) document.get("reviews");
                providerDetail.setReviews(reviews);
            }
            if (document.contains("ratingDistribution")) {
                providerDetail.setRatingDistribution((Map<String, Integer>) document.get("ratingDistribution"));
            }
            if (document.contains("portfolio")) {
                // Try to convert portfolio items
                List<com.example.homerepairs.models.PortfolioItem> portfolio = 
                    (List<com.example.homerepairs.models.PortfolioItem>) document.get("portfolio");
                providerDetail.setPortfolio(portfolio);
            }
            if (document.contains("portfolioDescription")) {
                providerDetail.setPortfolioDescription(document.getString("portfolioDescription"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error mapping provider detail fields", e);
        }
    }
}

