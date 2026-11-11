package com.example.chat.presentation.profile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.chat.data.repository.ProfileRepository;
import com.google.firebase.firestore.DocumentSnapshot;

public class ProfileViewModel extends ViewModel {
    public enum Operation {
        IDLE,
        LOADING_PROFILE,
        UPDATING_PROFILE,
        UPDATING_PASSWORD,
        LOGGING_OUT
    }

    public enum EventType {
        PROFILE_UPDATED,
        PASSWORD_UPDATED,
        LOGGED_OUT,
        ERROR
    }

    public static class Event {
        private final EventType type;
        private final String message;
        private boolean consumed;

        public Event(EventType type, String message) {
            this.type = type;
            this.message = message;
        }

        public EventType getType() {
            return type;
        }

        public String getMessage() {
            return message;
        }

        public boolean consume() {
            if (consumed) {
                return false;
            }
            consumed = true;
            return true;
        }
    }

    public static class ProfileState {
        private final String name;
        private final String email;
        private final Operation operation;
        private final Event event;

        public ProfileState(
                String name,
                String email,
                Operation operation,
                Event event
        ) {
            this.name = name;
            this.email = email;
            this.operation = operation;
            this.event = event;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }

        public Operation getOperation() {
            return operation;
        }

        public Event getEvent() {
            return event;
        }
    }

    private final ProfileRepository repository = new ProfileRepository();
    private final MutableLiveData<ProfileState> state = new MutableLiveData<>(
            new ProfileState(null, null, Operation.IDLE, null)
    );
    private boolean profileLoadInProgress;
    private boolean profileUpdateInProgress;
    private boolean passwordUpdateInProgress;
    private boolean logoutInProgress;
    private String profileName;
    private String profileEmail;

    public LiveData<ProfileState> getState() {
        return state;
    }

    public void loadProfile() {
        if (profileLoadInProgress || profileName != null || profileEmail != null
                || hasActiveOperation()) {
            return;
        }

        profileLoadInProgress = true;
        publish(Operation.LOADING_PROFILE, null);
        repository.loadCurrentProfile((email, snapshot, error) -> {
            profileLoadInProgress = false;
            if (error != null) {
                publish(Operation.IDLE, new Event(EventType.ERROR, "Failed to load profile"));
                return;
            }

            profileEmail = email;
            profileName = getProfileName(snapshot);
            publish(Operation.IDLE, null);
        });
    }

    public void updateProfile(String name, String email) {
        if (profileUpdateInProgress || hasActiveOperation()) {
            return;
        }

        profileUpdateInProgress = true;
        publish(Operation.UPDATING_PROFILE, null);
        repository.updateProfile(name, email, error -> {
            profileUpdateInProgress = false;
            if (error != null) {
                publish(Operation.IDLE, new Event(
                        EventType.ERROR,
                        "Failed to update profile: " + error.getMessage()
                ));
                return;
            }

            profileName = name;
            profileEmail = email;
            publish(Operation.IDLE, new Event(
                    EventType.PROFILE_UPDATED,
                    "Profile updated successfully"
            ));
        });
    }

    public void updatePassword(String currentPassword, String newPassword) {
        if (passwordUpdateInProgress || hasActiveOperation()) {
            return;
        }

        passwordUpdateInProgress = true;
        publish(Operation.UPDATING_PASSWORD, null);
        repository.updatePassword(currentPassword, newPassword, error -> {
            passwordUpdateInProgress = false;
            if (error != null) {
                publish(Operation.IDLE, new Event(
                        EventType.ERROR,
                        "Failed to update password: " + error
                ));
                return;
            }

            publish(Operation.IDLE, new Event(
                    EventType.PASSWORD_UPDATED,
                    "Password updated successfully"
            ));
        });
    }

    public void logout() {
        if (logoutInProgress || hasActiveOperation()) {
            return;
        }

        logoutInProgress = true;
        publish(Operation.LOGGING_OUT, null);
        repository.logout(error -> {
            logoutInProgress = false;
            publish(Operation.IDLE, new Event(EventType.LOGGED_OUT, null));
        });
    }

    private boolean hasActiveOperation() {
        return profileLoadInProgress
                || profileUpdateInProgress
                || passwordUpdateInProgress
                || logoutInProgress;
    }

    private String getProfileName(DocumentSnapshot snapshot) {
        if (snapshot != null && snapshot.exists()) {
            String name = snapshot.getString("name");
            if (name != null && !name.isEmpty()) {
                return name;
            }
        }
        return "No name set";
    }

    private void publish(Operation operation, Event event) {
        state.postValue(new ProfileState(profileName, profileEmail, operation, event));
    }
}
