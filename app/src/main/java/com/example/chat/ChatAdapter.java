package com.example.chat;



import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private List<Message> messages;
    private String currentUserId;

    public ChatAdapter(List<Message> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_sender, parent, false);
            return new SentMessageHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_receiver, parent, false);
            return new ReceivedMessageHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);

        if (holder.getItemViewType() == VIEW_TYPE_SENT) {
            ((SentMessageHolder) holder).bind(message);
        } else {
            ((ReceivedMessageHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (messages.get(position).getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    static class SentMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText, messageTime;
        ImageView messageImage, statusIcon;

        SentMessageHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            messageTime = itemView.findViewById(R.id.messageTime);
            messageImage = itemView.findViewById(R.id.messageImage);
            statusIcon = itemView.findViewById(R.id.statusIcon);
        }

        void bind(Message message) {
            // Set message content
            if (message.isImage()) {
                messageText.setVisibility(View.GONE);
                messageImage.setVisibility(View.VISIBLE);
                Glide.with(itemView.getContext())
                        .load(message.getContent())
                        .into(messageImage);
            } else {
                messageImage.setVisibility(View.GONE);
                messageText.setVisibility(View.VISIBLE);
                messageText.setText(message.getContent());
            }

            // Set message time
            if (message.getTimestamp() != null) {
                messageTime.setText(DateFormat.format("hh:mm a", message.getTimestamp()));
            }

            // Update status icon
            updateStatusIcon(message);
        }

        private void updateStatusIcon(Message message) {
            if (message.isRead()) {
                statusIcon.setImageResource(R.drawable.double_tick_blue);
                statusIcon.setContentDescription("Read");
            } else if (message.isDelivered()) {
                statusIcon.setImageResource(R.drawable.double_tick);
                statusIcon.setContentDescription("Delivered");
            } else {
                statusIcon.setImageResource(R.drawable.single_tick);
                statusIcon.setContentDescription("Sent");
            }
        }
    }

        static class ReceivedMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText, messageTime;
        ImageView messageImage;

        ReceivedMessageHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            messageTime = itemView.findViewById(R.id.messageTime);
            messageImage = itemView.findViewById(R.id.messageImage);
        }

        void bind(Message message) {
            if (message.isImage()) {
                messageText.setVisibility(View.GONE);
                messageImage.setVisibility(View.VISIBLE);
                Glide.with(itemView.getContext()).load(message.getContent()).into(messageImage);
            } else {
                messageImage.setVisibility(View.GONE);
                messageText.setVisibility(View.VISIBLE);
                messageText.setText(message.getContent());
            }

            messageTime.setText(DateFormat.format("hh:mm a", message.getTimestamp()));
        }
    }
}