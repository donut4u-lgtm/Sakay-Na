
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
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;

public class DriverActivity extends Activity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private LocationManager locationManager;

    private TextView statusText;
    private TextView currentRideText;
    private TextView requestsText;

    private Button onlineButton;
    private Button offlineButton;

    private boolean driverOnline = false;

    private double driverLatitude = 0.0;
    private double driverLongitude = 0.0;

    private String currentRideId = "";

    private ListenerRegistration rideListener;
    private ListenerRegistration requestListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        buildScreen();

        startLocationUpdates();

        loadDriverStatus();

        listenForRideRequests();

        listenForCurrentRide();
    }

    private void buildScreen() {

        ScrollView scrollView =
                new ScrollView(this);

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setPadding(
                24,
                24,
                24,
                24
        );

        root.setBackgroundColor(
                Color.WHITE
        );

        scrollView.addView(root);

        TextView title =
                new TextView(this);

        title.setText(
                "Sakay Na Driver"
        );

        title.setTextSize(28);

        title.setTextColor(
                Color.BLACK
        );

        title.setGravity(
                Gravity.CENTER
        );

        title.setPadding(
                0,
                0,
                0,
                20
        );

        root.addView(title);

        statusText =
                new TextView(this);

        statusText.setText(
                "Status: CHECKING..."
        );

        statusText.setTextSize(21);

        statusText.setTextColor(
                Color.BLACK
        );

        statusText.setGravity(
                Gravity.CENTER
        );

        statusText.setPadding(
                0,
                10,
                0,
                20
        );

        root.addView(statusText);

        LinearLayout statusButtons =
                new LinearLayout(this);

        statusButtons.setOrientation(
                LinearLayout.HORIZONTAL
        );

        onlineButton =
                new Button(this);

        onlineButton.setText(
                "🟢 GO ONLINE"
        );

        offlineButton =
                new Button(this);

        offlineButton.setText(
                "⚫ GO OFFLINE"
        );

        statusButtons.addView(
                onlineButton,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        statusButtons.addView(
                offlineButton,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        root.addView(
                statusButtons
        );

        onlineButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        setDriverOnline(true);
                    }
                }
        );

        offlineButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        setDriverOnline(false);
                    }
                }
        );

        Button profileButton =
                new Button(this);

        profileButton.setText(
                "DRIVER PROFILE"
        );

        root.addView(
                profileButton
        );

        profileButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        Intent intent =
                                new Intent(
                                        DriverActivity.this,
                                        DriverOnboardingActivity.class
                                );

                        startActivity(intent);
                    }
                }
        );

        TextView requestsTitle =
                new TextView(this);

        requestsTitle.setText(
                "Ride Requests"
        );

        requestsTitle.setTextSize(
                22
        );

        requestsTitle.setTextColor(
                Color.BLACK
        );

        requestsTitle.setPadding(
                0,
                30,
                0,
                10
        );

        root.addView(
                requestsTitle
        );

        requestsText =
                new TextView(this);

        requestsText.setText(
                "No ride requests."
        );

        requestsText.setTextSize(
                16
        );

        requestsText.setTextColor(
                Color.DKGRAY
        );

        root.addView(
                requestsText
        );

        TextView currentTitle =
                new TextView(this);

        currentTitle.setText(
                "Current Ride"
        );

        currentTitle.setTextSize(
                22
        );

        currentTitle.setTextColor(
                Color.BLACK
        );

        currentTitle.setPadding(
                0,
                30,
                0,
                10
        );

        root.addView(
                currentTitle
        );

        currentRideText =
                new TextView(this);

        currentRideText.setText(
                "No active ride."
        );

        currentRideText.setTextSize(
                17
        );

        currentRideText.setTextColor(
                Color.DKGRAY
        );

        root.addView(
                currentRideText
        );

        Button chatButton =
                new Button(this);

        chatButton.setText(
                "💬 OPEN RIDE CHAT"
        );

        root.addView(
                chatButton
        );

        chatButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        if (currentRideId.isEmpty()) {

                            Toast.makeText(
                                    DriverActivity.this,
                                    "No active ride.",
                                    Toast.LENGTH_SHORT
                            ).show();

                            return;
                        }

                        Intent intent =
                                new Intent(
                                        DriverActivity.this,
                                        RideChatActivity.class
                                );

                        intent.putExtra(
                                "ride_id",
                                currentRideId
                        );

                        startActivity(intent);
                    }
                }
        );

        Button logoutButton =
                new Button(this);

        logoutButton.setText(
                "LOGOUT"
        );

        root.addView(
                logoutButton
        );

        logoutButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        setDriverOnline(false);

                        auth.signOut();

                        finish();
                    }
                }
        );

        setContentView(
                scrollView
        );
    }

    private void loadDriverStatus() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            return;
        }

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(
                        snapshot -> {

                            Boolean online =
                                    snapshot.getBoolean(
                                            "driverOnline"
                                    );

                            driverOnline =
                                    Boolean.TRUE.equals(
                                            online
                                    );

                            updateStatusDisplay();
                        }
                );
    }

    private void setDriverOnline(
            boolean online
    ) {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            return;
        }

        driverOnline = online;

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "driverOnline",
                online
        );

        data.put(
                "driverAvailability",
                online
                        ? "ONLINE"
                        : "OFFLINE"
        );

        data.put(
                "availabilityUpdatedAt",
                FieldValue.serverTimestamp()
        );

        db.collection("users")
                .document(user.getUid())
                .update(data)
                .addOnSuccessListener(
                        unused -> {

                            updateStatusDisplay();

                            Toast.makeText(
                                    this,
                                    online
                                            ? "You are ONLINE."
                                            : "You are OFFLINE.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                )
                .addOnFailureListener(
                        e -> {

                            driverOnline =
                                    !online;

                            updateStatusDisplay();

                            Toast.makeText(
                                    this,
                                    "Status update failed: "
                                            + e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                );
    }

    private void updateStatusDisplay() {

        if (driverOnline) {

            statusText.setText(
                    "🟢 ONLINE — ACCEPTING RIDES"
            );

            statusText.setTextColor(
                    Color.rgb(
                            0,
                            130,
                            0
                    )
            );

        } else {

            statusText.setText(
                    "⚫ OFFLINE — NOT ACCEPTING RIDES"
            );

            statusText.setTextColor(
                    Color.DKGRAY
            );
        }
    }

    private void listenForRideRequests() {

        if (requestListener != null) {
            requestListener.remove();
        }

        requestListener =
                db.collection("rides")
                        .whereEqualTo(
                                "status",
                                "REQUESTED"
                        )
                        .orderBy(
                                "createdAt",
                                Query.Direction.DESCENDING
                        )
                        .addSnapshotListener(
                                (snapshots, error) -> {

                                    if (error != null) {
                                        return;
                                    }

                                    if (!driverOnline) {

                                        requestsText.setText(
                                                "Go ONLINE to receive ride requests."
                                        );

                                        return;
                                    }

                                    if (snapshots == null
                                            || snapshots.isEmpty()) {

                                        requestsText.setText(
                                                "No ride requests."
                                        );

                                        return;
                                    }

                                    StringBuilder text =
                                            new StringBuilder();

                                    text.append(
                                            "Available rides:\n\n"
                                    );

                                    for (
                                            DocumentSnapshot ride
                                            : snapshots.getDocuments()
                                    ) {

                                        String pickup =
                                                ride.getString(
                                                        "pickup"
                                                );

                                        String destination =
                                                ride.getString(
                                                        "destination"
                                                );

                                        Object fare =
                                                ride.get("fare");

                                        text.append(
                                                "Pickup: "
                                        )
                                                .append(
                                                        pickup
                                                )
                                                .append(
                                                        "\nDestination: "
                                                )
                                                .append(
                                                        destination
                                                )
                                                .append(
                                                        "\nFare: ₱"
                                                )
                                                .append(
                                                        fare
                                                )
                                                .append(
                                                        "\n\n"
                                                );
                                    }

                                    requestsText.setText(
                                            text.toString()
                                    );
                                }
                        );
    }

    private void listenForCurrentRide() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            return;
        }

        if (rideListener != null) {
            rideListener.remove();
        }

        rideListener =
                db.collection("rides")
                        .whereEqualTo(
                                "driverId",
                                user.getUid()
                        )
                        .whereIn(
                                "status",
                                java.util.Arrays.asList(
                                        "ACCEPTED",
                                        "DRIVER_ON_THE_WAY",
                                        "DRIVER_ARRIVED",
                                        "IN_PROGRESS",
                                        "FINISHED"
                                )
                        )
                        .limit(1)
                        .addSnapshotListener(
                                (snapshots, error) -> {

                                    if (error != null) {
                                        return;
                                    }

                                    if (snapshots == null
                                            || snapshots.isEmpty()) {

                                        currentRideId = "";

                                        currentRideText.setText(
                                                "No active ride."
                                        );

                                        return;
                                    }

                                    DocumentSnapshot ride =
                                            snapshots.getDocuments()
                                                    .get(0);

                                    currentRideId =
                                            ride.getId();

                                    String pickup =
                                            ride.getString(
                                                    "pickup"
                                            );

                                    String destination =
                                            ride.getString(
                                                    "destination"
                                            );

                                    String status =
                                            ride.getString(
                                                    "status"
                                            );

                                    Object fare =
                                            ride.get("fare");

                                    String payment =
                                            ride.getString(
                                                    "paymentMethod"
                                            );

                                    currentRideText.setText(
                                            "Pickup: "
                                                    + pickup
                                                    + "\n\nDestination: "
                                                    + destination
                                                    + "\n\nFare: ₱"
                                                    + fare
                                                    + "\n\nPayment: "
                                                    + payment
                                                    + "\n\nStatus: "
                                                    + status
                                    );
                                }
                        );
    }

    private void startLocationUpdates() {

        locationManager =
                (LocationManager)
                        getSystemService(
                                LOCATION_SERVICE
                        );

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    100
            );

            return;
        }

        LocationListener listener =
                new LocationListener() {

                    @Override
                    public void onLocationChanged(
                            Location location
                    ) {

                        driverLatitude =
                                location.getLatitude();

                        driverLongitude =
                                location.getLongitude();

                        uploadDriverLocation();
                    }
                };

        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000,
                10,
                listener
        );
    }

    private void uploadDriverLocation() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            return;
        }

        Map<String, Object> location =
                new HashMap<>();

        location.put(
                "driverId",
                user.getUid()
        );

        location.put(
                "latitude",
                driverLatitude
        );

        location.put(
                "longitude",
                driverLongitude
        );

        location.put(
                "driverOnline",
                driverOnline
        );

        location.put(
                "updatedAt",
                FieldValue.serverTimestamp()
        );

        db.collection("driverLocations")
                .document(user.getUid())
                .set(location);
    }

    @Override
    protected void onDestroy() {

        if (rideListener != null) {
            rideListener.remove();
        }

        if (requestListener != null) {
            requestListener.remove();
        }

        super.onDestroy();
    }
}
