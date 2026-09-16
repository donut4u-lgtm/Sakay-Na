package com.sakyna.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private Button historyButton;
    private Button logoutButton;

    private LocationManager locationManager;
    private LocationListener locationListener;

    private ListenerRegistration rideListener;

    private String activeRideId;

    private double pickupLat;
    private double pickupLng;

    private double destinationLat;
    private double destinationLng;

    private double baseFare = 50;
    private double perKm = 10;
    private double minFare = 50;
    private double maxFare = 500;

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
        loadFareSettings();
        requestLocation();
        restoreActiveRide();
    }

    private TextView text(
            String value,
            float size
    ) {

        TextView t = new TextView(this);

        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(Color.rgb(55, 65, 60));
        t.setPadding(0, 6, 0, 6);

        return t;
    }

    private Button actionButton(
            String value
    ) {

        Button b = new Button(this);

        b.setText(value);
        b.setTextSize(15);
        b.setTypeface(
                null,
                Typeface.BOLD
        );

        b.setAllCaps(false);

        b.setTextColor(Color.WHITE);

        b.setGravity(Gravity.CENTER);

        b.setPadding(
                10,
                12,
                10,
                12
        );

        b.setBackgroundColor(
                Color.rgb(0, 125, 75)
        );

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                );

        p.setMargins(
                0,
                5,
                0,
                5
        );

        b.setLayoutParams(p);

        return b;
    }

    private LinearLayout.LayoutParams fullParams() {

        return new LinearLayout.LayoutParams(
                -1,
                -2
        );
    }

    private void buildScreen() {

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setBackgroundColor(
                Color.rgb(245, 248, 246)
        );

        /*
         * HEADER
         */

        LinearLayout header =
                new LinearLayout(this);

        header.setOrientation(
                LinearLayout.VERTICAL
        );

        header.setGravity(
                Gravity.CENTER
        );

        header.setPadding(
                12,
                18,
                12,
                18
        );

        header.setBackgroundColor(
                Color.rgb(0, 125, 75)
        );

        TextView logo =
                new TextView(this);

        logo.setText(
                "🛺  SAKAY NA"
        );

        logo.setTextSize(28);
        logo.setTypeface(
                null,
                Typeface.BOLD
        );

        logo.setTextColor(
                Color.WHITE
        );

        logo.setGravity(
                Gravity.CENTER
        );

        header.addView(logo);

        TextView subtitle =
                new TextView(this);

        subtitle.setText(
                "Passenger • Your local tricycle ride made simple"
        );

        subtitle.setTextSize(14);

        subtitle.setTextColor(
                Color.WHITE
        );

        subtitle.setGravity(
                Gravity.CENTER
        );

        header.addView(subtitle);

        root.addView(
                header,
                fullParams()
        );

        /*
         * SCROLLABLE CONTENT
         */

        ScrollView scroll =
                new ScrollView(this);

        scroll.setFillViewport(true);

        LinearLayout content =
                new LinearLayout(this);

        content.setOrientation(
                LinearLayout.VERTICAL
        );

        content.setPadding(
                16,
                12,
                16,
                18
        );

        /*
         * BOOKING CARD TITLE
         */

        TextView bookingTitle =
                text(
                        "🚕 BOOK YOUR RIDE",
                        21
                );

        bookingTitle.setTypeface(
                null,
                Typeface.BOLD
        );

        bookingTitle.setTextColor(
                Color.rgb(0, 110, 70)
        );

        bookingTitle.setGravity(
                Gravity.CENTER
        );

        content.addView(
                bookingTitle,
                fullParams()
        );

        /*
         * PICKUP
         */

        TextView pickupLabel =
                text(
                        "📍 PICKUP LOCATION",
                        15
                );

        pickupLabel.setTypeface(
                null,
                Typeface.BOLD
        );

        content.addView(
                pickupLabel
        );

        pickupInput =
                new EditText(this);

        pickupInput.setHint(
                "Where should we pick you up?"
        );

        pickupInput.setTextSize(16);

        pickupInput.setSingleLine(false);

        pickupInput.setPadding(
                16,
                12,
                16,
                12
        );

        pickupInput.setBackgroundColor(
                Color.WHITE
        );

        content.addView(
                pickupInput,
                fullParams()
        );

        Button gps =
                actionButton(
                        "📍  USE MY CURRENT LOCATION"
                );

        gps.setOnClickListener(
                v -> useCurrentLocation()
        );

        content.addView(gps);

        /*
         * DESTINATION
         */

        TextView destinationLabel =
                text(
                        "🎯 DESTINATION",
                        15
                );

        destinationLabel.setTypeface(
                null,
                Typeface.BOLD
        );

        content.addView(
                destinationLabel
        );

        destinationInput =
                new EditText(this);

        destinationInput.setHint(
                "Where are you going?"
        );

        destinationInput.setTextSize(16);

        destinationInput.setPadding(
                16,
                12,
                16,
                12
        );

        destinationInput.setBackgroundColor(
                Color.WHITE
        );

        content.addView(
                destinationInput,
                fullParams()
        );

        Button choose =
                actionButton(
                        "🗺️  CHOOSE DESTINATION ON MAP"
                );

        choose.setOnClickListener(
                v -> chooseDestination()
        );

        content.addView(choose);

        /*
         * RIDE TYPE
         */

        TextView rideType =
                text(
                        "🛺 RIDE TYPE",
                        15
                );

        rideType.setTypeface(
                null,
                Typeface.BOLD
        );

        content.addView(rideType);

        TextView tricycle =
                text(
                        "🛺  TRICYCLE",
                        18
                );

        tricycle.setTypeface(
                null,
                Typeface.BOLD
        );

        tricycle.setTextColor(
                Color.rgb(0, 110, 70)
        );

        tricycle.setGravity(
                Gravity.CENTER
        );

        tricycle.setPadding(
                12,
                14,
                12,
                14
        );

        tricycle.setBackgroundColor(
                Color.WHITE
        );

        content.addView(
                tricycle,
                fullParams()
        );

        /*
         * PAYMENT
         */

        TextView paymentLabel =
                text(
                        "💳 PAYMENT METHOD",
                        15
                );

        paymentLabel.setTypeface(
                null,
                Typeface.BOLD
        );

        content.addView(paymentLabel);

        RadioGroup payment =
                new RadioGroup(this);

        payment.setOrientation(
                RadioGroup.HORIZONTAL
        );

        payment.setGravity(
                Gravity.CENTER
        );

        RadioButton cash =
                new RadioButton(this);

        cash.setText("Cash");
        cash.setTextSize(15);
        cash.setChecked(true);

        payment.addView(cash);

        RadioButton gcash =
                new RadioButton(this);

        gcash.setText("GCash");
        gcash.setTextSize(15);

        payment.addView(gcash);

        RadioButton maya =
                new RadioButton(this);

        maya.setText("Maya");
        maya.setTextSize(15);

        payment.addView(maya);

        content.addView(
                payment,
                fullParams()
        );

        /*
         * FARE
         */

        fareText =
                text(
                        "💰 Estimated fare: ₱50",
                        23
                );

        fareText.setTypeface(
                null,
                Typeface.BOLD
        );

        fareText.setGravity(
                Gravity.CENTER
        );

        fareText.setTextColor(
                Color.rgb(0, 125, 75)
        );

        fareText.setPadding(
                10,
                15,
                10,
                15
        );

        fareText.setBackgroundColor(
                Color.WHITE
        );

        content.addView(
                fareText,
                fullParams()
        );

        /*
         * STATUS
         */

        statusText =
                text(
                        "🟢 No active ride",
                        16
                );

        statusText.setGravity(
                Gravity.CENTER
        );

        statusText.setTypeface(
                null,
                Typeface.BOLD
        );

        statusText.setPadding(
                12,
                12,
                12,
                12
        );

        content.addView(
                statusText,
                fullParams()
        );

        /*
         * MAIN BOOK BUTTON
         */

        bookButton =
                actionButton(
                        "🛺  BOOK A RIDE"
                );

        bookButton.setTextSize(19);

        bookButton.setPadding(
                10,
                17,
                10,
                17
        );

        bookButton.setBackgroundColor(
                Color.rgb(0, 150, 80)
        );

        bookButton.setOnClickListener(
                v -> bookRide(payment)
        );

        content.addView(
                bookButton
        );

        /*
         * ACTIVE RIDE CONTROLS
         */

        mapButton =
                actionButton(
                        "🗺️  LIVE RIDE MAP"
                );

        mapButton.setOnClickListener(
                v -> openLiveMap()
        );

        content.addView(
                mapButton
        );

        chatButton =
                actionButton(
                        "💬  CHAT WITH DRIVER"
                );

        chatButton.setOnClickListener(
                v -> openChat()
        );

        content.addView(
                chatButton
        );

        cancelButton =
                actionButton(
                        "❌  CANCEL RIDE"
                );

        cancelButton.setBackgroundColor(
                Color.rgb(190, 55, 55)
        );

        cancelButton.setOnClickListener(
                v -> cancelRide()
        );

        content.addView(
                cancelButton
        );

        /*
         * HISTORY
         */

        historyButton =
                actionButton(
                        "📜  RIDE HISTORY"
                );

        historyButton.setBackgroundColor(
                Color.rgb(65, 90, 100)
        );

        historyButton.setOnClickListener(
                v -> showHistory()
        );

        content.addView(
                historyButton
        );

        /*
         * LOGOUT
         */

        logoutButton =
                actionButton(
                        "🚪  LOGOUT"
                );

        logoutButton.setBackgroundColor(
                Color.rgb(85, 85, 85)
        );

        logoutButton.setOnClickListener(
                v -> logout()
        );

        content.addView(
                logoutButton
        );

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
                        getSystemService(
                                LOCATION_SERVICE
                        );

        if (locationManager == null) {
            return;
        }

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

        if (
                pickupLat == 0 &&
                pickupLng == 0
        ) {

            Toast.makeText(
                    this,
                    "Waiting for GPS location...",
                    Toast.LENGTH_SHORT
            ).show();

            requestLocation();
            return;
        }

        String address =
                getAddress(
                        pickupLat,
                        pickupLng
                );

        if (address.isEmpty()) {

            address =
                    String.format(
                            Locale.US,
                            "%.6f, %.6f",
                            pickupLat,
                            pickupLng
                    );
        }

        pickupInput.setText(address);

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

                destinationInput.setText(
                        address
                );
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

        if (
                pickupLat == 0 &&
                pickupLng == 0
        ) {

            Toast.makeText(
                    this,
                    "Waiting for pickup GPS location.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (
                destinationLat == 0 &&
                destinationLng == 0
        ) {

            double[] point =
                    geocode(destination);

            if (point != null) {

                destinationLat =
                        point[0];

                destinationLng =
                        point[1];

            } else {

                Toast.makeText(
                        this,
                        "Please choose the destination on the map.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }
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
                "🔎 Finding available driver..."
        );

        db.collection("rides")
                .add(ride)
                .addOnSuccessListener(
                        d -> {

                            activeRideId =
                                    d.getId();

                            preferences
                                    .edit()
                                    .putString(
                                            "activeRideId",
                                            activeRideId
                                    )
                                    .apply();

                            statusText.setText(
                                    "🚦 Ride status: REQUESTED\n" +
                                    "Waiting for a driver"
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
                        e -> {

                            updateButtons();

                            Toast.makeText(
                                    this,
                                    "Booking failed: " +
                                    e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                );
    }

    private void loadFareSettings() {

        db.collection("settings")
                .document("fare")
                .get()
                .addOnSuccessListener(
                        snapshot -> {

                            if (snapshot.exists()) {

                                baseFare =
                                        number(
                                                snapshot,
                                                "baseFare",
                                                50
                                        );

                                perKm =
                                        number(
                                                snapshot,
                                                "perKm",
                                                10
                                        );

                                minFare =
                                        number(
                                                snapshot,
                                                "minimum",
                                                50
                                        );

                                maxFare =
                                        number(
                                                snapshot,
                                                "maximum",
                                                500
                                        );
                            }

                            calculateFare();
                        }
                )
                .addOnFailureListener(
                        e -> calculateFare()
                );
    }

    private void calculateFare() {

        if (fareText == null) {
            return;
        }

        double fare =
                getCurrentFare();

        fareText.setText(
                String.format(
                        Locale.US,
                        "💰 Estimated fare: ₱%.0f",
                        fare
                )
        );
    }

    private double getCurrentFare() {

        if (
                pickupLat == 0
                ||
                pickupLng == 0
                ||
                destinationLat == 0
                ||
                destinationLng == 0
        ) {

            return minFare;
        }

        float[] distance =
                new float[1];

        Location.distanceBetween(
                pickupLat,
                pickupLng,
                destinationLat,
                destinationLng,
                distance
        );

        double fare =
                baseFare
                +
                (distance[0] / 1000.0)
                * perKm;

        return Math.max(
                minFare,
                Math.min(
                        maxFare,
                        fare
                )
        );
    }

    private double[] geocode(
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

            if (
                    addresses != null
                    &&
                    !addresses.isEmpty()
            ) {

                return new double[]{
                        addresses
                                .get(0)
                                .getLatitude(),

                        addresses
                                .get(0)
                                .getLongitude()
                };
            }

        } catch (Exception ignored) {
        }

        return null;
    }

    private String getAddress(
            double lat,
            double lng
    ) {

        try {

            Geocoder geocoder =
                    new Geocoder(
                            this,
                            Locale.getDefault()
                    );

            List<Address> addresses =
                    geocoder.getFromLocation(
                            lat,
                            lng,
                            1
                    );

            if (
                    addresses != null
                    &&
                    !addresses.isEmpty()
                    &&
                    addresses
                            .get(0)
                            .getAddressLine(0)
                            != null
            ) {

                return addresses
                        .get(0)
                        .getAddressLine(0);
            }

        } catch (Exception ignored) {
        }

        return "";
    }

    private void restoreActiveRide() {

        String savedId =
                preferences.getString(
                        "activeRideId",
                        null
                );

        if (
                savedId != null
                &&
                !savedId.isEmpty()
        ) {

            activeRideId =
                    savedId;

            listenToRide(
                    savedId
            );

            return;
        }

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
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

                            if (!query.isEmpty()) {

                                activeRideId =
                                        query
                                                .getDocuments()
                                                .get(0)
                                                .getId();

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
                            }
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
            String id
    ) {

        if (rideListener != null) {

            rideListener.remove();
        }

        rideListener =
                db.collection("rides")
                        .document(id)
                        .addSnapshotListener(
                                (snapshot, error) -> {

                                    if (
                                            error != null
                                            ||
                                            snapshot == null
                                            ||
                                            !snapshot.exists()
                                    ) {

                                        return;
                                    }

                                    String status =
                                            snapshot.getString(
                                                    "status"
                                            );

                                    if (
                                            status == null
                                            ||
                                            status.isEmpty()
                                    ) {

                                        status =
                                                "REQUESTED";
                                    }

                                    statusText.setText(
                                            "🚦 Ride status: " +
                                            status
                                    );

                                    if (
                                            !activeStatus(
                                                    status
                                            )
                                    ) {

                                        activeRideId =
                                                null;

                                        preferences
                                                .edit()
                                                .remove(
                                                        "activeRideId"
                                                )
                                                .apply();
                                    }

                                    updateButtons();
                                }
                        );
    }

    private void updateButtons() {

        boolean active =
                activeRideId != null
                &&
                !activeRideId.isEmpty();

        if (bookButton != null) {

            bookButton.setEnabled(
                    !active
            );
        }

        if (cancelButton != null) {

            cancelButton.setEnabled(
                    active
            );
        }

        if (mapButton != null) {

            mapButton.setEnabled(
                    active
            );
        }

        if (chatButton != null) {

            chatButton.setEnabled(
                    active
            );
        }
    }

    private void openLiveMap() {

        if (
                activeRideId == null
                ||
                activeRideId.isEmpty()
        ) {

            Toast.makeText(
                    this,
                    "No active ride.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        db.collection("rides")
                .document(activeRideId)
                .get()
                .addOnSuccessListener(
                        snapshot -> {

                            if (!snapshot.exists()) {
                                return;
                            }

                            String status =
                                    snapshot.getString(
                                            "status"
                                    );

                            if (
                                    !activeStatus(
                                            status
                                    )
                            ) {

                                Toast.makeText(
                                        this,
                                        "Live map is available only for an active ride.",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            Intent i =
                                    new Intent(
                                            this,
                                            MapActivity.class
                                    );

                            i.putExtra(
                                    "mode",
                                    "LIVE_RIDE"
                            );

                            i.putExtra(
                                    "ride_id",
                                    activeRideId
                            );

                            i.putExtra(
                                    "pickup_latitude",
                                    number(
                                            snapshot,
                                            "pickupLatitude",
                                            pickupLat
                                    )
                            );

                            i.putExtra(
                                    "pickup_longitude",
                                    number(
                                            snapshot,
                                            "pickupLongitude",
                                            pickupLng
                                    )
                            );

                            i.putExtra(
                                    "destination_latitude",
                                    number(
                                            snapshot,
                                            "destinationLatitude",
                                            destinationLat
                                    )
                            );

                            i.putExtra(
                                    "destination_longitude",
                                    number(
                                            snapshot,
                                            "destinationLongitude",
                                            destinationLng
                                    )
                            );

                            i.putExtra(
                                    "pickup_address",
                                    snapshot.getString(
                                            "pickup"
                                    )
                            );

                            i.putExtra(
                                    "destination_address",
                                    snapshot.getString(
                                            "destination"
                                    )
                            );

                            startActivity(i);
                        }
                );
    }

    private void openChat() {

        if (
                activeRideId == null
                ||
                activeRideId.isEmpty()
        ) {

            return;
        }

        Intent i =
                new Intent(
                        this,
                        RideChatActivity.class
                );

        i.putExtra(
                "ride_id",
                activeRideId
        );

        startActivity(i);
    }

    private void cancelRide() {

        if (
                activeRideId == null
                ||
                activeRideId.isEmpty()
        ) {

            return;
        }

        db.collection("rides")
                .document(activeRideId)
                .update(
                        "status",
                        "CANCELLED",
                        "cancelledBy",
                        "PASSENGER",
                        "cancelledAt",
                        System.currentTimeMillis()
                )
                .addOnSuccessListener(
                        v -> {

                            activeRideId =
                                    null;

                            preferences
                                    .edit()
                                    .remove(
                                            "activeRideId"
                                    )
                                    .apply();

                            if (
                                    rideListener != null
                            ) {

                                rideListener.remove();

                                rideListener =
                                        null;
                            }

                            statusText.setText(
                                    "❌ Ride cancelled"
                            );

                            updateButtons();
                        }
                );
    }

    private void showHistory() {

        FirebaseUser user =
                auth.getCurrentUser();

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
                        query -> {

                            if (query.isEmpty()) {

                                Toast.makeText(
                                        this,
                                        "No ride history yet.",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            StringBuilder history =
                                    new StringBuilder();

                            for (
                                    DocumentSnapshot s :
                                    query.getDocuments()
                            ) {

                                history
                                        .append("📍 From: ")
                                        .append(
                                                s.getString(
                                                        "pickup"
                                                )
                                        )
                                        .append("\n")

                                        .append("🎯 To: ")
                                        .append(
                                                s.getString(
                                                        "destination"
                                                )
                                        )
                                        .append("\n")

                                        .append("🚦 Status: ")
                                        .append(
                                                s.getString(
                                                        "status"
                                                )
                                        )
                                        .append("\n")

                                        .append("💰 Fare: ₱")
                                        .append(
                                                number(
                                                        s,
                                                        "fare",
                                                        0
                                                )
                                        )
                                        .append("\n\n");
                            }

                            TextView historyText =
                                    new TextView(
                                            this
                                    );

                            historyText.setText(
                                    history.toString()
                            );

                            historyText.setTextSize(
                                    16
                            );

                            historyText.setPadding(
                                    25,
                                    15,
                                    25,
                                    15
                            );

                            new android.app.AlertDialog.Builder(
                                    this
                            )
                                    .setTitle(
                                            "📜 Ride History"
                                    )
                                    .setView(
                                            historyText
                                    )
                                    .setPositiveButton(
                                            "CLOSE",
                                            null
                                    )
                                    .show();
                        }
                );
    }

    private double number(
            DocumentSnapshot snapshot,
            String field,
            double defaultValue
    ) {

        Double value =
                snapshot.getDouble(
                        field
                );

        return value == null
                ? defaultValue
                : value;
    }

    private void logout() {

        if (rideListener != null) {

            rideListener.remove();

            rideListener =
                    null;
        }

        auth.signOut();

        preferences
                .edit()
                .remove("activeRideId")
                .remove("name")
                .remove("current_name")
                .remove("phone")
                .remove("current_phone")
                .apply();

        Intent i =
                new Intent(
                        this,
                        MainActivity.class
                );

        i.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        |
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(i);

        finish();
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

        if (
                requestCode ==
                        LOCATION_PERMISSION_REQUEST
                &&
                grantResults.length > 0
                &&
                grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED
        ) {

            startLocationUpdates();
        }
    }

    @Override
    protected void onDestroy() {

        if (rideListener != null) {

            rideListener.remove();

            rideListener =
                    null;
        }

        if (
                locationManager != null
                &&
                locationListener != null
        ) {

            try {

                locationManager.removeUpdates(
                        locationListener
                );

            } catch (SecurityException ignored) {
            }
        }

        super.onDestroy();
    }
}
