package com.example.chat;

import com.google.firebase.firestore.PropertyName;

public class User {
    private String userId;
    private String name;
    private String email;
    private String profileImage;
    private String status;
    @PropertyName("isOnline")
    private boolean isOnline;
    private long lastSeen;

    // Required empty constructor for Firebase
    public User() {}

    public User(String userId, String name, String email, String profileImage, String status) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.profileImage = profileImage;
        this.status = status;
        this.isOnline = false;
        this.lastSeen = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @PropertyName("isOnline")
    public boolean isOnline() {
        return isOnline;
    }

    @PropertyName("isOnline")
    public void setOnline(boolean online) {
        this.isOnline = online;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public String getFormattedLastSeen() {
        if (isOnline) {
            return "Online";
        } else {
            return "Last seen " + android.text.format.DateFormat.format("hh:mm a", lastSeen);
        }
    }
}