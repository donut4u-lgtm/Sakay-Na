package com.sakyna.app;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;

public class DriverActivity extends Activity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private LocationManager locationManager;
    private LocationListener locationListener;
    private ListenerRegistration rideListener;

    private LinearLayout root;
    private TextView statusText;
    private TextView gpsText;

    private boolean driverOnline = false;

    private double currentLatitude = 0.0;
    private double currentLongitude = 0.0;

    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    private String driverId = "";
    private String driverName = "";
    private String driverPhone = "";
    private String currentRideId = "";

    private final int GREEN = Color.rgb(0, 150, 80);
    private final int BLUE = Color.rgb(35, 110, 210);
    private final int RED = Color.rgb(210, 50, 50);
    private final int ORANGE = Color.rgb(245, 150, 30);
    private final int DARK = Color.rgb(35, 35, 35);
    private final int LIGHT = Color.rgb(245, 248, 246);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loadDriverProfile();
    }

    private void loadDriverProfile() {

        if (auth.getCurrentUser() == null) {
            showLoginMessage();
            return;
        }

        driverId = auth.getCurrentUser().getUid();

        SharedPreferences preferences =
                getSharedPreferences("SakayNa", MODE_PRIVATE);

        driverName = preferences.getString("name", "Driver");
        driverPhone = preferences.getString("phone", "");

        showDashboard();
    }

    private void setupRoot() {

        ScrollView scrollView = new ScrollView(this);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(25, 30, 25, 40);
        root.setBackgroundColor(LIGHT);

        scrollView.addView(root);
        setContentView(scrollView);
    }

    private TextView title(String text, int size) {

        TextView textView = new TextView(this);

        textView.setText(text);
        textView.setTextSize(size);
        textView.setTextColor(DARK);
        textView.setGravity(Gravity.CENTER);
        textView.setPadding(10, 15, 10, 15);

        return textView;
    }

    private Button button(String text, int color) {

        Button button = new Button(this);

        button.setText(text);
        button.setTextSize(17);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setBackgroundColor(color);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(0, 8, 0, 8);

        button.setLayoutParams(params);

        return button;
    }

    private void showDashboard() {

        setupRoot();

        TextView heading =
                title("🛺 SAKAY NA DRIVER", 28);

        heading.setTextColor(BLUE);
        heading.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        root.addView(heading);

        root.addView(
                title(
                        "Welcome, " + driverName,
                        20
                )
        );

        statusText =
                title(
                        driverOnline
                                ? "🟢 ONLINE"
                                : "🔴 OFFLINE",
                        20
                );

        statusText.setTextColor(
                driverOnline ? GREEN : RED
        );

        root.addView(statusText);

        Button onlineButton =
                button(
                        driverOnline
                                ? "GO OFFLINE"
                                : "GO ONLINE",
                        driverOnline ? RED : GREEN
                );

        root.addView(onlineButton);

        onlineButton.setOnClickListener(v -> {

            if (driverOnline) {
                goOffline();
            } else {
                goOnline();
            }
        });

        gpsText =
                title(
                        "📍 GPS\nWaiting for location...",
                        16
                );

        root.addView(gpsText);

        Button requests =
                button(
                        "🚕 RIDE REQUESTS",
                        ORANGE
                );

        root.addView(requests);

        requests.setOnClickListener(
                v -> showRideRequests()
        );

        Button current =
                button(
                        "🚗 CURRENT RIDE",
                        BLUE
                );

        root.addView(current);

        current.setOnClickListener(
                v -> showCurrentRide()
        );

        Button earnings =
                button(
                        "💰 EARNINGS",
                        GREEN
                );

        root.addView(earnings);

        earnings.setOnClickListener(
                v -> showEarnings()
        );

        Button logout =
                button(
                        "LOGOUT",
                        DARK
                );

        root.addView(logout);

        logout.setOnClickListener(
                v -> logout()
        );

        if (driverOnline) {
            startLocationTracking();
        }

        listenForRideRequests();
    }

    private void goOnline() {

        if (!hasLocationPermission()) {

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

        driverOnline = true;

        setDriverOnline(true);
        startLocationTracking();
        showDashboard();

        toast("You are now ONLINE.");
    }

    private void goOffline() {

        driverOnline = false;

        stopLocationTracking();
        setDriverOnline(false);
        showDashboard();

        toast("You are now OFFLINE.");
    }

    private boolean hasLocationPermission() {

        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
                ||
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;
    }

    private void startLocationTracking() {

        if (!hasLocationPermission()) {
            return;
        }

        locationManager =
                (LocationManager)
                        getSystemService(
                                Context.LOCATION_SERVICE
                        );

        locationListener =
                new LocationListener() {

                    @Override
                    public void onLocationChanged(
                            @NonNull Location location) {

                        currentLatitude =
                                location.getLatitude();

                        currentLongitude =
                                location.getLongitude();

                        updateGpsDisplay();
                        saveDriverLocation();
                    }

                    @Override
                    public void onProviderEnabled(
                            @NonNull String provider) {
                    }

                    @Override
                    public void onProviderDisabled(
                            @NonNull String provider) {
                    }
                };

        try {

            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    3000,
                    5,
                    locationListener
            );

        } catch (SecurityException ignored) {
        }

        try {

            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    5000,
                    10,
                    locationListener
            );

        } catch (SecurityException ignored) {
        }

        updateGpsDisplay();
    }

    private void stopLocationTracking() {

        if (locationManager != null
                && locationListener != null) {

            try {
                locationManager.removeUpdates(
                        locationListener
                );
            } catch (SecurityException ignored) {
            }
        }

        locationListener = null;
    }

    private void updateGpsDisplay() {

        if (gpsText == null) {
            return;
        }

        if (currentLatitude == 0.0
                && currentLongitude == 0.0) {

            gpsText.setText(
                    "📍 GPS\nWaiting for location..."
            );

            return;
        }

        gpsText.setText(
                "📍 GPS LOCATION\n\n" +
                        "Latitude: " +
                        currentLatitude +
                        "\nLongitude: " +
                        currentLongitude
        );
    }

    private void saveDriverLocation() {

        if (!driverOnline || driverId.isEmpty()) {
            return;
        }

        Map<String, Object> location =
                new HashMap<>();

        location.put(
                "driverId",
                driverId
        );

        location.put(
                "driverName",
                driverName
        );

        location.put(
                "driverPhone",
                driverPhone
        );

        location.put(
                "latitude",
                currentLatitude
        );

        location.put(
                "longitude",
                currentLongitude
        );

        location.put(
                "online",
                true
        );

        location.put(
                "updatedAt",
                Timestamp.now()
        );

        db.collection("driverLocations")
                .document(driverId)
                .set(location);
    }

    private void setDriverOnline(
            boolean online) {

        if (driverId.isEmpty()) {
            return;
        }

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "driverId",
                driverId
        );

        data.put(
                "driverName",
                driverName
        );

        data.put(
                "driverPhone",
                driverPhone
        );

        data.put(
                "online",
                online
        );

        data.put(
                "latitude",
                currentLatitude
        );

        data.put(
                "longitude",
                currentLongitude
        );

        data.put(
                "updatedAt",
                Timestamp.now()
        );

        db.collection("driverLocations")
                .document(driverId)
                .set(data);
    }

    private void listenForRideRequests() {

        if (rideListener != null) {
            rideListener.remove();
        }

        rideListener =
                db.collection("rides")
                        .whereEqualTo(
                                "status",
                                "REQUESTED"
                        )
                        .addSnapshotListener(
                                (snapshot, error) -> {

                                    if (error != null
                                            || snapshot == null) {
                                        return;
                                    }

                                    for (DocumentChange change :
                                            snapshot.getDocumentChanges()) {

                                        if (change.getType()
                                                == DocumentChange.Type.ADDED) {

                                            showRideNotification();
                                            break;
                                        }
                                    }
                                }
                        );
    }

    private void showRideNotification() {

        Toast.makeText(
                this,
                "🚕 NEW RIDE REQUEST",
                Toast.LENGTH_LONG
        ).show();
    }

    private void showRideRequests() {

        setupRoot();

        TextView heading =
                title(
                        "🚕 RIDE REQUESTS",
                        28
                );

        heading.setTextColor(ORANGE);

        heading.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        root.addView(heading);

        TextView loading =
                title(
                        "Loading ride requests...",
                        18
                );

        root.addView(loading);

        db.collection("rides")
                .whereEqualTo(
                        "status",
                        "REQUESTED"
                )
                .get()
                .addOnSuccessListener(
                        snapshot -> {

                            if (root == null) {
                                return;
                            }

                            root.removeView(loading);

                            if (snapshot == null
                                    || snapshot.isEmpty()) {

                                root.addView(
                                        title(
                                                "No ride requests right now.",
                                                18
                                        )
                                );

                                addBackButton();

                                return;
                            }

                            for (DocumentSnapshot document :
                                    snapshot.getDocuments()) {

                                try {

                                    addRideRequestCard(
                                            document
                                    );

                                } catch (Exception e) {

                                    TextView error =
                                            title(
                                                    "A ride could not be displayed.",
                                                    16
                                            );

                                    error.setTextColor(RED);

                                    root.addView(error);
                                }
                            }

                            addBackButton();
                        }
                )
                .addOnFailureListener(
                        e -> {

                            if (root == null) {
                                return;
                            }

                            root.removeView(loading);

                            root.addView(
                                    title(
                                            "Could not load rides.\n\n" +
                                                    "Please try again.",
                                            18
                                    )
                            );

                            addBackButton();
                        }
                );
    }

    private void addBackButton() {

        Button back =
                button(
                        "BACK",
                        DARK
                );

        root.addView(back);

        back.setOnClickListener(
                v -> showDashboard()
        );
    }

    private void addRideRequestCard(
            DocumentSnapshot document) {

        if (document == null || root == null) {
            return;
        }

        String rideId =
                document.getId();

        String passengerName =
                getSafeValue(
                        document,
                        "passengerName"
                );

        String passengerPhone =
                getSafeValue(
                        document,
                        "passengerPhone"
                );

        String pickup =
                getSafeValue(
                        document,
                        "pickup"
                );

        String destination =
                getSafeValue(
                        document,
                        "destination"
                );

        String fare =
                getSafeValue(
                        document,
                        "fare"
                );

        TextView card =
                title(
                        "👤 Passenger: " +
                                passengerName +
                                "\n📞 " +
                                passengerPhone +
                                "\n\n📍 Pickup: " +
                                pickup +
                                "\n\n🏁 Destination: " +
                                destination +
                                "\n\n💰 Fare: " +
                                fare,
                        16
                );

        card.setGravity(Gravity.START);

        card.setPadding(
                20,
                20,
                20,
                20
        );

        root.addView(card);

        Button accept =
                button(
                        "ACCEPT RIDE",
                        GREEN
                );

        root.addView(accept);

        accept.setOnClickListener(
                v -> acceptRide(rideId)
        );

        Button decline =
                button(
                        "DECLINE",
                        RED
                );

        root.addView(decline);

        decline.setOnClickListener(
                v -> declineRide(rideId)
        );
    }

    private String getSafeValue(
            DocumentSnapshot document,
            String field) {

        try {

            Object value =
                    document.get(field);

            if (value == null) {
                return "";
            }

            return String.valueOf(value);

        } catch (Exception e) {

            return "";
        }
    }

    private void acceptRide(
            String rideId) {

        Map<String, Object> update =
                new HashMap<>();

        update.put(
                "status",
                "ACCEPTED"
        );

        update.put(
                "driverId",
                driverId
        );

        update.put(
                "driverName",
                driverName
        );

        update.put(
                "driverPhone",
                driverPhone
        );

        update.put(
                "driverLatitude",
                currentLatitude
        );

        update.put(
                "driverLongitude",
                currentLongitude
        );

        update.put(
                "acceptedAt",
                Timestamp.now()
        );

        db.collection("rides")
                .document(rideId)
                .update(update)
                .addOnSuccessListener(
                        unused -> {

                            currentRideId =
                                    rideId;

                            toast(
                                    "Ride accepted."
                            );

                            showCurrentRide();
                        }
                )
                .addOnFailureListener(
                        e -> toast(
                                "Could not accept ride."
                        )
                );
    }

    private void declineRide(
            String rideId) {

        toast(
                "Ride declined."
        );
    }

    private void showCurrentRide() {

        setupRoot();

        TextView heading =
                title(
                        "🚗 CURRENT RIDE",
                        28
                );

        heading.setTextColor(BLUE);

        heading.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        root.addView(heading);

        if (currentRideId.isEmpty()) {
            findAcceptedRide();
        } else {
            loadCurrentRide(
                    currentRideId
            );
        }

        addBackButton();
    }

    private void findAcceptedRide() {

        db.collection("rides")
                .whereEqualTo(
                        "driverId",
                        driverId
                )
                .limit(20)
                .get()
                .addOnSuccessListener(
                        snapshot -> {

                            for (DocumentSnapshot document :
                                    snapshot.getDocuments()) {

                                String status =
                                        getSafeValue(
                                                document,
                                                "status"
                                        );

                                if (status.equals(
                                        "ACCEPTED")
                                        || status.equals(
                                        "DRIVER_ON_THE_WAY")
                                        || status.equals(
                                        "DRIVER_ARRIVED")
                                        || status.equals(
                                        "IN_PROGRESS")) {

                                    currentRideId =
                                            document.getId();

                                    loadCurrentRide(
                                            currentRideId
                                    );

                                    return;
                                }
                            }

                            root.addView(
                                    title(
                                            "No current ride.",
                                            18
                                    )
                            );
                        }
                )
                .addOnFailureListener(
                        e -> root.addView(
                                title(
                                        "Could not load current ride.",
                                        16
                                )
                        )
                );
    }

    private void loadCurrentRide(
            String rideId) {

        db.collection("rides")
                .document(rideId)
                .get()
                .addOnSuccessListener(
                        document -> {

                            if (!document.exists()) {

                                root.addView(
                                        title(
                                                "Ride not found.",
                                                18
                                        )
                                );

                                return;
                            }

                            String passenger =
                                    getSafeValue(
                                            document,
                                            "passengerName"
                                    );

                            String phone =
                                    getSafeValue(
                                            document,
                                            "passengerPhone"
                                    );

                            String pickup =
                                    getSafeValue(
                                            document,
                                            "pickup"
                                    );

                            String destination =
                                    getSafeValue(
                                            document,
                                            "destination"
                                    );

                            String status =
                                    getSafeValue(
                                            document,
                                            "status"
                                    );

                            String fare =
                                    getSafeValue(
                                            document,
                                            "fare"
                                    );

                            root.addView(
                                    title(
                                            "👤 Passenger\n" +
                                                    passenger +
                                                    "\n" +
                                                    phone +
                                                    "\n\n📍 Pickup\n" +
                                                    pickup +
                                                    "\n\n🏁 Destination\n" +
                                                    destination +
                                                    "\n\n💰 Fare\n" +
                                                    fare +
                                                    "\n\nSTATUS\n" +
                                                    status,
                                            18
                                    )
                            );

                            addTripButtons(
                                    rideId,
                                    status
                            );
                        }
                )
                .addOnFailureListener(
                        e -> root.addView(
                                title(
                                        "Could not load ride.",
                                        16
                                )
                        )
                );
    }

    private void addTripButtons(
            String rideId,
            String status) {

        if (status.equals(
                "ACCEPTED")) {

            Button onWay =
                    button(
                            "🚗 DRIVER ON THE WAY",
                            BLUE
                    );

            root.addView(onWay);

            onWay.setOnClickListener(
                    v -> updateRideStatus(
                            rideId,
                            "DRIVER_ON_THE_WAY"
                    )
            );
        }

        if (status.equals(
                "DRIVER_ON_THE_WAY")) {

            Button arrived =
                    button(
                            "📍 DRIVER ARRIVED",
                            ORANGE
                    );

            root.addView(arrived);

            arrived.setOnClickListener(
                    v -> updateRideStatus(
                            rideId,
                            "DRIVER_ARRIVED"
                    )
            );
        }

        if (status.equals(
                "DRIVER_ARRIVED")) {

            Button start =
                    button(
                            "▶ START TRIP",
                            GREEN
                    );

            root.addView(start);

            start.setOnClickListener(
                    v -> updateRideStatus(
                            rideId,
                            "IN_PROGRESS"
                    )
            );
        }

        if (status.equals(
                "IN_PROGRESS")) {

            Button finish =
                    button(
                            "✓ FINISH TRIP",
                            GREEN
                    );

            root.addView(finish);

            finish.setOnClickListener(
                    v -> updateRideStatus(
                            rideId,
                            "FINISHED"
                    )
            );
        }
    }

    private void updateRideStatus(
            String rideId,
            String newStatus) {

        Map<String, Object> update =
                new HashMap<>();

        update.put(
                "status",
                newStatus
        );

        update.put(
                "updatedAt",
                Timestamp.now()
        );

        db.collection("rides")
                .document(rideId)
                .update(update)
                .addOnSuccessListener(
                        unused -> {

                            toast(
                                    "Ride status updated."
                            );

                            showCurrentRide();
                        }
                )
                .addOnFailureListener(
                        e -> toast(
                                "Status update failed."
                        )
                );
    }

    private void showEarnings() {

        setupRoot();

        TextView heading =
                title(
                        "💰 DRIVER EARNINGS",
                        28
                );

        heading.setTextColor(GREEN);

        heading.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        root.addView(heading);

        db.collection("rides")
                .whereEqualTo(
                        "driverId",
                        driverId
                )
                .limit(100)
                .get()
                .addOnSuccessListener(
                        snapshot -> {

                            double total = 0;
                            int completed = 0;

                            for (DocumentSnapshot document :
                                    snapshot.getDocuments()) {

                                String status =
                                        getSafeValue(
                                                document,
                                                "status"
                                        );

                                if (!status.equals(
                                        "FINISHED")
                                        && !status.equals(
                                        "COMPLETED")) {

                                    continue;
                                }

                                completed++;

                                Object fare =
                                        document.get(
                                                "finalFare"
                                        );

                                if (!(fare
                                        instanceof Number)) {

                                    fare =
                                            document.get(
                                                    "fare"
                                            );
                                }

                                if (fare
                                        instanceof Number) {

                                    total +=
                                            ((Number) fare)
                                                    .doubleValue();
                                }
                            }

                            root.addView(
                                    title(
                                            "Completed rides: " +
                                                    completed +
                                                    "\n\nTotal earnings: " +
                                                    total,
                                            20
                                    )
                            );
                        }
                )
                .addOnFailureListener(
                        e -> root.addView(
                                title(
                                        "Could not load earnings.",
                                        16
                                )
                        )
                );

        addBackButton();
    }

    private void logout() {

        driverOnline = false;

        stopLocationTracking();

        setDriverOnline(false);

        if (rideListener != null) {

            rideListener.remove();
            rideListener = null;
        }

        auth.signOut();

        Intent intent =
                new Intent(
                        DriverActivity.this,
                        MainActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        finish();
    }

    private void showLoginMessage() {

        Toast.makeText(
                this,
                "Please login first.",
                Toast.LENGTH_LONG
        ).show();

        startActivity(
                new Intent(
                        this,
                        MainActivity.class
                )
        );

        finish();
    }

    private void toast(
            String message) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
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

        if (requestCode
                == LOCATION_PERMISSION_REQUEST) {

            boolean granted = false;

            for (int result :
                    grantResults) {

                if (result
                        == PackageManager.PERMISSION_GRANTED) {

                    granted = true;
                    break;
                }
            }

            if (granted) {

                driverOnline = true;

                setDriverOnline(true);
                startLocationTracking();
                showDashboard();

                toast(
                        "GPS enabled. You are ONLINE."
                );

            } else {

                toast(
                        "Location permission is required."
                );
            }
        }
    }

    @Override
    protected void onDestroy() {

        stopLocationTracking();

        if (rideListener != null) {

            rideListener.remove();
            rideListener = null;
        }

        super.onDestroy();
    }
}
