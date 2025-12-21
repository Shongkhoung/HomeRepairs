package com.example.homerepairs

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.homerepairs.databinding.ActivitySigninBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySigninBinding
    private lateinit var auth: FirebaseAuth
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySigninBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupUI()
        checkCurrentUser()
    }

    /** Set up UI listeners */
    private fun setupUI() {
        // Skip button — navigate to Home and finish sign-in screen
        binding.btnSkip.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        // Forgot password — navigate to ForgotActivity (don't finish so user can come back)
        binding.forgotPW.setOnClickListener {
            startActivity(Intent(this, ForgotActivity::class.java))
        }

        // Remember checkbox
        binding.rememberCheck.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(this, if (isChecked) "Checked!" else "Unchecked!", Toast.LENGTH_SHORT).show()
        }

        // Navigate to SignUpActivity
        binding.signup.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
        }

        // Password eye toggle: detect taps on the drawable end (eye icon)
        binding.password.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawables = binding.password.compoundDrawablesRelative
                // index 2 = drawableEnd when using compoundDrawablesRelative
                val drawableEnd = drawables.getOrNull(2)
                if (drawableEnd != null) {
                    // compute touch bounds (x is relative to view)
                    val touchX = event.x.toInt()
                    val viewWidth = binding.password.width
                    val paddingEnd = binding.password.paddingEnd

                    // drawable width: prefer bounds, fallback to intrinsicWidth
                    val drawableWidth = drawableEnd.bounds.width().takeIf { it > 0 }
                        ?: drawableEnd.intrinsicWidth

                    val drawableLeftEdge = viewWidth - paddingEnd - drawableWidth

                    if (touchX >= drawableLeftEdge) {
                        togglePasswordVisibility()
                        // consume the event so keyboard doesn't also react
                        v.performClick()
                        return@setOnTouchListener true
                    }
                }
            }
            // don't consume other touch events
            false
        }

        // Sign in button
        binding.signinBtn.setOnClickListener { signInUser() }
    }

    /** Toggle password visibility using proper TransformationMethod */
    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible

        if (isPasswordVisible) {
            // show text
            binding.password.transformationMethod = HideReturnsTransformationMethod.getInstance()
        } else {
            // hide text
            binding.password.transformationMethod = PasswordTransformationMethod.getInstance()
        }

        // Preserve cursor at end
        binding.password.setSelection(binding.password.text?.length ?: 0)
    }

    /** Sign in using Firebase Auth */
    private fun signInUser() {
        val email = binding.email.text.toString().trim()
        val password = binding.password.text.toString().trim()

        if (email.isEmpty()) {
            binding.email.error = "Please enter email"
            binding.email.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.email.error = "Invalid email format"
            binding.email.requestFocus()
            return
        }

        if (password.isEmpty()) {
            binding.password.error = "Please enter password"
            binding.password.requestFocus()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.signinBtn.isEnabled = false

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                binding.progressBar.visibility = View.GONE
                binding.signinBtn.isEnabled = true

                if (task.isSuccessful) {
                    navigateToHome(auth.currentUser)
                } else {
                    Toast.makeText(
                        this,
                        "Authentication failed: ${task.exception?.localizedMessage ?: "Unknown error"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    /** Check if a user is already signed in */
    private fun checkCurrentUser() {
        auth.currentUser?.let { navigateToHome(it) }
    }

    /** Navigate to HomeActivity */
    private fun navigateToHome(user: FirebaseUser?) {
        user?.let {
            Toast.makeText(this, "Welcome ${it.email}", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, PrivacyAndSecurityActivity::class.java))
            finish()
        }
    }
}
