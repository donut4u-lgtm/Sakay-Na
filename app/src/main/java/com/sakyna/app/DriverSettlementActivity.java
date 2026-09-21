package com.sakyna.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DriverSettlementActivity extends Activity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private LinearLayout content;

    private TextView totalFareText;
    private TextView platformFeeText;
    private TextView paidText;
    private TextView balanceText;

    private EditText amountInput;
    private EditText referenceInput;

    private String driverId;

    /*
     * Platform fee:
     * 10% for now.
     *
     * This is kept in one place so it can
     * later be changed to an Admin setting.
     */
    private static final double PLATFORM_FEE_RATE = 0.10;

    private static final int GREEN =
            Color.rgb(0, 125, 80);

    private static final int DARK =
            Color.rgb(35, 35, 35);

    private static final int GRAY =
            Color.rgb(110, 110, 110);

    private static final int LIGHT_GREEN =
            Color.rgb(232, 247, 238);

    private static final int LIGHT_RED =
            Color.rgb(255, 235, 235);

    private static final int LIGHT_BLUE =
            Color.rgb(235, 245, 255);

    private static final int LIGHT_YELLOW =
            Color.rgb(255, 248, 220);

    private double totalFare = 0;
    private double totalPlatformFee = 0;
    private double totalPaid = 0;
    private double balanceDue = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (auth.getCurrentUser() == null) {

            finish();
            return;
        }

        driverId =
                auth.getCurrentUser().getUid();

        buildScreen();
        loadFinancialData();
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
                18,
                18,
                18,
                30
        );

        root.setBackgroundColor(
                Color.WHITE
        );

        TextView title =
                new TextView(this);

        title.setText(
                "💰 SAKAY NA\nDRIVER SETTLEMENT"
        );

        title.setTextSize(27);

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

        title.setPadding(
                0,
                15,
                0,
                20
        );

        root.addView(title);

        TextView explanation =
                new TextView(this);

        explanation.setText(
                "Your passengers pay you directly. "
                        + "Sakay Na records the completed rides "
                        + "and calculates the platform fee."
        );

        explanation.setTextSize(16);

        explanation.setTextColor(
                DARK
        );

        explanation.setPadding(
                8,
                5,
                8,
                20
        );

        root.addView(
                explanation
        );

        totalFareText =
                addSummaryCard(
                        root,
                        "🏁 COMPLETED FARES",
                        "₱0.00",
                        LIGHT_BLUE
                );

        platformFeeText =
                addSummaryCard(
                        root,
                        "📊 SAKAY NA PLATFORM FEE",
                        "₱0.00",
                        LIGHT_YELLOW
                );

        paidText =
                addSummaryCard(
                        root,
                        "✅ AMOUNT PAID",
                        "₱0.00",
                        LIGHT_GREEN
                );

        balanceText =
                addSummaryCard(
                        root,
                        "🔴 BALANCE DUE",
                        "₱0.00",
                        LIGHT_RED
                );

        TextView settlementTitle =
                new TextView(this);

        settlementTitle.setText(
                "💳 SUBMIT SETTLEMENT"
        );

        settlementTitle.setTextSize(21);

        settlementTitle.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        settlementTitle.setTextColor(
                GREEN
        );

        settlementTitle.setPadding(
                0,
                25,
                0,
                12
        );

        root.addView(
                settlementTitle
        );

        TextView instruction =
                new TextView(this);

        instruction.setText(
                "Pay Sakay Na using the configured "
                        + "business payment method, then enter "
                        + "the amount and payment reference below."
        );

        instruction.setTextSize(15);

        instruction.setTextColor(
                DARK
        );

        instruction.setPadding(
                0,
                0,
                0,
                12
        );

        root.addView(
                instruction
        );

        TextView paymentInfo =
                new TextView(this);

        paymentInfo.setText(
                "💳 Payment destination\n"
                        + "Sakay Na official GCash / Maya account\n\n"
                        + "⚠️ Admin will verify your payment "
                        + "before your balance is cleared."
        );

        paymentInfo.setTextSize(16);

        paymentInfo.setTextColor(
                DARK
        );

        paymentInfo.setBackgroundColor(
                LIGHT_BLUE
        );

        paymentInfo.setPadding(
                18,
                18,
                18,
                18
        );

        root.addView(
                paymentInfo
        );

        amountInput =
                new EditText(this);

        amountInput.setHint(
                "Amount paid (₱)"
        );

        amountInput.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER
                        |
                        android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        );

        amountInput.setTextSize(17);

        amountInput.setPadding(
                15,
                15,
                15,
                15
        );

        LinearLayout.LayoutParams inputParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        inputParams.setMargins(
                0,
                15,
                0,
                10
        );

        root.addView(
                amountInput,
                inputParams
        );

        referenceInput =
                new EditText(this);

        referenceInput.setHint(
                "Payment reference number"
        );

        referenceInput.setTextSize(17);

        referenceInput.setPadding(
                15,
                15,
                15,
                15
        );

        root.addView(
                referenceInput
        );

        Button submitButton =
                new Button(this);

        submitButton.setText(
                "📤 SUBMIT PAYMENT"
        );

        submitButton.setTextColor(
                Color.WHITE
        );

        submitButton.setBackgroundColor(
                GREEN
        );

        submitButton.setOnClickListener(
                v -> submitSettlement()
        );

        root.addView(
                submitButton
        );

        Button refreshButton =
                new Button(this);

        refreshButton.setText(
                "🔄 REFRESH BALANCE"
        );

        refreshButton.setOnClickListener(
                v -> loadFinancialData()
        );

        root.addView(
                refreshButton
        );

        TextView historyTitle =
                new TextView(this);

        historyTitle.setText(
                "🧾 SETTLEMENT HISTORY"
        );

        historyTitle.setTextSize(21);

        historyTitle.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        historyTitle.setTextColor(
                GREEN
        );

        historyTitle.setPadding(
                0,
                30,
                0,
                12
        );

        root.addView(
                historyTitle
        );

        content =
                new LinearLayout(this);

        content.setOrientation(
                LinearLayout.VERTICAL
        );

        root.addView(
                content
        );

        Button backButton =
                new Button(this);

        backButton.setText(
                "⬅ BACK"
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

    private TextView addSummaryCard(
            LinearLayout parent,
            String title,
            String value,
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
                12
        );

        parent.addView(
                card,
                params
        );

        TextView titleText =
                new TextView(this);

        titleText.setText(
                title
        );

        titleText.setTextSize(16);

        titleText.setTextColor(
                DARK
        );

        card.addView(
                titleText
        );

        TextView valueText =
                new TextView(this);

        valueText.setText(
                value
        );

        valueText.setTextSize(25);

        valueText.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        valueText.setTextColor(
                GREEN
        );

        valueText.setPadding(
                0,
                5,
                0,
                0
        );

        card.addView(
                valueText
        );

        return valueText;
    }

    private void loadFinancialData() {

        totalFare = 0;
        totalPlatformFee = 0;
        totalPaid = 0;
        balanceDue = 0;

        db.collection("rides")
                .whereEqualTo(
                        "driverId",
                        driverId
                )
                .get()
                .addOnSuccessListener(
                        snapshot -> {

                            for (
                                    DocumentSnapshot ride :
                                    snapshot.getDocuments()
                            ) {

                                String status =
                                        ride.getString(
                                                "status"
                                        );

                                if (
                                        !"COMPLETED"
                                                .equalsIgnoreCase(
                                                        status
                                                )
                                ) {

                                    continue;
                                }

                                double fare =
                                        readNumber(
                                                ride,
                                                "fare"
                                        );

                                totalFare += fare;
                            }

                            totalPlatformFee =
                                    totalFare
                                            * PLATFORM_FEE_RATE;

                            loadPayments();

                        }
                )
                .addOnFailureListener(
                        e -> Toast.makeText(
                                this,
                                "Unable to load rides:\n"
                                        + safeMessage(e),
                                Toast.LENGTH_LONG
                        )
                );
    }

    private void loadPayments() {

        db.collection("driverSettlements")
                .whereEqualTo(
                        "driverId",
                        driverId
                )
                .get()
                .addOnSuccessListener(
                        snapshot -> {

                            totalPaid = 0;

                            for (
                                    DocumentSnapshot payment :
                                    snapshot.getDocuments()
                            ) {

                                String status =
                                        payment.getString(
                                                "status"
                                        );

                                /*
                                 * Only verified payments
                                 * reduce the driver's balance.
                                 */
                                if (
                                        !"VERIFIED"
                                                .equalsIgnoreCase(
                                                        status
                                                )
                                ) {

                                    continue;
                                }

                                totalPaid +=
                                        readNumber(
                                                payment,
                                                "amount"
                                        );
                            }

                            balanceDue =
                                    totalPlatformFee
                                            - totalPaid;

                            if (
                                    balanceDue < 0
                            ) {

                                balanceDue = 0;
                            }

                            updateSummary();

                            loadSettlementHistory(
                                    snapshot
                            );
                        }
                )
                .addOnFailureListener(
                        e -> {

                            updateSummary();

                            Toast.makeText(
                                    this,
                                    "Settlement history unavailable.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                );
    }

    private void updateSummary() {

        if (totalFareText != null) {

            totalFareText.setText(
                    formatPeso(
                            totalFare
                    )
            );
        }

        if (platformFeeText != null) {

            platformFeeText.setText(
                    formatPeso(
                            totalPlatformFee
                    )
                            + "  (10%)"
            );
        }

        if (paidText != null) {

            paidText.setText(
                    formatPeso(
                            totalPaid
                    )
            );
        }

        if (balanceText != null) {

            balanceText.setText(
                    formatPeso(
                            balanceDue
                    )
            );
        }
    }

    private void submitSettlement() {

        if (balanceDue <= 0) {

            Toast.makeText(
                    this,
                    "Your Sakay Na balance is already paid.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        String amountText =
                amountInput.getText()
                        .toString()
                        .trim();

        String reference =
                referenceInput.getText()
                        .toString()
                        .trim();

        if (amountText.isEmpty()) {

            amountInput.setError(
                    "Enter amount paid."
            );

            return;
        }

        if (reference.isEmpty()) {

            referenceInput.setError(
                    "Enter payment reference."
            );

            return;
        }

        double amount;

        try {

            amount =
                    Double.parseDouble(
                            amountText
                    );

        } catch (Exception e) {

            amountInput.setError(
                    "Invalid amount."
            );

            return;
        }

        if (amount <= 0) {

            amountInput.setError(
                    "Amount must be greater than zero."
            );

            return;
        }

        if (amount > balanceDue) {

            Toast.makeText(
                    this,
                    "Payment cannot be greater than "
                            + "your current balance of "
                            + formatPeso(balanceDue),
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        String paymentId =
                db.collection(
                        "driverSettlements"
                )
                        .document()
                        .getId();

        java.util.Map<String, Object> data =
                new java.util.HashMap<>();

        data.put(
                "driverId",
                driverId
        );

        data.put(
                "amount",
                amount
        );

        data.put(
                "referenceNumber",
                reference
        );

        data.put(
                "paymentMethod",
                "GCASH_OR_MAYA"
        );

        data.put(
                "status",
                "PENDING"
        );

        data.put(
                "submittedAt",
                System.currentTimeMillis()
        );

        data.put(
                "verifiedAt",
                null
        );

        db.collection(
                "driverSettlements"
        )
                .document(paymentId)
                .set(data)
                .addOnSuccessListener(
                        v -> {

                            amountInput.setText(
                                    ""
                            );

                            referenceInput.setText(
                                    ""
                            );

                            Toast.makeText(
                                    this,
                                    "✅ Payment submitted.\n"
                                            + "Waiting for Admin verification.",
                                    Toast.LENGTH_LONG
                            ).show();

                            loadFinancialData();
                        }
                )
                .addOnFailureListener(
                        e -> Toast.makeText(
                                this,
                                "Unable to submit payment:\n"
                                        + safeMessage(e),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void loadSettlementHistory(
            com.google.firebase.firestore.QuerySnapshot snapshot
    ) {

        if (content == null) {
            return;
        }

        content.removeAllViews();

        List<DocumentSnapshot> payments =
                new ArrayList<>(
                        snapshot.getDocuments()
                );

        if (payments.isEmpty()) {

            addHistoryInfo(
                    "No settlement payments submitted yet.",
                    LIGHT_YELLOW
            );

            return;
        }

        for (
                DocumentSnapshot payment :
                payments
        ) {

            addSettlementCard(
                    payment
            );
        }
    }

    private void addSettlementCard(
            DocumentSnapshot payment
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

        String status =
                payment.getString(
                        "status"
                );

        if (!hasText(status)) {
            status = "PENDING";
        }

        int background =
                "VERIFIED".equalsIgnoreCase(
                        status
                )
                        ? LIGHT_GREEN
                        : LIGHT_YELLOW;

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
                12
        );

        content.addView(
                card,
                params
        );

        double amount =
                readNumber(
                        payment,
                        "amount"
                );

        String reference =
                payment.getString(
                        "referenceNumber"
                );

        if (!hasText(reference)) {
            reference = "Not provided";
        }

        String paymentMethod =
                payment.getString(
                        "paymentMethod"
                );

        if (!hasText(paymentMethod)) {
            paymentMethod = "Not provided";
        }

        TextView title =
                new TextView(this);

        title.setText(
                "💰 "
                        + formatPeso(amount)
                        + "  •  "
                        + status
        );

        title.setTextSize(19);

        title.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        title.setTextColor(
                GREEN
        );

        card.addView(
                title
        );

        addHistoryText(
                card,
                "💳 Method: "
                        + paymentMethod
        );

        addHistoryText(
                card,
                "🔢 Reference: "
                        + reference
        );

        Long submittedAt =
                readTimestamp(
                        payment.get(
                                "submittedAt"
                        )
                );

        if (submittedAt != null) {

            addHistoryText(
                    card,
                    "🕒 Submitted: "
                            + formatDate(
                                    submittedAt
                            )
            );
        }

        Long verifiedAt =
                readTimestamp(
                        payment.get(
                                "verifiedAt"
                        )
                );

        if (verifiedAt != null) {

            addHistoryText(
                    card,
                    "✅ Verified: "
                            + formatDate(
                                    verifiedAt
                            )
            );
        }
    }

    private void addHistoryInfo(
            String text,
            int background
    ) {

        TextView view =
                new TextView(this);

        view.setText(
                text
        );

        view.setTextSize(
                16
        );

        view.setTextColor(
                DARK
        );

        view.setBackgroundColor(
                background
        );

        view.setPadding(
                18,
                18,
                18,
                18
        );

        content.addView(
                view
        );
    }

    private void addHistoryText(
            LinearLayout parent,
            String text
    ) {

        TextView view =
                new TextView(this);

        view.setText(
                text
        );

        view.setTextSize(
                15
        );

        view.setTextColor(
                DARK
        );

        view.setPadding(
                0,
                5,
                0,
                5
        );

        parent.addView(
                view
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

                return Double.parseDouble(
                        ((String) value).trim()
                );
            }

        } catch (Exception ignored) {
        }

        return 0;
    }

    private Long readTimestamp(
            Object value
    ) {

        if (value instanceof Number) {

            return ((Number) value)
                    .longValue();
        }

        if (
                value instanceof
                        com.google.firebase.Timestamp
        ) {

            return ((com.google.firebase.Timestamp)
                    value)
                    .toDate()
                    .getTime();
        }

        return null;
    }

    private String formatDate(
            long timestamp
    ) {

        java.text.SimpleDateFormat format =
                new java.text.SimpleDateFormat(
                        "MMM dd, yyyy hh:mm a",
                        Locale.getDefault()
                );

        return format.format(
                new java.util.Date(
                        timestamp
                )
        );
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
        ) {

            return "Unknown Firebase error.";
        }

        return e.getMessage();
    }
}
