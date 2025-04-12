package com.example.lostandfoundapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.example.lostandfoundapp.auth.LoginActivity;
import com.example.lostandfoundapp.utils.FirebaseSessionManager;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_TIMEOUT = 2000; // 2 seconds
    private FirebaseSessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        // Using theme instead of setContentView

        // Initialize session manager
        sessionManager = new FirebaseSessionManager(this);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Check if user is logged in
                if (sessionManager.isLoggedIn()) {
                    // User is logged in, go to main activity
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                } else {
                    // User is not logged in, go to login activity
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                }

                // Close this activity
                finish();
            }
        }, SPLASH_TIMEOUT);
    }
}