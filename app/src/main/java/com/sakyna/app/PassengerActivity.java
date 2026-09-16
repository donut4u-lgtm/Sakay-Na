
package com.sakyna.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PassengerActivity extends Activity {

```
private FirebaseAuth auth;
private FirebaseFirestore db;
private SharedPreferences preferences;

private EditText pickupInput;
private EditText destinationInput;

private TextView locationText;
private TextView fareText;
private TextView rideStatusText;

private RadioGroup paymentGroup;

private Button bookButton;
private Button cancelButton;
private Button mapButton;
private Button chatButton;
private Button historyButton;
private Button logoutButton;

private LocationManager locationManager;
private Location currentLocation;

private String activeRideId = "";
private ListenerRegistration rideListener;

private static final int LOCATION_PERMISSION_REQUEST = 1001;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    auth = FirebaseAuth.getInstance();
    db = FirebaseFirestore.getInstance();
    preferences = getSharedPreferences("SakayNa", MODE_PRIVATE);

    buildScreen();
    requestLocation();
    restoreActiveRide();
}

private void buildScreen() {

    ScrollView scrollView = new ScrollView(this);

    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(28, 28, 28, 28);

    TextView title = new TextView(this);
    title.setText("Sakay Na");
    title.setTextSize(30);
    title.setGravity(Gravity.CENTER);
    title.setPadding(0, 10, 0, 10);

    TextView subtitle = new TextView(this);
    subtitle.setText("Passenger • Book your tricycle ride");
    subtitle.setTextSize(17);
    subtitle.setGravity(Gravity.CENTER);
    subtitle.setPadding(0, 0, 0, 24);

    root.addView(title);
    root.addView(subtitle);

    locationText = new TextView(this);
    locationText.setText("Current location: Getting GPS location...");
    locationText.setTextSize(15);
    locationText.setPadding(0, 10, 0, 18);
    root.addView(locationText);

    TextView pickupLabel = new TextView(this);
    pickupLabel.setText("Pickup location");
    pickupLabel.setTextSize(17);
    root.addView(pickupLabel);

    pickupInput = new EditText(this);
    pickupInput.setHint("Enter pickup location");
    pickupInput.setSingleLine(false);
    root.addView(pickupInput,
            new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));

    TextView destinationLabel = new TextView(this);
    destinationLabel.setText("Destination");
    destinationLabel.setTextSize(17);
    destinationLabel.setPadding(0, 18, 0, 0);
    root.addView(destinationLabel);

    destinationInput = new EditText(this);
    destinationInput.setHint("Enter destination");
    destinationInput.setSingleLine(false);
    root.addView(destinationInput,
            new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));

    TextView paymentLabel = new TextView(this);
    paymentLabel.setText("Payment method");
    paymentLabel.setTextSize(17);
    paymentLabel.setPadding(0, 18, 0, 5);
    root.addView(paymentLabel);

    paymentGroup = new RadioGroup(this);
    paymentGroup.setOrientation(RadioGroup.VERTICAL);

    RadioButton cash = new RadioButton(this);
    cash.setText("Cash");
    cash.setId(View.generateViewId());
    cash.setChecked(true);

    RadioButton gcash = new RadioButton(this);
    gcash.setText("GCash");
    gcash.setId(View.generateViewId());

    RadioButton maya = new RadioButton(this);
    maya.setText("Maya");
    maya.setId(View.generateViewId());

    paymentGroup.addView(cash);
    paymentGroup.addView(gcash);
    paymentGroup.addView(maya);

    root.addView(paymentGroup);

    fareText = new TextView(this);
    fareText.setText("Estimated fare: ₱50");
    fareText.setTextSize(19);
    fareText.setPadding(0, 18, 0, 18);
    root.addView(fareText);

    bookButton = new Button(this);
    bookButton.setText("BOOK A RIDE");
    root.addView(bookButton);

    cancelButton = new Button(this);
    cancelButton.setText("CANCEL RIDE");
    root.addView(cancelButton);

    mapButton = new Button(this);
    mapButton.setText("LIVE RIDE MAP");
    root.addView(mapButton);

    chatButton = new Button(this);
    chatButton.setText("CHAT WITH DRIVER");
    root.addView(chatButton);

    historyButton = new Button(this);
    historyButton.setText("RIDE HISTORY");
    root.addView(historyButton);

    rideStatusText = new TextView(this);
    rideStatusText.setText("No active ride");
    rideStatusText.setTextSize(16);
    rideStatusText.setGravity(Gravity.CENTER);
    rideStatusText.setPadding(0, 18, 0, 18);
    root.addView(rideStatusText);

    logoutButton = new Button(this);
    logoutButton.setText("LOGOUT");
    root.addView(logoutButton);

    bookButton.setOnClickListener(v -> bookRide());
    cancelButton.setOnClickListener(v -> cancelRide());
    mapButton.setOnClickListener(v -> openLiveMap());
    chatButton.setOnClickListener(v -> openChat());
    historyButton.setOnClickListener(v -> showRideHistory());
    logoutButton.setOnClickListener(v -> logout());

    cancelButton.setEnabled(false);
    mapButton.setEnabled(false);
    chatButton.setEnabled(false);

    scrollView.addView(root);
    setContentView(scrollView);
}

private void requestLocation() {

    locationManager =
            (LocationManager) getSystemService(LOCATION_SERVICE);

    if (locationManager == null) {
        locationText.setText("GPS is not available.");
        return;
    }

    if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
            && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

        requestPermissions(
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                LOCATION_PERMISSION_REQUEST
        );
        return;
    }

    startLocationUpdates();
}

private void startLocationUpdates() {

    if (locationManager == null) {
        return;
    }

    try {

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000,
                    5,
                    locationListener
            );
        }

        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    3000,
                    10,
                    locationListener
            );
        }

    } catch (Exception e) {
        locationText.setText("Unable to start GPS.");
    }
}

private final LocationListener locationListener =
        new LocationListener() {

            @Override
            public void onLocationChanged(@NonNull Location location) {

                currentLocation = location;

                locationText.setText(
                        "Current location:\n" +
                        "Latitude: " + location.getLatitude() +
                        "\nLongitude: " + location.getLongitude()
                );
            }

            @Override
            public void onProviderEnabled(@NonNull String provider) {
            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {
            }
        };

private void bookRide() {

    FirebaseUser user = auth.getCurrentUser();

    if (user == null) {
        Toast.makeText(
                this,
                "Please log in again.",
                Toast.LENGTH_SHORT
        ).show();

        startActivity(new Intent(this, MainActivity.class));
        finish();
        return;
    }

    String pickup = pickupInput.getText().toString().trim();
    String destination = destinationInput.getText().toString().trim();

    if (pickup.isEmpty() && currentLocation != null) {
        pickup = getAddress(
                currentLocation.getLatitude(),
                currentLocation.getLongitude()
        );
    }

    if (pickup.isEmpty()) {
        Toast.makeText(
                this,
                "Please enter pickup location.",
                Toast.LENGTH_SHORT
        ).show();
        return;
    }

    if (destination.isEmpty()) {
        Toast.makeText(
                this,
                "Please enter destination.",
                Toast.LENGTH_SHORT
        ).show();
        return;
    }

    int selectedPaymentId = paymentGroup.getCheckedRadioButtonId();

    if (selectedPaymentId == -1) {
        Toast.makeText(
                this,
                "Please select a payment method.",
                Toast.LENGTH_SHORT
        ).show();
        return;
    }

    RadioButton selectedPayment =
            paymentGroup.findViewById(selectedPaymentId);

    String paymentMethod =
            selectedPayment.getText().toString();

    Location destinationLocation =
            geocodeLocation(destination);

    if (destinationLocation == null) {
        Toast.makeText(
                this,
                "Destination could not be found. Please enter a valid location.",
                Toast.LENGTH_LONG
        ).show();
        return;
    }

    double pickupLatitude = 0;
    double pickupLongitude = 0;

    if (currentLocation != null) {
        pickupLatitude = currentLocation.getLatitude();
        pickupLongitude = currentLocation.getLongitude();
    }

    double destinationLatitude =
            destinationLocation.getLatitude();

    double destinationLongitude =
            destinationLocation.getLongitude();

    double distanceKm = 0;

    if (currentLocation != null) {
        float[] results = new float[1];

        Location.distanceBetween(
                pickupLatitude,
                pickupLongitude,
                destinationLatitude,
                destinationLongitude,
                results
        );

        distanceKm = results[0] / 1000.0;
    }

    double fare = calculateFare(distanceKm);

    fareText.setText(
            "Estimated fare: ₱" +
                    String.format(Locale.US, "%.0f", fare)
    );

    Map<String, Object> ride = new HashMap<>();

    ride.put("passengerId", user.getUid());
    ride.put("passengerPhone",
            user.getPhoneNumber() == null
                    ? ""
                    : user.getPhoneNumber());

    ride.put("pickup", pickup);
    ride.put("destination", destination);

    ride.put("pickupLatitude", pickupLatitude);
    ride.put("pickupLongitude", pickupLongitude);

    ride.put("destinationLatitude", destinationLatitude);
    ride.put("destinationLongitude", destinationLongitude);

    ride.put("paymentMethod", paymentMethod);
    ride.put("paymentStatus", "PENDING");

    ride.put("fare", fare);
    ride.put("distanceKm", distanceKm);

    ride.put("status", "REQUESTED");
    ride.put("createdAt", Timestamp.now());

    ride.put("rideType", "Tricycle");

    bookButton.setEnabled(false);

    db.collection("rides")
            .add(ride)
            .addOnSuccessListener(documentReference -> {

                activeRideId = documentReference.getId();

                preferences.edit()
                        .putString("activeRideId", activeRideId)
                        .apply();

                rideStatusText.setText(
                        "Ride request is waiting for a driver."
                );

                cancelButton.setEnabled(true);
                mapButton.setEnabled(true);
                chatButton.setEnabled(true);

                Toast.makeText(
                        this,
                        "Ride request sent.",
                        Toast.LENGTH_SHORT
                ).show();

                listenToRide(activeRideId);
            })
            .addOnFailureListener(e -> {

                bookButton.setEnabled(true);

                Toast.makeText(
                        this,
                        "Booking failed: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            });
}

private double calculateFare(double distanceKm) {

    double baseFare = 50;
    double perKm = 10;
    double minimumFare = 50;
    double maximumFare = 500;

    double fare = baseFare + (distanceKm * perKm);

    if (fare < minimumFare) {
        fare = minimumFare;
    }

    if (fare > maximumFare) {
        fare = maximumFare;
    }

    return fare;
}

private Location geocodeLocation(String text) {

    try {

        Geocoder geocoder =
                new Geocoder(this, Locale.getDefault());

        List<Address> results =
                geocoder.getFromLocationName(text, 1);

        if (results != null && !results.isEmpty()) {

            Address address = results.get(0);

            Location location =
                    new Location("geocoder");

            location.setLatitude(address.getLatitude());
            location.setLongitude(address.getLongitude());

            return location;
        }

    } catch (IOException ignored) {
    } catch (Exception ignored) {
    }

    return null;
}

private String getAddress(double latitude, double longitude) {

    try {

        Geocoder geocoder =
                new Geocoder(this, Locale.getDefault());

        List<Address> addresses =
                geocoder.getFromLocation(latitude, longitude, 1);

        if (addresses != null && !addresses.isEmpty()) {

            Address address = addresses.get(0);

            String value = address.getAddressLine(0);

            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }

    } catch (Exception ignored) {
    }

    return "Current GPS location";
}

private void restoreActiveRide() {

    String savedRideId =
            preferences.getString("activeRideId", "");

    if (savedRideId == null || savedRideId.trim().isEmpty()) {
        return;
    }

    db.collection("rides")
            .document(savedRideId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {

                if (!documentSnapshot.exists()) {

                    preferences.edit()
                            .remove("activeRideId")
                            .apply();

                    return;
                }

                String status =
                        documentSnapshot.getString("status");

                if (isActiveStatus(status)) {

                    activeRideId = savedRideId;

                    updateRideButtons(status);
                    listenToRide(activeRideId);

                } else {

                    preferences.edit()
                            .remove("activeRideId")
                            .apply();
                }
            });
}

private boolean isActiveStatus(String status) {

    if (status == null) {
        return false;
    }

    return status.equals("REQUESTED")
            || status.equals("ACCEPTED")
            || status.equals("DRIVER_ON_THE_WAY")
            || status.equals("DRIVER_ARRIVED")
            || status.equals("IN_PROGRESS");
}

private void listenToRide(String rideId) {

    if (rideListener != null) {
        rideListener.remove();
    }

    rideListener =
            db.collection("rides")
                    .document(rideId)
                    .addSnapshotListener((snapshot, error) -> {

                        if (error != null || snapshot == null) {
                            return;
                        }

                        if (!snapshot.exists()) {
                            return;
                        }

                        String status =
                                snapshot.getString("status");

                        updateRideButtons(status);

                        if (status != null) {

                            if (status.equals("COMPLETED")) {

                                rideStatusText.setText(
                                        "Ride completed."
                                );

                                preferences.edit()
                                        .remove("activeRideId")
                                        .apply();

                            } else if (status.equals("CANCELLED")) {

                                rideStatusText.setText(
                                        "Ride cancelled."
                                );

                                preferences.edit()
                                        .remove("activeRideId")
                                        .apply();

                            } else {

                                rideStatusText.setText(
                                        "Ride status: " + status
                                );
                            }
                        }
                    });
}

private void updateRideButtons(String status) {

    boolean active = isActiveStatus(status);

    cancelButton.setEnabled(active);
    mapButton.setEnabled(active);
    chatButton.setEnabled(active);

    if (status != null && status.equals("REQUESTED")) {

        bookButton.setEnabled(false);

    } else if (status != null && status.equals("COMPLETED")) {

        bookButton.setEnabled(true);
        cancelButton.setEnabled(false);
        mapButton.setEnabled(false);
        chatButton.setEnabled(false);

    } else if (status != null && status.equals("CANCELLED")) {

        bookButton.setEnabled(true);
        cancelButton.setEnabled(false);
        mapButton.setEnabled(false);
        chatButton.setEnabled(false);
    }
}

private void openLiveMap() {

    if (activeRideId == null ||
            activeRideId.trim().isEmpty()) {

        Toast.makeText(
                this,
                "There is no active ride.",
                Toast.LENGTH_SHORT
        ).show();

        return;
    }

    verifyRideBeforeOpening();
}

private void verifyRideBeforeOpening() {

    db.collection("rides")
            .document(activeRideId)
            .get()
            .addOnSuccessListener(snapshot -> {

                if (!snapshot.exists()) {

                    Toast.makeText(
                            this,
                            "Ride no longer exists.",
                            Toast.LENGTH_SHORT
                    ).show();

                    return;
                }

                String status =
                        snapshot.getString("status");

                if (!isActiveStatus(status)) {

                    Toast.makeText(
                            this,
                            "This ride is no longer active.",
                            Toast.LENGTH_SHORT
                    ).show();

                    return;
                }

                openMapFromRide(snapshot);
            })
            .addOnFailureListener(e ->
                    Toast.makeText(
                            this,
                            "Unable to open map.",
                            Toast.LENGTH_SHORT
                    ).show()
            );
}

private void openMapFromRide(DocumentSnapshot snapshot) {

    Intent intent =
            new Intent(
                    PassengerActivity.this,
                    MapActivity.class
            );

    intent.putExtra("mode", "LIVE_RIDE");

    intent.putExtra(
            "ride_id",
            activeRideId
    );

    intent.putExtra(
            "rideId",
            activeRideId
    );

    String pickup =
            snapshot.getString("pickup");

    String destination =
            snapshot.getString("destination");

    String status =
            snapshot.getString("status");

    Double pickupLatitude =
            getDouble(snapshot, "pickupLatitude");

    Double pickupLongitude =
            getDouble(snapshot, "pickupLongitude");

    Double destinationLatitude =
            getDouble(snapshot, "destinationLatitude");

    Double destinationLongitude =
            getDouble(snapshot, "destinationLongitude");

    intent.putExtra(
            "pickup_address",
            pickup == null ? "" : pickup
    );

    intent.putExtra(
            "destination_address",
            destination == null ? "" : destination
    );

    intent.putExtra(
            "pickup",
            pickup == null ? "" : pickup
    );

    intent.putExtra(
            "destination",
            destination == null ? "" : destination
    );

    intent.putExtra(
            "status",
            status == null ? "" : status
    );

    if (pickupLatitude != null) {
        intent.putExtra(
                "pickup_latitude",
                pickupLatitude
        );
    }

    if (pickupLongitude != null) {
        intent.putExtra(
                "pickup_longitude",
                pickupLongitude
        );
    }

    if (destinationLatitude != null) {
        intent.putExtra(
                "destination_latitude",
                destinationLatitude
        );
    }

    if (destinationLongitude != null) {
        intent.putExtra(
                "destination_longitude",
                destinationLongitude
        );
    }

    startActivity(intent);
}

private Double getDouble(
        DocumentSnapshot snapshot,
        String field) {

    Object value = snapshot.get(field);

    if (value instanceof Number) {
        return ((Number) value).doubleValue();
    }

    return null;
}

private void openChat() {

    if (activeRideId == null ||
            activeRideId.trim().isEmpty()) {

        Toast.makeText(
                this,
                "There is no active ride.",
                Toast.LENGTH_SHORT
        ).show();

        return;
    }

    Intent intent =
            new Intent(
                    PassengerActivity.this,
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

private void cancelRide() {

    if (activeRideId == null ||
            activeRideId.trim().isEmpty()) {

        Toast.makeText(
                this,
                "There is no active ride.",
                Toast.LENGTH_SHORT
        ).show();

        return;
    }

    Map<String, Object> update =
            new HashMap<>();

    update.put("status", "CANCELLED");
    update.put("cancelledBy", "PASSENGER");
    update.put("cancelledAt", Timestamp.now());

    db.collection("rides")
            .document(activeRideId)
            .update(update)
            .addOnSuccessListener(unused -> {

                rideStatusText.setText(
                        "Ride cancelled."
                );

                preferences.edit()
                        .remove("activeRideId")
                        .apply();

                activeRideId = "";

                bookButton.setEnabled(true);
                cancelButton.setEnabled(false);
                mapButton.setEnabled(false);
                chatButton.setEnabled(false);

                Toast.makeText(
                        this,
                        "Ride cancelled.",
                        Toast.LENGTH_SHORT
                ).show();
            })
            .addOnFailureListener(e ->
                    Toast.makeText(
                            this,
                            "Unable to cancel ride: " +
                                    e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show()
            );
}

private void showRideHistory() {

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
            .addOnSuccessListener(querySnapshot -> {

                StringBuilder history =
                        new StringBuilder();

                if (querySnapshot.isEmpty()) {

                    history.append(
                            "No ride history yet."
                    );

                } else {

                    for (DocumentSnapshot document :
                            querySnapshot.getDocuments()) {

                        String pickup =
                                document.getString("pickup");

                        String destination =
                                document.getString(
                                        "destination"
                                );

                        String status =
                                document.getString("status");

                        Double fare =
                                getDouble(
                                        document,
                                        "fare"
                                );

                        history.append(
                                "Pickup: "
                        ).append(
                                pickup == null
                                        ? ""
                                        : pickup
                        );

                        history.append(
                                "\nDestination: "
                        ).append(
                                destination == null
                                        ? ""
                                        : destination
                        );

                        history.append(
                                "\nFare: ₱"
                        ).append(
                                fare == null
                                        ? "0"
                                        : String.format(
                                                Locale.US,
                                                "%.0f",
                                                fare
                                        )
                        );

                        history.append(
                                "\nStatus: "
                        ).append(
                                status == null
                                        ? ""
                                        : status
                        );

                        history.append(
                                "\n\n--------------------\n\n"
                        );
                    }
                }

                TextView historyView =
                        new TextView(this);

                historyView.setText(history.toString());
                historyView.setTextSize(16);
                historyView.setPadding(
                        30,
                        20,
                        30,
                        20
                );

                android.app.AlertDialog dialog =
                        new android.app.AlertDialog.Builder(
                                this
                        )
                                .setTitle("Ride History")
                                .setView(historyView)
                                .setPositiveButton(
                                        "CLOSE",
                                        null
                                )
                                .create();

                dialog.show();
            })
            .addOnFailureListener(e ->
                    Toast.makeText(
                            this,
                            "Unable to load ride history.",
                            Toast.LENGTH_SHORT
                    ).show()
            );
}

private void logout() {

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

    auth.signOut();

    preferences.edit()
            .remove("activeRideId")
            .remove("name")
            .remove("current_name")
            .remove("phone")
            .remove("current_phone")
            .apply();

    Intent intent =
            new Intent(
                    PassengerActivity.this,
                    MainActivity.class
            );

    intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
    );

    startActivity(intent);
    finish();
}

@Override
public void onRequestPermissionsResult(
        int requestCode,
        @NonNull String[] permissions,
        @NonNull int[] grantResults) {

    super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
    );

    if (requestCode == LOCATION_PERMISSION_REQUEST) {

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

            locationText.setText(
                    "Location permission was not granted."
            );
        }
    }
}

@Override
protected void onDestroy() {

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

    super.onDestroy();
}
```

}
