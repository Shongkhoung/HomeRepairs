package com.example.homerepairs

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.homerepairs.databinding.ActivityHelpCenterBinding
import com.google.firebase.auth.FirebaseAuth

class HelpCenterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHelpCenterBinding

    // Anti-spam: store last click time
    private var lastClickTime: Long = 0

    // Firebase Auth instance
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHelpCenterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Back button
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Email click
        binding.btnMail.setOnClickListener {
            if (isUserSignedIn()) {
                if (canClick()) openEmail()
            } else {
                promptSignIn()
            }
        }

        // Call click
        binding.btnCall.setOnClickListener {
            if (isUserSignedIn()) {
                if (canClick()) openDialer()
            } else {
                promptSignIn()
            }
        }
    }

    // Anti-spam click function (1 second debounce)
    private fun canClick(): Boolean {
        val currentTime = SystemClock.elapsedRealtime()
        if (currentTime - lastClickTime < 1000) {
            Toast.makeText(this, "Please wait before clicking again", Toast.LENGTH_SHORT).show()
            return false
        }
        lastClickTime = currentTime
        return true
    }

    // Check if user is signed in using Firebase Auth
    private fun isUserSignedIn(): Boolean {
        return auth.currentUser != null
    }

    private fun promptSignIn() {
        Toast.makeText(this, "Please sign in first to contact support", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, SignInActivity::class.java)
        startActivity(intent)
    }

    private fun openEmail() {
        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:roeunshongkhoung@gmail.com")
            putExtra(Intent.EXTRA_SUBJECT, "Help Center Inquiry")
        }
        startActivity(Intent.createChooser(emailIntent, "Send email using"))
    }

    private fun openDialer() {
        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:0974567005")
        }
        startActivity(dialIntent)
    }
}
