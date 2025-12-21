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

public class ProviderProfileActivity extends AppCompatActivity {

    private boolean isJobCostsExpanded = false;
    private boolean isPaymentMethodsExpanded = false;
    private boolean isServiceAreaExpanded = false;
    private boolean isLanguagesExpanded = false;
    
    private Calendar currentCalendar;
    private CalendarAdapter calendarAdapter;
    private ReviewAdapter reviewAdapter;
    private PortfolioAdapter portfolioAdapter;
    
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

        currentCalendar = Calendar.getInstance();
        firebaseService = new FirebaseProviderService();
        
        initializeViews();
        setupExpandableSections();
        setupCalendar();
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
        listenerRegistration = firebaseService.listenToProviderByName(providerName, new FirebaseProviderService.ProviderDetailCallback() {
            @Override
            public void onSuccess(ProviderDetail providerDetail) {
                android.util.Log.d("ProviderProfileActivity", "Provider data loaded successfully: " + 
                    (providerDetail != null ? providerDetail.getName() : "null"));
                currentProvider = providerDetail;
                if (providerDetail != null) {
                    populateUI(providerDetail);
                } else {
                    android.util.Log.e("ProviderProfileActivity", "ProviderDetail is null after successful load");
                    Toast.makeText(ProviderProfileActivity.this, "Provider data is incomplete", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onError(String error) {
                android.util.Log.e("ProviderProfileActivity", "Error loading provider: " + error);
                Toast.makeText(ProviderProfileActivity.this, "Error loading provider: " + error, Toast.LENGTH_LONG).show();
                // Try to load from one-time fetch as fallback
                android.util.Log.d("ProviderProfileActivity", "Attempting fallback fetch for: " + providerName);
                firebaseService.getProviderByName(providerName, new FirebaseProviderService.ProviderDetailCallback() {
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
                        android.util.Log.e("ProviderProfileActivity", "Fallback fetch also failed: " + error);
                        Toast.makeText(ProviderProfileActivity.this, "Failed to load provider data. Please check your Firebase connection.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void initializeViews() {
        // Back button
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Search button
        ImageButton btnSearch = findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(v -> {
            Toast.makeText(this, "Search functionality", Toast.LENGTH_SHORT).show();
        });

        // Favorite button
        ImageButton btnFavorite = findViewById(R.id.btnFavorite);
        if (btnFavorite != null) {
            btnFavorite.setOnClickListener(v -> {
                // Toggle favorite state
                boolean isFavorite = btnFavorite.getTag() != null && (Boolean) btnFavorite.getTag();
                isFavorite = !isFavorite;
                btnFavorite.setTag(isFavorite);
                
                // Update icon to show favorite state - filled red heart or outline
                if (isFavorite) {
                    btnFavorite.setImageResource(R.drawable.heart_filled);
                    btnFavorite.clearColorFilter(); // No tint needed for filled heart
                } else {
                    btnFavorite.setImageResource(R.drawable.heart_outline);
                    btnFavorite.setColorFilter(getResources().getColor(R.color.text_primary));
                }
                
                Toast.makeText(this, isFavorite ? "Added to favorites" : "Removed from favorites", Toast.LENGTH_SHORT).show();
            });
        }

        // Share button
        ImageButton btnShare = findViewById(R.id.btnShare);
        if (btnShare != null) {
            btnShare.setOnClickListener(v -> {
                // Share provider profile
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                String shareText = "Check out this service provider: " + 
                    (currentProvider != null && currentProvider.getName() != null ? currentProvider.getName() : providerName);
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
                startActivity(Intent.createChooser(shareIntent, "Share provider profile"));
            });
        }
    }
    
    private void populateUI(ProviderDetail provider) {
        if (provider == null) {
            Toast.makeText(this, "Provider data not found", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Basic Profile Info
        TextView tvProviderName = findViewById(R.id.tvProviderName);
        TextView tvProfession = findViewById(R.id.tvProfession);
        TextView tvRating = findViewById(R.id.tvRating);
        TextView tvAvailable = findViewById(R.id.tvAvailable);
        ImageView ivProfilePicture = findViewById(R.id.ivProfilePicture);
        
        if (tvProviderName != null) tvProviderName.setText(provider.getName() != null ? provider.getName() : "");
        if (tvProfession != null) tvProfession.setText(provider.getService() != null ? provider.getService() : "");
        
        if (provider.getRating() > 0 && provider.getReviewCount() > 0) {
            String ratingText = String.format("%.1f", provider.getRating());
            if (tvRating != null) tvRating.setText(ratingText);
            
            // Set review count in separate TextView
            TextView tvReviewCount = findViewById(R.id.tvReviewCount);
            if (tvReviewCount != null) {
                String reviewText = String.format("(%d reviews)", provider.getReviewCount());
                tvReviewCount.setText(reviewText);
            }
        }
        
        if (provider.isAvailableNow()) {
            if (tvAvailable != null) {
                tvAvailable.setText("Available Now");
                tvAvailable.setTextColor(getResources().getColor(R.color.text_white));
                tvAvailable.setBackgroundResource(R.drawable.availability_badge_background);
            }
        } else {
            if (tvAvailable != null) {
                tvAvailable.setText("Unavailable");
                tvAvailable.setTextColor(getResources().getColor(R.color.text_white));
                tvAvailable.setBackgroundResource(R.drawable.availability_badge_unavailable);
            }
        }
        
        // Load profile image
        if (provider.getProfileImageUrl() != null && !provider.getProfileImageUrl().isEmpty()) {
            Glide.with(this)
                .load(provider.getProfileImageUrl())
                .placeholder(R.drawable.ic_profile)
                .into(ivProfilePicture);
        } else if (provider.getProfileImageResId() != 0) {
            ivProfilePicture.setImageResource(provider.getProfileImageResId());
        }
        
        // Key Metrics
        TextView tvYearsInBusiness = findViewById(R.id.tvYearsInBusiness);
        TextView tvJobsCompleted = findViewById(R.id.tvJobsCompleted);
        TextView tvResponseTime = findViewById(R.id.tvResponseTime);
        TextView tvRepeatCustomers = findViewById(R.id.tvRepeatCustomers);
        
        if (tvYearsInBusiness != null && provider.getYearsInBusiness() > 0) {
            tvYearsInBusiness.setText(String.valueOf(provider.getYearsInBusiness()));
        }
        if (tvJobsCompleted != null && provider.getJobsCompleted() != null) {
            tvJobsCompleted.setText(provider.getJobsCompleted());
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
        
        // Services Tags
        LinearLayout llServiceTags = findViewById(R.id.llServiceTags);
        if (llServiceTags != null && provider.getServices() != null && !provider.getServices().isEmpty()) {
            llServiceTags.removeAllViews();
            for (String service : provider.getServices()) {
                TextView serviceTag = new TextView(this);
                serviceTag.setText(service);
                serviceTag.setTextSize(13);
                serviceTag.setTextColor(getResources().getColor(R.color.text_secondary));
                serviceTag.setBackgroundResource(R.drawable.search_bar_background);
                serviceTag.getBackground().setTint(0xFFE3F2FD); // Light blue tint
                serviceTag.setPadding(28, 16, 28, 16);
                LinearLayout.MarginLayoutParams params = new LinearLayout.MarginLayoutParams(
                    LinearLayout.MarginLayoutParams.WRAP_CONTENT,
                    LinearLayout.MarginLayoutParams.WRAP_CONTENT
                );
                params.setMargins(0, 0, 16, 16);
                serviceTag.setLayoutParams(params);
                llServiceTags.addView(serviceTag);
            }
        }
        
        // About Section - Completed Jobs and Experience
        TextView tvJobsCompletedAbout = findViewById(R.id.tvJobsCompletedAbout);
        TextView tvExperience = findViewById(R.id.tvExperience);
        
        if (tvJobsCompletedAbout != null && provider.getJobsCompleted() != null) {
            // Extract just the number if it contains text like "328 jobs" or "500+"
            String jobsText = provider.getJobsCompleted();
            // Remove non-numeric characters except + and keep the number
            String jobsNumber = jobsText.replaceAll("[^0-9+]", "");
            tvJobsCompletedAbout.setText(jobsNumber.isEmpty() ? jobsText : jobsNumber);
        }
        
        if (tvExperience != null && provider.getYearsInBusiness() > 0) {
            tvExperience.setText(provider.getYearsInBusiness() + " years");
        }
        
        // Service Area - Create list items with map pin icons
        LinearLayout llServiceAreaContent = findViewById(R.id.llServiceAreaContent);
        if (llServiceAreaContent != null) {
            llServiceAreaContent.removeAllViews();
            
            // Parse service area string (comma-separated or array)
            List<String> serviceAreas = new ArrayList<>();
            if (provider.getServiceArea() != null && !provider.getServiceArea().isEmpty()) {
                // Split by comma if it's a string, or use as single item
                String[] areas = provider.getServiceArea().split(",");
                for (String area : areas) {
                    String trimmed = area.trim();
                    if (!trimmed.isEmpty()) {
                        serviceAreas.add(trimmed);
                    }
                }
            }
            
            // Default areas if none provided
            if (serviceAreas.isEmpty()) {
                serviceAreas.add("Phnom Penh Downtown");
                serviceAreas.add("Toul Kork");
                serviceAreas.add("BKK1");
                serviceAreas.add("Riverside");
            }
            
            // Create a list item for each service area
            for (String area : serviceAreas) {
                LinearLayout itemLayout = new LinearLayout(this);
                itemLayout.setOrientation(LinearLayout.HORIZONTAL);
                itemLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
                itemLayout.setBackgroundResource(R.drawable.service_area_item_background);
                itemLayout.setPadding(32, 16, 32, 16);
                
                LinearLayout.MarginLayoutParams params = new LinearLayout.MarginLayoutParams(
                    LinearLayout.MarginLayoutParams.MATCH_PARENT,
                    LinearLayout.MarginLayoutParams.WRAP_CONTENT
                );
                params.setMargins(0, 0, 0, 10);
                itemLayout.setLayoutParams(params);
                
                // Map pin icon
                ImageView pinIcon = new ImageView(this);
                pinIcon.setImageResource(R.drawable.ic_location);
                pinIcon.setColorFilter(0xFF64B5F6); // Light blue color
                LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(20, 20);
                iconParams.setMargins(0, 0, 12, 0);
                pinIcon.setLayoutParams(iconParams);
                
                // Area name text
                TextView areaText = new TextView(this);
                areaText.setText(area);
                areaText.setTextSize(14);
                areaText.setTextColor(getResources().getColor(R.color.text_primary));
                LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                areaText.setLayoutParams(textParams);
                
                itemLayout.addView(pinIcon);
                itemLayout.addView(areaText);
                llServiceAreaContent.addView(itemLayout);
            }
        }
        
        // Languages - Create tags with light purple background
        LinearLayout llLanguagesContent = findViewById(R.id.llLanguagesContent);
        if (llLanguagesContent != null) {
            llLanguagesContent.removeAllViews();
            
            List<String> languages = new ArrayList<>();
            if (provider.getLanguages() != null && !provider.getLanguages().isEmpty()) {
                languages = provider.getLanguages();
            } else {
                // Default languages if none provided
                languages.add("English");
                languages.add("Khmer");
                languages.add("Chinese");
            }
            
            for (String language : languages) {
                TextView languageTag = new TextView(this);
                languageTag.setText(language);
                languageTag.setTextSize(14);
                languageTag.setTextColor(0xFF7B1FA2); // Dark purple text
                languageTag.setBackgroundResource(R.drawable.language_tag_background);
                languageTag.setPadding(28, 14, 28, 14);
                LinearLayout.MarginLayoutParams params = new LinearLayout.MarginLayoutParams(
                    LinearLayout.MarginLayoutParams.WRAP_CONTENT,
                    LinearLayout.MarginLayoutParams.WRAP_CONTENT
                );
                params.setMargins(0, 0, 12, 12);
                languageTag.setLayoutParams(params);
                llLanguagesContent.addView(languageTag);
            }
        }
        
        // Payment Methods
        LinearLayout llPaymentMethodsContent = findViewById(R.id.llPaymentMethodsContent);
        if (llPaymentMethodsContent != null && provider.getPaymentMethods() != null && !provider.getPaymentMethods().isEmpty()) {
            llPaymentMethodsContent.removeAllViews();
            for (String paymentMethod : provider.getPaymentMethods()) {
                TextView paymentTag = new TextView(this);
                paymentTag.setText(paymentMethod);
                paymentTag.setTextSize(14);
                paymentTag.setTextColor(getResources().getColor(R.color.text_secondary));
                paymentTag.setBackgroundResource(R.drawable.search_bar_background);
                paymentTag.setPadding(24, 12, 24, 12);
                LinearLayout.MarginLayoutParams params = new LinearLayout.MarginLayoutParams(
                    LinearLayout.MarginLayoutParams.WRAP_CONTENT,
                    LinearLayout.MarginLayoutParams.WRAP_CONTENT
                );
                params.setMargins(0, 0, 16, 0);
                paymentTag.setLayoutParams(params);
                llPaymentMethodsContent.addView(paymentTag);
            }
        }
        
        // Typical Job Costs
        LinearLayout llJobCostsContent = findViewById(R.id.llJobCostsContent);
        if (llJobCostsContent != null && provider.getTypicalJobCosts() != null && !provider.getTypicalJobCosts().isEmpty()) {
            llJobCostsContent.removeAllViews();
            for (Map<String, String> jobCost : provider.getTypicalJobCosts()) {
                String job = jobCost.get("job");
                String cost = jobCost.get("cost");
                if (job != null && cost != null) {
                    TextView costText = new TextView(this);
                    costText.setText("• " + job + ": " + cost);
                    costText.setTextSize(14);
                    costText.setTextColor(getResources().getColor(R.color.text_secondary));
                    LinearLayout.MarginLayoutParams params = new LinearLayout.MarginLayoutParams(
                        LinearLayout.MarginLayoutParams.MATCH_PARENT,
                        LinearLayout.MarginLayoutParams.WRAP_CONTENT
                    );
                    params.setMargins(0, 0, 0, 8);
                    costText.setLayoutParams(params);
                    llJobCostsContent.addView(costText);
                }
            }
        }
        
        // Setup Reviews with Firebase data
        setupReviews(provider);
        
        // Setup Portfolio with Firebase data
        setupPortfolio(provider);
        
        // Update rating distribution
        updateRatingDistribution(provider);
    }

    private void setupExpandableSections() {
        // Typical Job Costs
        MaterialCardView cardJobCosts = findViewById(R.id.cardJobCosts);
        LinearLayout llJobCostsContent = findViewById(R.id.llJobCostsContent);
        ImageView ivJobCostsArrow = findViewById(R.id.ivJobCostsArrow);

        cardJobCosts.setOnClickListener(v -> {
            isJobCostsExpanded = !isJobCostsExpanded;
            toggleExpandableSection(llJobCostsContent, ivJobCostsArrow, isJobCostsExpanded);
        });

        // Payment Methods
        MaterialCardView cardPaymentMethods = findViewById(R.id.cardPaymentMethods);
        LinearLayout llPaymentMethodsContent = findViewById(R.id.llPaymentMethodsContent);
        ImageView ivPaymentMethodsArrow = findViewById(R.id.ivPaymentMethodsArrow);

        cardPaymentMethods.setOnClickListener(v -> {
            isPaymentMethodsExpanded = !isPaymentMethodsExpanded;
            toggleExpandableSection(llPaymentMethodsContent, ivPaymentMethodsArrow, isPaymentMethodsExpanded);
        });

        // Service Area
        MaterialCardView cardServiceArea = findViewById(R.id.cardServiceArea);
        LinearLayout llServiceAreaContent = findViewById(R.id.llServiceAreaContent);
        ImageView ivServiceAreaArrow = findViewById(R.id.ivServiceAreaArrow);
        
        // Get Languages section references for auto-close
        LinearLayout llLanguagesContent = findViewById(R.id.llLanguagesContent);
        ImageView ivLanguagesArrow = findViewById(R.id.ivLanguagesArrow);

        cardServiceArea.setOnClickListener(v -> {
            isServiceAreaExpanded = !isServiceAreaExpanded;
            toggleExpandableSection(llServiceAreaContent, ivServiceAreaArrow, isServiceAreaExpanded);
            
            // Auto-close Languages Spoken when Service Area Coverage is expanded
            if (isServiceAreaExpanded && isLanguagesExpanded) {
                isLanguagesExpanded = false;
                toggleExpandableSection(llLanguagesContent, ivLanguagesArrow, false);
            }
        });

        // Languages
        MaterialCardView cardLanguages = findViewById(R.id.cardLanguages);

        cardLanguages.setOnClickListener(v -> {
            isLanguagesExpanded = !isLanguagesExpanded;
            toggleExpandableSection(llLanguagesContent, ivLanguagesArrow, isLanguagesExpanded);
            
            // Auto-close Service Area Coverage when Languages Spoken is expanded
            if (isLanguagesExpanded && isServiceAreaExpanded) {
                isServiceAreaExpanded = false;
                toggleExpandableSection(llServiceAreaContent, ivServiceAreaArrow, false);
            }
        });
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

    private void setupCalendar() {
        RecyclerView rvCalendar = findViewById(R.id.rvCalendar);
        TextView tvMonthYear = findViewById(R.id.tvMonthYear);
        TextView tvSelectedDate = findViewById(R.id.tvSelectedDate);
        ImageButton btnPrevMonth = findViewById(R.id.btnPrevMonth);
        ImageButton btnNextMonth = findViewById(R.id.btnNextMonth);

        updateMonthYearDisplay(tvMonthYear);

        calendarAdapter = new CalendarAdapter(currentCalendar, (day, formattedDate) -> {
            if (tvSelectedDate != null) {
                tvSelectedDate.setText("Selected: " + formattedDate);
                tvSelectedDate.setVisibility(View.VISIBLE);
            }
        });
        
        GridLayoutManager layoutManager = new GridLayoutManager(this, 7);
        rvCalendar.setLayoutManager(layoutManager);
        rvCalendar.setAdapter(calendarAdapter);

        btnPrevMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            updateMonthYearDisplay(tvMonthYear);
            calendarAdapter.updateCalendar(currentCalendar);
            // Clear selected date when month changes
            if (tvSelectedDate != null) {
                tvSelectedDate.setVisibility(View.GONE);
            }
        });

        btnNextMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            updateMonthYearDisplay(tvMonthYear);
            calendarAdapter.updateCalendar(currentCalendar);
            // Clear selected date when month changes
            if (tvSelectedDate != null) {
                tvSelectedDate.setVisibility(View.GONE);
            }
        });
    }

    private void updateMonthYearDisplay(TextView tvMonthYear) {
        String[] months = {"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};
        int month = currentCalendar.get(Calendar.MONTH);
        int year = currentCalendar.get(Calendar.YEAR);
        tvMonthYear.setText(months[month] + " " + year);
    }

    private void setupReviews(ProviderDetail provider) {
        RecyclerView rvReviews = findViewById(R.id.rvReviews);
        Button btnSortReviews = findViewById(R.id.btnSortReviews);
        Button btnFilterReviews = findViewById(R.id.btnFilterReviews);

        List<com.example.homerepairs.models.Review> reviews = new ArrayList<>();
        if (provider != null && provider.getReviews() != null) {
            reviews = provider.getReviews();
        }
        
        // Convert to inner Review class for adapter compatibility
        List<Review> adapterReviews = new ArrayList<>();
        for (com.example.homerepairs.models.Review review : reviews) {
            adapterReviews.add(new Review(
                review.getReviewerName(),
                review.getTimeAgo(),
                review.getRating(),
                review.getComment(),
                review.getThumbsUp(),
                review.getThumbsDown()
            ));
        }
        
        reviewAdapter = new ReviewAdapter(adapterReviews);
        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        rvReviews.setAdapter(reviewAdapter);

        btnSortReviews.setOnClickListener(v -> showSortDialog());
        btnFilterReviews.setOnClickListener(v -> showFilterDialog());
    }

    private void showSortDialog() {
        String[] options = {"Most Recent", "Highest Rated", "Lowest Rated", "Most Helpful"};
        new AlertDialog.Builder(this)
                .setTitle("Sort Reviews")
                .setItems(options, (dialog, which) -> {
                    Toast.makeText(this, "Sorted by: " + options[which], Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showFilterDialog() {
        String[] options = {"All Ratings", "5 Stars", "4 Stars", "3 Stars", "2 Stars", "1 Star"};
        new AlertDialog.Builder(this)
                .setTitle("Filter Reviews")
                .setItems(options, (dialog, which) -> {
                    Toast.makeText(this, "Filtered by: " + options[which], Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void setupPortfolio(ProviderDetail provider) {
        RecyclerView rvPortfolio = findViewById(R.id.rvPortfolio);
        TextView tvPortfolioDescription = findViewById(R.id.tvPortfolioDescription);
        
        List<PortfolioItem> portfolioItems = new ArrayList<>();
        if (provider != null && provider.getPortfolio() != null && !provider.getPortfolio().isEmpty()) {
            portfolioItems = provider.getPortfolio();
        } else {
            // Default portfolio items to match the design
            PortfolioItem item1 = new PortfolioItem();
            item1.setLabel("Kitchen Sink Installation");
            item1.setDescription("Installed new sink with modern fixtures");
            portfolioItems.add(item1);
            
            PortfolioItem item2 = new PortfolioItem();
            item2.setLabel("Bathroom Renovation");
            item2.setDescription("Complete bathroom plumbing overhaul");
            portfolioItems.add(item2);
            
            PortfolioItem item3 = new PortfolioItem();
            item3.setLabel("Water Heater Replacement");
            item3.setDescription("Replaced old heater with energy-efficient model");
            portfolioItems.add(item3);
        }
        
        // Set portfolio description
        if (tvPortfolioDescription != null) {
            if (provider != null && provider.getPortfolioDescription() != null && !provider.getPortfolioDescription().isEmpty()) {
            tvPortfolioDescription.setText(provider.getPortfolioDescription());
                tvPortfolioDescription.setVisibility(View.VISIBLE);
            } else {
                tvPortfolioDescription.setVisibility(View.GONE);
            }
        }

        portfolioAdapter = new PortfolioAdapter(portfolioItems);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvPortfolio.setLayoutManager(layoutManager);
        rvPortfolio.setAdapter(portfolioAdapter);
    }
    
    private void updateRatingDistribution(ProviderDetail provider) {
        if (provider == null || provider.getRatingDistribution() == null) {
            return;
        }
        
        Map<String, Integer> distribution = provider.getRatingDistribution();
        ProgressBar progress5Stars = findViewById(R.id.progress5Stars);
        ProgressBar progress4Stars = findViewById(R.id.progress4Stars);
        ProgressBar progress3Stars = findViewById(R.id.progress3Stars);
        ProgressBar progress2Stars = findViewById(R.id.progress2Stars);
        ProgressBar progress1Star = findViewById(R.id.progress1Star);
        
        if (progress5Stars != null && distribution.containsKey("5")) {
            progress5Stars.setProgress(distribution.get("5"));
        }
        if (progress4Stars != null && distribution.containsKey("4")) {
            progress4Stars.setProgress(distribution.get("4"));
        }
        if (progress3Stars != null && distribution.containsKey("3")) {
            progress3Stars.setProgress(distribution.get("3"));
        }
        if (progress2Stars != null && distribution.containsKey("2")) {
            progress2Stars.setProgress(distribution.get("2"));
        }
        if (progress1Star != null && distribution.containsKey("1")) {
            progress1Star.setProgress(distribution.get("1"));
        }
        
        // Update overall rating display
        TextView tvOverallRating = findViewById(R.id.tvOverallRating);
        TextView tvReviewCount = findViewById(R.id.tvReviewCount);
        if (tvOverallRating != null && provider.getRating() > 0) {
            tvOverallRating.setText(String.format("%.1f", provider.getRating()));
        }
        if (tvReviewCount != null && provider.getReviewCount() > 0) {
            tvReviewCount.setText(provider.getReviewCount() + " reviews");
        }
    }

    private void setupButtons() {
        Button btnGetQuote = findViewById(R.id.btnGetQuote);
        Button btnMessage = findViewById(R.id.btnMessage);
        Button btnBookNow = findViewById(R.id.btnBookNow);

        // Setup Get Quote button
        if (btnGetQuote != null) {
            btnGetQuote.setOnClickListener(v -> {
                animateButtonClick(v);
                android.util.Log.d("ProviderProfileActivity", "Get Quote button clicked");
                Toast.makeText(this, "Quote request sent!", Toast.LENGTH_SHORT).show();
            });
        } else {
            android.util.Log.e("ProviderProfileActivity", "btnGetQuote not found in layout");
        }

        // Setup Message button - navigate to ChatActivity directly
        if (btnMessage != null) {
            btnMessage.setOnClickListener(v -> {
                animateButtonClick(v);
                android.util.Log.d("ProviderProfileActivity", "Message button clicked");
                try {
                    Intent intent = new Intent(this, ChatActivity.class);
                    // Pass provider information to chat screen
                    if (currentProvider != null) {
                        String providerName = currentProvider.getName();
                        String providerId = currentProvider.getId();
                        if (providerName != null) {
                            intent.putExtra("providerName", providerName);
                        }
                        if (providerId != null) {
                            intent.putExtra("providerId", providerId);
                        }
                    }
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                } catch (Exception e) {
                    android.util.Log.e("ProviderProfileActivity", "Error navigating to ChatActivity", e);
                    Toast.makeText(this, "Error opening chat: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            android.util.Log.e("ProviderProfileActivity", "btnMessage not found in layout");
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
                        ? currentProvider.getName() : providerName;
                    String serviceCategory = currentProvider != null && currentProvider.getService() != null 
                        ? currentProvider.getService() : "Plumbing";
                    String providerId = currentProvider != null && currentProvider.getId() != null 
                        ? currentProvider.getId() : null;
                    
                    if (bookingProviderName != null) {
                        intent.putExtra("provider_name", bookingProviderName);
                    }
                    if (serviceCategory != null) {
                        intent.putExtra("service_category", serviceCategory);
                    }
                    if (providerId != null) {
                        intent.putExtra("provider_id", providerId);
                    }
                    
                    android.util.Log.d("ProviderProfileActivity", "Navigating to NewBookingActivity with provider: " + bookingProviderName);
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                } catch (Exception e) {
                    android.util.Log.e("ProviderProfileActivity", "Error navigating to NewBookingActivity", e);
                    Toast.makeText(this, "Error opening booking: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
        
        // Don't set selected item - ProviderProfileActivity is not a main navigation screen
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

        public String getReviewerName() { return reviewerName; }
        public String getTimeAgo() { return timeAgo; }
        public int getRating() { return rating; }
        public String getComment() { return comment; }
        public int getThumbsUp() { return thumbsUp; }
        public int getThumbsDown() { return thumbsDown; }
    }
}

