package com.example.homerepairs

import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.homerepairs.databinding.ActivitySigninBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import android.util.Patterns
import android.view.MotionEvent

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
        // Skip button
        binding.btnSkip.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        // Forgot password
        binding.forgotPW.setOnClickListener {
            startActivity(Intent(this, ForgotActivity::class.java))
           finish()
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


        binding.password.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawables = binding.password.compoundDrawablesRelative
                val drawableEnd = drawables.getOrNull(2) // index 2 == end
                if (drawableEnd != null) {
                    val touchX = event.x.toInt()
                    val width = binding.password.width
                    val paddingEnd = binding.password.paddingEnd
                    val drawableWidth = drawableEnd.bounds.width()
                    if (touchX >= (width - paddingEnd - drawableWidth)) {
                        togglePasswordVisibility()

                        // Call performClick on the view, not override
                        v.performClick()
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }





        // Sign in button
        binding.signinBtn.setOnClickListener { signInUser() }
    }

    /** Toggle password visibility using transformationMethod (no keyboard side-effects) */
    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible

        if (isPasswordVisible) {
            binding.password.transformationMethod = null
        } else {
            binding.password.transformationMethod = PasswordTransformationMethod.getInstance()
        }

        // Keep the cursor at the end after changing transformation
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
                        "Authentication failed: ${task.exception?.message}",
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
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
    }
}
