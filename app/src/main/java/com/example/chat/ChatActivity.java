package com.example.chat;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chat.data.repository.ChatRepository;
import com.example.chat.presentation.chat.ChatViewModel;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {
    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private ImageButton sendButton, attachButton, emojiButton, voiceButton;
    private List<Message> messages = new ArrayList<>();
    private ChatAdapter chatAdapter;
    private ChatViewModel chatViewModel;
    private String currentUserId, receiverId, chatId;
    private TextView chatName, chatStatus;
    private CircleImageView chatAvatar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatViewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends androidx.lifecycle.ViewModel> T create(@NonNull Class<T> modelClass) {
                return modelClass.cast(new ChatViewModel(new ChatRepository()));
            }
        }).get(ChatViewModel.class);

        initializeViews();
        setupUserData();
        setupRecyclerView();
        observeChatState();
        setupListeners();
    }

    private void initializeViews() {
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        attachButton = findViewById(R.id.attachButton);
        emojiButton = findViewById(R.id.emojiButton);
        voiceButton = findViewById(R.id.voiceButton);
        chatName = findViewById(R.id.chatName);
        chatStatus = findViewById(R.id.chatStatus);
        chatAvatar = findViewById(R.id.chatAvatar);

        findViewById(R.id.backButton).setOnClickListener(v -> finish());
    }

    private void setupUserData() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        receiverId = getIntent().getStringExtra("receiverId");
        if (receiverId == null || receiverId.isEmpty()) {
            finish();
            return;
        }

        chatId = generateChatId(currentUserId, receiverId);
        chatViewModel.startChat(currentUserId, receiverId, chatId);
        chatViewModel.markChatAsRead();
    }

    private void observeChatState() {
        chatViewModel.getChatState().observe(this, state -> {
            if (state.getStatus() == ChatViewModel.Status.ERROR) {
                if (state.getErrorMessage() != null) {
                    Toast.makeText(this, state.getErrorMessage(), Toast.LENGTH_SHORT).show();
                }
                return;
            }

            messages.clear();
            messages.addAll(state.getMessages());
            if (chatAdapter != null) {
                chatAdapter.notifyDataSetChanged();
                scrollToBottom();
            }
        });

        chatViewModel.getChatUserState().observe(this, state -> {
            chatName.setText(state.getName());
            chatStatus.setText(state.isOnline() ? "online" : "last seen recently");
            chatStatus.setTextColor(state.isOnline()
                    ? ContextCompat.getColor(this, R.color.online_green)
                    : ContextCompat.getColor(this, R.color.offline_gray));

            if (state.getAvatarUrl() != null && !state.getAvatarUrl().isEmpty()) {
                Glide.with(this)
                        .load(state.getAvatarUrl())
                        .placeholder(R.drawable.default_avatar)
                        .into(chatAvatar);
            }

            if (state.getErrorMessage() != null) {
                Log.e("ChatActivity", state.getErrorMessage());
            }
        });

        chatViewModel.getSendState().observe(this, state -> {
            if (!state.consume()) {
                return;
            }

            if (state.getStatus() == ChatViewModel.Status.SUCCESS) {
                messageInput.setText("");
                scrollToBottom();
            } else if (state.getStatus() == ChatViewModel.Status.ERROR) {
                Toast.makeText(this, state.getErrorMessage(), Toast.LENGTH_SHORT).show();
                Log.e("ChatActivity", "Message send failed: " + state.getErrorMessage());
            }
        });
    }

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter(messages, currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);
    }

    private void setupListeners() {
        sendButton.setOnClickListener(v -> sendMessage());

        messageInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = s.length() > 0;
                sendButton.setVisibility(hasText ? View.VISIBLE : View.GONE);
                voiceButton.setVisibility(hasText ? View.GONE : View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        attachButton.setOnClickListener(v -> showAttachmentOptions());
        emojiButton.setOnClickListener(v -> toggleEmojiKeyboard());
    }

    private void markChatAsRead() {
        chatViewModel.markChatAsRead();
    }

    @Override
    protected void onResume() {
        super.onResume();
        markChatAsRead();
        chatViewModel.markAllMessagesAsRead();
    }

    private void sendMessage() {
        String content = messageInput.getText().toString().trim();
        if (content.isEmpty()) {
            return;
        }

        chatViewModel.sendMessage(content);
    }

    private String generateChatId(String user1, String user2) {
        return user1.compareTo(user2) < 0 ? user1 + "_" + user2 : user2 + "_" + user1;
    }

    private void scrollToBottom() {
        if (!messages.isEmpty()) {
            chatRecyclerView.postDelayed(
                    () -> chatRecyclerView.smoothScrollToPosition(messages.size() - 1),
                    100
            );
        }
    }

    private void showAttachmentOptions() {
        // Implement attachment options
    }

    private void toggleEmojiKeyboard() {
        // Implement emoji keyboard
    }
}
