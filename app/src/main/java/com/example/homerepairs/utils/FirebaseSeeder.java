package com.example.homerepairs.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.homerepairs.models.ProviderDetail;
import com.example.homerepairs.models.Review;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class FirebaseSeeder {
    private static final String TAG = "FirebaseSeeder";
    private static final String PREF_NAME = "AppPrefs";
    private static final String KEY_PROVIDERS_SEEDED = "providers_seeded_v3";
    private static final String COLLECTION_PROVIDERS = "providers";

    private static final String[] CATEGORIES = {
            "Plumbing", "Electrical", "Cleaning", "Painting", "Carpentry",
            "Roofing", "Landscaping", "HVAC", "Appliance", "General"
    };

    private static final String[] FIRST_NAMES = {
            "Sok", "Dara", "Vily", "Sopheak", "Chan", "Visal", "Bona", "Mony", "Rithy", "Sambath",
            "Kanya", "Leakhena", "Srey", "Pich", "Theara", "Vanna", "Sovann", "Nary", "Kalyan", "Ravy"
    };

    private static final String[] LAST_NAMES = {
            "Heng", "Chea", "Sok", "Chan", "Kim", "Lim", "Ly", "Ngoun", "Pen", "Sam",
            "Meas", "Mao", "Prom", "Ouk", "Tep", "Keo", "Khim", "Nov", "Ros", "Sin"
    };

    public static void seedProviders(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if (prefs.getBoolean(KEY_PROVIDERS_SEEDED, false)) {
            Log.d(TAG, "Providers already seeded.");
            return;
        }

        Log.d(TAG, "Starting provider seeding...");
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batch = db.batch();
        Random random = new Random();

        int batchCount = 0;
        int totalProviders = 0;

        for (String category : CATEGORIES) {
            for (int i = 0; i < 10; i++) {
                String docId = UUID.randomUUID().toString();
                ProviderDetail provider = generateProvider(category, i, random);

                // Set ID
                provider.setId(docId);

                batch.set(db.collection(COLLECTION_PROVIDERS).document(docId), provider);
                batchCount++;
                totalProviders++;

                // Commit batch every 50 writes to avoid large batches failing easily
                if (batchCount >= 50) {
                    batch.commit()
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Batch committed successfully"))
                            .addOnFailureListener(e -> Log.e(TAG, "Batch commit failed", e));
                    batch = db.batch();
                    batchCount = 0;
                }
            }
        }

        // Commit remaining
        if (batchCount > 0) {
            batch.commit()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Final batch committed successfully. Total seeded: " + 100);
                        prefs.edit().putBoolean(KEY_PROVIDERS_SEEDED, true).apply();
                        // Update counts after seeding
                        updateCategoryCounts(context);

                        // Notify user of success
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> android.widget.Toast
                                .makeText(context, "Data Seeding Complete!", android.widget.Toast.LENGTH_LONG).show());
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Final batch commit failed", e);
                        // Notify user of failure
                        new android.os.Handler(android.os.Looper.getMainLooper())
                                .post(() -> android.widget.Toast.makeText(context, "Seeding Failed: " + e.getMessage(),
                                        android.widget.Toast.LENGTH_LONG).show());
                    });
        } else {
            // If no remaining batch (unlikely with 100 items and batch 50), still update
            // counts
            prefs.edit().putBoolean(KEY_PROVIDERS_SEEDED, true).apply();
            updateCategoryCounts(context);
        }
    }

    public static void updateCategoryCounts(Context context) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Log.d(TAG, "Updating category counts...");

        db.collection(COLLECTION_PROVIDERS).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Map<String, Integer> counts = new HashMap<>();
                    // Initialize with 0 for all known categories
                    for (String cat : CATEGORIES) {
                        counts.put(cat, 0);
                    }

                    // Count from actual data
                    Log.d(TAG, "Counting " + queryDocumentSnapshots.size() + " providers...");
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        String service = doc.getString("service");
                        if (service != null) {
                            // Normalize string if needed, or assume exact match
                            // Try to match with known categories
                            boolean matched = false;
                            for (String cat : CATEGORIES) {
                                if (cat.equalsIgnoreCase(service)) {
                                    counts.put(cat, counts.get(cat) + 1);
                                    matched = true;
                                    break;
                                }
                            }
                            if (!matched) {
                                Log.w(TAG, "Service '" + service + "' did not match any known category.");
                            }
                        } else {
                            Log.w(TAG, "Provider " + doc.getId() + " has null service");
                        }
                    }
                    Log.d(TAG, "Final counts: " + counts.toString());

                    // Write to "categories" collection
                    WriteBatch batch = db.batch();
                    for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                        String categoryName = entry.getKey();
                        int count = entry.getValue();

                        Map<String, Object> data = new HashMap<>();
                        data.put("name", categoryName);
                        data.put("providerCount", count);
                        // We don't store iconResId in Firestore as it's local

                        batch.set(db.collection("categories").document(categoryName), data);
                    }

                    batch.commit()
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Category counts updated successfully"))
                            .addOnFailureListener(e -> Log.e(TAG, "Failed to update category counts", e));

                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to fetch providers for counting", e));
    }

    private static ProviderDetail generateProvider(String category, int index, Random random) {
        ProviderDetail p = new ProviderDetail();

        String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
        String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
        String name = firstName + " " + lastName;

        // Basic Info
        p.setName(name);
        p.setService(category);

        // Rating 3.5 to 5.0
        double rating = 3.5 + (random.nextDouble() * 1.5);
        p.setRating(Math.round(rating * 10.0) / 10.0);

        p.setReviewCount(10 + random.nextInt(490));
        p.setAvailability("Available Today");

        // Price
        int price = 20 + random.nextInt(130);
        p.setPrice(price + ""); // Stored as string in model derived from Provider

        // Use a generic avatar or potentially a specific resource ID if we had mapping
        // p.setProfileImageResId(...);
        // p.setProfileImageUrl(...); // Can trigger update later

        p.setVerified(random.nextBoolean());
        p.setAvailableNow(random.nextBoolean());

        // Detail Info
        p.setYearsInBusiness(1 + random.nextInt(20));
        p.setJobsCompleted((50 + random.nextInt(950)) + "+");
        p.setResponseTime("~" + (5 + random.nextInt(55)) + " mins");
        p.setRepeatCustomers((70 + random.nextInt(30)) + "%");
        p.setAbout("Professional " + category + " services with over " + p.getYearsInBusiness()
                + " years of experience. High quality workmanship guaranteed.");

        p.setServices(Arrays.asList(category + " Repair", category + " Installation", "Maintenance",
                "Emergency " + category));
        p.setPricingRange("$" + (price - 10) + " - $" + (price + 40) + " / hr");

        List<Map<String, String>> costs = new ArrayList<>();
        Map<String, String> cost1 = new HashMap<>();
        cost1.put("job", "Standard Visit");
        cost1.put("cost", "$" + price);
        costs.add(cost1);
        p.setTypicalJobCosts(costs);

        p.setPaymentMethods(Arrays.asList("Cash", "ABA Pay", "Credit Card"));
        p.setServiceArea("Phnom Penh, Cambodia");
        p.setLanguages(Arrays.asList("Khmer", "English"));

        p.setHasLicense(random.nextBoolean());
        p.setHasInsurance(random.nextBoolean());
        p.setHasCertifications(random.nextBoolean());
        p.setHasBackgroundCheck(true);
        p.setHasBusinessRegistration(random.nextBoolean());

        // Generate some reviews
        List<Review> reviews = new ArrayList<>();
        int reviewCount = 3 + random.nextInt(5);
        for (int i = 0; i < reviewCount; i++) {
            Review r = new Review();
            r.setReviewerName(FIRST_NAMES[random.nextInt(FIRST_NAMES.length)] + " "
                    + LAST_NAMES[random.nextInt(LAST_NAMES.length)]);
            r.setRating(4 + random.nextInt(2)); // Generally positive
            r.setTimeAgo("2 days ago");
            r.setComment("Great service! Very professional and on time.");
            reviews.add(r);
        }
        p.setReviews(reviews);

        Map<String, Integer> dist = new HashMap<>();
        dist.put("5", 70);
        dist.put("4", 20);
        dist.put("3", 5);
        dist.put("2", 3);
        dist.put("1", 2);
        p.setRatingDistribution(dist);

        return p;
    }
}
