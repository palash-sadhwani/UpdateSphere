package com.example.chat.presentation.chat;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.chat.Message;
import com.example.chat.data.repository.ChatRepository;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChatViewModel extends ViewModel {
    public enum Status {
        IDLE,
        LOADING,
        SUCCESS,
        ERROR
    }

    public static class ChatState {
        private final Status status;
        private final List<Message> messages;
        private final String errorMessage;

        public ChatState(Status status, List<Message> messages, String errorMessage) {
            this.status = status;
            this.messages = messages;
            this.errorMessage = errorMessage;
        }

        public Status getStatus() {
            return status;
        }

        public List<Message> getMessages() {
            return messages;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    public static class ChatUserState {
        private final String name;
        private final boolean online;
        private final String avatarUrl;
        private final String errorMessage;

        public ChatUserState(String name, boolean online, String avatarUrl, String errorMessage) {
            this.name = name;
            this.online = online;
            this.avatarUrl = avatarUrl;
            this.errorMessage = errorMessage;
        }

        public String getName() {
            return name;
        }

        public boolean isOnline() {
            return online;
        }

        public String getAvatarUrl() {
            return avatarUrl;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    public static class SendState {
        private final Status status;
        private final String errorMessage;
        private boolean consumed;

        public SendState(Status status, String errorMessage) {
            this.status = status;
            this.errorMessage = errorMessage;
        }

        public Status getStatus() {
            return status;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public boolean consume() {
            if (consumed) {
                return false;
            }
            consumed = true;
            return true;
        }
    }

    private final ChatRepository repository;
    private final MutableLiveData<ChatState> chatState = new MutableLiveData<>(
            new ChatState(Status.IDLE, new ArrayList<>(), null)
    );
    private final MutableLiveData<ChatUserState> chatUserState = new MutableLiveData<>();
    private final MutableLiveData<SendState> sendState = new MutableLiveData<>();
    private final List<Message> messages = new ArrayList<>();

    private ListenerRegistration messagesListener;
    private ListenerRegistration chatUserListener;
    private String currentUserId;
    private String receiverId;
    private String chatId;

    public ChatViewModel(ChatRepository repository) {
        this.repository = repository;
    }

    public LiveData<ChatState> getChatState() {
        return chatState;
    }

    public LiveData<ChatUserState> getChatUserState() {
        return chatUserState;
    }

    public LiveData<SendState> getSendState() {
        return sendState;
    }

    public void startChat(String currentUserId, String receiverId, String chatId) {
        if (chatId.equals(this.chatId)
                && currentUserId.equals(this.currentUserId)
                && receiverId.equals(this.receiverId)
                && messagesListener != null) {
            return;
        }

        stopListeners();
        this.currentUserId = currentUserId;
        this.receiverId = receiverId;
        this.chatId = chatId;
        messages.clear();
        chatState.setValue(new ChatState(Status.LOADING, new ArrayList<>(messages), null));

        chatUserListener = repository.observeChatUser(receiverId, this::handleChatUser);
        messagesListener = repository.observeMessages(chatId, this::handleMessages);
    }

    public void markChatAsRead() {
        if (!hasChat()) {
            return;
        }

        repository.markChatAsRead(chatId, currentUserId)
                .addOnFailureListener(error -> publishError(error.getMessage()));
    }

    public void markAllMessagesAsRead() {
        if (!hasChat()) {
            return;
        }

        for (Message message : new ArrayList<>(messages)) {
            if (!currentUserId.equals(message.getSenderId()) && !message.isRead()) {
                markMessageAsRead(message);
            }
        }
        markChatAsRead();
    }

    public void sendMessage(String content) {
        if (!hasChat() || content == null || content.trim().isEmpty()) {
            return;
        }

        Message message = new Message(
                currentUserId,
                receiverId,
                chatId,
                content.trim()
        );
        message.setTimestamp(new Date(System.currentTimeMillis()));
        message.setStatus(Message.STATUS_SENT);
        sendState.setValue(new SendState(Status.LOADING, null));

        repository.sendMessage(chatId, currentUserId, receiverId, message)
                .addOnSuccessListener(messageId -> {
                    updateMessageStatus(messageId, true, false);
                    sendState.postValue(new SendState(Status.SUCCESS, null));
                })
                .addOnFailureListener(error -> {
                    sendState.postValue(new SendState(
                            Status.ERROR,
                            getErrorMessage(error, "Failed to send message")
                    ));
                });
    }

    private void handleChatUser(DocumentSnapshot snapshot, FirebaseFirestoreException error) {
        if (error != null) {
            chatUserState.postValue(new ChatUserState(
                    "Unknown",
                    false,
                    null,
                    getErrorMessage(error, "Failed to load chat user")
            ));
            return;
        }

        if (snapshot != null && snapshot.exists()) {
            String name = snapshot.getString("name");
            Boolean isOnline = snapshot.getBoolean("isOnline");
            chatUserState.postValue(new ChatUserState(
                    name != null ? name : "Unknown",
                    isOnline != null && isOnline,
                    snapshot.getString("avatarUrl"),
                    null
            ));
        }
    }

    private void handleMessages(QuerySnapshot snapshots, FirebaseFirestoreException error) {
        if (error != null) {
            publishError(getErrorMessage(error, "Error loading messages"));
            return;
        }

        if (snapshots == null) {
            return;
        }

        for (DocumentChange change : snapshots.getDocumentChanges()) {
            Message message = change.getDocument().toObject(Message.class);
            message.setMessageId(change.getDocument().getId());

            if (change.getType() == DocumentChange.Type.ADDED) {
                messages.add(message);
                if (!currentUserId.equals(message.getSenderId()) && !message.isRead()) {
                    markMessageAsRead(message);
                }
            } else if (change.getType() == DocumentChange.Type.MODIFIED) {
                updateLocalMessage(message);
            }
        }

        publishMessages(Status.SUCCESS, null);
    }

    private void markMessageAsRead(Message message) {
        if (message.isRead() || currentUserId.equals(message.getSenderId())) {
            return;
        }

        repository.markMessageAsRead(chatId, message.getMessageId())
                .addOnSuccessListener(unused -> {
                    message.markAsRead();
                    publishMessages(Status.SUCCESS, null);
                })
                .addOnFailureListener(error -> publishError(error.getMessage()));
    }

    private void updateMessageStatus(String messageId, boolean isDelivered, boolean isRead) {
        repository.updateMessageStatus(chatId, messageId, isDelivered, isRead)
                .addOnFailureListener(error -> publishError(error.getMessage()));
    }

    private void updateLocalMessage(Message updatedMessage) {
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i).getMessageId().equals(updatedMessage.getMessageId())) {
                messages.set(i, updatedMessage);
                return;
            }
        }
    }

    private void publishMessages(Status status, String errorMessage) {
        chatState.postValue(new ChatState(
                status,
                new ArrayList<>(messages),
                errorMessage
        ));
    }

    private void publishError(String message) {
        publishMessages(Status.ERROR, getErrorMessage(message, "Chat operation failed"));
    }

    private String getErrorMessage(Exception error, String fallback) {
        return error != null && error.getMessage() != null ? error.getMessage() : fallback;
    }

    private String getErrorMessage(String message, String fallback) {
        return message != null && !message.isEmpty() ? message : fallback;
    }

    private boolean hasChat() {
        return currentUserId != null && receiverId != null && chatId != null;
    }

    private void stopListeners() {
        if (messagesListener != null) {
            messagesListener.remove();
            messagesListener = null;
        }
        if (chatUserListener != null) {
            chatUserListener.remove();
            chatUserListener = null;
        }
    }

    @Override
    protected void onCleared() {
        stopListeners();
        super.onCleared();
    }
}
