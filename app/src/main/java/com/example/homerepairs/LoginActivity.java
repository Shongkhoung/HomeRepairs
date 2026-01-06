package com.example.homerepairs;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.AdditionalUserInfo;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

import com.example.homerepairs.utils.AuthHelper;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private TextInputEditText etName;
    private TextInputEditText etPhone;
    private TextInputEditText etConfirmPassword;
    private TextInputLayout tilName;
    private TextInputLayout tilConfirmPassword;
    private MaterialButton btnAction;
    private MaterialButton btnGoogle;
    private MaterialButton btnFacebook;
    private TextView tvTitle;
    private TextView tvSubtitle;
    private TextView tvSwitchMode;
    private TextView tvForgotPassword;
    private TextView tvError;
    private TextView tvTerms;
    private ProgressBar progressBar;
    private CheckBox cbTerms;
    private ImageButton btnBack;
    private LinearLayout llName;
    private LinearLayout llPhone;
    private LinearLayout llConfirmPassword;
    private LinearLayout llTerms;
    private LinearLayout llDivider;
    private LinearLayout llSocialLogin;

    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    private boolean isSignUpMode = true; // Default to Sign Up mode
    private static final String EXTRA_MODE = "mode";
    private static final String MODE_SIGN_IN = "sign_in";
    private static final String MODE_SIGN_UP = "sign_up";
    private static final int RC_GOOGLE_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Set status bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getWindow().getDecorView().setSystemUiVisibility(
                        getWindow().getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }

        // Check intent for mode preference
        // If launched as launcher activity (first-time app launch), show sign-in mode
        // (Welcome Back)
        // If redirected from other activities (logout, auth failure), respect the mode
        // extra
        String mode = getIntent().getStringExtra(EXTRA_MODE);
        if (MODE_SIGN_IN.equals(mode)) {
            isSignUpMode = false; // Show sign in mode (e.g., after logout)
        } else if (MODE_SIGN_UP.equals(mode)) {
            isSignUpMode = true; // Show sign up mode (explicit request)
        } else {
            // No mode extra = first-time app launch, default to sign-in mode (Welcome Back)
            isSignUpMode = false;
        }

        initializeViews();
        setupClickListeners();

        // Check if user is already logged in - redirect to MainActivity if
        // authenticated
        checkAuthState();
    }

    private void initializeViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        tilName = findViewById(R.id.tilName);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        btnAction = findViewById(R.id.btnAction);
        btnGoogle = findViewById(R.id.btnGoogle);
        btnFacebook = findViewById(R.id.btnFacebook);
        tvTitle = findViewById(R.id.tvTitle);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvSwitchMode = findViewById(R.id.tvSwitchMode);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvError = findViewById(R.id.tvError);
        tvTerms = findViewById(R.id.tvTerms);
        progressBar = findViewById(R.id.progressBar);
        cbTerms = findViewById(R.id.cbTerms);
        btnBack = findViewById(R.id.btnBack);
        llName = findViewById(R.id.llName);
        llPhone = findViewById(R.id.llPhone);
        llConfirmPassword = findViewById(R.id.llConfirmPassword);
        llTerms = findViewById(R.id.llTerms);
        llDivider = findViewById(R.id.llDivider);
        llSocialLogin = findViewById(R.id.llSocialLogin);

        auth = FirebaseAuth.getInstance();

        // Configure Google Sign-In
        // Note: You need to get the Web Client ID from Firebase Console
        // Firebase Console > Project Settings > Your Apps > Web App > Web Client ID
        // Then add it to res/values/strings.xml as: <string
        // name="default_web_client_id">YOUR_WEB_CLIENT_ID</string>
        try {
            String webClientId = getString(R.string.default_web_client_id);
            android.util.Log.d("LoginActivity", "Using Web Client ID: " + webClientId);
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(webClientId)
                    .requestEmail()
                    .build();
            googleSignInClient = GoogleSignIn.getClient(this, gso);
            android.util.Log.d("LoginActivity", "Google Sign-In client initialized successfully");
        } catch (android.content.res.Resources.NotFoundException e) {
            // Web Client ID not configured yet
            android.util.Log.w("LoginActivity", "Google Sign-In Web Client ID not configured. " +
                    "Please add it to res/values/strings.xml. See Firebase Console for Web Client ID.");
            googleSignInClient = null;
        }

        // Set up terms text with clickable links
        setupTermsText();

        // Initialize to Sign Up mode (default)
        updateUIForMode();
    }

    private void setupClickListeners() {
        btnAction.setOnClickListener(v -> {
            if (isSignUpMode) {
                signUp();
            } else {
                signIn();
            }
        });

        tvSwitchMode.setOnClickListener(v -> toggleMode());

        tvForgotPassword.setOnClickListener(v -> {
            if (!isSignUpMode) {
                navigateToForgotPassword();
            }
        });

        btnBack.setOnClickListener(v -> {
            if (isSignUpMode) {
                // Switch to Sign In mode
                toggleMode();
            } else {
                // Move to background
                moveTaskToBack(true);
            }
        });

        btnGoogle.setOnClickListener(v -> {
            if (googleSignInClient != null) {
                signInWithGoogle();
            } else {
                Toast.makeText(this, "Google Sign-In not configured. Please set up Web Client ID in Firebase Console.",
                        Toast.LENGTH_LONG).show();
            }
        });

        btnFacebook.setOnClickListener(v -> {
            Toast.makeText(this, "Facebook sign in coming soon", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupTermsText() {
        String fullText = "I agree to the Terms of Service and Privacy Policy";
        SpannableString spannable = new SpannableString(fullText);

        // Make "Terms of Service" clickable
        int termsStart = fullText.indexOf("Terms of Service");
        int termsEnd = termsStart + "Terms of Service".length();
        spannable.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.link_blue)),
                termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                Toast.makeText(LoginActivity.this, "Terms of Service", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
            }
        }, termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Make "Privacy Policy" clickable
        int privacyStart = fullText.indexOf("Privacy Policy");
        int privacyEnd = privacyStart + "Privacy Policy".length();
        spannable.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.link_blue)),
                privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                Toast.makeText(LoginActivity.this, "Privacy Policy", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
            }
        }, privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        if (tvTerms != null) {
            tvTerms.setText(spannable);
            tvTerms.setMovementMethod(LinkMovementMethod.getInstance());
        }
    }

    private void toggleMode() {
        isSignUpMode = !isSignUpMode;
        updateUIForMode();
    }

    private void updateUIForMode() {
        if (isSignUpMode) {
            // Switch to Sign Up mode
            tvTitle.setText("Create Account");
            tvSubtitle.setText("Join us and get started");
            btnAction.setText("Create Account");
            tvSwitchMode.setText("Sign In");

            // Show Sign Up fields
            if (llName != null)
                llName.setVisibility(View.VISIBLE);
            if (llPhone != null)
                llPhone.setVisibility(View.VISIBLE);
            if (llConfirmPassword != null)
                llConfirmPassword.setVisibility(View.VISIBLE);
            if (llTerms != null)
                llTerms.setVisibility(View.VISIBLE);
            if (llDivider != null)
                llDivider.setVisibility(View.VISIBLE);
            if (llSocialLogin != null)
                llSocialLogin.setVisibility(View.VISIBLE);

            // Hide Sign In fields
            if (tvForgotPassword != null)
                tvForgotPassword.setVisibility(View.GONE);
        } else {
            // Switch to Sign In mode
            tvTitle.setText("Welcome Back");
            tvSubtitle.setText("Sign in to continue");
            btnAction.setText("Sign In");
            tvSwitchMode.setText("Sign Up");

            // Hide Sign Up fields
            if (llName != null)
                llName.setVisibility(View.GONE);
            if (llPhone != null)
                llPhone.setVisibility(View.GONE);
            if (llConfirmPassword != null)
                llConfirmPassword.setVisibility(View.GONE);
            if (llTerms != null)
                llTerms.setVisibility(View.GONE);
            if (llDivider != null)
                llDivider.setVisibility(View.GONE);
            if (llSocialLogin != null)
                llSocialLogin.setVisibility(View.GONE);

            // Show Sign In fields
            if (tvForgotPassword != null)
                tvForgotPassword.setVisibility(View.VISIBLE);
        }

        // Clear fields
        if (etEmail != null)
            etEmail.setText("");
        if (etPassword != null)
            etPassword.setText("");
        if (etName != null)
            etName.setText("");
        if (etPhone != null)
            etPhone.setText("");
        if (etConfirmPassword != null)
            etConfirmPassword.setText("");
        if (cbTerms != null)
            cbTerms.setChecked(false);
        hideError();
    }

    private void signIn() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!validateInput(email, password, false)) {
            return;
        }

        showLoading(true);
        hideError();

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            // Save user name if available
                            if (user.getDisplayName() != null) {
                                AuthHelper.saveUserName(this, user.getDisplayName());
                            }
                            navigateToMain();
                        }
                    } else {
                        String errorMessage = "Sign in failed";
                        if (task.getException() != null) {
                            errorMessage = getErrorMessage(task.getException().getMessage());
                        }
                        showError(errorMessage);
                    }
                });
    }

    private void signUp() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String name = etName.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String phone = etPhone != null ? etPhone.getText().toString().trim() : "";

        if (!validateInput(email, password, true)) {
            return;
        }

        if (TextUtils.isEmpty(name)) {
            showError("Please enter your full name");
            etName.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        if (cbTerms == null || !cbTerms.isChecked()) {
            showError("Please agree to the Terms of Service and Privacy Policy");
            return;
        }

        showLoading(true);
        hideError();

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            // Update user profile with name
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();

                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(updateTask -> {
                                        showLoading(false);
                                        // Save user name locally
                                        AuthHelper.saveUserName(this, name);

                                        // Create user profile in Firestore
                                        createUserProfile(user, name, phone);

                                        navigateToMain();
                                    });
                        }
                    } else {
                        showLoading(false);
                        String errorMessage = "Sign up failed";
                        if (task.getException() != null) {
                            errorMessage = getErrorMessage(task.getException().getMessage());
                        }
                        showError(errorMessage);
                    }
                });
    }

    private void navigateToForgotPassword() {
        String email = etEmail.getText().toString().trim();
        Intent intent = new Intent(this, ForgotPasswordActivity.class);
        if (!TextUtils.isEmpty(email)) {
            intent.putExtra("email", email);
        }
        startActivity(intent);
    }

    private boolean validateInput(String email, String password, boolean isSignUp) {
        if (TextUtils.isEmpty(email)) {
            showError("Please enter your email");
            etEmail.requestFocus();
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Please enter a valid email address");
            etEmail.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            showError("Please enter your password");
            etPassword.requestFocus();
            return false;
        }

        if (isSignUp && password.length() < 6) {
            showError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return false;
        }

        return true;
    }

    private String getErrorMessage(String error) {
        if (error == null) {
            return "An error occurred";
        }

        if (error.contains("email address is badly formatted")) {
            return "Invalid email address";
        } else if (error.contains("password is too weak")) {
            return "Password is too weak. Please use a stronger password";
        } else if (error.contains("email address is already in use")) {
            return "This email is already registered. Please sign in instead";
        } else if (error.contains("no user record")) {
            return "No account found with this email. Please sign up first";
        } else if (error.contains("wrong password")) {
            return "Incorrect password. Please try again";
        } else if (error.contains("network")) {
            return "Network error. Please check your internet connection";
        } else {
            return error;
        }
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        tvError.setVisibility(View.GONE);
    }

    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            btnAction.setEnabled(false);
            btnAction.setAlpha(0.6f);
        } else {
            progressBar.setVisibility(View.GONE);
            btnAction.setEnabled(true);
            btnAction.setAlpha(1.0f);
        }
    }

    private void signInWithGoogle() {
        if (googleSignInClient == null) {
            showError("Google Sign-In is not configured. Please check your configuration.");
            return;
        }

        showLoading(true);
        hideError();

        // Sign out from any previously selected Google account to force account picker
        // This ensures users can choose a different account each time they click "Sign
        // in with Google"
        // Without this, Google Sign-In would automatically use the last selected
        // account
        googleSignInClient.signOut()
                .addOnCompleteListener(this, signOutTask -> {
                    // After signing out from Google Sign-In client, show the account picker
                    // This allows users to select any Google account, not just the last one used
                    Intent signInIntent = googleSignInClient.getSignInIntent();
                    startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN);
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign-In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                showLoading(false);
                String errorMessage = "Google sign in failed";
                if (e.getStatusCode() == 12500) {
                    errorMessage = "Google Play Services update required";
                } else if (e.getStatusCode() == 10) {
                    errorMessage = "Configuration error (Code 10). Please:\n" +
                            "1. Download fresh google-services.json from Firebase\n" +
                            "2. Verify SHA-1 is added: 51:BA:2E:09:77:48:0A:0B:7A:C0:06:1E:DF:BA:2D:87:EA:1C:DB:67\n" +
                            "3. Ensure Google Sign-In is enabled in Firebase\n" +
                            "4. Make sure Web App exists in Firebase\n" +
                            "5. Wait 5-10 minutes after changes\n" +
                            "6. Uninstall and reinstall the app";
                } else if (e.getStatusCode() == 7) {
                    errorMessage = "Network error. Please check your internet connection";
                }
                showError(errorMessage);
                android.util.Log.e("LoginActivity", "Google sign in failed: " + e.getStatusCode() +
                        " | Web Client ID: " + getString(R.string.default_web_client_id), e);
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        // Sign in success
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            // Save user name if available
                            if (user.getDisplayName() != null) {
                                AuthHelper.saveUserName(this, user.getDisplayName());
                            }

                            // Check if this is a new user (first time signing in)
                            AdditionalUserInfo additionalUserInfo = task.getResult().getAdditionalUserInfo();
                            if (additionalUserInfo != null && additionalUserInfo.isNewUser()) {
                                // Create user profile in Firestore for new users
                                createUserProfile(user);
                            }

                            navigateToMain();
                        }
                    } else {
                        // Sign in failed
                        String errorMessage = "Authentication failed";
                        if (task.getException() != null) {
                            errorMessage = getErrorMessage(task.getException().getMessage());
                        }
                        showError(errorMessage);
                    }
                });
    }

    /**
     * Create a user profile document in Firestore for new users (Google Sign-In)
     */
    private void createUserProfile(FirebaseUser user) {
        createUserProfile(user, null, null);
    }

    /**
     * Create a user profile document in Firestore for new users
     * 
     * @param user  Firebase user object
     * @param name  User's name (for email/password sign-up, null for Google
     *              Sign-In)
     * @param phone User's phone number (optional)
     */
    private void createUserProfile(FirebaseUser user, String name, String phone) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("userId", user.getUid());
        userProfile.put("email", user.getEmail());

        // Use provided name or user's display name
        String userName = name != null ? name : (user.getDisplayName() != null ? user.getDisplayName() : "");
        userProfile.put("name", userName);

        // Use user's photo URL if available
        userProfile.put("photoUrl", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");

        // Use provided phone or empty string
        userProfile.put("phone", phone != null ? phone : "");

        // Location field (can be updated later)
        userProfile.put("location", "");

        userProfile.put("createdAt", com.google.firebase.Timestamp.now());
        userProfile.put("updatedAt", com.google.firebase.Timestamp.now());

        // Create user document in 'users' collection
        db.collection("users").document(user.getUid())
                .set(userProfile)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("LoginActivity", "User profile created successfully for: " + user.getEmail());
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("LoginActivity", "Error creating user profile", e);
                    // Don't show error to user - profile creation is not critical for login
                });
    }

    private void checkAuthState() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            // User is already logged in, navigate to main
            navigateToMain();
        }
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    @android.annotation.SuppressLint("MissingSuperCall")
    public void onBackPressed() {
        // Prevent going back if user is not logged in - force them to login or sign up
        if (!AuthHelper.isAuthenticated()) {
            // Move app to background instead of closing
            moveTaskToBack(true);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check authentication state when activity becomes visible
        // This ensures users are redirected if they somehow bypass the initial check
        checkAuthState();
    }
}
