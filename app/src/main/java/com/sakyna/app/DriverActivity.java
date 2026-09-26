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

    private Button onlineButton;
    private Button offlineButton;

    private boolean driverOnline = false;
    private boolean driverApproved = false;
    private boolean driverSuspended = false;

    private double driverSettlementBalance = 0.0;

    private long suspensionDeadlineAt = 0L;

    private String currentRideId = "";

    private ListenerRegistration requestListener;
    private ListenerRegistration currentRideListener;

    private QuerySnapshot latestRequestSnapshot;

    private final Set<String> hiddenRequestIds =
            new HashSet<>();

    private final Set<String> notifiedRequestIds =
            new HashSet<>();

    private final Handler expiryHandler =
            new Handler(Looper.getMainLooper());

    /*
     * REQUESTED rides older than 30 minutes are no longer valid.
     */
    private static final long REQUEST_EXPIRATION_MS =
            30L * 60L * 1000L;

    private static final long REQUEST_REFRESH_MS =
            30L * 1000L;

    private static final int UNPAID_DUE_DAYS = 7;

    private static final long ONE_DAY_MS =
            24L * 60L * 60L * 1000L;

    private static final double PLATFORM_FEE_RATE =
            0.10;

    private LocationManager locationManager;
    private LocationListener locationListener;

    private static final int LOCATION_PERMISSION = 2001;

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {
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
        restoreCurrentRide();
        startLocationUpdates();
        startRequestExpiryChecker();

        SakayNaNotificationHelper
                .requestPermission(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (db != null && user != null) {
            loadDriverStatus();
            restoreCurrentRide();
        }
    }

    private void buildScreen() {

        ScrollView scroll =
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
                35
        );

        root.setBackgroundColor(
                Color.rgb(248, 250, 252)
        );

        TextView title =
                new TextView(this);

        title.setText(
                "🛺 SAKAY NA\nDRIVER"
        );

        title.setTextSize(29);

        title.setGravity(
                Gravity.CENTER
        );

        title.setTextColor(
                Color.rgb(0, 70, 120)
        );

        title.setPadding(
                10,
                20,
                10,
                20
        );

        root.addView(title);

        statusText =
                new TextView(this);

        statusText.setText(
                "Loading driver status..."
        );

        statusText.setTextSize(20);

        statusText.setGravity(
                Gravity.CENTER
        );

        statusText.setTextColor(
                Color.DKGRAY
        );

        statusText.setPadding(
                15,
                15,
                15,
                20
        );

        root.addView(statusText);

        LinearLayout onlineRow =
                new LinearLayout(this);

        onlineRow.setOrientation(
                LinearLayout.HORIZONTAL
        );

        onlineButton =
                new Button(this);

        onlineButton.setText(
                "🟢 GO ONLINE"
        );

        onlineButton.setTextColor(
                Color.WHITE
        );

        onlineButton.setTextSize(15);

        onlineButton.setBackgroundColor(
                Color.rgb(0, 155, 70)
        );

        onlineButton.setEnabled(false);

        onlineButton.setOnClickListener(
                v -> setDriverOnline(true)
        );

        offlineButton =
                new Button(this);

        offlineButton.setText(
                "🔴 GO OFFLINE"
        );

        offlineButton.setTextColor(
                Color.WHITE
        );

        offlineButton.setTextSize(15);

        offlineButton.setBackgroundColor(
                Color.rgb(205, 35, 35)
        );

        offlineButton.setEnabled(false);

        offlineButton.setOnClickListener(
                v -> setDriverOnline(false)
        );

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

        onlineRow.addView(
                onlineButton,
                onlineParams
        );

        onlineRow.addView(
                offlineButton,
                offlineParams
        );

        root.addView(onlineRow);

        TextView requestTitle =
                new TextView(this);

        requestTitle.setText(
                "🔔 RIDE REQUESTS"
        );

        requestTitle.setTextSize(23);

        requestTitle.setTextColor(
                Color.rgb(0, 70, 120)
        );

        requestTitle.setPadding(
                5,
                30,
                5,
                12
        );

        root.addView(requestTitle);

        requestsText =
                new TextView(this);

        requestsText.setText(
                "Checking for new rides..."
        );

        requestsText.setTextSize(17);

        requestsText.setTextColor(
                Color.DKGRAY
        );

        requestsText.setPadding(
                5,
                5,
                5,
                10
        );

        root.addView(requestsText);

        requestContainer =
                new LinearLayout(this);

        requestContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        root.addView(requestContainer);

        TextView currentTitle =
                new TextView(this);

        currentTitle.setText(
                "🚦 CURRENT RIDE"
        );

        currentTitle.setTextSize(23);

        currentTitle.setTextColor(
                Color.rgb(0, 70, 120)
        );

        currentTitle.setPadding(
                5,
                30,
                5,
                12
        );

        root.addView(currentTitle);

        currentRideText =
                new TextView(this);

        currentRideText.setText(
                "No current ride."
        );

        currentRideText.setTextSize(17);

        currentRideText.setTextColor(
                Color.DKGRAY
        );

        currentRideText.setPadding(
                5,
                10,
                5,
                15
        );

        root.addView(currentRideText);

        rideStatusContainer =
                new LinearLayout(this);

        rideStatusContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        root.addView(rideStatusContainer);

        Button map =
                new Button(this);

        map.setText(
                "🗺️ OPEN LIVE MAP"
        );

        map.setTextSize(16);

        map.setOnClickListener(
                v -> openMap()
        );

        root.addView(map);

        Button chat =
                new Button(this);

        chat.setText(
                "💬 RIDE CHAT"
        );

        chat.setTextSize(16);

        chat.setOnClickListener(
                v -> openChat()
        );

        root.addView(chat);

        Button settlement =
                new Button(this);

        settlement.setText(
                "💰 DRIVER SETTLEMENT"
        );

        settlement.setTextColor(
                Color.WHITE
        );

        settlement.setTextSize(16);

        settlement.setBackgroundColor(
                Color.rgb(0, 120, 200)
        );

        settlement.setOnClickListener(
                v -> startActivity(
                        new Intent(
                                this,
                                DriverSettlementActivity.class
                        )
                )
        );

        root.addView(settlement);

        Button gcashPayment =
                new Button(this);

        gcashPayment.setText(
                "💳 PAY SAKAY NA VIA GCASH"
        );

        gcashPayment.setTextSize(16);

        gcashPayment.setTextColor(
                Color.WHITE
        );

        gcashPayment.setBackgroundColor(
                Color.rgb(0, 120, 200)
        );

        gcashPayment.setOnClickListener(
                v -> showGcashQr()
        );

        root.addView(gcashPayment);

        Button logout =
                new Button(this);

        logout.setText(
                "🚪 LOGOUT"
        );

        logout.setTextSize(16);

        logout.setOnClickListener(
                v -> logout()
        );

        root.addView(logout);

        scroll.addView(root);

        setContentView(scroll);
    }

    private void loadDriverStatus() {

        if (user == null) {
            return;
        }

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(profile -> {

                    checkUnpaidDues(
                            profile,
                            () -> {

                                driverApproved =
                                        profile.exists()
                                                && Boolean.TRUE.equals(
                                                profile.getBoolean(
                                                        "approved"
                                                )
                                        )
                                                && "APPROVED".equalsIgnoreCase(
                                                string(
                                                        profile,
                                                        "driverStatus"
                                                )
                                        )
                                                && Boolean.TRUE.equals(
                                                profile.getBoolean(
                                                        "canAcceptRides"
                                                )
                                        );

                                if (!driverApproved
                                        || driverSuspended) {

                                    driverOnline = false;

                                    db.collection("drivers")
                                            .document(user.getUid())
                                            .set(
                                                    buildOfflineData(),
                                                    SetOptions.merge()
                                            );

                                } else {
                                    loadOnlineStatus();
                                }

                                updateStatusText();
                                updateOnlineButtons();

                                listenForRideRequests();
                            }
                    );
                })
                .addOnFailureListener(e -> {

                    driverApproved = false;
                    driverSuspended = false;
                    driverOnline = false;

                    updateStatusText();
                    updateOnlineButtons();

                    listenForRideRequests();
                });
    }

    private void checkUnpaidDues(
            DocumentSnapshot profile,
            Runnable afterCheck
    ) {

        if (user == null) {
            afterCheck.run();
            return;
        }

        db.collection("rides")
                .whereEqualTo(
                        "driverId",
                        user.getUid()
                )
                .whereEqualTo(
                        "driverDuesStatus",
                        "DUE"
                )
                .get()
                .addOnSuccessListener(dues -> {

                    long now =
                            System.currentTimeMillis();

                    long oldestDueAt = 0L;
                    double totalDue = 0.0;

                    for (
                            DocumentSnapshot ride :
                            dues.getDocuments()
                    ) {

                        long dueCreatedAt =
                                longValue(
                                        ride,
                                        "driverDuesCreatedAt"
                                );

                        if (dueCreatedAt <= 0L) {

                            dueCreatedAt =
                                    longValue(
                                            ride,
                                            "acceptedAt"
                                    );
                        }

                        if (dueCreatedAt > 0L
                                && (
                                oldestDueAt == 0L
                                        || dueCreatedAt
                                        < oldestDueAt
                        )) {

                            oldestDueAt =
                                    dueCreatedAt;
                        }

                        totalDue +=
                                getDriverDue(ride);
                    }

                    totalDue =
                            roundMoney(totalDue);

                    driverSettlementBalance =
                            totalDue;

                    if (oldestDueAt == 0L) {

                        suspensionDeadlineAt = 0L;

                        String accountStatus =
                                string(
                                        profile,
                                        "driverAccountStatus"
                                );

                        String suspensionReason =
                                string(
                                        profile,
                                        "suspensionReason"
                                );

                        if (
                                "SUSPENDED".equalsIgnoreCase(
                                        accountStatus
                                )
                                        &&
                                "UNPAID_DUES".equalsIgnoreCase(
                                        suspensionReason
                                )
                        ) {

                            driverSuspended = false;

                            Map<String, Object> restore =
                                    new HashMap<>();

                            restore.put(
                                    "driverAccountStatus",
                                    "ACTIVE"
                            );

                            restore.put(
                                    "suspensionReason",
                                    ""
                            );

                            restore.put(
                                    "suspensionDeadlineAt",
                                    null
                            );

                            restore.put(
                                    "suspendedAt",
                                    null
                            );

                            restore.put(
                                    "driverSettlementBalance",
                                    0.0
                            );

                            db.collection("users")
                                    .document(user.getUid())
                                    .set(
                                            restore,
                                            SetOptions.merge()
                                    );

                        } else {

                            driverSuspended =
                                    "SUSPENDED".equalsIgnoreCase(
                                            accountStatus
                                    );
                        }

                        afterCheck.run();
                        return;
                    }

                    suspensionDeadlineAt =
                            oldestDueAt
                                    + (
                                    UNPAID_DUE_DAYS
                                            * ONE_DAY_MS
                    );

                    boolean overdue =
                            now >= suspensionDeadlineAt;

                    if (overdue) {

                        driverSuspended = true;
                        driverOnline = false;

                        Map<String, Object> suspension =
                                new HashMap<>();

                        suspension.put(
                                "driverAccountStatus",
                                "SUSPENDED"
                        );

                        suspension.put(
                                "suspensionReason",
                                "UNPAID_DUES"
                        );

                        suspension.put(
                                "suspendedAt",
                                now
                        );

                        suspension.put(
                                "suspensionDeadlineAt",
                                suspensionDeadlineAt
                        );

                        suspension.put(
                                "driverSettlementBalance",
                                totalDue
                        );

                        db.collection("users")
                                .document(user.getUid())
                                .set(
                                        suspension,
                                        SetOptions.merge()
                                );

                        db.collection("drivers")
                                .document(user.getUid())
                                .set(
                                        buildOfflineData(),
                                        SetOptions.merge()
                                );

                        SakayNaNotificationHelper.show(
                                this,
                                7301,
                                "🚫 Sakay Na Driver Suspended",
                                "Your driver account is suspended because of unpaid platform dues of ₱"
                                        + String.format(
                                        java.util.Locale.US,
                                        "%.2f",
                                        totalDue
                                )
                                        + ". Full payment must be verified by Admin before you can go ONLINE."
                        );

                    } else {

                        driverSuspended = false;

                        Map<String, Object> reminder =
                                new HashMap<>();

                        reminder.put(
                                "driverAccountStatus",
                                "ACTIVE"
                        );

                        reminder.put(
                                "suspensionReason",
                                ""
                        );

                        reminder.put(
                                "suspensionDeadlineAt",
                                suspensionDeadlineAt
                        );

                        reminder.put(
                                "driverSettlementBalance",
                                totalDue
                        );

                        db.collection("users")
                                .document(user.getUid())
                                .set(
                                        reminder,
                                        SetOptions.merge()
                                );

                        long remaining =
                                suspensionDeadlineAt - now;

                        if (
                                remaining > 0L
                                        &&
                                remaining <= ONE_DAY_MS
                        ) {

                            SakayNaNotificationHelper.show(
                                    this,
                                    7300,
                                    "⚠️ Sakay Na Settlement Warning",
                                    "You have ₱"
                                            + String.format(
                                            java.util.Locale.US,
                                            "%.2f",
                                            totalDue
                                    )
                                            + " in unpaid platform dues. Your driver account will be suspended when the 7-day deadline passes."
                            );
                        }
                    }

                    afterCheck.run();
                })
                .addOnFailureListener(e ->
                        afterCheck.run()
                );
    }

    private double getDriverDue(
            DocumentSnapshot ride
    ) {

        Object stored =
                ride.get("driverDuesAmount");

        if (stored instanceof Number) {

            return roundMoney(
                    ((Number) stored).doubleValue()
            );
        }

        double fare = getFare(ride);

        return roundMoney(
                fare * PLATFORM_FEE_RATE
        );
    }

    private double getFare(
            DocumentSnapshot document
    ) {

        Object value =
                document.get("fare");

        if (value == null) {
            return 0.0;
        }

        if (value instanceof Number) {

            return ((Number) value).doubleValue();
        }

        try {

            return Double.parseDouble(
                    String.valueOf(value)
            );

        } catch (Exception e) {

            return 0.0;
        }
    }

    private double roundMoney(
            double amount
    ) {

        return Math.round(
                amount * 100.0
        ) / 100.0;
    }

    private void loadOnlineStatus() {

        db.collection("drivers")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {

                    if (driverSuspended) {

                        driverOnline = false;

                    } else {

                        driverOnline =
                                doc.exists()
                                        && Boolean.TRUE.equals(
                                        doc.getBoolean("online")
                                );
                    }

                    updateStatusText();
                    updateOnlineButtons();
                    listenForRideRequests();
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
                "driverId",
                user.getUid()
        );

        data.put(
                "online",
                false
        );

        data.put(
                "updatedAt",
                System.currentTimeMillis()
        );

        return data;
    }

    private void updateOnlineButtons() {

        if (onlineButton == null
                || offlineButton == null) {
            return;
        }

        if (!driverApproved
                || driverSuspended) {

            onlineButton.setEnabled(false);
            offlineButton.setEnabled(false);

            return;
        }

        onlineButton.setEnabled(
                !driverOnline
        );

        offlineButton.setEnabled(
                driverOnline
        );
    }

    private void setDriverOnline(
            boolean online
    ) {

        if (online && driverSuspended) {

            driverOnline = false;

            updateStatusText();
            updateOnlineButtons();

            Toast.makeText(
                    this,
                    "🚫 Your driver account is suspended for unpaid dues. Please pay the full balance and wait for Admin verification.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (online && !driverApproved) {

            driverOnline = false;

            updateStatusText();
            updateOnlineButtons();

            Toast.makeText(
                    this,
                    "⏳ Your driver account is waiting for Admin approval.",
                    Toast.LENGTH_LONG
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
                "online",
                online
        );

        data.put(
                "updatedAt",
                System.currentTimeMillis()
        );

        statusText.setText(
                online
                        ? "⏳ GOING ONLINE..."
                        : "⏳ GOING OFFLINE..."
        );

        db.collection("drivers")
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
                    updateOnlineButtons();

                    Toast.makeText(
                            this,
                            "Unable to change status:\n"
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void updateStatusText() {

        if (driverSuspended) {

            String balance =
                    String.format(
                            java.util.Locale.US,
                            "%.2f",
                            driverSettlementBalance
                    );

            statusText.setText(
                    "🚫 DRIVER ACCOUNT SUSPENDED\n"
                            + "UNPAID PLATFORM DUES: ₱"
                            + balance
                            + "\nFull payment must be verified by Admin before you can go ONLINE."
            );

            statusText.setTextColor(
                    Color.rgb(180, 0, 0)
            );

            return;
        }

        if (!driverApproved) {

            statusText.setText(
                    "⏳ DRIVER APPROVAL PENDING\n"
                            + "Admin approval is required before going ONLINE."
            );

            statusText.setTextColor(
                    Color.rgb(190, 90, 0)
            );

            return;
        }

        if (driverSettlementBalance > 0.0) {

            statusText.setText(
                    (
                            driverOnline
                                    ? "🟢 DRIVER ONLINE — READY FOR RIDES"
                                    : "🔴 DRIVER OFFLINE"
                    )
                            + "\n💰 Unpaid platform dues: ₱"
                            + String.format(
                            java.util.Locale.US,
                            "%.2f",
                            driverSettlementBalance
                    )
            );

            statusText.setTextColor(
                    driverOnline
                            ? Color.rgb(0, 145, 65)
                            : Color.rgb(190, 25, 25)
            );

            return;
        }

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

    /*
     * DRIVER REQUEST LIST
     *
     * Only REQUESTED rides are considered.
     * Old REQUESTED rides are expired automatically.
     * If the driver already has an active ride, no additional
     * booking request is displayed.
     */
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

                                    expireOldRequestedRides(
                                            snapshots
                                    );

                                    notifyNewRideRequests(
                                            snapshots
                                    );

                                    renderRideRequests(
                                            snapshots
                                    );
                                }
                        );
    }

    /*
     * Automatically removes stale REQUESTED bookings from the
     * live booking queue without deleting the Firestore document.
     *
     * The ride becomes EXPIRED so PassengerActivity can no longer
     * treat it as a current REQUESTED ride.
     */
    private void expireOldRequestedRides(
            QuerySnapshot snapshots
    ) {

        if (snapshots == null) {
            return;
        }

        long now =
                System.currentTimeMillis();

        for (
                DocumentSnapshot ride :
                snapshots.getDocuments()
        ) {

            String rideId =
                    ride.getId();

            long createdAt =
                    longValue(
                            ride,
                            "createdAt"
                    );

            if (createdAt <= 0L) {
                continue;
            }

            if (
                    now - createdAt
                            < REQUEST_EXPIRATION_MS
            ) {
                continue;
            }

            db.runTransaction(
                            transaction -> {

                                DocumentSnapshot current =
                                        transaction.get(
                                                db.collection("rides")
                                                        .document(rideId)
                                );

                                String status =
                                        string(
                                                current,
                                                "status"
                                        );

                                long currentCreatedAt =
                                        longValue(
                                                current,
                                                "createdAt"
                                        );

                                if (
                                        "REQUESTED".equalsIgnoreCase(
                                                status
                                        )
                                                &&
                                        currentCreatedAt > 0L
                                                &&
                                        System.currentTimeMillis()
                                                - currentCreatedAt
                                                >= REQUEST_EXPIRATION_MS
                                ) {

                                    Map<String, Object> update =
                                            new HashMap<>();

                                    update.put(
                                            "status",
                                            "EXPIRED"
                                    );

                                    update.put(
                                            "expiredAt",
                                            System.currentTimeMillis()
                                    );

                                    update.put(
                                            "expirationReason",
                                            "REQUEST_TIMEOUT"
                                    );

                                    transaction.update(
                                            db.collection("rides")
                                                    .document(rideId),
                                            update
                                    );
                                }

                                return null;
                            }
                    )
                    .addOnFailureListener(
                            ignored -> {
                                // Another update may have won the race.
                            }
                    );
        }
    }

    private void notifyNewRideRequests(
            QuerySnapshot snapshots
    ) {

        if (!driverOnline
                || !driverApproved
                || driverSuspended
                || !currentRideId.isEmpty()
                || snapshots == null) {

            return;
        }

        Set<String> currentIds =
                new HashSet<>();

        for (
                DocumentSnapshot ride :
                snapshots.getDocuments()
        ) {

            String rideId =
                    ride.getId();

            String status =
                    string(ride, "status");

            String passengerId =
                    string(ride, "passengerId");

            if (!"REQUESTED".equalsIgnoreCase(status)
                    || passengerId.isEmpty()) {
                continue;
            }

            long createdAt =
                    longValue(ride, "createdAt");

            if (createdAt > 0
                    && System.currentTimeMillis()
                    - createdAt
                    >= REQUEST_EXPIRATION_MS) {
                continue;
            }

            currentIds.add(rideId);

            if (notifiedRequestIds.contains(rideId)) {
                continue;
            }

            String pickup =
                    placeName(ride, "pickup");

            String destination =
                    placeName(ride, "destination");

            String fare =
                    formatFare(ride.get("fare"));

            String payment =
                    string(ride, "paymentMethod");

            if (payment.isEmpty()) {
                payment =
                        string(ride, "payment");
            }

            String message =
                    "🛺 Passenger: "
                            + passengerNameFromRide(ride)
                            + "\n📍 "
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
                            ("REQUEST:" + rideId)
                                    .hashCode()
                    ),
                    "🛺 New Sakay Na Ride Request",
                    message
            );

            notifiedRequestIds.add(rideId);
        }

        notifiedRequestIds.retainAll(currentIds);
    }

    private void startRequestExpiryChecker() {

        expiryHandler.removeCallbacksAndMessages(null);

        expiryHandler.postDelayed(
                new Runnable() {

                    @Override
                    public void run() {

                        expireOldRequestedRides(
                                latestRequestSnapshot
                        );

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

        if (driverSuspended) {

            requestsText.setText(
                    "🚫 DRIVER SUSPENDED\n"
                            + "New ride requests are unavailable until full settlement is verified by Admin."
            );

            return;
        }

        if (!driverApproved) {

            requestsText.setText(
                    "⏳ DRIVER APPROVAL PENDING\n"
                            + "Ride requests are unavailable until Admin approval."
            );

            return;
        }

        /*
         * One driver handles one active ride at a time.
         */
        if (!currentRideId.isEmpty()) {

            requestsText.setText(
                    "🚦 CURRENT RIDE ACTIVE\n"
                            + "Finish the current ride before accepting another booking."
            );

            return;
        }

        /*
         * Offline means no booking queue is displayed.
         */
        if (!driverOnline) {

            requestsText.setText(
                    "🔴 OFFLINE\n"
                            + "Go ONLINE to receive ride requests."
            );

            return;
        }

        if (snapshots == null
                || snapshots.isEmpty()) {

            requestsText.setText(
                    "🟢 No new ride requests."
            );

            return;
        }

        long now =
                System.currentTimeMillis();

        int count = 0;

        for (
                DocumentSnapshot ride :
                snapshots.getDocuments()
        ) {

            String rideId =
                    ride.getId();

            if (hiddenRequestIds.contains(rideId)) {
                continue;
            }

            String status =
                    string(ride, "status");

            if (!"REQUESTED".equalsIgnoreCase(status)) {
                continue;
            }

            String passengerId =
                    string(ride, "passengerId");

            if (passengerId.isEmpty()) {
                continue;
            }

            long createdAt =
                    longValue(ride, "createdAt");

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
                    "🟢 No new ride requests."
            );

        } else {

            requestsText.setText(
                    "🟢 NEW RIDE REQUESTS: "
                            + count
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
                placeName(ride, "pickup");

        String destination =
                placeName(ride, "destination");

        String payment =
                string(ride, "paymentMethod");

        if (payment.isEmpty()) {
            payment =
                    string(ride, "payment");
        }

        String fare =
                formatFare(ride.get("fare"));

        String passengerName =
                passengerNameFromRide(ride);

        if (passengerName.isEmpty()) {
            passengerName = "Passenger";
        }

        TextView details =
                new TextView(this);

        details.setText(
                "\n👤 PASSENGER\n"
                        + passengerName
                        + "\n\n👥 PASSENGERS\n"
                        + passengerCountText(ride)
                        + "\n\n📍 PICKUP\n"
                        + pickup
                        + "\n\n🏁 DESTINATION\n"
                        + destination
                        + "\n\n💰 FARE\n"
                        + fare
                        + "\n\n💳 PAYMENT\n"
                        + (
                        payment.isEmpty()
                                ? "Not specified"
                                : payment
                )
                        + "\n\n🔒 PASSENGER PHONE\n"
                        + "Protected — use Ride Chat"
        );

        details.setTextSize(17);

        details.setTextColor(
                Color.rgb(45, 45, 45)
        );

        card.addView(details);

        loadPassengerName(
                ride,
                details,
                pickup,
                destination,
                fare,
                payment
        );

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
                        && driverApproved
                        && !driverSuspended
                        && currentRideId.isEmpty()
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

        decline.setEnabled(
                driverOnline
                        && driverApproved
                        && !driverSuspended
                        && currentRideId.isEmpty()
        );

        accept.setOnClickListener(v -> {

            if (driverSuspended) {

                Toast.makeText(
                        this,
                        "🚫 Your account is suspended for unpaid dues.",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            if (!driverApproved) {

                Toast.makeText(
                        this,
                        "⏳ Admin approval is required before accepting rides.",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            if (!driverOnline) {

                Toast.makeText(
                        this,
                        "🔴 Go ONLINE first.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            if (!currentRideId.isEmpty()) {

                Toast.makeText(
                        this,
                        "🚦 You already have an active ride.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            accept.setEnabled(false);
            decline.setEnabled(false);

            acceptRide(
                    rideId,
                    ride,
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

    private void loadPassengerName(
            DocumentSnapshot ride,
            TextView details,
            String pickup,
            String destination,
            String fare,
            String payment
    ) {

        String storedName =
                passengerNameFromRide(ride);

        if (!storedName.isEmpty()
                && !"Passenger".equalsIgnoreCase(storedName)) {
            return;
        }

        String passengerId =
                string(ride, "passengerId");

        if (passengerId.isEmpty()) {
            return;
        }

        db.collection("users")
                .document(passengerId)
                .get()
                .addOnSuccessListener(profile -> {

                    String name =
                            passengerNameFromProfile(
                                    profile
                            );

                    if (name.isEmpty()) {
                        name = "Passenger";
                    }

                    final String finalName =
                            name;

                    details.setText(
                            "\n👤 PASSENGER\n"
                                    + finalName
                                    + "\n\n👥 PASSENGERS\n"
                                    + passengerCountText(ride)
                                    + "\n\n📍 PICKUP\n"
                                    + pickup
                                    + "\n\n🏁 DESTINATION\n"
                                    + destination
                                    + "\n\n💰 FARE\n"
                                    + fare
                                    + "\n\n💳 PAYMENT\n"
                                    + (
                                    payment.isEmpty()
                                            ? "Not specified"
                                            : payment
                            )
                                    + "\n\n🔒 PASSENGER PHONE\n"
                                    + "Protected — use Ride Chat"
                    );
                });
    }

    private String passengerNameFromRide(
            DocumentSnapshot ride
    ) {

        String name =
                string(
                        ride,
                        "passengerName"
                );

        if (!name.isEmpty()) {
            return name;
        }

        return "";
    }

    private String passengerNameFromProfile(
            DocumentSnapshot profile
    ) {

        String name =
                string(
                        profile,
                        "passengerName"
                );

        if (!name.isEmpty()) {
            return name;
        }

        name =
                string(
                        profile,
                        "name"
                );

        if (!name.isEmpty()) {
            return name;
        }

        return "";
    }

    /*
     * ACCEPT RIDE
     *
     * The transaction checks that the ride is STILL REQUESTED.
     * This prevents an old/stale card from accepting a ride that
     * Passenger or another driver already changed.
     */
    private void acceptRide(
            String rideId,
            DocumentSnapshot ride,
            LinearLayout card
    ) {

        if (driverSuspended) {

            card.setVisibility(
                    LinearLayout.VISIBLE
            );

            Toast.makeText(
                    this,
                    "🚫 Driver account is suspended for unpaid dues.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (!driverApproved
                || !driverOnline) {

            card.setVisibility(
                    LinearLayout.VISIBLE
            );

            Toast.makeText(
                    this,
                    "Driver must be approved and ONLINE.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (!currentRideId.isEmpty()) {

            card.setVisibility(
                    LinearLayout.VISIBLE
            );

            Toast.makeText(
                    this,
                    "🚦 You already have an active ride.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        hiddenRequestIds.add(rideId);

        card.setVisibility(
                LinearLayout.GONE
        );

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(profile -> {

                    checkUnpaidDues(
                            profile,
                            () -> {

                                if (driverSuspended) {

                                    hiddenRequestIds.remove(
                                            rideId
                                    );

                                    card.setVisibility(
                                            LinearLayout.VISIBLE
                                    );

                                    updateStatusText();
                                    updateOnlineButtons();

                                    Toast.makeText(
                                            this,
                                            "🚫 Your account is suspended for unpaid dues.",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    return;
                                }

                                boolean approvedNow =
                                        profile.exists()
                                                && Boolean.TRUE.equals(
                                                profile.getBoolean(
                                                        "approved"
                                                )
                                        )
                                                && "APPROVED".equalsIgnoreCase(
                                                string(
                                                        profile,
                                                        "driverStatus"
                                                )
                                        )
                                                && Boolean.TRUE.equals(
                                                profile.getBoolean(
                                                        "canAcceptRides"
                                                )
                                        );

                                if (!approvedNow) {

                                    driverApproved = false;
                                    driverOnline = false;

                                    db.collection("drivers")
                                            .document(user.getUid())
                                            .set(
                                                    buildOfflineData(),
                                                    SetOptions.merge()
                                            );

                                    updateStatusText();
                                    updateOnlineButtons();

                                    card.setVisibility(
                                            LinearLayout.VISIBLE
                                    );

                                    Toast.makeText(
                                            this,
                                            "⏳ Admin approval is required before accepting rides.",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    return;
                                }

                                Map<String, Object> update =
                                        buildDriverRideUpdate(
                                                profile
                                        );

                                long acceptedAt =
                                        System.currentTimeMillis();

                                double acceptedFare =
                                        getFare(ride);

                                double driverDuesAmount =
                                        roundMoney(
                                                acceptedFare
                                                        * PLATFORM_FEE_RATE
                                        );

                                update.put(
                                        "status",
                                        "ACCEPTED"
                                );

                                update.put(
                                        "acceptedAt",
                                        acceptedAt
                                );

                                update.put(
                                        "adminTransactionRecorded",
                                        true
                                );

                                update.put(
                                        "adminTransactionStatus",
                                        "RECORDED"
                                );

                                update.put(
                                        "adminTransactionRecordedAt",
                                        acceptedAt
                                );

                                update.put(
                                        "driverDuesStatus",
                                        "DUE"
                                );

                                update.put(
                                        "driverDuesAmount",
                                        driverDuesAmount
                                );

                                update.put(
                                        "driverDuesRate",
                                        PLATFORM_FEE_RATE
                                );

                                update.put(
                                        "driverDuesCreatedAt",
                                        acceptedAt
                                );

                                db.runTransaction(
                                                transaction -> {

                                                    DocumentSnapshot current =
                                                            transaction.get(
                                                                    db.collection("rides")
                                                                            .document(rideId)
                                                            );

                                                    String currentStatus =
                                                            string(
                                                                    current,
                                                                    "status"
                                                            );

                                                    long createdAt =
                                                            longValue(
                                                                    current,
                                                                    "createdAt"
                                                            );

                                                    if (
                                                            !"REQUESTED"
                                                                    .equalsIgnoreCase(
                                                                            currentStatus
                                                                    )
                                                    ) {

                                                        throw new IllegalStateException(
                                                                "This ride is no longer available."
                                                        );
                                                    }

                                                    if (
                                                            createdAt > 0L
                                                                    &&
                                                            System.currentTimeMillis()
                                                                    - createdAt
                                                                    >= REQUEST_EXPIRATION_MS
                                                    ) {

                                                        Map<String, Object> expired =
                                                                new HashMap<>();

                                                        expired.put(
                                                                "status",
                                                                "EXPIRED"
                                                        );

                                                        expired.put(
                                                                "expiredAt",
                                                                System.currentTimeMillis()
                                                        );

                                                        expired.put(
                                                                "expirationReason",
                                                                "REQUEST_TIMEOUT"
                                                        );

                                                        transaction.update(
                                                                db.collection("rides")
                                                                        .document(rideId),
                                                                expired
                                                        );

                                                        throw new IllegalStateException(
                                                                "This ride request has expired."
                                                        );
                                                    }

                                                    transaction.update(
                                                            db.collection("rides")
                                                                    .document(rideId),
                                                            update
                                                    );

                                                    return null;
                                                }
                                        )
                                        .addOnSuccessListener(v -> {

                                            currentRideId =
                                                    rideId;

                                            hiddenRequestIds.add(
                                                    rideId
                                            );

                                            Toast.makeText(
                                                    this,
                                                    "✅ Ride accepted!\n"
                                                            + "🧾 Admin transaction recorded.\n"
                                                            + "💰 Driver dues created: ₱"
                                                            + String.format(
                                                            java.util.Locale.US,
                                                            "%.2f",
                                                            driverDuesAmount
                                                    ),
                                                    Toast.LENGTH_LONG
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

                                            String message =
                                                    e.getMessage();

                                            if (message == null) {
                                                message =
                                                        "Unable to accept ride.";
                                            }

                                            Toast.makeText(
                                                    this,
                                                    message,
                                                    Toast.LENGTH_LONG
                                            ).show();

                                            listenForRideRequests();
                                        });
                            }
                    );
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
                            "Unable to verify driver approval:\n"
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
                profile.getString("driverName");

        String driverPhone =
                profile.getString("phone");

        String plate =
                profile.getString("plateNumber");

        String vehicle =
                profile.getString(
                        "vehicleDescription"
                );

        update.put(
                "driverName",
                driverName == null ? "" : driverName
        );

        update.put(
                "driverPhone",
                driverPhone == null ? "" : driverPhone
        );

        update.put(
                "driverPlateNumber",
                plate == null ? "" : plate
        );

        update.put(
                "driverVehicle",
                vehicle == null ? "" : vehicle
        );

        return update;
    }

    private void declineRide(
            String rideId,
            LinearLayout card
    ) {

        hiddenRequestIds.add(rideId);

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
                .addOnSuccessListener(v -> {

                    Toast.makeText(
                            this,
                            "Ride declined.",
                            Toast.LENGTH_SHORT
                    ).show();

                    listenForRideRequests();
                })
                .addOnFailureListener(e -> {

                    hiddenRequestIds.remove(rideId);

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

    /*
     * IMPORTANT:
     * Restore an active ride from Firestore whenever DriverActivity
     * starts/reopens.
     *
     * Previously currentRideId existed only in RAM. After reopening
     * the Activity it became empty, so ACCEPT/Map/Chat could say
     * "No active ride" even though Firestore contained ACCEPTED.
     */
    private void restoreCurrentRide() {

        if (user == null) {
            return;
        }

        db.collection("rides")
                .whereEqualTo(
                        "driverId",
                        user.getUid()
                )
                .get()
                .addOnSuccessListener(snapshot -> {

                    DocumentSnapshot newest =
                            null;

                    long newestTime = -1L;

                    for (
                            DocumentSnapshot ride :
                            snapshot.getDocuments()
                    ) {

                        String status =
                                string(
                                        ride,
                                        "status"
                                );

                        if (!isActive(status)) {
                            continue;
                        }

                        long time =
                                longValue(
                                        ride,
                                        "acceptedAt"
                                );

                        if (time <= 0L) {

                            time =
                                    longValue(
                                            ride,
                                            "statusUpdatedAt"
                                    );
                        }

                        if (time <= 0L) {

                            time =
                                    longValue(
                                            ride,
                                            "createdAt"
                                    );
                        }

                        if (
                                newest == null
                                        || time > newestTime
                        ) {

                            newest =
                                    ride;

                            newestTime =
                                    time;
                        }
                    }

                    if (newest != null) {

                        currentRideId =
                                newest.getId();

                        hiddenRequestIds.add(
                                currentRideId
                        );

                        showCurrentRide(
                                newest,
                                string(
                                        newest,
                                        "status"
                                )
                        );

                        listenForCurrentRide();

                    } else {

                        currentRideId = "";

                        currentRideText.setText(
                                "No current ride."
                        );

                        clearRideStatusButtons();

                        listenForCurrentRide();
                    }

                    renderRideRequests(
                            latestRequestSnapshot
                    );
                })
                .addOnFailureListener(e -> {

                    /*
                     * Do not invent a ride locally when Firestore
                     * cannot be read.
                     */
                    currentRideId = "";

                    currentRideText.setText(
                            "No current ride."
                    );

                    clearRideStatusButtons();
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

        final String rideId =
                currentRideId;

        currentRideListener =
                db.collection("rides")
                        .document(rideId)
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

                                        currentRideId = "";

                                        currentRideText.setText(
                                                "No current ride."
                                        );

                                        clearRideStatusButtons();

                                        listenForRideRequests();

                                        return;
                                    }

                                    String status =
                                            string(
                                                    ride,
                                                    "status"
                                            );

                                    if ("COMPLETED".equalsIgnoreCase(
                                            status
                                    )
                                            || "CANCELLED".equalsIgnoreCase(
                                            status
                                    )
                                            || "DECLINED".equalsIgnoreCase(
                                            status
                                    )
                                            || "EXPIRED".equalsIgnoreCase(
                                            status
                                    )) {

                                        currentRideId = "";

                                        if ("COMPLETED".equalsIgnoreCase(
                                                status
                                        )) {

                                            currentRideText.setText(
                                                    "✅ TRIP FINISHED"
                                            );

                                        } else {

                                            currentRideText.setText(
                                                    "No current ride."
                                            );
                                        }

                                        clearRideStatusButtons();

                                        listenForRideRequests();

                                        return;
                                    }

                                    if (!isActive(status)) {

                                        currentRideId = "";

                                        currentRideText.setText(
                                                "No current ride."
                                        );

                                        clearRideStatusButtons();

                                        listenForRideRequests();

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
                placeName(ride, "pickup");

        String destination =
                placeName(ride, "destination");

        String payment =
                string(ride, "paymentMethod");

        if (payment.isEmpty()) {
            payment =
                    string(ride, "payment");
        }

        String fare =
                formatFare(ride.get("fare"));

        String passengerName =
                passengerNameFromRide(ride);

        if (passengerName.isEmpty()) {
            passengerName = "Passenger";
        }

        currentRideText.setText(
                "🚕 ACTIVE RIDE\n\n"
                        + "👤 PASSENGER\n"
                        + passengerName
                        + "\n\n"
                        + "🔒 PHONE\n"
                        + "Protected — use Ride Chat"
                        + "\n\n"
                        + "📍 PICKUP\n"
                        + pickup
                        + "\n\n"
                        + "🏁 DESTINATION\n"
                        + destination
                        + "\n\n"
                        + "💰 FARE\n"
                        + fare
                        + "\n\n"
                        + "👥 PASSENGERS\n"
                        + passengerCountText(ride)
                        + "\n\n"
                        + "💳 PAYMENT\n"
                        + (
                        payment.isEmpty()
                                ? "Not specified"
                                : payment
                )
                        + "\n\n"
                        + "🚦 STATUS\n"
                        + status
        );

        loadCurrentRidePassengerName(
                ride,
                pickup,
                destination,
                fare,
                payment,
                status
        );

        showRideStatusButtons(status);
    }

    private void loadCurrentRidePassengerName(
            DocumentSnapshot ride,
            String pickup,
            String destination,
            String fare,
            String payment,
            String status
    ) {

        if (!passengerNameFromRide(ride).isEmpty()) {
            return;
        }

        String passengerId =
                string(ride, "passengerId");

        if (passengerId.isEmpty()) {
            return;
        }

        db.collection("users")
                .document(passengerId)
                .get()
                .addOnSuccessListener(profile -> {

                    String name =
                            passengerNameFromProfile(
                                    profile
                            );

                    if (name.isEmpty()) {
                        name = "Passenger";
                    }

                    currentRideText.setText(
                            "🚕 ACTIVE RIDE\n\n"
                                    + "👤 PASSENGER\n"
                                    + name
                                    + "\n\n"
                                    + "🔒 PHONE\n"
                                    + "Protected — use Ride Chat"
                                    + "\n\n"
                                    + "📍 PICKUP\n"
                                    + pickup
                                    + "\n\n"
                                    + "🏁 DESTINATION\n"
                                    + destination
                                    + "\n\n"
                                    + "💰 FARE\n"
                                    + fare
                                    + "\n\n"
                                    + "👥 PASSENGERS\n"
                                    + passengerCountText(ride)
                                    + "\n\n"
                                    + "💳 PAYMENT\n"
                                    + (
                                    payment.isEmpty()
                                            ? "Not specified"
                                            : payment
                            )
                                    + "\n\n"
                                    + "🚦 STATUS\n"
                                    + status
                    );
                });
    }

    private String passengerCountText(
            DocumentSnapshot ride
    ) {

        Object value =
                ride.get("passengerCount");

        if (value instanceof Number) {

            return String.valueOf(
                    ((Number) value).intValue()
            );
        }

        String valueText =
                ride.getString(
                        "passengerCount"
                );

        if (valueText != null
                && !valueText.trim().isEmpty()) {

            return valueText.trim();
        }

        return "1";
    }

    private void showRideStatusButtons(
            String status
    ) {

        rideStatusContainer.removeAllViews();

        TextView heading =
                new TextView(this);

        heading.setText(
                "🚦 RIDE STATUS"
        );

        heading.setTextSize(20);

        heading.setTextColor(
                Color.rgb(0, 70, 120)
        );

        heading.setPadding(
                5,
                10,
                5,
                10
        );

        rideStatusContainer.addView(
                heading
        );

        String normalizedStatus =
                status == null
                        ? ""
                        : status.trim().toUpperCase();

        Button onTheWay =
                new Button(this);

        onTheWay.setText(
                "🚗 DRIVER ON THE WAY"
        );

        onTheWay.setTextColor(
                Color.WHITE
        );

        onTheWay.setBackgroundColor(
                Color.rgb(255, 140, 0)
        );

        Button arrived =
                new Button(this);

        arrived.setText(
                "📍 I HAVE ARRIVED"
        );

        arrived.setTextColor(
                Color.WHITE
        );

        arrived.setBackgroundColor(
                Color.rgb(0, 120, 200)
        );

        Button start =
                new Button(this);

        start.setText(
                "▶️ START RIDE"
        );

        start.setTextColor(
                Color.WHITE
        );

        start.setBackgroundColor(
                Color.rgb(0, 145, 75)
        );

        Button finish =
                new Button(this);

        finish.setText(
                "🏁 FINISHED TRIP"
        );

        finish.setTextColor(
                Color.WHITE
        );

        finish.setBackgroundColor(
                Color.rgb(150, 0, 150)
        );

        onTheWay.setEnabled(
                "ACCEPTED".equals(
                        normalizedStatus
                )
        );

        arrived.setEnabled(
                "DRIVER_ON_THE_WAY".equals(
                        normalizedStatus
                )
                        || "ON_THE_WAY".equals(
                        normalizedStatus
                )
        );

        start.setEnabled(
                "DRIVER_ARRIVED".equals(
                        normalizedStatus
                )
                        || "ARRIVED".equals(
                        normalizedStatus
                )
        );

        finish.setEnabled(
                "IN_PROGRESS".equals(
                        normalizedStatus
                )
                        || "ONGOING".equals(
                        normalizedStatus
                )
        );

        onTheWay.setOnClickListener(
                v -> updateRideStatus(
                        "DRIVER_ON_THE_WAY"
                )
        );

        arrived.setOnClickListener(
                v -> updateRideStatus(
                        "DRIVER_ARRIVED"
                )
        );

        start.setOnClickListener(
                v -> updateRideStatus(
                        "IN_PROGRESS"
                )
        );

        finish.setOnClickListener(
                v -> updateRideStatus(
                        "COMPLETED"
                )
        );

        rideStatusContainer.addView(onTheWay);
        rideStatusContainer.addView(arrived);
        rideStatusContainer.addView(start);
        rideStatusContainer.addView(finish);
    }

    private void clearRideStatusButtons() {

        if (rideStatusContainer != null) {
            rideStatusContainer.removeAllViews();
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

        String rideId =
                currentRideId;

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

        if ("DRIVER_ON_THE_WAY".equals(
                newStatus
        )) {

            update.put(
                    "driverOnTheWayAt",
                    System.currentTimeMillis()
            );

        } else if ("DRIVER_ARRIVED".equals(
                newStatus
        )) {

            update.put(
                    "driverArrivedAt",
                    System.currentTimeMillis()
            );

        } else if ("IN_PROGRESS".equals(
                newStatus
        )) {

            update.put(
                    "rideStartedAt",
                    System.currentTimeMillis()
            );

        } else if ("COMPLETED".equals(
                newStatus
        )) {

            update.put(
                    "completedAt",
                    System.currentTimeMillis()
            );
        }

        db.collection("rides")
                .document(rideId)
                .update(update)
                .addOnSuccessListener(v -> {

                    Toast.makeText(
                            this,
                            statusMessage(newStatus),
                            Toast.LENGTH_SHORT
                    ).show();

                    if ("COMPLETED".equals(
                            newStatus
                    )) {

                        currentRideId = "";

                        hiddenRequestIds.remove(
                                rideId
                        );

                        currentRideText.setText(
                                "✅ TRIP FINISHED"
                        );

                        clearRideStatusButtons();

                        listenForCurrentRide();
                        listenForRideRequests();

                    } else {

                        listenForCurrentRide();
                    }
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            "Unable to update ride status:\n"
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private String statusMessage(
            String status
    ) {

        if ("DRIVER_ON_THE_WAY".equals(status)) {
            return "🚗 Driver is on the way.";
        }

        if ("DRIVER_ARRIVED".equals(status)) {
            return "📍 Driver has arrived.";
        }

        if ("IN_PROGRESS".equals(status)) {
            return "▶️ Ride started.";
        }

        if ("COMPLETED".equals(status)) {
            return "🏁 Trip finished.";
        }

        return "Ride status updated.";
    }

    private boolean isActive(
            String status
    ) {

        if (status == null) {
            return false;
        }

        return
                "ACCEPTED".equalsIgnoreCase(status)
                        || "DRIVER_ON_THE_WAY"
                        .equalsIgnoreCase(status)
                        || "DRIVER_ARRIVED".equalsIgnoreCase(status)
                        || "IN_PROGRESS".equalsIgnoreCase(status)
                        || "ARRIVED".equalsIgnoreCase(status)
                        || "ONGOING".equalsIgnoreCase(status);
    }

    private long longValue(
            DocumentSnapshot doc,
            String field
    ) {

        Object value =
                doc.get(field);

        if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        return 0;
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

        String address =
                string(
                        ride,
                        type + "Address"
                );

        if (!address.isEmpty()) {
            return address;
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

    private String formatFare(
            Object fareObject
    ) {

        if (fareObject == null) {
            return "Not available";
        }

        if (fareObject instanceof Number) {

            double value =
                    ((Number) fareObject)
                            .doubleValue();

            if (value == Math.floor(value)) {
                return "₱" + (long) value;
            }

            return "₱" + value;
        }

        String value =
                String.valueOf(
                        fareObject
                ).trim();

        if (value.isEmpty()) {
            return "Not available";
        }

        if (value.startsWith("₱")) {
            return value;
        }

        return "₱" + value;
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

    /*
     * GCash QR PRESERVED.
     *
     * R.drawable.gcash_qr is intentionally retained.
     */
    private void showGcashQr() {

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setGravity(
                Gravity.CENTER
        );

        layout.setPadding(
                25,
                15,
                25,
                10
        );

        TextView title =
                new TextView(this);

        title.setText(
                "💳 PAY SAKAY NA — GCASH"
        );

        title.setTextSize(22);

        title.setGravity(
                Gravity.CENTER
        );

        title.setTextColor(
                Color.rgb(0, 90, 150)
        );

        title.setPadding(
                5,
                5,
                5,
                15
        );

        layout.addView(title);

        TextView warning =
                new TextView(this);

        warning.setText(
                "⚠️ DRIVER PAYMENT ONLY"
                        + "\n\n"
                        + "This QR is for DRIVER → SAKAY NA payment only."
                        + "\n\n"
                        + "Scan this QR to pay your Sakay Na platform dues/fee."
                        + "\n\n"
                        + "🚫 DO NOT ask the passenger to scan this QR."
        );

        warning.setTextSize(17);

        warning.setGravity(
                Gravity.CENTER
        );

        warning.setTextColor(
                Color.rgb(150, 0, 0)
        );

        warning.setPadding(
                10,
                5,
                10,
                15
        );

        layout.addView(warning);

        ImageView qrImage =
                new ImageView(this);

        qrImage.setImageResource(
                R.drawable.gcash_qr
        );

        qrImage.setAdjustViewBounds(true);

        qrImage.setScaleType(
                ImageView.ScaleType.CENTER_INSIDE
        );

        LinearLayout.LayoutParams imageParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        650
                );

        imageParams.setMargins(
                10,
                5,
                10,
                5
        );

        layout.addView(
                qrImage,
                imageParams
        );

        TextView instruction =
                new TextView(this);

        instruction.setText(
                "💰 After payment, keep your GCash confirmation."
                        + "\n"
                        + "Admin must verify the payment before the driver dues are cleared."
        );

        instruction.setTextSize(16);

        instruction.setGravity(
                Gravity.CENTER
        );

        instruction.setTextColor(
                Color.DKGRAY
        );

        instruction.setPadding(
                5,
                10,
                5,
                5
        );

        layout.addView(instruction);

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setView(layout)
                        .setPositiveButton(
                                "CLOSE",
                                null
                        )
                        .create();

        dialog.show();
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

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {
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

        if (user == null) {
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
                && results.length > 0) {

            boolean granted = false;

            for (int result : results) {

                if (result ==
                        PackageManager.PERMISSION_GRANTED) {

                    granted = true;
                    break;
                }
            }

            if (granted) {
                beginLocationTracking();
            }
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

        expiryHandler
                .removeCallbacksAndMessages(null);

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
