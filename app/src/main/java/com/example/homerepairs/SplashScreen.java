package com.example.homerepairs;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashScreen extends AppCompatActivity {

    private static final int SPLASH_TIME_OUT = 3000; // 3 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Check if user is already logged in
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

                Intent intent;
                if (currentUser != null) {
                    // User is logged in → go to MainActivity
                    intent = new Intent(SplashScreen.this, MainActivity.class);
                } else {
                    // User not logged in → go to GetStartActivity (or SignIn)
                    intent = new Intent(SplashScreen.this, GetStartActivity.class);
                }

                startActivity(intent);
                finish(); // Remove SplashScreen from back stack
            }
        }, SPLASH_TIME_OUT);
    }
}
