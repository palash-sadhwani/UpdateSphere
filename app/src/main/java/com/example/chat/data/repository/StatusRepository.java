package com.example.chat.data.repository;

import android.content.Context;
import android.net.Uri;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatusRepository {
    private final FirebaseFirestore firestore;
    private ListenerRegistration statusesListener;

    public StatusRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    public Task<DocumentSnapshot> loadCurrentUser(String userId) {
        return firestore.collection("users")
                .document(userId)
                .get();
    }

    public void observeStatuses(EventListener<QuerySnapshot> listener) {
        if (statusesListener != null) {
            return;
        }

        statusesListener = firestore.collection("status")
                .addSnapshotListener(listener);
    }

    public void removeStatusesListener() {
        if (statusesListener != null) {
            statusesListener.remove();
            statusesListener = null;
        }
    }

    public Task<DocumentSnapshot> getUserStatuses(String userId) {
        return firestore.collection("status")
                .document(userId)
                .get();
    }

    public Task<Void> updateUserStatuses(String userId, List<?> statuses) {
        return firestore.collection("status")
                .document(userId)
                .update("statuses", statuses);
    }

    public interface ImageUploadListener {
        void onSuccess(String imageUrl);
        void onError(String errorMessage);
    }

    public void uploadImage(Context context, Uri imageUri, ImageUploadListener listener) {
        try {
            MediaManager.init(context.getApplicationContext());
        } catch (IllegalStateException ignored) {
            // Cloudinary is already initialized.
        }

        MediaManager.get().upload(imageUri)
                .option("folder", "status_images")
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        listener.onSuccess((String) resultData.get("url"));
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        listener.onError(error.getDescription());
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                    }
                })
                .dispatch();
    }

    public void uploadImage(Uri imageUri, ImageUploadListener listener) {
        try {
            MediaManager.get().upload(imageUri)
                    .option("folder", "status_images")
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {
                        }

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {
                        }

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            listener.onSuccess((String) resultData.get("url"));
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            listener.onError(error.getDescription());
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {
                        }
                    })
                    .dispatch();
        } catch (IllegalStateException error) {
            listener.onError(error.getMessage());
        }
    }

    public Task<Void> saveStatus(String userId, String text, String imageUrl) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null || !auth.getCurrentUser().getUid().equals(userId)) {
            return Tasks.forException(new IllegalStateException("No authenticated user"));
        }

        return getUserStatuses(userId).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                return Tasks.forException(task.getException());
            }

            DocumentSnapshot documentSnapshot = task.getResult();
            List<Map<String, Object>> statusList = new ArrayList<>();
            if (documentSnapshot.exists()) {
                List<Map<String, Object>> existingStatuses =
                        (List<Map<String, Object>>) documentSnapshot.get("statuses");
                if (existingStatuses != null) {
                    statusList = existingStatuses;
                }
            }

            Map<String, Object> newStatus = new HashMap<>();
            newStatus.put("text", text);
            newStatus.put("imageUrl", imageUrl);
            newStatus.put("timestamp", System.currentTimeMillis());
            statusList.add(newStatus);

            Map<String, Object> data = new HashMap<>();
            data.put("userId", userId);
            data.put("userName", auth.getCurrentUser().getDisplayName());
            data.put("statuses", statusList);

            return firestore.collection("status")
                    .document(userId)
                    .set(data);
        });
    }
}
