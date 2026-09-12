
package com.sakyna.app;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;

public class PassengerActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private LinearLayout root;
    private ListenerRegistration rideListener;

    private String rideId = "";

    private final int GREEN = Color.rgb(25, 135, 84);
    private final int ORANGE = Color.rgb(245, 145, 30);
    private final int BLUE = Color.rgb(35, 105, 190);
    private final int RED = Color.rgb(200, 55, 55);
    private final int GRAY = Color.rgb(110, 110, 110);
    private final int DARK = Color.rgb(35, 35, 35);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        loadSavedRide();
        showDashboard();
    }

    private void setupScreen(String title) {

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(30, 35, 30, 30);
        root.setBackgroundColor(Color.rgb(248, 250, 249));

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextSize(27);
        titleView.setTextColor(GREEN);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 0, 0, 25);

        root.addView(titleView);

        setContentView(root);
    }

    private TextView text(String value, float size) {

        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(DARK);
        view.setGravity(Gravity.CENTER);
        view.setPadding(5, 8, 5, 8);

        return view;
    }

    private Button button(String value, int color) {

        Button button = new Button(this);

        button.setText(value);
        button.setTextSize(17);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);

        android.graphics.drawable.GradientDrawable background =
                new android.graphics.drawable.GradientDrawable();

        background.setColor(color);
        background.setCornerRadius(30);

        button.setBackground(background);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        62
                );

        params.setMargins(0, 8, 0, 8);

        root.addView(button, params);

        return button;
    }

    private EditText input(String hint) {

        EditText editText = new EditText(this);

        editText.setHint(hint);
        editText.setTextSize(17);
        editText.setSingleLine(true);
        editText.setPadding(22, 5, 22, 5);

        android.graphics.drawable.GradientDrawable background =
                new android.graphics.drawable.GradientDrawable();

        background.setColor(Color.WHITE);
        background.setCornerRadius(25);
        background.setStroke(2, Color.LTGRAY);

        editText.setBackground(background);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        62
                );

        params.setMargins(0, 6, 0, 6);

        root.addView(editText, params);

        return editText;
    }

    private void showDashboard() {

        setupScreen("Passenger Dashboard");

        String name =
                getSharedPreferences("SakayNa", MODE_PRIVATE)
                        .getString("current_name", "Passenger");

        TextView welcome =
                text("Welcome, " + name + "!", 20);

        welcome.setTextColor(DARK);
        welcome.setTypeface(null, Typeface.BOLD);

        root.addView(welcome);

        Button book = button("🟠  BOOK A RIDE", ORANGE);
        book.setOnClickListener(v -> showBooking());

        Button current = button("🚕  CURRENT RIDE", GREEN);
        current.setOnClickListener(v -> showCurrentRide());

        Button history = button("📋  RIDE HISTORY", BLUE);
        history.setOnClickListener(v -> showHistory());

        Button help = button("🆘  HELP / EMERGENCY", RED);
        help.setOnClickListener(v ->
                toast("Emergency assistance feature will be connected later.")
        );

        Button logout = button("LOGOUT", GRAY);
        logout.setOnClickListener(v -> logout());
    }

    private void showBooking() {

        setupScreen("Book a Ride");

        TextView info =
                text("Enter your pickup and destination.", 17);

        info.setPadding(0, 0, 0, 18);
        root.addView(info);

        EditText pickup = input("Pickup location");

        EditText destination = input("Destination");

        TextView fareText =
                text("Fare: Not calculated", 20);

        fareText.setTextColor(GREEN);
        fareText.setTypeface(null, Typeface.BOLD);

        root.addView(fareText);

        Button calculate =
                button("CALCULATE FARE", BLUE);

        calculate.setOnClickListener(v -> {

            String p = pickup.getText().toString().trim();
            String d = destination.getText().toString().trim();

            if (p.isEmpty() || d.isEmpty()) {
                toast("Enter pickup and destination first.");
                return;
            }

            int difference =
                    Math.abs(p.length() - d.length());

            int fare = 50 + (difference * 2);

            if (fare > 200) {
                fare = 200;
            }

            fareText.setText("Estimated Fare: ₱" + fare);
        });

        Button request =
                button("REQUEST RIDE", ORANGE);

        request.setOnClickListener(v -> {

            String p = pickup.getText().toString().trim();
            String d = destination.getText().toString().trim();

            if (p.isEmpty() || d.isEmpty()) {
                toast("Enter pickup and destination.");
                return;
            }

            requestRide(p, d);
        });

        Button back =
                button("BACK", GRAY);

        back.setOnClickListener(v -> showDashboard());
    }

    private void requestRide(String pickup, String destination) {

        if (auth.getCurrentUser() == null) {
            toast("Please login again.");
            return;
        }

        if (!rideId.isEmpty()) {

            db.collection("rides")
                    .document(rideId)
                    .get()
                    .addOnSuccessListener(document -> {

                        if (document.exists()) {

                            String status =
                                    document.getString("status");

                            if (isActiveStatus(status)) {
                                toast("You already have an active ride.");
                                return;
                            }
                        }

                        createRide(pickup, destination);
                    });

            return;
        }

        createRide(pickup, destination);
    }

    private void createRide(
            String pickup,
            String destination) {

        String uid = auth.getCurrentUser().getUid();

        String name =
                getSharedPreferences("SakayNa", MODE_PRIVATE)
                        .getString("current_name", "Passenger");

        String phone =
                getSharedPreferences("SakayNa", MODE_PRIVATE)
                        .getString("current_phone", "");

        int difference =
                Math.abs(pickup.length() - destination.length());

        int fare = 50 + (difference * 2);

        if (fare > 200) {
            fare = 200;
        }

        Map<String, Object> ride =
                new HashMap<>();

        ride.put("passengerId", uid);
        ride.put("passengerName", name);
        ride.put("passengerPhone", phone);

        ride.put("pickup", pickup);
        ride.put("destination", destination);

        ride.put("fare", fare);
        ride.put("finalFare", 0);

        ride.put("status", "REQUESTED");

        ride.put("driverId", "");
        ride.put("driverName", "");

        ride.put("rating", 0);

        ride.put(
                "createdAt",
                com.google.firebase.firestore.FieldValue.serverTimestamp()
        );

        db.collection("rides")
                .add(ride)
                .addOnSuccessListener(documentReference -> {

                    rideId = documentReference.getId();

                    getSharedPreferences(
                            "SakayNa",
                            MODE_PRIVATE
                    )
                            .edit()
                            .putString("ride_id", rideId)
                            .putString("ride_pickup", pickup)
                            .putString("ride_destination", destination)
                            .putInt("ride_fare", fare)
                            .putString("ride_status", "REQUESTED")
                            .apply();

                    toast("Ride requested successfully!");

                    showCurrentRide();
                })
                .addOnFailureListener(e ->
                        toast(
                                "Could not request ride: "
                                        + e.getMessage()
                        )
                );
    }

    private void showCurrentRide() {

        setupScreen("Current Ride");

        if (rideId.isEmpty()) {

            TextView none =
                    text(
                            "No current ride.",
                            18
                    );

            root.addView(none);

            Button book =
                    button("BOOK A RIDE", ORANGE);

            book.setOnClickListener(v ->
                    showBooking()
            );

            Button back =
                    button("BACK", GRAY);

            back.setOnClickListener(v ->
                    showDashboard()
            );

            return;
        }

        TextView status =
                text("Loading ride...", 18);

        root.addView(status);

        listenToRide(status);

        Button refresh =
                button("REFRESH STATUS", BLUE);

        refresh.setOnClickListener(v ->
                loadCurrentRide(status)
        );

        Button cancel =
                button("CANCEL RIDE", RED);

        cancel.setOnClickListener(v ->
                cancelRide()
        );

        Button back =
                button("BACK", GRAY);

        back.setOnClickListener(v -> {

            removeRideListener();
            showDashboard();
        });

        loadCurrentRide(status);
    }

    private void loadCurrentRide(TextView statusView) {

        if (rideId.isEmpty()) {
            statusView.setText("No current ride.");
            return;
        }

        db.collection("rides")
                .document(rideId)
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {

                        statusView.setText(
                                "Ride no longer exists."
                        );

                        return;
                    }

                    updateRideDisplay(
                            document,
                            statusView
                    );
                })
                .addOnFailureListener(e ->
                        statusView.setText(
                                "Could not load ride."
                        )
                );
    }

    private void listenToRide(TextView statusView) {

        removeRideListener();

        if (rideId.isEmpty()) {
            return;
        }

        rideListener =
                db.collection("rides")
                        .document(rideId)
                        .addSnapshotListener(
                                (snapshot, error) -> {

                                    if (error != null) {
                                        return;
                                    }

                                    if (snapshot == null
                                            || !snapshot.exists()) {
                                        return;
                                    }

                                    updateRideDisplay(
                                            snapshot,
                                            statusView
                                    );
                                }
                        );
    }

    private void updateRideDisplay(
            DocumentSnapshot document,
            TextView statusView) {

        String pickup =
                document.getString("pickup");

        String destination =
                document.getString("destination");

        String status =
                document.getString("status");

        String driver =
                document.getString("driverName");

        Long fare =
                document.getLong("fare");

        Long finalFare =
                document.getLong("finalFare");

        if (pickup == null) {
            pickup = "";
        }

        if (destination == null) {
            destination = "";
        }

        if (status == null) {
            status = "UNKNOWN";
        }

        if (driver == null || driver.isEmpty()) {
            driver = "Waiting for driver";
        }

        long displayFare =
                fare == null ? 0 : fare;

        if (finalFare != null && finalFare > 0) {
            displayFare = finalFare;
        }

        String readableStatus =
                readableStatus(status);

        statusView.setText(
                "PICKUP\n"
                        + pickup
                        + "\n\n"
                        + "DESTINATION\n"
                        + destination
                        + "\n\n"
                        + "FARE\n₱"
                        + displayFare
                        + "\n\n"
                        + "DRIVER\n"
                        + driver
                        + "\n\n"
                        + "STATUS\n"
                        + readableStatus
        );

        statusView.setTextSize(17);
        statusView.setTextColor(DARK);
    }

    private String readableStatus(String status) {

        switch (status) {

            case "REQUESTED":
                return "WAITING FOR DRIVER";

            case "ACCEPTED":
                return "DRIVER ACCEPTED";

            case "DRIVER_ON_THE_WAY":
                return "DRIVER ON THE WAY";

            case "DRIVER_ARRIVED":
                return "DRIVER ARRIVED";

            case "IN_PROGRESS":
                return "TRIP IN PROGRESS";

            case "FINISHED":
                return "TRIP FINISHED";

            case "COMPLETED":
                return "RIDE COMPLETED";

            case "CANCELLED":
                return "RIDE CANCELLED";

            case "DECLINED":
                return "RIDE DECLINED";

            default:
                return status;
        }
    }

    private boolean isActiveStatus(String status) {

        if (status == null) {
            return false;
        }

        return status.equals("REQUESTED")
                || status.equals("ACCEPTED")
                || status.equals("DRIVER_ON_THE_WAY")
                || status.equals("DRIVER_ARRIVED")
                || status.equals("IN_PROGRESS");
    }

    private void cancelRide() {

        if (rideId.isEmpty()) {
            toast("No active ride.");
            return;
        }

        db.collection("rides")
                .document(rideId)
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {
                        toast("Ride not found.");
                        return;
                    }

                    String status =
                            document.getString("status");

                    if (status == null) {
                        return;
                    }

                    if (!status.equals("REQUESTED")) {

                        toast(
                                "This ride can no longer be cancelled here."
                        );

                        return;
                    }

                    Map<String, Object> update =
                            new HashMap<>();

                    update.put("status", "CANCELLED");

                    db.collection("rides")
                            .document(rideId)
                            .update(update)
                            .addOnSuccessListener(unused -> {

                                getSharedPreferences(
                                        "SakayNa",
                                        MODE_PRIVATE
                                )
                                        .edit()
                                        .putString(
                                                "ride_status",
                                                "CANCELLED"
                                        )
                                        .apply();

                                toast("Ride cancelled.");
                            })
                            .addOnFailureListener(e ->
                                    toast(
                                            "Could not cancel ride."
                                    )
                            );
                });
    }

    private void showHistory() {

        setupScreen("Ride History");

        if (auth.getCurrentUser() == null) {

            root.addView(
                    text(
                            "Please login again.",
                            18
                    )
            );

            return;
        }

        TextView history =
                text("Loading ride history...", 17);

        root.addView(history);

        db.collection("rides")
                .whereEqualTo(
                        "passengerId",
                        auth.getCurrentUser().getUid()
                )
                .get()
                .addOnSuccessListener(result -> {

                    if (result.isEmpty()) {

                        history.setText(
                                "No rides yet."
                        );

                        return;
                    }

                    StringBuilder builder =
                            new StringBuilder();

                    for (DocumentSnapshot document :
                            result.getDocuments()) {

                        String pickup =
                                document.getString("pickup");

                        String destination =
                                document.getString("destination");

                        String status =
                                document.getString("status");

                        Long fare =
                                document.getLong("fare");

                        builder.append(
                                "Pickup: "
                        )
                                .append(pickup)
                                .append("\n");

                        builder.append(
                                "Destination: "
                        )
                                .append(destination)
                                .append("\n");

                        builder.append(
                                "Fare: ₱"
                        )
                                .append(
                                        fare == null
                                                ? 0
                                                : fare
                                )
                                .append("\n");

                        builder.append(
                                "Status: "
                        )
                                .append(
                                        readableStatus(status)
                                )
                                .append("\n");

                        builder.append(
                                "--------------------\n"
                        );
                    }

                    history.setText(builder.toString());
                    history.setTextSize(16);
                    history.setGravity(Gravity.START);
                })
                .addOnFailureListener(e ->
                        history.setText(
                                "Could not load history."
                        )
                );

        Button back =
                button("BACK", GRAY);

        back.setOnClickListener(v ->
                showDashboard()
        );
    }

    private void loadSavedRide() {

        rideId =
                getSharedPreferences(
                        "SakayNa",
                        MODE_PRIVATE
                )
                        .getString(
                                "ride_id",
                                ""
                        );
    }

    private void removeRideListener() {

        if (rideListener != null) {
            rideListener.remove();
            rideListener = null;
        }
    }

    private void logout() {

        removeRideListener();

        auth.signOut();

        getSharedPreferences(
                "SakayNa",
                MODE_PRIVATE
        )
                .edit()
                .remove("current_phone")
                .remove("current_name")
                .remove("current_role")
                .remove("ride_id")
                .apply();

        Intent intent =
                new Intent(
                        PassengerActivity.this,
                        MainActivity.class
                );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        finish();
    }

    private void toast(String message) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    @Override
    protected void onDestroy() {

        removeRideListener();

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {

        removeRideListener();
        showDashboard();
    }
}
