package com.example.homerepairs

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.homerepairs.databinding.ActivitySavelocationBinding

class SavedLocationActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySavelocationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySavelocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get saved link
        val prefs = getSharedPreferences("PROPERTY_PREFS", MODE_PRIVATE)
        val locationLink = prefs.getString("location_link", null)

        binding.btnOpenInMaps.setOnClickListener {
            if (!locationLink.isNullOrEmpty()) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(locationLink))
                intent.setPackage("com.google.android.apps.maps")
                startActivity(intent)
            }
        }
    }
}
