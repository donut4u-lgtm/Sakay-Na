
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
    private LinearLayout driversSection;
    private LinearLayout approvalSection;
    private LinearLayout activeRidesSection;
    private LinearLayout historySection;
    private LinearLayout paymentSection;

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

    private int historyDays = 30;
    private int paymentDays = 30;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        buildScreen();
        loadDashboard();
    }

    private void buildScreen() {

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setBackgroundColor(
                WHITE
        );

        root.setPadding(
                16,
                16,
                16,
                16
        );

        TextView title =
                new TextView(this);

        title.setText(
                "🛺 SAKAY NA ADMIN"
        );

        title.setTextSize(28);

        title.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        title.setTextColor(
                GREEN
        );

        title.setGravity(
                Gravity.CENTER
        );

        root.addView(title);

        TextView subtitle =
                new TextView(this);

        subtitle.setText(
                "ADMIN CONTROL CENTER"
        );

        subtitle.setTextSize(15);

        subtitle.setTextColor(
                DARK
        );

        subtitle.setGravity(
                Gravity.CENTER
        );

        subtitle.setPadding(
                0,
                0,
                0,
                12
        );

        root.addView(subtitle);

        statusText =
                new TextView(this);

        statusText.setText(
                "Loading dashboard..."
        );

        statusText.setTextSize(15);

        statusText.setTextColor(
                GRAY
        );

        statusText.setGravity(
                Gravity.CENTER
        );

        root.addView(statusText);

        Button refresh =
                new Button(this);

        refresh.setText(
                "🔄 REFRESH"
        );

        refresh.setOnClickListener(
                v -> loadDashboard()
        );

        root.addView(refresh);

        /*
         * DRIVER SETTLEMENTS
         */
        Button settlementButton =
                new Button(this);

        settlementButton.setText(
                "💰 DRIVER SETTLEMENTS"
        );

        settlementButton.setOnClickListener(
                v -> {

                    Intent intent =
                            new Intent(
                                    AdminActivity.this,
                                    AdminSettlementActivity.class
                            );

                    startActivity(intent);
                }
        );

        root.addView(settlementButton);

        ScrollView scrollView =
                new ScrollView(this);

        contentContainer =
                new LinearLayout(this);

        contentContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        contentContainer.setPadding(
                0,
                10,
                0,
                20
        );

        scrollView.addView(
                contentContainer
        );

        root.addView(
                scrollView,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                )
        );

        Button logout =
                new Button(this);

        logout.setText(
                "🚪 LOGOUT"
        );

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
                .addOnSuccessListener(
                        snapshot -> {

                            for (
                                    DocumentSnapshot user :
                                    snapshot.getDocuments()
                            ) {

                                usersById.put(
                                        user.getId(),
                                        user
                                );
                            }

                            buildUserSections();
                            loadRides();
                        }
                )
                .addOnFailureListener(
                        e -> {

                            statusText.setText(
                                    "Unable to load users."
                            );

                            buildUserSections();
                            loadRides();
                        }
                );
    }

    private void buildUserSections() {

        int passengerCount = 0;
        int driverCount = 0;
        int pendingCount = 0;
        int approvedCount = 0;
        int rejectedCount = 0;
        int onlineDrivers = 0;

        for (
                DocumentSnapshot user :
                usersById.values()
        ) {

            String role =
                    user.getString("role");

            if (
                    "PASSENGER".equalsIgnoreCase(
                            role
                    )
            ) {

                passengerCount++;

            } else if (
                    "DRIVER".equalsIgnoreCase(
                            role
                    )
            ) {

                driverCount++;

                String approval =
                        getApprovalStatus(user);

                if (
                        "APPROVED".equalsIgnoreCase(
                                approval
                        )
                ) {

                    approvedCount++;

                } else if (
                        "REJECTED".equalsIgnoreCase(
                                approval
                        )
                ) {

                    rejectedCount++;

                } else {

                    pendingCount++;
                }

                Boolean online =
                        user.getBoolean("online");

                if (
                        Boolean.TRUE.equals(
                                online
                        )
                ) {

                    onlineDrivers++;
                }
            }
        }

        overviewSection =
                createSection(
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
                "❌ REJECTED DRIVERS",
                String.valueOf(rejectedCount),
                LIGHT_RED
        );

        addInfoCard(
                overviewSection,
                "🟢 DRIVERS ONLINE",
                String.valueOf(onlineDrivers),
                LIGHT_GREEN
        );

        passengersSection =
                createSection(
                        "👤 PASSENGER MANAGEMENT"
                );

        int passengerShown = 0;

        for (
                DocumentSnapshot user :
                usersById.values()
        ) {

            if (
                    "PASSENGER".equalsIgnoreCase(
                            user.getString("role")
                    )
            ) {

                addPassengerCard(
                        passengersSection,
                        user
                );

                passengerShown++;
            }
        }

        if (passengerShown == 0) {

            addInfoCard(
                    passengersSection,
                    "👤 PASSENGERS",
                    "No passenger accounts found.",
                    LIGHT_YELLOW
            );
        }

        driversSection =
                createSection(
                        "🚕 DRIVER MANAGEMENT"
                );

        int driverShown = 0;

        for (
                DocumentSnapshot user :
                usersById.values()
        ) {

            if (
                    "DRIVER".equalsIgnoreCase(
                            user.getString("role")
                    )
            ) {

                addDriverCard(
                        driversSection,
                        user,
                        false
                );

                driverShown++;
            }
        }

        if (driverShown == 0) {

            addInfoCard(
                    driversSection,
                    "🚕 DRIVERS",
                    "No driver accounts found.",
                    LIGHT_YELLOW
            );
        }

        approvalSection =
                createSection(
                        "🔔 DRIVER APPROVAL"
                );

        int pendingShown = 0;

        for (
                DocumentSnapshot user :
                usersById.values()
        ) {

            if (
                    "DRIVER".equalsIgnoreCase(
                            user.getString("role")
                    )
                    &&
                    "PENDING_APPROVAL".equalsIgnoreCase(
                            getApprovalStatus(user)
                    )
            ) {

                addDriverCard(
                        approvalSection,
                        user,
                        true
                );

                pendingShown++;
            }
        }

        if (pendingShown == 0) {

            addInfoCard(
                    approvalSection,
                    "🔔 PENDING APPROVAL",
                    "No drivers waiting for approval.",
                    LIGHT_GREEN
            );
        }
    }

    private void loadRides() {

        db.collection("rides")
                .get()
                .addOnSuccessListener(
                        snapshot -> {

                            rideDocuments.clear();

                            rideDocuments.addAll(
                                    snapshot.getDocuments()
                            );

                            buildRideSections();

                            statusText.setText(
                                    "Dashboard loaded."
                            );
                        }
                )
                .addOnFailureListener(
                        e -> {

                            buildRideSections();

                            statusText.setText(
                                    "Users loaded. Ride history unavailable."
                            );
                        }
                );
    }

    private void buildRideSections() {

        activeRidesSection =
                createSection(
                        "🚦 ACTIVE RIDES"
                );

        int activeCount = 0;

        for (
                DocumentSnapshot ride :
                rideDocuments
        ) {

            if (
                    isActiveStatus(
                            ride.getString("status")
                    )
            ) {

                addRideCard(
                        activeRidesSection,
                        ride
                );

                activeCount++;
            }
        }

        if (activeCount == 0) {

            addInfoCard(
                    activeRidesSection,
                    "🚦 ACTIVE RIDES",
                    "No active rides.",
                    LIGHT_GREEN
            );
        }

        historySection =
                createSection(
                        "📋 RIDE / BOOKING HISTORY"
                );

        addHistoryFilters(
                historySection
        );

        renderHistory();

        paymentSection =
                createSection(
                        "💰 FARE & PAYMENT"
                );

        addPaymentFilters(
                paymentSection
        );

        renderPayments();
    }

    private void addPaymentFilters(
            LinearLayout parent
    ) {

        TextView label =
                new TextView(this);

        label.setText(
                "Show payment transactions:"
        );

        label.setTextSize(16);
        label.setTextColor(DARK);

        parent.addView(label);

        LinearLayout row =
                new LinearLayout(this);

        row.setOrientation(
                LinearLayout.HORIZONTAL
        );

        Button today =
                new Button(this);

        today.setText("TODAY");

        today.setOnClickListener(
                v -> {

                    paymentDays = 1;
                    renderPayments();
                }
        );

        Button week =
                new Button(this);

        week.setText("7 DAYS");

        week.setOnClickListener(
                v -> {

                    paymentDays = 7;
                    renderPayments();
                }
        );

        Button month =
                new Button(this);

        month.setText("30 DAYS");

        month.setOnClickListener(
                v -> {

                    paymentDays = 30;
                    renderPayments();
                }
        );

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

        long now =
                System.currentTimeMillis();

        long cutoff =
                now -
                        (
                                paymentDays
                                        * 24L
                                        * 60L
                                        * 60L
                                        * 1000L
                        );

        double completedFare = 0;
        double cashTotal = 0;
        double gcashTotal = 0;
        double mayaTotal = 0;

        int completedCount = 0;
        int cashCount = 0;
        int gcashCount = 0;
        int mayaCount = 0;

        List<DocumentSnapshot> paymentRides =
                new ArrayList<>();

        for (
                DocumentSnapshot ride :
                rideDocuments
        ) {

            String status =
                    ride.getString("status");

            if (
                    !"COMPLETED".equalsIgnoreCase(
                            status
                    )
            ) {
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

            completedFare += fare;
            completedCount++;

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
                "🏁 COMPLETED RIDES",
                String.valueOf(completedCount),
                LIGHT_GREEN
        );

        addInfoCard(
                paymentSection,
                "💰 COMPLETED FARE",
                formatPeso(completedFare),
                LIGHT_GREEN
        );

        addInfoCard(
                paymentSection,
                "💵 CASH",
                formatPeso(cashTotal)
                        + " • "
                        + cashCount
                        + " ride(s)",
                LIGHT_YELLOW
        );

        addInfoCard(
                paymentSection,
                "📱 GCASH",
                formatPeso(gcashTotal)
                        + " • "
                        + gcashCount
                        + " ride(s)",
                LIGHT_BLUE
        );

        addInfoCard(
                paymentSection,
                "📱 MAYA",
                formatPeso(mayaTotal)
                        + " • "
                        + mayaCount
                        + " ride(s)",
                LIGHT_GREEN
        );

        TextView note =
                new TextView(this);

        note.setText(
                "ℹ️ Payment is recorded as a "
                        + "passenger-to-driver payment. "
                        + "Sakay Na does not collect or hold "
                        + "the passenger fare."
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
                "🧾 PAYMENT TRANSACTIONS"
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
                    "No completed payment transactions "
                            + "for the last "
                            + paymentDays
                            + " day(s).",
                    LIGHT_YELLOW
            );

            return;
        }

        for (
                DocumentSnapshot ride :
                paymentRides
        ) {

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

        String passengerTown =
                passenger == null
                        ? ""
                        : passenger.getString("town");

        String passengerProvince =
                passenger == null
                        ? ""
                        : passenger.getString("province");

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

        String date =
                getRideDateText(ride);

        if (!hasText(passengerName)) {
            passengerName = "Name not provided";
        }

        if (!hasText(passengerPhone)) {
            passengerPhone = "Phone not provided";
        }

        if (!hasText(driverName)) {
            driverName = "Driver not assigned";
        }

        if (!hasText(driverPhone)) {
            driverPhone = "Phone not available";
        }

        addCardText(
                card,
                "🧾 RIDE #" + ride.getId(),
                18,
                GREEN
        );

        addCardText(
                card,
                "👤 Passenger: " + passengerName,
                16,
                DARK
        );

        addCardText(
                card,
                "📱 Passenger Phone: "
                        + passengerPhone,
                15,
                DARK
        );

        if (hasText(passengerTown)) {

            addCardText(
                    card,
                    "🏘️ Passenger Town/City: "
                            + passengerTown,
                    15,
                    DARK
            );
        }

        if (hasText(passengerProvince)) {

            addCardText(
                    card,
                    "🗺️ Passenger Province: "
                            + passengerProvince,
                    15,
                    DARK
            );
        }

        addCardText(
                card,
                "🚕 Driver: " + driverName,
                16,
                DARK
        );

        addCardText(
                card,
                "📱 Driver Phone: " + driverPhone,
                15,
                DARK
        );

        addCardText(
                card,
                "💰 Fare: " + formatPeso(fare),
                18,
                GREEN
        );

        addCardText(
                card,
                "💳 Payment: " + payment,
                16,
                DARK
        );

        addCardText(
                card,
                "🏁 Status: COMPLETED",
                16,
                DARK
        );

        if (hasText(date)) {

            addCardText(
                    card,
                    "🕒 " + date,
                    14,
                    GRAY
            );
        }
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

    private String formatPeso(double amount) {

        return "₱"
                + String.format(
                Locale.US,
                "%.2f",
                amount
        );
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
                ride.getString("status");

        String payment =
                ride.getString("paymentMethod");

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

        String passengerTown =
                passenger == null
                        ? ""
                        : passenger.getString("town");

        String passengerProvince =
                passenger == null
                        ? ""
                        : passenger.getString("province");

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

        String franchise =
                driver == null
                        ? ""
                        : driver.getString(
                                "franchiseNumber"
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

        String driverTown =
                driver == null
                        ? ""
                        : driver.getString("town");

        String driverProvince =
                driver == null
                        ? ""
                        : driver.getString("province");

        if (!hasText(pickup)) {
            pickup = "Not provided";
        }

        if (!hasText(destination)) {
            destination = "Not provided";
        }

        if (!hasText(status)) {
            status = "UNKNOWN";
        }

        if (!hasText(payment)) {
            payment = "Not provided";
        }

        if (!hasText(passengerName)) {
            passengerName = "Name not provided";
        }

        if (!hasText(passengerPhone)) {
            passengerPhone = "Phone not provided";
        }

        if (!hasText(driverName)) {
            driverName = "Driver not assigned";
        }

        if (!hasText(driverPhone)) {
            driverPhone = "Phone not available";
        }

        addCardText(
                card,
                "🛺 RIDE #" + ride.getId(),
                18,
                GREEN
        );

        addCardText(
                card,
                "👤 PASSENGER\n"
                        + passengerName
                        + "\n📱 "
                        + passengerPhone,
                16,
                DARK
        );

        if (hasText(passengerTown)) {

            addCardText(
                    card,
                    "🏘️ Town/City: "
                            + passengerTown,
                    15,
                    DARK
            );
        }

        if (hasText(passengerProvince)) {

            addCardText(
                    card,
                    "🗺️ Province: "
                            + passengerProvince,
                    15,
                    DARK
            );
        }

        addCardText(
                card,
                "🚕 DRIVER\n"
                        + driverName
                        + "\n📱 "
                        + driverPhone,
                16,
                DARK
        );

        if (hasText(driverTown)) {

            addCardText(
                    card,
                    "🏘️ Driver Town/City: "
                            + driverTown,
                    15,
                    DARK
            );
        }

        if (hasText(driverProvince)) {

            addCardText(
                    card,
                    "🗺️ Driver Province: "
                            + driverProvince,
                    15,
                    DARK
            );
        }

        if (hasText(plate)) {

            addCardText(
                    card,
                    "🪪 Plate: " + plate,
                    15,
                    DARK
            );
        }

        if (hasText(franchise)) {

            addCardText(
                    card,
                    "📄 Franchise: " + franchise,
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
                "📍 Pickup:\n" + pickup,
                15,
                DARK
        );

        addCardText(
                card,
                "🎯 Destination:\n"
                        + destination,
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
                "💳 Payment: "
                        + payment,
                16,
                DARK
        );

        addCardText(
                card,
                "🚦 Status: "
                        + status,
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

        if (!hasText(name)) {
            name = "Name not provided";
        }

        if (!hasText(phone)) {
            phone = "Phone not provided";
        }

        addCardText(
                card,
                "👤 " + name,
                19,
                GREEN
        );

        addCardText(
                card,
                "📱 Phone: " + phone,
                16,
                DARK
        );

        if (hasText(town)) {

            addCardText(
                    card,
                    "🏘️ Town / City: " + town,
                    16,
                    DARK
            );
        }

        if (hasText(province)) {

            addCardText(
                    card,
                    "🗺️ Province: " + province,
                    16,
                    DARK
            );
        }

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
    }

    private void addDriverCard(
            LinearLayout parent,
            DocumentSnapshot driver,
            boolean approvalOnly
    ) {

        LinearLayout card =
                createChildCard(
                        parent,
                        LIGHT_GREEN
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

        Boolean onlineValue =
                driver.getBoolean("online");

        boolean online =
                Boolean.TRUE.equals(
                        onlineValue
                );

        if (!hasText(name)) {
            name = "Driver name not provided";
        }

        if (!hasText(phone)) {
            phone = "Phone not provided";
        }

        addCardText(
                card,
                "🚕 " + name,
                19,
                GREEN
        );

        addCardText(
                card,
                "📱 Phone: " + phone,
                16,
                DARK
        );

        if (hasText(town)) {

            addCardText(
                    card,
                    "🏘️ Town / City: "
                            + town,
                    16,
                    DARK
            );
        }

        if (hasText(province)) {

            addCardText(
                    card,
                    "🗺️ Province: "
                            + province,
                    16,
                    DARK
            );
        }

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

        if (
                !hasText(plate)
                        &&
                !hasText(franchise)
        ) {

            addCardText(
                    card,
                    "🪪/📄 Vehicle ID: Not supplied",
                    16,
                    Color.rgb(180, 90, 0)
            );
        }

        if (hasText(vehicle)) {

            addCardText(
                    card,
                    "🛺 Tricycle: " + vehicle,
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
                    "❌ REJECT DRIVER"
            );

            reject.setOnClickListener(
                    v -> rejectDriver(
                            driver.getId()
                    )
            );

            card.addView(reject);
        }
    }

    private void addHistoryFilters(
            LinearLayout parent
    ) {

        TextView label =
                new TextView(this);

        label.setText(
                "Show ride history:"
        );

        label.setTextSize(16);
        label.setTextColor(DARK);

        parent.addView(label);

        LinearLayout row =
                new LinearLayout(this);

        row.setOrientation(
                LinearLayout.HORIZONTAL
        );

        Button today =
                new Button(this);

        today.setText("TODAY");

        today.setOnClickListener(
                v -> {

                    historyDays = 1;
                    renderHistory();
                }
        );

        Button week =
                new Button(this);

        week.setText("7 DAYS");

        week.setOnClickListener(
                v -> {

                    historyDays = 7;
                    renderHistory();
                }
        );

        Button month =
                new Button(this);

        month.setText("30 DAYS");

        month.setOnClickListener(
                v -> {

                    historyDays = 30;
                    renderHistory();
                }
        );

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

    private void renderHistory() {

        if (historySection == null) {
            return;
        }

        int childCount =
                historySection.getChildCount();

        if (childCount > 2) {

            historySection.removeViews(
                    2,
                    childCount - 2
            );
        }

        long now =
                System.currentTimeMillis();

        long cutoff =
                now -
                        (
                                historyDays
                                        * 24L
                                        * 60L
                                        * 60L
                                        * 1000L
                        );

        int shown = 0;

        for (
                DocumentSnapshot ride :
                rideDocuments
        ) {

            Long timestamp =
                    getRideTimestamp(ride);

            if (
                    timestamp == null
                            ||
                    timestamp < cutoff
            ) {
                continue;
            }

            addRideCard(
                    historySection,
                    ride
            );

            shown++;
        }

        if (shown == 0) {

            addInfoCard(
                    historySection,
                    "📋 RIDE HISTORY",
                    "No rides found for the last "
                            + historyDays
                            + " day(s).",
                    LIGHT_YELLOW
            );
        }
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

        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        buttonParams.setMargins(
                0,
                8,
                0,
                8
        );

        contentContainer.addView(
                sectionButton,
                buttonParams
        );

        contentContainer.addView(section);

        sectionButton.setOnClickListener(
                v -> {

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
                }
        );

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
                .addOnSuccessListener(
                        v -> {

                            Toast.makeText(
                                    this,
                                    "Driver approved.",
                                    Toast.LENGTH_LONG
                            ).show();

                            loadDashboard();
                        }
                )
                .addOnFailureListener(
                        e -> Toast.makeText(
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
                .update(
                        "approved",
                        false,
                        "approvalStatus",
                        "REJECTED",
                        "driverStatus",
                        "REJECTED",
                        "canAcceptRides",
                        false,
                        "online",
                        false,
                        "rejectedAt",
                        System.currentTimeMillis()
                )
                .addOnSuccessListener(
                        v -> {

                            Toast.makeText(
                                    this,
                                    "Driver rejected.",
                                    Toast.LENGTH_LONG
                            ).show();

                            loadDashboard();
                        }
                )
                .addOnFailureListener(
                        e -> Toast.makeText(
                                this,
                                "Rejection failed:\n"
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
                        ((String) value)
                                .trim();

                if (!text.isEmpty()) {

                    return Double.parseDouble(text);
                }
            }

        } catch (Exception ignored) {
        }

        return 0;
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
                e.getMessage().trim().isEmpty()
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
}
