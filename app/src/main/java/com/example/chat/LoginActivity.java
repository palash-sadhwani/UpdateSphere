package com.example.chat;


import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.chat.data.repository.AuthRepository;
import com.example.chat.presentation.auth.AuthViewModel;

public class LoginActivity extends AppCompatActivity {
    private EditText emailInput, passwordInput;
    private Button loginButton, signupButton;
    private ProgressBar progressBar;
    private TextView forgotPassword;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        signupButton = findViewById(R.id.signupButton);
        progressBar = findViewById(R.id.progressBar);
        forgotPassword = findViewById(R.id.forgotPasswordButton);

        authViewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends androidx.lifecycle.ViewModel> T create(@NonNull Class<T> modelClass) {
                return modelClass.cast(new AuthViewModel(new AuthRepository()));
            }
        }).get(AuthViewModel.class);
        authViewModel.getAuthState().observe(this, this::handleAuthState);

        loginButton.setOnClickListener(v -> loginUser());
        signupButton.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
        forgotPassword.setOnClickListener(v -> resetPassword());
    }

    private void loginUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password is required");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        authViewModel.login(email, password);
    }

    private void resetPassword() {
        String email = emailInput.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email is required");
            return;
        }

        authViewModel.resetPassword(email);
    }

    private void handleAuthState(AuthViewModel.AuthState state) {
        if (state.getStatus() == AuthViewModel.Status.LOADING) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.GONE);
        }

        if (state.getStatus() == AuthViewModel.Status.SUCCESS) {
            if (state.getMessage() != null) {
                Toast.makeText(this, state.getMessage(), Toast.LENGTH_SHORT).show();
            } else {
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            }
        } else if (state.getStatus() == AuthViewModel.Status.ERROR) {
            Toast.makeText(this, "Error: " + state.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}