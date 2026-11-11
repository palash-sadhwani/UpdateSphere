package com.example.chat.data.repository;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfileRepository {
    public interface ProfileCallback {
        void onResult(String email, DocumentSnapshot snapshot, Exception error);
    }

    public interface ResultCallback {
        void onResult(Exception error);
    }

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    public ProfileRepository() {
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    public void loadCurrentProfile(ProfileCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onResult(null, null, new IllegalStateException("No authenticated user"));
            return;
        }

        firestore.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> callback.onResult(user.getEmail(), snapshot, null))
                .addOnFailureListener(error -> callback.onResult(user.getEmail(), null, error));
    }

    public void updateProfile(String name, String email, ResultCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onResult(new IllegalStateException("No authenticated user"));
            return;
        }

        user.updateEmail(email).addOnCompleteListener(emailTask -> {
            if (!emailTask.isSuccessful()) {
                callback.onResult(getTaskException(emailTask, "Failed to update email"));
                return;
            }

            Map<String, Object> userData = new HashMap<>();
            userData.put("name", name);
            userData.put("email", email);
            firestore.collection("users").document(user.getUid())
                    .update(userData)
                    .addOnSuccessListener(ignored -> callback.onResult(null))
                    .addOnFailureListener(callback::onResult);
        });
    }

    public void updatePassword(
            String currentPassword,
            String newPassword,
            ResultCallback callback
    ) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onResult(new IllegalStateException("No authenticated user"));
            return;
        }
        if (user.getEmail() == null) {
            callback.onResult(new IllegalStateException("Authenticated user has no email"));
            return;
        }

        AuthCredential credential = EmailAuthProvider
                .getCredential(user.getEmail(), currentPassword);
        user.reauthenticate(credential).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                callback.onResult(getTaskException(task, "Authentication failed"));
                return;
            }

            user.updatePassword(newPassword)
                    .addOnCompleteListener(updateTask -> {
                        if (updateTask.isSuccessful()) {
                            callback.onResult(null);
                        } else {
                            callback.onResult(getTaskException(
                                    updateTask,
                                    "Failed to update password"
                            ));
                        }
                    });
        });
    }

    public void logout(ResultCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            auth.signOut();
            callback.onResult(null);
            return;
        }

        firestore.collection("users")
                .document(user.getUid())
                .update("isOnline", false, "lastSeen", System.currentTimeMillis())
                .addOnCompleteListener(task -> {
                    auth.signOut();
                    callback.onResult(null);
                });
    }

    private Exception getTaskException(com.google.android.gms.tasks.Task<?> task,
                                       String fallbackMessage) {
        return task.getException() != null
                ? task.getException()
                : new IllegalStateException(fallbackMessage);
    }
}
