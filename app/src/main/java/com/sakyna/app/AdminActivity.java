
package com.sakyna.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class AdminActivity extends Activity {

private FirebaseAuth auth;
private FirebaseFirestore db;

private LinearLayout contentContainer;
private TextView statusText;

private final int GREEN = Color.rgb(0, 125, 80);
private final int DARK = Color.rgb(35, 35, 35);
private final int LIGHT_GREEN = Color.rgb(232, 247, 238);
private final int LIGHT_RED = Color.rgb(255, 235, 235);
private final int LIGHT_YELLOW = Color.rgb(255, 248, 220);
private final int LIGHT_BLUE = Color.rgb(235, 245, 255);

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    auth = FirebaseAuth.getInstance();
    db = FirebaseFirestore.getInstance();

    buildScreen();
    loadDashboard();
}

private void buildScreen() {

    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(16, 16, 16, 16);
    root.setBackgroundColor(Color.WHITE);

    TextView title = new TextView(this);
    title.setText("🛺 SAKAY NA ADMIN");
    title.setTextSize(28);
    title.setTextColor(GREEN);
    title.setGravity(Gravity.CENTER);
    title.setPadding(0, 10, 0, 5);
    root.addView(title);

    TextView subtitle = new TextView(this);
    subtitle.setText(
            "Admin Control Center\n" +
            "Drivers • Passengers • Ride Transactions"
    );
    subtitle.setTextSize(16);
    subtitle.setTextColor(DARK);
    subtitle.setGravity(Gravity.CENTER);
    subtitle.setPadding(0, 0, 0, 12);
    root.addView(subtitle);

    statusText = new TextView(this);
    statusText.setText("Loading dashboard...");
    statusText.setTextSize(16);
    statusText.setTextColor(DARK);
    statusText.setGravity(Gravity.CENTER);
    statusText.setPadding(0, 8, 0, 12);
    root.addView(statusText);

    Button refresh = new Button(this);
    refresh.setText("🔄 REFRESH DASHBOARD");
    refresh.setTextSize(16);
    refresh.setOnClickListener(v -> loadDashboard());
    root.addView(refresh);

    ScrollView scrollView = new ScrollView(this);

    contentContainer = new LinearLayout(this);
    contentContainer.setOrientation(LinearLayout.VERTICAL);
    contentContainer.setPadding(0, 10, 0, 20);

    scrollView.addView(contentContainer);

    root.addView(
            scrollView,
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

private void loadDashboard() {

    contentContainer.removeAllViews();

    statusText.setText(
            "Loading users and ride transactions..."
    );

    loadUsers();
}

private void loadUsers() {

    db.collection("users")
            .get()
            .addOnSuccessListener(snapshot -> {

                int passengers = 0;
                int drivers = 0;
                int pendingDrivers = 0;
                int approvedDrivers = 0;
                int rejectedDrivers = 0;

                for (DocumentSnapshot user :
                        snapshot.getDocuments()) {

                    String role =
                            user.getString("role");

                    if ("PASSENGER".equalsIgnoreCase(role)) {

                        passengers++;

                    } else if ("DRIVER".equalsIgnoreCase(role)) {

                        drivers++;

                        String approvalStatus =
                                user.getString(
                                        "approvalStatus"
                                );

                        if (
                                approvalStatus == null
                                ||
                                approvalStatus.trim().isEmpty()
                        ) {

                            Boolean approved =
                                    user.getBoolean(
                                            "approved"
                                    );

                            if (
                                    approved != null
                                    &&
                                    approved
                            ) {

                                approvalStatus =
                                        "APPROVED";

                            } else {

                                approvalStatus =
                                        "PENDING_APPROVAL";
                            }
                        }

                        if (
                                "APPROVED".equalsIgnoreCase(
                                        approvalStatus
                                )
                        ) {

                            approvedDrivers++;

                        } else if (
                                "REJECTED".equalsIgnoreCase(
                                        approvalStatus
                                )
                        ) {

                            rejectedDrivers++;

                        } else {

                            pendingDrivers++;
                        }
                    }
                }

                addSectionTitle(
                        "👥 USER OVERVIEW"
                );

                addInfoCard(
                        "🧍 PASSENGERS",
                        String.valueOf(passengers),
                        LIGHT_BLUE
                );

                addInfoCard(
                        "🚕 DRIVERS",
                        String.valueOf(drivers),
                        LIGHT_GREEN
                );

                addInfoCard(
                        "⏳ PENDING DRIVER APPROVAL",
                        String.valueOf(pendingDrivers),
                        LIGHT_YELLOW
                );

                addInfoCard(
                        "✅ APPROVED DRIVERS",
                        String.valueOf(approvedDrivers),
                        LIGHT_GREEN
                );

                addInfoCard(
                        "❌ REJECTED DRIVERS",
                        String.valueOf(rejectedDrivers),
                        LIGHT_RED
                );

                addSectionTitle(
                        "🚕 DRIVER APPROVAL"
                );

                loadDrivers();

            })
            .addOnFailureListener(e -> {

                statusText.setText(
                        "Unable to load users:\n"
                                + safeMessage(e)
                );

                loadDrivers();
            });
}

private void loadDrivers() {

    db.collection("users")
            .whereEqualTo(
                    "role",
                    "DRIVER"
            )
            .get()
            .addOnSuccessListener(snapshot -> {

                if (snapshot.isEmpty()) {

                    addInfoCard(
                            "🚕 DRIVERS",
                            "No driver accounts found.",
                            LIGHT_YELLOW
                    );

                } else {

                    for (
                            DocumentSnapshot driver :
                            snapshot.getDocuments()
                    ) {

                        addDriverCard(
                                driver
                        );
                    }
                }

                addSectionTitle(
                        "🛺 RIDE TRANSACTIONS"
                );

                loadRides();

            })
            .addOnFailureListener(e -> {

                addInfoCard(
                        "🚕 DRIVER ERROR",
                        safeMessage(e),
                        LIGHT_RED
                );

                addSectionTitle(
                        "🛺 RIDE TRANSACTIONS"
                );

                loadRides();
            });
}

private void addDriverCard(
        DocumentSnapshot d
) {

    String phone =
            d.getString("phone");

    if (
            phone == null
            ||
            phone.trim().isEmpty()
    ) {

        phone = "Phone not available";
    }

    String name =
            firstNonEmpty(
                    d.getString("name"),
                    d.getString("driverName")
            );

    if (
            name == null
            ||
            name.trim().isEmpty()
    ) {

        name = "Driver";
    }

    String approvalStatus =
            d.getString("approvalStatus");

    if (
            approvalStatus == null
            ||
            approvalStatus.trim().isEmpty()
    ) {

        Boolean approved =
                d.getBoolean("approved");

        approvalStatus =
                approved != null && approved
                        ? "APPROVED"
                        : "PENDING_APPROVAL";
    }

    Boolean onlineValue =
            d.getBoolean("online");

    boolean online =
            onlineValue != null
                    && onlineValue;

    LinearLayout card =
            createCard();

    if (
            "APPROVED".equalsIgnoreCase(
                    approvalStatus
            )
    ) {

        card.setBackgroundColor(
                LIGHT_GREEN
        );

    } else if (
            "REJECTED".equalsIgnoreCase(
                    approvalStatus
            )
    ) {

        card.setBackgroundColor(
                LIGHT_RED
        );

    } else {

        card.setBackgroundColor(
                LIGHT_YELLOW
        );
    }

    addCardText(
            card,
            "🚕 " + name,
            20,
            GREEN
    );

    addCardText(
            card,
            "📱 Phone: " + phone,
            16,
            DARK
    );

    addCardText(
            card,
            "Approval: " + approvalStatus,
            16,
            DARK
    );

    addCardText(
            card,
            "Online: " +
                    (online ? "🟢 YES" : "🔴 NO"),
            16,
            DARK
    );

    Button actionButton =
            new Button(this);

    if (
            "APPROVED".equalsIgnoreCase(
                    approvalStatus
            )
    ) {

        actionButton.setText(
                "❌ REJECT DRIVER"
        );

        actionButton.setOnClickListener(
                v -> rejectDriver(d.getId())
        );

    } else {

        actionButton.setText(
                "✅ APPROVE DRIVER"
        );

        actionButton.setOnClickListener(
                v -> approveDriver(d.getId())
        );
    }

    card.addView(actionButton);

    if (
            "APPROVED".equalsIgnoreCase(
                    approvalStatus
            )
    ) {

        Button rejectButton =
                new Button(this);

        rejectButton.setText(
                "❌ REJECT DRIVER"
        );

        rejectButton.setOnClickListener(
                v -> rejectDriver(d.getId())
        );

        card.addView(rejectButton);
    }
}

private void loadRides() {

    db.collection("rides")
            .get()
            .addOnSuccessListener(snapshot -> {

                int total = snapshot.size();

                int requested = 0;
                int accepted = 0;
                int onTheWay = 0;
                int arrived = 0;
                int inProgress = 0;
                int completed = 0;
                int cancelled = 0;

                if (snapshot.isEmpty()) {

                    addInfoCard(
                            "🛺 RIDES",
                            "No ride transactions yet.",
                            LIGHT_YELLOW
                    );

                } else {

                    for (
                            DocumentSnapshot ride :
                            snapshot.getDocuments()
                    ) {

                        String status =
                                ride.getString(
                                        "status"
                                );

                        if (status == null) {
                            status = "UNKNOWN";
                        }

                        if (
                                "REQUESTED".equalsIgnoreCase(
                                        status
                                )
                        ) {

                            requested++;

                        } else if (
                                "ACCEPTED".equalsIgnoreCase(
                                        status
                                )
                        ) {

                            accepted++;

                        } else if (
                                "DRIVER_ON_THE_WAY".equalsIgnoreCase(
                                        status
                                )
                        ) {

                            onTheWay++;

                        } else if (
                                "DRIVER_ARRIVED".equalsIgnoreCase(
                                        status
                                )
                        ) {

                            arrived++;

                        } else if (
                                "IN_PROGRESS".equalsIgnoreCase(
                                        status
                                )
                        ) {

                            inProgress++;

                        } else if (
                                "COMPLETED".equalsIgnoreCase(
                                        status
                                )
                        ) {

                            completed++;

                        } else if (
                                "CANCELLED".equalsIgnoreCase(
                                        status
                                )
                        ) {

                            cancelled++;
                        }

                        addRideCard(ride);
                    }
                }

                statusText.setText(
                        "Dashboard loaded • " +
                        total +
                        " ride transaction(s)"
                );

                addInfoCard(
                        "📊 TOTAL RIDES",
                        String.valueOf(total),
                        LIGHT_BLUE
                );

                addInfoCard(
                        "🔔 REQUESTED",
                        String.valueOf(requested),
                        LIGHT_YELLOW
                );

                addInfoCard(
                        "✅ ACCEPTED",
                        String.valueOf(accepted),
                        LIGHT_GREEN
                );

                addInfoCard(
                        "🚗 DRIVER ON THE WAY",
                        String.valueOf(onTheWay),
                        LIGHT_BLUE
                );

                addInfoCard(
                        "📍 DRIVER ARRIVED",
                        String.valueOf(arrived),
                        LIGHT_GREEN
                );

                addInfoCard(
                        "🛺 IN PROGRESS",
                        String.valueOf(inProgress),
                        LIGHT_BLUE
                );

                addInfoCard(
                        "🏁 COMPLETED",
                        String.valueOf(completed),
                        LIGHT_GREEN
                );

                addInfoCard(
                        "❌ CANCELLED",
                        String.valueOf(cancelled),
                        LIGHT_RED
                );

            })
            .addOnFailureListener(e ->
                    statusText.setText(
                            "Unable to load rides:\n"
                                    + safeMessage(e)
                    )
            );
}

private void addRideCard(
        DocumentSnapshot ride
) {

    String pickup =
            firstNonEmpty(
                    ride.getString("pickupName"),
                    ride.getString("pickup")
            );

    if (!hasText(pickup)) {
        pickup = "Not provided";
    }

    String destination =
            firstNonEmpty(
                    ride.getString(
                            "destinationName"
                    ),
                    ride.getString(
                            "destination"
                    )
            );

    if (!hasText(destination)) {
        destination = "Not provided";
    }

    String status =
            ride.getString("status");

    if (!hasText(status)) {
        status = "UNKNOWN";
    }

    String payment =
            ride.getString(
                    "paymentMethod"
            );

    if (!hasText(payment)) {
        payment = "Not provided";
    }

    double fare =
            readNumber(
                    ride,
                    "fare"
            );

    String passengerId =
            ride.getString(
                    "passengerId"
            );

    String driverId =
            ride.getString(
                    "driverId"
            );

    LinearLayout card =
            createCard();

    card.setBackgroundColor(
            LIGHT_BLUE
    );

    addCardText(
            card,
            "🛺 RIDE #" +
                    ride.getId(),
            18,
            GREEN
    );

    addCardText(
            card,
            "📍 Pickup:\n" +
                    pickup,
            15,
            DARK
    );

    addCardText(
            card,
            "🎯 Destination:\n" +
                    destination,
            15,
            DARK
    );

    addCardText(
            card,
            "💰 Fare: ₱" +
                    String.format(
                            Locale.US,
                            "%.0f",
                            fare
                    ),
            16,
            DARK
    );

    addCardText(
            card,
            "💳 Payment: " +
                    payment,
            16,
            DARK
    );

    addCardText(
            card,
            "🚦 Status: " +
                    status,
            16,
            DARK
    );

    addCardText(
            card,
            "🧍 Passenger: " +
                    safeId(passengerId),
            13,
            DARK
    );

    addCardText(
            card,
            "🚕 Driver: " +
                    safeId(driverId),
            13,
            DARK
    );
}

private void approveDriver(
        String driverId
) {

    db.collection("users")
            .document(driverId)
            .update(
                    "approved",
                    true,
                    "approvalStatus",
                    "APPROVED",
                    "driverStatus",
                    "APPROVED",
                    "canAcceptRides",
                    true,
                    "online",
                    false,
                    "approvedAt",
                    System.currentTimeMillis()
            )
            .addOnSuccessListener(v -> {

                Toast.makeText(
                        this,
                        "Driver approved successfully.",
                        Toast.LENGTH_LONG
                ).show();

                loadDashboard();
            })
            .addOnFailureListener(e ->
                    Toast.makeText(
                            this,
                            "Approval failed:\n" +
                                    safeMessage(e),
                            Toast.LENGTH_LONG
                    ).show()
            );
}

private void rejectDriver(
        String driverId
) {

    db.collection("users")
            .document(driverId)
            .update(
                    "approved",
                    false,
                    "approvalStatus",
                    "REJECTED",
                    "driverStatus",
                    "REJECTED",
                    "canAcceptRides",
                    false,
                    "online",
                    false,
                    "rejectedAt",
                    System.currentTimeMillis()
            )
            .addOnSuccessListener(v -> {

                Toast.makeText(
                        this,
                        "Driver rejected.",
                        Toast.LENGTH_LONG
                ).show();

                loadDashboard();
            })
            .addOnFailureListener(e ->
                    Toast.makeText(
                            this,
                            "Rejection failed:\n" +
                                    safeMessage(e),
                            Toast.LENGTH_LONG
                    ).show()
            );
}

private LinearLayout createCard() {

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

    LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );

    params.setMargins(
            0,
            0,
            0,
            14
    );

    contentContainer.addView(
            card,
            params
    );

    return card;
}

private void addSectionTitle(
        String title
) {

    TextView text =
            new TextView(this);

    text.setText(title);
    text.setTextSize(21);
    text.setTypeface(
            null,
            android.graphics.Typeface.BOLD
    );
    text.setTextColor(GREEN);
    text.setPadding(
            4,
            18,
            4,
            10
    );

    contentContainer.addView(text);
}

private void addInfoCard(
        String title,
        String value,
        int background
) {

    LinearLayout card =
            createCard();

    card.setBackgroundColor(
            background
    );

    TextView titleText =
            new TextView(this);

    titleText.setText(title);
    titleText.setTextSize(17);
    titleText.setTypeface(
            null,
            android.graphics.Typeface.BOLD
    );
    titleText.setTextColor(DARK);

    card.addView(titleText);

    TextView valueText =
            new TextView(this);

    valueText.setText(value);
    valueText.setTextSize(22);
    valueText.setTextColor(GREEN);
    valueText.setPadding(
            0,
            5,
            0,
            0
    );

    card.addView(valueText);
}

private void addCardText(
        LinearLayout card,
        String value,
        float size,
        int color
) {

    TextView text =
            new TextView(this);

    text.setText(value);
    text.setTextSize(size);
    text.setTextColor(color);
    text.setPadding(
            0,
            5,
            0,
            5
    );

    card.addView(text);
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

            String text =
                    ((String) value).trim();

            if (!text.isEmpty()) {

                return Double.parseDouble(
                        text
                );
            }
        }

    } catch (Exception ignored) {
    }

    return 0;
}

private String firstNonEmpty(
        String first,
        String second
) {

    if (hasText(first)) {
        return first;
    }

    if (hasText(second)) {
        return second;
    }

    return "";
}

private boolean hasText(
        String value
) {

    return value != null
            &&
            !value.trim().isEmpty();
}

private String safeId(
        String value
) {

    if (!hasText(value)) {
        return "Not assigned";
    }

    return value;
}

private String safeMessage(
        Exception e
) {

    if (
            e == null
            ||
            e.getMessage() == null
            ||
            e.getMessage()
                    .trim()
                    .isEmpty()
    ) {

        return "Unknown Firebase error.";
    }

    return e.getMessage();
}

private void logout() {

    auth.signOut();

    Intent intent =
            new Intent(
                    this,
                    MainActivity.class
            );

    intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK
                    |
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
    );

    startActivity(intent);
    finish();
}

}
