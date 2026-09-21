
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
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

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    auth = FirebaseAuth.getInstance();
    db = FirebaseFirestore.getInstance();
    user = auth.getCurrentUser();

    if (user == null
            || !"Ld3rzaCvAGNlXBDCofB3mWjgXWp2"
            .equals(user.getUid())) {

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

    ScrollView scrollView = new ScrollView(this);

    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(25, 25, 25, 40);
    root.setBackgroundColor(Color.WHITE);

    TextView title = new TextView(this);
    title.setText(
            "🛡️ ADMIN\nSETTLEMENT VERIFICATION"
    );
    title.setTextSize(27);
    title.setTextColor(Color.BLACK);
    title.setGravity(Gravity.CENTER);
    title.setPadding(10, 20, 10, 25);
    root.addView(title);

    statusText = new TextView(this);
    statusText.setText("Loading settlements...");
    statusText.setTextSize(16);
    statusText.setTextColor(Color.DKGRAY);
    statusText.setGravity(Gravity.CENTER);
    statusText.setPadding(10, 5, 10, 15);
    root.addView(statusText);

    summaryText = new TextView(this);
    summaryText.setText(
            "Pending: ₱0.00\n"
                    + "Verified: ₱0.00"
    );
    summaryText.setTextSize(19);
    summaryText.setTextColor(Color.BLACK);
    summaryText.setGravity(Gravity.CENTER);
    summaryText.setPadding(20, 25, 20, 25);
    summaryText.setBackgroundColor(
            Color.rgb(245, 245, 245)
    );
    root.addView(summaryText);

    Button refreshButton = new Button(this);
    refreshButton.setText("🔄 REFRESH SETTLEMENTS");
    refreshButton.setOnClickListener(
            v -> loadSettlements()
    );
    root.addView(refreshButton);

    TextView listTitle = new TextView(this);
    listTitle.setText("💳 DRIVER PAYMENTS");
    listTitle.setTextSize(22);
    listTitle.setTextColor(Color.BLACK);
    listTitle.setPadding(0, 30, 0, 15);
    root.addView(listTitle);

    settlementContainer = new LinearLayout(this);
    settlementContainer.setOrientation(
            LinearLayout.VERTICAL
    );
    root.addView(settlementContainer);

    Button backButton = new Button(this);
    backButton.setText("⬅️ BACK TO ADMIN");
    backButton.setOnClickListener(
            v -> finish()
    );
    root.addView(backButton);

    scrollView.addView(root);
    setContentView(scrollView);
}

private void loadSettlements() {

    if (user == null) {
        return;
    }

    statusText.setText(
            "Loading settlement records..."
    );

    settlementContainer.removeAllViews();

    pendingTotal = 0.0;
    verifiedTotal = 0.0;

    db.collection("driverSettlements")
            .get()
            .addOnSuccessListener(
                    snapshots -> {

                        if (snapshots.isEmpty()) {

                            summaryText.setText(
                                    "⏳ Pending Payments\n₱0.00\n\n"
                                            + "✅ Verified Payments\n₱0.00"
                            );

                            statusText.setText(
                                    "No settlement payments found."
                            );

                            return;
                        }

                        for (DocumentSnapshot settlement
                                : snapshots.getDocuments()) {

                            String status =
                                    getString(
                                            settlement,
                                            "status"
                                    );

                            double amount =
                                    getAmount(settlement);

                            if ("PENDING".equalsIgnoreCase(
                                    status)) {

                                pendingTotal += amount;

                            } else if ("VERIFIED".equalsIgnoreCase(
                                    status)) {

                                verifiedTotal += amount;
                            }

                            addSettlementCard(
                                    settlement
                            );
                        }

                        updateSummary();

                        statusText.setText(
                                snapshots.size()
                                        + " settlement record(s) found."
                        );
                    }
            )
            .addOnFailureListener(
                    e -> {

                        statusText.setText(
                                "Unable to load settlements."
                        );

                        Toast.makeText(
                                this,
                                "Unable to load settlements:\n"
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
            );
}

private void updateSummary() {

    summaryText.setText(
            "⏳ Pending Payments\n₱"
                    + formatMoney(pendingTotal)
                    + "\n\n"
                    + "✅ Verified Payments\n₱"
                    + formatMoney(verifiedTotal)
    );
}

private void addSettlementCard(
        DocumentSnapshot settlement) {

    LinearLayout card = new LinearLayout(this);
    card.setOrientation(LinearLayout.VERTICAL);
    card.setPadding(25, 25, 25, 25);

    LinearLayout.LayoutParams cardParams =
            new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );

    cardParams.setMargins(0, 10, 0, 20);
    card.setLayoutParams(cardParams);

    card.setBackgroundColor(
            Color.rgb(245, 245, 245)
    );

    String settlementId =
            settlement.getId();

    String driverId =
            getString(
                    settlement,
                    "driverId"
            );

    double amount =
            getAmount(settlement);

    String status =
            getString(
                    settlement,
                    "status"
            );

    String reference =
            getString(
                    settlement,
                    "referenceNumber"
            );

    String paymentMethod =
            getString(
                    settlement,
                    "paymentMethod"
            );

    long submittedAt =
            getLong(
                    settlement,
                    "submittedAt"
            );

    TextView title = new TextView(this);
    title.setText("💳 SETTLEMENT PAYMENT");
    title.setTextSize(21);
    title.setTextColor(Color.BLACK);
    title.setPadding(0, 0, 0, 15);
    card.addView(title);

    TextView details = new TextView(this);

    details.setText(
            "👤 Driver ID\n"
                    + driverId
                    + "\n\n"
                    + "💰 Amount\n₱"
                    + formatMoney(amount)
                    + "\n\n"
                    + "💳 Method\n"
                    + paymentMethod
                    + "\n\n"
                    + "🔢 Reference\n"
                    + reference
                    + "\n\n"
                    + "📊 Status\n"
                    + status
                    + "\n\n"
                    + "📅 Submitted\n"
                    + formatDate(submittedAt)
    );

    details.setTextSize(16);
    details.setTextColor(Color.DKGRAY);
    details.setPadding(0, 0, 0, 20);
    card.addView(details);

    if ("PENDING".equalsIgnoreCase(status)) {

        Button verifyButton = new Button(this);
        verifyButton.setText("✅ VERIFY PAYMENT");
        verifyButton.setTextColor(Color.WHITE);
        verifyButton.setBackgroundColor(
                Color.rgb(0, 150, 0)
        );

        verifyButton.setOnClickListener(
                v -> verifySettlement(
                        settlementId
                )
        );

        card.addView(verifyButton);

        Button rejectButton = new Button(this);
        rejectButton.setText("❌ REJECT PAYMENT");
        rejectButton.setTextColor(Color.WHITE);
        rejectButton.setBackgroundColor(
                Color.rgb(200, 0, 0)
        );

        rejectButton.setOnClickListener(
                v -> rejectSettlement(
                        settlementId
                )
        );

        card.addView(rejectButton);
    }

    if ("VERIFIED".equalsIgnoreCase(status)) {

        TextView verified = new TextView(this);

        verified.setText(
                "✅ PAYMENT VERIFIED"
        );

        verified.setTextSize(18);
        verified.setTextColor(
                Color.rgb(0, 130, 0)
        );

        verified.setPadding(
                0,
                10,
                0,
                5
        );

        card.addView(verified);
    }

    if ("REJECTED".equalsIgnoreCase(status)) {

        TextView rejected = new TextView(this);

        rejected.setText(
                "❌ PAYMENT REJECTED"
        );

        rejected.setTextSize(18);
        rejected.setTextColor(
                Color.rgb(190, 0, 0)
        );

        rejected.setPadding(
                0,
                10,
                0,
                5
        );

        card.addView(rejected);
    }

    settlementContainer.addView(card);

    loadDriverInformation(
            driverId,
            details
    );
}

private void loadDriverInformation(
        String driverId,
        TextView details) {

    if (driverId == null
            || driverId.trim().isEmpty()) {
        return;
    }

    db.collection("users")
            .document(driverId)
            .get()
            .addOnSuccessListener(
                    driver -> {

                        if (!driver.exists()) {
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

                        String plate =
                                firstAvailable(
                                        driver,
                                        "plateNumber",
                                        "driverPlateNumber"
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
                                        + "🛺 Plate\n"
                                        + plate
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
        String settlementId) {

    if (settlementId == null
            || settlementId.isEmpty()) {
        return;
    }

    Map<String, Object> update =
            new HashMap<>();

    update.put(
            "status",
            "VERIFIED"
    );

    update.put(
            "verifiedAt",
            System.currentTimeMillis()
    );

    update.put(
            "verifiedBy",
            user.getUid()
    );

    db.collection("driverSettlements")
            .document(settlementId)
            .update(update)
            .addOnSuccessListener(
                    v -> {

                        Toast.makeText(
                                this,
                                "✅ Payment verified.",
                                Toast.LENGTH_SHORT
                        ).show();

                        loadSettlements();
                    }
            )
            .addOnFailureListener(
                    e -> Toast.makeText(
                            this,
                            "Unable to verify payment:\n"
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show()
            );
}

private void rejectSettlement(
        String settlementId) {

    if (settlementId == null
            || settlementId.isEmpty()) {
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

    db.collection("driverSettlements")
            .document(settlementId)
            .update(update)
            .addOnSuccessListener(
                    v -> {

                        Toast.makeText(
                                this,
                                "❌ Payment rejected.",
                                Toast.LENGTH_SHORT
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

private String firstAvailable(
        DocumentSnapshot document,
        String first,
        String second) {

    String value =
            getString(
                    document,
                    first
            );

    if (value != null
            && !value.trim().isEmpty()) {
        return value;
    }

    value =
            getString(
                    document,
                    second
            );

    if (value != null
            && !value.trim().isEmpty()) {
        return value;
    }

    return "Not provided";
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

private double getAmount(
        DocumentSnapshot document) {

    Object value =
            document.get("amount");

    if (value instanceof Number) {
        return ((Number) value).doubleValue();
    }

    if (value != null) {

        try {
            return Double.parseDouble(
                    String.valueOf(value)
            );
        } catch (Exception ignored) {
        }
    }

    return 0.0;
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
