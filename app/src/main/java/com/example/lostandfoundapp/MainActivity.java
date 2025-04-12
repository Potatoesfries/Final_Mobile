package com.example.lostandfoundapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.lostandfoundapp.auth.LoginActivity;
import com.example.lostandfoundapp.fragments.AddItemFragment;
import com.example.lostandfoundapp.fragments.HomeFragment;
import com.example.lostandfoundapp.fragments.ProfileFragment;
import com.example.lostandfoundapp.utils.FirebaseSessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    private BottomNavigationView bottomNavigationView;
    private FirebaseSessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize session manager
        sessionManager = new FirebaseSessionManager(this);

        // Check if user is logged in
        if (!sessionManager.isLoggedIn()) {
            // User is not logged in, redirect to login activity
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Initialize bottom navigation
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(this);

        // Set default fragment
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        // Add this after you've uploaded images through ADB or Device File Explorer
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file:///sdcard/Pictures/")));
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            return true;
        }
        return false;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment fragment = null;

        int itemId = item.getItemId();
        if (itemId == R.id.navigation_home) {
            fragment = new HomeFragment();
        } else if (itemId == R.id.navigation_add) {
            fragment = new AddItemFragment();
        } else if (itemId == R.id.navigation_profile) {
            fragment = new ProfileFragment();
        }

        return loadFragment(fragment);
    }
}