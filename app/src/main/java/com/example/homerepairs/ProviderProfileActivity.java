package com.example.homerepairs;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.homerepairs.adapters.PortfolioAdapter;
import com.example.homerepairs.adapters.ReviewAdapter;
import com.example.homerepairs.databinding.ActivityProviderProfileBinding;
import com.example.homerepairs.models.PortfolioItem;
import com.example.homerepairs.models.ProviderDetail;
import com.example.homerepairs.models.Review;
import com.example.homerepairs.services.FirebaseProviderService;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProviderProfileActivity extends BaseActivity {

    private ActivityProviderProfileBinding binding;
    private FirebaseProviderService firebaseService;
    private ListenerRegistration listenerRegistration;
    private ProviderDetail currentProvider;
    private String providerName = "Panha";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProviderProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String intentName = getIntent().getStringExtra("provider_name");
        if (intentName != null && !intentName.isEmpty())
            providerName = intentName;

        firebaseService = new FirebaseProviderService();
        initializeViews();
        setupButtons();
        loadProviderData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenerRegistration != null)
            listenerRegistration.remove();
    }

    private void loadProviderData() {
        listenerRegistration = firebaseService.listenToProviderByName(providerName,
                new FirebaseProviderService.ProviderDetailCallback() {
                    @Override
                    public void onSuccess(ProviderDetail detail) {
                        currentProvider = detail;
                        if (detail != null)
                            populateUI(detail);
                        else
                            Toast.makeText(ProviderProfileActivity.this, getString(R.string.msg_data_incomplete),
                                    Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String error) {
                        Log.e("ProviderProfileActivity", "Error: " + error);
                        firebaseService.getProviderByName(providerName,
                                new FirebaseProviderService.ProviderDetailCallback() {
                                    @Override
                                    public void onSuccess(ProviderDetail detail) {
                                        currentProvider = detail;
                                        if (detail != null)
                                            populateUI(detail);
                                    }

                                    @Override
                                    public void onError(String e) {
                                        Toast.makeText(ProviderProfileActivity.this,
                                                getString(R.string.msg_load_failed), Toast.LENGTH_SHORT)
                                                .show();
                                    }
                                });
                    }
                });
    }

    private void initializeViews() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnShare.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SEND).setType("text/plain");
            String name = currentProvider != null ? currentProvider.getName() : providerName;
            intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_provider_text_2, name));
            startActivity(Intent.createChooser(intent, getString(R.string.share)));
        });

    }

    private void populateUI(ProviderDetail provider) {
        binding.tvProviderName.setText(provider.getName() != null ? provider.getName() : "");
        binding.tvProfession.setText(provider.getService() != null ? provider.getService() : "");
        binding.tvRating.setText(String.format("%.1f", provider.getRating()));
        binding.tvLocation.setText(provider.getServiceArea() != null ? provider.getServiceArea() : "");

        Glide.with(this)
                .load(provider.getProfileImageUrl())
                .placeholder(R.drawable.no_profile_image)
                .error(R.drawable.no_profile_image)
                .circleCrop()
                .into(binding.ivProfilePicture);

        binding.tvJobsCount.setText(provider.getJobsCompleted() != null ? provider.getJobsCompleted() : "0");
        binding.tvRepeatRate.setText(provider.getRepeatCustomers() != null ? provider.getRepeatCustomers() : "0%");
        binding.tvArrival.setText(provider.getResponseTime() != null ? provider.getResponseTime() : "--");
        binding.tvExperience.setText(provider.getYearsInBusiness() + "y");

        binding.layoutBadgeVerified.setVisibility(provider.isVerified() ? View.VISIBLE : View.GONE);
        binding.layoutBadgeTopRated.setVisibility(provider.getRating() >= 4.5 ? View.VISIBLE : View.GONE);

        boolean fast = provider.getResponseTime() != null && (provider.getResponseTime().toLowerCase().contains("min")
                || provider.getResponseTime().contains("1 hour"));
        binding.layoutBadgeFastResponse.setVisibility(fast ? View.VISIBLE : View.GONE);

        String priceText = provider.getPricingRange() != null ? provider.getPricingRange() : provider.getPrice();
        if (priceText != null) {
            // Remove redundant per hour suffix as it's shown in the UI label below
            priceText = priceText.replace("/hr", "").replace("/ hr", "").replace("per hour", "").trim();
            // Remove trailing slash if it was " /"
            if (priceText.endsWith("/")) {
                priceText = priceText.substring(0, priceText.length() - 1).trim();
            }

            if (!priceText.isEmpty() && !priceText.startsWith("$") && priceText.matches(".*\\d.*")) {
                binding.tvPricing.setText("$" + priceText);
            } else {
                binding.tvPricing.setText(priceText);
            }
        } else {
            binding.tvPricing.setText("");
        }

        binding.tvAbout.setText(provider.getAbout() != null ? provider.getAbout() : "");
        binding.tvServiceArea.setText(provider.getServiceArea() != null ? provider.getServiceArea() : "");
        binding.layoutCertifications.setVisibility(provider.isHasCertifications() ? View.VISIBLE : View.GONE);

        setupSpecializations(provider);
        setupServices(provider);
        setupRatingBreakdown(provider);
        setupReviews(provider);

        binding.tvContactPhone
                .setText(provider.getPhoneNumber() != null ? provider.getPhoneNumber() : "+855 12 345 678");
        binding.btnContactPhone
                .setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_DIAL,
                        android.net.Uri.parse("tel:" + (provider.getPhoneNumber() != null
                                ? provider.getPhoneNumber().replaceAll("[^0-9+]", "")
                                : "12345678")))));

        binding.tvContactEmail.setText(provider.getEmail() != null ? provider.getEmail() : "contact@provider.com");
        binding.btnContactEmail.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_SENDTO, android.net.Uri
                .parse("mailto:" + (provider.getEmail() != null ? provider.getEmail() : "contact@provider.com")))));
    }

    // Portfolio section removed

    private void setupSpecializations(ProviderDetail provider) {
        binding.chipGroupSpecializations.removeAllViews();
        List<String> services = provider.getServices();
        if (services != null) {
            for (String s : services) {
                Chip chip = new Chip(this);
                chip.setText(s);
                chip.setClickable(false);
                binding.chipGroupSpecializations.addView(chip);
            }
        }
    }

    private void setupServices(ProviderDetail provider) {
        if (provider.getTypicalJobCosts() == null)
            return;
        binding.llServicesList.removeAllViews();
        for (Map<String, String> job : provider.getTypicalJobCosts()) {
            View v = getLayoutInflater().inflate(R.layout.item_service_box, binding.llServicesList, false);
            TextView t = v.findViewById(R.id.tvServiceTitle);
            TextView d = v.findViewById(R.id.tvServiceDesc);
            TextView c = v.findViewById(R.id.tvServiceCost);
            if (t != null)
                t.setText(job.get("job"));
            if (d != null)
                d.setText(getString(R.string.default_service_description));
            if (c != null)
                c.setText(job.get("cost"));
            binding.llServicesList.addView(v);
        }
    }

    private void setupRatingBreakdown(ProviderDetail provider) {
        Map<String, Integer> dist = provider.getRatingDistribution();
        if (dist == null)
            dist = new java.util.HashMap<>();

        // Calculate total safely handling potential Long types
        int total = 0;
        for (Object value : dist.values()) {
            if (value instanceof Number) {
                total += ((Number) value).intValue();
            }
        }
        int finalTotal = total == 0 ? 1 : total;

        binding.layoutRatingBreakdown.removeAllViews();
        for (int i = 5; i >= 1; i--) {
            View row = getLayoutInflater().inflate(R.layout.item_rating_row, binding.layoutRatingBreakdown, false);
            TextView star = row.findViewById(R.id.tvStarLabel);
            ProgressBar bar = row.findViewById(R.id.pbRating);
            TextView perc = row.findViewById(R.id.tvPercentLabel);

            star.setText(String.valueOf(i));

            // Safe retrieval handling potential Long/Integer mismatch
            int count = 0;
            Object val = dist.get(String.valueOf(i));
            if (val instanceof Number) {
                count = ((Number) val).intValue();
            }

            int p = (count * 100) / finalTotal;
            bar.setProgress(p);
            perc.setText(p + "%");
            binding.layoutRatingBreakdown.addView(row);
        }
    }

    private void setupReviews(ProviderDetail provider) {
        List<Review> list = new ArrayList<>();
        if (provider.getReviews() != null) {
            for (Object o : provider.getReviews()) {
                if (o instanceof Review)
                    list.add((Review) o);
                else if (o instanceof Map) {
                    Map<?, ?> m = (Map<?, ?>) o;
                    Review r = new Review((String) m.get("reviewerName"), (String) m.get("timeAgo"),
                            ((Number) m.get("rating")).intValue(), (String) m.get("comment"),
                            ((Number) m.get("thumbsUp")).intValue(), ((Number) m.get("thumbsDown")).intValue());
                    list.add(r);
                }
            }
        }
        if (list.isEmpty())
            list.add(new Review("User", "1d", 5, "Great!", 0, 0));
        binding.rvReviews.setLayoutManager(new LinearLayoutManager(this));
        binding.rvReviews.setAdapter(new ReviewAdapter(list));
    }

    private void setupButtons() {
        View.OnClickListener book = v -> {
            animateButtonClick(v);
            Intent intent = new Intent(this, NewBookingActivity.class);
            String safeServiceName = (currentProvider != null && currentProvider.getService() != null
                    && !currentProvider.getService().isEmpty())
                            ? currentProvider.getService()
                            : "General Service";

            intent.putExtra("provider_name", currentProvider != null ? currentProvider.getName() : providerName);
            intent.putExtra("service_category", safeServiceName);
            intent.putExtra("service_name", safeServiceName);
            intent.putExtra("provider_id", currentProvider != null ? currentProvider.getId() : null);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        };
        binding.btnBookNow.setOnClickListener(book);
        binding.btnBookTop.setOnClickListener(book);

        binding.btnMessage.setOnClickListener(v -> {
            animateButtonClick(v);
            startActivity(new Intent(this, MessagesActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
    }

    private void animateButtonClick(View v) {
        v.animate().scaleX(0.94f).scaleY(0.94f).setDuration(80).withEndAction(() -> v.animate().scaleX(1f).scaleY(1f)
                .setDuration(120).setInterpolator(new OvershootInterpolator(1.1f)).start()).start();
    }
}
