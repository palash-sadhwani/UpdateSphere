package com.example.chat.data.repository;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AuthRepository {
    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    public AuthRepository() {
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    public Task<AuthResult> login(String email, String password) {
        return auth.signInWithEmailAndPassword(email, password);
    }

    public Task<Void> register(String name, String email, String password) {
        TaskCompletionSource<Void> result = new TaskCompletionSource<>();

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(authTask -> {
                    if (!authTask.isSuccessful()) {
                        result.setException(getTaskException(authTask, "Registration failed"));
                        return;
                    }

                    FirebaseUser user = authTask.getResult() != null
                            ? authTask.getResult().getUser()
                            : null;
                    if (user == null) {
                        result.setException(new IllegalStateException("Registration returned no user"));
                        return;
                    }

                    Map<String, Object> userData = new HashMap<>();
                    userData.put("name", name);
                    userData.put("email", email);
                    userData.put("userId", user.getUid());
                    userData.put("status", "Hey there! I'm using WhatsApp Clone");
                    userData.put("isOnline", false);
                    userData.put("lastSeen", System.currentTimeMillis());
                    userData.put("profileImage", "");

                    firestore.collection("users").document(user.getUid())
                            .set(userData)
                            .addOnCompleteListener(profileTask -> {
                                if (!profileTask.isSuccessful()) {
                                    result.setException(getTaskException(
                                            profileTask,
                                            "Failed to create user profile"
                                    ));
                                    return;
                                }

                                user.sendEmailVerification()
                                        .addOnCompleteListener(verificationTask -> {
                                            if (verificationTask.isSuccessful()) {
                                                result.setResult(null);
                                            } else {
                                                result.setException(getTaskException(
                                                        verificationTask,
                                                        "Failed to send verification email"
                                                ));
                                            }
                                        });
                            });
                });

        return result.getTask();
    }

    public Task<Void> resetPassword(String email) {
        return auth.sendPasswordResetEmail(email);
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public boolean isAuthenticated() {
        return getCurrentUser() != null;
    }

    public boolean isCurrentUserEmailVerified() {
        FirebaseUser user = getCurrentUser();
        return user != null && user.isEmailVerified();
    }

    public void logout() {
        auth.signOut();
    }

    private Exception getTaskException(@NonNull Task<?> task, String fallbackMessage) {
        return task.getException() != null
                ? task.getException()
                : new IllegalStateException(fallbackMessage);
    }
}
