package com.sakyna.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
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
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
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
    private TextView passengerSelectedText;

    private Button bookButton;
    private Button mapButton;
    private Button chatButton;
    private Button cancelButton;

    private Button passengerOneButton;
    private Button passengerTwoButton;
    private Button passengerThreeButton;
    private Button passengerFourButton;

    private LocationManager locationManager;
    private LocationListener locationListener;
    private ListenerRegistration rideListener;

    private long rideGeneration = 0L;

    private String activeRideId = null;
    private String activeRideStatus = "";

    private boolean bookingInProgress = false;

    private double pickupLat = 0.0;
    private double pickupLng = 0.0;
    private double destinationLat = 0.0;
    private double destinationLng = 0.0;

    private int passengerCount = 1;

    private double baseFare = 25.0;
    private double perKm = 10.0;
    private double minimumFare = 25.0;
    private double maximumFare = 500.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        prefs = getSharedPreferences(
                "SakayNa",
                MODE_PRIVATE
        );

        buildScreen();
        loadFare();
        requestLocation();

        SakayNaNotificationHelper.requestPermission(this);

        /*
         * Never resurrect an old booking when PassengerActivity
         * starts again.
         */
        prefs.edit()
                .remove("activeRideId")
                .apply();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (auth.getCurrentUser() == null) {
            clearRide();
            updateButtons();
            return;
        }

        updateButtons();
    }

    private void buildScreen() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(
                Color.rgb(245, 248, 246)
        );

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(Gravity.CENTER);
        header.setPadding(12, 18, 12, 18);
        header.setBackgroundColor(
                Color.rgb(0, 125, 75)
        );

        TextView title = new TextView(this);
        title.setText("🛺 SAKAY NA");
        title.setTextSize(28);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        header.addView(title, full());

        TextView subtitle = new TextView(this);
        subtitle.setText("Passenger • Tricycle Ride");
        subtitle.setTextSize(14);
        subtitle.setTextColor(Color.WHITE);
        subtitle.setGravity(Gravity.CENTER);
        header.addView(subtitle, full());

        root.addView(header, full());

        ScrollView scroll = new ScrollView(this);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(16, 12, 16, 25);

        TextView heading = text(
                "🚕 BOOK YOUR RIDE",
                22
        );
        heading.setTypeface(null, Typeface.BOLD);
        heading.setGravity(Gravity.CENTER);
        heading.setTextColor(Color.rgb(0, 110, 70));
        content.addView(heading, full());

        TextView pickupLabel = text(
                "📍 PICKUP LOCATION",
                15
        );
        pickupLabel.setTypeface(null, Typeface.BOLD);
        content.addView(pickupLabel);

        pickupInput = new EditText(this);
        pickupInput.setHint("Pickup place");
        pickupInput.setTextSize(16);
        pickupInput.setPadding(16, 12, 16, 12);
        pickupInput.setBackgroundColor(Color.WHITE);
        content.addView(pickupInput, full());

        Button gpsButton = button(
                "📍 USE MY CURRENT LOCATION"
        );
        gpsButton.setOnClickListener(
                v -> useCurrentLocation()
        );
        content.addView(gpsButton);

        TextView destinationLabel = text(
                "🎯 DESTINATION",
                15
        );
        destinationLabel.setTypeface(null, Typeface.BOLD);
        content.addView(destinationLabel);

        destinationInput = new EditText(this);
        destinationInput.setHint(
                "Example: Jollibee, SM City"
        );
        destinationInput.setTextSize(16);
        destinationInput.setPadding(16, 12, 16, 12);
        destinationInput.setBackgroundColor(Color.WHITE);
        content.addView(destinationInput, full());

        Button destinationButton = button(
                "🗺️ SEARCH / CHOOSE DESTINATION"
        );
        destinationButton.setOnClickListener(
                v -> chooseDestination()
        );
        content.addView(destinationButton);

        TextView rideType = text(
                "🛺 RIDE TYPE",
                15
        );
        rideType.setTypeface(null, Typeface.BOLD);
        content.addView(rideType);

        TextView tricycle = text(
                "🛺 TRICYCLE",
                18
        );
        tricycle.setTypeface(null, Typeface.BOLD);
        tricycle.setGravity(Gravity.CENTER);
        tricycle.setTextColor(Color.rgb(0, 110, 70));
        tricycle.setBackgroundColor(Color.WHITE);
        content.addView(tricycle, full());

        TextView passengerLabel = text(
                "👥 NUMBER OF PASSENGERS",
                15
        );
        passengerLabel.setTypeface(null, Typeface.BOLD);
        content.addView(passengerLabel);

        LinearLayout passengerButtons =
                new LinearLayout(this);

        passengerButtons.setOrientation(
                LinearLayout.HORIZONTAL
        );
        passengerButtons.setGravity(Gravity.CENTER);

        passengerOneButton =
                passengerNumberButton("1", 1);

        passengerTwoButton =
                passengerNumberButton("2", 2);

        passengerThreeButton =
                passengerNumberButton("3", 3);

        passengerFourButton =
                passengerNumberButton("4", 4);

        passengerButtons.addView(
                passengerOneButton,
                passengerButtonParams()
        );
        passengerButtons.addView(
                passengerTwoButton,
                passengerButtonParams()
        );
        passengerButtons.addView(
                passengerThreeButton,
                passengerButtonParams()
        );
        passengerButtons.addView(
                passengerFourButton,
                passengerButtonParams()
        );

        content.addView(
                passengerButtons,
                full()
        );

        passengerSelectedText = text(
                "Selected: 1 passenger",
                14
        );
        passengerSelectedText.setGravity(Gravity.CENTER);
        passengerSelectedText.setTypeface(
                null,
                Typeface.BOLD
        );
        passengerSelectedText.setTextColor(
                Color.rgb(0, 110, 70)
        );
        content.addView(
                passengerSelectedText,
                full()
        );

        TextView fareRule = text(
                "₱25 per passenger + ₱10/km",
                14
        );
        fareRule.setGravity(Gravity.CENTER);
        fareRule.setTextColor(
                Color.rgb(0, 110, 70)
        );
        content.addView(fareRule, full());

        TextView paymentLabel = text(
                "💳 PAYMENT METHOD",
                15
        );
        paymentLabel.setTypeface(
                null,
                Typeface.BOLD
        );
        content.addView(paymentLabel);

        RadioGroup payment = new RadioGroup(this);
        payment.setOrientation(
                RadioGroup.HORIZONTAL
        );
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

        fareText = text(
                "👥 1 Passenger\n💰 Estimated fare: ₱25",
                23
        );
        fareText.setTypeface(null, Typeface.BOLD);
        fareText.setGravity(Gravity.CENTER);
        fareText.setTextColor(
                Color.rgb(0, 125, 75)
        );
        fareText.setBackgroundColor(Color.WHITE);
        content.addView(fareText, full());

        statusText = text(
                "🟢 Ready to book",
                16
        );
        statusText.setTypeface(null, Typeface.BOLD);
        statusText.setGravity(Gravity.CENTER);
        content.addView(statusText, full());

        driverInfoText = text(
                "👤 DRIVER: Waiting for driver...",
                16
        );
        driverInfoText.setTypeface(
                null,
                Typeface.BOLD
        );
        driverInfoText.setTextColor(
                Color.rgb(0, 100, 70)
        );
        driverInfoText.setBackgroundColor(
                Color.WHITE
        );
        driverInfoText.setPadding(
                16,
                16,
                16,
                16
        );
        content.addView(driverInfoText, full());

        bookButton = button(
                "🛺 BOOK A RIDE"
        );
        bookButton.setTextSize(19);
        bookButton.setBackgroundColor(
                Color.rgb(0, 150, 80)
        );
        bookButton.setOnClickListener(
                v -> bookRide(payment)
        );
        content.addView(bookButton);

        mapButton = button(
                "🗺️ LIVE RIDE MAP"
        );
        mapButton.setOnClickListener(
                v -> openLiveMap()
        );
        content.addView(mapButton);

        chatButton = button(
                "💬 CHAT WITH DRIVER"
        );
        chatButton.setOnClickListener(
                v -> openChat()
        );
        content.addView(chatButton);

        cancelButton = button(
                "❌ CANCEL RIDE"
        );
        cancelButton.setBackgroundColor(
                Color.rgb(190, 55, 55)
        );
        cancelButton.setOnClickListener(
                v -> cancelRide()
        );
        content.addView(cancelButton);

        Button historyButton = button(
                "📜 RIDE HISTORY"
        );
        historyButton.setBackgroundColor(
                Color.rgb(65, 90, 100)
        );
        historyButton.setOnClickListener(
                v -> showHistory()
        );
        content.addView(historyButton);

        Button logoutButton = button(
                "🚪 LOGOUT"
        );
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

        selectPassengerCount(1);
        updateButtons();
    }

    private Button passengerNumberButton(
            String number,
            int count
    ) {

        Button b = new Button(this);

        b.setText(number);
        b.setTextSize(18);
        b.setAllCaps(false);
        b.setTypeface(null, Typeface.BOLD);
        b.setTextColor(Color.WHITE);
        b.setPadding(4, 10, 4, 10);
        b.setBackgroundColor(
                Color.rgb(125, 135, 135)
        );

        b.setOnClickListener(
                v -> selectPassengerCount(count)
        );

        return b;
    }

    private LinearLayout.LayoutParams
    passengerButtonParams() {

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(
                        0,
                        -2,
                        1
                );

        p.setMargins(4, 5, 4, 5);

        return p;
    }

    private void selectPassengerCount(int count) {

        passengerCount =
                Math.max(
                        1,
                        Math.min(4, count)
                );

        updatePassengerSelectionUI();
        calculateFare();
    }

    private void updatePassengerSelectionUI() {

        if (passengerSelectedText != null) {

            passengerSelectedText.setText(
                    "Selected: "
                            + passengerCount
                            + " passenger"
                            + (
                            passengerCount == 1
                                    ? ""
                                    : "s"
                    )
            );
        }

        int selected =
                Color.rgb(0, 150, 80);

        int normal =
                Color.rgb(125, 135, 135);

        if (passengerOneButton != null) {

            passengerOneButton.setBackgroundColor(
                    passengerCount == 1
                            ? selected
                            : normal
            );

            passengerTwoButton.setBackgroundColor(
                    passengerCount == 2
                            ? selected
                            : normal
            );

            passengerThreeButton.setBackgroundColor(
                    passengerCount == 3
                            ? selected
                            : normal
            );

            passengerFourButton.setBackgroundColor(
                    passengerCount == 4
                            ? selected
                            : normal
            );
        }
    }

    private TextView text(
            String value,
            float size
    ) {

        TextView t = new TextView(this);

        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(Color.DKGRAY);
        t.setPadding(8, 8, 8, 8);

        return t;
    }

    private Button button(String value) {

        Button b = new Button(this);

        b.setText(value);
        b.setTextSize(16);
        b.setAllCaps(false);
        b.setTypeface(null, Typeface.BOLD);
        b.setTextColor(Color.WHITE);
        b.setPadding(12, 12, 12, 12);
        b.setBackgroundColor(
                Color.rgb(0, 125, 75)
        );

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                );

        p.setMargins(0, 7, 0, 7);

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
                            number(
                                    snapshot.get("baseFare"),
                                    25.0
                            );

                    perKm =
                            number(
                                    snapshot.get("perKm"),
                                    10.0
                            );

                    minimumFare =
                            number(
                                    snapshot.get("minimumFare"),
                                    25.0
                            );

                    maximumFare =
                            number(
                                    snapshot.get("maximumFare"),
                                    500.0
                            );

                    if (baseFare <= 0) {
                        baseFare = 25.0;
                    }

                    if (perKm <= 0) {
                        perKm = 10.0;
                    }

                    if (minimumFare <= 0) {
                        minimumFare = 25.0;
                    }

                    if (maximumFare < minimumFare) {
                        maximumFare = 500.0;
                    }

                    calculateFare();
                })
                .addOnFailureListener(
                        e -> calculateFare()
                );
    }

    private double number(
            Object value,
            double fallback
    ) {

        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        try {

            if (value != null) {

                return Double.parseDouble(
                        String.valueOf(value)
                );
            }

        } catch (Exception ignored) {
        }

        return fallback;
    }

    private double distanceInKm(
            double lat1,
            double lon1,
            double lat2,
            double lon2
    ) {

        if (
                lat1 == 0
                        ||
                        lon1 == 0
                        ||
                        lat2 == 0
                        ||
                        lon2 == 0
        ) {
            return 0;
        }

        float[] result = new float[1];

        Location.distanceBetween(
                lat1,
                lon1,
                lat2,
                lon2,
                result
        );

        return result[0] / 1000.0;
    }

    private void calculateFare() {

        double distanceKm =
                distanceInKm(
                        pickupLat,
                        pickupLng,
                        destinationLat,
                        destinationLng
                );

        double fare =
                (baseFare * passengerCount)
                        +
                        (perKm * distanceKm);

        if (fare < minimumFare) {
            fare = minimumFare;
        }

        if (fare > maximumFare) {
            fare = maximumFare;
        }

        if (fareText != null) {

            fareText.setText(
                    "👥 "
                            + passengerCount
                            + " Passenger"
                            + (
                            passengerCount == 1
                                    ? ""
                                    : "s"
                    )
                            + "\n💰 Estimated fare: ₱"
                            + String.format(
                            Locale.US,
                            "%.0f",
                            fare
                    )
            );
        }
    }

    private void bookRide(
            RadioGroup paymentGroup
    ) {

        if (bookingInProgress) {
            return;
        }

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

        if (hasCurrentRideWithoutToast()) {

            Toast.makeText(
                    this,
                    "You already have an active ride.",
                    Toast.LENGTH_LONG
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
                    "Please enter your pickup location.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (destination.isEmpty()) {

            Toast.makeText(
                    this,
                    "Please enter your destination.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (
                pickupLat == 0
                        ||
                        pickupLng == 0
        ) {

            Toast.makeText(
                    this,
                    "Please use your current location for pickup.",
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
                    "Please choose the destination on the map.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        bookingInProgress = true;

        final long generation =
                ++rideGeneration;

        String paymentMethod = "Cash";

        int selected =
                paymentGroup.getCheckedRadioButtonId();

        if (selected != -1) {

            View selectedView =
                    paymentGroup.findViewById(selected);

            if (selectedView instanceof RadioButton) {

                paymentMethod =
                        ((RadioButton) selectedView)
                                .getText()
                                .toString();
            }
        }

        createRide(
                user.getUid(),
                pickup,
                destination,
                paymentMethod,
                generation
        );

        updateButtons();
    }

    private boolean hasCurrentRideWithoutToast() {

        return
                activeRideId != null
                        &&
                        !activeRideId.trim().isEmpty()
                        &&
                        isActive(activeRideStatus);
    }

    private void createRide(
            String passengerId,
            String pickup,
            String destination,
            String paymentMethod,
            long generation
    ) {

        double distanceKm =
                distanceInKm(
                        pickupLat,
                        pickupLng,
                        destinationLat,
                        destinationLng
                );

        double fare =
                (baseFare * passengerCount)
                        +
                        (perKm * distanceKm);

        fare =
                Math.max(
                        minimumFare,
                        Math.min(
                                maximumFare,
                                fare
                        )
                );

        long now =
                System.currentTimeMillis();

        Map<String, Object> ride =
                new HashMap<>();

        ride.put("passengerId", passengerId);
        ride.put("pickup", pickup);
        ride.put("pickupName", pickup);
        ride.put("destination", destination);
        ride.put("destinationName", destination);

        ride.put("pickupLatitude", pickupLat);
        ride.put("pickupLongitude", pickupLng);

        ride.put(
                "destinationLatitude",
                destinationLat
        );

        ride.put(
                "destinationLongitude",
                destinationLng
        );

        ride.put("fare", fare);
        ride.put("distanceKm", distanceKm);
        ride.put("baseFare", baseFare);
        ride.put("perKm", perKm);
        ride.put("passengerCount", passengerCount);
        ride.put("paymentMethod", paymentMethod);
        ride.put("paymentStatus", "PENDING");
        ride.put("status", "REQUESTED");
        ride.put("createdAt", now);
        ride.put("statusUpdatedAt", now);
        ride.put("passengerBookingSession", now);

        db.collection("rides")
                .add(ride)
                .addOnSuccessListener(document -> {

                    if (
                            generation
                                    !=
                                    rideGeneration
                    ) {
                        return;
                    }

                    activeRideId =
                            document.getId();

                    activeRideStatus =
                            "REQUESTED";

                    prefs.edit()
                            .putString(
                                    "activeRideId",
                                    activeRideId
                            )
                            .apply();

                    bookingInProgress = false;

                    statusText.setText(
                            "🔎 LOOKING FOR DRIVER..."
                    );

                    statusText.setTextColor(
                            Color.rgb(0, 110, 70)
                    );

                    driverInfoText.setText(
                            "👤 DRIVER: Waiting for driver..."
                    );

                    listenToRide(activeRideId);
                    updateButtons();

                    Toast.makeText(
                            this,
                            "Ride booked successfully.",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(e -> {

                    if (
                            generation
                                    !=
                                    rideGeneration
                    ) {
                        return;
                    }

                    bookingInProgress = false;
                    updateButtons();

                    Toast.makeText(
                            this,
                            "Booking failed:\n"
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private boolean isActive(String status) {

        return
                "REQUESTED".equalsIgnoreCase(status)
                        ||
                        "ACCEPTED".equalsIgnoreCase(status)
                        ||
                        "DRIVER_ON_THE_WAY".equalsIgnoreCase(status)
                        ||
                        "DRIVER_ARRIVED".equalsIgnoreCase(status)
                        ||
                        "IN_PROGRESS".equalsIgnoreCase(status)
                        ||
                        "ARRIVED".equalsIgnoreCase(status)
                        ||
                        "ONGOING".equalsIgnoreCase(status);
    }

    private boolean requiresDriver(String status) {

        return
                "ACCEPTED".equalsIgnoreCase(status)
                        ||
                        "DRIVER_ON_THE_WAY".equalsIgnoreCase(status)
                        ||
                        "DRIVER_ARRIVED".equalsIgnoreCase(status)
                        ||
                        "ARRIVED".equalsIgnoreCase(status)
                        ||
                        "IN_PROGRESS".equalsIgnoreCase(status)
                        ||
                        "ONGOING".equalsIgnoreCase(status);
    }

    private boolean hasTerminalMarker(
            DocumentSnapshot ride
    ) {

        if (
                ride == null
                        ||
                        !ride.exists()
        ) {
            return true;
        }

        String status =
                safeStatus(
                        ride.getString("status")
                );

        if (
                "COMPLETED".equalsIgnoreCase(status)
                        ||
                        "CANCELLED".equalsIgnoreCase(status)
                        ||
                        "DECLINED".equalsIgnoreCase(status)
                        ||
                        "EXPIRED".equalsIgnoreCase(status)
                        ||
                        "FINISHED".equalsIgnoreCase(status)
        ) {
            return true;
        }

        return
                readTime(ride.get("completedAt")) > 0
                        ||
                        readTime(ride.get("finishedAt")) > 0
                        ||
                        readTime(ride.get("cancelledAt")) > 0
                        ||
                        readTime(ride.get("declinedAt")) > 0
                        ||
                        readTime(ride.get("expiredAt")) > 0;
    }

    private String safeStatus(String value) {

        if (
                value == null
                        ||
                        value.trim().isEmpty()
        ) {
            return "";
        }

        return value.trim();
    }

    private long readTime(Object value) {

        if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        if (
                value instanceof
                        com.google.firebase.Timestamp
        ) {

            return ((com.google.firebase.Timestamp) value)
                    .toDate()
                    .getTime();
        }

        if (value instanceof Date) {
            return ((Date) value).getTime();
        }

        try {

            if (value != null) {

                return Long.parseLong(
                        String.valueOf(value)
                );
            }

        } catch (Exception ignored) {
        }

        return 0L;
    }

    /*
     * ============================================================
     * CURRENT RIDE LISTENER
     * ============================================================
     */

    private void listenToRide(String rideId) {

        if (
                rideId == null
                        ||
                        rideId.trim().isEmpty()
        ) {
            return;
        }

        if (rideListener != null) {

            rideListener.remove();
            rideListener = null;
        }

        final long generation =
                rideGeneration;

        final String listeningRideId =
                rideId;

        rideListener =
                db.collection("rides")
                        .document(rideId)
                        .addSnapshotListener(
                                (snapshot, error) -> {

                                    if (
                                            generation
                                                    !=
                                                    rideGeneration
                                    ) {
                                        return;
                                    }

                                    if (
                                            activeRideId == null
                                                    ||
                                                    !listeningRideId.equals(
                                                            activeRideId
                                                    )
                                    ) {
                                        return;
                                    }

                                    if (error != null) {
                                        return;
                                    }

                                    if (
                                            snapshot == null
                                                    ||
                                                    !snapshot.exists()
                                    ) {

                                        finishCurrentBooking(
                                                "🟢 READY TO BOOK"
                                        );

                                        return;
                                    }

                                    if (
                                            hasTerminalMarker(
                                                    snapshot
                                            )
                                    ) {

                                        finishCurrentBooking(
                                                terminalMessage(
                                                        snapshot.getString(
                                                                "status"
                                                        )
                                                )
                                        );

                                        return;
                                    }

                                    String status =
                                            safeStatus(
                                                    snapshot.getString(
                                                            "status"
                                                    )
                                            );

                                    if (!isActive(status)) {

                                        finishCurrentBooking(
                                                "🟢 READY TO BOOK"
                                        );

                                        return;
                                    }

                                    /*
                                     * IMPORTANT GREEN FIX:
                                     *
                                     * Do NOT clear an accepted ride
                                     * just because driverId is temporarily
                                     * absent while DriverActivity is
                                     * writing the acceptance fields.
                                     */
                                    activeRideStatus = status;

                                    updateStatusText(status);

                                    showDriverInformation(
                                            snapshot,
                                            generation,
                                            listeningRideId
                                    );

                                    if (
                                            "ACCEPTED".equalsIgnoreCase(
                                                    status
                                            )
                                    ) {

                                        notifyPassengerDriverAccepted(
                                                listeningRideId,
                                                snapshot
                                        );
                                    }

                                    Object count =
                                            snapshot.get(
                                                    "passengerCount"
                                            );

                                    if (
                                            count instanceof Number
                                    ) {

                                        passengerCount =
                                                Math.max(
                                                        1,
                                                        Math.min(
                                                                4,
                                                                ((Number) count)
                                                                        .intValue()
                                                )
                                                );

                                        updatePassengerSelectionUI();
                                    }

                                    updateButtons();
                                }
                        );
    }

    private String terminalMessage(String status) {

        if (
                "COMPLETED".equalsIgnoreCase(status)
                        ||
                        "FINISHED".equalsIgnoreCase(status)
        ) {

            return
                    "✅ RIDE COMPLETED — READY FOR NEW BOOKING";
        }

        if ("CANCELLED".equalsIgnoreCase(status)) {

            return
                    "❌ RIDE CANCELLED — READY FOR NEW BOOKING";
        }

        if ("DECLINED".equalsIgnoreCase(status)) {

            return
                    "⚠️ RIDE DECLINED — READY FOR NEW BOOKING";
        }

        if ("EXPIRED".equalsIgnoreCase(status)) {

            return
                    "⌛ RIDE EXPIRED — READY FOR NEW BOOKING";
        }

        return "🟢 READY TO BOOK";
    }

    private void finishCurrentBooking(
            String message
    ) {

        rideGeneration++;

        if (rideListener != null) {

            rideListener.remove();
            rideListener = null;
        }

        activeRideId = null;
        activeRideStatus = "";

        prefs.edit()
                .remove("activeRideId")
                .apply();

        bookingInProgress = false;

        if (driverInfoText != null) {

            driverInfoText.setText(
                    "👤 DRIVER: Waiting for driver..."
            );
        }

        if (fareText != null) {

            fareText.setText(
                    "👥 "
                            + passengerCount
                            + " Passenger"
                            + (
                            passengerCount == 1
                                    ? ""
                                    : "s"
                    )
                            + "\n💰 Estimated fare: ₱"
                            + String.format(
                            Locale.US,
                            "%.0f",
                            minimumFare
                    )
            );
        }

        if (statusText != null) {

            statusText.setText(message);

            statusText.setTextColor(
                    Color.rgb(0, 110, 70)
            );
        }

        updateButtons();
    }

    private void updateStatusText(String status) {

        if (statusText == null) {
            return;
        }

        if ("REQUESTED".equalsIgnoreCase(status)) {

            statusText.setText(
                    "🔎 LOOKING FOR DRIVER..."
            );

        } else if ("ACCEPTED".equalsIgnoreCase(status)) {

            statusText.setText(
                    "✅ DRIVER ACCEPTED YOUR RIDE"
            );

        } else if (
                "DRIVER_ON_THE_WAY".equalsIgnoreCase(status)
        ) {

            statusText.setText(
                    "🚗 DRIVER IS ON THE WAY"
            );

        } else if (
                "DRIVER_ARRIVED".equalsIgnoreCase(status)
                        ||
                        "ARRIVED".equalsIgnoreCase(status)
        ) {

            statusText.setText(
                    "📍 DRIVER HAS ARRIVED"
            );

        } else if (
                "IN_PROGRESS".equalsIgnoreCase(status)
                        ||
                        "ONGOING".equalsIgnoreCase(status)
        ) {

            statusText.setText(
                    "🛺 RIDE IN PROGRESS"
            );

        } else {

            statusText.setText(
                    "🟢 " + status
            );
        }

        statusText.setTextColor(
                Color.rgb(0, 110, 70)
        );
    }

    /*
     * ============================================================
     * DRIVER INFORMATION
     * ============================================================
     */

    private void showDriverInformation(
            DocumentSnapshot ride,
            long generation,
            String rideId
    ) {

        if (
                ride == null
                        ||
                        !ride.exists()
        ) {

            driverInfoText.setText(
                    "👤 DRIVER: Waiting for driver..."
            );

            return;
        }

        /*
         * FIRST use information stored directly in the ride.
         */
        String rideDriverName =
                ride.getString("driverName");

        String rideDriverPhone =
                ride.getString("driverPhone");

        String rideDriverVehicle =
                ride.getString("driverVehicle");

        String rideDriverPlate =
                ride.getString("driverPlateNumber");

        boolean hasRideDriverInfo =
                !isEmpty(rideDriverName)
                        ||
                        !isEmpty(rideDriverPhone)
                        ||
                        !isEmpty(rideDriverVehicle)
                        ||
                        !isEmpty(rideDriverPlate);

        if (hasRideDriverInfo) {

            driverInfoText.setText(
                    formatDriverInformation(
                            rideDriverName,
                            rideDriverPhone,
                            firstNonEmpty(
                                    rideDriverVehicle,
                                    rideDriverPlate
                            )
                    )
            );
        }

        String driverId =
                ride.getString("driverId");

        if (isEmpty(driverId)) {

            if (!hasRideDriverInfo) {

                driverInfoText.setText(
                        "👤 DRIVER: Waiting for driver..."
                );
            }

            return;
        }

        final String expectedRideId = rideId;
        final long expectedGeneration = generation;

        db.collection("users")
                .document(driverId)
                .get()
                .addOnSuccessListener(user -> {

                    if (
                            expectedGeneration
                                    !=
                                    rideGeneration
                    ) {
                        return;
                    }

                    if (
                            activeRideId == null
                                    ||
                                    !expectedRideId.equals(
                                            activeRideId
                                    )
                    ) {
                        return;
                    }

                    String name =
                            firstNonEmpty(
                                    rideDriverName
                            );

                    String phone =
                            firstNonEmpty(
                                    rideDriverPhone
                            );

                    String vehicle =
                            firstNonEmpty(
                                    rideDriverVehicle,
                                    rideDriverPlate
                            );

                    if (
                            user != null
                                    &&
                                    user.exists()
                    ) {

                        name =
                                firstNonEmpty(
                                        name,
                                        user.getString("name"),
                                        user.getString("fullName")
                                );

                        phone =
                                firstNonEmpty(
                                        phone,
                                        user.getString("phone"),
                                        user.getString("phoneNumber")
                                );

                        vehicle =
                                firstNonEmpty(
                                        vehicle,
                                        user.getString("vehicle"),
                                        user.getString(
                                                "tricycleNumber"
                                        )
                                );
                    }

                    if (
                            isEmpty(name)
                                    &&
                                    isEmpty(phone)
                                    &&
                                    isEmpty(vehicle)
                    ) {

                        driverInfoText.setText(
                                "👤 DRIVER: Assigned"
                        );

                        return;
                    }

                    driverInfoText.setText(
                            formatDriverInformation(
                                    name,
                                    phone,
                                    vehicle
                            )
                    );
                })
                .addOnFailureListener(
                        e -> {

                            if (!hasRideDriverInfo) {

                                driverInfoText.setText(
                                        "👤 DRIVER: Assigned"
                                );
                            }
                        }
                );
    }

    private void notifyPassengerDriverAccepted(
            String rideId,
            DocumentSnapshot ride
    ) {

        if (
                isEmpty(rideId)
                        ||
                        ride == null
                        ||
                        !ride.exists()
        ) {
            return;
        }

        String lastNotifiedRide =
                prefs.getString(
                        "passengerAcceptedNotifiedRideId",
                        ""
                );

        if (
                rideId.equals(
                        lastNotifiedRide
                )
        ) {
            return;
        }

        String driverName =
                firstNonEmpty(
                        ride.getString("driverName"),
                        "Your driver"
                );

        prefs.edit()
                .putString(
                        "passengerAcceptedNotifiedRideId",
                        rideId
                )
                .apply();

        Intent intent =
                new Intent(
                        this,
                        PassengerActivity.class
                );

        intent.putExtra(
                "ride_id",
                rideId
        );

        intent.putExtra(
                "rideId",
                rideId
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                        |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        int notificationId =
                Math.abs(
                        (
                                rideId
                                        +
                                        ":PASSENGER_ACCEPTED"
                        ).hashCode()
                );

        int flags =
                PendingIntent.FLAG_UPDATE_CURRENT;

        if (
                Build.VERSION.SDK_INT
                        >=
                        Build.VERSION_CODES.M
        ) {

            flags |=
                    PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        this,
                        notificationId,
                        intent,
                        flags
                );

        SakayNaNotificationHelper.show(
                this,
                notificationId,
                "🛺 Sakay Na — Driver Accepted",
                "✅ "
                        + driverName
                        + " accepted your ride.",
                pendingIntent
        );
    }

    private boolean isEmpty(String value) {

        return
                value == null
                        ||
                        value.trim().isEmpty();
    }

    private String firstNonEmpty(
            String... values
    ) {

        if (values == null) {
            return "";
        }

        for (String value : values) {

            if (
                    value != null
                            &&
                            !value.trim().isEmpty()
            ) {

                return value.trim();
            }
        }

        return "";
    }

    private String formatDriverInformation(
            String name,
            String phone,
            String vehicle
    ) {

        StringBuilder info =
                new StringBuilder();

        info.append("👤 DRIVER: ");

        if (isEmpty(name)) {
            info.append("Assigned");
        } else {
            info.append(name);
        }

        if (!isEmpty(phone)) {

            info.append("\n📞 ");
            info.append(phone);
        }

        if (!isEmpty(vehicle)) {

            info.append("\n🛺 ");
            info.append(vehicle);
        }

        return info.toString();
    }

    private void clearRideInternal() {

        if (rideListener != null) {

            rideListener.remove();
            rideListener = null;
        }

        activeRideId = null;
        activeRideStatus = "";

        prefs.edit()
                .remove("activeRideId")
                .apply();

        if (driverInfoText != null) {

            driverInfoText.setText(
                    "👤 DRIVER: Waiting for driver..."
            );
        }
    }

    private void clearRide() {

        rideGeneration++;

        clearRideInternal();

        bookingInProgress = false;

        if (statusText != null) {

            statusText.setText(
                    "🟢 Ready to book"
            );

            statusText.setTextColor(
                    Color.rgb(0, 110, 70)
            );
        }
    }

    /*
     * ============================================================
     * MAP
     * ============================================================
     */

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
                requestCode
                        !=
                        DESTINATION_REQUEST
        ) {
            return;
        }

        if (
                resultCode
                        !=
                        RESULT_OK
        ) {
            return;
        }

        if (data == null) {
            return;
        }

        destinationLat =
                data.getDoubleExtra(
                        "latitude",
                        data.getDoubleExtra(
                                "destination_latitude",
                                0.0
                        )
                );

        destinationLng =
                data.getDoubleExtra(
                        "longitude",
                        data.getDoubleExtra(
                                "destination_longitude",
                                0.0
                        )
                );

        String name =
                data.getStringExtra(
                        "place_name"
                );

        if (isEmpty(name)) {

            name =
                    data.getStringExtra(
                            "destinationName"
                    );
        }

        if (!isEmpty(name)) {

            destinationInput.setText(name);

        } else if (
                destinationLat != 0
                        &&
                        destinationLng != 0
        ) {

            reverseGeocodeDestination();
        }

        calculateFare();
    }

    private void reverseGeocodeDestination() {

        try {

            Geocoder geocoder =
                    new Geocoder(
                            this,
                            Locale.getDefault()
                    );

            List<Address> addresses =
                    geocoder.getFromLocation(
                            destinationLat,
                            destinationLng,
                            1
                    );

            if (
                    addresses != null
                            &&
                            !addresses.isEmpty()
            ) {

                Address address =
                        addresses.get(0);

                StringBuilder name =
                        new StringBuilder();

                for (
                        int i = 0;
                        i <= address.getMaxAddressLineIndex();
                        i++
                ) {

                    String line =
                            address.getAddressLine(i);

                    if (!isEmpty(line)) {

                        if (name.length() > 0) {
                            name.append(", ");
                        }

                        name.append(line);
                    }
                }

                if (name.length() > 0) {

                    destinationInput.setText(
                            name.toString()
                    );
                }
            }

        } catch (Exception ignored) {
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

    /*
     * ============================================================
     * CHAT
     * ============================================================
     */

    private void openChat() {

        if (!hasActiveRide()) {
            return;
        }

        if (isEmpty(activeRideId)) {
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
                isEmpty(activeRideId)
                        ||
                        !isActive(activeRideStatus)
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

    /*
     * ============================================================
     * CANCEL
     * ============================================================
     */

    private void cancelRide() {

        if (!hasActiveRide()) {
            return;
        }

        final String rideId =
                activeRideId;

        db.runTransaction(
                transaction -> {

                    DocumentSnapshot ride =
                            transaction.get(
                                    db.collection("rides")
                                            .document(rideId)
                            );

                    if (!ride.exists()) {

                        throw new IllegalStateException(
                                "Ride no longer exists."
                        );
                    }

                    String status =
                            safeStatus(
                                    ride.getString("status")
                            );

                    if (
                            !"REQUESTED"
                                    .equalsIgnoreCase(status)
                    ) {

                        throw new IllegalStateException(
                                "The driver has already accepted this booking."
                        );
                    }

                    long now =
                            System.currentTimeMillis();

                    transaction.update(
                            ride.getReference(),
                            "status",
                            "CANCELLED",
                            "cancelledBy",
                            "PASSENGER",
                            "cancelledAt",
                            now,
                            "statusUpdatedAt",
                            now
                    );

                    return null;
                }
        )
                .addOnSuccessListener(
                        v -> finishCurrentBooking(
                                "❌ RIDE CANCELLED — READY FOR NEW BOOKING"
                        )
                )
                .addOnFailureListener(
                        e -> Toast.makeText(
                                this,
                                "Unable to cancel:\n"
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    /*
     * ============================================================
     * GCASH
     * ============================================================
     */

    private void showGcashQr() {

        ImageView image =
                new ImageView(this);

        image.setImageResource(
                R.drawable.gcash_qr
        );

        image.setAdjustViewBounds(true);

        image.setScaleType(
                ImageView.ScaleType.CENTER_INSIDE
        );

        LinearLayout container =
                new LinearLayout(this);

        container.setOrientation(
                LinearLayout.VERTICAL
        );

        container.setPadding(
                20,
                20,
                20,
                20
        );

        TextView title =
                text(
                        "📱 GCash Payment QR",
                        20
                );

        title.setGravity(Gravity.CENTER);
        title.setTypeface(
                null,
                Typeface.BOLD
        );

        container.addView(
                title,
                full()
        );

        container.addView(
                image,
                new LinearLayout.LayoutParams(
                        -1,
                        600
                )
        );

        new AlertDialog.Builder(this)
                .setView(container)
                .setPositiveButton(
                        "Close",
                        null
                )
                .show();
    }

    /*
     * ============================================================
     * LOCATION
     * ============================================================
     */

    private void requestLocation() {

        if (
                checkSelfPermission(
                        Manifest.permission.ACCESS_FINE_LOCATION
                )
                        !=
                        PackageManager.PERMISSION_GRANTED
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

        try {

            locationManager =
                    (LocationManager)
                            getSystemService(
                                    LOCATION_SERVICE
                            );

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
                                    pickupInput
                                            .getText()
                                            .toString()
                                            .trim()
                                            .isEmpty()
                            ) {

                                reverseGeocodePickup();
                            }

                            calculateFare();
                        }
                    };

            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000,
                    5,
                    locationListener
            );

            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    3000,
                    5,
                    locationListener
            );

        } catch (SecurityException ignored) {
        }
    }

    private void useCurrentLocation() {

        if (
                pickupLat == 0
                        ||
                        pickupLng == 0
        ) {

            requestLocation();

            Toast.makeText(
                    this,
                    "Getting your current location...",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        reverseGeocodePickup();

        Toast.makeText(
                this,
                "Pickup location updated.",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void reverseGeocodePickup() {

        try {

            Geocoder geocoder =
                    new Geocoder(
                            this,
                            Locale.getDefault()
                    );

            List<Address> addresses =
                    geocoder.getFromLocation(
                            pickupLat,
                            pickupLng,
                            1
                    );

            if (
                    addresses != null
                            &&
                            !addresses.isEmpty()
            ) {

                Address address =
                        addresses.get(0);

                StringBuilder name =
                        new StringBuilder();

                for (
                        int i = 0;
                        i <= address.getMaxAddressLineIndex();
                        i++
                ) {

                    String line =
                            address.getAddressLine(i);

                    if (!isEmpty(line)) {

                        if (name.length() > 0) {
                            name.append(", ");
                        }

                        name.append(line);
                    }
                }

                if (name.length() > 0) {

                    pickupInput.setText(
                            name.toString()
                    );
                }
            }

        } catch (Exception ignored) {
        }
    }

    /*
     * ============================================================
     * HISTORY
     * ============================================================
     */

    private void showHistory() {

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
                .get(Source.SERVER)
                .addOnSuccessListener(query -> {

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
                            (a, b) ->
                                    Long.compare(
                                            readTime(
                                                    b.get("createdAt")
                                            ),
                                            readTime(
                                                    a.get("createdAt")
                                            )
                                    )
                    );

                    SimpleDateFormat format =
                            new SimpleDateFormat(
                                    "MMM dd, yyyy • hh:mm a",
                                    Locale.US
                            );

                    StringBuilder history =
                            new StringBuilder();

                    for (DocumentSnapshot ride : rides) {

                        String pickup =
                                firstNonEmpty(
                                        ride.getString(
                                                "pickupName"
                                        ),
                                        ride.getString(
                                                "pickup"
                                        ),
                                        "Unknown"
                                );

                        String destination =
                                firstNonEmpty(
                                        ride.getString(
                                                "destinationName"
                                        ),
                                        ride.getString(
                                                "destination"
                                        ),
                                        "Unknown"
                                );

                        String status =
                                firstNonEmpty(
                                        ride.getString(
                                                "status"
                                        ),
                                        "UNKNOWN"
                                );

                        double fare =
                                number(
                                        ride.get("fare"),
                                        0
                                );

                        String payment =
                                firstNonEmpty(
                                        ride.getString(
                                                "paymentMethod"
                                        ),
                                        "Not provided"
                                );

                        long created =
                                readTime(
                                        ride.get("createdAt")
                                );

                        history.append(
                                "🗓️ "
                                        + (
                                        created > 0
                                                ? format.format(
                                                new Date(created)
                                        )
                                                : "Date unavailable"
                                )
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
                                        + payment
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
                })
                .addOnFailureListener(
                        e -> Toast.makeText(
                                this,
                                "History failed:\n"
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void updateButtons() {

        boolean active =
                hasCurrentRideWithoutToast();

        if (bookButton != null) {

            bookButton.setEnabled(
                    !bookingInProgress
            );

            bookButton.setText(
                    "🛺 BOOK A RIDE"
            );
        }

        if (mapButton != null) {
            mapButton.setEnabled(active);
        }

        if (chatButton != null) {
            chatButton.setEnabled(active);
        }

        if (cancelButton != null) {

            cancelButton.setEnabled(
                    active
                            &&
                            "REQUESTED".equalsIgnoreCase(
                                    activeRideStatus
                            )
            );
        }

        updatePassengerSelectionUI();
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
            @NonNull int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (
                requestCode
                        ==
                        LOCATION_REQUEST
        ) {

            for (int result : grantResults) {

                if (
                        result
                                ==
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

        rideGeneration++;

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
