package com.example.chat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import de.hdodenhof.circleimageview.CircleImageView;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ViewHolder> {
    private final Context context;
    private final List<Chat> chatList;
    private final String currentUserId;
    private final Map<String, User> userMap;
    private OnChatClickListener listener;

    public interface OnChatClickListener {
        void onChatClick(Chat chat);
        void onChatLongClick(Chat chat);
    }

    public ChatListAdapter(Context context, List<Chat> chatList,
                           String currentUserId, Map<String, User> userMap) {
        this.context = context;
        this.chatList = chatList;
        this.currentUserId = currentUserId;
        this.userMap = userMap;
    }

    public void setOnChatClickListener(OnChatClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.chat_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Chat chat = chatList.get(position);
        String otherUserId = chat.getOtherParticipantId(currentUserId);
        User otherUser = userMap.get(otherUserId);

        if (otherUser != null) {
            // This is the critical part for showing the online indicator
            holder.onlineIndicator.setVisibility(otherUser.isOnline() ? View.VISIBLE : View.GONE);
            holder.onlineIndicator.setBackgroundResource(
                    otherUser.isOnline() ?
                            R.drawable.online_indicator :
                            R.drawable.offline_indicator
            );
        }
        // Set chat name
        holder.chatName.setText(chat.getDisplayName(currentUserId, userMap));

        // Set last message
        String lastMessage = formatLastMessage(chat);
        holder.lastMessage.setText(lastMessage);

        // Set formatted time
        holder.chatTime.setText(formatTimestamp(chat.getTimestamp()));

        // Handle unread count
        if (chat.getUnreadCount() > 0 && !chat.isRead(currentUserId)) {
            holder.unreadCount.setVisibility(View.VISIBLE);
            holder.unreadCount.setText(String.valueOf(chat.getUnreadCount()));
        } else {
            holder.unreadCount.setVisibility(View.GONE);
        }

        // Load avatar
        String imageUrl = chat.getDisplayImage(currentUserId, userMap);
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(imageUrl)
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.default_avatar)
                            .error(R.drawable.default_avatar))
                    .into(holder.chatAvatar);
        } else {
            holder.chatAvatar.setImageResource(R.drawable.default_avatar);
        }

        // Set click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onChatClick(chat);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onChatLongClick(chat);
            return true;
        });


    }

    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp.getSeconds() * 1000));
    }

    private String formatLastMessage(Chat chat) {
        String senderId = chat.getLastMessageSenderId();
        String message = chat.getLastMessage() != null ? chat.getLastMessage() : "";

        if (senderId == null || senderId.equals(currentUserId)) {
            return "You: " + message;
        } else {
            User sender = userMap.get(senderId);
            String senderName = sender != null ? sender.getName() : "User";
            return senderName + ": " + message;
        }
    }

    @Override
    public int getItemCount() {
        return chatList != null ? chatList.size() : 0;
    }

    public void updateChats(List<Chat> newChats) {
        if (newChats != null) {
            chatList.clear();
            chatList.addAll(newChats);
            notifyDataSetChanged();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final CircleImageView chatAvatar;
        final TextView chatName;
        final TextView lastMessage;
        final TextView chatTime;
        final TextView unreadCount;
        final View onlineIndicator;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            chatAvatar = itemView.findViewById(R.id.chatAvatar);
            chatName = itemView.findViewById(R.id.chatName);
            lastMessage = itemView.findViewById(R.id.lastMessage);
            chatTime = itemView.findViewById(R.id.chatTime);
            unreadCount = itemView.findViewById(R.id.unreadCount);
            onlineIndicator = itemView.findViewById(R.id.onlineIndicator);
        }
    }
}