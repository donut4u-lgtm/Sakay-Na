
// NEXT CODE: DriverActivity.java
// Current main-branch version retrieved from your Sakay-Na repository.
// DO NOT CHANGE MapActivity.java.

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
import android.os.Looper;
import android.view.Gravity;
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
import com.google.firebase.firestore.SetOptions;

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
    private String currentRideId = "";

    private ListenerRegistration requestListener;
    private ListenerRegistration currentRideListener;

    private final Set<String> shownRequestIds = new HashSet<>();

    private LocationManager locationManager;
    private LocationListener locationListener;

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

        ScrollView scroll = new ScrollView(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(25, 25, 25, 35);
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("🛺 SAKAY NA\nDRIVER");
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(Color.BLACK);
        title.setPadding(10, 20, 10, 20);
        root.addView(title);

        statusText = new TextView(this);
        statusText.setText("Loading driver status...");
        statusText.setTextSize(18);
        statusText.setGravity(Gravity.CENTER);
        statusText.setTextColor(Color.DKGRAY);
        statusText.setPadding(10, 10, 10, 20);
        root.addView(statusText);

        LinearLayout onlineRow = new LinearLayout(this);
        onlineRow.setOrientation(LinearLayout.HORIZONTAL);

        Button online = new Button(this);
        online.setText("🟢 GO ONLINE");
        online.setTextColor(Color.WHITE);
        online.setBackgroundColor(Color.rgb(0, 150, 0));
        online.setOnClickListener(v -> setDriverOnline(true));

        Button offline = new Button(this);
        offline.setText("🔴 GO OFFLINE");
        offline.setTextColor(Color.WHITE);
        offline.setBackgroundColor(Color.rgb(200, 0, 0));
        offline.setOnClickListener(v -> setDriverOnline(false));

        onlineRow.addView(
                online,
                new LinearLayout.LayoutParams(0, 65, 1)
        );

        onlineRow.addView(
                offline,
                new LinearLayout.LayoutParams(0, 65, 1)
        );

        root.addView(onlineRow);

        TextView requestTitle = new TextView(this);
        requestTitle.setText("🔔 RIDE REQUESTS");
        requestTitle.setTextSize(22);
        requestTitle.setTextColor(Color.BLACK);
        requestTitle.setPadding(0, 30, 0, 12);
        root.addView(requestTitle);

        requestsText = new TextView(this);
        requestsText.setText("Checking for new rides...");
        requestsText.setTextSize(17);
        requestsText.setTextColor(Color.DKGRAY);
        root.addView(requestsText);

        requestContainer = new LinearLayout(this);
        requestContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(requestContainer);

        TextView currentTitle = new TextView(this);
        currentTitle.setText("🚦 CURRENT RIDE");
        currentTitle.setTextSize(22);
        currentTitle.setTextColor(Color.BLACK);
        currentTitle.setPadding(0, 30, 0, 12);
        root.addView(currentTitle);

        currentRideText = new TextView(this);
        currentRideText.setText("No current ride.");
        currentRideText.setTextSize(17);
        currentRideText.setTextColor(Color.DKGRAY);
        root.addView(currentRideText);

        Button map = new Button(this);
        map.setText("🗺️ OPEN MAP");
        map.setOnClickListener(v -> openMap());
        root.addView(map);

        Button chat = new Button(this);
        chat.setText("💬 CHAT");
        chat.setOnClickListener(v -> openChat());
        root.addView(chat);

        Button logout = new Button(this);
        logout.setText("LOGOUT");
        logout.setOnClickListener(v -> logout());
        root.addView(logout);

        scroll.addView(root);
        setContentView(scroll);
    }

    private void loadDriverStatus() {

        db.collection("drivers")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {

                    driverOnline =
                            doc.exists()
                            && Boolean.TRUE.equals(
                                    doc.getBoolean("online")
                            );

                    updateStatusText();
                    listenForRideRequests();
                })
                .addOnFailureListener(e -> {

                    driverOnline = false;
                    updateStatusText();
                    listenForRideRequests();
                });
    }

    private void setDriverOnline(boolean online) {

        Map<String, Object> data = new HashMap<>();

        data.put("driverId", user.getUid());
        data.put("online", online);
        data.put("updatedAt", System.currentTimeMillis());

        statusText.setText(
                online
                        ? "⏳ GOING ONLINE..."
                        : "⏳ GOING OFFLINE..."
        );

        db.collection("drivers")
                .document(user.getUid())
                .set(data, SetOptions.merge())
                .addOnSuccessListener(v -> {

                    driverOnline = online;
                    updateStatusText();
                    listenForRideRequests();

                    Toast.makeText(
                            this,
                            online
                                    ? "🟢 You are ONLINE."
                                    : "🔴 You are OFFLINE.",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(e -> {

                    driverOnline = false;
                    updateStatusText();

                    Toast.makeText(
                            this,
                            "Unable to change status:\n"
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void updateStatusText() {

        statusText.setText(
                driverOnline
                        ? "🟢 DRIVER ONLINE"
                        : "🔴 DRIVER OFFLINE"
        );
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
                        .addSnapshotListener(
                                (snapshots, error) -> {

                                    if (error != null) {
                                        requestsText.setText(
                                                "🔴 Unable to load requests:\n"
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

                                    int count = 0;

                                    Set<String> currentIds =
                                            new HashSet<>();

                                    for (DocumentSnapshot ride
                                            : snapshots.getDocuments()) {

                                        String rideId =
                                                ride.getId();

                                        String passengerId =
                                                string(
                                                        ride,
                                                        "passengerId"
                                                );

                                        if (passengerId.isEmpty()) {
                                            continue;
                                        }

                                        currentIds.add(rideId);
                                        count++;

                                        addRideCard(
                                                ride,
                                                rideId
                                        );
                                    }

                                    shownRequestIds.retainAll(
                                            currentIds
                                    );

                                    requestsText.setText(
                                            driverOnline
                                                    ? "🟢 NEW RIDE REQUESTS: "
                                                    + count
                                                    : "🔴 OFFLINE\n"
                                                    + count
                                                    + " ride request(s) waiting."
                                    );
                                }
                        );
    }

    private void addRideCard(
            DocumentSnapshot ride,
            String rideId
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

        card.setBackgroundColor(
                Color.rgb(245, 245, 245)
        );

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(
                0,
                10,
                0,
                15
        );

        card.setLayoutParams(cardParams);

        TextView title =
                new TextView(this);

        title.setText(
                "🔔 NEW RIDE REQUEST"
        );

        title.setTextSize(21);
        title.setTextColor(Color.BLACK);

        card.addView(title);

        String pickup =
                placeName(
                        ride,
                        "pickup"
                );

        String destination =
                placeName(
                        ride,
                        "destination"
                );

        String payment =
                string(
                        ride,
                        "paymentMethod"
                );

        Object fareObject =
                ride.get("fare");

        String fare =
                fareObject == null
                        ? "Not available"
                        : String.valueOf(
                                fareObject
                        );

        TextView details =
                new TextView(this);

        details.setText(
                "\n📍 PICKUP\n"
                        + pickup
                        + "\n\n🏁 DESTINATION\n"
                        + destination
                        + "\n\n💰 FARE\n₱"
                        + fare
                        + "\n\n💳 PAYMENT\n"
                        + (
                        payment.isEmpty()
                                ? "Not specified"
                                : payment
                )
        );

        details.setTextSize(17);
        details.setTextColor(Color.DKGRAY);

        card.addView(details);

        Button accept =
                new Button(this);

        accept.setText(
                "✅ ACCEPT RIDE"
        );

        accept.setTextColor(
                Color.WHITE
        );

        accept.setBackgroundColor(
                Color.rgb(0, 150, 0)
        );

        accept.setEnabled(
                driverOnline
        );

        Button decline =
                new Button(this);

        decline.setText(
                "❌ DECLINE"
        );

        decline.setTextColor(
                Color.WHITE
        );

        decline.setBackgroundColor(
                Color.rgb(200, 0, 0)
        );

        accept.setOnClickListener(v -> {

            if (!driverOnline) {

                Toast.makeText(
                        this,
                        "Go ONLINE first.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            accept.setEnabled(false);
            decline.setEnabled(false);

            acceptRide(
                    rideId,
                    card
            );
        });

        decline.setOnClickListener(v -> {

            accept.setEnabled(false);
            decline.setEnabled(false);

            declineRide(
                    rideId,
                    card
            );
        });

        card.addView(accept);
        card.addView(decline);

        requestContainer.addView(card);
    }

    private String placeName(
            DocumentSnapshot ride,
            String type
    ) {

        String name =
                string(
                        ride,
                        type + "Name"
                );

        if (!name.isEmpty()) {
            return name;
        }

        String value =
                string(
                        ride,
                        type
                );

        if (!value.isEmpty()) {
            return value;
        }

        String lat =
                numberText(
                        ride,
                        type + "Latitude"
                );

        String lng =
                numberText(
                        ride,
                        type + "Longitude"
                );

        if (!lat.isEmpty()
                && !lng.isEmpty()) {

            return lat + ", " + lng;
        }

        return "Not provided";
    }

    private String numberText(
            DocumentSnapshot doc,
            String field
    ) {

        Object value =
                doc.get(field);

        return value == null
                ? ""
                : String.valueOf(value);
    }

    private String string(
            DocumentSnapshot doc,
            String field
    ) {

        String value =
                doc.getString(field);

        return value == null
                ? ""
                : value.trim();
    }

    private void acceptRide(
            String rideId,
            LinearLayout card
    ) {

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
                System.currentTimeMillis()
        );

        db.collection("rides")
                .document(rideId)
                .update(data)
                .addOnSuccessListener(v -> {

                    currentRideId =
                            rideId;

                    card.setVisibility(
                            LinearLayout.GONE
                    );

                    Toast.makeText(
                            this,
                            "✅ Ride accepted.",
                            Toast.LENGTH_SHORT
                    ).show();

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
            String rideId,
            LinearLayout card
    ) {

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "status",
                "DECLINED"
        );

        data.put(
                "declinedBy",
                user.getUid()
        );

        data.put(
                "declinedDriverId",
                user.getUid()
        );

        data.put(
                "declinedAt",
                System.currentTimeMillis()
        );

        db.collection("rides")
                .document(rideId)
                .update(data)
                .addOnSuccessListener(v -> {

                    card.setVisibility(
                            LinearLayout.GONE
                    );

                    Toast.makeText(
                            this,
                            "Ride declined.",
                            Toast.LENGTH_SHORT
                    ).show();
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

    private void listenForCurrentRide() {

        if (currentRideListener != null) {
            currentRideListener.remove();
        }

        currentRideListener =
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

                                    currentRideId = "";

                                    if (snapshots == null
                                            || snapshots.isEmpty()) {

                                        currentRideText.setText(
                                                "No current ride."
                                        );

                                        return;
                                    }

                                    for (DocumentSnapshot ride
                                            : snapshots.getDocuments()) {

                                        String rideStatus =
                                                string(
                                                        ride,
                                                        "status"
                                                );

                                        if (!rideStatus.equals(
                                                "ACCEPTED"
                                        )
                                                && !rideStatus.equals(
                                                "ARRIVED"
                                        )
                                                && !rideStatus.equals(
                                                "ONGOING"
                                        )) {

                                            continue;
                                        }

                                        currentRideId =
                                                ride.getId();

                                        currentRideText.setText(
                                                "🚕 ACTIVE RIDE\n\n"
                                                        + "📍 "
                                                        + placeName(
                                                        ride,
                                                        "pickup"
                                                )
                                                        + "\n\n🏁 "
                                                        + placeName(
                                                        ride,
                                                        "destination"
                                                )
                                                        + "\n\nStatus: "
                                                        + rideStatus
                                        );

                                        break;
                                    }
                                }
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
                        this,
                        MapActivity.class
                );

        intent.putExtra(
                "ride_id",
                currentRideId
        );

        intent.putExtra(
                "rideId",
                currentRideId
        );

        intent.putExtra(
                "mode",
                "LIVE_RIDE"
        );

        startActivity(intent);
    }

    private void openChat() {

        if (currentRideId.isEmpty()) {

            Toast.makeText(
                    this,
                    "Accept a ride first.",
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
                currentRideId
        );

        intent.putExtra(
                "rideId",
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

        locationListener =
                new LocationListener() {

                    @Override
                    public void onLocationChanged(
                            @NonNull Location location
                    ) {

                        updateDriverLocation(
                                location
                        );
                    }
                };

        beginLocationTracking();
    }

    private void beginLocationTracking() {

        if (locationManager == null
                || locationListener == null) {

            return;
        }

        try {

            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    5000,
                    10,
                    locationListener,
                    Looper.getMainLooper()
            );

            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    5000,
                    10,
                    locationListener,
                    Looper.getMainLooper()
            );

        } catch (SecurityException ignored) {
        }
    }

    private void updateDriverLocation(
            Location location
    ) {

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
                .set(
                        data,
                        SetOptions.merge()
                );
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] results
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                results
        );

        if (requestCode ==
                LOCATION_PERMISSION
                && results.length > 0
                && results[0] ==
                PackageManager.PERMISSION_GRANTED) {

            beginLocationTracking();
        }
    }

    private void logout() {

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "online",
                false
        );

        data.put(
                "updatedAt",
                System.currentTimeMillis()
        );

        db.collection("drivers")
                .document(user.getUid())
                .set(
                        data,
                        SetOptions.merge()
                )
                .addOnCompleteListener(
                        task -> {

                            auth.signOut();

                            Intent intent =
                                    new Intent(
                                            this,
                                            MainActivity.class
                                    );

                            intent.addFlags(
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                                            |
                                    Intent.FLAG_ACTIVITY_NEW_TASK
                                            |
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK
                            );

                            startActivity(intent);
                            finish();
                        }
                );
    }

    @Override
    protected void onDestroy() {

        if (requestListener != null) {
            requestListener.remove();
            requestListener = null;
        }

        if (currentRideListener != null) {
            currentRideListener.remove();
            currentRideListener = null;
        }

        if (locationManager != null
                && locationListener != null) {

            try {

                locationManager.removeUpdates(
                        locationListener
                );

            } catch (Exception ignored) {
            }
        }

        super.onDestroy();
    }
}
