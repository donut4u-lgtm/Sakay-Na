
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
        title.setPadding(10, 18, 10, 18);

        root.addView(title);

        locationText = new TextView(this);
        locationText.setText("📡 Getting GPS location...");
        locationText.setTextSize(15);
        locationText.setTextColor(Color.DKGRAY);
        locationText.setPadding(15, 5, 15, 5);

        root.addView(locationText);

        addressText = new TextView(this);
        addressText.setText("Address: waiting for GPS...");
        addressText.setTextSize(15);
        addressText.setTextColor(Color.DKGRAY);
        addressText.setPadding(15, 5, 15, 10);

        root.addView(addressText);

        if (!mode.equals("LIVE_RIDE")) {

            LinearLayout searchContainer =
                    new LinearLayout(this);

            searchContainer.setOrientation(
                    LinearLayout.VERTICAL
            );

            searchContainer.setPadding(
                    12, 8, 12, 8
            );

            searchContainer.setBackgroundColor(
                    Color.rgb(245, 245, 245)
            );

            TextView label =
                    new TextView(this);

            label.setText(
                    "🔎 SEARCH DESTINATION"
            );

            label.setTextSize(17);
            label.setTextColor(Color.BLACK);

            searchContainer.addView(label);

            LinearLayout searchRow =
                    new LinearLayout(this);

            searchRow.setOrientation(
                    LinearLayout.HORIZONTAL
            );

            searchInput =
                    new EditText(this);

            searchInput.setHint(
                    "Jollibee, SM, street..."
            );

            searchInput.setTextSize(16);
            searchInput.setSingleLine(true);
            searchInput.setFocusable(true);
            searchInput.setFocusableInTouchMode(true);
            searchInput.setClickable(true);
            searchInput.setEnabled(true);
            searchInput.setBackgroundColor(Color.WHITE);

            LinearLayout.LayoutParams inputParams =
                    new LinearLayout.LayoutParams(
                            0,
                            58,
                            1
                    );

            searchRow.addView(
                    searchInput,
                    inputParams
            );

            Button searchButton =
                    new Button(this);

            searchButton.setText("SEARCH");
            searchButton.setTextColor(Color.WHITE);
            searchButton.setBackgroundColor(
                    Color.rgb(0, 130, 70)
            );

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

            LinearLayout.LayoutParams buttonParams =
                    new LinearLayout.LayoutParams(
                            125,
                            58
                    );

            buttonParams.setMargins(
                    8, 0, 0, 0
            );

            searchRow.addView(
                    searchButton,
                    buttonParams
            );

            searchContainer.addView(
                    searchRow
            );

            root.addView(
                    searchContainer
            );
        }

        webView = new WebView(this);

        WebSettings settings =
                webView.getSettings();

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

        Button currentButton =
                new Button(this);

        currentButton.setText(
                "📍 USE MY CURRENT LOCATION"
        );

        currentButton.setOnClickListener(
                v -> useCurrentLocation()
        );

        root.addView(currentButton);

        if (!mode.equals("LIVE_RIDE")) {

            Button confirmButton =
                    new Button(this);

            confirmButton.setText(
                    "✅ USE SELECTED DESTINATION"
            );

            confirmButton.setOnClickListener(
                    v -> confirmDestination()
            );

            root.addView(confirmButton);
        }

        Button closeButton =
                new Button(this);

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
                "</style>" +

                "</head>" +

                "<body>" +

                "<div id='map'></div>" +

                "<script>" +

                "var map=" +
                "L.map('map').setView([14.5995,120.9842],12);" +

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
                "destinationMarker=" +
                "L.marker([lat,lng]).addTo(map);" +
                "}else{" +
                "destinationMarker.setLatLng([lat,lng]);" +
                "}" +

                "destinationMarker.bindPopup(" +
                "name||'Destination').openPopup();" +

                "map.setView([lat,lng],17);" +

                "}" +

                "map.on('click',function(e){" +
                "AndroidMap.mapTap(" +
                "e.latlng.lat,e.latlng.lng);" +
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

                        updateMap(
                                currentLat,
                                currentLng
                        );

                        reverseGeocode(
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
                    chooseBest(gps, network);

            if (best != null) {

                currentLat =
                        best.getLatitude();

                currentLng =
                        best.getLongitude();

                updateCoordinates(
                        currentLat,
                        currentLng
                );

                updateMap(
                        currentLat,
                        currentLng
                );

                reverseGeocode(
                        currentLat,
                        currentLng
                );
            }

        } catch (SecurityException ignored) {
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
                "📍 GPS LOCATION\n" +
                        "Latitude: " +
                        String.format(
                                Locale.US,
                                "%.6f",
                                lat
                        ) +
                        "\nLongitude: " +
                        String.format(
                                Locale.US,
                                "%.6f",
                                lng
                        )
        );
    }

    private void updateMap(
            double lat,
            double lng) {

        if (webView == null
                || !mapReady) {
            return;
        }

        webView.post(() ->
                webView.evaluateJavascript(
                        "setGPS(" +
                                lat +
                                "," +
                                lng +
                                ");",
                        null
                )
        );
    }

    private void useCurrentLocation() {

        if (currentLat == 0
                && currentLng == 0) {

            Toast.makeText(
                    this,
                    "Waiting for GPS...",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        updateMap(
                currentLat,
                currentLng
        );

        reverseGeocode(
                currentLat,
                currentLng
        );
    }

    private void searchPlace() {

        if (searchInput == null
                || searching) {
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

        new Thread(() -> {

            JSONObject result = null;

            double searchLat =
                    currentLat != 0
                            ? currentLat
                            : 14.5995;

            double searchLng =
                    currentLng != 0
                            ? currentLng
                            : 120.9842;

            /*
             * KEEP THE OLD SEARCH ORDER.
             * This is important because the old version
             * found the correct physical location.
             */

            result =
                    searchAndroidGeocoder(
                            query,
                            searchLat,
                            searchLng
                    );

            if (result == null) {

                result =
                        searchPhoton(
                                query,
                                searchLat,
                                searchLng
                        );
            }

            if (result == null) {

                result =
                        searchNominatim(
                                query,
                                searchLat,
                                searchLng
                        );
            }

            final JSONObject finalResult =
                    result;

            runOnUiThread(() -> {

                searching = false;

                if (finalResult == null) {

                    Toast.makeText(
                            this,
                            "Place not found.",
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
            double lng) {

        try {

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
                            10
                    );

            if (results == null
                    || results.isEmpty()) {
                return null;
            }

            Address best = null;
            double bestScore =
                    -Double.MAX_VALUE;

            for (Address address : results) {

                if (address == null) {
                    continue;
                }

                double score = 0;

                String full =
                        address.getAddressLine(0);

                String feature =
                        address.getFeatureName();

                String locality =
                        address.getLocality();

                String q =
                        query.toLowerCase(
                                Locale.US
                        );

                if (feature != null
                        && feature.toLowerCase(
                        Locale.US
                ).equals(q)) {
                    score += 300;
                }

                if (feature != null
                        && feature.toLowerCase(
                        Locale.US
                ).contains(q)) {
                    score += 200;
                }

                if (full != null
                        && full.toLowerCase(
                        Locale.US
                ).contains(q)) {
                    score += 100;
                }

                if (locality != null
                        && locality.toLowerCase(
                        Locale.US
                ).contains(q)) {
                    score += 50;
                }

                if (address.hasLatitude()
                        && address.hasLongitude()) {

                    float[] distance =
                            new float[1];

                    Location.distanceBetween(
                            lat,
                            lng,
                            address.getLatitude(),
                            address.getLongitude(),
                            distance
                    );

                    score -= Math.min(
                            distance[0] / 1000.0,
                            50
                    );
                }

                if (score > bestScore) {

                    bestScore = score;
                    best = address;
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

            String name =
                    best.getFeatureName();

            if (name == null) {
                name = "";
            }

            String address =
                    buildAndroidAddress(
                            best
                    );

            if (address.isEmpty()) {
                address =
                        safe(
                                best.getAddressLine(0)
                        );
            }

            result.put(
                    "name",
                    name
            );

            result.put(
                    "address",
                    address
            );

            return result;

        } catch (Exception ignored) {

            return null;
        }
    }

    private JSONObject searchPhoton(
            String query,
            double lat,
            double lng) {

        try {

            String encoded =
                    URLEncoder.encode(
                            query,
                            "UTF-8"
                    );

            String url =
                    "https://photon.komoot.io/api/?" +
                            "q=" +
                            encoded +
                            "&limit=8" +
                            "&lat=" +
                            lat +
                            "&lon=" +
                            lng +
                            "&zoom=14" +
                            "&lang=en";

            JSONObject json =
                    getJson(
                            url,
                            10000
                    );

            JSONArray features =
                    json.optJSONArray(
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

            String name =
                    properties.optString(
                            "name",
                            ""
                    );

            String address =
                    buildPhotonAddress(
                            properties
                    );

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
                    name
            );

            result.put(
                    "address",
                    address
            );

            return result;

        } catch (Exception ignored) {

            return null;
        }
    }

    private JSONObject searchNominatim(
            String query,
            double lat,
            double lng) {

        try {

            String encoded =
                    URLEncoder.encode(
                            query + ", Philippines",
                            "UTF-8"
                    );

            String url =
                    "https://nominatim.openstreetmap.org/search" +
                            "?format=jsonv2" +
                            "&q=" +
                            encoded +
                            "&limit=8" +
                            "&addressdetails=1" +
                            "&namedetails=1" +
                            "&accept-language=en";

            JSONArray array =
                    getJsonArray(
                            url,
                            10000
                    );

            if (array.length() == 0) {
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
                        Double.parseDouble(
                                item.optString(
                                        "lat",
                                        "0"
                                )
                        );

                double itemLng =
                        Double.parseDouble(
                                item.optString(
                                        "lon",
                                        "0"
                                )
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
                        );

                if (display.toLowerCase(
                        Locale.US
                ).contains(q)) {

                    score -= 1000;
                }

                if (score < bestScore) {

                    bestScore = score;
                    best = item;
                }
            }

            if (best == null) {
                return null;
            }

            double resultLat =
                    Double.parseDouble(
                            best.optString(
                                    "lat",
                                    "0"
                            )
                    );

            double resultLng =
                    Double.parseDouble(
                            best.optString(
                                    "lon",
                                    "0"
                            )
                    );

            String name =
                    getBestNominatimName(
                            best
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
                    name
            );

            result.put(
                    "address",
                    address
            );

            return result;

        } catch (Exception ignored) {

            return null;
        }
    }

    /*
     * IMPORTANT:
     * Coordinates are NEVER changed here.
     *
     * We only improve the human-readable name/address.
     */
    private void applySearchResult(
            JSONObject result) {

        try {

            double lat =
                    result.getDouble("lat");

            double lng =
                    result.getDouble("lon");

            currentLat = lat;
            currentLng = lng;

            currentPlaceName =
                    result.optString(
                            "name",
                            ""
                    );

            currentAddress =
                    result.optString(
                            "address",
                            ""
                    );

            updateCoordinates(
                    lat,
                    lng
            );

            updateMap(
                    lat,
                    lng
            );

            String displayName =
                    currentPlaceName;

            if (displayName.isEmpty()) {
                displayName = "Selected location";
            }

            addressText.setText(
                    "📍 " +
                            displayName +
                            "\n" +
                            currentAddress
            );

            /*
             * SECONDARY NAME LOOKUP.
             *
             * The selected coordinates remain exactly
             * the same. This only fixes the name/address.
             */
            final double fixedLat = lat;
            final double fixedLng = lng;

            new Thread(() -> {

                JSONObject exact =
                        reverseNominatim(
                                fixedLat,
                                fixedLng
                        );

                if (exact == null) {
                    return;
                }

                String betterName =
                        exact.optString(
                                "name",
                                ""
                        );

                String betterAddress =
                        exact.optString(
                                "address",
                                ""
                        );

                runOnUiThread(() -> {

                    /*
                     * NEVER replace coordinates.
                     */
                    currentLat = fixedLat;
                    currentLng = fixedLng;

                    if (!betterName.isEmpty()) {

                        currentPlaceName =
                                betterName;
                    }

                    if (!betterAddress.isEmpty()) {

                        currentAddress =
                                betterAddress;
                    }

                    String shown =
                            currentPlaceName;

                    if (shown.isEmpty()) {
                        shown = "Selected location";
                    }

                    addressText
