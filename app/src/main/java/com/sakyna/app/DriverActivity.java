
package com.sakyna.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class DriverActivity extends Activity {

    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    private SharedPreferences prefs;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private LocationManager locationManager;
    private LocationListener locationListener;

    private double currentLatitude = 0.0;
    private double currentLongitude = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences(
                "SakayNa",
                MODE_PRIVATE
        );

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        setupLocationListener();

        showDashboard();
    }

    private void setupLocationListener() {

        locationManager =
                (LocationManager) getSystemService(LOCATION_SERVICE);

        locationListener =
                new LocationListener() {

                    @Override
                    public void onLocationChanged(
                            Location location
                    ) {

                        currentLatitude =
                                location.getLatitude();

                        currentLongitude =
                                location.getLongitude();

                        saveDriverLocation(
                                currentLatitude,
                                currentLongitude
                        );
                    }

                    @Override
                    public void onProviderEnabled(
                            String provider
                    ) {
                    }

                    @Override
                    public void onProviderDisabled(
                            String provider
                    ) {
                    }
                };
    }

    private void startGpsTracking() {

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
                    LOCATION_PERMISSION_REQUEST
            );

            return;
        }

        try {

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

            Toast.makeText(
                    this,
                    "GPS tracking started.",
                    Toast.LENGTH_LONG
            ).show();

        } catch (SecurityException e) {

            Toast.makeText(
                    this,
                    "GPS permission is required.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void stopGpsTracking() {

        if (locationManager == null) {
            return;
        }

        try {

            locationManager.removeUpdates(
                    locationListener
            );

        } catch (SecurityException ignored) {
        }
    }

    private void saveDriverLocation(
            double latitude,
            double longitude
    ) {

        if (auth.getCurrentUser() == null) {
            return;
        }

        String driverId =
                auth.getCurrentUser().getUid();

        String driverName =
                prefs.getString(
                        "current_name",
                        "Sakay Na Driver"
                );

        Map<String, Object> location =
                new HashMap<>();

        location.put(
                "driverId",
                driverId
        );

        location.put(
                "driverName",
                driverName
        );

        location.put(
                "latitude",
                latitude
        );

        location.put(
                "longitude",
                longitude
        );

        location.put(
                "online",
                true
        );

        location.put(
                "updatedAt",
                com.google.firebase.firestore.FieldValue.serverTimestamp()
        );

        db.collection("driverLocations")
                .document(driverId)
                .set(location);
    }

    private void markDriverOffline() {

        if (auth.getCurrentUser() == null) {
            return;
        }

        String driverId =
                auth.getCurrentUser().getUid();

        Map<String, Object> update =
                new HashMap<>();

        update.put(
                "online",
                false
        );

        update.put(
                "updatedAt",
                com.google.firebase.firestore.FieldValue.serverTimestamp()
        );

        db.collection("driverLocations")
                .document(driverId)
                .set(update);
    }

    private TextView makeText(
            String text,
            int size
    ) {

        TextView view =
                new TextView(this);

        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(Color.DKGRAY);

        view.setPadding(
                20,
                20,
                20,
                20
        );

        return view;
    }

    private Button makeButton(
            String text
    ) {

        Button button =
                new Button(this);

        button.setText(text);
        button.setTextSize(17);
        button.setAllCaps(false);

        return button;
    }

    private LinearLayout createPage(
            String titleText
    ) {

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setPadding(
                25,
                25,
                25,
                25
        );

        TextView title =
                makeText(
                        titleText,
                        28
                );

        title.setTypeface(
                null,
                Typeface.BOLD
        );

        title.setTextColor(
                Color.rgb(
                        30,
                        100,
                        200
                )
        );

        title.setGravity(
                Gravity.CENTER
        );

        layout.addView(title);

        return layout;
    }

    private void addBackButton(
            LinearLayout layout
    ) {

        Button back =
                makeButton(
                        "Back to Dashboard"
                );

        back.setOnClickListener(
                v -> showDashboard()
        );

        layout.addView(back);
    }

    private void showDashboard() {

        LinearLayout layout =
                createPage(
                        "SAKAY NA"
                );

        TextView title =
                makeText(
                        "Driver Dashboard",
                        21
                );

        title.setGravity(
                Gravity.CENTER
        );

        layout.addView(title);

        boolean online =
                prefs.getBoolean(
                        "driver_online",
                        false
                );

        TextView status =
                makeText(
                        online
                                ? "● DRIVER IS ONLINE"
                                : "● DRIVER IS OFFLINE",
                        21
                );

        status.setGravity(
                Gravity.CENTER
        );

        status.setTypeface(
                null,
                Typeface.BOLD
        );

        status.setTextColor(
                online
                        ? Color.rgb(
                                0,
                                150,
                                80
                        )
                        : Color.rgb(
                                180,
                                80,
                                80
                        )
        );

        layout.addView(status);

        Button onlineButton;

        if (online) {

            onlineButton =
                    makeButton(
                            "🔴 Go Offline"
                    );

        } else {

            onlineButton =
                    makeButton(
                            "🟢 Go Online"
                    );
        }

        onlineButton.setOnClickListener(
                v -> toggleOnline()
        );

        layout.addView(onlineButton);

        Button location =
                makeButton(
                        "📍 GPS LOCATION"
                );

        location.setOnClickListener(
                v -> showGpsStatus()
        );

        layout.addView(location);

        Button requests =
                makeButton(
                        "📥 Booking Requests"
                );

        requests.setOnClickListener(
                v -> showBookingRequest()
        );

        layout.addView(requests);

        Button current =
                makeButton(
                        "🚕 Current Trip"
                );

        current.setOnClickListener(
                v -> showCurrentTrip()
        );

        layout.addView(current);

        Button earnings =
                makeButton(
                        "💰 Earnings & History"
                );

        earnings.setOnClickListener(
                v -> showEarnings()
        );

        layout.addView(earnings);

        Button logout =
                makeButton(
                        "Logout"
                );

        logout.setOnClickListener(
                v -> logout()
        );

        layout.addView(logout);

        setContentView(layout);
    }

    private void showGpsStatus() {

        LinearLayout layout =
                createPage(
                        "GPS LOCATION"
                );

        boolean online =
                prefs.getBoolean(
                        "driver_online",
                        false
                );

        String locationText;

        if (currentLatitude == 0.0
                && currentLongitude == 0.0) {

            locationText =
                    "Waiting for GPS location...\n\n"
                            + "Make sure Location/GPS is enabled on the phone.";

        } else {

            locationText =
                    "LIVE DRIVER LOCATION\n\n"
                            + "Latitude:\n"
                            + currentLatitude
                            + "\n\n"
                            + "Longitude:\n"
                            + currentLongitude
                            + "\n\n"
                            + (
                            online
                                    ? "Status: ONLINE"
                                    : "Status: OFFLINE"
                    );
        }

        TextView location =
                makeText(
                        locationText,
                        19
                );

        location.setGravity(
                Gravity.CENTER
        );

        location.setTypeface(
                null,
                Typeface.BOLD
        );

        layout.addView(location);

        Button refresh =
                makeButton(
                        "🔄 Refresh GPS"
                );

        refresh.setOnClickListener(
                v -> {

                    startGpsTracking();

                    Toast.makeText(
                            this,
                            "Looking for GPS location...",
                            Toast.LENGTH_LONG
                    ).show();
                }
        );

        layout.addView(refresh);

        addBackButton(layout);

        setContentView(layout);
    }

    private void toggleOnline() {

        boolean online =
                prefs.getBoolean(
                        "driver_online",
                        false
                );

        if (!online) {

            prefs.edit()
                    .putBoolean(
                            "driver_online",
                            true
                    )
                    .apply();

            startGpsTracking();

            Toast.makeText(
                    this,
                    "You are now ONLINE. GPS tracking started.",
                    Toast.LENGTH_LONG
            ).show();

        } else {

            prefs.edit()
                    .putBoolean(
                            "driver_online",
                            false
                    )
                    .apply();

            markDriverOffline();

            stopGpsTracking();

            Toast.makeText(
                    this,
                    "You are now OFFLINE.",
                    Toast.LENGTH_LONG
            ).show();
        }

        showDashboard();
    }

    private void showBookingRequest() {

        LinearLayout layout =
                createPage(
                        "BOOKING REQUEST"
                );

        boolean online =
                prefs.getBoolean(
                        "driver_online",
                        false
                );

        if (!online) {

            TextView offline =
                    makeText(
                            "You are offline.\n\nGo ONLINE to receive booking requests.",
                            20
                    );

            offline.setGravity(
                    Gravity.CENTER
            );

            layout.addView(offline);

            addBackButton(layout);

            setContentView(layout);

            return;
        }

        TextView waiting =
                makeText(
                        "GPS is active.\n\nBooking synchronization will be connected to Firestore in the next step.",
                        19
                );

        waiting.setGravity(
                Gravity.CENTER
        );

        layout.addView(waiting);

        addBackButton(layout);

        setContentView(layout);
    }

    private void showCurrentTrip() {

        LinearLayout layout =
                createPage(
                        "CURRENT TRIP"
                );

        String status =
                prefs.getString(
                        "ride_status",
                        ""
                );

        TextView trip =
                makeText(
                        "CURRENT RIDE STATUS\n\n"
                                + (
                                status.isEmpty()
                                        ? "No current trip."
                                        : getReadableStatus(status)
                        )
                                + "\n\n"
                                + "Driver GPS:\n"
                                + currentLatitude
                                + ", "
                                + currentLongitude,
                        19
                );

        trip.setGravity(
                Gravity.CENTER
        );

        trip.setTypeface(
                null,
                Typeface.BOLD
        );

        layout.addView(trip);

        addBackButton(layout);

        setContentView(layout);
    }

    private void showEarnings() {

        LinearLayout layout =
                createPage(
                        "EARNINGS & HISTORY"
                );

        TextView earnings =
                makeText(
                        "Driver earnings and completed ride history will be connected to Firestore next.",
                        20
                );

        earnings.setGravity(
                Gravity.CENTER
        );

        layout.addView(earnings);

        addBackButton(layout);

        setContentView(layout);
    }

    private String getReadableStatus(
            String status
    ) {

        if (status.equals("REQUESTED")) {
            return "WAITING FOR DRIVER";
        }

        if (status.equals("ACCEPTED")) {
            return "RIDE ACCEPTED";
        }

        if (status.equals("DRIVER_ON_THE_WAY")) {
            return "DRIVER ON THE WAY";
        }

        if (status.equals("DRIVER_ARRIVED")) {
            return "DRIVER ARRIVED";
        }

        if (status.equals("IN_PROGRESS")) {
            return "TRIP IN PROGRESS";
        }

        if (status.equals("COMPLETED")) {
            return "RIDE COMPLETED";
        }

        if (status.equals("CANCELLED")) {
            return "RIDE CANCELLED";
        }

        if (status.equals("DECLINED")) {
            return "RIDE DECLINED";
        }

        return status;
    }

    private void logout() {

        stopGpsTracking();

        markDriverOffline();

        auth.signOut();

        prefs.edit()
                .remove("current_phone")
                .remove("current_name")
                .remove("current_role")
                .remove("driver_online")
                .apply();

        Intent intent =
                new Intent(
                        this,
                        MainActivity.class
                );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_NEW_TASK
        );

        startActivity(intent);

        finish();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode ==
                LOCATION_PERMISSION_REQUEST) {

            if (grantResults.length > 0
                    &&
                    grantResults[0]
                            == PackageManager.PERMISSION_GRANTED) {

                Toast.makeText(
                        this,
                        "GPS permission granted.",
                        Toast.LENGTH_LONG
                ).show();

                startGpsTracking();

            } else {

                Toast.makeText(
                        this,
                        "GPS permission was denied.",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    @Override
    protected void onDestroy() {

        stopGpsTracking();

        super.onDestroy();
    }
}
