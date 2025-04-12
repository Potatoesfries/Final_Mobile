package com.example.lostandfoundapp.auth;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.lostandfoundapp.MainActivity;
import com.example.lostandfoundapp.R;
import com.example.lostandfoundapp.firebase.FirebaseManager;
import com.example.lostandfoundapp.model.User;
import com.example.lostandfoundapp.utils.FirebaseSessionManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText editTextEmail, editTextPassword;
    private Button buttonLogin;
    private TextView textViewRegister;
    private ProgressBar progressBar;
    private FirebaseSessionManager sessionManager;
    private FirebaseManager firebaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize session manager and firebase manager
        sessionManager = new FirebaseSessionManager(this);
        firebaseManager = FirebaseManager.getInstance();

        // Check if user is already logged in
        if (sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // Initialize views
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        textViewRegister = findViewById(R.id.textViewRegister);
        progressBar = findViewById(R.id.progressBar);

        // Set click listeners
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        textViewRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });
    }

    private void loginUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // Validate input fields
        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Email is required");
            editTextEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("Password is required");
            editTextPassword.requestFocus();
            return;
        }

        // Show progress bar
        progressBar.setVisibility(View.VISIBLE);
        buttonLogin.setEnabled(false);

        // Login with Firebase
        firebaseManager.loginUser(email, password, new OnCompleteListener<User>() {
            @Override
            public void onComplete(Task<User> task) {
                progressBar.setVisibility(View.GONE);
                buttonLogin.setEnabled(true);

                if (task.isSuccessful() && task.getResult() != null) {
                    // Login successful
                    User user = task.getResult();

                    // Save user session
                    sessionManager.saveAuthUser(user);

                    // Navigate to main activity
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                } else {
                    // Login failed
                    String errorMessage = task.getException() != null ?
                            task.getException().getMessage() :
                            "Login failed. Please try again.";

                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}