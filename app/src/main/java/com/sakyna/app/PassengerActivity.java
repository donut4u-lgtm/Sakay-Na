
package com.sakyna.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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

    private String rideId = "";

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

        locationManager =
                (LocationManager) getSystemService(LOCATION_SERVICE);

        loadSavedRide();
        startPassengerLocation();
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
                getSharedPreferences(
                        "SakayNa",
                        MODE_PRIVATE
                ).getString(
                        "current_name",
                        "Passenger"
                );

        TextView welcome =
                text(
                        "Welcome, " + name + "!",
                        20
                );

        welcome.setTypeface(
                null,
                Typeface.BOLD
        );

        root.addView(welcome);

        Button gps =
                button(
                        "📍  MY GPS LOCATION",
                        GREEN
                );

        gps.setOnClickListener(
                v -> showPassengerLocation()
        );

        Button book =
                button(
                        "🟠  BOOK A RIDE",
                        ORANGE
                );

        book.setOnClickListener(
                v -> showBooking()
        );

        Button current =
                button(
                        "🚕  CURRENT RIDE",
                        GREEN
                );

        current.setOnClickListener(
                v -> showCurrentRide()
        );

        Button driverLocation =
                button(
                        "📍  DRIVER LOCATION",
                        BLUE
                );

        driverLocation.setOnClickListener(
                v -> showDriverLocation()
        );

        Button history =
                button(
                        "📋  RIDE HISTORY",
                        BLUE
                );

        history.setOnClickListener(
                v -> showHistory()
        );

        Button help =
                button(
                        "🆘  HELP / EMERGENCY",
                        RED
                );

        help.setOnClickListener(
                v -> toast(
                        "Emergency assistance feature will be connected later."
                )
        );

        Button logout =
                button(
                        "LOGOUT",
                        GRAY
                );

        logout.setOnClickListener(
                v -> logout()
        );
    }

    private void showBooking() {

        setupScreen("Book a Ride");

        TextView info =
                text(
                        "Your GPS is the pickup location.\n" +
                        "Enter the destination and its coordinates.",
                        17
                );

        root.addView(info);

        EditText pickup =
                input("Pickup location");

        EditText destination =
                input("Destination");

        TextView gpsStatus =
                text(
                        "📍 Getting GPS location...",
                        16
                );

        gpsStatus.setTextColor(GREEN);

        root.addView(gpsStatus);

        updatePassengerGpsStatus(gpsStatus);

        TextView pickupCoordinates =
                text(
                        "Pickup coordinates will use your current GPS.",
                        15
                );

        root.addView(pickupCoordinates);

        EditText destinationLatitudeInput =
                input("Destination Latitude");

        destinationLatitudeInput.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER
                        | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                        | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
        );

        EditText destinationLongitudeInput =
                input("Destination Longitude");

        destinationLongitudeInput.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER
                        | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                        | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
        );

        TextView distanceText =
                text(
                        "Distance: Not calculated",
                        19
                );

        distanceText.setTextColor(GREEN);
        distanceText.setTypeface(
                null,
                Typeface.BOLD
        );

        root.addView(distanceText);

        Button calculate =
                button(
                        "CALCULATE DISTANCE",
                        BLUE
                );

        calculate.setOnClickListener(v -> {

            try {

                double lat =
                        Double.parseDouble(
                                destinationLatitudeInput
                                        .getText()
                                        .toString()
                                        .trim()
                        );

                double lon =
                        Double.parseDouble(
                                destinationLongitudeInput
                                        .getText()
                                        .toString()
                                        .trim()
                        );

                if (passengerLatitude == 0.0
                        && passengerLongitude == 0.0) {

                    toast(
                            "Waiting for GPS location."
                    );

                    startPassengerLocation();

                    return;
                }

                destinationLatitude = lat;
                destinationLongitude = lon;

                rideDistanceKm =
                        calculateDistanceKm(
                                passengerLatitude,
                                passengerLongitude,
                                destinationLatitude,
                                destinationLongitude
                        );

                distanceText.setText(
                        String.format(
                                "Distance: %.2f km",
                                rideDistanceKm
                        )
                );

            } catch (Exception e) {

                toast(
                        "Enter valid destination coordinates."
                );
            }
        });

        Button request =
                button(
                        "REQUEST RIDE",
                        ORANGE
                );

        request.setOnClickListener(v -> {

            String p =
                    pickup.getText()
                            .toString()
                            .trim();

            String d =
                    destination.getText()
                            .toString()
                            .trim();

            if (p.isEmpty()) {

                toast(
                        "Enter pickup location."
                );

                return;
            }

            if (d.isEmpty()) {

                toast(
                        "Enter destination."
                );

                return;
            }

            if (passengerLatitude == 0.0
                    && passengerLongitude == 0.0) {

                toast(
                        "Waiting for GPS location."
                );

                startPassengerLocation();

                return;
            }

            try {

                destinationLatitude =
                        Double.parseDouble(
                                destinationLatitudeInput
                                        .getText()
                                        .toString()
                                        .trim()
                        );

                destinationLongitude =
                        Double.parseDouble(
                                destinationLongitudeInput
                                        .getText()
                                        .toString()
                                        .trim()
                        );

            } catch (Exception e) {

                toast(
                        "Enter destination latitude and longitude."
                );

                return;
            }

            rideDistanceKm =
                    calculateDistanceKm(
                            passengerLatitude,
                            passengerLongitude,
                            destinationLatitude,
                            destinationLongitude
                    );

            requestRide(
                    p,
                    d
            );
        });

        Button back =
                button(
                        "BACK",
                        GRAY
                );

        back.setOnClickListener(
                v -> showDashboard()
        );
    }

    private double calculateDistanceKm(
            double startLatitude,
            double startLongitude,
            double endLatitude,
            double endLongitude) {

        float[] result = new float[1];

        Location.distanceBetween(
                startLatitude,
                startLongitude,
                endLatitude,
                endLongitude,
                result
        );

        return result[0] / 1000.0;
    }

    private void requestRide(
            String pickup,
            String destination) {

        if (auth.getCurrentUser() == null) {

            toast(
                    "Please login again."
            );

            return;
        }

        if (!rideId.isEmpty()) {

            db.collection("rides")
                    .document(rideId)
                    .get()
                    .addOnSuccessListener(document -> {

                        if (document.exists()) {

                            String status =
                                    document.getString(
                                            "status"
                                    );

                            if (isActiveStatus(status)) {

                                toast(
                                        "You already have an active ride."
                                );

                                return;
                            }
                        }

                        createRide(
                                pickup,
                                destination
                        );
                    });

            return;
        }

        createRide(
                pickup,
                destination
        );
    }

    private void createRide(
            String pickup,
            String destination) {

        String uid =
                auth.getCurrentUser()
                        .getUid();

        String name =
                getSharedPreferences(
                        "SakayNa",
                        MODE_PRIVATE
                ).getString(
                        "current_name",
                        "Passenger"
                );

        String phone =
                getSharedPreferences(
                        "SakayNa",
                        MODE_PRIVATE
                ).getString(
                        "current_phone",
                        ""
                );

        int calculatedFare =
                50
                        + ((int)
                        Math.ceil(
                                rideDistanceKm
                        ) * 10);

        if (calculatedFare < 50) {
            calculatedFare = 50;
        }

        if (calculatedFare > 500) {
            calculatedFare = 500;
        }

        final int fare =
                calculatedFare;

        Map<String, Object> ride =
                new HashMap<>();

        ride.put(
                "passengerId",
                uid
        );

        ride.put(
                "passengerName",
                name
        );

        ride.put(
                "passengerPhone",
                phone
        );

        ride.put(
                "pickup",
                pickup
        );

        ride.put(
                "destination",
                destination
        );

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

        ride.put(
                "fare",
                fare
        );

        ride.put(
                "finalFare",
                0
        );

        ride.put(
                "status",
                "REQUESTED"
        );

        ride.put(
                "driverId",
                ""
        );

        ride.put(
                "driverName",
                ""
        );

        ride.put(
                "rating",
                0
        );

        ride.put(
                "createdAt",
                FieldValue.serverTimestamp()
        );

        db.collection("rides")
                .add(ride)
                .addOnSuccessListener(
                        documentReference -> {

                            rideId =
                                    documentReference
                                            .getId();

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
                                    .putFloat(
                                            "ride_distance_km",
                                            (float)
                                                    rideDistanceKm
                                    )
                                    .putInt(
                                            "ride_fare",
                                            fare
                                    )
                                    .putString(
                                            "ride_status",
                                            "REQUESTED"
                                    )
                                    .apply();

                            toast(
                                    "Ride requested successfully!"
                            );

                            showCurrentRide();
                        }
                )
                .addOnFailureListener(
                        e -> toast(
                                "Could not request ride: "
                                        + e.getMessage()
                        )
                );
    }

    private void showCurrentRide() {

        setupScreen("Current Ride");

        if (rideId.isEmpty()) {

            root.addView(
                    text(
                            "No current ride.",
                            18
                    )
            );

            Button book =
                    button(
                            "BOOK A RIDE",
                            ORANGE
                    );

            book.setOnClickListener(
                    v -> showBooking()
            );

            Button back =
                    button(
                            "BACK",
                            GRAY
                    );

            back.setOnClickListener(
                    v -> showDashboard()
            );

            return;
        }

        TextView status =
                text(
                        "Loading ride...",
                        18
                );

        root.addView(status);

        listenToRide(status);

        Button driverLocation =
                button(
                        "📍  VIEW DRIVER LOCATION",
                        BLUE
                );

        driverLocation.setOnClickListener(
                v -> showDriverLocation()
        );

        Button refresh =
                button(
                        "REFRESH STATUS",
                        BLUE
                );

        refresh.setOnClickListener(
                v -> loadCurrentRide(status)
        );

        Button cancel =
                button(
                        "CANCEL RIDE",
                        RED
                );

        cancel.setOnClickListener(
                v -> cancelRide()
        );

        Button back =
                button(
                        "BACK",
                        GRAY
                );

        back.setOnClickListener(v -> {

            removeRideListener();
            removeDriverLocationListener();

            showDashboard();
        });

        loadCurrentRide(status);
    }

    private void loadCurrentRide(
            TextView statusView) {

        if (rideId.isEmpty()) {

            statusView.setText(
                    "No current ride."
            );

            return;
        }

        db.collection("rides")
                .document(rideId)
                .get()
                .addOnSuccessListener(
                        document -> {

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
                        }
                )
                .addOnFailureListener(
                        e -> statusView.setText(
                                "Could not load ride."
                        )
                );
    }

    private void listenToRide(
            TextView statusView) {

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

                                    startDriverLocationListener(
                                            snapshot
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

        Double distance =
                document.getDouble("distanceKm");

        if (pickup == null) {
            pickup = "";
        }

        if (destination == null) {
            destination = "";
        }

        if (status == null) {
            status = "UNKNOWN";
        }

        if (driver == null
                || driver.isEmpty()) {

            driver =
                    "Waiting for driver";
        }

        long displayFare =
                fare == null
                        ? 0
                        : fare;

        if (finalFare != null
                && finalFare > 0) {

            displayFare =
                    finalFare;
        }

        String distanceText =
                distance == null
                        ? "Not available"
                        : String.format(
                                "%.2f km",
                                distance
                        );

        statusView.setText(
                "PICKUP\n"
                        + pickup
                        + "\n\n"
                        + "DESTINATION\n"
                        + destination
                        + "\n\n"
                        + "DISTANCE\n"
                        + distanceText
                        + "\n\n"
                        + "FARE\n₱"
                        + displayFare
                        + "\n\n"
                        + "DRIVER\n"
                        + driver
                        + "\n\n"
                        + "STATUS\n"
                        + readableStatus(status)
        );

        statusView.setTextSize(17);
        statusView.setTextColor(DARK);
    }

    private void startDriverLocationListener(
            DocumentSnapshot rideDocument) {

        String driverId =
                rideDocument.getString(
                        "driverId"
                );

        String status =
                rideDocument.getString(
                        "status"
                );

        if (driverId == null
                || driverId.isEmpty()
                || status == null) {

            removeDriverLocationListener();

            return;
        }

        if (status.equals("REQUESTED")
                || status.equals("CANCELLED")
                || status.equals("DECLINED")
                || status.equals("COMPLETED")) {

            removeDriverLocationListener();

            return;
        }

        removeDriverLocationListener();

        driverLocationListener =
                db.collection("driverLocations")
                        .document(driverId)
                        .addSnapshotListener(
                                (snapshot, error) -> {

                                    if (error != null) {
                                        return;
                                    }

                                    if (snapshot == null
                                            || !snapshot.exists()) {
                                        return;
                                    }

                                    Double lat =
                                            snapshot.getDouble(
                                                    "latitude"
                                            );

                                    Double lng =
                                            snapshot.getDouble(
                                                    "longitude"
                                            );

                                    if (lat != null
                                            && lng != null) {

                                        driverLatitude =
                                                lat;

                                        driverLongitude =
                                                lng;

                                        saveDriverLocationLocally(
                                                lat,
                                                lng
                                        );
                                    }
                                }
                        );
    }

    private void showDriverLocation() {

        setupScreen(
                "Driver Location"
        );

        TextView locationText =
                text(
                        "Waiting for driver location...",
                        18
                );

        root.addView(locationText);

        if (rideId.isEmpty()) {

            locationText.setText(
                    "No active ride.\n\n"
                            + "Book a ride first."
            );

        } else {

            loadDriverLocationForScreen(
                    locationText
            );
        }

        Button refresh =
                button(
                        "REFRESH LOCATION",
                        BLUE
                );

        refresh.setOnClickListener(
                v -> loadDriverLocationForScreen(
                        locationText
                )
        );

        Button back =
                button(
                        "BACK",
                        GRAY
                );

        back.setOnClickListener(v -> {

            removeDriverLocationListener();

            showDashboard();
        });
    }

    private void loadDriverLocationForScreen(
            TextView locationText) {

        if (rideId.isEmpty()) {

            locationText.setText(
                    "No active ride."
            );

            return;
        }

        db.collection("rides")
                .document(rideId)
                .get()
                .addOnSuccessListener(
                        ride -> {

                            if (!ride.exists()) {

                                locationText.setText(
                                        "Ride not found."
                                );

                                return;
                            }

                            String driverId =
                                    ride.getString(
                                            "driverId"
                                    );

                            if (driverId == null
                                    || driverId.isEmpty()) {

                                locationText.setText(
                                        "No driver has accepted "
                                                + "the ride yet.\n\n"
                                                + "Driver GPS will appear "
                                                + "after acceptance."
                                );

                                return;
                            }

                            db.collection(
                                            "driverLocations"
                                    )
                                    .document(driverId)
                                    .get()
                                    .addOnSuccessListener(
                                            location -> {

                                                if (!location.exists()) {

                                                    locationText.setText(
                                                            "Driver location "
                                                                    + "is not available yet."
                                                    );

                                                    return;
                                                }

                                                Double lat =
                                                        location.getDouble(
                                                                "latitude"
                                                        );

                                                Double lng =
                                                        location.getDouble(
                                                                "longitude"
                                                        );

                                                if (lat == null
                                                        || lng == null) {

                                                    locationText.setText(
                                                            "Driver GPS "
                                                                    + "coordinates are "
                                                                    + "not available yet."
                                                    );

                                                    return;
                                                }

                                                driverLatitude =
                                                        lat;

                                                driverLongitude =
                                                        lng;

                                                locationText.setText(
                                                        "🚕 DRIVER GPS\n\n"
                                                                + "Latitude:\n"
                                                                + lat
                                                                + "\n\n"
                                                                + "Longitude:\n"
                                                                + lng
                                                                + "\n\n"
                                                                + "Driver location received."
                                                );

                                                locationText.setTextSize(
                                                        17
                                                );
                                            }
                                    );
                        }
                )
                .addOnFailureListener(
                        e -> locationText.setText(
                                "Could not load driver location."
                        )
                );
    }

    private void showPassengerLocation() {

        setupScreen(
                "My GPS Location"
        );

        TextView gps =
                text(
                        "Getting your real GPS location...",
                        18
                );

        root.addView(gps);

        updatePassengerGpsStatus(gps);

        Button refresh =
                button(
                        "REFRESH GPS",
                        GREEN
                );

        refresh.setOnClickListener(v -> {

            startPassengerLocation();

            updatePassengerGpsStatus(
                    gps
            );
        });

        Button back =
                button(
                        "BACK",
                        GRAY
                );

        back.setOnClickListener(
                v -> showDashboard()
        );
    }

    private void updatePassengerGpsStatus(
            TextView view) {

        if (passengerLatitude == 0.0
                && passengerLongitude == 0.0) {

            view.setText(
                    "📍 Waiting for GPS...\n\n"
                            + "Make sure Location is ON."
            );

            return;
        }

        view.setText(
                "📍 GPS ACTIVE\n\n"
                        + "Latitude:\n"
                        + passengerLatitude
                        + "\n\n"
                        + "Longitude:\n"
                        + passengerLongitude
        );
    }

    private void startPassengerLocation() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(
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

        try {

            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    3000,
                    5,
                    passengerLocationListener
            );

            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    5000,
                    10,
                    passengerLocationListener
            );

            Location lastGps =
                    locationManager.getLastKnownLocation(
                            LocationManager.GPS_PROVIDER
                    );

            if (lastGps != null) {

                updatePassengerCoordinates(
                        lastGps
                );
            }

            Location lastNetwork =
                    locationManager.getLastKnownLocation(
                            LocationManager.NETWORK_PROVIDER
                    );

            if (lastNetwork != null) {

                updatePassengerCoordinates(
                        lastNetwork
                );
            }

        } catch (SecurityException e) {

            toast(
                    "Location permission is required."
            );
        }
    }

    private final LocationListener passengerLocationListener =
            new LocationListener() {

                @Override
                public void onLocationChanged(
                        Location location) {

                    updatePassengerCoordinates(
                            location
                    );
                }
            };

    private void updatePassengerCoordinates(
            Location location) {

        if (location == null) {
            return;
        }

        passengerLatitude =
                location.getLatitude();

        passengerLongitude =
                location.getLongitude();
    }

    private void saveDriverLocationLocally(
            double latitude,
            double longitude) {

        getSharedPreferences(
                "SakayNa",
                MODE_PRIVATE
        )
                .edit()
                .putString(
                        "driver_latitude",
                        String.valueOf(latitude)
                )
                .putString(
                        "driver_longitude",
                        String.valueOf(longitude)
                )
                .apply();
    }

    private String readableStatus(
            String status) {

        if (status == null) {
            return "UNKNOWN";
        }

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

    private boolean isActiveStatus(
            String status) {

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

            toast(
                    "No active ride."
            );

            return;
        }

        db.collection("rides")
                .document(rideId)
                .get()
                .addOnSuccessListener(
                        document -> {

                            if (!document.exists()) {

                                toast(
                                        "Ride not found."
                                );

                                return;
                            }

                            String status =
                                    document.getString(
                                            "status"
                                    );

                            if (status == null) {
                                return;
                            }

                            if (!status.equals(
                                    "REQUESTED"
                            )) {

                                toast(
                                        "This ride can no longer "
                                                + "be cancelled here."
                                );

                                return;
                            }

                            Map<String, Object> update =
                                    new HashMap<>();

                            update.put(
                                    "status",
                                    "CANCELLED"
                            );

                            db.collection("rides")
                                    .document(rideId)
                                    .update(update)
                                    .addOnSuccessListener(
                                            unused -> {

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

                                                toast(
                                                        "Ride cancelled."
                                                );
                                            }
                                    )
                                    .addOnFailureListener(
                                            e -> toast(
                                                    "Could not cancel ride."
                                            )
                                    );
                        }
                );
    }

    private void showHistory() {

        setupScreen(
                "Ride History"
        );

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
                text(
                        "Loading ride history...",
                        17
                );

        root.addView(history);

        db.collection("rides")
                .whereEqualTo(
                        "passengerId",
                        auth.getCurrentUser()
                                .getUid()
                )
                .get()
                .addOnSuccessListener(
                        result -> {

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
                                        document.getString(
                                                "pickup"
                                        );

                                String destination =
                                        document.getString(
                                                "destination"
                                        );

                                String status =
                                        document.getString(
                                                "status"
                                        );

                                Long fare =
                                        document.getLong(
                                                "fare"
                                        );

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
                                                readableStatus(
                                                        status
                                                )
                                        )
                                        .append("\n");

                                builder.append(
                                        "--------------------\n"
                                );
                            }

                            history.setText(
                                    builder.toString()
                            );

                            history.setTextSize(
                                    16
                            );

                            history.setGravity(
                                    Gravity.START
                            );
                        }
                )
                .addOnFailureListener(
                        e -> history.setText(
                                "Could not load history."
                        )
                );

        Button back =
                button(
                        "BACK",
                        GRAY
                );

        back.setOnClickListener(
                v -> showDashboard()
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

    private void removeDriverLocationListener() {

        if (driverLocationListener != null) {

            driverLocationListener.remove();
            driverLocationListener = null;
        }
    }

    private void logout() {

        removeRideListener();
        removeDriverLocationListener();
        stopPassengerLocation();

        auth.signOut();

        getSharedPreferences(
                "SakayNa",
                MODE_PRIVATE
        )
                .edit()
                .clear()
                .apply();

        Intent intent =
                new Intent(
                        PassengerActivity.this,
                        MainActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        finish();
    }

    private void stopPassengerLocation() {

        if (locationManager != null) {

            try {

                locationManager.removeUpdates(
                        passengerLocationListener
                );

            } catch (SecurityException ignored) {
            }
        }
    }

    private void toast(String message) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode ==
                LOCATION_PERMISSION_REQUEST) {

            if (grantResults.length > 0
                    && grantResults[0]
                    == PackageManager.PERMISSION_GRANTED) {

                startPassengerLocation();

                toast(
                        "GPS permission granted."
                );

            } else {

                toast(
                        "GPS permission is required for ride location."
                );
            }
        }
    }

    @Override
    protected void onDestroy() {

        removeRideListener();
        removeDriverLocationListener();
        stopPassengerLocation();

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {

        removeRideListener();
        removeDriverLocationListener();

        showDashboard();
    }
}
