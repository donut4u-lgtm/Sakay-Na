

    

        
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
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DriverActivity extends Activity {

private FirebaseAuth auth;
private FirebaseFirestore db;
private FirebaseUser user;

private LinearLayout requestContainer;
private TextView requestsText;
private TextView statusText;
private TextView currentRideText;

private boolean driverOnline = false;
private String currentRideId = null;

private ListenerRegistration requestListener;
private ListenerRegistration rideListener;

private final Set<String> shownRequestIds = new HashSet<>();

private LocationManager locationManager;
private Handler locationHandler;
private Runnable locationRunnable;

private static final int LOCATION_PERMISSION = 2001;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    auth = FirebaseAuth.getInstance();
    db = FirebaseFirestore.getInstance();
    user = auth.getCurrentUser();

    if (user == null) {
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
    root.setPadding(30, 30, 30, 40);
    root.setBackgroundColor(Color.WHITE);

    TextView title = new TextView(this);
    title.setText("🚕 SAKAY NA\nDRIVER");
    title.setTextSize(28);
    title.setTextColor(Color.BLACK);
    title.setGravity(Gravity.CENTER);
    title.setPadding(10, 20, 10, 20);
    root.addView(title);

    statusText = new TextView(this);
    statusText.setText("Loading driver status...");
    statusText.setTextSize(18);
    statusText.setTextColor(Color.DKGRAY);
    statusText.setGravity(Gravity.CENTER);
    statusText.setPadding(10, 10, 10, 20);
    root.addView(statusText);

    LinearLayout onlineRow = new LinearLayout(this);
    onlineRow.setOrientation(LinearLayout.HORIZONTAL);

    Button onlineButton = new Button(this);
    onlineButton.setText("🟢 GO ONLINE");
    onlineButton.setTextColor(Color.WHITE);
    onlineButton.setBackgroundColor(Color.rgb(0, 150, 0));
    onlineButton.setOnClickListener(v -> setDriverOnline(true));

    Button offlineButton = new Button(this);
    offlineButton.setText("🔴 GO OFFLINE");
    offlineButton.setTextColor(Color.WHITE);
    offlineButton.setBackgroundColor(Color.rgb(200, 0, 0));
    offlineButton.setOnClickListener(v -> setDriverOnline(false));

    onlineRow.addView(
            onlineButton,
            new LinearLayout.LayoutParams(0, 60, 1)
    );

    onlineRow.addView(
            offlineButton,
            new LinearLayout.LayoutParams(0, 60, 1)
    );

    root.addView(onlineRow);

    TextView requestTitle = new TextView(this);
    requestTitle.setText("🔔 RIDE REQUESTS");
    requestTitle.setTextSize(22);
    requestTitle.setTextColor(Color.BLACK);
    requestTitle.setPadding(0, 30, 0, 15);
    root.addView(requestTitle);

    requestsText = new TextView(this);
    requestsText.setText("Checking for new rides...");
    requestsText.setTextSize(17);
    requestsText.setTextColor(Color.DKGRAY);
    requestsText.setPadding(0, 5, 0, 15);
    root.addView(requestsText);

    requestContainer = new LinearLayout(this);
    requestContainer.setOrientation(LinearLayout.VERTICAL);
    root.addView(requestContainer);

    TextView currentTitle = new TextView(this);
    currentTitle.setText("🚦 CURRENT RIDE");
    currentTitle.setTextSize(22);
    currentTitle.setTextColor(Color.BLACK);
    currentTitle.setPadding(0, 35, 0, 15);
    root.addView(currentTitle);

    currentRideText = new TextView(this);
    currentRideText.setText("No current ride.");
    currentRideText.setTextSize(17);
    currentRideText.setTextColor(Color.DKGRAY);
    currentRideText.setPadding(0, 5, 0, 10);
    root.addView(currentRideText);

    Button mapButton = new Button(this);
    mapButton.setText("🗺️ OPEN MAP");
    mapButton.setOnClickListener(v -> {

        if (currentRideId == null || currentRideId.isEmpty()) {
            Toast.makeText(
                    this,
                    "No active ride.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        Intent intent = new Intent(
                DriverActivity.this,
                MapActivity.class
        );

        intent.putExtra("ride_id", currentRideId);
        intent.putExtra("rideId", currentRideId);

        startActivity(intent);
    });

    root.addView(mapButton);

    Button chatButton = new Button(this);
    chatButton.setText("💬 CHAT");
    chatButton.setOnClickListener(v -> {

        if (currentRideId == null || currentRideId.isEmpty()) {
            Toast.makeText(
                    this,
                    "Chat is available after accepting a ride.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        Intent intent = new Intent(
                DriverActivity.this,
                RideChatActivity.class
        );

        intent.putExtra("ride_id", currentRideId);
        intent.putExtra("rideId", currentRideId);

        startActivity(intent);
    });

    root.addView(chatButton);

    Button logoutButton = new Button(this);
    logoutButton.setText("LOGOUT");
    logoutButton.setOnClickListener(v -> logout());
    root.addView(logoutButton);

    scrollView.addView(root);
    setContentView(scrollView);
}

private void loadDriverStatus() {

    db.collection("drivers")
            .document(user.getUid())
            .get()
            .addOnSuccessListener(document -> {

                if (document.exists()
                        && Boolean.TRUE.equals(
                        document.getBoolean("online"))) {

                    driverOnline = true;

                    statusText.setText(
                            "🟢 DRIVER ONLINE"
                    );

                } else {

                    driverOnline = false;

                    statusText.setText(
                            "🔴 DRIVER OFFLINE"
                    );
                }

                listenForRideRequests();
            })
            .addOnFailureListener(e -> {

                driverOnline = false;

                statusText.setText(
                        "🔴 DRIVER OFFLINE"
                );

                listenForRideRequests();
            });
}

private void setDriverOnline(boolean online) {

    driverOnline = online;

    Map<String, Object> data = new HashMap<>();
    data.put("online", online);
    data.put("driverId", user.getUid());
    data.put("updatedAt", System.currentTimeMillis());

    db.collection("drivers")
            .document(user.getUid())
            .set(data)
            .addOnSuccessListener(v -> {

                statusText.setText(
                        online
                                ? "🟢 DRIVER ONLINE"
                                : "🔴 DRIVER OFFLINE"
                );

                if (online) {
                    Toast.makeText(
                            this,
                            "🟢 You are ONLINE. Waiting for ride requests.",
                            Toast.LENGTH_SHORT
                    ).show();
                } else {
                    Toast.makeText(
                            this,
                            "🔴 You are OFFLINE.",
                            Toast.LENGTH_SHORT
                    ).show();
                }

                listenForRideRequests();
            })
            .addOnFailureListener(e -> {

                Toast.makeText(
                        this,
                        "Unable to change driver status.",
                        Toast.LENGTH_SHORT
                ).show();
            });
}

private void listenForRideRequests() {

    if (requestListener != null) {
        requestListener.remove();
        requestListener = null;
    }

    requestListener =
            db.collection("rides")
                    .addSnapshotListener(
                            (snapshots, error) -> {

                                if (error != null) {

                                    requestsText.setText(
                                            "Unable to load ride requests:\n"
                                                    + error.getMessage()
                                    );

                                    return;
                                }

                                requestContainer.removeAllViews();

                                if (snapshots == null
                                        || snapshots.isEmpty()) {

                                    shownRequestIds.clear();

                                    requestsText.setText(
                                            driverOnline
                                                    ? "🟢 No new ride requests."
                                                    : "🔴 OFFLINE"
                                    );

                                    return;
                                }

                                int pendingCount = 0;

                                Set<String> currentRequestIds =
                                        new HashSet<>();

                                for (DocumentSnapshot ride
                                        : snapshots.getDocuments()) {

                                    String rideId = ride.getId();

                                    String status =
                                            ride.getString("status");

                                    if (status == null) {
                                        continue;
                                    }

                                    /*
                                     * ONLY REQUESTED rides are
                                     * available to a driver.
                                     *
                                     * Old ACCEPTED / DECLINED /
                                     * COMPLETED rides are ignored.
                                     */
                                    if (!"REQUESTED".equalsIgnoreCase(
                                            status)) {
                                        continue;
                                    }

                                    String passengerId =
                                            ride.getString("passengerId");

                                    if (passengerId == null
                                            || passengerId.isEmpty()) {
                                        continue;
                                    }

                                    /*
                                     * A request is considered current
                                     * only while it is REQUESTED.
                                     */
                                    currentRequestIds.add(rideId);
                                    pendingCount++;

                                    addRideCard(
                                            ride,
                                            rideId
                                    );

                                    if (!shownRequestIds.contains(
                                            rideId)) {

                                        shownRequestIds.add(rideId);

                                        if (driverOnline) {

                                            Toast.makeText(
                                                    this,
                                                    "🔔 NEW RIDE REQUEST\n"
                                                            + "Tap ACCEPT or DECLINE.",
                                                    Toast.LENGTH_LONG
                                            ).show();
                                        }
                                    }
                                }

                                /*
                                 * Remove IDs that are no longer
                                 * REQUESTED so that a future new
                                 * request can notify again.
                                 */
                                shownRequestIds.retainAll(
                                        currentRequestIds
                                );

                                if (pendingCount == 0) {

                                    requestsText.setText(
                                            driverOnline
                                                    ? "🟢 No new ride requests."
                                                    : "🔴 OFFLINE\nGo ONLINE to accept rides."
                                    );

                                } else if (driverOnline) {

                                    requestsText.setText(
                                            "🟢 NEW RIDE REQUESTS: "
                                                    + pendingCount
                                    );

                                } else {

                                    requestsText.setText(
                                            "🔴 DRIVER OFFLINE\n"
                                                    + pendingCount
                                                    + " ride request(s) waiting."
                                    );
                                }
                            }
                    );
}

private void addRideCard(
        DocumentSnapshot ride,
        String rideId) {

    LinearLayout card = new LinearLayout(this);
    card.setOrientation(LinearLayout.VERTICAL);
    card.setPadding(25, 25, 25, 25);
    card.setBackgroundColor(Color.rgb(245, 245, 245));

    LinearLayout.LayoutParams cardParams =
            new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );

    cardParams.setMargins(0, 10, 0, 20);
    card.setLayoutParams(cardParams);

    TextView title = new TextView(this);
    title.setText("🔔 NEW RIDE REQUEST");
    title.setTextSize(21);
    title.setTextColor(Color.BLACK);
    title.setPadding(0, 0, 0, 15);
    card.addView(title);

    String pickup =
            getStringValue(ride, "pickup");

    String destination =
            getStringValue(ride, "destination");

    String payment =
            getStringValue(ride, "paymentMethod");

    Object fareObject =
            ride.get("fare");

    String fare =
            fareObject == null
                    ? "Not available"
                    : String.valueOf(fareObject);

    TextView details = new TextView(this);

    details.setText(
            "📍 PICKUP\n"
                    + pickup
                    + "\n\n"
                    + "🏁 DESTINATION\n"
                    + destination
                    + "\n\n"
                    + "💰 FARE\n₱"
                    + fare
                    + "\n\n"
                    + "💳 PAYMENT\n"
                    + payment
    );

    details.setTextSize(17);
    details.setTextColor(Color.DKGRAY);
    details.setPadding(0, 0, 0, 20);

    card.addView(details);

    Button acceptButton = new Button(this);
    acceptButton.setText("✅ ACCEPT RIDE");
    acceptButton.setTextColor(Color.WHITE);
    acceptButton.setBackgroundColor(
            Color.rgb(0, 150, 0)
    );

    Button declineButton = new Button(this);
    declineButton.setText("❌ DECLINE");
    declineButton.setTextColor(Color.WHITE);
    declineButton.setBackgroundColor(
            Color.rgb(200, 0, 0)
    );

    if (!driverOnline) {
        acceptButton.setEnabled(false);
    }

    acceptButton.setOnClickListener(v -> {

        if (!driverOnline) {

            Toast.makeText(
                    this,
                    "Go ONLINE first.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        acceptButton.setEnabled(false);
        declineButton.setEnabled(false);

        acceptRide(
                rideId,
                card
        );
    });

    declineButton.setOnClickListener(v -> {

        acceptButton.setEnabled(false);
        declineButton.setEnabled(false);

        declineRide(
                rideId,
                card
        );
    });

    card.addView(acceptButton);
    card.addView(declineButton);

    requestContainer.addView(card);
}

private String getStringValue(
        DocumentSnapshot document,
        String field) {

    String value = document.getString(field);

    if (value == null || value.trim().isEmpty()) {
        return "Not provided";
    }

    return value;
}

private void acceptRide(
        String rideId,
        View card) {

    Location lastLocation = null;

    if (locationManager != null
            && ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED) {

        lastLocation =
                locationManager.getLastKnownLocation(
                        LocationManager.GPS_PROVIDER
                );

        if (lastLocation == null) {

            lastLocation =
                    locationManager.getLastKnownLocation(
                            LocationManager.NETWORK_PROVIDER
                    );
        }
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
            System.currentTimeMillis()
    );

    if (lastLocation != null) {

        update.put(
                "driverLatitude",
                lastLocation.getLatitude()
        );

        update.put(
                "driverLongitude",
                lastLocation.getLongitude()
        );
    }

    db.collection("rides")
            .document(rideId)
            .update(update)
            .addOnSuccessListener(v -> {

                currentRideId = rideId;

                Toast.makeText(
                        this,
                        "✅ Ride accepted.",
                        Toast.LENGTH_SHORT
                ).show();

                listenForCurrentRide();
            })
            .addOnFailureListener(e -> {

                Toast.makeText(
                        this,
                        "Unable to accept ride:\n"
                                + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            });
}

private void declineRide(
        String rideId,
        View card) {

    Map<String, Object> update =
            new HashMap<>();

    update.put(
            "status",
            "DECLINED"
    );

    update.put(
            "declinedBy",
            user.getUid()
    );

    update.put(
            "declinedDriverId",
            user.getUid()
    );

    update.put(
            "declinedAt",
            System.currentTimeMillis()
    );

    db.collection("rides")
            .document(rideId)
            .update(update)
            .addOnSuccessListener(v -> {

                card.setVisibility(View.GONE);

                Toast.makeText(
                        this,
                        "Ride declined.",
                        Toast.LENGTH_SHORT
                ).show();
            })
            .addOnFailureListener(e -> {

                Toast.makeText(
                        this,
                        "Unable to decline ride:\n"
                                + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            });
}

private void listenForCurrentRide() {

    if (rideListener != null) {
        rideListener.remove();
        rideListener = null;
    }

    if (user == null) {
        return;
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
                                            "Unable to load current ride."
                                    );
                                    return;
                                }

                                currentRideId = null;

                                if (snapshots == null
                                        || snapshots.isEmpty()) {

                                    currentRideText.setText(
                                            "No current ride."
                                    );

                                    return;
                                }

                                DocumentSnapshot selected =
                                        null;

                                for (DocumentSnapshot ride
                                        : snapshots.getDocuments()) {

                                    String status =
                                            ride.getString("status");

                                    if ("ACCEPTED".equalsIgnoreCase(
                                            status)
                                            || "ARRIVED".equalsIgnoreCase(
                                            status)
                                            || "ONGOING".equalsIgnoreCase(
                                            status)) {

                                        selected = ride;
                                        break;
                                    }
                                }

                                if (selected == null) {

                                    currentRideText.setText(
                                            "No current ride."
                                    );

                                    return;
                                }

                                currentRideId =
                                        selected.getId();

                                String pickup =
                                        getStringValue(
                                                selected,
                                                "pickup"
                                        );

                                String destination =
                                        getStringValue(
                                                selected,
                                                "destination"
                                        );

                                String status =
                                        getStringValue(
                                                selected,
                                                "status"
                                        );

                                currentRideText.setText(
                                        "🚕 ACTIVE RIDE\n\n"
                                                + "📍 "
                                                + pickup
                                                + "\n\n"
                                                + "🏁 "
                                                + destination
                                                + "\n\n"
                                                + "Status: "
                                                + status
                                );
                            }
                    );
}

private void startLocationUpdates() {

    locationManager =
            (LocationManager) getSystemService(
                    LOCATION_SERVICE
            );

    if (ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
    ) != PackageManager.PERMISSION_GRANTED) {

        ActivityCompat.requestPermissions(
                this,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                LOCATION_PERMISSION
        );

        return;
    }

    beginLocationTracking();
}

private void beginLocationTracking() {

    if (locationManager == null) {
        return;
    }

    try {

        LocationListener listener =
                new LocationListener() {

                    @Override
                    public void onLocationChanged(
                            @NonNull Location location) {

                        updateDriverLocation(
                                location
                        );
                    }
                };

        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000,
                10,
                listener,
                Looper.getMainLooper()
        );

        locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                5000,
                10,
                listener,
                Looper.getMainLooper()
        );

    } catch (SecurityException ignored) {
    }
}

private void updateDriverLocation(
        Location location) {

    Map<String, Object> data =
            new HashMap<>();

    data.put(
            "driverId",
            user.getUid()
    );

    data.put(
            "latitude",
            location.getLatitude()
    );

    data.put(
            "longitude",
            location.getLongitude()
    );

    data.put(
            "updatedAt",
            System.currentTimeMillis()
    );

    db.collection("driverLocations")
            .document(user.getUid())
            .set(data);
}

private void logout() {

    setDriverOnline(false);

    FirebaseAuth.getInstance()
            .signOut();

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

    if (locationManager != null) {
        try {
            locationManager.removeUpdates(
                    (LocationListener) null
            );
        } catch (Exception ignored) {
        }
    }

    super.onDestroy();
}

}
