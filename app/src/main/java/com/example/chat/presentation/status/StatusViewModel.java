package com.example.chat.presentation.status;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.chat.data.repository.StatusRepository;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatusViewModel extends ViewModel {
    public enum Status {
        IDLE,
        LOADING,
        SUCCESS,
        ERROR
    }

    public static class StatusState {
        private final Status status;
        private final String errorMessage;

        public StatusState(Status status, String errorMessage) {
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

    private final StatusRepository repository;
    private final MutableLiveData<List<com.example.chat.Status>> statuses =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> currentUserName = new MutableLiveData<>();
    private final MutableLiveData<StatusState> state =
            new MutableLiveData<>(new StatusState(Status.IDLE, null));
    private final MutableLiveData<Boolean> deleteCompleted = new MutableLiveData<>();

    private String currentUserId;
    private boolean started;

    public StatusViewModel() {
        repository = new StatusRepository();
    }

    public LiveData<List<com.example.chat.Status>> getStatuses() {
        return statuses;
    }

    public LiveData<String> getCurrentUserName() {
        return currentUserName;
    }

    public LiveData<StatusState> getState() {
        return state;
    }

    public LiveData<Boolean> getDeleteCompleted() {
        return deleteCompleted;
    }

    public void start(String userId) {
        if (userId == null || userId.isEmpty()) {
            return;
        }

        if (started && userId.equals(currentUserId)) {
            return;
        }

        currentUserId = userId;
        started = true;
        state.setValue(new StatusState(Status.LOADING, null));

        repository.loadCurrentUser(userId)
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUserName.postValue(documentSnapshot.getString("name"));
                        publishStatuses(statuses.getValue());
                    }
                })
                .addOnFailureListener(error ->
                        publishError(error.getMessage(), "Failed to load user")
                );

        observeStatuses();
    }

    public void refresh() {
        if (currentUserId != null) {
            observeStatuses();
        }
    }

    public void stop() {
        repository.removeStatusesListener();
    }

    public void deleteStatus(com.example.chat.Status statusToDelete) {
        if (currentUserId == null || statusToDelete == null) {
            return;
        }

        repository.getUserStatuses(currentUserId)
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        return;
                    }

                    List<Map<String, Object>> savedStatuses =
                            (List<Map<String, Object>>) documentSnapshot.get("statuses");
                    if (savedStatuses == null) {
                        return;
                    }

                    List<Map<String, Object>> updatedList = new ArrayList<>();
                    for (Map<String, Object> status : savedStatuses) {
                        long timestamp = status.get("timestamp") != null
                                ? (long) status.get("timestamp")
                                : -1;
                        if (timestamp != statusToDelete.getTimestamp()) {
                            updatedList.add(status);
                        }
                    }

                    repository.updateUserStatuses(currentUserId, updatedList)
                            .addOnSuccessListener(unused -> deleteCompleted.postValue(true))
                            .addOnFailureListener(error ->
                                    publishError(error.getMessage(), "Failed to delete status")
                            );
                })
                .addOnFailureListener(error ->
                        publishError(error.getMessage(), "Failed to delete status")
                );
    }

    private void observeStatuses() {
        repository.observeStatuses(this::handleStatuses);
    }

    private void handleStatuses(QuerySnapshot value, FirebaseFirestoreException error) {
        if (error != null) {
            publishError(error.getMessage(), "Error loading statuses");
            return;
        }

        List<com.example.chat.Status> newStatusList = new ArrayList<>();
        Map<String, com.example.chat.Status> userStatusMap = new HashMap<>();

        com.example.chat.Status myStatus = new com.example.chat.Status(
                currentUserId,
                currentUserName.getValue() != null ? currentUserName.getValue() : "My Status",
                null,
                "Tap to add status"
        );
        newStatusList.add(myStatus);

        if (value != null) {
            for (DocumentSnapshot doc : value.getDocuments()) {
                String userId = doc.getString("userId");
                String userName = doc.getString("userName");
                List<Map<String, Object>> savedStatuses =
                        (List<Map<String, Object>>) doc.get("statuses");

                if (savedStatuses != null && !savedStatuses.isEmpty()) {
                    Map<String, Object> latestStatus = savedStatuses.get(0);
                    String text = (String) latestStatus.get("text");
                    String imageUrl = (String) latestStatus.get("imageUrl");
                    long timestamp = (long) latestStatus.get("timestamp");

                    com.example.chat.Status status = new com.example.chat.Status(
                            doc.getId(),
                            userId,
                            userName,
                            imageUrl,
                            timestamp,
                            text
                    );
                    status.updateTime();

                    if (!userStatusMap.containsKey(userId)) {
                        userStatusMap.put(userId, status);
                    }
                }
            }
        }

        newStatusList.addAll(userStatusMap.values());
        Collections.sort(newStatusList.subList(1, newStatusList.size()),
                (first, second) -> Long.compare(second.getTimestamp(), first.getTimestamp()));

        publishStatuses(newStatusList);
    }

    private void publishStatuses(List<com.example.chat.Status> newStatuses) {
        if (newStatuses != null) {
            statuses.postValue(new ArrayList<>(newStatuses));
        }
        state.postValue(new StatusState(Status.SUCCESS, null));
    }

    private void publishError(String message, String fallback) {
        state.postValue(new StatusState(
                Status.ERROR,
                message != null ? message : fallback
        ));
    }

    @Override
    protected void onCleared() {
        repository.removeStatusesListener();
        super.onCleared();
    }
}
