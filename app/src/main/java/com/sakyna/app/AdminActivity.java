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
import java.util.TreeMap;

public class AdminActivity extends Activity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private LinearLayout contentContainer;
    private TextView statusText;

    private LinearLayout overviewSection;
    private LinearLayout passengersSection;
    private LinearLayout driversSection;
    private LinearLayout onlineDriversSection;
    private LinearLayout approvalSection;
    private LinearLayout historySection;
    private LinearLayout paymentSection;
    private LinearLayout suspendedDriversSection;
    private LinearLayout driverEarningsDuesSection;

    private final Map<String, DocumentSnapshot> usersById =
            new HashMap<>();

    private final List<DocumentSnapshot> rideDocuments =
            new ArrayList<>();

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
                                user.getBoolean("online")
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
                "👤 TOTAL PASSENGERS",
                String.valueOf(passengerCount),
                LIGHT_BLUE
        );

        addInfoCard(
                overviewSection,
                "🚕 TOTAL DRIVERS",
                String.valueOf(driverCount),
                LIGHT_GREEN
        );

        addInfoCard(
                overviewSection,
                "⏳ PENDING DRIVERS",
                String.valueOf(pendingCount),
                LIGHT_YELLOW
        );

        addInfoCard(
                overviewSection,
                "✅ APPROVED DRIVERS",
                String.valueOf(approvedCount),
                LIGHT_GREEN
        );

        addInfoCard(
                overviewSection,
                "🟢 DRIVERS ONLINE",
                String.valueOf(onlineDrivers),
                LIGHT_GREEN
        );

        /*
         * ADMIN LIST ORDER
         *
         * 1. Passenger Management
         * 2. Driver Approval Applications
         * 3. Approved Drivers
         * 4. Online Drivers
         * 5. Suspended Drivers
         * 6. Driver Earnings & Dues
         * 7. Ride History
         * 8. Payments
         */
        buildPassengers();
        buildPendingDrivers();
        buildGroupedDrivers();
        buildOnlineDrivers();
        buildSuspendedDrivers();

        /*
         * This section is populated after rides are loaded.
         */
        driverEarningsDuesSection = createSection(
                "💰 DRIVER EARNINGS & DUES"
        );

        addInfoCard(
                driverEarningsDuesSection,
                "💰 DRIVER EARNINGS & DUES",
                "Loading driver earnings and settlement status...",
                LIGHT_BLUE
        );
    }

    /*
     * ============================================================
     * PASSENGER MANAGEMENT
     * ============================================================
     */
    private void buildPassengers() {

        passengersSection = createSection(
                "👤 PASSENGER MANAGEMENT — NEW TODAY"
        );

        List<DocumentSnapshot> passengers =
                new ArrayList<>();

        for (DocumentSnapshot user :
                usersById.values()) {

            if (!"PASSENGER".equalsIgnoreCase(
                    user.getString("role"))) {
                continue;
            }

            if (!isRegisteredToday(user)) {
                continue;
            }

            passengers.add(user);
        }

        Collections.sort(
                passengers,
                (a, b) -> {

                    Long timeA =
                            getUserTimestamp(
                                    a,
                                    "createdAt",
                                    "registeredAt"
                            );

                    Long timeB =
                            getUserTimestamp(
                                    b,
                                    "createdAt",
                                    "registeredAt"
                            );

                    if (
                            timeA == null
                            &&
                            timeB == null
                    ) {

                        return getDisplayName(a)
                                .compareToIgnoreCase(
                                        getDisplayName(b)
                                );
                    }

                    if (timeA == null) {
                        return 1;
                    }

                    if (timeB == null) {
                        return -1;
                    }

                    return Long.compare(
                            timeB,
                            timeA
                    );
                }
        );

        if (passengers.isEmpty()) {

            addInfoCard(
                    passengersSection,
                    "👤 NEW PASSENGER ACCOUNTS",
                    "No passenger accounts registered today.",
                    LIGHT_YELLOW
            );

        } else {

            for (DocumentSnapshot passenger :
                    passengers) {

                addPassengerCard(
                        passengersSection,
                        passenger
                );
            }
        }
    }

    /*
     * ============================================================
     * PASSENGER BOOKINGS
     * ============================================================
     */
    private void addPassengerBookings() {

        if (passengersSection == null) {
            return;
        }

        TextView bookingTitle =
                new TextView(this);

        bookingTitle.setText(
                "🛺 CURRENT PASSENGER BOOKINGS"
        );

        bookingTitle.setTextSize(20);
        bookingTitle.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );
        bookingTitle.setTextColor(GREEN);
        bookingTitle.setPadding(
                0,
                18,
                0,
                12
        );

        passengersSection.addView(
                bookingTitle
        );

        List<DocumentSnapshot> activeBookings =
                new ArrayList<>();

        for (DocumentSnapshot ride :
                rideDocuments) {

            String status =
                    ride.getString("status");

            if (!isActiveStatus(status)) {
                continue;
            }

            String passengerId =
                    ride.getString("passengerId");

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

            activeBookings.add(ride);
        }

        Collections.sort(
                activeBookings,
                newestFirstComparator()
        );

        if (activeBookings.isEmpty()) {

            addInfoCard(
                    passengersSection,
                    "🛺 CURRENT BOOKINGS",
                    "No active passenger bookings.",
                    LIGHT_GREEN
            );

            return;
        }

        for (DocumentSnapshot ride :
                activeBookings) {

            addRideCard(
                    passengersSection,
                    ride
            );
        }
    }

    /*
     * ============================================================
     * DRIVER APPROVAL APPLICATIONS
     * ============================================================
     */
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

    /*
     * ============================================================
     * APPROVED DRIVERS
     * ============================================================
     */
    private void buildGroupedDrivers() {

        driversSection = createSection(
                "🚕 APPROVED DRIVERS — PROVINCE → TOWN/CITY"
        );

        Map<String, Map<String, List<DocumentSnapshot>>>
                grouped =
                new TreeMap<>(
                        String.CASE_INSENSITIVE_ORDER
                );

        for (DocumentSnapshot driver :
                usersById.values()) {

            if (!"DRIVER".equalsIgnoreCase(
                    driver.getString("role"))) {
                continue;
            }

            if (!"APPROVED".equalsIgnoreCase(
                    getApprovalStatus(driver))) {
                continue;
            }

            String province =
                    cleanLocation(
                            driver.getString("province"),
                            "PROVINCE NOT PROVIDED"
                    );

            String town =
                    cleanLocation(
                            driver.getString("town"),
                            "TOWN/CITY NOT PROVIDED"
                    );

            Map<String, List<DocumentSnapshot>>
                    towns =
                    grouped.get(province);

            if (towns == null) {

                towns =
                        new TreeMap<>(
                                String.CASE_INSENSITIVE_ORDER
                        );

                grouped.put(
                        province,
                        towns
                );
            }

            List<DocumentSnapshot> drivers =
                    towns.get(town);

            if (drivers == null) {

                drivers =
                        new ArrayList<>();

                towns.put(
                        town,
                        drivers
                );
            }

            drivers.add(driver);
        }

        if (grouped.isEmpty()) {

            addInfoCard(
                    driversSection,
                    "🚕 APPROVED DRIVERS",
                    "No approved drivers found.",
                    LIGHT_YELLOW
            );

            return;
        }

        for (Map.Entry<
                String,
                Map<String, List<DocumentSnapshot>>
                > provinceEntry :
                grouped.entrySet()) {

            addGroupHeader(
                    driversSection,
                    "🗺️ " + provinceEntry.getKey(),
                    LIGHT_GREEN
            );

            Map<String, List<DocumentSnapshot>>
                    towns =
                    provinceEntry.getValue();

            for (Map.Entry<
                    String,
                    List<DocumentSnapshot>
                    > townEntry :
                    towns.entrySet()) {

                addGroupHeader(
                        driversSection,
                        "🏘️ " + townEntry.getKey(),
                        LIGHT_BLUE
                );

                List<DocumentSnapshot> drivers =
                        townEntry.getValue();

                Collections.sort(
                        drivers,
                        (a, b) ->
                                getDisplayName(a)
                                        .compareToIgnoreCase(
                                                getDisplayName(b)
                                        )
                );

                for (DocumentSnapshot driver :
                        drivers) {

                    addDriverCard(
                            driversSection,
                            driver,
                            false
                    );
                }
            }
        }
    }

    /*
     * ============================================================
     * ONLINE DRIVERS
     * ============================================================
     */
    private void buildOnlineDrivers() {

        onlineDriversSection = createSection(
                "🟢 ONLINE DRIVERS — LIVE"
        );

        List<DocumentSnapshot> online =
                new ArrayList<>();

        for (DocumentSnapshot driver :
                usersById.values()) {

            if (!"DRIVER".equalsIgnoreCase(
                    driver.getString("role"))) {
                continue;
            }

            if (!"APPROVED".equalsIgnoreCase(
                    getApprovalStatus(driver))) {
                continue;
            }

            if (!Boolean.TRUE.equals(
                    driver.getBoolean("online"))) {
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
    }

    /*
     * ============================================================
     * SUSPENDED DRIVERS
     * ============================================================
     */
    private void buildSuspendedDrivers() {

        suspendedDriversSection = createSection(
                "🚫 SUSPENDED DRIVERS — UNPAID DUES"
        );

        List<DocumentSnapshot> suspended =
                new ArrayList<>();

        for (DocumentSnapshot user :
                usersById.values()) {

            if (!"DRIVER".equalsIgnoreCase(
                    user.getString("role"))) {
                continue;
            }

            if (!"SUSPENDED".equalsIgnoreCase(
                    user.getString("driverAccountStatus"))) {
                continue;
            }

            suspended.add(user);
        }

        Collections.sort(
                suspended,
                (a, b) ->
                        getDisplayName(a)
                                .compareToIgnoreCase(
                                        getDisplayName(b)
                                )
        );

        if (suspended.isEmpty()) {

            addInfoCard(
                    suspendedDriversSection,
                    "🚫 SUSPENDED DRIVERS",
                    "No suspended drivers.",
                    LIGHT_GREEN
            );

            return;
        }

        for (DocumentSnapshot driver :
                suspended) {

            addSuspendedDriverCard(
                    suspendedDriversSection,
                    driver
            );
        }
    }

    /*
     * ============================================================
     * DRIVER EARNINGS & DUES MONITOR
     * ============================================================
     *
     * This section lets Admin see which drivers have earned,
     * how much the 10% platform fee is, how much is paid,
     * what remains outstanding, and how long the oldest DUE
     * has remained unpaid.
     */
    private void buildDriverEarningsDues() {

        if (driverEarningsDuesSection == null) {
            return;
        }

        driverEarningsDuesSection.removeAllViews();

        double totalEarnings = 0;
        double totalPlatformFees = 0;
        double totalOutstanding = 0;
        double totalPaid = 0;

        int settledCount = 0;
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

            records.add(record);

            totalEarnings += record.earnings;
            totalPlatformFees += record.platformFee;
            totalOutstanding += record.outstanding;
            totalPaid += record.paid;

            if ("SETTLED".equals(record.status)) {
                settledCount++;
            } else if ("OVERDUE".equals(record.status)) {
                overdueCount++;
            } else {
                dueCount++;
            }
        }

        addInfoCard(
                driverEarningsDuesSection,
                "💵 TOTAL DRIVER EARNINGS",
                formatPeso(totalEarnings),
                LIGHT_GREEN
        );

        addInfoCard(
                driverEarningsDuesSection,
                "🏦 TOTAL SAKAY NA FEE — 10%",
                formatPeso(totalPlatformFees),
                LIGHT_BLUE
        );

        addInfoCard(
                driverEarningsDuesSection,
                "✅ TOTAL SETTLED / PAID",
                formatPeso(totalPaid),
                LIGHT_GREEN
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
                "🟢 SETTLED DRIVERS",
                String.valueOf(settledCount),
                LIGHT_GREEN
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

        TextView explanation =
                new TextView(this);

        explanation.setText(
                "ℹ️ Earnings are calculated from completed rides. "
                        + "The Sakay Na platform fee is 10%. "
                        + "Outstanding balance comes from the driver's "
                        + "driverSettlementBalance. "
                        + "A DUE becomes OVERDUE when the oldest unpaid "
                        + "DUE ride is more than 7 days old."
        );

        explanation.setTextSize(14);
        explanation.setTextColor(DARK);
        explanation.setPadding(
                12,
                12,
                12,
                18
        );

        driverEarningsDuesSection.addView(
                explanation
        );

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

        TextView allTitle =
                new TextView(this);

        allTitle.setText(
                "👥 ALL DRIVER SETTLEMENT STATUS"
        );

        allTitle.setTextSize(20);
        allTitle.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );
        allTitle.setTextColor(GREEN);
        allTitle.setPadding(
                0,
                10,
                0,
                12
        );

        driverEarningsDuesSection.addView(
                allTitle
        );

        for (DriverDuesRecord record :
                records) {

            addDriverDuesCard(
                    driverEarningsDuesSection,
                    record
            );
        }

        if (records.isEmpty()) {

            addInfoCard(
                    driverEarningsDuesSection,
                    "💰 DRIVER DUES",
                    "No driver accounts found.",
                    LIGHT_YELLOW
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

        /*
         * Existing driverSettlementBalance is the authoritative
         * outstanding balance already maintained by the app.
         */
        record.outstanding =
                Math.max(
                        0,
                        readNumber(
                                driver,
                                "driverSettlementBalance"
                        )
                );

        /*
         * Driver earnings are calculated from COMPLETED rides.
         */
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

        /*
         * The driver's current outstanding balance tells us how
         * much of the platform fee remains unpaid.
         *
         * Paid = assessed platform fee - current outstanding.
         */
        record.paid =
                Math.max(
                        0,
                        record.platformFee
                                - record.outstanding
                );

        /*
         * Find the oldest ride still marked DUE for this driver.
         */
        record.oldestDueTimestamp =
                findOldestDueTimestamp(
                        record.driverId
                );

        if (record.outstanding <= 0.009) {

            record.status =
                    "SETTLED";

            record.statusRank = 3;

            record.daysUnpaid = 0;

        } else if (
                record.oldestDueTimestamp != null
        ) {

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

            /*
             * If a balance exists but no DUE ride timestamp is
             * available, keep the driver visible as DUE rather
             * than incorrectly calling the driver OVERDUE.
             */
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

        } else if ("DUE".equals(
                record.status
        )) {

            background = LIGHT_YELLOW;
            statusColor =
                    Color.rgb(180, 120, 0);

        } else {

            background = LIGHT_GREEN;
            statusColor = GREEN;
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
                "✅ Paid / Settled: "
                        + formatPeso(
                        record.paid
                ),
                16,
                GREEN
        );

        addCardText(
                card,
                "💰 OUTSTANDING DUE: "
                        + formatPeso(
                        record.outstanding
                ),
                19,
                record.outstanding > 0
                        ? Color.rgb(190, 0, 0)
                        : GREEN
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

            if (record.outstanding > 0) {

                addCardText(
                        card,
                        "⏳ Days Unpaid: "
                                + record.daysUnpaid
                                + " day(s)",
                        17,
                        statusColor
                );
            }
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

        } else if ("DUE".equals(
                record.status
        )) {

            addCardText(
                    card,
                    "🟡 PAYMENT DUE",
                    16,
                    Color.rgb(180, 120, 0)
            );

        } else {

            addCardText(
                    card,
                    "🟢 FULLY SETTLED",
                    16,
                    GREEN
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

        if ("DUE".equals(status)) {
            return "🟡";
        }

        return "🟢";
    }

    /*
     * ============================================================
     * SUSPENDED DRIVER CARD
     * ============================================================
     */
    private void addSuspendedDriverCard(
            LinearLayout parent,
            DocumentSnapshot driver
    ) {

        LinearLayout card =
                createChildCard(
                        parent,
                        LIGHT_RED
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
                driver.getString("vehicleDescription");

        String reason =
                valueOrDefault(
                        driver.getString(
                                "suspensionReason"
                        ),
                        "SUSPENDED"
                );

        double balance =
                readNumber(
                        driver,
                        "driverSettlementBalance"
                );

        addCardText(
                card,
                "🚫 "
                        + valueOrDefault(
                        name,
                        "Driver name not provided"
                ),
                20,
                Color.rgb(190, 0, 0)
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
                "⚠️ Reason: " + reason,
                17,
                Color.rgb(190, 0, 0)
        );

        addCardText(
                card,
                "💰 Unpaid Platform Fee: "
                        + formatPeso(balance),
                18,
                Color.rgb(190, 0, 0)
        );

        Long suspendedAt =
                getUserTimestamp(
                        driver,
                        "suspendedAt"
                );

        if (suspendedAt != null) {

            addCardText(
                    card,
                    "🕒 Suspended: "
                            + formatDate(suspendedAt),
                    14,
                    GRAY
            );
        }

        Long deadline =
                getUserTimestamp(
                        driver,
                        "suspensionDeadlineAt"
                );

        if (deadline != null) {

            addCardText(
                    card,
                    "⏰ Deadline: "
                            + formatDate(deadline),
                    14,
                    GRAY
            );
        }

        addCardText(
                card,
                "🔴 ONLINE: NOT ALLOWED",
                17,
                Color.rgb(190, 0, 0)
        );

        TextView note =
                new TextView(this);

        note.setText(
                "Automatic suspension for unpaid platform dues. "
                        + "Driver is restored automatically after the "
                        + "outstanding DUE rides are fully paid and verified."
        );

        note.setTextSize(14);
        note.setTextColor(DARK);
        note.setPadding(
                0,
                8,
                0,
                0
        );

        card.addView(note);
    }

    /*
     * ============================================================
     * USER TIMESTAMP HELPERS
     * ============================================================
     */

    private Long getUserTimestamp(
            DocumentSnapshot user,
            String... fields
    ) {

        for (String field : fields) {

            Object value =
                    user.get(field);

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

    private boolean isRegisteredToday(
            DocumentSnapshot user
    ) {

        Long timestamp =
                getUserTimestamp(
                        user,
                        "createdAt",
                        "registeredAt"
                );

        if (timestamp == null) {
            return false;
        }

        Calendar today =
                Calendar.getInstance();

        Calendar registration =
                Calendar.getInstance();

        registration.setTimeInMillis(
                timestamp
        );

        return
                today.get(Calendar.YEAR)
                        ==
                registration.get(Calendar.YEAR)
                        &&
                today.get(Calendar.DAY_OF_YEAR)
                        ==
                registration.get(
                        Calendar.DAY_OF_YEAR
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

    /*
     * ============================================================
     * RIDES
     * ============================================================
     */

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

                    /*
                     * Driver Earnings & Dues uses both users
                     * and rides, so it is rendered only after
                     * rides have finished loading.
                     */
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

    /*
     * ============================================================
     * RIDE SECTIONS
     * ============================================================
     */

    private void buildRideSections() {

        historySection = createSection(
                "📋 RIDE HISTORY — LAST 7 DAYS"
        );

        addHistoryNotice(
                historySection
        );

        renderHistory();

        paymentSection = createSection(
                "💰 FARE & PAYMENT"
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

    /*
     * ============================================================
     * 7-DAY RIDE HISTORY
     * ============================================================
     */

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

    /*
     * ============================================================
     * PAYMENTS
     * ============================================================
     */

    private void addPaymentFilters(
            LinearLayout parent
    ) {

        TextView label = new TextView(this);
        label.setText(
                "Show Admin transactions:"
        );
        label.setTextSize(16);
        label.setTextColor(DARK);
        parent.addView(label);

        LinearLayout row =
                new LinearLayout(this);

        row.setOrientation(
                LinearLayout.HORIZONTAL
        );

        Button today = new Button(this);
        today.setText("TODAY");
        today.setOnClickListener(v -> {
            paymentDays = 1;
            renderPayments();
        });

        Button week = new Button(this);
        week.setText("7 DAYS");
        week.setOnClickListener(v -> {
            paymentDays = 7;
            renderPayments();
        });

        Button month = new Button(this);
        month.setText("30 DAYS");
        month.setOnClickListener(v -> {
            paymentDays = 30;
            renderPayments();
        });

        row.addView(today, weighted());
        row.addView(week, weighted());
        row.addView(month, weighted());

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

            Boolean recorded =
                    ride.getBoolean(
                            "adminTransactionRecorded"
                    );

            if (!Boolean.TRUE.equals(recorded)) {
                continue;
            }

            Long timestamp =
                    getAdminTransactionTimestamp(ride);

            if (
                    timestamp == null
                    ||
                    timestamp < cutoff
            ) {
                continue;
            }

            double fare =
                    readNumber(
                            ride,
                            "fare"
                    );

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
                "🧾 ADMIN TRANSACTIONS",
                String.valueOf(recordedCount),
                LIGHT_GREEN
        );

        addInfoCard(
                paymentSection,
                "💰 RECORDED FARE",
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
                "ℹ️ Admin transaction is recorded "
                        + "when the driver accepts the booking. "
                        + "It remains recorded even before "
                        + "ride completion. Passenger cancellation "
                        + "while the booking is still REQUESTED "
                        + "does not create an Admin transaction."
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
                "🧾 ACCEPTED BOOKING TRANSACTIONS"
        );

        transactionTitle.setTextSize(19);

        transactionTitle.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        transactionTitle.setTextColor(GREEN);

        paymentSection.addView(
                transactionTitle
        );

        if (paymentRides.isEmpty()) {

            addInfoCard(
                    paymentSection,
                    "🧾 TRANSACTIONS",
                    "No recorded Admin transactions "
                            + "for the last "
                            + paymentDays
                            + " day(s).",
                    LIGHT_YELLOW
            );

            return;
        }

        Collections.sort(
                paymentRides,
                (a, b) -> {

                    Long timeA =
                            getAdminTransactionTimestamp(a);

                    Long timeB =
                            getAdminTransactionTimestamp(b);

                    if (
                            timeA == null
                            &&
                            timeB == null
                    ) {
                        return 0;
                    }

                    if (timeA == null) {
                        return 1;
                    }

                    if (timeB == null) {
                        return -1;
                    }

                    return Long.compare(
                            timeB,
                            timeA
                    );
                }
        );

        for (DocumentSnapshot ride :
                paymentRides) {

            addPaymentTransactionCard(
                    paymentSection,
                    ride
            );
        }
    }

    private Long getAdminTransactionTimestamp(
            DocumentSnapshot ride
    ) {

        Object value =
                ride.get(
                        "adminTransactionRecordedAt"
                );

        if (value instanceof Number) {

            return ((Number) value)
                    .longValue();
        }

        if (value instanceof Timestamp) {

            return ((Timestamp) value)
                    .toDate()
                    .getTime();
        }

        return getRideTimestamp(ride);
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

        String passengerId =
                ride.getString("passengerId");

        String driverId =
                ride.getString("driverId");

        DocumentSnapshot passenger =
                usersById.get(passengerId);

        DocumentSnapshot driver =
                usersById.get(driverId);

        String passengerName =
                firstNonEmpty(
                        ride.getString("passengerName"),
                        passenger == null
                                ? ""
                                : passenger.getString("name")
                );

        String passengerPhone =
                firstNonEmpty(
                        ride.getString("passengerPhone"),
                        passenger == null
                                ? ""
                                : passenger.getString("phone")
                );

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
                                : driver.getString("phone")
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

        addCardText(
                card,
                "🧾 RIDE #" + ride.getId(),
                18,
                GREEN
        );

        addCardText(
                card,
                "👤 Passenger: "
                        + valueOrDefault(
                        passengerName,
                        "Name not provided"
                ),
                16,
                DARK
        );

        addCardText(
                card,
                "📱 Passenger Phone: "
                        + valueOrDefault(
                        passengerPhone,
                        "Phone not provided"
                ),
                15,
                DARK
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

        String adminTransactionStatus =
                valueOrDefault(
                        ride.getString(
                                "adminTransactionStatus"
                        ),
                        "RECORDED"
                );

        String rideStatus =
                valueOrDefault(
                        ride.getString("status"),
                        "UNKNOWN"
                );

        addCardText(
                card,
                "🧾 Admin Transaction: "
                        + adminTransactionStatus,
                16,
                GREEN
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
                "💰 Fare: "
                        + formatPeso(fare),
                18,
                GREEN
        );

        addCardText(
                card,
                "💳 Payment: " + payment,
                16,
                DARK
        );

        Long recordedAt =
                getAdminTransactionTimestamp(ride);

        if (recordedAt != null) {

            SimpleDateFormat adminFormat =
                    new SimpleDateFormat(
                            "MMM dd, yyyy hh:mm a",
                            Locale.getDefault()
                    );

            addCardText(
                    card,
                    "🕒 Accepted / Recorded: "
                            + adminFormat.format(
                            new Date(recordedAt)
                    ),
                    14,
                    GRAY
            );
        }
    }

    /*
     * ============================================================
     * RIDE CARD
     * ============================================================
     */

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

        String passengerId =
                ride.getString("passengerId");

        String driverId =
                ride.getString("driverId");

        DocumentSnapshot passenger =
                usersById.get(passengerId);

        DocumentSnapshot driver =
                usersById.get(driverId);

        String passengerName =
                firstNonEmpty(
                        ride.getString("passengerName"),
                        passenger == null
                                ? ""
                                : passenger.getString("name")
                );

        String passengerPhone =
                firstNonEmpty(
                        ride.getString("passengerPhone"),
                        passenger == null
                                ? ""
                                : passenger.getString("phone")
                );

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
                                : driver.getString("phone")
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
                "👤 PASSENGER\n"
                        + valueOrDefault(
                        passengerName,
                        "Name not provided"
                )
                        + "\n📱 "
                        + valueOrDefault(
                        passengerPhone,
                        "Phone not provided"
                ),
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

    /*
     * ============================================================
     * PASSENGER CARD
     * ============================================================
     */

    private void addPassengerCard(
            LinearLayout parent,
            DocumentSnapshot passenger
    ) {

        LinearLayout card =
                createChildCard(
                        parent,
                        LIGHT_BLUE
                );

        String name =
                firstNonEmpty(
                        passenger.getString("name"),
                        passenger.getString(
                                "passengerName"
                        )
                );

        String phone =
                passenger.getString("phone");

        String town =
                passenger.getString("town");

        String province =
                passenger.getString("province");

        addCardText(
                card,
                "👤 "
                        + valueOrDefault(
                        name,
                        "Name not provided"
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

        addCardText(
                card,
                "Role: PASSENGER",
                15,
                DARK
        );

        addCardText(
                card,
                "🟢 Account: ACTIVE",
                15,
                DARK
        );

        Long registered =
                getUserTimestamp(
                        passenger,
                        "createdAt",
                        "registeredAt"
                );

        if (registered != null) {

            addCardText(
                    card,
                    "🕒 Registered: "
                            + formatDate(registered),
                    14,
                    GRAY
            );
        }
    }

    /*
     * ============================================================
     * PROVINCE / TOWN COLUMNS
     * ============================================================
     */

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
                        "Not provided"
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
                "🏘️ TOWN / MUNICIPALITY"
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
                        "Not provided"
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
                        driver.getBoolean("online")
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
                            "Driver approved. Driver moved to Approved Drivers.",
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

    private String cleanLocation(
            String value,
            String fallback
    ) {

        if (!hasText(value)) {
            return fallback;
        }

        return value.trim();
    }

    private void addGroupHeader(
            LinearLayout parent,
            String text,
            int background
    ) {

        LinearLayout card =
                createChildCard(
                        parent,
                        background
                );

        addCardText(
                card,
                text,
                20,
                GREEN
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

        contentContainer.addView(section);

        sectionButton.setOnClickListener(v -> {

            if (section.getVisibility()
                    == View.VISIBLE) {

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
                "createdAt",
                "requestedAt",
                "acceptedAt",
                "updatedAt"
        };

        for (String field : fields) {

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

            if (time1 == null && time2 == null) {
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

                    return Double.parseDouble(text);
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
                        .toUpperCase(Locale.US);

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

    /*
     * ============================================================
     * DRIVER DUES DATA OBJECT
     * ============================================================
     */
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

        String status = "SETTLED";

        /*
         * 0 = OVERDUE
         * 1 = DUE
         * 3 = SETTLED
         */
        int statusRank = 3;
    }
}
