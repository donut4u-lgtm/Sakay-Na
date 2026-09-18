
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

    private LocationManager locationManager;
    private LocationListener locationListener;
    private ListenerRegistration rideListener;

    private String activeRideId;

    private double pickupLat = 0;
    private double pickupLng = 0;
    private double destinationLat = 0;
    private double destinationLng = 0;

    private double baseFare = 25;
    private double perKm = 10;
    private double minFare = 25;
    private double maxFare = 500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        preferences = getSharedPreferences("SakayNa", MODE_PRIVATE);

        buildScreen();
        loadFareSettings();
        requestLocation();
        restoreActiveRide();
    }

    private TextView text(String value, float size) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(Color.rgb(45, 55, 50));
        t.setPadding(0, 7, 0, 7);
        return t;
    }

    private Button button(String value) {
        Button b = new Button(this);

        b.setText(value);
        b.setTextSize(16);
        b.setAllCaps(false);
        b.setTypeface(null, Typeface.BOLD);
        b.setTextColor(Color.WHITE);
        b.setGravity(Gravity.CENTER);
        b.setPadding(10, 12, 10, 12);
        b.setBackgroundColor(Color.rgb(0, 125, 75));

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(-1, -2);

        p.setMargins(0, 5, 0, 5);
        b.setLayoutParams(p);

        return b;
    }

    private LinearLayout.LayoutParams full() {
        return new LinearLayout.LayoutParams(-1, -2);
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

        TextView sub = new TextView(this);
        sub.setText("Passenger • Local tricycle ride");
        sub.setTextSize(14);
        sub.setTextColor(Color.WHITE);
        sub.setGravity(Gravity.CENTER);
        header.addView(sub);

        root.addView(header, full());

        ScrollView scroll = new ScrollView(this);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(16, 12, 16, 20);

        TextView booking = text("🚕 BOOK YOUR RIDE", 22);
        booking.setTypeface(null, Typeface.BOLD);
        booking.setGravity(Gravity.CENTER);
        booking.setTextColor(Color.rgb(0, 110, 70));
        content.addView(booking, full());

        TextView pickupLabel = text("📍 PICKUP LOCATION", 15);
        pickupLabel.setTypeface(null, Typeface.BOLD);
        content.addView(pickupLabel);

        pickupInput = new EditText(this);
        pickupInput.setHint("Where should we pick you up?");
        pickupInput.setTextSize(16);
        pickupInput.setPadding(16, 12, 16, 12);
        pickupInput.setBackgroundColor(Color.WHITE);
        content.addView(pickupInput, full());

        Button gpsButton = button("📍 USE MY CURRENT LOCATION");
        gpsButton.setOnClickListener(v -> useCurrentLocation());
        content.addView(gpsButton);

        TextView destinationLabel = text("🎯 DESTINATION", 15);
        destinationLabel.setTypeface(null, Typeface.BOLD);
        content.addView(destinationLabel);

        destinationInput = new EditText(this);
        destinationInput.setHint("Where are you going?");
        destinationInput.setTextSize(16);
        destinationInput.setPadding(16, 12, 16, 12);
        destinationInput.setBackgroundColor(Color.WHITE);
        content.addView(destinationInput, full());

        Button destinationButton = button("🗺️ CHOOSE DESTINATION ON MAP");
        destinationButton.setOnClickListener(v -> chooseDestination());
        content.addView(destinationButton);

        TextView type = text("🛺 RIDE TYPE", 15);
        type.setTypeface(null, Typeface.BOLD);
        content.addView(type);

        TextView tricycle = text("🛺 TRICYCLE", 18);
        tricycle.setTypeface(null, Typeface.BOLD);
        tricycle.setGravity(Gravity.CENTER);
        tricycle.setTextColor(Color.rgb(0, 110, 70));
        tricycle.setBackgroundColor(Color.WHITE);
        content.addView(tricycle, full());

        TextView paymentLabel = text("💳 PAYMENT METHOD", 15);
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

        content.addView(payment, full());

        fareText = text("💰 Estimated fare: ₱25", 23);
        fareText.setTypeface(null, Typeface.BOLD);
        fareText.setGravity(Gravity.CENTER);
        fareText.setTextColor(Color.rgb(0, 125, 75));
        fareText.setBackgroundColor(Color.WHITE);
        content.addView(fareText, full());

        statusText = text("🟢 No active ride", 16);
        statusText.setTypeface(null, Typeface.BOLD);
        statusText.setGravity(Gravity.CENTER);
        content.addView(statusText, full());

        bookButton = button("🛺 BOOK A RIDE");
        bookButton.setTextSize(19);
        bookButton.setBackgroundColor(Color.rgb(0, 150, 80));
        bookButton.setOnClickListener(v -> bookRide(payment));
        content.addView(bookButton);

        mapButton = button("🗺️ LIVE RIDE MAP");
        mapButton.setOnClickListener(v -> openLiveMap());
        content.addView(mapButton);

        chatButton = button("💬 CHAT WITH DRIVER");
        chatButton.setOnClickListener(v -> openChat());
        content.addView(chatButton);

        cancelButton = button("❌ CANCEL RIDE");
        cancelButton.setBackgroundColor(Color.rgb(190, 55, 55));
        cancelButton.setOnClickListener(v -> cancelRide());
        content.addView(cancelButton);

        historyButton = button("📜 RIDE HISTORY");
        historyButton.setBackgroundColor(Color.rgb(65, 90, 100));
        historyButton.setOnClickListener(v -> showHistory());
        content.addView(historyButton);

        Button logoutButton = button("🚪 LOGOUT");
        logoutButton.setBackgroundColor(Color.rgb(85, 85, 85));
        logoutButton.setOnClickListener(v -> logout());
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
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED
                &&
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED
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
                (LocationManager) getSystemService(LOCATION_SERVICE);

        if (locationManager == null) {
            return;
        }

        locationListener =
                new LocationListener() {

                    @Override
                    public void onLocationChanged(
                            @NonNull Location location
                    ) {

                        pickupLat = location.getLatitude();
                        pickupLng = location.getLongitude();

                        if (
                                pickupInput != null
                                &&
                                pickupInput.getText()
                                        .toString()
                                        .trim()
                                        .isEmpty()
                        ) {

                            pickupInput.setText(
                                    String.format(
                                            Locale.US,
                                            "%.6f, %.6f",
                                            pickupLat,
                                            pickupLng
                                    )
                            );
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

        String address = getAddress(pickupLat, pickupLng);

        if (address == null || address.isEmpty()) {

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

        Intent intent =
                new Intent(this, MapActivity.class);

        intent.putExtra("mode", "SELECT_DESTINATION");
        intent.putExtra("pickup_latitude", pickupLat);
        intent.putExtra("pickup_longitude", pickupLng);
        intent.putExtra(
                "pickup_address",
                pickupInput.getText().toString()
        );

        startActivityForResult(
                intent,
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

    private void bookRide(RadioGroup payment) {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {

            Toast.makeText(
                    this,
                    "Please log in first.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String pickup =
                pickupInput.getText().toString().trim();

        String destination =
                destinationInput.getText().toString().trim();

        if (pickup.isEmpty()) {

            Toast.makeText(
                    this,
                    "Please select pickup.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (pickupLat == 0 && pickupLng == 0) {

            Toast.makeText(
                    this,
                    "Please get your pickup GPS location.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (destination.isEmpty()) {

            Toast.makeText(
                    this,
                    "Please select destination.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (destinationLat == 0 || destinationLng == 0) {

            Toast.makeText(
                    this,
                    "Please choose destination on the map.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String paymentMethod = "Cash";

        int selectedId =
                payment.getCheckedRadioButtonId();

        if (selectedId != -1) {

            RadioButton selected =
                    findViewById(selectedId);

            if (selected != null) {
                paymentMethod =
                        selected.getText().toString();
            }
        }

        Map<String, Object> ride =
                new HashMap<>();

        ride.put("passengerId", user.getUid());
        ride.put("pickup", pickup);
        ride.put("destination", destination);

        ride.put("pickupLatitude", pickupLat);
        ride.put("pickupLongitude", pickupLng);

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
                                    "🚦 REQUESTED\nWaiting for a driver"
                            );

                            listenToRide(
                                    activeRideId
                            );

                            updateButtons();

                            Toast.makeText(
                                    this,
                                    "🛺 Ride request sent.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                )
                .addOnFailureListener(
                        error -> {

                            bookButton.setEnabled(true);

                            statusText.setText(
                                    "🔴 Booking failed"
                            );

                            Toast.makeText(
                                    this,
                                    "Booking failed: " +
                                            error.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();

                            updateButtons();
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
                                                25
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
                                                25
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

    private double number(
            DocumentSnapshot snapshot,
            String field,
            double fallback
    ) {

        Object value = snapshot.get(field);

        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        return fallback;
    }

    private void calculateFare() {

        if (fareText == null) {
            return;
        }

        fareText.setText(
                String.format(
                        Locale.US,
                        "💰 Estimated fare: ₱%.0f",
                        getCurrentFare()
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
                    addresses.get(0).getAddressLine(0) != null
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

        String saved =
                preferences.getString(
                        "activeRideId",
                        null
                );

        if (saved != null && !saved.isEmpty()) {

            db.collection("rides")
                    .document(saved)
                    .get()
                    .addOnSuccessListener(
                            snapshot -> {

                                if (
                                        snapshot.exists()
                                        &&
                                        activeStatus(
                                                snapshot.getString(
                                                        "status"
                                                )
                                        )
                                ) {

                                    activeRideId = saved;

                                    listenToRide(saved);

                                } else {

                                    clearActiveRide();
                                }

                                updateButtons();
                            }
                    )
                    .addOnFailureListener(
                            e -> {

                                clearActiveRide();
                                updateButtons();
                            }
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
                                        query.getDocuments()
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

                                updateButtons();
                            }
                        }
                );
    }

    private boolean activeStatus(String status) {

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

    private void listenToRide(String rideId) {

        if (rideListener != null) {
            rideListener.remove();
        }

        rideListener =
                db.collection("rides")
                        .document(rideId)
                        .addSnapshotListener(
                                (snapshot, error) -> {

                                    if (error != null) {

                                        statusText.setText(
                                                "🔴 Ride update error"
                                        );

                                        return;
                                    }

                                    if (
                                            snapshot == null
                                            ||
                                            !snapshot.exists()
                                    ) {

                                        clearActiveRide();
                                        updateButtons();
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

                                        status = "REQUESTED";
                                    }

                                    statusText.setText(
                                            "🚦 Ride status: " +
                                                    status
                                    );

                                    if (!activeStatus(status)) {
                                        clearActiveRide();
                                    }

                                    updateButtons();
                                }
                        );
    }

    private void clearActiveRide() {

        activeRideId = null;

        preferences
                .edit()
                .remove("activeRideId")
                .apply();

        if (rideListener != null) {

            rideListener.remove();
            rideListener = null;
        }
    }

    private void updateButtons() {

        boolean active =
                activeRideId != null
                &&
                !activeRideId.isEmpty();

        if (bookButton != null) {
            bookButton.setEnabled(!active);
        }

        if (cancelButton != null) {
            cancelButton.setEnabled(active);
        }

        if (mapButton != null) {
            mapButton.setEnabled(active);
        }

        if (chatButton != null) {
            chatButton.setEnabled(active);
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

        Intent intent =
                new Intent(
                        this,
                        MapActivity.class
                );

        intent.putExtra(
                "mode",
                "LIVE_RIDE"
        );

        intent.putExtra(
                "ride_id",
                activeRideId
        );

        startActivity(intent);
    }

    private void openChat() {

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

        Intent intent =
                new Intent(
                        this,
                        RideChatActivity.class
                );

        intent.putExtra(
                "ride_id",
                activeRideId
        );

        intent.putExtra(
                "rideId",
                activeRideId
        );

        startActivity(intent);
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

                            clearActiveRide();

                            statusText.setText(
                                    "❌ Ride cancelled"
                            );

                            updateButtons();
                        }
                )
                .addOnFailureListener(
                        e ->
                                Toast.makeText(
                                        this,
                                        "Cancel failed: " +
                                                e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show()
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

                            history.append(
                                    "📜 RIDE HISTORY\n\n"
                            );

                            for (
                                    DocumentSnapshot ride :
                                    query.getDocuments()
                            ) {

                                String pickup =
                                        safe(
                                                ride.getString(
                                                        "pickup"
                                                )
                                        );

                                String destination =
                                        safe(
                                                ride.getString(
                                                        "destination"
                                                )
                                        );

                                String status =
                                        safe(
                                                ride.getString(
                                                        "status"
                                                )
                                        );

                                String payment =
                                        safe(
                                                ride.getString(
                                                        "paymentMethod"
                                                )
                                        );

                                double fare =
                                        number(
                                                ride,
                                                "fare",
                                                0
                                        );

                                history.append(
                                        "📍 "
                                );

                                history.append(pickup);

                                history.append(
                                        "\n🎯 "
                                );

                                history.append(destination);

                                history.append(
                                        "\n💰 ₱"
                                );

                                history.append(
                                        String.format(
                                                Locale.US,
                                                "%.0f",
                                                fare
                                        )
                                );

                                history.append(
                                        "\n💳 "
                                );

                                history.append(payment);

                                history.append(
                                        "\n🚦 "
                                );

                                history.append(status);

                                history.append(
                                        "\n\n--------------------\n\n"
                                );
                            }

                            showHistoryDialog(
                                    history.toString()
                            );
                        }
                )
                .addOnFailureListener(
                        e ->
                                Toast.makeText(
                                        this,
                                        "History failed: " +
                                                e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show()
                );
    }

    private String safe(String value) {

        if (value == null || value.trim().isEmpty()) {
            return "Unknown";
        }

        return value;
    }

    private void showHistoryDialog(String message) {

        android.app.AlertDialog.Builder builder =
                new android.app.AlertDialog.Builder(this);

        builder.setTitle("📜 Ride History");
        builder.setMessage(message);

        builder.setPositiveButton(
                "Close",
                null
        );

        builder.show();
    }

    private void logout() {

        if (rideListener != null) {
            rideListener.remove();
            rideListener = null;
        }

        activeRideId = null;

        preferences
                .edit()
                .remove("activeRideId")
                .apply();

        auth.signOut();

        Intent intent =
                new Intent(
                        this,
                        MainActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                |
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

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
        ) {

            boolean granted = false;

            for (int result : grantResults) {

                if (
                        result ==
                                PackageManager.PERMISSION_GRANTED
                ) {

                    granted = true;
                    break;
                }
            }

            if (granted) {
                startLocationUpdates();
            } else {

                Toast.makeText(
                        this,
                        "Location permission is needed for pickup.",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    @Override
    protected void onDestroy() {

        if (rideListener != null) {
            rideListener.remove();
            rideListener = null;
        }

        if (locationManager != null
                && locationListener != null) {

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
