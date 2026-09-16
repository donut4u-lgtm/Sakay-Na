
```java
package com.sakyna.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PassengerActivity extends Activity {

    private static final int LOCATION_PERMISSION_REQUEST = 7001;

    private static final double BASE_FARE = 20.0;
    private static final double FARE_PER_KM = 10.0;
    private static final double MIN_FARE = 50.0;
    private static final double MAX_FARE = 500.0;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private EditText pickupInput;
    private EditText destinationInput;

    private TextView statusText;
    private TextView fareText;
    private TextView locationText;

    private Button bookButton;
    private Button cancelButton;
    private Button liveMapButton;
    private Button chatButton;
    private Button historyButton;
    private Button logoutButton;

    private RadioGroup paymentGroup;

    private LocationManager locationManager;
    private Location currentLocation;

    private ListenerRegistration rideListener;

    private String currentRideId = "";

    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        preferences =
                getSharedPreferences(
                        "SakayNa",
                        MODE_PRIVATE
                );

        buildScreen();
        requestLocation();
        restoreActiveRide();
    }

    // ------------------------------------------------------------
    // SCREEN
    // ------------------------------------------------------------

    private void buildScreen() {

        ScrollView scrollView =
                new ScrollView(this);

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setPadding(
                32,
                32,
                32,
                40
        );

        scrollView.addView(root);

        TextView title =
                new TextView(this);

        title.setText("SAKAY NA");
        title.setTextSize(30);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 10, 0, 8);

        root.addView(title);

        TextView subtitle =
                new TextView(this);

        subtitle.setText(
                "Passenger • Book your tricycle ride"
        );

        subtitle.setTextSize(16);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 0, 0, 25);

        root.addView(subtitle);

        locationText =
                new TextView(this);

        locationText.setText(
                "Location: getting GPS location..."
        );

        locationText.setTextSize(15);
        locationText.setPadding(0, 10, 0, 20);

        root.addView(locationText);

        TextView pickupLabel =
                new TextView(this);

        pickupLabel.setText(
                "Pickup location"
        );

        pickupLabel.setTextSize(16);

        root.addView(pickupLabel);

        pickupInput =
                new EditText(this);

        pickupInput.setHint(
                "Where should the driver pick you up?"
        );

        pickupInput.setSingleLine(false);
        pickupInput.setMinLines(2);

        root.addView(pickupInput);

        TextView destinationLabel =
                new TextView(this);

        destinationLabel.setText(
                "Destination"
        );

        destinationLabel.setTextSize(16);
        destinationLabel.setPadding(0, 20, 0, 0);

        root.addView(destinationLabel);

        destinationInput =
                new EditText(this);

        destinationInput.setHint(
                "Where are you going?"
        );

        destinationInput.setSingleLine(false);
        destinationInput.setMinLines(2);

        root.addView(destinationInput);

        TextView paymentLabel =
                new TextView(this);

        paymentLabel.setText(
                "Payment method"
        );

        paymentLabel.setTextSize(16);
        paymentLabel.setPadding(0, 22, 0, 5);

        root.addView(paymentLabel);

        paymentGroup =
                new RadioGroup(this);

        paymentGroup.setOrientation(
                RadioGroup.VERTICAL
        );

        RadioButton cash =
                new RadioButton(this);

        cash.setText("Cash");
        cash.setId(View.generateViewId());
        cash.setChecked(true);

        paymentGroup.addView(cash);

        RadioButton gcash =
                new RadioButton(this);

        gcash.setText("GCash");
        gcash.setId(View.generateViewId());

        paymentGroup.addView(gcash);

        RadioButton maya =
                new RadioButton(this);

        maya.setText("Maya");
        maya.setId(View.generateViewId());

        paymentGroup.addView(maya);

        root.addView(paymentGroup);

        fareText =
                new TextView(this);

        fareText.setText(
                "Estimated fare: ₱50.00 minimum"
        );

        fareText.setTextSize(19);
        fareText.setPadding(0, 20, 0, 15);

        root.addView(fareText);

        bookButton =
                makeButton("BOOK A RIDE");

        root.addView(bookButton);

        statusText =
                new TextView(this);

        statusText.setText(
                "Status: Ready to book"
        );

        statusText.setTextSize(17);
        statusText.setPadding(0, 20, 0, 15);

        root.addView(statusText);

        cancelButton =
                makeButton("CANCEL RIDE");

        cancelButton.setVisibility(
                View.GONE
        );

        root.addView(cancelButton);

        liveMapButton =
                makeButton("LIVE RIDE MAP");

        liveMapButton.setVisibility(
                View.GONE
        );

        root.addView(liveMapButton);

        chatButton =
                makeButton("CHAT WITH DRIVER");

        chatButton.setVisibility(
                View.GONE
        );

        root.addView(chatButton);

        historyButton =
                makeButton("RIDE HISTORY");

        root.addView(historyButton);

        logoutButton =
                makeButton("LOGOUT");

        root.addView(logoutButton);

        setContentView(scrollView);

        bookButton.setOnClickListener(
                v -> bookRide()
        );

        cancelButton.setOnClickListener(
                v -> cancelRide()
        );

        liveMapButton.setOnClickListener(
                v -> openLiveMap()
        );

        chatButton.setOnClickListener(
                v -> openChat()
        );

        historyButton.setOnClickListener(
                v -> showRideHistory()
        );

        logoutButton.setOnClickListener(
                v -> logout()
        );

        destinationInput.setOnFocusChangeListener(
                (v, hasFocus) -> {

                    if (!hasFocus) {
                        calculateDisplayedFare();
                    }
                }
        );
    }

    private Button makeButton(String text) {

        Button button =
                new Button(this);

        button.setText(text);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(
                0,
                8,
                0,
                8
        );

        button.setLayoutParams(params);

        return button;
    }

    // ------------------------------------------------------------
    // AUTH
    // ------------------------------------------------------------

    private FirebaseUser getCurrentUser() {

        if (auth == null) {
            return null;
        }

        return auth.getCurrentUser();
    }

    // ------------------------------------------------------------
    // LOCATION
    // ------------------------------------------------------------

    private void requestLocation() {

        locationManager =
                (LocationManager)
                        getSystemService(
                                LOCATION_SERVICE
                        );

        if (locationManager == null) {

            locationText.setText(
                    "Location unavailable."
            );

            return;
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST
            );

            return;
        }

        startLocationUpdates();
    }

    private void startLocationUpdates() {

        if (locationManager == null) {
            return;
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {

            return;
        }

        Location last =
                locationManager.getLastKnownLocation(
                        LocationManager.GPS_PROVIDER
                );

        if (last != null) {
            updateLocation(last);
        }

        try {

            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    3000,
                    5,
                    locationListener
            );

        } catch (Exception ignored) {
        }

        try {

            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    5000,
                    10,
                    locationListener
            );

        } catch (Exception ignored) {
        }
    }

    private final LocationListener locationListener =
            new LocationListener() {

                @Override
                public void onLocationChanged(
                        @NonNull Location location
                ) {

                    updateLocation(location);
                }

                @Override
                public void onProviderEnabled(
                        @NonNull String provider
                ) {
                }

                @Override
                public void onProviderDisabled(
                        @NonNull String provider
                ) {
                }
            };

    private void updateLocation(
            Location location
    ) {

        currentLocation = location;

        String text =
                "GPS: " +
                        String.format(
                                Locale.US,
                                "%.6f, %.6f",
                                location.getLatitude(),
                                location.getLongitude()
                        );

        locationText.setText(text);
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

        if (requestCode ==
                LOCATION_PERMISSION_REQUEST) {

            if (grantResults.length > 0 &&
                    grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED) {

                startLocationUpdates();

            } else {

                locationText.setText(
                        "GPS permission not granted."
                );
            }
        }
    }

    // ------------------------------------------------------------
    // BOOK RIDE
    // ------------------------------------------------------------

    private void bookRide() {

        FirebaseUser user =
                getCurrentUser();

        if (user == null) {

            Toast.makeText(
                    this,
                    "Please log in first.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        String pickup =
                pickupInput
                        .getText()
                        .toString()
                        .trim();

        String destination =
                destinationInput
                        .getText()
                        .toString()
                        .trim();

        if (pickup.isEmpty()) {

            pickup =
                    getCurrentAddress();

            if (pickup.isEmpty()) {

                Toast.makeText(
                        this,
                        "Please enter your pickup location.",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            pickupInput.setText(pickup);
        }

        if (destination.isEmpty()) {

            Toast.makeText(
                    this,
                    "Please enter your destination.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        String paymentMethod =
                getPaymentMethod();

        double fare =
                calculateFare();

        Location destinationLocation =
                geocodeLocation(destination);

        Map<String, Object> ride =
                new HashMap<>();

        ride.put(
                "passengerId",
                user.getUid()
        );

        ride.put(
                "pickup",
                pickup
        );

        ride.put(
                "destination",
                destination
        );

        ride.put(
                "paymentMethod",
                paymentMethod
        );

        ride.put(
                "paymentStatus",
                "PENDING"
        );

        ride.put(
                "fare",
                fare
        );

        ride.put(
                "status",
                "REQUESTED"
        );

        ride.put(
                "createdAt",
                FieldValue.serverTimestamp()
        );

        if (currentLocation != null) {

            ride.put(
                    "pickupLatitude",
                    currentLocation.getLatitude()
            );

            ride.put(
                    "pickupLongitude",
                    currentLocation.getLongitude()
            );
        }

        if (destinationLocation != null) {

            ride.put(
                    "destinationLatitude",
                    destinationLocation.getLatitude()
            );

            ride.put(
                    "destinationLongitude",
                    destinationLocation.getLongitude()
            );
        }

        bookButton.setEnabled(false);

        db.collection("rides")
                .add(ride)
                .addOnSuccessListener(
                        documentReference -> {

                            currentRideId =
                                    documentReference.getId();

                            preferences.edit()
                                    .putString(
                                            "passenger_active_ride_id",
                                            currentRideId
                                    )
                                    .apply();

                            showActiveRideControls();

                            statusText.setText(
                                    "Status: WAITING FOR DRIVER"
                            );

                            Toast.makeText(
                                    this,
                                    "Ride request sent. Waiting for a driver.",
                                    Toast.LENGTH_LONG
                            ).show();

                            listenForRide();
                        }
                )
                .addOnFailureListener(
                        error -> {

                            bookButton.setEnabled(
                                    true
                            );

                            Toast.makeText(
                                    this,
                                    "Booking failed: " +
                                            error.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                );
    }

    private String getPaymentMethod() {

        int checkedId =
                paymentGroup
                        .getCheckedRadioButtonId();

        if (checkedId == -1) {
            return "Cash";
        }

        RadioButton selected =
                paymentGroup.findViewById(
                        checkedId
                );

        if (selected == null) {
            return "Cash";
        }

        return selected
                .getText()
                .toString();
    }

    // ------------------------------------------------------------
    // FARE
    // ------------------------------------------------------------

    private double calculateFare() {

        if (currentLocation == null) {
            return MIN_FARE;
        }

        String destination =
                destinationInput
                        .getText()
                        .toString()
                        .trim();

        if (destination.isEmpty()) {
            return MIN_FARE;
        }

        Location destinationLocation =
                geocodeLocation(destination);

        if (destinationLocation == null) {
            return MIN_FARE;
        }

        float meters =
                currentLocation.distanceTo(
                        destinationLocation
                );

        double kilometers =
                meters / 1000.0;

        double fare =
                BASE_FARE +
                        (kilometers *
                                FARE_PER_KM);

        if (fare < MIN_FARE) {
            fare = MIN_FARE;
        }

        if (fare > MAX_FARE) {
            fare = MAX_FARE;
        }

        return Math.round(
                fare * 100.0
        ) / 100.0;
    }

    private void calculateDisplayedFare() {

        double fare =
                calculateFare();

        fareText.setText(
                String.format(
                        Locale.US,
                        "Estimated fare: ₱%.2f",
                        fare
                )
        );
    }

    private Location geocodeLocation(
            String text
    ) {

        try {

            Geocoder geocoder =
                    new Geocoder(
                            this,
                            Locale.getDefault()
                    );

            List<Address> addresses =
                    geocoder.getFromLocationName(
                            text,
                            1
                    );

            if (addresses != null &&
                    !addresses.isEmpty()) {

                Address address =
                        addresses.get(0);

                Location location =
                        new Location(
                                "geocoder"
                        );

                location.setLatitude(
                        address.getLatitude()
                );

                location.setLongitude(
                        address.getLongitude()
                );

                return location;
            }

        } catch (IOException ignored) {
        } catch (Exception ignored) {
        }

        return null;
    }

    private String getCurrentAddress() {

        if (currentLocation == null) {
            return "";
        }

        try {

            Geocoder geocoder =
                    new Geocoder(
                            this,
                            Locale.getDefault()
                    );

            List<Address> addresses =
                    geocoder.getFromLocation(
                            currentLocation.getLatitude(),
                            currentLocation.getLongitude(),
                            1
                    );

            if (addresses != null &&
                    !addresses.isEmpty()) {

                Address address =
                        addresses.get(0);

                String value =
                        address.getAddressLine(0);

                if (value != null) {
                    return value;
                }
            }

        } catch (Exception ignored) {
        }

        return String.format(
                Locale.US,
                "%.6f, %.6f",
                currentLocation.getLatitude(),
                currentLocation.getLongitude()
        );
    }

    // ------------------------------------------------------------
    // ACTIVE RIDE
    // ------------------------------------------------------------

    private void restoreActiveRide() {

        FirebaseUser user =
                getCurrentUser();

        if (user == null) {
            return;
        }

        String savedRide =
                preferences.getString(
                        "passenger_active_ride_id",
                        ""
                );

        if (savedRide != null &&
                !savedRide.trim().isEmpty()) {

            currentRideId =
                    savedRide;

            db.collection("rides")
                    .document(currentRideId)
                    .get()
                    .addOnSuccessListener(
                            document -> {

                                if (!document.exists()) {

                                    clearActiveRide();
                                    findExistingActiveRide();

                                    return;
                                }

                                String passengerId =
                                        document.getString(
                                                "passengerId"
                                        );

                                if (!user.getUid()
                                        .equals(passengerId)) {

                                    clearActiveRide();
                                    findExistingActiveRide();

                                    return;
                                }

                                String status =
                                        document.getString(
                                                "status"
                                        );

                                if (isActiveStatus(status)) {

                                    showActiveRideControls();
                                    listenForRide();

                                } else {

                                    clearActiveRide();
                                    findExistingActiveRide();
                                }
                            }
                    )
                    .addOnFailureListener(
                            error ->
                                    findExistingActiveRide()
                    );

        } else {

            findExistingActiveRide();
        }
    }

    private void findExistingActiveRide() {

        FirebaseUser user =
                getCurrentUser();

        if (user == null) {
            return;
        }

        db.collection("rides")
                .whereEqualTo(
                        "passengerId",
                        user.getUid()
                )
                .get()
                .addOnSuccessListener(
                        querySnapshot -> {

                            DocumentSnapshot selected =
                                    null;

                            for (
                                    DocumentSnapshot document :
                                    querySnapshot.getDocuments()
                            ) {

                                String status =
                                        document.getString(
                                                "status"
                                        );

                                if (isActiveStatus(status)) {
                                    selected = document;
                                }
                            }

                            if (selected != null) {

                                currentRideId =
                                        selected.getId();

                                preferences.edit()
                                        .putString(
                                                "passenger_active_ride_id",
                                                currentRideId
                                        )
                                        .apply();

                                showActiveRideControls();
                                listenForRide();

                            } else {

                                clearActiveRide();
                            }
                        }
                );
    }

    private boolean isActiveStatus(
            String status
    ) {

        if (status == null) {
            return false;
        }

        return status.equals("REQUESTED")
                || status.equals("ACCEPTED")
                || status.equals("DRIVER_ON_THE_WAY")
                || status.equals("DRIVER_ARRIVED")
                || status.equals("IN_PROGRESS");
    }

    private void showActiveRideControls() {

        bookButton.setEnabled(false);

        cancelButton.setVisibility(
                View.VISIBLE
        );

        liveMapButton.setVisibility(
                View.VISIBLE
        );

        chatButton.setVisibility(
                View.VISIBLE
        );

        statusText.setText(
                "Status: RIDE REQUEST ACTIVE"
        );
    }

    private void clearActiveRide() {

        currentRideId = "";

        preferences.edit()
                .remove(
                        "passenger_active_ride_id"
                )
                .apply();

        if (rideListener != null) {

            rideListener.remove();
            rideListener = null;
        }

        bookButton.setEnabled(true);

        cancelButton.setVisibility(
                View.GONE
        );

        liveMapButton.setVisibility(
                View.GONE
        );

        chatButton.setVisibility(
                View.GONE
        );

        statusText.setText(
                "Status: Ready to book"
        );
    }

    // ------------------------------------------------------------
    // RIDE LISTENER
    // ------------------------------------------------------------

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

                                        Toast.makeText(
                                                this,
                                                "Ride update failed: " +
                                                        error.getMessage(),
                                                Toast.LENGTH_SHORT
                                        ).show();

                                        return;
                                    }

                                    if (snapshot == null ||
                                            !snapshot.exists()) {

                                        return;
                                    }

                                    String status =
                                            snapshot.getString(
                                                    "status"
                                            );

                                    if (status == null) {
                                        return;
                                    }

                                    updateRideStatus(
                                            status
                                    );

                                    if (status.equals(
                                            "COMPLETED"
                                    )
                                            || status.equals(
                                            "CANCELLED"
                                    )
                                            || status.equals(
                                            "DECLINED"
                                    )) {

                                        clearActiveRide();
                                    }
                                }
                        );
    }

    private void updateRideStatus(
            String status
    ) {

        String display;

        if (status.equals("REQUESTED")) {

            display =
                    "WAITING FOR DRIVER";

        } else if (status.equals("ACCEPTED")) {

            display =
                    "DRIVER ACCEPTED YOUR RIDE";

        } else if (status.equals(
                "DRIVER_ON_THE_WAY"
        )) {

            display =
                    "DRIVER IS ON THE WAY";

        } else if (status.equals(
                "DRIVER_ARRIVED"
        )) {

            display =
                    "DRIVER HAS ARRIVED";

        } else if (status.equals(
                "IN_PROGRESS"
        )) {

            display =
                    "RIDE IN PROGRESS";

        } else if (status.equals(
                "FINISHED"
        )) {

            display =
                    "RIDE FINISHED — PAYMENT REQUIRED";

        } else if (status.equals(
                "COMPLETED"
        )) {

            display =
                    "RIDE COMPLETED";

        } else if (status.equals(
                "CANCELLED"
        )) {

            display =
                    "RIDE CANCELLED";

        } else if (status.equals(
                "DECLINED"
        )) {

            display =
                    "DRIVER DECLINED THE RIDE";

        } else {

            display = status;
        }

        statusText.setText(
                "Status: " + display
        );

        if (status.equals("FINISHED")) {

            cancelButton.setVisibility(
                    View.GONE
            );
        }

        if (status.equals("COMPLETED")
                || status.equals("CANCELLED")
                || status.equals("DECLINED")) {

            cancelButton.setVisibility(
                    View.GONE
            );

            liveMapButton.setVisibility(
                    View.GONE
            );

            chatButton.setVisibility(
                    View.GONE
            );

            bookButton.setEnabled(true);
        }
    }

    // ------------------------------------------------------------
    // CHAT
    // ------------------------------------------------------------

    private void openChat() {

        verifyRideBeforeOpening(true);
    }

    // ------------------------------------------------------------
    // LIVE MAP
    // ------------------------------------------------------------

    private void openLiveMap() {

        verifyRideBeforeOpening(false);
    }

    private void verifyRideBeforeOpening(
            boolean openChat
    ) {

        FirebaseUser user =
                getCurrentUser();

        if (user == null) {

            Toast.makeText(
                    this,
                    "Please log in again.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (currentRideId == null ||
                currentRideId.trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "No active ride.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        String rideId =
                currentRideId;

        db.collection("rides")
                .document(rideId)
                .get()
                .addOnSuccessListener(
                        document -> {

                            if (!document.exists()) {

                                Toast.makeText(
                                        this,
                                        "Ride no longer exists.",
                                        Toast.LENGTH_LONG
                                ).show();

                                clearActiveRide();
                                return;
                            }

                            String passengerId =
                                    document.getString(
                                            "passengerId"
                                    );

                            if (passengerId == null ||
                                    !passengerId.equals(
                                            user.getUid()
                                    )) {

                                Toast.makeText(
                                        this,
                                        "This ride does not belong to this passenger.",
                                        Toast.LENGTH_LONG
                                ).show();

                                return;
                            }

                            String status =
                                    document.getString(
                                            "status"
                                    );

                            if (openChat) {

                                Intent intent =
                                        new Intent(
                                                PassengerActivity.this,
                                                RideChatActivity.class
                                        );

                                intent.putExtra(
                                        "ride_id",
                                        rideId
                                );

                                intent.putExtra(
                                        "rideId",
                                        rideId
                                );

                                startActivity(intent);

                            } else {

                                Intent intent =
                                        new Intent(
                                                PassengerActivity.this,
                                                MapActivity.class
                                        );

                                intent.putExtra(
                                        "mode",
                                        "LIVE_RIDE"
                                );

                                intent.putExtra(
                                        "ride_id",
                                        rideId
                                );

                                intent.putExtra(
                                        "rideId",
                                        rideId
                                );

                                intent.putExtra(
                                        "pickup",
                                        document.getString(
                                                "pickup"
                                        )
                                );

                                intent.putExtra(
                                        "destination",
                                        document.getString(
                                                "destination"
                                        )
                                );

                                intent.putExtra(
                                        "pickup_address",
                                        document.getString(
                                                "pickup"
                                        )
                                );

                                intent.putExtra(
                                        "destination_address",
                                        document.getString(
                                                "destination"
                                        )
                                );

                                Double pickupLatitude =
                                        document.getDouble(
                                                "pickupLatitude"
                                        );

                                Double pickupLongitude =
                                        document.getDouble(
                                                "pickupLongitude"
                                        );

                                Double destinationLatitude =
                                        document.getDouble(
                                                "destinationLatitude"
                                        );

                                Double destinationLongitude =
                                        document.getDouble(
                                                "destinationLongitude"
                                        );

                                if (pickupLatitude != null) {

                                    intent.putExtra(
                                            "pickup_latitude",
                                            pickupLatitude
                                    );
                                }

                                if (pickupLongitude != null) {

                                    intent.putExtra(
                                            "pickup_longitude",
                                            pickupLongitude
                                    );
                                }

                                if (destinationLatitude != null) {

                                    intent.putExtra(
                                            "destination_latitude",
                                            destinationLatitude
                                    );
                                }

                                if (destinationLongitude != null) {

                                    intent.putExtra(
                                            "destination_longitude",
                                            destinationLongitude
                                    );
                                }

                                intent.putExtra(
                                        "status",
                                        status
                                );

                                startActivity(intent);
                            }
                        }
                )
                .addOnFailureListener(
                        error ->
                                Toast.makeText(
                                        this,
                                        "Unable to open ride: " +
                                                error.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show()
                );
    }

    // ------------------------------------------------------------
    // CANCEL
    // ------------------------------------------------------------

    private void cancelRide() {

        FirebaseUser user =
                getCurrentUser();

        if (user == null) {
            return;
        }

        if (currentRideId == null ||
                currentRideId.isEmpty()) {

            return;
        }

        String rideId =
                currentRideId;

        db.collection("rides")
                .document(rideId)
                .get()
                .addOnSuccessListener(
                        document -> {

                            if (!document.exists()) {

                                clearActiveRide();
                                return;
                            }

                            String passengerId =
                                    document.getString(
                                            "passengerId"
                                    );

                            if (!user.getUid().equals(
                                    passengerId
                            )) {

                                Toast.makeText(
                                        this,
                                        "You cannot cancel this ride.",
                                        Toast.LENGTH_LONG
                                ).show();

                                return;
                            }

                            String status =
                                    document.getString(
                                            "status"
                                    );

                            if (status == null ||
                                    !isActiveStatus(status)) {

                                Toast.makeText(
                                        this,
                                        "This ride can no longer be cancelled.",
                                        Toast.LENGTH_LONG
                                ).show();

                                return;
                            }

                            Map<String, Object> updates =
                                    new HashMap<>();

                            updates.put(
                                    "status",
                                    "CANCELLED"
                            );

                            updates.put(
                                    "cancelledBy",
                                    "PASSENGER"
                            );

                            updates.put(
                                    "passengerId",
                                    passengerId
                            );

                            String driverId =
                                    document.getString(
                                            "driverId"
                                    );

                            if (driverId != null) {

                                updates.put(
                                        "driverId",
                                        driverId
                                );
                            }

                            db.collection("rides")
                                    .document(rideId)
                                    .update(updates)
                                    .addOnSuccessListener(
                                            v -> {

                                                Toast.makeText(
                                                        this,
                                                        "Ride cancelled.",
                                                        Toast.LENGTH_LONG
                                                ).show();

                                                clearActiveRide();
                                            }
                                    )
                                    .addOnFailureListener(
                                            error ->
                                                    Toast.makeText(
                                                            this,
                                                            "Cancel failed: " +
                                                                    error.getMessage(),
                                                            Toast.LENGTH_LONG
                                                    ).show()
                                    );
                        }
                );
    }

    // ------------------------------------------------------------
    // HISTORY
    // ------------------------------------------------------------

    private void showRideHistory() {

        FirebaseUser user =
                getCurrentUser();

        if (user == null) {
            return;
        }

        db.collection("rides")
                .whereEqualTo(
                        "passengerId",
                        user.getUid()
                )
                .get()
                .addOnSuccessListener(
                        querySnapshot -> {

                            StringBuilder history =
                                    new StringBuilder();

                            history.append(
                                    "RIDE HISTORY\n\n"
                            );

                            if (querySnapshot.isEmpty()) {

                                history.append(
                                        "No rides found."
                                );

                            } else {

                                for (
                                        DocumentSnapshot document :
                                        querySnapshot.getDocuments()
                                ) {

                                    String pickup =
                                            document.getString(
                                                    "pickup"
                                            );

                                    String destination =
                                            document.getString(
                                                    "destination"
                                            );

                                    String status =
                                            document.getString(
                                                    "status"
                                            );

                                    Double fare =
                                            document.getDouble(
                                                    "fare"
                                            );

                                    history.append(
                                            "From: "
                                    )
                                            .append(
                                                    pickup == null
                                                            ? "-"
                                                            : pickup
                                            )
                                            .append("\n");

                                    history.append(
                                            "To: "
                                    )
                                            .append(
                                                    destination == null
                                                            ? "-"
                                                            : destination
                                            )
                                            .append("\n");

                                    history.append(
                                            "Status: "
                                    )
                                            .append(
                                                    status == null
                                                            ? "-"
                                                            : status
                                            )
                                            .append("\n");

                                    if (fare != null) {

                                        history.append(
                                                        String.format(
                                                                Locale.US,
                                                                "Fare: ₱%.2f\n",
                                                                fare
                                                        )
                                                );
                                    }

                                    history.append(
                                            "--------------------\n"
                                    );
                                }
                            }

                            showHistoryDialog(
                                    history.toString()
                            );
                        }
                )
                .addOnFailureListener(
                        error ->
                                Toast.makeText(
                                        this,
                                        "History failed: " +
                                                error.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show()
                );
    }

    private void showHistoryDialog(
            String text
    ) {

        final android.app.AlertDialog dialog =
                new android.app.AlertDialog.Builder(this)
                        .setTitle(
                                "Sakay Na — Ride History"
                        )
                        .setMessage(text)
                        .setPositiveButton(
                                "CLOSE",
                                null
                        )
                        .create();

        dialog.show();
    }

    // ------------------------------------------------------------
    // LOGOUT
    // ------------------------------------------------------------

    private void logout() {

        if (rideListener != null) {

            rideListener.remove();
            rideListener = null;
        }

        if (locationManager != null) {

            try {

                locationManager.removeUpdates(
                        locationListener
                );

            } catch (Exception ignored) {
            }
        }

        auth.signOut();

        preferences.edit()
                .remove(
                        "passenger_active_ride_id"
                )
                .remove("name")
                .remove("current_name")
                .remove("phone")
                .remove("current_phone")
                .apply();

        Intent intent =
                new Intent(
                        this,
                        MainActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        finish();
    }

    // ------------------------------------------------------------
    // LIFECYCLE
    // ------------------------------------------------------------

    @Override
    protected void onDestroy() {

        if (rideListener != null) {

            rideListener.remove();
            rideListener = null;
        }

        if (locationManager != null) {

            try {

                locationManager.removeUpdates(
                        locationListener
                );

            } catch (Exception ignored) {
            }
        }

        super.onDestroy();
    }
}
```
