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

import com.example.chat.presentation.user.UserViewModel;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class UserActivity extends AppCompatActivity implements UserAdapter.OnUserClickListener {
    private RecyclerView userRecyclerView;
    private UserAdapter userAdapter;
    private List<User> userList = new ArrayList<>();
    private UserViewModel userViewModel;
    private TextView emptyView;
    private SearchView searchView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);

        // Initialize views
        userRecyclerView = findViewById(R.id.userRecyclerView);
        emptyView = findViewById(R.id.emptyView);
        searchView = findViewById(R.id.searchView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            finish();
            return;
        }
        currentUserId = auth.getCurrentUser().getUid();

        // Setup RecyclerView
        userRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        userAdapter = new UserAdapter(this, userList, this);
        userRecyclerView.setAdapter(userAdapter);

        userViewModel = new androidx.lifecycle.ViewModelProvider(this)
                .get(UserViewModel.class);
        observeUsers();

        // Setup search and refresh
        setupSearchView();
        setupRefreshLayout();
        userViewModel.start(currentUserId);
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
            userViewModel.refresh();
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void observeUsers() {
        userViewModel.getUsers().observe(this, users -> {
            userList = new ArrayList<>(users);
            userAdapter.updateList(userList);
            showEmptyView(userList.isEmpty(), "No users found");
        });

        userViewModel.getState().observe(this, state -> {
            if (state.getStatus() == UserViewModel.Status.ERROR) {
                showEmptyView(true, state.getErrorMessage());
            }
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