package com.example.chat.presentation.user;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.chat.User;
import com.example.chat.data.repository.UserRepository;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class UserViewModel extends ViewModel {
    public enum Status {
        IDLE,
        LOADING,
        SUCCESS,
        ERROR
    }

    public static class UserState {
        private final Status status;
        private final String errorMessage;

        public UserState(Status status, String errorMessage) {
            this.status = status;
            this.errorMessage = errorMessage;
        }

        public Status getStatus() {
            return status;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    private final UserRepository repository = new UserRepository();
    private final MutableLiveData<List<User>> users = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<UserState> state = new MutableLiveData<>(
            new UserState(Status.IDLE, null)
    );
    private String currentUserId;
    private boolean observing;

    public LiveData<List<User>> getUsers() {
        return users;
    }

    public LiveData<UserState> getState() {
        return state;
    }

    public void start(String currentUserId) {
        if (currentUserId == null || currentUserId.isEmpty()) {
            return;
        }

        if (observing && currentUserId.equals(this.currentUserId)) {
            return;
        }

        this.currentUserId = currentUserId;
        state.setValue(new UserState(Status.LOADING, null));
        observing = true;
        repository.observeUsers(this::handleUsers);
    }

    public void refresh() {
        if (currentUserId != null) {
            state.setValue(new UserState(Status.LOADING, null));
            repository.observeUsers(this::handleUsers);
        }
    }

    private void handleUsers(QuerySnapshot value, FirebaseFirestoreException error) {
        if (error != null) {
            state.postValue(new UserState(Status.ERROR, "Error loading users"));
            return;
        }

        List<User> loadedUsers = new ArrayList<>();
        if (value != null) {
            for (DocumentSnapshot doc : value.getDocuments()) {
                User user = doc.toObject(User.class);
                if (user != null && !currentUserId.equals(user.getUserId())) {
                    loadedUsers.add(user);
                }
            }
        }

        users.postValue(loadedUsers);
        state.postValue(new UserState(Status.SUCCESS, null));
    }

    @Override
    protected void onCleared() {
        repository.removeUsersListener();
        super.onCleared();
    }
}
