package com.example.chat;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Chat {
    private String chatId;
    private List<String> participants;
    private String lastMessage;
    private Timestamp timestamp;
    private String lastMessageSenderId;
    private Map<String, Boolean> readStatus;
    private int unreadCount;

    public Chat() {}

    public Chat(String chatId, List<String> participants, String lastMessage,
                Timestamp timestamp, String lastMessageSenderId, int unreadCount) {
        this.chatId = chatId;
        this.participants = participants;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
        this.lastMessageSenderId = lastMessageSenderId;
        this.unreadCount = unreadCount;
    }

    // Getters and Setters
    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }
    public List<String> getParticipants() { return participants; }
    public void setParticipants(List<String> participants) { this.participants = participants; }
    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    public String getLastMessageSenderId() { return lastMessageSenderId; }
    public void setLastMessageSenderId(String lastMessageSenderId) { this.lastMessageSenderId = lastMessageSenderId; }
    public Map<String, Boolean> getReadStatus() { return readStatus; }
    public void setReadStatus(Map<String, Boolean> readStatus) { this.readStatus = readStatus; }
    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }

    @Exclude
    public long getTimestampMillis() {
        return timestamp != null ? timestamp.getSeconds() * 1000 : 0;
    }

    public boolean isRead(String userId) {
        return readStatus != null && readStatus.containsKey(userId) && readStatus.get(userId);
    }

    public void markAsRead(String userId) {
        if (readStatus == null) readStatus = new HashMap<>();
        readStatus.put(userId, true);
    }

    public String getDisplayName(String currentUserId, Map<String, User> userMap) {
        for (String participantId : participants) {
            if (!participantId.equals(currentUserId)) {
                User user = userMap.get(participantId);
                if (user != null) return user.getName();
            }
        }
        return "Unknown";
    }

    public String getDisplayImage(String currentUserId, Map<String, User> userMap) {
        for (String participantId : participants) {
            if (!participantId.equals(currentUserId)) {
                User user = userMap.get(participantId);
                if (user != null) return user.getProfileImage();
            }
        }
        return "";
    }

    public String getOtherParticipantId(String currentUserId) {
        if (participants == null || participants.size() < 2) return "";
        return participants.get(0).equals(currentUserId) ? participants.get(1) : participants.get(0);
    }

    public String getFormattedTime() {
        if (timestamp == null) return "";
        long messageTime = getTimestampMillis();
        long now = System.currentTimeMillis();
        long diff = now - messageTime;

        if (diff < 24 * 60 * 60 * 1000) {
            return android.text.format.DateFormat.format("hh:mm a", messageTime).toString();
        } else if (diff < 7 * 24 * 60 * 60 * 1000) {
            return android.text.format.DateFormat.format("EEE", messageTime).toString();
        } else {
            return android.text.format.DateFormat.format("MMM d", messageTime).toString();
        }
    }

    public boolean hasUnreadMessages(String currentUserId) {
        return readStatus == null || !readStatus.getOrDefault(currentUserId, false);
    }

    public String getLastMessageDisplay(String currentUserId) {
        return lastMessageSenderId.equals(currentUserId) ? "You: " + lastMessage : lastMessage;
    }

    public void updateLastMessage(String message, String senderId) {
        this.lastMessage = message;
        this.lastMessageSenderId = senderId;
        this.timestamp = Timestamp.now();
    }
}