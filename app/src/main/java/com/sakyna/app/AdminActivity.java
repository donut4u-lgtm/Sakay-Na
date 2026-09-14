
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AdminActivity extends Activity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private LinearLayout content;

    private TextView statisticsText;

    private int totalUsers = 0;
    private int totalPassengers = 0;
    private int totalDrivers = 0;
    private int approvedDrivers = 0;
    private int pendingDrivers = 0;
    private int activeUsers = 0;
    private int totalRides = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();

        db = FirebaseFirestore.getInstance();

        showDashboard();

        loadUsers();

        loadRides();
    }

    private void showDashboard() {

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setBackgroundColor(
                Color.WHITE
        );

        TextView header =
                text(
                        "👨‍💼 SAKAY NA ADMIN",
                        26,
                        Color.rgb(20, 20, 20)
                );

        header.setGravity(
                Gravity.CENTER
        );

        header.setPadding(
                20,
                35,
                20,
                20
        );

        root.addView(
                header
        );

        TextView subtitle =
                text(
                        "Control Center",
                        16,
                        Color.DKGRAY
                );

        subtitle.setGravity(
                Gravity.CENTER
        );

        root.addView(
                subtitle
        );

        statisticsText =
                text(
                        "Loading statistics...",
                        16,
                        Color.rgb(30, 100, 70)
                );

        statisticsText.setPadding(
                20,
                20,
                20,
                20
        );

        root.addView(
                statisticsText
        );

        Button refresh =
                button(
                        "🔄 REFRESH DATA"
                );

        refresh.setOnClickListener(
                v -> {

                    resetStatistics();

                    loadUsers();

                    loadRides();

                    showMessage(
                            "Refreshing admin data..."
                    );
                }
        );

        root.addView(
                refresh
        );

        Button usersButton =
                button(
                        "👥 USERS"
                );

        usersButton.setOnClickListener(
                v -> loadUsers()
        );

        root.addView(
                usersButton
        );

        Button ridesButton =
                button(
                        "🚕 RIDES"
                );

        ridesButton.setOnClickListener(
                v -> loadRides()
        );

        root.addView(
                ridesButton
        );

        Button logout =
                button(
                        "🚪 LOGOUT"
                );

        logout.setOnClickListener(
                v -> logout()
        );

        root.addView(
                logout
        );

        ScrollView scrollView =
                new ScrollView(this);

        content =
                new LinearLayout(this);

        content.setOrientation(
                LinearLayout.VERTICAL
        );

        content.setPadding(
                15,
                10,
                15,
                30
        );

        scrollView.addView(
                content
        );

        root.addView(
                scrollView,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        setContentView(
                root
        );
    }

    private void loadUsers() {

        if (content == null) {
            return;
        }

        db.collection("users")
                .get()
                .addOnSuccessListener(
                        documents -> {

                            resetUserStatistics();

                            content.removeAllViews();

                            TextView title =
                                    text(
                                            "👥 REGISTERED USERS",
                                            20,
                                            Color.BLACK
                                    );

                            content.addView(
                                    title
                            );

                            if (documents.isEmpty()) {

                                content.addView(
                                        text(
                                                "No users found.",
                                                15,
                                                Color.GRAY
                                        )
                                );

                                updateStatistics();

                                return;
                            }

                            for (DocumentSnapshot doc :
                                    documents) {

                                totalUsers++;

                                String uid =
                                        doc.getId();

                                String email =
                                        doc.getString(
                                                "email"
                                        );

                                String role =
                                        doc.getString(
                                                "role"
                                        );

                                Boolean active =
                                        doc.getBoolean(
                                                "active"
                                        );

                                Boolean approved =
                                        doc.getBoolean(
                                                "approved"
                                        );

                                if (email == null) {
                                    email = "No email";
                                }

                                if (role == null) {
                                    role = "UNKNOWN";
                                }

                                boolean isActive =
                                        active == null ||
                                        active;

                                if (isActive) {
                                    activeUsers++;
                                }

                                if (role.equals(
                                        "PASSENGER"
                                )) {

                                    totalPassengers++;

                                } else if (role.equals(
                                        "DRIVER"
                                )) {

                                    totalDrivers++;

                                    if (approved != null &&
                                            approved) {

                                        approvedDrivers++;

                                    } else {

                                        pendingDrivers++;
                                    }
                                }

                                addUserCard(
                                        doc,
                                        uid,
                                        email,
                                        role,
                                        isActive,
                                        approved
                                );
                            }

                            updateStatistics();
                        }
                )
                .addOnFailureListener(
                        e -> {

                            showMessage(
                                    "Unable to load users: " +
                                    e.getMessage()
                            );
                        }
                );
    }

    private void addUserCard(
            DocumentSnapshot doc,
            String uid,
            String email,
            String role,
            boolean active,
            Boolean approved) {

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

        TextView account =
                text(
                        roleIcon(role) +
                        " " +
                        role,
                        19,
                        Color.BLACK
                );

        card.addView(
                account
        );

        TextView emailText =
                text(
                        "Email: " + email,
                        15,
                        Color.DKGRAY
                );

        card.addView(
                emailText
        );

        TextView uidText =
                text(
                        "UID: " + uid,
                        11,
                        Color.GRAY
                );

        card.addView(
                uidText
        );

        String status;

        if (!active) {

            status =
                    "⛔ Account disabled";

        } else if (role.equals(
                "DRIVER"
        )) {

            if (approved != null &&
                    approved) {

                status =
                        "🟢 Driver approved";

            } else {

                status =
                        "🟡 Driver approval pending";
            }

        } else {

            status =
                    "🟢 Account active";
        }

        TextView statusText =
                text(
                        status,
                        14,
                        Color.rgb(50, 110, 70)
                );

        card.addView(
                statusText
        );

        if (role.equals(
                "DRIVER"
        )) {

            Button approvalButton =
                    button(
                            approved != null &&
                            approved
                                    ? "⛔ REMOVE APPROVAL"
                                    : "✅ APPROVE DRIVER"
                    );

            approvalButton.setOnClickListener(
                    v -> {

                        boolean newValue =
                                !(approved != null &&
                                        approved);

                        updateDriverApproval(
                                uid,
                                newValue
                        );
                    }
            );

            card.addView(
                    approvalButton
            );
        }

        Button activeButton =
                button(
                        active
                                ? "⛔ DISABLE ACCOUNT"
                                : "✅ ENABLE ACCOUNT"
                );

        activeButton.setOnClickListener(
                v -> updateAccountStatus(
                        uid,
                        !active
                )
        );

        card.addView(
                activeButton
        );

        addSeparator();

        content.addView(
                card
        );
    }

    private void updateDriverApproval(
            String uid,
            boolean approved) {

        db.collection("users")
                .document(uid)
                .update(
                        "approved",
                        approved
                )
                .addOnSuccessListener(
                        unused -> {

                            showMessage(
                                    approved
                                            ? "Driver approved."
                                            : "Driver approval removed."
                            );

                            loadUsers();
                        }
                )
                .addOnFailureListener(
                        e -> showMessage(
                                "Approval update failed: " +
                                e.getMessage()
                        )
                );
    }

    private void updateAccountStatus(
            String uid,
            boolean active) {

        db.collection("users")
                .document(uid)
                .update(
                        "active",
                        active
                )
                .addOnSuccessListener(
                        unused -> {

                            showMessage(
                                    active
                                            ? "Account enabled."
                                            : "Account disabled."
                            );

                            loadUsers();
                        }
                )
                .addOnFailureListener(
                        e -> showMessage(
                                "Account update failed: " +
                                e.getMessage()
                        )
                );
    }

    private void loadRides() {

        db.collection("rides")
                .get()
                .addOnSuccessListener(
                        documents -> {

                            totalRides =
                                    documents.size();

                            updateStatistics();

                            content.removeAllViews();

                            TextView title =
                                    text(
                                            "🚕 RIDE MANAGEMENT",
                                            20,
                                            Color.BLACK
                                    );

                            content.addView(
                                    title
                            );

                            if (documents.isEmpty()) {

                                content.addView(
                                        text(
                                                "No rides found.",
                                                15,
                                                Color.GRAY
                                        )
                                );

                                return;
                            }

                            for (DocumentSnapshot doc :
                                    documents) {

                                addRideCard(
                                        doc
                                );
                            }
                        }
                )
                .addOnFailureListener(
                        e -> showMessage(
                                "Unable to load rides: " +
                                e.getMessage()
                        )
                );
    }

    private void addRideCard(
            DocumentSnapshot doc) {

        String rideId =
                doc.getId();

        String status =
                doc.getString(
                        "status"
                );

        String passengerId =
                doc.getString(
                        "passengerId"
                );

        String driverId =
                doc.getString(
                        "driverId"
                );

        String pickup =
                doc.getString(
                        "pickup"
                );

        String destination =
                doc.getString(
                        "destination"
                );

        if (status == null) {
            status = "UNKNOWN";
        }

        if (passengerId == null) {
            passengerId = "Not assigned";
        }

        if (driverId == null ||
                driverId.isEmpty()) {

            driverId =
                    "Not assigned";
        }

        if (pickup == null) {
            pickup = "Unknown pickup";
        }

        if (destination == null) {
            destination =
                    "Unknown destination";
        }

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

        TextView rideTitle =
                text(
                        "🚕 Ride " + rideId,
                        17,
                        Color.BLACK
                );

        card.addView(
                rideTitle
        );

        card.addView(
                text(
                        "Status: " + status,
                        15,
                        Color.rgb(20, 110, 70)
                )
        );

        card.addView(
                text(
                        "Passenger: " +
                        passengerId,
                        13,
                        Color.DKGRAY
                )
        );

        card.addView(
                text(
                        "Driver: " +
                        driverId,
                        13,
                        Color.DKGRAY
                )
        );

        card.addView(
                text(
                        "Pickup: " +
                        pickup,
                        13,
                        Color.DKGRAY
                )
        );

        card.addView(
                text(
                        "Destination: " +
                        destination,
                        13,
                        Color.DKGRAY
                )
        );

        addSeparator();

        content.addView(
                card
        );
    }

    private void resetStatistics() {

        totalUsers = 0;

        totalPassengers = 0;

        totalDrivers = 0;

        approvedDrivers = 0;

        pendingDrivers = 0;

        activeUsers = 0;

        totalRides = 0;
    }

    private void resetUserStatistics() {

        totalUsers = 0;

        totalPassengers = 0;

        totalDrivers = 0;

        approvedDrivers = 0;

        pendingDrivers = 0;

        activeUsers = 0;
    }

    private void updateStatistics() {

        if (statisticsText == null) {
            return;
        }

        String statistics =
                "📊 SAKAY NA STATISTICS\n\n" +

                "👥 Total Users: " +
                totalUsers +

                "\n🧍 Passengers: " +
                totalPassengers +

                "\n🛺 Drivers: " +
                totalDrivers +

                "\n✅ Approved Drivers: " +
                approvedDrivers +

                "\n🟡 Pending Drivers: " +
                pendingDrivers +

                "\n🟢 Active Accounts: " +
                activeUsers +

                "\n🚕 Total Rides: " +
                totalRides;

        statisticsText.setText(
                statistics
        );
    }

    private String roleIcon(
            String role) {

        if (role.equals(
                "DRIVER"
        )) {

            return "🛺";

        }

        if (role.equals(
                "ADMIN"
        )) {

            return "👨‍💼";

        }

        if (role.equals(
                "PASSENGER"
        )) {

            return "🧍";
        }

        return "👤";
    }

    private void addSeparator() {

        TextView separator =
                text(
                        "────────────────────",
                        10,
                        Color.LTGRAY
                );

        content.addView(
                separator
        );
    }

    private TextView text(
            String value,
            int size,
            int color) {

        TextView view =
                new TextView(this);

        view.setText(
                value
        );

        view.setTextSize(
                size
        );

        view.setTextColor(
                color
        );

        view.setPadding(
                8,
                8,
                8,
                8
        );

        return view;
    }

    private Button button(
            String label) {

        Button button =
                new Button(this);

        button.setText(
                label
        );

        button.setTextSize(
                14
        );

        button.setAllCaps(
                false
        );

        return button;
    }

    private void logout() {

        auth.signOut();

        Intent intent =
                new Intent(
                        this,
                        MainActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        finish();
    }

    private void showMessage(
            String message) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }
}
