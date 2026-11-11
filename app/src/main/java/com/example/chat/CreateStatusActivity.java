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
import androidx.lifecycle.ViewModelProvider;

import com.example.chat.presentation.status.CreateStatusViewModel;
import com.google.firebase.auth.FirebaseAuth;

public class CreateStatusActivity extends AppCompatActivity {

    private EditText statusText;
    private ImageView statusImage;
    private View imagePreviewContainer;
    private ProgressBar progressBar;
    private Uri imageUri;
    private CreateStatusViewModel createStatusViewModel;
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

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            finish();
            return;
        }
        currentUserId = auth.getCurrentUser().getUid();
        createStatusViewModel = new ViewModelProvider(this)
                .get(CreateStatusViewModel.class);
        observeCreateStatusState();

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

        createStatusViewModel.createStatus(currentUserId, text, imageUri);
    }

    private void observeCreateStatusState() {
        createStatusViewModel.getState().observe(this, event -> {
            CreateStatusViewModel.CreateStatusState state = event.getContent();
            if (state.getStatus() == CreateStatusViewModel.Status.LOADING) {
                if (progressBar != null) {
                    progressBar.setVisibility(View.VISIBLE);
                }
                findViewById(R.id.postStatusButton).setEnabled(false);
            } else if (state.getStatus() == CreateStatusViewModel.Status.SUCCESS
                    && event.consume()) {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                Toast.makeText(this, "Status posted successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else if (state.getStatus() == CreateStatusViewModel.Status.ERROR
                    && event.consume()) {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                findViewById(R.id.postStatusButton).setEnabled(true);
                Toast.makeText(this,
                        "Failed to post status: " + state.getErrorMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}