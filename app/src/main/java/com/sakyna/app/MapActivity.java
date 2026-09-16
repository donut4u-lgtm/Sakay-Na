
package com.sakyna.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
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
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapActivity extends Activity {

    private static final int LOCATION_PERMISSION_REQUEST = 5001;

    private WebView webView;
    private TextView destinationText;
    private TextView statusText;

    private Button gpsButton;
    private Button destinationButton;
    private Button liveMapButton;
    private Button backButton;

    private LocationManager locationManager;
    private LocationListener locationListener;

    private FirebaseFirestore db;
    private ListenerRegistration rideListener;
    private ListenerRegistration driverListener;

    private String mode = "SELECT_DESTINATION";
    private String rideId = "";

    private double pickupLatitude = 0;
    private double pickupLongitude = 0;

    private double destinationLatitude = 0;
    private double destinationLongitude = 0;

    private double driverLatitude = 0;
    private double driverLongitude = 0;

    private String pickupAddress = "";
    private String destinationAddress = "";

    private boolean mapReady = false;
    private boolean destinationChosen = false;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();

        readIntent();
        buildInterface();
        setupMap();
        requestLocation();
    }

    private void readIntent() {

        Intent intent = getIntent();

        if (intent == null) {
            return;
        }

        String incomingMode =
                intent.getStringExtra("mode");

        if (incomingMode != null &&
                !incomingMode.trim().isEmpty()) {

            mode = incomingMode;
        }

        rideId =
                safe(intent.getStringExtra("ride_id"));

        pickupLatitude =
                intent.getDoubleExtra(
                        "pickup_latitude", 0);

        pickupLongitude =
                intent.getDoubleExtra(
                        "pickup_longitude", 0);

        destinationLatitude =
                intent.getDoubleExtra(
                        "destination_latitude", 0);

        destinationLongitude =
                intent.getDoubleExtra(
                        "destination_longitude", 0);

        pickupAddress =
                safe(intent.getStringExtra(
                        "pickup_address"));

        destinationAddress =
                safe(intent.getStringExtra(
                        "destination_address"));

        destinationChosen =
                destinationLatitude != 0 &&
                destinationLongitude != 0;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void buildInterface() {

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL);

        root.setBackgroundColor(
                Color.rgb(245, 248, 246));

        TextView header =
                new TextView(this);

        header.setText("🛺  SAKAY NA");
        header.setTextSize(25);
        header.setTypeface(
                null,
                Typeface.BOLD);
        header.setTextColor(
                Color.WHITE);
        header.setGravity(
                Gravity.CENTER);
        header.setPadding(
                10, 20, 10, 20);

        header.setBackgroundColor(
                Color.rgb(0, 125, 75));

        root.addView(
                header,
                new LinearLayout.LayoutParams(
                        -1,
                        -2));

        TextView subtitle =
                new TextView(this);

        if ("LIVE_RIDE".equalsIgnoreCase(mode)) {

            subtitle.setText(
                    "🚦 LIVE RIDE • Your trip is in progress");

        } else {

            subtitle.setText(
                    "📍 Choose where you want to go");
        }

        subtitle.setTextSize(16);
        subtitle.setTypeface(
                null,
                Typeface.BOLD);
        subtitle.setTextColor(
                Color.rgb(30, 80, 55));
        subtitle.setGravity(
                Gravity.CENTER);
        subtitle.setPadding(
                12, 12, 12, 8);

        root.addView(subtitle);

        destinationText =
                new TextView(this);

        if (destinationChosen) {

            destinationText.setText(
                    "🎯 Destination:\n" +
                    destinationAddress);

        } else {

            destinationText.setText(
                    "🎯 Destination not selected\n" +
                    "Tap the map to choose one.");
        }

        destinationText.setTextSize(15);
        destinationText.setTextColor(
                Color.DKGRAY);
        destinationText.setGravity(
                Gravity.CENTER);
        destinationText.setPadding(
                14, 8, 14, 8);

        root.addView(destinationText);

        statusText =
                new TextView(this);

        statusText.setText(
                "🗺️ Loading OpenStreetMap...");

        statusText.setTextSize(14);
        statusText.setTextColor(
                Color.GRAY);
        statusText.setGravity(
                Gravity.CENTER);
        statusText.setPadding(
                10, 4, 10, 8);

        root.addView(statusText);

        webView =
                new WebView(this);

        LinearLayout.LayoutParams mapParams =
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1);

        mapParams.setMargins(
                10, 4, 10, 6);

        root.addView(
                webView,
                mapParams);

        LinearLayout controls =
                new LinearLayout(this);

        controls.setOrientation(
                LinearLayout.VERTICAL);

        controls.setGravity(
                Gravity.CENTER_HORIZONTAL);

        controls.setPadding(
                12, 4, 12, 10);

        gpsButton =
                makeButton(
                        "📍 CURRENT GPS");

        gpsButton.setOnClickListener(
                v -> centerOnGPS());

        controls.addView(
                gpsButton,
                buttonParams());

        if ("SELECT_DESTINATION"
                .equalsIgnoreCase(mode)) {

            destinationButton =
                    makeButton(
                            "🎯 SET DESTINATION");

            destinationButton.setEnabled(
                    destinationChosen);

            destinationButton.setOnClickListener(
                    v -> returnDestination());

            controls.addView(
                    destinationButton,
                    buttonParams());
        }

        liveMapButton =
                makeButton(
                        "🚦 LIVE MAP");

        liveMapButton.setOnClickListener(
                v -> {

                    if ("LIVE_RIDE"
                            .equalsIgnoreCase(mode)) {

                        Toast.makeText(
                                this,
                                "You are already on the live map.",
                                Toast.LENGTH_SHORT
                        ).show();

                    } else {

                        Toast.makeText(
                                this,
                                "Live map becomes available after a ride is active.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });

        controls.addView(
                liveMapButton,
                buttonParams());

        backButton =
                makeButton(
                        "← BACK / HOME");

        backButton.setOnClickListener(
                v -> finish());

        controls.addView(
                backButton,
                buttonParams());

        root.addView(controls);

        setContentView(root);
    }

    private Button makeButton(String text) {

        Button button =
                new Button(this);

        button.setText(text);
        button.setTextSize(15);
        button.setTypeface(
                null,
                Typeface.BOLD);
        button.setTextColor(
                Color.WHITE);
        button.setGravity(
                Gravity.CENTER);

        button.setAllCaps(false);

        button.setBackgroundColor(
                Color.rgb(0, 125, 75));

        button.setPadding(
                12, 12, 12, 12);

        return button;
    }

    private LinearLayout.LayoutParams buttonParams() {

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        -1,
                        -2);

        params.setMargins(
                0, 3, 0, 3);

        return params;
    }

    private void setupMap() {

        WebSettings settings =
                webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        settings.setSupportZoom(true);

        settings.setLoadWithOverviewMode(false);
        settings.setUseWideViewPort(false);

        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.setClickable(true);

        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);

        webView.setWebViewClient(
                new WebViewClient());

        webView.setWebChromeClient(
                new WebChromeClient());

        webView.addJavascriptInterface(
                new MapBridge(),
                "Android");

        webView.loadDataWithBaseURL(
                "https://sakyna-map.local/",
                mapHtml(),
                "text/html",
                "UTF-8",
                null);
    }

    private String mapHtml() {

        String lat =
                pickupLatitude != 0
                        ? String.format(
                        Locale.US,
                        "%.7f",
                        pickupLatitude)
                        : "14.000000";

        String lng =
                pickupLongitude != 0
                        ? String.format(
                        Locale.US,
                        "%.7f",
                        pickupLongitude)
                        : "121.000000";

        return "<!DOCTYPE html>" +
                "<html><head>" +

                "<meta name='viewport' " +
                "content='width=device-width," +
                "initial-scale=1.0," +
                "maximum-scale=1.0," +
                "user-scalable=yes'>" +

                "<link rel='stylesheet' " +
                "href='https://unpkg.com/" +
                "leaflet@1.9.4/dist/leaflet.css'>" +

                "<script src='https://unpkg.com/" +
                "leaflet@1.9.4/dist/leaflet.js'>" +
                "</script>" +

                "<style>" +
                "html,body,#map{" +
                "height:100%;" +
                "width:100%;" +
                "margin:0;" +
                "padding:0;" +
                "overflow:hidden;" +
                "}" +
                "</style>" +

                "</head><body>" +

                "<div id='map'></div>" +

                "<script>" +

                "var map=L.map('map'," +
                "{zoomControl:true," +
                "dragging:true," +
                "touchZoom:true," +
                "scrollWheelZoom:true," +
                "doubleClickZoom:true});" +

                "var userMarker=null;" +
                "var destinationMarker=null;" +
                "var driverMarker=null;" +

                "map.setView([" +
                lat + "," + lng + "],15);" +

                "L.tileLayer(" +
                "'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'," +
                "{maxZoom:19," +
                "attribution:'© OpenStreetMap contributors'}" +
                ").addTo(map);" +

                "function updateUser(lat,lng,center){" +
                "if(userMarker===null){" +
                "userMarker=L.marker([lat,lng])" +
                ".addTo(map)" +
                ".bindPopup('📍 You are here');" +
                "}else{" +
                "userMarker.setLatLng([lat,lng]);" +
                "}" +
                "if(center)map.setView([lat,lng],17);" +
                "}" +

                "function setDestination(lat,lng,address){" +
                "if(destinationMarker!==null)" +
                "map.removeLayer(destinationMarker);" +
                "destinationMarker=L.marker([lat,lng])" +
                ".addTo(map)" +
                ".bindPopup('🎯 '+address)" +
                ".openPopup();" +
                "map.setView([lat,lng],17);" +
                "}" +

                "function setDriver(lat,lng){" +
                "if(driverMarker===null){" +
                "driverMarker=L.marker([lat,lng])" +
                ".addTo(map)" +
                ".bindPopup('🛺 Driver');" +
                "}else{" +
                "driverMarker.setLatLng([lat,lng]);" +
                "}" +
                "}" +

                "map.on('click',function(e){" +
                "if(window.Android)" +
                "Android.mapClicked(" +
                "e.latlng.lat,e.latlng.lng);" +
                "});" +

                "map.whenReady(function(){" +
                "if(window.Android)" +
                "Android.mapReady();" +
                "});" +

                "</script></body></html>";
    }

    private void requestLocation() {

        if (
                checkSelfPermission(
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED
                        &&
                checkSelfPermission(
                        Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED
        ) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST);

            return;
        }

        startLocation();
    }

    private void startLocation() {

        locationManager =
                (LocationManager)
                        getSystemService(
                                LOCATION_SERVICE);

        if (locationManager == null) {
            return;
        }

        locationListener =
                new LocationListener() {

                    @Override
                    public void onLocationChanged(
                            @NonNull Location location) {

                        pickupLatitude =
                                location.getLatitude();

                        pickupLongitude =
                                location.getLongitude();

                        if (mapReady) {

                            sendJS(
                                    "updateUser(" +
                                    pickupLatitude +
                                    "," +
                                    pickupLongitude +
                                    ",false);");
                        }
                    }
                };

        try {

            if (
                    checkSelfPermission(
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED
                            ||
                    checkSelfPermission(
                            Manifest.permission.ACCESS_COARSE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED
            ) {

                if (locationManager.isProviderEnabled(
                        LocationManager.GPS_PROVIDER)) {

                    locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            3000,
                            5,
                            locationListener);
                }

                if (locationManager.isProviderEnabled(
                        LocationManager.NETWORK_PROVIDER)) {

                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            3000,
                            5,
                            locationListener);
                }
            }

        } catch (SecurityException ignored) {
        }

        if ("LIVE_RIDE".equalsIgnoreCase(mode)) {
            startLiveRide();
        }
    }

    private void centerOnGPS() {

        if (pickupLatitude == 0 ||
                pickupLongitude == 0) {

            Toast.makeText(
                    this,
                    "Waiting for GPS location...",
                    Toast.LENGTH_SHORT).show();

            requestLocation();
            return;
        }

        sendJS(
                "updateUser(" +
                pickupLatitude + "," +
                pickupLongitude +
                ",true);");
    }

    private void sendJS(String javascript) {

        if (webView == null) {
            return;
        }

        webView.post(
                () -> webView.evaluateJavascript(
                        javascript,
                        null));
    }

    private void mapReady() {

        mapReady = true;

        if (pickupLatitude != 0 &&
                pickupLongitude != 0) {

            sendJS(
                    "updateUser(" +
                    pickupLatitude + "," +
                    pickupLongitude +
                    ",true);");
        }

        if (destinationChosen) {

            sendJS(
                    "setDestination(" +
                    destinationLatitude + "," +
                    destinationLongitude +
                    ",'" +
                    escapeJS(destinationAddress) +
                    "');");
        }

        statusText.setText(
                "🗺️ Map ready • Tap anywhere to choose destination");
    }

    private String escapeJS(String text) {

        if (text == null) {
            return "";
        }

        return text
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", " ")
                .replace("\r", " ");
    }

    private void mapClicked(
            double lat,
            double lng) {

        if (!"SELECT_DESTINATION"
                .equalsIgnoreCase(mode)) {
            return;
        }

        destinationLatitude = lat;
        destinationLongitude = lng;
        destinationChosen = true;

        statusText.setText(
                "🎯 Destination selected • Finding address...");

        reverseGeocode(lat, lng);
    }

    private void reverseGeocode(
            double lat,
            double lng) {

        new Thread(() -> {

            String address = "";

            try {

                Geocoder geocoder =
                        new Geocoder(
                                this,
                                Locale.getDefault());

                List<Address> results =
                        geocoder.getFromLocation(
                                lat,
                                lng,
                                1);

                if (results != null &&
                        !results.isEmpty()) {

                    Address a =
                            results.get(0);

                    if (a.getAddressLine(0) != null) {
                        address =
                                a.getAddressLine(0);
                    }
                }

            } catch (IOException ignored) {
            } catch (Exception ignored) {
            }

            final String finalAddress =
                    address.isEmpty()
                            ? String.format(
                            Locale.US,
                            "%.6f, %.6f",
                            lat,
                            lng)
                            : address;

            handler.post(() -> {

                destinationAddress =
                        finalAddress;

                destinationText.setText(
                        "🎯 Destination:\n" +
                        destinationAddress);

                statusText.setText(
                        "✅ Destination selected");

                if (destinationButton != null) {
                    destinationButton.setEnabled(true);
                }

                sendJS(
                        "setDestination(" +
                        lat + "," + lng +
                        ",'" +
                        escapeJS(finalAddress) +
                        "');");
            });

        }).start();
    }

    private void returnDestination() {

        if (!destinationChosen) {

            Toast.makeText(
                    this,
                    "Please tap the map first.",
                    Toast.LENGTH_SHORT).show();

            return;
        }

        Intent result =
                new Intent();

        result.putExtra(
                "destination_latitude",
                destinationLatitude);

        result.putExtra(
                "destination_longitude",
                destinationLongitude);

        result.putExtra(
                "destination_address",
                destinationAddress);

        setResult(
                RESULT_OK,
                result);

        finish();
    }

    private void startLiveRide() {

        if (rideId == null ||
                rideId.trim().isEmpty()) {
            return;
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

                                    Double pLat =
                                            snapshot.getDouble(
                                                    "pickupLatitude");

                                    Double pLng =
                                            snapshot.getDouble(
                                                    "pickupLongitude");

                                    Double dLat =
                                            snapshot.getDouble(
                                                    "destinationLatitude");

                                    Double dLng =
                                            snapshot.getDouble(
                                                    "destinationLongitude");

                                    if (pLat != null &&
                                            pLng != null) {

                                        pickupLatitude =
                                                pLat;

                                        pickupLongitude =
                                                pLng;

                                        sendJS(
                                                "updateUser(" +
                                                pLat + "," +
                                                pLng +
                                                ",false);");
                                    }

                                    if (dLat != null &&
                                            dLng != null) {

                                        destinationLatitude =
                                                dLat;

                                        destinationLongitude =
                                                dLng;

                                        destinationChosen =
                                                true;

                                        sendJS(
                                                "setDestination(" +
                                                dLat + "," +
                                                dLng +
                                                ",'Destination');");
                                    }

                                    String status =
                                            snapshot.getString(
                                                    "status");

                                    if (status != null) {

                                        statusText.setText(
                                                "🚦 Ride status: " +
                                                status);
                                    }

                                    String driverId =
                                            snapshot.getString(
                                                    "driverId");

                                    if (driverId != null &&
                                            !driverId.trim().isEmpty()) {

                                        listenDriver(driverId);
                                    }
                                });
    }

    private void listenDriver(
            String driverId) {

        if (driverListener != null) {
            driverListener.remove();
        }

        driverListener =
                db.collection("driverLocations")
                        .document(driverId)
                        .addSnapshotListener(
                                (snapshot, error) -> {

                                    if (error != null ||
                                            snapshot == null ||
                                            !snapshot.exists()) {
                                        return;
                                    }

                                    Double lat =
                                            snapshot.getDouble(
                                                    "latitude");

                                    Double lng =
                                            snapshot.getDouble(
                                                    "longitude");

                                    if (lat == null ||
                                            lng == null) {
                                        return;
                                    }

                                    driverLatitude = lat;
                                    driverLongitude = lng;

                                    sendJS(
                                            "setDriver(" +
                                            lat + "," +
                                            lng + ");");
                                });
    }

    public class MapBridge {

        @JavascriptInterface
        public void mapReady() {

            runOnUiThread(
                    () -> MapActivity.this.mapReady());
        }

        @JavascriptInterface
        public void mapClicked(
                double lat,
                double lng) {

            runOnUiThread(
                    () -> MapActivity.this.mapClicked(
                            lat,
                            lng));
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] results) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                results);

        if (requestCode ==
                LOCATION_PERMISSION_REQUEST &&
                results.length > 0 &&
                results[0] ==
                        PackageManager.PERMISSION_GRANTED) {

            startLocation();
        }
    }

    @Override
    protected void onDestroy() {

        if (rideListener != null) {
            rideListener.remove();
            rideListener = null;
        }

        if (driverListener != null) {
            driverListener.remove();
            driverListener = null;
        }

        if (locationManager != null &&
                locationListener != null) {

            try {
                locationManager.removeUpdates(
                        locationListener);
            } catch (SecurityException ignored) {
            }
        }

        if (webView != null) {

            webView.stopLoading();

            webView.removeJavascriptInterface(
                    "Android");

            webView.destroy();
            webView = null;
        }

        super.onDestroy();
    }
}
