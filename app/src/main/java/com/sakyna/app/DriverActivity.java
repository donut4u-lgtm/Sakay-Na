package com.sakyna.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
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
import android.widget.Button;
import android.widget.ImageView;
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
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Source;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DriverActivity extends Activity {

    private static final int LOCATION_PERMISSION_REQUEST = 2001;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private LinearLayout rootLayout;
    private TextView statusText;
    private TextView requestsText;
    private TextView currentRideText;
    private TextView duesText;

    private Button onlineButton;
    private Button offlineButton;
    private Button logoutButton;

    private Button acceptButton;
    private Button declineButton;

    private Button onTheWayButton;
    private Button arrivedButton;
    private Button startButton;
    private Button finishButton;
    private Button mapButton;
    private Button chatButton;

    private boolean driverOnline = false;

    private String currentRideId = null;
    private String currentRideStatus = "";

    private ListenerRegistration requestListener;
    private ListenerRegistration currentRideListener;

    private QuerySnapshot latestRequestSnapshot;

    private final Set<String> notifiedRideIds = new HashSet<>();

    private LocationManager locationManager;
    private LocationListener locationListener;

    private final Handler requestHandler =
            new Handler(Looper.getMainLooper());

    private final Runnable requestRefreshRunnable =
            new Runnable() {
                @Override
                public void run() {
                    refreshRideRequestsFromServer();

                    requestHandler.postDelayed(
                            this,
                            30000
                    );
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            finish();
            return;
        }

        buildScreen();
        loadDriverStatus();
        listenForCurrentRide();
        startRequestExpiryChecker();
        requestLocationPermissionAndStart();
    }

    private void buildScreen() {

        rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(20, 20, 20, 20);
        rootLayout.setBackgroundColor(Color.rgb(245, 248, 252));

        ScrollView scrollView = new ScrollView(this);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        ImageView logo = new ImageView(this);
        logo.setImageResource(android.R.drawable.ic_menu_mylocation);
        logo.setColorFilter(Color.rgb(0, 140, 80));

        LinearLayout.LayoutParams logoParams =
                new LinearLayout.LayoutParams(
                        100,
                        100
                );

        logoParams.gravity = Gravity.CENTER_HORIZONTAL;

        content.addView(
                logo,
                logoParams
        );

        TextView title = new TextView(this);
        title.setText("🚖 SAKAY NA — DRIVER");
        title.setTextSize(25);
        title.setTextColor(Color.rgb(0, 100, 70));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 10, 0, 15);

        content.addView(title);

        statusText = new TextView(this);
        statusText.setText("⚪ Checking driver status...");
        statusText.setTextSize(18);
        statusText.setTextColor(Color.DKGRAY);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(10, 15, 10, 15);

        content.addView(statusText);

        LinearLayout onlineRow = new LinearLayout(this);
        onlineRow.setOrientation(LinearLayout.HORIZONTAL);
        onlineRow.setGravity(Gravity.CENTER);

        onlineButton = new Button(this);
        onlineButton.setText("🟢 GO ONLINE");
        onlineButton.setTextColor(Color.WHITE);
        onlineButton.setBackgroundColor(Color.rgb(0, 150, 70));

        offlineButton = new Button(this);
        offlineButton.setText("🔴 GO OFFLINE");
        offlineButton.setTextColor(Color.WHITE);
        offlineButton.setBackgroundColor(Color.rgb(200, 40, 40));

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

        content.addView(onlineRow);

        duesText = new TextView(this);
        duesText.setText("💰 Driver dues: checking...");
        duesText.setTextSize(16);
        duesText.setTextColor(Color.rgb(120, 70, 0));
        duesText.setPadding(10, 15, 10, 15);

        content.addView(duesText);

        requestsText = new TextView(this);
        requestsText.setText("📥 RIDE REQUESTS\nNo new requests.");
        requestsText.setTextSize(18);
        requestsText.setTextColor(Color.rgb(0, 80, 120));
        requestsText.setPadding(10, 20, 10, 20);

        content.addView(requestsText);

        acceptButton = new Button(this);
        acceptButton.setText("✅ ACCEPT RIDE");
        acceptButton.setTextColor(Color.WHITE);
        acceptButton.setBackgroundColor(Color.rgb(0, 150, 70));

        declineButton = new Button(this);
        declineButton.setText("❌ DECLINE RIDE");
        declineButton.setTextColor(Color.WHITE);
        declineButton.setBackgroundColor(Color.rgb(210, 50, 50));

        content.addView(acceptButton);
        content.addView(declineButton);

        currentRideText = new TextView(this);
        currentRideText.setText("🚕 CURRENT RIDE\nNo active ride.");
        currentRideText.setTextSize(18);
        currentRideText.setTextColor(Color.rgb(70, 40, 120));
        currentRideText.setPadding(10, 20, 10, 20);

        content.addView(currentRideText);

        onTheWayButton = new Button(this);
        onTheWayButton.setText("🚗 DRIVER ON THE WAY");

        arrivedButton = new Button(this);
        arrivedButton.setText("📍 DRIVER ARRIVED");

        startButton = new Button(this);
        startButton.setText("▶️ START RIDE");

        finishButton = new Button(this);
        finishButton.setText("🏁 FINISH RIDE");

        mapButton = new Button(this);
        mapButton.setText("🗺️ OPEN LIVE MAP");

        chatButton = new Button(this);
        chatButton.setText("💬 OPEN CHAT");

        content.addView(onTheWayButton);
        content.addView(arrivedButton);
        content.addView(startButton);
        content.addView(finishButton);
        content.addView(mapButton);
        content.addView(chatButton);

        logoutButton = new Button(this);
        logoutButton.setText("🚪 LOGOUT");
        logoutButton.setTextColor(Color.WHITE);
        logoutButton.setBackgroundColor(Color.rgb(90, 90, 90));

        content.addView(logoutButton);

        scrollView.addView(content);
        rootLayout.addView(scrollView);

        setContentView(rootLayout);

        onlineButton.setOnClickListener(
                v -> setDriverOnline(true)
        );

        offlineButton.setOnClickListener(
                v -> setDriverOnline(false)
        );

        acceptButton.setOnClickListener(
                v -> {

                    if (latestRequestSnapshot == null ||
                            latestRequestSnapshot.isEmpty()) {

                        Toast.makeText(
                                this,
                                "No ride request available.",
                                Toast.LENGTH_SHORT
                        ).show();

                        refreshRideRequestsFromServer();
                        return;
                    }

                    DocumentSnapshot newest =
                            findNewestRequest(latestRequestSnapshot);

                    if (newest == null) {
                        Toast.makeText(
                                this,
                                "No active ride request.",
                                Toast.LENGTH_SHORT
                        ).show();

                        refreshRideRequestsFromServer();
                        return;
                    }

                    acceptRide(newest.getId());
                }
        );

        declineButton.setOnClickListener(
                v -> {

                    if (latestRequestSnapshot == null ||
                            latestRequestSnapshot.isEmpty()) {

                        Toast.makeText(
                                this,
                                "No ride request available.",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    DocumentSnapshot newest =
                            findNewestRequest(latestRequestSnapshot);

                    if (newest != null) {
                        declineRide(newest.getId());
                    }
                }
        );

        onTheWayButton.setOnClickListener(
                v -> updateRideStatus("DRIVER_ON_THE_WAY")
        );

        arrivedButton.setOnClickListener(
                v -> updateRideStatus("DRIVER_ARRIVED")
        );

        startButton.setOnClickListener(
                v -> updateRideStatus("IN_PROGRESS")
        );

        finishButton.setOnClickListener(
                v -> updateRideStatus("COMPLETED")
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

        updateOnlineButtons();
        clearRideStatusButtons();
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

                    if (!doc.exists()) {
                        statusText.setText(
                                "🔴 Driver profile not found."
                        );
                        return;
                    }

                    String status =
                            string(doc.getString("driverStatus"));

                    String approval =
                            string(doc.getString("approvalStatus"));

                    if (!approval.isEmpty()) {
                        status = approval;
                    }

                    if ("APPROVED".equalsIgnoreCase(status) ||
                            "ACTIVE".equalsIgnoreCase(status)) {

                        statusText.setText(
                                "🟢 Driver account APPROVED"
                        );

                    } else if ("PENDING".equalsIgnoreCase(status)) {

                        statusText.setText(
                                "🟡 Driver approval is PENDING"
                        );

                    } else if ("REJECTED".equalsIgnoreCase(status)) {

                        statusText.setText(
                                "🔴 Driver application REJECTED"
                        );

                    } else {

                        statusText.setText(
                                "🟡 Driver status: " + status
                        );
                    }

                    checkUnpaidDues();
                })
                .addOnFailureListener(e -> {

                    statusText.setText(
                            "🔴 Unable to load driver status."
                    );

                    checkUnpaidDues();
                });

        loadOnlineStatus();
    }

    private void checkUnpaidDues() {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            return;
        }

        db.collection("driverSettlements")
                .whereEqualTo(
                        "driverId",
                        user.getUid()
                )
                .whereEqualTo(
                        "status",
                        "PENDING"
                )
                .get()
                .addOnSuccessListener(snapshot -> {

                    double total = 0;

                    for (DocumentSnapshot doc :
                            snapshot.getDocuments()) {

                        total += getDriverDue(doc);
                    }

                    total = roundMoney(total);

                    if (total > 0) {

                        duesText.setText(
                                "💰 UNPAID DRIVER DUES: ₱"
                                        + formatFare(total)
                                        + "\nPlease settle your dues."
                        );

                        duesText.setTextColor(
                                Color.rgb(190, 70, 0)
                        );

                    } else {

                        duesText.setText(
                                "💚 Driver dues: ₱0.00"
                        );

                        duesText.setTextColor(
                                Color.rgb(0, 130, 70)
                        );
                    }
                })
                .addOnFailureListener(e -> {

                    duesText.setText(
                            "⚠️ Driver dues unavailable."
                    );
                });
    }

    private double getDriverDue(
            DocumentSnapshot doc
    ) {

        Object value = doc.get("amount");

        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        if (value instanceof String) {

            try {
                return Double.parseDouble(
                        ((String) value).trim()
                );
            } catch (Exception ignored) {
            }
        }

        Object due = doc.get("driverDue");

        if (due instanceof Number) {
            return ((Number) due).doubleValue();
        }

        if (due instanceof String) {

            try {
                return Double.parseDouble(
                        ((String) due).trim()
                );
            } catch (Exception ignored) {
            }
        }

        return 0;
    }

    private double getFare(
            DocumentSnapshot ride
    ) {

        Object fare = ride.get("fare");

        if (fare instanceof Number) {
            return ((Number) fare).doubleValue();
        }

        if (fare instanceof String) {

            try {
                return Double.parseDouble(
                        ((String) fare).trim()
                );
            } catch (Exception ignored) {
            }
        }

        Object totalFare = ride.get("totalFare");

        if (totalFare instanceof Number) {
            return ((Number) totalFare).doubleValue();
        }

        if (totalFare instanceof String) {

            try {
                return Double.parseDouble(
                        ((String) totalFare).trim()
                );
            } catch (Exception ignored) {
            }
        }

        return 0;
    }

    private double roundMoney(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private void loadOnlineStatus() {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            return;
        }

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {

                    Object online =
                            doc.get("driverOnline");

                    if (online instanceof Boolean) {
                        driverOnline =
                                (Boolean) online;
                    } else {
                        driverOnline = false;
                    }

                    updateStatusText();
                    updateOnlineButtons();

                    listenForRideRequests();

                    if (driverOnline) {
                        refreshRideRequestsFromServer();
                    }
                })
                .addOnFailureListener(e -> {

                    driverOnline = false;

                    updateStatusText();
                    updateOnlineButtons();

                    listenForRideRequests();
                });
    }

    private Map<String, Object> buildOfflineData() {

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
                System.currentTimeMillis()
        );

        return data;
    }

    private void updateOnlineButtons() {

        if (onlineButton == null ||
                offlineButton == null) {
            return;
        }

        if (driverOnline) {

            onlineButton.setEnabled(false);
            offlineButton.setEnabled(true);

            onlineButton.setBackgroundColor(
                    Color.rgb(80, 150, 100)
            );

            offlineButton.setBackgroundColor(
                    Color.rgb(210, 40, 40)
            );

        } else {

            onlineButton.setEnabled(true);
            offlineButton.setEnabled(false);

            onlineButton.setBackgroundColor(
                    Color.rgb(0, 160, 70)
            );

            offlineButton.setBackgroundColor(
                    Color.rgb(150, 100, 100)
            );
        }
    }

    private void setDriverOnline(
            boolean online
    ) {

        FirebaseUser user = auth.getCurrentUser();

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
                System.currentTimeMillis()
        );

        db.collection("users")
                .document(user.getUid())
                .set(
                        data,
                        SetOptions.merge()
                )
                .addOnSuccessListener(v -> {

                    driverOnline = online;

                    updateStatusText();
                    updateOnlineButtons();
                    listenForRideRequests();

                    if (online) {
                        refreshRideRequestsFromServer();
                    }

                    Toast.makeText(
                            this,
                            online
                                    ? "🟢 You are ONLINE."
                                    : "🔴 You are OFFLINE.",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            "Unable to change online status:\n"
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void updateStatusText() {

        if (statusText == null) {
            return;
        }

        if (driverOnline) {

            statusText.setText(
                    "🟢 DRIVER ONLINE — Waiting for rides"
            );

            statusText.setTextColor(
                    Color.rgb(0, 140, 70)
            );

        } else {

            statusText.setText(
                    "🔴 DRIVER OFFLINE"
            );

            statusText.setTextColor(
                    Color.rgb(190, 40, 40)
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
                                                "🔴 Unable to load requests:\n"
                                                        + error.getMessage()
                                        );

                                        return;
                                    }

                                    latestRequestSnapshot =
                                            snapshots;

                                    notifyNewRideRequests(
                                            snapshots
                                    );

                                    renderRideRequests(
                                            snapshots
                                    );
                                }
                        );
    }

    /**
     * Server-authoritative refresh.
     *
     * This prevents the driver screen from depending
     * only on a cached Firestore snapshot.
     */
    private void refreshRideRequestsFromServer() {

        if (!driverOnline) {
            return;
        }

        db.collection("rides")
                .whereEqualTo(
                        "status",
                        "REQUESTED"
                )
                .get(Source.SERVER)
                .addOnSuccessListener(
                        snapshots -> {

                            latestRequestSnapshot =
                                    snapshots;

                            notifyNewRideRequests(
                                    snapshots
                            );

                            renderRideRequests(
                                    snapshots
                            );
                        }
                )
                .addOnFailureListener(
                        e -> {

                            if (latestRequestSnapshot != null) {

                                renderRideRequests(
                                        latestRequestSnapshot
                                );
                            }
                        }
                );
    }

    private void notifyNewRideRequests(
            QuerySnapshot snapshots
    ) {

        if (snapshots == null ||
                snapshots.isEmpty()) {
            return;
        }

        for (DocumentSnapshot ride :
                snapshots.getDocuments()) {

            String rideId = ride.getId();

            if (!notifiedRideIds.contains(rideId)) {

                notifiedRideIds.add(rideId);

                try {

                    SakayNaNotificationHelper
                            .showNotification(
                                    this,
                                    "🚖 New Sakay Na Ride",
                                    "A passenger is requesting a ride."
                            );

                } catch (Exception ignored) {
                }
            }
        }
    }

    private void startRequestExpiryChecker() {

        requestHandler.removeCallbacks(
                requestRefreshRunnable
        );

        requestHandler.post(
                requestRefreshRunnable
        );
    }

    private DocumentSnapshot findNewestRequest(
            QuerySnapshot snapshots
    ) {

        if (snapshots == null ||
                snapshots.isEmpty()) {
            return null;
        }

        DocumentSnapshot newest = null;
        long newestTime = Long.MIN_VALUE;

        for (DocumentSnapshot ride :
                snapshots.getDocuments()) {

            String status =
                    safeStatus(
                            ride.getString("status")
                    );

            if (!"REQUESTED".equals(status)) {
                continue;
            }

            long created =
                    longValue(
                            ride,
                            "createdAt"
                    );

            if (newest == null ||
                    created > newestTime) {

                newest = ride;
                newestTime = created;
            }
        }

        return newest;
    }

    private void renderRideRequests(
            QuerySnapshot snapshots
    ) {

        if (requestsText == null) {
            return;
        }

        if (!driverOnline) {

            requestsText.setText(
                    "📥 RIDE REQUESTS\n"
                            + "You are OFFLINE."
            );

            return;
        }

        if (snapshots == null ||
                snapshots.isEmpty()) {

            requestsText.setText(
                    "📥 RIDE REQUESTS\n"
                            + "No new requests."
            );

            return;
        }

        DocumentSnapshot newest =
                findNewestRequest(snapshots);

        if (newest == null) {

            requestsText.setText(
                    "📥 RIDE REQUESTS\n"
                            + "No new requests."
            );

            return;
        }

        StringBuilder text =
                new StringBuilder();

        text.append("📥 NEW RIDE REQUEST\n\n");

        String pickup =
                placeName(
                        newest,
                        "pickupName",
                        "pickup"
                );

        String destination =
                placeName(
                        newest,
                        "destinationName",
                        "destination"
                );

        String passenger =
                passengerNameFromRide(
                        newest
                );

        double fare =
                getFare(newest);

        String payment =
                string(
                        newest.getString(
                                "paymentMethod"
                        )
                );

        int passengers =
                passengerCountText(
                        newest
                );

        text.append("👤 Passenger: ")
                .append(passenger)
                .append("\n");

        text.append("👥 Passengers: ")
                .append(passengers)
                .append("\n\n");

        text.append("📍 Pickup:\n")
                .append(pickup)
                .append("\n\n");

        text.append("🏁 Destination:\n")
                .append(destination)
                .append("\n\n");

        text.append("💰 Fare: ₱")
                .append(formatFare(fare))
                .append("\n");

        text.append("💳 Payment: ")
                .append(
                        payment.isEmpty()
                                ? "CASH"
                                : payment
                )
                .append("\n\n");

        text.append(
                "Tap ACCEPT to take this ride."
        );

        requestsText.setText(
                text.toString()
        );
    }

    private void addRideCard(
            DocumentSnapshot ride
    ) {

        // Requests are rendered in requestsText.
        // This method remains available for compatibility
        // with the existing DriverActivity flow.
    }

    private void loadPassengerName(
            String passengerId
    ) {

        if (passengerId == null ||
                passengerId.trim().isEmpty()) {
            return;
        }

        db.collection("users")
                .document(passengerId)
                .get()
                .addOnSuccessListener(
                        doc -> {

                            String name =
                                    passengerNameFromProfile(
                                            doc
                                    );

                            if (!name.isEmpty() &&
                                    currentRideText != null) {

                                currentRideText.setText(
                                        currentRideText
                                                .getText()
                                                .toString()
                                                + "\n👤 Passenger: "
                                                + name
                                );
                            }
                        }
                );
    }

    private String passengerNameFromRide(
            DocumentSnapshot ride
    ) {

        String name =
                string(
                        ride.getString(
                                "passengerName"
                        )
                );

        if (!name.isEmpty()) {
            return name;
        }

        name =
                string(
                        ride.getString(
                                "name"
                        )
                );

        if (!name.isEmpty()) {
            return name;
        }

        String passengerId =
                string(
                        ride.getString(
                                "passengerId"
                        )
                );

        if (!passengerId.isEmpty()) {

            DocumentSnapshot cached =
                    null;
        }

        return "Passenger";
    }

    private String passengerNameFromProfile(
            DocumentSnapshot doc
    ) {

        if (doc == null ||
                !doc.exists()) {
            return "";
        }

        String name =
                string(
                        doc.getString("name")
                );

        if (!name.isEmpty()) {
            return name;
        }

        name =
                string(
                        doc.getString("fullName")
                );

        if (!name.isEmpty()) {
            return name;
        }

        name =
                string(
                        doc.getString("displayName")
                );

        return name;
    }

    private void acceptRide(
            String rideId
    ) {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null ||
                rideId == null ||
                rideId.trim().isEmpty()) {
            return;
        }

        Map<String, Object> update =
                buildDriverRideUpdate(
                        user.getUid(),
                        "ACCEPTED"
                );

        db.collection("rides")
                .document(rideId)
                .update(update)
                .addOnSuccessListener(v -> {

                    currentRideId = rideId;
                    currentRideStatus = "ACCEPTED";

                    Toast.makeText(
                            this,
                            "✅ Ride accepted!",
                            Toast.LENGTH_SHORT
                    ).show();

                    listenForCurrentRide();

                    refreshRideRequestsFromServer();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            "Unable to accept ride:\n"
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();

                    refreshRideRequestsFromServer();
                });
    }

    private Map<String, Object> buildDriverRideUpdate(
            String driverId,
            String status
    ) {

        Map<String, Object> update =
                new HashMap<>();

        update.put(
                "driverId",
                driverId
        );

        update.put(
                "status",
                status
        );

        update.put(
                "acceptedAt",
                System.currentTimeMillis()
        );

        return update;
    }

    private void declineRide(
            String rideId
    ) {

        if (rideId == null ||
                rideId.trim().isEmpty()) {
            return;
        }

        Map<String, Object> update =
                new HashMap<>();

        update.put(
                "status",
                "REQUESTED"
        );

        update.put(
                "declinedBy",
                auth.getCurrentUser() != null
                        ? auth.getCurrentUser().getUid()
                        : ""
        );

        update.put(
                "declinedAt",
                System.currentTimeMillis()
        );

        db.collection("rides")
                .document(rideId)
                .update(update)
                .addOnSuccessListener(v -> {

                    Toast.makeText(
                            this,
                            "Ride declined.",
                            Toast.LENGTH_SHORT
                    ).show();

                    refreshRideRequestsFromServer();
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

        if (currentRideListener != null) {

            currentRideListener.remove();
            currentRideListener = null;
        }

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            return;
        }

        if (currentRideId != null &&
                !currentRideId.trim().isEmpty()) {

            currentRideListener =
                    db.collection("rides")
                            .document(currentRideId)
                            .addSnapshotListener(
                                    (doc, error) -> {

                                        if (error != null ||
                                                doc == null ||
                                                !doc.exists()) {

                                            return;
                                        }

                                        showCurrentRide(
                                                doc
                                        );
                                    }
                            );

            return;
        }

        currentRideListener =
                db.collection("rides")
                        .whereEqualTo(
                                "driverId",
                                user.getUid()
                        )
                        .addSnapshotListener(
                                (snapshots, error) -> {

                                    if (error != null ||
                                            snapshots == null) {
                                        return;
                                    }

                                    DocumentSnapshot newest =
                                            findNewestDriverRide(
                                                    snapshots
                                            );

                                    if (newest == null) {

                                        currentRideId = null;
                                        currentRideStatus = "";

                                        currentRideText.setText(
                                                "🚕 CURRENT RIDE\n"
                                                        + "No active ride."
                                        );

                                        clearRideStatusButtons();

                                        return;
                                    }

                                    currentRideId =
                                            newest.getId();

                                    showCurrentRide(
                                            newest
                                    );
                                }
                        );
    }

    private DocumentSnapshot findNewestDriverRide(
            QuerySnapshot snapshots
    ) {

        if (snapshots == null ||
                snapshots.isEmpty()) {
            return null;
        }

        DocumentSnapshot newest = null;
        long newestTime = Long.MIN_VALUE;

        for (DocumentSnapshot ride :
                snapshots.getDocuments()) {

            String status =
                    safeStatus(
                            ride.getString("status")
                    );

            if (!isActive(status)) {
                continue;
            }

            long created =
                    longValue(
                            ride,
                            "createdAt"
                    );

            if (newest == null ||
                    created > newestTime) {

                newest = ride;
                newestTime = created;
            }
        }

        return newest;
    }

    private void showCurrentRide(
            DocumentSnapshot ride
    ) {

        if (ride == null ||
                !ride.exists()) {
            return;
        }

        currentRideId =
                ride.getId();

        currentRideStatus =
                safeStatus(
                        ride.getString("status")
                );

        String pickup =
                placeName(
                        ride,
                        "pickupName",
                        "pickup"
                );

        String destination =
                placeName(
                        ride,
                        "destinationName",
                        "destination"
                );

        double fare =
                getFare(ride);

        String payment =
                string(
                        ride.getString(
                                "paymentMethod"
                        )
                );

        int passengers =
                passengerCountText(
                        ride
                );

        String passenger =
                passengerNameFromRide(
                        ride
                );

        StringBuilder text =
                new StringBuilder();

        text.append("🚕 CURRENT RIDE\n\n");

        text.append("👤 Passenger: ")
                .append(passenger)
                .append("\n");

        text.append("👥 Passengers: ")
                .append(passengers)
                .append("\n\n");

        text.append("📍 Pickup:\n")
                .append(pickup)
                .append("\n\n");

        text.append("🏁 Destination:\n")
                .append(destination)
                .append("\n\n");

        text.append("💰 Fare: ₱")
                .append(formatFare(fare))
                .append("\n");

        text.append("💳 Payment: ")
                .append(
                        payment.isEmpty()
                                ? "CASH"
                                : payment
                )
                .append("\n\n");

        text.append("📊 Status: ")
                .append(currentRideStatus);

        currentRideText.setText(
                text.toString()
        );

        showRideStatusButtons(
                currentRideStatus
        );

        if (!passenger.isEmpty() &&
                "Passenger".equals(passenger)) {

            loadCurrentRidePassengerName(
                    ride
            );
        }

        if (!isActive(currentRideStatus)) {

            if ("COMPLETED".equals(
                    currentRideStatus
            ) ||
                    "CANCELLED".equals(
                            currentRideStatus
                    )) {

                currentRideId = null;
            }
        }
    }

    private void loadCurrentRidePassengerName(
            DocumentSnapshot ride
    ) {

        String passengerId =
                string(
                        ride.getString(
                                "passengerId"
                        )
                );

        if (passengerId.isEmpty()) {
            return;
        }

        db.collection("users")
                .document(passengerId)
                .get()
                .addOnSuccessListener(
                        doc -> {

                            String name =
                                    passengerNameFromProfile(
                                            doc
                                    );

                            if (name.isEmpty()) {
                                return;
                            }

                            String current =
                                    currentRideText
                                            .getText()
                                            .toString();

                            currentRideText.setText(
                                    current.replace(
                                            "👤 Passenger: Passenger",
                                            "👤 Passenger: "
                                                    + name
                                    )
                            );
                        }
                );
    }

    private int passengerCountText(
            DocumentSnapshot ride
    ) {

        Object value =
                ride.get("passengerCount");

        if (value instanceof Number) {

            return Math.max(
                    1,
                    Math.min(
                            4,
                            ((Number) value)
                                    .intValue()
                    )
            );
        }

        if (value instanceof String) {

            try {

                return Math.max(
                        1,
                        Math.min(
                                4,
                                Integer.parseInt(
                                        ((String) value)
                                                .trim()
                                )
                        )
                );

            } catch (Exception ignored) {
            }
        }

        return 1;
    }

    private void showRideStatusButtons(
            String status
    ) {

        clearRideStatusButtons();

        if (status == null) {
            return;
        }

        switch (status) {

            case "ACCEPTED":

                onTheWayButton.setVisibility(
                        Button.VISIBLE
                );

                mapButton.setVisibility(
                        Button.VISIBLE
                );

                chatButton.setVisibility(
                        Button.VISIBLE
                );

                break;

            case "DRIVER_ON_THE_WAY":

                arrivedButton.setVisibility(
                        Button.VISIBLE
                );

                mapButton.setVisibility(
                        Button.VISIBLE
                );

                chatButton.setVisibility(
                        Button.VISIBLE
                );

                break;

            case "DRIVER_ARRIVED":

                startButton.setVisibility(
                        Button.VISIBLE
                );

                mapButton.setVisibility(
                        Button.VISIBLE
                );

                chatButton.setVisibility(
                        Button.VISIBLE
                );

                break;

            case "IN_PROGRESS":

                finishButton.setVisibility(
                        Button.VISIBLE
                );

                mapButton.setVisibility(
                        Button.VISIBLE
                );

                chatButton.setVisibility(
                        Button.VISIBLE
                );

                break;

            default:
                break;
        }
    }

    private void clearRideStatusButtons() {

        if (onTheWayButton != null) {
            onTheWayButton.setVisibility(
                    Button.GONE
            );
        }

        if (arrivedButton != null) {
            arrivedButton.setVisibility(
                    Button.GONE
            );
        }

        if (startButton != null) {
            startButton.setVisibility(
                    Button.GONE
            );
        }

        if (finishButton != null) {
            finishButton.setVisibility(
                    Button.GONE
            );
        }

        if (mapButton != null) {
            mapButton.setVisibility(
                    Button.GONE
            );
        }

        if (chatButton != null) {
            chatButton.setVisibility(
                    Button.GONE
            );
        }
    }

    private void updateRideStatus(
            String newStatus
    ) {

        if (currentRideId == null ||
                currentRideId.trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "No active ride.",
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

        update.put(
                "statusUpdatedAt",
                System.currentTimeMillis()
        );

        db.collection("rides")
                .document(currentRideId)
                .update(update)
                .addOnSuccessListener(v -> {

                    currentRideStatus =
                            newStatus;

                    Toast.makeText(
                            this,
                            statusMessage(
                                    newStatus
                            ),
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            "Unable to update ride:\n"
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private String statusMessage(
            String status
    ) {

        switch (status) {

            case "DRIVER_ON_THE_WAY":
                return "🚗 Passenger notified: Driver is on the way.";

            case "DRIVER_ARRIVED":
                return "📍 Passenger notified: Driver has arrived.";

            case "IN_PROGRESS":
                return "▶️ Ride started.";

            case "COMPLETED":
                return "🏁 Ride completed.";

            default:
                return "Ride status updated.";
        }
    }

    private boolean isActive(
            String status
    ) {

        if (status == null) {
            return false;
        }

        return "ACCEPTED".equals(status)
                || "DRIVER_ON_THE_WAY".equals(status)
                || "DRIVER_ARRIVED".equals(status)
                || "IN_PROGRESS".equals(status);
    }

    private long longValue(
            DocumentSnapshot doc,
            String field
    ) {

        Object value =
                doc.get(field);

        if (value instanceof Number) {

            return ((Number) value)
                    .longValue();
        }

        if (value instanceof String) {

            try {

                return Long.parseLong(
                        ((String) value).trim()
                );

            } catch (Exception ignored) {
            }
        }

        if (value instanceof com.google.firebase.Timestamp) {

            return ((com.google.firebase.Timestamp) value)
                    .toDate()
                    .getTime();
        }

        if (value instanceof java.util.Date) {

            return ((java.util.Date) value)
                    .getTime();
        }

        return 0;
    }

    private String placeName(
            DocumentSnapshot doc,
            String nameField,
            String fallbackField
    ) {

        String name =
                string(
                        doc.getString(nameField)
                );

        if (!name.isEmpty()) {
            return name;
        }

        name =
                string(
                        doc.getString(fallbackField)
                );

        if (!name.isEmpty()) {
            return name;
        }

        Object lat =
                doc.get("pickupLatitude");

        Object lng =
                doc.get("pickupLongitude");

        if (fallbackField.equals(
                "destination"
        )) {

            lat =
                    doc.get(
                            "destinationLatitude"
                    );

            lng =
                    doc.get(
                            "destinationLongitude"
                    );
        }

        if (lat != null &&
                lng != null) {

            return numberText(lat)
                    + ", "
                    + numberText(lng);
        }

        return "Location not available";
    }

    private String formatFare(
            double amount
    ) {

        return String.format(
                java.util.Locale.US,
                "%.2f",
                amount
        );
    }

    private String numberText(
            Object value
    ) {

        if (value instanceof Number) {

            return String.format(
                    java.util.Locale.US,
                    "%.6f",
                    ((Number) value)
                            .doubleValue()
            );
        }

        return String.valueOf(value);
    }

    private String string(
            String value
    ) {

        return value == null
                ? ""
                : value.trim();
    }

    private String safeStatus(
            String status
    ) {

        return status == null
                ? ""
                : status.trim().toUpperCase();
    }

    private void showGcashQr() {

        try {

            Intent intent =
                    new Intent(
                            this,
                            DriverSettlementActivity.class
                    );

            startActivity(intent);

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Settlement screen unavailable.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void openMap() {

        if (currentRideId == null ||
                currentRideId.trim().isEmpty()) {

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
                "mode",
                "LIVE_RIDE"
        );

        startActivity(intent);
    }

    private void openChat() {

        if (currentRideId == null ||
                currentRideId.trim().isEmpty()) {

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
                "rideId",
                currentRideId
        );

        startActivity(intent);
    }

    private void requestLocationPermissionAndStart() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
                &&
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) {

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

                        updateDriverLocation(
                                location
                        );
                    }
                };

        try {

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
                    &&
                    ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED) {

                return;
            }

            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    5000,
                    5,
                    locationListener
            );

            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    5000,
                    5,
                    locationListener
            );

        } catch (Exception ignored) {
        }
    }

    private void updateDriverLocation(
            Location location
    ) {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null ||
                location == null) {
            return;
        }

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "driverLatitude",
                location.getLatitude()
        );

        data.put(
                "driverLongitude",
                location.getLongitude()
        );

        data.put(
                "driverLocationUpdatedAt",
                System.currentTimeMillis()
        );

        db.collection("users")
                .document(user.getUid())
                .set(
                        data,
                        SetOptions.merge()
                );

        if (currentRideId != null &&
                isActive(currentRideStatus)) {

            Map<String, Object> rideData =
                    new HashMap<>();

            rideData.put(
                    "driverLatitude",
                    location.getLatitude()
            );

            rideData.put(
                    "driverLongitude",
                    location.getLongitude()
            );

            rideData.put(
                    "driverLocationUpdatedAt",
                    System.currentTimeMillis()
            );

            db.collection("rides")
                    .document(currentRideId)
                    .set(
                            rideData,
                            SetOptions.merge()
                    );
        }
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

        if (requestCode ==
                LOCATION_PERMISSION_REQUEST) {

            if (grantResults.length > 0 &&
                    grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED) {

                startLocationUpdates();

            } else {

                Toast.makeText(
                        this,
                        "Location permission is needed for driver tracking.",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    private void logout() {

        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage(
                        "Are you sure you want to logout?"
                )
                .setNegativeButton(
                        "CANCEL",
                        null
                )
                .setPositiveButton(
                        "LOGOUT",
                        (dialog, which) -> {

                            FirebaseUser user =
                                    auth.getCurrentUser();

                            if (user != null) {

                                db.collection("users")
                                        .document(user.getUid())
                                        .set(
                                                buildOfflineData(),
                                                SetOptions.merge()
                                        );
                            }

                            if (requestListener != null) {

                                requestListener.remove();
                                requestListener = null;
                            }

                            if (currentRideListener != null) {

                                currentRideListener.remove();
                                currentRideListener = null;
                            }

                            auth.signOut();

                            Intent intent =
                                    new Intent(
                                            this,
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
                )
                .show();
    }

    @Override
    protected void onDestroy() {

        requestHandler.removeCallbacks(
                requestRefreshRunnable
        );

        if (requestListener != null) {

            requestListener.remove();
            requestListener = null;
        }

        if (currentRideListener != null) {

            currentRideListener.remove();
            currentRideListener = null;
        }

        if (locationManager != null &&
                locationListener != null) {

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
