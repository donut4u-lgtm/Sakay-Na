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
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Transaction;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DriverActivity extends Activity {

    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseUser user;

    private LinearLayout root;
    private LinearLayout requestContainer;
    private LinearLayout rideStatusContainer;

    private TextView statusText;
    private TextView requestsText;
    private TextView currentRideText;

    private Button onlineButton;
    private Button offlineButton;

    private boolean driverOnline = false;
    private boolean driverApproved = false;
    private boolean driverSuspended = false;

    private String driverName = "";
    private String driverPhone = "";
    private String driverPlateNumber = "";
    private String driverVehicle = "";

    private String currentRideId = "";

    /*
     * IMPORTANT:
     * acceptedRideLocked prevents an old asynchronous restoreCurrentRide()
     * from clearing the ride immediately after ACCEPT RIDE succeeds.
     */
    private boolean acceptedRideLocked = false;

    /*
     * Protects against old asynchronous callbacks.
     */
    private long rideStateGeneration = 0L;

    private ListenerRegistration requestListener;
    private ListenerRegistration currentRideListener;

    private final Set<String> hiddenRequestIds = new HashSet<>();
    private final Set<String> notifiedRequestIds = new HashSet<>();

    private final Handler expiryHandler = new Handler(Looper.getMainLooper());

    private LocationManager locationManager;
    private LocationListener locationListener;

    private double driverLatitude = 0.0;
    private double driverLongitude = 0.0;

    private DocumentSnapshot latestRequestSnapshot;

    private boolean locationStarted = false;

    private final String[] ACTIVE_STATUSES = new String[]{
            "ACCEPTED",
            "DRIVER_ON_THE_WAY",
            "DRIVER_ARRIVED",
            "IN_PROGRESS"
    };

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

        /*
         * Only restore if we don't already have a ride locked.
         */
        if (currentRideId.isEmpty() && !acceptedRideLocked) {
            restoreCurrentRide();
        }

        startLocationUpdates();
        requestNotificationPermissionIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (auth == null) {
            return;
        }

        user = auth.getCurrentUser();

        if (user == null) {
            return;
        }

        loadDriverStatus();

        /*
         * NEVER run restore against an already accepted ride.
         */
        if (!currentRideId.isEmpty()) {
            listenForCurrentRide();
        } else if (!acceptedRideLocked) {
            restoreCurrentRide();
        }
    }

    private void buildScreen() {

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(18, 18, 18, 18);
        root.setBackgroundColor(Color.rgb(245, 250, 255));

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(this);
        title.setText("🚖 SAKAY NA - DRIVER");
        title.setTextSize(26);
        title.setTextColor(Color.rgb(0, 70, 140));
        title.setGravity(Gravity.CENTER);
        title.setPadding(10, 15, 10, 15);
        title.setTypeface(null, android.graphics.Typeface.BOLD);

        content.addView(title);

        statusText = new TextView(this);
        statusText.setText("Checking driver status...");
        statusText.setTextSize(18);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(10, 10, 10, 15);
        content.addView(statusText);

        LinearLayout onlineRow = new LinearLayout(this);
        onlineRow.setOrientation(LinearLayout.HORIZONTAL);
        onlineRow.setGravity(Gravity.CENTER);

        onlineButton = new Button(this);
        onlineButton.setText("🟢 GO ONLINE");
        onlineButton.setTextColor(Color.WHITE);
        onlineButton.setBackgroundColor(Color.rgb(0, 150, 70));
        onlineButton.setOnClickListener(v -> setDriverOnline(true));

        offlineButton = new Button(this);
        offlineButton.setText("🔴 GO OFFLINE");
        offlineButton.setTextColor(Color.WHITE);
        offlineButton.setBackgroundColor(Color.rgb(200, 40, 40));
        offlineButton.setOnClickListener(v -> setDriverOnline(false));

        onlineRow.addView(
                onlineButton,
                new LinearLayout.LayoutParams(0, 60, 1)
        );

        onlineRow.addView(
                offlineButton,
                new LinearLayout.LayoutParams(0, 60, 1)
        );

        content.addView(onlineRow);

        TextView requestHeader = new TextView(this);
        requestHeader.setText("📢 RIDE REQUESTS");
        requestHeader.setTextSize(22);
        requestHeader.setTextColor(Color.rgb(0, 90, 160));
        requestHeader.setTypeface(null, android.graphics.Typeface.BOLD);
        requestHeader.setPadding(5, 25, 5, 10);

        content.addView(requestHeader);

        requestsText = new TextView(this);
        requestsText.setText("Checking...");
        requestsText.setTextSize(17);
        requestsText.setPadding(10, 5, 10, 10);

        content.addView(requestsText);

        requestContainer = new LinearLayout(this);
        requestContainer.setOrientation(LinearLayout.VERTICAL);

        content.addView(requestContainer);

        TextView currentHeader = new TextView(this);
        currentHeader.setText("🚕 CURRENT RIDE");
        currentHeader.setTextSize(22);
        currentHeader.setTextColor(Color.rgb(0, 110, 70));
        currentHeader.setTypeface(null, android.graphics.Typeface.BOLD);
        currentHeader.setPadding(5, 25, 5, 10);

        content.addView(currentHeader);

        currentRideText = new TextView(this);
        currentRideText.setText("No current ride");
        currentRideText.setTextSize(18);
        currentRideText.setPadding(10, 10, 10, 10);

        content.addView(currentRideText);

        rideStatusContainer = new LinearLayout(this);
        rideStatusContainer.setOrientation(LinearLayout.VERTICAL);

        content.addView(rideStatusContainer);

        Button mapButton = new Button(this);
        mapButton.setText("🗺 OPEN LIVE MAP");
        mapButton.setOnClickListener(v -> openMap());

        content.addView(mapButton);

        Button chatButton = new Button(this);
        chatButton.setText("💬 CHAT WITH PASSENGER");
        chatButton.setOnClickListener(v -> openChat());

        content.addView(chatButton);

        Button logoutButton = new Button(this);
        logoutButton.setText("🚪 LOG OUT");
        logoutButton.setTextColor(Color.WHITE);
        logoutButton.setBackgroundColor(Color.rgb(100, 100, 100));
        logoutButton.setOnClickListener(v -> logout());

        content.addView(logoutButton);

        scrollView.addView(content);
        root.addView(
                scrollView,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                )
        );

        setContentView(root);
    }

    private void loadDriverStatus() {

        if (user == null) {
            return;
        }

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {

                    if (!doc.exists()) {
                        statusText.setText("Driver profile not found.");
                        return;
                    }

                    driverName = safe(doc.getString("name"));
                    driverPhone = safe(doc.getString("phone"));
                    driverPlateNumber = safe(doc.getString("plateNumber"));
                    driverVehicle = safe(doc.getString("vehicle"));

                    Boolean approved = doc.getBoolean("driverApproved");
                    Boolean suspended = doc.getBoolean("driverSuspended");
                    Boolean online = doc.getBoolean("driverOnline");

                    driverApproved = approved != null && approved;
                    driverSuspended = suspended != null && suspended;
                    driverOnline = online != null && online;

                    if (driverSuspended) {
                        statusText.setText("⛔ DRIVER SUSPENDED");
                        statusText.setTextColor(Color.RED);
                        onlineButton.setEnabled(false);
                        offlineButton.setEnabled(false);
                        listenForRideRequests();
                        return;
                    }

                    if (!driverApproved) {
                        statusText.setText("⏳ DRIVER APPROVAL PENDING");
                        statusText.setTextColor(Color.rgb(200, 120, 0));
                        onlineButton.setEnabled(false);
                        offlineButton.setEnabled(false);
                        listenForRideRequests();
                        return;
                    }

                    onlineButton.setEnabled(true);
                    offlineButton.setEnabled(true);

                    updateOnlineDisplay();

                    checkUnpaidDues();

                    listenForRideRequests();
                });
    }

    private void checkUnpaidDues() {

        if (user == null) {
            return;
        }

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {

                    if (!doc.exists()) {
                        return;
                    }

                    Long unpaid = doc.getLong("unpaidDues");

                    if (unpaid != null && unpaid > 0) {
                        statusText.setText(
                                "⚠️ UNPAID DRIVER DUES: ₱" + unpaid
                        );
                    }
                });
    }

    private void loadOnlineStatus() {

        if (user == null) {
            return;
        }

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {

                    if (!doc.exists()) {
                        return;
                    }

                    Boolean online = doc.getBoolean("driverOnline");

                    if (online != null) {
                        driverOnline = online;
                    }

                    updateOnlineDisplay();
                });
    }

    private void updateOnlineDisplay() {

        if (driverOnline) {
            statusText.setText("🟢 DRIVER ONLINE - WAITING FOR RIDES");
            statusText.setTextColor(Color.rgb(0, 140, 70));
        } else {
            statusText.setText("🔴 DRIVER OFFLINE");
            statusText.setTextColor(Color.rgb(200, 40, 40));
        }
    }

    private void setDriverOnline(boolean online) {

        if (user == null) {
            return;
        }

        if (!driverApproved) {
            Toast.makeText(
                    this,
                    "Driver approval required.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        driverOnline = online;

        db.collection("users")
                .document(user.getUid())
                .update(
                        "driverOnline", online,
                        "driverAvailability", online ? "ONLINE" : "OFFLINE",
                        "availabilityUpdatedAt", FieldValue.serverTimestamp()
                )
                .addOnSuccessListener(v -> {

                    updateOnlineDisplay();
                    listenForRideRequests();

                    Toast.makeText(
                            this,
                            online ? "You are ONLINE." : "You are OFFLINE.",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(e -> {

                    driverOnline = !online;
                    updateOnlineDisplay();

                    Toast.makeText(
                            this,
                            "Could not change status.",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void listenForRideRequests() {

        if (user == null) {
            return;
        }

        if (requestListener != null) {
            requestListener.remove();
            requestListener = null;
        }

        /*
         * We still listen for REQUESTED rides even while currentRideId exists.
         * This prevents the dashboard from getting stuck.
         */
        Query query = db.collection("rides")
                .whereEqualTo("status", "REQUESTED");

        requestListener = query.addSnapshotListener((snapshots, error) -> {

            if (error != null) {
                requestsText.setText("Unable to load ride requests.");
                return;
            }

            if (snapshots == null) {
                requestsText.setText("No new ride request");
                return;
            }

            renderRideRequests(snapshots.getDocuments());
        });
    }

    private void renderRideRequests(List<DocumentSnapshot> rides) {

        requestContainer.removeAllViews();

        int visible = 0;

        for (DocumentSnapshot ride : rides) {

            String rideId = ride.getId();

            if (hiddenRequestIds.contains(rideId)) {
                continue;
            }

            String status = safe(ride.getString("status"));

            if (!"REQUESTED".equals(status)) {
                continue;
            }

            visible++;

            addRideCard(ride);
        }

        if (visible == 0) {
            requestsText.setText("No new ride request");
        } else {
            requestsText.setText(
                    "🔔 " + visible + " NEW RIDE REQUEST"
                            + (visible == 1 ? "" : "S")
            );
        }
    }

    private void addRideCard(DocumentSnapshot ride) {

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(18, 18, 18, 18);
        card.setBackgroundColor(Color.WHITE);

        TextView info = new TextView(this);
        info.setTextSize(17);
        info.setTextColor(Color.DKGRAY);

        String passengerName = safe(ride.getString("passengerName"));
        String pickup = safe(ride.getString("pickup"));
        String destination = safe(ride.getString("destination"));
        String payment = safe(ride.getString("paymentMethod"));

        Double fareValue = ride.getDouble("fare");

        if (fareValue == null) {
            Long fareLong = ride.getLong("fare");
            fareValue = fareLong == null ? 0.0 : fareLong.doubleValue();
        }

        String fare = String.format(
                Locale.US,
                "₱%.2f",
                fareValue
        );

        info.setText(
                "👤 Passenger: " + passengerName
                        + "\n\n📍 Pickup: " + pickup
                        + "\n\n🏁 Destination: " + destination
                        + "\n\n💰 Fare: " + fare
                        + "\n\n💳 Payment: " + payment
        );

        card.addView(info);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);

        Button accept = new Button(this);
        accept.setText("✅ ACCEPT RIDE");
        accept.setTextColor(Color.WHITE);
        accept.setBackgroundColor(Color.rgb(0, 150, 70));

        Button decline = new Button(this);
        decline.setText("❌ DECLINE");
        decline.setTextColor(Color.WHITE);
        decline.setBackgroundColor(Color.rgb(200, 50, 50));

        accept.setOnClickListener(v -> acceptRide(ride));
        decline.setOnClickListener(v -> declineRide(ride));

        buttons.addView(
                accept,
                new LinearLayout.LayoutParams(0, 60, 1)
        );

        buttons.addView(
                decline,
                new LinearLayout.LayoutParams(0, 60, 1)
        );

        card.addView(buttons);

        requestContainer.addView(card);

        latestRequestSnapshot = ride;
    }

    private void acceptRide(DocumentSnapshot ride) {

        if (user == null) {
            return;
        }

        final String rideId = ride.getId();

        /*
         * Lock immediately.
         *
         * This is the important part of the fix.
         */
        rideStateGeneration++;
        final long myGeneration = rideStateGeneration;

        acceptedRideLocked = true;
        currentRideId = rideId;

        hiddenRequestIds.add(rideId);

        /*
         * Immediately show the accepted ride.
         * Do not wait for restoreCurrentRide().
         */
        showCurrentRide(ride);

        currentRideText.setText(
                "⏳ Accepting ride...\n\n"
                        + "📍 " + safe(ride.getString("pickup"))
                        + "\n🏁 " + safe(ride.getString("destination"))
        );

        db.runTransaction((Transaction.Function<Void>) transaction -> {

            DocumentSnapshot fresh =
                    transaction.get(
                            db.collection("rides")
                                    .document(rideId)
                    );

            if (!fresh.exists()) {
                throw new IllegalStateException(
                        "Ride no longer exists."
                );
            }

            String status = safe(fresh.getString("status"));

            if (!"REQUESTED".equals(status)) {
                throw new IllegalStateException(
                        "Ride was already accepted by another driver."
                );
            }

            transaction.update(
                    db.collection("rides").document(rideId),

                    "driverId", user.getUid(),
                    "driverName", driverName,
                    "driverPhone", driverPhone,
                    "driverPlateNumber", driverPlateNumber,
                    "driverVehicle", driverVehicle,

                    "status", "ACCEPTED",
                    "acceptedAt", FieldValue.serverTimestamp(),
                    "statusUpdatedAt", FieldValue.serverTimestamp(),

                    "adminDriverId", user.getUid(),
                    "adminDriverName", driverName
            );

            return null;
        })
        .addOnSuccessListener(v -> {

            /*
             * Ignore any old callback.
             */
            if (myGeneration != rideStateGeneration) {
                return;
            }

            /*
             * Re-lock after Firestore success.
             */
            acceptedRideLocked = true;
            currentRideId = rideId;

            hiddenRequestIds.add(rideId);

            Toast.makeText(
                    this,
                    "✅ RIDE ACCEPTED",
                    Toast.LENGTH_LONG
            ).show();

            /*
             * Start the current ride listener using the exact accepted ID.
             */
            listenForCurrentRide();

            /*
             * Rebuild request list but DO NOT restore current ride.
             */
            listenForRideRequests();

            showCurrentRide(ride);

            currentRideText.setText(
                    "✅ RIDE ACCEPTED\n\n"
                            + "👤 Passenger: "
                            + safe(ride.getString("passengerName"))
                            + "\n\n📍 Pickup: "
                            + safe(ride.getString("pickup"))
                            + "\n\n🏁 Destination: "
                            + safe(ride.getString("destination"))
            );
        })
        .addOnFailureListener(e -> {

            if (myGeneration != rideStateGeneration) {
                return;
            }

            /*
             * Only unlock if ACCEPT actually failed.
             */
            acceptedRideLocked = false;
            currentRideId = "";

            hiddenRequestIds.remove(rideId);

            rideStateGeneration++;

            currentRideText.setText("No current ride");

            Toast.makeText(
                    this,
                    "Accept failed: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();

            listenForRideRequests();
        });
    }

    private void declineRide(DocumentSnapshot ride) {

        String rideId = ride.getId();

        hiddenRequestIds.add(rideId);

        db.collection("rides")
                .document(rideId)
                .update(
                        "status", "DECLINED",
                        "declinedBy", user.getUid(),
                        "declinedAt", FieldValue.serverTimestamp(),
                        "statusUpdatedAt", FieldValue.serverTimestamp()
                )
                .addOnSuccessListener(v -> {

                    Toast.makeText(
                            this,
                            "Ride declined.",
                            Toast.LENGTH_SHORT
                    ).show();

                    listenForRideRequests();
                });
    }

    /*
     * Restore only when there is NO accepted ride already locked.
     */
    private void restoreCurrentRide() {

        if (user == null) {
            return;
        }

        if (acceptedRideLocked && !currentRideId.isEmpty()) {
            listenForCurrentRide();
            return;
        }

        if (!currentRideId.isEmpty()) {
            listenForCurrentRide();
            return;
        }

        final long restoreGeneration = ++rideStateGeneration;

        db.collection("rides")
                .whereEqualTo("driverId", user.getUid())
                .get()
                .addOnSuccessListener(snapshots -> {

                    /*
                     * An ACCEPT RIDE happened while this query was running.
                     * DO NOT TOUCH currentRideId.
                     */
                    if (restoreGeneration != rideStateGeneration) {
                        return;
                    }

                    if (acceptedRideLocked || !currentRideId.isEmpty()) {
                        return;
                    }

                    DocumentSnapshot newest = null;
                    long newestTime = Long.MIN_VALUE;

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {

                        String status =
                                safe(doc.getString("status"));

                        if (!isActive(status)) {
                            continue;
                        }

                        Timestamp accepted =
                                doc.getTimestamp("acceptedAt");

                        long time = 0;

                        if (accepted != null) {
                            time = accepted.toDate().getTime();
                        }

                        if (newest == null || time > newestTime) {
                            newest = doc;
                            newestTime = time;
                        }
                    }

                    if (newest != null) {

                        acceptedRideLocked = true;
                        currentRideId = newest.getId();

                        showCurrentRide(newest);
                        listenForCurrentRide();

                    } else {

                        /*
                         * Only clear if we are STILL empty.
                         */
                        if (currentRideId.isEmpty()
                                && !acceptedRideLocked
                                && restoreGeneration == rideStateGeneration) {

                            currentRideText.setText(
                                    "No current ride"
                            );

                            rideStatusContainer.removeAllViews();
                        }
                    }
                })
                .addOnFailureListener(e -> {

                    /*
                     * NEVER clear currentRideId on query failure.
                     */
                    if (!currentRideId.isEmpty()
                            || acceptedRideLocked) {
                        return;
                    }

                    currentRideText.setText(
                            "No current ride"
                    );
                });
    }

    private void listenForCurrentRide() {

        if (user == null) {
            return;
        }

        if (currentRideId.isEmpty()) {
            return;
        }

        final String rideId = currentRideId;
        final long listenerGeneration = rideStateGeneration;

        if (currentRideListener != null) {
            currentRideListener.remove();
            currentRideListener = null;
        }

        currentRideListener = db.collection("rides")
                .document(rideId)
                .addSnapshotListener((ride, error) -> {

                    /*
                     * Old listener must never overwrite a newer ride.
                     */
                    if (listenerGeneration != rideStateGeneration) {
                        return;
                    }

                    if (!rideId.equals(currentRideId)) {
                        return;
                    }

                    if (error != null) {

                        /*
                         * IMPORTANT:
                         * Do NOT clear currentRideId on listener error.
                         */
                        currentRideText.setText(
                                "🔄 Current ride connection is reconnecting..."
                        );

                        return;
                    }

                    /*
                     * Do NOT clear the ride if a temporary null/nonexistent
                     * snapshot appears.
                     */
                    if (ride == null || !ride.exists()) {

                        currentRideText.setText(
                                "🔄 Current ride is syncing..."
                        );

                        return;
                    }

                    String status =
                            safe(ride.getString("status"));

                    if (isActive(status)) {

                        /*
                         * Keep it locked.
                         */
                        acceptedRideLocked = true;
                        currentRideId = rideId;

                        showCurrentRide(ride);
                        showRideStatusButtons(status);

                    } else if (
                            "COMPLETED".equals(status)
                                    || "CANCELLED".equals(status)
                                    || "DECLINED".equals(status)
                                    || "EXPIRED".equals(status)
                                    || "FINISHED".equals(status)
                    ) {

                        showCurrentRide(ride);

                        currentRideText.append(
                                "\n\n🏁 RIDE FINISHED"
                        );

                        clearCurrentRideState();

                    } else {

                        showCurrentRide(ride);
                        showRideStatusButtons(status);
                    }
                });
    }

    private void showCurrentRide(DocumentSnapshot ride) {

        if (ride == null || !ride.exists()) {
            return;
        }

        String passenger =
                safe(ride.getString("passengerName"));

        String pickup =
                safe(ride.getString("pickup"));

        String destination =
                safe(ride.getString("destination"));

        String payment =
                safe(ride.getString("paymentMethod"));

        String status =
                safe(ride.getString("status"));

        Double fareValue = ride.getDouble("fare");

        if (fareValue == null) {

            Long fareLong = ride.getLong("fare");

            if (fareLong != null) {
                fareValue = fareLong.doubleValue();
            }
        }

        if (fareValue == null) {
            fareValue = 0.0;
        }

        String fare = String.format(
                Locale.US,
                "₱%.2f",
                fareValue
        );

        currentRideText.setText(
                "👤 Passenger: " + passenger
                        + "\n\n📍 Pickup: " + pickup
                        + "\n\n🏁 Destination: " + destination
                        + "\n\n💰 Fare: " + fare
                        + "\n\n💳 Payment: " + payment
                        + "\n\n📌 Status: " + status
        );

        currentRideText.setTextColor(
                Color.rgb(20, 60, 80)
        );
    }

    private void showRideStatusButtons(String status) {

        rideStatusContainer.removeAllViews();

        if (currentRideId.isEmpty()) {
            return;
        }

        if ("ACCEPTED".equals(status)) {

            addStatusButton(
                    "🚗 DRIVER ON THE WAY",
                    "DRIVER_ON_THE_WAY"
            );

        } else if ("DRIVER_ON_THE_WAY".equals(status)) {

            addStatusButton(
                    "📍 I HAVE ARRIVED",
                    "DRIVER_ARRIVED"
            );

        } else if ("DRIVER_ARRIVED".equals(status)) {

            addStatusButton(
                    "🚕 START RIDE",
                    "IN_PROGRESS"
            );

        } else if ("IN_PROGRESS".equals(status)) {

            addStatusButton(
                    "🏁 FINISHED TRIP",
                    "COMPLETED"
            );
        }

        Button chat = new Button(this);
        chat.setText("💬 CHAT WITH PASSENGER");
        chat.setTextSize(17);
        chat.setOnClickListener(v -> openChat());

        rideStatusContainer.addView(chat);

        Button map = new Button(this);
        map.setText("🗺 LIVE MAP");
        map.setTextSize(17);
        map.setOnClickListener(v -> openMap());

        rideStatusContainer.addView(map);
    }

    private void addStatusButton(
            String text,
            String newStatus
    ) {

        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(18);
        button.setTextColor(Color.WHITE);
        button.setBackgroundColor(
                Color.rgb(0, 110, 180)
        );

        button.setOnClickListener(
                v -> updateRideStatus(newStatus)
        );

        rideStatusContainer.addView(
                button,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        65
                )
        );
    }

    private void updateRideStatus(String newStatus) {

        if (user == null || currentRideId.isEmpty()) {

            Toast.makeText(
                    this,
                    "No current ride.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        final String rideId = currentRideId;

        db.collection("rides")
                .document(rideId)
                .update(
                        "status", newStatus,
                        "statusUpdatedAt",
                        FieldValue.serverTimestamp()
                )
                .addOnSuccessListener(v -> {

                    /*
                     * Keep the exact accepted ride locked.
                     */
                    if (rideId.equals(currentRideId)) {

                        acceptedRideLocked = true;

                        Toast.makeText(
                                this,
                                "Status updated.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            "Status update failed: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void clearCurrentRideState() {

        rideStateGeneration++;

        acceptedRideLocked = false;
        currentRideId = "";

        if (currentRideListener != null) {
            currentRideListener.remove();
            currentRideListener = null;
        }

        rideStatusContainer.removeAllViews();

        currentRideText.setText(
                "No current ride"
        );

        listenForRideRequests();
    }

    private boolean isActive(String status) {

        if (status == null) {
            return false;
        }

        for (String active : ACTIVE_STATUSES) {

            if (active.equals(status)) {
                return true;
            }
        }

        return false;
    }

    private String safe(String value) {

        return value == null ? "" : value;
    }

    private void openMap() {

        if (currentRideId.isEmpty()) {

            Toast.makeText(
                    this,
                    "Accept a ride first.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Intent intent =
                new Intent(this, MapActivity.class);

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

        if (currentRideId.isEmpty()) {

            Toast.makeText(
                    this,
                    "Accept a ride first.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Intent intent =
                new Intent(this, RideChatActivity.class);

        intent.putExtra(
                "rideId",
                currentRideId
        );

        startActivity(intent);
    }

    private void startLocationUpdates() {

        if (locationStarted) {
            return;
        }

        locationManager =
                (LocationManager) getSystemService(
                        LOCATION_SERVICE
                );

        if (locationManager == null) {
            return;
        }

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

        locationListener =
                new LocationListener() {

                    @Override
                    public void onLocationChanged(
                            @NonNull Location location
                    ) {

                        driverLatitude =
                                location.getLatitude();

                        driverLongitude =
                                location.getLongitude();

                        updateDriverLocation();
                    }
                };

        try {

            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    3000,
                    5,
                    locationListener
            );

            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    5000,
                    10,
                    locationListener
            );

            locationStarted = true;

        } catch (SecurityException ignored) {
        }
    }

    private void updateDriverLocation() {

        if (user == null) {
            return;
        }

        if (currentRideId.isEmpty()) {
            return;
        }

        if (driverLatitude == 0.0
                && driverLongitude == 0.0) {
            return;
        }

        db.collection("rides")
                .document(currentRideId)
                .update(
                        "driverLatitude",
                        driverLatitude,
                        "driverLongitude",
                        driverLongitude
                );
    }

    private void requestNotificationPermissionIfNeeded() {

        if (android.os.Build.VERSION.SDK_INT >= 33) {

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.POST_NOTIFICATIONS
                        },
                        2002
                );
            }
        }
    }

    private void logout() {

        new AlertDialog.Builder(this)
                .setTitle("Log out?")
                .setMessage(
                        "Are you sure you want to log out?"
                )
                .setNegativeButton(
                        "CANCEL",
                        null
                )
                .setPositiveButton(
                        "LOG OUT",
                        (dialog, which) -> {

                            if (user != null) {

                                db.collection("users")
                                        .document(user.getUid())
                                        .update(
                                                "driverOnline",
                                                false,
                                                "driverAvailability",
                                                "OFFLINE",
                                                "availabilityUpdatedAt",
                                                FieldValue.serverTimestamp()
                                        );
                            }

                            rideStateGeneration++;

                            acceptedRideLocked = false;
                            currentRideId = "";

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

                            intent.setFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK
                                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
                            );

                            startActivity(intent);
                            finish();
                        })
                .show();
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

            } catch (SecurityException ignored) {
            }
        }

        expiryHandler.removeCallbacksAndMessages(null);

        super.onDestroy();
    }
}
