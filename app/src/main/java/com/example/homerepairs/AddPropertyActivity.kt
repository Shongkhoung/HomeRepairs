package com.example.homerepairs

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.homerepairs.databinding.ActivityPropertyBinding
import com.google.firebase.auth.FirebaseAuth

class AddPropertyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPropertyBinding
    private lateinit var auth: FirebaseAuth
    private var selectedPropertyType: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPropertyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            navigateToSignIn()
            return
        }

        // Check if user already selected property
        val prefs = getSharedPreferences("PROPERTY_PREFS", MODE_PRIVATE)
        val savedProperty = prefs.getString("property_type", null)
        if (savedProperty != null) {
            // User already selected a property, go to HomeActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // Property type click listeners
        setPropertyTypeClick(binding.house, "House")
        setPropertyTypeClick(binding.apartment, "Apartment")
        setPropertyTypeClick(binding.condo, "Condo")
        setPropertyTypeClick(binding.commercial, "Commercial")

        // Skip button
        binding.BtNskip.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        // Continue button
        binding.Btncontinue.setOnClickListener {
            if (selectedPropertyType == null) {
                Toast.makeText(this, "Please select a property type", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Save to SharedPreferences
            prefs.edit()
                .putString("property_type", selectedPropertyType)
                .apply()

            // Navigate to HomeActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun setPropertyTypeClick(view: TextView, type: String) {
        view.setOnClickListener {
            selectedPropertyType = type
            // Highlight selected view
            resetPropertyTypeBackgrounds()
            view.setBackgroundResource(R.drawable.rounded_corner)
        }
    }

    private fun resetPropertyTypeBackgrounds() {
        binding.house.setBackgroundResource(R.drawable.rounded_edittext)
        binding.apartment.setBackgroundResource(R.drawable.rounded_edittext)
        binding.condo.setBackgroundResource(R.drawable.rounded_edittext)
        binding.commercial.setBackgroundResource(R.drawable.rounded_edittext)
    }

    private fun navigateToSignIn() {
        val intent = Intent(this, SignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
