package com.example.homerepairs

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.homerepairs.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth

    companion object {
        private const val TAG = "SignupActivity"
    }

    public override fun onStart() {
        super.onStart()
        // Check if user is already signed in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            navigateToPropertyScreen()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Back button
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Remember checkbox
        binding.rememberCheck.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(this, if (isChecked) "Checked!" else "Unchecked!", Toast.LENGTH_SHORT).show()
        }

        // Navigate to SignInActivity
        binding.signin.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
        }

        // Sign In with facebook

        binding.SignInFacebook.setOnClickListener(){

        }
        // Sign up button
        binding.signupBtn.setOnClickListener {
            val email = binding.email.text.toString().trim()
            val password = binding.password.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email and password cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Firebase sign up
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign-up success
                        val user = auth.currentUser
                        Toast.makeText(this, "Signup successful", Toast.LENGTH_SHORT).show()
                        updateUI(user)
                        navigateToPropertyScreen()
                    } else {
                        // Sign-up failed
                        Log.w(TAG, "createUserWithEmail:failure", task.exception)
                        Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        updateUI(null)
                    }
                }
        }
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            Toast.makeText(this, "Welcome ${user.email}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToPropertyScreen() {
        val intent = Intent(this, AddPropertyActivity::class.java)
        startActivity(intent)
        finish()
    }
}
