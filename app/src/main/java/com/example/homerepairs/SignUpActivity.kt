package com.example.homerepairs

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.homerepairs.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth
    private val TAG = "SignUpActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Back button
        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Skip button
        binding.btnSkip.setOnClickListener {
            startActivity(Intent(this, AddPropertyActivity::class.java))
            finish()
        }

        // Navigate to SignIn
        binding.signin.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
        }

        // Sign up button
        binding.signupBtn.setOnClickListener {
            val email = binding.email.text.toString().trim()
            val password = binding.password.text.toString().trim()
            val confirmPassword = binding.confirmPassword.text.toString().trim()

            if (email.isEmpty()) {
                binding.email.error = "Email is required"
                binding.email.requestFocus()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.email.error = "Enter a valid email address"
                binding.email.requestFocus()
                return@setOnClickListener
            }

            if (password.isEmpty() || password.length < 6) {
                binding.password.error = "Password must be at least 6 characters"
                binding.password.requestFocus()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                binding.confirmPassword.error = "Passwords do not match"
                binding.confirmPassword.requestFocus()
                return@setOnClickListener
            }

            // Sign up with email
            signUpWithEmail(email, password)
        }
    }

    private fun signUpWithEmail(email: String, password: String) {
        binding.signupBtn.isEnabled = false
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                binding.signupBtn.isEnabled = true
                if (task.isSuccessful) {
                    Toast.makeText(this, "Signup successful", Toast.LENGTH_SHORT).show()
                    // Navigate to AddPropertyActivity
                    startActivity(Intent(this, AddPropertyActivity::class.java))
                    finish()
                } else {
                    val msg = task.exception?.message ?: "Signup failed"
                    Log.e(TAG, "createUserWithEmailAndPassword failed: $msg", task.exception)
                    Toast.makeText(this, "Signup failed: $msg", Toast.LENGTH_LONG).show()
                }
            }
    }
}
