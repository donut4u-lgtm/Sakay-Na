
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

import java.util.HashMap;
import java.util.Map;

public class DriverActivity extends Activity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private LocationManager locationManager;
    private LocationListener locationListener;

    private TextView statusText;
    private TextView currentRideText;
    private TextView requestsText;

    private Button onlineButton;
    private Button offlineButton;

    private Button acceptButton;
    private Button declineButton;
    private Button onTheWayButton;
    private Button arrivedButton;
    private Button startRideButton;
    private Button finishRideButton;
    private Button chatButton;
    private Button mapButton;

    private LinearLayout requestContainer;

    private boolean driverOnline = false;

    private double driverLatitude = 0.0;
    private double driverLongitude = 0.0;

    private String currentRideId = "";
    private String currentRideStatus = "";

    private ListenerRegistration rideListener;
    private ListenerRegistration requestListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (auth.getCurrentUser() == null) {
            finish();
            return;
        }

        buildScreen();
        startLocationUpdates();
        loadDriverStatus();
        listenForRideRequests();
        listenForCurrentRide();
    }

    private void buildScreen() {

        ScrollView scrollView = new ScrollView(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("SAKAY NA - DRIVER");
        title.setTextSize(25);
        title.setTextColor(Color.rgb(20, 80, 150));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 10, 0, 20);
        root.addView(title);

        statusText = new TextView(this);
        statusText.setText("Checking driver status...");
        statusText.setTextSize(18);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(10, 15, 10, 15);
        root.addView(statusText);

        LinearLayout onlineRow = new LinearLayout(this);
        onlineRow.setOrientation(LinearLayout.HORIZONTAL);

        onlineButton = new Button(this);
        onlineButton.setText("GO ONLINE");

        offlineButton = new Button(this);
        offlineButton.setText("GO OFFLINE");

        onlineRow.addView(
                onlineButton,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        onlineRow.addView(
                offlineButton,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        root.addView(onlineRow);

        Button profileButton = new Button(this);
        profileButton.setText("DRIVER PROFILE");
        root.addView(profileButton);

        TextView requestTitle = new TextView(this);
        requestTitle.setText("RIDE REQUESTS");
        requestTitle.setTextSize(21);
        requestTitle.setTextColor(Color.rgb(20, 80, 150));
        requestTitle.setPadding(0, 25, 0, 10);
        root.addView(requestTitle);

        requestsText = new TextView(this);
        requestsText.setText("Loading ride requests...");
        requestsText.setTextSize(17);
        requestsText.setPadding(5, 5, 5, 10);
        root.addView(requestsText);

        requestContainer = new LinearLayout(this);
        requestContainer.setOrientation(LinearLayout.VERTICAL);

        root.addView(requestContainer);

        TextView currentTitle = new TextView(this);
        currentTitle.setText("CURRENT RIDE");
        currentTitle.setTextSize(21);
        currentTitle.setTextColor(Color.rgb(20, 80, 150));
        currentTitle.setPadding(0, 25, 0, 10);
        root.addView(currentTitle);

        currentRideText = new TextView(this);
        currentRideText.setText("No current ride.");
        currentRideText.setTextSize(17);
        currentRideText.setPadding(5, 5, 5, 15);
        root.addView(currentRideText);

        acceptButton = new Button(this);
        acceptButton.setText("ACCEPT RIDE");
        acceptButton.setVisibility(View.GONE);
        root.addView(acceptButton);

        declineButton = new Button(this);
        declineButton.setText("DECLINE RIDE");
        declineButton.setVisibility(View.GONE);
        root.addView(declineButton);

        onTheWayButton = new Button(this);
        onTheWayButton.setText("I'M ON THE WAY");
        onTheWayButton.setVisibility(View.GONE);
        root.addView(onTheWayButton);

        arrivedButton = new Button(this);
        arrivedButton.setText("I HAVE ARRIVED");
        arrivedButton.setVisibility(View.GONE);
        root.addView(arrivedButton);

        startRideButton = new Button(this);
        startRideButton.setText("START RIDE");
        startRideButton.setVisibility(View.GONE);
        root.addView(startRideButton);

        finishRideButton = new Button(this);
        finishRideButton.setText("FINISH RIDE");
        finishRideButton.setVisibility(View.GONE);
        root.addView(finishRideButton);

        mapButton = new Button(this);
        mapButton.setText("LIVE RIDE MAP");
        mapButton.setVisibility(View.GONE);
        root.addView(mapButton);

        chatButton = new Button(this);
        chatButton.setText("CHAT WITH PASSENGER");
        chatButton.setVisibility(View.GONE);
        root.addView(chatButton);

        Button logoutButton = new Button(this);
        logoutButton.setText("LOGOUT");
        root.addView(logoutButton);

        scrollView.addView(root);
        setContentView(scrollView);

        onlineButton.setOnClickListener(v -> setDriverOnline(true));

        offlineButton.setOnClickListener(v -> setDriverOnline(false));

        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(
                    DriverActivity.this,
                    DriverOnboardingActivity.class
            );
            startActivity(intent);
        });

        logoutButton.setOnClickListener(v -> logout());

        acceptButton.setOnClickListener(v -> {
            if (!currentRideId.isEmpty()) {
                acceptRide(currentRideId);
            }
        });

        declineButton.setOnClickListener(v -> {
            if (!currentRideId.isEmpty()) {
                declineRequestedRide(currentRideId);
            }
        });

        onTheWayButton.setOnClickListener(v -> {
            if (!currentRideId.isEmpty()) {
                updateRideStatus(
                        currentRideId,
                        "DRIVER_ON_THE_WAY"
                );
            }
        });

        arrivedButton.setOnClickListener(v -> {
            if (!currentRideId.isEmpty()) {
                updateRideStatus(
                        currentRideId,
                        "DRIVER_ARRIVED"
                );
            }
        });

        startRideButton.setOnClickListener(v -> {
            if (!currentRideId.isEmpty()) {
                updateRideStatus(
                        currentRideId,
                        "IN_PROGRESS"
                );
            }
        });

        finishRideButton.setOnClickListener(v -> {
            if (!currentRideId.isEmpty()) {
                updateRideStatus(
                        currentRideId,
                        "FINISHED"
                );
            }
        });

        mapButton.setOnClickListener(v -> openLiveMap());

        chatButton.setOnClickListener(v -> openChat());
    }

    private void loadDriverStatus() {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            return;
        }

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {
                        statusText.setText("Driver profile not found.");
                        return;
                    }

                    Boolean online = document.getBoolean("driverOnline");

                    driverOnline = online != null && online;

                    updateOnlineDisplay();
                })
                .addOnFailureListener(e -> {
                    statusText.setText(
                            "Unable to load driver status: "
                                    + e.getMessage()
                    );
                });
    }

    private void setDriverOnline(boolean online) {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            return;
        }

        Map<String, Object> updates = new HashMap<>();

        updates.put("driverOnline", online);
        updates.put(
                "driverAvailability",
                online ? "ONLINE" : "OFFLINE"
        );
        updates.put(
                "availabilityUpdatedAt",
                FieldValue.serverTimestamp()
        );

        db.collection("users")
                .document(user.getUid())
                .update(updates)
                .addOnSuccessListener(unused -> {

                    driverOnline = online;

                    updateOnlineDisplay();

                    listenForRideRequests();

                    uploadDriverLocation();

                    Toast.makeText(
                            DriverActivity.this,
                            online
                                    ? "You are now ONLINE."
                                    : "You are now OFFLINE.",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            DriverActivity.this,
                            "Unable to change status: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void updateOnlineDisplay() {

        if (driverOnline) {

            statusText.setText(
                    "ONLINE - ACCEPTING RIDE REQUESTS"
            );

            statusText.setTextColor(
                    Color.rgb(0, 130, 60)
            );

        } else {

            statusText.setText(
                    "OFFLINE - NOT ACCEPTING RIDES"
            );

            statusText.setTextColor(
                    Color.rgb(180, 60, 40)
            );
        }
    }

    private void listenForRideRequests() {

        if (requestListener != null) {
            requestListener.remove();
            requestListener = null;
        }

        requestListener =
                db.collection("rides")
                        .whereEqualTo(
                                "status",
                                "REQUESTED"
                        )
                        .addSnapshotListener(
                                (snapshots, error) -> {

                                    if (error != null) {

                                        requestsText.setText(
                                                "Unable to load ride requests:\n"
                                                        + error.getMessage()
                                        );

                                        requestContainer
                                                .removeAllViews();

                                        return;
                                    }

                                    requestContainer
                                            .removeAllViews();

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

                                    requestsText.setText(
                                            "Available rides:"
                                    );

                                    for (DocumentSnapshot ride
                                            : snapshots.getDocuments()) {

                                        addRideRequestCard(ride);
                                    }
                                }
                        );
    }

    private void addRideRequestCard(
            DocumentSnapshot ride
    ) {

        LinearLayout card =
                new LinearLayout(this);

        card.setOrientation(
                LinearLayout.VERTICAL
        );

        card.setPadding(
                20,
                20,
                20,
                20
        );

        TextView rideText =
                new TextView(this);

        rideText.setTextSize(17);
        rideText.setTextColor(Color.BLACK);

        String pickup =
                getStringValue(
                        ride,
                        "pickup"
                );

        String destination =
                getStringValue(
                        ride,
                        "destination"
                );

        String fare =
                getStringValue(
                        ride,
                        "fare"
                );

        String payment =
                getStringValue(
                        ride,
                        "paymentMethod"
                );

        if (fare.isEmpty()) {
            Double fareNumber =
                    ride.getDouble("fare");

            if (fareNumber != null) {
                fare =
                        String.valueOf(
                                fareNumber
                        );
            }
        }

        StringBuilder text =
                new StringBuilder();

        text.append("PICKUP\n");
        text.append(
                pickup.isEmpty()
                        ? "Not provided"
                        : pickup
        );

        text.append("\n\nDESTINATION\n");
        text.append(
                destination.isEmpty()
                        ? "Not provided"
                        : destination
        );

        text.append("\n\nFARE\n₱");
        text.append(
                fare.isEmpty()
                        ? "0"
                        : fare
        );

        text.append("\n\nPAYMENT\n");
        text.append(
                payment.isEmpty()
                        ? "Not specified"
                        : payment
        );

        rideText.setText(text.toString());

        card.addView(rideText);

        Button accept =
                new Button(this);

        accept.setText("ACCEPT");

        Button decline =
                new Button(this);

        decline.setText("DECLINE");

        card.addView(accept);
        card.addView(decline);

        requestContainer.addView(card);

        String rideId =
                ride.getId();

        accept.setOnClickListener(v ->
                acceptRide(rideId)
        );

        decline.setOnClickListener(v ->
                declineRequestedRide(rideId)
        );
    }

    private String getStringValue(
            DocumentSnapshot document,
            String field
    ) {

        String value =
                document.getString(field);

        return value == null ? "" : value;
    }

    private void listenForCurrentRide() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            return;
        }

        if (rideListener != null) {
            rideListener.remove();
            rideListener = null;
        }

        rideListener =
                db.collection("rides")
                        .whereEqualTo(
                                "driverId",
                                user.getUid()
                        )
                        .addSnapshotListener(
                                (snapshots, error) -> {

                                    if (error != null) {

                                        currentRideText.setText(
                                                "Unable to load current ride:\n"
                                                        + error.getMessage()
                                        );

                                        return;
                                    }

                                    currentRideId = "";
                                    currentRideStatus = "";

                                    if (snapshots == null
                                            || snapshots.isEmpty()) {

                                        currentRideText.setText(
                                                "No current ride."
                                        );

                                        hideRideButtons();

                                        return;
                                    }

                                    DocumentSnapshot selected =
                                            null;

                                    for (DocumentSnapshot ride
                                            : snapshots.getDocuments()) {

                                        String status =
                                                ride.getString("status");

                                        if (status == null) {
                                            continue;
                                        }

                                        if (status.equals("ACCEPTED")
                                                || status.equals(
                                                "DRIVER_ON_THE_WAY")
                                                || status.equals(
                                                "DRIVER_ARRIVED")
                                                || status.equals(
                                                "IN_PROGRESS")) {

                                            selected = ride;
                                            break;
                                        }
                                    }

                                    if (selected == null) {

                                        currentRideText.setText(
                                                "No current ride."
                                        );

                                        hideRideButtons();

                                        return;
                                    }

                                    currentRideId =
                                            selected.getId();

                                    currentRideStatus =
                                            selected.getString(
                                                    "status"
                                            );

                                    showCurrentRide(
                                            selected
                                    );
                                }
                        );
    }

    private void showCurrentRide(
            DocumentSnapshot ride
    ) {

        String pickup =
                getStringValue(
                        ride,
                        "pickup"
                );

        String destination =
                getStringValue(
                        ride,
                        "destination"
                );

        String status =
                getStringValue(
                        ride,
                        "status"
                );

        String passengerId =
                getStringValue(
                        ride,
                        "passengerId"
                );

        String text =
                "Status: "
                        + status
                        + "\n\nPickup:\n"
                        + pickup
                        + "\n\nDestination:\n"
                        + destination;

        if (!passengerId.isEmpty()) {
            text +=
                    "\n\nPassenger:\n"
                            + passengerId;
        }

        currentRideText.setText(text);

        acceptButton.setVisibility(
                View.GONE
        );

        declineButton.setVisibility(
                View.GONE
        );

        mapButton.setVisibility(
                View.VISIBLE
        );

        chatButton.setVisibility(
                View.VISIBLE
        );

        onTheWayButton.setVisibility(
                status.equals("ACCEPTED")
                        ? View.VISIBLE
                        : View.GONE
        );

        arrivedButton.setVisibility(
                status.equals("DRIVER_ON_THE_WAY")
                        ? View.VISIBLE
                        : View.GONE
        );

        startRideButton.setVisibility(
                status.equals("DRIVER_ARRIVED")
                        ? View.VISIBLE
                        : View.GONE
        );

        finishRideButton.setVisibility(
                status.equals("IN_PROGRESS")
                        ? View.VISIBLE
                        : View.GONE
        );
    }

    private void hideRideButtons() {

        acceptButton.setVisibility(
                View.GONE
        );

        declineButton.setVisibility(
                View.GONE
        );

        onTheWayButton.setVisibility(
                View.GONE
        );

        arrivedButton.setVisibility(
                View.GONE
        );

        startRideButton.setVisibility(
                View.GONE
        );

        finishRideButton.setVisibility(
                View.GONE
        );

        mapButton.setVisibility(
                View.GONE
        );

        chatButton.setVisibility(
                View.GONE
        );
    }

    private void acceptRide(
            String rideId
    ) {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null
                || rideId == null
                || rideId.isEmpty()) {

            return;
        }

        Map<String, Object> updates =
                new HashMap<>();

        updates.put(
                "driverId",
                user.getUid()
        );

        updates.put(
                "status",
                "ACCEPTED"
        );

        updates.put(
                "acceptedAt",
                FieldValue.serverTimestamp()
        );

        updates.put(
                "driverLatitude",
                driverLatitude
        );

        updates.put(
                "driverLongitude",
                driverLongitude
        );

        db.collection("rides")
                .document(rideId)
                .update(updates)
                .addOnSuccessListener(unused -> {

                    currentRideId =
                            rideId;

                    currentRideStatus =
                            "ACCEPTED";

                    Toast.makeText(
                            DriverActivity.this,
                            "Ride accepted.",
                            Toast.LENGTH_SHORT
                    ).show();

                    listenForRideRequests();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                DriverActivity.this,
                                "Unable to accept ride:\n"
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void declineRequestedRide(
            String rideId
    ) {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null
                || rideId == null
                || rideId.isEmpty()) {

            return;
        }

        Map<String, Object> updates =
                new HashMap<>();

        updates.put(
                "status",
                "DECLINED"
        );

        updates.put(
                "declinedBy",
                "DRIVER"
        );

        updates.put(
                "declinedDriverId",
                user.getUid()
        );

        updates.put(
                "declinedAt",
                FieldValue.serverTimestamp()
        );

        db.collection("rides")
                .document(rideId)
                .update(updates)
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            DriverActivity.this,
                            "Ride declined.",
                            Toast.LENGTH_SHORT
                    ).show();

                    listenForRideRequests();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                DriverActivity.this,
                                "Unable to decline ride:\n"
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void updateRideStatus(
            String rideId,
            String status
    ) {

        if (rideId == null
                || rideId.isEmpty()) {

            return;
        }

        Map<String, Object> updates =
                new HashMap<>();

        updates.put(
                "status",
                status
        );

        updates.put(
                "statusUpdatedAt",
                FieldValue.serverTimestamp()
        );

        if (status.equals("DRIVER_ON_THE_WAY")) {

            updates.put(
                    "driverLatitude",
                    driverLatitude
            );

            updates.put(
                    "driverLongitude",
                    driverLongitude
            );

            updates.put(
                    "onTheWayAt",
                    FieldValue.serverTimestamp()
            );

        } else if (status.equals("DRIVER_ARRIVED")) {

            updates.put(
                    "arrivedAt",
                    FieldValue.serverTimestamp()
            );

        } else if (status.equals("IN_PROGRESS")) {

            updates.put(
                    "startedAt",
                    FieldValue.serverTimestamp()
            );

        } else if (status.equals("FINISHED")) {

            updates.put(
                    "finishedAt",
                    FieldValue.serverTimestamp()
            );
        }

        db.collection("rides")
                .document(rideId)
                .update(updates)
                .addOnSuccessListener(unused -> {

                    currentRideStatus =
                            status;

                    Toast.makeText(
                            DriverActivity.this,
                            "Ride status updated.",
                            Toast.LENGTH_SHORT
                    ).show();

                    if (status.equals("FINISHED")) {

                        currentRideId = "";

                        hideRideButtons();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                DriverActivity.this,
                                "Unable to update ride:\n"
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void openLiveMap() {

        if (currentRideId == null
                || currentRideId.isEmpty()) {

            Toast.makeText(
                    this,
                    "No active ride.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Intent intent =
                new Intent(
                        DriverActivity.this,
                        MapActivity.class
                );

        intent.putExtra(
                "ride_id",
                currentRideId
        );

        startActivity(intent);
    }

    private void openChat() {

        if (currentRideId == null
                || currentRideId.isEmpty()) {

            Toast.makeText(
                    this,
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

    private void startLocationUpdates() {

        locationManager =
                (LocationManager)
                        getSystemService(
                                LOCATION_SERVICE
                        );

        locationListener =
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

                    @Override
                    public void onProviderEnabled(
                            String provider
                    ) {
                    }

                    @Override
                    public void onProviderDisabled(
                            String provider
                    ) {
                    }
                };

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
                    1001
            );

            return;
        }

        try {

            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    5000,
                    10,
                    locationListener
            );

            Location lastLocation =
                    locationManager.getLastKnownLocation(
                            LocationManager.GPS_PROVIDER
                    );

            if (lastLocation != null) {

                driverLatitude =
                        lastLocation.getLatitude();

                driverLongitude =
                        lastLocation.getLongitude();

                uploadDriverLocation();
            }

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "GPS unavailable: "
                            + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void uploadDriverLocation() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            return;
        }

        if (driverLatitude == 0.0
                && driverLongitude == 0.0) {

            return;
        }

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "driverId",
                user.getUid()
        );

        data.put(
                "latitude",
                driverLatitude
        );

        data.put(
                "longitude",
                driverLongitude
        );

        data.put(
                "driverOnline",
                driverOnline
        );

        data.put(
                "updatedAt",
                FieldValue.serverTimestamp()
        );

        db.collection("driverLocations")
                .document(user.getUid
