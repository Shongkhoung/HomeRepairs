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
    }

    private fun setupUI() {

        // Skip → go to MainActivity directly
        binding.btnSkip.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Go to Sign Up screen
        binding.signup.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Forgot password
        binding.forgotPW.setOnClickListener {
            startActivity(Intent(this, ForgotActivity::class.java))
        }

        // Password visibility toggle
        binding.password.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = binding.password.compoundDrawablesRelative[2]
                drawableEnd?.let {
                    val touchX = event.x
                    val drawableWidth = it.intrinsicWidth
                    val rightEdge = binding.password.width - binding.password.paddingEnd

                    if (touchX >= rightEdge - drawableWidth) {
                        togglePasswordVisibility()
                        v.performClick()
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }

        // Sign in button
        binding.signinBtn.setOnClickListener {
            signInUser()
        }
    }

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        binding.password.transformationMethod =
            if (isPasswordVisible) HideReturnsTransformationMethod.getInstance()
            else PasswordTransformationMethod.getInstance()
        binding.password.setSelection(binding.password.text?.length ?: 0)
    }

    private fun signInUser() {
        val email = binding.email.text.toString().trim()
        val password = binding.password.text.toString().trim()

        if (email.isEmpty()) {
            binding.email.error = "Enter email"
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.email.error = "Invalid email"
            return
        }

        if (password.isEmpty()) {
            binding.password.error = "Enter password"
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.signinBtn.isEnabled = false

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                binding.signinBtn.isEnabled = true
                navigateToHome(auth.currentUser)
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                binding.signinBtn.isEnabled = true
                Toast.makeText(this, it.localizedMessage, Toast.LENGTH_LONG).show()
            }
    }

    private fun navigateToHome(user: FirebaseUser?) {
        if (user == null) return
        Toast.makeText(this, "Welcome ${user.email}", Toast.LENGTH_SHORT).show()
        startMainActivity()
    }

    private fun startMainActivity(skipLogin: Boolean = false) {
        val intent = Intent(this, MainActivity::class.java)
        if (skipLogin) {
            intent.putExtra("skip_login", true)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
