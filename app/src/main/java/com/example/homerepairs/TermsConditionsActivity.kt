package com.example.homerepairs

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.homerepairs.databinding.ActivityTermsConditionsBinding

class TermsConditionsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTermsConditionsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTermsConditionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Back button
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Accept button: go back to previous screen
        binding.btnAccept.setOnClickListener {
            // Optionally show a Toast
            // Toast.makeText(this, "You accepted the Terms & Conditions", Toast.LENGTH_SHORT).show()

            // Finish this activity to return to the previous screen
            finish()
        }
    }
}
