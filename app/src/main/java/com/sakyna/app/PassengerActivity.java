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
import com.google.firebase.firestore.QuerySnapshot;
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

    private static final long REQUEST_TIMEOUT_MS =
            15 * 60 * 1000L;

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

    /*
     * ============================================================
     * RED MANTRA
     * ============================================================
     *
     * ONLY the CURRENT VALID BOOKING may control this screen.
     *
     * Old Firestore callbacks are NEVER allowed to restore:
     *
     * - old booking
     * - old driver
     * - old status
     * - old map
     * - old chat ride
     *
     * Every ride switch/clear increments this generation.
     * ============================================================
     */
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

        /*
         * Local activeRideId is NOT trusted.
         * Firebase SERVER is the source of truth.
         */
        restoreSavedRide();
    }

    @Override
    protected void onResume() {
        super.onResume();

        /*
         * Invalidate callbacks from previous screen state.
         */
        final long generation =
                ++rideGeneration;

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {

            clearRide();

            updateButtons();

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
                            generation
                                    !=
                                    rideGeneration
                    ) {
                        return;
                    }

                    DocumentSnapshot ride =
                            findActiveRide(query);

                    /*
                     * RED MANTRA:
                     *
                     * No valid active ride exists on SERVER.
                     * Destroy all local ride state.
                     */
                    if (ride == null) {

                        clearRide();

                        updateButtons();

                        return;
                    }

                    attachToRide(
                            ride,
                            generation
                    );
                })
                .addOnFailureListener(e -> {

                    if (
                            generation
                                    !=
                                    rideGeneration
                    ) {
                        return;
                    }

                    updateButtons();
                });
    }

    private void buildScreen() {

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setBackgroundColor(
                Color.rgb(245, 248, 246)
        );

        LinearLayout header =
                new LinearLayout(this);

        header.setOrientation(
                LinearLayout.VERTICAL
        );

        header.setGravity(
                Gravity.CENTER
        );

        header.setPadding(
                12,
                18,
                12,
                18
        );

        header.setBackgroundColor(
                Color.rgb(0, 125, 75)
        );

        TextView title =
                new TextView(this);

        title.setText(
                "🛺 SAKAY NA"
        );

        title.setTextSize(28);

        title.setTypeface(
                null,
                Typeface.BOLD
        );

        title.setTextColor(
                Color.WHITE
        );

        title.setGravity(
                Gravity.CENTER
        );

        header.addView(
                title,
                full()
        );

        TextView subtitle =
                new TextView(this);

        subtitle.setText(
                "Passenger • Tricycle Ride"
        );

        subtitle.setTextSize(14);

        subtitle.setTextColor(
                Color.WHITE
        );

        subtitle.setGravity(
                Gravity.CENTER
        );

        header.addView(
                subtitle,
                full()
        );

        root.addView(
                header,
                full()
        );

        ScrollView scroll =
                new ScrollView(this);

        LinearLayout content =
                new LinearLayout(this);

        content.setOrientation(
                LinearLayout.VERTICAL
        );

        content.setPadding(
                16,
                12,
                16,
                25
        );

        TextView heading =
                text(
                        "🚕 BOOK YOUR RIDE",
                        22
                );

        heading.setTypeface(
                null,
                Typeface.BOLD
        );

        heading.setGravity(
                Gravity.CENTER
        );

        heading.setTextColor(
                Color.rgb(0, 110, 70)
        );

        content.addView(
                heading,
                full()
        );

        TextView pickupLabel =
                text(
                        "📍 PICKUP LOCATION",
                        15
                );

        pickupLabel.setTypeface(
                null,
                Typeface.BOLD
        );

        content.addView(
                pickupLabel
        );

        pickupInput =
                new EditText(this);

        pickupInput.setHint(
                "Pickup place"
        );

        pickupInput.setTextSize(16);

        pickupInput.setPadding(
                16,
                12,
                16,
                12
        );

        pickupInput.setBackgroundColor(
                Color.WHITE
        );

        content.addView(
                pickupInput,
                full()
        );

        Button gpsButton =
                button(
                        "📍 USE MY CURRENT LOCATION"
                );

        gpsButton.setOnClickListener(
                v -> useCurrentLocation()
        );

        content.addView(
                gpsButton
        );

        TextView destinationLabel =
                text(
                        "🎯 DESTINATION",
                        15
                );

        destinationLabel.setTypeface(
                null,
                Typeface.BOLD
        );

        content.addView(
                destinationLabel
        );

        destinationInput =
                new EditText(this);

        destinationInput.setHint(
                "Example: Jollibee, SM City"
        );

        destinationInput.setTextSize(16);

        destinationInput.setPadding(
                16,
                12,
                16,
                12
        );

        destinationInput.setBackgroundColor(
                Color.WHITE
        );

        content.addView(
                destinationInput,
                full()
        );

        Button destinationButton =
                button(
                        "🗺️ SEARCH / CHOOSE DESTINATION"
                );

        destinationButton.setOnClickListener(
                v -> chooseDestination()
        );

        content.addView(
                destinationButton
        );

        TextView rideType =
                text(
                        "🛺 RIDE TYPE",
                        15
                );

        rideType.setTypeface(
                null,
                Typeface.BOLD
        );

        content.addView(
                rideType
        );

        TextView tricycle =
                text(
                        "🛺 TRICYCLE",
                        18
                );

        tricycle.setTypeface(
                null,
                Typeface.BOLD
        );

        tricycle.setGravity(
                Gravity.CENTER
        );

        tricycle.setTextColor(
                Color.rgb(0, 110, 70)
        );

        tricycle.setBackgroundColor(
                Color.WHITE
        );

        content.addView(
                tricycle,
                full()
        );

        TextView passengerLabel =
                text(
                        "👥 NUMBER OF PASSENGERS",
                        15
                );

        passengerLabel.setTypeface(
                null,
                Typeface.BOLD
        );

        content.addView(
                passengerLabel
        );

        LinearLayout passengerButtons =
                new LinearLayout(this);

        passengerButtons.setOrientation(
                LinearLayout.HORIZONTAL
        );

        passengerButtons.setGravity(
                Gravity.CENTER
        );

        passengerOneButton =
                passengerNumberButton(
                        "1",
                        1
                );

        passengerTwoButton =
                passengerNumberButton(
                        "2",
                        2
                );

        passengerThreeButton =
                passengerNumberButton(
                        "3",
                        3
                );

        passengerFourButton =
                passengerNumberButton(
                        "4",
                        4
                );

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

        passengerSelectedText =
                text(
                        "Selected: 1 passenger",
                        14
                );

        passengerSelectedText.setGravity(
                Gravity.CENTER
        );

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

        TextView fareRule =
                text(
                        "₱25 per passenger + ₱10/km",
                        14
                );

        fareRule.setGravity(
                Gravity.CENTER
        );

        fareRule.setTextColor(
                Color.rgb(0, 110, 70)
        );

        content.addView(
                fareRule,
                full()
        );

        TextView paymentLabel =
                text(
                        "💳 PAYMENT METHOD",
                        15
                );

        paymentLabel.setTypeface(
                null,
                Typeface.BOLD
        );

        content.addView(
                paymentLabel
        );

        RadioGroup payment =
                new RadioGroup(this);

        payment.setOrientation(
                RadioGroup.HORIZONTAL
        );

        payment.setGravity(
                Gravity.CENTER
        );

        RadioButton cash =
                new RadioButton(this);

        cash.setText("Cash");

        cash.setChecked(true);

        payment.addView(cash);

        RadioButton gcash =
                new RadioButton(this);

        gcash.setText("GCash");

        payment.addView(gcash);

        RadioButton maya =
                new RadioButton(this);

        maya.setText("Maya");

        payment.addView(maya);

        content.addView(
                payment,
                full()
        );

        fareText =
                text(
                        "👥 1 Passenger\n💰 Estimated fare: ₱25",
                        23
                );

        fareText.setTypeface(
                null,
                Typeface.BOLD
        );

        fareText.setGravity(
                Gravity.CENTER
        );

        fareText.setTextColor(
                Color.rgb(0, 125, 75)
        );

        fareText.setBackgroundColor(
                Color.WHITE
        );

        content.addView(
                fareText,
                full()
        );

        statusText =
                text(
                        "🟢 Ready to book",
                        16
                );

        statusText.setTypeface(
                null,
                Typeface.BOLD
        );

        statusText.setGravity(
                Gravity.CENTER
        );

        content.addView(
                statusText,
                full()
        );

        driverInfoText =
                text(
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

        content.addView(
                driverInfoText,
                full()
        );

        bookButton =
                button(
                        "🛺 BOOK A RIDE"
                );

        bookButton.setTextSize(19);

        bookButton.setBackgroundColor(
                Color.rgb(0, 150, 80)
        );

        bookButton.setOnClickListener(
                v -> bookRide(payment)
        );

        content.addView(
                bookButton
        );

        mapButton =
                button(
                        "🗺️ LIVE RIDE MAP"
                );

        mapButton.setOnClickListener(
                v -> openLiveMap()
        );

        content.addView(
                mapButton
        );

        chatButton =
                button(
                        "💬 CHAT WITH DRIVER"
                );

        chatButton.setOnClickListener(
                v -> openChat()
        );

        content.addView(
                chatButton
        );

        cancelButton =
                button(
                        "❌ CANCEL RIDE"
                );

        cancelButton.setBackgroundColor(
                Color.rgb(190, 55, 55)
        );

        cancelButton.setOnClickListener(
                v -> cancelRide()
        );

        content.addView(
                cancelButton
        );

        Button historyButton =
                button(
                        "📜 RIDE HISTORY"
                );

        historyButton.setBackgroundColor(
                Color.rgb(65, 90, 100)
        );

        historyButton.setOnClickListener(
                v -> showHistory()
        );

        content.addView(
                historyButton
        );

        Button logoutButton =
                button(
                        "🚪 LOGOUT"
                );

        logoutButton.setBackgroundColor(
                Color.rgb(85, 85, 85)
        );

        logoutButton.setOnClickListener(
                v -> logout()
        );

        content.addView(
                logoutButton
        );

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

        Button b =
                new Button(this);

        b.setText(number);

        b.setTextSize(18);

        b.setAllCaps(false);

        b.setTypeface(
                null,
                Typeface.BOLD
        );

        b.setTextColor(
                Color.WHITE
        );

        b.setPadding(
                4,
                10,
                4,
                10
        );

        b.setBackgroundColor(
                Color.rgb(125, 135, 135)
        );

        b.setOnClickListener(
                v -> selectPassengerCount(count)
        );

        return b;
    }

    private LinearLayout.LayoutParams passengerButtonParams() {

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(
                        0,
                        -2,
                        1
                );

        p.setMargins(
                4,
                5,
                4,
                5
        );

        return p;
    }

    private void selectPassengerCount(
            int count
    ) {

        passengerCount =
                Math.max(
                        1,
                        Math.min(
                                4,
                                count
                        )
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
                Color.rgb(
                        0,
                        150,
                        80
                );

        int normal =
                Color.rgb(
                        125,
                        135,
                        135
                );

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

        TextView t =
                new TextView(this);

        t.setText(value);

        t.setTextSize(size);

        t.setTextColor(
                Color.DKGRAY
        );

        t.setPadding(
                8,
                8,
                8,
                8
        );

        return t;
    }

    private Button button(
            String value
    ) {

        Button b =
                new Button(this);

        b.setText(value);

        b.setTextSize(16);

        b.setAllCaps(false);

        b.setTypeface(
                null,
                Typeface.BOLD
        );

        b.setTextColor(
                Color.WHITE
        );

        b.setPadding(
                12,
                12,
                12,
                12
        );

        b.setBackgroundColor(
                Color.rgb(
                        0,
                        125,
                        75
                )
        );

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                );

        p.setMargins(
                0,
                7,
                0,
                7
        );

        b.setLayoutParams(p);

        return b;
    }

    private LinearLayout.LayoutParams full() {

        return new LinearLayout.LayoutParams(
                -1,
                -2
        );
    }

    /*
     * ============================================================
     * FARE
     * ============================================================
     */

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

            return ((Number) value)
                    .doubleValue();
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

    /*
     * ============================================================
     * BOOKING
     * ============================================================
     */

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

        updateButtons();

        final long bookingGeneration =
                ++rideGeneration;

        db.collection("rides")
                .whereEqualTo(
                        "passengerId",
                        user.getUid()
                )
                .get(Source.SERVER)
                .addOnSuccessListener(query -> {

                    if (
                            bookingGeneration
                                    !=
                                    rideGeneration
                    ) {
                        return;
                    }

                    DocumentSnapshot existing =
                            findActiveRide(query);

                    /*
                     * RED MANTRA:
                     *
                     * Only a REAL valid active ride gets here.
                     *
                     * Old DRIVER_ARRIVED without driverId
                     * was already rejected by findActiveRide().
                     */
                    if (existing != null) {

                        attachToRide(
                                existing,
                                bookingGeneration
                        );

                        bookingInProgress = false;

                        updateButtons();

                        Toast.makeText(
                                this,
                                "You already have an active ride.",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    /*
                     * NO VALID SERVER RIDE.
                     *
                     * Completely remove stale local ride state.
                     */
                    clearRideInternal();

                    /*
                     * New booking generation.
                     */
                    final long createGeneration =
                            ++rideGeneration;

                    bookingInProgress = true;

                    String paymentMethod =
                            "Cash";

                    int selected =
                            paymentGroup
                                    .getCheckedRadioButtonId();

                    if (selected != -1) {

                        View selectedView =
                                paymentGroup
                                        .findViewById(
                                                selected
                                        );

                        if (
                                selectedView
                                        instanceof RadioButton
                        ) {

                            paymentMethod =
                                    ((RadioButton)
                                            selectedView)
                                            .getText()
                                            .toString();
                        }
                    }

                    createRideAfterCheck(
                            user.getUid(),
                            pickup,
                            destination,
                            paymentMethod,
                            createGeneration
                    );
                })
                .addOnFailureListener(e -> {

                    if (
                            bookingGeneration
                                    !=
                                    rideGeneration
                    ) {
                        return;
                    }

                    bookingInProgress = false;

                    updateButtons();

                    Toast.makeText(
                            this,
                            "Unable to check previous booking:\n"
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void createRideAfterCheck(
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

        Map<String, Object> ride =
                new HashMap<>();

        ride.put(
                "passengerId",
                passengerId
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
                "fare",
                fare
        );

        ride.put(
                "distanceKm",
                distanceKm
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
                "passengerCount",
                passengerCount
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
                "status",
                "REQUESTED"
        );

        ride.put(
                "createdAt",
                System.currentTimeMillis()
        );

        ride.put(
                "statusUpdatedAt",
                System.currentTimeMillis()
        );

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
                            Color.rgb(
                                    0,
                                    110,
                                    70
                            )
                    );

                    driverInfoText.setText(
                            "👤 DRIVER: Waiting for driver..."
                    );

                    listenToRide(
                            activeRideId
                    );

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

    /*
     * ============================================================
     * FIND ACTIVE RIDE
     * ============================================================
     *
     * RED MANTRA:
     *
     * 1. Terminal rides NEVER block.
     * 2. Old REQUESTED rides NEVER block after timeout.
     * 3. Driver-stage rides WITHOUT driverId NEVER block.
     * 4. Sort by the actual createdAt field.
     * ============================================================
     */

    private DocumentSnapshot findActiveRide(
            QuerySnapshot query
    ) {

        if (
                query == null
                        ||
                        query.isEmpty()
        ) {
            return null;
        }

        List<DocumentSnapshot> rides =
                new ArrayList<>(
                        query.getDocuments()
                );

        /*
         * IMPORTANT FIX:
         *
         * The previous version used readTime(DocumentSnapshot),
         * which returned 0 because readTime() expects a field value.
         *
         * Now we REALLY sort by createdAt.
         */
        Collections.sort(
                rides,
                new Comparator<DocumentSnapshot>() {

                    @Override
                    public int compare(
                            DocumentSnapshot a,
                            DocumentSnapshot b
                    ) {

                        return Long.compare(
                                readTime(
                                        b.get(
                                                "createdAt"
                                        )
                                ),
                                readTime(
                                        a.get(
                                                "createdAt"
                                        )
                                )
                        );
                    }
                }
        );

        long now =
                System.currentTimeMillis();

        for (
                DocumentSnapshot ride :
                rides
        ) {

            if (
                    ride == null
                            ||
                            !ride.exists()
            ) {
                continue;
            }

            String status =
                    safeStatus(
                            ride.getString(
                                    "status"
                            )
                    );

            /*
             * RED MANTRA #1
             *
             * Terminal ride = NEVER ACTIVE.
             */
            if (hasTerminalMarker(ride)) {
                continue;
            }

            /*
             * Explicit terminal statuses.
             */
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
                continue;
            }

            if (!isActive(status)) {
                continue;
            }

            /*
             * RED MANTRA #2
             *
             * REQUESTED older than 15 minutes = stale.
             */
            if (
                    "REQUESTED".equalsIgnoreCase(status)
            ) {

                long created =
                        readTime(
                                ride.get(
                                        "createdAt"
                                )
                        );

                if (
                        created > 0
                                &&
                                now - created
                                        >
                                        REQUEST_TIMEOUT_MS
                ) {
                    continue;
                }
            }

            /*
             * RED MANTRA #3
             *
             * These statuses MUST have a real driver.
             *
             * This kills the exact broken record:
             *
             * DRIVER_ARRIVED
             * +
             * NO driverId
             */
            if (requiresDriver(status)) {

                String driverId =
                        ride.getString(
                                "driverId"
                        );

                if (
                        driverId == null
                                ||
                                driverId.trim().isEmpty()
                ) {

                    continue;
                }
            }

            /*
             * ONLY a genuinely valid active ride
             * can block a new booking.
             */
            return ride;
        }

        return null;
    }

    private boolean requiresDriver(
            String status
    ) {

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

    private boolean isActive(
            String status
    ) {

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
                        ride.getString(
                                "status"
                        )
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
                readTime(
                        ride.get(
                                "completedAt"
                        )
                ) > 0

                        ||

                readTime(
                        ride.get(
                                "finishedAt"
                        )
                ) > 0

                        ||

                readTime(
                        ride.get(
                                "cancelledAt"
                        )
                ) > 0

                        ||

                readTime(
                        ride.get(
                                "declinedAt"
                        )
                ) > 0

                        ||

                readTime(
                        ride.get(
                                "expiredAt"
                        )
                ) > 0;
    }

    private String safeStatus(
            String value
    ) {

        if (
                value == null
                        ||
                        value.trim().isEmpty()
        ) {
            return "";
        }

        return value.trim();
    }

    private long readTime(
            Object value
    ) {

        if (value instanceof Number) {

            return ((Number) value)
                    .longValue();
        }

        if (
                value instanceof
                        com.google.firebase.Timestamp
        ) {

            return ((com.google.firebase.Timestamp)
                    value)
                    .toDate()
                    .getTime();
        }

        if (value instanceof Date) {

            return ((Date) value)
                    .getTime();
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
     * ATTACH RIDE
     * ============================================================
     */

    private void attachToRide(
            DocumentSnapshot ride,
            long generation
    ) {

        if (
                ride == null
                        ||
                        !ride.exists()
        ) {

            clearRide();

            return;
        }

        if (
                generation
                        !=
                        rideGeneration
        ) {
            return;
        }

        /*
         * RED MANTRA:
         * Finished ride cannot be attached.
         */
        if (hasTerminalMarker(ride)) {

            clearRide();

            updateButtons();

            return;
        }

        String rideId =
                ride.getId();

        String status =
                safeStatus(
                        ride.getString(
                                "status"
                        )
                );

        if (!isActive(status)) {

            clearRide();

            updateButtons();

            return;
        }

        /*
         * RED MANTRA:
         *
         * Driver-stage status with no driverId
         * is broken/stale and MUST NOT be attached.
         */
        if (requiresDriver(status)) {

            String driverId =
                    ride.getString(
                            "driverId"
                    );

            if (
                    driverId == null
                            ||
                            driverId.trim().isEmpty()
            ) {

                clearRide();

                updateButtons();

                return;
            }
        }

        activeRideId =
                rideId;

        activeRideStatus =
                status;

        Object count =
                ride.get(
                        "passengerCount"
                );

        if (count instanceof Number) {

            passengerCount =
                    Math.max(
                            1,
                            Math.min(
                                    4,
                                    ((Number)
                                            count)
                                            .intValue()
                            )
                    );
        }

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

        if (
                pickup != null
                        &&
                        !pickup.equals(
                                "Unknown"
                        )
        ) {

            pickupInput.setText(
                    pickup
            );
        }

        if (
                destination != null
                        &&
                        !destination.equals(
                                "Unknown"
                        )
        ) {

            destinationInput.setText(
                    destination
            );
        }

        pickupLat =
                number(
                        ride.get(
                                "pickupLatitude"
                        ),
                        pickupLat
                );

        pickupLng =
                number(
                        ride.get(
                                "pickupLongitude"
                        ),
                        pickupLng
                );

        destinationLat =
                number(
                        ride.get(
                                "destinationLatitude"
                        ),
                        destinationLat
                );

        destinationLng =
                number(
                        ride.get(
                                "destinationLongitude"
                        ),
                        destinationLng
                );

        prefs.edit()
                .putString(
                        "activeRideId",
                        activeRideId
                )
                .apply();

        updateStatusText(
                status
        );

        updatePassengerSelectionUI();

        calculateFare();

        listenToRide(
                rideId
        );

        updateButtons();
    }

    /*
     * ============================================================
     * REALTIME RIDE LISTENER
     * ============================================================
     */

    private void listenToRide(
            String rideId
    ) {

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

                                    /*
                                     * RED MANTRA:
                                     *
                                     * Old callbacks cannot update
                                     * the current screen.
                                     */
                                    if (
                                            generation
                                                    !=
                                                    rideGeneration
                                    ) {
                                        return;
                                    }

                                    if (
                                            activeRideId
                                                    == null
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

                                        clearRide();

                                        updateButtons();

                                        return;
                                    }

                                    /*
                                     * Finished/cancelled ride
                                     * disappears immediately.
                                     */
                                    if (
                                            hasTerminalMarker(
                                                    snapshot
                                            )
                                    ) {

                                        clearRide();

                                        updateButtons();

                                        return;
                                    }

                                    String status =
                                            safeStatus(
                                                    snapshot.getString(
                                                            "status"
                                                    )
                                            );

                                    if (
                                            !isActive(status)
                                    ) {

                                        clearRide();

                                        statusText.setText(
                                                "🟢 READY TO BOOK"
                                        );

                                        updateButtons();

                                        return;
                                    }

                                    /*
                                     * RED MANTRA:
                                     *
                                     * NEVER show:
                                     *
                                     * DRIVER HAS ARRIVED
                                     * DRIVER: Waiting for driver
                                     *
                                     * at the same time.
                                     */
                                    if (
                                            requiresDriver(
                                                    status
                                            )
                                    ) {

                                        String driverId =
                                                snapshot.getString(
                                                        "driverId"
                                                );

                                        if (
                                                driverId == null
                                                        ||
                                                        driverId.trim().isEmpty()
                                        ) {

                                            clearRide();

                                            statusText.setText(
                                                    "🟢 READY TO BOOK"
                                            );

                                            driverInfoText.setText(
                                                    "👤 DRIVER: Waiting for driver..."
                                            );

                                            updateButtons();

                                            return;
                                        }
                                    }

                                    /*
                                     * ONLY NOW can this ride
                                     * control the passenger screen.
                                     */
                                    activeRideStatus =
                                            status;

                                    updateStatusText(
                                            status
                                    );

                                    showDriverInformation(
                                            snapshot,
                                            generation,
                                            listeningRideId
                                    );

                                    Object count =
                                            snapshot.get(
                                                    "passengerCount"
                                            );

                                    if (
                                            count
                                                    instanceof Number
                                    ) {

                                        passengerCount =
                                                Math.max(
                                                        1,
                                                        Math.min(
                                                                4,
                                                                ((Number)
                                                                        count)
                                                                        .intValue()
                                                )
                                                );

                                        updatePassengerSelectionUI();
                                    }

                                    updateButtons();
                                }
                        );
    }

    private void updateStatusText(
            String status
    ) {

        if (statusText == null) {
            return;
        }

        if (
                "REQUESTED".equalsIgnoreCase(
                        status
                )
        ) {

            statusText.setText(
                    "🔎 LOOKING FOR DRIVER..."
            );

            statusText.setTextColor(
                    Color.rgb(
                            0,
                            110,
                            70
                    )
            );

        } else if (
                "ACCEPTED".equalsIgnoreCase(
                        status
                )
        ) {

            statusText.setText(
                    "✅ DRIVER ACCEPTED YOUR RIDE"
            );

            statusText.setTextColor(
                    Color.rgb(
                            0,
                            130,
                            70
                    )
            );

        } else if (
                "DRIVER_ON_THE_WAY".equalsIgnoreCase(
                        status
                )
        ) {

            statusText.setText(
                    "🚗 DRIVER IS ON THE WAY"
            );

        } else if (
                "DRIVER_ARRIVED".equalsIgnoreCase(
                        status
                )
                        ||
                        "ARRIVED".equalsIgnoreCase(
                                status
                        )
        ) {

            statusText.setText(
                    "📍 DRIVER HAS ARRIVED"
            );

        } else if (
                "IN_PROGRESS".equalsIgnoreCase(
                        status
                )
                        ||
                        "ONGOING".equalsIgnoreCase(
                                status
                        )
        ) {

            statusText.setText(
                    "🛺 RIDE IN PROGRESS"
            );

        } else {

            statusText.setText(
                    "🟢 " + status
            );
        }
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

        String driverId =
                ride.getString(
                        "driverId"
                );

        if (
                driverId == null
                        ||
                        driverId.trim().isEmpty()
        ) {

            driverInfoText.setText(
                    "👤 DRIVER: Waiting for driver..."
            );

            return;
        }

        final String expectedRideId =
                rideId;

        final long expectedGeneration =
                generation;

        db.collection("users")
                .document(driverId)
                .get()
                .addOnSuccessListener(user -> {

                    /*
                     * RED MANTRA:
                     *
                     * Old user lookup cannot overwrite
                     * the current booking.
                     */
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

                    if (
                            user == null
                                    ||
                                    !user.exists()
                    ) {

                        driverInfoText.setText(
                                "👤 DRIVER: Assigned"
                        );

                        return;
                    }

                    String name =
                            safe(
                                    user.getString(
                                            "name"
                                    ),
                                    user.getString(
                                            "fullName"
                                    )
                            );

                    String phone =
                            safe(
                                    user.getString(
                                            "phone"
                                    ),
                                    user.getString(
                                            "phoneNumber"
                                    )
                            );

                    String vehicle =
                            safe(
                                    user.getString(
                                            "vehicle"
                                    ),
                                    user.getString(
                                            "tricycleNumber"
                                    )
                            );

                    StringBuilder info =
                            new StringBuilder();

                    info.append(
                            "👤 DRIVER: "
                    );

                    info.append(
                            name
                    );

                    if (
                            phone != null
                                    &&
                                    !phone.equals(
                                            "Unknown"
                                    )
                    ) {

                        info.append(
                                "\n📞 "
                        );

                        info.append(
                                phone
                        );
                    }

                    if (
                            vehicle != null
                                    &&
                                    !vehicle.equals(
                                            "Unknown"
                                    )
                    ) {

                        info.append(
                                "\n🛺 "
                        );

                        info.append(
                                vehicle
                        );
                    }

                    driverInfoText.setText(
                            info.toString()
                    );
                });
    }

    /*
     * ============================================================
     * CLEAR RIDE
     * ============================================================
     */

    private void clearRideInternal() {

        if (rideListener != null) {

            rideListener.remove();

            rideListener = null;
        }

        activeRideId = null;

        activeRideStatus = "";

        prefs.edit()
                .remove(
                        "activeRideId"
                )
                .apply();

        if (driverInfoText != null) {

            driverInfoText.setText(
                    "👤 DRIVER: Waiting for driver..."
            );
        }
    }

    private void clearRide() {

        /*
         * IMPORTANT:
         *
         * Increment FIRST.
         *
         * This invalidates every old Firestore callback
         * before clearing the ride.
         */
        rideGeneration++;

        clearRideInternal();

        bookingInProgress = false;

        if (statusText != null) {

            statusText.setText(
                    "🟢 Ready to book"
            );

            statusText.setTextColor(
                    Color.rgb(
                            0,
                            110,
                            70
                    )
            );
        }
    }

    private void restoreSavedRide() {

        /*
         * Deliberately do nothing with the saved ID.
         *
         * Firebase SERVER is authoritative.
         */
        prefs.getString(
                "activeRideId",
                null
        );
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

        if (
                name == null
                        ||
                        name.trim().isEmpty()
        ) {

            name =
                    data.getStringExtra(
                            "destinationName"
                    );
        }

        if (
                name != null
                        &&
                        !name.trim().isEmpty()
        ) {

            destinationInput.setText(
                    name
            );

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
                            address.getAddressLine(
                                    i
                            );

                    if (
                            line != null
                                    &&
                                    !line.trim().isEmpty()
                    ) {

                        if (name.length() > 0) {

                            name.append(
                                    ", "
                            );
                        }

                        name.append(
                                line
                        );
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

        final String currentRideId =
                activeRideId;

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
                currentRideId
        );

        startActivity(intent);
    }

    /*
     * ============================================================
     * CHAT
     * ============================================================
     *
     * PassengerActivity only passes the CURRENT ride ID.
     *
     * RideChatActivity must separately clear its old listener/UI.
     * ============================================================
     */

    private void openChat() {

        if (!hasActiveRide()) {
            return;
        }

        final String currentRideId =
                activeRideId;

        if (
                currentRideId == null
                        ||
                        currentRideId.trim().isEmpty()
        ) {
            return;
        }

        Intent intent =
                new Intent(
                        this,
                        RideChatActivity.class
                );

        intent.putExtra(
                "ride_id",
                currentRideId
        );

        /*
         * Keep both names for compatibility.
         */
        intent.putExtra(
                "rideId",
                currentRideId
        );

        startActivity(intent);
    }

    private boolean hasActiveRide() {

        /*
         * RED MANTRA:
         *
         * Local ID alone is NOT enough.
         */
        if (
                activeRideId == null
                        ||
                        activeRideId.trim().isEmpty()
                        ||
                        !isActive(
                                activeRideStatus
                        )
        ) {

            Toast.makeText(
                    this,
                    "No active ride.",
                    Toast.LENGTH_SHORT
            ).show();

            return false;
        }

        /*
         * A driver-stage ride without driverId
         * is never considered valid.
         */
        if (
                requiresDriver(
                        activeRideStatus
                )
        ) {

            Toast.makeText(
                    this,
                    "No valid active ride.",
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
                                            .document(
                                                    rideId
                                            )
                            );

                    if (!ride.exists()) {

                        throw new IllegalStateException(
                                "Ride no longer exists."
                        );
                    }

                    String status =
                            safeStatus(
                                    ride.getString(
                                            "status"
                                    )
                            );

                    if (
                            !"REQUESTED"
                                    .equalsIgnoreCase(
                                            status
                                    )
                    ) {

                        throw new IllegalStateException(
                                "The driver has already accepted this booking."
                        );
                    }

                    transaction.update(
                            ride.getReference(),
                            "status",
                            "CANCELLED",
                            "cancelledBy",
                            "PASSENGER",
                            "cancelledAt",
                            System.currentTimeMillis(),
                            "statusUpdatedAt",
                            System.currentTimeMillis()
                    );

                    return null;
                }
        )
                .addOnSuccessListener(
                        v -> {

                            clearRide();

                            statusText.setText(
                                    "❌ RIDE CANCELLED"
                            );

                            updateButtons();
                        }
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

        image.setAdjustViewBounds(
                true
        );

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

        title.setGravity(
                Gravity.CENTER
        );

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
                            address.getAddressLine(
                                    i
                            );

                    if (
                            line != null
                                    &&
                                    !line.trim().isEmpty()
                    ) {

                        if (name.length() > 0) {

                            name.append(
                                    ", "
                            );
                        }

                        name.append(
                                line
                        );
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
                .addOnSuccessListener(
                        query -> {

                            if (
                                    query == null
                                            ||
                                            query.isEmpty()
                            ) {

                                new AlertDialog.Builder(
                                        this
                                )
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
                                    (
                                            a,
                                            b
                                    ) ->
                                            Long.compare(
                                                    readTime(
                                                            b.get(
                                                                    "createdAt"
                                                            )
                                                    ),
                                                    readTime(
                                                            a.get(
                                                                    "createdAt"
                                                            )
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
                                        number(
                                                ride.get(
                                                        "fare"
                                                ),
                                                0
                                        );

                                String payment =
                                        safe(
                                                ride.getString(
                                                        "paymentMethod"
                                                ),
                                                "Not provided"
                                        );

                                long created =
                                        readTime(
                                                ride.get(
                                                        "createdAt"
                                                )
                                        );

                                history.append(
                                        "🗓️ "
                                                + (
                                                created > 0
                                                        ? format.format(
                                                        new Date(
                                                                created
                                                        )
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

                            new AlertDialog.Builder(
                                    this
                            )
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
                        }
                )
                .addOnFailureListener(
                        e -> Toast.makeText(
                                this,
                                "History failed:\n"
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    /*
     * ============================================================
     * HELPERS
     * ============================================================
     */

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

    private void updateButtons() {

        boolean active =
                activeRideId != null
                        &&
                        !activeRideId.trim().isEmpty()
                        &&
                        isActive(
                                activeRideStatus
                        );

        /*
         * A driver-stage ride without driverId
         * is never allowed to remain active locally.
         */
        if (
                active
                        &&
                        requiresDriver(
                                activeRideStatus
                        )
        ) {

            active = false;
        }

        if (bookButton != null) {

            bookButton.setEnabled(
                    !bookingInProgress
            );

            if (active) {

                bookButton.setText(
                        "🛺 NEW BOOKING / CHECK RIDE"
                );

            } else {

                bookButton.setText(
                        "🛺 BOOK A RIDE"
                );
            }
        }

        if (mapButton != null) {

            mapButton.setEnabled(
                    active
            );
        }

        if (chatButton != null) {

            chatButton.setEnabled(
                    active
            );
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

            for (
                    int result :
                    grantResults
            ) {

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

        /*
         * Invalidate every outstanding callback.
         */
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
