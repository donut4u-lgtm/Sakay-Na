
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
import android.widget.RadioButton;
import android.widget.RadioGroup;
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

    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private static final int DESTINATION_REQUEST = 2001;

    private static final double BASE_FARE = 20.00;
    private static final double FARE_PER_KM = 10.00;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseUser user;

    private LocationManager locationManager;
    private Location currentLocation;

    private double pickupLat = 0.0;
    private double pickupLng = 0.0;

    private double destinationLat = 0.0;
    private double destinationLng = 0.0;

    private double distanceKm = 0.0;
    private double fare = 0.0;

    private String currentRideId = "";
    private ListenerRegistration rideListener;

    private TextView statusText;
    private TextView pickupText;
    private TextView destinationText;
    private TextView distanceText;
    private TextView fareText;
    private TextView paymentSummaryText;

    private RadioGroup paymentGroup;
    private RadioButton cashButton;
    private RadioButton gcashButton;
    private RadioButton mayaButton;

    private Button chooseDestinationButton;
    private Button bookButton;
    private Button cancelButton;
    private Button liveMapButton;
    private Button backButton;
    private Button logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        user = auth.getCurrentUser();

        if (user == null) {
            goToLogin();
            return;
        }

        buildScreen();
        setupLocation();
    }

    private void buildScreen() {

        ScrollView scrollView = new ScrollView(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(35, 35, 35, 45);

        scrollView.addView(root);

        TextView title = new TextView(this);
        title.setText("🛺 SAKAY NA");
        title.setTextSize(30);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 10, 0, 5);

        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Passenger Home");
        subtitle.setTextSize(18);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 0, 0, 20);

        root.addView(subtitle);

        statusText = new TextView(this);
        statusText.setText("Status: Ready");
        statusText.setTextSize(17);
        statusText.setPadding(0, 10, 0, 20);

        root.addView(statusText);

        pickupText = new TextView(this);
        pickupText.setText("📍 Pickup: Getting your location...");
        pickupText.setTextSize(17);
        pickupText.setPadding(0, 10, 0, 10);

        root.addView(pickupText);

        destinationText = new TextView(this);
        destinationText.setText("🏁 Destination: Not selected");
        destinationText.setTextSize(17);
        destinationText.setPadding(0, 10, 0, 10);

        root.addView(destinationText);

        chooseDestinationButton = new Button(this);
        chooseDestinationButton.setText("🗺️ CHOOSE DESTINATION");
        chooseDestinationButton.setOnClickListener(v -> openDestinationMap());

        root.addView(chooseDestinationButton);

        distanceText = new TextView(this);
        distanceText.setText("📏 Distance: -- km");
        distanceText.setTextSize(18);
        distanceText.setPadding(0, 20, 0, 5);

        root.addView(distanceText);

        fareText = new TextView(this);
        fareText.setText("💰 Estimated Fare: Select destination");
        fareText.setTextSize(21);
        fareText.setPadding(0, 5, 0, 20);

        root.addView(fareText);

        TextView fareInfo = new TextView(this);
        fareInfo.setText(
                "Fare calculation:\n" +
                "Base fare: ₱20.00\n" +
                "Additional: ₱10.00 per km"
        );
        fareInfo.setTextSize(14);
        fareInfo.setPadding(0, 0, 0, 20);

        root.addView(fareInfo);

        TextView paymentTitle = new TextView(this);
        paymentTitle.setText("💳 PAYMENT METHOD");
        paymentTitle.setTextSize(19);
        paymentTitle.setPadding(0, 10, 0, 10);

        root.addView(paymentTitle);

        paymentGroup = new RadioGroup(this);
        paymentGroup.setOrientation(RadioGroup.VERTICAL);

        cashButton = new RadioButton(this);
        cashButton.setText("💵 Cash");
        cashButton.setTextSize(17);
        cashButton.setId(View.generateViewId());

        gcashButton = new RadioButton(this);
        gcashButton.setText("🟢 GCash");
        gcashButton.setTextSize(17);
        gcashButton.setId(View.generateViewId());

        mayaButton = new RadioButton(this);
        mayaButton.setText("🟣 Maya (PayMaya)");
        mayaButton.setTextSize(17);
        mayaButton.setId(View.generateViewId());

        paymentGroup.addView(cashButton);
        paymentGroup.addView(gcashButton);
        paymentGroup.addView(mayaButton);

        cashButton.setChecked(true);

        root.addView(paymentGroup);

        paymentSummaryText = new TextView(this);
        paymentSummaryText.setText("Selected payment: Cash");
        paymentSummaryText.setTextSize(16);
        paymentSummaryText.setPadding(0, 10, 0, 20);

        root.addView(paymentSummaryText);

        paymentGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == cashButton.getId()) {
                paymentSummaryText.setText("Selected payment: Cash");
            } else if (checkedId == gcashButton.getId()) {
                paymentSummaryText.setText("Selected payment: GCash");
            } else if (checkedId == mayaButton.getId()) {
                paymentSummaryText.setText("Selected payment: Maya (PayMaya)");
            }
        });

        bookButton = new Button(this);
        bookButton.setText("🛺 BOOK SAKAY");
        bookButton.setOnClickListener(v -> bookRide());

        root.addView(bookButton);

        cancelButton = new Button(this);
        cancelButton.setText("❌ CANCEL RIDE");
        cancelButton.setOnClickListener(v -> cancelRide());

        root.addView(cancelButton);

        liveMapButton = new Button(this);
        liveMapButton.setText("📍 LIVE RIDE MAP");
        liveMapButton.setOnClickListener(v -> openLiveMap());

        root.addView(liveMapButton);

        backButton = new Button(this);
        backButton.setText("⬅️ BACK");
        backButton.setOnClickListener(v -> finish());

        root.addView(backButton);

        logoutButton = new Button(this);
        logoutButton.setText("🚪 LOG OUT");
        logoutButton.setOnClickListener(v -> logout());

        root.addView(logoutButton);

        setContentView(scrollView);

        cancelButton.setVisibility(View.GONE);
        liveMapButton.setVisibility(View.GONE);
    }

    private void setupLocation() {

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

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

            pickupText.setText("📍 Pickup: Location permission required");
            return;
        }

        requestCurrentLocation();
    }

    private void requestCurrentLocation() {

        if (locationManager == null) {
            pickupText.setText("📍 Pickup: Location service unavailable");
            return;
        }

        try {

            Location gpsLocation = null;
            Location networkLocation = null;

            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                gpsLocation = locationManager.getLastKnownLocation(
                        LocationManager.GPS_PROVIDER
                );
            }

            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                networkLocation = locationManager.getLastKnownLocation(
                        LocationManager.NETWORK_PROVIDER
                );
            }

            Location best = chooseBestLocation(gpsLocation, networkLocation);

            if (best != null) {
                updateLocation(best);
            } else {
                pickupText.setText("📍 Pickup: Waiting for GPS location...");
            }

            LocationListener listener = new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    updateLocation(location);
                }
            };

            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        3000,
                        5,
                        listener
                );
            }

            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        3000,
                        5,
                        listener
                );
            }

        } catch (SecurityException e) {
            pickupText.setText("📍 Pickup: Location permission denied");
        } catch (Exception e) {
            pickupText.setText("📍 Pickup: Unable to get location");
        }
    }

    private Location chooseBestLocation(Location first, Location second) {

        if (first == null) {
            return second;
        }

        if (second == null) {
            return first;
        }

        if (first.getTime() >= second.getTime()) {
            return first;
        }

        return second;
    }

    private void updateLocation(Location location) {

        if (location == null) {
            return;
        }

        currentLocation = location;

        pickupLat = location.getLatitude();
        pickupLng = location.getLongitude();

        updatePickupDisplay();

        if (destinationLat != 0.0 && destinationLng != 0.0) {
            calculateFare();
        }
    }

    private void updatePickupDisplay() {

        if (pickupLat == 0.0 && pickupLng == 0.0) {
            pickupText.setText("📍 Pickup: Getting location...");
            return;
        }

        pickupText.setText(
                String.format(
                        Locale.US,
                        "📍 Pickup:\n%.6f, %.6f",
                        pickupLat,
                        pickupLng
                )
        );
    }

    private void openDestinationMap() {

        if (pickupLat == 0.0 && pickupLng == 0.0) {
            Toast.makeText(
                    this,
                    "Please wait for your current location first.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        Intent intent = new Intent(this, MapActivity.class);

        intent.putExtra("mode", "SELECT_DESTINATION");
        intent.putExtra("pickup_latitude", pickupLat);
        intent.putExtra("pickup_longitude", pickupLng);

        startActivityForResult(intent, DESTINATION_REQUEST);
    }

    private void readDestinationFromIntent(Intent data) {

        if (data == null) {
            return;
        }

        double lat = data.getDoubleExtra("destination_latitude", 0.0);
        double lng = data.getDoubleExtra("destination_longitude", 0.0);

        String address = data.getStringExtra("destination_address");

        if (lat == 0.0 || lng == 0.0) {
            Toast.makeText(
                    this,
                    "Destination location is invalid.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        destinationLat = lat;
        destinationLng = lng;

        if (address == null || address.trim().isEmpty()) {
            address = String.format(
                    Locale.US,
                    "%.6f, %.6f",
                    destinationLat,
                    destinationLng
            );
        }

        destinationText.setText(
                "🏁 Destination:\n" + address
        );

        calculateFare();
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == DESTINATION_REQUEST &&
                resultCode == RESULT_OK) {

            readDestinationFromIntent(data);
        }
    }

    private void calculateFare() {

        if (pickupLat == 0.0 ||
                pickupLng == 0.0 ||
                destinationLat == 0.0 ||
                destinationLng == 0.0) {

            distanceText.setText("📏 Distance: -- km");
            fareText.setText("💰 Estimated Fare: Select destination");
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

        fare = BASE_FARE + (distanceKm * FARE_PER_KM);

        distanceText.setText(
                String.format(
                        Locale.US,
                        "📏 Distance: %.2f km",
                        distanceKm
                )
        );

        fareText.setText(
                String.format(
                        Locale.US,
                        "💰 Estimated Fare: ₱%.2f",
                        fare
                )
        );
    }

    private String getSelectedPaymentMethod() {

        int selectedId = paymentGroup.getCheckedRadioButtonId();

        if (selectedId == gcashButton.getId()) {
            return "GCASH";
        }

        if (selectedId == mayaButton.getId()) {
            return "MAYA";
        }

        return "CASH";
    }

    private void bookRide() {

        if (user == null) {
            goToLogin();
            return;
        }

        if (pickupLat == 0.0 || pickupLng == 0.0) {
            Toast.makeText(
                    this,
                    "Waiting for your pickup location.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        if (destinationLat == 0.0 || destinationLng == 0.0) {
            Toast.makeText(
                    this,
                    "Please choose your destination first.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        calculateFare();

        if (fare <= 0.0) {
            Toast.makeText(
                    this,
                    "Fare calculation failed. Please select destination again.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        String paymentMethod = getSelectedPaymentMethod();

        String paymentLabel;

        if ("GCASH".equals(paymentMethod)) {
            paymentLabel = "GCash";
        } else if ("MAYA".equals(paymentMethod)) {
            paymentLabel = "Maya (PayMaya)";
        } else {
            paymentLabel = "Cash";
        }

        Map<String, Object> ride = new HashMap<>();

        ride.put("passengerId", user.getUid());

        ride.put(
                "passengerPhone",
                user.getPhoneNumber() == null
                        ? ""
                        : user.getPhoneNumber()
        );

        ride.put(
                "pickup",
                String.format(
                        Locale.US,
                        "%.6f, %.6f",
                        pickupLat,
                        pickupLng
                )
        );

        ride.put(
                "destination",
                String.format(
                        Locale.US,
                        "%.6f, %.6f",
                        destinationLat,
                        destinationLng
                )
        );

        ride.put("passengerLatitude", pickupLat);
        ride.put("passengerLongitude", pickupLng);

        ride.put("destinationLatitude", destinationLat);
        ride.put("destinationLongitude", destinationLng);

        ride.put("distanceKm", distanceKm);
        ride.put("fare", fare);

        ride.put("paymentMethod", paymentMethod);

        // Payment gateway is not connected yet.
        // CASH / GCASH / MAYA are selected methods only.
        ride.put("paymentStatus", "PENDING");

        ride.put("status", "REQUESTED");
        ride.put("createdAt", FieldValue.serverTimestamp());

        bookButton.setEnabled(false);

        db.collection("rides")
                .add(ride)
                .addOnSuccessListener(documentReference -> {

                    currentRideId = documentReference.getId();

                    statusText.setText(
                            "Status: RIDE REQUESTED"
                    );

                    cancelButton.setVisibility(View.VISIBLE);
                    liveMapButton.setVisibility(View.VISIBLE);

                    Toast.makeText(
                            this,
                            "Ride requested.\nPayment: " + paymentLabel +
                                    "\nFare: ₱" +
                                    String.format(
                                            Locale.US,
                                            "%.2f",
                                            fare
                                    ),
                            Toast.LENGTH_LONG
                    ).show();

                    listenForRide();

                })
                .addOnFailureListener(e -> {

                    bookButton.setEnabled(true);

                    Toast.makeText(
                            this,
                            "Booking failed: " + e.getMessage(),
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

        rideListener = db.collection("rides")
                .document(currentRideId)
                .addSnapshotListener((snapshot, error) -> {

                    if (error != null) {
                        statusText.setText(
                                "Status: Connection error"
                        );
                        return;
                    }

                    if (snapshot == null || !snapshot.exists()) {
                        return;
                    }

                    String status = snapshot.getString("status");

                    if (status == null) {
                        status = "UNKNOWN";
                    }

                    String displayStatus = status;

                    if ("REQUESTED".equals(status)) {
                        displayStatus = "WAITING FOR DRIVER";
                    } else if ("ACCEPTED".equals(status)) {
                        displayStatus = "DRIVER ACCEPTED";
                    } else if ("ARRIVING".equals(status)) {
                        displayStatus = "DRIVER IS ARRIVING";
                    } else if ("IN_PROGRESS".equals(status)) {
                        displayStatus = "RIDE IN PROGRESS";
                    } else if ("COMPLETED".equals(status)) {
                        displayStatus = "RIDE COMPLETED";
                    } else if ("CANCELLED".equals(status)) {
                        displayStatus = "RIDE CANCELLED";
                    }

                    statusText.setText(
                            "Status: " + displayStatus
                    );

                    if ("COMPLETED".equals(status) ||
                            "CANCELLED".equals(status)) {

                        cancelButton.setVisibility(View.GONE);
                    }
                });
    }

    private void cancelRide() {

        if (currentRideId == null ||
                currentRideId.isEmpty()) {

            Toast.makeText(
                    this,
                    "No active ride.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        db.collection("rides")
                .document(currentRideId)
                .update("status", "CANCELLED")
                .addOnSuccessListener(unused -> {

                    statusText.setText(
                            "Status: RIDE CANCELLED"
                    );

                    cancelButton.setVisibility(View.GONE);

                    Toast.makeText(
                            this,
                            "Ride cancelled.",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
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

        Intent intent = new Intent(
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
        goToLogin();
    }

    private void goToLogin() {

        try {

            Intent intent = new Intent(
                    this,
                    MainActivity.class
            );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
                            Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(intent);
            finish();

        } catch (Exception e) {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (rideListener != null) {
            rideListener.remove();
            rideListener = null;
        }

        try {
            if (locationManager != null) {
                locationManager.removeUpdates(
                        new LocationListener() {
                            @Override
                            public void onLocationChanged(
                                    @NonNull Location location
                            ) {
                            }
                        }
                );
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == LOCATION_PERMISSION_REQUEST) {

            if (grantResults.length > 0 &&
                    grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED) {

                requestCurrentLocation();

            } else {

                pickupText.setText(
                        "📍 Pickup: Location permission denied"
                );

                Toast.makeText(
                        this,
                        "Location permission is required for pickup and fare calculation.",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }
}
