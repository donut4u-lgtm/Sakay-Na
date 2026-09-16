
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
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class MapActivity extends Activity {

    private static final int LOCATION_REQUEST = 8001;

    private WebView webView;
    private TextView titleText;
    private TextView selectedText;

    private LocationManager locationManager;
    private Location currentLocation;

    private FirebaseFirestore db;
    private ListenerRegistration rideListener;

    private String rideId;
    private String mode;

    private double pickupLatitude;
    private double pickupLongitude;

    private double destinationLatitude;
    private double destinationLongitude;

    private String destinationAddress = "";

    private boolean mapReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();

        rideId = getIntent().getStringExtra("rideId");
        mode = getIntent().getStringExtra("mode");

        if (mode == null) {
            mode = "";
        }

        pickupLatitude = getIntent().getDoubleExtra(
                "pickup_latitude", 0.0);

        pickupLongitude = getIntent().getDoubleExtra(
                "pickup_longitude", 0.0);

        destinationLatitude = getIntent().getDoubleExtra(
                "destination_latitude", 0.0);

        destinationLongitude = getIntent().getDoubleExtra(
                "destination_longitude", 0.0);

        destinationAddress = getIntent().getStringExtra(
                "destination_address");

        if (destinationAddress == null) {
            destinationAddress = "";
        }

        buildScreen();
        setupLocation();

        if ("SELECT_DESTINATION".equals(mode)) {
            setupDestinationMode();
        } else if (rideId != null && !rideId.isEmpty()) {
            setupLiveRideMode();
        } else {
            setupDestinationMode();
        }
    }

    private void buildScreen() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        titleText = new TextView(this);
        titleText.setTextSize(20);
        titleText.setGravity(Gravity.CENTER);
        titleText.setPadding(12, 18, 12, 18);
        root.addView(titleText);

        selectedText = new TextView(this);
        selectedText.setTextSize(15);
        selectedText.setPadding(16, 8, 16, 8);
        root.addView(selectedText);

        webView = new WebView(this);

        LinearLayout.LayoutParams webParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1);

        root.addView(webView, webParams);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);

        Button myLocation = new Button(this);
        myLocation.setText("My Location");

        buttons.addView(
                myLocation,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1));

        Button confirmButton = new Button(this);
        confirmButton.setText("Confirm Destination");

        buttons.addView(
                confirmButton,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1));

        root.addView(buttons);

        Button backButton = new Button(this);
        backButton.setText("Back");
        root.addView(backButton);

        setContentView(root);

        myLocation.setOnClickListener(
                v -> centerOnMyLocation());

        confirmButton.setOnClickListener(
                v -> confirmDestination());

        backButton.setOnClickListener(
                v -> finish());

        setupWebView();
    }

    private void setupDestinationMode() {

        titleText.setText(
                "Choose Your Destination");

        selectedText.setText(
                "Tap anywhere on the map to choose your destination.");
    }

    private void setupLiveRideMode() {

        titleText.setText("Live Ride Map");

        selectedText.setText(
                "Driver location will appear on the map.");

        listenForRide();
    }

    private void setupWebView() {

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setLoadWithOverviewMode(false);
        settings.setUseWideViewPort(true);

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());

        webView.addJavascriptInterface(
                new MapBridge(),
                "Android");

        webView.setBackgroundColor(
                android.graphics.Color.WHITE);

        loadMapHtml();
    }

    private void loadMapHtml() {

        String html =
                "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +

                "<meta name='viewport' " +
                "content='width=device-width, initial-scale=1.0'>" +

                "<link rel='stylesheet' " +
                "href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'>" +

                "<style>" +
                "html,body,#map{" +
                "height:100%;" +
                "width:100%;" +
                "margin:0;" +
                "padding:0;" +
                "}" +

                "#map{" +
                "background:#e8e8e8;" +
                "}" +
                "</style>" +

                "</head>" +

                "<body>" +

                "<div id='map'></div>" +

                "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'>" +
                "</script>" +

                "<script>" +

                "var map;" +
                "var destinationMarker=null;" +
                "var pickupMarker=null;" +
                "var driverMarker=null;" +

                "function startMap() {" +

                "try {" +

                "var lat=" +
                getInitialLatitude() +
                ";" +

                "var lng=" +
                getInitialLongitude() +
                ";" +

                "map=L.map('map',{zoomControl:true})" +
                ".setView([lat,lng],15);" +

                "L.tileLayer(" +
                "'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'," +
                "{" +
                "maxZoom:19," +
                "attribution:'© OpenStreetMap contributors'" +
                "}" +
                ").addTo(map);" +

                "map.on('click',function(e){" +

                "Android.mapClicked(" +
                "e.latlng.lat," +
                "e.latlng.lng);" +

                "});" +

                "map.whenReady(function(){" +
                "Android.mapReady();" +
                "});" +

                "} catch(e) {" +

                "Android.mapError(String(e));" +

                "}" +
                "}" +

                "function setDestination(lat,lng) {" +

                "if(!map)return;" +

                "if(destinationMarker){" +
                "map.removeLayer(destinationMarker);" +
                "}" +

                "destinationMarker=" +
                "L.marker([lat,lng]).addTo(map);" +

                "destinationMarker" +
                ".bindPopup('Destination')" +
                ".openPopup();" +

                "}" +

                "function setPickup(lat,lng) {" +

                "if(!map)return;" +

                "if(pickupMarker){" +
                "map.removeLayer(pickupMarker);" +
                "}" +

                "pickupMarker=" +
                "L.marker([lat,lng]).addTo(map);" +

                "pickupMarker.bindPopup('Pickup');" +

                "}" +

                "function setDriver(lat,lng) {" +

                "if(!map)return;" +

                "if(driverMarker){" +
                "map.removeLayer(driverMarker);" +
                "}" +

                "driverMarker=" +
                "L.marker([lat,lng]).addTo(map);" +

                "driverMarker.bindPopup('Driver');" +

                "map.setView([lat,lng],15);" +

                "}" +

                "function centerMap(lat,lng) {" +

                "if(!map)return;" +

                "map.setView([lat,lng],16);" +

                "}" +

                "</script>" +

                "</body>" +
                "</html>";

        webView.loadDataWithBaseURL(
                "https://sakyna-map.local/",
                html,
                "text/html",
                "UTF-8",
                null);
    }

    private double getInitialLatitude() {

        if (pickupLatitude != 0.0) {
            return pickupLatitude;
        }

        if (destinationLatitude != 0.0) {
            return destinationLatitude;
        }

        return 14.35;
    }

    private double getInitialLongitude() {

        if (pickupLongitude != 0.0) {
            return pickupLongitude;
        }

        if (destinationLongitude != 0.0) {
            return destinationLongitude;
        }

        return 121.05;
    }

    private class MapBridge {

        @JavascriptInterface
        public void mapReady() {

            runOnUiThread(() -> {

                mapReady = true;

                if (pickupLatitude != 0.0
                        && pickupLongitude != 0.0) {

                    webView.loadUrl(
                            "javascript:setPickup("
                                    + pickupLatitude
                                    + ","
                                    + pickupLongitude
                                    + ")");
                }

                if ("SELECT_DESTINATION".equals(mode)
                        && destinationLatitude != 0.0
                        && destinationLongitude != 0.0) {

                    webView.loadUrl(
                            "javascript:setDestination("
                                    + destinationLatitude
                                    + ","
                                    + destinationLongitude
                                    + ")");
                }
            });
        }

        @JavascriptInterface
        public void mapClicked(
                double lat,
                double lng) {

            runOnUiThread(() -> {

                destinationLatitude = lat;
                destinationLongitude = lng;

                webView.loadUrl(
                        "javascript:setDestination("
                                + lat
                                + ","
                                + lng
                                + ")");

                selectedText.setText(
                        String.format(
                                Locale.US,
                                "Selected: %.6f, %.6f\nGetting address...",
                                lat,
                                lng));

                reverseGeocode(lat, lng);
            });
        }

        @JavascriptInterface
        public void mapError(String message) {

            runOnUiThread(() -> {

                selectedText.setText(
                        "Map could not load. Check your internet connection.");

                Toast.makeText(
                        MapActivity.this,
                        "OpenStreetMap could not load.",
                        Toast.LENGTH_LONG).show();
            });
        }
    }

    private void reverseGeocode(
            double lat,
            double lng) {

        Executors.newSingleThreadExecutor()
                .execute(() -> {

                    String address =
                            getAddressText(lat, lng);

                    runOnUiThread(() -> {

                        if (address == null
                                || address.trim().isEmpty()) {

                            destinationAddress =
                                    String.format(
                                            Locale.US,
                                            "%.6f, %.6f",
                                            lat,
                                            lng);

                        } else {

                            destinationAddress = address;
                        }

                        selectedText.setText(
                                "Destination:\n"
                                        + destinationAddress);
                    });
                });
    }

    private String getAddressText(
            double lat,
            double lng) {

        if (!Geocoder.isPresent()) {
            return null;
        }

        try {

            Geocoder geocoder =
                    new Geocoder(
                            this,
                            Locale.getDefault());

            List<Address> addresses =
                    geocoder.getFromLocation(
                            lat,
                            lng,
                            1);

            if (addresses != null
                    && !addresses.isEmpty()) {

                Address address =
                        addresses.get(0);

                String line =
                        address.getAddressLine(0);

                if (line != null
                        && !line.isEmpty()) {

                    return line;
                }
            }

        } catch (Exception ignored) {
        }

        return null;
    }

    private void centerOnMyLocation() {

        if (currentLocation == null) {

            Toast.makeText(
                    this,
                    "Waiting for your GPS location.",
                    Toast.LENGTH_SHORT).show();

            return;
        }

        double lat =
                currentLocation.getLatitude();

        double lng =
                currentLocation.getLongitude();

        if (mapReady) {

            webView.loadUrl(
                    "javascript:centerMap("
                            + lat
                            + ","
                            + lng
                            + ")");
        }
    }

    private void confirmDestination() {

        if (destinationLatitude == 0.0
                && destinationLongitude == 0.0) {

            Toast.makeText(
                    this,
                    "Tap the map to select a destination first.",
                    Toast.LENGTH_LONG).show();

            return;
        }

        Intent result = new Intent();

        result.putExtra(
                "destination_latitude",
                destinationLatitude);

        result.putExtra(
                "destination_longitude",
                destinationLongitude);

        result.putExtra(
                "destination_address",
                destinationAddress);

        result.putExtra(
                "pickup_latitude",
                pickupLatitude);

        result.putExtra(
                "pickup_longitude",
                pickupLongitude);

        result.putExtra(
                "pickup_address",
                getIntent().getStringExtra(
                        "pickup_address"));

        setResult(
                RESULT_OK,
                result);

        finish();
    }

    private void setupLocation() {

        locationManager =
                (LocationManager)
                        getSystemService(
                                LOCATION_SERVICE);

        if (locationManager == null) {
            return;
        }

        if (checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_REQUEST);

            return;
        }

        try {

            Location last =
                    locationManager.getLastKnownLocation(
                            LocationManager.GPS_PROVIDER);

            if (last == null) {

                last =
                        locationManager.getLastKnownLocation(
                                LocationManager.NETWORK_PROVIDER);
            }

            if (last != null) {
                currentLocation = last;
            }

            if (locationManager.isProviderEnabled(
                    LocationManager.GPS_PROVIDER)) {

                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        2000,
                        5,
                        locationListener);

            } else if (locationManager.isProviderEnabled(
                    LocationManager.NETWORK_PROVIDER)) {

                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        2000,
                        5,
                        locationListener);
            }

        } catch (SecurityException ignored) {
        } catch (Exception ignored) {
        }
    }

    private final LocationListener locationListener =
            new LocationListener() {

        @Override
        public void onLocationChanged(
                @NonNull Location location) {

            currentLocation = location;

            if ("SELECT_DESTINATION".equals(mode)
                    && pickupLatitude == 0.0
                    && pickupLongitude == 0.0) {

                pickupLatitude =
                        location.getLatitude();

                pickupLongitude =
                        location.getLongitude();

                if (mapReady) {

                    webView.loadUrl(
                            "javascript:centerMap("
                                    + pickupLatitude
                                    + ","
                                    + pickupLongitude
                                    + ")");

                    webView.loadUrl(
                            "javascript:setPickup("
                                    + pickupLatitude
                                    + ","
                                    + pickupLongitude
                                    + ")");
                }
            }
        }
    };

    private void listenForRide() {

        if (rideId == null
                || rideId.isEmpty()) {
            return;
        }

        rideListener =
                db.collection("rides")
                        .document(rideId)
                        .addSnapshotListener(
                                (snapshot, error) -> {

                                    if (error != null
                                            || snapshot == null
                                            || !snapshot.exists()) {
                                        return;
                                    }

                                    updateLiveRide(snapshot);
                                });
    }

    private void updateLiveRide(
            DocumentSnapshot ride) {

        Double driverLat =
                ride.getDouble(
                        "driverLatitude");

        Double driverLng =
                ride.getDouble(
                        "driverLongitude");

        if (driverLat == null) {

            driverLat =
                    ride.getDouble(
                            "driverLat");
        }

        if (driverLng == null) {

            driverLng =
                    ride.getDouble(
                            "driverLng");
        }

        if (driverLat == null
                || driverLng == null) {
            return;
        }

        if (mapReady) {

            final double lat = driverLat;
            final double lng = driverLng;

            runOnUiThread(() -> {

                webView.loadUrl(
                        "javascript:setDriver("
                                + lat
                                + ","
                                + lng
                                + ")");

                selectedText.setText(
                        String.format(
                                Locale.US,
                                "Driver location: %.6f, %.6f",
                                lat,
                                lng));
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults);

        if (requestCode == LOCATION_REQUEST) {

            boolean granted = false;

            for (int result : grantResults) {

                if (result ==
                        PackageManager.PERMISSION_GRANTED) {

                    granted = true;
                    break;
                }
            }

            if (granted) {
                setupLocation();
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
                        locationListener);
            } catch (SecurityException ignored) {
            } catch (Exception ignored) {
            }
        }

        if (webView != null) {
            webView.destroy();
        }

        super.onDestroy();
    }
}
