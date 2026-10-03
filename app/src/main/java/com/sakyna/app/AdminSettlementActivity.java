package com.sakyna.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
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

private LinearLayout unpaidContainer;
private LinearLayout verificationContainer;
private LinearLayout paidHistoryContainer;

private TextView statusText;
private TextView summaryText;

private double pendingTotal = 0.0;
private double paidTotal = 0.0;

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
protected void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);

    auth = FirebaseAuth.getInstance();
    db = FirebaseFirestore.getInstance();
    user = auth.getCurrentUser();

    if (
            user == null
                    ||
            !"Ld3rzaCvAGNlXBDCofB3mWjgXWp2"
                    .equals(user.getUid())
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
            "🛡️ ADMIN\nDRIVER PAYMENTS"
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
            20
    );

    root.addView(title);

    statusText =
            new TextView(this);

    statusText.setText(
            "Loading driver payments..."
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

    root.addView(statusText);

    summaryText =
            new TextView(this);

    summaryText.setText(
            "🔴 UNPAID\n₱0.00\n\n"
                    + "🟠 NEED VERIFICATION\n0\n\n"
                    + "🟢 PAID / VERIFIED\n₱0.00"
    );

    summaryText.setTextSize(17);

    summaryText.setTextColor(
            Color.BLACK
    );

    summaryText.setGravity(
            Gravity.CENTER
    );

    summaryText.setPadding(
            20,
            20,
            20,
            20
    );

    summaryText.setBackgroundColor(
            Color.rgb(
                    245,
                    245,
                    245
            )
    );

    root.addView(summaryText);

    Button refreshButton =
            new Button(this);

    refreshButton.setText(
            "🔄 REFRESH"
    );

    refreshButton.setOnClickListener(
            v -> loadSettlements()
    );

    root.addView(refreshButton);

    /*
     * RED — UNPAID
     */
    Button unpaidButton =
            createLargeButton(
                    "🔴 UNPAID",
                    Color.rgb(
                            210,
                            0,
                            0
                    )
            );

    root.addView(unpaidButton);

    unpaidContainer =
            new LinearLayout(this);

    unpaidContainer.setOrientation(
            LinearLayout.VERTICAL
    );

    unpaidContainer.setVisibility(
            View.GONE
    );

    root.addView(
            unpaidContainer
    );

    /*
     * ORANGE — NEED VERIFICATION
     */
    Button verificationButton =
            createLargeButton(
                    "🟠 NEED VERIFICATION",
                    Color.rgb(
                            230,
                            120,
                            0
                    )
            );

    root.addView(
            verificationButton
    );

    verificationContainer =
            new LinearLayout(this);

    verificationContainer.setOrientation(
            LinearLayout.VERTICAL
    );

    verificationContainer.setVisibility(
            View.GONE
    );

    root.addView(
            verificationContainer
    );

    /*
     * GREEN — PAID / VERIFIED
     */
    Button paidButton =
            createLargeButton(
                    "🟢 PAID / VERIFIED",
                    Color.rgb(
                            0,
                            145,
                            0
                    )
            );

    root.addView(
            paidButton
    );

    paidHistoryContainer =
            new LinearLayout(this);

    paidHistoryContainer.setOrientation(
            LinearLayout.VERTICAL
    );

    paidHistoryContainer.setVisibility(
            View.GONE
    );

    root.addView(
            paidHistoryContainer
    );

    unpaidButton.setOnClickListener(
            v -> showOnly(
                    unpaidContainer
            )
    );

    verificationButton.setOnClickListener(
            v -> showOnly(
                    verificationContainer
            )
    );

    paidButton.setOnClickListener(
            v -> showOnly(
                    paidHistoryContainer
            )
    );

    Button backButton =
            new Button(this);

    backButton.setText(
            "⬅️ BACK TO ADMIN"
    );

    backButton.setOnClickListener(
            v -> finish()
    );

    root.addView(backButton);

    scrollView.addView(root);

    setContentView(scrollView);
}

private Button createLargeButton(
        String text,
        int color
) {

    Button button =
            new Button(this);

    button.setText(text);

    button.setTextSize(18);

    button.setTextColor(
            Color.WHITE
    );

    button.setGravity(
            Gravity.CENTER
    );

    button.setBackgroundColor(
            color
    );

    LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );

    params.setMargins(
            0,
            12,
            0,
            8
    );

    button.setLayoutParams(params);

    return button;
}

private void showOnly(
        LinearLayout selected
) {

    unpaidContainer.setVisibility(
            View.GONE
    );

    verificationContainer.setVisibility(
            View.GONE
    );

    paidHistoryContainer.setVisibility(
            View.GONE
    );

    selected.setVisibility(
            View.VISIBLE
    );
}

private void loadSettlements() {

    if (user == null) {
        return;
    }

    statusText.setText(
            "Loading driver payments..."
    );

    unpaidContainer.removeAllViews();
    verificationContainer.removeAllViews();
    paidHistoryContainer.removeAllViews();

    pendingTotal = 0.0;
    paidTotal = 0.0;

    loadCurrentDues();
    loadPaidHistory();
}

private void loadCurrentDues() {

    db.collection("rides")
            .whereEqualTo(
                    "driverDuesStatus",
                    "DUE"
            )
            .get()
            .addOnSuccessListener(
                    this::loadPendingPayments
            )
            .addOnFailureListener(
                    e -> {

                        statusText.setText(
                                "Unable to load driver dues."
                        );

                        addMessage(
                                unpaidContainer,
                                "Unable to load unpaid driver dues:\n"
                                        + e.getMessage(),
                                Color.RED
                        );
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

                        Map<String, DriverDues> grouped =
                                new LinkedHashMap<>();

                        /*
                         * Build unpaid driver list.
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

                            dues.totalFare +=
                                    getFare(ride);

                            dues.totalDues +=
                                    getDriverDue(ride);

                            dues.dueRides.add(
                                    ride
                            );
                        }

                        /*
                         * Attach pending payment records.
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

                            dues.pendingPayment =
                                    payment;
                        }

                        pendingTotal = 0.0;

                        int verificationCount = 0;

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

                            if (
                                    dues.pendingPayment
                                            != null
                            ) {
                                verificationCount++;
                            }
                        }

                        pendingTotal =
                                roundMoney(
                                        pendingTotal
                                );

                        /*
                         * Remove drivers that have no actual
                         * unpaid rides.
                         */
                        List<String> remove =
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

                                remove.add(
                                        entry.getKey()
                                );
                            }
                        }

                        for (
                                String id
                                : remove
                        ) {

                            grouped.remove(id);
                        }

                        /*
                         * Build the RED unpaid section.
                         */
                        boolean hasUnpaid =
                                false;

                        /*
                         * Build the ORANGE verification section.
                         */
                        boolean hasVerification =
                                false;

                        for (
                                DriverDues dues
                                : grouped.values()
                        ) {

                            if (
                                    dues.dueRides
                                            .isEmpty()
                            ) {
                                continue;
                            }

                            hasUnpaid = true;

                            addUnpaidDriverCard(
                                    dues
                            );

                            if (
                                    dues.pendingPayment
                                            != null
                            ) {

                                hasVerification =
                                        true;

                                addVerificationCard(
                                        dues
                                );
                            }
                        }

                        if (!hasUnpaid) {

                            addMessage(
                                    unpaidContainer,
                                    "🟢 NO UNPAID DRIVER DUES\n\n"
                                            + "All current 10% platform fees "
                                            + "are settled.",
                                    Color.rgb(
                                            0,
                                            130,
                                            0
                                    )
                            );
                        }

                        if (!hasVerification) {

                            addMessage(
                                    verificationContainer,
                                    "🟢 NO PAYMENTS NEED VERIFICATION\n\n"
                                            + "There are no driver payments "
                                            + "waiting for Admin verification.",
                                    Color.rgb(
                                            0,
                                            130,
                                            0
                                    )
                            );
                        }

                        summaryText.setText(
                                "🔴 UNPAID\n₱"
                                        + formatMoney(
                                        pendingTotal
                                )
                                        + "\n\n"
                                        + "🟠 NEED VERIFICATION\n"
                                        + verificationCount
                                        + "\n\n"
                                        + "🟢 PAID / VERIFIED\n₱"
                                        + formatMoney(
                                        paidTotal
                                )
                        );

                        statusText.setText(
                                grouped.size()
                                        + " driver(s) with current unpaid dues."
                        );
                    }
            )
            .addOnFailureListener(
                    e -> {

                        addMessage(
                                unpaidContainer,
                                "Unable to load unpaid dues:\n"
                                        + e.getMessage(),
                                Color.RED
                        );

                        addMessage(
                                verificationContainer,
                                "Unable to load payments:\n"
                                        + e.getMessage(),
                                Color.RED
                        );
                    }
            );
}

private void addUnpaidDriverCard(
        DriverDues dues
) {

    LinearLayout card =
            createCard(
                    Color.rgb(
                            255,
                            242,
                            242
                    )
            );

    card.addView(
            makeText(
                    "🔴 UNPAID DRIVER DUES",
                    21,
                    Color.rgb(
                            190,
                            0,
                            0
                    )
            )
    );

    TextView details =
            makeText(
                    "👤 Driver ID\n"
                            + dues.driverId
                            + "\n\n"
                            + "💰 Fares With Unpaid Dues\n₱"
                            + formatMoney(
                            dues.totalFare
                    )
                            + "\n\n"
                            + "📊 Sakay Na Fee — 10%\n₱"
                            + formatMoney(
                            dues.totalDues
                    )
                            + "\n\n"
                            + "🧾 Unpaid Bookings\n"
                            + dues.dueRides.size(),
                    16,
                    Color.DKGRAY
            );

    card.addView(details);

    loadDriverInformation(
            dues.driverId,
            details
    );

    if (
            dues.pendingPayment == null
    ) {

        card.addView(
                makeText(
                        "🔴 PAYMENT NOT YET SUBMITTED\n\n"
                                + "Driver still owes ₱"
                                + formatMoney(
                                dues.totalDues
                        ),
                        17,
                        Color.rgb(
                                180,
                                70,
                                0
                        )
                )
        );
    }

    unpaidContainer.addView(card);
}

private void addVerificationCard(
        DriverDues dues
) {

    DocumentSnapshot payment =
            dues.pendingPayment;

    if (payment == null) {
        return;
    }

    LinearLayout card =
            createCard(
                    Color.rgb(
                            255,
                            248,
                            225
                    )
            );

    card.addView(
            makeText(
                    "🟠 NEED VERIFICATION",
                    21,
                    Color.rgb(
                            210,
                            105,
                            0
                    )
            )
    );

    TextView details =
            makeText(
                    "👤 Driver ID\n"
                            + dues.driverId
                            + "\n\n"
                            + "💰 Current Unpaid Dues\n₱"
                            + formatMoney(
                            dues.totalDues
                    ),
                    16,
                    Color.DKGRAY
            );

    card.addView(details);

    loadDriverInformation(
            dues.driverId,
            details
    );

    double paymentAmount =
            getAmount(
                    payment
            );

    double duesAtSubmission =
            getAmountField(
                    payment,
                    "duesAmountAtSubmission"
            );

    String method =
            getString(
                    payment,
                    "paymentMethod"
            );

    String reference =
            getString(
                    payment,
                    "referenceNumber"
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

    card.addView(
            makeText(
                    "🟠 PAYMENT SUBMITTED\n\n"
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
                            + "💳 Payment Method\n"
                            + safeText(
                            method,
                            "Not provided"
                    )
                            + "\n\n"
                            + "🔢 Reference Number\n"
                            + safeText(
                            reference,
                            "Not provided"
                    )
                            + "\n\n"
                            + "📅 Submitted\n"
                            + formatDate(
                            submittedAt
                    ),
                    16,
                    Color.rgb(
                            130,
                            85,
                            0
                    )
            )
    );

    Button verifyButton =
            new Button(this);

    verifyButton.setText(
            "✅ VERIFY PAYMENT"
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
            v ->
                    verifySettlement(
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
            v ->
                    rejectSettlement(
                            settlementId
                    )
    );

    card.addView(
            rejectButton
    );

    if (cutoffAt > 0) {

        card.addView(
                makeText(
                        "ℹ️ Dues created after "
                                + formatDate(
                                cutoffAt
                        )
                                + " remain unpaid.",
                        14,
                        Color.DKGRAY
                )
        );
    }

    verificationContainer.addView(
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

    db.collection("users")
            .document(driverId)
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
                                        + safeText(
                                        name,
                                        driverId
                                )
                                        + "\n\n"
                                        + "📱 Phone\n"
                                        + safeText(
                                        phone,
                                        "Not provided"
                                )
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

private void loadPaidHistory() {

    db.collection(
            "driverSettlements"
    )
            .whereEqualTo(
                    "status",
                    "VERIFIED"
            )
            .get()
            .addOnSuccessListener(
                    payments -> {

                        paidHistoryContainer
                                .removeAllViews();

                        paidTotal = 0.0;

                        if (
                                payments.isEmpty()
                        ) {

                            addMessage(
                                    paidHistoryContainer,
                                    "No verified driver payments yet.",
                                    Color.DKGRAY
                            );

                            updateSummary();

                            return;
                        }

                        for (
                                DocumentSnapshot payment
                                : payments.getDocuments()
                        ) {

                            paidTotal +=
                                    getAmount(
                                            payment
                                    );

                            addPaidHistoryCard(
                                    payment
                            );
                        }

                        paidTotal =
                                roundMoney(
                                        paidTotal
                                );

                        updateSummary();
                    }
            )
            .addOnFailureListener(
                    e -> {

                        addMessage(
                                paidHistoryContainer,
                                "Unable to load paid history:\n"
                                        + e.getMessage(),
                                Color.RED
                        );
                    }
            );
}

private void addPaidHistoryCard(
        DocumentSnapshot payment
) {

    LinearLayout card =
            createCard(
                    Color.rgb(
                            240,
                            255,
                            240
                    )
            );

    String driverId =
            getString(
                    payment,
                    "driverId"
            );

    double amount =
            getAmount(
                    payment
            );

    String method =
            getString(
                    payment,
                    "paymentMethod"
            );

    String reference =
            getString(
                    payment,
                    "referenceNumber"
            );

    long submittedAt =
            getLong(
                    payment,
                    "submittedAt"
            );

    long verifiedAt =
            getLong(
                    payment,
                    "verifiedAt"
            );

    long rideCount =
            getLong(
                    payment,
                    "ridesPaidCount"
            );

    card.addView(
            makeText(
                    "🟢 PAID / VERIFIED",
                    21,
                    Color.rgb(
                            0,
                            125,
                            0
                    )
            )
    );

    TextView details =
            makeText(
                    "👤 Driver ID\n"
                            + safeText(
                            driverId,
                            "Not provided"
                    )
                            + "\n\n"
                            + "💰 Amount Paid\n₱"
                            + formatMoney(
                            amount
                    )
                            + "\n\n"
                            + "💳 Payment Method\n"
                            + safeText(
                            method,
                            "Not provided"
                    )
                            + "\n\n"
                            + "🔢 Reference Number\n"
                            + safeText(
                            reference,
                            "Not provided"
                    )
                            + "\n\n"
                            + "📅 Payment Submitted\n"
                            + formatDate(
                            submittedAt
                    )
                            + "\n\n"
                            + "✅ Verified\n"
                            + formatDate(
                            verifiedAt
                    )
                            + "\n\n"
                            + "🧾 Bookings Covered\n"
                            + (
                            rideCount > 0
                                    ? String.valueOf(
                                    rideCount
                            )
                                    : "Recorded in paid rides"
                    ),
                    16,
                    Color.DKGRAY
            );

    card.addView(details);

    loadPaidDriverInformation(
            driverId,
            details
    );

    paidHistoryContainer.addView(
            card
    );
}

private void loadPaidDriverInformation(
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

    db.collection("users")
            .document(driverId)
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

                        String existing =
                                details.getText()
                                        .toString();

                        details.setText(
                                "👤 Driver\n"
                                        + safeText(
                                        name,
                                        driverId
                                )
                                        + "\n\n"
                                        + "📱 Phone\n"
                                        + safeText(
                                        phone,
                                        "Not provided"
                                )
                                        + "\n\n"
                                        + existing
                        );
                    }
            );
}

private void updateSummary() {

    summaryText.setText(
            "🔴 UNPAID\n₱"
                    + formatMoney(
                    pendingTotal
            )
                    + "\n\n"
                    + "🟠 NEED VERIFICATION\n"
                    + "Tap orange button to review"
                    + "\n\n"
                    + "🟢 PAID / VERIFIED\n₱"
                    + formatMoney(
                    paidTotal
            )
    );
}

private void verifySettlement(
        String settlementId
) {

    if (
            settlementId == null
                    ||
            settlementId.isEmpty()
                    ||
            user == null
    ) {
        return;
    }

    db.collection(
            "driverSettlements"
    )
            .document(settlementId)
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
                                    "❌ Payment amount does not match submitted dues.",
                                    Toast.LENGTH_LONG
                            ).show();

                            return;
                        }

                        db.collection("rides")
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

                                            List<DocumentSnapshot>
                                                    ridesToPay =
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

                                            if (
                                                    Math.abs(
                                                            amountToMarkPaid
                                                                    - paymentAmount
                                                    ) > 0.009
                                            ) {

                                                Toast.makeText(
                                                        this,
                                                        "❌ Current unpaid dues do not match payment.\nExpected: ₱"
                                                                + formatMoney(
                                                                paymentAmount
                                                        )
                                                                + "\nFound: ₱"
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
                                                        "No unpaid rides were found for this payment.",
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
                                        e ->
                                                Toast.makeText(
                                                        this,
                                                        "Unable to load driver dues:\n"
                                                                + e.getMessage(),
                                                        Toast.LENGTH_LONG
                                                ).show()
                                );
                    }
            )
            .addOnFailureListener(
                    e ->
                            Toast.makeText(
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
            verifiedAt,
            ridesToPay.size()
    );
}

private void markRideChunk(
        List<List<DocumentSnapshot>> chunks,
        int index,
        String settlementId,
        DocumentSnapshot settlement,
        long verifiedAt,
        int rideCount
) {

    if (
            index >= chunks.size()
    ) {

        saveVerifiedSettlement(
                settlementId,
                settlement,
                verifiedAt,
                rideCount
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
                    unused ->
                            markRideChunk(
                                    chunks,
                                    index + 1,
                                    settlementId,
                                    settlement,
                                    verifiedAt,
                                    rideCount
                            )
            )
            .addOnFailureListener(
                    e -> {

                        Toast.makeText(
                                this,
                                "Unable to mark dues as paid:\n"
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();

                        loadSettlements();
                    }
            );
}

private void saveVerifiedSettlement(
        String settlementId,
        DocumentSnapshot settlement,
        long verifiedAt,
        int rideCount
) {

    Map<String, Object> update =
            new HashMap<>();

    update.put(
            "status",
            "VERIFIED"
    );

    update.put(
            "verifiedAt",
            verifiedAt
    );

    update.put(
            "verifiedBy",
            user.getUid()
    );

    update.put(
            "ridesPaidCount",
            rideCount
    );

    update.put(
            "paidAmount",
            getAmount(
                    settlement
            )
    );

    db.collection(
            "driverSettlements"
    )
            .document(settlementId)
            .update(update)
            .addOnSuccessListener(
                    unused -> {

                        Toast.makeText(
                                this,
                                "🟢 PAYMENT VERIFIED. Driver dues marked PAID.",
                                Toast.LENGTH_LONG
                        ).show();

                        loadSettlements();
                    }
            )
            .addOnFailureListener(
                    e -> {

                        Toast.makeText(
                                this,
                                "⚠️ Rides were marked PAID, but payment history failed:\n"
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
                    ||
            user == null
    ) {
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
            .document(settlementId)
            .update(update)
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
                    e ->
                            Toast.makeText(
                                    this,
                                    "Unable to reject payment:\n"
                                            + e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show()
            );
}

private LinearLayout createCard(
        int backgroundColor
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

    LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );

    params.setMargins(
            0,
            10,
            0,
            20
    );

    card.setLayoutParams(params);

    card.setBackgroundColor(
            backgroundColor
    );

    return card;
}

private void addMessage(
        LinearLayout container,
        String message,
        int color
) {

    TextView text =
            makeText(
                    message,
                    17,
                    color
            );

    text.setGravity(
            Gravity.CENTER
    );

    text.setPadding(
            20,
            35,
            20,
            35
    );

    container.addView(text);
}

private TextView makeText(
        String text,
        float size,
        int color
) {

    TextView view =
            new TextView(this);

    view.setText(text);
    view.setTextSize(size);
    view.setTextColor(color);

    view.setPadding(
            0,
            8,
            0,
            15
    );

    return view;
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
            document.getString(field);

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
            document.get(field);

    if (
            value instanceof Number
    ) {

        return ((Number) value)
                .longValue();
    }

    return 0L;
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
            document.get(field);

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

private double getFare(
        DocumentSnapshot document
) {

    Object value =
            document.get("fare");

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

private double getDriverDue(
        DocumentSnapshot document
) {

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

    return roundMoney(
            getFare(document) * 0.10
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

private String safeText(
        String value,
        String fallback
) {

    if (
            value == null
                    ||
            value.trim().isEmpty()
    ) {

        return fallback;
    }

    return value;
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
            new Date(timestamp)
    );
}

}
