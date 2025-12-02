package com.example.homerepairs

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.homerepairs.databinding.ActivityHomeBinding
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Back button
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Sign out button
        binding.signOut.setOnClickListener {
            showSignOutConfirmation()
        }

        // Get saved info from SharedPreferences
        val prefs = getSharedPreferences("PROPERTY_PREFS", MODE_PRIVATE)
        val propertyType = prefs.getString("property_type", "No property type")
        val address = prefs.getString("address", "No address")
        val unit = prefs.getString("unit", "No unit")
        val locationLink = prefs.getString("location_link", "No location selected")

        // Display info
        binding.tvPropertyType.text = propertyType
        binding.tvAddress.text = address
        binding.tvUnit.text = unit
        binding.tvLocationLink.text = locationLink

        // Make location link clickable
        binding.tvLocationLink.setOnClickListener {
            if (!locationLink.isNullOrEmpty() && locationLink != "No location selected") {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(locationLink))
                startActivity(intent)
            } else {
                Toast.makeText(this, "No location selected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showSignOutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Sign Out")
            .setMessage("Are you sure you want to sign out?")
            .setPositiveButton("Sign Out") { _, _ -> performSignOut() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performSignOut() {
        try {
            auth.signOut()
            Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Could not sign out. Try again.", Toast.LENGTH_LONG).show()
        }
        startActivity(Intent(this, SignInActivity::class.java))
        finish()
    }
}
