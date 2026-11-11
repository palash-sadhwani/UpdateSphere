//package com.example.chat;
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.fragment.app.Fragment;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
//
//import com.google.android.material.floatingactionbutton.FloatingActionButton;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.firestore.DocumentSnapshot;
//import com.google.firebase.firestore.EventListener;
//import com.google.firebase.firestore.FirebaseFirestore;
//import com.google.firebase.firestore.FirebaseFirestoreException;
//import com.google.firebase.firestore.ListenerRegistration;
//import com.google.firebase.firestore.Query;
//import com.google.firebase.firestore.QuerySnapshot;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//public class ChatsFragment extends Fragment implements ChatListAdapter.OnChatClickListener {
//    private RecyclerView chatsRecyclerView;
//    private ChatListAdapter chatAdapter;
//    private List<Chat> chatList = new ArrayList<>();
//    private Map<String, User> userMap = new HashMap<>();
//    private FirebaseFirestore firestore;
//    private String currentUserId;
//    private TextView emptyView;
//    private SwipeRefreshLayout swipeRefreshLayout;
//    private ListenerRegistration chatsListener;
//    private FloatingActionButton fabNewChat;
//
//    @Override
//    public void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        firestore = FirebaseFirestore.getInstance();
//        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
//    }
//
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.fragment_chats, container, false);
//        initViews(view);
//        setupAdapter();
//        setupListeners();
//        loadChats();
//        return view;
//    }
//
//    private void initViews(View view) {
//        chatsRecyclerView = view.findViewById(R.id.chatsRecyclerView);
//        emptyView = view.findViewById(R.id.emptyView);
//        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
//        fabNewChat = view.findViewById(R.id.fabNewChat);
//
//        chatsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
//    }
//
//    private void setupListeners() {
//        swipeRefreshLayout.setOnRefreshListener(() -> {
//            loadChats();
//            swipeRefreshLayout.setRefreshing(false);
//        });
//
//        fabNewChat.setOnClickListener(v -> openNewChatActivity());
//    }
//
//    private void setupAdapter() {
//        chatAdapter = new ChatListAdapter(
//                requireContext(),
//                chatList,
//                currentUserId,
//                userMap
//        );
//        chatAdapter.setOnChatClickListener(this);
//        chatsRecyclerView.setAdapter(chatAdapter);
//    }
//
//    private void loadChats() {
//        if (chatsListener != null) {
//            chatsListener.remove();
//        }
//
//        chatsListener = firestore.collection("chats")
//                .whereArrayContains("participants", currentUserId)
//                .orderBy("timestamp", Query.Direction.DESCENDING)
//                .addSnapshotListener((value, error) -> {
//                    swipeRefreshLayout.setRefreshing(false);
//
//                    if (error != null) {
//                        showEmptyView(true, "Error loading chats");
//                        Log.e("ChatsFragment", "Error loading chats", error);
//                        return;
//                    }
//
//                    if (value != null && !value.isEmpty()) {
//                        List<Chat> tempList = new ArrayList<>();
//                        userMap.clear(); // Clear previous user data
//
//                        for (DocumentSnapshot snapshot : value.getDocuments()) {
//                            try {
//                                Chat chat = snapshot.toObject(Chat.class);
//                                if (chat != null) {
//                                    chat.setChatId(snapshot.getId());
//                                    tempList.add(chat);
//                                    loadUserData(chat.getParticipants());
//                                }
//                            } catch (Exception e) {
//                                Log.e("ChatsFragment", "Error parsing chat", e);
//                            }
//                        }
//
//                        chatList.clear();
//                        chatList.addAll(tempList);
//                        updateEmptyView();
//                        chatAdapter.notifyDataSetChanged();
//                    } else {
//                        showEmptyView(true, "No chats available");
//                    }
//                });
//    }
//
//    private void loadUserData(List<String> participantIds) {
//        for (String userId : participantIds) {
//            if (!userId.equals(currentUserId) {
//                firestore.collection("users").document(userId).get()
//                        .addOnSuccessListener(documentSnapshot -> {
//                            if (documentSnapshot.exists()) {
//                                User user = documentSnapshot.toObject(User.class);
//                                if (user != null) {
//                                    userMap.put(userId, user);
//                                    chatAdapter.notifyDataSetChanged();
//                                }
//                            }
//                        })
//                        .addOnFailureListener(e ->
//                                Log.e("ChatsFragment", "Error loading user data", e));
//            }
//        }
//    }
//
//
////    private void loadChats() {
////        // Remove previous listener to avoid duplicates
////        if (chatsListener != null) {
////            chatsListener.remove();
////        }
////
////        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
////
////        chatsListener = firestore.collection("chats")
////                .whereArrayContains("participants", currentUserId)
////                .orderBy("timestamp", Query.Direction.DESCENDING)
////                .addSnapshotListener((value, error) -> {
////                    if (error != null) {
////                        showEmptyView(true, "Error loading chats");
////                        Log.e("ChatsFragment", "Error loading chats", error);
////                        return;
////                    }
////
////                    if (value != null && !value.isEmpty()) {
////                        List<Chat> tempList = new ArrayList<>();
////                        for (DocumentSnapshot snapshot : value.getDocuments()) {
////                            try {
////                                Chat chat = snapshot.toObject(Chat.class);
////                                if (chat != null) {
////                                    chat.setChatId(snapshot.getId());
////                                    tempList.add(chat);
////                                    // Load user data for participants
////                                    loadUserData(chat.getParticipants());
////                                }
////                            } catch (Exception e) {
////                                Log.e("ChatsFragment", "Error parsing chat", e);
////                            }
////                        }
////
////                        chatList.clear();
////                        chatList.addAll(tempList);
////                        updateEmptyView();
////                        chatAdapter.notifyDataSetChanged();
////                    } else {
////                        showEmptyView(true, "No chats available");
////                    }
////                });
////    }
////
////    private void loadUserData(List<String> participantIds) {
////        for (String userId : participantIds) {
////            if (!userId.equals(currentUserId) && !userMap.containsKey(userId)) {
////                firestore.collection("users").document(userId)
////                        .addSnapshotListener((snapshot, error) -> {
////                            if (error != null || snapshot == null || !snapshot.exists()) return;
////
////                            User user = snapshot.toObject(User.class);
////                            if (user != null) {
////                                userMap.put(userId, user);
////                                chatAdapter.notifyDataSetChanged();
////                            }
////                        });
////            }
////        }
////    }
//
//    private void showEmptyView(boolean show, String message) {
//        if (emptyView != null) {
//            emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
//            emptyView.setText(message);
//        }
//    }
//
//    private void updateEmptyView() {
//        showEmptyView(chatList.isEmpty(), "No chats available");
//    }
//
//    private void openNewChatActivity() {
////        fabNewChat.setOnClickListener(v -> {
//            // Add null check and animation
////            if (getActivity() != null) {
//                Intent intent = new Intent(getActivity(), UserActivity.class);
//                startActivity(intent);
////                getActivity().overridePendingTransition(R.anim.slide_up, R.anim.stay);
////            }
////        });
//    }
//
//    @Override
//    public void onChatClick(Chat chat) {
//        Intent intent = new Intent(getActivity(), ChatActivity.class);
//        intent.putExtra("chatId", chat.getChatId());
//        intent.putExtra("receiverId", chat.getOtherParticipantId(currentUserId));
//        startActivity(intent);
//    }
//
//    @Override
//    public void onChatLongClick(Chat chat) {
//        // Handle long click (e.g., show chat options)
//    }
//
//    @Override
//    public void onDestroyView() {
//        super.onDestroyView();
//        if (chatsListener != null) chatsListener.remove();
//    }
//}
//
package com.example.chat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.example.chat.presentation.chat.ChatListViewModel;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatsFragment extends Fragment implements ChatListAdapter.OnChatClickListener {
    private RecyclerView chatsRecyclerView;
    private ChatListAdapter chatAdapter;
    private List<Chat> chatList = new ArrayList<>();
    private Map<String, User> userMap = new HashMap<>();
    private ChatListViewModel chatListViewModel;
    private String currentUserId;
    private TextView emptyView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FloatingActionButton fabNewChat;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            return;
        }
        currentUserId = auth.getCurrentUser().getUid();

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chats, container, false);
        if (currentUserId == null) {
            if (isAdded()) {
                requireActivity().finish();
            }
            return view;
        }
        initViews(view);
        setupAdapter();
        setupListeners();
        chatListViewModel = new androidx.lifecycle.ViewModelProvider(this)
                .get(ChatListViewModel.class);
        observeChatList();
        chatListViewModel.start(currentUserId);
        return view;
    }

    private void initViews(View view) {
        chatsRecyclerView = view.findViewById(R.id.chatsRecyclerView);
        emptyView = view.findViewById(R.id.emptyView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        fabNewChat = view.findViewById(R.id.fabNewChat);

        chatsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    private void setupListeners() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            chatListViewModel.refresh();
        });

        fabNewChat.setOnClickListener(v -> openNewChatActivity());
    }

    private void setupAdapter() {
        chatAdapter = new ChatListAdapter(
                requireContext(),
                chatList,
                currentUserId,
                userMap
        );
        chatAdapter.setOnChatClickListener(this);
        chatsRecyclerView.setAdapter(chatAdapter);
    }

    private void observeChatList() {
        chatListViewModel.getState().observe(getViewLifecycleOwner(), state -> {
            chatList.clear();
            chatList.addAll(state.getChats());
            userMap.clear();
            userMap.putAll(state.getUsers());
            chatAdapter.notifyDataSetChanged();

            if (state.getStatus() == ChatListViewModel.Status.LOADING) {
                swipeRefreshLayout.setRefreshing(true);
            } else {
                swipeRefreshLayout.setRefreshing(false);
            }

            if (state.getStatus() == ChatListViewModel.Status.ERROR) {
                if (state.getErrorEvent() != null
                        && state.getErrorEvent().getContentIfNotHandled() != null) {
                    showEmptyView(true, state.getErrorMessage());
                }
            } else if (state.getChats().isEmpty()) {
                showEmptyView(true, "No chats available");
            } else {
                updateEmptyView();
            }
        });
    }



//private void loadUserData(List<String> participantIds) {
//    for (String userId : participantIds) {
//        if (!userId.equals(currentUserId) && !userMap.containsKey(userId)) {
//            firestore.collection("users").document(userId)
//                    .addSnapshotListener((snapshot, error) -> {
//                        if (snapshot != null && snapshot.exists()) {
//                            User user = snapshot.toObject(User.class);
//                            if (user != null) {
//                                userMap.put(userId, user);
//                                chatAdapter.notifyDataSetChanged();
//                            }
//                        }
//                    });
//        }// When user comes online
//
//    }
//}
    private void showEmptyView(boolean show, String message) {
        if (emptyView != null) {
            emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
            emptyView.setText(message);
        }
    }

    private void updateEmptyView() {
        showEmptyView(chatList.isEmpty(), "No chats available");
    }

    private void openNewChatActivity() {
        Intent intent = new Intent(getActivity(), UserActivity.class);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }

    @Override
    public void onChatClick(Chat chat) {
        Intent intent = new Intent(getActivity(), ChatActivity.class);
        intent.putExtra("chatId", chat.getChatId());
        intent.putExtra("receiverId", chat.getOtherParticipantId(currentUserId));
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }

    @Override
    public void onChatLongClick(Chat chat) {
        // Handle long click (e.g., show chat options)
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}