
package com.sakyna.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PassengerActivity extends Activity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private LocationManager locationManager;

    private TextView statusText;
    private TextView pickupText;
    private TextView destinationText;
    private TextView distanceText;
    private TextView fareText;
    private TextView paymentText;

    private Button chooseDestinationButton;
    private Button bookRideButton;
    private Button cancelRideButton;
    private Button liveMapButton;
    private Button backButton;
    private Button logoutButton;

    private ScrollView scrollView;

    private Location currentLocation;

    private double destinationLatitude = 0.0;
    private double destinationLongitude = 0.0;
    private String destinationAddress = "";

    private double distanceKm = 0.0;
    private double fare = 0.0;

    private String currentRideId = "";

    private ListenerRegistration rideListener;

    private static final double BASE_FARE = 20.0;
    private static final double FARE_PER_KM = 10.0;

    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            goToLogin();
            return;
        }

        buildScreen();
        setupLocation();

        /*
         * If MapActivity returned a destination,
         * receive it here.
         */
        Intent intent = getIntent();

        if (intent != null) {
            if (intent.hasExtra("destination_latitude")) {
                destinationLatitude =
                        intent.getDoubleExtra("destination_latitude", 0.0);

                destinationLongitude =
                        intent.getDoubleExtra("destination_longitude", 0.0);

                destinationAddress =
                        intent.getStringExtra("destination_address");

                if (destinationAddress == null) {
                    destinationAddress = "";
                }

                updateDestinationDisplay();
                calculateFare();
            }
        }
    }

    private void buildScreen() {

        scrollView = new ScrollView(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(30, 30, 30, 40);

        scrollView.addView(root);

        TextView title = new TextView(this);
        title.setText("🛺 SAKAY NA");
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 10, 0, 20);

        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Passenger Home");
        subtitle.setTextSize(20);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 0, 0, 25);

        root.addView(subtitle);

        statusText = new TextView(this);
        statusText.setText("Ready to book a ride.");
        statusText.setTextSize(17);
        statusText.setPadding(0, 10, 0, 20);

        root.addView(statusText);

        pickupText = new TextView(this);
        pickupText.setText("📍 Pickup:\nGetting your current location...");
        pickupText.setTextSize(16);
        pickupText.setPadding(0, 15, 0, 15);

        root.addView(pickupText);

        destinationText = new TextView(this);
        destinationText.setText("🏁 Destination:\nNot selected");
        destinationText.setTextSize(16);
        destinationText.setPadding(0, 15, 0, 15);

        root.addView(destinationText);

        chooseDestinationButton = new Button(this);
        chooseDestinationButton.setText("🗺️ CHOOSE DESTINATION");
        chooseDestinationButton.setOnClickListener(v -> openDestinationMap());

        root.addView(chooseDestinationButton);

        distanceText = new TextView(this);
        distanceText.setText("Distance: --");
        distanceText.setTextSize(16);
        distanceText.setPadding(0, 20, 0, 10);

        root.addView(distanceText);

        fareText = new TextView(this);
        fareText.setText("Estimated Fare: ₱--");
        fareText.setTextSize(20);
        fareText.setPadding(0, 10, 0, 10);

        root.addView(fareText);

        paymentText = new TextView(this);
        paymentText.setText("Payment: Cash");
        paymentText.setTextSize(16);
        paymentText.setPadding(0, 10, 0, 20);

        root.addView(paymentText);

        bookRideButton = new Button(this);
        bookRideButton.setText("🚕 BOOK A RIDE");
        bookRideButton.setOnClickListener(v -> bookRide());

        root.addView(bookRideButton);

        cancelRideButton = new Button(this);
        cancelRideButton.setText("❌ CANCEL RIDE");
        cancelRideButton.setVisibility(View.GONE);
        cancelRideButton.setOnClickListener(v -> cancelRide());

        root.addView(cancelRideButton);

        liveMapButton = new Button(this);
        liveMapButton.setText("🗺️ LIVE RIDE MAP");
        liveMapButton.setVisibility(View.GONE);
        liveMapButton.setOnClickListener(v -> openLiveMap());

        root.addView(liveMapButton);

        /*
         * IMPORTANT:
         * This Back button no longer calls finish().
         *
         * Passenger Home is already the home screen.
         * Therefore pressing Back simply returns the screen
         * to the top instead of closing the Activity.
         */
        backButton = new Button(this);
        backButton.setText("⬆️ BACK TO PASSENGER HOME");

        backButton.setOnClickListener(v -> {
            scrollView.smoothScrollTo(0, 0);

            Toast.makeText(
                    PassengerActivity.this,
                    "You are already on Passenger Home.",
                    Toast.LENGTH_SHORT
            ).show();
        });

        root.addView(backButton);

        logoutButton = new Button(this);
        logoutButton.setText("🚪 LOG OUT");

        logoutButton.setOnClickListener(v -> logout());

        root.addView(logoutButton);

        setContentView(scrollView);
    }

    private void setupLocation() {

        locationManager =
                (LocationManager) getSystemService(LOCATION_SERVICE);

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST
            );

            return;
        }

        requestCurrentLocation();
    }

    private void requestCurrentLocation() {

        try {

            Location gpsLocation =
                    locationManager.getLastKnownLocation(
                            LocationManager.GPS_PROVIDER
                    );

            Location networkLocation =
                    locationManager.getLastKnownLocation(
                            LocationManager.NETWORK_PROVIDER
                    );

            if (gpsLocation != null) {
                currentLocation = gpsLocation;
            } else if (networkLocation != null) {
                currentLocation = networkLocation;
            }

            if (currentLocation != null) {

                pickupText.setText(
                        String.format(
                                Locale.US,
                                "📍 Pickup:\n%.6f, %.6f",
                                currentLocation.getLatitude(),
                                currentLocation.getLongitude()
                        )
                );

                return;
            }

            LocationListener listener = new LocationListener() {

                @Override
                public void onLocationChanged(@NonNull Location location) {

                    currentLocation = location;

                    pickupText.setText(
                            String.format(
                                    Locale.US,
                                    "📍 Pickup:\n%.6f, %.6f",
                                    location.getLatitude(),
                                    location.getLongitude()
                            )
                    );

                    try {
                        locationManager.removeUpdates(this);
                    } catch (Exception ignored) {
                    }
                }
            };

            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000,
                    1,
                    listener
            );

        } catch (SecurityException e) {

            Toast.makeText(
                    this,
                    "Location permission is required.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void openDestinationMap() {

        if (currentLocation == null) {
            Toast.makeText(
                    this,
                    "Waiting for your pickup location...",
                    Toast.LENGTH_LONG
            ).show();

            requestCurrentLocation();
            return;
        }

        Intent intent =
                new Intent(this, MapActivity.class);

        intent.putExtra(
                "mode",
                "SELECT_DESTINATION"
        );

        intent.putExtra(
                "passenger_latitude",
                currentLocation.getLatitude()
        );

        intent.putExtra(
                "passenger_longitude",
                currentLocation.getLongitude()
        );

        startActivityForResult(intent, 2001);
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode == 2001 &&
            resultCode == RESULT_OK &&
            data != null) {

            destinationLatitude =
                    data.getDoubleExtra(
                            "destination_latitude",
                            0.0
                    );

            destinationLongitude =
                    data.getDoubleExtra(
                            "destination_longitude",
                            0.0
                    );

            destinationAddress =
                    data.getStringExtra(
                            "destination_address"
                    );

            if (destinationAddress == null ||
                destinationAddress.trim().isEmpty()) {

                destinationAddress =
                        String.format(
                                Locale.US,
                                "%.6f, %.6f",
                                destinationLatitude,
                                destinationLongitude
                        );
            }

            updateDestinationDisplay();
            calculateFare();
        }
    }

    private void updateDestinationDisplay() {

        destinationText.setText(
                "🏁 Destination:\n" +
                destinationAddress
        );
    }

    private void calculateFare() {

        if (currentLocation == null) {
            return;
        }

        if (destinationLatitude == 0.0 &&
            destinationLongitude == 0.0) {
            return;
        }

        float[] results = new float[1];

        Location.distanceBetween(
                currentLocation.getLatitude(),
                currentLocation.getLongitude(),
                destinationLatitude,
                destinationLongitude,
                results
        );

        distanceKm =
                results[0] / 1000.0;

        fare =
                BASE_FARE +
                (distanceKm * FARE_PER_KM);

        distanceText.setText(
                String.format(
                        Locale.US,
                        "Distance: %.2f km",
                        distanceKm
                )
        );

        fareText.setText(
                String.format(
                        Locale.US,
                        "Estimated Fare: ₱%.2f",
                        fare
                )
        );
    }

    private void bookRide() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            goToLogin();
            return;
        }

        if (currentLocation == null) {
            Toast.makeText(
                    this,
                    "Pickup location is not ready yet.",
                    Toast.LENGTH_LONG
            ).show();

            requestCurrentLocation();
            return;
        }

        if (destinationLatitude == 0.0 &&
            destinationLongitude == 0.0) {

            Toast.makeText(
                    this,
                    "Please choose your destination first.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        calculateFare();

        Map<String, Object> ride =
                new HashMap<>();

        ride.put(
                "passengerId",
                user.getUid()
        );

        ride.put(
                "pickup",
                String.format(
                        Locale.US,
                        "%.6f, %.6f",
                        currentLocation.getLatitude(),
                        currentLocation.getLongitude()
                )
        );

        ride.put(
                "destination",
                destinationAddress
        );

        ride.put(
                "passengerLatitude",
                currentLocation.getLatitude()
        );

        ride.put(
                "passengerLongitude",
                currentLocation.getLongitude()
        );

        ride.put(
                "destinationLatitude",
                destinationLatitude
        );

        ride.put(
                "destinationLongitude",
                destinationLongitude
        );

        ride.put(
                "distanceKm",
                distanceKm
        );

        ride.put(
                "fare",
                fare
        );

        ride.put(
                "paymentMethod",
                "CASH"
        );

        ride.put(
                "status",
                "REQUESTED"
        );

        ride.put(
                "paymentStatus",
                "PENDING"
        );

        ride.put(
                "createdAt",
                FieldValue.serverTimestamp()
        );

        statusText.setText(
                "⏳ Requesting a driver..."
        );

        bookRideButton.setEnabled(false);

        db.collection("rides")
                .add(ride)
                .addOnSuccessListener(documentReference -> {

                    currentRideId =
                            documentReference.getId();

                    statusText.setText(
                            "🟢 Ride requested. Waiting for a driver."
                    );

                    cancelRideButton.setVisibility(
                            View.VISIBLE
                    );

                    liveMapButton.setVisibility(
                            View.VISIBLE
                    );

                    listenForRide();

                    Toast.makeText(
                            PassengerActivity.this,
                            "Ride requested successfully.",
                            Toast.LENGTH_LONG
                    ).show();
                })
                .addOnFailureListener(e -> {

                    bookRideButton.setEnabled(true);

                    statusText.setText(
                            "Unable to request ride."
                    );

                    Toast.makeText(
                            PassengerActivity.this,
                            "Booking failed: " +
                            e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void listenForRide() {

        if (currentRideId == null ||
            currentRideId.isEmpty()) {
            return;
        }

        if (rideListener != null) {
            rideListener.remove();
        }

        rideListener =
                db.collection("rides")
                        .document(currentRideId)
                        .addSnapshotListener(
                                (snapshot, error) -> {

            if (error != null) {
                return;
            }

            if (snapshot == null ||
                !snapshot.exists()) {
                return;
            }

            String status =
                    snapshot.getString("status");

            if (status == null) {
                return;
            }

            switch (status) {

                case "REQUESTED":

                    statusText.setText(
                            "⏳ Searching for a driver..."
                    );

                    break;

                case "ACCEPTED":

                    statusText.setText(
                            "🟢 Driver accepted your ride!"
                    );

                    break;

                case "ARRIVING":

                    statusText.setText(
                            "🚕 Your driver is coming."
                    );

                    break;

                case "IN_PROGRESS":

                    statusText.setText(
                            "🛺 Ride in progress."
                    );

                    break;

                case "COMPLETED":

                    statusText.setText(
                            "✅ Ride completed."
                    );

                    cancelRideButton.setVisibility(
                            View.GONE
                    );

                    break;

                case "CANCELLED":

                    statusText.setText(
                            "❌ Ride cancelled."
                    );

                    cancelRideButton.setVisibility(
                            View.GONE
                    );

                    liveMapButton.setVisibility(
                            View.GONE
                    );

                    bookRideButton.setEnabled(true);

                    currentRideId = "";

                    if (rideListener != null) {
                        rideListener.remove();
                        rideListener = null;
                    }

                    break;

                default:

                    statusText.setText(
                            "Ride status: " + status
                    );

                    break;
            }
        });
    }

    private void cancelRide() {

        if (currentRideId == null ||
            currentRideId.isEmpty()) {

            Toast.makeText(
                    this,
                    "There is no active ride.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String rideId =
                currentRideId;

        db.collection("rides")
                .document(rideId)
                .update(
                        "status",
                        "CANCELLED"
                )
                .addOnSuccessListener(v -> {

                    statusText.setText(
                            "❌ Ride cancelled. You can book again."
                    );

                    cancelRideButton.setVisibility(
                            View.GONE
                    );

                    liveMapButton.setVisibility(
                            View.GONE
                    );

                    bookRideButton.setEnabled(true);

                    currentRideId = "";

                    if (rideListener != null) {
                        rideListener.remove();
                        rideListener = null;
                    }

                    Toast.makeText(
                            PassengerActivity.this,
                            "Ride cancelled.",
                            Toast.LENGTH_SHORT
                    ).show();

                    /*
                     * IMPORTANT:
                     * DO NOT call finish().
                     * DO NOT call auth.signOut().
                     *
                     * Passenger stays on Passenger Home.
                     */
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            PassengerActivity.this,
                            "Unable to cancel ride: " +
                            e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void openLiveMap() {

        if (currentRideId == null ||
            currentRideId.isEmpty()) {

            Toast.makeText(
                    this,
                    "There is no active ride.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Intent intent =
                new Intent(
                        this,
                        MapActivity.class
                );

        intent.putExtra(
                "ride_id",
                currentRideId
        );

        startActivity(intent);
    }

    private void logout() {

        if (rideListener != null) {
            rideListener.remove();
            rideListener = null;
        }

        auth.signOut();

        Intent intent =
                new Intent(
                        PassengerActivity.this,
                        MainActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        finish();
    }

    /*
     * IMPORTANT:
     *
     * Android Back button no longer closes PassengerActivity.
     * It simply moves the screen back to the top.
     *
     * This prevents:
     *
     * Passenger Home
     *      ↓ Back
     * MainActivity
     *      ↓ auth.signOut()
     * Login screen
     */
    @Override
    public void onBackPressed() {

        if (scrollView != null) {
            scrollView.smoothScrollTo(0, 0);
        }

        Toast.makeText(
                this,
                "You are already on Passenger Home.",
                Toast.LENGTH_SHORT
        ).show();
    }

    @Override
    protected void onDestroy() {

        if (rideListener != null) {
            rideListener.remove();
            rideListener = null;
        }

        super.onDestroy();
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

        if (requestCode ==
                LOCATION_PERMISSION_REQUEST) {

            if (grantResults.length > 0 &&
                grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED) {

                requestCurrentLocation();

            } else {

                Toast.makeText(
                        this,
                        "Location permission is required to book a ride.",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    private void goToLogin() {

        Intent intent =
                new Intent(
                        PassengerActivity.this,
                        MainActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        finish();
    }
}

Do this now

1. GitHub → "app/src/main/java/com/sakyna/app/"
2. Open "PassengerActivity.java"
3. Tap Edit
4. Delete everything
5. Paste the complete code above.
6. Commit changes to "main".
7. Let Build Sakay Na run.

We want 🟢 GREEN first.

After installing that APK, test this exact sequence:

Login → Passenger → Book Ride → Cancel Ride → tap Back

Expected result: you remain on Passenger Home. No login screen.
