package com.example.chat;

import android.os.Parcel;
import android.os.Parcelable;
import android.content.Intent;
import androidx.annotation.NonNull;
import java.util.Objects;

public class Status implements Parcelable {
    private String id;          // Document ID from Firestore
    private String userId;      // User ID who created the status
    private String userName;    // Display name of the user
    private String time;        // Formatted time string
    private String imageUrl;    // URL of status image
    private long timestamp;     // Timestamp in milliseconds
    private String statusText;  // Text content of status
    private boolean viewed;     // Whether the status has been viewed

    // Empty constructor required for Firestore
    public Status() {
        this.timestamp = System.currentTimeMillis();
        updateTime();
    }

    // Full constructor
    public Status(String id, String userId, String userName, String imageUrl,
                  long timestamp, String statusText) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
        this.statusText = statusText;
        this.viewed = false;
        updateTime();
    }

    // Simplified constructor for new statuses
    public Status(String userId, String userName, String imageUrl, String statusText) {
        this("", userId, userName, imageUrl, System.currentTimeMillis(), statusText);
    }

    // Parcelable implementation
    protected Status(Parcel in) {
        id = in.readString();
        userId = in.readString();
        userName = in.readString();
        time = in.readString();
        imageUrl = in.readString();
        timestamp = in.readLong();
        statusText = in.readString();
        viewed = in.readByte() != 0;
    }

    public static final Creator<Status> CREATOR = new Creator<Status>() {
        @Override
        public Status createFromParcel(Parcel in) {
            return new Status(in);
        }

        @Override
        public Status[] newArray(int size) {
            return new Status[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(userId);
        dest.writeString(userName);
        dest.writeString(time);
        dest.writeString(imageUrl);
        dest.writeLong(timestamp);
        dest.writeString(statusText);
        dest.writeByte((byte) (viewed ? 1 : 0));
    }

    // Getters and Setters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getTime() { return time; }
    public String getImageUrl() { return imageUrl; }
    public long getTimestamp() { return timestamp; }
    public String getStatusText() { return statusText; }
    public boolean isViewed() { return viewed; }

    public void setId(String id) { this.id = id; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setUserName(String userName) { this.userName = userName; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        updateTime();
    }
    public void setStatusText(String statusText) { this.statusText = statusText; }
    public void setViewed(boolean viewed) { this.viewed = viewed; }

    // Helper methods
    public static Status fromIntent(Intent intent) {
        return intent.getParcelableExtra("status");
    }

    public void markAsViewed() {
        this.viewed = true;
    }

    public void updateTime() {
        this.time = formatTime(this.timestamp);
    }

    private String formatTime(long timestamp) {
        long currentTime = System.currentTimeMillis();
        long diff = currentTime - timestamp;
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (seconds < 60) {
            return "Just now";
        } else if (minutes < 60) {
            return minutes + "m ago";
        } else if (hours < 24) {
            return hours + "h ago";
        } else if (days == 1) {
            return "Yesterday";
        } else {
            return days + "d ago";
        }
    }

    // Equality comparison
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Status status = (Status) o;
        return timestamp == status.timestamp &&
                viewed == status.viewed &&
                Objects.equals(id, status.id) &&
                Objects.equals(userId, status.userId) &&
                Objects.equals(userName, status.userName) &&
                Objects.equals(imageUrl, status.imageUrl) &&
                Objects.equals(statusText, status.statusText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, userName, time, imageUrl, timestamp, statusText, viewed);
    }

    @NonNull
    @Override
    public String toString() {
        return "Status{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", userName='" + userName + '\'' +
                ", time='" + time + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", timestamp=" + timestamp +
                ", statusText='" + statusText + '\'' +
                ", viewed=" + viewed +
                '}';
    }

    // Builder pattern for easier construction
    public static class Builder {
        private String id;
        private String userId;
        private String userName;
        private String imageUrl;
        private long timestamp;
        private String statusText;

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder setUserId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder setUserName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public Builder setTimestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder setStatusText(String statusText) {
            this.statusText = statusText;
            return this;
        }

        public Status build() {
            return new Status(id, userId, userName, imageUrl, timestamp, statusText);
        }
    }
}