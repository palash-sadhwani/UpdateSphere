package com.example.chat.data.repository;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

public class ChatListRepository {
    public interface UserCallback {
        void onResult(DocumentSnapshot snapshot, Exception error);
    }

    private final FirebaseFirestore firestore;
    private ListenerRegistration chatsListener;

    public ChatListRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    public void observeChats(
            String currentUserId,
            EventListener<QuerySnapshot> listener
    ) {
        if (chatsListener != null) {
            return;
        }

        chatsListener = firestore.collection("chats")
                .whereArrayContains("participants", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener(listener);
    }

    public void loadUser(String userId, UserCallback callback) {
        firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(snapshot -> callback.onResult(snapshot, null))
                .addOnFailureListener(error -> callback.onResult(null, error));
    }

    public void removeChatsListener() {
        if (chatsListener != null) {
            chatsListener.remove();
            chatsListener = null;
        }
    }
}
