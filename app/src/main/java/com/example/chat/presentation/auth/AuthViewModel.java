package com.example.chat.presentation.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.chat.data.repository.AuthRepository;

public class AuthViewModel extends ViewModel {
    public enum Status {
        IDLE,
        LOADING,
        SUCCESS,
        ERROR
    }

    public static class AuthState {
        private final Status status;
        private final String message;

        public AuthState(Status status, String message) {
            this.status = status;
            this.message = message;
        }

        public Status getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }
    }

    private final AuthRepository repository;
    private final MutableLiveData<AuthState> authState =
            new MutableLiveData<>(new AuthState(Status.IDLE, null));

    public AuthViewModel(AuthRepository repository) {
        this.repository = repository;
    }

    public LiveData<AuthState> getAuthState() {
        return authState;
    }

    public void login(String email, String password) {
        authState.setValue(new AuthState(Status.LOADING, null));
        repository.login(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                authState.postValue(new AuthState(Status.SUCCESS, null));
            } else {
                authState.postValue(new AuthState(Status.ERROR, getErrorMessage(task.getException())));
            }
        });
    }

    public void register(String name, String email, String password) {
        authState.setValue(new AuthState(Status.LOADING, null));
        repository.register(name, email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                authState.postValue(new AuthState(
                        Status.SUCCESS,
                        "Registration successful! Please verify your email."
                ));
            } else {
                authState.postValue(new AuthState(
                        Status.ERROR,
                        getErrorMessage(task.getException())
                ));
            }
        });
    }

    public void resetPassword(String email) {
        authState.setValue(new AuthState(Status.LOADING, null));
        repository.resetPassword(email).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                authState.postValue(new AuthState(Status.SUCCESS, "Password reset email sent"));
            } else {
                authState.postValue(new AuthState(Status.ERROR, getErrorMessage(task.getException())));
            }
        });
    }

    private String getErrorMessage(Exception exception) {
        return exception != null && exception.getMessage() != null
                ? exception.getMessage()
                : "Authentication failed";
    }
}
