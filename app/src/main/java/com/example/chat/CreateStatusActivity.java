package com.example.chat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateStatusActivity extends AppCompatActivity {

    private EditText statusText;
    private ImageView statusImage;
    private View imagePreviewContainer;
    private ProgressBar progressBar;
    private Uri imageUri;
    private FirebaseFirestore db;
    private String currentUserId;
    // At the top with other view declarations:


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_status);

        // Initialize views
        statusText = findViewById(R.id.statusText);
        statusImage = findViewById(R.id.statusImage);
        imagePreviewContainer = findViewById(R.id.imagePreviewContainer);
        progressBar = findViewById(R.id.progressBar);

        // Initialize Cloudinary
        try {
            MediaManager.init(this);
        } catch (IllegalStateException e) {
            // Already initialized
        }

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Set up listeners
        findViewById(R.id.addImageButton).setOnClickListener(v -> pickImage());
        findViewById(R.id.postStatusButton).setOnClickListener(v -> postStatus());
        findViewById(R.id.removeImageButton).setOnClickListener(v -> removeImage());

        // Enable/disable post button based on content
        statusText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePostButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            statusImage.setImageURI(imageUri);
            imagePreviewContainer.setVisibility(View.VISIBLE);
            updatePostButtonState();
        }
    }

    private void removeImage() {
        imageUri = null;
        statusImage.setImageURI(null);
        imagePreviewContainer.setVisibility(View.GONE);
        updatePostButtonState();
    }

    private void updatePostButtonState() {
        boolean hasText = !TextUtils.isEmpty(statusText.getText());
        boolean hasImage = imageUri != null;
        findViewById(R.id.postStatusButton).setEnabled(hasText || hasImage);
    }

    private void postStatus() {
        String text = statusText.getText().toString().trim();

        if (TextUtils.isEmpty(text) && imageUri == null) {
            Toast.makeText(this, "Please add text or image", Toast.LENGTH_SHORT).show();
            return;
        }

        // Make sure progressBar is initialized
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        findViewById(R.id.postStatusButton).setEnabled(false);

        if (imageUri != null) {
            uploadImage(text);
        } else {
            saveStatusToFirestore(text, null);
        }
    }

    private void uploadImage(String text) {
        MediaManager.get().upload(imageUri)
                .option("folder", "status_images")
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String) resultData.get("url");
                        saveStatusToFirestore(text, imageUrl);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }
                        findViewById(R.id.postStatusButton).setEnabled(true);
                        Toast.makeText(CreateStatusActivity.this,
                                "Failed to upload image: " + error.getDescription(),
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                })
                .dispatch();
    }

    private void saveStatusToFirestore(String text, String imageUrl) {
        DocumentReference docRef = db.collection("status").document(currentUserId);

        docRef.get().addOnSuccessListener(documentSnapshot -> {
            List<Map<String, Object>> statusList = new ArrayList<>();

            if (documentSnapshot.exists()) {
                statusList = (List<Map<String, Object>>) documentSnapshot.get("statuses");
                if (statusList == null) {
                    statusList = new ArrayList<>();
                }
            }

            Map<String, Object> newStatus = new HashMap<>();
            newStatus.put("text", text);
            newStatus.put("imageUrl", imageUrl);
            newStatus.put("timestamp", System.currentTimeMillis());

            statusList.add(newStatus);

            Map<String, Object> data = new HashMap<>();
            data.put("userId", currentUserId);
            data.put("userName", FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
            data.put("statuses", statusList);

            docRef.set(data)
                    .addOnSuccessListener(unused -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(CreateStatusActivity.this,
                                "Status posted successfully",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        findViewById(R.id.postStatusButton).setEnabled(true);
                        Toast.makeText(CreateStatusActivity.this,
                                "Failed to post status: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        });
    }
}