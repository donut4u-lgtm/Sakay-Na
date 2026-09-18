package com.sakyna.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Gravity;
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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PassengerActivity extends Activity {

    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private static final int MAP_REQUEST = 2001;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private SharedPreferences preferences;

    private EditText pickupInput;
    private EditText destinationInput;
    private TextView fareText;
    private TextView statusText;

    private Button bookButton;
    private Button cancelButton;
    private Button mapButton;
    private Button chatButton;

    private LocationManager locationManager;
    private LocationListener locationListener;
    private ListenerRegistration rideListener;

    private String activeRideId;

    private double pickupLat = 0;
    private double pickupLng = 0;
    private double destinationLat = 0;
    private double destinationLng = 0;

    private double baseFare = 50;
    private double perKm = 10;
    private double minFare = 50;
    private double maxFare = 500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        preferences = getSharedPreferences(
                "SakayNa",
                MODE_PRIVATE
        );

        buildScreen();
        loadFareSettings();
        requestLocation();
        restoreActiveRide();
    }

    private TextView makeText(String value, float size) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(Color.rgb(45, 55, 50));
        t.setPadding(0, 8, 0, 8);
        return t;
    }

    private Button makeButton(String value) {
        Button b = new Button(this);
        b.setText(value);
        b.setTextSize(16);
        b.setAllCaps(false);
        b.setTypeface(null, Typeface.BOLD);
        b.setTextColor(Color.WHITE);
        b.setGravity(Gravity.CENTER);
        b.setPadding(10, 14, 10, 14);
        b.setBackgroundColor(Color.rgb(0, 125, 75));

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(-1, -2);

        p.setMargins(0, 6, 0, 6);
        b.setLayoutParams(p);

        return b;
    }

    private void buildScreen() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(245, 248, 246));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(Gravity.CENTER);
        header.setPadding(12, 18, 12, 18);
        header.setBackgroundColor(Color.rgb(0, 125, 75));

        TextView title = new TextView(this);
        title.setText("🛺 SAKAY NA");
        title.setTextSize(28);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        header.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Passenger • Your local tricycle ride made simple");
        subtitle.setTextSize(14);
        subtitle.setTextColor(Color.WHITE);
        subtitle.setGravity(Gravity.CENTER);
        header.addView(subtitle);

        root.addView(header,
                new LinearLayout.LayoutParams(-1, -2));

        ScrollView scroll = new ScrollView(this);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(16, 12, 16, 20);

        TextView booking = makeText(
                "🚕 BOOK YOUR RIDE",
                22
        );
        booking.setTypeface(null, Typeface.BOLD);
        booking.setTextColor(Color.rgb(0, 110, 70));
        booking.setGravity(Gravity.CENTER);
        content.addView(booking);

        TextView pickupLabel =
                makeText("📍 PICKUP LOCATION", 15);
        pickupLabel.setTypeface(null, Typeface.BOLD);
        content.addView(pickupLabel);

        pickupInput = new EditText(this);
        pickupInput.setHint("Where should we pick you up?");
        pickupInput.setTextSize(16);
        pickupInput.setPadding(16, 12, 16, 12);
        pickupInput.setBackgroundColor(Color.WHITE);
        content.addView(pickupInput,
                new LinearLayout.LayoutParams(-1, -2));

        Button gps = makeButton(
                "📍 USE MY CURRENT LOCATION"
        );
        gps.setOnClickListener(v -> useCurrentLocation());
        content.addView(gps);

        TextView destinationLabel =
                makeText("🎯 DESTINATION", 15);
        destinationLabel.setTypeface(null, Typeface.BOLD);
        content.addView(destinationLabel);

        destinationInput = new EditText(this);
        destinationInput.setHint("Where are you going?");
        destinationInput.setTextSize(16);
        destinationInput.setPadding(16, 12, 16, 12);
        destinationInput.setBackgroundColor(Color.WHITE);
        content.addView(destinationInput,
                new LinearLayout.LayoutParams(-1, -2));

        Button chooseMap = makeButton(
                "🗺️ CHOOSE DESTINATION ON MAP"
        );
        chooseMap.setOnClickListener(v -> chooseDestination());
        content.addView(chooseMap);

        TextView type = makeText(
                "🛺 RIDE TYPE",
                15
        );
        type.setTypeface(null, Typeface.BOLD);
        content.addView(type);

        TextView tricycle = makeText(
                "🛺 TRICYCLE",
                18
        );
        tricycle.setTypeface(null, Typeface.BOLD);
        tricycle.setTextColor(Color.rgb(0, 110, 70));
        tricycle.setGravity(Gravity.CENTER);
        tricycle.setPadding(12, 15, 12, 15);
        tricycle.setBackgroundColor(Color.WHITE);
        content.addView(tricycle,
                new LinearLayout.LayoutParams(-1, -2));

        TextView paymentLabel = makeText(
                "💳 PAYMENT METHOD",
                15
        );
        paymentLabel.setTypeface(null, Typeface.BOLD);
        content.addView(paymentLabel);

        RadioGroup payment = new RadioGroup(this);
        payment.setOrientation(RadioGroup.HORIZONTAL);
        payment.setGravity(Gravity.CENTER);

        RadioButton cash = new RadioButton(this);
        cash.setText("Cash");
        cash.setChecked(true);
        payment.addView(cash);

        RadioButton gcash = new RadioButton(this);
        gcash.setText("GCash");
        payment.addView(gcash);

        RadioButton maya = new RadioButton(this);
        maya.setText("Maya");
        payment.addView(maya);

        content.addView(payment,
                new LinearLayout.LayoutParams(-1, -2));

        fareText = makeText(
                "💰 Estimated fare: ₱50",
                23
        );
        fareText.setTypeface(null, Typeface.BOLD);
        fareText.setTextColor(Color.rgb(0, 125, 75));
        fareText.setGravity(Gravity.CENTER);
        fareText.setPadding(10, 16, 10, 16);
        fareText.setBackgroundColor(Color.WHITE);
        content.addView(fareText,
                new LinearLayout.LayoutParams(-1, -2));

        statusText = makeText(
                "🟢 Ready to book a ride",
                16
        );
        statusText.setTypeface(null, Typeface.BOLD);
        statusText.setGravity(Gravity.CENTER);
        content.addView(statusText,
                new LinearLayout.LayoutParams(-1, -2));

        bookButton = makeButton(
                "🛺 BOOK A RIDE"
        );
        bookButton.setTextSize(19);
        bookButton.setOnClickListener(
                v -> bookRide(payment)
        );
        content.addView(bookButton);

        mapButton = makeButton(
                "🗺️ LIVE RIDE MAP"
        );
        mapButton.setOnClickListener(
                v -> openLiveMap()
        );
        content.addView(mapButton);

        chatButton = makeButton(
                "💬 CHAT WITH DRIVER"
        );
        chatButton.setOnClickListener(
                v -> openChat()
        );
        content.addView(chatButton);

        cancelButton = makeButton(
                "❌ CANCEL RIDE"
        );
        cancelButton.setBackgroundColor(
                Color.rgb(190, 55, 55)
        );
        cancelButton.setOnClickListener(
                v -> cancelRide()
        );
        content.addView(cancelButton);

        Button history = makeButton(
                "📜 RIDE HISTORY"
        );
        history.setBackgroundColor(
                Color.rgb(65, 90, 100)
        );
        history.setOnClickListener(
                v -> showHistory()
        );
        content.addView(history);

        Button logout = makeButton(
                "🚪 LOGOUT"
        );
        logout.setBackgroundColor(
                Color.rgb(85, 85, 85)
        );
        logout.setOnClickListener(
                v -> logout()
        );
        content.addView(logout);

        scroll.addView(content);

        root.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                )
        );

        setContentView(root);
        updateButtons();
    }

    private void requestLocation() {

        if (
                checkSelfPermission(
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
                &&
                checkSelfPermission(
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
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

        locationManager =
                (LocationManager)
                        getSystemService(LOCATION_SERVICE);

        if (locationManager == null) return;

        locationListener =
                new LocationListener() {

                    @Override
                    public void onLocationChanged(
                            @NonNull Location location
                    ) {
                        pickupLat =
                                location.getLatitude();

                        pickupLng =
                                location.getLongitude();

                        if (
                                pickupInput != null
                                &&
                                pickupInput
                                        .getText()
                                        .toString()
                                        .trim()
                                        .isEmpty()
                        ) {
                            useCurrentLocation();
                        }

                        calculateFare();
                    }
                };

        try {

            if (
                    checkSelfPermission(
                            Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    ||
                    checkSelfPermission(
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
            ) {

                if (
                        locationManager.isProviderEnabled(
                                LocationManager.GPS_PROVIDER
                        )
                ) {
                    locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            3000,
                            10,
                            locationListener
                    );
                }

                if (
                        locationManager.isProviderEnabled(
                                LocationManager.NETWORK_PROVIDER
                        )
                ) {
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            3000,
                            10,
                            locationListener
                    );
                }
            }

        } catch (SecurityException ignored) {
        }
    }

    private void useCurrentLocation() {

        if (pickupLat == 0 && pickupLng == 0) {

            Toast.makeText(
                    this,
                    "Waiting for GPS location...",
                    Toast.LENGTH_SHORT
            ).show();

            requestLocation();
            return;
        }

        pickupInput.setText(
                String.format(
                        Locale.US,
                        "%.6f, %.6f",
                        pickupLat,
                        pickupLng
                )
        );

        calculateFare();
    }

    private void chooseDestination() {

        Intent i =
                new Intent(
                        this,
                        MapActivity.class
                );

        i.putExtra(
                "mode",
                "SELECT_DESTINATION"
        );

        i.putExtra(
                "pickup_latitude",
                pickupLat
        );

        i.putExtra(
                "pickup_longitude",
                pickupLng
        );

        i.putExtra(
                "pickup_address",
                pickupInput
                        .getText()
                        .toString()
                        .trim()
        );

        startActivityForResult(
                i,
                MAP_REQUEST
        );
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {
        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (
                requestCode == MAP_REQUEST
                &&
                resultCode == RESULT_OK
                &&
                data != null
        ) {

            destinationLat =
                    data.getDoubleExtra(
                            "destination_latitude",
                            0
                    );

            destinationLng =
                    data.getDoubleExtra(
                            "destination_longitude",
                            0
                    );

            String address =
                    data.getStringExtra(
                            "destination_address"
                    );

            if (
                    address != null
                    &&
                    !address.trim().isEmpty()
            ) {
                destinationInput.setText(address);
            }

            calculateFare();
        }
    }

    private void bookRide(
            RadioGroup payment
    ) {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            Toast.makeText(
                    this,
                    "Please log in again.",
                    Toast.LENGTH_SHORT
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
            Toast.makeText(
                    this,
                    "Enter pickup location.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (destination.isEmpty()) {
            Toast.makeText(
                    this,
                    "Choose your destination on the map.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (pickupLat == 0 && pickupLng == 0) {
            Toast.makeText(
                    this,
                    "Waiting for pickup GPS location.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        /*
         * IMPORTANT:
         * Do not geocode here.
         * The old code performed synchronous Geocoder work
         * on the Android UI thread.
         *
         * Destination must come from MapActivity.
         */
        if (
                destinationLat == 0
                &&
                destinationLng == 0
        ) {
            Toast.makeText(
                    this,
                    "Please choose the destination on the map first.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        RadioButton selected =
                findViewById(
                        payment.getCheckedRadioButtonId()
                );

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
                "pickupLatitude",
                pickupLat
        );

        ride.put(
                "pickupLongitude",
                pickupLng
        );

        ride.put(
                "destinationLatitude",
                destinationLat
        );

        ride.put(
                "destinationLongitude",
                destinationLng
        );

        ride.put(
                "paymentMethod",
                selected == null
                        ? "Cash"
                        : selected.getText().toString()
        );

        ride.put(
                "paymentStatus",
                "PENDING"
        );

        ride.put(
                "fare",
                getCurrentFare()
        );

        ride.put(
                "status",
                "REQUESTED"
        );

        ride.put(
                "createdAt",
                System.currentTimeMillis()
        );

        bookButton.setEnabled(false);

        statusText.setText(
                "🔎 Sending ride request..."
        );

        db.collection("rides")
                .add(ride)
                .addOnSuccessListener(
                        document -> {

                            activeRideId =
                                    document.getId();

                            preferences
                                    .edit()
                                    .putString(
                                            "activeRideId",
                                            activeRideId
                                    )
                                    .apply();

                            statusText.setText(
                                    "🚦 Ride status: REQUESTED\n"
                                            + "Waiting for a driver"
                            );

                            listenToRide(
                                    activeRideId
                            );

                            updateButtons();

                            Toast.makeText(
                                    this,
                                    "🛺 Ride request sent. Waiting for a driver.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                )
                .addOnFailureListener(
                        error -> {

                            clearActiveRide();

                            statusText.setText(
                                    "🔴 Booking failed"
                            );

                            bookButton.setEnabled(true);

                            Toast.makeText(
                                    this,
                                    "Booking failed:\n"
                                            + error.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();

                            updateButtons();
                        }
                );
    }

    private void restoreActiveRide() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) return;

        String savedId =
                preferences.getString(
                        "activeRideId",
                        null
                );

        if (
                savedId != null
                &&
                !savedId.trim().isEmpty()
        ) {

            db.collection("rides")
                    .document(savedId)
                    .get()
                    .addOnSuccessListener(
                            snapshot -> {

                                String passengerId =
                                        snapshot.getString(
                                                "passengerId"
                                        );

                                String status =
                                        snapshot.getString(
                                                "status"
                                        );

                                if (
                                        snapshot.exists()
                                        &&
                                        user.getUid().equals(
                                                passengerId
                                        )
                                        &&
                                        activeStatus(status)
                                ) {

                                    activeRideId =
                                            savedId;

                                    listenToRide(
                                            savedId
                                    );

                                } else {

                                    clearActiveRide();

                                    statusText.setText(
                                            "🟢 Ready to book a ride"
                                    );

                                    updateButtons();
                                }
                            }
                    )
                    .addOnFailureListener(
                            error -> {

                                clearActiveRide();

                                statusText.setText(
                                        "🟢 Ready to book a ride"
                                );

                                updateButtons();
                            }
                    );

            return;
        }

        ArrayList<String> statuses =
                new ArrayList<>();

        statuses.add("REQUESTED");
        statuses.add("ACCEPTED");
        statuses.add("DRIVER_ON_THE_WAY");
        statuses.add("DRIVER_ARRIVED");
        statuses.add("IN_PROGRESS");

        db.collection("rides")
                .whereEqualTo(
                        "passengerId",
                        user.getUid()
                )
                .whereIn(
                        "status",
                        statuses
                )
                .limit(1)
                .get()
                .addOnSuccessListener(
                        query -> {

                            if (
                                    !query.isEmpty()
                            ) {

                                DocumentSnapshot ride =
                                        query.getDocuments()
                                                .get(0);

                                activeRideId =
                                        ride.getId();

                                preferences
                                        .edit()
                                        .putString(
                                                "activeRideId",
                                                activeRideId
                                        )
                                        .apply();

                                listenToRide(
                                        activeRideId
                                );

                            } else {

                                clearActiveRide();

                                statusText.setText(
                                        "🟢 Ready to book a ride"
                                );

                                updateButtons();
                            }
                        }
                )
                .addOnFailureListener(
                        error -> {

                            clearActiveRide();

                            statusText.setText(
                                    "🟢 Ready to book a ride"
                            );

                            updateButtons();
                        }
                );
    }

    private boolean activeStatus(
            String status
    ) {

        return
                "REQUESTED".equals(status)
                        ||
                "ACCEPTED".equals(status)
                        ||
                "DRIVER_ON_THE_WAY".equals(status)
                        ||
                "DRIVER_ARRIVED".equals(status)
                        ||
                "IN_PROGRESS".equals(status);
    }

    private void listenToRide(
            String rideId
    ) {

        if (rideListener != null) {
            rideListener.remove();
            rideListener = null;
        }

        if (
                rideId == null
                ||
