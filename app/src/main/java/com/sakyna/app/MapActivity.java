
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
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentSnapshot;
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

    private double pickupLat = 0.0;
    private double pickupLng = 0.0;

    private String pickupAddress = "";
    private String destinationAddress = "";

    private String mode = "SELECT_DESTINATION";
    private String rideId = "";

    private boolean firstLocationReceived = false;
    private boolean mapReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();

        Intent intent = getIntent();

        if (intent != null) {

            String receivedMode =
                    intent.getStringExtra("mode");

            if (receivedMode != null &&
                    !receivedMode.isEmpty()) {

                mode = receivedMode;
            }

            rideId =
                    intent.getStringExtra(
                            "ride_id"
                    );

            if (rideId == null) {
                rideId = "";
            }

            if (!rideId.isEmpty()) {
                mode = "LIVE_RIDE";
            }

            pickupLat =
                    intent.getDoubleExtra(
                            "pickup_latitude",
                            0.0
                    );

            pickupLng =
                    intent.getDoubleExtra(
                            "pickup_longitude",
                            0.0
                    );

            pickupAddress =
                    intent.getStringExtra(
                            "pickup_address"
                    );

            if (pickupAddress == null) {
                pickupAddress = "";
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

            destinationAddress =
                    intent.getStringExtra(
                            "destination_address"
                    );

            if (destinationAddress == null) {
                destinationAddress = "";
            }
        }

        buildScreen();
        setupLocation();

        if ("LIVE_RIDE".equals(mode) &&
                !rideId.isEmpty()) {

            startLiveRideListener();
        }
    }

    private void buildScreen() {

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        TextView title =
                new TextView(this);

        title.setText(
                "LIVE_RIDE".equals(mode)
                        ? "🗺️ LIVE RIDE MAP"
                        : "📍 CHOOSE DESTINATION"
        );

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
                "📍 Finding exact location..."
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

        webView =
                new WebView(this);

        WebSettings settings =
                webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
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

            Button locateButton =
                    new Button(this);

            locateButton.setText(
                    "📍 CENTER ON MY LOCATION"
            );

            locateButton.setOnClickListener(
                    v -> centerOnCurrentLocation()
            );

            root.addView(
                    locateButton,
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

        backButton.setText(
                "⬅️ BACK TO PASSENGER"
        );

        backButton.setOnClickListener(
                v -> returnToPassenger()
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
                "<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=yes'>" +
                "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'>" +
                "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>" +
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

                "var map=L.map('map',{zoomControl:true});" +

                "var userMarker=null;" +
                "var destinationMarker=null;" +
                "var driverMarker=null;" +

                "map.setView([14.0,121.0],15);" +

                "L.tileLayer(" +
                "'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'," +
                "{maxZoom:19,attribution:'© OpenStreetMap contributors'}" +
                ").addTo(map);" +

                "function updateUser(lat,lng,center){" +

                "if(userMarker===null){" +

                "userMarker=L.marker([lat,lng])" +
                ".addTo(map)" +
                ".bindPopup('📍 You are here');" +

                "}else{" +

                "userMarker.setLatLng([lat,lng]);" +

                "}" +

                "if(center===true){" +
                "map.setView([lat,lng],17);" +
                "}" +

                "}" +

                "function setDestination(lat,lng,address){" +

                "if(destinationMarker!==null){" +
                "map.removeLayer(destinationMarker);" +
                "}" +

                "destinationMarker=L.marker([lat,lng])" +
                ".addTo(map)" +
                ".bindPopup('🏁 '+address)" +
                ".openPopup();" +

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

                "if(window.Android){" +
                "Android.mapClicked(e.latlng.lat,e.latlng.lng);" +
                "}" +

                "});" +

                "map.whenReady(function(){" +
                "if(window.Android){" +
                "Android.mapReady();" +
                "}" +
                "});" +

                "</script>" +
                "</body>" +
                "</html>";

        webView.loadDataWithBaseURL(
                "https://sakyna-map.local/",
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

            return;
        }

        startLocationUpdates();
    }

    private void startLocationUpdates() {

        if (locationManager == null) {
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

                        boolean center =
                                !firstLocationReceived;

                        firstLocationReceived = true;

                        statusText.setText(
                                String.format(
                                        Locale.US,
                                        "📍 Your location: %.6f, %.6f",
                                        currentLat,
                                        currentLng
                                )
                        );

                        updateUserMarker(center);

                        if ("SELECT_DESTINATION".equals(
                                mode
                        ) &&
                                pickupAddress.isEmpty()) {

                            reverseGeocodePickup();
                        }
                    }
                };

        try {

            if (locationManager.isProviderEnabled(
                    LocationManager.GPS_PROVIDER
            )) {

                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        1500,
                        2,
                        locationListener
                );
            }

            if (locationManager.isProviderEnabled(
                    LocationManager.NETWORK_PROVIDER
            )) {

                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        2500,
                        5,
                        locationListener
                );
            }

            Location gps = null;
            Location network = null;

            if (locationManager.isProviderEnabled(
                    LocationManager.GPS_PROVIDER
            )) {

                gps =
                        locationManager.getLastKnownLocation(
                                LocationManager.GPS_PROVIDER
                        );
            }

            if (locationManager.isProviderEnabled(
                    LocationManager.NETWORK_PROVIDER
            )) {

                network =
                        locationManager.getLastKnownLocation(
                                LocationManager.NETWORK_PROVIDER
                        );
            }

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

                updateUserMarker(
                        !firstLocationReceived
                );

                firstLocationReceived = true;

                reverseGeocodePickup();
            }

        } catch (SecurityException ignored) {
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

    private void updateUserMarker(
            boolean center
    ) {

        if (!mapReady) {
            return;
        }

        if (currentLat == 0.0 ||
                currentLng == 0.0) {
            return;
        }

        String javascript =
                "updateUser(" +
                        currentLat +
                        "," +
                        currentLng +
                        "," +
                        center +
                        ");";

        webView.post(() ->
                webView.evaluateJavascript(
                        javascript,
                        null
                )
        );
    }

    private void centerOnCurrentLocation() {

        if (currentLat == 0.0 ||
                currentLng == 0.0) {

            statusText.setText(
                    "📍 Waiting for GPS..."
            );

            return;
        }

        String javascript =
                "updateUser(" +
                        currentLat +
                        "," +
                        currentLng +
                        ",true);";

        webView.evaluateJavascript(
                javascript,
                null
        );
    }

    private void reverseGeocodePickup() {

        if (currentLat == 0.0 ||
                currentLng == 0.0) {
            return;
        }

        final double lat = currentLat;
        final double lng = currentLng;

        new Thread(() -> {

            String addressText = "";

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

                    addressText =
                            address.getAddressLine(0);

                    if (addressText == null ||
                            addressText.trim().isEmpty()) {

                        addressText =
                                buildAddress(
                                        address
                                );
                    }
                }

            } catch (Exception ignored) {
            }

            final String result =
                    addressText;

            runOnUiThread(() -> {

                if (!result.isEmpty()) {

                    pickupAddress = result;

                    statusText.setText(
                            "📍 Pickup: " +
                                    pickupAddress
                    );
                }
            });

        }).start();
    }

    private String buildAddress(
            Address address
    ) {

        StringBuilder result =
                new StringBuilder();

        for (int i = 0;
             i <= address.getMaxAddressLineIndex();
             i++) {

            String line =
                    address.getAddressLine(i);

            if (line != null &&
                    !line.trim().isEmpty()) {

                if (result.length() > 0) {
                    result.append(", ");
                }

                result.append(line);
            }
        }

        return result.toString();
    }

    private class MapBridge {

        @JavascriptInterface
        public void mapReady() {

            runOnUiThread(() -> {

                mapReady = true;

                if (currentLat != 0.0 &&
                        currentLng != 0.0) {

                    updateUserMarker(
                            true
                    );
                }

                if (destinationLat != 0.0 &&
                        destinationLng != 0.0) {

                    updateDestinationMarker();
                }
            });
        }

        @JavascriptInterface
        public void mapClicked(
                double lat,
                double lng
        ) {

            runOnUiThread(() -> {

                if (!"SELECT_DESTINATION".equals(
                        mode
                )) {
                    return;
                }

                destinationLat = lat;
                destinationLng = lng;

                statusText.setText(
                        String.format(
                                Locale.US,
                                "🏁 Getting destination address..."
                        )
                );

                reverseGeocodeDestination(
                        lat,
                        lng
                );
            });
        }
    }

    private void reverseGeocodeDestination(
            double lat,
            double lng
    ) {

        new Thread(() -> {

            String addressText = "";

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

                    addressText =
                            address.getAddressLine(0);

                    if (addressText == null ||
                            addressText.trim().isEmpty()) {

                        addressText =
                                buildAddress(
                                        address
                                );
                    }
                }

            } catch (IOException ignored) {
            } catch (Exception ignored) {
            }

            final String result =
                    addressText;

            runOnUiThread(() -> {

                if (!result.isEmpty()) {

                    destinationAddress =
                            result;

                } else {

                    destinationAddress =
                            String.format(
                                    Locale.US,
                                    "GPS %.6f, %.6f",
                                    lat,
                                    lng
                            );
                }

                updateDestinationMarker();

                statusText.setText(
                        "🏁 " +
                                destinationAddress
                );
            });

        }).start();
    }

    private void updateDestinationMarker() {

        if (!mapReady ||
                destinationLat == 0.0 ||
                destinationLng == 0.0) {
            return;
        }

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
                        destinationLat +
                        "," +
                        destinationLng +
                        ",'" +
                        safeAddress +
                        "');";

        webView.evaluateJavascript(
                javascript,
                null
        );
    }

    private void confirmDestination() {

        if (destinationLat == 0.0 ||
                destinationLng == 0.0) {

            statusText.setText(
                    "🏁 Tap the map to choose a destination."
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

        result.putExtra(
                "pickup_latitude",
                currentLat
        );

        result.putExtra(
                "pickup_longitude",
                currentLng
        );

        result.putExtra(
                "pickup_address",
                pickupAddress
        );

        setResult(
                RESULT_OK,
                result
        );

        finish();
    }

    private void startLiveRideListener() {

        if (rideId.isEmpty()) {
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

                                        showDriver(
                                                driverLat,
                                                driverLng
                                        );
                                    }
                                }
                        );
    }

    private Double getDouble(
            DocumentSnapshot snapshot,
            String field
    ) {

        Object value =
                snapshot.get(field);

        if (value instanceof Number) {
            return (
                    (Number) value
            ).doubleValue();
        }

        return null;
    }

    private void showDriver(
            double lat,
            double lng
    ) {

        if (!mapReady) {
            return;
        }

        String javascript =
                "setDriver(" +
                        lat +
                        "," +
                        lng +
                        ");";

        webView.post(() ->
                webView.evaluateJavascript(
                        javascript,
                        null
                )
        );

        statusText.setText(
                String.format(
                        Locale.US,
                        "🛺 Driver location: %.6f, %.6f",
                        lat,
                        lng
                )
        );
    }

    private void returnToPassenger() {

        Intent intent =
                new Intent(
                        this,
                        PassengerActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
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
                statusText.setText(
                        "❌ Location permission denied."
                );
            }
        }
    }

    @Override
    protected void onDestroy() {

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

        super.onDestroy();
    }
}
