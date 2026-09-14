
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

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;

public class PassengerActivity extends Activity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private LocationManager locationManager;
    private LocationListener locationListener;

    private EditText pickupField;
    private EditText destinationField;
    private TextView gpsText;
    private TextView fareText;
    private TextView rideStatusText;

    private double pickupLat = 0;
    private double pickupLng = 0;
    private double destinationLat = 0;
    private double destinationLng = 0;

    private double distanceKm = 0;
    private double fare = 0;

    private String currentRideId = "";

    private ListenerRegistration rideListener;

    private static final int LOCATION_PERMISSION = 100;
    private static final int DESTINATION_MAP = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        buildScreen();
        startGps();
    }

    private int dp(int value) {
        return (int) (
                value * getResources().getDisplayMetrics().density
        );
    }

    private void addSpace(LinearLayout root, int height) {

        TextView space = new TextView(this);

        root.addView(
                space,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(height)
                )
        );
    }

    private void buildScreen() {

        ScrollView scroll = new ScrollView(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(
                dp(24),
                dp(24),
                dp(24),
                dp(40)
        );

        TextView title = new TextView(this);
        title.setText("SAKAY NA");
        title.setTextSize(30);
        title.setTextColor(Color.rgb(0, 120, 70));
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        addSpace(root, 8);

        TextView subtitle = new TextView(this);
        subtitle.setText("Passenger • Book a Ride");
        subtitle.setTextSize(20);
        subtitle.setTextColor(Color.DKGRAY);
        subtitle.setGravity(Gravity.CENTER);

        root.addView(subtitle);

        addSpace(root, 22);

        gpsText = new TextView(this);
        gpsText.setText("📍 Getting your current location...");
        gpsText.setTextSize(16);
        gpsText.setGravity(Gravity.CENTER);
        root.addView(gpsText);

        addSpace(root, 18);

        TextView pickupLabel = new TextView(this);
        pickupLabel.setText("PICKUP LOCATION");
        pickupLabel.setTextSize(14);
        pickupLabel.setTextColor(Color.DKGRAY);
        root.addView(pickupLabel);

        addSpace(root, 6);

        pickupField = new EditText(this);
        pickupField.setHint("Your current GPS location");
        pickupField.setTextSize(17);
        pickupField.setSingleLine(true);
        pickupField.setEnabled(false);

        root.addView(
                pickupField,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(60)
                )
        );

        addSpace(root, 10);

        Button refreshLocation = new Button(this);
        refreshLocation.setText("📍 USE MY CURRENT LOCATION");
        refreshLocation.setTextSize(15);
        refreshLocation.setOnClickListener(v -> startGps());

        root.addView(refreshLocation);

        addSpace(root, 24);

        TextView destinationLabel = new TextView(this);
        destinationLabel.setText("DESTINATION");
        destinationLabel.setTextSize(14);
        destinationLabel.setTextColor(Color.DKGRAY);
        root.addView(destinationLabel);

        addSpace(root, 6);

        destinationField = new EditText(this);
        destinationField.setHint("Choose destination on map");
        destinationField.setTextSize(17);
        destinationField.setSingleLine(true);
        destinationField.setFocusable(false);
        destinationField.setClickable(false);

        root.addView(
                destinationField,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(60)
                )
        );

        addSpace(root, 10);

        Button chooseDestination = new Button(this);
        chooseDestination.setText(
                "🗺️ CHOOSE DESTINATION ON MAP"
        );
        chooseDestination.setTextSize(15);
        chooseDestination.setOnClickListener(
                v -> openDestinationMap()
        );

        root.addView(chooseDestination);

        addSpace(root, 24);

        fareText = new TextView(this);
        fareText.setText("Distance: —\nEstimated Fare: —");
        fareText.setTextSize(19);
        fareText.setTextColor(
                Color.rgb(0, 120, 70)
        );
        fareText.setGravity(Gravity.CENTER);
        root.addView(fareText);

        addSpace(root, 10);

        Button calculate = new Button(this);
        calculate.setText("CALCULATE FARE");
        calculate.setTextSize(16);
        calculate.setOnClickListener(
                v -> calculateFare()
        );

        root.addView(calculate);

        addSpace(root, 24);

        TextView paymentLabel = new TextView(this);
        paymentLabel.setText("PAYMENT METHOD");
        paymentLabel.setTextSize(15);
        paymentLabel.setTextColor(Color.DKGRAY);
        paymentLabel.setGravity(Gravity.CENTER);
        root.addView(paymentLabel);

        addSpace(root, 8);

        RadioGroup paymentGroup = new RadioGroup(this);
        paymentGroup.setOrientation(
                RadioGroup.VERTICAL
        );

        RadioButton cash = new RadioButton(this);
        cash.setText("Cash");
        cash.setTextSize(16);
        cash.setId(
                android.view.View.generateViewId()
        );
        cash.setChecked(true);

        RadioButton gcash = new RadioButton(this);
        gcash.setText("GCash");
        gcash.setTextSize(16);
        gcash.setId(
                android.view.View.generateViewId()
        );

        RadioButton maya = new RadioButton(this);
        maya.setText("Maya");
        maya.setTextSize(16);
        maya.setId(
                android.view.View.generateViewId()
        );

        paymentGroup.addView(cash);
        paymentGroup.addView(gcash);
        paymentGroup.addView(maya);

        root.addView(paymentGroup);

        addSpace(root, 20);

        Button book = new Button(this);
        book.setText("🚕 BOOK RIDE");
        book.setTextSize(18);
        book.setOnClickListener(
                v -> bookRide(
                        paymentGroup.getCheckedRadioButtonId()
                )
        );

        root.addView(book);

        addSpace(root, 18);

        rideStatusText = new TextView(this);
        rideStatusText.setText(
                "Current Ride: None"
        );
        rideStatusText.setTextSize(17);
        rideStatusText.setTextColor(Color.DKGRAY);
        rideStatusText.setGravity(Gravity.CENTER);
        root.addView(rideStatusText);

        addSpace(root, 10);

        Button liveMap = new Button(this);
        liveMap.setText("🗺️ LIVE RIDE MAP");
        liveMap.setTextSize(16);
        liveMap.setOnClickListener(
                v -> openLiveMap()
        );

        root.addView(liveMap);

        addSpace(root, 8);

        Button cancel = new Button(this);
        cancel.setText("❌ CANCEL CURRENT RIDE");
        cancel.setTextSize(16);
        cancel.setOnClickListener(
                v -> cancelRide()
        );

        root.addView(cancel);

        addSpace(root, 18);

        Button back = new Button(this);
        back.setText("↩ BACK");
        back.setTextSize(16);
        back.setOnClickListener(
                v -> finish()
        );

        root.addView(back);

        addSpace(root, 8);

        Button logout = new Button(this);
        logout.setText("LOGOUT");
        logout.setTextSize(16);
        logout.setOnClickListener(
                v -> logout()
        );

        root.addView(logout);

        scroll.addView(root);

        setContentView(scroll);
    }

    private void startGps() {

        if (checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
                &&
            checkSelfPermission(
                    Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION
            );

            return;
        }

        locationManager =
                (LocationManager)
                        getSystemService(LOCATION_SERVICE);

        locationListener =
                new LocationListener() {

                    @Override
                    public void onLocationChanged(
                            @NonNull Location location) {

                        pickupLat =
                                location.getLatitude();

                        pickupLng =
                                location.getLongitude();

                        pickupField.setText(
                                String.format(
                                        java.util.Locale.US,
                                        "Current location\n%.6f, %.6f",
                                        pickupLat,
                                        pickupLng
                                )
                        );

                        gpsText.setText(
                                String.format(
                                        java.util.Locale.US,
                                        "📍 GPS LOCATION\n%.6f, %.6f",
                                        pickupLat,
                                        pickupLng
                                )
                        );
                    }
                };

        try {

            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000,
                    5,
                    locationListener
            );

            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    2000,
                    5,
                    locationListener
            );

        } catch (Exception e) {

            gpsText.setText(
                    "Unable to start GPS."
            );
        }
    }

    private void openDestinationMap() {

        if (pickupLat == 0 &&
                pickupLng == 0) {

            Toast.makeText(
                    this,
                    "Waiting for your pickup GPS location.",
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
                "passenger_latitude",
                pickupLat
        );

        intent.putExtra(
                "passenger_longitude",
                pickupLng
        );

        startActivityForResult(
                intent,
                DESTINATION_MAP
        );
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode == DESTINATION_MAP &&
                resultCode == RESULT_OK &&
                data != null) {

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

            if (destinationLat != 0 &&
                    destinationLng != 0) {

                if (address == null ||
                        address.trim().isEmpty()) {

                    address = String.format(
                            java.util.Locale.US,
                            "Selected location\n%.6f, %.6f",
                            destinationLat,
                            destinationLng
                    );
                }

                destinationField.setText(
                        address
                );

                calculateFare();

                Toast.makeText(
                        this,
                        "Destination selected.",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    private void calculateFare() {

        if (pickupLat == 0 ||
                pickupLng == 0) {

            Toast.makeText(
                    this,
                    "Pickup GPS location is not ready.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (destinationLat == 0 ||
                destinationLng == 0) {

            Toast.makeText(
                    this,
                    "Choose your destination on the map.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        float[] results = new float[1];

        Location.distanceBetween(
                pickupLat,
                pickupLng,
                destinationLat,
                destinationLng,
                results
        );

        distanceKm =
                results[0] / 1000.0;

        double baseFare = 20.0;
        double perKm = 10.0;

        fare =
                baseFare +
                (distanceKm * perKm);

        if (fare < 20) {
            fare = 20;
        }

        fareText.setText(
                String.format(
                        java.util.Locale.US,
                        "Distance: %.2f km\nEstimated Fare: ₱%.2f",
                        distanceKm,
                        fare
                )
        );
    }

    private void bookRide(
            int paymentId
    ) {

        if (pickupLat == 0 ||
                pickupLng == 0) {

            Toast.makeText(
                    this,
                    "Pickup location is not ready.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (destinationLat == 0 ||
                destinationLng == 0) {

            Toast.makeText(
                    this,
                    "Choose a destination first.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        calculateFare();

        if (auth.getCurrentUser() == null) {

            Toast.makeText(
                    this,
                    "Please login again.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        String passengerId =
                auth.getCurrentUser().getUid();

        RadioButton selected =
                findViewById(paymentId);

        String paymentMethod =
                selected == null
                        ? "Cash"
                        : selected.getText().toString();

        Map<String, Object> ride =
                new HashMap<>();

        ride.put(
                "passengerId",
                passengerId
        );

        ride.put(
                "pickup",
                pickupField.getText().toString()
        );

        ride.put(
                "destination",
                destinationField.getText().toString()
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
                "status",
                "REQUESTED"
        );

        ride.put(
                "paymentStatus",
                "PENDING"
        );

        ride.put(
                "createdAt",
                com.google.firebase.firestore
                        .FieldValue
                        .serverTimestamp()
        );

        db.collection("rides")
                .add(ride)
                .addOnSuccessListener(
                        documentReference -> {

                            currentRideId =
                                    documentReference
                                            .getId();

                            rideStatusText.setText(
                                    "Current Ride: REQUESTED\n" +
                                    "Waiting for a driver..."
                            );

                            listenToRide(
                                    currentRideId
                            );

                            Toast.makeText(
                                    this,
                                    "🚕 Ride requested!",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                )
                .addOnFailureListener(
                        e -> Toast.makeText(
                                this,
                                "Booking failed: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void listenToRide(
            String rideId
    ) {

        if (rideListener != null) {
            rideListener.remove();
        }

        rideListener =
                db.collection("rides")
                        .document(rideId)
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
                                        status = "REQUESTED";
                                    }

                                    String driverId =
                                            snapshot.getString(
                                                    "driverId"
                                            );

                                    rideStatusText.setText(
                                            "Current Ride: "
                                                    + status
                                                    + (
                                                    driverId == null
                                                    ? "\nWaiting for a driver..."
                                                    : "\nDriver assigned."
                                            )
                                    );
                                }
                        );
    }

    private void openLiveMap() {

        if (pickupLat == 0 ||
                destinationLat == 0) {

            Toast.makeText(
                    this,
                    "Select pickup and destination first.",
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
                "ride_id",
                currentRideId
        );

        intent.putExtra(
                "passenger_latitude",
                pickupLat
        );

        intent.putExtra(
                "passenger_longitude",
                pickupLng
        );

        intent.putExtra(
                "destination_latitude",
                destinationLat
        );

        intent.putExtra(
                "destination_longitude",
                destinationLng
        );

        startActivity(intent);
    }

    private void cancelRide() {

        if (currentRideId == null ||
                currentRideId.isEmpty()) {

            Toast.makeText(
                    this,
                    "No current ride.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String rideId =
                currentRideId;

        db.collection("rides")
                .document(rideId)
                .update(
                        "status",
                        "CANCELLED"
                )
                .addOnSuccessListener(
                        v -> {

                            if (rideListener != null) {
                                rideListener.remove();
                                rideListener = null;
                            }

                            currentRideId = "";

                            rideStatusText.setText(
                                    "Current Ride: None"
                            );

                            destinationLat = 0;
                            destinationLng = 0;

                            distanceKm = 0;
                            fare = 0;

                            destinationField.setText("");

                            fareText.setText(
                                    "Distance: —\nEstimated Fare: —"
                            );

                            Toast.makeText(
                                    this,
                                    "Ride cancelled. You can book another ride.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                )
                .addOnFailureListener(
                        e -> Toast.makeText(
                                this,
                                "Cancel failed: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void logout() {

        if (rideListener != null) {
            rideListener.remove();
            rideListener = null;
        }

        stopGps();

        auth.signOut();

        Intent intent =
                new Intent(
                        this,
                        MainActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        finish();
    }

    private void stopGps() {

        if (locationManager != null &&
                locationListener != null) {

            try {
                locationManager.removeUpdates(
                        locationListener
                );
            } catch (Exception ignored) {
            }
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

        if (requestCode == LOCATION_PERMISSION) {

            if (grantResults.length > 0 &&
                    grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED) {

                startGps();

            } else {

                gpsText.setText(
                        "📍 Location permission is required."
                );
            }
        }
    }

    @Override
    public void onBackPressed() {

        finish();
    }

    @Override
    protected void onDestroy() {

        if (rideListener != null) {
            rideListener.remove();
            rideListener = null;
        }

        stopGps();

        super.onDestroy();
    }
}
