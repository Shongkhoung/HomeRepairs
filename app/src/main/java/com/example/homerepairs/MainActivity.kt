package com.example.homerepairs

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.homerepairs.databinding.ActivityMainBinding
import com.google.android.gms.maps.GoogleMap
import com.google.android.libraries.places.api.Places
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // 🚀 Check if the user is already logged in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // User already signed in → go to PropertyActivity
            val intent = Intent(this, AddPropertyActivity::class.java)
            startActivity(intent)
            finish()
            return
        }



        // 🟢 If NOT signed in → allow navigation
        binding.getStartButton.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        binding.SignIn.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
        }
    }
}
