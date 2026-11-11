package com.example.chat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> implements Filterable {
    private Context context;
    private List<User> userList;
    private List<User> filteredList;
    private String currentUserId;
    private OnUserClickListener listener;

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    public UserAdapter(Context context, List<User> userList, OnUserClickListener listener) {
        this.context = context;
        this.userList = userList;
        this.filteredList = new ArrayList<>(userList);
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        this.listener = listener;
    }
    public void updateList(List<User> newList) {
        userList = newList;
        filteredList = new ArrayList<>(userList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.user_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = filteredList.get(position);

        holder.userName.setText(user.getName());
        holder.userStatus.setText(user.getFormattedLastSeen());

        Glide.with(context)
                .load(user.getProfileImage())
                .placeholder(R.drawable.default_avatar)
                .into(holder.userAvatar);

        holder.onlineIndicator.setVisibility(user.isOnline() ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUserClick(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<User> filtered = new ArrayList<>();
                if (constraint == null || constraint.length() == 0) {
                    filtered.addAll(userList);
                } else {
                    String pattern = constraint.toString().toLowerCase().trim();
                    for (User user : userList) {
                        if (user.getName().toLowerCase().contains(pattern)) {
                            filtered.add(user);
                        }
                    }
                }

                FilterResults results = new FilterResults();
                results.values = filtered;
                return results;
            }


            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredList.clear();
                filteredList.addAll((List) results.values);
                notifyDataSetChanged();
            }
        };
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView userAvatar;
        TextView userName, userStatus;
        View onlineIndicator;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            userAvatar = itemView.findViewById(R.id.userAvatar);
            userName = itemView.findViewById(R.id.userNameTextView);
            userStatus = itemView.findViewById(R.id.userSubtitle);
            onlineIndicator = itemView.findViewById(R.id.onlineIndicator);
        }
    }
}