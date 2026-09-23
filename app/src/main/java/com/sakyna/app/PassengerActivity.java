
package com.sakyna.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
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

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Source;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PassengerActivity extends Activity {

    private static final int LOCATION_REQUEST = 1001;
    private static final int DESTINATION_REQUEST = 2001;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private SharedPreferences prefs;

    private EditText pickupInput;
    private EditText destinationInput;

    private TextView fareText;
    private TextView statusText;
    private TextView driverInfoText;

    private Button bookButton;
    private Button mapButton;
    private Button chatButton;
    private Button cancelButton;

    private LocationManager locationManager;
    private LocationListener locationListener;
    private ListenerRegistration rideListener;

    private String activeRideId = null;

    /*
     * Prevents the same ride status from showing
     * the same local notification repeatedly.
     */
    private String lastNotifiedRideStatus = "";

    private double pickupLat = 0;
    private double pickupLng = 0;
    private double destinationLat = 0;
    private double destinationLng = 0;

    private double baseFare = 25;
    private double perKm = 10;
    private double minimumFare = 25;
    private double maximumFare = 500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        prefs = getSharedPreferences("SakayNa", MODE_PRIVATE);

        buildScreen();
        loadFare();
        requestLocation();
        restoreSavedRide();
    }

    private void buildScreen() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(245, 248, 246));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(Gravity.CENTER);
        header.setPadding(12, 18, 12, 18);
        header.setBackgroundColor(Color.rgb(0, 125, 75));

        TextView title = new TextView(this);
        title.setText("🛺 SAKAY NA");
        title.setTextSize(28);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        header.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Passenger • Tricycle Ride");
        subtitle.setTextSize(14);
        subtitle.setTextColor(Color.WHITE);
        subtitle.setGravity(Gravity.CENTER);
        header.addView(subtitle);

        root.addView(header, full());

        ScrollView scroll = new ScrollView(this);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(16, 12, 16, 25);

        TextView heading = text("🚕 BOOK YOUR RIDE", 22);
        heading.setTypeface(null, Typeface.BOLD);
        heading.setGravity(Gravity.CENTER);
        heading.setTextColor(Color.rgb(0, 110, 70));
        content.addView(heading, full());

        TextView pickupLabel = text("📍 PICKUP LOCATION", 15);
        pickupLabel.setTypeface(null, Typeface.BOLD);
        content.addView(pickupLabel);

        pickupInput = new EditText(this);
        pickupInput.setHint("Pickup place");
        pickupInput.setTextSize(16);
        pickupInput.setSingleLine(false);
        pickupInput.setPadding(16, 12, 16, 12);
        pickupInput.setBackgroundColor(Color.WHITE);
        content.addView(pickupInput, full());

        Button gpsButton = button("📍 USE MY CURRENT LOCATION");
        gpsButton.setOnClickListener(v -> useCurrentLocation());
        content.addView(gpsButton);

        TextView destinationLabel = text("🎯 DESTINATION", 15);
        destinationLabel.setTypeface(null, Typeface.BOLD);
        content.addView(destinationLabel);

        destinationInput = new EditText(this);
        destinationInput.setHint("Example: Jollibee, SM City Imus");
        destinationInput.setTextSize(16);
        destinationInput.setSingleLine(false);
        destinationInput.setPadding(16, 12, 16, 12);
        destinationInput.setBackgroundColor(Color.WHITE);
        content.addView(destinationInput, full());

        Button destinationButton =
                button("🗺️ SEARCH / CHOOSE DESTINATION");

        destinationButton.setOnClickListener(
                v -> chooseDestination()
        );

        content.addView(destinationButton);

        TextView rideType = text("🛺 RIDE TYPE", 15);
        rideType.setTypeface(null, Typeface.BOLD);
        content.addView(rideType);

        TextView tricycle = text("🛺 TRICYCLE", 18);
        tricycle.setTypeface(null, Typeface.BOLD);
        tricycle.setGravity(Gravity.CENTER);
        tricycle.setTextColor(Color.rgb(0, 110, 70));
        tricycle.setBackgroundColor(Color.WHITE);
        content.addView(tricycle, full());

        TextView paymentLabel =
                text("💳 PAYMENT METHOD", 15);

        paymentLabel.setTypeface(null, Typeface.BOLD);
        content.addView(paymentLabel);

        RadioGroup payment = new RadioGroup(this);
        payment.setOrientation(RadioGroup.HORIZONTAL);
        payment.setGravity(Gravity.CENTER);

        RadioButton cash = new RadioButton(this);
        cash.setText("Cash");
        cash.setChecked(true);
        payment.addView(cash);

        RadioButton gcash = new RadioButton(this);
        gcash.setText("GCash");
        payment.addView(gcash);

        RadioButton maya = new RadioButton(this);
        maya.setText("Maya");
        payment.addView(maya);

        content.addView(payment, full());

        fareText = text("💰 Estimated fare: ₱25", 23);
        fareText.setTypeface(null, Typeface.BOLD);
        fareText.setGravity(Gravity.CENTER);
        fareText.setTextColor(Color.rgb(0, 125, 75));
        fareText.setBackgroundColor(Color.WHITE);
        content.addView(fareText, full());

        statusText = text("🟢 Ready to book", 16);
        statusText.setTypeface(null, Typeface.BOLD);
        statusText.setGravity(Gravity.CENTER);
        content.addView(statusText, full());

        driverInfoText =
                text("👤 DRIVER: Waiting for driver...", 16);

        driverInfoText.setTypeface(null, Typeface.BOLD);
        driverInfoText.setTextColor(Color.rgb(0, 100, 70));
        driverInfoText.setBackgroundColor(Color.WHITE);
        driverInfoText.setPadding(16, 16, 16, 16);

        content.addView(driverInfoText, full());

        bookButton = button("🛺 BOOK A RIDE");
        bookButton.setTextSize(19);
        bookButton.setBackgroundColor(Color.rgb(0, 150, 80));
        bookButton.setOnClickListener(
                v -> bookRide(payment)
        );
        content.addView(bookButton);

        mapButton = button("🗺️ LIVE RIDE MAP");
        mapButton.setOnClickListener(
                v -> openLiveMap()
        );
        content.addView(mapButton);

        chatButton = button("💬 CHAT WITH DRIVER");
        chatButton.setOnClickListener(
                v -> openChat()
        );
        content.addView(chatButton);

        cancelButton = button("❌ CANCEL RIDE");
        cancelButton.setBackgroundColor(
                Color.rgb(190, 55, 55)
        );
        cancelButton.setOnClickListener(
                v -> cancelRide()
        );
        content.addView(cancelButton);

        Button historyButton = button("📜 RIDE HISTORY");
        historyButton.setBackgroundColor(
                Color.rgb(65, 90, 100)
        );
        historyButton.setOnClickListener(
                v -> showHistory()
        );
        content.addView(historyButton);

        Button logoutButton = button("🚪 LOGOUT");
        logoutButton.setBackgroundColor(
                Color.rgb(85, 85, 85)
        );
        logoutButton.setOnClickListener(
                v -> logout()
        );
        content.addView(logoutButton);

        scroll.addView(content);

        root.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                )
        );

        setContentView(root);

        updateButtons();
    }

    private TextView text(String value, float size) {

        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(Color.rgb(45, 55, 50));
        t.setPadding(0, 7, 0, 7);

        return t;
    }

    private Button button(String value) {

        Button b = new Button(this);
        b.setText(value);
        b.setTextSize(16);
        b.setAllCaps(false);
        b.setTypeface(null, Typeface.BOLD);
        b.setTextColor(Color.WHITE);
        b.setGravity(Gravity.CENTER);
        b.setPadding(10, 12, 10, 12);
        b.setBackgroundColor(Color.rgb(0, 125, 75));

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(-1, -2);

        p.setMargins(0, 5, 0, 5);
        b.setLayoutParams(p);

        return b;
    }

    private LinearLayout.LayoutParams full() {

        return new LinearLayout.LayoutParams(
                -1,
                -2
        );
    }

    private void loadFare() {

        db.collection("settings")
                .document("fare")
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (!snapshot.exists()) {
                        calculateFare();
                        return;
                    }

                    baseFare =
                            number(snapshot, "baseFare", 25);

                    perKm =
                            number(snapshot, "perKm", 10);

                    minimumFare =
                            number(snapshot, "minimum", 25);

                    maximumFare =
                            number(snapshot, "maximum", 500);

                    calculateFare();
                })
                .addOnFailureListener(e -> {

                    baseFare = 25;
                    perKm = 10;
                    minimumFare = 25;
                    maximumFare = 500;

                    calculateFare();
                });
    }

    private double number(
            DocumentSnapshot snapshot,
            String field,
            double fallback
    ) {

        try {

            Object value = snapshot.get(field);

            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }

            if (value instanceof String) {

                String text =
                        ((String) value).trim();

                if (!text.isEmpty()) {
                    return Double.parseDouble(text);
                }
            }

        } catch (Exception ignored) {
        }

        return fallback;
    }

    private void requestLocation() {

        if (
                checkSelfPermission(
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
                &&
                checkSelfPermission(
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
        ) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_REQUEST
            );

            return;
        }

        startLocation();
    }

    private void startLocation() {

        locationManager =
                (LocationManager)
                        getSystemService(LOCATION_SERVICE);

        if (locationManager == null) {
            return;
        }

        locationListener =
                new LocationListener() {

                    @Override
                    public void onLocationChanged(
                            @NonNull Location location
                    ) {

                        pickupLat =
                                location.getLatitude();

                        pickupLng =
                                location.getLongitude();

                        if (
                                pickupInput != null
                                &&
                                pickupInput
                                        .getText()
                                        .toString()
                                        .trim()
                                        .isEmpty()
                        ) {

                            String place =
                                    getAddress(
                                            pickupLat,
                                            pickupLng
                                    );

                            if (
                                    place == null
                                    ||
                                    place.trim().isEmpty()
                            ) {

                                place = String.format(
                                        Locale.US,
                                        "%.6f, %.6f",
                                        pickupLat,
                                        pickupLng
                                );
                            }

                            pickupInput.setText(place);
                        }

                        calculateFare();
                    }
                };

        try {

            if (
                    checkSelfPermission(
                            Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    ||
                    checkSelfPermission(
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
            ) {

                if (
                        locationManager.isProviderEnabled(
                                LocationManager.GPS_PROVIDER
                        )
                ) {

                    locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            3000,
                            10,
                            locationListener
                    );
                }

                if (
                        locationManager.isProviderEnabled(
                                LocationManager.NETWORK_PROVIDER
                        )
                ) {

                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            3000,
                            10,
                            locationListener
                    );
                }
            }

        } catch (SecurityException ignored) {
        }
    }

    private void useCurrentLocation() {

        if (pickupLat == 0 && pickupLng == 0) {

            Toast.makeText(
                    this,
                    "Waiting for GPS location...",
                    Toast.LENGTH_SHORT
            ).show();

            requestLocation();
            return;
        }

        String place =
                getAddress(
                        pickupLat,
                        pickupLng
                );

        if (
                place == null
                ||
                place.trim().isEmpty()
        ) {

            place = String.format(
                    Locale.US,
                    "%.6f, %.6f",
                    pickupLat,
                    pickupLng
            );
        }

        pickupInput.setText(place);
        calculateFare();
    }

    private void chooseDestination() {

        Intent intent =
                new Intent(
                        this,
                        MapActivity.class
                );

        intent.putExtra(
                "mode",
                "SELECT_DESTINATION"
        );

        intent.putExtra(
                "pickup_latitude",
                pickupLat
        );

        intent.putExtra(
                "pickup_longitude",
                pickupLng
        );

        intent.putExtra(
                "pickup_address",
                pickupInput.getText()
                        .toString()
        );

        startActivityForResult(
                intent,
                DESTINATION_REQUEST
        );
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (
                requestCode != DESTINATION_REQUEST
                ||
                resultCode != RESULT_OK
                ||
                data == null
        ) {
            return;
        }

        destinationLat =
                data.getDoubleExtra(
                        "destination_latitude",
                        0
                );

        destinationLng =
                data.getDoubleExtra(
                        "destination_longitude",
                        0
                );

        String address =
                data.getStringExtra(
                        "destination_address"
                );

        String name =
                data.getStringExtra(
                        "destinationName"
                );

        if (
                name != null
                &&
                !name.trim().isEmpty()
        ) {
            address = name;
        }

        if (
                address == null
                ||
                address.trim().isEmpty()
        ) {

            address = String.format(
                    Locale.US,
                    "%.6f, %.6f",
                    destinationLat,
                    destinationLng
            );
        }

        destinationInput.setText(address);

        calculateFare();
    }

    private void bookRide(RadioGroup payment) {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {

            Toast.makeText(
                    this,
                    "Please log in first.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (
                activeRideId != null
                &&
                !activeRideId.isEmpty()
        ) {

            Toast.makeText(
                    this,
                    "You already have an active ride.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String pickup =
                pickupInput.getText()
                        .toString()
                        .trim();

        String destination =
                destinationInput.getText()
                        .toString()
                        .trim();

        if (pickup.isEmpty()) {

            Toast.makeText(
                    this,
                    "Please select pickup location.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (pickupLat == 0 && pickupLng == 0) {

            Toast.makeText(
                    this,
                    "Please use your current location first.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (destination.isEmpty()) {

            Toast.makeText(
                    this,
                    "Please select a destination.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (
                destinationLat == 0
                ||
                destinationLng == 0
        ) {

            Toast.makeText(
                    this,
                    "Choose the destination on the map.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        String paymentMethod = "Cash";

        int selected =
                payment.getCheckedRadioButtonId();

        if (selected != -1) {

            RadioButton radio =
                    findViewById(selected);

            if (radio != null) {

                paymentMethod =
                        radio.getText().toString();
            }
        }

        double fare = getCurrentFare();

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
                "pickupName",
                pickup
        );

        ride.put(
                "destination",
                destination
        );

        ride.put(
                "destinationName",
                destination
        );

        ride.put(
                "pickupLatitude",
                pickupLat
        );

        ride.put(
                "pickupLongitude",
                pickupLng
        );

        ride.put(
                "destinationLatitude",
                destinationLat
        );

        ride.put(
                "destinationLongitude",
                destinationLng
        );

        ride.put(
                "paymentMethod",
                paymentMethod
        );

        ride.put(
                "paymentStatus",
                "PENDING"
        );

        ride.put(
                "fare",
                fare
        );

        ride.put(
                "baseFare",
                baseFare
        );

        ride.put(
                "perKm",
                perKm
        );

        ride.put(
                "status",
                "REQUESTED"
        );

        ride.put(
                "createdAt",
                System.currentTimeMillis()
        );

        bookButton.setEnabled(false);

        statusText.setText(
                "🔎 SENDING RIDE REQUEST..."
        );

        db.collection("rides")
                .add(ride)
                .addOnSuccessListener(document -> {

                    activeRideId =
                            document.getId();

                    /*
                     * New ride = allow REQUESTED
                     * notification to appear.
                     */
                    lastNotifiedRideStatus = "";

                    prefs.edit()
                            .putString(
                                    "activeRideId",
                                    activeRideId
                            )
                            .apply();

                    statusText.setText(
                            "🟢 RIDE REQUEST SENT\n"
                                    + "Waiting for a driver..."
                    );

                    driverInfoText.setText(
                            "👤 DRIVER\n"
                                    + "Waiting for a driver to accept..."
                    );

                    listenToRide(activeRideId);
                    updateButtons();

                    Toast.makeText(
                            this,
                            "🛺 Ride request sent.",
                            Toast.LENGTH_LONG
                    ).show();
                })
                .addOnFailureListener(e -> {

                    activeRideId = null;
                    lastNotifiedRideStatus = "";

                    prefs.edit()
                            .remove("activeRideId")
                            .apply();

                    bookButton.setEnabled(true);

                    statusText.setText(
                            "🔴 BOOKING FAILED"
                    );

                    Toast.makeText(
                            this,
                            "Booking failed:\n"
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();

                    updateButtons();
                });
    }

    private void calculateFare() {

        if (fareText == null) {
            return;
        }

        fareText.setText(
                String.format(
                        Locale.US,
                        "💰 Estimated fare: ₱%.0f",
                        getCurrentFare()
                )
        );
    }

    private double getCurrentFare() {

        if (
                pickupLat == 0
                ||
                pickupLng == 0
                ||
                destinationLat == 0
                ||
                destinationLng == 0
        ) {

            return minimumFare;
        }

        float[] distance =
                new float[1];

        Location.distanceBetween(
                pickupLat,
                pickupLng,
                destinationLat,
                destinationLng,
                distance
        );

        double kilometers =
                distance[0] / 1000.0;

        double fare =
                baseFare
                        +
                        kilometers * perKm;

        return Math.max(
                minimumFare,
                Math.min(
                        maximumFare,
                        fare
                )
        );
    }

    private String getAddress(
            double lat,
            double lng
    ) {

        try {

            Geocoder geocoder =
                    new Geocoder(
                            this,
                            Locale.getDefault()
                    );

            List<Address> addresses =
                    geocoder.getFromLocation(
                            lat,
                            lng,
                            1
                    );

            if (
                    addresses != null
                    &&
                    !addresses.isEmpty()
            ) {

                Address address =
                        addresses.get(0);

                String line =
                        address.getAddressLine(0);

                if (
                        line != null
                        &&
                        !line.trim().isEmpty()
                ) {

                    return line;
                }
            }

        } catch (Exception ignored) {
        }

        return "";
    }

    private void restoreSavedRide() {

        String saved =
                prefs.getString(
                        "activeRideId",
                        ""
                );

        if (
                saved == null
                ||
                saved.trim().isEmpty()
        ) {

            activeRideId = null;
            updateButtons();
            return;
        }

        db.collection("rides")
                .document(saved)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (
                            snapshot.exists()
                            &&
                            isActive(
                                    snapshot.getString(
                                            "status"
                                    )
                            )
                    ) {

                        FirebaseUser user =
                                auth.getCurrentUser();

                        String passengerId =
                                snapshot.getString(
                                        "passengerId"
                                );

                        if (
                                user != null
                                &&
                                user.getUid().equals(
                                        passengerId
                                )
                        ) {

                            activeRideId = saved;

                            listenToRide(saved);

                            updateButtons();
                            return;
                        }
                    }

                    clearRide();
                    updateButtons();
                })
                .addOnFailureListener(e -> {

                    clearRide();
                    updateButtons();
                });
    }

    private boolean isActive(String status) {

        return
                "REQUESTED".equals(status)
                ||
                "ACCEPTED".equals(status)
                ||
                "DRIVER_ON_THE_WAY".equals(status)
                ||
                "DRIVER_ARRIVED".equals(status)
                ||
                "IN_PROGRESS".equals(status);
    }

    private void listenToRide(String rideId) {

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
                                                "🔴 RIDE UPDATE ERROR"
                                        );

                                        return;
                                    }

                                    if (
                                            snapshot == null
                                            ||
                                            !snapshot.exists()
                                    ) {

                                        clearRide();
                                        updateButtons();
                                        return;
                                    }

                                    String status =
                                            snapshot.getString(
                                                    "status"
                                            );

                                    if (
                                            status == null
                                            ||
                                            status.isEmpty()
                                    ) {

                                        status =
                                                "REQUESTED";
                                    }

                                    String driverId =
                                            snapshot.getString(
                                                    "driverId"
                                            );

                                    /*
                                     * Passenger notification for
                                     * every supported ride status.
                                     */
                                    notifyPassengerRideStatus(
                                            status,
                                            snapshot
                                    );

                                    if (
                                            "REQUESTED".equals(status)
                                    ) {

                                        statusText.setText(
                                                "🔎 LOOKING FOR DRIVER..."
                                        );

                                        driverInfoText.setText(
                                                "👤 DRIVER\n"
                                                        + "Waiting for a driver to accept..."
                                        );

                                    } else if (
                                            "ACCEPTED".equals(status)
                                    ) {

                                        statusText.setText(
                                                "🟢 DRIVER ACCEPTED\n"
                                                        + "Your ride is confirmed."
                                        );

                                        showDriverInformation(
                                                snapshot,
                                                driverId
                                        );

                                    } else if (
                                            "DRIVER_ON_THE_WAY".equals(status)
                                            ||
                                            "DRIVER_ARRIVED".equals(status)
                                            ||
                                            "IN_PROGRESS".equals(status)
                                    ) {

                                        statusText.setText(
                                                "🚦 RIDE STATUS: "
                                                        + status
                                        );

                                        showDriverInformation(
                                                snapshot,
                                                driverId
                                        );

                                    } else {

                                        statusText.setText(
                                                "🚦 RIDE STATUS: "
                                                        + status
                                        );
                                    }

                                    if (!isActive(status)) {

                                        driverInfoText.setText(
                                                "👤 DRIVER\n"
                                                        + "Trip completed."
                                        );

                                        clearRide();
                                        updateButtons();
                                    }
                                }
                        );
    }

    /*
     * Passenger ride-status notifications.
     *
     * This uses the existing local notification helper.
     * It prevents duplicate notifications for the same
     * status while this PassengerActivity instance is alive.
     */
    private void notifyPassengerRideStatus(
            String status,
            DocumentSnapshot ride
    ) {

        if (status == null || status.trim().isEmpty()) {
            return;
        }

        String normalized =
                status.trim().toUpperCase(Locale.US);

        if (normalized.equals(lastNotifiedRideStatus)) {
            return;
        }

        lastNotifiedRideStatus = normalized;

        String title;
        String message;

        if ("REQUESTED".equals(normalized)) {

            title = "🛺 Sakay Na";
            message =
                    "Your ride request is looking for a driver.";

        } else if ("ACCEPTED".equals(normalized)) {

            title = "✅ Driver Accepted";
            message =
                    "Your Sakay Na driver accepted your ride.";

        } else if ("DRIVER_ON_THE_WAY".equals(normalized)) {

            title = "🚗 Driver On The Way";
            message =
                    "Your driver is on the way to your pickup location.";

        } else if ("DRIVER_ARRIVED".equals(normalized)) {

            title = "📍 Driver Arrived";
            message =
                    "Your driver has arrived at the pickup location.";

        } else if ("IN_PROGRESS".equals(normalized)) {

            title = "▶️ Ride Started";
            message =
                    "Your Sakay Na ride has started.";

        } else if ("COMPLETED".equals(normalized)) {

            title = "🏁 Ride Completed";
            message =
                    "Your Sakay Na ride has been completed.";

        } else if ("CANCELLED".equals(normalized)) {

            title = "❌ Ride Cancelled";
            message =
                    "Your Sakay Na ride was cancelled.";

        } else {

            return;
        }

        int notificationId =
                Math.abs(
                        (
                                "PASSENGER:"
                                        + activeRideId
                                        + ":"
                                        + normalized
                        ).hashCode()
                );

        SakayNaNotificationHelper.show(
                this,
                notificationId,
                title,
                message
        );
    }

    private void showDriverInformation(
            DocumentSnapshot ride,
            String driverId
    ) {

        String driverName =
                ride.getString("driverName");

        String driverPhone =
                ride.getString("driverPhone");

        String driverPlate =
                ride.getString("driverPlateNumber");

        String driverVehicle =
                ride.getString("driverVehicle");

        boolean hasRideDriverInfo =
                hasText(driverName)
                        ||
                        hasText(driverPhone)
                        ||
                        hasText(driverPlate)
                        ||
                        hasText(driverVehicle);

        if (hasRideDriverInfo) {

            displayDriverInformation(
                    driverName,
                    driverPhone,
                    driverPlate,
                    driverVehicle
            );

            return;
        }

        if (
                driverId == null
                ||
                driverId.trim().isEmpty()
        ) {

            driverInfoText.setText(
                    "👤 DRIVER\n"
                            + "Driver accepted the ride.\n"
                            + "Driver information is not available yet."
            );

            return;
        }

        db.collection("users")
                .document(driverId)
                .get()
                .addOnSuccessListener(userSnapshot -> {

                    if (
                            userSnapshot != null
                            &&
                            userSnapshot.exists()
                    ) {

                        String name =
                                firstNonEmpty(
                                        userSnapshot.getString(
                                                "driverName"
                                        ),
                                        userSnapshot.getString(
                                                "name"
                                        )
                                );

                        String phone =
                                firstNonEmpty(
                                        userSnapshot.getString(
                                                "phone"
                                        ),
                                        userSnapshot.getString(
                                                "driverPhone"
                                        )
                                );

                        String plate =
                                userSnapshot.getString(
                                        "plateNumber"
                                );

                        String vehicle =
                                userSnapshot.getString(
                                        "vehicleDescription"
                                );

                        displayDriverInformation(
                                name,
                                phone,
                                plate,
                                vehicle
                        );

                    } else {

                        driverInfoText.setText(
                                "👤 DRIVER\n"
                                        + "Driver accepted the ride.\n"
                                        + "Driver profile not found."
                        );
                    }
                })
                .addOnFailureListener(e -> {

                    driverInfoText.setText(
                            "👤 DRIVER\n"
                                    + "Driver accepted the ride.\n"
                                    + "Unable to load driver information."
                    );
                });
    }

    private void displayDriverInformation(
            String name,
            String phone,
            String plate,
            String vehicle
    ) {

        name =
                safeDriverValue(
                        name,
                        "Not provided"
                );

        phone =
                safeDriverValue(
                        phone,
                        "Not provided"
                );

        plate =
                safeDriverValue(
                        plate,
                        "Not provided"
                );

        vehicle =
                safeDriverValue(
                        vehicle,
                        "Tricycle"
                );

        driverInfoText.setText(
                "👤 DRIVER\n\n"
                        + "Name: " + name + "\n"
                        + "📱 Phone: " + phone + "\n"
                        + "🔢 Plate: " + plate + "\n"
                        + "🛺 Vehicle: " + vehicle
        );
    }

    private String firstNonEmpty(
            String first,
            String second
    ) {

        if (
                first != null
                &&
                !first.trim().isEmpty()
        ) {
            return first;
        }

        if (
                second != null
                &&
                !second.trim().isEmpty()
        ) {
            return second;
        }

        return "";
    }

    private String safeDriverValue(
            String value,
            String fallback
    ) {

        if (
                value == null
                ||
                value.trim().isEmpty()
        ) {
            return fallback;
        }

        return value;
    }

    private boolean hasText(String value) {

        return
                value != null
                &&
                !value.trim().isEmpty();
    }

    private void clearRide() {

        /*
         * Reset notification tracking when the ride ends
         * so the next ride can notify normally.
         */
        lastNotifiedRideStatus = "";

        activeRideId = null;

        prefs.edit()
                .remove("activeRideId")
                .apply();

        if (rideListener != null) {

            rideListener.remove();
            rideListener = null;
        }

        if (driverInfoText != null) {

            driverInfoText.setText(
                    "👤 DRIVER: Waiting for driver..."
            );
        }
    }

    private void updateButtons() {

        boolean active =
                activeRideId != null
                        &&
                        !activeRideId.isEmpty();

        if (bookButton != null) {
            bookButton.setEnabled(!active);
        }

        if (mapButton != null) {
            mapButton.setEnabled(active);
        }

        if (chatButton != null) {
            chatButton.setEnabled(active);
        }

        if (cancelButton != null) {
            cancelButton.setEnabled(active);
        }
    }

    private void openLiveMap() {

        if (!hasActiveRide()) {
            return;
        }

        Intent intent =
                new Intent(
                        this,
                        MapActivity.class
                );

        intent.putExtra(
                "mode",
                "LIVE_RIDE"
        );

        intent.putExtra(
                "ride_id",
                activeRideId
        );

        startActivity(intent);
    }

    private void openChat() {

        if (!hasActiveRide()) {
            return;
        }

        Intent intent =
                new Intent(
                        this,
                        RideChatActivity.class
                );

        intent.putExtra(
                "ride_id",
                activeRideId
        );

        intent.putExtra(
                "rideId",
                activeRideId
        );

        startActivity(intent);
    }

    private boolean hasActiveRide() {

        if (
                activeRideId == null
                ||
                activeRideId.isEmpty()
        ) {

            Toast.makeText(
                    this,
                    "No active ride.",
                    Toast.LENGTH_SHORT
            ).show();

            return false;
        }

        return true;
    }

    private void cancelRide() {

        if (!hasActiveRide()) {
            return;
        }

        db.collection("rides")
                .document(activeRideId)
                .update(
                        "status",
                        "CANCELLED",
                        "cancelledBy",
                        "PASSENGER",
                        "cancelledAt",
                        System.currentTimeMillis()
                )
                .addOnSuccessListener(v -> {

                    clearRide();

                    statusText.setText(
                            "❌ RIDE CANCELLED"
                    );

                    updateButtons();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Cancel failed:\n"
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    /*
     * FIXED HISTORY
     *
     * Uses Source.SERVER so the latest completed
     * ride is fetched from Firestore.
     *
     * Sorts newest booking first locally.
     *
     * No Firestore composite index is required.
     */
    private void showHistory() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {

            Toast.makeText(
                    this,
                    "Please log in first.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        final String passengerId =
                user.getUid();

        db.collection("rides")
                .whereEqualTo(
                        "passengerId",
                        passengerId
                )
                .get(Source.SERVER)
                .addOnSuccessListener(query -> {

                    try {

                        if (
                                query == null
                                ||
                                query.isEmpty()
                        ) {

                            new AlertDialog.Builder(this)
                                    .setTitle(
                                            "📜 Ride History"
                                    )
                                    .setMessage(
                                            "No ride history yet."
                                    )
                                    .setPositiveButton(
                                            "Close",
                                            null
                                    )
                                    .show();

                            return;
                        }

                        List<DocumentSnapshot> rides =
                                new ArrayList<>(
                                        query.getDocuments()
                                );

                        Collections.sort(
                                rides,
                                new Comparator<DocumentSnapshot>() {

                                    @Override
                                    public int compare(
                                            DocumentSnapshot a,
                                            DocumentSnapshot b
                                    ) {

                                        long timeA =
                                                readCreatedAt(a);

                                        long timeB =
                                                readCreatedAt(b);

                                        return Long.compare(
                                                timeB,
                                                timeA
                                        );
                                    }
                                }
                        );

                        StringBuilder history =
                                new StringBuilder();

                        SimpleDateFormat dateFormat =
                                new SimpleDateFormat(
                                        "MMM dd, yyyy • hh:mm a",
                                        Locale.US
                                );

                        for (
                                DocumentSnapshot ride :
                                rides
                        ) {

                            String pickup =
                                    safe(
                                            ride.getString(
                                                    "pickupName"
                                            ),
                                            ride.getString(
                                                    "pickup"
                                            )
                                    );

                            String destination =
                                    safe(
                                            ride.getString(
                                                    "destinationName"
                                            ),
                                            ride.getString(
                                                    "destination"
                                            )
                                    );

                            String status =
                                    safe(
                                            ride.getString(
                                                    "status"
                                            ),
                                            "UNKNOWN"
                                    );

                            double fare =
                                    readFare(ride);

                            String paymentMethod =
                                    safe(
                                            ride.getString(
                                                    "paymentMethod"
                                            ),
                                            "Not provided"
                                    );

                            long createdAt =
                                    readCreatedAt(ride);

                            String dateText =
                                    createdAt > 0
                                            ? dateFormat.format(
                                                    new Date(
                                                            createdAt
                                                    )
                                            )
                                            : "Date not available";

                            history.append(
                                    "🗓️ "
                                            + dateText
                                            + "\n"
                            );

                            history.append(
                                    "📍 "
                                            + pickup
                                            + "\n"
                            );

                            history.append(
                                    "🎯 "
                                            + destination
                                            + "\n"
                            );

                            history.append(
                                    "💰 ₱"
                                            + String.format(
                                            Locale.US,
                                            "%.0f",
                                            fare
                                    )
                                            + "\n"
                            );

                            history.append(
                                    "💳 "
                                            + paymentMethod
                                            + "\n"
                            );

                            history.append(
                                    "🚦 "
                                            + status
                                            + "\n"
                            );

                            history.append(
                                    "────────────────\n\n"
                            );
                        }

                        if (history.length() == 0) {

                            history.append(
                                    "No ride history yet."
                            );
                        }

                        new AlertDialog.Builder(this)
                                .setTitle(
                                        "📜 RIDE HISTORY"
                                )
                                .setMessage(
                                        history.toString()
                                )
                                .setPositiveButton(
                                        "Close",
                                        null
                                )
                                .show();

                    } catch (Exception e) {

                        Toast.makeText(
                                this,
                                "Unable to display ride history:\n"
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }

                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            "History failed:\n"
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private long readCreatedAt(
            DocumentSnapshot ride
    ) {

        try {

            Object value =
                    ride.get("createdAt");

            if (value instanceof Number) {

                return ((Number) value)
                        .longValue();
            }

            if (value instanceof String) {

                String text =
                        ((String) value).trim();

                if (!text.isEmpty()) {

                    return Long.parseLong(text);
                }
            }

            if (value instanceof com.google.firebase.Timestamp) {

                return ((com.google.firebase.Timestamp) value)
                        .toDate()
                        .getTime();
            }

            if (value instanceof Date) {

                return ((Date) value).getTime();
            }

        } catch (Exception ignored) {
        }

        return 0;
    }

    private double readFare(
            DocumentSnapshot ride
    ) {

        try {

            Object value =
                    ride.get("fare");

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

    private String safe(
            String first,
            String second
    ) {

        if (
                first != null
                &&
                !first.trim().isEmpty()
        ) {
            return first;
        }

        if (
                second != null
                &&
                !second.trim().isEmpty()
        ) {
            return second;
        }

        return "Unknown";
    }

    private void logout() {

        clearRide();

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

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] results
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                results
        );

        if (
                requestCode == LOCATION_REQUEST
        ) {

            for (int result : results) {

                if (
                        result ==
                                PackageManager.PERMISSION_GRANTED
                ) {

                    startLocation();
                    return;
                }
            }

            Toast.makeText(
                    this,
                    "Location permission is required.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    @Override
    protected void onDestroy() {

        if (rideListener != null) {

            rideListener.remove();
            rideListener = null;
        }

        if (
                locationManager != null
                &&
                locationListener != null
        ) {

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
