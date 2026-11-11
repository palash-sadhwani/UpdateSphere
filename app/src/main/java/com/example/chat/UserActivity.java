package com.example.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class UserActivity extends AppCompatActivity implements UserAdapter.OnUserClickListener {
    private RecyclerView userRecyclerView;
    private UserAdapter userAdapter;
    private List<User> userList = new ArrayList<>();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private TextView emptyView;
    private SearchView searchView;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);

        // Initialize views
        userRecyclerView = findViewById(R.id.userRecyclerView);
        emptyView = findViewById(R.id.emptyView);
        searchView = findViewById(R.id.searchView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        // Setup RecyclerView
        userRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        userAdapter = new UserAdapter(this, userList, this);
        userRecyclerView.setAdapter(userAdapter);

        // Setup search and refresh
        setupSearchView();
        setupRefreshLayout();
        loadUsers();
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (userAdapter != null) {
                    userAdapter.getFilter().filter(newText);
                }
                return true;
            }
        });
    }

    private void setupRefreshLayout() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadUsers();
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void loadUsers() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("users")
                .orderBy("name", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        showEmptyView(true, "Error loading users");
                        return;
                    }

                    userList.clear();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            User user = doc.toObject(User.class);
                            if (user != null && !user.getUserId().equals(currentUserId)) {
                                userList.add(user);
                            }
                        }
                    }

                    userAdapter.updateList(userList);
                    showEmptyView(userList.isEmpty(), "No users found");
                });
    }

    private void showEmptyView(boolean show, String message) {
        emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
        emptyView.setText(message);
    }

    @Override
    public void onUserClick(User user) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("receiverId", user.getUserId());
        startActivity(intent);
    }
}