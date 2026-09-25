package com.sakyna.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

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
        scrollView.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(30, 30, 30, 40);
        root.setBackgroundColor(Color.WHITE);

        scrollView.addView(root);

        TextView title = new TextView(this);
        title.setText("💰 DRIVER SETTLEMENT");
        title.setTextSize(27);
        title.setTextColor(Color.rgb(10, 55, 120));
        title.setGravity(Gravity.CENTER);
        title.setPadding(10, 20, 10, 25);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText(
                "Your unpaid Sakay Na platform fees are automatically combined into one balance."
        );
        subtitle.setTextSize(16);
        subtitle.setTextColor(Color.DKGRAY);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(10, 0, 10, 25);
        root.addView(subtitle);

        totalFareText = createSummaryText(
                "Accepted Fares With Dues\n₱0.00"
        );
        root.addView(totalFareText);

        platformFeeText = createSummaryText(
                "Sakay Na Platform Fee\n₱0.00"
        );
        root.addView(platformFeeText);

        paidText = createSummaryText(
                "Verified Payments\n₱0.00"
        );
        root.addView(paidText);

        balanceText = createSummaryText(
                "BALANCE DUE\n₱0.00"
        );
        root.addView(balanceText);

        TextView paymentTitle = new TextView(this);
        paymentTitle.setText("💳 MAKE A SETTLEMENT PAYMENT");
        paymentTitle.setTextSize(22);
        paymentTitle.setTextColor(Color.rgb(10, 55, 120));
        paymentTitle.setPadding(0, 35, 0, 15);
        root.addView(paymentTitle);

        TextView paymentInfo = new TextView(this);
        paymentInfo.setText(
                "Platform fee: 10% of every accepted booking fare.\n\n"
                        + "All unpaid dues are combined into ONE balance.\n\n"
                        + "⚠️ You must pay the FULL current balance.\n\n"
                        + "Payment remains PENDING until Admin verifies the actual payment."
        );
        paymentInfo.setTextSize(16);
        paymentInfo.setTextColor(Color.DKGRAY);
        paymentInfo.setPadding(0, 0, 0, 15);
        root.addView(paymentInfo);

        TextView gcashTitle = new TextView(this);
        gcashTitle.setText("📲 PAY DRIVER DUES WITH GCASH");
        gcashTitle.setTextSize(21);
        gcashTitle.setTextColor(Color.rgb(0, 90, 190));
        gcashTitle.setGravity(Gravity.CENTER);
        gcashTitle.setPadding(0, 15, 0, 10);
        root.addView(gcashTitle);

        TextView gcashInfo = new TextView(this);
        gcashInfo.setText(
                "Scan this QR with GCash using another phone/device.\n"
                        + "Pay the exact BALANCE DUE shown above.\n"
                        + "After payment, keep the GCash reference number.\n\n"
                        + "📱 QR READY TO SCAN"
        );
        gcashInfo.setTextSize(15);
        gcashInfo.setTextColor(Color.DKGRAY);
        gcashInfo.setGravity(Gravity.CENTER);
        gcashInfo.setPadding(10, 0, 10, 12);
        root.addView(gcashInfo);

        /*
         * ============================================================
         * GCASH QR
         * ============================================================
         *
         * Uses the EXISTING gcash_qr drawable.
         *
         * The QR is displayed inside a fixed square viewing area.
         * The surrounding parts of the original photo are cropped
         * visually by CENTER_CROP.
         *
         * No tap-to-zoom.
         * No pinch-to-zoom.
         * No zoom controls.
         */
        QrCropImageView gcashQr =
                new QrCropImageView(this);

        gcashQr.setImageResource(
                R.drawable.gcash_qr
        );

        gcashQr.setScaleType(
                ImageView.ScaleType.CENTER_CROP
        );

        gcashQr.setBackgroundColor(
                Color.WHITE
        );

        gcashQr.setPadding(
                0,
                0,
                0,
                0
        );

        gcashQr.setClickable(false);
        gcashQr.setFocusable(false);
        gcashQr.setLongClickable(false);

        LinearLayout.LayoutParams qrParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                );

        qrParams.setMargins(
                0,
                8,
                0,
                18
        );

        gcashQr.setLayoutParams(
                qrParams
        );

        root.addView(gcashQr);

        TextView qrNotice = new TextView(this);
        qrNotice.setText(
                "GCash QR payment is the temporary manual payment method. "
                        + "Admin verifies the actual GCash transaction before dues are marked PAID."
        );
        qrNotice.setTextSize(14);
        qrNotice.setTextColor(
                Color.rgb(80, 80, 80)
        );
        qrNotice.setGravity(
                Gravity.CENTER
        );
        qrNotice.setPadding(
                10,
                5,
                10,
                20
        );
        root.addView(qrNotice);

        amountInput = new EditText(this);
        amountInput.setHint(
                "Full payment amount"
        );
        amountInput.setInputType(
                InputType.TYPE_CLASS_NUMBER
                        | InputType.TYPE_NUMBER_FLAG_DECIMAL
        );
        root.addView(amountInput);

        referenceInput = new EditText(this);
        referenceInput.setHint(
                "GCash reference number"
        );
        referenceInput.setSingleLine(true);
        root.addView(referenceInput);

        Button submitButton =
                new Button(this);

        submitButton.setText(
                "💸 SUBMIT FULL GCASH PAYMENT"
        );

        submitButton.setTextColor(
                Color.WHITE
        );

        submitButton.setBackgroundColor(
                Color.rgb(0, 120, 200)
        );

        submitButton.setOnClickListener(
                v -> submitPayment()
        );

        root.addView(submitButton);

        Button refreshButton =
                new Button(this);

        refreshButton.setText(
                "🔄 REFRESH BALANCE"
        );

        refreshButton.setOnClickListener(
                v -> loadSettlement()
        );

        root.addView(refreshButton);

        TextView historyTitle =
                new TextView(this);

        historyTitle.setText(
                "📜 SETTLEMENT HISTORY"
        );

        historyTitle.setTextSize(22);

        historyTitle.setTextColor(
                Color.rgb(10, 55, 120)
        );

        historyTitle.setPadding(
                0,
                35,
                0,
                15
        );

        root.addView(historyTitle);

        historyText =
                new TextView(this);

        historyText.setText(
                "Loading settlement history..."
        );

        historyText.setTextSize(16);

        historyText.setTextColor(
                Color.DKGRAY
        );

        historyText.setPadding(
                0,
                5,
                0,
                20
        );

        root.addView(historyText);

        Button backButton =
                new Button(this);

        backButton.setText(
                "⬅️ BACK TO DRIVER DASHBOARD"
        );

        backButton.setOnClickListener(
                v -> finish()
        );

        root.addView(backButton);

        setContentView(
                scrollView
        );
    }

    private TextView createSummaryText(
            String text
    ) {

        TextView view =
                new TextView(this);

        view.setText(text);
        view.setTextSize(19);
        view.setTextColor(Color.BLACK);
        view.setGravity(Gravity.CENTER);
        view.setPadding(
                20,
                25,
                20,
                25
        );

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

        view.setLayoutParams(params);

        view.setBackgroundColor(
                Color.rgb(245, 245, 245)
        );

        return view;
    }

    private void loadSettlement() {

        if (user == null) return;

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
                        "driverDuesStatus",
                        "DUE"
                )
                .get()
                .addOnSuccessListener(
                        duesRides -> {

                            for (
                                    DocumentSnapshot ride :
                                    duesRides.getDocuments()
                            ) {

                                totalFare +=
                                        getFare(ride);

                                platformFee +=
                                        getDriverDue(ride);
                            }

                            totalFare =
                                    roundMoney(
                                            totalFare
                                    );

                            platformFee =
                                    roundMoney(
                                            platformFee
                                    );

                            loadPendingPayments();
                        }
                )
                .addOnFailureListener(
                        e -> {

                            Toast.makeText(
                                    this,
                                    "Unable to load driver dues.",
                                    Toast.LENGTH_LONG
                            ).show();

                            updateSummary();
                            loadSettlementHistory();
                        }
                );
    }

    private void loadPendingPayments() {

        db.collection(
                        "driverSettlements"
                )
                .whereEqualTo(
                        "driverId",
                        user.getUid()
                )
                .whereEqualTo(
                        "status",
                        "PENDING"
                )
                .get()
                .addOnSuccessListener(
                        pendingPayments -> {

                            balanceDue =
                                    roundMoney(
                                            platformFee
                                    );

                            loadVerifiedPayments();
                        }
                )
                .addOnFailureListener(
                        e -> {

                            balanceDue =
                                    roundMoney(
                                            platformFee
                                    );

                            loadVerifiedPayments();
                        }
                );
    }

    private void loadVerifiedPayments() {

        db.collection(
                        "driverSettlements"
                )
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

                            for (
                                    DocumentSnapshot payment :
                                    payments.getDocuments()
                            ) {

                                totalPaid +=
                                        getAmount(
                                                payment
                                        );
                            }

                            totalPaid =
                                    roundMoney(
                                            totalPaid
                                    );

                            balanceDue =
                                    roundMoney(
                                            platformFee
                                    );

                            updateSummary();
                            loadSettlementHistory();
                        }
                )
                .addOnFailureListener(
                        e -> {

                            balanceDue =
                                    roundMoney(
                                            platformFee
                                    );

                            updateSummary();
                            loadSettlementHistory();
                        }
                );
    }

    private void updateSummary() {

        totalFareText.setText(
                "Accepted Fares With Dues\n₱"
                        + formatMoney(
                        totalFare
                )
        );

        platformFeeText.setText(
                "Sakay Na Platform Fee\n₱"
                        + formatMoney(
                        platformFee
                )
                        + "  (10%)"
        );

        paidText.setText(
                "Verified Payments\n₱"
                        + formatMoney(
                        totalPaid
                )
        );

        balanceText.setText(
                "BALANCE DUE\n₱"
                        + formatMoney(
                        balanceDue
                )
        );
    }

    private void submitPayment() {

        if (user == null) return;

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
                .addOnSuccessListener(
                        duesRides -> {

                            double currentDue =
                                    0.0;

                            for (
                                    DocumentSnapshot ride :
                                    duesRides.getDocuments()
                            ) {

                                currentDue +=
                                        getDriverDue(
                                                ride
                                        );
                            }

                            currentDue =
                                    roundMoney(
                                            currentDue
                                    );

                            submitPaymentAgainstCurrentDues(
                                    currentDue,
                                    duesRides
                            );
                        }
                )
                .addOnFailureListener(
                        e ->
                                Toast.makeText(
                                        this,
                                        "Unable to verify current dues.",
                                        Toast.LENGTH_LONG
                                ).show()
                );
    }

    private void submitPaymentAgainstCurrentDues(
            double currentDue,
            QuerySnapshot duesRides
    ) {

        String amountString =
                amountInput
                        .getText()
                        .toString()
                        .trim();

        String reference =
                referenceInput
                        .getText()
                        .toString()
                        .trim()
                        .toUpperCase(
                                Locale.US
                        );

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
                    "Enter GCash reference number.",
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

        amount =
                roundMoney(
                        amount
                );

        if (currentDue <= 0.0) {

            Toast.makeText(
                    this,
                    "No unpaid platform fee is currently due.",
                    Toast.LENGTH_LONG
            ).show();

            loadSettlement();

            return;
        }

        if (
                Math.abs(
                        amount - currentDue
                ) > 0.009
        ) {

            Toast.makeText(
                    this,
                    "Full payment required.\nCurrent balance: ₱"
                            + formatMoney(
                            currentDue
                    ),
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

        if (
                reference.length() > 100
                        || reference.contains("/")
                        || ".".equals(reference)
                        || "..".equals(reference)
                        || reference.startsWith("__")
        ) {

            Toast.makeText(
                    this,
                    "Invalid reference number.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        final double paymentAmount =
                amount;

        final String referenceKey =
                reference;

        db.collection(
                        "driverSettlements"
                )
                .document(
                        referenceKey
                )
                .get()
                .addOnSuccessListener(
                        existing -> {

                            if (existing.exists()) {

                                Toast.makeText(
                                        this,
                                        "❌ This payment reference has already been submitted.",
                                        Toast.LENGTH_LONG
                                ).show();

                                return;
                            }

                            db.collection(
                                            "driverSettlements"
                                    )
                                    .whereEqualTo(
                                            "driverId",
                                            user.getUid()
                                    )
                                    .whereEqualTo(
                                            "status",
                                            "PENDING"
                                    )
                                    .get()
                                    .addOnSuccessListener(
                                            pending -> {

                                                if (
                                                        !pending.isEmpty()
                                                ) {

                                                    Toast.makeText(
                                                            this,
                                                            "A settlement payment is already pending Admin verification.",
                                                            Toast.LENGTH_LONG
                                                    ).show();

                                                    return;
                                                }

                                                long submittedAt =
                                                        System.currentTimeMillis();

                                                Map<String, Object> data =
                                                        new HashMap<>();

                                                data.put(
                                                        "driverId",
                                                        user.getUid()
                                                );

                                                data.put(
                                                        "amount",
                                                        paymentAmount
                                                );

                                                data.put(
                                                        "duesAmountAtSubmission",
                                                        currentDue
                                                );

                                                data.put(
                                                        "duesCutoffAt",
                                                        submittedAt
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
                                                        "GCASH"
                                                );

                                                data.put(
                                                        "status",
                                                        "PENDING"
                                                );

                                                data.put(
                                                        "submittedAt",
                                                        submittedAt
                                                );

                                                data.put(
                                                        "verifiedAt",
                                                        null
                                                );

                                                db.collection(
                                                                "driverSettlements"
                                                        )
                                                        .document(
                                                                referenceKey
                                                        )
                                                        .set(
                                                                data
                                                        )
                                                        .addOnSuccessListener(
                                                                unused -> {

                                                                    amountInput.setText(
                                                                            ""
                                                                    );

                                                                    referenceInput.setText(
                                                                            ""
                                                                    );

                                                                    Toast.makeText(
                                                                            this,
                                                                            "✅ GCash settlement submitted. Admin must verify the actual payment.",
                                                                            Toast.LENGTH_LONG
                                                                    ).show();

                                                                    loadSettlement();
                                                                }
                                                        )
                                                        .addOnFailureListener(
                                                                e ->
                                                                        Toast.makeText(
                                                                                this,
                                                                                "Unable to submit payment:\n"
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
                                                            "Unable to check pending settlement:\n"
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
                                        "Unable to check payment reference:\n"
                                                + e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show()
                );
    }

    private void loadSettlementHistory() {

        if (user == null) return;

        db.collection(
                        "driverSettlements"
                )
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

                            for (
                                    DocumentSnapshot payment :
                                    snapshots.getDocuments()
                            ) {

                                double amount =
                                        getAmount(
                                                payment
                                        );

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

                                double duesAtSubmission =
                                        getAmountField(
                                                payment,
                                                "duesAmountAtSubmission"
                                        );

                                builder.append(
                                        "💳 ₱"
                                                + formatMoney(
                                                amount
                                        )
                                                + "\n"
                                );

                                builder.append(
                                        "Method: "
                                                + getString(
                                                payment,
                                                "paymentMethod"
                                        )
                                                + "\n"
                                );

                                builder.append(
                                        "Status: "
                                                + status
                                                + "\n"
                                );

                                if (
                                        duesAtSubmission > 0
                                ) {

                                    builder.append(
                                            "Dues Covered: ₱"
                                                    + formatMoney(
                                                    duesAtSubmission
                                            )
                                                    + "\n"
                                    );
                                }

                                builder.append(
                                        "Reference: "
                                                + reference
                                                + "\n"
                                );

                                if (
                                        submittedAt > 0
                                ) {

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

                                if (
                                        verifiedAt != null
                                                && verifiedAt > 0
                                ) {

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
                        e ->
                                historyText.setText(
                                        "Unable to load settlement history."
                                )
                );
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

        double fare =
                getFare(
                        document
                );

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

    private String getString(
            DocumentSnapshot document,
            String field
    ) {

        String value =
                document.getString(
                        field
                );

        return value == null
                ? ""
                : value;
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

    private Long getNullableLong(
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

        return null;
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

        return new SimpleDateFormat(
                "MMM dd, yyyy hh:mm a",
                Locale.US
        ).format(
                new Date(timestamp)
        );
    }

    // ============================================================
    // FIXED QR DISPLAY
    // ============================================================

    private static class QrCropImageView
            extends ImageView {

        public QrCropImageView(Activity context) {
            super(context);

            setScaleType(
                    ImageView.ScaleType.CENTER_CROP
            );

            setAdjustViewBounds(false);

            setClickable(false);
            setFocusable(false);
            setLongClickable(false);

            setBackgroundColor(
                    Color.WHITE
            );
        }

        @Override
        protected void onMeasure(
                int widthMeasureSpec,
                int heightMeasureSpec
        ) {

            int width =
                    MeasureSpec.getSize(
                            widthMeasureSpec
                    );

            if (width > 0) {

                int squareSize =
                        Math.max(
                                1,
                                width
                        );

                heightMeasureSpec =
                        MeasureSpec.makeMeasureSpec(
                                squareSize,
                                MeasureSpec.EXACTLY
                        );
            }

            super.onMeasure(
                    widthMeasureSpec,
                    heightMeasureSpec
            );
        }
    }
}
