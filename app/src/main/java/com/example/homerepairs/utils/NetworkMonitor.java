package com.example.homerepairs.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;

/**
 * Utility class to monitor network connectivity and show/hide no-connection UI
 * Can be used across all activities
 */
public class NetworkMonitor {
    private static final String TAG = "NetworkMonitor";
    private static final long OFFLINE_DELAY_MS = 2000; // 2 seconds delay before showing offline
    
    private Context context;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private FrameLayout llInternetLoading;
    private TextView tvInternetMessage;
    private LottieAnimationView ivNoConnection;
    private Handler offlineDelayHandler;
    private Runnable offlineDelayRunnable;
    private boolean isMonitoring = false;
    
    public NetworkMonitor(Context context) {
        this.context = context;
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.offlineDelayHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * Initialize with UI components
     */
    public void initialize(FrameLayout llInternetLoading, TextView tvInternetMessage, LottieAnimationView ivNoConnection) {
        this.llInternetLoading = llInternetLoading;
        this.tvInternetMessage = tvInternetMessage;
        this.ivNoConnection = ivNoConnection;
        
        // Initialize as hidden
        if (llInternetLoading != null) {
            llInternetLoading.setVisibility(View.GONE);
        }
    }
    
    /**
     * Start monitoring network connectivity
     */
    public void startMonitoring() {
        if (isMonitoring) {
            return;
        }
        
        if (connectivityManager == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Log.w(TAG, "Network monitoring not available on this device");
            return;
        }
        
        // Check initial network status
        checkNetworkStatus();
        
        // Set up network callback
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                Log.d(TAG, "Network available");
                new Handler(Looper.getMainLooper()).post(() -> {
                    cancelOfflineDelay();
                    showInternetLoading(false);
                });
            }
            
            @Override
            public void onLost(Network network) {
                Log.d(TAG, "Network lost");
                new Handler(Looper.getMainLooper()).post(() -> {
                    scheduleOfflineDelay();
                });
            }
            
            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    boolean hasInternet = networkCapabilities != null &&
                            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                    Log.d(TAG, "Network capabilities changed - hasInternet: " + hasInternet);
                    if (hasInternet) {
                        cancelOfflineDelay();
                        showInternetLoading(false);
                    } else {
                        scheduleOfflineDelay();
                    }
                });
            }
        };
        
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                .build();
        
        try {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
            isMonitoring = true;
            Log.d(TAG, "Network monitoring started");
        } catch (Exception e) {
            Log.e(TAG, "Error registering network callback", e);
        }
    }
    
    /**
     * Stop monitoring network connectivity
     */
    public void stopMonitoring() {
        if (!isMonitoring) {
            return;
        }
        
        cancelOfflineDelay();
        
        if (connectivityManager != null && networkCallback != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
                isMonitoring = false;
                Log.d(TAG, "Network monitoring stopped");
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering network callback", e);
            }
        }
    }
    
    /**
     * Check current network status
     */
    public void checkNetworkStatus() {
        boolean hasNetwork = NetworkUtils.isNetworkAvailable(context);
        Log.d(TAG, "Network available: " + hasNetwork);
        if (!hasNetwork) {
            scheduleOfflineDelay();
        } else {
            showInternetLoading(false);
        }
    }
    
    /**
     * Show or hide internet loading overlay
     */
    private void showInternetLoading(boolean show) {
        if (llInternetLoading == null) {
            return;
        }
        
        if (show) {
            // Show with animation
            llInternetLoading.setVisibility(View.VISIBLE);
            llInternetLoading.setAlpha(0f);
            
            llInternetLoading.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
            
            // Animate content
            if (tvInternetMessage != null && ivNoConnection != null) {
                tvInternetMessage.setAlpha(0f);
                ivNoConnection.setAlpha(0f);
                ivNoConnection.setScaleX(0.8f);
                ivNoConnection.setScaleY(0.8f);
                
                // Start Lottie animation
                if (ivNoConnection != null) {
                    ivNoConnection.setAnimation(com.example.homerepairs.R.raw.no_connection);
                    ivNoConnection.playAnimation();
                }
                
                // Animate text
                tvInternetMessage.animate()
                        .alpha(1f)
                        .setDuration(400)
                        .setStartDelay(300)
                        .setInterpolator(new android.view.animation.DecelerateInterpolator())
                        .start();
                
                // Animate Lottie animation
                ivNoConnection.animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(400)
                        .setStartDelay(400)
                        .setInterpolator(new android.view.animation.DecelerateInterpolator())
                        .start();
            }
        } else {
            // Hide with animation
            llInternetLoading.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .setInterpolator(new android.view.animation.AccelerateInterpolator())
                    .withEndAction(() -> {
                        if (llInternetLoading != null) {
                            llInternetLoading.setVisibility(View.GONE);
                        }
                    })
                    .start();
        }
    }
    
    /**
     * Schedule offline delay before showing no-connection UI
     */
    private void scheduleOfflineDelay() {
        cancelOfflineDelay();
        
        offlineDelayRunnable = () -> {
            // Double-check network status before showing
            if (!NetworkUtils.isNetworkAvailable(context)) {
                showInternetLoading(true);
            }
        };
        
        offlineDelayHandler.postDelayed(offlineDelayRunnable, OFFLINE_DELAY_MS);
    }
    
    /**
     * Cancel offline delay
     */
    private void cancelOfflineDelay() {
        if (offlineDelayHandler != null && offlineDelayRunnable != null) {
            offlineDelayHandler.removeCallbacks(offlineDelayRunnable);
            offlineDelayRunnable = null;
        }
    }
    
    /**
     * Clean up resources
     */
    public void cleanup() {
        stopMonitoring();
        cancelOfflineDelay();
        llInternetLoading = null;
        tvInternetMessage = null;
        ivNoConnection = null;
    }
}

