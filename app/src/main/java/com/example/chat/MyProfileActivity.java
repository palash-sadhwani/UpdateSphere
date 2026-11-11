package com.example.chat;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MyProfileActivity extends AppCompatActivity {

    private TextView tvCurrentName, tvCurrentEmail;
    private EditText etName, etEmail, etCurrentPassword, etNewPassword;
    private Button btnUpdateProfile, btnUpdatePassword, btnLogout;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Initialize views
        tvCurrentName = findViewById(R.id.tvCurrentName);
        tvCurrentEmail = findViewById(R.id.tvCurrentEmail);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        btnUpdateProfile = findViewById(R.id.btnUpdateProfile);
        btnUpdatePassword = findViewById(R.id.btnUpdatePassword);
        btnLogout = findViewById(R.id.btnLogout);

        // Load current user data
        loadUserData();

        // Update profile button click
        btnUpdateProfile.setOnClickListener(v -> updateProfile());

        // Update password button click
        btnUpdatePassword.setOnClickListener(v -> updatePassword());

        // Logout button click
        btnLogout.setOnClickListener(v -> logoutUser());
    }

    private void loadUserData() {
        if (currentUser != null) {
            // Set email from Firebase Auth
            tvCurrentEmail.setText(currentUser.getEmail());
            etEmail.setText(currentUser.getEmail());

            // Get display name from Firestore
            db.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            if (name != null && !name.isEmpty()) {
                                tvCurrentName.setText(name);
                                etName.setText(name);
                            } else {
                                tvCurrentName.setText("No name set");
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                        Log.e("MyProfile", "Error loading user data", e);
                    });
        }
    }

    private void updateProfile() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email cannot be empty");
            return;
        }

        // Show progress
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Updating profile...");
        progressDialog.show();

        // Update email in Firebase Auth
        currentUser.updateEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Update name in Firestore
                        updateUserDataInFirestore(name, email, progressDialog);
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(MyProfileActivity.this,
                                "Failed to update email: " + task.getException(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateUserDataInFirestore(String name, String email, ProgressDialog progressDialog) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("email", email);

        db.collection("users")
                .document(currentUser.getUid())
                .update(userData)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    tvCurrentName.setText(name);
                    tvCurrentEmail.setText(email);
                    Toast.makeText(MyProfileActivity.this,
                            "Profile updated successfully",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(MyProfileActivity.this,
                            "Failed to update profile: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void updatePassword() {
        String currentPassword = etCurrentPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();

        if (TextUtils.isEmpty(currentPassword) || TextUtils.isEmpty(newPassword)) {
            Toast.makeText(this, "Both password fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPassword.length() < 6) {
            etNewPassword.setError("Password should be at least 6 characters");
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Updating password...");
        progressDialog.show();

        // Reauthenticate user before changing password
        AuthCredential credential = EmailAuthProvider
                .getCredential(currentUser.getEmail(), currentPassword);

        currentUser.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        currentUser.updatePassword(newPassword)
                                .addOnCompleteListener(updateTask -> {
                                    progressDialog.dismiss();
                                    if (updateTask.isSuccessful()) {
                                        Toast.makeText(MyProfileActivity.this,
                                                "Password updated successfully",
                                                Toast.LENGTH_SHORT).show();
                                        etCurrentPassword.setText("");
                                        etNewPassword.setText("");
                                    } else {
                                        Toast.makeText(MyProfileActivity.this,
                                                "Failed to update password: " + updateTask.getException(),
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(MyProfileActivity.this,
                                "Authentication failed: " + task.getException(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void logoutUser() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Logging out...");
        progressDialog.show();

        // Update online status before logging out
        db.collection("users")
                .document(currentUser.getUid())
                .update("online", false, "lastSeen", System.currentTimeMillis())
                .addOnCompleteListener(task -> {
                    mAuth.signOut();
                    progressDialog.dismiss();
                    startActivity(new Intent(MyProfileActivity.this, LoginActivity.class));
                    finish();
                });
    }
}