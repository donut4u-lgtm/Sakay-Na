
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

import java.util.Arrays;
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

    private LocationListener locationListener;

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

        ScrollView scrollView = new ScrollView(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);
        root.setBackgroundColor(Color.WHITE);

        scrollView.addView(root);

        TextView title = new TextView(this);
        title.setText("🛺 SAKAY NA DRIVER");
        title.setTextSize(28);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 20);

        root.addView(
                title,
                new LinearLayout.LayoutParams(
                        -1,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        statusText = new TextView(this);
        statusText.setText("Status: CHECKING...");
        statusText.setTextSize(21);
        statusText.setTextColor(Color.BLACK);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, 10, 0, 20);

        root.addView(statusText);

        LinearLayout statusButtons = new LinearLayout(this);
        statusButtons.setOrientation(LinearLayout.HORIZONTAL);

        onlineButton = new Button(this);
        onlineButton.setText("🟢 GO ONLINE");

        offlineButton = new Button(this);
        offlineButton.setText("⚫ GO OFFLINE");

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

        root.addView(statusButtons);

        onlineButton.setOnClickListener(
                view -> setDriverOnline(true)
        );

        offlineButton.setOnClickListener(
                view -> setDriverOnline(false)
        );

        Button profileButton = new Button(this);
        profileButton.setText("DRIVER PROFILE");

        root.addView(profileButton);

        profileButton.setOnClickListener(
                view -> {
                    Intent intent = new Intent(
                            DriverActivity.this,
                            DriverOnboardingActivity.class
                    );

                    startActivity(intent);
                }
        );

        TextView requestsTitle = new TextView(this);
        requestsTitle.setText("🚦 RIDE REQUESTS");
        requestsTitle.setTextSize(22);
        requestsTitle.setTextColor(Color.BLACK);
        requestsTitle.setPadding(0, 30, 0, 10);

        root.addView(requestsTitle);

        requestsText = new TextView(this);
        requestsText.setText("No ride requests.");
        requestsText.setTextSize(16);
        requestsText.setTextColor(Color.DKGRAY);

        root.addView(requestsText);

        requestContainer = new LinearLayout(this);
        requestContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        root.addView(requestContainer);

        TextView currentTitle = new TextView(this);
        currentTitle.setText("🛺 CURRENT RIDE");
        currentTitle.setTextSize(22);
        currentTitle.setTextColor(Color.BLACK);
        currentTitle.setPadding(0, 30, 0, 10);

        root.addView(currentTitle);

        currentRideText = new TextView(this);
        currentRideText.setText("No active ride.");
        currentRideText.setTextSize(17);
        currentRideText.setTextColor(Color.DKGRAY);

        root.addView(currentRideText);

        acceptButton = createActionButton("✅ ACCEPT RIDE");
        declineButton = createActionButton("❌ DECLINE RIDE");
        onTheWayButton = createActionButton("🚗 DRIVER ON THE WAY");
        arrivedButton = createActionButton("📍 DRIVER ARRIVED");
        startRideButton = createActionButton("🛺 START RIDE");
        finishRideButton = createActionButton("🏁 FINISH RIDE");
        mapButton = createActionButton("🗺️ OPEN LIVE RIDE MAP");
        chatButton = createActionButton("💬 OPEN RIDE CHAT");

        root.addView(acceptButton);
        root.addView(declineButton);
        root.addView(onTheWayButton);
        root.addView(arrivedButton);
        root.addView(startRideButton);
        root.addView(finishRideButton);
        root.addView(mapButton);
        root.addView(chatButton);

        acceptButton.setOnClickListener(
                view -> updateRideStatus("ACCEPTED")
        );

        declineButton.setOnClickListener(
                view -> declineRide()
        );

        onTheWayButton.setOnClickListener(
                view -> updateRideStatus(
                        "DRIVER_ON_THE_WAY"
                )
        );

        arrivedButton.setOnClickListener(
                view -> updateRideStatus(
                        "DRIVER_ARRIVED"
                )
        );

        startRideButton.setOnClickListener(
                view -> updateRideStatus(
                        "IN_PROGRESS"
                )
        );

        finishRideButton.setOnClickListener(
                view -> updateRideStatus(
                        "FINISHED"
                )
        );

        mapButton.setOnClickListener(
                view -> openLiveMap()
        );

        chatButton.setOnClickListener(
                view -> openChat()
        );

        Button logoutButton = new Button(this);
        logoutButton.setText("LOGOUT");

        root.addView(logoutButton);

        logoutButton.setOnClickListener(
                view -> logoutDriver()
        );

        hideRideButtons();

        setContentView(scrollView);
    }

    private Button createActionButton(String text) {

        Button button = new Button(this);

        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(16);

        return button;
    }

    private void hideRideButtons() {

        acceptButton.setVisibility(View.GONE);
        declineButton.setVisibility(View.GONE);
        onTheWayButton.setVisibility(View.GONE);
        arrivedButton.setVisibility(View.GONE);
        startRideButton.setVisibility(View.GONE);
        finishRideButton.setVisibility(View.GONE);
        mapButton.setVisibility(View.GONE);
        chatButton.setVisibility(View.GONE);
    }

    private void loadDriverStatus() {

        FirebaseUser user = auth.getCurrentUser();

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
                )
                .addOnFailureListener(
                        e -> Toast.makeText(
                                this,
                                "Unable to load driver status: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void setDriverOnline(boolean online) {

        FirebaseUser user = auth.getCurrentUser();

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

                            uploadDriverLocation();

                            Toast.makeText(
                                    this,
                                    online
                                            ? "You are ONLINE."
                                            : "You are OFFLINE.",
                                    Toast.LENGTH_SHORT
                            ).show();

                            listenForRideRequests();
                        }
                )
                .addOnFailureListener(
                        e -> {

                            driverOnline = !online;

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
                    Color.rgb(0, 130, 0)
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

    /*
     * IMPORTANT:
     * No orderBy("createdAt") is used here.
     *
     * The driver listens to every ride whose status
     * is REQUESTED.
     */
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
                        .addSnapshotListener(
                                (snapshots, error) -> {

                                    if (error != null) {

                                        requestContainer
                                                .removeAllViews();

                                        requestsText.setText(
                                                "Ride request error:\n"
                                                        + error.getMessage()
                                        );

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
                                            "Available rides: "
                                                    + snapshots.size()
                                    );

                                    for (
                                            DocumentSnapshot ride
                                            : snapshots.getDocuments()
                                    ) {

                                        addRideRequestCard(
                                                ride
                                        );
                                    }
                                }
                        );
    }

    private void addRideRequestCard(
            DocumentSnapshot ride
    ) {

        final String rideId = ride.getId();

        String pickup =
                ride.getString("pickup");

        String destination =
                ride.getString("destination");

        Object fare =
                ride.get("fare");

        String payment =
                ride.getString("paymentMethod");

        TextView rideInfo =
                new TextView(this);

        rideInfo.setText(
                "📍 Pickup: "
                        + safeText(pickup)
                        + "\n\n"
                        + "🏁 Destination: "
                        + safeText(destination)
                        + "\n\n"
                        + "💰 Fare: ₱"
                        + safeText(fare)
                        + "\n\n"
                        + "💳 Payment: "
                        + safeText(payment)
        );

        rideInfo.setTextSize(16);
        rideInfo.setTextColor(Color.BLACK);

        rideInfo.setPadding(
                20,
                20,
                20,
                10
        );

        requestContainer.addView(
                rideInfo
        );

        LinearLayout buttons =
                new LinearLayout(this);

        buttons.setOrientation(
                LinearLayout.HORIZONTAL
        );

        Button accept =
                createActionButton(
                        "✅ ACCEPT"
                );

        Button decline =
                createActionButton(
                        "❌ DECLINE"
                );

        buttons.addView(
                accept,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        buttons.addView(
                decline,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        requestContainer.addView(buttons);

        accept.setOnClickListener(
                view -> acceptRide(rideId)
        );

        decline.setOnClickListener(
                view -> declineRequestedRide(rideId)
        );
    }

    private String safeText(Object value) {

        if (value == null) {
            return "-";
        }

        return String.valueOf(value);
    }

    private void acceptRide(String rideId) {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            return;
        }

        if (!driverOnline) {

            Toast.makeText(
                    this,
                    "You must be ONLINE.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Map<String, Object> update =
                new HashMap<>();

        update.put(
                "driverId",
                user.getUid()
        );

        update.put(
                "status",
                "ACCEPTED"
        );

        update.put(
                "acceptedAt",
                FieldValue.serverTimestamp()
        );

        db.collection("rides")
                .document(rideId)
                .update(update)
                .addOnSuccessListener(
                        unused -> {

                            currentRideId =
                                    rideId;

                            Toast.makeText(
                                    this,
                                    "Ride accepted!",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                )
                .addOnFailureListener(
                        e -> Toast.makeText(
                                this,
                                "Accept failed: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void declineRequestedRide(
            String rideId
    ) {

        Map<String, Object> update =
                new HashMap<>();

        update.put(
                "status",
                "DECLINED"
        );

        update.put(
                "declinedBy",
                "DRIVER"
        );

        update.put(
                "declinedAt",
                FieldValue.serverTimestamp()
        );

        db.collection("rides")
                .document(rideId)
                .update(update)
                .addOnSuccessListener(
                        unused -> Toast.makeText(
                                this,
                                "Ride declined.",
                                Toast.LENGTH_SHORT
                        ).show()
                )
                .addOnFailureListener(
                        e -> Toast.makeText(
                                this,
                                "Decline failed: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void declineRide() {

        if (currentRideId.isEmpty()) {
            return;
        }

        declineRequestedRide(
                currentRideId
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
                                Arrays.asList(
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
                                        currentRideStatus = "";

                                        currentRideText.setText(
                                                "No active ride."
                                        );

                                        hideRideButtons();

                                        return;
                                    }

                                    DocumentSnapshot ride =
                                            snapshots
                                                    .getDocuments()
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

                                    currentRideStatus =
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
                                            "📍 Pickup: "
                                                    + safeText(pickup)
                                                    + "\n\n"
                                                    + "🏁 Destination: "
                                                    + safeText(destination)
                                                    + "\n\n"
                                                    + "💰 Fare: ₱"
                                                    + safeText(fare)
                                                    + "\n\n"
                                                    + "💳 Payment: "
                                                    + safeText(payment)
                                                    + "\n\n"
                                                    + "🚦 Status: "
                                                    + safeText(
                                                            currentRideStatus
                                                    )
                                    );

                                    updateRideButtons();
                                }
                        );
    }

    private void updateRideButtons() {

        hideRideButtons();

        if (currentRideId.isEmpty()) {
            return;
        }

        mapButton.setVisibility(
                View.VISIBLE
        );

        chatButton.setVisibility(
                View.VISIBLE
        );

        if ("ACCEPTED".equals(
                currentRideStatus
        )) {

            onTheWayButton.setVisibility(
                    View.VISIBLE
            );

        } else if ("DRIVER_ON_THE_WAY".equals(
                currentRideStatus
        )) {

            arrivedButton.setVisibility(
                    View.VISIBLE
            );

        } else if ("DRIVER_ARRIVED".equals(
                currentRideStatus
        )) {

            startRideButton.setVisibility(
                    View.VISIBLE
            );

        } else if ("IN_PROGRESS".equals(
                currentRideStatus
        )) {

            finishRideButton.setVisibility(
                    View.VISIBLE
            );

        } else if ("FINISHED".equals(
                currentRideStatus
        )) {

            currentRideText.append(
                    "\n\n🏁 Ride finished."
            );
        }
    }

    private void updateRideStatus(
            String newStatus
    ) {

        if (currentRideId.isEmpty()) {

            Toast.makeText(
                    this,
                    "No active ride.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (!isValidNextStatus(
                currentRideStatus,
                newStatus
        )) {

            Toast.makeText(
                    this,
                    "Invalid ride status change.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Map<String, Object> update =
                new HashMap<>();

        update.put(
                "status",
                newStatus
        );

        if ("DRIVER_ON_THE_WAY".equals(
                newStatus
        )) {

            update.put(
                    "driverOnTheWayAt",
                    FieldValue.serverTimestamp()
            );

        } else if ("DRIVER_ARRIVED".equals(
                newStatus
        )) {

            update.put(
                    "driverArrivedAt",
                    FieldValue.serverTimestamp()
            );

        } else if ("IN_PROGRESS".equals(
                newStatus
        )) {

            update.put(
                    "startedAt",
                    FieldValue.serverTimestamp()
            );

        } else if ("FINISHED".equals(
                newStatus
        )) {

            update.put(
                    "finishedAt",
                    FieldValue.serverTimestamp()
            );
        }

        db.collection("rides")
                .document(currentRideId)
                .update(update)
                .addOnSuccessListener(
                        unused -> Toast.makeText(
                                this,
                                "Ride status: "
                                        + newStatus,
                                Toast.LENGTH_SHORT
                        ).show()
                )
                .addOnFailureListener(
                        e -> Toast.makeText(
                                this,
                                "Status update failed: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private boolean isValidNextStatus(
            String oldStatus,
            String newStatus
    ) {

        if ("ACCEPTED".equals(oldStatus)
                && "DRIVER_ON_THE_WAY".equals(
                newStatus)) {
            return true;
        }

        if ("DRIVER_ON_THE_WAY".equals(oldStatus)
                && "DRIVER_ARRIVED".equals(
                newStatus)) {
            return true;
        }

        if ("DRIVER_ARRIVED".equals(oldStatus)
                && "IN_PROGRESS".equals(
                newStatus)) {
            return true;
        }

        if ("IN_PROGRESS".equals(oldStatus)
                && "FINISHED".equals(
                newStatus)) {
            return true;
        }

        return false;
    }

    private void openLiveMap() {

        if (currentRideId.isEmpty()) {

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

        if (currentRideId.isEmpty()) {

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

        locationListener =
                new LocationListener() {

                    @Override
                    public void onLocationChanged(
                            Location
