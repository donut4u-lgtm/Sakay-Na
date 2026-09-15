
package com.sakyna.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PassengerActivity extends Activity {

    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private static final int DESTINATION_REQUEST = 2001;

    private static final double BASE_FARE = 20.00;
    private static final double FARE_PER_KM = 10.00;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseUser user;

    private LocationManager locationManager;
    private LocationListener locationListener;
    private Location currentLocation;

    private double pickupLat = 0.0;
    private double pickupLng = 0.0;

    private double destinationLat = 0.0;
    private double destinationLng = 0.0;

    private double distanceKm = 0.0;
    private double fare = 0.0;

    private String pickupAddress = "";
    private String destinationAddress = "";

    private String currentRideId = "";

    private ListenerRegistration rideListener;

    private TextView statusText;
    private TextView pickupText;
    private TextView destinationText;
    private TextView distanceText;
    private TextView fareText;
    private TextView paymentSummaryText;

    private RadioGroup paymentGroup;
    private RadioButton cashButton;
    private RadioButton gcashButton;
    private RadioButton mayaButton;

    private Button chooseDestinationButton;
    private Button bookButton;
    private Button cancelButton;
    private Button liveMapButton;
    private Button chatButton;
    private Button historyButton;
    private Button backButton;
    private Button logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        user = auth.getCurrentUser();

        if (user == null) {
            goToLogin();
            return;
        }

        buildScreen();
        setupLocation();
    }

    private void buildScreen() {

        ScrollView scrollView = new ScrollView(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(30, 30, 30, 40);

        scrollView.addView(root);

        TextView title = new TextView(this);
        title.setText("🛺 SAKAY NA");
        title.setTextSize(30);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 10, 0, 5);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Passenger Home");
        subtitle.setTextSize(18);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 0, 0, 20);
        root.addView(subtitle);

        statusText = new TextView(this);
        statusText.setText("Status: Ready");
        statusText.setTextSize(17);
        statusText.setPadding(0, 10, 0, 20);
        root.addView(statusText);

        pickupText = new TextView(this);
        pickupText.setText("📍 Pickup: Finding your location...");
        pickupText.setTextSize(17);
        pickupText.setPadding(0, 10, 0, 15);
        root.addView(pickupText);

        destinationText = new TextView(this);
        destinationText.setText("🏁 Destination: Not selected");
        destinationText.setTextSize(17);
        destinationText.setPadding(0, 10, 0, 15);
        root.addView(destinationText);

        chooseDestinationButton = new Button(this);
        chooseDestinationButton.setText("🗺️ CHOOSE DESTINATION");
        chooseDestinationButton.setOnClickListener(
                v -> openDestinationMap()
        );
        root.addView(chooseDestinationButton);

        distanceText = new TextView(this);
        distanceText.setText("📏 Distance: -- km");
        distanceText.setTextSize(18);
        distanceText.setPadding(0, 20, 0, 5);
        root.addView(distanceText);

        fareText = new TextView(this);
        fareText.setText("💰 Estimated Fare: Select destination");
        fareText.setTextSize(21);
        fareText.setPadding(0, 5, 0, 15);
        root.addView(fareText);

        TextView fareInfo = new TextView(this);
        fareInfo.setText(
                "Base fare: ₱20.00\n" +
                "Additional: ₱10.00 per kilometer"
        );
        fareInfo.setTextSize(14);
        fareInfo.setPadding(0, 0, 0, 20);
        root.addView(fareInfo);

        TextView paymentTitle = new TextView(this);
        paymentTitle.setText("💳 PAYMENT METHOD");
        paymentTitle.setTextSize(19);
        paymentTitle.setPadding(0, 10, 0, 10);
        root.addView(paymentTitle);

        paymentGroup = new RadioGroup(this);
        paymentGroup.setOrientation(RadioGroup.VERTICAL);

        cashButton = new RadioButton(this);
        cashButton.setText("💵 Cash");
        cashButton.setTextSize(17);
        cashButton.setId(View.generateViewId());

        gcashButton = new RadioButton(this);
        gcashButton.setText("🟢 GCash");
        gcashButton.setTextSize(17);
        gcashButton.setId(View.generateViewId());

        mayaButton = new RadioButton(this);
        mayaButton.setText("🟣 Maya (PayMaya)");
        mayaButton.setTextSize(17);
        mayaButton.setId(View.generateViewId());

        paymentGroup.addView(cashButton);
        paymentGroup.addView(gcashButton);
        paymentGroup.addView(mayaButton);

        cashButton.setChecked(true);

        root.addView(paymentGroup);

        paymentSummaryText = new TextView(this);
        paymentSummaryText.setText("Selected payment: Cash");
        paymentSummaryText.setTextSize(16);
        paymentSummaryText.setPadding(0, 10, 0, 20);
        root.addView(paymentSummaryText);

        paymentGroup.setOnCheckedChangeListener(
                (group, checkedId) -> {

                    if (checkedId == cashButton.getId()) {
                        paymentSummaryText.setText(
                                "Selected payment: Cash"
                        );
                    } else if (checkedId == gcashButton.getId()) {
                        paymentSummaryText.setText(
                                "Selected payment: GCash"
                        );
                    } else if (checkedId == mayaButton.getId()) {
                        paymentSummaryText.setText(
                                "Selected payment: Maya (PayMaya)"
                        );
                    }
                }
        );

        bookButton = new Button(this);
        bookButton.setText("🛺 BOOK SAKAY");
        bookButton.setOnClickListener(
                v -> bookRide()
        );
        root.addView(bookButton);

        cancelButton = new Button(this);
        cancelButton.setText("❌ CANCEL RIDE");
        cancelButton.setOnClickListener(
                v -> cancelRide()
        );
        root.addView(cancelButton);

        liveMapButton = new Button(this);
        liveMapButton.setText("📍 LIVE RIDE MAP");
        liveMapButton.setOnClickListener(
                v -> openLiveMap()
        );
        root.addView(liveMapButton);

        chatButton = new Button(this);
        chatButton.setText("💬 CHAT WITH DRIVER");
        chatButton.setOnClickListener(
                v -> openChat()
        );
        root.addView(chatButton);

        historyButton = new Button(this);
        historyButton.setText("📜 RIDE HISTORY");
        historyButton.setOnClickListener(
                v -> showRideHistory()
        );
        root.addView(historyButton);

        backButton = new Button(this);
        backButton.setText("⬅️ BACK");
        backButton.setOnClickListener(
                v -> finish()
        );
        root.addView(backButton);

        logoutButton = new Button(this);
        logoutButton.setText("🚪 LOG OUT");
        logoutButton.setOnClickListener(
                v -> logout()
        );
        root.addView(logoutButton);

        setContentView(scrollView);

        cancelButton.setVisibility(View.GONE);
        liveMapButton.setVisibility(View.GONE);
        chatButton.setVisibility(View.GONE);
    }

    private void setupLocation() {

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
                    LOCATION_PERMISSION_REQUEST
            );

            pickupText.setText(
                    "📍 Pickup: Location permission required"
            );

            return;
        }

        startLocationUpdates();
    }

    private void startLocationUpdates() {

        if (locationManager == null) {
            pickupText.setText(
                    "📍 Pickup: Location service unavailable"
            );
            return;
        }

        locationListener = new LocationListener() {

            @Override
            public void onLocationChanged(
                    @NonNull Location location
            ) {

                updateLocation(location);
            }
        };

        try {

            if (locationManager.isProviderEnabled(
                    LocationManager.GPS_PROVIDER
            )) {

                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        2000,
                        2,
                        locationListener
                );
            }

            if (locationManager.isProviderEnabled(
                    LocationManager.NETWORK_PROVIDER
            )) {

                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        3000,
                        5,
                        locationListener
                );
            }

            Location gps = null;
            Location network = null;

            if (locationManager.isProviderEnabled(
                    LocationManager.GPS_PROVIDER
            )) {
                gps = locationManager.getLastKnownLocation(
                        LocationManager.GPS_PROVIDER
                );
            }

            if (locationManager.isProviderEnabled(
                    LocationManager.NETWORK_PROVIDER
            )) {
                network = locationManager.getLastKnownLocation(
                        LocationManager.NETWORK_PROVIDER
                );
            }

            Location best =
                    chooseBestLocation(
                            gps,
                            network
                    );

            if (best != null) {
                updateLocation(best);
            }

        } catch (SecurityException e) {

            pickupText.setText(
                    "📍 Pickup: Location permission denied"
            );
        }
    }

    private Location chooseBestLocation(
            Location a,
            Location b
    ) {

        if (a == null) return b;
        if (b == null) return a;

        return a.getTime() >= b.getTime()
                ? a
                : b;
    }

    private void updateLocation(Location location) {

        if (location == null) {
            return;
        }

        currentLocation = location;

        pickupLat = location.getLatitude();
        pickupLng = location.getLongitude();

        if (pickupAddress.isEmpty()) {

            pickupText.setText(
                    String.format(
                            Locale.US,
                            "📍 Pickup: Finding address...\n%.6f, %.6f",
                            pickupLat,
                            pickupLng
                    )
            );

            getReadableAddress(
                    pickupLat,
                    pickupLng,
                    true
            );

        } else {

            updatePickupText();
        }

        if (destinationLat != 0.0 &&
                destinationLng != 0.0) {

            calculateFare();
        }
    }

    private void getReadableAddress(
            double lat,
            double lng,
            boolean pickup
    ) {

        new Thread(() -> {

            String result = "";

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

                if (addresses != null &&
                        !addresses.isEmpty()) {

                    Address address =
                            addresses.get(0);

                    result =
                            address.getAddressLine(0);

                    if (result == null ||
                            result.trim().isEmpty()) {

                        result =
                                buildAddress(
                                        address
                                );
                    }
                }

            } catch (Exception ignored) {
            }

            final String finalResult = result;

            runOnUiThread(() -> {

                if (pickup) {

                    if (!finalResult.isEmpty()) {
                        pickupAddress = finalResult;
                    } else {
                        pickupAddress =
                                String.format(
                                        Locale.US,
                                        "GPS %.6f, %.6f",
                                        lat,
                                        lng
                                );
                    }

                    updatePickupText();

                } else {

                    if (!finalResult.isEmpty()) {
                        destinationAddress =
                                finalResult;
                    } else {
                        destinationAddress =
                                String.format(
                                        Locale.US,
                                        "GPS %.6f, %.6f",
                                        lat,
                                        lng
                                );
                    }

                    updateDestinationText();
                }
            });

        }).start();
    }

    private String buildAddress(Address address) {

        StringBuilder builder =
                new StringBuilder();

        for (int i = 0;
             i <= address.getMaxAddressLineIndex();
             i++) {

            String line =
                    address.getAddressLine(i);

            if (line != null &&
                    !line.trim().isEmpty()) {

                if (builder.length() > 0) {
                    builder.append(", ");
                }

                builder.append(line);
            }
        }

        return builder.toString();
    }

    private void updatePickupText() {

        pickupText.setText(
                "📍 PICKUP LOCATION\n" +
                pickupAddress +
                String.format(
                        Locale.US,
                        "\nGPS: %.6f, %.6f",
                        pickupLat,
                        pickupLng
                )
        );
    }

    private void openDestinationMap() {

        if (pickupLat == 0.0 ||
                pickupLng == 0.0) {

            Toast.makeText(
                    this,
                    "Please wait for your current location.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

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
                pickupAddress
        );

        startActivityForResult(
                intent,
                DESTINATION_REQUEST
        );
    }

    private void readDestinationFromIntent(
            Intent data
    ) {

        if (data == null) {
            return;
        }

        double lat =
                data.getDoubleExtra(
                        "destination_latitude",
                        0.0
                );

        double lng =
                data.getDoubleExtra(
                        "destination_longitude",
                        0.0
                );

        if (lat == 0.0 ||
                lng == 0.0) {

            Toast.makeText(
                    this,
                    "Invalid destination.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        destinationLat = lat;
        destinationLng = lng;

        destinationAddress =
                data.getStringExtra(
                        "destination_address"
                );

        if (destinationAddress == null ||
                destinationAddress.trim().isEmpty()) {

            getReadableAddress(
                    destinationLat,
                    destinationLng,
                    false
            );

        } else {

            updateDestinationText();
        }

        calculateFare();
    }

    private void updateDestinationText() {

        destinationText.setText(
                "🏁 DESTINATION\n" +
                destinationAddress +
                String.format(
                        Locale.US,
                        "\nGPS: %.6f, %.6f",
                        destinationLat,
                        destinationLng
                )
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

        if (requestCode ==
                DESTINATION_REQUEST &&
                resultCode ==
                        RESULT_OK) {

            readDestinationFromIntent(data);
        }
    }

    private void calculateFare() {

        if (pickupLat == 0.0 ||
                pickupLng == 0.0 ||
                destinationLat == 0.0 ||
                destinationLng == 0.0) {

            distanceText.setText(
                    "📏 Distance: -- km"
            );

            fareText.setText(
                    "💰 Estimated Fare: Select destination"
            );

            return;
        }

        float[] results =
                new float[1];

        Location.distanceBetween(
                pickupLat,
                pickupLng,
                destinationLat,
                destinationLng,
                results
        );

        distanceKm =
                results[0] / 1000.0;

        fare =
                BASE_FARE +
                (distanceKm * FARE_PER_KM);

        distanceText.setText(
                String.format(
                        Locale.US,
                        "📏 Distance: %.2f km",
                        distanceKm
                )
        );

        fareText.setText(
                String.format(
                        Locale.US,
                        "💰 ESTIMATED FARE: ₱%.2f",
                        fare
                )
        );
    }

    private String getSelectedPaymentMethod() {

        int selected =
                paymentGroup.getCheckedRadioButtonId();

        if (selected ==
                gcashButton.getId()) {
            return "GCASH";
        }

        if (selected ==
                mayaButton.getId()) {
            return "MAYA";
        }

        return "CASH";
    }

    private void bookRide() {

        if (user == null) {
            goToLogin();
            return;
        }

        if (pickupLat == 0.0 ||
                pickupLng == 0.0) {

            Toast.makeText(
                    this,
                    "Your pickup location is not ready yet.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (destinationLat == 0.0 ||
                destinationLng == 0.0) {

            Toast.makeText(
                    this,
                    "Please choose your destination first.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        calculateFare();

        String paymentMethod =
                getSelectedPaymentMethod();

        Map<String, Object> ride =
                new HashMap<>();

        ride.put(
                "passengerId",
                user.getUid()
        );

        ride.put(
                "passengerPhone",
                user.getPhoneNumber() == null
                        ? ""
                        : user.getPhoneNumber()
        );

        ride.put(
                "pickup",
                pickupAddress
        );

        ride.put(
                "pickupAddress",
                pickupAddress
        );

        ride.put(
                "passengerLatitude",
                pickupLat
        );

        ride.put(
                "passengerLongitude",
                pickupLng
        );

        ride.put(
                "destination",
                destinationAddress
        );

        ride.put(
                "destinationAddress",
                destinationAddress
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
                "distanceKm",
                distanceKm
        );

        ride.put(
                "fare",
                fare
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
                FieldValue.serverTimestamp()
        );

        bookButton.setEnabled(false);

        db.collection("rides")
                .add(ride)
                .addOnSuccessListener(
                        documentReference -> {

                            currentRideId =
                                    documentReference.getId();

                            statusText.setText(
                                    "Status: WAITING FOR DRIVER"
                            );

                            cancelButton.setVisibility(
                                    View.VISIBLE
                            );

                            liveMapButton.setVisibility(
                                    View.VISIBLE
                            );

                            chatButton.setVisibility(
                                    View.VISIBLE
                            );

                            Toast.makeText(
                                    this,
                                    String.format(
                                            Locale.US,
                                            "Ride requested!\nFare: ₱%.2f",
                                            fare
                                    ),
                                    Toast.LENGTH_LONG
                            ).show();

                            listenForRide();
                        }
                )
                .addOnFailureListener(
                        e -> {

                            bookButton.setEnabled(
                                    true
                            );

                            Toast.makeText(
                                    this,
                                    "Booking failed: " +
                                            e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                );
    }

    private void listenForRide() {

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
                                (snapshot, error) -> {

                                    if (error != null ||
                                            snapshot == null ||
                                            !snapshot.exists()) {
                                        return;
                                    }

                                    String status =
                                            snapshot.getString(
                                                    "status"
                                            );

                                    if (status == null) {
                                        status = "UNKNOWN";
                                    }

                                    String display =
                                            status;

                                    if ("REQUESTED".equals(
                                            status)) {
                                        display =
                                                "WAITING FOR DRIVER";
                                    } else if ("ACCEPTED".equals(
                                            status)) {
                                        display =
                                                "DRIVER ACCEPTED";
                                    } else if ("ARRIVING".equals(
                                            status)) {
                                        display =
                                                "DRIVER IS ARRIVING";
                                    } else if ("IN_PROGRESS".equals(
                                            status)) {
                                        display =
                                                "RIDE IN PROGRESS";
                                    } else if ("COMPLETED".equals(
                                            status)) {
                                        display =
                                                "RIDE COMPLETED";
                                    } else if ("CANCELLED".equals(
                                            status)) {
                                        display =
                                                "RIDE CANCELLED";
                                    }

                                    statusText.setText(
                                            "Status: " +
                                                    display
                                    );

                                    if ("COMPLETED".equals(
                                            status) ||
                                            "CANCELLED".equals(
                                                    status)) {

                                        cancelButton
                                                .setVisibility(
                                                        View.GONE
                                                );
                                    }
                                }
                        );
    }

    private void cancelRide() {

        if (currentRideId == null ||
                currentRideId.isEmpty()) {

            Toast.makeText(
                    this,
                    "No active ride.",
                    Toast.LENGTH_SHORT
            ).show();

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

                            statusText.setText(
                                    "Status: RIDE CANCELLED"
                            );

                            cancelButton
                                    .setVisibility(
                                            View.GONE
                                    );
                        }
                )
                .addOnFailureListener(
                        e -> Toast.makeText(
                                this,
                                "Unable to cancel: " +
                                        e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void openLiveMap() {

        if (currentRideId == null ||
                currentRideId.isEmpty()) {

            Toast.makeText(
                    this,
                    "No active ride.",
                    Toast.LENGTH_SHORT
            ).show();

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
                currentRideId
        );

        startActivity(intent);
    }

    private void openChat() {

        if (currentRideId == null ||
                currentRideId.isEmpty()) {

            Toast.makeText(
                    this,
                    "Chat becomes available after booking a ride.",
                    Toast.LENGTH_LONG
            ).show();

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

        startActivity(intent);
    }

    private void showRideHistory() {

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
                        querySnapshot -> {

                            StringBuilder history =
                                    new StringBuilder();

                            history.append(
                                    "📜 RIDE HISTORY\n\n"
                            );

                            if (querySnapshot.isEmpty()) {

                                history.append(
                                        "No rides yet."
                                );

                            } else {

                                for (
                                        com.google.firebase.firestore.DocumentSnapshot doc :
                                        querySnapshot
                                ) {

                                    String status =
                                            doc.getString(
                                                    "status"
                                            );

                                    String destination =
                                            doc.getString(
                                                    "destination"
                                            );

                                    Double savedFare =
                                            doc.getDouble(
                                                    "fare"
                                            );

                                    history.append(
                                            "Status: "
                                    ).append(
                                            status == null
                                                    ? "UNKNOWN"
                                                    : status
                                    ).append("\n");

                                    history.append(
                                            "Destination: "
                                    ).append(
                                            destination == null
                                                    ? "Unknown"
                                                    : destination
                                    ).append("\n");

                                    if (savedFare != null) {
                                        history.append(
                                                String.format(
                                                        Locale.US,
                                                        "Fare: ₱%.2f\n",
                                                        savedFare
                                                )
                                        );
                                    }

                                    history.append(
                                            "--------------------\n"
                                    );
                                }
                            }

                            new android.app.AlertDialog.Builder(
                                    this
                            )
                                    .setTitle(
                                            "📜 Ride History"
                                    )
                                    .setMessage(
                                            history.toString()
                                    )
                                    .setPositiveButton(
                                            "CLOSE",
                                            null
                                    )
                                    .show();
                        }
                )
                .addOnFailureListener(
                        e -> Toast.makeText(
                                this,
                                "Unable to load history: " +
                                        e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void logout() {

        if (rideListener != null) {
            rideListener.remove();
            rideListener = null;
        }

        auth.signOut();

        goToLogin();
    }

    private void goToLogin() {

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
            rideListener = null;
        }

        if (locationManager != null &&
                locationListener != null) {

            try {
                locationManager.removeUpdates(
                        locationListener
                );
            } catch (Exception ignored) {
            }
        }

        super.onDestroy();
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

            boolean granted = false;

            for (int result : grantResults) {

                if (result ==
                        PackageManager.PERMISSION_GRANTED) {

                    granted = true;
                    break;
                }
            }

            if (granted) {
                startLocationUpdates();
            } else {
                pickupText.setText(
                        "📍 Pickup: Location permission denied"
                );
            }
        }
    }
}
