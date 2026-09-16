
        
package com.sakyna.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
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

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapActivity extends Activity {

    private static final int LOCATION_PERMISSION_REQUEST = 5001;

    private WebView webView;
    private TextView addressText;
    private TextView statusText;
    private Button selectButton;
    private Button currentLocationButton;
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

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();

        readIntent();
        buildScreen();
        setupWebView();
        requestLocation();
    }

    private void readIntent() {

        Intent intent = getIntent();

        if (intent == null) {
            return;
        }

        String incomingMode = intent.getStringExtra("mode");

        if (incomingMode != null && !incomingMode.trim().isEmpty()) {
            mode = incomingMode;
        }

        rideId = intent.getStringExtra("ride_id");

        pickupLatitude =
                intent.getDoubleExtra("pickup_latitude", 0);

        pickupLongitude =
                intent.getDoubleExtra("pickup_longitude", 0);

        destinationLatitude =
                intent.getDoubleExtra("destination_latitude", 0);

        destinationLongitude =
                intent.getDoubleExtra("destination_longitude", 0);

        pickupAddress =
                safe(intent.getStringExtra("pickup_address"));

        destinationAddress =
                safe(intent.getStringExtra("destination_address"));

        destinationChosen =
                destinationLatitude != 0 &&
                destinationLongitude != 0;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void buildScreen() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);

        if ("LIVE_RIDE".equalsIgnoreCase(mode)) {
            title.setText("Sakay Na • Live Ride");
        } else {
            title.setText("Sakay Na • Ride Location");
        }

        title.setTextSize(22);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(Color.rgb(0, 120, 70));
        title.setGravity(Gravity.CENTER);
        title.setPadding(12, 16, 12, 10);

        root.addView(title);

        addressText = new TextView(this);
        addressText.setTextSize(15);
        addressText.setTextColor(Color.DKGRAY);
        addressText.setPadding(16, 6, 16, 6);
        addressText.setGravity(Gravity.CENTER);

        if ("LIVE_RIDE".equalsIgnoreCase(mode)) {
            addressText.setText("Loading live ride map...");
        } else {
            addressText.setText(
                    destinationChosen
                            ? destinationAddress
                            : "Tap the map to choose a destination."
            );
        }

        root.addView(addressText);

        statusText = new TextView(this);
        statusText.setTextSize(14);
        statusText.setTextColor(Color.GRAY);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(10, 4, 10, 8);

        root.addView(statusText);

        webView = new WebView(this);

        /*
         * IMPORTANT:
         * The WebView is NOT placed inside a ScrollView.
         * No touch listener is attached to the WebView.
         * This allows Leaflet to receive normal Android touch events:
         * drag, pinch zoom, tap and map movement.
         */
        LinearLayout.LayoutParams mapParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                );

        root.addView(webView, mapParams);

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);
        controls.setPadding(8, 8, 8, 8);

        currentLocationButton = new Button(this);
        currentLocationButton.setText("CURRENT GPS");

        currentLocationButton.setOnClickListener(
                v -> centerOnCurrentLocation()
        );

        controls.addView(
                currentLocationButton,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        if ("SELECT_DESTINATION".equalsIgnoreCase(mode)) {

            selectButton = new Button(this);
            selectButton.setText("SET DESTINATION");
            selectButton.setEnabled(false);

            selectButton.setOnClickListener(
                    v -> returnDestination()
            );

            controls.addView(
                    selectButton,
                    new LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1
                    )
            );
        }

        backButton = new Button(this);
        backButton.setText("BACK");

        backButton.setOnClickListener(
                v -> finish()
        );

        controls.addView(
                backButton,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        root.addView(controls);

        setContentView(root);
    }

    private void setupWebView() {

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setLoadWithOverviewMode(false);
        settings.setUseWideViewPort(false);
        settings.setSupportZoom(true);

        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.setClickable(true);
        webView.setLongClickable(true);
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);

        webView.setWebViewClient(new WebViewClient());

        webView.setWebChromeClient(new WebChromeClient());

        webView.addJavascriptInterface(
                new MapBridge(),
                "Android"
        );

        webView.loadDataWithBaseURL(
                "https://sakyna-map.local/",
                buildMapHtml(),
                "text/html",
                "UTF-8",
                null
        );
    }

    private String buildMapHtml() {

        String initialLat;
        String initialLng;

        if (pickupLatitude != 0 && pickupLongitude != 0) {
            initialLat = String.format(
                    Locale.US,
                    "%.7f",
                    pickupLatitude
            );

            initialLng = String.format(
                    Locale.US,
                    "%.7f",
                    pickupLongitude
            );
        } else if (
                destinationLatitude != 0 &&
                destinationLongitude != 0
        ) {
            initialLat = String.format(
                    Locale.US,
                    "%.7f",
                    destinationLatitude
            );

            initialLng = String.format(
                    Locale.US,
                    "%.7f",
                    destinationLongitude
            );
        } else {
            initialLat = "14.000000";
            initialLng = "121.000000";
        }

        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +

                "<meta name='viewport' " +
                "content='width=device-width, " +
                "initial-scale=1.0, " +
                "maximum-scale=1.0, " +
                "user-scalable=yes'>" +

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
                "overflow:hidden;" +
                "touch-action:none;" +
                "}" +

                ".leaflet-control-attribution{" +
                "font-size:9px;" +
                "}" +

                "</style>" +

                "</head>" +

                "<body>" +

                "<div id='map'></div>" +

                "<script>" +

                "var map=L.map('map'," +
                "{zoomControl:true," +
                "touchZoom:true," +
                "scrollWheelZoom:true," +
                "doubleClickZoom:true," +
                "dragging:true," +
                "boxZoom:true," +
                "keyboard:true});" +

                "var userMarker=null;" +
                "var destinationMarker=null;" +
                "var driverMarker=null;" +

                "map.setView([" +
                initialLat +
                "," +
                initialLng +
                "],15);" +

                "L.tileLayer(" +
                "'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'," +
                "{maxZoom:19," +
                "attribution:'&copy; OpenStreetMap contributors'}" +
                ").addTo(map);" +

                "function updateUser(lat,lng,center){" +

                "if(userMarker===null){" +

                "userMarker=L.marker([lat,lng])" +
                ".addTo(map)" +
                ".bindPopup('You are here');" +

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

                "destinationMarker=" +
                "L.marker([lat,lng])" +
                ".addTo(map)" +
                ".bindPopup(address || 'Destination')" +
                ".openPopup();" +

                "map.setView([lat,lng],17);" +

                "}" +

                "function setDriver(lat,lng){" +

                "if(driverMarker===null){" +

                "driverMarker=" +
                "L.marker([lat,lng])" +
                ".addTo(map)" +
                ".bindPopup('Driver');" +

                "}else{" +

                "driverMarker.setLatLng([lat,lng]);" +

                "}" +

                "}" +

                "map.on('click',function(e){" +

                "if(window.Android){" +

                "Android.mapClicked(" +
                "e.latlng.lat," +
                "e.latlng.lng" +
                ");" +

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
    }

    private void requestLocation() {

        if (
                checkSelfPermission(
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
                &&
                checkSelfPermission(
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
        ) {

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

        locationManager =
                (LocationManager)
                        getSystemService(LOCATION_SERVICE);

        if (locationManager == null) {
            return;
        }

        locationListener =
                new LocationListener() {

                    @Override
                    public void onLocationChanged(
                            @NonNull Location location
                    ) {

                        pickupLatitude =
                                location.getLatitude();

                        pickupLongitude =
                                location.getLongitude();

                        if (mapReady) {
                            sendUserLocationToMap(
                                    pickupLatitude,
                                    pickupLongitude,
                                    false
                            );
                        }

                        if (
                                "SELECT_DESTINATION"
                                        .equalsIgnoreCase(mode)
                                &&
                                pickupAddress.isEmpty()
                        ) {

                            reverseGeocodePickup(
                                    pickupLatitude,
                                    pickupLongitude
                            );
                        }
                    }
                };

        try {

            if (
                    checkSelfPermission(
                            Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    ||
                    checkSelfPermission(
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
            ) {

                if (
                        locationManager.isProviderEnabled(
                                LocationManager.GPS_PROVIDER
                        )
                ) {

                    locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            3000,
                            5,
                            locationListener
                    );
                }

                if (
                        locationManager.isProviderEnabled(
                                LocationManager.NETWORK_PROVIDER
                        )
                ) {

                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            3000,
                            5,
                            locationListener
                    );
                }
            }

        } catch (SecurityException ignored) {
        }

        if ("LIVE_RIDE".equalsIgnoreCase(mode)) {
            startLiveRideListener();
        }
    }

    private void sendUserLocationToMap(
            double lat,
            double lng,
            boolean center
    ) {

        if (webView == null) {
            return;
        }

        String js =
                "updateUser(" +
                lat +
                "," +
                lng +
                "," +
                center +
                ");";

        webView.post(
                () -> webView.evaluateJavascript(
                        js,
                        null
                )
        );
    }

    private void centerOnCurrentLocation() {

        if (pickupLatitude == 0 ||
                pickupLongitude == 0) {

            Toast.makeText(
                    this,
                    "Waiting for your current location...",
                    Toast.LENGTH_SHORT
            ).show();

            requestLocation();
            return;
        }

        sendUserLocationToMap(
                pickupLatitude,
                pickupLongitude,
                true
        );
    }

    private void reverseGeocodePickup(
            double lat,
            double lng
    ) {

        new Thread(() -> {

            String address =
                    getAddress(lat, lng);

            mainHandler.post(() -> {

                if (!address.isEmpty()) {

                    pickupAddress = address;

                    if (
                            "SELECT_DESTINATION"
                                    .equalsIgnoreCase(mode)
                            &&
                            addressText != null
                    ) {

                        addressText.setText(
                                "Pickup: " + address
                        );
                    }
                }
            });

        }).start();
    }

    private void reverseGeocodeDestination(
            double lat,
            double lng
    ) {

        if (statusText != null) {
            statusText.setText(
                    "Getting destination address..."
            );
        }

        new Thread(() -> {

            String address =
                    getAddress(lat, lng);

            mainHandler.post(() -> {

                destinationLatitude = lat;
                destinationLongitude = lng;

                destinationAddress =
                        address.isEmpty()
                                ? String.format(
                                Locale.US,
                                "%.6f, %.6f",
                                lat,
                                lng
                        )
                                : address;

                destinationChosen = true;

                if (addressText != null) {

                    addressText.setText(
                            "Destination: " +
                            destinationAddress
                    );
                }

                if (statusText != null) {

                    statusText.setText(
                            "Destination selected."
                    );
                }

                if (selectButton != null) {
                    selectButton.setEnabled(true);
                }

                setDestinationOnMap(
                        lat,
                        lng,
                        destinationAddress
                );
            });

        }).start();
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

            if (
                    addresses != null &&
                    !addresses.isEmpty()
            ) {

                Address a =
                        addresses.get(0);

                String line =
                        a.getAddressLine(0);

                if (
                        line != null &&
                        !line.trim().isEmpty()
                ) {

                    return line;
                }
            }

        } catch (IOException ignored) {
        } catch (Exception ignored) {
        }

        return "";
    }

    private void setDestinationOnMap(
            double lat,
            double lng,
            String address
    ) {

        if (webView == null) {
            return;
        }

        String safeAddress =
                address
                        .replace("\\", "\\\\")
                        .replace("'", "\\'")
                        .replace("\n", " ")
                        .replace("\r", " ");

        String js =
                "setDestination(" +
                lat +
                "," +
                lng +
                ",'" +
                safeAddress +
                "');";

        webView.post(
                () -> webView.evaluateJavascript(
                        js,
                        null
                )
        );
    }

    private void returnDestination() {

        if (!destinationChosen ||
                destinationLatitude == 0 ||
                destinationLongitude == 0) {

            Toast.makeText(
                    this,
                    "Please choose your destination on the map.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Intent result = new Intent();

        result.putExtra(
                "destination_latitude",
                destinationLatitude
        );

        result.putExtra(
                "destination_longitude",
                destinationLongitude
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

        if (
                rideId == null ||
                rideId.trim().isEmpty()
        ) {
            return;
        }

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

                                    Double pLat =
                                            snapshot.getDouble(
                                                    "pickupLatitude"
                                            );

                                    Double pLng =
                                            snapshot.getDouble(
                                                    "pickupLongitude"
                                            );

                                    Double dLat =
                                            snapshot.getDouble(
                                                    "destinationLatitude"
                                            );

                                    Double dLng =
                                            snapshot.getDouble(
                                                    "destinationLongitude"
                                            );

                                    if (pLat != null &&
                                            pLng != null) {

                                        pickupLatitude = pLat;
                                        pickupLongitude = pLng;

                                        sendUserLocationToMap(
                                                pLat,
                                                pLng,
                                                false
                                        );
                                    }

                                    if (dLat != null &&
                                            dLng != null) {

                                        destinationLatitude = dLat;
                                        destinationLongitude = dLng;

                                        setDestinationOnMap(
                                                dLat,
                                                dLng,
                                                safe(
                                                        snapshot.getString(
                                                                "destination"
                                                        )
                                                )
                                        );
                                    }

                                    String status =
                                            snapshot.getString(
                                                    "status"
                                            );

                                    if (status != null &&
                                            statusText != null) {

                                        statusText.setText(
                                                "Ride status: " +
                                                status
                                        );
                                    }

                                    String driverId =
                                            snapshot.getString(
                                                    "driverId"
                                            );

                                    if (
                                            driverId != null &&
                                            !driverId.trim().isEmpty()
                                    ) {

                                        listenForDriver(
                                                driverId
                                        );
                                    }
                                }
                        );
    }

    private void listenForDriver(
            String driverId
    ) {

        if (driverListener != null) {
            driverListener.remove();
        }

        driverListener =
                db.collection("driverLocations")
                        .document(driverId)
                        .addSnapshotListener(
                                (snapshot, error) -> {

                                    if (
                                            error != null ||
                                            snapshot == null ||
                                            !snapshot.exists()
                                    ) {
                                        return;
                                    }

                                    Double lat =
                                            snapshot.getDouble(
                                                    "latitude"
                                            );

                                    Double lng =
                                            snapshot.getDouble(
                                                    "longitude"
                                            );

                                    if (
                                            lat == null ||
                                            lng == null
                                    ) {
                                        return;
                                    }

                                    driverLatitude = lat;
                                    driverLongitude = lng;

                                    setDriverOnMap(
                                            lat,
                                            lng
                                    );
                                }
                        );
    }

    private void setDriverOnMap(
            double lat,
            double lng
    ) {

        if (webView == null) {
            return;
        }

        String js =
                "setDriver(" +
                lat +
                "," +
                lng +
                ");";

        webView.post(
                () -> webView.evaluateJavascript(
                        js,
                        null
                )
        );
    }

    private void mapReady() {

        mapReady = true;

        if (pickupLatitude != 0 &&
                pickupLongitude != 0) {

            sendUserLocationToMap(
                    pickupLatitude,
                    pickupLongitude,
                    true
            );
        }

        if (destinationChosen) {

            setDestinationOnMap(
                    destinationLatitude,
                    destinationLongitude,
                    destinationAddress
            );
        }

        if (driverLatitude != 0 &&
                driverLongitude != 0) {

            setDriverOnMap(
                    driverLatitude,
                    driverLongitude
            );
        }

        if (statusText != null) {

            if (
                    "SELECT_DESTINATION"
                            .equalsIgnoreCase(mode)
            ) {

                statusText.setText(
                        "Tap the map to choose a destination."
                );

            } else {

                statusText.setText(
                        "Live map ready."
                );
            }
        }
    }

    private void mapClicked(
            double lat,
            double lng
    ) {

        if (
                !"SELECT_DESTINATION"
                        .equalsIgnoreCase(mode)
        ) {
            return;
        }

        reverseGeocodeDestination(
                lat,
                lng
        );
    }

    public class MapBridge {

        @JavascriptInterface
        public void mapReady() {

            runOnUiThread(
                    () -> MapActivity.this.mapReady()
            );
        }

        @JavascriptInterface
        public void mapClicked(
                double lat,
                double lng
        ) {

            runOnUiThread(
                    () -> MapActivity.this.mapClicked(
                            lat,
                            lng
                    )
            );
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

        if (
                requestCode ==
                        LOCATION_PERMISSION_REQUEST
                &&
                grantResults.length > 0
                &&
                grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED
        ) {

            startLocationUpdates();
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

        if (
                locationManager != null &&
                locationListener != null
        ) {

            try {

                locationManager.removeUpdates(
                        locationListener
                );

            } catch (SecurityException ignored) {
            }
        }

        if (webView != null) {

            webView.stopLoading();
            webView.removeJavascriptInterface(
                    "Android"
            );
            webView.destroy();
            webView = null;
        }

        super.onDestroy();
    }
}
