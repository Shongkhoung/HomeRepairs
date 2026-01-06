package com.example.homerepairs

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.homerepairs.databinding.ActivityGetstartBinding

class GetStartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGetstartBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGetstartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get Started → Sign Up
        binding.getStartButton.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        // Already have account → Sign In
        binding.SignIn.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
        }
    }
}
