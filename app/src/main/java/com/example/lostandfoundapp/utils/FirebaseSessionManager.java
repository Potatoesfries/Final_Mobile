package com.example.lostandfoundapp.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.lostandfoundapp.firebase.FirebaseManager;
import com.example.lostandfoundapp.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class FirebaseSessionManager {
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private Context context;
    private FirebaseManager firebaseManager;

    public FirebaseSessionManager(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        firebaseManager = FirebaseManager.getInstance();
    }

    public void saveAuthUser(User user) {
        editor.putString(Constants.KEY_USER_NAME, user.getName());
        editor.putString(Constants.KEY_USER_EMAIL, user.getEmail());
        editor.putBoolean(Constants.KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    public boolean isLoggedIn() {
        // Check both Firebase and local storage
        return firebaseManager.isUserLoggedIn() &&
                sharedPreferences.getBoolean(Constants.KEY_IS_LOGGED_IN, false);
    }

    public String getUserId() {
        return firebaseManager.getCurrentUserId();
    }

    public String getUserName() {
        return sharedPreferences.getString(Constants.KEY_USER_NAME, null);
    }

    public String getUserEmail() {
        return sharedPreferences.getString(Constants.KEY_USER_EMAIL, null);
    }

    public void refreshUserData(final OnCompleteListener<User> listener) {
        firebaseManager.getUserData(new OnCompleteListener<User>() {
            @Override
            public void onComplete(Task<User> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    User user = task.getResult();
                    saveAuthUser(user);
                }

                if (listener != null) {
                    listener.onComplete(task);
                }
            }
        });
    }

    public void logout() {
        // Clear Firebase Auth
        firebaseManager.logoutUser();

        // Clear local storage
        editor.clear();
        editor.apply();
    }
}