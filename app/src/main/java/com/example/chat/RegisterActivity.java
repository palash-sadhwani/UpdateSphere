package com.example.chat;


import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText nameInput, emailInput, passwordInput, confirmPasswordInput;
    private Button registerButton, loginButton;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase instances
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Initialize views
        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        registerButton = findViewById(R.id.registerButton);
        loginButton = findViewById(R.id.loginButton);
        progressBar = findViewById(R.id.progressBar);

        // Set click listeners
        registerButton.setOnClickListener(v -> registerUser());
        loginButton.setOnClickListener(v -> navigateToLogin());
    }

    private void registerUser() {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        // Validate inputs
        if (TextUtils.isEmpty(name)) {
            nameInput.setError("Name is required");
            nameInput.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email is required");
            emailInput.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Please enter a valid email");
            emailInput.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password is required");
            passwordInput.requestFocus();
            return;
        }

        if (password.length() < 6) {
            passwordInput.setError("Password must be at least 6 characters");
            passwordInput.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError("Passwords don't match");
            confirmPasswordInput.requestFocus();
            return;
        }

        // Show progress bar
        progressBar.setVisibility(View.VISIBLE);
        registerButton.setEnabled(false);

        // Create user with Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // User created successfully
                        String userId = mAuth.getCurrentUser().getUid();

                        // Create user document in Firestore
                        Map<String, Object> user = new HashMap<>();
                        user.put("name", name);
                        user.put("email", email);
                        user.put("userId", userId);
                        user.put("status", "Hey there! I'm using WhatsApp Clone");
                        user.put("online", false);
                        user.put("lastSeen", System.currentTimeMillis());
                        user.put("profileImage", ""); // Default empty, user can set later

                        firestore.collection("users").document(userId)
                                .set(user)
                                .addOnSuccessListener(aVoid -> {
                                    // Send email verification
                                    mAuth.getCurrentUser().sendEmailVerification()
                                            .addOnCompleteListener(emailTask -> {
                                                progressBar.setVisibility(View.GONE);
                                                if (emailTask.isSuccessful()) {
                                                    Toast.makeText(RegisterActivity.this,
                                                            "Registration successful! Please verify your email.",
                                                            Toast.LENGTH_LONG).show();
                                                    navigateToLogin();
                                                } else {
                                                    Toast.makeText(RegisterActivity.this,
                                                            "Registration successful but failed to send verification email: " +
                                                                    emailTask.getException().getMessage(),
                                                            Toast.LENGTH_LONG).show();
                                                }
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    progressBar.setVisibility(View.GONE);
                                    registerButton.setEnabled(true);
                                    Toast.makeText(RegisterActivity.this,
                                            "Failed to create user profile: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                });
                    } else {
                        // Registration failed
                        progressBar.setVisibility(View.GONE);
                        registerButton.setEnabled(true);
                        Toast.makeText(RegisterActivity.this,
                                "Registration failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void navigateToLogin() {
        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // If user is already logged in, go to main activity
        if (mAuth.getCurrentUser() != null && mAuth.getCurrentUser().isEmailVerified()) {
            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
            finish();
        }
    }
}
