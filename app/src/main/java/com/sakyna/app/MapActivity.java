
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
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapActivity extends Activity {

    private static final int LOCATION_PERMISSION = 7001;

    private MapView mapView;
    private EditText searchInput;
    private LinearLayout resultsContainer;
    private TextView statusText;

    private LocationManager locationManager;
    private LocationListener locationListener;

    private FirebaseFirestore db;
    private ListenerRegistration rideListener;
    private ListenerRegistration driverLocationListener;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private Marker currentMarker;
    private Marker destinationMarker;
    private Marker driverMarker;

    private double currentLat = 14.5995;
    private double currentLng = 120.9842;

    private double destinationLat = 0;
    private double destinationLng = 0;

    private String destinationName = "";
    private String destinationAddress = "";

    private String mode = "";
    private String rideId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance()
                .setUserAgentValue(
                        getPackageName()
                );

        db = FirebaseFirestore.getInstance();

        mode = getIntent().getStringExtra("mode");
        rideId = getIntent().getStringExtra("ride_id");

        if (mode == null) {
            mode = getIntent().getStringExtra("map_mode");
        }

        if (rideId == null) {
            rideId = getIntent().getStringExtra("rideId");
        }

        if (rideId == null) {
            rideId = "";
        }

        buildScreen();

        startLocation();

        if ("LIVE_RIDE".equalsIgnoreCase(mode)) {
            startLiveRide();
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

        searchInput.setSingleLine(true);
        searchInput.setTextSize(16);

        searchRow.addView(
                searchInput,
                new LinearLayout.LayoutParams(
                        0,
                        60,
                        1
                )
        );

        Button searchButton =
                new Button(this);

        searchButton.setText("SEARCH");

        searchButton.setOnClickListener(
                v -> searchPlace()
        );

        searchRow.addView(
                searchButton,
                new LinearLayout.LayoutParams(
                        150,
                        60
                )
        );

        root.addView(searchRow);

        statusText =
                new TextView(this);

        statusText.setText(
                "🗺️ OpenStreetMap"
        );

        statusText.setTextSize(15);
        statusText.setTextColor(
                Color.DKGRAY
        );

        statusText.setPadding(
                15,
                10,
                15,
                10
        );

        root.addView(statusText);

        mapView =
                new MapView(this);

        mapView.setTileSource(
                TileSourceFactory.MAPNIK
        );

        mapView.setMultiTouchControls(true);

        mapView.getController()
                .setZoom(15.0);

        mapView.getController()
                .setCenter(
                        new GeoPoint(
                                currentLat,
                                currentLng
                        )
                );

        root.addView(
                mapView,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        TextView resultsTitle =
                new TextView(this);

        resultsTitle.setText(
                "SEARCH RESULTS"
        );

        resultsTitle.setTextSize(18);
        resultsTitle.setTextColor(
                Color.BLACK
        );

        resultsTitle.setPadding(
                15,
                10,
                15,
                5
        );

        root.addView(resultsTitle);

        ScrollView resultScroll =
                new ScrollView(this);

        resultsContainer =
                new LinearLayout(this);

        resultsContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        resultsContainer.setPadding(
                15,
                5,
                15,
                15
        );

        resultScroll.addView(
                resultsContainer
        );

        root.addView(
                resultScroll,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        260
                )
        );

        Button confirmButton =
                new Button(this);

        confirmButton.setText(
                "✅ USE SELECTED DESTINATION"
        );

        confirmButton.setOnClickListener(
                v -> confirmDestination()
        );

        root.addView(confirmButton);

        Button myLocationButton =
                new Button(this);

        myLocationButton.setText(
                "📍 MY LOCATION"
        );

        myLocationButton.setOnClickListener(
                v -> showCurrentLocation()
        );

        root.addView(myLocationButton);

        setContentView(root);
    }

    private void startLocation() {

        locationManager =
                (LocationManager)
                        getSystemService(
                                Context.LOCATION_SERVICE
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

                        updateCurrentMarker();
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

    private void updateCurrentMarker() {

        if (mapView == null) {
            return;
        }

        if (currentMarker == null) {

            currentMarker =
                    new Marker(mapView);

            currentMarker.setTitle(
                    "Your location"
            );

            mapView.getOverlays()
                    .add(currentMarker);
        }

        currentMarker.setPosition(
                new GeoPoint(
                        currentLat,
                        currentLng
                )
        );

        mapView.invalidate();
    }

    private void showCurrentLocation() {

        GeoPoint point =
                new GeoPoint(
                        currentLat,
                        currentLng
                );

        mapView.getController()
                .animateTo(point);

        mapView.getController()
                .setZoom(17.0);

        updateCurrentMarker();
    }

    private void searchPlace() {

        final String query =
                searchInput.getText()
                        .toString()
                        .trim();

        if (query.isEmpty()) {

            Toast.makeText(
                    this,
                    "Enter a place, street, barangay or establishment.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        statusText.setText(
                "🔎 Searching..."
        );

        resultsContainer.removeAllViews();

        executor.execute(() -> {

            List<SearchResult> results =
                    new ArrayList<>();

            /*
             * Keep the search broad.
             *
             * Photon searches establishments,
             * streets, barangays and places.
             */
            searchPhoton(
                    query,
                    results
            );

            /*
             * Nominatim is used as a second source.
             */
            if (results.isEmpty()) {

                searchNominatim(
                        query,
                        results
                );
            }

            handler.post(() -> {

                if (results.isEmpty()) {

                    statusText.setText(
                            "❌ No matching place found."
                    );

                    return;
                }

                statusText.setText(
                        "📍 " + results.size()
                                + " result(s) found"
                );

                displayResults(results);
            });
        });
    }

    private void searchPhoton(
            String query,
            List<SearchResult> results) {

        HttpURLConnection connection = null;

        try {

            String encoded =
                    URLEncoder.encode(
                            query,
                            "UTF-8"
                    );

            String urlString =
                    "https://photon.komoot.io/api/"
                            + "?q="
                            + encoded
                            + "&limit=20"
                            + "&lat="
                            + currentLat
                            + "&lon="
                            + currentLng
                            + "&lang=en";

            URL url =
                    new URL(urlString);

            connection =
                    (HttpURLConnection)
                            url.openConnection();

            connection.setRequestMethod(
                    "GET"
            );

            connection.setConnectTimeout(
                    10000
            );

            connection.setReadTimeout(
                    10000
            );

            InputStream input =
                    connection.getInputStream();

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    input
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
                    root.optJSONArray(
                            "features"
                    );

            if (features == null) {
                return;
            }

            for (int i = 0;
                 i < features.length();
                 i++) {

                JSONObject feature =
                        features.getJSONObject(i);

                JSONObject geometry =
                        feature.optJSONObject(
                                "geometry"
                        );

                JSONObject properties =
                        feature.optJSONObject(
                                "properties"
                        );

                if (geometry == null) {
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
                        coordinates.getDouble(0);

                double lat =
                        coordinates.getDouble(1);

                String name =
                        getPhotonName(
                                properties
                        );

                String address =
                        getPhotonAddress(
                                properties,
                                name
                        );

                if (name.isEmpty()
                        && address.isEmpty()) {
                    continue;
                }

                results.add(
                        new SearchResult(
                                name,
                                address,
                                lat,
                                lng
                        )
                );
            }

        } catch (Exception ignored) {

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String getPhotonName(
            JSONObject properties) {

        if (properties == null) {
            return "";
        }

        String name =
                properties.optString(
                        "name",
                        ""
                );

        if (!name.isEmpty()) {
            return name;
        }

        name =
                properties.optString(
                        "street",
                        ""
                );

        return name;
    }

    private String getPhotonAddress(
            JSONObject properties,
            String name) {

        if (properties == null) {
            return name;
        }

        ArrayList<String> parts =
                new ArrayList<>();

        String street =
                properties.optString(
                        "street",
                        ""
                );

        String house =
                properties.optString(
                        "housenumber",
                        ""
                );

        String district =
                properties.optString(
                        "district",
                        ""
                );

        String city =
                properties.optString(
                        "city",
                        ""
                );

        String state =
                properties.optString(
                        "state",
                        ""
                );

        if (!house.isEmpty()) {
            parts.add(house);
        }

        if (!street.isEmpty()
                && !street.equalsIgnoreCase(name)) {
            parts.add(street);
        }

        if (!district.isEmpty()) {
            parts.add(district);
        }

        if (!city.isEmpty()) {
            parts.add(city);
        }

        if (!state.isEmpty()) {
            parts.add(state);
        }

        if (parts.isEmpty()) {
            return name;
        }

        return joinParts(parts);
    }

    private void searchNominatim(
            String query,
            List<SearchResult> results) {

        HttpURLConnection connection = null;

        try {

            String encoded =
                    URLEncoder.encode(
                            query
                                    + ", Philippines",
                            "UTF-8"
                    );

            String urlString =
                    "https://nominatim.openstreetmap.org/search"
                            + "?format=jsonv2"
                            + "&q="
                            + encoded
                            + "&limit=20"
                            + "&addressdetails=1"
                            + "&namedetails=1"
                            + "&accept-language=en";

            URL url =
                    new URL(urlString);

            connection =
                    (HttpURLConnection)
                            url.openConnection();

            connection.setRequestMethod(
                    "GET"
            );

            connection.setRequestProperty(
                    "User-Agent",
                    "SakayNa/1.0"
            );

            connection.setConnectTimeout(
                    10000
            );

            connection.setReadTimeout(
                    10000
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

            JSONArray array =
                    new JSONArray(
                            response.toString()
                    );

            for (int i = 0;
                 i < array.length();
                 i++) {

                JSONObject item =
                        array.getJSONObject(i);

                double lat =
                        Double.parseDouble(
                                item.getString(
                                        "lat"
                                )
                        );

                double lng =
                        Double.parseDouble(
                                item.getString(
                                        "lon"
                                )
                        );

                String name =
                        extractNominatimName(
                                item
                        );

                String address =
                        item.optString(
                                "display_name",
                                ""
                        );

                if (name.isEmpty()) {

                    name = address;
                }

                results.add(
                        new SearchResult(
                                name,
                                address,
                                lat,
                                lng
                        )
                );
            }

        } catch (Exception ignored) {

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String extractNominatimName(
            JSONObject item) {

        String name =
                item.optString(
                        "name",
                        ""
                );

        if (!name.isEmpty()) {
            return name;
        }

        JSONObject namedetails =
                item.optJSONObject(
                        "namedetails"
                );

        if (namedetails != null) {

            name =
                    namedetails.optString(
                            "name",
                            ""
                    );

            if (!name.isEmpty()) {
                return name;
            }

            name =
                    namedetails.optString(
                            "brand",
                            ""
                    );

            if (!name.isEmpty()) {
                return name;
            }
        }

        return "";
    }

    private void displayResults(
            List<SearchResult> results) {

        resultsContainer.removeAllViews();

        for (SearchResult result : results) {

            LinearLayout card =
                    new LinearLayout(this);

            card.setOrientation(
                    LinearLayout.VERTICAL
            );

            card.setPadding(
                    20,
                    20,
                    20,
                    20
            );

            card.setBackgroundColor(
                    Color.rgb(
                            245,
                            245,
                            245
                    )
            );

            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );

            params.setMargins(
                    0,
                    0,
                    0,
                    12
            );

            card.setLayoutParams(
                    params
            );

            TextView name =
                    new TextView(this);

            name.setText(
                    "📍 "
                            + result.name
            );

            name.setTextSize(18);
            name.setTextColor(
                    Color.BLACK
            );

            card.addView(name);

            TextView address =
                    new TextView(this);

            address.setText(
                    result.address
            );

            address.setTextSize(14);
            address.setTextColor(
                    Color.DKGRAY
            );

            address.setPadding(
                    0,
                    8,
                    0,
                    8
            );

            card.addView(address);

            TextView coordinates =
                    new TextView(this);

            coordinates.setText(
                    String.format(
                            Locale.US,
                            "%.6f, %.6f",
                            result.lat,
                            result.lng
                    )
            );

            coordinates.setTextSize(12);
            coordinates.setTextColor(
                    Color.GRAY
            );

            card.addView(
                    coordinates
            );

            Button select =
                    new Button(this);

            select.setText(
                    "USE THIS DESTINATION"
            );

            select.setOnClickListener(
                    v -> selectDestination(
                            result
                    )
            );

            card.addView(select);

            resultsContainer.addView(
                    card
            );
        }
    }

    private void selectDestination(
            SearchResult result) {

        destinationLat =
                result.lat;

        destinationLng =
                result.lng;

        destinationName =
                result.name;

        destinationAddress =
                result.address;

        GeoPoint point =
                new GeoPoint(
                        destinationLat,
                        destinationLng
                );

        if (destinationMarker != null) {

            mapView.getOverlays()
                    .remove(
                            destinationMarker
                    );
        }

        destinationMarker =
                new Marker(mapView);

        destinationMarker.setPosition(
                point
        );

        destinationMarker.setTitle(
                destinationName
        );

        destinationMarker.setSnippet(
                destinationAddress
        );

        mapView.getOverlays()
                .add(destinationMarker);

        mapView.getController()
                .animateTo(point);

        mapView.getController()
                .setZoom(17.0);

        mapView.invalidate();

        statusText.setText(
                "✅ SELECTED: "
                        + destinationName
        );

        Toast.makeText(
                this,
                "Destination selected.",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void confirmDestination() {

        if (destinationLat == 0
                || destinationLng == 0) {

            Toast.makeText(
                    this,
                    "Search and select a destination first.",
                    Toast.LENGTH_SHORT
            ).show();

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
                "destinationName",
                destinationName
        );

        result.putExtra(
                "destination_address",
                destinationAddress
        );

        result.putExtra(
                "latitude",
                destinationLat
        );

        result.putExtra(
                "longitude",
                destinationLng
        );

        setResult(
                RESULT_OK,
                result
        );

        finish();
    }

    private void startLiveRide() {

        if (rideId.isEmpty()) {

            statusText.setText(
                    "No active ride."
            );

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

                                    updateLiveRide(
                                            snapshot
                                    );
                                }
                        );
    }

    private void updateLiveRide(
            DocumentSnapshot ride) {

        Double passengerLat =
                getDouble(
                        ride,
                        "pickupLatitude"
                );

        Double passengerLng =
                getDouble(
                        ride,
                        "pickupLongitude"
                );

        Double destLat =
                getDouble(
                        ride,
                        "destinationLatitude"
                );

        Double destLng =
                getDouble(
                        ride,
                        "destinationLongitude"
                );

        if (destLat == null) {

            destLat =
                    getDouble(
                            ride,
                            "destination_latitude"
                    );
        }

        if (destLng == null) {

            destLng =
                    getDouble(
                            ride,
                            "destination_longitude"
                    );
        }

        if (destLat != null
                && destLng != null) {

            destinationLat =
                    destLat;

            destinationLng =
                    destLng;

            destinationName =
                    firstNonEmpty(
                            ride.getString(
                                    "destinationName"
                            ),
                            ride.getString(
                                    "destination"
                            ),
                            "Destination"
                    );

            destinationAddress =
                    firstNonEmpty(
                            ride.getString(
                                    "destination_address"
                            ),
                            destinationName
                    );

            showDestinationMarker();
        }

        String status =
                ride.getString(
                        "status"
                );

        statusText.setText(
                "🚕 RIDE STATUS: "
                        + (
                        status == null
                                ? "UNKNOWN"
                                : status
                )
        );

        if (passengerLat != null
                && passengerLng != null) {

            addPassengerMarker(
                    passengerLat,
                    passengerLng
            );
        }

        String driverId =
                ride.getString(
                        "driverId"
                );

        if (driverId != null
                && !driverId.isEmpty()) {

            listenToDriverLocation(
                    driverId
            );
        }
    }

    private void showDestinationMarker() {

        if (destinationMarker != null) {

            mapView.getOverlays()
                    .remove(
                            destinationMarker
                    );
        }

        destinationMarker =
                new Marker(mapView);

        destinationMarker.setPosition(
                new GeoPoint(
                        destinationLat,
                        destinationLng
                )
        );

        destinationMarker.setTitle(
                destinationName
        );

        destinationMarker.setSnippet(
                destinationAddress
        );

        mapView.getOverlays()
                .add(
                        destinationMarker
                );

        mapView.invalidate();
    }

    private void addPassengerMarker(
            double lat,
            double lng) {

        if (currentMarker != null) {

            mapView.getOverlays()
                    .remove(
                            currentMarker
                    );
        }

        currentMarker =
                new Marker(mapView);

        currentMarker.setPosition(
                new GeoPoint(
                        lat,
                        lng
                )
        );

        currentMarker.setTitle(
                "Pickup"
        );

        mapView.getOverlays()
                .add(
                        currentMarker
                );

        mapView.invalidate();
    }

    private void listenToDriverLocation(
            String driverId) {

        if (driverLocationListener != null) {
            driverLocationListener.remove();
        }

        driverLocationListener =
                db.collection(
                        "driverLocations"
                )
                        .document(driverId)
                        .addSnapshotListener(
                                (snapshot, error) -> {

                                    if (error != null
                                            || snapshot == null
                                            || !snapshot.exists()) {
                                        return;
                                    }

                                    Double lat =
                                            getDouble(
                                                    snapshot,
                                                    "latitude"
                                            );

                                    Double lng =
                                            getDouble(
                                                    snapshot,
                                                    "longitude"
                                            );

                                    if (lat == null
                                            || lng == null) {
                                        return;
                                    }

                                    updateDriverMarker(
                                            lat,
                                            lng
                                    );
                                }
                        );
    }

    private void updateDriverMarker(
            double lat,
            double lng) {

        if (driverMarker != null) {

            mapView.getOverlays()
                    .remove(
                            driverMarker
                    );
        }

        driverMarker =
                new Marker(mapView);

        driverMarker.setPosition(
                new GeoPoint(
                        lat,
                        lng
                )
        );

        driverMarker.setTitle(
                "🚕 Driver"
        );

        mapView.getOverlays()
                .add(
                        driverMarker
                );

        mapView.invalidate();
    }

    private Double getDouble(
            DocumentSnapshot snapshot,
            String field) {

        Object value =
                snapshot.get(field);

        if (value instanceof Number) {

            return ((Number) value)
                    .doubleValue();
        }

        return null;
    }

    private String firstNonEmpty(
            String... values) {

        for (String value : values) {

            if (value != null
                    && !value.trim().isEmpty()) {

                return value;
            }
        }

        return "";
    }

    private String joinParts(
            List<String> parts) {

        StringBuilder builder =
                new StringBuilder();

        for (String part : parts) {

            if (part == null
                    || part.trim().isEmpty()) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append(", ");
            }

            builder.append(part);
        }

        return builder.toString();
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

        if (requestCode == LOCATION_PERMISSION) {

            if (grantResults.length > 0
                    && grantResults[0]
                    == PackageManager.PERMISSION_GRANTED) {

                beginLocationUpdates();
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

        if (rideListener != null) {
            rideListener.remove();
        }

        if (driverLocationListener != null) {
            driverLocationListener.remove();
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

        executor.shutdownNow();

        super.onDestroy();
    }

    private static class SearchResult {

        String name;
        String address;
        double lat;
        double lng;

        SearchResult(
                String name,
                String address,
                double lat,
                double lng) {

            this.name = name;
            this.address = address;
            this.lat = lat;
            this.lng = lng;
        }
    }
}
