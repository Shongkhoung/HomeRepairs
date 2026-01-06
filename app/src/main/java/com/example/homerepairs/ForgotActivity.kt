package com.example.homerepairs

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.homerepairs.databinding.ActivityForgotBinding
import com.google.firebase.auth.FirebaseAuth

class ForgotActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Back button
        binding.btnBack.setOnClickListener { finish() }

        // Reset button
        binding.btnReset.setOnClickListener {
            val email = binding.etEmailOrPhone.text.toString().trim()

            if (email.isEmpty()) {
                binding.etEmailOrPhone.error = "Email is required"
                binding.etEmailOrPhone.requestFocus()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.etEmailOrPhone.error = "Enter a valid email address"
                binding.etEmailOrPhone.requestFocus()
                return@setOnClickListener
            }

            // Directly send password reset email
            sendResetEmail(email)
        }
    }

    private fun sendResetEmail(email: String) {
        toggleLoading(true)
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                toggleLoading(false)
                if (task.isSuccessful) {
                    // Show friendly dialog
                    AlertDialog.Builder(this)
                        .setTitle("Reset Email Sent")
                        .setMessage("A password reset link has been sent to $email. Check your inbox and spam folder.")
                        .setPositiveButton("OK") { dialog, _ ->
                            dialog.dismiss()
                            finish()
                        }
                        .show()
                } else {
                    val errorMsg = task.exception?.message ?: "Failed to send reset email"
                    Toast.makeText(this, "Error: $errorMsg", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun toggleLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnReset.isEnabled = !isLoading
    }
}
