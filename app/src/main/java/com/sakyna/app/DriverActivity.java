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
    private TextView requestsText;
    private TextView currentRideText;

    private Button onlineButton;
    private Button offlineButton;
    private Button acceptButton;
    private Button declineButton;
    private Button onTheWayButton;
    private Button arrivedButton;
    private Button startRideButton;
    private Button finishRideButton;
    private Button mapButton;
    private Button chatButton;

    private LinearLayout requestContainer;

    private boolean driverOnline = false;

    private double driverLatitude = 0.0;
    private double driverLongitude = 0.0;

    private String currentRideId = "";
    private String currentRideStatus = "";

    private ListenerRegistration requestListener;
    private ListenerRegistration rideListener;

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
        loadDriverStatus();
        startLocationUpdates();
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
        onlineButton.setText("🟢 GO ONLINE");
        onlineButton.setTextColor(Color.WHITE);
        onlineButton.setBackgroundColor(Color.rgb(0, 150, 0));

        offlineButton = new Button(this);
        offlineButton.setText("🔴 GO OFFLINE");
        offlineButton.setTextColor(Color.WHITE);
        offlineButton.setBackgroundColor(Color.rgb(200, 0, 0));

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
        requestsText.setText("Loading...");
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

        acceptButton = makeButton("ACCEPT RIDE");
        declineButton = makeButton("DECLINE RIDE");
        onTheWayButton = makeButton("I'M ON THE WAY");
        arrivedButton = makeButton("I HAVE ARRIVED");
        startRideButton = makeButton("START RIDE");
        finishRideButton = makeButton("FINISH RIDE");
        mapButton = makeButton("LIVE RIDE MAP");
        chatButton = makeButton("CHAT WITH PASSENGER");

        root.addView(acceptButton);
        root.addView(declineButton);
        root.addView(onTheWayButton);
        root.addView(arrivedButton);
        root.addView(startRideButton);
        root.addView(finishRideButton);
        root.addView(mapButton);
        root.addView(chatButton);

        Button logoutButton = makeButton("LOGOUT");
        root.addView(logoutButton);

        hideRideButtons();

        scrollView.addView(root);
        setContentView(scrollView);

        onlineButton.setOnClickListener(
                v -> setDriverOnline(true)
        );

        offlineButton.setOnClickListener(
                v -> setDriverOnline(false)
        );

        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(
                    DriverActivity.this,
                    DriverOnboardingActivity.class
            );
            startActivity(intent);
        });

        acceptButton.setOnClickListener(v -> {
            if (!driverOnline) {
                Toast.makeText(
                        DriverActivity.this,
                        "Go ONLINE first to accept this ride.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            if (!currentRideId.isEmpty()) {
                acceptRide(currentRideId);
            }
        });

        declineButton.setOnClickListener(v -> {
            if (!currentRideId.isEmpty()) {
                declineRide(currentRideId);
            }
        });

        onTheWayButton.setOnClickListener(v ->
                updateRideStatus("DRIVER_ON_THE_WAY")
        );

        arrivedButton.setOnClickListener(v ->
                updateRideStatus("DRIVER_ARRIVED")
        );

        startRideButton.setOnClickListener(v ->
                updateRideStatus("IN_PROGRESS")
        );

        finishRideButton.setOnClickListener(v ->
                updateRideStatus("FINISHED")
        );

        mapButton.setOnClickListener(
                v -> openMap()
        );

        chatButton.setOnClickListener(
                v -> openChat()
        );

        logoutButton.setOnClickListener(
                v -> logout()
        );
    }

    private Button makeButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        return button;
    }

    private void loadDriverStatus() {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            return;
        }

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {

                    Boolean online =
                            doc.getBoolean("driverOnline");

                    driverOnline =
                            online != null && online;

                    updateOnlineDisplay();
                })
                .addOnFailureListener(e ->
                        statusText.setText(
                                "Unable to load driver status:\n"
                                        + e.getMessage()
                        )
                );
    }

    private void setDriverOnline(boolean online) {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            return;
        }

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
                .addOnSuccessListener(v -> {

                    driverOnline = online;

                    updateOnlineDisplay();

                    listenForRideRequests();

                    uploadDriverLocation();

                    Toast.makeText(
                            this,
                            online
                                    ? "You are now ONLINE."
                                    : "You are now OFFLINE.",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Unable to change status:\n"
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void updateOnlineDisplay() {

        if (driverOnline) {

            statusText.setText(
                    "🟢 ONLINE - ACCEPTING RIDE REQUESTS"
            );

            statusText.setTextColor(
                    Color.rgb(0, 130, 60)
            );

            onlineButton.setText("🟢 ONLINE");
            onlineButton.setTextColor(Color.WHITE);
            onlineButton.setBackgroundColor(
                    Color.rgb(0, 150, 0)
            );

            offlineButton.setText("🔴 GO OFFLINE");
            offlineButton.setTextColor(Color.WHITE);
            offlineButton.setBackgroundColor(
                    Color.rgb(200, 0, 0)
            );

        } else {

            statusText.setText(
                    "🔴 OFFLINE - NOT ACCEPTING RIDES"
            );

            statusText.setTextColor(
                    Color.rgb(180, 0, 0)
            );

            onlineButton.setText("🟢 GO ONLINE");
            onlineButton.setTextColor(Color.WHITE);
            onlineButton.setBackgroundColor(
                    Color.rgb(0, 150, 0)
            );

            offlineButton.setText("🔴 OFFLINE");
            offlineButton.setTextColor(Color.WHITE);
            offlineButton.setBackgroundColor(
                    Color.rgb(200, 0, 0)
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

                                    requestContainer
                                            .removeAllViews();

                                    if (error != null) {

                                        requestsText.setText(
                                                "Unable to load ride requests:\n"
                                                        + error.getMessage()
                                        );

                                        return;
                                    }

                                    if (snapshots == null
                                            || snapshots.isEmpty()) {

                                        requestsText.setText(
                                                driverOnline
                                                        ? "No ride requests."
                                                        : "🔴 OFFLINE\nRide requests are visible below, but go ONLINE to accept."
                                        );

                                        return;
                                    }

                                    if (!driverOnline) {

                                        requestsText.setText(
                                                "🔴 DRIVER OFFLINE\n"
                                                        + "Ride request found.\n"
                                                        + "Go ONLINE to accept."
                                        );

                                    } else {

                                        requestsText.setText(
                                                "🟢 AVAILABLE RIDE REQUESTS"
                                        );
                                    }

                                    for (
                                            DocumentSnapshot ride :
                                            snapshots.getDocuments()
                                    ) {

                                        addRideCard(ride);
                                    }
                                }
                        );
    }

    private void addRideCard(
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

        TextView info =
                new TextView(this);

        info.setTextSize(17);
        info.setTextColor(Color.BLACK);

        String pickup =
                value(ride, "pickup");

        String destination =
                value(ride, "destination");

        String payment =
                value(ride, "paymentMethod");

        Double fareNumber =
                ride.getDouble("fare");

        String fare;

        if (fareNumber != null) {
            fare = String.valueOf(fareNumber);
        } else {
            fare = value(ride, "fare");
        }

        info.setText(
                "PICKUP\n"
                        + (pickup.isEmpty()
                        ? "Not provided"
                        : pickup)
                        + "\n\nDESTINATION\n"
                        + (destination.isEmpty()
                        ? "Not provided"
                        : destination)
                        + "\n\nFARE\n₱"
                        + (fare.isEmpty()
                        ? "0"
                        : fare)
                        + "\n\nPAYMENT\n"
                        + (payment.isEmpty()
                        ? "Not specified"
                        : payment)
        );

        card.addView(info);

        Button accept =
                makeButton("ACCEPT");

        Button decline =
                makeButton("DECLINE");

        if (driverOnline) {
            accept.setEnabled(true);
            accept.setText("ACCEPT");
        } else {
            accept.setEnabled(false);
            accept.setText("🔴 GO ONLINE TO ACCEPT");
        }

        card.addView(accept);
        card.addView(decline);

        requestContainer.addView(card);

        String rideId =
                ride.getId();

        accept.setOnClickListener(v -> {

            if (!driverOnline) {

                Toast.makeText(
                        DriverActivity.this,
                        "Go ONLINE first to accept this ride.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            currentRideId = rideId;

            acceptRide(rideId);
        });

        decline.setOnClickListener(v -> {

            currentRideId = rideId;

            declineRide(rideId);
        });
    }

    private String value(
            DocumentSnapshot doc,
            String field
    ) {

        String result =
                doc.getString(field);

        return result == null
                ? ""
                : result;
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
                        .addSnapshotListener(
                                (snapshots, error) -> {

                                    if (error != null) {

                                        currentRideText.setText(
                                                "Unable to load current ride:\n"
                                                        + error.getMessage()
                                        );

                                        return;
                                    }

                                    DocumentSnapshot active =
                                            null;

                                    if (snapshots != null) {

                                        for (
                                                DocumentSnapshot ride :
                                                snapshots.getDocuments()
                                        ) {

                                            String status =
                                                    ride.getString(
                                                            "status"
                                                    );

                                            if (
                                                    "ACCEPTED"
                                                            .equals(status)
                                                            ||
                                                    "DRIVER_ON_THE_WAY"
                                                            .equals(status)
                                                            ||
                                                    "DRIVER_ARRIVED"
                                                            .equals(status)
                                                            ||
                                                    "IN_PROGRESS"
                                                            .equals(status)
                                            ) {

                                                active = ride;
                                                break;
                                            }
                                        }
                                    }

                                    if (active == null) {

                                        currentRideId = "";
                                        currentRideStatus = "";

                                        currentRideText.setText(
                                                "No current ride."
                                        );

                                        hideRideButtons();

                                        return;
                                    }

                                    currentRideId =
                                            active.getId();

                                    currentRideStatus =
                                            value(
                                                    active,
                                                    "status"
                                            );

                                    String pickup =
                                            value(
                                                    active,
                                                    "pickup"
                                            );

                                    String destination =
                                            value(
                                                    active,
                                                    "destination"
                                            );

                                    currentRideText.setText(
                                            "Status: "
                                                    + currentRideStatus
                                                    + "\n\nPickup:\n"
                                                    + pickup
                                                    + "\n\nDestination:\n"
                                                    + destination
                                    );

                                    showRideButtons();
                                }
                        );
    }

    private void showRideButtons() {

        mapButton.setVisibility(
                View.VISIBLE
        );

        chatButton.setVisibility(
                View.VISIBLE
        );

        onTheWayButton.setVisibility(
                "ACCEPTED".equals(
                        currentRideStatus
                )
                        ? View.VISIBLE
                        : View.GONE
        );

        arrivedButton.setVisibility(
                "DRIVER_ON_THE_WAY"
                        .equals(currentRideStatus)
                        ? View.VISIBLE
                        : View.GONE
        );

        startRideButton.setVisibility(
                "DRIVER_ARRIVED"
                        .equals(currentRideStatus)
                        ? View.VISIBLE
                        : View.GONE
        );

        finishRideButton.setVisibility(
                "IN_PROGRESS"
                        .equals(currentRideStatus)
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

        if (
                user == null
                        || rideId == null
                        || rideId.isEmpty()
        ) {
            return;
        }

        if (!driverOnline) {

            Toast.makeText(
                    this,
                    "Go ONLINE first to accept this ride.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "driverId",
                user.getUid()
        );

        data.put(
                "status",
                "ACCEPTED"
        );

        data.put(
                "acceptedAt",
                FieldValue.serverTimestamp()
        );

        data.put(
                "driverLatitude",
                driverLatitude
        );

        data.put(
                "driverLongitude",
                driverLongitude
        );

        db.collection("rides")
                .document(rideId)
                .update(data)
                .addOnSuccessListener(v -> {

                    currentRideId =
                            rideId;

                    currentRideStatus =
                            "ACCEPTED";

                    requestContainer
                            .removeAllViews();

                    requestsText.setText(
                            "🟢 Ride accepted. No pending ride requests."
                    );

                    Toast.makeText(
                            this,
                            "Ride accepted.",
                            Toast.LENGTH_SHORT
                    ).show();

                    listenForRideRequests();
                    listenForCurrentRide();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Unable to accept ride:\n"
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void declineRide(
            String rideId
    ) {

        FirebaseUser user =
                auth.getCurrentUser();

        if (
                user == null
                        || rideId == null
                        || rideId.isEmpty()
        ) {
            return;
        }

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "status",
                "DECLINED"
        );

        data.put(
                "declinedBy",
                "DRIVER"
        );

        data.put(
                "declinedDriverId",
                user.getUid()
        );

        data.put(
                "declinedAt",
                FieldValue.serverTimestamp()
        );

        db.collection("rides")
                .document(rideId)
                .update(data)
                .addOnSuccessListener(v -> {

                    Toast.makeText(
                            this,
                            "Ride declined.",
                            Toast.LENGTH_SHORT
                    ).show();

                    listenForRideRequests();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Unable to decline ride:\n"
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void updateRideStatus(
            String status
    ) {

        if (currentRideId.isEmpty()) {
            return;
        }

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "status",
                status
        );

        data.put(
                "statusUpdatedAt",
                FieldValue.serverTimestamp()
        );

        if (
                "DRIVER_ON_THE_WAY"
                        .equals(status)
        ) {

            data.put(
                    "driverLatitude",
                    driverLatitude
            );

            data.put(
                    "driverLongitude",
                    driverLongitude
            );

            data.put(
                    "onTheWayAt",
                    FieldValue.serverTimestamp()
            );
        }

        if (
                "DRIVER_ARRIVED"
                        .equals(status)
        ) {

            data.put(
                    "arrivedAt",
                    FieldValue.serverTimestamp()
            );
        }

        if (
                "IN_PROGRESS"
                        .equals(status)
        ) {

            data.put(
                    "startedAt",
                    FieldValue.serverTimestamp()
            );
        }

        if (
                "FINISHED"
                        .equals(status)
        ) {

            data.put(
                    "finishedAt",
                    FieldValue.serverTimestamp()
            );
        }

        db.collection("rides")
                .document(currentRideId)
                .update(data)
                .addOnSuccessListener(v -> {

                    currentRideStatus =
                            status;

                    if (
                            "FINISHED"
                                    .equals(status)
                    ) {

                        currentRideId = "";
                        currentRideStatus = "";

                        currentRideText.setText(
                                "No current ride."
                        );

                        hideRideButtons();
                    }

                    Toast.makeText(
                            this,
                            "Ride status updated.",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Unable to update ride:\n"
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void openMap() {

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

        if (
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                )
                        != PackageManager.PERMISSION_GRANTED
                        &&
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                )
                        != PackageManager.PERMISSION_GRANTED
        ) {

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

            Location last =
                    locationManager.getLastKnownLocation(
                            LocationManager.GPS_PROVIDER
                    );

            if (last != null) {

                driverLatitude =
                        last.getLatitude();

                driverLongitude =
                        last.getLongitude();

                uploadDriverLocation();
            }

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "GPS unavailable:\n"
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

        if (
                driverLatitude == 0.0
                        && driverLongitude == 0.0
        ) {
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
                .document(user.getUid())
                .set(data);
    }

    private void logout() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {

            finishLogout();

            return;
        }

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "driverOnline",
                false
        );

        data.put(
                "driverAvailability",
                "OFFLINE"
        );

        data.put(
                "availabilityUpdatedAt",
                FieldValue.serverTimestamp()
        );

        db.collection("users")
                .document(user.getUid())
                .update(data)
                .addOnCompleteListener(
                        task -> finishLogout()
                );
    }

    private void finishLogout() {

        auth.signOut();

        Intent intent =
                new Intent(
                        DriverActivity.this,
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

    @Override
    protected void onDestroy() {

        if (requestListener != null) {
            requestListener.remove();
            requestListener = null;
        }

        if (rideListener != null) {
            rideListener.remove();
            rideListener = null;
        }

        if (
                locationManager != null
                        && locationListener != null
        ) {

            if (
                    ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    )
                            == PackageManager.PERMISSION_GRANTED
                            ||
                    ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                            == PackageManager.PERMISSION_GRANTED
            ) {

                locationManager.removeUpdates(
                        locationListener
                );
            }
        }

        super.onDestroy();
    }
}
