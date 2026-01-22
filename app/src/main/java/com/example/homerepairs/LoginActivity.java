package com.example.homerepairs;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.example.homerepairs.databinding.ActivityLoginBinding;
import com.example.homerepairs.utils.AuthHelper;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AdditionalUserInfo;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends BaseActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    private boolean isSignUpMode = true;
    private static final String EXTRA_MODE = "mode";
    private static final String MODE_SIGN_IN = "sign_in";
    private static final String MODE_SIGN_UP = "sign_up";
    private static final int RC_GOOGLE_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String mode = getIntent().getStringExtra(EXTRA_MODE);
        if (MODE_SIGN_IN.equals(mode)) {
            isSignUpMode = false;
        } else if (MODE_SIGN_UP.equals(mode)) {
            isSignUpMode = true;
        } else {
            isSignUpMode = false;
        }

        auth = FirebaseAuth.getInstance();
        initializeGoogleSignIn();
        setupTermsText();
        setupClickListeners();
        updateUIForMode();
        checkAuthState();
    }

    private void initializeGoogleSignIn() {
        try {
            String webClientId = getString(R.string.default_web_client_id);
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(webClientId)
                    .requestEmail()
                    .build();
            googleSignInClient = GoogleSignIn.getClient(this, gso);
        } catch (android.content.res.Resources.NotFoundException e) {
            android.util.Log.w("LoginActivity", "Google Sign-In Web Client ID not configured.");
        }
    }

    private void setupClickListeners() {
        binding.btnAction.setOnClickListener(v -> {
            if (isSignUpMode)
                signUp();
            else
                signIn();
        });

        binding.tvSwitchMode.setOnClickListener(v -> {
            isSignUpMode = !isSignUpMode;
            updateUIForMode();
        });

        binding.tvForgotPassword.setOnClickListener(v -> {
            if (!isSignUpMode)
                navigateToForgotPassword();
        });

        binding.btnBack.setOnClickListener(v -> {
            if (isSignUpMode) {
                isSignUpMode = false;
                updateUIForMode();
            } else {
                moveTaskToBack(true);
            }
        });

        binding.btnGoogle.setOnClickListener(v -> {
            if (googleSignInClient != null)
                signInWithGoogle();
            else
                Toast.makeText(this, "Google Sign-In not configured.", Toast.LENGTH_LONG).show();
        });

        binding.btnFacebook.setOnClickListener(v -> Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show());
    }

    private void setupTermsText() {
        String fullText = "I agree to the Terms of Service and Privacy Policy";
        SpannableString spannable = new SpannableString(fullText);

        ClickableSpan termsSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                Toast.makeText(LoginActivity.this, "Terms of Service", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
            }
        };

        int termsStart = fullText.indexOf("Terms of Service");
        int termsEnd = termsStart + "Terms of Service".length();
        spannable.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.link_blue)), termsStart,
                termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(termsSpan, termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        ClickableSpan privacySpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                Toast.makeText(LoginActivity.this, "Privacy Policy", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
            }
        };

        int privacyStart = fullText.indexOf("Privacy Policy");
        int privacyEnd = privacyStart + "Privacy Policy".length();
        spannable.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.link_blue)), privacyStart,
                privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(privacySpan, privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        binding.tvTerms.setText(spannable);
        binding.tvTerms.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void updateUIForMode() {
        int signUpVisibility = isSignUpMode ? View.VISIBLE : View.GONE;
        int signInVisibility = isSignUpMode ? View.GONE : View.VISIBLE;

        binding.tvTitle.setText(isSignUpMode ? "Create Account" : "Welcome Back");
        binding.tvSubtitle.setText(isSignUpMode ? "Join us and get started" : "Sign in to continue");
        binding.btnAction.setText(isSignUpMode ? "Create Account" : "Sign In");
        binding.tvSwitchMode.setText(isSignUpMode ? "Sign In" : "Sign Up");

        binding.llName.setVisibility(signUpVisibility);
        binding.llPhone.setVisibility(signUpVisibility);
        binding.llConfirmPassword.setVisibility(signUpVisibility);
        binding.llTerms.setVisibility(signUpVisibility);
        binding.llDivider.setVisibility(signUpVisibility);
        binding.llSocialLogin.setVisibility(signUpVisibility);
        binding.tvForgotPassword.setVisibility(signInVisibility);

        binding.etEmail.setText("");
        binding.etPassword.setText("");
        binding.etName.setText("");
        binding.etPhone.setText("");
        binding.etConfirmPassword.setText("");
        binding.cbTerms.setChecked(false);
        binding.tvError.setVisibility(View.GONE);
    }

    private void signIn() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if (!validateInput(email, password, false))
            return;

        showLoading(true);
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            if (user.getDisplayName() != null)
                                AuthHelper.saveUserName(this, user.getDisplayName());
                            navigateToMain();
                        }
                    } else {
                        showError(getErrorMessage(
                                task.getException() != null ? task.getException().getMessage() : "Sign in failed"));
                    }
                });
    }

    private void signUp() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String name = binding.etName.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();

        if (!validateInput(email, password, true))
            return;
        if (TextUtils.isEmpty(name)) {
            showError("Please enter your full name");
            binding.etName.requestFocus();
            return;
        }
        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            binding.etConfirmPassword.requestFocus();
            return;
        }
        if (!binding.cbTerms.isChecked()) {
            showError("Please agree to the Terms of Service");
            return;
        }

        showLoading(true);
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();
                            user.updateProfile(profileUpdates).addOnCompleteListener(updateTask -> {
                                showLoading(false);
                                AuthHelper.saveUserName(this, name);
                                createUserProfile(user, name, phone);
                                navigateToMain();
                            });
                        }
                    } else {
                        showLoading(false);
                        showError(getErrorMessage(
                                task.getException() != null ? task.getException().getMessage() : "Sign up failed"));
                    }
                });
    }

    private void navigateToForgotPassword() {
        String email = binding.etEmail.getText().toString().trim();
        Intent intent = new Intent(this, ForgotPasswordActivity.class);
        if (!TextUtils.isEmpty(email))
            intent.putExtra("email", email);
        startActivity(intent);
    }

    private boolean validateInput(String email, String password, boolean isSignUp) {
        if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Please enter a valid email");
            binding.etEmail.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(password) || (isSignUp && password.length() < 6)) {
            showError(isSignUp ? "Password at least 6 characters" : "Please enter password");
            binding.etPassword.requestFocus();
            return false;
        }
        return true;
    }

    private String getErrorMessage(String error) {
        if (error == null)
            return "An error occurred";
        String lowerError = error.toLowerCase();
        if (lowerError.contains("badly formatted") || lowerError.contains("invalid email"))
            return "Invalid email address";
        if (lowerError.contains("too weak"))
            return "Password is too weak";
        if (lowerError.contains("already in use"))
            return "Email already registered";
        if (lowerError.contains("no user record"))
            return "Account not found";
        if (lowerError.contains("wrong password"))
            return "Incorrect password";
        if (lowerError.contains("network"))
            return "Network error";
        return error;
    }

    private void showError(String message) {
        binding.tvError.setText(message);
        binding.tvError.setVisibility(View.VISIBLE);
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.btnAction.setEnabled(!show);
        binding.btnAction.setAlpha(show ? 0.6f : 1.0f);
    }

    private void signInWithGoogle() {
        if (googleSignInClient == null)
            return;
        showLoading(true);
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
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
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                showLoading(false);
                showError("Google sign in failed: " + e.getStatusCode());
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            showLoading(false);
            if (task.isSuccessful()) {
                FirebaseUser user = auth.getCurrentUser();
                if (user != null) {
                    if (user.getDisplayName() != null)
                        AuthHelper.saveUserName(this, user.getDisplayName());
                    AdditionalUserInfo info = task.getResult().getAdditionalUserInfo();
                    if (info != null && info.isNewUser())
                        createUserProfile(user, null, null);
                    navigateToMain();
                }
            } else {
                showError(getErrorMessage(
                        task.getException() != null ? task.getException().getMessage() : "Authentication failed"));
            }
        });
    }

    private void createUserProfile(FirebaseUser user, String name, String phone) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> profile = new HashMap<>();
        profile.put("userId", user.getUid());
        profile.put("email", user.getEmail());
        profile.put("name", name != null ? name : (user.getDisplayName() != null ? user.getDisplayName() : ""));
        profile.put("photoUrl", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");
        profile.put("phone", phone != null ? phone : "");
        profile.put("createdAt", com.google.firebase.Timestamp.now());
        db.collection("users").document(user.getUid()).set(profile);
    }

    private void checkAuthState() {
        if (auth.getCurrentUser() != null)
            navigateToMain();
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    @android.annotation.SuppressLint("MissingSuperCall")
    public void onBackPressed() {
        if (!AuthHelper.isAuthenticated())
            moveTaskToBack(true);
        else
            super.onBackPressed();
    }
}
