package com.example.homerepairs;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homerepairs.adapters.CalendarAdapter;
import com.example.homerepairs.adapters.PortfolioAdapter;
import com.example.homerepairs.adapters.ReviewAdapter;
import com.example.homerepairs.models.ProviderDetail;
import com.example.homerepairs.models.Review;
import com.example.homerepairs.models.PortfolioItem;
import com.example.homerepairs.services.FirebaseProviderService;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.ListenerRegistration;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class ProviderProfileActivity extends BaseActivity {

    private boolean isJobCostsExpanded = false;
    private boolean isPaymentMethodsExpanded = false;

    // Removed unused fields for calendar, reviews, portfolio, service area,
    // languages
    private FirebaseProviderService firebaseService;
    private ListenerRegistration listenerRegistration;
    private ProviderDetail currentProvider;
    private String providerName = "Panha"; // Default to Panha
    private boolean isInitializingBottomNav = true; // Flag to prevent navigation during initialization

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_profile);

        // Get provider name from intent or use default
        String intentProviderName = getIntent().getStringExtra("provider_name");
        if (intentProviderName != null && !intentProviderName.isEmpty()) {
            providerName = intentProviderName;
            android.util.Log.d("ProviderProfileActivity", "Provider name from Intent: " + providerName);
        } else {
            android.util.Log.w("ProviderProfileActivity", "No provider name in Intent, using default: " + providerName);
        }

        // Also get service category if provided
        String serviceCategory = getIntent().getStringExtra("service_category");
        if (serviceCategory != null && !serviceCategory.isEmpty()) {
            android.util.Log.d("ProviderProfileActivity", "Service category from Intent: " + serviceCategory);
        }

        firebaseService = new FirebaseProviderService();

        initializeViews();
        setupExpandableSections();
        setupButtons();
        setupBottomNavigation();

        // Load provider data from Firebase
        loadProviderData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }

    private void loadProviderData() {
        android.util.Log.d("ProviderProfileActivity", "Loading provider data for: " + providerName);

        // Set up real-time listener for auto-sync
        listenerRegistration = firebaseService.listenToProviderByName(providerName,
                new FirebaseProviderService.ProviderDetailCallback() {
                    @Override
                    public void onSuccess(ProviderDetail providerDetail) {
                        android.util.Log.d("ProviderProfileActivity", "Provider data loaded successfully: " +
                                (providerDetail != null ? providerDetail.getName() : "null"));
                        currentProvider = providerDetail;
                        if (providerDetail != null) {
                            populateUI(providerDetail);
                        } else {
                            android.util.Log.e("ProviderProfileActivity",
                                    "ProviderDetail is null after successful load");
                            Toast.makeText(ProviderProfileActivity.this,
                                    getString(R.string.error_provider_data_incomplete),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(String error) {
                        android.util.Log.e("ProviderProfileActivity", "Error loading provider: " + error);
                        Toast.makeText(ProviderProfileActivity.this, getString(R.string.error_loading_provider, error),
                                Toast.LENGTH_LONG).show();
                        // Try to load from one-time fetch as fallback
                        android.util.Log.d("ProviderProfileActivity", "Attempting fallback fetch for: " + providerName);
                        firebaseService.getProviderByName(providerName,
                                new FirebaseProviderService.ProviderDetailCallback() {
                                    @Override
                                    public void onSuccess(ProviderDetail providerDetail) {
                                        android.util.Log.d("ProviderProfileActivity", "Fallback fetch successful: " +
                                                (providerDetail != null ? providerDetail.getName() : "null"));
                                        currentProvider = providerDetail;
                                        if (providerDetail != null) {
                                            populateUI(providerDetail);
                                        }
                                    }

                                    @Override
                                    public void onError(String error) {
                                        android.util.Log.e("ProviderProfileActivity",
                                                "Fallback fetch also failed: " + error);
                                        Toast.makeText(ProviderProfileActivity.this,
                                                "Failed to load provider data. Please check your Firebase connection.",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                });
    }

    private void initializeViews() {
        // Back button
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Favorite button removed

        // Share button
        ImageButton btnShare = findViewById(R.id.btnShare);
        if (btnShare != null) {
            btnShare.setOnClickListener(v -> {
                // Share provider profile
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                String shareText = getString(R.string.share_provider_text,
                        (currentProvider != null && currentProvider.getName() != null ? currentProvider.getName()
                                : providerName));
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
                startActivity(Intent.createChooser(shareIntent, "Share provider profile"));
            });
        }
    }

    private void populateUI(ProviderDetail provider) {
        if (provider == null) {
            Toast.makeText(this, getString(R.string.error_provider_data_not_found), Toast.LENGTH_SHORT).show();
            return;
        }

        // Basic Profile Info
        TextView tvProviderName = findViewById(R.id.tvProviderName);
        TextView tvProfession = findViewById(R.id.tvProfession);
        TextView tvRating = findViewById(R.id.tvRating);
        ImageView ivProfilePicture = findViewById(R.id.ivProfilePicture);

        if (tvProviderName != null)
            tvProviderName.setText(provider.getName() != null ? provider.getName() : "");
        if (tvProfession != null)
            tvProfession.setText(provider.getService() != null ? provider.getService() : "");

        if (provider.getRating() > 0) {
            String ratingText = String.format("%.1f", provider.getRating());
            if (tvRating != null)
                tvRating.setText(ratingText);
        }

        // Load profile image
        if (provider.getProfileImageUrl() != null && !provider.getProfileImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(provider.getProfileImageUrl())
                    .placeholder(R.drawable.no_profile_image)
                    .error(R.drawable.no_profile_image)
                    .into(ivProfilePicture);
        } else {
            // Default fallback for profile image
            ivProfilePicture.setImageResource(R.drawable.no_profile_image);
        }

        // Validating View References
        TextView tvJobsCompletedAbout = findViewById(R.id.tvJobsCompletedAbout);
        TextView tvResponseTime = findViewById(R.id.tvResponseTime);
        TextView tvRepeatCustomers = findViewById(R.id.tvRepeatCustomers);

        if (tvJobsCompletedAbout != null && provider.getJobsCompleted() != null) {
            tvJobsCompletedAbout.setText(provider.getJobsCompleted());
        }
        if (tvResponseTime != null && provider.getResponseTime() != null) {
            tvResponseTime.setText(provider.getResponseTime());
        }
        if (tvRepeatCustomers != null && provider.getRepeatCustomers() != null) {
            tvRepeatCustomers.setText(provider.getRepeatCustomers());
        }

        // Pricing
        TextView tvPricing = findViewById(R.id.tvPricing);
        if (tvPricing != null) {
            if (provider.getPricingRange() != null && !provider.getPricingRange().isEmpty()) {
                tvPricing.setText(provider.getPricingRange());
            } else if (provider.getPrice() != null && !provider.getPrice().isEmpty()) {
                tvPricing.setText(provider.getPrice());
            }
        }

        // About Section
        TextView tvAbout = findViewById(R.id.tvAbout);
        if (tvAbout != null && provider.getAbout() != null) {
            tvAbout.setText(provider.getAbout());
        }

        // Typical Job Costs
        LinearLayout llJobCostsContent = findViewById(R.id.llJobCostsContent);
        if (llJobCostsContent != null && provider.getTypicalJobCosts() != null
                && !provider.getTypicalJobCosts().isEmpty()) {
            llJobCostsContent.removeAllViews();
            for (Map<String, String> jobCost : provider.getTypicalJobCosts()) {
                String job = jobCost.get("job");
                String cost = jobCost.get("cost");
                if (job != null && cost != null) {
                    TextView costText = new TextView(this);

                    // Parse and format cost
                    String formattedCost = cost;
                    try {
                        String cleanCost = cost.replaceAll("[^\\d.]", "");
                        if (!cleanCost.isEmpty()) {
                            double priceVal = Double.parseDouble(cleanCost);
                            formattedCost = com.example.homerepairs.utils.CurrencyHelper.formatPrice(this, priceVal);
                        }
                    } catch (NumberFormatException e) {
                        // Keep original text if parsing fails
                    }

                    costText.setText(getString(R.string.cost_format, job, formattedCost));
                    costText.setTextSize(14);
                    costText.setTextColor(getResources().getColor(R.color.text_secondary));
                    LinearLayout.MarginLayoutParams params = new LinearLayout.MarginLayoutParams(
                            LinearLayout.MarginLayoutParams.MATCH_PARENT,
                            LinearLayout.MarginLayoutParams.WRAP_CONTENT);
                    params.setMargins(0, 0, 0, 8);
                    costText.setLayoutParams(params);
                    llJobCostsContent.addView(costText);
                }
            }
        }
    }

    private void setupExpandableSections() {
        // Typical Job Costs
        LinearLayout cardJobCosts = findViewById(R.id.cardJobCosts);
        LinearLayout llJobCostsContent = findViewById(R.id.llJobCostsContent);
        ImageView ivJobCostsArrow = findViewById(R.id.ivJobCostsArrow);

        if (cardJobCosts != null && llJobCostsContent != null && ivJobCostsArrow != null) {
            cardJobCosts.setOnClickListener(v -> {
                isJobCostsExpanded = !isJobCostsExpanded;
                toggleExpandableSection(llJobCostsContent, ivJobCostsArrow, isJobCostsExpanded);
            });
        }

        // Payment Methods logic removed as view ID is missing

    }

    private void toggleExpandableSection(View contentView, ImageView arrowView, boolean isExpanded) {
        if (isExpanded) {
            contentView.setVisibility(View.VISIBLE);
            // Rotate to 270 degrees (pointing up) when expanded
            ObjectAnimator rotation = ObjectAnimator.ofFloat(arrowView, "rotation", arrowView.getRotation(), 270f);
            rotation.setDuration(200);
            rotation.start();
        } else {
            contentView.setVisibility(View.GONE);
            // Rotate to 90 degrees (pointing down) when collapsed
            ObjectAnimator rotation = ObjectAnimator.ofFloat(arrowView, "rotation", arrowView.getRotation(), 90f);
            rotation.setDuration(200);
            rotation.start();
        }
    }

    private void setupButtons() {
        Button btnGetQuote = findViewById(R.id.btnGetQuote);

        Button btnBookNow = findViewById(R.id.btnBookNow);

        // Setup Get Quote button
        if (btnGetQuote != null) {
            btnGetQuote.setOnClickListener(v -> {
                animateButtonClick(v);
                android.util.Log.d("ProviderProfileActivity", "Get Quote button clicked");
                Toast.makeText(this, getString(R.string.msg_quote_sent), Toast.LENGTH_SHORT).show();
            });
        } else {
            android.util.Log.e("ProviderProfileActivity", "btnGetQuote not found in layout");
        }

        // Setup Book Now button - navigate to NewBookingActivity
        if (btnBookNow != null) {
            btnBookNow.setOnClickListener(v -> {
                animateButtonClick(v);
                android.util.Log.d("ProviderProfileActivity", "Book Now button clicked");
                try {
                    Intent intent = new Intent(this, NewBookingActivity.class);
                    // Pass provider information to booking screen
                    String bookingProviderName = currentProvider != null && currentProvider.getName() != null
                            ? currentProvider.getName()
                            : providerName;
                    String serviceCategory = currentProvider != null && currentProvider.getService() != null
                            ? currentProvider.getService()
                            : "Plumbing";
                    String providerId = currentProvider != null && currentProvider.getId() != null
                            ? currentProvider.getId()
                            : null;

                    if (bookingProviderName != null) {
                        intent.putExtra("provider_name", bookingProviderName);
                    }
                    if (serviceCategory != null) {
                        intent.putExtra("service_category", serviceCategory);
                    }
                    if (providerId != null) {
                        intent.putExtra("provider_id", providerId);
                    }

                    android.util.Log.d("ProviderProfileActivity",
                            "Navigating to NewBookingActivity with provider: " + bookingProviderName);
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                } catch (Exception e) {
                    android.util.Log.e("ProviderProfileActivity", "Error navigating to NewBookingActivity", e);
                    Toast.makeText(this, getString(R.string.error_opening_booking, e.getMessage()), Toast.LENGTH_SHORT)
                            .show();
                }
            });
        } else {
            android.util.Log.e("ProviderProfileActivity", "btnBookNow not found in layout");
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        if (bottomNavigation == null) {
            android.util.Log.w("ProviderProfileActivity", "BottomNavigationView not found");
            return;
        }

        bottomNavigation.setOnItemSelectedListener(item -> {
            // Ignore selections during initialization to prevent auto-navigation
            if (isInitializingBottomNav) {
                android.util.Log.d("ProviderProfileActivity", "Ignoring bottom nav selection during initialization");
                return false;
            }

            int itemId = item.getItemId();
            android.util.Log.d("ProviderProfileActivity", "Bottom nav item selected: " + itemId);

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

        // Don't set selected item - ProviderProfileActivity is not a main navigation
        // screen
        // Setting it would trigger navigation to ProfileActivity
        // Mark initialization as complete after a short delay to allow UI to settle
        bottomNavigation.post(() -> {
            isInitializingBottomNav = false;
            android.util.Log.d("ProviderProfileActivity", "Bottom navigation initialization complete");
        });
    }

    private void animateButtonClick(View button) {
        float originalScaleX = button.getScaleX();
        float originalScaleY = button.getScaleY();

        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(button, "scaleX", originalScaleX, 0.94f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(button, "scaleY", originalScaleY, 0.94f);

        scaleDownX.setDuration(80);
        scaleDownY.setDuration(80);
        scaleDownX.setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f));
        scaleDownY.setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f));

        scaleDownX.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(button, "scaleX", 0.94f, originalScaleX);
                ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(button, "scaleY", 0.94f, originalScaleY);

                scaleUpX.setDuration(120);
                scaleUpY.setDuration(120);

                OvershootInterpolator springInterpolator = new OvershootInterpolator(1.1f);
                scaleUpX.setInterpolator(springInterpolator);
                scaleUpY.setInterpolator(springInterpolator);

                scaleUpX.start();
                scaleUpY.start();
            }
        });

        scaleDownX.start();
        scaleDownY.start();
    }

    // Review data class
    public static class Review {
        private String reviewerName;
        private String timeAgo;
        private int rating;
        private String comment;
        private int thumbsUp;
        private int thumbsDown;

        public Review(String reviewerName, String timeAgo, int rating, String comment, int thumbsUp, int thumbsDown) {
            this.reviewerName = reviewerName;
            this.timeAgo = timeAgo;
            this.rating = rating;
            this.comment = comment;
            this.thumbsUp = thumbsUp;
            this.thumbsDown = thumbsDown;
        }

        public String getReviewerName() {
            return reviewerName;
        }

        public String getTimeAgo() {
            return timeAgo;
        }

        public int getRating() {
            return rating;
        }

        public String getComment() {
            return comment;
        }

        public int getThumbsUp() {
            return thumbsUp;
        }

        public int getThumbsDown() {
            return thumbsDown;
        }
    }
}
