
package com.sakyna.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;

public class DriverActivity extends Activity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private LocationManager locationManager;

    private TextView gpsText;
    private TextView statusText;
    private TextView currentRideText;
    private LinearLayout requestContainer;
    private LinearLayout historyContainer;

    private double driverLatitude = 0.0;
    private double driverLongitude = 0.0;

    private String currentRideId = "";

    private ListenerRegistration rideListener;

    private final int LOCATION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        showDashboard();
        startGps();
        loadRideRequests();
        loadHistory();
    }

    private void showDashboard() {

        ScrollView scrollView = new ScrollView(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(25, 30, 25, 30);
        root.setBackgroundColor(Color.WHITE);

        TextView title = text(
                "🛺 SAKAY NA",
                30,
                Color.BLACK
        );

        title.setGravity(Gravity.CENTER);

        root.addView(title);

        TextView heading = text(
                "DRIVER DASHBOARD",
                22,
                Color.rgb(20, 120, 70)
        );

        heading.setGravity(Gravity.CENTER);

        root.addView(heading);

        gpsText = text(
                "📍 GPS: Starting...",
                15,
                Color.DKGRAY
        );

        root.addView(gpsText);

        statusText = text(
                "🟢 Driver is online",
                16,
                Color.rgb(20, 120, 70)
        );

        root.addView(statusText);

        TextView requestTitle = text(
                "📋 RIDE REQUESTS",
                20,
                Color.BLACK
        );

        root.addView(requestTitle);

        requestContainer = new LinearLayout(this);
        requestContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        root.addView(requestContainer);

        TextView currentTitle = text(
                "🚕 CURRENT RIDE",
                20,
                Color.BLACK
        );

        root.addView(currentTitle);

        currentRideText = text(
                "No active ride.",
                16,
                Color.DKGRAY
        );

        root.addView(currentRideText);

        TextView historyTitle = text(
                "📜 RIDE HISTORY",
                20,
                Color.BLACK
        );

        root.addView(historyTitle);

        historyContainer = new LinearLayout(this);
        historyContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        root.addView(historyContainer);

        Button refresh = button(
                "🔄 REFRESH RIDES"
        );

        refresh.setOnClickListener(
                v -> {
                    loadRideRequests();
                    loadHistory();
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

        scrollView.addView(root);

        setContentView(scrollView);
    }

    private void startGps() {

        locationManager =
                (LocationManager) getSystemService(
                        LOCATION_SERVICE
                );

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_REQUEST_CODE
            );

            return;
        }

        try {

            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    5000,
                    10,
                    locationListener
            );

            Location lastLocation =
                    locationManager.getLastKnownLocation(
                            LocationManager.GPS_PROVIDER
                    );

            if (lastLocation != null) {

                updateDriverLocation(
                        lastLocation
                );
            }

        } catch (Exception e) {

            gpsText.setText(
                    "📍 GPS error: " +
                    e.getMessage()
            );
        }
    }

    private final LocationListener locationListener =
            new LocationListener() {

                @Override
                public void onLocationChanged(
                        Location location) {

                    updateDriverLocation(
                            location
                    );
                }
            };

    private void updateDriverLocation(
            Location location) {

        driverLatitude =
                location.getLatitude();

        driverLongitude =
                location.getLongitude();

        updateGpsText();

        uploadDriverLocation();
    }

    private void updateGpsText() {

        if (gpsText == null) {
            return;
        }

        gpsText.setText(
                String.format(
                        "📍 GPS: %.6f, %.6f",
                        driverLatitude,
                        driverLongitude
                )
        );
    }

    private void uploadDriverLocation() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            return;
        }

        Map<String, Object> location =
                new HashMap<>();

        location.put(
                "driverId",
                user.getUid()
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
                com.google.firebase.firestore.FieldValue
                        .serverTimestamp()
        );

        db.collection("driverLocations")
                .document(user.getUid())
                .set(location);
    }

    private void loadRideRequests() {

        if (requestContainer == null) {
            return;
        }

        requestContainer.removeAllViews();

        db.collection("rides")
                .whereEqualTo(
                        "status",
                        "REQUESTED"
                )
                .orderBy(
                        "createdAt",
                        Query.Direction.DESCENDING
                )
                .get()
                .addOnSuccessListener(
                        documents -> {

                            if (documents.isEmpty()) {

                                requestContainer.addView(
                                        text(
                                                "No ride requests right now.",
                                                15,
                                                Color.GRAY
                                        )
                                );

                                return;
                            }

                            for (DocumentSnapshot document :
                                    documents) {

                                addRideRequest(
                                        document
                                );
                            }
                        }
                )
                .addOnFailureListener(
                        e -> {

                            requestContainer.addView(
                                    text(
                                            "Unable to load requests: " +
                                            e.getMessage(),
                                            15,
                                            Color.RED
                                    )
                            );
                        }
                );
    }

    private void addRideRequest(
            DocumentSnapshot document) {

        String rideId =
                document.getId();

        String pickup =
                getString(
                        document,
                        "pickup"
                );

        String destination =
                getString(
                        document,
                        "destination"
                );

        String paymentMethod =
                getString(
                        document,
                        "paymentMethod"
                );

        String fare =
                getFareText(
                        document
                );

        String distance =
                getDistanceText(
                        document
                );

        LinearLayout card =
                new LinearLayout(this);

        card.setOrientation(
                LinearLayout.VERTICAL
        );

        card.setPadding(
                20,
                20,
                20,
                20
        );

        TextView route =
                text(
                        "📍 " + pickup +
                        "\n➡️ " + destination,
                        16,
                        Color.BLACK
                );

        card.addView(route);

        TextView fareView =
                text(
                        "💰 Fare: ₱" + fare,
                        18,
                        Color.rgb(20, 120, 70)
                );

        card.addView(fareView);

        TextView paymentView =
                text(
                        getPaymentDisplay(
                                paymentMethod
                        ),
                        16,
                        Color.rgb(30, 80, 150)
                );

        card.addView(paymentView);

        TextView distanceView =
                text(
                        "📏 Distance: " + distance,
                        15,
                        Color.DKGRAY
                );

        card.addView(distanceView);

        Button accept =
                button(
                        "✅ ACCEPT RIDE"
                );

        accept.setOnClickListener(
                v -> acceptRide(
                        rideId,
                        document
                )
        );

        card.addView(accept);

        requestContainer.addView(card);
    }

    private void acceptRide(
            String rideId,
            DocumentSnapshot document) {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            return;
        }

        Map<String, Object> update =
                new HashMap<>();

        update.put(
                "driverId",
                user.getUid()
        );

        update.put(
                "status",
                "ACCEPTED"
        );

        update.put(
                "acceptedAt",
                com.google.firebase.firestore.FieldValue
                        .serverTimestamp()
        );

        update.put(
                "driverLatitude",
                driverLatitude
        );

        update.put(
                "driverLongitude",
                driverLongitude
        );

        db.collection("rides")
                .document(rideId)
                .update(update)
                .addOnSuccessListener(
                        unused -> {

                            currentRideId =
                                    rideId;

                            showMessage(
                                    "🟢 Ride accepted!"
                            );

                            listenToCurrentRide(
                                    rideId
                            );

                            loadRideRequests();
                        }
                )
                .addOnFailureListener(
                        e -> showMessage(
                                "Accept failed: " +
                                e.getMessage()
                        )
                );
    }

    private void listenToCurrentRide(
            String rideId) {

        if (rideListener != null) {

            rideListener.remove();

            rideListener = null;
        }

        rideListener =
                db.collection("rides")
                        .document(rideId)
                        .addSnapshotListener(
                                new EventListener<DocumentSnapshot>() {

                                    @Override
                                    public void onEvent(
                                            DocumentSnapshot document,
                                            FirebaseFirestoreException error) {

                                        if (error != null) {

                                            currentRideText.setText(
                                                    "Ride listener error: " +
                                                    error.getMessage()
                                            );

                                            return;
                                        }

                                        if (document == null ||
                                                !document.exists()) {

                                            currentRideText.setText(
                                                    "Current ride no longer exists."
                                            );

                                            return;
                                        }

                                        currentRideId =
                                                document.getId();

                                        showCurrentRide(
                                                document
                                        );
                                    }
                                }
                        );
    }

    private void showCurrentRide(
            DocumentSnapshot document) {

        String pickup =
                getString(
                        document,
                        "pickup"
                );

        String destination =
                getString(
                        document,
                        "destination"
                );

        String status =
                getString(
                        document,
                        "status"
                );

        String paymentMethod =
                getString(
                        document,
                        "paymentMethod"
                );

        String fare =
                getFareText(
                        document
                );

        String distance =
                getDistanceText(
                        document
                );

        StringBuilder builder =
                new StringBuilder();

        builder.append(
                "📍 Pickup: "
        );

        builder.append(
                pickup
        );

        builder.append(
                "\n➡️ Destination: "
        );

        builder.append(
                destination
        );

        builder.append(
                "\n\n💰 Fare: ₱"
        );

        builder.append(
                fare
        );

        builder.append(
                "\n"
        );

        builder.append(
                getPaymentDisplay(
                        paymentMethod
                )
        );

        builder.append(
                "\n📏 Distance: "
        );

        builder.append(
                distance
        );

        builder.append(
                "\n\n📌 Status: "
        );

        builder.append(
                status
        );

        currentRideText.setText(
                builder.toString()
        );

        addStatusButtons(
                document
        );
    }

    private void addStatusButtons(
            DocumentSnapshot document) {

        String status =
                getString(
                        document,
                        "status"
                );

        LinearLayout parent =
                (LinearLayout) currentRideText
                        .getParent();

        removeOldStatusButtons(
                parent
        );

        if (status.equals(
                "ACCEPTED"
        )) {

            addStatusButton(
                    parent,
                    "🚗 DRIVER ON THE WAY",
                    "DRIVER_ON_THE_WAY"
            );

        } else if (status.equals(
                "DRIVER_ON_THE_WAY"
        )) {

            addStatusButton(
                    parent,
                    "📍 DRIVER ARRIVED",
                    "DRIVER_ARRIVED"
            );

        } else if (status.equals(
                "DRIVER_ARRIVED"
        )) {

            addStatusButton(
                    parent,
                    "🚦 START TRIP",
                    "IN_PROGRESS"
            );

        } else if (status.equals(
                "IN_PROGRESS"
        )) {

            addStatusButton(
                    parent,
                    "🏁 FINISH TRIP",
                    "FINISHED"
            );

        } else if (status.equals(
                "FINISHED"
        )) {

            addStatusButton(
                    parent,
                    "✅ COMPLETE RIDE",
                    "COMPLETED"
            );
        }
    }

    private void removeOldStatusButtons(
            LinearLayout parent) {

        for (int i =
                parent.getChildCount() - 1;
                i >= 0;
                i--) {

            android.view.View child =
                    parent.getChildAt(i);

            if (child.getTag() != null &&
                    child.getTag().equals(
                            "STATUS_BUTTON"
                    )) {

                parent.removeViewAt(i);
            }
        }
    }

    private void addStatusButton(
            LinearLayout parent,
            String label,
            String newStatus) {

        Button button =
                button(label);

        button.setTag(
                "STATUS_BUTTON"
        );

        button.setOnClickListener(
                v -> updateRideStatus(
                        newStatus
                )
        );

        parent.addView(button);
    }

    private void updateRideStatus(
            String newStatus) {

        if (currentRideId == null ||
                currentRideId.isEmpty()) {

            showMessage(
                    "No active ride."
            );

            return;
        }

        Map<String, Object> update =
                new HashMap<>();

        update.put(
                "status",
                newStatus
        );

        if (newStatus.equals(
                "DRIVER_ON_THE_WAY"
        )) {

            update.put(
                    "driverOnTheWayAt",
                    com.google.firebase.firestore.FieldValue
                            .serverTimestamp()
            );

        } else if (newStatus.equals(
                "DRIVER_ARRIVED"
        )) {

            update.put(
                    "driverArrivedAt",
                    com.google.firebase.firestore.FieldValue
                            .serverTimestamp()
            );

        } else if (newStatus.equals(
                "IN_PROGRESS"
        )) {

            update.put(
                    "tripStartedAt",
                    com.google.firebase.firestore.FieldValue
                            .serverTimestamp()
            );

        } else if (newStatus.equals(
                "FINISHED"
        )) {

            update.put(
                    "finishedAt",
                    com.google.firebase.firestore.FieldValue
                            .serverTimestamp()
            );
        }

        db.collection("rides")
                .document(currentRideId)
                .update(update)
                .addOnSuccessListener(
                        unused -> {

                            showMessage(
                                    "🟢 Status updated: " +
                                    newStatus
                            );

                            if (newStatus.equals(
                                    "COMPLETED"
                            )) {

                                currentRideId =
                                        "";

                                if (rideListener != null) {

                                    rideListener.remove();

                                    rideListener =
                                            null;
                                }

                                currentRideText.setText(
                                        "No active ride."
                                );

                                loadHistory();
                            }
                        }
                )
                .addOnFailureListener(
                        e -> showMessage(
                                "Status update failed: " +
                                e.getMessage()
                        )
                );
    }

    private void loadHistory() {

        if (historyContainer == null) {
            return;
        }

        historyContainer.removeAllViews();

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            return;
        }

        db.collection("rides")
                .whereEqualTo(
                        "driverId",
                        user.getUid()
                )
                .orderBy(
                        "createdAt",
                        Query.Direction.DESCENDING
                )
                .limit(20)
                .get()
                .addOnSuccessListener(
                        documents -> {

                            if (documents.isEmpty()) {

                                historyContainer.addView(
                                        text(
                                                "No ride history yet.",
                                                15,
                                                Color.GRAY
                                        )
                                );

                                return;
                            }

                            for (DocumentSnapshot document :
                                    documents) {

                                String pickup =
                                        getString(
                                                document,
                                                "pickup"
                                        );

                                String destination =
                                        getString(
                                                document,
                                                "destination"
                                        );

                                String status =
                                        getString(
                                                document,
                                                "status"
                                        );

                                String fare =
                                        getFareText(
                                                document
                                        );

                                String payment =
                                        getString(
                                                document,
                                                "paymentMethod"
                                        );

                                TextView item =
                                        text(
                                                "📍 " +
                                                pickup +
                                                "\n➡️ " +
                                                destination +
                                                "\n💰 Fare: ₱" +
                                                fare +
                                                "\n" +
                                                getPaymentDisplay(
                                                        payment
                                                ) +
                                                "\n📌 " +
                                                status +
                                                "\n",
                                                15,
                                                Color.DKGRAY
                                        );

                                historyContainer.addView(
                                        item
                                );
                            }
                        }
                )
                .addOnFailureListener(
                        e -> historyContainer.addView(
                                text(
                                        "History error: " +
                                        e.getMessage(),
                                        15,
                                        Color.RED
                                )
                        )
                );
    }

    private String getPaymentDisplay(
            String paymentMethod) {

        if (paymentMethod == null) {
            return "💳 Payment: Not specified";
        }

        if (paymentMethod.equals(
                "CASH"
        )) {

            return "💵 Payment: Cash";
        }

        if (paymentMethod.equals(
                "GCASH"
        )) {

            return "📱 Payment: GCash";
        }

        if (paymentMethod.equals(
                "MAYA"
        )) {

            return "📱 Payment: Maya / PayMaya";
        }

        return "💳 Payment: " +
                paymentMethod;
    }

    private String getFareText(
            DocumentSnapshot document) {

        Object fare =
                document.get(
                        "fare"
                );

        if (fare == null) {
            return "0";
        }

        if (fare instanceof Number) {

            return String.format(
                    "%.0f",
                    ((Number) fare).doubleValue()
            );
        }

        return fare.toString();
    }

    private String getDistanceText(
            DocumentSnapshot document) {

        Object distance =
                document.get(
                        "distanceKm"
                );

        if (distance == null) {
            return "0 km";
        }

        if (distance instanceof Number) {

            return String.format(
                    "%.2f km",
                    ((Number) distance).doubleValue()
            );
        }

        return distance.toString() +
                " km";
    }

    private String getString(
            DocumentSnapshot document,
            String field) {

        String value =
                document.getString(
                        field
                );

        if (value == null) {
            return "";
        }

        return value;
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
                10,
                10,
                10,
                10
        );

        return view;
    }

    private void logout() {

        if (rideListener != null) {

            rideListener.remove();

            rideListener = null;
        }

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

    @Override
    protected void onDestroy() {

        super.onDestroy();

        if (rideListener != null) {

            rideListener.remove();

            rideListener = null;
        }

        if (locationManager != null) {

            try {

                locationManager.removeUpdates(
                        locationListener
                );

            } catch (Exception ignored) {
            }
        }
    }
}
