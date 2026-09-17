
               
package com.sakyna.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
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

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DriverActivity extends Activity {

    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    private static final long REQUEST_EXPIRATION_MS =
            30L * 60L * 1000L;

    private static final long REFRESH_INTERVAL_MS =
            30L * 1000L;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private LocationManager locationManager;

    private double driverLatitude = 0.0;
    private double driverLongitude = 0.0;

    private boolean isOnline = false;

    private LinearLayout root;
    private LinearLayout requestContainer;
    private LinearLayout currentRideContainer;

    private TextView statusText;

    private Button onlineButton;
    private Button profileButton;
    private Button requestsButton;
    private Button currentRideButton;
    private Button earningsButton;
    private Button logoutButton;

    private Button onTheWayButton;
    private Button arrivedButton;
    private Button startRideButton;
    private Button finishRideButton;
    private Button mapButton;
    private Button chatButton;

    private String currentRideId = "";
    private String currentRideStatus = "";

    private ListenerRegistration requestListener;
    private ListenerRegistration currentRideListener;

    private final Handler refreshHandler =
            new Handler(Looper.getMainLooper());

    private final ExecutorService geocoderExecutor =
            Executors.newSingleThreadExecutor();

    private final Runnable refreshRunnable =
            new Runnable() {
                @Override
                public void run() {
                    listenForRideRequests();
                    listenForCurrentRide();

                    refreshHandler.postDelayed(
                            this,
                            REFRESH_INTERVAL_MS
                    );
                }
            };

    private final LocationListener locationListener =
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
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        buildDashboard();
        startLocationUpdates();

        listenForRideRequests();
        listenForCurrentRide();

        refreshHandler.postDelayed(
                refreshRunnable,
                REFRESH_INTERVAL_MS
        );
    }

    private void buildDashboard() {

        ScrollView scrollView =
                new ScrollView(this);

        root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setPadding(
                24,
                24,
                24,
                32
        );

        root.setBackgroundColor(
                Color.rgb(245, 248, 252)
        );

        scrollView.addView(root);

        TextView title =
                text(
                        "SAKAY NA",
                        28,
                        Color.rgb(20, 80, 150)
                );

        title.setGravity(Gravity.CENTER);

        root.addView(
                title,
                matchWrap()
        );

        TextView subtitle =
                text(
                        "DRIVER DASHBOARD",
                        15,
                        Color.DKGRAY
                );

        subtitle.setGravity(Gravity.CENTER);

        root.addView(
                subtitle,
                matchWrap()
        );

        statusText =
                text(
                        "Status: OFFLINE",
                        18,
                        Color.DKGRAY
                );

        statusText.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams statusParams =
                matchWrap();

        statusParams.setMargins(
                0,
                16,
                0,
                10
        );

        root.addView(
                statusText,
                statusParams
        );

        onlineButton =
                button("GO ONLINE");

        root.addView(
                onlineButton,
                matchWrap()
        );

        onlineButton.setOnClickListener(
                v -> toggleOnline()
        );

        profileButton =
                button("DRIVER PROFILE");

        root.addView(
                profileButton,
                matchWrap()
        );

        profileButton.setOnClickListener(
                v -> openProfile()
        );

        requestsButton =
                button("RIDE REQUESTS");

        root.addView(
                requestsButton,
                matchWrap()
        );

        currentRideButton =
                button("CURRENT RIDE");

        root.addView(
                currentRideButton,
                matchWrap()
        );

        earningsButton =
                button("EARNINGS");

        root.addView(
                earningsButton,
                matchWrap()
        );

        addSectionTitle(
                "NEW RIDE REQUESTS"
        );

        requestContainer =
                new LinearLayout(this);

        requestContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        root.addView(
                requestContainer,
                matchWrap()
        );

        addSectionTitle(
                "CURRENT RIDE"
        );

        currentRideContainer =
                new LinearLayout(this);

        currentRideContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        root.addView(
                currentRideContainer,
                matchWrap()
        );

        createRideActionButtons();

        logoutButton =
                button("LOGOUT");

        LinearLayout.LayoutParams logoutParams =
                matchWrap();

        logoutParams.setMargins(
                0,
                30,
                0,
                0
        );

        root.addView(
                logoutButton,
                logoutParams
        );

        logoutButton.setOnClickListener(
                v -> logout()
        );

        setContentView(scrollView);

        setOnlineButtonAppearance();
    }

    private void createRideActionButtons() {

        onTheWayButton =
                button("I'M ON THE WAY");

        arrivedButton =
                button("I HAVE ARRIVED");

        startRideButton =
                button("START RIDE");

        finishRideButton =
                button("FINISH RIDE");

        mapButton =
                button("LIVE RIDE MAP");

        chatButton =
                button("CHAT WITH PASSENGER");

        root.addView(
                onTheWayButton,
                matchWrap()
        );

        root.addView(
                arrivedButton,
                matchWrap()
        );

        root.addView(
                startRideButton,
                matchWrap()
        );

        root.addView(
                finishRideButton,
                matchWrap()
        );

        root.addView(
                mapButton,
                matchWrap()
        );

        root.addView(
                chatButton,
                matchWrap()
        );

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
                v -> finishRide()
        );

        mapButton.setOnClickListener(
                v -> openLiveMap()
        );

        chatButton.setOnClickListener(
                v -> openChat()
        );

        hideRideActions();
    }

    private void addSectionTitle(
            String value
    ) {

        TextView label =
                text(
                        value,
                        19,
                        Color.rgb(25, 95, 170)
                );

        LinearLayout.LayoutParams params =
                matchWrap();

        params.setMargins(
                0,
                25,
                0,
                10
        );

        root.addView(
                label,
                params
        );
    }

    private TextView text(
            String value,
            int size,
            int color
    ) {

        TextView view =
                new TextView(this);

        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setPadding(
                8,
                8,
                8,
                8
        );

        return view;
    }

    private Button button(
            String value
    ) {

        Button button =
                new Button(this);

        button.setText(value);
        button.setTextSize(14);
        button.setAllCaps(false);

        return button;
    }

    private LinearLayout.LayoutParams matchWrap() {

        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private void toggleOnline() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            return;
        }

        isOnline = !isOnline;

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "online",
                isOnline
        );

        data.put(
                "updatedAt",
                FieldValue.serverTimestamp()
        );

        db.collection("users")
                .document(user.getUid())
                .update(data)
                .addOnSuccessListener(
                        unused -> {

                            statusText.setText(
                                    isOnline
                                            ? "Status: ONLINE"
                                            : "Status: OFFLINE"
                            );

                            setOnlineButtonAppearance();

                            uploadDriverLocation();

                            Toast.makeText(
                                    this,
                                    isOnline
                                            ? "You are now ONLINE"
                                            : "You are now OFFLINE",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                )
                .addOnFailureListener(
                        e -> {

                            isOnline = !isOnline;

                            setOnlineButtonAppearance();

                            Toast.makeText(
                                    this,
                                    "Unable to change status: "
                                            + e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                );
    }

    private void setOnlineButtonAppearance() {

        if (onlineButton == null) {
            return;
        }

        if (isOnline) {

            onlineButton.setText(
                    "GO OFFLINE"
            );

            onlineButton.setTextColor(
                    Color.WHITE
            );

            onlineButton.setBackgroundColor(
                    Color.rgb(40, 170, 80)
            );

        } else {

            onlineButton.setText(
                    "GO ONLINE"
            );

            onlineButton.setTextColor(
                    Color.WHITE
            );

            onlineButton.setBackgroundColor(
                    Color.rgb(100, 100, 100)
            );
        }
    }

    private void listenForRideRequests() {

        if (requestContainer == null) {
            return;
        }

        if (requestListener != null) {
            requestListener.remove();
        }

        requestListener =
                db.collection("rides")
                        .whereEqualTo(
                                "status",
                                "REQUESTED"
                        )
                        .addSnapshotListener(
                                (snapshot, error) -> {

                                    if (error != null) {
                                        return;
                                    }

                                    if (snapshot == null) {
                                        return;
                                    }

                                    requestContainer.removeAllViews();

                                    for (
                                            DocumentSnapshot ride
                                            : snapshot.getDocuments()
                                    ) {

                                        if (isRideExpired(
                                                ride
                                        )) {

                                            expireRide(
                                                    ride.getId()
                                            );

                                            continue;
                                        }

                                        addRequestCard(
                                                ride
                                        );
                                    }

                                    if (
                                            requestContainer
                                                    .getChildCount()
                                                    == 0
                                    ) {

                                        TextView empty =
                                                text(
                                                        "No new ride requests.",
                                                        16,
                                                        Color.GRAY
                                                );

                                        empty.setGravity(
                                                Gravity.CENTER
                                        );

                                        requestContainer.addView(
                                                empty
                                        );
                                    }
                                }
                        );
    }

    private boolean isRideExpired(
            DocumentSnapshot ride
    ) {

        Long createdAt =
                ride.getLong(
                        "createdAt"
                );

        if (createdAt == null) {
            return false;
        }

        return System.currentTimeMillis()
                - createdAt
                >= REQUEST_EXPIRATION_MS;
    }

    private void expireRide(
            String rideId
    ) {

        if (
                rideId == null
                        || rideId.isEmpty()
        ) {
            return;
        }

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

    private void addRequestCard(
            DocumentSnapshot ride
    ) {

        LinearLayout card =
                new LinearLayout(this);

        card.setOrientation(
                LinearLayout.VERTICAL
        );

        card.setPadding(
                18,
                18,
                18,
                18
        );

        card.setBackgroundColor(
                Color.WHITE
        );

        LinearLayout.LayoutParams cardParams =
                matchWrap();

        cardParams.setMargins(
                0,
                0,
                0,
                16
        );

        requestContainer.addView(
                card,
                cardParams
        );

        TextView title =
                text(
                        "🛺 NEW RIDE REQUEST",
                        19,
                        Color.rgb(20, 95, 170)
                );

        card.addView(title);

        TextView pickup =
                text(
                        "Pickup: "
                                + locationValue(
                                ride,
                                "pickup"
                        ),
                        16,
                        Color.DKGRAY
                );

        TextView destination =
                text(
                        "Destination: "
                                + locationValue(
                                ride,
                                "destination"
                        ),
                        16,
                        Color.DKGRAY
                );

        TextView fare =
                text(
                        "Fare: ₱"
                                + String.valueOf(
                                ride.get("fare")
                        ),
                        17,
                        Color.rgb(30, 120, 70)
                );

        TextView payment =
                text(
                        "Payment: "
                                + String.valueOf(
                                ride.get("payment")
                        ),
                        16,
                        Color.DKGRAY
                );

        card.addView(pickup);
        card.addView(destination);
        card.addView(fare);
        card.addView(payment);

        resolveLocationNames(
                ride,
                pickup,
                destination
        );

        LinearLayout buttons =
                new LinearLayout(this);

        buttons.setOrientation(
                LinearLayout.HORIZONTAL
        );

        Button accept =
                button("ACCEPT");

        Button decline =
                button("DECLINE");

        accept.setTextColor(
                Color.WHITE
        );

        accept.setBackgroundColor(
                Color.rgb(35, 160, 75)
        );

        decline.setTextColor(
                Color.WHITE
        );

        decline.setBackgroundColor(
                Color.rgb(210, 55, 55)
        );

        buttons.addView(
                accept,
                weightParams()
        );

        buttons.addView(
                decline,
                weightParams()
        );

        card.addView(buttons);

        accept.setOnClickListener(
                v -> {

                    accept.setEnabled(false);
                    decline.setEnabled(false);

                    acceptRide(
                            ride.getId(),
                            card
                    );
                }
        );

        decline.setOnClickListener(
                v -> {

                    accept.setEnabled(false);
                    decline.setEnabled(false);

                    declineRide(
                            ride.getId(),
                            card
                    );
                }
        );
    }

    private LinearLayout.LayoutParams weightParams() {

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.weight = 1;

        params.setMargins(
                5,
                12,
                5,
                0
        );

        return params;
    }

    private String locationValue(
            DocumentSnapshot ride,
            String field
    ) {

        Object value =
                ride.get(field);

        if (value == null) {
            return "Not provided";
        }

        return String.valueOf(value);
    }

    private void resolveLocationNames(
            DocumentSnapshot ride,
            TextView pickupView,
            TextView destinationView
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

        geocoderExecutor.execute(
                () -> {

                    String pickupName =
                            reverseGeocode(
                                    pickupLat,
                                    pickupLng
                            );

                    String destinationName =
                            reverseGeocode(
                                    destinationLat,
                                    destinationLng
