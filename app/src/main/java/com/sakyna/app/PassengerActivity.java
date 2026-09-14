
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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;

public class PassengerActivity extends Activity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private LocationManager locationManager;

    private double passengerLatitude = 0.0;
    private double passengerLongitude = 0.0;

    private double destinationLatitude = 0.0;
    private double destinationLongitude = 0.0;

    private String currentRideId = "";

    private ListenerRegistration rideListener;
    private ListenerRegistration driverLocationListener;

    private EditText pickupInput;
    private EditText destinationInput;
    private EditText destinationLatitudeInput;
    private EditText destinationLongitudeInput;

    private TextView gpsText;
    private TextView fareText;
    private TextView rideStatusText;

    private RadioGroup paymentGroup;

    private double baseFare = 50.0;
    private double perKm = 10.0;
    private double minimumFare = 50.0;
    private double maximumFare = 500.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        showDashboard();

        loadFareSettings();

        startLocation();

        findCurrentRide();
    }

    private void showDashboard() {

        ScrollView scrollView =
                new ScrollView(this);

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setPadding(
                25,
                30,
                25,
                30
        );

        root.setBackgroundColor(
                Color.WHITE
        );

        scrollView.addView(root);

        TextView title =
                text(
                        "🛺 SAKAY NA",
                        30,
                        Color.BLACK
                );

        title.setGravity(
                Gravity.CENTER
        );

        root.addView(title);

        TextView heading =
                text(
                        "PASSENGER",
                        23,
                        Color.rgb(20, 120, 70)
                );

        heading.setGravity(
                Gravity.CENTER
        );

        root.addView(heading);

        TextView welcome =
                text(
                        "Book a tricycle ride from A to B.",
                        15,
                        Color.DKGRAY
                );

        welcome.setGravity(
                Gravity.CENTER
        );

        root.addView(welcome);

        addSpace(root, 15);

        gpsText =
                text(
                        "📍 GPS: Waiting...",
                        15,
                        Color.DKGRAY
                );

        root.addView(gpsText);

        addSpace(root, 15);

        TextView bookingTitle =
                text(
                        "🚕 BOOK A RIDE",
                        20,
                        Color.BLACK
                );

        root.addView(bookingTitle);

        pickupInput =
                input(
                        "Pickup location"
                );

        root.addView(pickupInput);

        destinationInput =
                input(
                        "Destination"
                );

        root.addView(destinationInput);

        destinationLatitudeInput =
                input(
                        "Destination latitude"
                );

        root.addView(
                destinationLatitudeInput
        );

        destinationLongitudeInput =
                input(
                        "Destination longitude"
                );

        root.addView(
                destinationLongitudeInput
        );

        addSpace(root, 10);

        TextView paymentTitle =
                text(
                        "💳 PAYMENT METHOD",
                        20,
                        Color.BLACK
                );

        root.addView(paymentTitle);

        paymentGroup =
                new RadioGroup(this);

        paymentGroup.setOrientation(
                RadioGroup.VERTICAL
        );

        RadioButton cash =
                new RadioButton(this);

        cash.setText(
                "💵 Cash"
        );

        cash.setTextSize(17);

        cash.setTag(
                "CASH"
        );

        paymentGroup.addView(cash);

        RadioButton gcash =
                new RadioButton(this);

        gcash.setText(
                "📱 E-Wallet - GCash"
        );

        gcash.setTextSize(17);

        gcash.setTag(
                "GCASH"
        );

        paymentGroup.addView(gcash);

        RadioButton maya =
                new RadioButton(this);

        maya.setText(
                "📱 E-Wallet - Maya / PayMaya"
        );

        maya.setTextSize(17);

        maya.setTag(
                "MAYA"
        );

        paymentGroup.addView(maya);

        cash.setChecked(true);

        root.addView(paymentGroup);

        addSpace(root, 10);

        fareText =
                text(
                        "💰 Fare: Loading...",
                        19,
                        Color.rgb(20, 120, 70)
                );

        root.addView(fareText);

        Button calculate =
                button(
                        "🧮 CALCULATE FARE"
                );

        calculate.setOnClickListener(
                v -> calculateAndShowFare()
        );

        root.addView(calculate);

        Button book =
                button(
                        "🚕 BOOK RIDE"
                );

        book.setOnClickListener(
                v -> bookRide()
        );

        root.addView(book);

        addSpace(root, 20);

        TextView currentTitle =
                text(
                        "📋 CURRENT RIDE",
                        20,
                        Color.BLACK
                );

        root.addView(currentTitle);

        rideStatusText =
                text(
                        "No active ride.",
                        16,
                        Color.DKGRAY
                );

        root.addView(
                rideStatusText
        );

        Button liveMap =
                button(
                        "🗺️ LIVE RIDE MAP"
                );

        liveMap.setOnClickListener(
                v -> openLiveRideMap()
        );

        root.addView(liveMap);

        Button cancel =
                button(
                        "❌ CANCEL CURRENT RIDE"
                );

        cancel.setOnClickListener(
                v -> cancelRide()
        );

        root.addView(cancel);

        Button history =
                button(
                        "📜 RIDE HISTORY"
                );

        history.setOnClickListener(
                v -> loadRideHistory()
        );

        root.addView(history);

        Button logout =
                button(
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

                            if (document.exists()) {

                                Double savedBase =
                                        document.getDouble(
                                                "baseFare"
                                        );

                                Double savedPerKm =
                                        document.getDouble(
                                                "perKm"
                                        );

                                Double savedMinimum =
                                        document.getDouble(
                                                "minimumFare"
                                        );

                                Double savedMaximum =
                                        document.getDouble(
                                                "maximumFare"
                                        );

                                if (savedBase != null) {
                                    baseFare =
                                            savedBase;
                                }

                                if (savedPerKm != null) {
                                    perKm =
                                            savedPerKm;
                                }

                                if (savedMinimum != null) {
                                    minimumFare =
                                            savedMinimum;
                                }

                                if (savedMaximum != null) {
                                    maximumFare =
                                            savedMaximum;
                                }
                            }

                            fareText.setText(
                                    "💰 Fare: Base ₱" +
                                    money(baseFare) +
                                    " + ₱" +
                                    money(perKm) +
                                    "/km"
                            );
                        }
                )
                .addOnFailureListener(
                        e -> {

                            fareText.setText(
                                    "💰 Fare: ₱" +
                                    money(baseFare) +
                                    " base + ₱" +
                                    money(perKm) +
                                    "/km"
                            );
                        }
                );
    }

    private void startLocation() {

        locationManager =
                (LocationManager)
                        getSystemService(
                                LOCATION_SERVICE
                        );

        if (checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    100
            );

            return;
        }

        try {

            Location lastGps =
                    locationManager.getLastKnownLocation(
                            LocationManager.GPS_PROVIDER
                    );

            if (lastGps != null) {
                updatePassengerLocation(
                        lastGps
                );
            }

            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    3000,
                    5,
                    locationListener
            );

            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    5000,
                    10,
                    locationListener
            );

        } catch (SecurityException e) {

            gpsText.setText(
                    "📍 GPS permission denied."
            );
        }
    }

    private final LocationListener locationListener =
            new LocationListener() {

                @Override
                public void onLocationChanged(
                        Location location) {

                    updatePassengerLocation(
                            location
                    );
                }

                @Override
                public void onProviderEnabled(
                        String provider) {
                }

                @Override
                public void onProviderDisabled(
                        String provider) {
                }
            };

    private void updatePassengerLocation(
            Location location) {

        passengerLatitude =
                location.getLatitude();

        passengerLongitude =
                location.getLongitude();

        gpsText.setText(
                "📍 GPS: " +
                String.format(
                        "%.6f, %.6f",
                        passengerLatitude,
                        passengerLongitude
                )
        );
    }

    private void calculateAndShowFare() {

        String latText =
                destinationLatitudeInput
                        .getText()
                        .toString()
                        .trim();

        String lonText =
                destinationLongitudeInput
                        .getText()
                        .toString()
                        .trim();

        if (latText.isEmpty() ||
                lonText.isEmpty()) {

            showMessage(
                    "Enter destination latitude and longitude."
            );

            return;
        }

        try {

            destinationLatitude =
                    Double.parseDouble(
                            latText
                    );

            destinationLongitude =
                    Double.parseDouble(
                            lonText
                    );

        } catch (NumberFormatException e) {

            showMessage(
                    "Destination coordinates are invalid."
            );

            return;
        }

        double distanceKm =
                calculateDistanceKm(
                        passengerLatitude,
                        passengerLongitude,
                        destinationLatitude,
                        destinationLongitude
                );

        double calculatedFare =
                baseFare +
                (Math.ceil(distanceKm) * perKm);

        if (calculatedFare < minimumFare) {
            calculatedFare =
                    minimumFare;
        }

        if (calculatedFare > maximumFare) {
            calculatedFare =
                    maximumFare;
        }

        fareText.setText(
                "💰 Estimated fare: ₱" +
                money(calculatedFare) +
                "\nDistance: " +
                String.format(
                        "%.2f km",
                        distanceKm
                )
        );
    }

    private void bookRide() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {

            showMessage(
                    "Please login first."
            );

            return;
        }

        String pickup =
                pickupInput
                        .getText()
                        .toString()
                        .trim();

        String destination =
                destinationInput
                        .getText()
                        .toString()
                        .trim();

        String latText =
                destinationLatitudeInput
                        .getText()
                        .toString()
                        .trim();

        String lonText =
                destinationLongitudeInput
                        .getText()
                        .toString()
                        .trim();

        if (pickup.isEmpty()) {

            pickup =
                    "Current GPS location";
        }

        if (destination.isEmpty()) {

            showMessage(
                    "Enter destination."
            );

            return;
        }

        if (latText.isEmpty() ||
                lonText.isEmpty()) {

            showMessage(
                    "Enter destination coordinates."
            );

            return;
        }

        try {

            destinationLatitude =
                    Double.parseDouble(
                            latText
                    );

            destinationLongitude =
                    Double.parseDouble(
                            lonText
                    );

        } catch (NumberFormatException e) {

            showMessage(
                    "Invalid destination coordinates."
            );

            return;
        }

        if (passengerLatitude == 0.0 &&
                passengerLongitude == 0.0) {

            showMessage(
                    "Waiting for your GPS location."
            );

            return;
        }

        String paymentMethod =
                getSelectedPaymentMethod();

        double distanceKm =
                calculateDistanceKm(
                        passengerLatitude,
                        passengerLongitude,
                        destinationLatitude,
                        destinationLongitude
                );

        double calculatedFare =
                baseFare +
                (Math.ceil(distanceKm) * perKm);

        if (calculatedFare < minimumFare) {
            calculatedFare =
                    minimumFare;
        }

        if (calculatedFare > maximumFare) {
            calculatedFare =
                    maximumFare;
        }

        Map<String, Object> ride =
                new HashMap<>();

        ride.put(
                "passengerId",
                user.getUid()
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
                "passengerLatitude",
                passengerLatitude
        );

        ride.put(
                "passengerLongitude",
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
                distanceKm
        );

        ride.put(
                "fare",
                calculatedFare
        );

        ride.put(
                "paymentMethod",
                paymentMethod
        );

        ride.put(
                "status",
                "REQUESTED"
        );

        ride.put(
                "createdAt",
                com.google.firebase.firestore.FieldValue
                        .serverTimestamp()
        );

        db.collection("rides")
                .add(ride)
                .addOnSuccessListener(
                        documentReference -> {

                            currentRideId =
                                    documentReference.getId();

                            showMessage(
                                    "🟢 Ride requested.\n" +
                                    "Payment: " +
                                    paymentLabel(
                                            paymentMethod
                                    )
                            );

                            listenToCurrentRide();
                        }
                )
                .addOnFailureListener(
                        e -> showMessage(
                                "Ride booking failed: " +
                                e.getMessage()
                        )
                );
    }

    private String getSelectedPaymentMethod() {

        int checkedId =
                paymentGroup.getCheckedRadioButtonId();

        if (checkedId == -1) {
            return "CASH";
        }

        RadioButton selected =
                findViewById(
                        checkedId
                );

        Object tag =
                selected.getTag();

        if (tag == null) {
            return "CASH";
        }

        return tag.toString();
    }

    private String paymentLabel(
            String method) {

        if ("GCASH".equals(method)) {
            return "GCash";
        }

        if ("MAYA".equals(method)) {
            return "Maya / PayMaya";
        }

        return "Cash";
    }

    private void findCurrentRide() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            return;
        }

        db.collection("rides")
                .whereEqualTo(
                        "passengerId",
                        user.getUid()
                )
                .whereEqualTo(
                        "status",
                        "REQUESTED"
                )
                .limit(1)
                .get()
                .addOnSuccessListener(
                        snapshot -> {

                            if (!snapshot.isEmpty()) {

                                DocumentSnapshot document =
                                        snapshot.getDocuments()
                                                .get(0);

                                currentRideId =
                                        document.getId();

                                listenToCurrentRide();
                            }
                        }
                );
    }

    private void listenToCurrentRide() {

        if (currentRideId == null ||
                currentRideId.isEmpty()) {

            return;
        }

        if (rideListener != null) {
            rideListener.remove();
        }

        rideListener =
                db.collection("rides")
                        .document(currentRideId)
                        .addSnapshotListener(
                                (document, error) -> {

                                    if (error != null) {

                                        rideStatusText.setText(
                                                "Ride error: " +
                                                error.getMessage()
                                        );

                                        return;
                                    }

                                    if (document == null ||
                                            !document.exists()) {

                                        rideStatusText.setText(
                                                "Ride not found."
                                        );

                                        return;
                                    }

                                    updateRideStatus(
                                            document
                                    );
                                }
                        );
    }

    private void updateRideStatus(
            DocumentSnapshot document) {

        String status =
                document.getString(
                        "status"
                );

        String paymentMethod =
                document.getString(
                        "paymentMethod"
                );

        Double fare =
                document.getDouble(
                        "fare"
                );

        String driverId =
                document.getString(
                        "driverId"
                );

        StringBuilder text =
                new StringBuilder();

        text.append(
                "Ride: "
        ).append(
                formatStatus(status)
        );

        if (fare != null) {

            text.append(
                    "\nFare: ₱"
            ).append(
                    money(fare)
            );
        }

        if (paymentMethod != null) {

            text.append(
                    "\nPayment: "
            ).append(
                    paymentLabel(
                            paymentMethod
                    )
            );
        }

        if (driverId != null &&
                !driverId.isEmpty()) {

            text.append(
                    "\nDriver assigned."
            );
        }

        rideStatusText.setText(
                text.toString()
        );

        if (driverId != null &&
                !driverId.isEmpty()) {

            listenToDriverLocation(
                    driverId
            );
        }
    }

    private void listenToDriverLocation(
            String driverId) {

        if (driverLocationListener != null) {

            driverLocationListener.remove();
        }

        driverLocationListener =
                db.collection(
                        "driverLocations"
                )
                .document(driverId)
                .addSnapshotListener(
                        (document, error) -> {

                            if (error != null ||
                                    document == null ||
                                    !document.exists()) {

                                return;
                            }

                            Double latitude =
                                    document.getDouble(
                                            "latitude"
                                    );

                            Double longitude =
                                    document.getDouble(
                                            "longitude"
                                    );

                            if (latitude == null ||
                                    longitude == null) {

                                return;
                            }

                            rideStatusText.append(
                                    "\n📍 Driver: " +
                                    String.format(
                                            "%.6f, %.6f",
                                            latitude,
                                            longitude
                                    )
                            );
                        }
                );
    }

    private void openLiveRideMap() {

        if (currentRideId == null ||
                currentRideId.isEmpty()) {

            showMessage(
                    "No active ride."
            );

            return;
        }

        Intent intent =
                new Intent(
                        this,
                        MapActivity.class
                );

        intent.putExtra(
                "ride_id",
                currentRideId
        );

        intent.putExtra(
                "passenger_latitude",
                passengerLatitude
        );

        intent.putExtra(
                "passenger_longitude",
                passengerLongitude
        );

        intent.putExtra(
                "destination_latitude",
                destinationLatitude
        );

        intent.putExtra(
                "destination_longitude",
                destinationLongitude
        );

        startActivity(intent);
    }

    private void cancelRide() {

        if (currentRideId == null ||
                currentRideId.isEmpty()) {

            showMessage(
                    "No active ride."
            );

            return;
        }

        db.collection("rides")
                .document(currentRideId)
                .update(
                        "status",
                        "CANCELLED"
                )
                .addOnSuccessListener(
                        unused -> {

                            showMessage(
                                    "Ride cancelled."
                            );

                            rideStatusText.setText(
                                    "Ride cancelled."
                            );
                        }
                )
                .addOnFailureListener(
                        e -> showMessage(
                                "Cancel failed: " +
                                e.getMessage()
                        )
                );
    }

    private void loadRideHistory() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            return;
        }

        db.collection("rides")
                .whereEqualTo(
                        "passengerId",
                        user.getUid()
                )
                .get()
                .addOnSuccessListener(
                        snapshot -> {

                            StringBuilder history =
                                    new StringBuilder();

                            history.append(
                                    "📜 RIDE HISTORY\n\n"
                            );

                            if (snapshot.isEmpty()) {

                                history.append(
                                        "No rides yet."
                                );

                            } else {

                                for (DocumentSnapshot document :
                                        snapshot.getDocuments()) {

                                    String status =
                                            document.getString(
                                                    "status"
                                            );

                                    String destination =
                                            document.getString(
                                                    "destination"
                                            );

                                    Double fare =
                                            document.getDouble(
                                                    "fare"
                                            );

                                    String payment =
                                            document.getString(
                                                    "paymentMethod"
                                            );

                                    history.append(
                                            "Status: "
                                    ).append(
                                            formatStatus(
                                                    status
                                            )
                                    );

                                    if (destination != null) {

                                        history.append(
                                                "\nDestination: "
                                        ).append(
                                                destination
                                        );
                                    }

                                    if (fare != null) {

                                        history.append(
                                                "\nFare: ₱"
                                        ).append(
                                                money(fare)
                                        );
                                    }

                                    if (payment != null) {

                                        history.append(
                                                "\nPayment: "
                                        ).append(
                                                paymentLabel(
                                                        payment
                                                )
                                        );
                                    }

                                    history.append(
                                            "\n\n"
                                    );
                                }
                            }

                            showMessage(
                                    history.toString()
                            );
                        }
                )
                .addOnFailureListener(
                        e -> showMessage(
                                "History failed: " +
                                e.getMessage()
                        )
                );
    }

    private double calculateDistanceKm(
            double lat1,
            double lon1,
            double lat2,
            double lon2) {

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

    private String formatStatus(
            String status) {

        if (status == null ||
                status.isEmpty()) {

            return "UNKNOWN";
        }

        return status.replace(
                "_",
                " "
        );
    }

    private String money(
            double value) {

        return String.format(
                "%.2f",
                value
        );
    }

    private EditText input(
            String hint) {

        EditText input =
                new EditText(this);

        input.setHint(hint);
        input.setTextSize(16);
        input.setSingleLine(true);

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

        input.setLayoutParams(
                params
        );

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

        parent.addView(
                space
        );
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

        if (rideListener != null) {
            rideListener.remove();
        }

        if (driverLocationListener != null) {
            driverLocationListener.remove();
        }

        if (locationManager != null) {

            try {

                locationManager.removeUpdates(
                        locationListener
                );

            } catch (SecurityException ignored) {
            }
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

    @Override
    protected void onDestroy() {

        if (rideListener != null) {
            rideListener.remove();
        }

        if (driverLocationListener != null) {
            driverLocationListener.remove();
        }

        if (locationManager != null) {

            try {

                locationManager.removeUpdates(
                        locationListener
                );

            } catch (SecurityException ignored) {
            }
        }

        super.onDestroy();
    }
}
