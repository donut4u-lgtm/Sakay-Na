
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

import java.util.HashMap;
import java.util.Map;

public class AdminActivity extends Activity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private TextView statisticsText;
    private LinearLayout usersContainer;
    private LinearLayout ridesContainer;

    private EditText baseFareInput;
    private EditText perKmInput;
    private EditText minimumFareInput;
    private EditText maximumFareInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        showAdminDashboard();

        loadFareSettings();
        loadStatistics();
        loadUsers();
        loadRides();
    }

    private void showAdminDashboard() {

        ScrollView scrollView = new ScrollView(this);

        LinearLayout root = new LinearLayout(this);

        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(25, 30, 25, 30);
        root.setBackgroundColor(Color.WHITE);

        scrollView.addView(root);

        TextView title = text(
                "🛺 SAKAY NA",
                30,
                Color.BLACK
        );

        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView heading = text(
                "ADMIN CONTROL CENTER",
                22,
                Color.rgb(20, 120, 70)
        );

        heading.setGravity(Gravity.CENTER);
        root.addView(heading);

        TextView info = text(
                "Manage fares, drivers, passengers and rides.",
                15,
                Color.DKGRAY
        );

        info.setGravity(Gravity.CENTER);
        root.addView(info);

        addSpace(root, 15);

        TextView statisticsTitle = text(
                "📊 SYSTEM STATISTICS",
                20,
                Color.BLACK
        );

        root.addView(statisticsTitle);

        statisticsText = text(
                "Loading statistics...",
                16,
                Color.DKGRAY
        );

        root.addView(statisticsText);

        addSpace(root, 20);

        TextView fareTitle = text(
                "💰 FARE SETTINGS",
                20,
                Color.BLACK
        );

        root.addView(fareTitle);

        TextView fareInfo = text(
                "Changes here become the central fare settings " +
                "used by the Sakay Na system.",
                14,
                Color.DKGRAY
        );

        root.addView(fareInfo);

        baseFareInput = input("Base fare");
        root.addView(baseFareInput);

        perKmInput = input("Fare per kilometer");
        root.addView(perKmInput);

        minimumFareInput = input("Minimum fare");
        root.addView(minimumFareInput);

        maximumFareInput = input("Maximum fare");
        root.addView(maximumFareInput);

        Button saveFare = button(
                "💾 SAVE FARE SETTINGS"
        );

        saveFare.setOnClickListener(
                v -> saveFareSettings()
        );

        root.addView(saveFare);

        addSpace(root, 25);

        TextView usersTitle = text(
                "👥 USERS / DRIVERS",
                20,
                Color.BLACK
        );

        root.addView(usersTitle);

        usersContainer = new LinearLayout(this);

        usersContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        root.addView(usersContainer);

        addSpace(root, 25);

        TextView ridesTitle = text(
                "🚕 RECENT RIDES",
                20,
                Color.BLACK
        );

        root.addView(ridesTitle);

        ridesContainer = new LinearLayout(this);

        ridesContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        root.addView(ridesContainer);

        addSpace(root, 25);

        Button refresh = button(
                "🔄 REFRESH ADMIN DATA"
        );

        refresh.setOnClickListener(
                v -> {
                    loadFareSettings();
                    loadStatistics();
                    loadUsers();
                    loadRides();
                }
        );

        root.addView(refresh);

        Button logout = button(
                "🚪 LOGOUT"
        );

        logout.setOnClickListener(
                v -> logout()
        );

        root.addView(logout);

        setContentView(scrollView);
    }

    private void loadFareSettings() {

        db.collection("settings")
                .document("fare")
                .get()
                .addOnSuccessListener(
                        document -> {

                            if (!document.exists()) {

                                baseFareInput.setText("50");
                                perKmInput.setText("10");
                                minimumFareInput.setText("50");
                                maximumFareInput.setText("500");

                                return;
                            }

                            Double baseFare =
                                    document.getDouble("baseFare");

                            Double perKm =
                                    document.getDouble("perKm");

                            Double minimumFare =
                                    document.getDouble("minimumFare");

                            Double maximumFare =
                                    document.getDouble("maximumFare");

                            if (baseFare != null) {
                                baseFareInput.setText(
                                        String.valueOf(baseFare)
                                );
                            }

                            if (perKm != null) {
                                perKmInput.setText(
                                        String.valueOf(perKm)
                                );
                            }

                            if (minimumFare != null) {
                                minimumFareInput.setText(
                                        String.valueOf(minimumFare)
                                );
                            }

                            if (maximumFare != null) {
                                maximumFareInput.setText(
                                        String.valueOf(maximumFare)
                                );
                            }
                        }
                )
                .addOnFailureListener(
                        e -> showMessage(
                                "Could not load fare settings: " +
                                e.getMessage()
                        )
                );
    }

    private void saveFareSettings() {

        String baseText =
                baseFareInput
                        .getText()
                        .toString()
                        .trim();

        String perKmText =
                perKmInput
                        .getText()
                        .toString()
                        .trim();

        String minimumText =
                minimumFareInput
                        .getText()
                        .toString()
                        .trim();

        String maximumText =
                maximumFareInput
                        .getText()
                        .toString()
                        .trim();

        if (baseText.isEmpty() ||
                perKmText.isEmpty() ||
                minimumText.isEmpty() ||
                maximumText.isEmpty()) {

            showMessage(
                    "Please complete all fare fields."
            );

            return;
        }

        double baseFare;
        double perKm;
        double minimumFare;
        double maximumFare;

        try {

            baseFare =
                    Double.parseDouble(baseText);

            perKm =
                    Double.parseDouble(perKmText);

            minimumFare =
                    Double.parseDouble(minimumText);

            maximumFare =
                    Double.parseDouble(maximumText);

        } catch (NumberFormatException e) {

            showMessage(
                    "Enter valid numbers for the fare."
            );

            return;
        }

        if (baseFare < 0 ||
                perKm < 0 ||
                minimumFare < 0 ||
                maximumFare < 0) {

            showMessage(
                    "Fare values cannot be negative."
            );

            return;
        }

        if (maximumFare < minimumFare) {

            showMessage(
                    "Maximum fare must be greater than minimum fare."
            );

            return;
        }

        Map<String, Object> fare =
                new HashMap<>();

        fare.put(
                "baseFare",
                baseFare
        );

        fare.put(
                "perKm",
                perKm
        );

        fare.put(
                "minimumFare",
                minimumFare
        );

        fare.put(
                "maximumFare",
                maximumFare
        );

        fare.put(
                "updatedAt",
                com.google.firebase.firestore.FieldValue
                        .serverTimestamp()
        );

        db.collection("settings")
                .document("fare")
                .set(fare)
                .addOnSuccessListener(
                        unused -> showMessage(
                                "🟢 Fare settings saved."
                        )
                )
                .addOnFailureListener(
                        e -> showMessage(
                                "Fare save failed: " +
                                e.getMessage()
                        )
                );
    }

    private void loadStatistics() {

        db.collection("users")
                .get()
                .addOnSuccessListener(
                        snapshot -> {

                            int passengers = 0;
                            int drivers = 0;
                            int approvedDrivers = 0;
                            int pendingDrivers = 0;
                            int activeUsers = 0;

                            for (DocumentSnapshot document :
                                    snapshot.getDocuments()) {

                                String role =
                                        document.getString("role");

                                Boolean approved =
                                        document.getBoolean("approved");

                                Boolean active =
                                        document.getBoolean("active");

                                if ("PASSENGER".equals(role)) {
                                    passengers++;
                                }

                                if ("DRIVER".equals(role)) {

                                    drivers++;

                                    if (Boolean.TRUE.equals(approved)) {
                                        approvedDrivers++;
                                    } else {
                                        pendingDrivers++;
                                    }
                                }

                                if (active == null ||
                                        Boolean.TRUE.equals(active)) {

                                    activeUsers++;
                                }
                            }

                            final int finalPassengers =
                                    passengers;

                            final int finalDrivers =
                                    drivers;

                            final int finalApprovedDrivers =
                                    approvedDrivers;

                            final int finalPendingDrivers =
                                    pendingDrivers;

                            final int finalActiveUsers =
                                    activeUsers;

                            db.collection("rides")
                                    .get()
                                    .addOnSuccessListener(
                                            rideSnapshot -> {

                                                int totalRides =
                                                        rideSnapshot.size();

                                                statisticsText.setText(
                                                        "Passengers: " +
                                                        finalPassengers +
                                                        "\nDrivers: " +
                                                        finalDrivers +
                                                        "\nApproved drivers: " +
                                                        finalApprovedDrivers +
                                                        "\nPending drivers: " +
                                                        finalPendingDrivers +
                                                        "\nActive users: " +
                                                        finalActiveUsers +
                                                        "\nTotal rides: " +
                                                        totalRides
                                                );
                                            }
                                    )
                                    .addOnFailureListener(
                                            e -> statisticsText.setText(
                                                    "Passengers: " +
                                                    finalPassengers +
                                                    "\nDrivers: " +
                                                    finalDrivers +
                                                    "\nApproved drivers: " +
                                                    finalApprovedDrivers +
                                                    "\nPending drivers: " +
                                                    finalPendingDrivers +
                                                    "\nActive users: " +
                                                    finalActiveUsers +
                                                    "\nTotal rides: unavailable"
                                            )
                                    );
                        }
                )
                .addOnFailureListener(
                        e -> statisticsText.setText(
                                "Unable to load statistics:\n" +
                                e.getMessage()
                        )
                );
    }

    private void loadUsers() {

        usersContainer.removeAllViews();

        TextView loading = text(
                "Loading users...",
                15,
                Color.DKGRAY
        );

        usersContainer.addView(loading);

        db.collection("users")
                .get()
                .addOnSuccessListener(
                        snapshot -> {

                            usersContainer.removeAllViews();

                            if (snapshot.isEmpty()) {

                                usersContainer.addView(
                                        text(
                                                "No users found.",
                                                15,
                                                Color.DKGRAY
                                        )
                                );

                                return;
                            }

                            for (DocumentSnapshot document :
                                    snapshot.getDocuments()) {

                                addUserCard(document);
                            }
                        }
                )
                .addOnFailureListener(
                        e -> {

                            usersContainer.removeAllViews();

                            usersContainer.addView(
                                    text(
                                            "Unable to load users: " +
                                            e.getMessage(),
                                            15,
                                            Color.RED
                                    )
                            );
                        }
                );
    }

    private void addUserCard(
            DocumentSnapshot document) {

        String uid =
                document.getId();

        String email =
                document.getString("email");

        String role =
                document.getString("role");

        String driverName =
                document.getString("driverName");

        String phone =
                document.getString("phone");

        Boolean approved =
                document.getBoolean("approved");

        Boolean canAcceptRides =
                document.getBoolean("canAcceptRides");

        String driverStatus =
                document.getString("driverStatus");

        String displayName =
                driverName;

        if (displayName == null ||
                displayName.trim().isEmpty()) {

            displayName =
                    document.getString("name");
        }

        if (displayName == null ||
                displayName.trim().isEmpty()) {

            displayName = "User";
        }

        StringBuilder details =
                new StringBuilder();

        details.append(displayName);

        if (email != null &&
                !email.isEmpty()) {

            details.append("\n")
                    .append(email);
        }

        if (phone != null &&
                !phone.isEmpty()) {

            details.append("\n")
                    .append(phone);
        }

        details.append("\nRole: ")
                .append(
                        role == null
                                ? "UNKNOWN"
                                : role
                );

        if ("DRIVER".equals(role)) {

            details.append("\nApproved: ")
                    .append(
                            Boolean.TRUE.equals(approved)
                    );

            details.append("\nDriver status: ")
                    .append(
                            driverStatus == null
                                    ? "UNKNOWN"
                                    : driverStatus
                    );

            details.append("\nCan accept rides: ")
                    .append(
                            Boolean.TRUE.equals(
                                    canAcceptRides
                            )
                    );
        }

        TextView userText =
                text(
                        details.toString(),
                        15,
                        Color.DKGRAY
                );

        usersContainer.addView(userText);

        if ("DRIVER".equals(role)) {

            Button approvalButton =
                    button(
                            Boolean.TRUE.equals(approved)
                                    ? "❌ REVOKE DRIVER APPROVAL"
                                    : "✅ APPROVE DRIVER"
                    );

            approvalButton.setOnClickListener(
                    v -> {

                        boolean newApproved =
                                !Boolean.TRUE.equals(
                                        approved
                                );

                        updateDriverApproval(
                                uid,
                                newApproved
                        );
                    }
            );

            usersContainer.addView(
                    approvalButton
            );
        }

        addSpace(
                usersContainer,
                10
        );
    }

    private void updateDriverApproval(
            String uid,
            boolean approved) {

        Map<String, Object> update =
                new HashMap<>();

        update.put(
                "approved",
                approved
        );

        if (approved) {

            update.put(
                    "driverStatus",
                    "APPROVED"
            );

            update.put(
                    "canAcceptRides",
                    true
            );

        } else {

            update.put(
                    "driverStatus",
                    "PENDING_APPROVAL"
            );

            update.put(
                    "canAcceptRides",
                    false
            );
        }

        db.collection("users")
                .document(uid)
                .update(update)
                .addOnSuccessListener(
                        unused -> {

                            showMessage(
                                    approved
                                            ? "🟢 Driver approved."
                                            : "Driver approval revoked."
                            );

                            loadStatistics();
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

    private void loadRides() {

        ridesContainer.removeAllViews();

        ridesContainer.addView(
                text(
                        "Loading rides...",
                        15,
                        Color.DKGRAY
                )
        );

        db.collection("rides")
                .get()
                .addOnSuccessListener(
                        snapshot -> {

                            ridesContainer.removeAllViews();

                            if (snapshot.isEmpty()) {

                                ridesContainer.addView(
                                        text(
                                                "No rides found.",
                                                15,
                                                Color.DKGRAY
                                        )
                                );

                                return;
                            }

                            for (DocumentSnapshot document :
                                    snapshot.getDocuments()) {

                                String rideId =
                                        document.getId();

                                String status =
                                        document.getString("status");

                                String paymentMethod =
                                        document.getString(
                                                "paymentMethod"
                                        );

                                String pickup =
                                        document.getString("pickup");

                                String destination =
                                        document.getString(
                                                "destination"
                                        );

                                Double fare =
                                        document.getDouble("fare");

                                StringBuilder ride =
                                        new StringBuilder();

                                ride.append(
                                        "Ride ID: "
                                ).append(
                                        rideId
                                );

                                ride.append(
                                        "\nStatus: "
                                ).append(
                                        status == null
                                                ? "UNKNOWN"
                                                : status
                                );

                                if (pickup != null) {

                                    ride.append(
                                            "\nPickup: "
                                    ).append(
                                            pickup
                                    );
                                }

                                if (destination != null) {

                                    ride.append(
                                            "\nDestination: "
                                    ).append(
                                            destination
                                    );
                                }

                                if (fare != null) {

                                    ride.append(
                                            "\nFare: ₱"
                                    ).append(
                                            String.format(
                                                    "%.2f",
                                                    fare
                                            )
                                    );
                                }

                                if (paymentMethod != null) {

                                    ride.append(
                                            "\nPayment: "
                                    ).append(
                                            paymentMethod
                                    );
                                }

                                TextView rideText =
                                        text(
                                                ride.toString(),
                                                15,
                                                Color.DKGRAY
                                        );

                                ridesContainer.addView(
                                        rideText
                                );

                                addSpace(
                                        ridesContainer,
                                        10
                                );
                            }
                        }
                )
                .addOnFailureListener(
                        e -> {

                            ridesContainer.removeAllViews();

                            ridesContainer.addView(
                                    text(
                                            "Unable to load rides: " +
                                            e.getMessage(),
                                            15,
                                            Color.RED
                                    )
                            );
                        }
                );
    }

    private EditText input(
            String hint) {

        EditText input =
                new EditText(this);

        input.setHint(hint);
        input.setTextSize(16);
        input.setSingleLine(true);
        input.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER |
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        );

        input.setPadding(
                15,
                12,
                15,
                12
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(
                0,
                6,
                0,
                6
        );

        input.setLayoutParams(params);

        return input;
    }

    private Button button(
            String label) {

        Button button =
                new Button(this);

        button.setText(label);
        button.setTextSize(15);
        button.setAllCaps(false);

        return button;
    }

    private TextView text(
            String value,
            int size,
            int color) {

        TextView view =
                new TextView(this);

        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);

        view.setPadding(
                10,
                10,
                10,
                10
        );

        return view;
    }

    private void addSpace(
            LinearLayout parent,
            int height) {

        TextView space =
                new TextView(this);

        space.setHeight(height);

        parent.addView(space);
    }

    private void showMessage(
            String message) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
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
}
