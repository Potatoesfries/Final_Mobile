package com.example.lostandfoundapp;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.FirebaseDatabase;

public class LostAndFoundApplication extends Application {
    private static final String TAG = "LostAndFoundApp";

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            // Initialize Firebase
            FirebaseApp.initializeApp(this);

            // Enable offline capabilities for Firebase Database
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);

            // Ensure proper database URL is set
            String databaseUrl = FirebaseDatabase.getInstance().getReference().toString();
            if (!databaseUrl.contains("lostandfoundapp-aba62")) {
                Log.w(TAG, "Firebase database URL doesn't contain expected project ID");

                // Get a new instance with the correct URL
                FirebaseDatabase.getInstance("https://lostandfoundapp-aba62-default-rtdb.firebaseio.com");
                Log.d(TAG, "Firebase database URL manually set");
            }

            Log.d(TAG, "Firebase successfully initialized");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase", e);

            // Try to initialize Firebase manually if there was an issue
            try {
                FirebaseOptions options = new FirebaseOptions.Builder()
                        .setApiKey("AIzaSyDmE-Hp6ODzN6PZzH-5EgaNeEnr-o2F2PA")
                        .setApplicationId("1:1046779342647:android:83ecfee62da7267f4c34b2")
                        .setDatabaseUrl("https://lostandfoundapp-aba62-default-rtdb.firebaseio.com")
                        .setProjectId("lostandfoundapp-aba62")
                        .setStorageBucket("lostandfoundapp-aba62.appspot.com")
                        .build();

                FirebaseApp.initializeApp(this, options);
                FirebaseDatabase.getInstance().setPersistenceEnabled(true);
                Log.d(TAG, "Firebase manually initialized successfully");
            } catch (Exception ex) {
                Log.e(TAG, "Failed to manually initialize Firebase", ex);
            }
        }
    }
}