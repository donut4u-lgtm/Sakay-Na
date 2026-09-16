
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminActivity extends Activity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private LinearLayout driverContainer;
    private TextView statusText;

    private final int GREEN = Color.rgb(0, 125, 80);
    private final int DARK = Color.rgb(35, 35, 35);
    private final int LIGHT_GREEN = Color.rgb(232, 247, 238);
    private final int LIGHT_RED = Color.rgb(255, 235, 235);
    private final int LIGHT_YELLOW = Color.rgb(255, 248, 220);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        buildScreen();
        loadDrivers();
    }

    private void buildScreen() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 20, 20, 20);
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("🛺 Sakay Na ADMIN");
        title.setTextSize(28);
        title.setTextColor(GREEN);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 10, 0, 10);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Driver Approval Dashboard");
        subtitle.setTextSize(18);
        subtitle.setTextColor(DARK);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 0, 0, 15);
        root.addView(subtitle);

        statusText = new TextView(this);
        statusText.setText("Loading drivers...");
        statusText.setTextSize(16);
        statusText.setTextColor(DARK);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, 10, 0, 15);
        root.addView(statusText);

        Button refresh = new Button(this);
        refresh.setText("🔄 REFRESH DRIVERS");
        refresh.setTextSize(16);
        refresh.setOnClickListener(v -> loadDrivers());
        root.addView(refresh);

        ScrollView scroll = new ScrollView(this);

        driverContainer = new LinearLayout(this);
        driverContainer.setOrientation(LinearLayout.VERTICAL);
        driverContainer.setPadding(0, 15, 0, 15);

        scroll.addView(driverContainer);

        root.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                )
        );

        Button logout = new Button(this);
        logout.setText("🚪 LOGOUT");
        logout.setTextSize(17);
        logout.setOnClickListener(v -> logout());
        root.addView(logout);

        setContentView(root);
    }

    private void loadDrivers() {

        driverContainer.removeAllViews();
        statusText.setText("Loading drivers...");

        db.collection("users")
                .whereEqualTo("role", "DRIVER")
                .get()
                .addOnSuccessListener(snapshot -> {

                    driverContainer.removeAllViews();

                    if (snapshot.isEmpty()) {
                        statusText.setText("No driver accounts found.");
                        return;
                    }

                    int pending = 0;
                    int approved = 0;
                    int rejected = 0;

                    for (DocumentSnapshot d : snapshot.getDocuments()) {

                        String approvalStatus = d.getString("approvalStatus");

                        if (approvalStatus == null) {
                            Boolean approvedValue = d.getBoolean("approved");

                            if (approvedValue != null && approvedValue) {
                                approvalStatus = "APPROVED";
                            } else {
                                approvalStatus = "PENDING_APPROVAL";
                            }
                        }

                        if ("APPROVED".equalsIgnoreCase(approvalStatus)) {
                            approved++;
                        } else if ("REJECTED".equalsIgnoreCase(approvalStatus)) {
                            rejected++;
                        } else {
                            pending++;
                        }

                        addDriverCard(d, approvalStatus);
                    }

                    statusText.setText(
                            "Drivers: " + snapshot.size()
                                    + "   •   Pending: " + pending
                                    + "   •   Approved: " + approved
                                    + "   •   Rejected: " + rejected
                    );

                })
                .addOnFailureListener(e ->
                        statusText.setText(
                                "Unable to load drivers: " + safeMessage(e)
                        )
                );
    }

    private void addDriverCard(DocumentSnapshot d, String approvalStatus) {

        String phone = d.getString("phone");

        if (phone == null || phone.trim().isEmpty()) {
            phone = "Phone not available";
        }

        String role = d.getString("role");

        if (role == null) {
            role = "DRIVER";
        }

        Boolean approvedValue = d.getBoolean("approved");
        boolean approved = approvedValue != null && approvedValue;

        Boolean onlineValue = d.getBoolean("online");
        boolean online = onlineValue != null && onlineValue;

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(20, 20, 20, 20);

        if ("APPROVED".equalsIgnoreCase(approvalStatus)) {
            card.setBackgroundColor(LIGHT_GREEN);
        } else if ("REJECTED".equalsIgnoreCase(approvalStatus)) {
            card.setBackgroundColor(LIGHT_RED);
        } else {
            card.setBackgroundColor(LIGHT_YELLOW);
        }

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(0, 0, 0, 20);
        driverContainer.addView(card, cardParams);

        TextView driverTitle = new TextView(this);
        driverTitle.setText("🛺 DRIVER");
        driverTitle.setTextSize(21);
        driverTitle.setTextColor(GREEN);
        driverTitle.setGravity(Gravity.CENTER);
        card.addView(driverTitle);

        TextView phoneText = new TextView(this);
        phoneText.setText("Phone: " + phone);
        phoneText.setTextSize(17);
        phoneText.setTextColor(DARK);
        phoneText.setPadding(0, 8, 0, 8);
        card.addView(phoneText);

        TextView roleText = new TextView(this);
        roleText.setText("Role: " + role);
        roleText.setTextSize(16);
        roleText.setTextColor(DARK);
        card.addView(roleText);

        TextView approvalText = new TextView(this);
        approvalText.setText(
                "Approval: " + approvalStatus
        );
        approvalText.setTextSize(17);
        approvalText.setTextColor(DARK);
        approvalText.setPadding(0, 8, 0, 8);
        card.addView(approvalText);

        TextView onlineText = new TextView(this);
        onlineText.setText(
                "Online: " + (online ? "YES" : "NO")
        );
        onlineText.setTextSize(16);
        onlineText.setTextColor(DARK);
        card.addView(onlineText);

        if ("APPROVED".equalsIgnoreCase(approvalStatus)) {

            Button reject = new Button(this);
            reject.setText("❌ REJECT DRIVER");
            reject.setTextSize(15);
            card.addView(reject);

            reject.setOnClickListener(v ->
                    rejectDriver(d.getId())
            );

        } else if ("REJECTED".equalsIgnoreCase(approvalStatus)) {

            Button approve = new Button(this);
            approve.setText("✅ APPROVE DRIVER");
            approve.setTextSize(15);
            card.addView(approve);

            approve.setOnClickListener(v ->
                    approveDriver(d.getId())
            );

        } else {

            Button approve = new Button(this);
            approve.setText("✅ APPROVE DRIVER");
            approve.setTextSize(16);
            card.addView(approve);

            approve.setOnClickListener(v ->
                    approveDriver(d.getId())
            );

            Button reject = new Button(this);
            reject.setText("❌ REJECT DRIVER");
            reject.setTextSize(16);
            card.addView(reject);

            reject.setOnClickListener(v ->
                    rejectDriver(d.getId())
            );
        }
    }

    private void approveDriver(String driverId) {

        db.collection("users")
                .document(driverId)
                .update(
                        "approved", true,
                        "approvalStatus", "APPROVED",
                        "online", false,
                        "approvedAt", System.currentTimeMillis()
                )
                .addOnSuccessListener(v -> {
                    Toast.makeText(
                            this,
                            "Driver approved successfully.",
                            Toast.LENGTH_LONG
                    ).show();

                    loadDrivers();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Approval failed: " + safeMessage(e),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void rejectDriver(String driverId) {

        db.collection("users")
                .document(driverId)
                .update(
                        "approved", false,
                        "approvalStatus", "REJECTED",
                        "online", false,
                        "rejectedAt", System.currentTimeMillis()
                )
                .addOnSuccessListener(v -> {
                    Toast.makeText(
                            this,
                            "Driver rejected.",
                            Toast.LENGTH_LONG
                    ).show();

                    loadDrivers();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Rejection failed: " + safeMessage(e),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void logout() {

        auth.signOut();

        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(i);
        finish();
    }

    private String safeMessage(Exception e) {

        if (e == null ||
                e.getMessage() == null ||
                e.getMessage().trim().isEmpty()) {

            return "Unknown Firebase error.";
        }

        return e.getMessage();
    }
}
