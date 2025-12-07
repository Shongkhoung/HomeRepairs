package com.example.homerepairs

import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.homerepairs.databinding.ActivityVerifyOtpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthProvider

class VerifyOtpActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVerifyOtpBinding
    private lateinit var auth: FirebaseAuth
    private var verificationId: String = ""
    private var isPasswordVisible = false

    private val TAG = "VerifyOtpActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyOtpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        verificationId = intent.getStringExtra("verificationId") ?: ""

        if (verificationId.isBlank()) {
            Toast.makeText(this, "Missing verification data. Please request OTP again.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupPasswordToggle()
        setupListeners()
    }

    private fun setupListeners() {
        binding.btnVerify.setOnClickListener {
            val otp = binding.etOtp.text.toString().trim()
            val newPassword = binding.etNewPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            if (!validateInputs(otp, newPassword, confirmPassword)) return@setOnClickListener

            verifyOtpAndResetPassword(otp, newPassword)
        }

        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    /** Toggle password visibility using transformationMethod */
    private fun setupPasswordToggle() {
        // start with password hidden
        binding.etNewPassword.transformationMethod = PasswordTransformationMethod.getInstance()
        binding.etConfirmPassword.transformationMethod = PasswordTransformationMethod.getInstance()
        binding.eyeIcon.setImageResource(R.drawable.ic_eye_hidden)

        binding.eyeIcon.setOnClickListener {
            isPasswordVisible = !isPasswordVisible

            if (isPasswordVisible) {
                // show password
                binding.etNewPassword.transformationMethod = null
                binding.etConfirmPassword.transformationMethod = null
                binding.eyeIcon.setImageResource(R.drawable.ic_eye)
            } else {
                // hide password
                binding.etNewPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                binding.etConfirmPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                binding.eyeIcon.setImageResource(R.drawable.ic_eye_hidden)
            }

            // keep cursor at end for both fields
            binding.etNewPassword.setSelection(binding.etNewPassword.text?.length ?: 0)
            binding.etConfirmPassword.setSelection(binding.etConfirmPassword.text?.length ?: 0)
        }
    }

    /** Validate OTP and password fields */
    private fun validateInputs(otp: String, password: String, confirmPassword: String): Boolean {
        // OTP typical length is 6 (depends on your configuration) - adjust if needed
        if (otp.isEmpty()) {
            binding.etOtp.error = "Enter OTP"
            binding.etOtp.requestFocus()
            return false
        }
        if (otp.length < 4) { // some setups use 4, some 6 — set 6 if you're using 6-digit codes
            binding.etOtp.error = "OTP seems too short"
            binding.etOtp.requestFocus()
            return false
        }

        if (password.length < 6) {
            binding.etNewPassword.error = "Password must be at least 6 characters"
            binding.etNewPassword.requestFocus()
            return false
        }
        if (password != confirmPassword) {
            binding.etConfirmPassword.error = "Passwords do not match"
            binding.etConfirmPassword.requestFocus()
            return false
        }
        return true
    }

    /** Verify OTP and reset password */
    private fun verifyOtpAndResetPassword(otp: String, newPassword: String) {
        setLoading(true)

        try {
            val credential = PhoneAuthProvider.getCredential(verificationId, otp)

            auth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        setLoading(false)
                        val message = task.exception?.message ?: "OTP verification failed"
                        Log.e(TAG, "signInWithCredential failed: $message", task.exception)
                        Toast.makeText(this, "OTP verification failed: $message", Toast.LENGTH_LONG).show()
                        return@addOnCompleteListener
                    }

                    // Signed in successfully with phone credential. Now update password.
                    val user = auth.currentUser
                    if (user == null) {
                        setLoading(false)
                        Toast.makeText(this, "Unable to get user after verification", Toast.LENGTH_LONG).show()
                        return@addOnCompleteListener
                    }

                    user.updatePassword(newPassword)
                        .addOnCompleteListener { updateTask ->
                            setLoading(false)
                            if (updateTask.isSuccessful) {
                                Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_LONG).show()
                                finish()
                            } else {
                                val err = updateTask.exception?.message ?: "Failed to update password"
                                Log.e(TAG, "updatePassword failed: $err", updateTask.exception)
                                Toast.makeText(this, "Error updating password: $err", Toast.LENGTH_LONG).show()
                            }
                        }
                }
        } catch (e: Exception) {
            setLoading(false)
            Log.e(TAG, "Exception while verifying OTP", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnVerify.isEnabled = !isLoading
        binding.etOtp.isEnabled = !isLoading
        binding.etNewPassword.isEnabled = !isLoading
        binding.etConfirmPassword.isEnabled = !isLoading
    }
}
