
package com.sakyna.app;

import android.Manifest;
import android.app.AlertDialog;
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

public class PassengerActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private LinearLayout root;

    private ListenerRegistration rideListener;
    private ListenerRegistration driverLocationListener;

    private LocationManager locationManager;

    private double passengerLatitude = 0.0;
    private double passengerLongitude = 0.0;

    private double destinationLatitude = 0.0;
    private double destinationLongitude = 0.0;

    private double driverLatitude = 0.0;
    private double driverLongitude = 0.0;

    private double rideDistanceKm = 0.0;
    private double driverDistanceKm = 0.0;

    private String rideId = "";

    private TextView liveDriverDistanceText;
    private TextView liveDriverGpsText;

    private LinearLayout completionPanel;
    private int selectedRating = 0;

    private final int LOCATION_PERMISSION_REQUEST = 2001;

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
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        SharedPreferences prefs =
                getSharedPreferences("SakayNa", MODE_PRIVATE);

        rideId = prefs.getString("ride_id", "");

        showDashboard();
        startPassengerLocation();
    }

    private TextView title(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(24);
        tv.setTextColor(DARK);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(20, 30, 20, 30);
        return tv;
    }

    private TextView label(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(16);
        tv.setTextColor(DARK);
        tv.setPadding(15, 12, 15, 12);
        return tv;
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(15);
        b.setAllCaps(false);
        return b;
    }

    private void setupRoot() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 20, 20, 20);
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

    private void addSpace() {
        TextView space = new TextView(this);
        space.setText("");
        space.setHeight(10);
        add(space);
    }

    private void showDashboard() {

        stopRideListeners();

        setupRoot();

        add(title("SAKAY NA"));
        add(label("Passenger Dashboard"));
        addSpace();

        Button gps = button("MY GPS LOCATION");
        gps.setOnClickListener(v -> showGps());
        add(gps);

        Button book = button("BOOK A RIDE");
        book.setOnClickListener(v -> showBooking());
        add(book);

        Button current = button("CURRENT RIDE");
        current.setOnClickListener(v -> showCurrentRide());
        add(current);

        Button driver = button("DRIVER LOCATION");
        driver.setOnClickListener(v -> showDriverLocation());
        add(driver);

        Button history = button("RIDE HISTORY");
        history.setOnClickListener(v -> showHistory());
        add(history);

        Button help = button("HELP / EMERGENCY");
        help.setOnClickListener(v ->
                Toast.makeText(
                        this,
                        "For emergency assistance, contact local emergency services.",
                        Toast.LENGTH_LONG
                ).show()
        );
        add(help);

        addSpace();

        Button logout = button("LOGOUT");
        logout.setOnClickListener(v -> logout());
        add(logout);

        setContentView(root);
    }

    private void showGps() {

        setupRoot();

        add(title("MY GPS LOCATION"));

        TextView gpsText = label(
                "Latitude: " + passengerLatitude +
                "\nLongitude: " + passengerLongitude
        );
        add(gpsText);

        Button refresh = button("REFRESH GPS");
        refresh.setOnClickListener(v -> {
            startPassengerLocation();
            gpsText.setText(
                    "Latitude: " + passengerLatitude +
                    "\nLongitude: " + passengerLongitude
            );
        });
        add(refresh);

        Button back = button("BACK");
        back.setOnClickListener(v -> showDashboard());
        add(back);

        setContentView(root);
    }

    private void showBooking() {

        setupRoot();

        add(title("BOOK A RIDE"));

        EditText pickup = new EditText(this);
        pickup.setHint("Pickup location");
        pickup.setText(
                passengerLatitude != 0.0
                        ? "My GPS: " + passengerLatitude + ", " + passengerLongitude
                        : ""
        );
        add(pickup);

        EditText destination = new EditText(this);
        destination.setHint("Destination");
        add(destination);

        EditText destLat = new EditText(this);
        destLat.setHint("Destination latitude");
        destLat.setInputType(8194);
        add(destLat);

        EditText destLon = new EditText(this);
        destLon.setHint("Destination longitude");
        destLon.setInputType(8194);
        add(destLon);

        TextView gpsStatus = label(
                "Passenger GPS:\n" +
                passengerLatitude + ", " + passengerLongitude
        );
        add(gpsStatus);

        TextView distanceText = label("Distance: --");
        add(distanceText);

        TextView fareText = label("Estimated Fare: --");
        add(fareText);

        Button calculate = button("CALCULATE DISTANCE & FARE");
        calculate.setOnClickListener(v -> {

            try {
                destinationLatitude =
                        Double.parseDouble(destLat.getText().toString().trim());

                destinationLongitude =
                        Double.parseDouble(destLon.getText().toString().trim());

                if (passengerLatitude == 0.0 &&
                        passengerLongitude == 0.0) {

                    Toast.makeText(
                            this,
                            "Waiting for passenger GPS location.",
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }

                rideDistanceKm = calculateDistanceKm(
                        passengerLatitude,
                        passengerLongitude,
                        destinationLatitude,
                        destinationLongitude
                );

                int calculatedFare =
                        50 + ((int) Math.ceil(rideDistanceKm) * 10);

                if (calculatedFare < 50) calculatedFare = 50;
                if (calculatedFare > 500) calculatedFare = 500;

                distanceText.setText(
                        String.format(
                                "Distance: %.2f km",
                                rideDistanceKm
                        )
                );

                fareText.setText(
                        "Estimated Fare: ₱" + calculatedFare
                );

            } catch (Exception e) {

                Toast.makeText(
                        this,
                        "Enter valid destination latitude and longitude.",
                        Toast.LENGTH_LONG
                ).show();
            }
        });
        add(calculate);

        Button request = button("REQUEST RIDE");
        request.setOnClickListener(v -> {

            String pickupText =
                    pickup.getText().toString().trim();

            String destinationText =
                    destination.getText().toString().trim();

            if (pickupText.isEmpty()) {
                Toast.makeText(
                        this,
                        "Enter pickup location.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            if (destinationText.isEmpty()) {
                Toast.makeText(
                        this,
                        "Enter destination.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            if (destinationLatitude == 0.0 &&
                    destinationLongitude == 0.0) {

                Toast.makeText(
                        this,
                        "Calculate the distance first.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            if (rideDistanceKm <= 0.0) {

                Toast.makeText(
                        this,
                        "Calculate the distance first.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            requestRide(
                    pickupText,
                    destinationText
            );
        });
        add(request);

        Button back = button("BACK");
        back.setOnClickListener(v -> showDashboard());
        add(back);

        setContentView(root);
    }

    private void requestRide(
            String pickup,
            String destination
    ) {

        if (auth.getCurrentUser() == null) {

            Toast.makeText(
                    this,
                    "Please login again.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(user -> {

                    String name =
                            user.getString("name");

                    String phone =
                            user.getString("phone");

                    if (name == null) name = "";
                    if (phone == null) phone = "";

                    int fare =
                            50 + ((int) Math.ceil(rideDistanceKm) * 10);

                    if (fare < 50) fare = 50;
                    if (fare > 500) fare = 500;

                    // IMPORTANT:
                    // fare is changed above, so make a final copy
                    // before using it inside the Firebase lambda.
                    final int safeFare = fare;

                    Map<String, Object> ride =
                            new HashMap<>();

                    ride.put("passengerId", uid);
                    ride.put("passengerName", name);
                    ride.put("passengerPhone", phone);

                    ride.put("pickup", pickup);
                    ride.put("destination", destination);

                    ride.put(
                            "pickupLatitude",
                            passengerLatitude
                    );

                    ride.put(
                            "pickupLongitude",
                            passengerLongitude
                    );

                    ride.put(
                            "destinationLatitude",
                            destinationLatitude
                    );

                    ride.put(
                            "destinationLongitude",
                            destinationLongitude
                    );

                    ride.put(
                            "distanceKm",
                            rideDistanceKm
                    );

                    ride.put("fare", safeFare);
                    ride.put("finalFare", 0);

                    ride.put("status", "REQUESTED");

                    ride.put("driverId", "");
                    ride.put("driverName", "");

                    ride.put("rating", 0);

                    ride.put(
                            "createdAt",
                            FieldValue.serverTimestamp()
                    );

                    db.collection("rides")
                            .add(ride)
                            .addOnSuccessListener(document -> {

                                rideId =
                                        document.getId();

                                getSharedPreferences(
                                        "SakayNa",
                                        MODE_PRIVATE
                                )
                                        .edit()
                                        .putString(
                                                "ride_id",
                                                rideId
                                        )
                                        .putString(
                                                "ride_pickup",
                                                pickup
                                        )
                                        .putString(
                                                "ride_destination",
                                                destination
                                        )
                                        .putString(
                                                "ride_distance_km",
                                                String.valueOf(
                                                        rideDistanceKm
                                                )
                                        )
                                        .putString(
                                                "ride_fare",
                                                String.valueOf(
                                                        safeFare
                                                )
                                        )
                                        .putString(
                                                "ride_status",
                                                "REQUESTED"
                                        )
                                        .apply();

                                Toast.makeText(
                                        this,
                                        "Ride requested successfully.",
                                        Toast.LENGTH_LONG
                                ).show();

                                showCurrentRide();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(
                                            this,
                                            "Ride request failed: " +
                                                    e.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Could not load passenger profile.",
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void showCurrentRide() {

        setupRoot();

        add(title("CURRENT RIDE"));

        TextView statusText = label(
                "Loading ride..."
        );
        add(statusText);

        TextView rideInfo = label("");
        add(rideInfo);

        liveDriverGpsText = label(
                "Driver GPS: --"
        );
        add(liveDriverGpsText);

        liveDriverDistanceText = label(
                "Distance to driver: --"
        );
        add(liveDriverDistanceText);

        completionPanel = new LinearLayout(this);
        completionPanel.setOrientation(
                LinearLayout.VERTICAL
        );
        completionPanel.setPadding(
                0, 15, 0, 15
        );
        add(completionPanel);

        Button map = button("VIEW DRIVER LOCATION");
        map.setOnClickListener(v -> showDriverLocation());
        add(map);

        Button refresh = button("REFRESH STATUS");
        refresh.setOnClickListener(v ->
                listenToRide(statusText, rideInfo)
        );
        add(refresh);

        Button cancel = button("CANCEL RIDE");
        cancel.setOnClickListener(v ->
                cancelRide()
        );
        add(cancel);

        Button back = button("BACK");
        back.setOnClickListener(v -> showDashboard());
        add(back);

        setContentView(root);

        if (rideId.isEmpty()) {

            SharedPreferences prefs =
                    getSharedPreferences(
                            "SakayNa",
                            MODE_PRIVATE
                    );

            rideId =
                    prefs.getString(
                            "ride_id",
                            ""
                    );
        }

        if (rideId.isEmpty()) {

            statusText.setText(
                    "No active ride."
            );

            updateCompletionPanel(
                    "NONE"
            );

            return;
        }

        listenToRide(
                statusText,
                rideInfo
        );
    }

    private void listenToRide(
            TextView statusText,
            TextView rideInfo
    ) {

        if (rideId.isEmpty()) return;

        if (rideListener != null) {
            rideListener.remove();
        }

        rideListener =
                db.collection("rides")
                        .document(rideId)
                        .addSnapshotListener(
                                (snapshot, error) -> {

                                    if (error != null) {

                                        statusText.setText(
                                                "Error loading ride:\n" +
                                                        error.getMessage()
                                        );
                                        return;
                                    }

                                    if (snapshot == null ||
                                            !snapshot.exists()) {

                                        statusText.setText(
                                                "Ride not found."
                                        );
                                        return;
                                    }

                                    String status =
                                            snapshot.getString(
                                                    "status"
                                            );

                                    if (status == null) {
                                        status = "UNKNOWN";
                                    }

                                    statusText.setText(
                                            "Status: " +
                                                    readableStatus(
                                                            status
                                                    )
                                    );

                                    updateRideDisplay(
                                            snapshot,
                                            rideInfo,
                                            status
                                    );

                                    updateCompletionPanel(
                                            status
                                    );

                                    updateDriverLocationListener(
                                            snapshot,
                                            status
                                    );

                                    getSharedPreferences(
                                            "SakayNa",
                                            MODE_PRIVATE
                                    )
                                            .edit()
                                            .putString(
                                                    "ride_status",
                                                    status
                                            )
                                            .apply();
                                }
                        );
    }

    private void updateRideDisplay(
            DocumentSnapshot document,
            TextView rideInfo,
            String status
    ) {

        String pickup =
                safe(document.getString("pickup"));

        String destination =
                safe(document.getString("destination"));

        String driverName =
                safe(document.getString("driverName"));

        Double distance =
                document.getDouble("distanceKm");

        Long fare =
                document.getLong("fare");

        Long finalFare =
                document.getLong("finalFare");

        if (distance == null) {
            distance = 0.0;
        }

        if (fare == null) {
            fare = 0L;
        }

        if (finalFare == null) {
            finalFare = 0L;
        }

        long displayFare =
                finalFare > 0 ? finalFare : fare;

        StringBuilder text =
                new StringBuilder();

        text.append("Pickup: ")
                .append(pickup)
                .append("\n\n");

        text.append("Destination: ")
                .append(destination)
                .append("\n\n");

        text.append("Trip Distance: ")
                .append(
                        String.format(
                                "%.2f km",
                                distance
                        )
                )
                .append("\n\n");

        text.append("Fare: ₱")
                .append(displayFare)
                .append("\n\n");

        text.append("Driver: ")
                .append(
                        driverName.isEmpty()
                                ? "Waiting for driver"
                                : driverName
                )
                .append("\n\n");

        text.append("Status: ")
                .append(
                        readableStatus(
                                status
                        )
                );

        Long rating =
                document.getLong("rating");

        if (rating != null &&
                rating >= 1 &&
                rating <= 5) {

            text.append("\n\nDriver Rating: ")
                    .append(rating)
                    .append("/5");
        }

        rideInfo.setText(
                text.toString()
        );

        updateLiveDriverDistanceDisplay();
    }

    private void updateCompletionPanel(
            String status
    ) {

        if (completionPanel == null) {
            return;
        }

        completionPanel.removeAllViews();

        if ("FINISHED".equals(status)) {

            TextView finished =
                    label(
                            "TRIP FINISHED\n\n" +
                            "Please rate your driver."
                    );

            finished.setTextSize(19);
            finished.setTextColor(GREEN);
            finished.setGravity(
                    Gravity.CENTER
            );

            completionPanel.addView(
                    finished
            );

            TextView selected =
                    label(
                            "Selected rating: " +
                                    (selectedRating == 0
                                            ? "None"
                                            : selectedRating + "/5")
                    );

            selected.setGravity(
                    Gravity.CENTER
            );

            completionPanel.addView(
                    selected
            );

            LinearLayout ratings =
                    new LinearLayout(this);

            ratings.setOrientation(
                    LinearLayout.HORIZONTAL
            );

            ratings.setGravity(
                    Gravity.CENTER
            );

            for (int i = 1; i <= 5; i++) {

                final int rating = i;

                Button ratingButton =
                        new Button(this);

                ratingButton.setText(
                        rating + " ★"
                );

                ratingButton.setAllCaps(
                        false
                );

                ratingButton.setOnClickListener(
                        v -> {

                            selectedRating =
                                    rating;

                            selected.setText(
                                    "Selected rating: " +
                                            selectedRating +
                                            "/5"
                            );

                            updateCompletionPanel(
                                    "FINISHED"
                            );
                        }
                );

                ratings.addView(
                        ratingButton,
                        new LinearLayout.LayoutParams(
                                0,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                1
                        )
                );
            }

            completionPanel.addView(
                    ratings
            );

            Button complete =
                    button(
                            "COMPLETE RIDE"
                    );

            complete.setTextColor(
                    Color.WHITE
            );

            complete.setBackgroundColor(
                    GREEN
            );

            complete.setEnabled(
                    selectedRating >= 1 &&
                            selectedRating <= 5
            );

            complete.setOnClickListener(
                    v -> completeRide(
                            selectedRating
                    )
            );

            completionPanel.addView(
                    complete
            );

        } else if ("COMPLETED".equals(status)) {

            TextView completed =
                    label(
                            "RIDE COMPLETED\n\n" +
                            "Thank you for riding with Sakay Na!"
                    );

            completed.setTextSize(19);
            completed.setTextColor(GREEN);
            completed.setGravity(
                    Gravity.CENTER
            );

            completionPanel.addView(
                    completed
            );
        }
    }

    private void completeRide(
            int rating
    ) {

        if (rideId.isEmpty()) {
            return;
        }

        if (rating < 1 || rating > 5) {

            Toast.makeText(
                    this,
                    "Please select a rating from 1 to 5.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String currentRideId =
                rideId;

        db.collection("rides")
                .document(currentRideId)
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {

                        Toast.makeText(
                                this,
                                "Ride no longer exists.",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    String status =
                            document.getString(
                                    "status"
                            );

                    if (!"FINISHED".equals(status)) {

                        Toast.makeText(
                                this,
                                "Ride is not ready for completion.",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    Long fare =
                            document.getLong(
                                    "fare"
                            );

                    Long finalFare =
                            document.getLong(
                                    "finalFare"
                            );

                    Map<String, Object> update =
                            new HashMap<>();

                    update.put(
                            "status",
                            "COMPLETED"
                    );

                    update.put(
                            "rating",
                            rating
                    );

                    update.put(
                            "completedAt",
                            FieldValue.serverTimestamp()
                    );

                    if (fare != null &&
                            (finalFare == null ||
                                    finalFare <= 0)) {

                        update.put(
                                "finalFare",
                                fare
                        );
                    }

                    db.collection("rides")
                            .document(currentRideId)
                            .update(update)
                            .addOnSuccessListener(
                                    unused -> {

                                        if (rideListener != null) {
                                            rideListener.remove();
                                            rideListener = null;
                                        }

                                        if (driverLocationListener != null) {
                                            driverLocationListener.remove();
                                            driverLocationListener = null;
                                        }

                                        getSharedPreferences(
                                                "SakayNa",
                                                MODE_PRIVATE
                                        )
                                                .edit()
                                                .remove("ride_id")
                                                .remove("ride_pickup")
                                                .remove("ride_destination")
                                                .remove("ride_distance_km")
                                                .remove("ride_fare")
                                                .putString(
                                                        "ride_status",
                                                        "COMPLETED"
                                                )
                                                .apply();

                                        rideId = "";
                                        selectedRating = 0;

                                        Toast.makeText(
                                                this,
                                                "Ride completed! Thank you.",
                                                Toast.LENGTH_LONG
                                        ).show();

                                        showDashboard();
                                    }
                            )
                            .addOnFailureListener(
                                    e -> Toast.makeText(
                                            this,
                                            "Could not complete ride: " +
                                                    e.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show()
                            );
                })
                .addOnFailureListener(
                        e -> Toast.makeText(
                                this,
                                "Could not verify ride: " +
                                        e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void cancelRide() {

        if (rideId.isEmpty()) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Cancel Ride")
                .setMessage(
                        "Are you sure you want to cancel this ride?"
                )
                .setNegativeButton(
                        "NO",
                        null
                )
                .setPositiveButton(
                        "YES",
                        (dialog, which) -> {

                            db.collection("rides")
                                    .document(rideId)
                                    .get()
                                    .addOnSuccessListener(
                                            document -> {

                                                String status =
                                                        document.getString(
                                                                "status"
                                                        );

                                                if (!"REQUESTED"
                                                        .equals(status)) {

                                                    Toast.makeText(
                                                            this,
                                                            "This ride can no longer be cancelled.",
                                                            Toast.LENGTH_LONG
                                                    ).show();

                                                    return;
                                                }

                                                Map<String, Object> update =
                                                        new HashMap<>();

                                                update.put(
                                                        "status",
                                                        "CANCELLED"
                                                );

                                                update.put(
                                                        "cancelledAt",
                                                        FieldValue.serverTimestamp()
                                                );

                                                db.collection("rides")
                                                        .document(rideId)
                                                        .update(update)
                                                        .addOnSuccessListener(
                                                                unused -> {

                                                                    if (rideListener != null) {
                                                                        rideListener.remove();
                                                                        rideListener = null;
                                                                    }

                                                                    if (driverLocationListener != null) {
                                                                        driverLocationListener.remove();
                                                                        driverLocationListener = null;
                                                                    }

                                                                    getSharedPreferences(
                                                                            "SakayNa",
                                                                            MODE_PRIVATE
                                                                    )
                                                                            .edit()
                                                                            .remove("ride_id")
                                                                            .remove("ride_pickup")
                                                                            .remove("ride_destination")
                                                                            .remove("ride_distance_km")
                                                                            .remove("ride_fare")
                                                                            .putString(
                                                                                    "ride_status",
                                                                                    "CANCELLED"
                                                                            )
                                                                            .apply();

                                                                    rideId = "";

                                                                    Toast.makeText(
                                                                            this,
                                                                            "Ride cancelled.",
                                                                            Toast.LENGTH_LONG
                                                                    ).show();

                                                                    showDashboard();
                                                                }
                                                        );
                                            }
                                    );
                        }
                )
                .show();
    }

    private void updateDriverLocationListener(
            DocumentSnapshot ride,
            String status
    ) {

        String driverId =
                ride.getString(
                        "driverId"
                );

        if (driverLocationListener != null) {
            driverLocationListener.remove();
            driverLocationListener = null;
        }

        if (driverId == null ||
                driverId.trim().isEmpty()) {

            updateLiveDriverDistanceDisplay();
            return;
        }

        if ("REQUESTED".equals(status) ||
                "CANCELLED".equals(status) ||
                "DECLINED".equals(status) ||
                "COMPLETED".equals(status)) {

            updateLiveDriverDistanceDisplay();
            return;
        }

        driverLocationListener =
                db.collection("driverLocations")
                        .document(driverId)
                        .addSnapshotListener(
                                (snapshot, error) -> {

                                    if (error != null ||
                                            snapshot == null ||
                                            !snapshot.exists()) {
                                        return;
                                    }

                                    Double lat =
                                            snapshot.getDouble(
                                                    "latitude"
                                            );

                                    Double lon =
                                            snapshot.getDouble(
                                                    "longitude"
                                            );

                                    if (lat == null ||
                                            lon == null) {
                                        return;
                                    }

                                    driverLatitude =
                                            lat;

                                    driverLongitude =
                                            lon;

                                    updateLiveDriverDistanceDisplay();
                                }
                        );
    }

    private void updateLiveDriverDistanceDisplay() {

        if (driverLatitude == 0.0 &&
                driverLongitude == 0.0) {

            if (liveDriverGpsText != null) {

                liveDriverGpsText.setText(
                        "Driver GPS: waiting..."
                );
            }

            if (liveDriverDistanceText != null) {

                liveDriverDistanceText.setText(
                        "Distance to driver: --"
                );
            }

            return;
        }

        if (liveDriverGpsText != null) {

            liveDriverGpsText.setText(
                    "Driver GPS:\n" +
                            driverLatitude +
                            ", " +
                            driverLongitude
            );
        }

        if (passengerLatitude != 0.0 &&
                passengerLongitude != 0.0) {

            driverDistanceKm =
                    calculateDistanceKm(
                            passengerLatitude,
                            passengerLongitude,
                            driverLatitude,
                            driverLongitude
                    );

            if (liveDriverDistanceText != null) {

                liveDriverDistanceText.setText(
                        String.format(
                                "Distance to driver: %.2f km",
                                driverDistanceKm
                        )
                );
            }
        }
    }

    private void showDriverLocation() {

        setupRoot();

        add(title("DRIVER LOCATION"));

        if (driverLatitude == 0.0 &&
                driverLongitude == 0.0) {

            add(label(
                    "Driver GPS is not available yet."
            ));

        } else {

            add(label(
                    "Driver latitude: " +
                            driverLatitude +
                            "\n\nDriver longitude: " +
                            driverLongitude +
                            String.format(
                                    "\n\nDistance from you: %.2f km",
                                    driverDistanceKm
                            )
            ));
        }

        Button refresh =
                button("REFRESH");

        refresh.setOnClickListener(
                v -> showDriverLocation()
        );

        add(refresh);

        Button back =
                button("BACK");

        back.setOnClickListener(
                v -> showDashboard()
        );

        add(back);

        setContentView(root);
    }

    private void showHistory() {

        setupRoot();

        add(title("RIDE HISTORY"));

        TextView loading =
                label("Loading ride history...");

        add(loading);

        Button back =
                button("BACK");

        back.setOnClickListener(
                v -> showDashboard()
        );

        add(back);

        setContentView(root);

        if (auth.getCurrentUser() == null) {

            loading.setText(
                    "Please login again."
            );

            return;
        }

        String uid =
                auth.getCurrentUser().getUid();

        db.collection("rides")
                .whereEqualTo(
                        "passengerId",
                        uid
                )
                .get()
                .addOnSuccessListener(
                        snapshots -> {

                            root.removeView(
                                    loading
                            );

                            if (snapshots.isEmpty()) {

                                add(label(
                                        "No rides yet."
                                ));

                                return;
                            }

                            for (
                                    DocumentSnapshot document :
                                    snapshots.getDocuments()
                            ) {

                                String pickup =
                                        safe(
                                                document.getString(
                                                        "pickup"
                                                )
                                        );

                                String destination =
                                        safe(
                                                document.getString(
                                                        "destination"
                                                )
                                        );

                                String status =
                                        safe(
                                                document.getString(
                                                        "status"
                                                )
                                        );

                                Long fare =
                                        document.getLong(
                                                "finalFare"
                                        );

                                if (fare == null ||
                                        fare <= 0) {

                                    fare =
                                            document.getLong(
                                                    "fare"
                                            );
                                }

                                if (fare == null) {
                                    fare = 0L;
                                }

                                Long rating =
                                        document.getLong(
                                                "rating"
                                        );

                                StringBuilder history =
                                        new StringBuilder();

                                history.append(
                                        "From: "
                                )
                                        .append(pickup)
                                        .append("\n");

                                history.append(
                                        "To: "
                                )
                                        .append(destination)
                                        .append("\n");

                                history.append(
                                        "Fare: ₱"
                                )
                                        .append(fare)
                                        .append("\n");

                                history.append(
                                        "Status: "
                                )
                                        .append(
                                                readableStatus(
                                                        status
                                                )
                                        );

                                if (rating != null &&
                                        rating >= 1 &&
                                        rating <= 5) {

                                    history.append(
                                            "\nRating: "
                                    )
                                            .append(
                                                    rating
                                            )
                                            .append(
                                                    "/5 ★"
                                            );
                                }

                                TextView item =
                                        label(
                                                history.toString()
                                        );

                                item.setPadding(
                                        15,
                                        20,
                                        15,
                                        20
                                );

                                add(item);

                                addSpace();
                            }
                        }
                )
                .addOnFailureListener(
                        e -> loading.setText(
                                "Could not load history:\n" +
                                        e.getMessage()
                        )
                );
    }

    private void startPassengerLocation() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
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

        LocationListener listener =
                new LocationListener() {

                    @Override
                    public void onLocationChanged(
                            @NonNull Location location
                    ) {

                        passengerLatitude =
                                location.getLatitude();

                        passengerLongitude =
                                location.getLongitude();

                        updateLiveDriverDistanceDisplay();
                    }
                };

        try {

            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    3000,
                    2,
                    listener
            );

            Location last =
                    locationManager.getLastKnownLocation(
                            LocationManager.GPS_PROVIDER
                    );

            if (last != null) {

                passengerLatitude =
                        last.getLatitude();

                passengerLongitude =
                        last.getLongitude();
            }

        } catch (SecurityException e) {

            Toast.makeText(
                    this,
                    "Location permission required.",
                    Toast.LENGTH_SHORT
            ).show();
        }
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

                startPassengerLocation();

            } else {

                Toast.makeText(
                        this,
                        "GPS permission was not granted.",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    private double calculateDistanceKm(
            double lat1,
            double lon1,
            double lat2,
            double lon2
    ) {

        float[] result =
                new float[1];

        Location.distanceBetween(
                lat1,
                lon1,
                lat2,
                lon2,
                result
        );

        return result[0] / 1000.0;
    }

    private String readableStatus(
            String status
    ) {

        if (status == null) {
            return "Unknown";
        }

        switch (status) {

            case "REQUESTED":
                return "Waiting for driver";

            case "ACCEPTED":
                return "Driver accepted";

            case "DRIVER_ON_THE_WAY":
                return "Driver is on the way";

            case "DRIVER_ARRIVED":
                return "Driver arrived";

            case "IN_PROGRESS":
                return "Trip in progress";

            case "FINISHED":
                return "Trip finished - awaiting passenger completion";

            case "COMPLETED":
                return "Completed";

            case "CANCELLED":
                return "Cancelled";

            case "DECLINED":
                return "Declined";

            default:
                return status;
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void stopRideListeners() {

        if (rideListener != null) {
            rideListener.remove();
            rideListener = null;
        }

        if (driverLocationListener != null) {
            driverLocationListener.remove();
            driverLocationListener = null;
        }
    }

    private void logout() {

        stopRideListeners();

        getSharedPreferences(
                "SakayNa",
                MODE_PRIVATE
        )
                .edit()
                .clear()
                .apply();

        auth.signOut();

        Toast.makeText(
                this,
                "Logged out.",
                Toast.LENGTH_SHORT
        ).show();

        finish();
    }

    @Override
    protected void onDestroy() {

        stopRideListeners();

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {

        showDashboard();
    }
}
