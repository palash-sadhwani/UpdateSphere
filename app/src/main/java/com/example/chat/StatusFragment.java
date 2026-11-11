package com.example.chat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatusFragment extends Fragment implements StatusAdapter.OnStatusClickListener {

    private RecyclerView statusRecyclerView;
    private StatusAdapter statusAdapter;
    private List<Status> statusList;
    private FirebaseFirestore db;
    private String currentUserId;
    private String currentUserName;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_status, container, false);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Initialize views
        statusRecyclerView = view.findViewById(R.id.statusRecyclerView);
        FloatingActionButton addStatusButton = view.findViewById(R.id.addStatusButton);

        // Setup RecyclerView
        statusList = new ArrayList<>();
        statusAdapter = new StatusAdapter(getContext(), statusList, this);
        statusRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        statusRecyclerView.setAdapter(statusAdapter);

        loadCurrentUserName();
        addStatusButton.setOnClickListener(v -> openStatusCreator());

        loadStatuses();

        return view;
    }

    private void loadCurrentUserName() {
        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUserName = documentSnapshot.getString("name");
                    }
                });
    }


    private void loadStatuses() {
        db.collection("status")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(getContext(), "Error loading statuses", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<Status> newStatusList = new ArrayList<>();
                    Map<String, Status> userStatusMap = new HashMap<>();

                    // Add My Status header
                    Status myStatus = new Status(
                            currentUserId,
                            currentUserName != null ? currentUserName : "My Status",
                            null,
                            "Tap to add status"
                    );
                    newStatusList.add(myStatus);

                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            String userId = doc.getString("userId");
                            String userName = doc.getString("userName");
                            List<Map<String, Object>> statuses = (List<Map<String, Object>>) doc.get("statuses");

                            if (statuses != null && !statuses.isEmpty()) {
                                // Get the latest status for each user
                                Map<String, Object> latestStatus = statuses.get(0);
                                String text = (String) latestStatus.get("text");
                                String imageUrl = (String) latestStatus.get("imageUrl");
                                long timestamp = (long) latestStatus.get("timestamp");

                                Status status = new Status(
                                        doc.getId(),
                                        userId,
                                        userName,
                                        imageUrl,
                                        timestamp,
                                        text
                                );
                                status.updateTime();

                                // Only add one status per user
                                if (!userStatusMap.containsKey(userId)) {
                                    userStatusMap.put(userId, status);
                                }
                            }
                        }
                    }

                    // Add all unique user statuses
                    newStatusList.addAll(userStatusMap.values());

                    // Sort by timestamp (newest first)
                    Collections.sort(newStatusList.subList(1, newStatusList.size()),
                            (s1, s2) -> Long.compare(s2.getTimestamp(), s1.getTimestamp()));

                    statusAdapter.updateStatusList(newStatusList);
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
        db.collection("status").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) return;

                    List<Map<String, Object>> statuses = (List<Map<String, Object>>) documentSnapshot.get("statuses");
                    if (statuses == null) return;

                    List<Map<String, Object>> updatedList = new ArrayList<>();
                    for (Map<String, Object> s : statuses) {
                        long ts = s.get("timestamp") != null ? (long) s.get("timestamp") : -1;
                        if (ts != statusToDelete.getTimestamp()) {
                            updatedList.add(s);
                        }
                    }

                    documentSnapshot.getReference()
                            .update("statuses", updatedList)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(getContext(), "Status deleted", Toast.LENGTH_SHORT).show();
                                loadStatuses();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Failed to delete status", Toast.LENGTH_SHORT).show();
                            });
                });
    }

    @Override
    public void onAddStatusClick() {
        openStatusCreator();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadStatuses();
    }
}
