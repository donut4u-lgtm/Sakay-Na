package com.sakyna.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminSettlementActivity extends Activity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseUser user;

    private LinearLayout settlementContainer;

    private TextView summaryText;
    private TextView statusText;

    private double pendingTotal = 0.0;
    private double verifiedTotal = 0.0;

    /*
     * Firebase Firestore batch limit is 500 writes.
     *
     * Keep each batch below the limit.
     */
    private static final int BATCH_SIZE = 450;

    private static class DriverDues {

        String driverId;

        double totalFare = 0.0;
        double totalDues = 0.0;

        List<DocumentSnapshot> dueRides =
                new ArrayList<>();

        DocumentSnapshot pendingPayment;

        DriverDues(String driverId) {
            this.driverId = driverId;
        }
    }

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(
                savedInstanceState
        );

        auth =
                FirebaseAuth.getInstance();

        db =
                FirebaseFirestore.getInstance();

        user =
                auth.getCurrentUser();

        if (
                user == null
                        ||
                !"Ld3rzaCvAGNlXBDCofB3mWjgXWp2"
                        .equals(
                                user.getUid()
                        )
        ) {

            Toast.makeText(
                    this,
                    "Admin access required.",
                    Toast.LENGTH_LONG
            ).show();

            finish();

            return;
        }

        buildScreen();

        loadSettlements();
    }

    private void buildScreen() {

        ScrollView scrollView =
                new ScrollView(this);

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setPadding(
                25,
                25,
                25,
                40
        );

        root.setBackgroundColor(
                Color.WHITE
        );

        TextView title =
                new TextView(this);

        title.setText(
                "🛡️ ADMIN\nSETTLEMENT VERIFICATION"
        );

        title.setTextSize(27);

        title.setTextColor(
                Color.BLACK
        );

        title.setGravity(
                Gravity.CENTER
        );

        title.setPadding(
                10,
                20,
                10,
                25
        );

        root.addView(
                title
        );

        statusText =
                new TextView(this);

        statusText.setText(
                "Loading settlements..."
        );

        statusText.setTextSize(16);

        statusText.setTextColor(
                Color.DKGRAY
        );

        statusText.setGravity(
                Gravity.CENTER
        );

        statusText.setPadding(
                10,
                5,
                10,
                15
        );

        root.addView(
                statusText
        );

        summaryText =
                new TextView(this);

        summaryText.setText(
                "💰 UNPAID DRIVER DUES\n₱0.00\n\n"
                        + "⏳ PENDING PAYMENTS\n₱0.00"
        );

        summaryText.setTextSize(19);

        summaryText.setTextColor(
                Color.BLACK
        );

        summaryText.setGravity(
                Gravity.CENTER
        );

        summaryText.setPadding(
                20,
                25,
                20,
                25
        );

        summaryText.setBackgroundColor(
                Color.rgb(
                        245,
                        245,
                        245
                )
        );

        root.addView(
                summaryText
        );

        Button refreshButton =
                new Button(this);

        refreshButton.setText(
                "🔄 REFRESH SETTLEMENTS"
        );

        refreshButton.setOnClickListener(
                v -> loadSettlements()
        );

        root.addView(
                refreshButton
        );

        TextView listTitle =
                new TextView(this);

        listTitle.setText(
                "💳 UNPAID DRIVER SETTLEMENTS"
        );

        listTitle.setTextSize(22);

        listTitle.setTextColor(
                Color.BLACK
        );

        listTitle.setPadding(
                0,
                30,
                0,
                15
        );

        root.addView(
                listTitle
        );

        settlementContainer =
                new LinearLayout(this);

        settlementContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        root.addView(
                settlementContainer
        );

        Button backButton =
                new Button(this);

        backButton.setText(
                "⬅️ BACK TO ADMIN"
        );

        backButton.setOnClickListener(
                v -> finish()
        );

        root.addView(
                backButton
        );

        scrollView.addView(
                root
        );

        setContentView(
                scrollView
        );
    }

    private void loadSettlements() {

        if (user == null) {
            return;
        }

        statusText.setText(
                "Loading unpaid driver dues..."
        );

        settlementContainer.removeAllViews();

        pendingTotal = 0.0;

        verifiedTotal = 0.0;

        /*
         * IMPORTANT:
         *
         * Admin settlement is based on rides with:
         *
         * driverDuesStatus == DUE
         *
         * Therefore the list automatically becomes:
         *
         * Driver A = all unpaid dues combined
         * Driver B = all unpaid dues combined
         * Driver C = all unpaid dues combined
         *
         * One entry per driver.
         */
        db.collection("rides")
                .whereEqualTo(
                        "driverDuesStatus",
                        "DUE"
                )
                .get()
                .addOnSuccessListener(
                        dueRides -> {

                            loadPendingPayments(
                                    dueRides
                            );
                        }
                )
                .addOnFailureListener(
                        e -> {

                            statusText.setText(
                                    "Unable to load unpaid driver dues."
                            );

                            Toast.makeText(
                                    this,
                                    "Unable to load unpaid driver dues:\n"
                                            + e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                );
    }

    private void loadPendingPayments(
            QuerySnapshot dueRides
    ) {

        db.collection(
                "driverSettlements"
        )
                .whereEqualTo(
                        "status",
                        "PENDING"
                )
                .get()
                .addOnSuccessListener(
                        payments -> {

                            /*
                             * LinkedHashMap keeps the order stable.
                             */
                            Map<String, DriverDues> grouped =
                                    new LinkedHashMap<>();

                            /*
                             * Group every DUE ride by driver.
                             */
                            for (
                                    DocumentSnapshot ride
                                    : dueRides.getDocuments()
                            ) {

                                String driverId =
                                        getString(
                                                ride,
                                                "driverId"
                                        );

                                if (
                                        driverId == null
                                                ||
                                        driverId.trim().isEmpty()
                                ) {
                                    continue;
                                }

                                DriverDues dues =
                                        grouped.get(
                                                driverId
                                        );

                                if (dues == null) {

                                    dues =
                                            new DriverDues(
                                                    driverId
                                            );

                                    grouped.put(
                                            driverId,
                                            dues
                                    );
                                }

                                double fare =
                                        getFare(
                                                ride
                                        );

                                double driverDue =
                                        getDriverDue(
                                                ride
                                        );

                                dues.totalFare +=
                                        fare;

                                dues.totalDues +=
                                        driverDue;

                                dues.dueRides.add(
                                        ride
                                );
                            }

                            /*
                             * Match pending settlement payments
                             * to their driver.
                             *
                             * Only one pending payment should exist
                             * for each driver because DriverSettlementActivity
                             * prevents duplicates.
                             */
                            for (
                                    DocumentSnapshot payment
                                    : payments.getDocuments()
                            ) {

                                String driverId =
                                        getString(
                                                payment,
                                                "driverId"
                                        );

                                if (
                                        driverId == null
                                                ||
                                        driverId.trim().isEmpty()
                                ) {
                                    continue;
                                }

                                DriverDues dues =
                                        grouped.get(
                                                driverId
                                        );

                                if (dues == null) {

                                    dues =
                                            new DriverDues(
                                                    driverId
                                            );

                                    grouped.put(
                                            driverId,
                                            dues
                                    );
                                }

                                /*
                                 * Keep the first pending payment.
                                 * Normally there can only be one.
                                 */
                                if (
                                        dues.pendingPayment
                                                == null
                                ) {

                                    dues.pendingPayment =
                                            payment;
                                }
                            }

                            /*
                             * Calculate totals.
                             */
                            pendingTotal = 0.0;

                            for (
                                    DriverDues dues
                                    : grouped.values()
                            ) {

                                dues.totalFare =
                                        roundMoney(
                                                dues.totalFare
                                        );

                                dues.totalDues =
                                        roundMoney(
                                                dues.totalDues
                                        );

                                pendingTotal +=
                                        dues.totalDues;
                            }

                            pendingTotal =
                                    roundMoney(
                                            pendingTotal
                                    );

                            updateSummary();

                            /*
                             * Remove drivers that have no DUE rides.
                             *
                             * A pending settlement with no remaining DUE
                             * rides should not create a new unpaid driver
                             * entry.
                             */
                            List<String> emptyDrivers =
                                    new ArrayList<>();

                            for (
                                    Map.Entry<
                                            String,
                                            DriverDues
                                            > entry
                                    : grouped.entrySet()
                            ) {

                                if (
                                        entry.getValue()
                                                .dueRides
                                                .isEmpty()
                                ) {

                                    emptyDrivers.add(
                                            entry.getKey()
                                    );
                                }
                            }

                            for (
                                    String driverId
                                    : emptyDrivers
                            ) {

                                grouped.remove(
                                        driverId
                                );
                            }

                            if (grouped.isEmpty()) {

                                statusText.setText(
                                        "No unpaid driver settlement dues."
                                );

                                TextView empty =
                                        new TextView(this);

                                empty.setText(
                                        "✅ ALL DRIVER SETTLEMENTS ARE PAID\n\n"
                                                + "No unpaid 10% platform fees."
                                );

                                empty.setTextSize(
                                        18
                                );

                                empty.setTextColor(
                                        Color.rgb(
                                                0,
                                                130,
                                                0
                                        )
                                );

                                empty.setGravity(
                                        Gravity.CENTER
                                );

                                empty.setPadding(
                                        20,
                                        40,
                                        20,
                                        40
                                );

                                settlementContainer.addView(
                                        empty
                                );

                                return;
                            }

                            /*
                             * One card per driver.
                             */
                            for (
                                    DriverDues dues
                                    : grouped.values()
                            ) {

                                addDriverSettlementCard(
                                        dues
                                );
                            }

                            statusText.setText(
                                    grouped.size()
                                            + " driver(s) with unpaid dues."
                            );
                        }
                )
                .addOnFailureListener(
                        e -> {

                            statusText.setText(
                                    "Unable to load pending payments."
                            );

                            Toast.makeText(
                                    this,
                                    "Unable to load pending payments:\n"
                                            + e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                );
    }

    private void updateSummary() {

        summaryText.setText(
                "💰 TOTAL UNPAID DRIVER DUES\n₱"
                        + formatMoney(
                        pendingTotal
                )
                        + "\n\n"
                        + "⏳ PENDING PAYMENT RECORDS\n"
                        + "Payments awaiting Admin verification"
        );
    }

    private void addDriverSettlementCard(
            DriverDues dues
    ) {

        LinearLayout card =
                new LinearLayout(this);

        card.setOrientation(
                LinearLayout.VERTICAL
        );

        card.setPadding(
                25,
                25,
                25,
                25
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
                20
        );

        card.setLayoutParams(
                cardParams
        );

        card.setBackgroundColor(
                Color.rgb(
                        245,
                        245,
                        245
                )
        );

        TextView title =
                new TextView(this);

        title.setText(
                "🛺 DRIVER SETTLEMENT"
        );

        title.setTextSize(
                21
        );

        title.setTextColor(
                Color.BLACK
        );

        title.setPadding(
                0,
                0,
                0,
                15
        );

        card.addView(
                title
        );

        TextView details =
                new TextView(this);

        details.setText(
                "👤 Driver ID\n"
                        + dues.driverId
                        + "\n\n"
                        + "💰 Fares With Unpaid Dues\n₱"
                        + formatMoney(
                        dues.totalFare
                )
                        + "\n\n"
                        + "📊 Platform Fee\n₱"
                        + formatMoney(
                        dues.totalDues
                )
                        + "  (10%)\n\n"
                        + "🧾 Unpaid Bookings\n"
                        + dues.dueRides.size()
        );

        details.setTextSize(
                16
        );

        details.setTextColor(
                Color.DKGRAY
        );

        details.setPadding(
                0,
                0,
                0,
                20
        );

        card.addView(
                details
        );

        /*
         * Load driver profile information.
         */
        loadDriverInformation(
                dues.driverId,
                details
        );

        /*
         * If a payment has already been submitted,
         * show its information.
         */
        if (
                dues.pendingPayment != null
        ) {

            DocumentSnapshot payment =
                    dues.pendingPayment;

            double paymentAmount =
                    getAmount(
                            payment
                    );

            double duesAtSubmission =
                    getAmountField(
                            payment,
                            "duesAmountAtSubmission"
                    );

            String reference =
                    getString(
                            payment,
                            "referenceNumber"
                    );

            String paymentMethod =
                    getString(
                            payment,
                            "paymentMethod"
                    );

            long submittedAt =
                    getLong(
                            payment,
                            "submittedAt"
                    );

            long cutoffAt =
                    getLong(
                            payment,
                            "duesCutoffAt"
                    );

            TextView paymentText =
                    new TextView(this);

            paymentText.setText(
                    "⏳ PAYMENT SUBMITTED\n\n"
                            + "💸 Amount Submitted\n₱"
                            + formatMoney(
                            paymentAmount
                    )
                            + "\n\n"
                            + "🧾 Dues At Submission\n₱"
                            + formatMoney(
                            duesAtSubmission
                    )
                            + "\n\n"
                            + "💳 Method\n"
                            + paymentMethod
                            + "\n\n"
                            + "🔢 Reference\n"
                            + reference
                            + "\n\n"
                            + "📅 Submitted\n"
                            + formatDate(
                            submittedAt
                    )
            );

            paymentText.setTextSize(
                    16
            );

            paymentText.setTextColor(
                    Color.rgb(
                            120,
                            80,
                            0
                    )
            );

            paymentText.setPadding(
                    0,
                    10,
                    0,
                    20
            );

            card.addView(
                    paymentText
            );

            /*
             * Verify only the payment record.
             *
             * Admin verification will mark the DUE rides that
             * existed at the driver's submission cutoff as PAID.
             */
            Button verifyButton =
                    new Button(this);

            verifyButton.setText(
                    "✅ VERIFY FULL PAYMENT"
            );

            verifyButton.setTextColor(
                    Color.WHITE
            );

            verifyButton.setBackgroundColor(
                    Color.rgb(
                            0,
                            150,
                            0
                    )
            );

            final String settlementId =
                    payment.getId();

            verifyButton.setOnClickListener(
                    v -> verifySettlement(
                            settlementId
                    )
            );

            card.addView(
                    verifyButton
            );

            Button rejectButton =
                    new Button(this);

            rejectButton.setText(
                    "❌ REJECT PAYMENT"
            );

            rejectButton.setTextColor(
                    Color.WHITE
            );

            rejectButton.setBackgroundColor(
                    Color.rgb(
                            200,
                            0,
                            0
                    )
            );

            rejectButton.setOnClickListener(
                    v -> rejectSettlement(
                            settlementId
                    )
            );

            card.addView(
                    rejectButton
            );

            if (cutoffAt > 0) {

                TextView cutoffText =
                        new TextView(this);

                cutoffText.setText(
                        "ℹ️ Dues accepted after "
                                + formatDate(
                                cutoffAt
                        )
                                + " remain unpaid."
                );

                cutoffText.setTextSize(
                        14
                );

                cutoffText.setTextColor(
                        Color.DKGRAY
                );

                cutoffText.setPadding(
                        0,
                        10,
                        0,
                        5
                );

                card.addView(
                        cutoffText
                );
            }

        } else {

            /*
             * No payment submitted yet.
             */
            TextView waiting =
                    new TextView(this);

            waiting.setText(
                    "⏳ PAYMENT NOT YET SUBMITTED\n\n"
                            + "Driver still has an unpaid balance of ₱"
                            + formatMoney(
                            dues.totalDues
                    )
            );

            waiting.setTextSize(
                    17
            );

            waiting.setTextColor(
                    Color.rgb(
                            180,
                            100,
                            0
                    )
            );

            waiting.setPadding(
                    0,
                    10,
                    0,
                    10
            );

            card.addView(
                    waiting
            );
        }

        settlementContainer.addView(
                card
        );
    }

    private void loadDriverInformation(
            String driverId,
            TextView details
    ) {

        if (
                driverId == null
                        ||
                driverId.trim().isEmpty()
        ) {
            return;
        }

        db.collection(
                "users"
        )
                .document(
                        driverId
                )
                .get()
                .addOnSuccessListener(
                        driver -> {

                            if (
                                    !driver.exists()
                            ) {
                                return;
                            }

                            String name =
                                    firstAvailable(
                                            driver,
                                            "driverName",
                                            "name"
                                    );

                            String phone =
                                    getString(
                                            driver,
                                            "phone"
                                    );

                            String province =
                                    firstAvailable(
                                            driver,
                                            "province",
                                            "driverProvince"
                                    );

                            String townCity =
                                    firstAvailable(
                                            driver,
                                            "townCity",
                                            "city"
                                    );

                            String plate =
                                    firstAvailable(
                                            driver,
                                            "plateNumber",
                                            "driverPlateNumber"
                                    );

                            String franchise =
                                    firstAvailable(
                                            driver,
                                            "franchiseNumber",
                                            "driverFranchiseNumber"
                                    );

                            String vehicle =
                                    firstAvailable(
                                            driver,
                                            "vehicleDescription",
                                            "driverVehicle"
                                    );

                            String existing =
                                    details.getText()
                                            .toString();

                            details.setText(
                                    "👤 Driver\n"
                                            + name
                                            + "\n\n"
                                            + "📱 Phone\n"
                                            + phone
                                            + "\n\n"
                                            + "📍 Province\n"
                                            + province
                                            + "\n\n"
                                            + "🏘️ Town / City\n"
                                            + townCity
                                            + "\n\n"
                                            + "🛺 Plate Number\n"
                                            + plate
                                            + "\n\n"
                                            + "📄 Franchise Number\n"
                                            + franchise
                                            + "\n\n"
                                            + "🚕 Vehicle\n"
                                            + vehicle
                                            + "\n\n"
                                            + existing
                            );
                        }
                );
    }

    private void verifySettlement(
            String settlementId
    ) {

        if (
                settlementId == null
                        ||
                settlementId.isEmpty()
        ) {
            return;
        }

        if (user == null) {
            return;
        }

        /*
         * Read the settlement payment again so Admin verifies
         * the latest actual payment record.
         */
        db.collection(
                "driverSettlements"
        )
                .document(
                        settlementId
                )
                .get()
                .addOnSuccessListener(
                        settlement -> {

                            if (
                                    !settlement.exists()
                            ) {

                                Toast.makeText(
                                        this,
                                        "Settlement payment no longer exists.",
                                        Toast.LENGTH_LONG
                                ).show();

                                loadSettlements();

                                return;
                            }

                            String status =
                                    getString(
                                            settlement,
                                            "status"
                                    );

                            if (
                                    !"PENDING".equalsIgnoreCase(
                                            status
                                    )
                            ) {

                                Toast.makeText(
                                        this,
                                        "This payment is no longer pending.",
                                        Toast.LENGTH_LONG
                                ).show();

                                loadSettlements();

                                return;
                            }

                            String driverId =
                                    getString(
                                            settlement,
                                            "driverId"
                                    );

                            double paymentAmount =
                                    getAmount(
                                            settlement
                                    );

                            double duesAtSubmission =
                                    getAmountField(
                                            settlement,
                                            "duesAmountAtSubmission"
                                    );

                            long cutoffAt =
                                    getLong(
                                            settlement,
                                            "duesCutoffAt"
                                    );

                            /*
                             * Payment must match the balance submitted.
                             */
                            if (
                                    duesAtSubmission <= 0.0
                            ) {

                                Toast.makeText(
                                        this,
                                        "Invalid settlement: no dues amount recorded.",
                                        Toast.LENGTH_LONG
                                ).show();

                                return;
                            }

                            if (
                                    Math.abs(
                                            paymentAmount
                                                    - duesAtSubmission
                                    ) > 0.009
                            ) {

                                Toast.makeText(
                                        this,
                                        "❌ Payment amount does not match the submitted dues.",
                                        Toast.LENGTH_LONG
                                ).show();

                                return;
                            }

                            /*
                             * Find all DUE rides belonging to this driver.
                             */
                            db.collection(
                                    "rides"
                            )
                                    .whereEqualTo(
                                            "driverId",
                                            driverId
                                    )
                                    .whereEqualTo(
                                            "driverDuesStatus",
                                            "DUE"
                                    )
                                    .get()
                                    .addOnSuccessListener(
                                            dueRides -> {

                                                /*
                                                 * Only dues that existed
                                                 * at the payment cutoff are
                                                 * covered by this payment.
                                                 */
                                                List<
                                                        DocumentSnapshot
                                                        > ridesToPay =
                                                        new ArrayList<>();

                                                double amountToMarkPaid =
                                                        0.0;

                                                for (
                                                        DocumentSnapshot ride
                                                        : dueRides.getDocuments()
                                                ) {

                                                    long duesCreatedAt =
                                                            getDuesCreatedAt(
                                                                    ride
                                                            );

                                                    /*
                                                     * Old rides without a
                                                     * timestamp are treated
                                                     * as eligible for this
                                                     * settlement.
                                                     */
                                                    if (
                                                            cutoffAt <= 0
                                                                    ||
                                                            duesCreatedAt <= 0
                                                                    ||
                                                            duesCreatedAt
                                                                    <= cutoffAt
                                                    ) {

                                                        ridesToPay.add(
                                                                ride
                                                        );

                                                        amountToMarkPaid +=
                                                                getDriverDue(
                                                                        ride
                                                                );
                                                    }
                                                }

                                                amountToMarkPaid =
                                                        roundMoney(
                                                                amountToMarkPaid
                                                        );

                                                /*
                                                 * The DUE rides covered by
                                                 * the submitted payment must
                                                 * match the payment amount.
                                                 *
                                                 * Small rounding tolerance
                                                 * is allowed.
                                                 */
                                                if (
                                                        Math.abs(
                                                                amountToMarkPaid
                                                                        - paymentAmount
                                                        ) > 0.009
                                                ) {

                                                    Toast.makeText(
                                                            this,
                                                            "❌ Current unpaid dues do not match the submitted payment.\nExpected: ₱"
                                                                    + formatMoney(
                                                                    paymentAmount
                                                            )
                                                                    + "\nFound at cutoff: ₱"
                                                                    + formatMoney(
                                                                    amountToMarkPaid
                                                            ),
                                                            Toast.LENGTH_LONG
                                                    ).show();

                                                    return;
                                                }

                                                if (
                                                        ridesToPay.isEmpty()
                                                ) {

                                                    Toast.makeText(
                                                            this,
                                                            "No unpaid rides were found for this settlement.",
                                                            Toast.LENGTH_LONG
                                                    ).show();

                                                    return;
                                                }

                                                verifyPaymentAndMarkRidesPaid(
                                                        settlementId,
                                                        settlement,
                                                        ridesToPay
                                                );
                                            }
                                    )
                                    .addOnFailureListener(
                                            e -> Toast.makeText(
                                                    this,
                                                    "Unable to load driver dues:\n"
                                                            + e.getMessage(),
                                                    Toast.LENGTH_LONG
                                            ).show()
                                    );
                        }
                )
                .addOnFailureListener(
                        e -> Toast.makeText(
                                this,
                                "Unable to load settlement:\n"
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void verifyPaymentAndMarkRidesPaid(
            String settlementId,
            DocumentSnapshot settlement,
            List<DocumentSnapshot> ridesToPay
    ) {

        long verifiedAt =
                System.currentTimeMillis();

        /*
         * First mark the covered rides PAID.
         *
         * Firestore batches are kept below 500 writes.
         */
        List<List<DocumentSnapshot>> chunks =
                new ArrayList<>();

        for (
                int start = 0;
                start < ridesToPay.size();
                start += BATCH_SIZE
        ) {

            int end =
                    Math.min(
                            start + BATCH_SIZE,
                            ridesToPay.size()
                    );

            chunks.add(
                    ridesToPay.subList(
                            start,
                            end
                    )
            );
        }

        markRideChunk(
                chunks,
                0,
                settlementId,
                settlement,
                verifiedAt
        );
    }

    private void markRideChunk(
            List<List<DocumentSnapshot>> chunks,
            int index,
            String settlementId,
            DocumentSnapshot settlement,
            long verifiedAt
    ) {

        if (
                index >= chunks.size()
        ) {

            /*
             * All covered rides are PAID.
             *
             * Now remove the verified settlement record.
             */
            deleteVerifiedSettlement(
                    settlementId,
                    settlement,
                    verifiedAt
            );

            return;
        }

        WriteBatch batch =
                db.batch();

        for (
                DocumentSnapshot ride
                : chunks.get(index)
        ) {

            DocumentReference reference =
                    ride.getReference();

            Map<String, Object> update =
                    new HashMap<>();

            update.put(
                    "driverDuesStatus",
                    "PAID"
            );

            update.put(
                    "driverDuesPaidAt",
                    verifiedAt
            );

            update.put(
                    "driverDuesPaidBy",
                    user.getUid()
            );

            update.put(
                    "driverDuesPaymentId",
                    settlementId
            );

            batch.update(
                    reference,
                    update
            );
        }

        batch.commit()
                .addOnSuccessListener(
                        unused -> {

                            markRideChunk(
                                    chunks,
                                    index + 1,
                                    settlementId,
                                    settlement,
                                    verifiedAt
                            );
                        }
                )
                .addOnFailureListener(
                        e -> {

                            Toast.makeText(
                                    this,
                                    "Unable to mark settlement dues as paid:\n"
                                            + e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();

                            loadSettlements();
                        }
                );
    }

    private void deleteVerifiedSettlement(
            String settlementId,
            DocumentSnapshot settlement,
            long verifiedAt
    ) {

        /*
         * The user requested that a paid settlement be removed
         * from the settlement list.
         *
         * Payment information is retained in the PAID ride fields:
         *
         * driverDuesStatus
         * driverDuesPaidAt
         * driverDuesPaidBy
         * driverDuesPaymentId
         *
         * Therefore the active driverSettlements record can be deleted.
         */
        db.collection(
                "driverSettlements"
        )
                .document(
                        settlementId
                )
                .delete()
                .addOnSuccessListener(
                        unused -> {

                            Toast.makeText(
                                    this,
                                    "✅ Settlement verified and paid dues removed.",
                                    Toast.LENGTH_LONG
                            ).show();

                            loadSettlements();
                        }
                )
                .addOnFailureListener(
                        e -> {

                            /*
                             * The rides are already PAID.
                             *
                             * If deletion fails, leave the settlement
                             * record visible so Admin can retry.
                             */
                            Toast.makeText(
                                    this,
                                    "⚠️ Dues were marked PAID, but the settlement record could not be removed:\n"
                                            + e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();

                            loadSettlements();
                        }
                );
    }

    private void rejectSettlement(
            String settlementId
    ) {

        if (
                settlementId == null
                        ||
                settlementId.isEmpty()
        ) {
            return;
        }

        if (user == null) {
            return;
        }

        Map<String, Object> update =
                new HashMap<>();

        update.put(
                "status",
                "REJECTED"
        );

        update.put(
                "rejectedAt",
                System.currentTimeMillis()
        );

        update.put(
                "rejectedBy",
                user.getUid()
        );

        db.collection(
                "driverSettlements"
        )
                .document(
                        settlementId
                )
                .update(
                        update
                )
                .addOnSuccessListener(
                        v -> {

                            Toast.makeText(
                                    this,
                                    "❌ Payment rejected. Driver dues remain unpaid.",
                                    Toast.LENGTH_LONG
                            ).show();

                            loadSettlements();
                        }
                )
                .addOnFailureListener(
                        e -> Toast.makeText(
                                this,
                                "Unable to reject payment:\n"
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private long getDuesCreatedAt(
            DocumentSnapshot document
    ) {

        Object value =
                document.get(
                        "driverDuesCreatedAt"
                );

        if (
                value instanceof Number
        ) {

            return ((Number) value)
                    .longValue();
        }

        /*
         * Compatibility fallback.
         */
        value =
                document.get(
                        "acceptedAt"
                );

        if (
                value instanceof Number
        ) {

            return ((Number) value)
                    .longValue();
        }

        return 0L;
    }

    private double getDriverDue(
            DocumentSnapshot document
    ) {

        /*
         * New DriverActivity stores the exact 10% amount.
         */
        Object stored =
                document.get(
                        "driverDuesAmount"
                );

        if (
                stored instanceof Number
        ) {

            return roundMoney(
                    ((Number) stored)
                            .doubleValue()
            );
        }

        /*
         * Compatibility fallback for older accepted rides.
         */
        double fare =
                getFare(
                        document
                );

        return roundMoney(
                fare * 0.10
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

        if (
                value instanceof Number
        ) {

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

    private double getAmount(
            DocumentSnapshot document
    ) {

        return getAmountField(
                document,
                "amount"
        );
    }

    private double getAmountField(
            DocumentSnapshot document,
            String field
    ) {

        Object value =
                document.get(
                        field
                );

        if (value == null) {
            return 0.0;
        }

        if (
                value instanceof Number
        ) {

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

    private String firstAvailable(
            DocumentSnapshot document,
            String first,
            String second
    ) {

        String value =
                getString(
                        document,
                        first
                );

        if (
                value != null
                        &&
                !value.trim().isEmpty()
        ) {

            return value;
        }

        value =
                getString(
                        document,
                        second
                );

        if (
                value != null
                        &&
                !value.trim().isEmpty()
        ) {

            return value;
        }

        return "Not provided";
    }

    private String getString(
            DocumentSnapshot document,
            String field
    ) {

        String value =
                document.getString(
                        field
                );

        if (value == null) {
            return "";
        }

        return value;
    }

    private long getLong(
            DocumentSnapshot document,
            String field
    ) {

        Object value =
                document.get(
                        field
                );

        if (
                value instanceof Number
        ) {

            return ((Number) value)
                    .longValue();
        }

        return 0L;
    }

    private double roundMoney(
            double amount
    ) {

        return Math.round(
                amount * 100.0
        ) / 100.0;
    }

    private String formatMoney(
            double amount
    ) {

        return String.format(
                Locale.US,
                "%.2f",
                amount
        );
    }

    private String formatDate(
            long timestamp
    ) {

        if (timestamp <= 0) {

            return "Not available";
        }

        return new SimpleDateFormat(
                "MMM dd, yyyy hh:mm a",
                Locale.US
        ).format(
                new Date(
                        timestamp
                )
        );
    }
}
                
