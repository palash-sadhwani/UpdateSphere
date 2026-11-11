package com.example.chat.data.repository;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;

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
}
