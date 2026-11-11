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
import androidx.lifecycle.ViewModelProvider;

import com.example.chat.presentation.profile.ProfileViewModel;

public class MyProfileActivity extends AppCompatActivity {

    private TextView tvCurrentName, tvCurrentEmail;
    private EditText etName, etEmail, etCurrentPassword, etNewPassword;
    private Button btnUpdateProfile, btnUpdatePassword, btnLogout;
    private ProfileViewModel profileViewModel;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_profile);

        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

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

        profileViewModel.getState().observe(this, this::renderState);
        profileViewModel.loadProfile();

        // Update profile button click
        btnUpdateProfile.setOnClickListener(v -> updateProfile());

        // Update password button click
        btnUpdatePassword.setOnClickListener(v -> updatePassword());

        // Logout button click
        btnLogout.setOnClickListener(v -> logoutUser());
    }

    private void updateProfile() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email cannot be empty");
            return;
        }

        profileViewModel.updateProfile(name, email);
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

        profileViewModel.updatePassword(currentPassword, newPassword);
    }

    private void logoutUser() {
        profileViewModel.logout();
    }

    private void renderState(ProfileViewModel.ProfileState state) {
        updateProgress(state.getOperation());

        if (state.getEmail() != null) {
            tvCurrentEmail.setText(state.getEmail());
            etEmail.setText(state.getEmail());
        }
        if (state.getName() != null) {
            tvCurrentName.setText(state.getName());
            if (!"No name set".equals(state.getName())) {
                etName.setText(state.getName());
            }
        }

        ProfileViewModel.Event event = state.getEvent();
        if (event == null || !event.consume()) {
            return;
        }

        if (event.getType() == ProfileViewModel.EventType.LOGGED_OUT) {
            startActivity(new Intent(MyProfileActivity.this, LoginActivity.class));
            finish();
            return;
        }

        if (event.getType() == ProfileViewModel.EventType.PASSWORD_UPDATED) {
            etCurrentPassword.setText("");
            etNewPassword.setText("");
        }
        Toast.makeText(this, event.getMessage(), Toast.LENGTH_SHORT).show();
    }

    private void updateProgress(ProfileViewModel.Operation operation) {
        String message = null;
        if (operation == ProfileViewModel.Operation.UPDATING_PROFILE) {
            message = "Updating profile...";
        } else if (operation == ProfileViewModel.Operation.UPDATING_PASSWORD) {
            message = "Updating password...";
        } else if (operation == ProfileViewModel.Operation.LOGGING_OUT) {
            message = "Logging out...";
        }

        if (message != null) {
            if (progressDialog == null) {
                progressDialog = new ProgressDialog(this);
            }
            progressDialog.setMessage(message);
            if (!progressDialog.isShowing()) {
                progressDialog.show();
            }
        } else if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}