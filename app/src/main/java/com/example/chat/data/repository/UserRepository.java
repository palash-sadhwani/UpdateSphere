package com.example.chat.data.repository;

import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

public class UserRepository {
    private final FirebaseFirestore firestore;
    private ListenerRegistration usersListener;

    public UserRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    public void observeUsers(EventListener<QuerySnapshot> listener) {
        if (usersListener != null) {
            return;
        }

        usersListener = firestore.collection("users")
                .orderBy("name", Query.Direction.ASCENDING)
                .addSnapshotListener(listener);
    }

    public void removeUsersListener() {
        if (usersListener != null) {
            usersListener.remove();
            usersListener = null;
        }
    }
}
