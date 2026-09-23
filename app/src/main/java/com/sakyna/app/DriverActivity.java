
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
import com.google.firebase.firestore.QuerySnapshot;
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
    private LinearLayout rideStatusContainer;

    private boolean driverOnline = false;
    private String currentRideId = "";

    private ListenerRegistration requestListener;
    private ListenerRegistration currentRideListener;

    private QuerySnapshot latestRequestSnapshot;

    private final Set<String> hiddenRequestIds = new HashSet<>();
    private final Set<String> notifiedRequestIds = new HashSet<>();

    private final Handler expiryHandler =
            new Handler(Looper.getMainLooper());

    private static final long REQUEST_EXPIRATION_MS =
            30L * 60L * 1000L;

    private static final long REQUEST_REFRESH_MS =
            30L * 1000L;

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
        listenForCurrentRide();
        startRequestExpiryChecker();

        SakayNaNotificationHelper.requestPermission(this);
    }

    private void buildScreen() {

        ScrollView scroll = new ScrollView(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 35);
        root.setBackgroundColor(Color.rgb(248, 250, 252));

        TextView title = new TextView(this);
        title.setText("🛺 SAKAY NA\nDRIVER");
        title.setTextSize(29);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(Color.rgb(0, 70, 120));
        title.setPadding(10, 20, 10, 20);
        root.addView(title);

        statusText = new TextView(this);
        statusText.setText("Loading driver status...");
        statusText.setTextSize(20);
        statusText.setGravity(Gravity.CENTER);
        statusText.setTextColor(Color.DKGRAY);
        statusText.setPadding(15, 15, 15, 20);
        root.addView(statusText);

        LinearLayout onlineRow = new LinearLayout(this);
        onlineRow.setOrientation(LinearLayout.HORIZONTAL);

        Button online = new Button(this);
        online.setText("🟢 GO ONLINE");
        online.setTextColor(Color.WHITE);
        online.setTextSize(15);
        online.setBackgroundColor(Color.rgb(0, 155, 70));
        online.setOnClickListener(v -> setDriverOnline(true));

        Button offline = new Button(this);
        offline.setText("🔴 GO OFFLINE");
        offline.setTextColor(Color.WHITE);
        offline.setTextSize(15);
        offline.setBackgroundColor(Color.rgb(205, 35, 35));
        offline.setOnClickListener(v -> setDriverOnline(false));

        LinearLayout.LayoutParams onlineParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );

        LinearLayout.LayoutParams offlineParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );

        onlineParams.setMargins(5, 5, 5, 5);
        offlineParams.setMargins(5, 5, 5, 5);

        onlineRow.addView(online, onlineParams);
        onlineRow.addView(offline, offlineParams);

        root.addView(onlineRow);

        TextView requestTitle = new TextView(this);
        requestTitle.setText("🔔 RIDE REQUESTS");
        requestTitle.setTextSize(23);
        requestTitle.setTextColor(Color.rgb(0, 70, 120));
        requestTitle.setPadding(5, 30, 5, 12);
        root.addView(requestTitle);

        requestsText = new TextView(this);
        requestsText.setText("Checking for new rides...");
        requestsText.setTextSize(17);
        requestsText.setTextColor(Color.DKGRAY);
        requestsText.setPadding(5, 5, 5, 10);
        root.addView(requestsText);

        requestContainer = new LinearLayout(this);
        requestContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(requestContainer);

        TextView currentTitle = new TextView(this);
        currentTitle.setText("🚦 CURRENT RIDE");
        currentTitle.setTextSize(23);
        currentTitle.setTextColor(Color.rgb(0, 70, 120));
        currentTitle.setPadding(5, 30, 5, 12);
        root.addView(currentTitle);

        currentRideText = new TextView(this);
        currentRideText.setText("No current ride.");
        currentRideText.setTextSize(17);
        currentRideText.setTextColor(Color.DKGRAY);
        currentRideText.setPadding(5, 10, 5, 15);
        root.addView(currentRideText);

        rideStatusContainer = new LinearLayout(this);
        rideStatusContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(rideStatusContainer);

        Button map = new Button(this);
        map.setText("🗺️ OPEN LIVE MAP");
        map.setTextSize(16);
        map.setOnClickListener(v -> openMap());
        root.addView(map);

        Button chat = new Button(this);
        chat.setText("💬 RIDE CHAT");
        chat.setTextSize(16);
        chat.setOnClickListener(v -> openChat());
        root.addView(chat);

        Button settlement = new Button(this);
        settlement.setText("💰 DRIVER SETTLEMENT");
        settlement.setTextColor(Color.WHITE);
        settlement.setTextSize(16);
        settlement.setBackgroundColor(Color.rgb(0, 120, 200));
        settlement.setOnClickListener(v ->
                startActivity(
                        new Intent(
                                this,
                                DriverSettlementActivity.class
                        )
                )
        );
        root.addView(settlement);

        Button logout = new Button(this);
        logout.setText("🚪 LOGOUT");
        logout.setTextSize(16);
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
                        ? "🟢 DRIVER ONLINE — READY FOR RIDES"
                        : "🔴 DRIVER OFFLINE"
        );

        statusText.setTextColor(
                driverOnline
                        ? Color.rgb(0, 145, 65)
                        : Color.rgb(190, 25, 25)
        );
    }

    private void listenForRideRequests() {

        if (requestListener != null) {
            requestListener.remove();
            requestListener = null;
        }

        requestListener =
                db.collection("rides")
                        .whereEqualTo("status", "REQUESTED")
                        .addSnapshotListener(
                                (snapshots, error) -> {

                                    if (error != null) {

                                        requestsText.setText(
                                                "🔴 Unable to load requests:\n"
                                                        + error.getMessage()
                                        );

                                        return;
                                    }

                                    latestRequestSnapshot = snapshots;

                                    notifyNewRideRequests(
                                            snapshots
                                    );

                                    renderRideRequests(
                                            snapshots
                                    );
                                }
                        );
    }

    private void notifyNewRideRequests(
            QuerySnapshot snapshots
    ) {

        if (!driverOnline || snapshots == null) {
            return;
        }

        Set<String> currentIds = new HashSet<>();

        for (DocumentSnapshot ride :
                snapshots.getDocuments()) {

            String rideId =
                    ride.getId();

            String status =
                    string(
                            ride,
                            "status"
                    );

            String passengerId =
                    string(
                            ride,
                            "passengerId"
                    );

            if (!"REQUESTED".equalsIgnoreCase(
                    status
            )
                    || passengerId.isEmpty()) {
                continue;
            }

            long createdAt =
                    longValue(
                            ride,
                            "createdAt"
                    );

            if (createdAt > 0
                    && System.currentTimeMillis()
                    - createdAt
                    >= REQUEST_EXPIRATION_MS) {
                continue;
            }

            currentIds.add(
                    rideId
            );

            if (notifiedRequestIds.contains(
                    rideId
            )) {
                continue;
            }

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

            String fare =
                    formatFare(
                            ride.get("fare")
                    );

            String payment =
                    string(
                            ride,
                            "paymentMethod"
                    );

            if (payment.isEmpty()) {

                payment =
                        string(
                                ride,
                                "payment"
                        );
            }

            String message =
                    "📍 "
                            + pickup
                            + " → "
                            + destination
                            + "\n💰 "
                            + fare
                            + "\n💳 "
                            + (
                            payment.isEmpty()
                                    ? "Not specified"
                                    : payment
                    );

            SakayNaNotificationHelper.show(
                    this,
                    Math.abs(
                            (
                                    "REQUEST:"
                                            + rideId
                            ).hashCode()
                    ),
                    "🛺 New Sakay Na Ride Request",
                    message
            );

            notifiedRequestIds.add(
                    rideId
            );
        }

        notifiedRequestIds.retainAll(
                currentIds
        );
    }

    private void startRequestExpiryChecker() {

        expiryHandler.removeCallbacksAndMessages(null);

        expiryHandler.postDelayed(
                new Runnable() {

                    @Override
                    public void run() {

                        renderRideRequests(
                                latestRequestSnapshot
                        );

                        expiryHandler.postDelayed(
                                this,
                                REQUEST_REFRESH_MS
                        );
                    }
                },
                REQUEST_REFRESH_MS
        );
    }

    private void renderRideRequests(
            QuerySnapshot snapshots
    ) {

        if (requestContainer == null
                || requestsText == null) {
            return;
        }

        requestContainer.removeAllViews();

        if (snapshots == null
                || snapshots.isEmpty()) {

            requestsText.setText(
                    driverOnline
                            ? "🟢 No new ride requests."
                            : "🔴 OFFLINE"
            );

            return;
        }

        long now =
                System.currentTimeMillis();

        int count = 0;

        for (DocumentSnapshot ride :
                snapshots.getDocuments()) {

            String rideId =
                    ride.getId();

            if (hiddenRequestIds.contains(
                    rideId
            )) {
                continue;
            }

            String status =
                    string(
                            ride,
                            "status"
                    );

            if (!"REQUESTED".equalsIgnoreCase(
                    status
            )) {
                continue;
            }

            String passengerId =
                    string(
                            ride,
                            "passengerId"
                    );

            if (passengerId.isEmpty()) {
                continue;
            }

            if (!currentRideId.isEmpty()
                    && currentRideId.equals(
                    rideId
            )) {
                continue;
            }

            long createdAt =
                    longValue(
                            ride,
                            "createdAt"
                    );

            if (createdAt > 0
                    && now - createdAt
                    >= REQUEST_EXPIRATION_MS) {
                continue;
            }

            count++;

            addRideCard(
                    ride,
                    rideId
            );
        }

        if (count == 0) {

            requestsText.setText(
                    driverOnline
                            ? "🟢 No new ride requests.\n"
                            + "⏱️ Old requests expire after 30 minutes."
                            : "🔴 OFFLINE"
            );

        } else {

            requestsText.setText(
                    driverOnline
                            ? "🟢 NEW RIDE REQUESTS: "
                            + count
                            : "🔴 OFFLINE\n"
                            + count
                            + " waiting"
            );
        }
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
                22,
                22,
                22,
                22
        );

        card.setBackgroundColor(
                Color.WHITE
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(
                0,
                10,
                0,
                15
        );

        card.setLayoutParams(params);

        TextView title =
                new TextView(this);

        title.setText(
                "🔔 NEW RIDE REQUEST"
        );

        title.setTextSize(21);
        title.setTextColor(
                Color.rgb(0, 90, 150)
        );

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

        if (payment.isEmpty()) {

            payment =
                    string(
                            ride,
                            "payment"
                    );
        }

        Object fareObject =
                ride.get("fare");

        String fare =
                formatFare(
                        fareObject
                );

        TextView details =
                new TextView(this);

        details.setText(
                "\n📍 PICKUP\n"
                        + pickup
                        + "\n\n"
                        + "🏁 DESTINATION\n"
                        + destination
                        + "\n\n"
                        + "💰 FARE\n"
                        + fare
                        + "\n\n"
                        + "💳 PAYMENT\n"
                        + (
                        payment.isEmpty()
                                ? "Not specified"
                                : payment
                )
        );

        details.setTextSize(17);
        details.setTextColor(
                Color.rgb(45, 45, 45)
        );

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
                Color.rgb(0, 155, 70)
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
                Color.rgb(205, 35, 35)
        );

        accept.setOnClickListener(v -> {

            if (!driverOnline) {

                Toast.makeText(
                        this,
                        "🔴 Go ONLINE first.",
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

    private void acceptRide(
            String rideId,
            LinearLayout card
    ) {

        hiddenRequestIds.add(
                rideId
        );

        card.setVisibility(
                LinearLayout.GONE
        );

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(profile -> {

                    Map<String, Object> update =
                            buildDriverRideUpdate(
                                    profile
                            );

                    update.put(
                            "status",
                            "ACCEPTED"
                    );

                    update.put(
                            "acceptedAt",
                            System.currentTimeMillis()
                    );

                    db.collection("rides")
                            .document(rideId)
                            .update(update)
                            .addOnSuccessListener(v -> {

                                currentRideId =
                                        rideId;

                                Toast.makeText(
                                        this,
                                        "✅ Ride accepted!",
                                        Toast.LENGTH_SHORT
                                ).show();

                                listenForCurrentRide();
                                listenForRideRequests();
                            })
                            .addOnFailureListener(e -> {

                                hiddenRequestIds.remove(
                                        rideId
                                );

                                card.setVisibility(
                                        LinearLayout.VISIBLE
                                );

                                Toast.makeText(
                                        this,
                                        "Unable to accept ride:\n"
                                                + e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();
                            });
                })
                .addOnFailureListener(e -> {

                    hiddenRequestIds.remove(
                            rideId
                    );

                    card.setVisibility(
                            LinearLayout.VISIBLE
                    );

                    Toast.makeText(
                            this,
                            "Unable to load driver profile:\n"
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private Map<String, Object> buildDriverRideUpdate(
            DocumentSnapshot profile
    ) {

        Map<String, Object> update =
                new HashMap<>();

        update.put(
                "driverId",
                user.getUid()
        );

        String driverName =
                profile.getString(
                        "driverName"
                );

        String driverPhone =
                profile.getString(
                        "phone"
                );

        String plate =
                profile.getString(
                        "plateNumber"
                );

        String vehicle =
                profile.getString(
                        "vehicleDescription"
                );

        update.put(
                "driverName",
                driverName == null
                        ? ""
                        : driverName
        );

        update.put(
                "driverPhone",
                driverPhone == null
                        ? ""
                        : driverPhone
        );

        update.put(
                "driverPlateNumber",
                plate == null
                        ? ""
                        : plate
        );

        update.put(
                "driverVehicle",
                vehicle == null
                        ? ""
                        : vehicle
        );

        return update;
    }

    private void declineRide(
            String rideId,
            LinearLayout card
    ) {

        hiddenRequestIds.add(
                rideId
        );

        card.setVisibility(
                LinearLayout.GONE
        );

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
                .addOnSuccessListener(v ->
                        Toast.makeText(
                                this,
                                "Ride declined.",
                                Toast.LENGTH_SHORT
                        ).show()
                )
                .addOnFailureListener(e -> {

                    hiddenRequestIds.remove(
                            rideId
                    );

                    card.setVisibility(
                            LinearLayout.VISIBLE
                    );

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

        if (currentRideId.isEmpty()) {

            currentRideText.setText(
                    "No current ride."
            );

            clearRideStatusButtons();

            return;
        }

        currentRideListener =
                db.collection("rides")
                        .document(currentRideId)
                        .addSnapshotListener(
                                (ride, error) -> {

                                    if (error != null) {

                                        currentRideText.setText(
                                                "🔴 Unable to load current ride:\n"
                                                        + error.getMessage()
                                        );

                                        return;
                                    }

                                    if (ride == null
                                            || !ride.exists()) {

                                        currentRideText.setText(
                                                "No current ride."
                                        );

                                        clearRideStatusButtons();

                                        return;
                                    }

                                    String status =
                                            string(
                                                    ride,
                                                    "status"
                                            );

                                    if (!isActive(status)
                                            && "COMPLETED".equalsIgnoreCase(
                                            status
                                    )) {

                                        currentRideText.setText(
                                                "✅ TRIP FINISHED"
                                        );

                                        clearRideStatusButtons();

                                        return;
                                    }

                                    showCurrentRide(
                                            ride,
                                            status
                                    );
                                }
                        );
    }

    private void showCurrentRide(
            DocumentSnapshot ride,
            String status
    ) {

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

        if (payment.isEmpty()) {
            payment =
                    string(
                            ride,
                            "payment"
                    );
        }

        String fare =
                formatFare(
                        ride.get("fare")
                );

        currentRideText.setText(
                "🚕 ACTIVE RIDE\n\n"
                        + "📍 PICKUP\n"
                        + pickup
                        +
