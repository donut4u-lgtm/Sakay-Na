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
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.Gravity;
import android.view.ViewGroup;
import android.graphics.Color;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapActivity extends Activity {

    private static final int LOCATION_PERMISSION_REQUEST = 5001;

    private WebView webView;
    private TextView statusText;

    private LocationManager locationManager;
    private LocationListener locationListener;

    private FirebaseFirestore db;
    private ListenerRegistration rideListener;

    private double currentLat = 0.0;
    private double currentLng = 0.0;

    private double destinationLat = 0.0;
    private double destinationLng = 0.0;

    private String destinationAddress = "";

    private String mode = "SELECT_DESTINATION";
    private String rideId = "";

    private boolean mapLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();

        Intent intent = getIntent();

        if (intent != null) {

            String receivedMode =
                    intent.getStringExtra("mode");

            if (receivedMode != null &&
                    !receivedMode.trim().isEmpty()) {

                mode = receivedMode;
            }

            String receivedRideId =
                    intent.getStringExtra("ride_id");

            if (receivedRideId != null &&
                    !receivedRideId.trim().isEmpty()) {

                rideId = receivedRideId;
                mode = "LIVE_RIDE";
            }

            destinationLat =
                    intent.getDoubleExtra(
                            "destination_latitude",
                            0.0
                    );

            destinationLng =
                    intent.getDoubleExtra(
                            "destination_longitude",
                            0.0
                    );
        }

        buildScreen();
        setupLocation();
    }

    private void buildScreen() {

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setBackgroundColor(
                Color.WHITE
        );

        TextView title =
                new TextView(this);

        if ("LIVE_RIDE".equals(mode)) {
            title.setText("🗺️ LIVE RIDE MAP");
        } else {
            title.setText("📍 SELECT DESTINATION");
        }

        title.setTextSize(21);
        title.setGravity(Gravity.CENTER);
        title.setPadding(10, 15, 10, 10);

        root.addView(
                title,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        statusText =
                new TextView(this);

        statusText.setText(
                "📍 Finding your exact location..."
        );

        statusText.setTextSize(15);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(10, 5, 10, 10);

        root.addView(
                statusText,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        webView = new WebView(this);

        WebSettings settings =
                webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setGeolocationEnabled(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        webView.setWebViewClient(
                new WebViewClient()
        );

        webView.setWebChromeClient(
                new WebChromeClient()
        );

        webView.addJavascriptInterface(
                new MapBridge(),
                "Android"
        );

        root.addView(
                webView,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        if ("SELECT_DESTINATION".equals(mode)) {

            Button useLocationButton =
                    new Button(this);

            useLocationButton.setText(
                    "📍 USE MY CURRENT LOCATION"
            );

            useLocationButton.setOnClickListener(
                    v -> centerOnCurrentLocation()
            );

            root.addView(
                    useLocationButton,
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )
            );

            Button confirmButton =
                    new Button(this);

            confirmButton.setText(
                    "🏁 CONFIRM DESTINATION"
            );

            confirmButton.setOnClickListener(
                    v -> confirmDestination()
            );

            root.addView(
                    confirmButton,
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )
            );
        }

        Button backButton =
                new Button(this);

        backButton.setText("⬅️ BACK");

        backButton.setOnClickListener(
                v -> finish()
        );

        root.addView(
                backButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        setContentView(root);

        loadMap();
    }

    private void loadMap() {

        String html =
                "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta name='viewport' " +
                "content='width=device-width, initial-scale=1.0'>" +

                "<link rel='stylesheet' " +
                "href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'>" +

                "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'>" +
                "</script>" +

                "<style>" +
                "html,body,#map{" +
                "height:100%;" +
                "width:100%;" +
                "margin:0;" +
                "padding:0;" +
                "}" +
                ".leaflet-control-attribution{" +
                "font-size:9px;" +
                "}" +
                "</style>" +

                "</head>" +

                "<body>" +

                "<div id='map'></div>" +

                "<script>" +

                "var map = L.map('map');" +

                "var userMarker = null;" +
                "var destinationMarker = null;" +
                "var driverMarker = null;" +

                "var defaultLat = 14.5995;" +
                "var defaultLng = 120.9842;" +

                "map.setView([" +
                "defaultLat,defaultLng" +
                "],16);" +

                "L.tileLayer(" +
                "'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'," +
                "{" +
                "maxZoom:19," +
                "attribution:'© OpenStreetMap contributors'" +
                "}" +
                ").addTo(map);" +

                "function setUser(lat,lng) {" +

                " if(userMarker !== null) {" +
                "   map.removeLayer(userMarker);" +
                " }" +

                " userMarker = L.marker([lat,lng])" +
                ".addTo(map)" +
                ".bindPopup('📍 Your current location')" +
                ".openPopup();" +

                " map.setView([lat,lng],17);" +

                "}" +

                "function setDestination(lat,lng,address) {" +

                " if(destinationMarker !== null) {" +
                "   map.removeLayer(destinationMarker);" +
                " }" +

                " destinationMarker = L.marker([lat,lng])" +
                ".addTo(map)" +
                ".bindPopup('🏁 ' + address);" +

                "}" +

                "function setDriver(lat,lng) {" +

                " if(driverMarker === null) {" +

                "   driverMarker = L.marker([lat,lng])" +
                ".addTo(map)" +
                ".bindPopup('🛺 Driver');" +

                " } else {" +

                "   driverMarker.setLatLng([lat,lng]);" +

                " }" +

                "}" +

                "map.on('click', function(e) {" +

                " if(window.Android) {" +

                "   Android.mapClicked(" +
                "e.latlng.lat,e.latlng.lng" +
                ");" +
                " }" +

                "});" +

                "window.mapReady = true;" +

                "</script>" +

                "</body>" +
                "</html>";

        webView.loadDataWithBaseURL(
                "https://sakyna.local/",
                html,
                "text/html",
                "UTF-8",
                null
        );
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

            statusText.setText(
                    "📍 Please allow location permission."
            );

            return;
        }

        startLocationUpdates();
    }

    private void startLocationUpdates() {

        if (locationManager == null) {
            statusText.setText(
                    "❌ Location service unavailable."
            );
            return;
        }

        locationListener =
                new LocationListener() {

                    @Override
                    public void onLocationChanged(
                            @NonNull Location location
                    ) {

                        currentLat =
                                location.getLatitude();

                        currentLng =
                                location.getLongitude();

                        statusText.setText(
                                String.format(
                                        Locale.US,
                                        "📍 Current location: %.6f, %.6f",
                                        currentLat,
                                        currentLng
                                )
                        );

                        updateUserMarker();

                        if ("LIVE_RIDE".equals(mode)) {
                            updateLivePassengerLocation();
                        }
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

            Location gps =
                    locationManager.getLastKnownLocation(
                            LocationManager.GPS_PROVIDER
                    );

            Location network =
                    locationManager.getLastKnownLocation(
                            LocationManager.NETWORK_PROVIDER
                    );

            Location best =
                    chooseBestLocation(
                            gps,
                            network
                    );

            if (best != null) {

                currentLat =
                        best.getLatitude();

                currentLng =
                        best.getLongitude();

                statusText.setText(
                        String.format(
                                Locale.US,
                                "📍 Current location: %.6f, %.6f",
                                currentLat,
                                currentLng
                        )
                );

                updateUserMarker();
            }

        } catch (SecurityException e) {

            statusText.setText(
                    "❌ Location permission denied."
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

    private void updateUserMarker() {

        if (!mapLoaded &&
                webView.getUrl() == null) {
            return;
        }

        if (currentLat == 0.0 ||
                currentLng == 0.0) {
            return;
        }

        webView.post(() -> {

            String javascript =
                    "if(typeof setUser === 'function')" +
                    "{setUser(" +
                    currentLat + "," +
                    currentLng +
                    ");}";

            webView.evaluateJavascript(
                    javascript,
                    null
            );

            mapLoaded = true;
        });
    }

    private void centerOnCurrentLocation() {

        if (currentLat == 0.0 ||
                currentLng == 0.0) {

            statusText.setText(
                    "📍 Still waiting for GPS..."
            );

            return;
        }

        webView.post(() -> {

            String javascript =
                    "map.setView([" +
                    currentLat + "," +
                    currentLng +
                    "],18);";

            webView.evaluateJavascript(
                    javascript,
                    null
            );

            updateUserMarker();
        });
    }

    private class MapBridge {

        @JavascriptInterface
        public void mapClicked(
                double lat,
                double lng
        ) {

            runOnUiThread(() -> {

                if (!"SELECT_DESTINATION".equals(mode)) {
                    return;
                }

                destinationLat = lat;
                destinationLng = lng;

                destinationAddress =
                        getAddress(
                                lat,
                                lng
                        );

                if (destinationAddress == null ||
                        destinationAddress.trim().isEmpty()) {

                    destinationAddress =
                            String.format(
                                    Locale.US,
                                    "%.6f, %.6f",
                                    lat,
                                    lng
                            );
                }

                statusText.setText(
                        String.format(
                                Locale.US,
                                "🏁 Destination selected: %.6f, %.6f",
                                lat,
                                lng
                        )
                );

                String safeAddress =
                        destinationAddress
                                .replace(
                                        "\\",
                                        "\\\\"
                                )
                                .replace(
                                        "'",
                                        "\\'"
                                )
                                .replace(
                                        "\n",
                                        " "
                                );

                String javascript =
                        "setDestination(" +
                        lat + "," +
                        lng + ",'" +
                        safeAddress +
                        "');";

                webView.evaluateJavascript(
                        javascript,
                        null
                );
            });
        }
    }

    private String getAddress(
            double lat,
            double lng
    ) {

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

                String line =
                        address.getAddressLine(0);

                if (line != null) {
                    return line;
                }
            }

        } catch (IOException ignored) {
        } catch (Exception ignored) {
        }

        return String.format(
                Locale.US,
                "%.6f, %.6f",
                lat,
                lng
        );
    }

    private void confirmDestination() {

        if (destinationLat == 0.0 ||
                destinationLng == 0.0) {

            statusText.setText(
                    "🏁 Tap the map to select a destination."
            );

            return;
        }

        Intent result =
                new Intent();

        result.putExtra(
                "destination_latitude",
                destinationLat
        );

        result.putExtra(
                "destination_longitude",
                destinationLng
        );

        result.putExtra(
                "destination_address",
                destinationAddress
        );

        setResult(
                RESULT_OK,
                result
        );

        finish();
    }

    private void startLiveRideListener() {

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

                                    Double driverLat =
                                            getDouble(
                                                    snapshot,
                                                    "driverLatitude"
                                            );

                                    Double driverLng =
                                            getDouble(
                                                    snapshot,
                                                    "driverLongitude"
                                            );

                                    if (driverLat != null &&
                                            driverLng != null) {

                                        showDriverLocation(
                                                driverLat,
                                                driverLng
                                        );
                                    }
                                }
                        );
    }

    private Double getDouble(
            com.google.firebase.firestore.DocumentSnapshot snapshot,
            String field
    ) {

        Object value =
                snapshot.get(field);

        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        return null;
    }

    private void showDriverLocation(
            double lat,
            double lng
    ) {

        runOnUiThread(() -> {

            String javascript =
                    "setDriver(" +
                    lat + "," +
                    lng +
                    ");";

            webView.evaluateJavascript(
                    javascript,
                    null
            );

            statusText.setText(
                    String.format(
                            Locale.US,
                            "🛺 Driver location: %.6f, %.6f",
                            lat,
                            lng
                    )
            );
        });
    }

    private void updateLivePassengerLocation() {
        // Passenger GPS is displayed locally.
        // Driver GPS will come from Firestore.
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

            if (grantResults.length > 0 &&
                    grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED) {

                startLocationUpdates();

            } else {

                statusText.setText(
                        "❌ Location permission denied."
                );
            }
        }
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        if (locationManager != null &&
                locationListener != null) {

            try {
                locationManager.removeUpdates(
                        locationListener
                );
            } catch (Exception ignored) {
            }
        }

        if (rideListener != null) {
            rideListener.remove();
            rideListener = null;
        }

        if (webView != null) {
            webView.destroy();
        }
    }
}
