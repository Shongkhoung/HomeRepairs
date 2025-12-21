package com.example.homerepairs

import android.content.Intent
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
            Snackbar.make(
                binding.root,
                "Thank you! Your privacy is safe.",
                Snackbar.LENGTH_SHORT
            ).show()

            binding.root.postDelayed({
                // Navigate to HomeActivity with fade animation
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }, 1500) // 1.5 seconds delay
        }
    }
}
