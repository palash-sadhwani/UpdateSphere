package com.example.chat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.HashMap;
import java.util.Map;

public class LocationFragment extends Fragment {

    private MapView map;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private boolean isSharingLocation = false;
    private SwitchMaterial locationSwitch;
    private Map<String, Marker> friendMarkers = new HashMap<>();
    private MyLocationNewOverlay myLocationOverlay;
    private FloatingActionButton fabCenter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        View view = inflater.inflate(R.layout.fragment_location, container, false);

        // Initialize map
        map = view.findViewById(R.id.map);
        initializeMap();

        // Initialize FAB
        fabCenter = view.findViewById(R.id.fabCenter);
        fabCenter.setOnClickListener(v -> centerOnMyLocation());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Initialize location switch
        locationSwitch = view.findViewById(R.id.locationSwitch);
        setupLocationSwitch();

        // Check current sharing status
        checkLocationSharingStatus();

        // Load friends locations
        loadFriendsLocations();

        return view;
    }

    private void initializeMap() {
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);

        // Add compass overlay
        CompassOverlay compassOverlay = new CompassOverlay(requireContext(),
                new InternalCompassOrientationProvider(requireContext()), map);
        compassOverlay.enableCompass();
        map.getOverlays().add(compassOverlay);

        // Initialize location overlay
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireContext()), map);

        myLocationOverlay.enableMyLocation();
        map.getOverlays().add(myLocationOverlay);
    }

    private void centerOnMyLocation() {
        if (checkLocationPermission()) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            GeoPoint currentGeoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                            map.getController().animateTo(currentGeoPoint);
                            map.getController().setZoom(15.0);
                        } else {
                            Toast.makeText(getContext(), "Location not available", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            requestLocationPermission();
        }
    }

    private void stopLocationUpdates() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    private void setupLocationSwitch() {
        locationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isAdded() || getContext() == null) {
                return;
            }

            isSharingLocation = isChecked;
            if (isChecked) {
                if (checkLocationPermission()) {
                    startLocationUpdates();
                } else {
                    requestLocationPermission();
                    locationSwitch.setChecked(false);
                }
            } else {
                stopLocationUpdates();
                removeUserLocationFromFirestore();
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void setupMap() {
        if (!checkLocationPermission()) {
            requestLocationPermission();
            return;
        }

        myLocationOverlay.enableMyLocation();
        getLastKnownLocation();
    }

    private boolean checkLocationPermission() {
        Context context = getContext();
        if (context == null || !isAdded()) {
            return false;
        }
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(requireActivity(),
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        createLocationRequest();
        createLocationCallback();
        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());

        // Show initial location immediately
        getLastKnownLocation();
    }

    private void createLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000); // 10 seconds
        locationRequest.setFastestInterval(5000); // 5 seconds
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                for (android.location.Location location : locationResult.getLocations()) {
                    GeoPoint currentGeoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                    map.getController().animateTo(currentGeoPoint);
                    saveUserLocation(currentGeoPoint);
                }
            }
        };
    }

    @SuppressLint("MissingPermission")
    private void getLastKnownLocation() {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        GeoPoint currentGeoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                        map.getController().setCenter(currentGeoPoint);
                        map.getController().setZoom(15.0);
                        if (isSharingLocation) {
                            saveUserLocation(currentGeoPoint);
                        }
                    }
                });
    }

    private void saveUserLocation(GeoPoint geoPoint) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        com.google.firebase.firestore.GeoPoint firestoreGeoPoint =
                new com.google.firebase.firestore.GeoPoint(geoPoint.getLatitude(), geoPoint.getLongitude());

        UserLocation userLocation = new UserLocation();
        userLocation.setUserId(userId);
        userLocation.setGeoPoint(firestoreGeoPoint);
        userLocation.setTimestamp(System.currentTimeMillis());
        userLocation.setUserName(FirebaseAuth.getInstance().getCurrentUser().getDisplayName());

        FirebaseFirestore.getInstance().collection("user_locations")
                .document(userId)
                .set(userLocation)
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to update location", Toast.LENGTH_SHORT).show();
                });
    }



    private void loadFriendsLocations() {
        FirebaseFirestore.getInstance().collection("user_locations")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        if (isAdded() && getContext() != null) {
                            Toast.makeText(requireContext(), "Error loading locations", Toast.LENGTH_SHORT).show();
                        }
                        Log.e("LocationFragment", "Error loading locations", error);
                        return;
                    }

                    if (value == null) return;

                    // Clear only friend markers
                    for (Marker marker : friendMarkers.values()) {
                        map.getOverlays().remove(marker);
                    }
                    friendMarkers.clear();

                    String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                            FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        UserLocation location = doc.toObject(UserLocation.class);
                        if (location != null && !location.getUserId().equals(currentUserId)) {
                            addFriendMarker(location);
                        }
                    }
                    map.invalidate(); // Refresh map
                });
    }
private void addFriendMarker(UserLocation location) {
    try {
        com.google.firebase.firestore.GeoPoint geoPoint = location.getGeoPoint();
        GeoPoint osmGeoPoint = new GeoPoint(geoPoint.getLatitude(), geoPoint.getLongitude());

        Marker friendMarker = new Marker(map);
        friendMarker.setPosition(osmGeoPoint);
        friendMarker.setTitle(location.getUserName() != null ? location.getUserName() : "Unknown");
        friendMarker.setSnippet("Last updated: " + formatTimestamp(location.getTimestamp()));

        // Set marker icon with fallback
        Drawable icon = null;
        if (getContext() != null) {
            icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_person_pin);
            if (icon == null) {
                icon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_mylocation);
            }
        }

        if (icon != null) {
            friendMarker.setIcon(icon);
        }

        friendMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(friendMarker);
        friendMarkers.put(location.getUserId(), friendMarker);
    } catch (Exception e) {
        // Use requireContext() only if fragment is attached
        if (isAdded() && getContext() != null) {
            Toast.makeText(requireContext(), "Error showing friend location", Toast.LENGTH_SHORT).show();
        }
        Log.e("LocationFragment", "Error showing friend location", e);
    }
}
    private String formatTimestamp(long timestamp) {
        return android.text.format.DateFormat.format("MMM dd, hh:mm a", timestamp).toString();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupMap();
                if (isSharingLocation) {
                    startLocationUpdates();
                }
            } else {
                locationSwitch.setChecked(false);
                Toast.makeText(getContext(), "Location permission required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (map != null) {
            map.onResume();
        }
        if (isSharingLocation && checkLocationPermission()) {
            startLocationUpdates();
        }
        loadFriendsLocations();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (map != null) {
            map.onPause();
        }
        stopLocationUpdates();
    }

    private void removeUserLocationFromFirestore() {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        if (!userId.isEmpty()) {
            FirebaseFirestore.getInstance().collection("user_locations")
                    .document(userId)
                    .delete()
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to stop sharing location", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void checkLocationSharingStatus() {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        if (!userId.isEmpty()) {
            FirebaseFirestore.getInstance().collection("user_locations")
                    .document(userId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (!isAdded() || getContext() == null) {
                            return;
                        }

                        if (task.isSuccessful() && task.getResult().exists()) {
                            isSharingLocation = true;
                            locationSwitch.setChecked(true);
                            if (checkLocationPermission()) {
                                startLocationUpdates();
                            }
                        }
                    });
        }
    }


}