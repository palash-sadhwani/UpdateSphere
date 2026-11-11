package com.example.chat.presentation.chat;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.chat.Chat;
import com.example.chat.User;
import com.example.chat.data.repository.ChatListRepository;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChatListViewModel extends ViewModel {
    public static class Event<T> {
        private final T content;
        private boolean handled;

        public Event(T content) {
            this.content = content;
        }

        public T getContentIfNotHandled() {
            if (handled) {
                return null;
            }
            handled = true;
            return content;
        }
    }

    public enum Status {
        IDLE,
        LOADING,
        SUCCESS,
        ERROR
    }

    public static class ChatListState {
        private final Status status;
        private final List<Chat> chats;
        private final Map<String, User> users;
        private final String errorMessage;
        private final Event<String> errorEvent;

        public ChatListState(
                Status status,
                List<Chat> chats,
                Map<String, User> users,
                String errorMessage,
                Event<String> errorEvent
        ) {
            this.status = status;
            this.chats = chats;
            this.users = users;
            this.errorMessage = errorMessage;
            this.errorEvent = errorEvent;
        }

        public Status getStatus() {
            return status;
        }

        public List<Chat> getChats() {
            return chats;
        }

        public Map<String, User> getUsers() {
            return users;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public Event<String> getErrorEvent() {
            return errorEvent;
        }
    }

    private final ChatListRepository repository = new ChatListRepository();
    private final MutableLiveData<ChatListState> state = new MutableLiveData<>(
            new ChatListState(Status.IDLE, new ArrayList<>(), new HashMap<>(), null, null)
    );
    private final List<Chat> chats = new ArrayList<>();
    private final Map<String, User> users = new HashMap<>();
    private final Set<String> pendingUserIds = new HashSet<>();
    private String currentUserId;
    private boolean observing;

    public LiveData<ChatListState> getState() {
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
        observing = true;
        state.setValue(newState(Status.LOADING, null));
        repository.observeChats(currentUserId, this::handleChats);
    }

    public void refresh() {
        if (currentUserId != null) {
            state.setValue(newState(Status.LOADING, null));
            repository.observeChats(currentUserId, this::handleChats);
        }
    }

    private void handleChats(QuerySnapshot value, Exception error) {
        if (error != null) {
            state.postValue(newState(Status.ERROR, "Error loading chats"));
            return;
        }

        List<Chat> loadedChats = new ArrayList<>();
        Set<String> participantIds = new HashSet<>();
        if (value != null) {
            for (DocumentSnapshot snapshot : value.getDocuments()) {
                try {
                    Chat chat = snapshot.toObject(Chat.class);
                    if (chat != null) {
                        chat.setChatId(snapshot.getId());
                        loadedChats.add(chat);
                        if (chat.getParticipants() != null) {
                            for (String participantId : chat.getParticipants()) {
                                if (!currentUserId.equals(participantId)) {
                                    participantIds.add(participantId);
                                }
                            }
                        }
                    }
                } catch (Exception exception) {
                    // Preserve the existing behavior of skipping malformed chat documents.
                }
            }
        }

        chats.clear();
        chats.addAll(loadedChats);
        pendingUserIds.clear();
        pendingUserIds.addAll(participantIds);
        users.keySet().retainAll(participantIds);
        publish(Status.SUCCESS, null);

        for (String participantId : participantIds) {
            if (!users.containsKey(participantId)) {
                loadUser(participantId);
            }
        }
    }

    private void loadUser(String participantId) {
        repository.loadUser(participantId, (snapshot, error) -> {
            pendingUserIds.remove(participantId);
            if (error == null && snapshot != null && snapshot.exists()) {
                User user = snapshot.toObject(User.class);
                if (user != null) {
                    users.put(participantId, user);
                }
            }
            publish(Status.SUCCESS, null);
        });
    }

    private void publish(Status status, String errorMessage) {
        state.postValue(newState(status, errorMessage));
    }

    private ChatListState newState(Status status, String errorMessage) {
        return new ChatListState(
                status,
                new ArrayList<>(chats),
                new HashMap<>(users),
                errorMessage,
                errorMessage == null ? null : new Event<>(errorMessage)
        );
    }

    @Override
    protected void onCleared() {
        repository.removeChatsListener();
        super.onCleared();
    }
}
