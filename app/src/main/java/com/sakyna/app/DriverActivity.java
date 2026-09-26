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

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DriverActivity extends Activity {

    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private static final int NOTIFICATION_PERMISSION_REQUEST = 2002;

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

    /*
     * ONLY the currently accepted ride.
     */
    private String currentRideId = "";

    /*
     * Once ACCEPT RIDE succeeds, this prevents another asynchronous
     * operation from replacing or clearing the accepted ride.
     */
    private boolean acceptedRideLocked = false;

    private long rideStateGeneration = 0L;

    private ListenerRegistration requestListener;
    private ListenerRegistration currentRideListener;

    private final Set<String> hiddenRequestIds = new HashSet<>();

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    private LocationManager locationManager;
    private LocationListener locationListener;

    private double driverLatitude = 0.0;
    private double driverLongitude = 0.0;

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

        try {
            auth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();
            user = auth.getCurrentUser();

            if (user == null) {
                finish();
                return;
            }

            /*
             * Build the screen FIRST.
             * Nothing asynchronous is allowed before the UI exists.
             */
            buildScreen();

            /*
             * Load driver profile.
             */
            loadDriverStatus();

            /*
             * Do NOT immediately run the old restore race.
             * First show the dashboard normally.
             */
            currentRideText.setText("No current ride");

            /*
             * Start location only after the screen is ready.
             */
            mainHandler.postDelayed(
                    () -> {
                        if (!isFinishing()
                                && !isDestroyed()) {
                            startLocationUpdatesSafely();
                        }
                    },
                    500
            );

            requestNotificationPermissionIfNeeded();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Driver screen error: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();

            finish();
        }
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

        /*
         * Do not call restoreCurrentRide() here.
         * That was part of the old race that could destroy currentRideId.
         */
        loadDriverStatus();

        if (!currentRideId.isEmpty()) {
            listenForCurrentRide();
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
        title.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

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
        onlineButton.setBackgroundColor(
                Color.rgb(0, 150, 70)
        );

        onlineButton.setOnClickListener(
                v -> setDriverOnline(true)
        );

        offlineButton = new Button(this);
        offlineButton.setText("🔴 GO OFFLINE");
        offlineButton.setTextColor(Color.WHITE);
        offlineButton.setBackgroundColor(
                Color.rgb(200, 40, 40)
        );

        offlineButton.setOnClickListener(
                v -> setDriverOnline(false)
        );

        onlineRow.addView(
                onlineButton,
                new LinearLayout.LayoutParams(
                        0,
                        60,
                        1
                )
        );

        onlineRow.addView(
                offlineButton,
                new LinearLayout.LayoutParams(
                        0,
                        60,
                        1
                )
        );

        content.addView(onlineRow);

        TextView requestHeader = new TextView(this);
        requestHeader.setText("📢 RIDE REQUESTS");
        requestHeader.setTextSize(22);
        requestHeader.setTextColor(
                Color.rgb(0, 90, 160)
        );
        requestHeader.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );
        requestHeader.setPadding(5, 25, 5, 10);

        content.addView(requestHeader);

        requestsText = new TextView(this);
        requestsText.setText("Checking...");
        requestsText.setTextSize(17);
        requestsText.setPadding(10, 5, 10, 10);

        content.addView(requestsText);

        requestContainer = new LinearLayout(this);
        requestContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        content.addView(requestContainer);

        TextView currentHeader = new TextView(this);
        currentHeader.setText("🚕 CURRENT RIDE");
        currentHeader.setTextSize(22);
        currentHeader.setTextColor(
                Color.rgb(0, 110, 70)
        );
        currentHeader.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );
        currentHeader.setPadding(5, 25, 5, 10);

        content.addView(currentHeader);

        currentRideText = new TextView(this);
        currentRideText.setText("No current ride");
        currentRideText.setTextSize(18);
        currentRideText.setPadding(10, 10, 10, 10);

        content.addView(currentRideText);

        rideStatusContainer = new LinearLayout(this);
        rideStatusContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        content.addView(rideStatusContainer);

        Button mapButton = new Button(this);
        mapButton.setText("🗺 OPEN LIVE MAP");
        mapButton.setOnClickListener(
                v -> openMap()
        );

        content.addView(mapButton);

        Button chatButton = new Button(this);
        chatButton.setText("💬 CHAT WITH PASSENGER");
        chatButton.setOnClickListener(
                v -> openChat()
        );

        content.addView(chatButton);

        Button logoutButton = new Button(this);
        logoutButton.setText("🚪 LOG OUT");
        logoutButton.setTextColor(Color.WHITE);
        logoutButton.setBackgroundColor(
                Color.rgb(100, 100, 100)
        );

        logoutButton.setOnClickListener(
                v -> logout()
        );

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

        if (user == null || db == null) {
            return;
        }

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {

                    if (doc == null || !doc.exists()) {

                        statusText.setText(
                                "Driver profile not found."
                        );

                        return;
                    }

                    driverName =
                            safe(doc.getString("name"));

                    driverPhone =
                            safe(doc.getString("phone"));

                    driverPlateNumber =
                            safe(doc.getString("plateNumber"));

                    driverVehicle =
                            safe(doc.getString("vehicle"));

                    Boolean approved =
                            doc.getBoolean("driverApproved");

                    Boolean suspended =
                            doc.getBoolean("driverSuspended");

                    Boolean online =
                            doc.getBoolean("driverOnline");

                    driverApproved =
                            approved != null && approved;

                    driverSuspended =
                            suspended != null && suspended;

                    driverOnline =
                            online != null && online;

                    if (driverSuspended) {

                        statusText.setText(
                                "⛔ DRIVER SUSPENDED"
                        );

                        statusText.setTextColor(Color.RED);

                        onlineButton.setEnabled(false);
                        offlineButton.setEnabled(false);

                        listenForRideRequests();

                        return;
                    }

                    if (!driverApproved) {

                        statusText.setText(
                                "⏳ DRIVER APPROVAL PENDING"
                        );

                        statusText.setTextColor(
                                Color.rgb(200, 120, 0)
                        );

                        onlineButton.setEnabled(false);
                        offlineButton.setEnabled(false);

                        listenForRideRequests();

                        return;
                    }

                    onlineButton.setEnabled(true);
                    offlineButton.setEnabled(true);

                    updateOnlineDisplay();

                    listenForRideRequests();

                    checkUnpaidDues();
                })
                .addOnFailureListener(e -> {

                    statusText.setText(
                            "Unable to load driver profile."
                    );
                });
    }

    private void checkUnpaidDues() {

        if (user == null || db == null) {
            return;
        }

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {

                    if (doc == null || !doc.exists()) {
                        return;
                    }

                    Long unpaid =
                            doc.getLong("unpaidDues");

                    if (unpaid != null && unpaid > 0) {

                        statusText.setText(
                                "⚠️ UNPAID DRIVER DUES: ₱"
                                        + unpaid
                        );
                    }
                });
    }

    private void updateOnlineDisplay() {

        if (statusText == null) {
            return;
        }

        if (driverOnline) {

            statusText.setText(
                    "🟢 DRIVER ONLINE - WAITING FOR RIDES"
            );

            statusText.setTextColor(
                    Color.rgb(0, 140, 70)
            );

        } else {

            statusText.setText(
                    "🔴 DRIVER OFFLINE"
            );

            statusText.setTextColor(
                    Color.rgb(200, 40, 40)
            );
        }
    }

    private void setDriverOnline(boolean online) {

        if (user == null || db == null) {
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

        final boolean previous = driverOnline;

        driverOnline = online;

        updateOnlineDisplay();

        db.collection("users")
                .document(user.getUid())
                .update(
                        "driverOnline",
                        online,
                        "driverAvailability",
                        online ? "ONLINE" : "OFFLINE",
                        "availabilityUpdatedAt",
                        FieldValue.serverTimestamp()
                )
                .addOnSuccessListener(v -> {

                    listenForRideRequests();

                    Toast.makeText(
                            this,
                            online
                                    ? "You are ONLINE."
                                    : "You are OFFLINE.",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(e -> {

                    driverOnline = previous;

                    updateOnlineDisplay();

                    Toast.makeText(
                            this,
                            "Could not change status.",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void listenForRideRequests() {

        if (user == null || db == null) {
            return;
        }

        if (requestListener != null) {

            requestListener.remove();
            requestListener = null;
        }

        Query query =
                db.collection("rides")
                        .whereEqualTo(
                                "status",
                                "REQUESTED"
                        );

        requestListener =
                query.addSnapshotListener(
                        (snapshots, error) -> {

                            if (error != null) {

                                requestsText.setText(
                                        "Unable to load ride requests."
                                );

                                return;
                            }

                            if (snapshots == null) {

                                requestsText.setText(
                                        "No new ride request"
                                );

                                return;
                            }

                            renderRideRequests(
                                    snapshots.getDocuments()
                            );
                        }
                );
    }

    private void renderRideRequests(
            List<DocumentSnapshot> rides
    ) {

        if (requestContainer == null) {
            return;
        }

        requestContainer.removeAllViews();

        int visible = 0;

        for (DocumentSnapshot ride : rides) {

            if (ride == null) {
                continue;
            }

            String rideId = ride.getId();

            if (hiddenRequestIds.contains(rideId)) {
                continue;
            }

            String status =
                    safe(ride.getString("status"));

            if (!"REQUESTED".equals(status)) {
                continue;
            }

            visible++;

            addRideCard(ride);
        }

        if (visible == 0) {

            requestsText.setText(
                    "No new ride request"
            );

        } else {

            requestsText.setText(
                    "🔔 "
                            + visible
                            + " NEW RIDE REQUEST"
                            + (visible == 1 ? "" : "S")
            );
        }
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
                18,
                18,
                18,
                18
        );

        card.setBackgroundColor(Color.WHITE);

        TextView info =
                new TextView(this);

        info.setTextSize(17);
        info.setTextColor(Color.DKGRAY);

        String passengerName =
                safe(ride.getString("passengerName"));

        String pickup =
                safe(ride.getString("pickup"));

        String destination =
                safe(ride.getString("destination"));

        String payment =
                safe(ride.getString("paymentMethod"));

        Double fareValue =
                ride.getDouble("fare");

        if (fareValue == null) {

            Long fareLong =
                    ride.getLong("fare");

            if (fareLong != null) {
                fareValue =
                        fareLong.doubleValue();
            }
        }

        if (fareValue == null) {
            fareValue = 0.0;
        }

        String fare =
                String.format(
                        Locale.US,
                        "₱%.2f",
                        fareValue
                );

        info.setText(
                "👤 Passenger: "
                        + passengerName
                        + "\n\n📍 Pickup: "
                        + pickup
                        + "\n\n🏁 Destination: "
                        + destination
                        + "\n\n💰 Fare: "
                        + fare
                        + "\n\n💳 Payment: "
                        + payment
        );

        card.addView(info);

        LinearLayout buttons =
                new LinearLayout(this);

        buttons.setOrientation(
                LinearLayout.HORIZONTAL
        );

        Button accept =
                new Button(this);

        accept.setText("✅ ACCEPT RIDE");
        accept.setTextColor(Color.WHITE);
        accept.setBackgroundColor(
                Color.rgb(0, 150, 70)
        );

        Button decline =
                new Button(this);

        decline.setText("❌ DECLINE");
        decline.setTextColor(Color.WHITE);
        decline.setBackgroundColor(
                Color.rgb(200, 50, 50)
        );

        accept.setOnClickListener(
                v -> acceptRide(ride)
        );

        decline.setOnClickListener(
                v -> declineRide(ride)
        );

        buttons.addView(
                accept,
                new LinearLayout.LayoutParams(
                        0,
                        60,
                        1
                )
        );

        buttons.addView(
                decline,
                new LinearLayout.LayoutParams(
                        0,
                        60,
                        1
                )
        );

        card.addView(buttons);

        requestContainer.addView(card);
    }

    private void acceptRide(
            DocumentSnapshot ride
    ) {

        if (user == null || db == null) {
            return;
        }

        if (ride == null || !ride.exists()) {
            return;
        }

        final String rideId =
                ride.getId();

        /*
         * LOCK THE RIDE BEFORE FIRESTORE.
         */
        rideStateGeneration++;

        final long generation =
                rideStateGeneration;

        acceptedRideLocked = true;
        currentRideId = rideId;

        hiddenRequestIds.add(rideId);

        /*
         * Immediately remove the request card.
         */
        renderCurrentRequestState();

        /*
         * Immediately display the accepted ride.
         */
        showCurrentRide(ride);

        currentRideText.setText(
                "⏳ ACCEPTING RIDE...\n\n"
                        + "📍 "
                        + safe(ride.getString("pickup"))
                        + "\n\n🏁 "
                        + safe(ride.getString("destination"))
        );

        db.runTransaction(
                (Transaction.Function<Void>)
                        transaction -> {

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

                            String status =
                                    safe(
                                            fresh.getString(
                                                    "status"
                                            )
                                    );

                            if (!"REQUESTED".equals(status)) {

                                throw new IllegalStateException(
                                        "Ride was already accepted."
                                );
                            }

                            transaction.update(
                                    db.collection("rides")
                                            .document(rideId),

                                    "driverId",
                                    user.getUid(),

                                    "driverName",
                                    driverName,

                                    "driverPhone",
                                    driverPhone,

                                    "driverPlateNumber",
                                    driverPlateNumber,

                                    "driverVehicle",
                                    driverVehicle,

                                    "status",
                                    "ACCEPTED",

                                    "acceptedAt",
                                    FieldValue.serverTimestamp(),

                                    "statusUpdatedAt",
                                    FieldValue.serverTimestamp(),

                                    "adminDriverId",
                                    user.getUid(),

                                    "adminDriverName",
                                    driverName
                            );

                            return null;
                        }
                )
                .addOnSuccessListener(v -> {

                    if (generation != rideStateGeneration) {
                        return;
                    }

                    /*
                     * KEEP THE ACCEPTED RIDE.
                     */
                    acceptedRideLocked = true;
                    currentRideId = rideId;

                    hiddenRequestIds.add(rideId);

                    Toast.makeText(
                            this,
                            "✅ RIDE ACCEPTED",
                            Toast.LENGTH_LONG
                    ).show();

                    showCurrentRide(ride);

                    showRideStatusButtons(
                            "ACCEPTED"
                    );

                    listenForCurrentRide();

                    renderCurrentRequestState();
                })
                .addOnFailureListener(e -> {

                    if (generation != rideStateGeneration) {
                        return;
                    }

                    acceptedRideLocked = false;
                    currentRideId = "";

                    hiddenRequestIds.remove(rideId);

                    rideStateGeneration++;

                    currentRideText.setText(
                            "No current ride"
                    );

                    rideStatusContainer.removeAllViews();

                    Toast.makeText(
                            this,
                            "Accept failed: "
                                    + safe(e.getMessage()),
                            Toast.LENGTH_LONG
                    ).show();

                    listenForRideRequests();
                });
    }

    private void renderCurrentRequestState() {

        if (requestContainer != null) {
            requestContainer.removeAllViews();
        }

        if (requestsText != null) {
            requestsText.setText(
                    "Ride accepted. No new ride request."
            );
        }
    }

    private void declineRide(
            DocumentSnapshot ride
    ) {

        if (user == null || db == null) {
            return;
        }

        final String rideId =
                ride.getId();

        hiddenRequestIds.add(rideId);

        db.collection("rides")
                .document(rideId)
                .update(
                        "status",
                        "DECLINED",

                        "declinedBy",
                        user.getUid(),

                        "declinedAt",
                        FieldValue.serverTimestamp(),

                        "statusUpdatedAt",
                        FieldValue.serverTimestamp()
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

    private void listenForCurrentRide() {

        if (user == null || db == null) {
            return;
        }

        if (currentRideId.isEmpty()) {
            return;
        }

        final String rideId =
                currentRideId;

        final long generation =
                rideStateGeneration;

        if (currentRideListener != null) {

            currentRideListener.remove();
            currentRideListener = null;
        }

        currentRideListener =
                db.collection("rides")
                        .document(rideId)
                        .addSnapshotListener(
                                (ride, error) -> {

                                    /*
                                     * Ignore old listeners.
                                     */
                                    if (generation
                                            != rideStateGeneration) {
                                        return;
                                    }

                                    /*
                                     * Ignore another ride.
                                     */
                                    if (!rideId.equals(
                                            currentRideId
                                    )) {
                                        return;
                                    }

                                    if (error != null) {

                                        currentRideText.setText(
                                                "🔄 Current ride reconnecting..."
                                        );

                                        return;
                                    }

                                    /*
                                     * NEVER erase the ride because of
                                     * a temporary Firestore state.
                                     */
                                    if (ride == null
                                            || !ride.exists()) {

                                        currentRideText.setText(
                                                "🔄 Current ride syncing..."
                                        );

                                        return;
                                    }

                                    String status =
                                            safe(
                                                    ride.getString(
                                                            "status"
                                                    )
                                            );

                                    if (isActive(status)) {

                                        acceptedRideLocked = true;
                                        currentRideId = rideId;

                                        showCurrentRide(ride);

                                        showRideStatusButtons(
                                                status
                                        );

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

                                        showRideStatusButtons(
                                                status
                                        );
                                    }
                                }
                        );
    }

    private void showCurrentRide(
            DocumentSnapshot ride
    ) {

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

        Double fareValue =
                ride.getDouble("fare");

        if (fareValue == null) {

            Long fareLong =
                    ride.getLong("fare");

            if (fareLong != null) {
                fareValue =
                        fareLong.doubleValue();
            }
        }

        if (fareValue == null) {
            fareValue = 0.0;
        }

        String fare =
                String.format(
                        Locale.US,
                        "₱%.2f",
                        fareValue
                );

        currentRideText.setText(
                "👤 Passenger: "
                        + passenger
                        + "\n\n📍 Pickup: "
                        + pickup
                        + "\n\n🏁 Destination: "
                        + destination
                        + "\n\n💰 Fare: "
                        + fare
                        + "\n\n💳 Payment: "
                        + payment
                        + "\n\n📌 Status: "
                        + status
        );

        currentRideText.setTextColor(
                Color.rgb(20, 60, 80)
        );
    }

    private void showRideStatusButtons(
            String status
    ) {

        rideStatusContainer.removeAllViews();

        if (currentRideId.isEmpty()) {
            return;
        }

        if ("ACCEPTED".equals(status)) {

            addStatusButton(
                    "🚗 DRIVER ON THE WAY",
                    "DRIVER_ON_THE_WAY"
            );

        } else if (
                "DRIVER_ON_THE_WAY".equals(status)
        ) {

            addStatusButton(
                    "📍 I HAVE ARRIVED",
                    "DRIVER_ARRIVED"
            );

        } else if (
                "DRIVER_ARRIVED".equals(status)
        ) {

            addStatusButton(
                    "🚕 START RIDE",
                    "IN_PROGRESS"
            );

        } else if (
                "IN_PROGRESS".equals(status)
        ) {

            addStatusButton(
                    "🏁 FINISHED TRIP",
                    "COMPLETED"
            );
        }

        Button chat =
                new Button(this);

        chat.setText(
                "💬 CHAT WITH PASSENGER"
        );

        chat.setTextSize(17);

        chat.setOnClickListener(
                v -> openChat()
        );

        rideStatusContainer.addView(chat);

        Button map =
                new Button(this);

        map.setText("🗺 LIVE MAP");
        map.setTextSize(17);

        map.setOnClickListener(
                v -> openMap()
        );

        rideStatusContainer.addView(map);
    }

    private void addStatusButton(
            String text,
            String newStatus
    ) {

        Button button =
                new Button(this);

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

    private void updateRideStatus(
            String newStatus
    ) {

        if (user == null
                || db == null
                || currentRideId.isEmpty()) {

            Toast.makeText(
                    this,
                    "No current ride.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        final String rideId =
                currentRideId;

        db.collection("rides")
                .document(rideId)
                .update(
                        "status",
                        newStatus,

                        "statusUpdatedAt",
                        FieldValue.serverTimestamp()
                )
                .addOnSuccessListener(v -> {

                    if (rideId.equals(
                            currentRideId
                    )) {

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
                                    + safe(e.getMessage()),
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

    private boolean isActive(
            String status
    ) {

        if (status == null) {
            return false;
        }

        for (String active :
                ACTIVE_STATUSES) {

            if (active.equals(status)) {
                return true;
            }
        }

        return false;
    }

    private String safe(
            String value
    ) {

        return value == null
                ? ""
                : value;
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
                "rideId",
                currentRideId
        );

        startActivity(intent);
    }

    /*
     * SAFER LOCATION START
     *
     * The previous version could fail during driver startup on a device
     * where a location provider was unavailable.
     */
    private void startLocationUpdatesSafely() {

        if (locationStarted) {
            return;
        }

        try {

            locationManager =
                    (LocationManager)
                            getSystemService(
                                    LOCATION_SERVICE
                            );

            if (locationManager == null) {
                return;
            }

            boolean fineGranted =
                    ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED;

            boolean coarseGranted =
                    ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED;

            if (!fineGranted && !coarseGranted) {

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

                            try {

                                driverLatitude =
                                        location.getLatitude();

                                driverLongitude =
                                        location.getLongitude();

                                updateDriverLocation();

                            } catch (Exception ignored) {
                            }
                        }
                    };

            /*
             * Check each provider before requesting updates.
             */
            boolean gpsAvailable = false;
            boolean networkAvailable = false;

            try {
                gpsAvailable =
                        locationManager.isProviderEnabled(
                                LocationManager.GPS_PROVIDER
                        );
            } catch (Exception ignored) {
            }

            try {
                networkAvailable =
                        locationManager.isProviderEnabled(
                                LocationManager.NETWORK_PROVIDER
                        );
            } catch (Exception ignored) {
            }

            if (gpsAvailable) {

                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        3000,
                        5,
                        locationListener
                );
            }

            if (networkAvailable) {

                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        5000,
                        10,
                        locationListener
                );
            }

            locationStarted =
                    gpsAvailable || networkAvailable;

        } catch (SecurityException ignored) {

            locationStarted = false;

        } catch (IllegalArgumentException ignored) {

            locationStarted = false;

        } catch (Exception ignored) {

            locationStarted = false;
        }
    }

    private void updateDriverLocation() {

        if (user == null
                || db == null
                || currentRideId.isEmpty()) {
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

        try {

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
                            NOTIFICATION_PERMISSION_REQUEST
                    );
                }
            }

        } catch (Exception ignored) {
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

            if (grantResults.length > 0
                    && grantResults[0]
                    == PackageManager.PERMISSION_GRANTED) {

                startLocationUpdatesSafely();
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

                            try {

                                if (user != null
                                        && db != null) {

                                    db.collection("users")
                                            .document(
                                                    user.getUid()
                                            )
                                            .update(
                                                    "driverOnline",
                                                    false,

                                                    "driverAvailability",
                                                    "OFFLINE",

                                                    "availabilityUpdatedAt",
                                                    FieldValue.serverTimestamp()
                                            );
                                }

                            } catch (Exception ignored) {
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

                            try {

                                if (auth != null) {
                                    auth.signOut();
                                }

                            } catch (Exception ignored) {
                            }

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
                        }
                )
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
            } catch (Exception ignored) {
            }
        }

        mainHandler.removeCallbacksAndMessages(null);

        super.onDestroy();
    }
}
