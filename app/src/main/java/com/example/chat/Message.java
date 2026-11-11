package com.example.chat;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
import java.util.Locale;

public class Message {
    // Message types
    public static final int TYPE_TEXT = 0;
    public static final int TYPE_IMAGE = 1;
    public static final int TYPE_AUDIO = 2;
    public static final int TYPE_VIDEO = 3;
    public static final int TYPE_LOCATION = 4;
    public static final int TYPE_DOCUMENT = 5;

    // Status constants
    public static final int STATUS_SENT = 0;
    public static final int STATUS_DELIVERED = 1;
    public static final int STATUS_READ = 2;
    public static final int STATUS_FAILED = -1;

    private String messageId;
    private String senderId;
    private String receiverId;
    private String chatId; // Added to track which chat this belongs to
    private String content;
    private @ServerTimestamp Date timestamp; // Using Firestore timestamp
    private int messageType;
    private int status; // Using status codes instead of separate booleans
    private String mediaUrl;
    private long audioDuration;
    private String thumbnailUrl; // For video/image thumbnails
    private double fileSize; // For documents/media size tracking
    private String fileName; // For documents
    private double latitude; // For location messages
    private double longitude; // For location messages


    // Required empty constructor for Firebase
    public Message() {}

    // Constructor for text messages
    public Message(String senderId, String receiverId, String chatId, String content) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.chatId = chatId;
        this.content = content;
        this.messageType = TYPE_TEXT;
        this.status = STATUS_SENT;
    }

    // Constructor for media messages
    public Message(String senderId, String receiverId, String chatId,
                   String mediaUrl, int messageType) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.chatId = chatId;
        this.mediaUrl = mediaUrl;
        this.messageType = messageType;
        this.status = STATUS_SENT;

        // Set content based on type
        switch (messageType) {
            case TYPE_IMAGE:
                this.content = "📷 Photo";
                break;
            case TYPE_AUDIO:
                this.content = "🎤 Audio";
                break;
            case TYPE_VIDEO:
                this.content = "🎥 Video";
                break;
            case TYPE_DOCUMENT:
                this.content = "📄 Document";
                break;
        }
    }

    // Getters and Setters
    public String getMessageId() {
        return messageId;
    }


    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public int getMessageType() {
        return messageType;
    }

    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public long getAudioDuration() {
        return audioDuration;
    }

    public void setAudioDuration(long audioDuration) {
        this.audioDuration = audioDuration;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public double getFileSize() {
        return fileSize;
    }

    public void setFileSize(double fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    // Helper methods
    @Exclude
    public boolean isImage() {
        return messageType == TYPE_IMAGE;
    }

    @Exclude
    public boolean isAudio() {
        return messageType == TYPE_AUDIO;
    }

    @Exclude
    public boolean isVideo() {
        return messageType == TYPE_VIDEO;
    }

    @Exclude
    public boolean isDocument() {
        return messageType == TYPE_DOCUMENT;
    }

    @Exclude
    public boolean isLocation() {
        return messageType == TYPE_LOCATION;
    }

    @Exclude
    public boolean isSent() {
        return status == STATUS_SENT;
    }

    @Exclude
    public boolean isDelivered() {
        return status == STATUS_DELIVERED;
    }

    @Exclude
    public boolean isRead() {
        return status == STATUS_READ;
    }

    @Exclude
    public boolean isFailed() {
        return status == STATUS_FAILED;
    }


//    @Exclude
//    public boolean isRead() {
//        return status == STATUS_READ;
//    }

    @Exclude
    public void markAsRead() {
        this.status = STATUS_READ;
    }
    @Exclude
    public void setRead(boolean read) {
        this.status = read ? STATUS_READ : STATUS_SENT;
    }
    @Exclude
    public String getFormattedTime() {
        if (timestamp != null) {
            return android.text.format.DateFormat.format("hh:mm a", timestamp).toString();
        }
        return "";
    }
    @Exclude
    public void setDelivered(boolean delivered) {
        this.status = delivered ? STATUS_DELIVERED : STATUS_SENT;
    }
    @Exclude
    public String getFormattedFileSize() {
        if (fileSize < 1024) {
            return String.format(Locale.getDefault(), "%.1f B", fileSize);
        } else if (fileSize < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1f KB", fileSize / 1024);
        } else {
            return String.format(Locale.getDefault(), "%.1f MB", fileSize / (1024 * 1024));
        }
    }

    @Exclude
    public String getFormattedDuration() {
        long seconds = audioDuration / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }
}