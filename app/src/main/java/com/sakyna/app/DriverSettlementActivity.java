package com.sakyna.app;

import android.app.Activity;
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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DriverSettlementActivity extends Activity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseUser user;

    private TextView totalFareText;
    private TextView platformFeeText;
    private TextView paidText;
    private TextView balanceText;
    private TextView historyText;

    private EditText amountInput;
    private EditText referenceInput;

    private static final double PLATFORM_FEE_RATE = 0.10;

    private double totalFare = 0.0;
    private double totalPaid = 0.0;
    private double platformFee = 0.0;
    private double balanceDue = 0.0;

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
        loadSettlement();
    }

    private void buildScreen() {

        ScrollView scrollView = new ScrollView(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(30, 30, 30, 40);
        root.setBackgroundColor(Color.WHITE);

        scrollView.addView(root);

        TextView title = new TextView(this);
        title.setText("💰 DRIVER SETTLEMENT");
        title.setTextSize(27);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);
        title.setPadding(10, 20, 10, 25);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText(
                "Track your completed fares, Sakay Na platform fee, "
                        + "payments and remaining balance."
        );
        subtitle.setTextSize(16);
        subtitle.setTextColor(Color.DKGRAY);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(10, 0, 10, 25);
        root.addView(subtitle);

        totalFareText =
                createSummaryText("Completed Fares\n₱0.00");
        root.addView(totalFareText);

        platformFeeText =
                createSummaryText("Sakay Na Platform Fee\n₱0.00");
        root.addView(platformFeeText);

        paidText =
                createSummaryText("Amount Paid\n₱0.00");
        root.addView(paidText);

        balanceText =
                createSummaryText("Balance Due\n₱0.00");
        root.addView(balanceText);

        TextView paymentTitle = new TextView(this);
        paymentTitle.setText("💳 MAKE A PAYMENT");
        paymentTitle.setTextSize(22);
        paymentTitle.setTextColor(Color.BLACK);
        paymentTitle.setPadding(0, 35, 0, 15);
        root.addView(paymentTitle);

        TextView paymentInfo = new TextView(this);
        paymentInfo.setText(
                "Send your settlement payment through the "
                        + "approved Sakay Na payment channel.\n\n"
                        + "Payment methods: GCash / Maya\n\n"
                        + "⚠️ Payments remain PENDING until Admin "
                        + "verifies the actual payment.\n\n"
                        + "A reference number or screenshot alone "
                        + "is NOT proof of payment."
        );
        paymentInfo.setTextSize(16);
        paymentInfo.setTextColor(Color.DKGRAY);
        paymentInfo.setPadding(0, 0, 0, 20);
        root.addView(paymentInfo);

        amountInput = new EditText(this);
        amountInput.setHint("Payment amount");
        amountInput.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER
                        | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        );
        root.addView(amountInput);

        referenceInput = new EditText(this);
        referenceInput.setHint("GCash / Maya reference number");
        referenceInput.setSingleLine(true);
        root.addView(referenceInput);

        Button submitButton = new Button(this);
        submitButton.setText("💸 SUBMIT PAYMENT");
        submitButton.setTextColor(Color.WHITE);
        submitButton.setBackgroundColor(
                Color.rgb(0, 120, 200)
        );

        submitButton.setOnClickListener(
                v -> submitPayment()
        );

        root.addView(submitButton);

        Button refreshButton = new Button(this);
        refreshButton.setText("🔄 REFRESH BALANCE");

        refreshButton.setOnClickListener(
                v -> loadSettlement()
        );

        root.addView(refreshButton);

        TextView historyTitle = new TextView(this);
        historyTitle.setText("📜 SETTLEMENT HISTORY");
        historyTitle.setTextSize(22);
        historyTitle.setTextColor(Color.BLACK);
        historyTitle.setPadding(0, 35, 0, 15);
        root.addView(historyTitle);

        historyText = new TextView(this);
        historyText.setText("Loading settlement history...");
        historyText.setTextSize(16);
        historyText.setTextColor(Color.DKGRAY);
        historyText.setPadding(0, 5, 0, 20);
        root.addView(historyText);

        Button backButton = new Button(this);
        backButton.setText("⬅️ BACK TO DRIVER DASHBOARD");

        backButton.setOnClickListener(
                v -> finish()
        );

        root.addView(backButton);

        setContentView(scrollView);
    }

    private TextView createSummaryText(String text) {

        TextView view = new TextView(this);

        view.setText(text);
        view.setTextSize(19);
        view.setTextColor(Color.BLACK);
        view.setGravity(Gravity.CENTER);
        view.setPadding(20, 25, 20, 25);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(0, 8, 0, 8);

        view.setLayoutParams(params);
        view.setBackgroundColor(
                Color.rgb(245, 245, 245)
        );

        return view;
    }

    private void loadSettlement() {

        if (user == null) {
            return;
        }

        totalFare = 0.0;
        totalPaid = 0.0;
        platformFee = 0.0;
        balanceDue = 0.0;

        db.collection("rides")
                .whereEqualTo(
                        "driverId",
                        user.getUid()
                )
                .whereEqualTo(
                        "status",
                        "COMPLETED"
                )
                .get()
                .addOnSuccessListener(
                        completedRides -> {

                            for (DocumentSnapshot ride
                                    : completedRides.getDocuments()) {

                                totalFare += getFare(ride);
                            }

                            platformFee =
                                    totalFare
                                            * PLATFORM_FEE_RATE;

                            loadVerifiedPayments();
                        }
                )
                .addOnFailureListener(
                        e -> {

                            Toast.makeText(
                                    this,
                                    "Unable to load completed rides.",
                                    Toast.LENGTH_LONG
                            ).show();

                            updateSummary();
                        }
                );
    }

    private void loadVerifiedPayments() {

        db.collection("driverSettlements")
                .whereEqualTo(
                        "driverId",
                        user.getUid()
                )
                .whereEqualTo(
                        "status",
                        "VERIFIED"
                )
                .get()
                .addOnSuccessListener(
                        payments -> {

                            totalPaid = 0.0;

                            for (DocumentSnapshot payment
                                    : payments.getDocuments()) {

                                totalPaid +=
                                        getAmount(payment);
                            }

                            balanceDue =
                                    platformFee
                                            - totalPaid;

                            if (balanceDue < 0) {
                                balanceDue = 0;
                            }

                            updateSummary();
                            loadSettlementHistory();
                        }
                )
                .addOnFailureListener(
                        e -> {

                            Toast.makeText(
                                    this,
                                    "Unable to load payments.",
                                    Toast.LENGTH_LONG
                            ).show();

                            updateSummary();
                            loadSettlementHistory();
                        }
                );
    }

    private void updateSummary() {

        totalFareText.setText(
                "Completed Fares\n₱"
                        + formatMoney(totalFare)
        );

        platformFeeText.setText(
                "Sakay Na Platform Fee\n₱"
                        + formatMoney(platformFee)
                        + "  (10%)"
        );

        paidText.setText(
                "Amount Paid\n₱"
                        + formatMoney(totalPaid)
        );

        balanceText.setText(
                "Balance Due\n₱"
                        + formatMoney(balanceDue)
        );
    }

    private void submitPayment() {

        if (user == null) {
            return;
        }

        String amountString =
                amountInput.getText()
                        .toString()
                        .trim();

        String reference =
                referenceInput.getText()
                        .toString()
                        .trim()
                        .toUpperCase(Locale.US);

        if (amountString.isEmpty()) {

            Toast.makeText(
                    this,
                    "Enter payment amount.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (reference.isEmpty()) {

            Toast.makeText(
                    this,
                    "Enter GCash / Maya reference number.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        double amount;

        try {

            amount =
                    Double.parseDouble(
                            amountString
                    );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Invalid payment amount.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (amount <= 0) {

            Toast.makeText(
                    this,
                    "Payment must be greater than zero.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (balanceDue <= 0) {

            Toast.makeText(
                    this,
                    "No balance is currently due.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (amount > balanceDue) {

            Toast.makeText(
                    this,
                    "Payment cannot exceed your current balance.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (reference.length() < 4) {

            Toast.makeText(
                    this,
                    "Reference number is too short.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (reference.length() > 100
                || reference.contains("/")
                || ".".equals(reference)
                || "..".equals(reference)
                || reference.startsWith("__")) {

            Toast.makeText(
                    this,
                    "Invalid reference number.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        final String referenceKey =
                reference;

        db.collection("driverSettlements")
                .document(referenceKey)
                .get()
                .addOnSuccessListener(
                        existing -> {

                            if (existing.exists()) {

                                Toast.makeText(
                                        this,
                                        "❌ This payment reference has already been submitted. It cannot be reused.",
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
                                    "amount",
                                    amount
                            );

                            data.put(
                                    "referenceNumber",
                                    referenceKey
                            );

                            data.put(
                                    "referenceKey",
                                    referenceKey
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

                            db.collection("driverSettlements")
                                    .document(referenceKey)
                                    .set(data)
                                    .addOnSuccessListener(
                                            unused -> {

                                                amountInput.setText("");
                                                referenceInput.setText("");

                                                Toast.makeText(
                                                        this,
                                                        "✅ Payment submitted. Admin must verify the actual payment.",
                                                        Toast.LENGTH_LONG
                                                ).show();

                                                loadSettlement();
                                            }
                                    )
                                    .addOnFailureListener(
                                            e -> Toast.makeText(
                                                    this,
                                                    "Unable to submit payment:\n"
                                                            + e.getMessage(),
                                                    Toast.LENGTH_LONG
                                            ).show()
                                    );
                        }
                )
                .addOnFailureListener(
                        e -> Toast.makeText(
                                this,
                                "Unable to check payment reference:\n"
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void loadSettlementHistory() {

        db.collection("driverSettlements")
                .whereEqualTo(
                        "driverId",
                        user.getUid()
                )
                .get()
                .addOnSuccessListener(
                        snapshots -> {

                            if (snapshots.isEmpty()) {

                                historyText.setText(
                                        "No settlement payments yet."
                                );

                                return;
                            }

                            StringBuilder builder =
                                    new StringBuilder();

                            for (DocumentSnapshot payment
                                    : snapshots.getDocuments()) {

                                double amount =
                                        getAmount(payment);

                                String status =
                                        getString(
                                                payment,
                                                "status"
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

                                builder.append(
                                        "💳 ₱"
                                                + formatMoney(amount)
                                                + "\n"
                                );

                                builder.append(
                                        "Status: "
                                                + status
                                                + "\n"
                                );

                                builder.append(
                                        "Reference: "
                                                + reference
                                                + "\n"
                                );

                                if (submittedAt > 0) {

                                    builder.append(
                                            "Submitted: "
                                                    + formatDate(
                                                    submittedAt
                                            )
                                                    + "\n"
                                    );
                                }

                                Long verifiedAt =
                                        getNullableLong(
                                                payment,
                                                "verifiedAt"
                                        );

                                if (verifiedAt != null
                                        && verifiedAt > 0) {

                                    builder.append(
                                            "Verified: "
                                                    + formatDate(
                                                    verifiedAt
                                            )
                                                    + "\n"
                                    );
                                }

                                builder.append(
                                        "\n--------------------\n\n"
                                );
                            }

                            historyText.setText(
                                    builder.toString()
                            );
                        }
                )
                .addOnFailureListener(
                        e -> historyText.setText(
                                "Unable to load settlement history."
                        )
                );
    }

    private double getFare(
            DocumentSnapshot document) {

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

    private double getAmount(
            DocumentSnapshot document) {

        Object value =
                document.get("amount");

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

    private String getString(
            DocumentSnapshot document,
            String field) {

        String value =
                document.getString(field);

        if (value == null) {
            return "";
        }

        return value;
    }

    private long getLong(
            DocumentSnapshot document,
            String field) {

        Object value =
                document.get(field);

        if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        return 0L;
    }

    private Long getNullableLong(
            DocumentSnapshot document,
            String field) {

        Object value =
                document.get(field);

        if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        return null;
    }

    private String formatMoney(
            double amount) {

        return String.format(
                Locale.US,
                "%.2f",
                amount
        );
    }

    private String formatDate(
            long timestamp) {

        return new SimpleDateFormat(
                "MMM dd, yyyy hh:mm a",
                Locale.US
        ).format(
                new Date(timestamp)
        );
    }
}
