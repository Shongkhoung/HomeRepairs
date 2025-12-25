package com.example.homerepairs

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.homerepairs.databinding.ActivityPrivacySecurityBinding
import com.google.android.material.snackbar.Snackbar

class PrivacyAndSecurityActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPrivacySecurityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrivacySecurityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnUnderstand.setOnClickListener {

            // Optional message
            Snackbar.make(
                binding.root,
                "Thank you! Your privacy is safe.",
                Snackbar.LENGTH_SHORT
            ).show()

            // Close this screen and go back
            finish()

            // Rotate-back animation

        }
    }
}
