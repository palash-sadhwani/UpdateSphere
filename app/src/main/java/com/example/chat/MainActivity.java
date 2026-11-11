package com.example.chat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.cloudinary.android.MediaManager;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private BottomNavigationView bottomNavigation;
    private Toolbar toolbar;
    private FloatingActionButton fab;
    private int currentNavItem = R.id.nav_chats;
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Initialize Cloudinary
        try {
            Map config = new HashMap();
            config.put("cloud_name", BuildConfig.CLOUDINARY_CLOUD_NAME);
            config.put("api_key", BuildConfig.CLOUDINARY_API_KEY);
            config.put("api_secret", BuildConfig.CLOUDINARY_API_SECRET);
            MediaManager.init(this, config);
        } catch (IllegalStateException e) {

            Log.d("CloudinaryInit", "MediaManager already initialized: " + e.getMessage());
        } catch (Exception e) {
            Log.e("CloudinaryInit", "Initialization failed", e);
            Toast.makeText(this, "Media uploads unavailable", Toast.LENGTH_SHORT).show();
        }

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null) {
            redirectToLogin();
            return;
        }
//        startActivity(new Intent(this, UserActivity.class));

        // Initialize views
        toolbar = findViewById(R.id.customToolbar);
        bottomNavigation = findViewById(R.id.bottom_navigation);
        fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, UserActivity.class);
            startActivity(intent);
            // Optional: Add animation
//            overridePendingTransition(R.anim.slide_up, R.anim.stay);
        });

        // Setup toolbar with menu
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }

        // Setup Bottom Navigation
        setupBottomNavigation();

        // Load ChatsFragment by default
        loadFragment(new ChatsFragment(), "chats_fragment");
    }

    private void loadFragment(Fragment fragment, String tag) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment, tag)
                .commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_settings) {
            startActivity(new Intent(this, MyProfileActivity.class));
            return true;
        } else if (id == R.id.menu_about) {
            // Handle about menu item
            return true;
        } else if (id == R.id.menu_logout) {
            logoutUser();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void logoutUser() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            FirebaseFirestore.getInstance().collection("users")
                    .document(currentUser.getUid())
                    .update("online", false, "lastSeen", System.currentTimeMillis())
                    .addOnCompleteListener(task -> {
                        mAuth.signOut();
                        redirectToLogin();
                    });
        } else {
            mAuth.signOut();
            redirectToLogin();
        }
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment fragment = null;
                currentNavItem = item.getItemId();

                if (currentNavItem == R.id.nav_chats) {
                    fragment = new ChatsFragment();
                    fab.show();
                } else if (currentNavItem == R.id.nav_updates) {
                    fragment = new StatusFragment();
                    fab.hide();
                } else if (currentNavItem == R.id.nav_location) {
                    fragment = new LocationFragment();
                    fab.hide();
                }

                if (fragment != null) {
                    loadFragment(fragment, currentNavItem == R.id.nav_chats ? "chats_fragment" : null);
                    return true;
                }
                return false;
            }
        });

        // Set Chats as default selected item
        bottomNavigation.setSelectedItemId(R.id.nav_chats);
    }

    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setOnlineStatus(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        setOnlineStatus(false);
    }
    @Override
    protected void onStart() {
        super.onStart();
        setOnlineStatus(true); // User comes online
    }

    @Override
    protected void onStop() {
        super.onStop();
        setOnlineStatus(false); // User goes offline
    }

    private void setOnlineStatus(boolean isOnline) {
        if (currentUserId != null) {
            Map<String, Object> status = new HashMap<>();
            status.put("isOnline", isOnline);
            if (!isOnline) {
                status.put("lastSeen", System.currentTimeMillis());
            }

            db.collection("users").document(currentUserId)
                    .update(status)
                    .addOnSuccessListener(aVoid -> Log.d("Status", "Status updated successfully"))
                    .addOnFailureListener(e -> Log.e("Status", "Error updating status", e));
        }
    }


    private void redirectToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}