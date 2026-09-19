
package com.sakyna.app;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Locale;

public class MapActivity extends Activity {

    private static final int LOCATION_PERMISSION = 4001;

    private WebView webView;
    private EditText searchInput;
    private TextView locationText;
    private TextView addressText;

    private LocationManager locationManager;
    private LocationListener locationListener;

    private double currentLat = 0.0;
    private double currentLng = 0.0;

    private String currentPlaceName = "";
    private String currentAddress = "";

    private boolean mapReady = false;
    private boolean searching = false;

    private String mode = "";
    private String rideId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        if (intent != null) {
            mode = safe(intent.getStringExtra("mode"));
            rideId = safe(intent.getStringExtra("ride_id"));

            if (rideId.isEmpty()) {
                rideId = safe(intent.getStringExtra("rideId"));
            }
        }

        buildScreen();
        startLocation();
    }

    private void buildScreen() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText(
                mode.equals("LIVE_RIDE")
                        ? "🚕 LIVE RIDE MAP"
                        : "📍 SELECT DESTINATION"
        );
        title.setTextSize(22);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);
        title.setPadding(10, 16, 10, 16);

        root.addView(title);

        locationText = new TextView(this);
        locationText.setText("📡 Getting GPS location...");
        locationText.setTextSize(15);
        locationText.setTextColor(Color.DKGRAY);
        locationText.setPadding(12, 4, 12, 4);

        root.addView(locationText);

        addressText = new TextView(this);
        addressText.setText("Address: waiting for GPS...");
        addressText.setTextSize(15);
        addressText.setTextColor(Color.DKGRAY);
        addressText.setPadding(12, 4, 12, 8);

        root.addView(addressText);

        if (!mode.equals("LIVE_RIDE")) {

            LinearLayout searchBox = new LinearLayout(this);
            searchBox.setOrientation(LinearLayout.VERTICAL);
            searchBox.setPadding(10, 6, 10, 8);
            searchBox.setBackgroundColor(Color.rgb(245, 245, 245));

            TextView label = new TextView(this);
            label.setText("🔎 SEARCH DESTINATION");
            label.setTextSize(17);
            label.setTextColor(Color.BLACK);
            label.setPadding(0, 0, 0, 5);

            searchBox.addView(label);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);

            searchInput = new EditText(this);
            searchInput.setHint("Jollibee, SM, street...");
            searchInput.setSingleLine(true);
            searchInput.setTextSize(16);
            searchInput.setTextColor(Color.BLACK);
            searchInput.setHintTextColor(Color.GRAY);
            searchInput.setFocusable(true);
            searchInput.setFocusableInTouchMode(true);
            searchInput.setClickable(true);
            searchInput.setEnabled(true);
            searchInput.setBackgroundColor(Color.WHITE);
            searchInput.setPadding(12, 0, 12, 0);

            row.addView(
                    searchInput,
                    new LinearLayout.LayoutParams(
                            0,
                            58,
                            1
                    )
            );

            Button searchButton = new Button(this);
            searchButton.setText("SEARCH");
            searchButton.setTextColor(Color.WHITE);
            searchButton.setTextSize(14);
            searchButton.setBackgroundColor(
                    Color.rgb(0, 130, 70)
            );
            searchButton.setClickable(true);
            searchButton.setEnabled(true);

            LinearLayout.LayoutParams searchButtonParams =
                    new LinearLayout.LayoutParams(
                            125,
                            58
                    );

            searchButtonParams.setMargins(8, 0, 0, 0);

            row.addView(
                    searchButton,
                    searchButtonParams
            );

            searchBox.addView(row);

            searchButton.setOnClickListener(v -> {

                InputMethodManager imm =
                        (InputMethodManager)
                                getSystemService(
                                        Context.INPUT_METHOD_SERVICE
                                );

                if (imm != null) {
                    imm.hideSoftInputFromWindow(
                            searchInput.getWindowToken(),
                            0
                    );
                }

                searchPlace();
            });

            searchInput.setOnEditorActionListener(
                    (v, actionId, event) -> {

                        searchPlace();
                        return true;
                    }
            );

            root.addView(searchBox);
        }

        webView = new WebView(this);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setGeolocationEnabled(true);

        webView.setWebViewClient(
                new WebViewClient()
        );

        webView.addJavascriptInterface(
                new MapBridge(),
                "AndroidMap"
        );

        root.addView(
                webView,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        Button currentButton = new Button(this);
        currentButton.setText(
                "📍 USE MY CURRENT LOCATION"
        );

        currentButton.setOnClickListener(
                v -> useCurrentLocation()
        );

        root.addView(currentButton);

        if (!mode.equals("LIVE_RIDE")) {

            Button confirmButton = new Button(this);
            confirmButton.setText(
                    "✅ USE SELECTED DESTINATION"
            );

            confirmButton.setOnClickListener(
                    v -> confirmDestination()
            );

            root.addView(confirmButton);
        }

        Button closeButton = new Button(this);
        closeButton.setText("CLOSE MAP");

        closeButton.setOnClickListener(
                v -> finish()
        );

        root.addView(closeButton);

        setContentView(root);

        loadMap();
    }

    private void loadMap() {

        String html =
                "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta name='viewport' " +
                "content='width=device-width,initial-scale=1.0'>" +

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
                "</style>" +

                "</head>" +
                "<body>" +

                "<div id='map'></div>" +

                "<script>" +

                "var map=L.map('map').setView([14.5995,120.9842],12);" +

                "L.tileLayer(" +
                "'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'," +
                "{maxZoom:19," +
                "attribution:'© OpenStreetMap contributors'}" +
                ").addTo(map);" +

                "var gpsMarker=null;" +
                "var destinationMarker=null;" +

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
                "destinationMarker.bindPopup(name||'Destination').openPopup();" +
                "map.setView([lat,lng],17);" +
                "}" +

                "map.on('click',function(e){" +
                "AndroidMap.mapTap(e.latlng.lat,e.latlng.lng);" +
                "});" +

                "</script>" +
                "</body>" +
                "</html>";

        webView.loadDataWithBaseURL(
                "https://www.openstreetmap.org/",
                html,
                "text/html",
                "UTF-8",
                null
        );

        mapReady = true;
    }

    private class MapBridge {

        @JavascriptInterface
        public void mapTap(
                double lat,
                double lng) {

            runOnUiThread(() -> {

                currentLat = lat;
                currentLng = lng;

                updateCoordinates(
                        lat,
                        lng
                );

                setMapDestination(
                        lat,
                        lng,
                        "Selected location"
                );

                reverseGeocode(
                        lat,
                        lng
                );
            });
        }
    }

    private void startLocation() {

        locationManager =
                (LocationManager)
                        getSystemService(
                                LOCATION_SERVICE
                        );

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
                &&
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
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

        locationListener =
                new LocationListener() {

                    @Override
                    public void onLocationChanged(
                            @NonNull Location location) {

                        currentLat =
                                location.getLatitude();

                        currentLng =
                                location.getLongitude();

                        updateCoordinates(
                                currentLat,
                                currentLng
                        );

                        updateGPSMap(
                                currentLat,
                                currentLng
                        );
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
                        locationListener,
                        Looper.getMainLooper()
                );
            }

            if (locationManager.isProviderEnabled(
                    LocationManager.NETWORK_PROVIDER
            )) {

                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        3000,
                        5,
                        locationListener,
                        Looper.getMainLooper()
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
                    chooseBest(
                            gps,
                            network
                    );

            if (best != null) {

                currentLat =
                        best.getLatitude();

                currentLng =
                        best.getLongitude();

                updateCoordinates(
                        currentLat,
                        currentLng
                );

                updateGPSMap(
                        currentLat,
                        currentLng
                );

                reverseGeocode(
                        currentLat,
                        currentLng
                );
            }

        } catch (SecurityException e) {

            locationText.setText(
                    "📍 Location permission required."
            );
        }
    }

    private Location chooseBest(
            Location a,
            Location b) {

        if (a == null) return b;
        if (b == null) return a;

        if (a.hasAccuracy()
                && b.hasAccuracy()) {

            return a.getAccuracy()
                    <= b.getAccuracy()
                    ? a
                    : b;
        }

        return a;
    }

    private void updateCoordinates(
            double lat,
            double lng) {

        locationText.setText(
                "📍 GPS LOCATION\n"
                        + "Latitude: "
                        + String.format(
                        Locale.US,
                        "%.6f",
                        lat
                )
                        + "\nLongitude: "
                        + String.format(
                        Locale.US,
                        "%.6f",
                        lng
                )
        );
    }

    private void updateGPSMap(
            double lat,
            double lng) {

        if (webView == null
                || !mapReady) {
            return;
        }

        webView.post(() ->
                webView.evaluateJavascript(
                        "setGPS("
                                + lat
                                + ","
                                + lng
                                + ");",
                        null
                )
        );
    }

    private void setMapDestination(
            double lat,
            double lng,
            String name) {

        if (webView == null
                || !mapReady) {
            return;
        }

        String safeName =
                name == null
                        ? ""
                        : name
                        .replace(
                                "\\",
                                "\\\\"
                        )
                        .replace(
                                "'",
                                "\\'"
                        );

        webView.post(() ->
                webView.evaluateJavascript(
                        "setDestination("
                                + lat
                                + ","
                                + lng
                                + ",'"
                                + safeName
                                + "');",
                        null
                )
        );
    }

    private void useCurrentLocation() {

        if (currentLat == 0.0
                && currentLng == 0.0) {

            Toast.makeText(
                    this,
                    "Waiting for GPS...",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        setMapDestination(
                currentLat,
                currentLng,
                "Your current location"
        );

        reverseGeocode(
                currentLat,
                currentLng
        );
    }

    private void searchPlace() {

        if (searching) {
            return;
        }

        if (searchInput == null) {
            return;
        }

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

        searching = true;

        Toast.makeText(
                this,
                "🔎 Searching...",
                Toast.LENGTH_SHORT
        ).show();

        final double searchLat =
                currentLat == 0.0
                        ? 14.5995
                        : currentLat;

        final double searchLng =
                currentLng == 0.0
                        ? 120.9842
                        : currentLng;

        new Thread(() -> {

            JSONObject result = null;

            try {
                result =
                        searchAndroidGeocoder(
                                query,
                                searchLat,
                                searchLng
                        );
            } catch (Exception ignored) {
            }

            if (result == null) {

                try {
                    result =
                            searchPhoton(
                                    query,
                                    searchLat,
                                    searchLng
                            );
                } catch (Exception ignored) {
                }
            }

            if (result == null) {

                try {
                    result =
                            searchNominatim(
                                    query,
                                    searchLat,
                                    searchLng
                            );
                } catch (Exception ignored) {
                }
            }

            final JSONObject finalResult =
                    result;

            runOnUiThread(() -> {

                searching = false;

                if (finalResult == null) {

                    Toast.makeText(
                            this,
                            "Place not found. Try a more specific name.",
                            Toast.LENGTH_LONG
                    ).show();

                    return;
                }

                applySearchResult(
                        finalResult
                );
            });

        }).start();
    }

    private JSONObject searchAndroidGeocoder(
            String query,
            double lat,
            double lng)
            throws Exception {

        if (!Geocoder.isPresent()) {
            return null;
        }

        Geocoder geocoder =
                new Geocoder(
                        this,
                        Locale.ENGLISH
                );

        List<Address> results =
                geocoder.getFromLocationName(
                        query,
                        10,
                        4.0,
                        116.0,
                        21.5,
                        127.0
                );

        if (results == null
                || results.isEmpty()) {
            return null;
        }

        Address best = null;
        double bestScore =
                -Double.MAX_VALUE;

        String q =
                query.toLowerCase(
                        Locale.US
                );

        for (Address a : results) {

            if (a == null) {
                continue;
            }

            double score = 0;

            String feature =
                    safe(a.getFeatureName())
                            .toLowerCase(Locale.US);

            String locality =
                    safe(a.getLocality())
                            .toLowerCase(Locale.US);

            String admin =
                    safe(a.getAdminArea())
                            .toLowerCase(Locale.US);

            String line =
                    safe(a.getAddressLine(0))
                            .toLowerCase(Locale.US);

            if (feature.equals(q)) {
                score += 500;
            }

            if (feature.contains(q)) {
                score += 300;
            }

            if (line.contains(q)) {
                score += 200;
            }

            if (locality.contains(q)) {
                score += 100;
            }

            if (admin.contains(q)) {
                score += 50;
            }

            if (a.getLatitude() != 0
                    || a.getLongitude() != 0) {

                float[] distance =
                        new float[1];

                Location.distanceBetween(
                        lat,
                        lng,
                        a.getLatitude(),
                        a.getLongitude(),
                        distance
                );

                score -= Math.min(
                        distance[0] / 1000.0,
                        100
                );
            }

            if (best == null
                    || score > bestScore) {

                best = a;
                bestScore = score;
            }
        }

        if (best == null) {
            return null;
        }

        JSONObject result =
                new JSONObject();

        result.put(
                "lat",
                best.getLatitude()
        );

        result.put(
                "lon",
                best.getLongitude()
        );

        result.put(
                "name",
                safe(
                        best.getFeatureName()
                )
        );

        result.put(
                "address",
                buildAndroidAddress(
                        best
                )
        );

        return result;
    }

    private JSONObject searchPhoton(
            String query,
            double lat,
            double lng)
            throws Exception {

        String url =
                "https://photon.komoot.io/api/?q="
                        + URLEncoder.encode(
                        query,
                        "UTF-8"
                )
                        + "&limit=8"
                        + "&lat="
                        + lat
                        + "&lon="
                        + lng
                        + "&zoom=14"
                        + "&lang=en";

        JSONObject object =
                getJson(
                        url,
                        12000
                );

        JSONArray features =
                object.optJSONArray(
                        "features"
                );

        if (features == null
                || features.length() == 0) {
            return null;
        }

        JSONObject best =
                chooseBestPhoton(
                        features,
                        query,
                        lat,
                        lng
                );

        if (best == null) {
            return null;
        }

        JSONObject properties =
                best.optJSONObject(
                        "properties"
                );

        JSONObject geometry =
                best.optJSONObject(
                        "geometry"
                );

        if (properties == null
                || geometry == null) {
            return null;
        }

        JSONArray coordinates =
                geometry.optJSONArray(
                        "coordinates"
                );

        if (coordinates == null
                || coordinates.length() < 2) {
            return null;
        }

        double resultLng =
                coordinates.optDouble(0);

        double resultLat =
                coordinates.optDouble(1);

        JSONObject result =
                new JSONObject();

        result.put(
                "lat",
                resultLat
        );

        result.put(
                "lon",
                resultLng
        );

        result.put(
                "name",
                properties.optString(
                        "name",
                        ""
                )
        );

        result.put(
                "address",
                buildPhotonAddress(
                        properties
                )
        );

        return result;
    }

    private JSONObject chooseBestPhoton(
            JSONArray features,
            String query,
            double lat,
            double lng) {

        JSONObject best = null;
        double bestScore =
                -Double.MAX_VALUE;

        String q =
                query.toLowerCase(
                        Locale.US
                );

        for (int i = 0;
             i < features.length();
             i++) {

            JSONObject item =
                    features.optJSONObject(i);

            if (item == null) {
                continue;
            }

            JSONObject p =
                    item.optJSONObject(
                            "properties"
                    );

            JSONObject geometry =
                    item.optJSONObject(
                            "geometry"
                    );

            if (p == null
                    || geometry == null) {
                continue;
            }

            String name =
                    p.optString(
                            "name",
                            ""
                    );

            String country =
                    p.optString(
                            "country",
                            ""
                    );

            String city =
                    p.optString(
                            "city",
                            ""
                    );

            double score = 0;

            String lowerName =
                    name.toLowerCase(
                            Locale.US
                    );

            if (lowerName.equals(q)) {
                score += 300;
            }

            if (lowerName.contains(q)) {
                score += 200;
            }

            if (q.contains(lowerName)
                    && !lowerName.isEmpty()) {
                score += 150;
            }

            if (country.equalsIgnoreCase(
                    "Philippines"
            )) {
                score += 100;
            }

            if (!city.isEmpty()) {
                score += 10;
            }

            JSONArray coordinates =
                    geometry.optJSONArray(
                            "coordinates"
                    );

            if (coordinates != null
                    && coordinates.length() >= 2) {

                double itemLng =
                        coordinates.optDouble(0);

                double itemLat =
                        coordinates.optDouble(1);

                float[] distance =
                        new float[1];

                Location.distanceBetween(
                        lat,
                        lng,
                        itemLat,
                        itemLng,
                        distance
                );

                score -= Math.min(
                        distance[0] / 1000.0,
                        30
                );
            }

            if (best == null
                    || score > bestScore) {

                best = item;
                bestScore = score;
            }
        }

        return best;
    }

    private JSONObject searchNominatim(
            String query,
            double lat,
            double lng)
            throws Exception {

        String url =
                "https://nominatim.openstreetmap.org/search"
                        + "?format=jsonv2"
                        + "&q="
                        + URLEncoder.encode(
                        query + ", Philippines",
                        "UTF-8"
                )
                        + "&limit=8"
                        + "&addressdetails=1"
                        + "&namedetails=1"
                        + "&accept-language=en";

        JSONArray array =
                getJsonArray(
                        url,
                        12000
                );

        if (array == null
                || array.length() == 0) {
            return null;
        }

        JSONObject best = null;
        double bestScore =
                Double.MAX_VALUE;

        String q =
                query.toLowerCase(
                        Locale.US
                );

        for (int i = 0;
             i < array.length();
             i++) {

            JSONObject item =
                    array.optJSONObject(i);

            if (item == null) {
                continue;
            }

            double itemLat =
                    item.optDouble(
                            "lat",
                            0
                    );

            double itemLng =
                    item.optDouble(
                            "lon",
                            0
                    );

            float[] distance =
                    new float[1];

            Location.distanceBetween(
                    lat,
                    lng,
                    itemLat,
                    itemLng,
                    distance
            );

            double score =
                    distance[0];

            String display =
                    item.optString(
                            "display_name",
                            ""
                    ).toLowerCase(
                            Locale.US
                    );

            String name =
                    getBestNominatimName(
                            item
                    ).toLowerCase(
                            Locale.US
                    );

            if (name.equals(q)) {
                score -= 5000;
            } else if (name.contains(q)) {
                score -= 3000;
            } else if (display.contains(q)) {
                score -= 1000;
            }

            if (best == null
                    || score < bestScore) {

                best = item;
                bestScore = score;
            }
        }

        if (best == null) {
            return null;
        }

        JSONObject result =
                new JSONObject();

        result.put(
                "lat",
                best.optDouble("lat")
        );

        result.put(
                "lon",
                best.optDouble("lon")
        );

        result.put(
                "name",
                getBestNominatimName(
                        best
                )
        );

        String address =
                buildNominatimAddress(
                        best.optJSONObject(
                                "address"
                        )
                );

        if (address.isEmpty()) {

            address =
                    best.optString(
                            "display_name",
                            ""
                    );
        }

        result.put(
                "address",
                address
        );

        return result;
    }

    private void applySearchResult(
            JSONObject result) {

        try {

            /*
             * IMPORTANT:
             * These coordinates are the coordinates
             * returned by the ORIGINAL search.
             *
             * We do NOT move them while fixing
             * the human-readable name.
             */

            double lat =
                    result.optDouble(
                            "lat",
                            0
                    );

            double lng =
                    result.optDouble(
                            "lon",
                            0
                    );

            if (lat == 0
                    && lng == 0) {

                Toast.makeText(
                        this,
                        "Invalid search location.",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            currentLat = lat;
            currentLng = lng;

            String originalName =
                    result.optString(
                            "name",
                            ""
                    );

            String originalAddress =
                    result.optString(
                            "address",
                            ""
                    );

            currentPlaceName =
                    originalName;

            currentAddress =
                    originalAddress;

            /*
             * First put the EXACT selected search
             * coordinates on the map.
             */
            setMapDestination(
                    lat,
                    lng,
                    originalName
            );

            updateCoordinates(
                    lat,
                    lng
            );

            addressText.setText(
                    "📍 "
                            + (
                            originalName.isEmpty()
                                    ? "Selected location"
                                    : originalName
                    )
                            + "\n"
                            + originalAddress
            );

            /*
             * Now improve ONLY the name/address.
             * Coordinates are NEVER replaced.
             */
            final double fixedLat = lat;
            final double fixedLng = lng;

            new Thread(() ->
                    reverseNominatim(
                            fixedLat,
                            fixedLng
                    )
            ).start();

            Toast.makeText(
                    this,
                    "📍 Location found.",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Unable to use search result.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void reverseNominatim(
            double lat,
            double lng) {

        try {

            String url =
                    "https://nominatim.openstreetmap.org/reverse"
                            + "?format=jsonv2"
                            + "&lat="
                            + lat
                            + "&lon="
                            + lng
                            + "&zoom=18"
                            + "&addressdetails=1"
                            + "&namedetails=1"
                            + "&accept-language=en";

            JSONObject object =
                    getJson(
                            url,
                            12000
                    );

            String name =
                    getBestNominatimName(
                            object
                    );

            String address =
                    buildNominatimAddress(
                            object.optJSONObject(
                                    "address"
                            )
                    );

            if (address.isEmpty()) {

                address =
                        object.optString(
                                "display_name",
                                ""
                        );
            }

            final String finalName =
                    name;

            final String finalAddress =
                    address;

            runOnUiThread(() -> {

                /*
                 * Keep original coordinates.
                 */
                currentLat = lat;
                currentLng = lng;

                if (!finalName.isEmpty()) {
                    currentPlaceName =
                            finalName;
                }

                if (!finalAddress.isEmpty()) {
                    currentAddress =
                            finalAddress;
                }

                String displayName =
                        currentPlaceName.isEmpty()
                                ? "Selected location"
                                : currentPlaceName;

                addressText.setText(
                        "📍 "
                                + displayName
                                + "\n"
                                + currentAddress
                );

                setMapDestination(
                        lat,
                        lng,
                        displayName
                );
            });

        } catch (Exception ignored) {
        }
    }

    private String getBestNominatimName(
            JSONObject object) {

        if (object == null) {
            return "";
        }

        JSONObject namedetails =
                object.optJSONObject(
                        "namedetails"
                );

        if (namedetails != null) {

            String brand =
                    firstNonEmpty(
                            namedetails.optString(
                                    "brand",
                                    ""
                            ),
                            namedetails.optString(
                                    "official_name",
                                    ""
                            )
                    );

            if (!brand.isEmpty()) {
                return brand;
            }

            String name =
                    namedetails.optString(
                            "name",
                            ""
                    );

            if (!name.isEmpty()) {
                return name;
            }
        }

        String objectName =
                object.optString(
                        "name",
                        ""
                );

        if (!objectName.isEmpty()) {
            return objectName;
        }

        JSONObject address =
                object.optJSONObject(
                        "address"
                );

        if (address != null) {

            String poi =
                    firstNonEmpty(
                            address.optString(
                                    "amenity",
                                    ""
                            ),
                            address.optString(
                                    "shop",
                                    ""
                            ),
                            address.optString(
                                    "tourism",
                                    ""
                            ),
                            address.optString(
                                    "office",
                                    ""
                            ),
                            address.optString(
                                    "attraction",
                                    ""
                            ),
                            address.optString(
                                    "building",
                                    ""
                            )
                    );

            if (!poi.isEmpty()) {
                return poi;
            }
        }

        return "";
    }

    private void reverseGeocode(
            double lat,
            double lng) {

        new Thread(() -> {

            /*
             * Android Geocoder first, matching
             * the original working implementation.
             */
            try {

                if (Geocoder.isPresent()) {

                    Geocoder geocoder =
                            new Geocoder(
                                    this,
                                    Locale.ENGLISH
                            );

                    List<Address> results =
                            geocoder.getFromLocation(
                                    lat,
                                    lng,
                                    1
                            );

                    if (results != null
                            && !results.isEmpty()) {

                        Address a =
                                results.get(0);

                        String name =
                                safe(
                                        a.getFeatureName()
                                );

                        String address =
                                buildAndroidAddress(
                                        a
                                );

                        if (address.isEmpty()) {

                            address =
                                    safe(
                                            a.getAddressLine(0)
                                    );
                        }

                        if (!name.isEmpty()
                                || !address.isEmpty()) {

                            final String finalName =
                                    name;

                            final String finalAddress =
                                    address;

                            runOnUiThread(() -> {

                                currentLat = lat;
                                currentLng = lng;

                                if (!finalName.isEmpty()) {
                                    currentPlaceName =
                                            finalName;
                                }

                                if (!finalAddress.isEmpty()) {
                                    currentAddress =
                                            finalAddress;
                                }

                                String shown =
                                        currentPlaceName.isEmpty()
                                                ? "Current location"
                                                : currentPlaceName;

                                addressText.setText(
                                        "📍 "
                                                + shown
                                                + "\n"
                                                + currentAddress
                                );
                            });

                            return;
                        }
                    }
                }

            } catch (Exception ignored) {
            }

            /*
             * Nominatim fallback.
             */
            reverseNominatim(
                    lat,
                    lng
            );

        }).start();
    }

    private String buildAndroidAddress(
            Address address) {

        if (address == null) {
            return "";
        }

        StringBuilder result =
                new StringBuilder();

        addPart(
                result,
                address.getSubThoroughfare()
        );

        addPart(
                result,
                address.getThoroughfare()
        );

        addPart(
                result,
                address.getSubLocality()
        );

        addPart(
                result,
                address.getLocality()
        );

        addPart(
                result,
                address.getAdminArea()
        );

        addPart(
                result,
                address.getPostalCode()
        );

        return result.toString();
    }

    private String buildNominatimAddress(
            JSONObject address) {

        if (address == null) {
            return "";
        }

        StringBuilder result =
                new StringBuilder();

        addPart(
                result,
                address.optString(
                        "house_number",
                        ""
                )
        );

        addPart(
                result,
                address.optString(
                        "road",
                        ""
                )
        );

        addPart(
                result,
                firstNonEmpty(
                        address.optString(
                                "neighbourhood",
                                ""
                        ),
                        address.optString(
                                "suburb",
                                ""
                        ),
                        address.optString(
                                "village",
                                ""
                        ),
                        address.optString(
                                "barangay",
                                ""
                        )
                )
        );

        addPart(
                result,
                firstNonEmpty(
                        address.optString(
                                "town",
                                ""
                        ),
                        address.optString(
                                "city",
                                ""
                        ),
                        address.optString(
                                "municipality",
                                ""
                        )
                )
        );

        addPart(
                result,
                firstNonEmpty(
                        address.optString(
                                "province",
                                ""
                        ),
                        address.optString(
                                "state",
                                ""
                        )
                )
        );

        addPart(
                result,
                address.optString(
                        "postcode",
                        ""
                )
        );

        return result.toString();
    }

    private String buildPhotonAddress(
            JSONObject properties) {

        if (properties == null) {
            return "";
        }

        StringBuilder result =
                new StringBuilder();

        addPart(
                result,
                properties.optString(
                        "housenumber",
                        ""
                )
        );

        addPart(
                result,
                properties.optString(
                        "street",
                        ""
                )
        );

        addPart(
                result,
                properties.optString(
                        "district",
                        ""
                )
        );

        addPart(
                result,
                properties.optString(
                        "city",
                        ""
                )
        );

        addPart(
                result,
                properties.optString(
                        "state",
                        ""
                )
        );

        return result.toString();
    }

    private String firstNonEmpty(
            String... values) {

        for (String value : values) {

            if (value != null
                    && !value.trim().isEmpty()) {

                return value.trim();
            }
        }

        return "";
    }

    private void addPart(
            StringBuilder builder,
            String value) {

        if (value == null
                || value.trim().isEmpty()) {
            return;
        }

        if (builder.length() > 0) {
            builder.append(", ");
        }

        builder.append(
                value.trim()
        );
    }

    private JSONObject getJson(
            String urlString,
            int timeout)
            throws Exception {

        return new JSONObject(
                getText(
                        urlString,
                        timeout
                )
        );
    }

    private JSONArray getJsonArray(
            String urlString,
            int timeout)
            throws Exception {

        return new JSONArray(
                getText(
                        urlString,
                        timeout
                )
        );
    }

    private String getText(
            String urlString,
            int timeout)
            throws Exception {

        HttpURLConnection connection =
                null;

        try {

            URL url =
                    new URL(
                            urlString
                    );

            connection =
                    (HttpURLConnection)
                            url.openConnection();

            connection.setRequestMethod(
                    "GET"
            );

            connection.setConnectTimeout(
                    timeout
            );

            connection.setReadTimeout(
                    timeout
            );

            connection.setUseCaches(
                    false
            );

            connection.setRequestProperty(
                    "User-Agent",
                    "SakayNa/1.0 Android"
            );

            connection.setRequestProperty(
                    "Accept",
                    "application/json"
            );

            int code =
                    connection.getResponseCode();

            if (code != 200) {

                throw new Exception(
                        "HTTP " + code
                );
            }

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    connection.getInputStream()
                            )
                    );

            StringBuilder result =
                    new StringBuilder();

            String line;

            while ((line =
                    reader.readLine()) != null) {

                result.append(line);
            }

            reader.close();

            return result.toString();

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void confirmDestination() {

        if (currentLat == 0.0
                && currentLng == 0.0) {

            Toast.makeText(
                    this,
                    "Select a destination first.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Intent result =
                new Intent();

        result.putExtra(
                "destination_latitude",
                currentLat
        );

        result.putExtra(
                "destination_longitude",
                currentLng
        );

        result.putExtra(
                "destinationName",
                currentPlaceName
        );

        result.putExtra(
                "destination_address",
                currentAddress
        );

        result.putExtra(
                "latitude",
                currentLat
        );

        result.putExtra(
                "longitude",
                currentLng
        );

        setResult(
                RESULT_OK,
                result
        );

        finish();
    }

    private String safe(
            String value) {

        return value == null
                ? ""
                : value.trim();
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

        if (requestCode ==
                LOCATION_PERMISSION) {

            boolean granted = false;

            for (int result :
                    grantResults) {

                if (result ==
                        PackageManager.PERMISSION_GRANTED) {

                    granted = true;
                    break;
                }
            }

            if (granted) {
                beginLocationUpdates();
            } else {
                locationText.setText(
                        "📍 Location permission denied."
                );
            }
        }
    }

    @Override
    protected void onDestroy() {

        if (locationManager != null
                && locationListener != null) {

            try {

                locationManager.removeUpdates(
                        locationListener
                );

            } catch (SecurityException ignored) {
            }
        }

        if (webView != null) {

            webView.stopLoading();
            webView.destroy();
            webView = null;
        }

        super.onDestroy();
    }
}
