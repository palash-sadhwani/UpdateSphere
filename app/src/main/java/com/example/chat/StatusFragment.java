package com.example.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.example.chat.presentation.status.StatusViewModel;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class StatusFragment extends Fragment implements StatusAdapter.OnStatusClickListener {

    private RecyclerView statusRecyclerView;
    private StatusAdapter statusAdapter;
    private List<Status> statusList;
    private StatusViewModel statusViewModel;
    private String currentUserId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_status, container, false);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            return view;
        }

        currentUserId = auth.getCurrentUser().getUid();
        // Initialize views
        statusRecyclerView = view.findViewById(R.id.statusRecyclerView);
        FloatingActionButton addStatusButton = view.findViewById(R.id.addStatusButton);

        // Setup RecyclerView
        statusList = new ArrayList<>();
        statusAdapter = new StatusAdapter(getContext(), statusList, this);
        statusRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        statusRecyclerView.setAdapter(statusAdapter);

        statusViewModel = new androidx.lifecycle.ViewModelProvider(this)
                .get(StatusViewModel.class);
        observeStatusState();
        statusViewModel.start(currentUserId);
        addStatusButton.setOnClickListener(v -> openStatusCreator());

        return view;
    }

    private void observeStatusState() {
        statusViewModel.getStatuses().observe(getViewLifecycleOwner(), statuses -> {
            statusList = statuses;
            statusAdapter.updateStatusList(statuses);
        });

        statusViewModel.getState().observe(getViewLifecycleOwner(), state -> {
            if (state.getStatus() == StatusViewModel.Status.ERROR) {
                Toast.makeText(getContext(), state.getErrorMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        statusViewModel.getDeleteCompleted().observe(getViewLifecycleOwner(), completed -> {
            if (Boolean.TRUE.equals(completed)) {
                Toast.makeText(getContext(), "Status deleted", Toast.LENGTH_SHORT).show();
            }
        });
    }
//    private void loadStatuses() {
//        db.collection("status")
//                .orderBy("timestamp", Query.Direction.DESCENDING)
//                .addSnapshotListener((value, error) -> {
//                    if (error != null) {
//                        Toast.makeText(getContext(), "Error loading statuses: " + error.getMessage(), Toast.LENGTH_SHORT).show();
//                        return;
//                    }
//
//                    List<Status> newStatusList = new ArrayList<>();
//
//                    // Add My Status header
//                    newStatusList.add(new Status(
//                            currentUserId,
//                            currentUserName != null ? currentUserName : "My Status",
//                            "",
//                            "",
//                            System.currentTimeMillis(),
//                            "Tap to add status"
//                    ));
//
//                    if (value != null && !value.isEmpty()) {
//                        for (DocumentSnapshot doc : value.getDocuments()) {
//                            try {
//                                String userId = doc.getString("userId");
//                                String userName = doc.getString("userName");
//                                String text = doc.getString("text");
//                                String imageUrl = doc.getString("imageUrl");
//                                Long timestamp = doc.getLong("timestamp");
//
//                                if (userId != null && userName != null && timestamp != null) {
//                                    Status status = new Status(
//                                            userId,
//                                            userName,
//                                            text != null ? text : "",
//                                            imageUrl != null ? imageUrl : "",
//                                            timestamp,
//                                            ""
//                                    );
//                                    status.updateTime();
//                                    newStatusList.add(status);
//                                }
//                            } catch (Exception e) {
//                                Log.e("StatusFragment", "Error parsing status", e);
//                            }
//                        }
//                    }
//
//                    // Update adapter
//                    statusAdapter.updateStatusList(newStatusList);
//                });
//    }

    private void openStatusCreator() {
        Intent intent = new Intent(getContext(), CreateStatusActivity.class);
        startActivity(intent);
    }

    @Override
    public void onStatusClick(Status status) {
        if (statusList.indexOf(status) == 0) {
            openStatusCreator();
        } else {
            Intent intent = new Intent(getContext(), ViewStatusActivity.class);
            intent.putExtra("status", status);
            startActivity(intent);
        }
    }

    @Override
    public void onDeleteStatus(Status statusToDelete) {
        statusViewModel.deleteStatus(statusToDelete);
    }

    @Override
    public void onAddStatusClick() {
        openStatusCreator();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (statusViewModel != null) {
            statusViewModel.refresh();
        }
    }

    @Override
    public void onDestroyView() {
        if (statusViewModel != null) {
            statusViewModel.stop();
        }
        super.onDestroyView();
    }
}
