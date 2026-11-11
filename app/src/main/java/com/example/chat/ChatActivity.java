package com.example.chat;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {
    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private ImageButton sendButton, attachButton, emojiButton, voiceButton;
    private List<Message> messages = new ArrayList<>();
    private ChatAdapter chatAdapter;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String currentUserId, receiverId, chatId;
    private TextView chatName, chatStatus;
    private CircleImageView chatAvatar;
    private ListenerRegistration messagesListener, chatListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        initializeViews();
        setupUserData();
        setupRecyclerView();
        setupListeners();
        loadMessages();
        markChatAsRead();
    }

    private void initializeViews() {
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        attachButton = findViewById(R.id.attachButton);
        emojiButton = findViewById(R.id.emojiButton);
        voiceButton = findViewById(R.id.voiceButton);
        chatName = findViewById(R.id.chatName);
        chatStatus = findViewById(R.id.chatStatus);
        chatAvatar = findViewById(R.id.chatAvatar);

        findViewById(R.id.backButton).setOnClickListener(v -> finish());
    }

    private void setupUserData() {
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        receiverId = getIntent().getStringExtra("receiverId");
        chatId = generateChatId(currentUserId, receiverId);

        db.collection("users").document(receiverId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e("ChatActivity", "Listen failed", error);
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        String name = snapshot.getString("name");
                        chatName.setText(name != null ? name : "Unknown");

                        Boolean isOnline = snapshot.getBoolean("isOnline");
                        boolean onlineStatus = isOnline != null ? isOnline : false;

                        chatStatus.setText(onlineStatus ? "online" : "last seen recently");
                        chatStatus.setTextColor(onlineStatus ?
                                ContextCompat.getColor(this, R.color.online_green) :
                                ContextCompat.getColor(this, R.color.offline_gray));

                        String avatarUrl = snapshot.getString("avatarUrl");
                        if (avatarUrl != null && !avatarUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(avatarUrl)
                                    .placeholder(R.drawable.default_avatar)
                                    .into(chatAvatar);
                        }
                    }
                });
    }

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter(messages, currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);
    }

    private void setupListeners() {
        sendButton.setOnClickListener(v -> sendMessage());

        messageInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = s.length() > 0;
                sendButton.setVisibility(hasText ? View.VISIBLE : View.GONE);
                voiceButton.setVisibility(hasText ? View.GONE : View.VISIBLE);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        attachButton.setOnClickListener(v -> showAttachmentOptions());
        emojiButton.setOnClickListener(v -> toggleEmojiKeyboard());
    }

//    private void loadMessages() {
//        messagesListener = db.collection("chats").document(chatId)
//                .collection("messages")
//                .orderBy("timestamp", Query.Direction.ASCENDING)
//                .addSnapshotListener((snapshots, error) -> {
//                    if (error != null) {
//                        Toast.makeText(this, "Error loading messages", Toast.LENGTH_SHORT).show();
//                        return;
//                    }
//
//                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
//                        if (dc.getType() == DocumentChange.Type.ADDED) {
//                            Message message = dc.getDocument().toObject(Message.class);
//                            message.setMessageId(dc.getDocument().getId());
//
//                            // Add to messages list
//                            int messageIndex = -1;
//                            for (int i = 0; i < messages.size(); i++) {
//                                if (messages.get(i).getMessageId().equals(message.getMessageId())) {
//                                    messageIndex = i;
//                                    break;
//                                }
//                            }
//
//                            if (messageIndex == -1) {
//                                messages.add(message);
//                                chatAdapter.notifyItemInserted(messages.size() - 1);
//                            } else {
//                                messages.set(messageIndex, message);
//                                chatAdapter.notifyItemChanged(messageIndex);
//                            }
//
//                            // Mark as read if it's a received message
//                            // Mark as read if it's a received message
//                            if (!message.getSenderId().equals(currentUserId)) {
//                                if (!message.isRead()) {
//                                    markMessageAsRead(message);
//                                }
//
//                                // Update message status to delivered if not already
//                                if (!message.isDelivered()) {
//                                    updateMessageStatus(message.getMessageId(), true, false);
//                                }
//                            }
//                        } else if (dc.getType() == DocumentChange.Type.MODIFIED) {
//                            Message updatedMessage = dc.getDocument().toObject(Message.class);
//                            updatedMessage.setMessageId(dc.getDocument().getId());
//
//                            // Update existing message
//                            for (int i = 0; i < messages.size(); i++) {
//                                if (messages.get(i).getMessageId().equals(updatedMessage.getMessageId())) {
//                                    messages.set(i, updatedMessage);
//                                    chatAdapter.notifyItemChanged(i);
//                                    break;
//                                }
//                            }
//                        }
//                    }
//                    scrollToBottom();
//                });
//    }
private void loadMessages() {
    messagesListener = db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null) return;

                for (DocumentChange dc : snapshots.getDocumentChanges()) {
                    Message message = dc.getDocument().toObject(Message.class);
                    message.setMessageId(dc.getDocument().getId());

                    switch (dc.getType()) {
                        case ADDED:
                            handleNewMessage(message);
                            break;
                        case MODIFIED:
                            handleUpdatedMessage(message);
                            break;
                    }
                }
                scrollToBottom();
            });
}

    private void handleNewMessage(Message message) {
        // Add to messages list
        messages.add(message);
        chatAdapter.notifyItemInserted(messages.size() - 1);

        // Mark as read if received message
        if (!message.getSenderId().equals(currentUserId) && !message.isRead()) {
            markMessageAsRead(message);
        }
    }

    private void handleUpdatedMessage(Message message) {
        // Update existing message
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i).getMessageId().equals(message.getMessageId())) {
                messages.set(i, message);
                chatAdapter.notifyItemChanged(i);
                break;
            }
        }
    }
    private void markChatAsRead() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("readStatus." + currentUserId, true);
        updates.put("unreadCount", 0); // Reset unread count

        db.collection("chats").document(chatId)
                .update(updates)
                .addOnFailureListener(e -> Log.e("ChatActivity", "Error marking chat as read", e));
    }

//    private void markMessageAsRead(Message message) {
//        db.collection("chats").document(chatId)
//                .collection("messages").document(message.getMessageId())
//                .update("isRead", true)
//                .addOnFailureListener(e -> Log.e("ChatActivity", "Error marking message as read", e));
//    }
@Override
protected void onResume() {
    super.onResume();
    markChatAsRead();
    markAllMessagesAsRead();

    // Mark all received messages as read
    for (Message message : messages) {
        if (!message.getSenderId().equals(currentUserId)) {  // Fixed missing parenthesis
            markMessageAsRead(message);
        }
    }
}

//    private void markMessageAsRead(Message message) {
//        if (message == null || message.isRead() || message.getSenderId().equals(currentUserId)) {
//            return;
//        }
//
//        db.collection("chats").document(chatId)
//                .collection("messages").document(message.getMessageId())
//                .update("status", Message.STATUS_READ)  // Update status to READ
//                .addOnSuccessListener(aVoid -> {
//                    // Update local message status
//                    message.setStatus(Message.STATUS_READ);
//                    int position = messages.indexOf(message);
//                    if (position != -1) {
//                        chatAdapter.notifyItemChanged(position);
//                    }
//                })
//                .addOnFailureListener(e ->
//                        Log.e("ChatActivity", "Error marking message as read", e));
//    }
private void markMessageAsRead(Message message) {
    if (message.isRead() || message.getSenderId().equals(currentUserId)) {
        return;
    }

    // Update in Firestore
    db.collection("chats").document(chatId)
            .collection("messages").document(message.getMessageId())
            .update("status", Message.STATUS_READ)
            .addOnSuccessListener(aVoid -> {
                // Update local message
                message.markAsRead();
                chatAdapter.notifyItemChanged(messages.indexOf(message));
            });
}



    private void markAllMessagesAsRead() {
        for (Message message : messages) {
            if (!message.getSenderId().equals(currentUserId) && !message.isRead()) {
                markMessageAsRead(message);
            }
        }

        // Also update chat-level read status
        db.collection("chats").document(chatId)
                .update("readStatus." + currentUserId, true,
                        "unreadCount", 0);
    }

    private void sendMessage() {
        String content = messageInput.getText().toString().trim();
        if (content.isEmpty()) return;

        // Create new message
        Message message = new Message(currentUserId, receiverId, chatId, content);
        message.setTimestamp(new Date(System.currentTimeMillis()));
        message.setStatus(Message.STATUS_SENT);

        // Create batch operation
        WriteBatch batch = db.batch();

        // First, ensure the chat document exists
        DocumentReference chatRef = db.collection("chats").document(chatId);
        batch.set(chatRef,
                new HashMap<String, Object>() {{
                    put("participants", Arrays.asList(currentUserId, receiverId));
                    put("lastMessage", "");
                    put("timestamp", FieldValue.serverTimestamp());
                }},
                SetOptions.merge() // Only update fields that are specified
        );

        // Add message to subcollection
        DocumentReference messageRef = chatRef.collection("messages").document();
        message.setMessageId(messageRef.getId());
        batch.set(messageRef, message);

        // Update chat document with latest message info
        Map<String, Object> chatUpdates = new HashMap<>();
        chatUpdates.put("lastMessage", content);
        chatUpdates.put("lastMessageSenderId", currentUserId);
        chatUpdates.put("timestamp", FieldValue.serverTimestamp());
        chatUpdates.put("readStatus." + receiverId, false);
        chatUpdates.put("unreadCount", FieldValue.increment(1));

        batch.update(chatRef, chatUpdates);

        // Commit batch
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    messageInput.setText("");
                    updateMessageStatus(messageRef.getId(), true, false);
                    scrollToBottom();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show();
                    Log.e("ChatActivity", "Message send failed", e);
                });
    }
    private void updateMessageStatus(String messageId, boolean isDelivered, boolean isRead) {
        Map<String, Object> updates = new HashMap<>();
        if (isRead) {
            updates.put("status", Message.STATUS_READ);
        } else if (isDelivered) {
            updates.put("status", Message.STATUS_DELIVERED);
        } else {
            updates.put("status", Message.STATUS_SENT);
        }

        db.collection("chats").document(chatId)
                .collection("messages").document(messageId)
                .update(updates)
                .addOnFailureListener(e -> Log.e("ChatActivity", "Error updating status", e));
    }

    private String generateChatId(String user1, String user2) {
        return user1.compareTo(user2) < 0 ? user1 + "_" + user2 : user2 + "_" + user1;
    }

    private void scrollToBottom() {
        if (messages.size() > 0) {
            chatRecyclerView.postDelayed(() ->
                    chatRecyclerView.smoothScrollToPosition(messages.size() - 1), 100);
        }
    }

    private void showAttachmentOptions() {
        // Implement attachment options
    }

    private void toggleEmojiKeyboard() {
        // Implement emoji keyboard
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messagesListener != null) messagesListener.remove();
        if (chatListener != null) chatListener.remove();
    }
}