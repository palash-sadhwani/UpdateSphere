package com.example.chat.presentation.status;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.chat.data.repository.StatusRepository;

public class CreateStatusViewModel extends ViewModel {
    public enum Status {
        IDLE,
        LOADING,
        SUCCESS,
        ERROR
    }

    public static class CreateStatusState {
        private final Status status;
        private final String errorMessage;

        public CreateStatusState(Status status, String errorMessage) {
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

    private final StatusRepository repository = new StatusRepository();
    private final MutableLiveData<Event<CreateStatusState>> state = new MutableLiveData<>(
            new Event<>(new CreateStatusState(Status.IDLE, null))
    );
    private boolean operationInProgress;

    public LiveData<Event<CreateStatusState>> getState() {
        return state;
    }

    public void createStatus(String userId, String text, Uri imageUri) {
        if (operationInProgress || userId == null || userId.isEmpty()) {
            return;
        }

        operationInProgress = true;
        state.setValue(new Event<>(new CreateStatusState(Status.LOADING, null)));

        if (imageUri != null) {
            repository.uploadImage(imageUri, new StatusRepository.ImageUploadListener() {
                @Override
                public void onSuccess(String imageUrl) {
                    saveStatus(userId, text, imageUrl);
                }

                @Override
                public void onError(String errorMessage) {
                    publishError(errorMessage);
                }
            });
        } else {
            saveStatus(userId, text, null);
        }
    }

    private void saveStatus(String userId, String text, String imageUrl) {
        repository.saveStatus(userId, text, imageUrl)
                .addOnSuccessListener(unused -> publishSuccess())
                .addOnFailureListener(error -> publishError(error.getMessage()));
    }

    private void publishSuccess() {
        operationInProgress = false;
        state.postValue(new Event<>(new CreateStatusState(Status.SUCCESS, null)));
    }

    private void publishError(String errorMessage) {
        operationInProgress = false;
        state.postValue(new Event<>(new CreateStatusState(
                Status.ERROR,
                errorMessage != null ? errorMessage : "Failed to create status"
        )));
    }

    public static class Event<T> {
        private final T content;
        private boolean consumed;

        public Event(T content) {
            this.content = content;
        }

        public T getContent() {
            return content;
        }

        public boolean consume() {
            if (consumed) {
                return false;
            }
            consumed = true;
            return true;
        }
    }
}
