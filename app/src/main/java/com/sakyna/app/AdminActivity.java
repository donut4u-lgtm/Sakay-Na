
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AdminActivity extends Activity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private LinearLayout root;

    private TextView statisticsText;
    private TextView usersText;
    private TextView ridesText;

    private EditText baseFareInput;
    private EditText perKmInput;
    private EditText minimumFareInput;
    private EditText maximumFareInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        showDashboard();

        loadFareSettings();
        loadStatistics();
        loadUsers();
        loadRides();
    }

    private void showDashboard() {

        ScrollView scrollView =
                new ScrollView(this);

        root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setPadding(
                24,
                30,
                24,
                30
        );

        root.setBackgroundColor(
                Color.WHITE
        );

        scrollView.addView(root);

        TextView title =
                text(
                        "👨‍💼 SAKAY NA",
                        30,
                        Color.BLACK
                );

        title.setGravity(
                Gravity.CENTER
        );

        root.addView(title);

        TextView heading =
                text(
                        "ADMIN CONTROL CENTER",
                        22,
                        Color.rgb(20, 120, 70)
                );

        heading.setGravity(
                Gravity.CENTER
        );

        root.addView(heading);

        root.addView(
                separator()
        );

        addSectionTitle(
                "💰 FARE SETTINGS"
        );

        TextView fareInfo =
                text(
                        "These values control the fare used by " +
                        "Passenger and Driver screens.",
                        14,
                        Color.DKGRAY
                );

        root.addView(fareInfo);

        baseFareInput =
                input(
                        "Base fare (₱)"
                );

        root.addView(
                baseFareInput
        );

        perKmInput =
                input(
                        "Fare per kilometer (₱)"
                );

        root.addView(
                perKmInput
        );

        minimumFareInput =
                input(
                        "Minimum fare (₱)"
                );

        root.addView(
                minimumFareInput
        );

        maximumFareInput =
                input(
                        "Maximum fare (₱)"
                );

        root.addView(
                maximumFareInput
        );

        Button saveFare =
                button(
                        "💾 SAVE FARE SETTINGS"
                );

        saveFare.setOnClickListener(
                v -> saveFareSettings()
        );

        root.addView(
                saveFare
        );

        root.addView(
                separator()
        );

        addSectionTitle(
                "📊 STATISTICS"
        );

        statisticsText =
                text(
                        "Loading statistics...",
                        16,
                        Color.DKGRAY
                );

        root.addView(
                statisticsText
        );

        root.addView(
                separator()
        );

        addSectionTitle(
                "👥 USERS"
        );

        usersText =
                text(
                        "Loading users...",
                        15,
                        Color.DKGRAY
                );

        root.addView(
                usersText
        );

        root.addView(
                separator()
        );

        addSectionTitle(
                "🚕 RIDES"
        );

        ridesText =
                text(
                        "Loading rides...",
                        15,
                        Color.DKGRAY
                );

        root.addView(
                ridesText
        );

        root.addView(
                separator()
        );

        Button refresh =
                button(
                        "🔄 REFRESH DASHBOARD"
                );

        refresh.setOnClickListener(
                v -> {

                    loadFareSettings();
                    loadStatistics();
                    loadUsers();
                    loadRides();

                    showMessage(
                            "Dashboard refreshed."
                    );
                }
        );

        root.addView(
                refresh
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

        setContentView(
                scrollView
        );
    }

    private void loadFareSettings() {

        db.collection("settings")
                .document("fare")
                .get()
                .addOnSuccessListener(
                        document -> {

                            if (!document.exists()) {

                                baseFareInput.setText(
                                        "50"
                                );

                                perKmInput.setText(
                                        "10"
                                );

                                minimumFareInput.setText(
                                        "50"
                                );

                                maximumFareInput.setText(
                                        "500"
                                );

                                return;
                            }

                            setNumber(
                                    baseFareInput,
                                    document,
                                    "baseFare",
                                    50
                            );

                            setNumber(
                                    perKmInput,
                                    document,
                                    "perKm",
                                    10
                            );

                            setNumber(
                                    minimumFareInput,
                                    document,
                                    "minimumFare",
                                    50
                            );

                            setNumber(
                                    maximumFareInput,
                                    document,
                                    "maximumFare",
                                    500
                            );
                        }
                )
                .addOnFailureListener(
                        e -> showMessage(
                                "Unable to load fare settings."
                        )
                );
    }

    private void saveFareSettings() {

        Double baseFare =
                readNumber(
                        baseFareInput
                );

        Double perKm =
                readNumber(
                        perKmInput
                );

        Double minimumFare =
                readNumber(
                        minimumFareInput
                );

        Double maximumFare =
                readNumber(
                        maximumFareInput
                );

        if (baseFare == null ||
                perKm == null ||
                minimumFare == null ||
                maximumFare == null) {

            showMessage(
                    "Enter valid fare numbers."
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

        if (minimumFare > maximumFare) {

            showMessage(
                    "Minimum fare cannot exceed maximum fare."
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
                FieldValue.serverTimestamp()
        );

        FirebaseAuth currentAuth =
                FirebaseAuth.getInstance();

        if (currentAuth.getCurrentUser() != null) {

            fare.put(
                    "updatedBy",
                    currentAuth
                            .getCurrentUser()
                            .getUid()
            );
        }

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

                            int totalUsers =
                                    snapshot.size();

                            int passengers = 0;
                            int drivers = 0;
                            int approvedDrivers = 0;
                            int pendingDrivers = 0;
                            int activeUsers = 0;

                            for (
                                    DocumentSnapshot doc :
                                    snapshot.getDocuments()
                            ) {

                                String role =
                                        doc.getString(
                                                "role"
                                        );

                                Boolean active =
                                        doc.getBoolean(
                                                "active"
                                        );

                                if (Boolean.TRUE.equals(
                                        active
                                )) {
                                    activeUsers++;
                                }

                                if ("PASSENGER".equals(
                                        role
                                )) {

                                    passengers++;

                                } else if ("DRIVER".equals(
                                        role
                                )) {

                                    drivers++;

                                    Boolean approved =
                                            doc.getBoolean(
                                                    "approved"
                                            );

                                    String status =
                                            doc.getString(
                                                    "driverStatus"
                                            );

                                    if (Boolean.TRUE.equals(
                                            approved
                                    ) &&
                                            "APPROVED".equals(
                                                    status
                                            )) {

                                        approvedDrivers++;

                                    } else {

                                        pendingDrivers++;
                                    }
                                }
                            }

                            db.collection("rides")
                                    .get()
                                    .addOnSuccessListener(
                                            rides -> {

                                                statisticsText
                                                        .setText(
                                                                "Total users: " +
                                                                totalUsers +
                                                                "\n" +
                                                                "Passengers: " +
                                                                passengers +
                                                                "\n" +
                                                                "Drivers: " +
                                                                drivers +
                                                                "\n" +
                                                                "Approved drivers: " +
                                                                approvedDrivers +
                                                                "\n" +
                                                                "Pending drivers: " +
                                                                pendingDrivers +
                                                                "\n" +
                                                                "Active users: " +
                                                                activeUsers +
                                                                "\n" +
                                                                "Total rides: " +
                                                                rides.size()
                                                        );
                                            }
                                    )
                                    .addOnFailureListener(
                                            e ->
                                                    statisticsText
                                                            .setText(
                                                                    "Total users: " +
                                                                    totalUsers +
                                                                    "\n" +
                                                                    "Passengers: " +
                                                                    passengers +
                                                                    "\n" +
                                                                    "Drivers: " +
                                                                    drivers +
                                                                    "\n" +
                                                                    "Approved drivers: " +
                                                                    approvedDrivers +
                                                                    "\n" +
                                                                    "Pending drivers: " +
                                                                    pendingDrivers +
                                                                    "\n" +
                                                                    "Active users: " +
                                                                    activeUsers +
                                                                    "\n" +
                                                                    "Total rides: unavailable"
                                                            )
                                    );
                        }
                )
                .addOnFailureListener(
                        e ->
                                statisticsText.setText(
                                        "Unable to load statistics."
                                )
                );
    }

    private void loadUsers() {

        db.collection("users")
                .get()
                .addOnSuccessListener(
                        snapshot -> {

                            StringBuilder text =
                                    new StringBuilder();

                            text.append(
                                    "Registered users: "
                            );

                            text.append(
                                    snapshot.size()
                            );

                            text.append(
                                    "\n\n"
                            );

                            for (
                                    DocumentSnapshot doc :
                                    snapshot.getDocuments()
                            ) {

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

                                text.append(
                                        "Email: "
                                );

                                text.append(
                                        email == null
                                                ? "-"
                                                : email
                                );

                                text.append(
                                        "\nRole: "
                                );

                                text.append(
                                        role == null
                                                ? "-"
                                                : role
                                );

                                text.append(
                                        "\nActive: "
                                );

                                text.append(
                                        Boolean.TRUE.equals(
                                                active
                                        )
                                                ? "YES"
                                                : "NO"
                                );

                                if ("DRIVER".equals(
                                        role
                                )) {

                                    Boolean approved =
                                            doc.getBoolean(
                                                    "approved"
                                            );

                                    String status =
                                            doc.getString(
                                                    "driverStatus"
                                            );

                                    text.append(
                                            "\nApproved: "
                                    );

                                    text.append(
                                            Boolean.TRUE.equals(
                                                    approved
                                            )
                                                    ? "YES"
                                                    : "NO"
                                    );

                                    text.append(
                                            "\nDriver status: "
                                    );

                                    text.append(
                                            status == null
                                                    ? "-"
                                                    : status
                                    );

                                    text.append(
                                            "\n"
                                    );

                                    Button approvalButton =
                                            button(
                                                    Boolean.TRUE.equals(
                                                            approved
                                                    )
                                                            ? "❌ REMOVE DRIVER APPROVAL"
                                                            : "✅ APPROVE DRIVER"
                                            );

                                    String uid =
                                            doc.getId();

                                    approvalButton
                                            .setOnClickListener(
                                                    v ->
                                                            updateDriverApproval(
                                                                    uid,
                                                                    !Boolean.TRUE.equals(
                                                                            approved
                                                                    )
                                                            )
                                            );

                                    root.addView(
                                            approvalButton
                                    );
                                }

                                text.append(
                                        "\n"
                                );
                            }

                            usersText.setText(
                                    text.toString()
                            );
                        }
                )
                .addOnFailureListener(
                        e ->
                                usersText.setText(
                                        "Unable to load users."
                                )
                );
    }

    private void updateDriverApproval(
            String uid,
            boolean approved) {

        Map<String, Object> updates =
                new HashMap<>();

        if (approved) {

            updates.put(
                    "approved",
                    true
            );

            updates.put(
                    "driverStatus",
                    "APPROVED"
            );

            updates.put(
                    "canAcceptRides",
                    true
            );

        } else {

            updates.put(
                    "approved",
                    false
            );

            updates.put(
                    "driverStatus",
                    "PENDING_APPROVAL"
            );

            updates.put(
                    "canAcceptRides",
                    false
            );
        }

        db.collection("users")
                .document(uid)
                .update(updates)
                .addOnSuccessListener(
                        unused -> {

                            showMessage(
                                    approved
                                            ? "🟢 Driver approved."
                                            : "🟡 Driver approval removed."
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

        db.collection("rides")
                .get()
                .addOnSuccessListener(
                        snapshot -> {

                            StringBuilder text =
                                    new StringBuilder();

                            text.append(
                                    "Total rides: "
                            );

                            text.append(
                                    snapshot.size()
                            );

                            text.append(
                                    "\n\n"
                            );

                            for (
                                    DocumentSnapshot doc :
                                    snapshot.getDocuments()
                            ) {

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

                                String paymentMethod =
                                        doc.getString(
                                                "paymentMethod"
                                        );

                                Object fare =
                                        doc.get(
                                                "fare"
                                        );

                                text.append(
                                        "Ride: "
                                );

                                text.append(
                                        doc.getId()
                                );

                                text.append(
                                        "\nStatus: "
                                );

                                text.append(
                                        status == null
                                                ? "-"
                                                : status
                                );

                                text.append(
                                        "\nPassenger: "
                                );

                                text.append(
                                        passengerId == null
                                                ? "-"
                                                : passengerId
                                );

                                text.append(
                                        "\nDriver: "
                                );

                                text.append(
                                        driverId == null
                                                ? "-"
                                                : driverId
                                );

                                text.append(
                                        "\nFare: ₱"
                                );

                                text.append(
                                        fare == null
                                                ? "-"
                                                : fare
                                );

                                if (paymentMethod != null) {

                                    text.append(
                                            "\nPayment: "
                                    );

                                    text.append(
                                            paymentMethod
                                    );
                                }

                                text.append(
                                        "\n\n"
                                );
                            }

                            ridesText.setText(
                                    text.toString()
                            );
                        }
                )
                .addOnFailureListener(
                        e ->
                                ridesText.setText(
                                        "Unable to load rides."
                                )
                );
    }

    private void logout() {

        auth.signOut();

        android.content.Intent intent =
                new android.content.Intent(
                        this,
                        MainActivity.class
                );

        intent.addFlags(
                android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP |
                android.content.Intent.FLAG_ACTIVITY_NEW_TASK |
                android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        finish();
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

        input.setLayoutParams(
                params
        );

        return input;
    }

    private Button button(
            String label) {

        Button button =
                new Button(this);

        button.setText(
                label
        );

        button.setTextSize(
                15
        );

        button.setAllCaps(
                false
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

        button.setLayoutParams(
                params
        );

        return button;
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

    private void addSectionTitle(
            String title) {

        TextView view =
                text(
                        title,
                        20,
                        Color.BLACK
                );

        view.setPadding(
                5,
                15,
                5,
                10
        );

        root.addView(
                view
        );
    }

    private TextView separator() {

        TextView line =
                new TextView(this);

        line.setText(
                "────────────────────────"
        );

        line.setTextColor(
                Color.LTGRAY
        );

        line.setGravity(
                Gravity.CENTER
        );

        line.setPadding(
                0,
                12,
                0,
                12
        );

        return line;
    }

    private void setNumber(
            EditText input,
            DocumentSnapshot document,
            String field,
            double defaultValue) {

        Object value =
                document.get(field);

        if (value instanceof Number) {

            input.setText(
                    String.valueOf(
                            ((Number) value).doubleValue()
                    )
            );

        } else {

            input.setText(
                    String.valueOf(
                            defaultValue
                    )
            );
        }
    }

    private Double readNumber(
            EditText input) {

        try {

            String value =
                    input.getText()
                            .toString()
                            .trim();

            if (value.isEmpty()) {
                return null;
            }

            return Double.parseDouble(
                    value
            );

        } catch (Exception e) {

            return null;
        }
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
