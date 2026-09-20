
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

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
    private TextView locationText;
    private TextView addressText;
    private EditText searchInput;

    private LocationManager locationManager;
    private LocationListener locationListener;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private Location currentLocation;

    private double currentLat = 0.0;
    private double currentLng = 0.0;

    private String currentPlaceName = "";
    private String currentAddress = "";

    private boolean mapReady = false;
    private boolean destinationSelected = false;

    private String mode = "";
    private String rideId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        Intent intent = getIntent();

        if (intent != null) {
            mode = safe(intent.getStringExtra("mode"));

            rideId = safe(
                    intent.getStringExtra("ride_id")
            );

            if (rideId.isEmpty()) {
                rideId = safe(
                        intent.getStringExtra("rideId")
                );
            }
        }

        buildScreen();
        startLocation();
    }

    private void buildScreen() {

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setBackgroundColor(Color.WHITE);

        TextView title =
                new TextView(this);

        title.setText(
                "LIVE_RIDE".equals(mode)
                        ? "🚕 LIVE RIDE MAP"
                        : "📍 SELECT DESTINATION"
        );

        title.setTextSize(22);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);
        title.setPadding(10, 18, 10, 18);

        root.addView(title);

        locationText =
                new TextView(this);

        locationText.setText(
                "📡 Getting GPS location..."
        );

        locationText.setTextSize(15);
        locationText.setTextColor(Color.DKGRAY);
        locationText.setPadding(
                15, 5, 15, 5
        );

        root.addView(locationText);

        addressText =
                new TextView(this);

        addressText.setText(
                "Address: waiting for GPS..."
        );

        addressText.setTextSize(15);
        addressText.setTextColor(Color.DKGRAY);
        addressText.setPadding(
                15, 5, 15, 10
        );

        root.addView(addressText);

        if (!"LIVE_RIDE".equals(mode)) {

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

            searchInput.setPadding(
                    15, 5, 15, 5
            );

            searchRow.addView(
                    searchInput,
                    new LinearLayout.LayoutParams(
                            0,
                            58,
                            1
                    )
            );

            Button searchButton =
                    new Button(this);

            searchButton.setText("SEARCH");
            searchButton.setTextColor(Color.WHITE);
            searchButton.setBackgroundColor(
                    Color.rgb(0, 130, 70)
            );

            searchButton.setOnClickListener(
                    v -> {

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
                    }
            );

            LinearLayout.LayoutParams searchParams =
                    new LinearLayout.LayoutParams(
                            125,
                            58
                    );

            searchParams.setMargins(
                    8, 0, 0, 0
            );

            searchRow.addView(
                    searchButton,
                    searchParams
            );

            searchContainer.addView(
                    searchRow
            );

            root.addView(
                    searchContainer
            );
        }

        webView =
                new WebView(this);

        WebSettings settings =
                webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setGeolocationEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        webView.setFocusable(false);
        webView.setFocusableInTouchMode(false);

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

        if (!"LIVE_RIDE".equals(mode)) {

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

        closeButton.setText(
                "CLOSE MAP"
        );

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
                "content='width=device-width, " +
                "initial-scale=1.0, " +
                "maximum-scale=1.0, " +
                "user-scalable=no'>" +

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
                "overflow:hidden;" +
                "}" +

                ".leaflet-control-attribution{" +
                "font-size:9px;" +
                "}" +

                "</style>" +

                "</head>" +

                "<body>" +

                "<div id='map'></div>" +

                "<script>" +

                "var map=" +
                "L.map('map'," +
                "{zoomControl:true}).setView(" +
                "[14.5995,120.9842],12);" +

                "L.tileLayer(" +
                "'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'," +
                "{" +
                "maxZoom:19," +
                "attribution:'© OpenStreetMap contributors'" +
                "}" +
                ").addTo(map);" +

                "var gpsMarker=null;" +
                "var destinationMarker=null;" +

                "function setGPS(lat,lng){" +

                "if(gpsMarker===null){" +

                "gpsMarker=L.marker(" +
                "[lat,lng]," +
                "{title:'Your Location'}" +
                ").addTo(map);" +

                "}else{" +

                "gpsMarker.setLatLng(" +
                "[lat,lng]" +
                ");" +

                "}" +

                "gpsMarker.bindPopup(" +
                "'📍 Your GPS location'" +
                ");" +

                "}" +

                "function setDestination(lat,lng,name){" +

                "if(destinationMarker===null){" +

                "destinationMarker=L.marker(" +
                "[lat,lng]," +
                "{title:name||'Destination'}" +
                ").addTo(map);" +

                "}else{" +

                "destinationMarker.setLatLng(" +
                "[lat,lng]" +
                ");" +

                "}" +

                "destinationMarker.bindPopup(" +
                "name||'Destination'" +
                ").openPopup();" +

                "map.setView(" +
                "[lat,lng],17" +
                ");" +

                "}" +

                "map.on('click',function(e){" +

                "AndroidMap.mapTap(" +
                "e.latlng.lat," +
                "e.latlng.lng" +
                ");" +

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

        if (currentLocation != null) {

            updateMap(
                    currentLocation.getLatitude(),
                    currentLocation.getLongitude()
            );
        }
    }

    private class MapBridge {

        @JavascriptInterface
        public void mapTap(
                double lat,
                double lng
        ) {

            runOnUiThread(() -> {

                currentLat = lat;
                currentLng = lng;

                destinationSelected = true;

                updateCoordinates(
                        lat,
                        lng
                );

                updateMapDestination(
                        lat,
                        lng,
                        "Selected destination"
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
                            @NonNull Location location
                    ) {

                        currentLocation =
                                location;

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

                        if (!destinationSelected) {

                            reverseGeocode(
                                    currentLat,
                                    currentLng
                            );
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

                currentLocation = best;

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

        } catch (SecurityException e) {

            locationText.setText(
                    "📍 Location permission required."
            );
        }
    }

    private Location chooseBest(
            Location a,
            Location b
    ) {

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
            double lng
    ) {

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

    private void updateMap(
            double lat,
            double lng
    ) {

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

    private void updateMapDestination(
            double lat,
            double lng,
            String name
    ) {

        if (webView == null
                || !mapReady) {
            return;
        }

        String safeName =
                name == null
                        ? "Destination"
                        : name.replace(
                                "\\",
                                "\\\\"
                        ).replace(
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

        if (currentLocation == null) {

            Toast.makeText(
                    this,
                    "Waiting for GPS...",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        currentLat =
                currentLocation.getLatitude();

        currentLng =
                currentLocation.getLongitude();

        currentPlaceName = "";
        currentAddress = "";

        destinationSelected = true;

        updateCoordinates(
                currentLat,
                currentLng
        );

        updateMapDestination(
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

        addressText.setText(
                "🔎 Searching: " + query
        );

        new Thread(() -> {

            JSONObject result = null;

            try {
                result =
                        searchPhoton(query);
            } catch (Exception ignored) {
            }

            if (result == null) {

                try {
                    result =
                            searchNominatim(query);
                } catch (Exception ignored) {
                }
            }

            final JSONObject finalResult =
                    result;

            runOnUiThread(() -> {

                if (finalResult == null) {

                    addressText.setText(
                            "❌ Place not found.\n"
                                    + "Try the full place name."
                    );

                    Toast.makeText(
                            this,
                            "Place not found.",
                            Toast.LENGTH_SHORT
                    ).show();

                    return;
                }

                try {

                    double lat =
                            finalResult.getDouble(
                                    "lat"
                            );

                    double lng =
                            finalResult.getDouble(
                                    "lon"
                            );

                    String name =
                            finalResult.optString(
                                    "name",
                                    ""
                            );

                    String address =
                            finalResult.optString(
                                    "address",
                                    ""
                            );

                    currentLat = lat;
                    currentLng = lng;

                    currentPlaceName =
                            name.trim();

                    currentAddress =
                            address.trim();

                    if (currentPlaceName.isEmpty()) {
                        currentPlaceName = query;
                    }

                    if (currentAddress.isEmpty()) {
                        currentAddress =
                                finalResult.optString(
                                        "display_name",
                                        currentPlaceName
                                );
                    }

                    destinationSelected = true;

                    updateCoordinates(
                            lat,
                            lng
                    );

                    updateAddressText();

                    updateMapDestination(
                            lat,
                            lng,
                            currentPlaceName
                    );

                    Toast.makeText(
                            this,
                            "✅ Destination selected.",
                            Toast.LENGTH_SHORT
                    ).show();

                } catch (Exception e) {

                    addressText.setText(
                            "❌ Unable to read search result."
                    );
                }
            });

        }).start();
    }

    private JSONObject searchPhoton(
            String query
    ) throws Exception {

        String encoded =
                URLEncoder.encode(
                        query,
                        "UTF-8"
                );

        String url =
                "https://photon.komoot.io/api/"
                        + "?q="
                        + encoded
                        + "&limit=10";

        JSONObject object =
                getJson(
                        url,
                        10000
                );

        JSONArray features =
                object.optJSONArray(
                        "features"
                );

        if (features == null
                || features.length() == 0) {
            return null;
        }

        return chooseBestPhoton(
                features,
                query
        );
    }

    private JSONObject chooseBestPhoton(
            JSONArray features,
            String query
    ) {

        JSONObject best = null;

        double bestScore =
                -Double.MAX_VALUE;

        String q =
                query.toLowerCase(
                        Locale.US
                ).trim();

        for (int i = 0;
             i < features.length();
             i++) {

            JSONObject item =
                    features.optJSONObject(i);

            if (item == null) {
                continue;
            }

            JSONObject properties =
                    item.optJSONObject(
                            "properties"
                    );

            JSONObject geometry =
                    item.optJSONObject(
                            "geometry"
                    );

            if (properties == null
                    || geometry == null) {
                continue;
            }

            JSONArray coordinates =
                    geometry.optJSONArray(
                            "coordinates"
                    );

            if (coordinates == null
                    || coordinates.length() < 2) {
                continue;
            }

            double lng =
                    coordinates.optDouble(0);

            double lat =
                    coordinates.optDouble(1);

            String name =
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

            double score = 0;

            String lowerName =
                    name.toLowerCase(
                            Locale.US
                    );

            if (lowerName.equals(q)) {
                score += 500;
            }

            if (lowerName.contains(q)) {
                score += 300;
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
                score += 20;
            }

            if (currentLocation != null) {

                float[] distance =
                        new float[1];

                Location.distanceBetween(
                        currentLat,
                        currentLng,
                        lat,
                        lng,
                        distance
                );

                score -= Math.min(
                        distance[0] / 1000.0,
                        100
                );
            }

            if (score > bestScore) {

                bestScore = score;
                best = item;
            }
        }

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

        JSONArray coordinates =
                geometry.optJSONArray(
                        "coordinates"
                );

        double lng =
                coordinates.optDouble(0);

        double lat =
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
                lat
        );

        result.put(
                "lon",
                lng
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
    }

    private JSONObject searchNominatim(
            String query
    ) throws Exception {

        String encoded =
                URLEncoder.encode(
                        query,
                        "UTF-8"
                );

        String url =
                "https://nominatim.openstreetmap.org/search"
                        + "?format=jsonv2"
                        + "&q="
                        + encoded
                        + "&limit=10"
                        + "&addressdetails=1"
                        + "&namedetails=1"
                        + "&accept-language=en";

        JSONArray results =
                getJsonArray(
                        url,
                        10000
                );

        if (results.length() == 0) {
            return null;
        }

        return chooseBestNominatim(
                results,
                query
        );
    }

    private JSONObject chooseBestNominatim(
            JSONArray results,
            String query
    ) {

        JSONObject best = null;

        double bestScore =
                -Double.MAX_VALUE;

        String q =
                query.toLowerCase(
                        Locale.US
                ).trim();

        for (int i = 0;
             i < results.length();
             i++) {

            JSONObject item =
                    results.optJSONObject(i);

            if (item == null) {
                continue;
            }

            String display =
                    item.optString(
                            "display_name",
                            ""
                    );

            String name =
                    item.optString(
                            "name",
                            ""
                    );

            String lowerDisplay =
                    display.toLowerCase(
                            Locale.US
                    );

            String lowerName =
                    name.toLowerCase(
                            Locale.US
                    );

            double score = 0;

            if (lowerName.equals(q)) {
                score += 500;
            }

            if (lowerName.contains(q)) {
                score += 300;
            }

            if (lowerDisplay.contains(q)) {
                score += 150;
            }

            String country =
                    item.optString(
                            "type",
                            ""
                    );

            if (!country.isEmpty()) {
                score += 5;
            }

            if (currentLocation != null) {

                try {

                    double lat =
                            Double.parseDouble(
                                    item.optString(
                                            "lat",
                                            "0"
                                    )
                            );

                    double lng =
                            Double.parseDouble(
                                    item.optString(
                                            "lon",
                                            "0"
                                    )
                            );

                    float[] distance =
                            new float[1];

                    Location.distanceBetween(
                            currentLat,
                            currentLng,
                            lat,
                            lng,
                            distance
                    );

                    score -= Math.min(
                            distance[0] / 1000.0,
                            100
                    );

                } catch (Exception ignored) {
                }
            }

            if (score > bestScore) {

                bestScore = score;
                best = item;
            }
        }

        if (best == null) {
            return null;
        }

        double lat =
                Double.parseDouble(
                        best.optString(
                                "lat",
                                "0"
                        )
                );

        double lng =
                Double.parseDouble(
                        best.optString(
                                "lon",
                                "0"
                        )
                );

        String name =
                best.optString(
                        "name",
                        ""
                );

        if (name.isEmpty()) {

            JSONObject namedetails =
                    best.optJSONObject(
                            "namedetails"
                    );

            if (namedetails != null) {

                name =
                        namedetails.optString(
                                "name",
                                ""
                        );
            }
        }

        JSONObject addressObject =
                best.optJSONObject(
                        "address"
                );

        String address =
                buildNominatimAddress(
                        addressObject
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

        try {

            result.put(
                    "lat",
                    lat
            );

            result.put(
                    "lon",
                    lng
            );

            result.put(
                    "name",
                    name
            );

            result.put(
                    "address",
                    address
            );

        } catch (Exception ignored) {
        }

        return result;
    }

    private void reverseGeocode(
            double lat,
            double lng
    ) {

        new Thread(() -> {

            String name = "";
            String address = "";

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

                        Address result =
                                results.get(0);

                        name =
                                result.getFeatureName();

                        if (name == null) {
                            name = "";
                        }

                        address =
                                buildAndroidAddress(
                                        result
                                );
                    }
                }

            } catch (Exception ignored) {
            }

            if (address.isEmpty()) {

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
                                    10000
                            );

                    JSONObject addressObject =
                            object.optJSONObject(
                                    "address"
                            );

                    address =
                            buildNominatimAddress(
                                    addressObject
                            );

                    if (address.isEmpty()) {

                        address =
                                object.optString(
                                        "display_name",
                                        ""
                                );
                    }

                    if (name.isEmpty()) {

                        name =
                                object.optString(
                                        "name",
                                        ""
                                );
                    }

                    if (name.isEmpty()) {

                        JSONObject namedetails =
                                object.optJSONObject(
                                        "namedetails"
                                );

                        if (namedetails != null) {

                            name =
                                    namedetails.optString(
                                            "name",
                                            ""
                                    );
                        }
                    }

                } catch (Exception ignored) {
                }
            }

            final String finalName =
                    name == null
                            ? ""
                            : name.trim();

            final String finalAddress =
                    address == null
                            ? ""
                            : address.trim();

            runOnUiThread(() -> {

                if (!finalName.isEmpty()) {

                    currentPlaceName =
                            finalName;
                }

                if (!finalAddress.isEmpty()) {

                    currentAddress =
                            finalAddress;
                }

                updateAddressText();
            });

        }).start();
    }

    private String buildAndroidAddress(
            Address address
    ) {

        if (address == null) {
            return "";
        }

        String line =
                address.getAddressLine(0);

        return line == null
                ? ""
                : line.trim();
    }

    private String buildPhotonAddress(
            JSONObject properties
    ) {

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

    private String buildNominatimAddress(
            JSONObject address
    ) {

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

    private String firstNonEmpty(
            String... values
    ) {

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
            String value
    ) {

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

    private void updateAddressText() {

        if (!currentPlaceName.isEmpty()) {

            addressText.setText(
                    "📍 "
                            + currentPlaceName
                            + "\n"
                            + currentAddress
            );

        } else if (!currentAddress.isEmpty()) {

            addressText.setText(
                    "📍 "
                            + currentAddress
            );
        }
    }

    private JSONObject getJson(
            String urlString,
            int timeout
    ) throws Exception {

        return new JSONObject(
                getText(
                        urlString,
                        timeout
                )
        );
    }

    private JSONArray getJsonArray(
            String urlString,
            int timeout
    ) throws Exception {

        return new JSONArray(
                getText(
                        urlString,
                        timeout
                )
        );
    }

    private String getText(
            String urlString,
            int timeout
    ) throws Exception {

        HttpURLConnection connection = null;

        try {

            URL url =
                    new URL(urlString);

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

            connection.setUseCaches(false);

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

        if (!destinationSelected
                || (currentLat == 0.0
                && currentLng == 0.0)) {

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
            String value
    ) {

        return value == null
                ? ""
                : value.trim();
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
    protected void onResume() {

        super.onResume();

        if (webView != null) {
            webView.onResume();
        }
    }

    @Override
    protected void onPause() {

        if (webView != null) {
            webView.onPause();
        }

        super.onPause();
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
