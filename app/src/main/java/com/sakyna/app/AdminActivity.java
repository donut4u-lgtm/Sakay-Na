package com.sakyna.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminActivity extends Activity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private LinearLayout contentContainer;
    private TextView statusText;

    private LinearLayout overviewSection;
    private LinearLayout passengersSection;
    private LinearLayout approvalSection;
    private LinearLayout onlineDriversSection;
    private LinearLayout historySection;
    private LinearLayout paymentSection;
    private LinearLayout driverEarningsDuesSection;

    private final Map<String, DocumentSnapshot> usersById =
            new HashMap<>();

    private final List<DocumentSnapshot> rideDocuments =
            new ArrayList<>();

    /*
     * ============================================================
     * LIVE DRIVER ONLINE STATUS
     *
     * DriverActivity stores the live online/offline state in:
     *
     * drivers/{uid}.online
     *
     * Admin therefore reads the drivers collection instead of
     * depending on users/{uid}.online.
     * ============================================================
     */
    private final Map<String, Boolean> driverOnlineById =
            new HashMap<>();

    private ListenerRegistration driversListener;

    private TextView onlineDriversOverviewValue;

    private static final int GREEN =
            Color.rgb(0, 125, 80);

    private static final int DARK =
            Color.rgb(35, 35, 35);

    private static final int WHITE =
            Color.WHITE;

    private static final int LIGHT_GREEN =
            Color.rgb(232, 247, 238);

    private static final int LIGHT_RED =
            Color.rgb(255, 235, 235);

    private static final int LIGHT_YELLOW =
            Color.rgb(255, 248, 220);

    private static final int LIGHT_BLUE =
            Color.rgb(235, 245, 255);

    private static final int GRAY =
            Color.rgb(110, 110, 110);

    private static final int HISTORY_DAYS = 7;

    private int paymentDays = 30;

    private static final double PLATFORM_FEE_RATE = 0.10;

    private static final long SEVEN_DAYS_MILLIS =
            7L
                    * 24L
                    * 60L
                    * 60L
                    * 1000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        buildScreen();
        loadDashboard();
    }

    private void buildScreen() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(WHITE);
        root.setPadding(16, 16, 16, 16);

        TextView title = new TextView(this);
        title.setText("🛺 SAKAY NA ADMIN");
        title.setTextSize(28);
        title.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );
        title.setTextColor(GREEN);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("ADMIN CONTROL CENTER");
        subtitle.setTextSize(15);
        subtitle.setTextColor(DARK);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 0, 0, 12);
        root.addView(subtitle);

        statusText = new TextView(this);
        statusText.setText("Loading dashboard...");
        statusText.setTextSize(15);
        statusText.setTextColor(GRAY);
        statusText.setGravity(Gravity.CENTER);
        root.addView(statusText);

        Button refresh = new Button(this);
        refresh.setText("🔄 REFRESH");
        refresh.setOnClickListener(
                v -> loadDashboard()
        );
        root.addView(refresh);

        Button settlementButton = new Button(this);
        settlementButton.setText("💰 DRIVER SETTLEMENTS");
        settlementButton.setOnClickListener(v -> {

            Intent intent = new Intent(
                    AdminActivity.this,
                    AdminSettlementActivity.class
            );

            startActivity(intent);
        });
        root.addView(settlementButton);

        ScrollView scrollView = new ScrollView(this);

        contentContainer = new LinearLayout(this);
        contentContainer.setOrientation(
                LinearLayout.VERTICAL
        );
        contentContainer.setPadding(
                0,
                10,
                0,
                20
        );

        scrollView.addView(contentContainer);

        root.addView(
                scrollView,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                )
        );

        Button logout = new Button(this);
        logout.setText("🚪 LOGOUT");
        logout.setOnClickListener(
                v -> logout()
        );
        root.addView(logout);

        setContentView(root);
    }

    private void loadDashboard() {

        contentContainer.removeAllViews();

        usersById.clear();
        rideDocuments.clear();

        /*
         * ============================================================
         * IMPORTANT FIX
         *
         * contentContainer was completely cleared above.
         *
         * Therefore the old section references must also be reset.
         *
         * Previously onlineDriversSection still pointed to the old,
         * detached section. buildOnlineDrivers() then reused that
         * detached section instead of creating a new section/button.
         *
         * That caused:
         *
         * 🟢 ONLINE DRIVERS — LIVE
         *
         * to disappear completely after REFRESH.
         *
         * Resetting the references forces createSection() to create
         * the section and its button again.
         * ============================================================
         */

        overviewSection = null;
        passengersSection = null;
        approvalSection = null;
        onlineDriversSection = null;
        historySection = null;
        paymentSection = null;
        driverEarningsDuesSection = null;

        /*
         * Clear old live driver status before starting
         * a fresh dashboard listener.
         */
        driverOnlineById.clear();

        /*
         * Remove the previous real-time Firestore listener.
         */
        if (driversListener != null) {
            driversListener.remove();
            driversListener = null;
        }

        /*
         * The old overview TextView belonged to the old
         * dashboard and is no longer valid.
         */
        onlineDriversOverviewValue = null;

        statusText.setText(
                "Loading users and rides..."
        );

        loadUsers();
    }

    private void loadUsers() {

        db.collection("users")
                .get()
                .addOnSuccessListener(snapshot -> {

                    for (DocumentSnapshot user :
                            snapshot.getDocuments()) {

                        usersById.put(
                                user.getId(),
                                user
                        );
                    }

                    listenToDriverOnlineStatus();

                    buildUserSections();
                    loadRides();
                })
                .addOnFailureListener(e -> {

                    statusText.setText(
                            "Unable to load users."
                    );

                    buildUserSections();
                    loadRides();
                });
    }

    private void listenToDriverOnlineStatus() {

        if (driversListener != null) {
            driversListener.remove();
        }

        driversListener =
                db.collection("drivers")
                        .addSnapshotListener(
                                (snapshot, error) -> {

                                    if (error != null) {

                                        statusText.setText(
                                                "Users loaded. Live driver status unavailable."
                                        );

                                        return;
                                    }

                                    driverOnlineById.clear();

                                    if (snapshot != null) {

                                        for (
                                                DocumentSnapshot driver :
                                                snapshot.getDocuments()
                                        ) {

                                            Boolean online =
                                                    driver.getBoolean(
                                                            "online"
                                                    );

                                            driverOnlineById.put(
                                                    driver.getId(),
                                                    Boolean.TRUE.equals(
                                                            online
                                                    )
                                            );
                                        }
                                    }

                                    if (!usersById.isEmpty()) {

                                        buildOnlineDrivers();

                                        updateOnlineOverview(
                                                countLiveApprovedDrivers()
                                        );
                                    }
                                }
                        );
    }

    private int countLiveApprovedDrivers() {

        int count = 0;

        for (DocumentSnapshot user :
                usersById.values()) {

            if (!"DRIVER".equalsIgnoreCase(
                    user.getString("role")
            )) {
                continue;
            }

            if (!"APPROVED".equalsIgnoreCase(
                    getApprovalStatus(user)
            )) {
                continue;
            }

            if ("SUSPENDED".equalsIgnoreCase(
                    user.getString(
                            "driverAccountStatus"
                    )
            )) {
                continue;
            }

            if (Boolean.TRUE.equals(
                    driverOnlineById.get(
                            user.getId()
                    )
            )) {

                count++;
            }
        }

        return count;
    }

    private void buildUserSections() {

        int passengerCount = 0;
        int driverCount = 0;
        int pendingCount = 0;
        int approvedCount = 0;
        int onlineDrivers = 0;

        for (DocumentSnapshot user :
                usersById.values()) {

            String role =
                    user.getString("role");

            if ("PASSENGER".equalsIgnoreCase(role)) {

                passengerCount++;

            } else if ("DRIVER".equalsIgnoreCase(role)) {

                driverCount++;

                String approval =
                        getApprovalStatus(user);

                if ("APPROVED".equalsIgnoreCase(
                        approval)) {

                    approvedCount++;

                } else if ("PENDING_APPROVAL"
                        .equalsIgnoreCase(approval)) {

                    pendingCount++;
                }

                if (
                        "APPROVED".equalsIgnoreCase(
                                approval
                        )
                        &&
                        Boolean.TRUE.equals(
                                driverOnlineById.get(
                                        user.getId()
                                )
                        )
                        &&
                        !"SUSPENDED".equalsIgnoreCase(
                                user.getString(
                                        "driverAccountStatus"
                                )
                        )
                ) {

                    onlineDrivers++;
                }
            }
        }

        overviewSection = createSection(
                "📊 DASHBOARD OVERVIEW"
        );

        addInfoCard(
                overviewSection,
                "👤 PASSENGER ACCOUNTS",
                String.valueOf(passengerCount),
                LIGHT_BLUE
        );

        addInfoCard(
                overviewSection,
                "🚕 DRIVER ACCOUNTS",
                String.valueOf(driverCount),
                LIGHT_GREEN
        );

        addInfoCard(
                overviewSection,
                "⏳ PENDING DRIVER APPLICATIONS",
                String.valueOf(pendingCount),
                LIGHT_YELLOW
        );

        addInfoCard(
                overviewSection,
                "✅ APPROVED DRIVERS",
                String.valueOf(approvedCount),
                LIGHT_GREEN
        );

        addOnlineOverviewCard(
                onlineDrivers
        );

        buildPendingDrivers();

        buildOnlineDrivers();

        passengersSection = createSection(
                "🛺 TODAY'S PASSENGER BOOKINGS"
        );

        addInfoCard(
                passengersSection,
                "🛺 TODAY'S BOOKINGS",
                "Loading today's passenger bookings...",
                LIGHT_BLUE
        );

        driverEarningsDuesSection = createSection(
                "💰 DRIVER DUES — OUTSTANDING ONLY"
        );

        addInfoCard(
                driverEarningsDuesSection,
                "💰 DRIVER DUES",
                "Loading outstanding driver dues...",
                LIGHT_BLUE
        );
    }

    private void addOnlineOverviewCard(
            int onlineCount
    ) {

        LinearLayout card =
                createChildCard(
                        overviewSection,
                        LIGHT_GREEN
                );

        addCardText(
                card,
                "🟢 DRIVERS ONLINE",
                17,
                DARK
        );

        onlineDriversOverviewValue =
                new TextView(this);

        onlineDriversOverviewValue.setText(
                String.valueOf(
                        onlineCount
                )
        );

        onlineDriversOverviewValue.setTextSize(
                23
        );

        onlineDriversOverviewValue.setTextColor(
                GREEN
        );

        onlineDriversOverviewValue.setPadding(
                0,
                4,
                0,
                4
        );

        card.addView(
                onlineDriversOverviewValue
        );
    }

    private void updateOnlineOverview(
            int onlineCount
    ) {

        if (
                onlineDriversOverviewValue
                        != null
        ) {

            onlineDriversOverviewValue.setText(
                    String.valueOf(
                            onlineCount
                    )
            );
        }
    }

    private void buildPendingDrivers() {

        approvalSection = createSection(
                "🔔 DRIVER APPROVAL APPLICATIONS"
        );

        List<DocumentSnapshot> pending =
                new ArrayList<>();

        for (DocumentSnapshot user :
                usersById.values()) {

            if (
                    "DRIVER".equalsIgnoreCase(
                            user.getString("role")
                    )
                    &&
                    "PENDING_APPROVAL".equalsIgnoreCase(
                            getApprovalStatus(user)
                    )
            ) {

                pending.add(user);
            }
        }

        Collections.sort(
                pending,
                (a, b) ->
                        getDisplayName(a)
                                .compareToIgnoreCase(
                                        getDisplayName(b)
                                )
        );

        if (pending.isEmpty()) {

            addInfoCard(
                    approvalSection,
                    "🔔 DRIVER APPROVAL APPLICATIONS",
                    "No drivers waiting for approval.",
                    LIGHT_GREEN
            );

            return;
        }

        for (DocumentSnapshot driver :
                pending) {

            addDriverCard(
                    approvalSection,
                    driver,
                    true
            );
        }
    }

    private void buildOnlineDrivers() {

        /*
         * IMPORTANT:
         *
         * If the dashboard was refreshed, loadDashboard()
         * has reset onlineDriversSection to null.
         *
         * Therefore this creates a fresh section and button.
         *
         * On live Firestore updates, the existing attached section
         * is reused and only its contents are refreshed.
         */
        if (onlineDriversSection == null) {

            onlineDriversSection =
                    createSection(
                            "🟢 ONLINE DRIVERS — LIVE"
                    );

        } else {

            onlineDriversSection.removeAllViews();
        }

        List<DocumentSnapshot> online =
                new ArrayList<>();

        for (DocumentSnapshot driver :
                usersById.values()) {

            if (!"DRIVER".equalsIgnoreCase(
                    driver.getString("role")
            )) {

                continue;
            }

            if (!"APPROVED".equalsIgnoreCase(
                    getApprovalStatus(driver)
            )) {

                continue;
            }

            /*
             * LIVE STATUS:
             *
             * drivers/{uid}.online
             */
            if (!Boolean.TRUE.equals(
                    driverOnlineById.get(
                            driver.getId()
                    )
            )) {

                continue;
            }

            /*
             * Suspended drivers must never appear online.
             */
            if ("SUSPENDED".equalsIgnoreCase(
                    driver.getString(
                            "driverAccountStatus"
                    )
            )) {

                continue;
            }

            online.add(driver);
        }

        Collections.sort(
                online,
                (a, b) ->
                        getDisplayName(a)
                                .compareToIgnoreCase(
                                        getDisplayName(b)
                                )
        );

        if (online.isEmpty()) {

            addInfoCard(
                    onlineDriversSection,
                    "🟢 ONLINE DRIVERS",
                    "No approved drivers are currently online.",
                    LIGHT_YELLOW
            );

            updateOnlineOverview(0);

            return;
        }

        for (DocumentSnapshot driver :
                online) {

            addDriverCard(
                    onlineDriversSection,
                    driver,
                    false
            );
        }

        updateOnlineOverview(
                online.size()
        );
    }

    private void buildDriverEarningsDues() {

        if (driverEarningsDuesSection == null) {
            return;
        }

        driverEarningsDuesSection.removeAllViews();

        double totalEarnings = 0;
        double totalPlatformFees = 0;
        double totalOutstanding = 0;

        int dueCount = 0;
        int overdueCount = 0;

        List<DriverDuesRecord> records =
                new ArrayList<>();

        for (DocumentSnapshot driver :
                usersById.values()) {

            if (!"DRIVER".equalsIgnoreCase(
                    driver.getString("role"))) {
                continue;
            }

            DriverDuesRecord record =
                    calculateDriverDues(driver);

            if (record.outstanding <= 0.009) {
                continue;
            }

            records.add(record);

            totalEarnings += record.earnings;
            totalPlatformFees += record.platformFee;
            totalOutstanding += record.outstanding;

            if ("OVERDUE".equals(
                    record.status)) {

                overdueCount++;

            } else {

                dueCount++;
            }
        }

        addInfoCard(
                driverEarningsDuesSection,
                "💵 DRIVER EARNINGS",
                formatPeso(totalEarnings),
                LIGHT_GREEN
        );

        addInfoCard(
                driverEarningsDuesSection,
                "🏦 SAKAY NA FEE — 10%",
                formatPeso(totalPlatformFees),
                LIGHT_BLUE
        );

        addInfoCard(
                driverEarningsDuesSection,
                "🔴 TOTAL OUTSTANDING",
                formatPeso(totalOutstanding),
                totalOutstanding > 0
                        ? LIGHT_RED
                        : LIGHT_GREEN
        );

        addInfoCard(
                driverEarningsDuesSection,
                "🟡 DRIVERS WITH DUES",
                String.valueOf(dueCount),
                LIGHT_YELLOW
        );

        addInfoCard(
                driverEarningsDuesSection,
                "🔴 OVERDUE > 7 DAYS",
                String.valueOf(overdueCount),
                overdueCount > 0
                        ? LIGHT_RED
                        : LIGHT_GREEN
        );

        if (records.isEmpty()) {

            addInfoCard(
                    driverEarningsDuesSection,
                    "💰 DRIVER DUES",
                    "No drivers currently have outstanding dues.",
                    LIGHT_GREEN
            );

            return;
        }

        TextView title =
                new TextView(this);

        title.setText(
                "💰 DRIVERS WITH OUTSTANDING DUES"
        );

        title.setTextSize(20);
        title.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );
        title.setTextColor(GREEN);
        title.setPadding(
                0,
                12,
                0,
                12
        );

        driverEarningsDuesSection.addView(title);

        Collections.sort(
                records,
                (a, b) -> {

                    if (
                            a.statusRank
                                    !=
                            b.statusRank
                    ) {

                        return Integer.compare(
                                a.statusRank,
                                b.statusRank
                        );
                    }

                    return Double.compare(
                            b.outstanding,
                            a.outstanding
                    );
                }
        );

        for (DriverDuesRecord record :
                records) {

            addDriverDuesCard(
                    driverEarningsDuesSection,
                    record
            );
        }
    }

    private DriverDuesRecord calculateDriverDues(
            DocumentSnapshot driver
    ) {

        DriverDuesRecord record =
                new DriverDuesRecord();

        record.driverId =
                driver.getId();

        record.name =
                firstNonEmpty(
                        driver.getString("driverName"),
                        driver.getString("name")
                );

        record.phone =
                driver.getString("phone");

        record.province =
                driver.getString("province");

        record.town =
                driver.getString("town");

        record.approvalStatus =
                getApprovalStatus(driver);

        record.accountStatus =
                valueOrDefault(
                        driver.getString(
                                "driverAccountStatus"
                        ),
                        ""
                );

        record.outstanding =
                Math.max(
                        0,
                        readNumber(
                                driver,
                                "driverSettlementBalance"
                        )
                );

        for (DocumentSnapshot ride :
                rideDocuments) {

            String rideDriverId =
                    ride.getString("driverId");

            if (
                    rideDriverId == null
                            ||
                    !record.driverId.equals(
                            rideDriverId
                    )
            ) {
                continue;
            }

            String status =
                    ride.getString("status");

            if (!"COMPLETED".equalsIgnoreCase(
                    status)) {
                continue;
            }

            double fare =
                    readNumber(
                            ride,
                            "fare"
                    );

            if (fare > 0) {
                record.earnings += fare;
            }
        }

        record.platformFee =
                record.earnings
                        * PLATFORM_FEE_RATE;

        record.paid =
                Math.max(
                        0,
                        record.platformFee
                                - record.outstanding
                );

        record.oldestDueTimestamp =
                findOldestDueTimestamp(
                        record.driverId
                );

        if (record.oldestDueTimestamp != null) {

            long age =
                    System.currentTimeMillis()
                            -
                            record.oldestDueTimestamp;

            if (age > SEVEN_DAYS_MILLIS) {

                record.status =
                        "OVERDUE";

                record.statusRank = 0;

            } else {

                record.status =
                        "DUE";

                record.statusRank = 1;
            }

            record.daysUnpaid =
                    Math.max(
                            0,
                            age / (
                                    24L
                                            * 60L
                                            * 60L
                                            * 1000L
                            )
                    );

        } else {

            record.status =
                    "DUE";

            record.statusRank = 1;
            record.daysUnpaid = 0;
        }

        return record;
    }

    private Long findOldestDueTimestamp(
            String driverId
    ) {

        Long oldest = null;

        for (DocumentSnapshot ride :
                rideDocuments) {

            String rideDriverId =
                    ride.getString("driverId");

            if (
                    rideDriverId == null
                            ||
                    !driverId.equals(
                            rideDriverId
                    )
            ) {
                continue;
            }

            String duesStatus =
                    ride.getString(
                            "driverDuesStatus"
                    );

            if (!"DUE".equalsIgnoreCase(
                    duesStatus)) {
                continue;
            }

            Long timestamp =
                    getDueTimestamp(ride);

            if (timestamp == null) {
                continue;
            }

            if (
                    oldest == null
                            ||
                    timestamp < oldest
            ) {

                oldest = timestamp;
            }
        }

        return oldest;
    }

    private Long getDueTimestamp(
            DocumentSnapshot ride
    ) {

        String[] fields = {
                "driverDueAt",
                "driverDuesCreatedAt",
                "dueAt",
                "acceptedAt",
                "completedAt",
                "createdAt",
                "requestedAt",
                "updatedAt"
        };

        for (String field :
                fields) {

            Object value =
                    ride.get(field);

            if (value instanceof Number) {

                return ((Number) value)
                        .longValue();
            }

            if (value instanceof Timestamp) {

                return ((Timestamp) value)
                        .toDate()
                        .getTime();
            }
        }

        return null;
    }

    private void addDriverDuesCard(
            LinearLayout parent,
            DriverDuesRecord record
    ) {

        int background;
        int statusColor;

        if ("OVERDUE".equals(
                record.status
        )) {

            background = LIGHT_RED;
            statusColor =
                    Color.rgb(190, 0, 0);

        } else {

            background = LIGHT_YELLOW;
            statusColor =
                    Color.rgb(180, 120, 0);
        }

        LinearLayout card =
                createChildCard(
                        parent,
                        background
                );

        addCardText(
                card,
                statusIcon(record.status)
                        + " "
                        + valueOrDefault(
                        record.name,
                        "Driver name not provided"
                ),
                20,
                statusColor
        );

        addCardText(
                card,
                "📱 Phone: "
                        + valueOrDefault(
                        record.phone,
                        "Phone not provided"
                ),
                15,
                DARK
        );

        addLocationColumns(
                card,
                record.province,
                record.town
        );

        addCardText(
                card,
                "💵 Completed Ride Earnings: "
                        + formatPeso(
                        record.earnings
                ),
                16,
                DARK
        );

        addCardText(
                card,
                "🏦 Sakay Na Fee — 10%: "
                        + formatPeso(
                        record.platformFee
                ),
                16,
                DARK
        );

        addCardText(
                card,
                "💰 OUTSTANDING DUE: "
                        + formatPeso(
                        record.outstanding
                ),
                19,
                Color.rgb(190, 0, 0)
        );

        if (
                record.oldestDueTimestamp
                        != null
        ) {

            addCardText(
                    card,
                    "📅 Oldest DUE: "
                            + formatDate(
                            record.oldestDueTimestamp
                    ),
                    14,
                    GRAY
            );

            addCardText(
                    card,
                    "⏳ Days Unpaid: "
                            + record.daysUnpaid
                            + " day(s)",
                    17,
                    statusColor
            );
        }

        addCardText(
                card,
                "📌 SETTLEMENT STATUS: "
                        + record.status,
                18,
                statusColor
        );

        if ("OVERDUE".equals(
                record.status
        )) {

            addCardText(
                    card,
                    "🔴 OVERDUE MORE THAN 7 DAYS",
                    17,
                    Color.rgb(190, 0, 0)
            );

        } else {

            addCardText(
                    card,
                    "🟡 PAYMENT DUE",
                    16,
                    Color.rgb(180, 120, 0)
            );
        }

        if ("SUSPENDED".equalsIgnoreCase(
                record.accountStatus
        )) {

            addCardText(
                    card,
                    "🚫 DRIVER ACCOUNT: SUSPENDED",
                    17,
                    Color.rgb(190, 0, 0)
            );
        }
    }

    private String statusIcon(
            String status
    ) {

        if ("OVERDUE".equals(status)) {
            return "🔴";
        }

        return "🟡";
    }

    private void addPassengerBookings() {

        if (passengersSection == null) {
            return;
        }

        passengersSection.removeAllViews();

        TextView bookingTitle =
                new TextView(this);

        bookingTitle.setText(
                "🛺 TODAY'S PASSENGER BOOKINGS"
        );

        bookingTitle.setTextSize(20);
        bookingTitle.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );
        bookingTitle.setTextColor(GREEN);
        bookingTitle.setPadding(
                0,
                8,
                0,
                12
        );

        passengersSection.addView(
                bookingTitle
        );

        long startOfToday =
                getStartOfToday();

        List<DocumentSnapshot> todayBookings =
                new ArrayList<>();

        for (DocumentSnapshot ride :
                rideDocuments) {

            Long timestamp =
                    getRideTimestamp(ride);

            if (
                    timestamp == null
                            ||
                    timestamp < startOfToday
            ) {
                continue;
            }

            String passengerId =
                    ride.getString(
                            "passengerId"
                    );

            if (!hasText(passengerId)) {
                continue;
            }

            DocumentSnapshot passenger =
                    usersById.get(passengerId);

            if (
                    passenger == null
                            ||
                    !"PASSENGER".equalsIgnoreCase(
                            passenger.getString("role")
                    )
            ) {
                continue;
            }

            todayBookings.add(ride);
        }

        Collections.sort(
                todayBookings,
                newestFirstComparator()
        );

        if (todayBookings.isEmpty()) {

            addInfoCard(
                    passengersSection,
                    "🛺 TODAY'S BOOKINGS",
                    "No passenger bookings today.",
                    LIGHT_YELLOW
            );

            return;
        }

        for (DocumentSnapshot ride :
                todayBookings) {

            addRideCard(
                    passengersSection,
                    ride
            );
        }
    }

    private long getStartOfToday() {

        Calendar calendar =
                Calendar.getInstance();

        calendar.set(
                Calendar.HOUR_OF_DAY,
                0
        );

        calendar.set(
                Calendar.MINUTE,
                0
        );

        calendar.set(
                Calendar.SECOND,
                0
        );

        calendar.set(
                Calendar.MILLISECOND,
                0
        );

        return calendar.getTimeInMillis();
    }

    private void loadRides() {

        db.collection("rides")
                .get()
                .addOnSuccessListener(snapshot -> {

                    rideDocuments.clear();

                    rideDocuments.addAll(
                            snapshot.getDocuments()
                    );

                    buildRideSections();

                    addPassengerBookings();

                    buildDriverEarningsDues();

                    statusText.setText(
                            "Dashboard loaded."
                    );
                })
                .addOnFailureListener(e -> {

                    buildRideSections();

                    addPassengerBookings();

                    buildDriverEarningsDues();

                    statusText.setText(
                            "Users loaded. Ride history unavailable."
                    );
                });
    }

    private void buildRideSections() {

        historySection = createSection(
                "📋 RIDE HISTORY — LAST 7 DAYS"
        );

        addHistoryNotice(
                historySection
        );

        renderHistory();

        paymentSection = createSection(
                "💰 FARE & PAYMENT — TAP TO OPEN"
        );

        addPaymentFilters(
                paymentSection
        );

        renderPayments();
    }

    private void addHistoryNotice(
            LinearLayout parent
    ) {

        TextView notice =
                new TextView(this);

        notice.setText(
                "Completed, cancelled, declined and expired rides "
                        + "from the last 7 days only. "
                        + "Older ride history is automatically hidden."
        );

        notice.setTextSize(15);
        notice.setTextColor(DARK);
        notice.setPadding(
                0,
                0,
                0,
                12
        );

        parent.addView(notice);
    }

    private void renderHistory() {

        if (historySection == null) {
            return;
        }

        int childCount =
                historySection.getChildCount();

        if (childCount > 1) {

            historySection.removeViews(
                    1,
                    childCount - 1
            );
        }

        long cutoff =
                System.currentTimeMillis()
                        -
                        HISTORY_DAYS
                                * 24L
                                * 60L
                                * 60L
                                * 1000L;

        List<DocumentSnapshot> history =
                new ArrayList<>();

        for (DocumentSnapshot ride :
                rideDocuments) {

            String status =
                    ride.getString("status");

            if (!isHistoryStatus(status)) {
                continue;
            }

            Long timestamp =
                    getRideTimestamp(ride);

            if (
                    timestamp == null
                            ||
                    timestamp < cutoff
            ) {
                continue;
            }

            history.add(ride);
        }

        Collections.sort(
                history,
                newestFirstComparator()
        );

        if (history.isEmpty()) {

            addInfoCard(
                    historySection,
                    "📋 RIDE HISTORY",
                    "No completed/cancelled ride history "
                            + "found in the last 7 days.",
                    LIGHT_YELLOW
            );

            return;
        }

        for (DocumentSnapshot ride :
                history) {

            addRideCard(
                    historySection,
                    ride
            );
        }
    }

    private boolean isHistoryStatus(
            String status
    ) {

        if (!hasText(status)) {
            return false;
        }

        return
                "COMPLETED".equalsIgnoreCase(status)
                        ||
                "CANCELLED".equalsIgnoreCase(status)
                        ||
                "DECLINED".equalsIgnoreCase(status)
                        ||
                "EXPIRED".equalsIgnoreCase(status);
    }

    private void addPaymentFilters(
            LinearLayout parent
    ) {

        TextView label =
                new TextView(this);

        label.setText(
                "💰 FARE & PAYMENT — SELECT PERIOD"
        );

        label.setTextSize(16);
        label.setTextColor(DARK);

        label.setPadding(
                0,
                4,
                0,
                4
        );

        parent.addView(label);

        LinearLayout row =
                new LinearLayout(this);

        row.setOrientation(
                LinearLayout.HORIZONTAL
        );

        Button today =
                new Button(this);

        today.setText("TODAY");

        today.setOnClickListener(v -> {

            paymentDays = 1;

            renderPayments();
        });

        Button week =
                new Button(this);

        week.setText("7 DAYS");

        week.setOnClickListener(v -> {

            paymentDays = 7;

            renderPayments();
        });

        Button month =
                new Button(this);

        month.setText("30 DAYS");

        month.setOnClickListener(v -> {

            paymentDays = 30;

            renderPayments();
        });

        row.addView(
                today,
                weighted()
        );

        row.addView(
                week,
                weighted()
        );

        row.addView(
                month,
                weighted()
        );

        parent.addView(row);
    }

    private void renderPayments() {

        if (paymentSection == null) {
            return;
        }

        int childCount =
                paymentSection.getChildCount();

        if (childCount > 2) {

            paymentSection.removeViews(
                    2,
                    childCount - 2
            );
        }

        long cutoff =
                System.currentTimeMillis()
                        -
                        paymentDays
                                * 24L
                                * 60L
                                * 60L
                                * 1000L;

        double recordedFare = 0;
        double cashTotal = 0;
        double gcashTotal = 0;
        double mayaTotal = 0;

        int recordedCount = 0;
        int cashCount = 0;
        int gcashCount = 0;
        int mayaCount = 0;

        List<DocumentSnapshot> paymentRides =
                new ArrayList<>();

        for (DocumentSnapshot ride :
                rideDocuments) {

            Long timestamp =
                    getRideTimestamp(ride);

            if (
                    timestamp == null
                            ||
                    timestamp < cutoff
            ) {
                continue;
            }

            String status =
                    valueOrDefault(
                            ride.getString("status"),
                            ""
                    );

            if (
                    "REQUESTED".equalsIgnoreCase(status)
                            &&
                    !hasText(
                            ride.getString(
                                    "driverId"
                            )
                    )
            ) {
                continue;
            }

            double fare =
                    readNumber(
                            ride,
                            "fare"
                    );

            if (fare <= 0) {
                continue;
            }

            String payment =
                    normalizePaymentMethod(
                            ride.getString(
                                    "paymentMethod"
                            )
                    );

            recordedFare += fare;
            recordedCount++;

            if ("CASH".equals(payment)) {

                cashTotal += fare;
                cashCount++;

            } else if ("GCASH".equals(payment)) {

                gcashTotal += fare;
                gcashCount++;

            } else if ("MAYA".equals(payment)) {

                mayaTotal += fare;
                mayaCount++;
            }

            paymentRides.add(ride);
        }

        addInfoCard(
                paymentSection,
                "🧾 RIDE PAYMENT TRANSACTIONS",
                String.valueOf(recordedCount),
                LIGHT_GREEN
        );

        addInfoCard(
                paymentSection,
                "💰 TOTAL FARE",
                formatPeso(recordedFare),
                LIGHT_GREEN
        );

        addInfoCard(
                paymentSection,
                "💵 CASH",
                formatPeso(cashTotal)
                        + " • "
                        + cashCount
                        + " transaction(s)",
                LIGHT_YELLOW
        );

        addInfoCard(
                paymentSection,
                "📱 GCASH",
                formatPeso(gcashTotal)
                        + " • "
                        + gcashCount
                        + " transaction(s)",
                LIGHT_BLUE
        );

        addInfoCard(
                paymentSection,
                "📱 MAYA",
                formatPeso(mayaTotal)
                        + " • "
                        + mayaCount
                        + " transaction(s)",
                LIGHT_GREEN
        );

        TextView note =
                new TextView(this);

        note.setText(
                "ℹ️ Fare and payment are read directly "
                        + "from ride records. "
                        + "Cash, GCash and Maya are counted "
                        + "from the ride's payment method. "
                        + "A booking with no fare is not counted."
        );

        note.setTextSize(14);
        note.setTextColor(DARK);
        note.setPadding(
                12,
                12,
                12,
                18
        );

        paymentSection.addView(note);

        TextView transactionTitle =
                new TextView(this);

        transactionTitle.setText(
                "🧾 RECENT FARE & PAYMENT TRANSACTIONS"
        );

        transactionTitle.setTextSize(19);

        transactionTitle.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        transactionTitle.setTextColor(GREEN);

        transactionTitle.setPadding(
                0,
                8,
                0,
                12
        );

        paymentSection.addView(
                transactionTitle
        );

        if (paymentRides.isEmpty()) {

            addInfoCard(
                    paymentSection,
                    "🧾 TRANSACTIONS",
                    "No fare/payment transactions found "
                            + "for the selected period.",
                    LIGHT_YELLOW
            );

            return;
        }

        Collections.sort(
                paymentRides,
                newestFirstComparator()
        );

        for (DocumentSnapshot ride :
                paymentRides) {

            addPaymentTransactionCard(
                    paymentSection,
                    ride
            );
        }
    }

    private void addPaymentTransactionCard(
            LinearLayout parent,
            DocumentSnapshot ride
    ) {

        LinearLayout card =
                createChildCard(
                        parent,
                        LIGHT_BLUE
                );

        String driverId =
                ride.getString("driverId");

        DocumentSnapshot driver =
                usersById.get(driverId);

        String driverName =
                firstNonEmpty(
                        ride.getString("driverName"),
                        driver == null
                                ? ""
                                : firstNonEmpty(
                                driver.getString(
                                        "driverName"
                                ),
                                driver.getString(
                                        "name"
                                )
                        )
                );

        String driverPhone =
                firstNonEmpty(
                        ride.getString("driverPhone"),
                        driver == null
                                ? ""
                                : driver.getString(
                                "phone"
                        )
                );

        String payment =
                normalizePaymentMethod(
                        ride.getString(
                                "paymentMethod"
                        )
                );

        double fare =
                readNumber(
                        ride,
                        "fare"
                );

        String rideStatus =
                valueOrDefault(
                        ride.getString("status"),
                        "UNKNOWN"
                );

        addCardText(
                card,
                "🧾 RIDE #" + ride.getId(),
                18,
                GREEN
        );

        addCardText(
                card,
                "🚕 Driver: "
                        + valueOrDefault(
                        driverName,
                        "Driver not assigned"
                ),
                16,
                DARK
        );

        addCardText(
                card,
                "📱 Driver Phone: "
                        + valueOrDefault(
                        driverPhone,
                        "Phone not available"
                ),
                15,
                DARK
        );

        addCardText(
                card,
                "🚦 Ride Status: "
                        + rideStatus,
                16,
                DARK
        );

        addCardText(
                card,
                "💰 FARE: "
                        + formatPeso(fare),
                19,
                GREEN
        );

        addCardText(
                card,
                "💳 PAYMENT: "
                        + payment,
                17,
                DARK
        );

        Long timestamp =
                getRideTimestamp(ride);

        if (timestamp != null) {

            addCardText(
                    card,
                    "🕒 "
                            + formatDate(timestamp),
                    14,
                    GRAY
            );
        }
    }

    private void addRideCard(
            LinearLayout parent,
            DocumentSnapshot ride
    ) {

        LinearLayout card =
                createChildCard(
                        parent,
                        LIGHT_BLUE
                );

        String pickup =
                firstNonEmpty(
                        ride.getString("pickupName"),
                        ride.getString("pickup")
                );

        String destination =
                firstNonEmpty(
                        ride.getString("destinationName"),
                        ride.getString("destination")
                );

        String status =
                valueOrDefault(
                        ride.getString("status"),
                        "UNKNOWN"
                );

        String payment =
                valueOrDefault(
                        ride.getString("paymentMethod"),
                        "Not provided"
                );

        double fare =
                readNumber(
                        ride,
                        "fare"
                );

        String driverId =
                ride.getString("driverId");

        DocumentSnapshot driver =
                usersById.get(driverId);

        String driverName =
                firstNonEmpty(
                        ride.getString("driverName"),
                        driver == null
                                ? ""
                                : firstNonEmpty(
                                driver.getString(
                                        "driverName"
                                ),
                                driver.getString(
                                        "name"
                                )
                        )
                );

        String driverPhone =
                firstNonEmpty(
                        ride.getString("driverPhone"),
                        driver == null
                                ? ""
                                : driver.getString(
                                "phone"
                        )
                );

        String plate =
                firstNonEmpty(
                        ride.getString(
                                "driverPlateNumber"
                        ),
                        driver == null
                                ? ""
                                : driver.getString(
                                "plateNumber"
                        )
                );

        String vehicle =
                firstNonEmpty(
                        ride.getString(
                                "driverVehicle"
                        ),
                        driver == null
                                ? ""
                                : driver.getString(
                                "vehicleDescription"
                        )
                );

        addCardText(
                card,
                "🛺 RIDE #" + ride.getId(),
                18,
                GREEN
        );

        addCardText(
                card,
                "🛺 PASSENGER BOOKING",
                16,
                DARK
        );

        addCardText(
                card,
                "🚕 DRIVER\n"
                        + valueOrDefault(
                        driverName,
                        "Driver not assigned"
                )
                        + "\n📱 "
                        + valueOrDefault(
                        driverPhone,
                        "Phone not available"
                ),
                16,
                DARK
        );

        if (hasText(plate)) {

            addCardText(
                    card,
                    "🪪 Plate: " + plate,
                    15,
                    DARK
            );
        }

        if (hasText(vehicle)) {

            addCardText(
                    card,
                    "🛺 Tricycle: " + vehicle,
                    15,
                    DARK
            );
        }

        addCardText(
                card,
                "📍 Pickup:\n"
                        + valueOrDefault(
                        pickup,
                        "Not provided"
                ),
                15,
                DARK
        );

        addCardText(
                card,
                "🎯 Destination:\n"
                        + valueOrDefault(
                        destination,
                        "Not provided"
                ),
                15,
                DARK
        );

        addCardText(
                card,
                "💰 Fare: "
                        + formatPeso(fare),
                16,
                DARK
        );

        addCardText(
                card,
                "💳 Payment: " + payment,
                16,
                DARK
        );

        addCardText(
                card,
                "🚦 Status: " + status,
                16,
                DARK
        );

        String date =
                getRideDateText(ride);

        if (hasText(date)) {

            addCardText(
                    card,
                    "🕒 " + date,
                    14,
                    GRAY
            );
        }
    }

    private void addLocationColumns(
            LinearLayout parent,
            String province,
            String town
    ) {

        LinearLayout row =
                new LinearLayout(this);

        row.setOrientation(
                LinearLayout.HORIZONTAL
        );

        row.setPadding(
                0,
                6,
                0,
                6
        );

        LinearLayout provinceColumn =
                new LinearLayout(this);

        provinceColumn.setOrientation(
                LinearLayout.VERTICAL
        );

        LinearLayout townColumn =
                new LinearLayout(this);

        townColumn.setOrientation(
                LinearLayout.VERTICAL
        );

        TextView provinceLabel =
                new TextView(this);

        provinceLabel.setText(
                "🗺️ PROVINCE"
        );

        provinceLabel.setTextSize(13);

        provinceLabel.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        provinceLabel.setTextColor(GRAY);

        TextView provinceValue =
                new TextView(this);

        provinceValue.setText(
                valueOrDefault(
                        province,
                        "PROVINCE NOT PROVIDED"
                )
        );

        provinceValue.setTextSize(16);
        provinceValue.setTextColor(DARK);

        provinceColumn.addView(
                provinceLabel
        );

        provinceColumn.addView(
                provinceValue
        );

        TextView townLabel =
                new TextView(this);

        townLabel.setText(
                "🏘️ TOWN"
        );

        townLabel.setTextSize(13);

        townLabel.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        townLabel.setTextColor(GRAY);

        TextView townValue =
                new TextView(this);

        townValue.setText(
                valueOrDefault(
                        town,
                        "TOWN NOT PROVIDED"
                )
        );

        townValue.setTextSize(16);
        townValue.setTextColor(DARK);

        townColumn.addView(
                townLabel
        );

        townColumn.addView(
                townValue
        );

        row.addView(
                provinceColumn,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                )
        );

        row.addView(
                townColumn,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                )
        );

        parent.addView(row);
    }

    private void addDriverCard(
            LinearLayout parent,
            DocumentSnapshot driver,
            boolean approvalOnly
    ) {

        LinearLayout card =
                createChildCard(
                        parent,
                        approvalOnly
                                ? LIGHT_YELLOW
                                : LIGHT_GREEN
                );

        String name =
                firstNonEmpty(
                        driver.getString("driverName"),
                        driver.getString("name")
                );

        String phone =
                driver.getString("phone");

        String town =
                driver.getString("town");

        String province =
                driver.getString("province");

        String plate =
                driver.getString("plateNumber");

        String franchise =
                driver.getString("franchiseNumber");

        String vehicle =
                driver.getString(
                        "vehicleDescription"
                );

        String approval =
                getApprovalStatus(driver);

        boolean online =
                Boolean.TRUE.equals(
                        driverOnlineById.get(
                                driver.getId()
                        )
                );

        addCardText(
                card,
                "🚕 "
                        + valueOrDefault(
                        name,
                        "Driver name not provided"
                ),
                19,
                GREEN
        );

        addCardText(
                card,
                "📱 Phone: "
                        + valueOrDefault(
                        phone,
                        "Phone not provided"
                ),
                16,
                DARK
        );

        addLocationColumns(
                card,
                province,
                town
        );

        if (hasText(plate)) {

            addCardText(
                    card,
                    "🪪 Plate Number: "
                            + plate,
                    16,
                    DARK
            );
        }

        if (hasText(franchise)) {

            addCardText(
                    card,
                    "📄 Franchise Number: "
                            + franchise,
                    16,
                    DARK
            );
        }

        if (hasText(vehicle)) {

            addCardText(
                    card,
                    "🛺 Tricycle: "
                            + vehicle,
                    16,
                    DARK
            );
        }

        addCardText(
                card,
                "Approval: " + approval,
                16,
                DARK
        );

        addCardText(
                card,
                "Online: "
                        + (
                        online
                                ? "🟢 YES"
                                : "🔴 NO"
                ),
                16,
                DARK
        );

        if (approvalOnly) {

            Button approve =
                    new Button(this);

            approve.setText(
                    "✅ APPROVE DRIVER"
            );

            approve.setOnClickListener(
                    v -> approveDriver(
                            driver.getId()
                    )
            );

            card.addView(approve);

            Button reject =
                    new Button(this);

            reject.setText(
                    "❌ REJECT & ERASE DRIVER"
            );

            reject.setOnClickListener(
                    v -> rejectDriver(
                            driver.getId()
                    )
            );

            card.addView(reject);
        }
    }

    private void approveDriver(
            String driverId
    ) {

        db.collection("users")
                .document(driverId)
                .update(
                        "approved",
                        true,
                        "approvalStatus",
                        "APPROVED",
                        "driverStatus",
                        "APPROVED",
                        "canAcceptRides",
                        true,
                        "online",
                        false,
                        "approvedAt",
                        System.currentTimeMillis()
                )
                .addOnSuccessListener(v -> {

                    Toast.makeText(
                            this,
                            "Driver approved. Driver removed from pending applications.",
                            Toast.LENGTH_LONG
                    ).show();

                    loadDashboard();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Approval failed:\n"
                                        + safeMessage(e),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void rejectDriver(
            String driverId
    ) {

        db.collection("users")
                .document(driverId)
                .delete()
                .addOnSuccessListener(v -> {

                    db.collection("driverLocations")
                            .document(driverId)
                            .delete()
                            .addOnCompleteListener(
                                    ignored -> {

                                        Toast.makeText(
                                                this,
                                                "Driver rejected and erased from Firestore.",
                                                Toast.LENGTH_LONG
                                        ).show();

                                        loadDashboard();
                                    }
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Driver erase failed:\n"
                                        + safeMessage(e),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private String getApprovalStatus(
            DocumentSnapshot user
    ) {

        String status =
                user.getString(
                        "approvalStatus"
                );

        if (hasText(status)) {
            return status;
        }

        Boolean approved =
                user.getBoolean(
                        "approved"
                );

        if (Boolean.TRUE.equals(approved)) {
            return "APPROVED";
        }

        return "PENDING_APPROVAL";
    }

    private String getDisplayName(
            DocumentSnapshot user
    ) {

        String name =
                firstNonEmpty(
                        user.getString(
                                "driverName"
                        ),
                        user.getString(
                                "name"
                        )
                );

        return valueOrDefault(
                name,
                "Unnamed User"
        );
    }

    private LinearLayout createSection(
            String title
    ) {

        LinearLayout section =
                new LinearLayout(this);

        section.setOrientation(
                LinearLayout.VERTICAL
        );

        section.setVisibility(
                View.GONE
        );

        Button sectionButton =
                new Button(this);

        sectionButton.setText(title);
        sectionButton.setTextSize(17);
        sectionButton.setTextColor(Color.WHITE);
        sectionButton.setBackgroundColor(GREEN);

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

        contentContainer.addView(
                sectionButton,
                params
        );

        contentContainer.addView(
                section
        );

        sectionButton.setOnClickListener(v -> {

            if (
                    section.getVisibility()
                            == View.VISIBLE
            ) {

                section.setVisibility(
                        View.GONE
                );

            } else {

                section.setVisibility(
                        View.VISIBLE
                );
            }
        });

        return section;
    }

    private boolean isActiveStatus(
            String status
    ) {

        if (!hasText(status)) {
            return false;
        }

        return
                "REQUESTED".equalsIgnoreCase(status)
                        ||
                "ACCEPTED".equalsIgnoreCase(status)
                        ||
                "DRIVER_ON_THE_WAY".equalsIgnoreCase(status)
                        ||
                "DRIVER_ARRIVED".equalsIgnoreCase(status)
                        ||
                "IN_PROGRESS".equalsIgnoreCase(status)
                        ||
                "ARRIVED".equalsIgnoreCase(status)
                        ||
                "ONGOING".equalsIgnoreCase(status);
    }

    private Long getRideTimestamp(
            DocumentSnapshot ride
    ) {

        String[] fields = {
                "completedAt",
                "cancelledAt",
                "declinedAt",
                "expiredAt",
                "acceptedAt",
                "requestedAt",
                "createdAt",
                "updatedAt"
        };

        for (String field :
                fields) {

            Object value =
                    ride.get(field);

            if (value instanceof Number) {

                return ((Number) value)
                        .longValue();
            }

            if (value instanceof Timestamp) {

                return ((Timestamp) value)
                        .toDate()
                        .getTime();
            }
        }

        return null;
    }

    private String getRideDateText(
            DocumentSnapshot ride
    ) {

        Long timestamp =
                getRideTimestamp(ride);

        if (timestamp == null) {
            return "";
        }

        SimpleDateFormat format =
                new SimpleDateFormat(
                        "MMM dd, yyyy hh:mm a",
                        Locale.getDefault()
                );

        return format.format(
                new Date(timestamp)
        );
    }

    private Comparator<DocumentSnapshot>
    newestFirstComparator() {

        return (ride1, ride2) -> {

            Long time1 =
                    getRideTimestamp(ride1);

            Long time2 =
                    getRideTimestamp(ride2);

            if (
                    time1 == null
                            &&
                    time2 == null
            ) {
                return 0;
            }

            if (time1 == null) {
                return 1;
            }

            if (time2 == null) {
                return -1;
            }

            return Long.compare(
                    time2,
                    time1
            );
        };
    }

    private LinearLayout createChildCard(
            LinearLayout parent,
            int background
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

        card.setBackgroundColor(
                background
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(
                0,
                0,
                0,
                14
        );

        parent.addView(
                card,
                params
        );

        return card;
    }

    private void addInfoCard(
            LinearLayout parent,
            String title,
            String value,
            int background
    ) {

        LinearLayout card =
                createChildCard(
                        parent,
                        background
                );

        addCardText(
                card,
                title,
                17,
                DARK
        );

        addCardText(
                card,
                value,
                23,
                GREEN
        );
    }

    private void addCardText(
            LinearLayout card,
            String value,
            float size,
            int color
    ) {

        TextView text =
                new TextView(this);

        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);

        text.setPadding(
                0,
                4,
                0,
                4
        );

        card.addView(text);
    }

    private LinearLayout.LayoutParams weighted() {

        return new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
    }

    private double readNumber(
            DocumentSnapshot snapshot,
            String field
    ) {

        try {

            Object value =
                    snapshot.get(field);

            if (value instanceof Number) {

                return ((Number) value)
                        .doubleValue();
            }

            if (value instanceof String) {

                String text =
                        ((String) value).trim();

                if (!text.isEmpty()) {

                    return Double.parseDouble(
                            text
                    );
                }
            }

        } catch (Exception ignored) {
        }

        return 0;
    }

    private String normalizePaymentMethod(
            String payment
    ) {

        if (!hasText(payment)) {
            return "NOT PROVIDED";
        }

        String value =
                payment.trim()
                        .toUpperCase(
                                Locale.US
                        );

        if (value.contains("GCASH")) {
            return "GCASH";
        }

        if (
                value.contains("MAYA")
                        ||
                value.contains("PAYMAYA")
        ) {
            return "MAYA";
        }

        if (value.contains("CASH")) {
            return "CASH";
        }

        return value;
    }

    private String formatPeso(
            double amount
    ) {

        return "₱"
                + String.format(
                Locale.US,
                "%.2f",
                amount
        );
    }

    private String formatDate(
            long timestamp
    ) {

        SimpleDateFormat format =
                new SimpleDateFormat(
                        "MMM dd, yyyy hh:mm a",
                        Locale.getDefault()
                );

        return format.format(
                new Date(timestamp)
        );
    }

    private String firstNonEmpty(
            String first,
            String second
    ) {

        if (hasText(first)) {
            return first;
        }

        if (hasText(second)) {
            return second;
        }

        return "";
    }

    private String valueOrDefault(
            String value,
            String fallback
    ) {

        return hasText(value)
                ? value
                : fallback;
    }

    private boolean hasText(
            String value
    ) {

        return value != null
                &&
                !value.trim().isEmpty();
    }

    private String safeMessage(
            Exception e
    ) {

        if (
                e == null
                        ||
                e.getMessage() == null
                        ||
                e.getMessage()
                        .trim()
                        .isEmpty()
        ) {

            return "Unknown Firebase error.";
        }

        return e.getMessage();
    }

    private void logout() {

        auth.signOut();

        Intent intent =
                new Intent(
                        this,
                        MainActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        |
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        finish();
    }

    @Override
    protected void onDestroy() {

        if (driversListener != null) {

            driversListener.remove();

            driversListener = null;
        }

        super.onDestroy();
    }

    private static class DriverDuesRecord {

        String driverId = "";

        String name = "";

        String phone = "";

        String province = "";

        String town = "";

        String approvalStatus = "";

        String accountStatus = "";

        double earnings = 0;

        double platformFee = 0;

        double paid = 0;

        double outstanding = 0;

        Long oldestDueTimestamp = null;

        long daysUnpaid = 0;

        String status = "DUE";

        /*
         * 0 = OVERDUE
         * 1 = DUE
         */
        int statusRank = 1;
    }
}
