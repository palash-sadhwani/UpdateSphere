package com.example.chat;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

public class ViewStatusActivity extends AppCompatActivity {

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_status);

        Status status = getIntent().getParcelableExtra("status");
        if (status == null) {
            Toast.makeText(this, "Status not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ImageView statusImage = findViewById(R.id.statusImage);
        TextView statusText = findViewById(R.id.statusText);
        TextView userName = findViewById(R.id.userName);
        TextView time = findViewById(R.id.time);

        // Set user name with null check
        userName.setText(status.getUserName() != null ? status.getUserName() : "Unknown");

        // Set time with null check
        time.setText(status.getTime() != null ? status.getTime() : "Just now");

        if (status.getImageUrl() != null && !status.getImageUrl().isEmpty()) {
            // Replace http:// with https:// if present
            String secureUrl = status.getImageUrl().replace("http://", "https://");
            Glide.with(this)
                    .load(secureUrl)
                    .into(statusImage);
            statusImage.setVisibility(View.VISIBLE);
            statusText.setVisibility(View.GONE);
        }else if (status.getStatusText() != null && !status.getStatusText().isEmpty()) {
            statusText.setVisibility(View.VISIBLE);
            statusText.setText(status.getStatusText());
            statusImage.setVisibility(View.GONE);
        } else {
            // Handle case where both image and text are null
            statusText.setVisibility(View.VISIBLE);
            statusText.setText("No content available");
            statusImage.setVisibility(View.GONE);
        }
    }
}