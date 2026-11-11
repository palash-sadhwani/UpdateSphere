package com.example.chat;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;
import de.hdodenhof.circleimageview.CircleImageView;

public class StatusAdapter extends RecyclerView.Adapter<StatusAdapter.StatusViewHolder> {

    private Context context;
    private List<Status> statusList;
    private OnStatusClickListener listener;

    public interface OnStatusClickListener {
        void onStatusClick(Status status);
        void onAddStatusClick();
        void onDeleteStatus(Status status);
    }

    public StatusAdapter(Context context, List<Status> statusList, OnStatusClickListener listener) {
        this.context = context;
        this.statusList = statusList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public StatusViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_status, parent, false);
        return new StatusViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull StatusViewHolder holder, int position) {
        Status status = statusList.get(position);
        holder.bind(status, position);
    }

    @Override
    public int getItemCount() {
        return statusList.size();
    }

    public void updateStatusList(List<Status> newStatusList) {
        statusList = newStatusList;
        notifyDataSetChanged();
    }

    public static class StatusViewHolder extends RecyclerView.ViewHolder {
        CircleImageView statusImage;
        TextView userName, statusTime, statusText;
        ImageButton deleteButton;
        View addStatusIndicator;
        private OnStatusClickListener viewHolderListener;

        public StatusViewHolder(@NonNull View itemView, OnStatusClickListener listener) {
            super(itemView);
            statusImage = itemView.findViewById(R.id.statusImage);
            userName = itemView.findViewById(R.id.userName);
            statusTime = itemView.findViewById(R.id.statusTime);
            statusText = itemView.findViewById(R.id.statusContent);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            addStatusIndicator = itemView.findViewById(R.id.addStatus);
            this.viewHolderListener = listener;

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Status status = (Status) itemView.getTag();
                    if (position == 0) {
                        viewHolderListener.onAddStatusClick();
                    } else {
                        viewHolderListener.onStatusClick(status);
                    }
                }
            });

            deleteButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Status status = (Status) itemView.getTag();
                    viewHolderListener.onDeleteStatus(status);
                }
            });
        }

        public void bind(Status status, int position) {
            itemView.setTag(status);

            if (position == 0) {
                // My Status header
                userName.setText("My Status");
                statusTime.setText("Tap to add status");
                statusText.setVisibility(View.GONE);
                deleteButton.setVisibility(View.GONE);
                addStatusIndicator.setVisibility(View.VISIBLE);
            } else {
                // Regular status item
                userName.setText(status.getUserName() != null ? status.getUserName() : "Unknown");
                statusTime.setText(status.getTime() != null ? status.getTime() : "Just now");

                if (!TextUtils.isEmpty(status.getStatusText())) {
                    statusText.setText(status.getStatusText());
                    statusText.setVisibility(View.VISIBLE);
                } else {
                    statusText.setVisibility(View.GONE);
                }

                deleteButton.setVisibility(
                        status.getUserId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                ? View.VISIBLE
                                : View.GONE
                );
                addStatusIndicator.setVisibility(View.GONE);
            }

            // Load image
            if (status.getImageUrl() != null && !status.getImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(status.getImageUrl())
                        .circleCrop()
                        .placeholder(R.drawable.default_avatar)
                        .into(statusImage);
            } else {
                statusImage.setImageResource(R.drawable.default_avatar);
            }
        }
    }
}