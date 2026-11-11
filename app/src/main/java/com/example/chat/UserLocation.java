package com.example.chat;

import com.google.firebase.firestore.GeoPoint;

public class UserLocation {
    private String userId;
    private String userName;
    private GeoPoint geoPoint;
    private long timestamp;

    public UserLocation() {}

    public UserLocation(String userId, GeoPoint geoPoint, long timestamp) {
        this.userId = userId;
        this.geoPoint = geoPoint;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public GeoPoint getGeoPoint() { return geoPoint; }
    public long getTimestamp() { return timestamp; }

    public void setUserId(String userId) { this.userId = userId; }
    public void setUserName(String userName) { this.userName = userName; }
    public void setGeoPoint(GeoPoint geoPoint) { this.geoPoint = geoPoint; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}