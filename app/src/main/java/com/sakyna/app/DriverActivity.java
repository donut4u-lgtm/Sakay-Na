
package com.sakyna.app;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;

public class DriverActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private LinearLayout root;

    private LocationManager locationManager;

    private double driverLatitude = 0.0;
    private double driverLongitude = 0.0;

    private String currentRideId = "";

    private ListenerRegistration rideListener;

    private TextView gpsText;
    private TextView rideText;

    private final int LOCATION_PERMISSION_REQUEST = 3001;

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

        SharedPreferences prefs =
                getSharedPreferences("SakayNa", MODE_PRIVATE);

        currentRideId = prefs.getString("driver_ride_id", "");

        setupRoot();
        showDashboard();
        startDriverLocation();
    }

    private void setupRoot() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(30, 30, 30, 30);
        root.setBackgroundColor(Color.WHITE);

        setContentView(root);
    }

    private void clearRoot() {
        root.removeAllViews();
    }

    private void add(View view) {
        root.addView(
                view,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );
    }

    private void addSpace(int height) {
        TextView space = new TextView(this);
        space.setHeight(height);
        add(space);
    }

    private TextView title(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(25);
        t.setTextColor(DARK);
        t.setGravity(Gravity.CENTER);
        t.setPadding(10, 20, 10, 20);
        return t;
    }

    private TextView label(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(17);
        t.setTextColor(DARK);
        t.setPadding(5, 10, 5, 10);
        return t;
    }

    private Button button(String text, int color) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(16);
        b.setTextColor(Color.WHITE);
        b.setBackgroundColor(color);
        return b;
    }

    private void showDashboard() {

        clearRoot();

        add(title("Sakay Na - Driver"));

        add(label("Driver Dashboard"));

        addSpace(10);

        Button gps = button("📍 My GPS Location", BLUE);
        gps.setOnClickListener(v -> showGPS());
        add(gps);

        addSpace(10);

        Button rides = button("🚕 Ride Requests", ORANGE);
        rides.setOnClickListener(v -> showRideRequests());
        add(rides);

        addSpace(10);

        Button current = button("🚗 Current Ride", GREEN);
        current.setOnClickListener(v -> showCurrentRide());
        add(current);

        addSpace(10);

        Button history = button("📋 Ride History", BLUE);
        history.setOnClickListener(v -> showHistory());
        add(history);

        addSpace(10);

        Button logout = button("Logout", RED);
        logout.setOnClickListener(v -> logout());
        add(logout);

        if (!currentRideId.isEmpty()) {
            listenToRide(currentRideId);
        }
    }

    private void showGPS() {

        clearRoot();

        add(title("Driver GPS"));

        gpsText = label(
                "Latitude: " + driverLatitude +
                        "\nLongitude: " + driverLongitude
        );

        add(gpsText);

        addSpace(10);

        Button refresh = button("Refresh GPS", BLUE);
        refresh.setOnClickListener(v -> updateGpsText());
        add(refresh);

        addSpace(10);

        Button back = button("Back", GRAY);
        back.setOnClickListener(v -> showDashboard());
        add(back);
    }

    private void updateGpsText() {

        if (gpsText != null) {
            gpsText.setText(
                    "Latitude: " + driverLatitude +
                            "\nLongitude: " + driverLongitude
            );
        }

        uploadDriverLocation();
    }

    private void showRideRequests() {

        clearRoot();

        add(title("Ride Requests"));

        TextView info = label("Searching for REQUESTED rides...");
        add(info);

        db.collection("rides")
                .whereEqualTo("status", "REQUESTED")
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    clearRoot();

                    add(title("Available Rides"));

                    if (querySnapshot.isEmpty()) {

                        add(label("No ride requests available."));

                    } else {

                        for (DocumentSnapshot doc :
                                querySnapshot.getDocuments()) {

                            String rideId = doc.getId();

                            String passenger =
                                    safe(doc.getString("passengerName"));

                            String pickup =
                                    safe(doc.getString("pickup"));

                            String destination =
                                    safe(doc.getString("destination"));

                            Double distance =
                                    doc.getDouble("distanceKm");

                            Long fare =
                                    doc.getLong("fare");

                            String text =
                                    "Passenger: " + passenger +
                                            "\nPickup: " + pickup +
                                            "\nDestination: " + destination +
                                            "\nDistance: " +
                                            (distance == null ? "0" : distance)
                                            + " km" +
                                            "\nFare: ₱" +
                                            (fare == null ? "0" : fare);

                            add(label(text));

                            Button accept =
                                    button("ACCEPT RIDE", GREEN);

                            accept.setOnClickListener(
                                    v -> acceptRide(rideId)
                            );

                            add(accept);
                            addSpace(15);
                        }
                    }

                    Button back = button("Back", GRAY);
                    back.setOnClickListener(v -> showDashboard());
                    add(back);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Could not load rides: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void acceptRide(String rideId) {

        String uid = auth.getCurrentUser() == null
                ? ""
                : auth.getCurrentUser().getUid();

        if (uid.isEmpty()) {
            Toast.makeText(
                    this,
                    "Driver is not logged in.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        Map<String, Object> update =
                new HashMap<>();

        update.put("driverId", uid);
        update.put("driverName", "Sakay Na Driver");
        update.put("status", "ACCEPTED");
        update.put("acceptedAt",
                FieldValue.serverTimestamp());

        db.collection("rides")
                .document(rideId)
                .update(update)
                .addOnSuccessListener(v -> {

                    currentRideId = rideId;

                    getSharedPreferences(
                            "SakayNa",
                            MODE_PRIVATE
                    )
                            .edit()
                            .putString(
                                    "driver_ride_id",
                                    rideId
                            )
                            .putString(
                                    "driver_ride_status",
                                    "ACCEPTED"
                            )
                            .apply();

                    Toast.makeText(
                            this,
                            "Ride accepted!",
                            Toast.LENGTH_SHORT
                    ).show();

                    listenToRide(rideId);
                    showCurrentRide();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Accept failed: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void showCurrentRide() {

        clearRoot();

        add(title("Current Ride"));

        rideText = label("Loading ride...");
        add(rideText);

        addSpace(15);

        if (currentRideId.isEmpty()) {

            rideText.setText(
                    "No current ride."
            );

            Button find =
                    button("Find Ride Requests", ORANGE);

            find.setOnClickListener(
                    v -> showRideRequests()
            );

            add(find);

        } else {

            listenToRide(currentRideId);
        }

        addSpace(15);

        Button back = button("Back", GRAY);
        back.setOnClickListener(v -> showDashboard());
        add(back);
    }

    private void listenToRide(String rideId) {

        if (rideListener != null) {
            rideListener.remove();
        }

        if (rideId == null || rideId.isEmpty()) {
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

                                    if (snapshot == null ||
                                            !snapshot.exists()) {
                                        return;
                                    }

                                    updateRideDisplay(snapshot);
                                }
                        );
    }

    private void updateRideDisplay(
            DocumentSnapshot doc
    ) {

        String status =
                safe(doc.getString("status"));

        String passenger =
                safe(doc.getString("passengerName"));

        String phone =
                safe(doc.getString("passengerPhone"));

        String pickup =
                safe(doc.getString("pickup"));

        String destination =
                safe(doc.getString("destination"));

        Double distance =
                doc.getDouble("distanceKm");

        Long fare =
                doc.getLong("fare");

        String text =
                "STATUS: " + readableStatus(status) +
                        "\n\nPassenger: " + passenger +
                        "\nPhone: " + phone +
                        "\n\nPickup:\n" + pickup +
                        "\n\nDestination:\n" + destination +
                        "\n\nDistance: " +
                        (distance == null ? "0" : distance) +
                        " km" +
                        "\nFare: ₱" +
                        (fare == null ? "0" : fare);

        if (rideText != null) {
            rideText.setText(text);
        }

        if (root != null &&
                root.getChildCount() > 0) {

            buildStatusButtons(status);
        }
    }

    private void buildStatusButtons(String status) {

        if (currentRideId.isEmpty()) {
            return;
        }

        if (status.equals("ACCEPTED")) {

            addSpace(10);

            Button onTheWay =
                    button(
                            "🚗 DRIVER ON THE WAY",
                            ORANGE
                    );

            onTheWay.setOnClickListener(
                    v -> updateRideStatus(
                            "DRIVER_ON_THE_WAY"
                    )
            );

            add(onTheWay);

        } else if (status.equals("DRIVER_ON_THE_WAY")) {

            addSpace(10);

            Button arrived =
                    button(
                            "📍 DRIVER ARRIVED",
                            BLUE
                    );

            arrived.setOnClickListener(
                    v -> updateRideStatus(
                            "DRIVER_ARRIVED"
                    )
            );

            add(arrived);

        } else if (status.equals("DRIVER_ARRIVED")) {

            addSpace(10);

            Button start =
                    button(
                            "▶ START TRIP",
                            GREEN
                    );

            start.setOnClickListener(
                    v -> updateRideStatus(
                            "IN_PROGRESS"
                    )
            );

            add(start);

        } else if (status.equals("IN_PROGRESS")) {

            addSpace(10);

            Button finish =
                    button(
                            "🏁 FINISH TRIP",
                            GREEN
                    );

            finish.setOnClickListener(
                    v -> updateRideStatus(
                            "FINISHED"
                    )
            );

            add(finish);

        } else if (status.equals("FINISHED")) {

            addSpace(10);

            TextView finished =
                    label(
                            "Trip finished.\n" +
                                    "Waiting for passenger to complete the ride and give a rating."
                    );

            add(finished);

        } else if (status.equals("COMPLETED")) {

            addSpace(10);

            TextView completed =
                    label(
                            "✅ Ride completed successfully."
                    );

            add(completed);

            Button dashboard =
                    button(
                            "Back to Dashboard",
                            GREEN
                    );

            dashboard.setOnClickListener(
                    v -> {
                        currentRideId = "";

                        getSharedPreferences(
                                "SakayNa",
                                MODE_PRIVATE
                        )
                                .edit()
                                .remove("driver_ride_id")
                                .remove("driver_ride_status")
                                .apply();

                        showDashboard();
                    }
            );

            add(dashboard);
        }
    }

    private void updateRideStatus(String newStatus) {

        if (currentRideId.isEmpty()) {
            return;
        }

        Map<String, Object> update =
                new HashMap<>();

        update.put("status", newStatus);

        if (newStatus.equals("DRIVER_ON_THE_WAY")) {
            update.put(
                    "driverOnTheWayAt",
                    FieldValue.serverTimestamp()
            );
        }

        if (newStatus.equals("DRIVER_ARRIVED")) {
            update.put(
                    "driverArrivedAt",
                    FieldValue.serverTimestamp()
            );
        }

        if (newStatus.equals("IN_PROGRESS")) {
            update.put(
                    "tripStartedAt",
                    FieldValue.serverTimestamp()
            );
        }

        if (newStatus.equals("FINISHED")) {
            update.put(
                    "finishedAt",
                    FieldValue.serverTimestamp()
            );
        }

        db.collection("rides")
                .document(currentRideId)
                .update(update)
                .addOnSuccessListener(v -> {

                    getSharedPreferences(
                            "SakayNa",
                            MODE_PRIVATE
                    )
                            .edit()
                            .putString(
                                    "driver_ride_status",
                                    newStatus
                            )
                            .apply();

                    Toast.makeText(
                            this,
                            "Status updated: " +
                                    readableStatus(newStatus),
                            Toast.LENGTH_SHORT
                    ).show();

                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Update failed: " +
                                        e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void showHistory() {

        clearRoot();

        add(title("Ride History"));

        String uid = auth.getCurrentUser() == null
                ? ""
                : auth.getCurrentUser().getUid();

        if (uid.isEmpty()) {

            add(label("Driver not logged in."));

        } else {

            db.collection("rides")
                    .whereEqualTo("driverId", uid)
                    .get()
                    .addOnSuccessListener(snapshot -> {

                        if (snapshot.isEmpty()) {

                            add(label(
                                    "No completed rides yet."
                            ));

                        } else {

                            for (DocumentSnapshot doc :
                                    snapshot.getDocuments()) {

                                String passenger =
                                        safe(doc.getString(
                                                "passengerName"
                                        ));

                                String destination =
                                        safe(doc.getString(
                                                "destination"
                                        ));

                                String status =
                                        safe(doc.getString(
                                                "status"
                                        ));

                                Long fare =
                                        doc.getLong("fare");

                                add(label(
                                        "Passenger: " +
                                                passenger +
                                                "\nDestination: " +
                                                destination +
                                                "\nStatus: " +
                                                readableStatus(
                                                        status
                                                ) +
                                                "\nFare: ₱" +
                                                (fare == null
                                                        ? "0"
                                                        : fare)
                                ));

                                addSpace(10);
                            }
                        }

                        Button back =
                                button("Back", GRAY);

                        back.setOnClickListener(
                                v -> showDashboard()
                        );

                        add(back);
                    });
        }
    }

    private void startDriverLocation() {

        locationManager =
                (LocationManager)
                        getSystemService(
                                LOCATION_SERVICE
                        );

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST
            );

            return;
        }

        requestLocationUpdates();
    }

    private void requestLocationUpdates() {

        if (locationManager == null) {
            return;
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationListener listener =
                new LocationListener() {

                    @Override
                    public void onLocationChanged(
                            @NonNull Location location
                    ) {

                        driverLatitude =
                                location.getLatitude();

                        driverLongitude =
                                location.getLongitude();

                        uploadDriverLocation();

                        updateGpsText();
                    }
                };

        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000,
                5,
                listener
        );

        Location last =
                locationManager.getLastKnownLocation(
                        LocationManager.GPS_PROVIDER
                );

        if (last != null) {

            driverLatitude =
                    last.getLatitude();

            driverLongitude =
                    last.getLongitude();

            uploadDriverLocation();
        }
    }

    private void uploadDriverLocation() {

        String uid = auth.getCurrentUser() == null
                ? ""
                : auth.getCurrentUser().getUid();

        if (uid.isEmpty()) {
            return;
        }

        Map<String, Object> location =
                new HashMap<>();

        location.put(
                "driverId",
                uid
        );

        location.put(
                "latitude",
                driverLatitude
        );

        location.put(
                "longitude",
                driverLongitude
        );

        location.put(
                "updatedAt",
                FieldValue.serverTimestamp()
        );

        db.collection("driverLocations")
                .document(uid)
                .set(location);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode ==
                LOCATION_PERMISSION_REQUEST) {

            if (grantResults.length > 0 &&
                    grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED) {

                requestLocationUpdates();

            } else {

                Toast.makeText(
                        this,
                        "Location permission is required for driver GPS.",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    private String readableStatus(String status) {

        if (status == null || status.isEmpty()) {
            return "UNKNOWN";
        }

        return status.replace(
                "_",
                " "
        );
    }

    private String safe(String value) {

        if (value == null) {
            return "";
        }

        return value;
    }

    private void logout() {

        stopRideListener();

        auth.signOut();

        getSharedPreferences(
                "SakayNa",
                MODE_PRIVATE
        )
                .edit()
                .clear()
                .apply();

        Toast.makeText(
                this,
                "Logged out.",
                Toast.LENGTH_SHORT
        ).show();

        finish();
    }

    private void stopRideListener() {

        if (rideListener != null) {
            rideListener.remove();
            rideListener = null;
        }
    }

    @Override
    protected void onDestroy() {

        stopRideListener();

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {

        showDashboard();
    }
}
