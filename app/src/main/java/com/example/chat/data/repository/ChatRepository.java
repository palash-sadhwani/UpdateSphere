package com.example.chat.data.repository;

import com.example.chat.Message;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ChatRepository {
    private final FirebaseFirestore firestore;

    public ChatRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    public ListenerRegistration observeChatUser(
            String receiverId,
            EventListener<DocumentSnapshot> listener
    ) {
        return firestore.collection("users")
                .document(receiverId)
                .addSnapshotListener(listener);
    }

    public ListenerRegistration observeMessages(
            String chatId,
            EventListener<QuerySnapshot> listener
    ) {
        return firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener(listener);
    }

    public Task<Void> markChatAsRead(String chatId, String currentUserId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("readStatus." + currentUserId, true);
        updates.put("unreadCount", 0);

        return firestore.collection("chats")
                .document(chatId)
                .update(updates);
    }

    public Task<Void> markMessageAsRead(String chatId, String messageId) {
        return firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .document(messageId)
                .update("status", Message.STATUS_READ);
    }

    public Task<Void> updateMessageStatus(
            String chatId,
            String messageId,
            boolean isDelivered,
            boolean isRead
    ) {
        Map<String, Object> updates = new HashMap<>();
        if (isRead) {
            updates.put("status", Message.STATUS_READ);
        } else if (isDelivered) {
            updates.put("status", Message.STATUS_DELIVERED);
        } else {
            updates.put("status", Message.STATUS_SENT);
        }

        return firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .document(messageId)
                .update(updates);
    }

    public Task<String> sendMessage(
            String chatId,
            String currentUserId,
            String receiverId,
            Message message
    ) {
        TaskCompletionSource<String> result = new TaskCompletionSource<>();
        WriteBatch batch = firestore.batch();

        DocumentReference chatReference = firestore.collection("chats").document(chatId);
        batch.set(
                chatReference,
                new HashMap<String, Object>() {{
                    put("participants", Arrays.asList(currentUserId, receiverId));
                    put("lastMessage", "");
                    put("timestamp", FieldValue.serverTimestamp());
                }},
                SetOptions.merge()
        );

        DocumentReference messageReference = chatReference.collection("messages").document();
        message.setMessageId(messageReference.getId());
        batch.set(messageReference, message);

        Map<String, Object> chatUpdates = new HashMap<>();
        chatUpdates.put("lastMessage", message.getContent());
        chatUpdates.put("lastMessageSenderId", currentUserId);
        chatUpdates.put("timestamp", FieldValue.serverTimestamp());
        chatUpdates.put("readStatus." + receiverId, false);
        chatUpdates.put("unreadCount", FieldValue.increment(1));
        batch.update(chatReference, chatUpdates);

        batch.commit()
                .addOnSuccessListener(unused -> result.setResult(messageReference.getId()))
                .addOnFailureListener(result::setException);

        return result.getTask();
    }
}
