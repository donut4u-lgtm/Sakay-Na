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

    private Button onlineButton;
    private Button offlineButton;

    private boolean driverOnline = false;
    private boolean driverApproved = false;
    private boolean driverSuspended = false;

    private boolean suspensionCheckRunning = false;

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
        startLocationUpdates();
        listenForCurrentRide();
        startRequestExpiryChecker();

        SakayNaNotificationHelper
                .requestPermission(this);
    }

    @Override
    protected void onResume() {

        super.onResume();

        if (db != null && user != null) {
            loadDriverStatus();
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
        title.setGravity(Gravity.CENTER);

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
        statusText.setGravity(Gravity.CENTER);

        statusText.setTextColor(Color.DKGRAY);

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

        onlineButton.setTextColor(Color.WHITE);
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

        offlineButton.setTextColor(Color.WHITE);
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
        requestsText.setTextColor(Color.DKGRAY);

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
        currentRideText.setTextColor(Color.DKGRAY);

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

        settlement.setTextColor(Color.WHITE);
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

        if (suspensionCheckRunning) {
            afterCheck.run();
            return;
        }

        suspensionCheckRunning = true;

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

                    try {

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

                            if (
                                    dueCreatedAt > 0L
                                    &&
                                    (
                                            oldestDueAt == 0L
                                            ||
                                            dueCreatedAt <
                                                    oldestDueAt
                                    )
                            ) {

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

                        if (oldestDueAt == 0L) {

                            suspensionDeadlineAt = 0L;

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

                                restore.put(
                                        "suspensionNoticeAt",
                                        null
                                );

                                restore.put(
                                        "settlementWarningAt",
                                        null
                                );

                                db.collection("users")
                                        .document(user.getUid())
                                        .set(
                                                restore,
                                                SetOptions.merge()
                                        );

                                db.collection("drivers")
                                        .document(user.getUid())
                                        .set(
                                                buildOfflineData(),
                                                SetOptions.merge()
                                        );

                                Toast.makeText(
                                        this,
                                        "✅ Settlement verified. Your driver account is active again.",
                                        Toast.LENGTH_LONG
                                ).show();

                            } else {

                                driverSuspended =
                                        "SUSPENDED".equalsIgnoreCase(
                                                accountStatus
                                        );
                            }

                            suspensionCheckRunning =
                                    false;

                            afterCheck.run();

                            return;
                        }

                        long deadline =
                                oldestDueAt
                                        +
                                        (
                                                UNPAID_DUE_DAYS
                                                        * ONE_DAY_MS
                                        );

                        suspensionDeadlineAt =
                                deadline;

                        boolean shouldSuspend =
                                now >= deadline;

                        if (shouldSuspend) {

                            driverSuspended = true;
                            driverOnline = false;

                            Map<String, Object> suspend =
                                    new HashMap<>();

                            suspend.put(
                                    "driverAccountStatus",
                                    "SUSPENDED"
                            );

                            suspend.put(
                                    "suspensionReason",
                                    "UNPAID_DUES"
                            );

                            Object existingSuspendedAt =
                                    profile.get(
                                            "suspendedAt"
                                    );

                            suspend.put(
                                    "suspendedAt",
                                    existingSuspendedAt == null
                                            ? now
                                            : existingSuspendedAt
                            );

                            suspend.put(
                                    "suspensionDeadlineAt",
                                    deadline
                            );

                            suspend.put(
                                    "driverSettlementBalance",
                                    totalDue
                            );

                            Object previousNotice =
                                    profile.get(
                                            "suspensionNoticeAt"
                                    );

                            if (previousNotice == null) {

                                suspend.put(
                                        "suspensionNoticeAt",
                                        now
                                );

                                SakayNaNotificationHelper.show(
                                        this,
                                        7201,
                                        "🚫 Sakay Na Driver Suspended",
                                        "Your account is suspended because unpaid platform dues are past the 7-day deadline."
                                );

                                Toast.makeText(
                                        this,
                                        "🚫 Driver account suspended for unpaid dues.",
                                        Toast.LENGTH_LONG
                                ).show();
                            }

                            db.collection("users")
                                    .document(user.getUid())
                                    .set(
                                            suspend,
                                            SetOptions.merge()
                                    );

                            db.collection("drivers")
                                    .document(user.getUid())
                                    .set(
                                            buildOfflineData(),
                                            SetOptions.merge()
                                    );

                        } else {

                            driverSuspended =
                                    "SUSPENDED".equalsIgnoreCase(
                                            accountStatus
                                    )
                                    &&
                                    "UNPAID_DUES".equalsIgnoreCase(
                                            suspensionReason
                                    );

                            long warningAt =
                                    deadline -
                                            ONE_DAY_MS;

                            if (
                                    now >= warningAt
                                    &&
                                    now < deadline
                            ) {

                                Object previousWarning =
                                        profile.get(
                                                "settlementWarningAt"
                                        );

                                if (previousWarning == null) {

                                    Map<String, Object> warning =
                                            new HashMap<>();

                                    warning.put(
                                            "settlementWarningAt",
                                            now
                                    );

                                    warning.put(
                                            "driverSettlementBalance",
                                            totalDue
                                    );

                                    db.collection("users")
                                            .document(user.getUid())
                                            .set(
                                                    warning,
                                                    SetOptions.merge()
                                            );

                                    SakayNaNotificationHelper.show(
                                            this,
                                            7202,
                                            "⚠️ Sakay Na Driver Dues Warning",
                                            "Your unpaid platform dues are due for settlement within 24 hours to avoid suspension."
                                    );

                                    Toast.makeText(
                                            this,
                                            "⚠️ Final 24-hour warning: please settle your unpaid platform dues.",
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            }
                        }

                        suspensionCheckRunning =
                                false;

                        afterCheck.run();

                    } catch (Exception ignored) {

                        suspensionCheckRunning =
                                false;

                        afterCheck.run();
                    }
                })
                .addOnFailureListener(e -> {

                    suspensionCheckRunning =
                            false;

                    afterCheck.run();
                });
    }    private double getDriverDue(
            DocumentSnapshot ride
    ) {

        Object stored =
                ride.get(
                        "driverDuesAmount"
                );

        if (stored instanceof Number) {

            return roundMoney(
                    ((Number) stored)
                            .doubleValue()
            );
        }

        double fare =
                getFare(ride);

        return roundMoney(
                fare * PLATFORM_FEE_RATE
        );
    }

    private double getFare(
            DocumentSnapshot document
    ) {

        Object value =
                document.get(
                        "fare"
                );

        if (value == null) {
            return 0.0;
        }

        if (value instanceof Number) {

            return ((Number) value)
                    .doubleValue();
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
                                        doc.getBoolean(
                                                "online"
                                        )
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

    private Map<String, Object>
    buildOfflineData() {

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

        if (
                onlineButton == null
                ||
                offlineButton == null
        ) {

            return;
        }

        if (
                !driverApproved
                ||
                driverSuspended
        ) {

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

        if (
                online
                &&
                driverSuspended
        ) {

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

        if (
                online
                &&
                !driverApproved
        ) {

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
                            + "\n"
                            + "Full payment must be verified by Admin before you can go ONLINE."
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

        if (
                driverSettlementBalance
                        > 0.0
        ) {

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
                            ? Color.rgb(
                            0,
                            145,
                            65
                    )
                            : Color.rgb(
                            190,
                            25,
                            25
                    )
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
                        ? Color.rgb(
                        0,
                        145,
                        65
                )
                        : Color.rgb(
                        190,
                        25,
                        25
                )
        );
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

    private void notifyNewRideRequests(
            QuerySnapshot snapshots
    ) {

        if (
                snapshots == null
                ||
                !driverOnline
                ||
                !driverApproved
                ||
                driverSuspended
        ) {

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

            String status =
                    string(
                            ride,
                            "status"
                    );

            if (
                    !"REQUESTED".equalsIgnoreCase(
                            status
                    )
            ) {

                continue;
            }

            long createdAt =
                    longValue(
                            ride,
                            "createdAt"
                    );

            if (createdAt <= 0L) {

                createdAt =
                        longValue(
                                ride,
                                "requestedAt"
                        );
            }

            if (
                    createdAt > 0L
                    &&
                    now - createdAt
                            >= REQUEST_EXPIRATION_MS
            ) {

                continue;
            }

            if (
                    notifiedRequestIds.contains(
                            rideId
                    )
            ) {

                continue;
            }

            notifiedRequestIds.add(
                    rideId
            );

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
                            ride.get(
                                    "fare"
                            )
                    );

            String payment =
                    string(
                            ride,
                            "paymentMethod"
                    );

            if (payment.isEmpty()) {
                payment = "Not specified";
            }

            SakayNaNotificationHelper.show(
                    this,
                    5000 + Math.abs(
                            rideId.hashCode()
                    ),
                    "🛺 New Sakay Na Ride",
                    "Pickup: "
                            + pickup
                            + "\nDestination: "
                            + destination
                            + "\nFare: "
                            + fare
                            + "\nPayment: "
                            + payment
            );
        }
    }

    private void startRequestExpiryChecker() {

        expiryHandler.postDelayed(
                new Runnable() {

                    @Override
                    public void run() {

                        if (
                                latestRequestSnapshot
                                        != null
                        ) {

                            renderRideRequests(
                                    latestRequestSnapshot
                            );
                        }

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

        if (requestContainer == null) {
            return;
        }

        requestContainer.removeAllViews();

        if (driverSuspended) {

            requestsText.setText(
                    "🚫 Ride requests are disabled while your account is suspended."
            );

            return;
        }

        if (!driverApproved) {

            requestsText.setText(
                    "⏳ Driver approval is required before receiving rides."
            );

            return;
        }

        if (
                !driverOnline
        ) {

            requestsText.setText(
                    "🔴 You are OFFLINE.\nGo ONLINE to receive ride requests."
            );

            return;
        }

        if (
                snapshots == null
                ||
                snapshots.isEmpty()
        ) {

            requestsText.setText(
                    "No new ride requests."
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

            String status =
                    string(
                            ride,
                            "status"
                    );

            if (
                    !"REQUESTED".equalsIgnoreCase(
                            status
                    )
            ) {

                continue;
            }

            if (
                    hiddenRequestIds.contains(
                            rideId
                    )
            ) {

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

            long createdAt =
                    longValue(
                            ride,
                            "createdAt"
                    );

            if (createdAt <= 0L) {

                createdAt =
                        longValue(
                                ride,
                                "requestedAt"
                        );
            }

            if (
                    createdAt > 0L
                    &&
                    now - createdAt
                            >= REQUEST_EXPIRATION_MS
            ) {

                hiddenRequestIds.add(
                        rideId
                );

                continue;
            }

            if (
                    rideId.equals(
                            currentRideId
                    )
            ) {

                continue;
            }

            addRideCard(
                    ride
            );

            count++;
        }

        if (count == 0) {

            requestsText.setText(
                    "No new ride requests."
            );

        } else {

            requestsText.setText(
                    "🟢 "
                            + count
                            + " ride request"
                            + (
                            count == 1
                                    ? ""
                                    : "s"
                    )
                            + " available."
            );
        }
    }    private void addRideCard(
            DocumentSnapshot ride
    ) {

        String rideId =
                ride.getId();

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
                Color.WHITE
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

        requestContainer.addView(
                card,
                cardParams
        );

        TextView title =
                new TextView(this);

        title.setText(
                "🛺 NEW RIDE REQUEST"
        );

        title.setTextSize(20);

        title.setTextColor(
                Color.rgb(0, 100, 180)
        );

        card.addView(title);

        TextView details =
                new TextView(this);

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

        String passengers =
                passengerCountText(
                        ride
                );

        String payment =
                string(
                        ride,
                        "paymentMethod"
                );

        if (payment.isEmpty()) {
            payment = "Not specified";
        }

        details.setText(
                "📍 Pickup:\n"
                        + pickup
                        + "\n\n"
                        + "🏁 Destination:\n"
                        + destination
                        + "\n\n"
                        + "💰 Fare: "
                        + fare
                        + "\n"
                        + "👥 Passengers: "
                        + passengers
                        + "\n"
                        + "💳 Payment: "
                        + payment
        );

        details.setTextSize(17);

        details.setTextColor(
                Color.rgb(40, 40, 40)
        );

        details.setPadding(
                0,
                15,
                0,
                15
        );

        card.addView(details);

        LinearLayout buttons =
                new LinearLayout(this);

        buttons.setOrientation(
                LinearLayout.HORIZONTAL
        );

        Button accept =
                new Button(this);

        accept.setText(
                "✅ ACCEPT"
        );

        accept.setTextColor(
                Color.WHITE
        );

        accept.setBackgroundColor(
                Color.rgb(0, 145, 65)
        );

        accept.setEnabled(
                driverOnline
                        && driverApproved
                        && !driverSuspended
        );

        accept.setOnClickListener(
                v -> acceptRide(
                        rideId,
                        ride,
                        card
                )
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

        decline.setOnClickListener(
                v -> declineRide(
                        rideId,
                        card
                )
        );

        LinearLayout.LayoutParams acceptParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );

        LinearLayout.LayoutParams declineParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );

        acceptParams.setMargins(
                5,
                5,
                5,
                5
        );

        declineParams.setMargins(
                5,
                5,
                5,
                5
        );

        buttons.addView(
                accept,
                acceptParams
        );

        buttons.addView(
                decline,
                declineParams
        );

        card.addView(buttons);
    }

    private void acceptRide(
            String rideId,
            DocumentSnapshot ride,
            LinearLayout card
    ) {

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
                    "⏳ Driver approval is required.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (!driverOnline) {

            Toast.makeText(
                    this,
                    "🔴 You must be ONLINE to accept a ride.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (card != null) {
            card.setEnabled(false);
        }

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(profile -> {

                    checkUnpaidDues(
                            profile,
                            () -> {

                                if (driverSuspended) {

                                    if (card != null) {
                                        card.setEnabled(true);
                                    }

                                    Toast.makeText(
                                            this,
                                            "🚫 Your account is suspended for unpaid dues.",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    return;
                                }

                                if (!driverApproved) {

                                    if (card != null) {
                                        card.setEnabled(true);
                                    }

                                    Toast.makeText(
                                            this,
                                            "⏳ Driver approval is required.",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    return;
                                }

                                acceptRideNow(
                                        rideId,
                                        ride,
                                        card
                                );
                            }
                    );
                })
                .addOnFailureListener(e -> {

                    if (card != null) {
                        card.setEnabled(true);
                    }

                    Toast.makeText(
                            this,
                            "Unable to verify driver status:\n"
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void acceptRideNow(
            String rideId,
            DocumentSnapshot ride,
            LinearLayout card
    ) {

        long acceptedAt =
                System.currentTimeMillis();

        Map<String, Object> update =
                buildDriverRideUpdate();

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

        double acceptedFare =
                getFare(ride);

        double driverDuesAmount =
                roundMoney(
                        acceptedFare
                                * PLATFORM_FEE_RATE
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

        db.collection("rides")
                .document(rideId)
                .update(update)
                .addOnSuccessListener(v -> {

                    currentRideId =
                            rideId;

                    hiddenRequestIds.add(
                            rideId
                    );

                    currentRideText.setText(
                            "🟢 RIDE ACCEPTED\n"
                                    + "Fare: "
                                    + formatFare(
                                    ride.get("fare")
                            )
                    );

                    Toast.makeText(
                            this,
                            "✅ Ride accepted.\n"
                                    + "Admin transaction recorded.\n"
                                    + "10% platform fee added to your dues.",
                            Toast.LENGTH_LONG
                    ).show();

                    listenForCurrentRide();

                    renderRideRequests(
                            latestRequestSnapshot
                    );

                    notifyPassengerRideAccepted(
                            ride
                    );
                })
                .addOnFailureListener(e -> {

                    if (card != null) {
                        card.setEnabled(true);
                    }

                    Toast.makeText(
                            this,
                            "Unable to accept ride:\n"
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private Map<String, Object>
    buildDriverRideUpdate() {

        Map<String, Object> update =
                new HashMap<>();

        update.put(
                "driverId",
                user.getUid()
        );

        update.put(
                "driverName",
                getDriverName()
        );

        update.put(
                "driverPhone",
                getDriverPhone()
        );

        update.put(
                "driverPlateNumber",
                getDriverPlateNumber()
        );

        update.put(
                "driverVehicle",
                getDriverVehicle()
        );

        return update;
    }

    private String getDriverName() {

        return user == null
                ? ""
                : (
                user.getDisplayName()
                        == null
                        ? ""
                        : user.getDisplayName()
        );
    }

    private String getDriverPhone() {

        return "";
    }

    private String getDriverPlateNumber() {

        return "";
    }

    private String getDriverVehicle() {

        return "";
    }

    private void notifyPassengerRideAccepted(
            DocumentSnapshot ride
    ) {

        /*
         * Passenger-side FCM notification is handled
         * by the existing notification architecture.
         *
         * The ride itself is already updated to
         * ACCEPTED here    private void showRideStatusButtons(
            String status
    ) {

        rideStatusContainer.removeAllViews();

        if (status == null) {
            return;
        }

        String normalized =
                status.trim()
                        .toUpperCase();

        Button action =
                new Button(this);

        if ("ACCEPTED".equals(normalized)) {

            action.setText(
                    "🛵 DRIVER ON THE WAY"
            );

            action.setTextColor(
                    Color.WHITE
            );

            action.setBackgroundColor(
                    Color.rgb(0, 120, 200)
            );

            action.setOnClickListener(
                    v -> updateRideStatus(
                            "DRIVER_ON_THE_WAY"
                    )
            );

        } else if (
                "DRIVER_ON_THE_WAY".equals(
                        normalized
                )
                ||
                "ON_THE_WAY".equals(
                        normalized
                )
        ) {

            action.setText(
                    "📍 DRIVER ARRIVED"
            );

            action.setTextColor(
                    Color.WHITE
            );

            action.setBackgroundColor(
                    Color.rgb(230, 135, 0)
            );

            action.setOnClickListener(
                    v -> updateRideStatus(
                            "DRIVER_ARRIVED"
                    )
            );

        } else if (
                "DRIVER_ARRIVED".equals(
                        normalized
                )
                ||
                "ARRIVED".equals(
                        normalized
                )
        ) {

            action.setText(
                    "▶️ START RIDE"
            );

            action.setTextColor(
                    Color.WHITE
            );

            action.setBackgroundColor(
                    Color.rgb(0, 145,     private void showRideStatusButtons(
            String status
    ) {

        rideStatusContainer.removeAllViews();

        if (status == null) {
            return;
        }

        String normalized =
                status.trim()
                        .toUpperCase();

        Button action =
                new Button(this);

        if ("ACCEPTED".equals(normalized)) {

            action.setText(
                    "🛵 DRIVER ON THE WAY"
            );

            action.setTextColor(
                    Color.WHITE
            );

            action.setBackgroundColor(
                    Color.rgb(0, 120, 200)
            );

            action.setOnClickListener(
                    v -> updateRideStatus(
                            "DRIVER_ON_THE_WAY"
                    )
            );

        } else if (
                "DRIVER_ON_THE_WAY".equals(
                        normalized
                )
                ||
                "ON_THE_WAY".equals(
                        normalized
                )
        ) {

            action.setText(
                    "📍 DRIVER ARRIVED"
            );

            action.setTextColor(
                    Color.WHITE
            );

            action.setBackgroundColor(
                    Color.rgb(230, 135, 0)
            );

            action.setOnClickListener(
                    v -> updateRideStatus(
                            "DRIVER_ARRIVED"
                    )
            );

        } else if (
                "DRIVER_ARRIVED".equals(
                        normalized
                )
                ||
                "ARRIVED".equals(
                        normalized
                )
        ) {

            action.setText(
                    "▶️ START RIDE"
            );

            action.setTextColor(
                    Color.WHITE
            );

            action.setBackgroundColor(
                    Color.rgb(0, 145,     private void showRideStatusButtons(
            String status
    ) {

        rideStatusContainer.removeAllViews();

        if (status == null) {
            return;
        }

        String normalized =
                status.trim()
                        .toUpperCase();

        Button action =
                new Button(this);

        if ("ACCEPTED".equals(normalized)) {

            action.setText(
                    "🛵 DRIVER ON THE WAY"
            );

            action.setTextColor(
                    Color.WHITE
            );

            action.setBackgroundColor(
                    Color.rgb(0, 120, 200)
            );

            action.setOnClickListener(
                    v -> updateRideStatus(
                            "DRIVER_ON_THE_WAY"
                    )
            );

        } else if (
                "DRIVER_ON_THE_WAY".equals(
                        normalized
                )
                ||
                "ON_THE_WAY".equals(
                        normalized
                )
        ) {

            action.setText(
                    "📍 DRIVER ARRIVED"
            );

            action.setTextColor(
                    Color.WHITE
            );

            action.setBackgroundColor(
                    Color.rgb(230, 135, 0)
            );

            action.setOnClickListener(
                    v -> updateRideStatus(
                            "DRIVER_ARRIVED"
                    )
            );

        } else if (
                "DRIVER_ARRIVED".equals(
                        normalized
                )
                ||
                "ARRIVED".equals(
                        normalized
                )
        ) {

            action.setText(
                    "▶️ START RIDE"
            );

            action.setTextColor(
                    Color.WHITE
            );

            action.setBackgroundColor(
                    Color.rgb(0, 145, 65)
            );

            action.setOnClickListener(
                    v -> updateRideStatus(
                            "IN_PROGRESS"
                    )
            );

        } else if (
                "IN_PROGRESS".equals(
                        normalized
                )
                ||
                "ONGOING".equals(
                        normalized
                )
        ) {

            action.setText(
                    "🏁 FINISH RIDE"
            );

            action.setTextColor(
                    Color.WHITE
            );

            action.setBackgroundColor(
                    Color.rgb(150, 0, 120)
            );

            action.setOnClickListener(
                    v -> updateRideStatus(
                            "COMPLETED"
                    )
            );

        } else {

            return;
        }

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(
                0,
                8,
                0,
                8
        );

        rideStatusContainer.addView(
                action,
                params
        );
    }

    private void updateRideStatus(
            String newStatus
    ) {

        if (
                currentRideId == null
                ||
                currentRideId.trim().isEmpty()
        ) {

            Toast.makeText(
                    this,
                    "No active ride.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Map<String, Object> update =
                new HashMap<>();

        long now =
                System.currentTimeMillis();

        update.put(
                "status",
                newStatus
        );

        update.put(
                "statusUpdatedAt",
                now
        );

        if (
                "DRIVER_ON_THE_WAY".equals(
                        newStatus
                )
        ) {

            update.put(
                    "driverOnTheWayAt",
                    now
            );

        } else if (
                "DRIVER_ARRIVED".equals(
                        newStatus
                )
        ) {

            update.put(
                    "driverArrivedAt",
                    now
            );

        } else if (
                "IN_PROGRESS".equals(
                        newStatus
                )
        ) {

            update.put(
                    "rideStartedAt",
                    now
            );

        } else if (
                "COMPLETED".equals(
                        newStatus
                )
        ) {

            update.put(
                    "completedAt",
                    now
            );
        }

        db.collection("rides")
                .document(currentRideId)
                .update(update)
                .addOnSuccessListener(v -> {

                    Toast.makeText(
                            this,
                            statusMessage(
                                    newStatus
                            ),
                            Toast.LENGTH_SHORT
                    ).show();

                    if (
                            "COMPLETED".equals(
                                    newStatus
                            )
                    ) {

                        currentRideId = "";

                        rideStatusContainer
                                .removeAllViews();

                        currentRideText.setText(
                                "🏁 TRIP FINISHED"
                        );

                        listenForCurrentRide();

                    } else {

                        listenForCurrentRide();
                    }
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

        if (
                "DRIVER_ON_THE_WAY".equals(
                        status
                )
        ) {

            return "🛵 Passenger notified: driver is on the way.";

        }

        if (
                "DRIVER_ARRIVED".equals(
                        status
                )
        ) {

            return "📍 Passenger notified: driver has arrived.";

        }

        if (
                "IN_PROGRESS".equals(
                        status
                )
        ) {

            return "▶️ Ride started.";

        }

        if (
                "COMPLETED".equals(
                        status
                )
        ) {

            return "🏁 Ride completed.";

        }

        return "Ride status updated.";
    }

    private boolean isActive(
            String status
    ) {

        if (status == null) {
            return false;
        }

        String value =
                status.trim()
                        .toUpperCase();

        return "ACCEPTED".equals(value)
                ||
                "DRIVER_ON_THE_WAY".equals(value)
                ||
                "DRIVER_ARRIVED".equals(value)
                ||
                "IN_PROGRESS".equals(value)
                ||
                "ARRIVED".equals(value)
                ||
                "ONGOING".equals(value);
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

        return 0L;
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

        if (
                !lat.isEmpty()
                &&
                !lng.isEmpty()
        ) {

            return lat
                    + ", "
                    + lng;
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

            if (
                    value
                            ==
                            Math.floor(value)
            ) {

                return "₱"
                        + (long) value;
            }

            return "₱"
                    + value;
        }

        String value =
                String.valueOf(
                        fareObject
                ).trim();

        if (value.isEmpty()) {
            return "Not available";
        }

        if (
                value.startsWith("₱")
        ) {

            return value;
        }

        return "₱"
                + value;
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

    private void openMap() {

        if (
                currentRideId == null
                ||
                currentRideId.trim().isEmpty()
        ) {

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

        if (
                currentRideId == null
                ||
                currentRideId.trim().isEmpty()
        ) {

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

        if (locationManager == null) {
            return;
        }

        locationListener =
                new LocationListener() {

                    @Override
                    public void onLocationChanged(
                            @NonNull Location location
                    ) {

                        saveDriverLocation(
                                location
                        );
                    }

                    @Override
                    public void onProviderEnabled(
                            @NonNull String provider
                    ) {
                    }

                    @Override
                    public void onProviderDisabled(
                            @NonNull String provider
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
                    LOCATION_PERMISSION
            );

            return;
        }

        requestLocationUpdatesNow();
    }

    private void requestLocationUpdatesNow() {

        if (locationManager == null) {
            return;
        }

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

            return;
        }

        try {

            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    5000L,
                    10.0f,
                    locationListener,
                    Looper.getMainLooper()
            );

            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    5000L,
                    10.0f,
                    locationListener,
                    Looper.getMainLooper()
            );

        } catch (Exception ignored) {
        }
    }

    private void saveDriverLocation(
            Location location
    ) {

        if (
                user == null
                ||
                location == null
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

        db.collection(
                        "driverLocations"
                )
                .document(
                        user.getUid()
                )
                .set(
                        data,
                        SetOptions.merge()
                );
    }

    private void logout() {

        if (user == null) {
            return;
        }

        String uid =
                user.getUid();

        Map<String, Object> offline =
                buildOfflineData();

        db.collection("drivers")
                .document(uid)
                .set(
                        offline,
                        SetOptions.merge()
                )
                .addOnCompleteListener(
                        task -> {

                            auth.signOut();

                            Intent intent =
                                    new Intent(
                                            DriverActivity.this,
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

        if (
                requestCode
                        ==
                        LOCATION_PERMISSION
        ) {

            if (
                    grantResults.length > 0
                    &&
                    grantResults[0]
                            ==
                            PackageManager.PERMISSION_GRANTED
            ) {

                requestLocationUpdatesNow();

            } else {

                Toast.makeText(
                        this,
                        "Location permission is required for live driver location.",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        expiryHandler.removeCallbacksAndMessages(
                null
        );

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
    }
}
