
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
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
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
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapActivity extends Activity {

    private static final int LOCATION_PERMISSION = 4001;

    private MapView mapView;
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

    private String currentAddress = "";
    private String currentPlaceName = "";

    private Marker gpsMarker;
    private Marker destinationMarker;

    private String mode = "";
    private String rideId = "";

    private boolean destinationSelected = false;
    private boolean firstLocation = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance()
                .setUserAgentValue("SakayNa/1.0");

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

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
                "LIVE_RIDE".equals(mode)
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

        if (!"LIVE_RIDE".equals(mode)) {

            LinearLayout searchArea = new LinearLayout(this);
            searchArea.setOrientation(LinearLayout.VERTICAL);
            searchArea.setPadding(10, 5, 10, 8);
            searchArea.setBackgroundColor(
                    Color.rgb(245, 245, 245)
            );

            TextView label = new TextView(this);
            label.setText("🔎 SEARCH DESTINATION");
            label.setTextSize(17);
            label.setTextColor(Color.BLACK);

            searchArea.addView(label);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);

            searchInput = new EditText(this);
            searchInput.setHint(
                    "Jollibee, SM, street..."
            );
            searchInput.setTextSize(16);
            searchInput.setSingleLine(true);
            searchInput.setBackgroundColor(Color.WHITE);
            searchInput.setPadding(15, 5, 15, 5);

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

            LinearLayout.LayoutParams searchButtonParams =
                    new LinearLayout.LayoutParams(
                            125,
                            58
                    );

            searchButtonParams.setMargins(
                    8,
                    0,
                    0,
                    0
            );

            row.addView(
                    searchButton,
                    searchButtonParams
            );

            searchArea.addView(row);

            root.addView(searchArea);
        }

        mapView = new MapView(this);

        mapView.setTileSource(
                TileSourceFactory.MAPNIK
        );

        mapView.setMultiTouchControls(true);

        mapView.setBuiltInZoomControls(true);

        mapView.setMinZoomLevel(4.0);

        mapView.setMaxZoomLevel(20.0);

        root.addView(
                mapView,
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

        if (!"LIVE_RIDE".equals(mode)) {

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

        mapView.getController().setZoom(12.0);

        GeoPoint defaultPoint =
                new GeoPoint(
                        14.5995,
                        120.9842
                );

        mapView.getController()
                .setCenter(defaultPoint);

        mapView.invalidate();
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

                        currentLocation = location;

                        currentLat =
                                location.getLatitude();

                        currentLng =
                                location.getLongitude();

                        updateCoordinates();

                        updateGpsMarker();

                        if (firstLocation) {

                            firstLocation = false;

                            mapView.getController()
                                    .setCenter(
                                            new GeoPoint(
                                                    currentLat,
                                                    currentLng
                                            )
                                    );

                            mapView.getController()
                                    .setZoom(17.0);

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

                updateCoordinates();
                updateGpsMarker();

                mapView.getController()
                        .setCenter(
                                new GeoPoint(
                                        currentLat,
                                        currentLng
                                )
                        );

                mapView.getController()
                        .setZoom(17.0);

                reverseGeocode(
                        currentLat,
                        currentLng
                );

                firstLocation = false;
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

    private void updateCoordinates() {

        locationText.setText(
                "📍 GPS LOCATION\n"
                        + "Latitude: "
                        + String.format(
                        Locale.US,
                        "%.6f",
                        currentLat
                )
                        + "\nLongitude: "
                        + String.format(
                        Locale.US,
                        "%.6f",
                        currentLng
                )
        );
    }

    private void updateGpsMarker() {

        if (mapView == null) {
            return;
        }

        GeoPoint point =
                new GeoPoint(
                        currentLat,
                        currentLng
                );

        if (gpsMarker == null) {

            gpsMarker =
                    new Marker(mapView);

            gpsMarker.setTitle(
                    "📍 Your Location"
            );

            gpsMarker.setAnchor(
                    Marker.ANCHOR_CENTER,
                    Marker.ANCHOR_BOTTOM
            );

            mapView.getOverlays()
                    .add(gpsMarker);
        }

        gpsMarker.setPosition(point);

        mapView.invalidate();
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

        updateCoordinates();
        updateGpsMarker();

        GeoPoint point =
                new GeoPoint(
                        currentLat,
                        currentLng
                );

        mapView.getController()
                .setCenter(point);

        mapView.getController()
                .setZoom(18.0);

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
                "🔎 Searching for: "
                        + query
        );

        new Thread(() -> {

            JSONObject result = null;

            try {

                result =
                        searchAndroidGeocoder(
                                query
                        );

            } catch (Exception ignored) {
            }

            if (result == null) {

                try {

                    result =
                            searchPhoton(
                                    query
                            );

                } catch (Exception ignored) {
                }
            }

            if (result == null) {

                try {

                    result =
                            searchNominatim(
                                    query
                            );

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

                    destinationSelected = true;

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

                    GeoPoint point =
                            new GeoPoint(
                                    lat,
                                    lng
                            );

                    if (destinationMarker == null) {

                        destinationMarker =
                                new Marker(mapView);

                        destinationMarker.setAnchor(
                                Marker.ANCHOR_CENTER,
                                Marker.ANCHOR_BOTTOM
                        );

                        mapView.getOverlays()
                                .add(destinationMarker);
                    }

                    destinationMarker.setPosition(
                            point
                    );

                    destinationMarker.setTitle(
                            currentPlaceName
                    );

                    destinationMarker.setSnippet(
                            currentAddress
                    );

                    mapView.getController()
                            .setCenter(point);

                    mapView.getController()
                            .setZoom(18.0);

                    mapView.invalidate();

                    addressText.setText(
                            "📍 "
                                    + currentPlaceName
                                    + "\n"
                                    + currentAddress
                    );

                    destinationMarker.showInfoWindow();

                } catch (Exception e) {

                    addressText.setText(
                            "❌ Invalid search result."
                    );
                }
            });

        }).start();
    }

    private JSONObject searchAndroidGeocoder(
            String query
    ) throws Exception {

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

        Address best =
                chooseBestAndroidResult(
                        results,
                        query
                );

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

        if (name == null
                || name.trim().isEmpty()) {

            name = query;
        }

        result.put(
                "name",
                name
        );

        String address =
                buildAndroidAddress(
                        best
                );

        if (address.isEmpty()) {
            address =
                    best.getAddressLine(0);
        }

        result.put(
                "address",
                address == null
                        ? ""
                        : address
        );

        return result;
    }

    private Address chooseBestAndroidResult(
            List<Address> results,
            String query
    ) {

        Address best = null;
        double bestScore =
                -Double.MAX_VALUE;

        String q =
                query.toLowerCase(
                        Locale.US
                );

        for (Address item : results) {

            if (item == null) {
                continue;
            }

            double score = 0;

            String feature =
                    safeLower(
                            item.getFeatureName()
                    );

            String locality =
                    safeLower(
                            item.getLocality()
                    );

            String admin =
                    safeLower(
                            item.getAdminArea()
                    );

            String line =
                    safeLower(
                            item.getAddressLine(0)
                    );

            if (feature.equals(q)) {
                score += 500;
            }

            if (feature.contains(q)) {
                score += 300;
            }

            if (line.contains(q)) {
                score += 250;
            }

            if (locality.contains(q)) {
                score += 100;
            }

            if (admin.contains(q)) {
                score += 30;
            }

            if (item.getCountryCode() != null
                    && item.getCountryCode()
                    .equalsIgnoreCase("PH")) {
                score += 100;
            }

            if (currentLocation != null) {

                float[] distance =
                        new float[1];

                Location.distanceBetween(
                        currentLat,
                        currentLng,
                        item.getLatitude(),
                        item.getLongitude(),
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

        return best;
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

        JSONObject root =
                getJson(
                        url,
                        12000
                );

        JSONArray features =
                root.optJSONArray(
                        "features"
                );

        if (features == null
                || features.length() == 0) {
            return null;
        }

        JSONObject best =
                chooseBestPhoton(
                        features,
                        query
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

            if (p == null) {
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

            String lower =
                    name.toLowerCase(
                            Locale.US
                    );

            if (lower.equals(q)) {
                score += 500;
            }

            if (lower.contains(q)) {
                score += 300;
            }

            if (q.contains(lower)
                    && !lower.isEmpty()) {
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

                JSONObject geometry =
                        item.optJSONObject(
                                "geometry"
                        );

                if (geometry != null) {

                    JSONArray c =
                            geometry.optJSONArray(
                                    "coordinates"
                            );

                    if (c != null
                            && c.length() >= 2) {

                        double lng =
                                c.optDouble(0);

                        double lat =
                                c.optDouble(1);

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
                }
            }

            if (score > bestScore) {

                bestScore = score;
                best = item;
            }
        }

        return best;
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
                        + "&countrycodes=ph"
                        + "&accept-language=en";

        JSONArray results =
                getJsonArray(
                        url,
                        12000
                );

        if (results == null
                || results.length() == 0) {
            return null;
        }

        JSONObject best = null;
        double bestScore =
                -Double.MAX_VALUE;

        String q =
                query.toLowerCase(
                        Locale.US
                );

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

            String lower =
                    display.toLowerCase(
                            Locale.US
                    );

            double score = 0;

            if (lower.contains(q)) {
                score += 300;
            }

            String type =
                    item.optString(
                            "type",
                            ""
                    );

            if (!type.isEmpty()) {
                score += 10;
            }

            if (currentLocation != null) {

                double lat =
                        item.optDouble(
                                "lat",
                                0
                        );

                double lng =
                        item.optDouble(
                                "lon",
                                0
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
                best.optDouble(
                        "lat",
                        0
                );

        double lng =
                best.optDouble(
                        "lon",
                        0
                );

        JSONObject address =
                best.optJSONObject(
                        "address"
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

        String formatted =
                buildNominatimAddress(
                        address
                );

        if (formatted.isEmpty()) {

            formatted =
                    best.optString(
                            "display_name",
                            ""
                    );
        }

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
                formatted
        );

        return result;
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

        HttpURLConnection connection =
                null;

        try {

            URL url =
                    new URL(urlString);

            connection =
                    (HttpURLConnection)
                            url.openConnection();

            connection.setRequestMethod("GET");

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

    private void reverseGeocode(
            double lat,
            double lng
    ) {

        new Thread(() -> {

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

                        Address address =
                                results.get(0);

                        String formatted =
                                buildAndroidAddress(
                                        address
                                );

                        if (formatted.isEmpty()) {

                            formatted =
                                    address.getAddressLine(
                                            0
                                    );
                        }

                        String name =
                                address.getFeatureName();

                        final String finalName =
                                name == null
                                        ? ""
                                        : name;

                        final String finalAddress =
                                formatted == null
                                        ? ""
                                        : formatted;

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

                        return;
                    }
                }

            } catch (Exception ignored) {
            }

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

                JSONObject address =
                        object.optJSONObject(
                                "address"
                        );

                String formatted =
                        buildNominatimAddress(
                                address
                        );

                if (formatted.isEmpty()) {

                    formatted =
                            object.optString(
                                    "display_name",
                                    ""
                            );
                }

                String name =
                        object.optString(
                                "name",
                                ""
                        );

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

                final String finalName =
                        name;

                final String finalAddress =
                        formatted;

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

            } catch (Exception ignored) {

                runOnUiThread(() ->
                        addressText.setText(
                                "📍 GPS location active"
                        )
                );
            }

        }).start();
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

    private String safeLower(
            String value
    ) {

        return value == null
                ? ""
                : value.toLowerCase(
                        Locale.US
                );
    }

    private String safe(
            String value
    ) {

        return value == null
                ? ""
                : value.trim();
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

            for (int result : grantResults) {

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

        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    protected void onPause() {

        if (mapView != null) {
            mapView.onPause();
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

        if (mapView != null) {
            mapView.onDetach();
        }

        super.onDestroy();
    }
}
