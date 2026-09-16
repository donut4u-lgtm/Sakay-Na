
package com.sakyna.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

public class PassengerActivity extends ComponentActivity {

    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private LocationManager locationManager;
    private Location currentLocation;

    private EditText pickupInput;
    private EditText destinationInput;
    private TextView gpsText;
    private TextView fareText;
    private TextView statusText;
    private Button chooseDestinationButton;
    private Button bookButton;
    private Button cancelButton;
    private Button mapButton;
    private Button chatButton;
    private Button historyButton;
    private Button logoutButton;
    private RadioGroup paymentGroup;

    private ListenerRegistration activeRideListener;
    private String activeRideId = null;

    private double baseFare = 50.0;
    private double perKm = 10.0;
    private double minimumFare = 50.0;
    private double maximumFare = 500.0;

    private double pickupLatitude = 0.0;
    private double pickupLongitude = 0.0;
    private double destinationLatitude = 0.0;
    private double destinationLongitude = 0.0;

    private boolean ignoreDestinationTextChange = false;

    // Modern ActivityResultLauncher replacing deprecated startActivityForResult
    private final ActivityResultLauncher<Intent> mapLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    double lat = data.getDoubleExtra("destination_latitude", 0.0);
                    double lng = data.getDoubleExtra("destination_longitude", 0.0);
                    String address = data.getStringExtra("destination_address");

                    if (lat == 0.0 && lng == 0.0) {
                        Toast.makeText(this, "No destination was selected.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    destinationLatitude = lat;
                    destinationLongitude = lng;

                    if (address == null || address.trim().isEmpty()) {
                        address = String.format(Locale.US, "%.6f, %.6f", lat, lng);
                    }

                    ignoreDestinationTextChange = true;
                    destinationInput.setText(address);
                    destinationInput.setSelection(destinationInput.getText().length());
                    ignoreDestinationTextChange = false;

                    updateEstimatedFare();
                    Toast.makeText(this, "Destination selected.", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            finish();
            return;
        }

        buildScreen();
        loadFareSettings();
        startLocationUpdates();
        listenForActiveRide();
    }

    private void buildScreen() {
        ScrollView scrollView = new ScrollView(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);
        scrollView.addView(root);

        TextView title = new TextView(this);
        title.setText("Sakay Na");
        title.setTextSize(30);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 12);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Passenger • Book a Tricycle Ride");
        subtitle.setTextSize(17);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 0, 0, 24);
        root.addView(subtitle);

        gpsText = new TextView(this);
        gpsText.setText("Getting your location...");
        gpsText.setTextSize(15);
        gpsText.setPadding(0, 8, 0, 16);
        root.addView(gpsText);

        pickupInput = new EditText(this);
        pickupInput.setHint("Pickup location");
        pickupInput.setSingleLine(false);
        pickupInput.setMinLines(2);
        root.addView(pickupInput);

        destinationInput = new EditText(this);
        destinationInput.setHint("Destination");
        destinationInput.setSingleLine(false);
        destinationInput.setMinLines(2);

        LinearLayout.LayoutParams destinationParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        destinationParams.topMargin = 16;
        root.addView(destinationInput, destinationParams);

        chooseDestinationButton = new Button(this);
        chooseDestinationButton.setText("Choose Destination on Map");
        root.addView(chooseDestinationButton);

        destinationInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (ignoreDestinationTextChange) {
                    return;
                }
                geocodeDestination(s.toString().trim());
            }
        });

        chooseDestinationButton.setOnClickListener(v -> openDestinationMap());

        TextView paymentTitle = new TextView(this);
        paymentTitle.setText("Payment");
        paymentTitle.setTextSize(18);
        paymentTitle.setPadding(0, 24, 0, 8);
        root.addView(paymentTitle);

        paymentGroup = new RadioGroup(this);
        paymentGroup.setOrientation(RadioGroup.VERTICAL);

        RadioButton cash = new RadioButton(this);
        cash.setText("Cash");
        cash.setId(View.generateViewId());

        RadioButton gcash = new RadioButton(this);
        gcash.setText("GCash");
        gcash.setId(View.generateViewId());

        RadioButton maya = new RadioButton(this);
        maya.setText("Maya");
        maya.setId(View.generateViewId());

        paymentGroup.addView(cash);
        paymentGroup.addView(gcash);
        paymentGroup.addView(maya);
        cash.setChecked(true);

        root.addView(paymentGroup);

        fareText = new TextView(this);
        fareText.setText("Estimated fare: ₱50.00");
        fareText.setTextSize(20);
        fareText.setPadding(0, 20, 0, 12);
        root.addView(fareText);

        statusText = new TextView(this);
        statusText.setText("No active ride.");
        statusText.setTextSize(16);
        statusText.setPadding(0, 8, 0, 20);
        root.addView(statusText);

        bookButton = new Button(this);
        bookButton.setText("Book a Ride");
        root.addView(bookButton);

        cancelButton = new Button(this);
        cancelButton.setText("Cancel Ride");
        cancelButton.setEnabled(false);
        root.addView(cancelButton);

        mapButton = new Button(this);
        mapButton.setText("Live Ride Map");
        mapButton.setEnabled(false);
        root.addView(mapButton);

        chatButton = new Button(this);
        chatButton.setText("Chat");
        chatButton.setEnabled(false);
        root.addView(chatButton);

        historyButton = new Button(this);
        historyButton.setText("Ride History");
        root.addView(historyButton);

        logoutButton = new Button(this);
        logoutButton.setText("Logout");
        root.addView(logoutButton);

        setContentView(scrollView);

        bookButton.setOnClickListener(v -> bookRide());
        cancelButton.setOnClickListener(v -> cancelRide());
        mapButton.setOnClickListener(v -> openMapSafely());
        chatButton.setOnClickListener(v -> openChatSafely());
        historyButton.setOnClickListener(v ->
                Toast.makeText(PassengerActivity.this, "Ride history is available from your ride records.", Toast.LENGTH_SHORT).show()
        );
        logoutButton.setOnClickListener(v -> logout());
    }

    private void openDestinationMap() {
        try {
            Intent intent = new Intent(PassengerActivity.this, MapActivity.class);
            intent.putExtra("mode", "SELECT_DESTINATION");
            intent.putExtra("pickup_latitude", pickupLatitude);
            intent.putExtra("pickup_longitude", pickupLongitude);
            intent.putExtra("pickup_address", pickupInput.getText().toString().trim());
            intent.putExtra("destination_latitude", destinationLatitude);
            intent.putExtra("destination_longitude", destinationLongitude);
            intent.putExtra("destination_address", destinationInput.getText().toString().trim());

            mapLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open the map.", Toast.LENGTH_LONG).show();
        }
    }

    private void geocodeDestination(String destinationName) {
        if (destinationName.isEmpty() || !Geocoder.isPresent()) {
            destinationLatitude = 0.0;
            destinationLongitude = 0.0;
            updateEstimatedFare();
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocationName(destinationName, 1);

                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    destinationLatitude = address.getLatitude();
                    destinationLongitude = address.getLongitude();
                } else {
                    destinationLatitude = 0.0;
                    destinationLongitude = 0.0;
                }
            } catch (Exception ignored) {
                destinationLatitude = 0.0;
                destinationLongitude = 0.0;
            }

            new Handler(Looper.getMainLooper()).post(this::updateEstimatedFare);
        });
    }

    private void loadFareSettings() {
        db.collection("settings").document("fare").get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Double value = document.getDouble("baseFare");
                        if (value != null) baseFare = value;

                        value = document.getDouble("perKm");
                        if (value != null) perKm = value;

                        value = document.getDouble("minimum");
                        if (value != null) minimumFare = value;

                        value = document.getDouble("maximum");
                        if (value != null) maximumFare = value;
                    }
                    updateEstimatedFare();
                })
                .addOnFailureListener(e -> updateEstimatedFare());
    }

    private void startLocationUpdates() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (locationManager == null) {
            gpsText.setText("Location service unavailable.");
            return;
        }

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST
            );
            return;
        }

        try {
            boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (gpsEnabled) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 5, locationListener);
            } else if (networkEnabled) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 5, locationListener);
            } else {
                gpsText.setText("Please turn on Location/GPS.");
            }

            Location lastGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location lastNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            if (lastGps != null) {
                updateLocation(lastGps);
            } else if (lastNetwork != null) {
                updateLocation(lastNetwork);
            }
        } catch (SecurityException e) {
            gpsText.setText("Location permission is required.");
        } catch (Exception e) {
            gpsText.setText("Unable to start location.");
        }
    }

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            updateLocation(location);
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
            gpsText.setText("Location enabled.");
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            gpsText.setText("Location disabled.");
        }
    };

    private void updateLocation(Location location) {
        if (location == null) return;

        currentLocation = location;
        pickupLatitude = location.getLatitude();
        pickupLongitude = location.getLongitude();

        gpsText.setText(String.format(Locale.US, "Current location: %.6f, %.6f", pickupLatitude, pickupLongitude));

        Executors.newSingleThreadExecutor().execute(() -> {
            String locationText = getAddressText(pickupLatitude, pickupLongitude);
            new Handler(Looper.getMainLooper()).post(() -> {
                if (locationText != null && !locationText.isEmpty() && pickupInput.getText().toString().isEmpty()) {
                    pickupInput.setText(locationText);
                }
                updateEstimatedFare();
            });
        });
    }

    private String getAddressText(double latitude, double longitude) {
        if (!Geocoder.isPresent()) return null;

        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String line = address.getAddressLine(0);
                if (line != null && !line.isEmpty()) return line;
            }
        } catch (IOException ignored) {
        } catch (Exception ignored) {
        }

        return null;
    }

    private void updateEstimatedFare() {
        double fare = calculateFare();
        fareText.setText(String.format(Locale.US, "Estimated fare: ₱%.2f", fare));
    }

    private double calculateFare() {
        double fare = baseFare;

        if (currentLocation != null && destinationLatitude != 0.0 && destinationLongitude != 0.0) {
            float[] distance = new float[1];
            Location.distanceBetween(pickupLatitude, pickupLongitude, destinationLatitude, destinationLongitude, distance);
            double kilometers = distance[0] / 1000.0;
            fare = baseFare + (kilometers * perKm);
        }

        if (fare < minimumFare) fare = minimumFare;
        if (fare > maximumFare) fare = maximumFare;

        return fare;
    }

    private void bookRide() {
        currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String pickup = pickupInput.getText().toString().trim();
        String destination = destinationInput.getText().toString().trim();

        if (pickup.isEmpty()) {
            Toast.makeText(this, "Please enter your pickup location.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (destination.isEmpty()) {
            Toast.makeText(this, "Please enter your destination.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (pickupLatitude == 0.0 && pickupLongitude == 0.0) {
            Toast.makeText(this, "Waiting for your GPS location. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        String paymentMethod = getSelectedPaymentMethod();
        double fare = calculateFare();

        Map<String, Object> ride = new HashMap<>();
        ride.put("passengerId", currentUser.getUid());
        ride.put("passengerPhone", currentUser.getPhoneNumber() == null ? "" : currentUser.getPhoneNumber());
        ride.put("pickup", pickup);
        ride.put("destination", destination);
        ride.put("pickupLatitude", pickupLatitude);
        ride.put("pickupLongitude", pickupLongitude);
        ride.put("destinationLatitude", destinationLatitude);
        ride.put("destinationLongitude", destinationLongitude);
        ride.put("rideType", "Tricycle");
        ride.put("fare", fare);
        ride.put("paymentMethod", paymentMethod);
        ride.put("paymentStatus", "PENDING");
        ride.put("status", "REQUESTED");
        ride.put("createdAt", FieldValue.serverTimestamp());

        bookButton.setEnabled(false);

        db.collection("rides").add(ride)
                .addOnSuccessListener(documentReference -> {
                    activeRideId = documentReference.getId();
                    statusText.setText("Ride request is waiting for a driver.");
                    cancelButton.setEnabled(true);
                    mapButton.setEnabled(false);
                    chatButton.setEnabled(false);
                    Toast.makeText(PassengerActivity.this, "Ride request sent.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    bookButton.setEnabled(true);
                    Toast.makeText(PassengerActivity.this, "Could not book the ride.", Toast.LENGTH_LONG).show();
                });
    }

    private String getSelectedPaymentMethod() {
        int selectedId = paymentGroup.getCheckedRadioButtonId();
        if (selectedId == -1) return "Cash";

        View selected = paymentGroup.findViewById(selectedId);
        if (selected instanceof RadioButton) {
            return ((RadioButton) selected).getText().toString();
        }
        return "Cash";
    }

    private void listenForActiveRide() {
        if (currentUser == null) return;

        activeRideListener = db.collection("rides")
                .whereEqualTo("passengerId", currentUser.getUid())
                .whereIn("status", Arrays.asList("REQUESTED", "ACCEPTED", "ARRIVED", "IN_PROGRESS"))
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) return;

                    if (snapshot.isEmpty()) {
                        activeRideId = null;
                        statusText.setText("No active ride.");
                        bookButton.setEnabled(true);
                        cancelButton.setEnabled(false);
                        mapButton.setEnabled(false);
                        chatButton.setEnabled(false);
                        return;
                    }

                    DocumentSnapshot ride = snapshot.getDocuments().get(0);
                    activeRideId = ride.getId();
                    String status = ride.getString("status");
                    if (status == null) status = "REQUESTED";

                    statusText.setText("Ride status: " + status);
                    bookButton.setEnabled(false);
                    cancelButton.setEnabled("REQUESTED".equals(status));

                    boolean active = "ACCEPTED".equals(status) || "ARRIVED".equals(status) || "IN_PROGRESS".equals(status);
                    mapButton.setEnabled(active);
                    chatButton.setEnabled(active);
                });
    }

    private void cancelRide() {
        if (activeRideId == null) return;

        db.collection("rides").document(activeRideId)
                .update("status", "CANCELLED")
                .addOnSuccessListener(unused -> {
                    Toast.makeText(PassengerActivity.this, "Ride cancelled.", Toast.LENGTH_SHORT).show();
                    activeRideId = null;
                    statusText.setText("Ride cancelled.");
                    bookButton.setEnabled(true);
                    cancelButton.setEnabled(false);
                    mapButton.setEnabled(false);
                    chatButton.setEnabled(false);
                })
                .addOnFailureListener(e -> Toast.makeText(PassengerActivity.this, "Could not cancel the ride.", Toast.LENGTH_LONG).show());
    }

    private void openMapSafely() {
        if (activeRideId == null) {
            Toast.makeText(this, "There is no active ride.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Intent intent = new Intent(PassengerActivity.this, MapActivity.class);
            intent.putExtra("rideId", activeRideId);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Map is temporarily unavailable.", Toast.LENGTH_SHORT).show();
        }
    }

    private void openChatSafely() {
        if (activeRideId == null) {
            Toast.makeText(this, "There is no active ride.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Intent intent = new Intent(PassengerActivity.this, RideChatActivity.class);
            intent.putExtra("rideId", activeRideId);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Chat is temporarily unavailable.", Toast.LENGTH_SHORT).show();
        }
    }

    private void logout() {
        if (activeRideListener != null) {
            activeRideListener.remove();
            activeRideListener = null;
        }

        stopLocationUpdates();
        auth.signOut();

        Intent intent = new Intent(PassengerActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void stopLocationUpdates() {
        if (locationManager == null) return;

        try {
            locationManager.removeUpdates(locationListener);
        } catch (SecurityException ignored) {
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            boolean granted = false;
            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_GRANTED) {
                    granted = true;
                    break;
                }
            }

            if (granted) {
                startLocationUpdates();
            } else {
                gpsText.setText("Location permission was not granted.");
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onDestroy() {
        if (activeRideListener != null) {
            activeRideListener.remove();
            activeRideListener = null;
        }
        stopLocationUpdates();
        super.onDestroy();
    }
}
