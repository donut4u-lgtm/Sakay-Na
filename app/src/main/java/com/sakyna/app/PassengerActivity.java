
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

    private double pickupLat = 0;
    private double pickupLng = 0;
    private double destinationLat = 0;
    private double destinationLng = 0;

    // SAKAY NA FARE
    private double baseFare = 25;
    private double perKm = 10;
    private double minFare = 25;
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

    private TextView text(String value, float size) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(Color.rgb(55, 65, 60));
        t.setPadding(0, 6, 0, 6);
        return t;
    }

    private Button actionButton(String value) {
        Button b = new Button(this);

        b.setText(value);
        b.setTextSize(15);
        b.setTypeface(null, Typeface.BOLD);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setGravity(Gravity.CENTER);

        b.setPadding(10, 12, 10, 12);

        b.setBackgroundColor(
                Color.rgb(0, 125, 75)
        );

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                );

        p.setMargins(0, 5, 0, 5);

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

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(
                Color.rgb(245, 248, 246)
        );

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(Gravity.CENTER);
        header.setPadding(12, 18, 12, 18);
        header.setBackgroundColor(
                Color.rgb(0, 125, 75)
        );

        TextView logo = new TextView(this);
        logo.setText("🛺  SAKAY NA");
        logo.setTextSize(28);
        logo.setTypeface(null, Typeface.BOLD);
        logo.setTextColor(Color.WHITE);
        logo.setGravity(Gravity.CENTER);
        header.addView(logo);

        TextView subtitle = new TextView(this);
        subtitle.setText(
                "Passenger • Your local tricycle ride made simple"
        );
        subtitle.setTextSize(14);
        subtitle.setTextColor(Color.WHITE);
        subtitle.setGravity(Gravity.CENTER);
        header.addView(subtitle);

        root.addView(header, fullParams());

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(16, 12, 16, 18);

        TextView bookingTitle = text(
                "🚕 BOOK YOUR RIDE",
                21
        );

        bookingTitle.setTypeface(null, Typeface.BOLD);
        bookingTitle.setTextColor(
                Color.rgb(0, 110, 70)
        );
        bookingTitle.setGravity(Gravity.CENTER);

        content.addView(
                bookingTitle,
                fullParams()
        );

        TextView pickupLabel = text(
                "📍 PICKUP LOCATION",
                15
        );

        pickupLabel.setTypeface(null, Typeface.BOLD);
        content.addView(pickupLabel);

        pickupInput = new EditText(this);
        pickupInput.setHint(
                "Where should we pick you up?"
        );
        pickupInput.setTextSize(16);
        pickupInput.setSingleLine(false);
        pickupInput.setPadding(16, 12, 16, 12);
        pickupInput.setBackgroundColor(Color.WHITE);

        content.addView(
                pickupInput,
                fullParams()
        );

        Button gps = actionButton(
                "📍  USE MY CURRENT LOCATION"
        );

        gps.setOnClickListener(
                v -> useCurrentLocation()
        );

        content.addView(gps);

        TextView destinationLabel = text(
                "🎯 DESTINATION",
                15
        );

        destinationLabel.setTypeface(
                null,
                Typeface.BOLD
        );

        content.addView(destinationLabel);

        destinationInput = new EditText(this);
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

        Button choose = actionButton(
                "🗺️  CHOOSE DESTINATION ON MAP"
        );

        choose.setOnClickListener(
                v -> chooseDestination()
        );

        content.addView(choose);

        TextView rideType = text(
                "🛺 RIDE TYPE",
                15
        );

        rideType.setTypeface(
                null,
                Typeface.BOLD
        );

        content.addView(rideType);

        TextView tricycle = text(
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

        tricycle.setGravity(Gravity.CENTER);
        tricycle.setPadding(12, 14, 12, 14);
        tricycle.setBackgroundColor(Color.WHITE);

        content.addView(
                tricycle,
                fullParams()
        );

        TextView paymentLabel = text(
                "💳 PAYMENT METHOD",
                15
        );

        paymentLabel.setTypeface(
                null,
                Typeface.BOLD
        );

        content.addView(paymentLabel);

        RadioGroup payment = new RadioGroup(this);

        payment.setOrientation(
                RadioGroup.HORIZONTAL
        );

        payment.setGravity(Gravity.CENTER);

        RadioButton cash = new RadioButton(this);
        cash.setText("Cash");
        cash.setTextSize(15);
        cash.setChecked(true);
        payment.addView(cash);

        RadioButton gcash = new RadioButton(this);
        gcash.setText("GCash");
        gcash.setTextSize(15);
        payment.addView(gcash);

        RadioButton maya = new RadioButton(this);
        maya.setText("Maya");
        maya.setTextSize(15);
        payment.addView(maya);

        content.addView(
                payment,
                fullParams()
        );

        fareText = text(
                "💰 Estimated fare: ₱25",
                23
        );

        fareText.setTypeface(
                null,
                Typeface.BOLD
        );

        fareText.setGravity(Gravity.CENTER);
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

        statusText = text(
                "🟢 No active ride",
                16
        );

        statusText.setGravity(Gravity.CENTER);
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

        bookButton = actionButton(
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

        content.addView(bookButton);

        mapButton = actionButton(
                "🗺️  LIVE RIDE MAP"
        );

        mapButton.setOnClickListener(
                v -> openLiveMap()
        );

        content.addView(mapButton);

        chatButton = actionButton(
                "💬  CHAT WITH DRIVER"
        );

        chatButton.setOnClickListener(
                v -> openChat()
        );

        content.addView(chatButton);

        cancelButton = actionButton(
                "❌  CANCEL RIDE"
        );

        cancelButton.setBackgroundColor(
                Color.rgb(190, 55, 55)
        );

        cancelButton.setOnClickListener(
                v -> cancelRide()
        );

        content.addView(cancelButton);

        historyButton = actionButton(
                "📜  RIDE HISTORY"
        );

        historyButton.setBackgroundColor(
                Color.rgb(65, 90, 100)
        );

        historyButton.setOnClickListener(
                v -> showHistory()
        );

        content.addView(historyButton);

        logoutButton = actionButton(
                "🚪  LOGOUT"
        );

        logoutButton.setBackgroundColor(
                Color.rgb(85, 85, 85)
        );

        logoutButton.setOnClickListener(
                v -> logout()
        );

        content.addView(logoutButton);

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

                            String address =
                                    getAddress(
                                            pickupLat,
                                            pickupLng
                                    );

                            if (
                                    address != null
                                    &&
                                    !address.trim().isEmpty()
                            ) {

                                pickupInput.setText(
                                        address
                                );
                            }
                        }

                        calculateFare();
                    }
                };

        try {

            boolean fine =
                    checkSelfPermission(
                            Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED;

            boolean coarse =
                    checkSelfPermission(
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED;

            if (!fine && !coarse) {
                return;
            }

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

        } catch (SecurityException ignored) {
        }
    }

    private void useCurrentLocation() {

        if (
                pickupLat == 0
                &&
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

        if (
                address == null
                ||
                address.trim().isEmpty()
        ) {

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
                requestCode != MAP_REQUEST
                ||
                resultCode != RESULT_OK
                ||
                data == null
        ) {
            return;
        }

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
                address == null
                ||
                address.trim().isEmpty()
        ) {

            address =
                    getAddress(
                            destinationLat,
                            destinationLng
                    );
        }

        if (
                address == null
                ||
                address.trim().isEmpty()
        ) {

            address =
                    String.format(
                            Locale.US,
                            "%.6f, %.6f",
                            destinationLat,
                            destinationLng
                    );
        }

        destinationInput.setText(address);

        calculateFare();
    }

    private void bookRide(
            RadioGroup payment
    ) {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {

            Toast.makeText(
                    this,
                    "Please log in first.",
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
                    "Please select your pickup location.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (
                pickupLat == 0
                &&
                pickupLng == 0
        ) {

            Toast.makeText(
                    this,
                    "Please get your current location first.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (destination.isEmpty()) {

            Toast.makeText(
                    this,
                    "Please select your destination.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (
                destinationLat == 0
                ||
                destinationLng == 0
        ) {

            Toast.makeText(
                    this,
                    "Please choose the destination on the map.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        int selectedId =
                payment.getCheckedRadioButtonId();

        String paymentMethod = "Cash";

        if (selectedId != -1) {

            RadioButton selected =
                    findViewById(selectedId);

            if (selected != null) {

                paymentMethod =
                        selected
                                .getText()
                                .toString();
            }
        }

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
                paymentMethod
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

        db.collection("rides
