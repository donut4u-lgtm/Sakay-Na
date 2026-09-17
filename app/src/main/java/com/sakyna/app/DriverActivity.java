
package com.sakyna.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DriverActivity extends Activity {

    private static final long REQUEST_EXPIRATION_MS =
            30L * 60L * 1000L;

    private static final long REFRESH_INTERVAL_MS =
            30L * 1000L;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private LocationManager locationManager;
    private LocationListener locationListener;

    private TextView statusText;
    private TextView requestsText;
    private TextView currentRideText;

    private Button onlineButton;
    private Button offlineButton;

    private Button acceptButton;
    private Button declineButton;
    private Button onTheWayButton;
    private Button arrivedButton;
    private Button startRideButton;
    private Button finishRideButton;
    private Button mapButton;
    private Button chatButton;

    private LinearLayout requestContainer;

    private boolean driverOnline = false;

    private double driverLatitude = 0.0;
    private double driverLongitude = 0.0;

    private String currentRideId = "";
    private String currentRideStatus = "";

    private ListenerRegistration requestListener;
    private ListenerRegistration rideListener;

    private final Handler refreshHandler =
            new Handler(Looper.getMainLooper());

    private final ExecutorService geocoderExecutor =
            Executors.newSingleThreadExecutor();

    private final Runnable refreshRunnable =
            new Runnable() {
                @Override
                public void run() {

                    if (!isFinishing()) {
                        listenForRideRequests();
                        listenForCurrentRide();

                        refreshHandler.postDelayed(
                                this,
                                REFRESH_INTERVAL_MS
                        );
                    }
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (auth.getCurrentUser() == null) {
            finish();
            return;
        }

        buildScreen();
        loadDriverStatus();
        startLocationUpdates();

        listenForRideRequests();
        listenForCurrentRide();

        refreshHandler.postDelayed(
                refreshRunnable,
                REFRESH_INTERVAL_MS
        );
    }

    private void buildScreen() {

        ScrollView scrollView = new ScrollView(this);

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setPadding(
                20,
                20,
                20,
                30
        );

        root.setBackgroundColor(
                Color.rgb(245, 248, 252)
        );

        TextView title =
                new TextView(this);

        title.setText(
                "🚕 SAKAY NA - DRIVER"
        );

        title.setTextSize(27);
        title.setTextColor(
                Color.rgb(15, 90, 170)
        );

        title.setGravity(
                Gravity.CENTER
        );

        title.setPadding(
                0,
                15,
                0,
                15
        );

        root.addView(title);

        statusText =
                new TextView(this);

        statusText.setText(
                "Checking driver status..."
        );

        statusText.setTextSize(18);
        statusText.setGravity(
                Gravity.CENTER
        );

        statusText.setPadding(
                10,
                18,
                10,
                18
        );

        root.addView(statusText);

        LinearLayout onlineRow =
                new LinearLayout(this);

        onlineRow.setOrientation(
                LinearLayout.HORIZONTAL
        );

        onlineButton =
                makeColoredButton(
                        "🟢 GO ONLINE",
                        Color.rgb(35, 170, 80)
                );

        offlineButton =
                makeColoredButton(
                        "⚪ GO OFFLINE",
                        Color.rgb(110, 110, 110)
                );

        onlineRow.addView(
                onlineButton,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        offlineRowSpacer(onlineRow);

        onlineRow.addView(
                offlineButton,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        root.addView(onlineRow);

        Button profileButton =
                makeColoredButton(
                        "👤 DRIVER PROFILE",
                        Color.rgb(40, 120, 210)
                );

        root.addView(profileButton);

        TextView requestTitle =
                sectionTitle(
                        "🛎️ RIDE REQUESTS"
                );

        root.addView(requestTitle);

        requestsText =
                new TextView(this);

        requestsText.setText(
                "Loading ride requests..."
        );

        requestsText.setTextSize(17);
        requestsText.setTextColor(
                Color.rgb(70, 70, 70)
        );

        requestsText.setPadding(
                10,
                10,
                10,
                10
        );

        root.addView(requestsText);

        requestContainer =
                new LinearLayout(this);

        requestContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        root.addView(requestContainer);

        TextView currentTitle =
                sectionTitle(
                        "🚕 CURRENT RIDE"
                );

        root.addView(currentTitle);

        currentRideText =
                new TextView(this);

        currentRideText.setText(
                "No current ride."
        );

        currentRideText.setTextSize(17);
        currentRideText.setTextColor(
                Color.DKGRAY
        );

        currentRideText.setPadding(
                15,
                15,
                15,
                15
        );

        root.addView(currentRideText);

        acceptButton =
                makeColoredButton(
                        "✅ ACCEPT RIDE",
                        Color.rgb(35, 160, 75)
                );

        declineButton =
                makeColoredButton(
                        "❌ DECLINE RIDE",
                        Color.rgb(205, 55, 55)
                );

        onTheWayButton =
                makeColoredButton(
                        "🚦 I'M ON THE WAY",
                        Color.rgb(245, 150, 30)
                );

        arrivedButton =
                makeColoredButton(
                        "📍 I HAVE ARRIVED",
                        Color.rgb(40, 125, 210)
                );

        startRideButton =
                makeColoredButton(
                        "🚕 START RIDE",
                        Color.rgb(30, 145, 145)
                );

        finishRideButton =
                makeColoredButton(
                        "🏁 FINISH RIDE",
                        Color.rgb(110, 75, 180)
                );

        mapButton =
                makeColoredButton(
                        "🗺️ LIVE RIDE MAP",
                        Color.rgb(35, 120, 180)
                );

        chatButton =
                makeColoredButton(
                        "💬 CHAT WITH PASSENGER",
                        Color.rgb(120, 80, 180)
                );

        root.addView(acceptButton);
        root.addView(declineButton);
        root.addView(onTheWayButton);
        root.addView(arrivedButton);
        root.addView(startRideButton);
        root.addView(finishRideButton);
        root.addView(mapButton);
        root.addView(chatButton);

        Button logoutButton =
                makeColoredButton(
                        "🚪 LOGOUT",
                        Color.rgb(90, 90, 90)
                );

        root.addView(logoutButton);

        hideRideButtons();

        scrollView.addView(root);

        setContentView(scrollView);

        onlineButton.setOnClickListener(
                v -> setDriverOnline(true)
        );

        offlineButton.setOnClickListener(
                v -> setDriverOnline(false)
        );

        profileButton.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            DriverActivity.this,
                            DriverOnboardingActivity.class
                    );

            startActivity(intent);
        });

        acceptButton.setOnClickListener(v -> {

            if (!currentRideId.isEmpty()) {
                acceptRide(currentRideId);
            }
        });

        declineButton.setOnClickListener(v -> {

            if (!currentRideId.isEmpty()) {
                declineRide(currentRideId);
            }
        });

        onTheWayButton.setOnClickListener(
                v -> updateRideStatus(
                        "DRIVER_ON_THE_WAY"
                )
        );

        arrivedButton.setOnClickListener(
                v -> updateRideStatus(
                        "DRIVER_ARRIVED"
                )
        );

        startRideButton.setOnClickListener(
                v -> updateRideStatus(
                        "IN_PROGRESS"
                )
        );

        finishRideButton.setOnClickListener(
                v -> updateRideStatus(
                        "FINISHED"
                )
        );

        mapButton.setOnClickListener(
                v -> openMap()
        );

        chatButton.setOnClickListener(
                v -> openChat()
        );

        logoutButton.setOnClickListener(
                v -> logout()
        );
    }

    private void offlineRowSpacer(
            LinearLayout row
    ) {

        TextView spacer =
                new TextView(this);

        row.addView(
                spacer,
                new LinearLayout.LayoutParams(
                        10,
                        1
                )
        );
    }

    private TextView sectionTitle(
            String text
    ) {

        TextView title =
                new TextView(this);

        title.setText(text);
        title.setTextSize(21);
        title.setTextColor(
                Color.rgb(15, 90, 170)
        );

        title.setPadding(
                5,
                25,
                5,
                10
        );

        return title;
    }

    private Button makeButton(
            String text
    ) {

        Button button =
                new Button(this);

        button.setText(text);

        return button;
    }

    private Button makeColoredButton(
            String text,
            int color
    ) {

        Button button =
                new Button(this);

        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(15);

        GradientDrawable background =
                new GradientDrawable();

        background.setColor(color);
        background.setCornerRadius(22);

        button.setBackground(
                background
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

        button.setLayoutParams(params);

        return button;
    }

    private void loadDriverStatus() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            return;
        }

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {

                    Boolean online =
                            doc.getBoolean(
                                    "driverOnline"
                            );

                    driverOnline =
                            online != null
                                    && online;

                    updateOnlineDisplay();
                    listenForRideRequests();
                })
                .addOnFailureListener(e ->
                        statusText.setText(
                                "Unable to load driver status:\n"
                                        + e.getMessage()
                        )
                );
    }

    private void setDriverOnline(
            boolean online
    ) {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            return;
        }

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "driverOnline",
                online
        );

        data.put(
                "driverAvailability",
                online
                        ? "ONLINE"
                        : "OFFLINE"
        );

        data.put(
                "availabilityUpdatedAt",
                FieldValue.serverTimestamp()
        );

        db.collection("users")
                .document(user.getUid())
                .update(data)
                .addOnSuccessListener(v -> {

                    driverOnline = online;

                    updateOnlineDisplay();

                    listenForRideRequests();

                    uploadDriverLocation();

                    Toast.makeText(
                            this,
                            online
                                    ? "You are now ONLINE."
                                    : "You are now OFFLINE.",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Unable to change status:\n"
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void updateOnlineDisplay() {

        if (driverOnline) {

            statusText.setText(
                    "🟢 ONLINE - ACCEPTING RIDE REQUESTS"
            );

            statusText.setTextColor(
                    Color.rgb(0, 130, 60)
            );

            setButtonColor(
                    onlineButton,
                    Color.rgb(35, 170, 80)
            );

            setButtonColor(
                    offlineButton,
                    Color.rgb(160, 160, 160)
            );

        } else {

            statusText.setText(
                    "⚪ OFFLINE - NOT ACCEPTING RIDES"
            );

            statusText.setTextColor(
                    Color.rgb(150, 80, 60)
            );

            setButtonColor(
                    onlineButton,
                    Color.rgb(35, 150, 75)
            );

            setButtonColor(
                    offlineButton,
                    Color.rgb(90, 90, 90)
            );
        }
    }

    private void setButtonColor(
            Button button,
            int color
    ) {

        if (button == null) {
            return;
        }

        GradientDrawable background =
                new GradientDrawable();

        background.setColor(color);
        background.setCornerRadius(22);

        button.setBackground(
                background
        );

        button.setTextColor(
                Color.WHITE
        );
    }

    private void listenForRideRequests() {

        if (requestListener != null) {

            requestListener.remove();
            requestListener = null;
        }

        requestContainer.removeAllViews();

        if (!driverOnline) {

            requestsText.setText(
                    "Go ONLINE to receive ride requests."
            );

            return;
        }

        requestListener =
                db.collection("rides")
                        .whereEqualTo(
                                "status",
                                "REQUESTED"
                        )
                        .addSnapshotListener(
                                (snapshots, error) -> {

                                    requestContainer
                                            .removeAllViews();

                                    if (error != null) {

                                        requestsText.setText(
                                                "Unable to load ride requests:\n"
                                                        + error.getMessage()
                                        );

                                        return;
                                    }

                                    if (!driverOnline) {

                                        requestsText.setText(
                                                "Go ONLINE to receive ride requests."
                                        );

                                        return;
                                    }

                                    if (snapshots == null
                                            || snapshots.isEmpty()) {

                                        requestsText.setText(
                                                "No ride requests."
                                        );

                                        return;
                                    }

                                    int visibleCount = 0;

                                    for (
                                            DocumentSnapshot ride :
                                            snapshots.getDocuments()
                                    ) {

                                        if (isRideExpired(ride)) {

                                            expireRide(ride);
                                            continue;
                                        }

                                        visibleCount++;

                                        addRideCard(ride);
                                    }

                                    if (visibleCount == 0) {

                                        requestsText.setText(
                                                "No active ride requests."
                                        );

                                    } else {

                                        requestsText.setText(
                                                "🛎️ Available rides: "
                                                        + visibleCount
                                        );
                                    }
                                }
                        );
    }

    private boolean isRideExpired(
            DocumentSnapshot ride
    ) {

        Long createdAt =
                ride.getLong("createdAt");

        if (createdAt == null) {

            return false;
        }

        long age =
                System.currentTimeMillis()
                        - createdAt;

        return age >=
                REQUEST_EXPIRATION_MS;
    }

    private void expireRide(
            DocumentSnapshot ride
    ) {

        String rideId =
                ride.getId();

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "status",
                "EXPIRED"
        );

        data.put(
                "expiredAt",
                FieldValue.serverTimestamp()
        );

        db.collection("rides")
                .document(rideId)
                .update(data);
    }

    private void addRideCard(
            DocumentSnapshot ride
    ) {

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

        GradientDrawable cardBackground =
                new GradientDrawable();

        cardBackground.setColor(
                Color.WHITE
        );

        cardBackground.setStroke(
                2,
                Color.rgb(50, 135, 205)
        );

        cardBackground.setCornerRadius(
                24
        );

        card.setBackground(
                cardBackground
        );

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(
                0,
                8,
                0,
                12
        );

        card.setLayoutParams(
                cardParams
        );

        TextView header =
                new TextView(this);

        header.setText(
                "🚕 NEW RIDE REQUEST"
        );

        header.setTextSize(20);
        header.setTextColor(
                Color.rgb(15, 100, 175)
        );

        header.setPadding(
                0,
                0,
                0,
                12
        );

        card.addView(header);

        TextView info =
                new TextView(this);

        info.setTextSize(17);
        info.setTextColor(
                Color.rgb(45, 45, 45)
        );

        String pickup =
                value(
                        ride,
                        "pickup"
                );

        String destination =
                value(
                        ride,
                        "destination"
                );

        String payment =
                value(
                        ride,
                        "paymentMethod"
                );

        Double fareNumber =
                ride.getDouble("fare");

        String fare;

        if (fareNumber != null) {

            fare =
                    String.format(
                            Locale.US,
                            "%.0f",
                            fareNumber
                    );

        } else {

            fare =
                    value(
                            ride,
                            "fare"
                    );
        }

        String pickupText =
                pickup.isEmpty()
                        ? "Not provided"
                        : pickup;

        String destinationText =
                destination.isEmpty()
                        ? "Not provided"
                        : destination;

        String fareText =
                fare.isEmpty()
                        ? "0"
                        : fare;

        String paymentText =
                payment.isEmpty()
                        ? "Not specified"
                        : payment;

        info.setText(
                "📍 PICKUP\n"
                        + pickupText
                        + "\n\n🏁 DESTINATION\n"
                        + destinationText
                        + "\n\n💰 FARE\n₱"
                        + fareText
                        + "\n\n💳 PAYMENT\n"
                        + paymentText
        );

        card.addView(info);

        Button accept =
                makeColoredButton(
                        "✅ ACCEPT",
                        Color.rgb(35, 165, 75)
                );

        Button decline =
                makeColoredButton(
                        "❌ DECLINE",
                        Color.rgb(205, 55, 55)
                );

        card.addView(accept);
        card.addView(decline);

        requestContainer.addView(
                card
        );

        String rideId =
                ride.getId();

        resolveLocationNames(
                ride,
                info
        );

        accept.setOnClickListener(v ->
                acceptRide(rideId)
        );

        decline.setOnClickListener(v ->
                declineRide(rideId)
        );
    }

    private void resolveLocationNames(
            DocumentSnapshot ride,
            TextView info
    ) {

        Double pickupLat =
                ride.getDouble(
                        "pickupLatitude"
                );

        Double pickupLng =
                ride.getDouble(
                        "pickupLongitude"
                );

        Double destinationLat =
                ride.getDouble(
                        "destinationLatitude"
                );

        Double destinationLng =
                ride.getDouble(
                        "destinationLongitude"
                );

        if (pickupLat == null
                || pickupLng == null
                || destinationLat == null
                || destinationLng == null) {

            return;
        }

        geocoderExecutor.execute(() -> {

            String pickupName =
                    reverseGeocode(
                            pickupLat,
                            pickupLng
                    );

            String destinationName =
                    reverseGeocode(
                            destinationLat,
                            destinationLng
                    );

            runOnUiThread(() -> {

                if (isFinishing()) {
                    return;
                }

                String pickupText =
                        pickupName.isEmpty()
                                ? value(
                                        ride,
                                        "pickup"
                                )
                                : pickupName;

                String destinationText =
                        destinationName.isEmpty()
                                ? value(
                                        ride,
                                        "destination"
                                )
                                : destinationName;

                String payment =
                        value(
                                ride,
                                "paymentMethod"
                        );

                Double fareNumber =
                        ride.getDouble("fare");

                String fare =
                        fareNumber == null
                                ? value(
                                        ride,
                                        "fare"
                                )
                                : String.format(
                                        Locale.US,
                                        "%.0f",
                                        fareNumber
                                );

                info.setText(
                        "📍 PICKUP\n"
                                + (pickupText.isEmpty()
                                ? "Not provided"
                                : pickupText)
                                + "\n\n🏁 DESTINATION\n"
                                + (destinationText.isEmpty()
                                ? "Not provided"
                                : destinationText)
                                + "\n\n💰 FARE\n₱"
                                + (fare.isEmpty()
                                ? "0"
                                : fare)
                                + "\n\n💳 PAYMENT\n"
                                + (payment.isEmpty()
                                ? "Not specified"
                                : payment)
                );
            });
        });
    }

    private String reverseGeocode(
            double latitude,
            double longitude
    ) {

        try {

            Geocoder geocoder =
                    new Geocoder(
                            this,
                            Locale.getDefault()
                    );

            List<Address> addresses =
                    geocoder.getFromLocation(
                            latitude,
                            longitude,
                            1
                    );

            if (addresses != null
                    && !addresses.isEmpty()) {

                Address address =
                        addresses.get(0);

                String feature =
                        address.getFeatureName();

                if (feature != null
                        && !feature.trim().isEmpty()
                        && !feature.matches(
                        "^[0-9.\\-]+$"
                )) {

                    return feature;
                }

                String line =
                        address.getAddressLine(0);

                if (line != null
                        && !line.trim().isEmpty()) {

                    return line;
                }
            }

        } catch (Exception ignored) {
        }

        return "";
    }

    private String value(
            DocumentSnapshot doc,
            String field
    ) {

        String result =
                doc.getString(field);

        return result == null
                ? ""
                : result;
    }

    private void listenForCurrentRide() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            return;
        }

        if (rideListener != null) {

            rideListener.remove();
            rideListener = null;
        }

        rideListener =
                db.collection("rides")
                        .whereEqualTo(
                                "driverId",
                                user.getUid()
                        )
                        .addSnapshotListener(
                                (snapshots, error) -> {

                                    if (error != null) {

                                        currentRideText.setText(
                                                "Unable to load current ride:\n"
                                                        + error.getMessage()
                                        );

                                        return;
                                    }

                                    DocumentSnapshot active =
                                            null;

                                    if (snapshots != null) {

                                        for (
                                                DocumentSnapshot ride :
                                                snapshots.getDocuments()
                                        ) {

                                            String status =
                                                    ride.getString(
                                                            "status"
                                                    );

                                            if (
                                                    "ACCEPTED"
                                                            .equals(status)
                                                            ||
                                                    "DRIVER_ON_THE_WAY"
                                                            .equals(status)
                                                            ||
                                                    "DRIVER_ARRIVED"
                                                            .equals(status)
                                                            ||
                                                    "IN_PROGRESS"
                                                            .equals(status)
                                            ) {

                                                active = ride;
                                                break;
                                            }
                                        }
                                    }

                                    if (active == null) {

                                        currentRideId = "";
                                        currentRideStatus = "";

                                        currentRideText.setText(
                                                "No current ride."
                                        );

                                        hideRideButtons();

                                        return;
                                    }

                                    currentRideId =
                                            active.getId();

                                    currentRideStatus =
                                            value(
                                                    active,
                                                    "status"
                                            );

                                    String pickup =
                                            value(
                                                    active,
                                                    "pickup"
                                            );

                                    String destination =
                                            value(
                                                    active,
                                                    "destination"
                                            );

                                    currentRideText.setText(
                                            "🚦 Status: "
                                                    + currentRideStatus
                                                    + "\n\n📍 Pickup:\n"
                                                    + pickup
                                                    + "\n\n🏁 Destination:\n"
                                                    + destination
                                    );

                                    showRideButtons();
                                }
                        );
    }

    private void showRideButtons() {

        mapButton.setVisibility(
                View.VISIBLE
        );

        chatButton.setVisibility(
                View.VISIBLE
        );

        onTheWayButton.setVisibility(
                "ACCEPTED".equals(
                        currentRideStatus
                )
                        ? View.VISIBLE
                        : View.GONE
        );

        arrivedButton.setVisibility(
                "DRIVER_ON_THE_WAY"
                        .equals(
                                currentRideStatus
                        )
                        ? View.VISIBLE
                        : View.GONE
        );

        startRideButton.setVisibility(
                "DRIVER_ARRIVED"
                        .equals(
                                currentRideStatus
                        )
                        ? View.VISIBLE
                        : View.GONE
        );

        finishRideButton.setVisibility(
                "IN_PROGRESS"
                        .equals(
                                currentRideStatus
                        )
                        ? View.VISIBLE
                        : View.GONE
        );
    }

    private void hideRideButtons() {

        acceptButton.setVisibility(
                View.GONE
        );

        declineButton.setVisibility(
                View.GONE
        );

        onTheWayButton.setVisibility(
                View.GONE
        );

        arrivedButton.setVisibility(
                View.GONE
        );

        startRideButton.setVisibility(
                View.GONE
        );

        finishRideButton.setVisibility(
                View.GONE
        );

        mapButton.setVisibility(
                View.GONE
        );

        chatButton.setVisibility(
                View.GONE
        );
    }

    private void acceptRide(
            String rideId
    ) {

        FirebaseUser user =
                auth.getCurrentUser();

        if (
                user == null
                        || rideId == null
                        || rideId.isEmpty()
        ) {
            return;
        }

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "driverId",
                user.getUid()
        );

        data.put(
                "status",
                "ACCEPTED"
        );

        data.put(
                "acceptedAt",
                FieldValue.serverTimestamp()
        );

        data.put(
                "driverLatitude",
                driverLatitude
        );

        data.put(
                "driverLongitude",
                driverLongitude
        );

        db.collection("rides")
                .document(rideId)
                .update(data)
               
