
package com.sakyna.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
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

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;

public class PassengerActivity extends Activity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private LocationManager locationManager;

    private EditText pickupField;
    private EditText destinationField;
    private TextView gpsText;
    private TextView fareText;
    private TextView rideStatusText;

    private double pickupLat = 0;
    private double pickupLng = 0;
    private double destinationLat = 0;
    private double destinationLng = 0;

    private double distanceKm = 0;
    private double fare = 0;

    private String currentRideId = "";

    private ListenerRegistration rideListener;

    private static final int LOCATION_PERMISSION = 100;
    private static final int DESTINATION_MAP = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        buildScreen();
        startGps();
    }

    private void buildScreen() {

        ScrollView scroll = new ScrollView(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(30, 30, 30, 40);

        TextView title = new TextView(this);
        title.setText("SAKAY NA");
        title.setTextSize(30);
        title.setTextColor(Color.rgb(0, 120, 70));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 10, 0, 25);
        root.addView(title);

        TextView passengerTitle = new TextView(this);
        passengerTitle.setText("Passenger • Book a Ride");
        passengerTitle.setTextSize(21);
        passengerTitle.setTextColor(Color.DKGRAY);
        root.addView(passengerTitle);

        gpsText = new TextView(this);
        gpsText.setText("📍 Getting your current location...");
        gpsText.setTextSize(16);
        gpsText.setPadding(0, 20, 0, 20);
        root.addView(gpsText);

        TextView pickupLabel = new TextView(this);
        pickupLabel.setText("PICKUP LOCATION");
        pickupLabel.setTextSize(14);
        pickupLabel.setTextColor(Color.DKGRAY);
        root.addView(pickupLabel);

        pickupField = new EditText(this);
        pickupField.setHint("Your current location");
        pickupField.setTextSize(17);
        pickupField.setSingleLine(true);
        pickupField.setEnabled(false);
        root.addView(pickupField);

        Button refreshLocation = new Button(this);
        refreshLocation.setText("📍 USE MY CURRENT LOCATION");
        refreshLocation.setOnClickListener(v -> startGps());
        root.addView(refreshLocation);

        TextView destinationLabel = new TextView(this);
        destinationLabel.setText("DESTINATION");
        destinationLabel.setTextSize(14);
        destinationLabel.setTextColor(Color.DKGRAY);
        destinationLabel.setPadding(0, 20, 0, 5);
        root.addView(destinationLabel);

        destinationField = new EditText(this);
        destinationField.setHint("Choose destination on map");
        destinationField.setTextSize(17);
        destinationField.setSingleLine(true);
        destinationField.setFocusable(false);
        root.addView(destinationField);

        Button chooseDestination = new Button(this);
        chooseDestination.setText("🗺️ CHOOSE DESTINATION ON MAP");
        chooseDestination.setTextSize(16);
        chooseDestination.setOnClickListener(v -> openDestinationMap());
        root.addView(chooseDestination);

        fareText = new TextView(this);
        fareText.setText("Fare: —");
        fareText.setTextSize(20);
        fareText.setTextColor(Color.rgb(0, 120, 70));
        fareText.setPadding(0, 25, 0, 15);
        root.addView(fareText);

        Button calculate = new Button(this);
        calculate.setText("CALCULATE FARE");
        calculate.setOnClickListener(v -> calculateFare());
        root.addView(calculate);

        TextView paymentLabel = new TextView(this);
        paymentLabel.setText("PAYMENT METHOD");
        paymentLabel.setTextSize(15);
        paymentLabel.setPadding(0, 20, 0, 5);
        root.addView(paymentLabel);

        RadioGroup paymentGroup = new RadioGroup(this);

        RadioButton cash = new RadioButton(this);
        cash.setText("Cash");
        cash.setId(View.generateViewId());
        cash.setChecked(true);

        RadioButton gcash = new RadioButton(this);
        gcash.setText("GCash");
        gcash.setId(View.generateViewId());

        RadioButton maya = new RadioButton(this);
        maya.setText("Maya");
        maya.setId(View.generateViewId());

        paymentGroup.addView(cash);
        paymentGroup.addView(gcash);
        paymentGroup.addView(maya);

        root.addView(paymentGroup);

        Button book = new Button(this);
        book.setText("🚕 BOOK RIDE");
        book.setTextSize(18);
        book.setOnClickListener(v ->
                bookRide(paymentGroup.getCheckedRadioButtonId()));
        root.addView(book);

        rideStatusText = new TextView(this);
        rideStatusText.setText("Current Ride: None");
        rideStatusText.setTextSize(17);
        rideStatusText.setPadding(0, 25, 0, 15);
        root.addView(rideStatusText);

        Button liveMap = new Button(this);
        liveMap.setText("🗺️ LIVE RIDE MAP");
        liveMap.setOnClickListener(v -> openLiveMap());
        root.addView(liveMap);

        Button cancel = new Button(this);
        cancel.setText("CANCEL CURRENT RIDE");
        cancel.setOnClickListener(v -> cancelRide());
        root.addView(cancel);

        Button logout = new Button(this);
        logout.setText("LOGOUT");
        logout.setOnClickListener(v -> logout());
        root.addView(logout);

        scroll.addView(root);
        setContentView(scroll);
    }

    private void startGps() {

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION
            );

            return;
        }

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {

                pickupLat = location.getLatitude();
                pickupLng = location.getLongitude();

                pickupField.setText(
                        String.format(
                                java.util.Locale.US,
                                "Current location (%.6f, %.6f)",
                                pickupLat,
                                pickupLng
                        )
                );

                gpsText.setText(
                        String.format(
                                java.util.Locale.US,
                                "📍 GPS: %.6f, %.6f",
                                pickupLat,
                                pickupLng
                        )
                );
            }
        };

        try {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000,
                    5,
                    listener
            );

            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    2000,
                    5,
                    listener
            );

        } catch (Exception e) {
            gpsText.setText("Unable to start GPS: " + e.getMessage());
        }
    }

    private void openDestinationMap() {

        if (pickupLat == 0 && pickupLng == 0) {
            Toast.makeText(
                    this,
                    "Waiting for your pickup location.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        Intent intent = new Intent(this, MapActivity.class);

        intent.putExtra("mode", "SELECT_DESTINATION");

        intent.putExtra("passenger_latitude", pickupLat);
        intent.putExtra("passenger_longitude", pickupLng);

        startActivityForResult(intent, DESTINATION_MAP);
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == DESTINATION_MAP &&
                resultCode == RESULT_OK &&
                data != null) {

            destinationLat =
                    data.getDoubleExtra("destination_latitude", 0);

            destinationLng =
                    data.getDoubleExtra("destination_longitude", 0);

            String address =
                    data.getStringExtra("destination_address");

            if (destinationLat != 0 && destinationLng != 0) {

                if (address == null || address.trim().isEmpty()) {
                    address = String.format(
                            java.util.Locale.US,
                            "Selected location (%.6f, %.6f)",
                            destinationLat,
                            destinationLng
                    );
                }

                destinationField.setText(address);

                Toast.makeText(
                        this,
                        "Destination selected.",
                        Toast.LENGTH_SHORT
                ).show();

                calculateFare();
            }
        }
    }

    private void calculateFare() {

        if (pickupLat == 0 || pickupLng == 0) {
            Toast.makeText(
                    this,
                    "Pickup GPS location is not ready.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        if (destinationLat == 0 || destinationLng == 0) {
            Toast.makeText(
                    this,
                    "Please choose your destination on the map.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        float[] results = new float[1];

        Location.distanceBetween(
                pickupLat,
                pickupLng,
                destinationLat,
                destinationLng,
                results
        );

        distanceKm = results[0] / 1000.0;

        // Default Sakay Na fare.
        // These values can later be controlled from Firestore.
        double baseFare = 20.0;
        double perKm = 10.0;

        fare = baseFare + (distanceKm * perKm);

        if (fare < 20) {
            fare = 20;
        }

        fareText.setText(
                String.format(
                        java.util.Locale.US,
                        "Distance: %.2f km\nEstimated Fare: ₱%.2f",
                        distanceKm,
                        fare
                )
        );
    }

    private void bookRide(int paymentId) {

        if (pickupLat == 0 || pickupLng == 0) {
            Toast.makeText(
                    this,
                    "Pickup location is not ready.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        if (destinationLat == 0 || destinationLng == 0) {
            Toast.makeText(
                    this,
                    "Choose a destination on the map first.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        if (distanceKm <= 0 || fare <= 0) {
            calculateFare();
        }

        if (auth.getCurrentUser() == null) {
            Toast.makeText(
                    this,
                    "Please login again.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        String passengerId =
                auth.getCurrentUser().getUid();

        RadioButton selected =
                findViewById(paymentId);

        String paymentMethod =
                selected == null
                        ? "Cash"
                        : selected.getText().toString();

        Map<String, Object> ride = new HashMap<>();

        ride.put("passengerId", passengerId);

        ride.put("pickup", pickupField.getText().toString());
        ride.put("destination",
                destinationField.getText().toString());

        ride.put("passengerLatitude", pickupLat);
        ride.put("passengerLongitude", pickupLng);

        ride.put("destinationLatitude", destinationLat);
        ride.put("destinationLongitude", destinationLng);

        ride.put("distanceKm", distanceKm);
        ride.put("fare", fare);

        ride.put("paymentMethod", paymentMethod);

        ride.put("status", "REQUESTED");
        ride.put("paymentStatus", "PENDING");

        ride.put(
                "createdAt",
                com.google.firebase.firestore.FieldValue.serverTimestamp()
        );

        db.collection("rides")
                .add(ride)
                .addOnSuccessListener(documentReference -> {

                    currentRideId =
                            documentReference.getId();

                    rideStatusText.setText(
                            "Current Ride: REQUESTED\n" +
                            "Waiting for a driver..."
                    );

                    listenToRide(currentRideId);

                    Toast.makeText(
                            this,
                            "🚕 Ride requested successfully!",
                            Toast.LENGTH_LONG
                    ).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Booking failed: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void listenToRide(String rideId) {

        if (rideListener != null) {
            rideListener.remove();
        }

        rideListener = db.collection("rides")
                .document(rideId)
                .addSnapshotListener((snapshot, error) -> {

                    if (error != null || snapshot == null ||
                            !snapshot.exists()) {
                        return;
                    }

                    String status =
                            snapshot.getString("status");

                    if (status == null) {
                        status = "REQUESTED";
                    }

                    String driverId =
                            snapshot.getString("driverId");

                    rideStatusText.setText(
                            "Current Ride: " + status +
                            (driverId == null
                                    ? "\nWaiting for a driver..."
                                    : "\nDriver assigned.")
                    );
                });
    }

    private void openLiveMap() {

        if (pickupLat == 0 || destinationLat == 0) {
            Toast.makeText(
                    this,
                    "Please select pickup and destination first.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        Intent intent = new Intent(
                this,
                MapActivity.class
        );

        intent.putExtra(
                "ride_id",
                currentRideId
        );

        intent.putExtra(
                "passenger_latitude",
                pickupLat
        );

        intent.putExtra(
                "passenger_longitude",
                pickupLng
        );

        intent.putExtra(
                "destination_latitude",
                destinationLat
        );

        intent.putExtra(
                "destination_longitude",
                destinationLng
        );

        startActivity(intent);
    }

    private void cancelRide() {

        if (currentRideId == null ||
                currentRideId.isEmpty()) {

            Toast.makeText(
                    this,
                    "No current ride.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        db.collection("rides")
                .document(currentRideId)
                .update("status", "CANCELLED")
                .addOnSuccessListener(v -> {

                    rideStatusText.setText(
                            "Current Ride: CANCELLED"
                    );

                    Toast.makeText(
                            this,
                            "Ride cancelled.",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Cancel failed: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void logout() {

        if (rideListener != null) {
            rideListener.remove();
            rideListener = null;
        }

        if (locationManager != null) {
            try {
                locationManager.removeUpdates(
                        (LocationListener) null
                );
            } catch (Exception ignored) {
            }
        }

        auth.signOut();

        Intent intent =
                new Intent(this, MainActivity.class);

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == LOCATION_PERMISSION) {

            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                startGps();

            } else {
                gpsText.setText(
                        "📍 Location permission is required for pickup."
                );
            }
        }
    }

    @Override
    protected void onDestroy() {

        if (rideListener != null) {
            rideListener.remove();
            rideListener = null;
        }

        super.onDestroy();
    }
}
