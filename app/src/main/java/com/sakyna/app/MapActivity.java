
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
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MapActivity extends Activity {

    private WebView webView;
    private EditText searchInput;
    private TextView statusText;

    private LocationManager locationManager;
    private LocationListener locationListener;

    private FirebaseFirestore db;
    private ListenerRegistration rideListener;

    private String rideId = null;
    private String mode = "SELECT_DESTINATION";

    private boolean mapReady = false;
    private boolean destinationSelected = false;

    private double selectedLat = 0.0;
    private double selectedLng = 0.0;
    private String selectedAddress = "";

    private double currentLat = 0.0;
    private double currentLng = 0.0;

    private static final int LOCATION_PERMISSION = 3001;

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();

        Intent intent = getIntent();

        if (intent != null) {

            String incomingRideId =
                    intent.getStringExtra("ride_id");

            if (incomingRideId == null || incomingRideId.isEmpty()) {
                incomingRideId =
                        intent.getStringExtra("rideId");
            }

            if (incomingRideId != null
                    && !incomingRideId.isEmpty()) {

                rideId = incomingRideId;
                mode = "LIVE_RIDE";
            }

            String incomingMode =
                    intent.getStringExtra("mode");

            if (incomingMode != null
                    && !incomingMode.isEmpty()) {

                mode = incomingMode;
            }
        }

        buildScreen();
        setupWebView();
        startLocationUpdates();

        if ("LIVE_RIDE".equalsIgnoreCase(mode)
                && rideId != null
                && !rideId.isEmpty()) {

            listenForRide();
        }
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

        if ("LIVE_RIDE".equalsIgnoreCase(mode)) {
            title.setText("🚕 LIVE RIDE MAP");
        } else {
            title.setText("🗺️ CHOOSE DESTINATION");
        }

        title.setTextSize(22);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        title.setPadding(10, 25, 10, 25);
        title.setBackgroundColor(
                Color.rgb(0, 150, 80)
        );

        root.addView(title);

        LinearLayout searchRow =
                new LinearLayout(this);

        searchRow.setOrientation(
                LinearLayout.HORIZONTAL
        );

        searchInput =
                new EditText(this);

        searchInput.setHint(
                "Search place, street, barangay..."
        );

        searchInput.setTextSize(16);
        searchInput.setSingleLine(true);

        Button searchButton =
                new Button(this);

        searchButton.setText("🔎 SEARCH");

        searchButton.setOnClickListener(
                v -> searchPlace()
        );

        searchRow.addView(
                searchInput,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        searchRow.addView(
                searchButton,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        root.addView(searchRow);

        statusText =
                new TextView(this);

        statusText.setText(
                "Loading map..."
        );

        statusText.setTextSize(15);
        statusText.setTextColor(Color.DKGRAY);
        statusText.setPadding(15, 10, 15, 10);

        root.addView(statusText);

        webView =
                new WebView(this);

        root.addView(
                webView,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        Button locationButton =
                new Button(this);

        locationButton.setText(
                "📍 CENTER ON MY LOCATION"
        );

        locationButton.setOnClickListener(
                v -> sendCurrentLocationToMap()
        );

        root.addView(locationButton);

        if (!"LIVE_RIDE".equalsIgnoreCase(mode)) {

            Button confirmButton =
                    new Button(this);

            confirmButton.setText(
                    "✅ CONFIRM DESTINATION"
            );

            confirmButton.setOnClickListener(
                    v -> confirmDestination()
            );

            root.addView(confirmButton);
        }

        Button backButton =
                new Button(this);

        backButton.setText(
                "⬅️ BACK"
        );

        backButton.setOnClickListener(
                v -> finish()
        );

        root.addView(backButton);

        setContentView(root);
    }

    private void setupWebView() {

        WebSettings settings =
                webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        // Important for Android WebView location support.
        settings.setGeolocationEnabled(true);

        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        /*
         * Prevent WebView from stealing focus
         * from the native search box.
         */
        webView.setFocusable(false);
        webView.setFocusableInTouchMode(false);

        webView.setBackgroundColor(Color.WHITE);

        webView.setWebViewClient(
                new WebViewClient() {

                    @Override
                    public void onPageFinished(
                            WebView view,
                            String url) {

                        super.onPageFinished(
                                view,
                                url
                        );

                        mapReady = true;

                        statusText.setText(
                                "🟢 MAP READY"
                        );

                        sendCurrentLocationToMap();

                        /*
                         * Leaflet sometimes calculates the
                         * WebView size before Android has
                         * completed the layout.
                         *
                         * Recalculate once after layout.
                         */
                        handler.postDelayed(
                                () -> {

                                    if (webView != null) {

                                        webView.evaluateJavascript(
                                                "if(window.map){map.invalidateSize();}",
                                                null
                                        );

                                        sendCurrentLocationToMap();
                                    }

                                },
                                500
                        );
                    }
                }
        );

        webView.addJavascriptInterface(
                new MapBridge(),
                "AndroidMap"
        );

        loadMap();
    }

    private void loadMap() {

        String html =
                "<!DOCTYPE html>" +
                "<html>" +

                "<head>" +

                "<meta name='viewport' " +
                "content='width=device-width, " +
                "initial-scale=1.0, " +
                "maximum-scale=1.0, " +
                "user-scalable=yes'>" +

                "<link rel='stylesheet' " +
                "href='https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.css'>" +

                "<script src='https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.js'>" +
                "</script>" +

                "<style>" +

                "html,body,#map{" +
                "height:100%;" +
                "width:100%;" +
                "margin:0;" +
                "padding:0;" +
                "}" +

                "</style>" +

                "</head>" +

                "<body>" +

                "<div id='map'></div>" +

                "<script>" +

                "var map=L.map('map').setView([14.5995,120.9842],12);" +

                "L.tileLayer(" +
                "'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'," +
                "{" +
                "maxZoom:19," +
                "attribution:'© OpenStreetMap contributors'" +
                "}" +
                ").addTo(map);" +

                "var gpsMarker=null;" +
                "var destinationMarker=null;" +
                "var driverMarker=null;" +

                "function setGPS(lat,lng){" +

                "if(gpsMarker==null){" +

                "gpsMarker=L.marker([lat,lng]).addTo(map);" +

                "}else{" +

                "gpsMarker.setLatLng([lat,lng]);" +

                "}" +

                "gpsMarker.bindPopup('📍 Your GPS location');" +

                "map.setView([lat,lng],17);" +

                "}" +

                "function setDestination(lat,lng,name){" +

                "if(destinationMarker==null){" +

                "destinationMarker=L.marker([lat,lng]).addTo(map);" +

                "}else{" +

                "destinationMarker.setLatLng([lat,lng]);" +

                "}" +

                "destinationMarker" +
                ".bindPopup(name||'Destination')" +
                ".openPopup();" +

                "map.setView([lat,lng],17);" +

                "}" +

                "function setDriver(lat,lng){" +

                "if(driverMarker==null){" +

                "driverMarker=L.marker([lat,lng]).addTo(map);" +

                "}else{" +

                "driverMarker.setLatLng([lat,lng]);" +

                "}" +

                "driverMarker.bindPopup('🚕 Driver');" +

                "}" +

                "map.on('click',function(e){" +

                "AndroidMap.tap(" +
                "e.latlng.lat," +
                "e.latlng.lng" +
                ");" +

                "});" +

                "</script>" +

                "</body>" +

                "</html>";

        /*
         * A real HTTPS base URL is used so WebView has
         * a normal origin when resolving remote resources.
         */
        webView.loadDataWithBaseURL(
                "https://www.openstreetmap.org/",
                html,
                "text/html",
                "UTF-8",
                null
        );
    }

    private void startLocationUpdates() {

        locationManager =
                (LocationManager)
                        getSystemService(
                                LOCATION_SERVICE
                        );

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION
            );

            return;
        }

        beginLocationUpdates();
    }

    private void beginLocationUpdates() {

        if (locationManager == null) {
            return;
        }

        locationListener =
                new LocationListener() {

                    @Override
                    public void onLocationChanged(
                            @NonNull Location location) {

                        currentLat =
                                location.getLatitude();

                        currentLng =
                                location.getLongitude();

                        sendCurrentLocationToMap();
                    }
                };

        try {

            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    3000,
                    5,
                    locationListener,
                    Looper.getMainLooper()
            );

            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    3000,
                    5,
                    locationListener,
                    Looper.getMainLooper()
            );

        } catch (SecurityException ignored) {
        }
    }

    private void sendCurrentLocationToMap() {

        if (!mapReady) {
            return;
        }

        if (currentLat == 0.0
                && currentLng == 0.0) {
            return;
        }

        final double lat = currentLat;
        final double lng = currentLng;

        runOnUiThread(
                () -> {

                    if (webView == null) {
                        return;
                    }

                    String javascript =
                            "if(window.setGPS){" +
                            "setGPS(" +
                            lat +
                            "," +
                            lng +
                            ");" +
                            "}";

                    webView.evaluateJavascript(
                            javascript,
                            null
                    );
                }
        );
    }

    private void searchPlace() {

        String query =
                searchInput.getText()
                        .toString()
                        .trim();

        if (query.isEmpty()) {

            Toast.makeText(
                    this,
                    "Enter a place to search.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        statusText.setText(
                "🔎 Searching..."
        );

        new Thread(
                () -> {

                    boolean found =
                            searchWithGeocoder(query);

                    if (!found) {
                        found =
                                searchWithPhoton(query);
                    }

                    if (!found) {
                        found =
                                searchWithNominatim(query);
                    }

                    if (!found) {

                        runOnUiThread(
                                () -> statusText.setText(
                                        "❌ Place not found."
                                )
                        );
                    }
                }
        ).start();
    }

    private boolean searchWithGeocoder(
            String query) {

        try {

            Geocoder geocoder =
                    new Geocoder(
                            this,
                            Locale.getDefault()
                    );

            List<Address> results =
                    geocoder.getFromLocationName(
                            query,
                            1
                    );

            if (results != null
                    && !results.isEmpty()) {

                Address address =
                        results.get(0);

                double lat =
                        address.getLatitude();

                double lng =
                        address.getLongitude();

                String name =
                        address.getAddressLine(0);

                showSearchResult(
                        lat,
                        lng,
                        name
                );

                return true;
            }

        } catch (Exception ignored) {
        }

        return false;
    }

    private boolean searchWithPhoton(
            String query) {

        HttpURLConnection connection =
                null;

        try {

            String encoded =
                    URLEncoder.encode(
                            query,
                            "UTF-8"
                    );

            URL url =
                    new URL(
                            "https://photon.komoot.io/api/?q="
                                    + encoded
                                    + "&limit=1"
                    );

            connection =
                    (HttpURLConnection)
                            url.openConnection();

            connection.setConnectTimeout(
                    10000
            );

            connection.setReadTimeout(
                    10000
            );

            connection.setRequestMethod(
                    "GET"
            );

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    connection.getInputStream()
                            )
                    );

            StringBuilder response =
                    new StringBuilder();

            String line;

            while ((line = reader.readLine())
                    != null) {

                response.append(line);
            }

            reader.close();

            JSONObject root =
                    new JSONObject(
                            response.toString()
                    );

            JSONArray features =
                    root.getJSONArray(
                            "features"
                    );

            if (features.length() == 0) {
                return false;
            }

            JSONObject feature =
                    features.getJSONObject(0);

            JSONObject geometry =
                    feature.getJSONObject(
                            "geometry"
                    );

            JSONArray coordinates =
                    geometry.getJSONArray(
                            "coordinates"
                    );

            double lng =
                    coordinates.getDouble(0);

            double lat =
                    coordinates.getDouble(1);

            JSONObject properties =
                    feature.optJSONObject(
                            "properties"
                    );

            String name =
                    query;

            if (properties != null) {

                String label =
                        properties.optString(
                                "name",
                                ""
                        );

                String city =
                        properties.optString(
                                "city",
                                ""
                        );

                String country =
                        properties.optString(
                                "country",
                                ""
                        );

                StringBuilder text =
                        new StringBuilder();

                if (!label.isEmpty()) {
                    text.append(label);
                }

                if (!city.isEmpty()) {

                    if (text.length() > 0) {
                        text.append(", ");
                    }

                    text.append(city);
                }

                if (!country.isEmpty()) {

                    if (text.length() > 0) {
                        text.append(", ");
                    }

                    text.append(country);
                }

                if (text.length() > 0) {
                    name = text.toString();
                }
            }

            showSearchResult(
                    lat,
                    lng,
                    name
            );

            return true;

        } catch (Exception ignored) {

            return false;

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private boolean searchWithNominatim(
            String query) {

        HttpURLConnection connection =
                null;

        try {

            String encoded =
                    URLEncoder.encode(
                            query,
                            "UTF-8"
                    );

            URL url =
                    new URL(
                            "https://nominatim.openstreetmap.org/search"
                                    + "?format=json"
                                    + "&limit=1"
                                    + "&q="
                                    + encoded
                    );

            connection =
                    (HttpURLConnection)
                            url.openConnection();

            connection.setConnectTimeout(
                    10000
            );

            connection.setReadTimeout(
                    10000
            );

            connection.setRequestProperty(
                    "User-Agent",
                    "SakayNa/1.0 Android"
            );

            connection.setRequestMethod(
                    "GET"
            );

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    connection.getInputStream()
                            )
                    );

            StringBuilder response =
                    new StringBuilder();

            String line;

            while ((line = reader.readLine())
                    != null) {

                response.append(line);
            }

            reader.close();

            JSONArray results =
                    new JSONArray(
                            response.toString()
                    );

            if (results.length() == 0) {
                return false;
            }

            JSONObject result =
                    results.getJSONObject(0);

            double lat =
                    Double.parseDouble(
                            result.getString("lat")
                    );

            double lng =
                    Double.parseDouble(
                            result.getString("lon")
                    );

            String name =
                    result.optString(
                            "display_name",
                            query
                    );

            showSearchResult(
                    lat,
                    lng,
                    name
            );

            return true;

        } catch (Exception ignored) {

            return false;

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void showSearchResult(
            double lat,
            double lng,
            String name) {

        runOnUiThread(
                () -> {

                    selectedLat = lat;
                    selectedLng = lng;

                    selectedAddress =
                            name == null
                                    ? ""
                                    : name;

                    destinationSelected =
                            true;

                    if (statusText != null) {

                        statusText.setText(
                                "📍 "
                                        + selectedAddress
                        );
                    }

                    if (webView != null) {

                        String safeName =
                                JSONObject
                                        .quote(
                                                selectedAddress
                                        );

                        String javascript =
                                "if(window.setDestination){" +
                                "setDestination(" +
                                lat +
                                "," +
                                lng +
                                "," +
                                safeName +
                                ");" +
                                "}";

                        webView.evaluateJavascript(
                                javascript,
                                null
                        );
                    }
                }
        );
    }

    private void confirmDestination() {

        if (!destinationSelected) {

            Toast.makeText(
                    this,
                    "Search for a destination or tap the map first.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Intent result =
                new Intent();

        result.putExtra(
                "destination_latitude",
                selectedLat
        );

        result.putExtra(
                "destination_longitude",
                selectedLng
        );

        result.putExtra(
                "destinationName",
                selectedAddress
        );

        result.putExtra(
                "destination_address",
                selectedAddress
        );

        result.putExtra(
                "latitude",
                selectedLat
        );

        result.putExtra(
                "longitude",
                selectedLng
        );

        setResult(
                RESULT_OK,
                result
        );

        finish();
    }

    private void listenForRide() {

        if (rideListener != null) {

            rideListener.remove();
            rideListener = null;
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

                                    updateLiveRideMap(
                                            snapshot
                                    );
                                }
                        );
    }

    private void updateLiveRideMap(
            DocumentSnapshot ride) {

        Double driverLat =
                getDouble(
                        ride,
                        "driverLatitude"
                );

        Double driverLng =
                getDouble(
                        ride,
                        "driverLongitude"
                );

        if (driverLat == null
                || driverLng == null) {

            driverLat =
                    getDouble(
                            ride,
                            "latitude"
                    );

            driverLng =
                    getDouble(
                            ride,
                            "longitude"
                    );
        }

        if (driverLat == null
                || driverLng == null) {

            return;
        }

        final double lat =
                driverLat;

        final double lng =
                driverLng;

        runOnUiThread(
                () -> {

                    if (webView == null
                            || !mapReady) {
                        return;
                    }

                    String javascript =
                            "if(window.setDriver){" +
                            "setDriver(" +
                            lat +
                            "," +
                            lng +
                            ");" +
                            "}";

                    webView.evaluateJavascript(
                            javascript,
                            null
                    );

                    statusText.setText(
                            "🚕 Driver location updated"
                    );
                }
        );
    }

    private Double getDouble(
            DocumentSnapshot document,
            String field) {

        Object value =
                document.get(field);

        if (value instanceof Number) {

            return ((Number) value)
                    .doubleValue();
        }

        if (value instanceof String) {

            try {

                return Double.parseDouble(
                        (String) value
                );

            } catch (Exception ignored) {
            }
        }

        return null;
    }

    public class MapBridge {

        @JavascriptInterface
        public void tap(
                double lat,
                double lng) {

            reverseGeocode(
                    lat,
                    lng
            );
        }
    }

    private void reverseGeocode(
            double lat,
            double lng) {

        new Thread(
                () -> {

                    String addressText =
                            null;

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

                        if (addresses != null
                                && !addresses.isEmpty()) {

                            addressText =
                                    addresses
                                            .get(0)
                                            .getAddressLine(0);
                        }

                    } catch (Exception ignored) {
                    }

                    if (addressText == null
                            || addressText.trim().isEmpty()) {

                        addressText =
                                String.format(
                                        Locale.US,
                                        "%.6f, %.6f",
                                        lat,
                                        lng
                                );
                    }

                    final String finalAddress =
                            addressText;

                    runOnUiThread(
                            () -> {

                                selectedLat =
                                        lat;

                                selectedLng =
                                        lng;

                                selectedAddress =
                                        finalAddress;

                                destinationSelected =
                                        true;

                                searchInput.setText(
                                        finalAddress
                                );

                                statusText.setText(
                                        "📍 "
                                                + finalAddress
                                );

                                String safeName =
                                        JSONObject
                                                .quote(
                                                        finalAddress
                                                );

                                String javascript =
                                        "if(window.setDestination){" +
                                        "setDestination(" +
                                        lat +
                                        "," +
                                        lng +
                                        "," +
                                        safeName +
                                        ");" +
                                        "}";

                                webView.evaluateJavascript(
                                        javascript,
                                        null
                                );
                            }
                    );
                }
        ).start();
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

        if (requestCode
                == LOCATION_PERMISSION) {

            if (grantResults.length > 0
                    && grantResults[0]
                    == PackageManager.PERMISSION_GRANTED) {

                beginLocationUpdates();

            } else {

                Toast.makeText(
                        this,
                        "Location permission is needed for GPS.",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    @Override
    protected void onDestroy() {

        if (rideListener != null) {

            rideListener.remove();
            rideListener = null;
        }

        if (locationManager != null
                && locationListener != null) {

            try {

                locationManager.removeUpdates(
                        locationListener
                );

            } catch (Exception ignored) {
            }
        }

        handler.removeCallbacksAndMessages(
                null
        );

        if (webView != null) {

            webView.stopLoading();
            webView.destroy();
            webView = null;
        }

        super.onDestroy();
    }
}
